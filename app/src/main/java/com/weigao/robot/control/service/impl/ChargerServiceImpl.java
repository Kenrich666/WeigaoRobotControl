package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.keenon.sdk.component.charger.PeanutCharger;
import com.keenon.sdk.component.charger.common.Charger;

import com.weigao.robot.control.callback.IChargerCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.SdkErrorCode;
import com.weigao.robot.control.model.ChargerInfo;
import com.weigao.robot.control.service.IChargerService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 充电服务实现类
 * <p>
 * 封装 Peanut SDK 的 {@code PeanutCharger} 组件，提供充电控制和状态监听功能。
 * </p>
 *
 * <h3>充电状态判断策略</h3>
 * <p>
 * 充电状态完全由 {@code onChargerStatusChanged(int status)} 回调决定（SDK文档5.8节）：
 * <ul>
 * <li>status=4（充电中）或 status=5（适配器充电中）→ isCharging=true</li>
 * <li>其他status值 → isCharging=false</li>
 * </ul>
 * {@code onChargerInfoChanged} 仅用于更新电量和事件数据，不影响充电状态。
 * </p>
 *
 * <h3>启动残留状态处理</h3>
 * <p>
 * SDK 在 {@code execute()} 后可能沿用上次未正确结束的充电状态。
 * 因此初始化后延迟发送 STOP 指令清除残留，期间忽略充电状态上报。
 * </p>
 */
public class ChargerServiceImpl implements IChargerService {

    private static final String TAG = "ChargerServiceImpl";

    private final Context context;
    private final List<IChargerCallback> callbacks = new CopyOnWriteArrayList<>();
    private final ChargerInfo chargerInfo = new ChargerInfo();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private PeanutCharger peanutCharger;
    private int currentPileId = 0;
    private boolean isCharging = false;

    // ==================== 电源状态常量（SDK文档 5.8节） ====================

    private static final int CHARGER_STATUS_IDLE = 1; // 空闲
    private static final int CHARGER_STATUS_AUTO_GOING = 2; // 自动去充电中
    private static final int CHARGER_STATUS_MANUAL_GOING = 3; // 手动去充电中
    private static final int CHARGER_STATUS_CHARGING = 4; // 充电中
    private static final int CHARGER_STATUS_ADAPTER_CHARGING = 5; // 适配器充电中
    private static final int CHARGER_STATUS_CANCELLING = 6; // 正在取消充电

    // ==================== 启动残留状态清除 ====================

    /** 启动保护标志：在延迟STOP生效前，不信任 status=4/5 */
    private boolean initResetPending = true;

    /** 延迟发送 STOP 的时间（毫秒），等待 SDK 充分初始化 */
    private static final long INIT_STOP_DELAY_MS = 3000;

    // ==================== 构造与初始化 ====================

    public ChargerServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        Log.d(TAG, "ChargerServiceImpl 已创建");
        initPeanutCharger();
    }

    /**
     * 初始化 PeanutCharger
     * <p>
     * execute() 后延迟3秒发送 STOP 清除残留充电状态。
     * 在 STOP 生效前（收到非充电状态之前），忽略 status=4/5 防止误判。
     * 如果机器人真的在充电桩上，STOP 后 SDK 会重新检测并恢复充电状态。
     * </p>
     */
    private void initPeanutCharger() {
        try {
            PeanutCharger.Builder builder = new PeanutCharger.Builder()
                    .setPile(currentPileId)
                    .setListener(mChargerListener);
            peanutCharger = createPeanutCharger(builder);
            peanutCharger.execute();
            Log.d(TAG, "PeanutCharger 初始化成功");

            // 延迟发送 STOP 清除残留状态
            initResetPending = true;
            handler.postDelayed(() -> {
                if (peanutCharger != null && initResetPending) {
                    Log.d(TAG, "发送延迟STOP指令清除残留充电状态");
                    peanutCharger.performAction(PeanutCharger.CHARGE_ACTION_STOP);
                }
            }, INIT_STOP_DELAY_MS);
        } catch (Exception e) {
            Log.e(TAG, "PeanutCharger 初始化异常", e);
        }
    }

    // ==================== SDK 回调 ====================

    private final Charger.Listener mChargerListener = new Charger.Listener() {

        /**
         * 电量/事件信息回调
         * <p>
         * 仅更新电量和事件数据，不改变充电状态。
         * </p>
         */
        @Override
        public void onChargerInfoChanged(int event,
                com.keenon.sdk.component.charger.common.ChargerInfo sdkInfo) {
            Log.d(TAG, "onChargerInfoChanged: event=" + event
                    + ", power=" + (sdkInfo != null ? sdkInfo.getPower() : "null")
                    + ", isCharging=" + isCharging);

            if (sdkInfo != null) {
                chargerInfo.setPower(sdkInfo.getPower());
                chargerInfo.setEvent(sdkInfo.getEvent());
            }
            chargerInfo.setCharging(isCharging);
            notifyChargerInfoChanged(event, chargerInfo);
        }

        /**
         * 充电状态变化回调（充电状态唯一来源）
         * <p>
         * 启动保护期内：status=4/5 被忽略（可能是残留），status=1/6 表示 STOP 已生效。
         * 正常模式：status=4/5 → 充电中，其他 → 非充电。
         * </p>
         */
        @Override
        public void onChargerStatusChanged(int status) {
            Log.d(TAG, "onChargerStatusChanged: status=" + status
                    + "(" + getStatusDesc(status) + ")"
                    + ", isCharging=" + isCharging
                    + ", initResetPending=" + initResetPending);

            // 启动保护：等待延迟STOP生效
            if (initResetPending) {
                if (status == CHARGER_STATUS_IDLE || status == CHARGER_STATUS_CANCELLING) {
                    initResetPending = false;
                    Log.d(TAG, "STOP已生效(status=" + status + ")，恢复正常检测");
                } else if (status == CHARGER_STATUS_CHARGING
                        || status == CHARGER_STATUS_ADAPTER_CHARGING) {
                    Log.d(TAG, "保护期内，忽略残留充电状态(status=" + status + ")");
                    chargerInfo.setStatus(status);
                    notifyChargerStatusChanged(status);
                    return;
                }
            }

            // 正常充电状态判断
            boolean wasCharging = isCharging;
            isCharging = (status == CHARGER_STATUS_CHARGING
                    || status == CHARGER_STATUS_ADAPTER_CHARGING);

            chargerInfo.setStatus(status);
            chargerInfo.setCharging(isCharging);

            if (isCharging != wasCharging) {
                Log.d(TAG, "充电状态变化: " + (isCharging ? "开始充电" : "停止充电"));
                notifyChargerInfoChanged(chargerInfo.getEvent(), chargerInfo);
            }

            notifyChargerStatusChanged(status);
        }

        @Override
        public void onError(int errorCode) {
            Log.e(TAG, "充电错误: errorCode=" + errorCode);
        }
    };

    // ==================== 充电控制 ====================

    @Override
    public void startAutoCharge(IResultCallback<Void> callback) {
        Log.d(TAG, "startAutoCharge");
        performAction(CHARGE_ACTION_AUTO, callback);
    }

    @Override
    public void startManualCharge(IResultCallback<Void> callback) {
        Log.d(TAG, "startManualCharge");
        performAction(CHARGE_ACTION_MANUAL, callback);
    }

    @Override
    public void startAdapterCharge(IResultCallback<Void> callback) {
        Log.d(TAG, "startAdapterCharge");
        performAction(CHARGE_ACTION_ADAPTER, callback);
    }

    @Override
    public void stopCharge(IResultCallback<Void> callback) {
        Log.d(TAG, "stopCharge");
        performAction(CHARGE_ACTION_STOP, callback);
    }

    @Override
    public void performAction(int action, IResultCallback<Void> callback) {
        Log.d(TAG, "performAction: action=" + action);
        try {
            if (peanutCharger != null) {
                int sdkAction = switch (action) {
                    case CHARGE_ACTION_AUTO -> PeanutCharger.CHARGE_ACTION_AUTO;
                    case CHARGE_ACTION_MANUAL -> PeanutCharger.CHARGE_ACTION_MANUAL;
                    case CHARGE_ACTION_ADAPTER -> PeanutCharger.CHARGE_ACTION_ADAPTER;
                    case CHARGE_ACTION_STOP -> PeanutCharger.CHARGE_ACTION_STOP;
                    default -> action;
                };
                peanutCharger.performAction(sdkAction);
                if (action == CHARGE_ACTION_STOP) {
                    isCharging = false;
                    chargerInfo.setCharging(false);
                    chargerInfo.setStatus(CHARGER_STATUS_IDLE);
                    chargerInfo.setEvent(SdkErrorCode.CHARGER_EVENT_EXIT);
                    notifyChargerInfoChanged(SdkErrorCode.CHARGER_EVENT_EXIT, chargerInfo);
                }

                initResetPending = false;
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "performAction 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    // ==================== 充电桩设置 ====================

    @Override
    public void setChargePile(int pileId, IResultCallback<Void> callback) {
        Log.d(TAG, "setChargePile: " + pileId);
        try {
            this.currentPileId = pileId;
            if (peanutCharger != null) {
                peanutCharger.setPile(pileId);
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "setChargePile 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void getChargePile(IResultCallback<Integer> callback) {
        if (callback != null) {
            try {
                int pile = currentPileId;
                if (peanutCharger != null) {
                    pile = peanutCharger.getPile();
                }
                callback.onSuccess(pile);
            } catch (Exception e) {
                Log.e(TAG, "getChargePile 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    // ==================== 状态查询 ====================

    @Override
    public void getChargerInfo(IResultCallback<ChargerInfo> callback) {
        if (callback != null) {
            callback.onSuccess(chargerInfo);
        }
    }

    @Override
    public void getBatteryLevel(IResultCallback<Integer> callback) {
        if (callback != null) {
            callback.onSuccess(chargerInfo.getPower());
        }
    }

    @Override
    public void isCharging(IResultCallback<Boolean> callback) {
        if (callback != null) {
            callback.onSuccess(isCharging);
        }
    }

    // ==================== 回调管理 ====================

    @Override
    public void registerCallback(IChargerCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
            Log.d(TAG, "回调已注册，当前数量：" + callbacks.size());
        }
    }

    @Override
    public void unregisterCallback(IChargerCallback callback) {
        if (callbacks.remove(callback)) {
            Log.d(TAG, "回调已注销，当前数量：" + callbacks.size());
        }
    }

    @Override
    public void release() {
        Log.d(TAG, "释放 ChargerService 资源");
        handler.removeCallbacksAndMessages(null);
        callbacks.clear();
        if (peanutCharger != null) {
            try {
                peanutCharger.release();
            } catch (Exception e) {
                Log.e(TAG, "释放 peanutCharger 异常", e);
            }
            peanutCharger = null;
        }
    }

    // ==================== 回调分发 ====================

    private void notifyChargerInfoChanged(int event, ChargerInfo info) {
        for (IChargerCallback callback : callbacks) {
            try {
                callback.onChargerInfoChanged(event, info);
            } catch (Exception e) {
                Log.e(TAG, "回调 onChargerInfoChanged 异常", e);
            }
        }
    }

    private void notifyChargerStatusChanged(int status) {
        for (IChargerCallback callback : callbacks) {
            try {
                callback.onChargerStatusChanged(status);
            } catch (Exception e) {
                Log.e(TAG, "回调 onChargerStatusChanged 异常", e);
            }
        }
    }

    private void notifySuccess(IResultCallback<Void> callback) {
        if (callback != null)
            callback.onSuccess(null);
    }

    private void notifyError(IResultCallback<?> callback, int code, String message) {
        if (callback != null)
            callback.onError(new ApiError(code, message));
    }

    // ==================== 工具方法 ====================

    /**
     * 创建 PeanutCharger 实例的工厂方法。
     * 设计为 protected 以便在测试子类中重写并注入 Mock 对象。
     */
    protected PeanutCharger createPeanutCharger(PeanutCharger.Builder builder) {
        return builder.build();
    }

    /** 获取充电状态描述（用于日志） */
    private static String getStatusDesc(int status) {
        return switch (status) {
            case CHARGER_STATUS_IDLE -> "空闲";
            case CHARGER_STATUS_AUTO_GOING -> "自动去充电中";
            case CHARGER_STATUS_MANUAL_GOING -> "手动去充电中";
            case CHARGER_STATUS_CHARGING -> "充电中";
            case CHARGER_STATUS_ADAPTER_CHARGING -> "适配器充电中";
            case CHARGER_STATUS_CANCELLING -> "正在取消充电";
            default -> "未知(" + status + ")";
        };
    }
}
