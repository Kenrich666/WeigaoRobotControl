package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.util.Log;

import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.component.runtime.RuntimeInfo;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.IStateCallback;
import com.weigao.robot.control.model.RobotState;
import com.weigao.robot.control.service.IRobotStateService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 机器人状态服务实现类
 * <p>
 * 封装 Peanut SDK 的 {@code PeanutRuntime} 组件，提供机器人状态监控功能。
 * 1. 作为 SDK 数据的唯一入口。
 * 2. 内部维护 currentState 缓存。
 * 3. 负责向 UI 分发状态变化。
 * </p>
 */
public class RobotStateServiceImpl implements IRobotStateService {

    private static final String TAG = "RobotStateServiceImpl";

    private final Context context;

    /**
     * 回调列表（线程安全）
     */
    private final List<IStateCallback> callbacks = new CopyOnWriteArrayList<>();

    /**
     * Peanut SDK 运行时组件
     */
    private PeanutRuntime peanutRuntime;

    /**
     * 当前机器人状态缓存
     */
    private final RobotState currentState;

    /**
     * 当前工作模式
     */
    private int workMode = WORK_MODE_STANDBY;

    public RobotStateServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        this.currentState = new RobotState();
        Log.d(TAG, "RobotStateServiceImpl 已创建");
        initPeanutRuntime();
    }

    /**
     * 初始化 PeanutRuntime
     */
    private void initPeanutRuntime() {
        try {
            // 获取单例实例
            peanutRuntime = getPeanutRuntimeInstance();

            if (peanutRuntime != null) {
                // 注册监听器 (支持多个监听者)
                peanutRuntime.registerListener(mRuntimeListener);
                // 启动运行时 (如果尚未启动)
                peanutRuntime.start();
                Log.d(TAG, "PeanutRuntime 初始化及监听注册成功");
                // 初始化时立即同步一次状态
                updateStateFromSdk();
            } else {
                Log.e(TAG, "PeanutRuntime 获取失败 (null)");
            }
        } catch (Exception e) {
            Log.e(TAG, "PeanutRuntime 初始化异常", e);
        }
    }

    /**
     * SDK 运行时回调
     */
    private final PeanutRuntime.Listener mRuntimeListener = new PeanutRuntime.Listener() {
        @Override
        public void onEvent(int event, Object obj) {
            // Log.d(TAG, "onEvent: event=" + event); // 日志量可能较大，建议按需开启
            // 收到事件时，主动去拉取最新状态更新缓存
            updateStateFromSdk();
        }

        @Override
        public void onHealth(Object content) {
            Log.d(TAG, "onHealth: " + content);
            // 可以在此解析健康状态并更新 currentState
        }

        @Override
        public void onHeartbeat(Object content) {
            // 心跳回调，RuntimeInfo 通常在心跳中自动更新
        }
    };

    /**
     * 从 RuntimeInfo 更新状态
     */
    private void updateStateFromSdk() {
        if (peanutRuntime != null) {
            RuntimeInfo info = peanutRuntime.getRuntimeInfo();
            if (info != null) {
                boolean stateChanged = false;
                synchronized (currentState) {
                    // 1. 更新电量
                    int power = info.getPower();
                    if (power != currentState.getBatteryLevel()) {
                        currentState.setBatteryLevel(power);
                        notifyBatteryLevelChanged(power);
                        stateChanged = true;
                    }

                    // 2. 更新工作模式
                    int newWorkMode = info.getWorkMode();
                    if (workMode != newWorkMode) {
                        workMode = newWorkMode;
                        currentState.setWorkMode(workMode);
                        stateChanged = true;
                    }

                    // 3. 更新急停按钮状态
                    boolean scram = info.isEmergencyOpen();
                    if (scram != currentState.isScramButtonPressed()) {
                        currentState.setScramButtonPressed(scram);
                        stateChanged = true;
                    }
                    // 4. 更新电机状态
                    int motorStatus = info.getMotorStatus();
                    if (motorStatus != currentState.getMotorStatus()) {
                        currentState.setMotorStatus(motorStatus);
                        stateChanged = true;
                    }

                    // 5. 更新其他属性（如位置等）
                    // currentState.setLocation(...);
                }
                // 仅当状态发生实质变化时通知，避免频繁回调
                if (stateChanged) {
                    notifyStateChanged(currentState);
                }
            }
        }
    }
    // ==================== 回调注册 (合并了 StateManager 的功能) ====================

    @Override
    public void registerCallback(IStateCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
            Log.d(TAG, "监听器已注册");

            // [关键功能] 注册时立即回调当前状态，避免 UI 空白
            // 类似于 StateManager 的行为
            synchronized (currentState) {
                callback.onStateChanged(currentState);
                callback.onBatteryLevelChanged(currentState.getBatteryLevel());
            }
        }
    }

    @Override
    public void unregisterCallback(IStateCallback callback) {
        callbacks.remove(callback);
    }

    // ==================== 状态查询 ====================

    @Override
    public void getRobotState(IResultCallback<RobotState> callback) {
        if (callback != null) {
            // 触发一次更新以确保最新
            updateStateFromSdk();
            callback.onSuccess(currentState);
        }
    }

    @Override
    public void getBatteryLevel(IResultCallback<Integer> callback) {
        if (callback != null) {
            try {
                int power = currentState.getBatteryLevel();
                if (peanutRuntime != null) {
                    RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                    if (info != null) {
                        power = info.getPower();
                        currentState.setBatteryLevel(power);
                    }
                }
                callback.onSuccess(power);
            } catch (Exception e) {
                Log.e(TAG, "getBatteryLevel 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    @Override
    public void getCurrentLocation(IResultCallback<RobotState.LocationInfo> callback) {
        if (callback != null) {
            callback.onSuccess(currentState.getLocation());
        }
    }

    @Override
    public void isScramButtonPressed(IResultCallback<Boolean> callback) {
        if (callback != null) {
            try {
                boolean pressed = currentState.isScramButtonPressed();
                if (peanutRuntime != null) {
                    RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                    if (info != null) {
                        pressed = info.isEmergencyOpen();
                    }
                }
                callback.onSuccess(pressed);
            } catch (Exception e) {
                Log.e(TAG, "isScramButtonPressed 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    @Override
    public void getMotorStatus(IResultCallback<Integer> callback) {
        if (callback != null) {
            try {
                int status = currentState.getMotorStatus();
                if (peanutRuntime != null) {
                    RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                    if (info != null) {
                        status = info.getMotorStatus();
                    }
                }
                callback.onSuccess(status);
            } catch (Exception e) {
                Log.e(TAG, "getMotorStatus 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    // ==================== 设备控制 ====================

    @Override
    public void setWorkMode(int mode, IResultCallback<Void> callback) {
        Log.d(TAG, "setWorkMode: " + mode);
        try {
            if (peanutRuntime != null) {
                peanutRuntime.setWorkMode(mode);
            }
            this.workMode = mode;
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "setWorkMode 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void setEmergencyEnabled(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "setEmergencyEnabled: " + enabled);
        try {
            if (peanutRuntime != null) {
                peanutRuntime.setEmergencyEnable(enabled);
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "setEmergencyEnabled 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void setMotorEnabled(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "setMotorEnabled: " + enabled);
        try {
            // 调用 PeanutSDK 电机控制接口
            PeanutSDK.getInstance().motor().enable(new IDataCallback() {
                @Override
                public void success(String result) {
                    Log.d(TAG, "setMotorEnabled 成功: " + result);
                    notifySuccess(callback);
                }

                @Override
                public void error(com.keenon.sdk.hedera.model.ApiError error) {
                    Log.e(TAG, "setMotorEnabled 失败: " + error.getMsg());
                    notifyError(callback, error.getCode(), error.getMsg());
                }
            }, enabled ? 1 : 0);
        } catch (Exception e) {
            Log.e(TAG, "setMotorEnabled 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void syncParams(boolean needReboot, IResultCallback<Void> callback) {
        Log.d(TAG, "syncParams: needReboot=" + needReboot);
        try {
            if (peanutRuntime != null) {
                peanutRuntime.syncParams2Robot(needReboot);
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "syncParams 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void reboot(IResultCallback<Void> callback) {
        Log.d(TAG, "reboot");
        try {
            // 优先使用 PeanutRuntime 封装的带同步逻辑的重启，比较安全
            // 或者使用 DeviceComponent 的 reboot
            if (peanutRuntime != null) {
                peanutRuntime.syncParams2Robot(true);
                notifySuccess(callback);
            } else {
                // 调用 PeanutSDK 设备重启接口
                PeanutSDK.getInstance().device().reboot(new IDataCallback() {
                    @Override
                    public void success(String result) {
                        Log.d(TAG, "reboot 成功: " + result);
                        notifySuccess(callback);
                    }

                    @Override
                    public void error(com.keenon.sdk.hedera.model.ApiError error) {
                        Log.e(TAG, "reboot 失败: " + error.getMsg());
                        notifyError(callback, error.getCode(), error.getMsg());
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "reboot 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void performLocalization(IResultCallback<Void> callback) {
        Log.d(TAG, "performLocalization");
        try {
            if (peanutRuntime != null) {
                peanutRuntime.location();
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "performLocalization 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    // ==================== 设备信息查询 ====================

    @Override
    public void getTotalOdometer(IResultCallback<Double> callback) {
        if (callback != null) {
            try {
                double odo = 0.0;
                if (peanutRuntime != null) {
                    RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                    if (info != null) {
                        odo = info.getTotalOdo();
                    }
                }
                callback.onSuccess(odo);
            } catch (Exception e) {
                Log.e(TAG, "getTotalOdometer 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    @Override
    public void getRobotIp(IResultCallback<String> callback) {
        if (callback != null) {
            try {
                String ip = "";
                if (peanutRuntime != null) {
                    RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                    if (info != null) {
                        ip = info.getRobotIp();
                    }
                }
                callback.onSuccess(ip != null ? ip : "");
            } catch (Exception e) {
                Log.e(TAG, "getRobotIp 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    @Override
    public void getRobotArmInfo(IResultCallback<String> callback) {
        if (callback != null) {
            try {
                String info = "";
                if (peanutRuntime != null) {
                    RuntimeInfo rtInfo = peanutRuntime.getRuntimeInfo();
                    if (rtInfo != null) {
                        info = rtInfo.getRobotArmInfo();
                    }
                }
                callback.onSuccess(info != null ? info : "");
            } catch (Exception e) {
                Log.e(TAG, "getRobotArmInfo 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    @Override
    public void getRobotStm32Info(IResultCallback<String> callback) {
        if (callback != null) {
            try {
                String info = "";
                if (peanutRuntime != null) {
                    RuntimeInfo rtInfo = peanutRuntime.getRuntimeInfo();
                    if (rtInfo != null) {
                        info = rtInfo.getRobotStm32Info();
                    }
                }
                callback.onSuccess(info != null ? info : "");
            } catch (Exception e) {
                Log.e(TAG, "getRobotStm32Info 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    @Override
    public void getDestinationList(IResultCallback<String> callback) {
        if (callback != null) {
            try {
                String list = "[]";
                if (peanutRuntime != null) {
                    RuntimeInfo rtInfo = peanutRuntime.getRuntimeInfo();
                    if (rtInfo != null) {
                        list = rtInfo.getDestList();
                    }
                }
                callback.onSuccess(list != null ? list : "[]");
            } catch (Exception e) {
                Log.e(TAG, "getDestinationList 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    @Override
    public void getRobotProperties(IResultCallback<String> callback) {
        if (callback != null) {
            try {
                String props = "{}";
                if (peanutRuntime != null) {
                    RuntimeInfo rtInfo = peanutRuntime.getRuntimeInfo();
                    if (rtInfo != null) {
                        props = rtInfo.getRobotProperties();
                    }
                }
                callback.onSuccess(props != null ? props : "{}");
            } catch (Exception e) {
                Log.e(TAG, "getRobotProperties 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    @Override
    public void release() {
        Log.d(TAG, "释放 RobotStateService 资源");
        callbacks.clear();
        if (peanutRuntime != null) {
            try {
                // [修正 4] 切勿调用 destroy()，仅注销监听
                peanutRuntime.removeListener(mRuntimeListener);
                Log.d(TAG, "PeanutRuntime 监听已移除");
            } catch (Exception e) {
                Log.e(TAG, "移除监听异常", e);
            }
            peanutRuntime = null;
        }
    }

    // ==================== 回调分发 ====================

    private void notifyStateChanged(RobotState state) {
        for (IStateCallback callback : callbacks) {
            try {
                callback.onStateChanged(state);
            } catch (Exception e) {
                Log.e(TAG, "回调 onStateChanged 异常", e);
            }
        }
    }

    private void notifyBatteryLevelChanged(int level) {
        for (IStateCallback callback : callbacks) {
            try {
                callback.onBatteryLevelChanged(level);
            } catch (Exception e) {
                Log.e(TAG, "回调 onBatteryLevelChanged 异常", e);
            }
        }
    }

    // ==================== 辅助方法 ====================
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
     * 获取 PeanutRuntime 单例的工厂方法。
     * <p>
     * 设计为 protected 以便在测试子类中重写并注入 Mock 对象。
     * </p>
     */
    protected PeanutRuntime getPeanutRuntimeInstance() {
        return PeanutRuntime.getInstance();
    }
}
