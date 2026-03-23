package com.weigao.robot.control.manager;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.keenon.sdk.scmIot.protopack.base.ProtoDev;
import com.keenon.sdk.sensor.uvlamp.SensorUVLamp;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.IStateCallback;
import com.weigao.robot.control.model.ChargingState;
import com.weigao.robot.control.model.RobotState;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.service.impl.DoorServiceImpl;
import com.weigao.robot.control.service.impl.ProjectionDoorService;

/**
 * 紫外灯消毒全局管理器。
 */
public class UVDisinfectionManager {

    private static final String TAG = "UVDisinfectionManager";
    private static final long UV_DISINFECTION_DURATION_MS = 20 * 60 * 1000L;
    private static final long CHARGING_STOP_DEBOUNCE_MS = 5000L;

    private static final UVDisinfectionManager INSTANCE = new UVDisinfectionManager();

    public static UVDisinfectionManager getInstance() {
        return INSTANCE;
    }

    private UVDisinfectionManager() {
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean initialized = false;
    private boolean wasCharging = false;
    private boolean isDisinfecting = false;
    private CountDownTimer countdownTimer;
    private long remainingMs = 0;
    private Runnable pendingStopRunnable;

    public interface OnStateChangeListener {
        void onDisinfectionStateChanged(boolean isDisinfecting, long remainingMs);
    }

    private OnStateChangeListener stateChangeListener;

    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        IRobotStateService robotStateService = ServiceManager.getInstance().getRobotStateService();
        if (robotStateService == null) {
            Log.e(TAG, "RobotStateService unavailable, UV disinfection manager init failed");
            return;
        }

        robotStateService.registerCallback(stateCallback);
        Log.d(TAG, "UV disinfection manager initialized, listening for charging state");

        robotStateService.getChargingState(new IResultCallback<ChargingState>() {
            @Override
            public void onSuccess(ChargingState result) {
                if (result != null) {
                    handler.post(() -> handleChargingState(result.isCharging()));
                }
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "Failed to query initial charging state: " + error.getMessage());
            }
        });
    }

    public void setStateChangeListener(OnStateChangeListener listener) {
        this.stateChangeListener = listener;
        if (listener != null) {
            listener.onDisinfectionStateChanged(isDisinfecting, remainingMs);
        }
    }

    public void removeStateChangeListener() {
        this.stateChangeListener = null;
    }

    public void stopDisinfection() {
        stopUVDisinfection();
    }

    public boolean isDisinfecting() {
        return isDisinfecting;
    }

    public long getRemainingMs() {
        return remainingMs;
    }

    private final IStateCallback stateCallback = new IStateCallback() {
        @Override
        public void onStateChanged(RobotState newState) {
        }

        @Override
        public void onLocationChanged(double x, double y) {
        }

        @Override
        public void onBatteryLevelChanged(int level) {
        }

        @Override
        public void onChargingStateChanged(ChargingState chargingState) {
            Log.d(TAG, "【紫外灯】收到充电状态回调, event="
                    + (chargingState != null ? chargingState.getEvent() : "null")
                    + ", isCharging=" + (chargingState != null ? chargingState.isCharging() : "null"));
            if (chargingState != null) {
                handler.post(() -> handleChargingState(chargingState.isCharging()));
            }
        }

        @Override
        public void onScramButtonPressed(boolean pressed) {
        }
    };

    private void handleChargingState(boolean isCharging) {
        Log.d(TAG, "Charging state updated, isCharging=" + isCharging + ", wasCharging=" + wasCharging);

        if (isCharging && !wasCharging) {
            if (pendingStopRunnable != null) {
                handler.removeCallbacks(pendingStopRunnable);
                pendingStopRunnable = null;
                Log.d(TAG, "【紫外灯】取消待执行的关灯操作（充电状态抖动）");
            }

            setChargingLock(true);
            ensureProjectionLightOff();
            ensureDoorsClosed();

            if (!isDisinfecting) {
                Log.d(TAG, "【紫外灯】检测到开始充电，启动紫外灯消毒");
                startUVDisinfection();
            }
        } else if (!isCharging && wasCharging) {
            Log.d(TAG, "【紫外灯】检测到停止充电信号，等待" + (CHARGING_STOP_DEBOUNCE_MS / 1000) + "秒确认");
            if (pendingStopRunnable == null) {
                pendingStopRunnable = () -> {
                    Log.d(TAG, "【紫外灯】确认停止充电，关闭紫外灯");
                    stopUVDisinfection();
                    setChargingLock(false);
                    pendingStopRunnable = null;
                };
                handler.postDelayed(pendingStopRunnable, CHARGING_STOP_DEBOUNCE_MS);
            }
        }

        wasCharging = isCharging;
    }

    private void setChargingLock(boolean locked) {
        Log.d(TAG, "【充电锁定】" + (locked ? "锁定舱门和投影灯" : "解锁舱门和投影灯"));

        ProjectionDoorService.getInstance().setChargingLocked(locked);

        IDoorService doorService = ServiceManager.getInstance().getDoorService();
        if (doorService instanceof DoorServiceImpl) {
            ((DoorServiceImpl) doorService).setChargingLocked(locked);
        }
    }

    private void startUVDisinfection() {
        if (isDisinfecting) {
            return;
        }
        isDisinfecting = true;

        setAllUVLamps(true);

        countdownTimer = new CountDownTimer(UV_DISINFECTION_DURATION_MS, 1000L) {
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
        if (!isDisinfecting) {
            return;
        }
        isDisinfecting = false;

        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }

        setAllUVLamps(false);
        remainingMs = 0;
        notifyStateChanged();
        Log.d(TAG, "【紫外灯】消毒已停止，紫外灯已关闭");
    }

    private void setAllUVLamps(boolean isOpen) {
        try {
            SensorUVLamp.getInstance().setUSBDirect(true);
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

    private void ensureProjectionLightOff() {
        try {
            ProjectionDoorService.getInstance().ensureLightOff();
            Log.d(TAG, "【充电前准备】投影灯已关闭");
        } catch (Exception e) {
            Log.e(TAG, "【充电前准备】关闭投影灯异常", e);
        }
    }

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
