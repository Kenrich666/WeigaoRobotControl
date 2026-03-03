package com.weigao.robot.control.manager;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.keenon.sdk.scmIot.protopack.base.ProtoDev;
import com.keenon.sdk.sensor.uvlamp.SensorUVLamp;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IChargerCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.ChargerInfo;
import com.weigao.robot.control.model.DoorType;
import com.weigao.robot.control.service.IChargerService;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.service.impl.ProjectionDoorService;

/**
 * 紫外灯消毒全局管理器（单例）
 * <p>
 * 全局监听充电状态变化，当机器人开始充电时自动启动紫外灯消毒（20分钟），
 * 充电停止或倒计时结束时自动关闭紫外灯。
 * 不依赖任何 Activity/Fragment 的可见状态。
 * </p>
 */
public class UVDisinfectionManager {

    private static final String TAG = "UVDisinfectionManager";

    /** 消毒时长：20分钟 */
    private static final long UV_DISINFECTION_DURATION_MS = 20 * 60 * 1000;

    // ==================== 单例 ====================

    private static final UVDisinfectionManager INSTANCE = new UVDisinfectionManager();

    public static UVDisinfectionManager getInstance() {
        return INSTANCE;
    }

    private UVDisinfectionManager() {
    }

    // ==================== 状态 ====================

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean initialized = false;
    private boolean wasCharging = false;
    private boolean isDisinfecting = false;
    private CountDownTimer countdownTimer;
    private long remainingMs = 0;

    /** UI 状态变化监听器（可选，供 ChargerSettingsFragment 使用） */
    public interface OnStateChangeListener {
        void onDisinfectionStateChanged(boolean isDisinfecting, long remainingMs);
    }

    private OnStateChangeListener stateChangeListener;

    // ==================== 公开方法 ====================

    /**
     * 初始化：注册全局充电状态监听。应在 SDK 初始化完成后调用。
     */
    public void init() {
        if (initialized)
            return;
        initialized = true;

        IChargerService chargerService = ServiceManager.getInstance().getChargerService();
        if (chargerService == null) {
            Log.e(TAG, "充电服务不可用，紫外灯消毒管理器初始化失败");
            return;
        }

        chargerService.registerCallback(chargerCallback);
        Log.d(TAG, "紫外灯消毒管理器已初始化，开始监听充电状态");

        // 主动查询一次当前充电状态
        chargerService.getChargerInfo(new IResultCallback<ChargerInfo>() {
            @Override
            public void onSuccess(ChargerInfo result) {
                if (result != null) {
                    handler.post(() -> handleChargingState(result.isCharging()));
                }
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "初始查询充电状态失败: " + error.getMessage());
            }
        });
    }

    /**
     * 设置 UI 状态变化监听器
     */
    public void setStateChangeListener(OnStateChangeListener listener) {
        this.stateChangeListener = listener;
        // 立即通知当前状态
        if (listener != null) {
            listener.onDisinfectionStateChanged(isDisinfecting, remainingMs);
        }
    }

    /**
     * 移除 UI 状态变化监听器
     */
    public void removeStateChangeListener() {
        this.stateChangeListener = null;
    }

    /**
     * 手动停止消毒
     */
    public void stopDisinfection() {
        stopUVDisinfection();
    }

    /**
     * 是否正在消毒
     */
    public boolean isDisinfecting() {
        return isDisinfecting;
    }

    /**
     * 获取剩余时间（毫秒）
     */
    public long getRemainingMs() {
        return remainingMs;
    }

    // ==================== 充电状态监听 ====================

    private final IChargerCallback chargerCallback = new IChargerCallback() {
        @Override
        public void onChargerInfoChanged(int event, ChargerInfo chargerInfo) {
            Log.d(TAG, "【紫外灯】收到充电回调: event=" + event
                    + ", isCharging=" + (chargerInfo != null ? chargerInfo.isCharging() : "null")
                    + ", power=" + (chargerInfo != null ? chargerInfo.getPower() : "null"));
            if (chargerInfo != null) {
                handler.post(() -> handleChargingState(chargerInfo.isCharging()));
            }
        }

        @Override
        public void onChargerStatusChanged(int status) {
            Log.d(TAG, "【紫外灯】充电状态变化: status=" + status);
        }

        @Override
        public void onChargerError(int errorCode) {
            Log.e(TAG, "【紫外灯】充电错误: errorCode=" + errorCode);
        }
    };

    /** 防抖延迟：充电停止后等待5秒再真正关灯 */
    private static final long CHARGING_STOP_DEBOUNCE_MS = 5000;
    private Runnable pendingStopRunnable;

    private void handleChargingState(boolean isCharging) {
        Log.d(TAG, "充电状态更新: isCharging=" + isCharging + ", wasCharging=" + wasCharging);

        if (isCharging && !wasCharging) {
            // 充电开始：取消待执行的关灯操作（如果有），立即开灯
            if (pendingStopRunnable != null) {
                handler.removeCallbacks(pendingStopRunnable);
                pendingStopRunnable = null;
                Log.d(TAG, "【紫外灯】取消待执行的关灯操作（充电状态抖动）");
            }

            // 充电前确保投影灯关闭
            ensureProjectionLightOff();
            // 充电前确保舱门关闭
            ensureDoorsClosed();

            if (!isDisinfecting) {
                Log.d(TAG, "【紫外灯】检测到充电开始，启动紫外灯消毒");
                startUVDisinfection();
            }
        } else if (!isCharging && wasCharging) {
            // 充电停止：延迟5秒再关灯（防抖）
            Log.d(TAG, "【紫外灯】检测到充电停止信号，等待" + (CHARGING_STOP_DEBOUNCE_MS / 1000) + "秒确认...");
            if (pendingStopRunnable == null) {
                pendingStopRunnable = () -> {
                    Log.d(TAG, "【紫外灯】确认充电已停止，关闭紫外灯");
                    stopUVDisinfection();
                    pendingStopRunnable = null;
                };
                handler.postDelayed(pendingStopRunnable, CHARGING_STOP_DEBOUNCE_MS);
            }
        }
        wasCharging = isCharging;
    }

    // ==================== 紫外灯消毒控制 ====================

    private void startUVDisinfection() {
        if (isDisinfecting)
            return;
        isDisinfecting = true;

        // 打开所有紫外灯
        setAllUVLamps(true);

        // 启动20分钟倒计时
        countdownTimer = new CountDownTimer(UV_DISINFECTION_DURATION_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMs = millisUntilFinished;
                notifyStateChanged();
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "【紫外灯】消毒倒计时结束，自动关闭紫外灯");
                stopUVDisinfection();
            }
        };
        countdownTimer.start();
        remainingMs = UV_DISINFECTION_DURATION_MS;
        notifyStateChanged();
        Log.d(TAG, "【紫外灯】消毒已启动，倒计时20分钟");
    }

    private void stopUVDisinfection() {
        if (!isDisinfecting)
            return;
        isDisinfecting = false;

        // 取消倒计时
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }

        // 关闭所有紫外灯
        setAllUVLamps(false);

        remainingMs = 0;
        notifyStateChanged();
        Log.d(TAG, "【紫外灯】消毒已停止，紫外灯已关闭");
    }

    private void setAllUVLamps(boolean isOpen) {
        try {
            SensorUVLamp.getInstance().setUVLampSwitch(ProtoDev.SENSOR_UV_LAMP_1, isOpen);
            SensorUVLamp.getInstance().setUVLampSwitch(ProtoDev.SENSOR_UV_LAMP_2, isOpen);
            SensorUVLamp.getInstance().setUVLampSwitch(ProtoDev.SENSOR_UV_LAMP_3, isOpen);
            Log.d(TAG, "【紫外灯】所有紫外灯已" + (isOpen ? "打开" : "关闭"));
        } catch (Exception e) {
            Log.e(TAG, "【紫外灯】控制紫外灯异常", e);
        }
    }

    private void notifyStateChanged() {
        if (stateChangeListener != null) {
            stateChangeListener.onDisinfectionStateChanged(isDisinfecting, remainingMs);
        }
    }

    // ==================== 充电前置准备 ====================

    /**
     * 确保投影灯关闭（充电前调用）
     */
    private void ensureProjectionLightOff() {
        try {
            ProjectionDoorService.getInstance().ensureLightOff();
            Log.d(TAG, "【充电前准备】投影灯已关闭");
        } catch (Exception e) {
            Log.e(TAG, "【充电前准备】关闭投影灯异常", e);
        }
    }

    /**
     * 确保所有舱门关闭（充电前调用）
     */
    private void ensureDoorsClosed() {
        try {
            IDoorService doorService = ServiceManager.getInstance().getDoorService();
            if (doorService == null) {
                Log.w(TAG, "【充电前准备】舱门服务不可用");
                return;
            }
            doorService.closeAllDoors(new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "【充电前准备】所有舱门已关闭");
                }

                @Override
                public void onError(ApiError error) {
                    Log.e(TAG, "【充电前准备】关闭舱门失败: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "【充电前准备】关闭舱门异常", e);
        }
    }
}
