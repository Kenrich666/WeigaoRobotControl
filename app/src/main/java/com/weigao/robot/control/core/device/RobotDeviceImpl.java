package com.weigao.robot.control.core.device;

import android.content.Context;
import android.util.Log;

import com.keenon.peanut.api.PeanutRuntime;
import com.keenon.peanut.api.entity.RuntimeInfo;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.DeviceInfo;

/**
 * 机器人设备实现类
 * <p>
 * 封装 Peanut SDK 的设备信息获取和控制功能。
 * 通过 {@code PeanutRuntime} 组件获取设备状态和配置信息。
 * </p>
 */
public class RobotDeviceImpl implements IRobotDevice {

    private static final String TAG = "RobotDeviceImpl";

    private final Context context;

    /** Peanut SDK 运行时组件 */
    private PeanutRuntime peanutRuntime;

    public RobotDeviceImpl(Context context) {
        this.context = context.getApplicationContext();
        Log.d(TAG, "RobotDeviceImpl 已创建");
        initPeanutRuntime();
    }

    /**
     * 初始化 PeanutRuntime
     */
    private void initPeanutRuntime() {
        try {
            peanutRuntime = new PeanutRuntime.Builder().build();
            Log.d(TAG, "PeanutRuntime 初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "PeanutRuntime 初始化异常", e);
        }
    }

    @Override
    public void getDeviceList(IResultCallback<DeviceInfo> callback) {
        Log.d(TAG, "getDeviceList");
        try {
            DeviceInfo deviceInfo = new DeviceInfo();

            if (peanutRuntime != null) {
                RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                if (info != null) {
                    deviceInfo.setRobotIp(info.getRobotIp());
                    deviceInfo.setArmInfo(info.getRobotArmInfo());
                    deviceInfo.setStm32Info(info.getRobotStm32Info());
                    deviceInfo.setProperties(info.getRobotProperties());
                }
            }

            if (callback != null) {
                callback.onSuccess(deviceInfo);
            }
        } catch (Exception e) {
            Log.e(TAG, "getDeviceList 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public void getBoardInfo(String board, IResultCallback<DeviceInfo> callback) {
        Log.d(TAG, "getBoardInfo: " + board);
        try {
            DeviceInfo deviceInfo = new DeviceInfo();

            if (peanutRuntime != null) {
                RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                if (info != null) {
                    if ("arm".equalsIgnoreCase(board)) {
                        deviceInfo.setArmInfo(info.getRobotArmInfo());
                    } else if ("stm32".equalsIgnoreCase(board)) {
                        deviceInfo.setStm32Info(info.getRobotStm32Info());
                    }
                }
            }

            if (callback != null) {
                callback.onSuccess(deviceInfo);
            }
        } catch (Exception e) {
            Log.e(TAG, "getBoardInfo 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public void getConfig(IResultCallback<String> callback) {
        Log.d(TAG, "getConfig");
        try {
            String config = "{}";

            if (peanutRuntime != null) {
                RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                if (info != null) {
                    config = info.getRobotProperties();
                }
            }

            if (callback != null) {
                callback.onSuccess(config != null ? config : "{}");
            }
        } catch (Exception e) {
            Log.e(TAG, "getConfig 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public void updateConfig(String params, IResultCallback<String> callback) {
        Log.d(TAG, "updateConfig: " + params);
        try {
            if (peanutRuntime != null) {
                // 同步参数到机器人
                peanutRuntime.syncParams2Robot(false);
            }

            if (callback != null) {
                callback.onSuccess("{\"status\":\"ok\"}");
            }
        } catch (Exception e) {
            Log.e(TAG, "updateConfig 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public void reboot(IResultCallback<Void> callback) {
        Log.d(TAG, "reboot");
        try {
            if (peanutRuntime != null) {
                // 同步参数并重启
                peanutRuntime.syncParams2Robot(true);
            }

            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "reboot 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public void getScramButtonStatus(IResultCallback<Boolean> callback) {
        Log.d(TAG, "getScramButtonStatus");
        try {
            boolean pressed = false;

            if (peanutRuntime != null) {
                RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                if (info != null) {
                    pressed = info.isEmergencyOpen();
                }
            }

            if (callback != null) {
                callback.onSuccess(pressed);
            }
        } catch (Exception e) {
            Log.e(TAG, "getScramButtonStatus 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public void openFan(int fanId, IResultCallback<Void> callback) {
        Log.d(TAG, "openFan: " + fanId);
        try {
            // TODO: 调用 SDK 风扇控制接口（如果存在）
            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "openFan 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public void closeFan(int fanId, IResultCallback<Void> callback) {
        Log.d(TAG, "closeFan: " + fanId);
        try {
            // TODO: 调用 SDK 风扇控制接口（如果存在）
            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "closeFan 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "释放 RobotDevice 资源");
        if (peanutRuntime != null) {
            try {
                peanutRuntime.destory();
            } catch (Exception e) {
                Log.e(TAG, "释放 peanutRuntime 异常", e);
            }
            peanutRuntime = null;
        }
    }
}
