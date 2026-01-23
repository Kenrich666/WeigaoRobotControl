package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.util.Log;

import com.keenon.sdk.component.charger.PeanutCharger;
import com.keenon.sdk.component.charger.common.Charger;
// SDK 的 ChargerInfo 通过完整包名引用以避免冲突

import com.weigao.robot.control.callback.IChargerCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.model.ChargerInfo;
import com.weigao.robot.control.service.IChargerService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 充电服务实现类
 * <p>
 * 封装 Peanut SDK 的 {@code PeanutCharger} 组件，提供充电控制功能。
 * </p>
 */
public class ChargerServiceImpl implements IChargerService {

    private static final String TAG = "ChargerServiceImpl";

    private final Context context;

    /** 回调列表（线程安全） */
    private final List<IChargerCallback> callbacks = new CopyOnWriteArrayList<>();

    /** Peanut SDK 充电组件 */
    private PeanutCharger peanutCharger;

    /** 当前充电桩ID */
    private int currentPileId = 0;

    /** 当前充电信息 */
    private final ChargerInfo chargerInfo;

    /** 是否正在充电 */
    private boolean isCharging = false;

    public ChargerServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        this.chargerInfo = new ChargerInfo();
        Log.d(TAG, "ChargerServiceImpl 已创建");
        initPeanutCharger();
    }

    /**
     * 初始化 PeanutCharger
     */
    private void initPeanutCharger() {
        try {
            PeanutCharger.Builder builder = new PeanutCharger.Builder()
                    .setPile(currentPileId)
                    .setListener(mChargerListener);
            peanutCharger = createPeanutCharger(builder);
            peanutCharger.execute();
            Log.d(TAG, "PeanutCharger 初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "PeanutCharger 初始化异常", e);
        }
    }

    /**
     * SDK 充电回调
     */
    private final Charger.Listener mChargerListener = new Charger.Listener() {
        @Override
        public void onChargerInfoChanged(int event, com.keenon.sdk.component.charger.common.ChargerInfo sdkInfo) {
            Log.d(TAG, "onChargerInfoChanged: event=" + event + ", power=" +
                    (sdkInfo != null ? sdkInfo.getPower() : "null"));

            // 更新充电信息
            if (sdkInfo != null) {
                chargerInfo.setPower(sdkInfo.getPower());
                chargerInfo.setEvent(sdkInfo.getEvent());
            }

            // 判断充电状态
            isCharging = isChargingEvent(event);

            // 通知回调
            notifyChargerInfoChanged(event, chargerInfo);
        }

        @Override
        public void onChargerStatusChanged(int status) {
            Log.d(TAG, "onChargerStatusChanged: status=" + status);
            chargerInfo.setStatus(status);
            notifyChargerStatusChanged(status);
        }

        @Override
        public void onError(int errorCode) {
            Log.e(TAG, "onError: " + errorCode);
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
                // 映射动作到 SDK 常量
                int sdkAction;
                switch (action) {
                    case CHARGE_ACTION_AUTO:
                        sdkAction = PeanutCharger.CHARGE_ACTION_AUTO;
                        break;
                    case CHARGE_ACTION_MANUAL:
                        sdkAction = PeanutCharger.CHARGE_ACTION_MANUAL;
                        break;
                    case CHARGE_ACTION_ADAPTER:
                        sdkAction = PeanutCharger.CHARGE_ACTION_ADAPTER;
                        break;
                    case CHARGE_ACTION_STOP:
                        sdkAction = PeanutCharger.CHARGE_ACTION_STOP;
                        break;
                    default:
                        sdkAction = action;
                }
                peanutCharger.performAction(sdkAction);

                // 更新本地状态
                if (action == CHARGE_ACTION_STOP) {
                    isCharging = false;
                }
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

    // ==================== 回调注册 ====================

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

    // ==================== 辅助方法 ====================

    /**
     * 判断是否是充电中的事件
     */
    private boolean isChargingEvent(int event) {
        // 根据 SDK 充电事件判断
        // 具体事件码需参考 SDK 文档
        return event >= 1 && event <= 3;
    }

    private void notifySuccess(IResultCallback<Void> callback) {
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    private void notifyError(IResultCallback<?> callback, int code, String message) {
        if (callback != null) {
            callback.onError(new ApiError(code, message));
        }
    }

    /**
     * 创建 PeanutCharger 实例的工厂方法。
     * <p>
     * 设计为 protected 以便在测试子类中重写并注入 Mock 对象。
     * </p>
     */
    protected PeanutCharger createPeanutCharger(PeanutCharger.Builder builder) {
        return builder.build();
    }
}
