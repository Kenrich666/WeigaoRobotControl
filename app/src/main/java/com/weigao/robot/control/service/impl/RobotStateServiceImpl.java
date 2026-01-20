package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.util.Log;

import com.keenon.peanut.api.PeanutRuntime;
import com.keenon.peanut.api.callback.Runtime;
import com.keenon.peanut.api.entity.RuntimeInfo;

import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.IStateCallback;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.model.RobotState;
import com.weigao.robot.control.service.IRobotStateService;

import com.keenon.peanut.api.PeanutSDK;
import com.keenon.peanut.api.callback.IRobotCallBack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 机器人状态服务实现类
 * <p>
 * 封装 Peanut SDK 的 {@code PeanutRuntime} 组件，提供机器人状态监控功能。
 * </p>
 */
public class RobotStateServiceImpl implements IRobotStateService {

    private static final String TAG = "RobotStateServiceImpl";

    private final Context context;

    /** 回调列表（线程安全） */
    private final List<IStateCallback> callbacks = new CopyOnWriteArrayList<>();

    /** Peanut SDK 运行时组件 */
    private PeanutRuntime peanutRuntime;

    /** 当前机器人状态 */
    private RobotState currentState;

    /** 当前工作模式 */
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
            peanutRuntime = new PeanutRuntime.Builder()
                    .setListener(mRuntimeListener)
                    .build();
            peanutRuntime.execute();
            Log.d(TAG, "PeanutRuntime 初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "PeanutRuntime 初始化异常", e);
        }
    }

    /**
     * SDK 运行时回调
     */
    private final Runtime.Listener mRuntimeListener = new Runtime.Listener() {
        @Override
        public void onEvent(int event, Object obj) {
            Log.d(TAG, "onEvent: event=" + event);
            // 更新状态
            updateStateFromEvent(event, obj);
        }

        @Override
        public void onHealth(Object content) {
            Log.d(TAG, "onHealth: " + content);
        }

        @Override
        public void onHeartbeat(Object content) {
            // 心跳回调，可用于检测连接状态
        }
    };

    /**
     * 从事件更新状态
     */
    private void updateStateFromEvent(int event, Object obj) {
        if (peanutRuntime != null) {
            RuntimeInfo info = peanutRuntime.getRuntimeInfo();
            if (info != null) {
                // 更新电量
                int power = info.getPower();
                if (power != currentState.getBatteryLevel()) {
                    currentState.setBatteryLevel(power);
                    notifyBatteryLevelChanged(power);
                }

                // 更新工作模式
                workMode = info.getWorkMode();
                currentState.setWorkMode(workMode);

                // 更新急停按钮状态
                currentState.setScramButtonPressed(info.isEmergencyOpen());

                // 更新电机状态
                currentState.setMotorStatus(info.getMotorStatus());

                // 通知状态变化
                notifyStateChanged(currentState);
            }
        }
    }

    // ==================== 状态查询 ====================

    @Override
    public void getRobotState(IResultCallback<RobotState> callback) {
        if (callback != null) {
            updateCurrentState();
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
                boolean pressed = false;
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
                int status = 0;
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
            PeanutSDK.getInstance().motor().enable(new IRobotCallBack() {
                @Override
                public void success(String result) {
                    Log.d(TAG, "setMotorEnabled 成功: " + result);
                    notifySuccess(callback);
                }

                @Override
                public void error(com.keenon.peanut.api.entity.ApiError error) {
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
            // 调用 PeanutSDK 设备重启接口
            PeanutSDK.getInstance().device().reboot(new IRobotCallBack() {
                @Override
                public void success(String result) {
                    Log.d(TAG, "reboot 成功: " + result);
                    notifySuccess(callback);
                }

                @Override
                public void error(com.keenon.peanut.api.entity.ApiError error) {
                    Log.e(TAG, "reboot 失败: " + error.getMsg());
                    notifyError(callback, error.getCode(), error.getMsg());
                }
            });
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

    // ==================== 回调注册 ====================

    @Override
    public void registerCallback(IStateCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
            Log.d(TAG, "回调已注册，当前数量：" + callbacks.size());
        }
    }

    @Override
    public void unregisterCallback(IStateCallback callback) {
        if (callbacks.remove(callback)) {
            Log.d(TAG, "回调已注销，当前数量：" + callbacks.size());
        }
    }

    @Override
    public void release() {
        Log.d(TAG, "释放 RobotStateService 资源");
        callbacks.clear();
        if (peanutRuntime != null) {
            try {
                peanutRuntime.destory();
            } catch (Exception e) {
                Log.e(TAG, "释放 peanutRuntime 异常", e);
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

    private void updateCurrentState() {
        if (peanutRuntime != null) {
            RuntimeInfo info = peanutRuntime.getRuntimeInfo();
            if (info != null) {
                currentState.setBatteryLevel(info.getPower());
                currentState.setWorkMode(info.getWorkMode());
                currentState.setScramButtonPressed(info.isEmergencyOpen());
                currentState.setMotorStatus(info.getMotorStatus());
            }
        }
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
}
