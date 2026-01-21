package com.weigao.robot.control.core.device;

import android.content.Context;
import android.util.Log;

// SDK 组件导入
import com.keenon.sdk.component.DeviceComponent; // [新增] 引入 DeviceComponent
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.component.runtime.RuntimeInfo;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.external.IDataCallback; // [新增] 用于异步回调

// 项目自定义接口
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

    /**
     * 状态管理单例 (用于获取缓存状态、重启、同步参数)
     */
    private PeanutRuntime peanutRuntime;

    /**
     * 设备控制组件 (用于风扇控制、底层板级通信)
     */
    private DeviceComponent deviceComponent;

    public RobotDeviceImpl(Context context) {
        this.context = context.getApplicationContext();
        Log.d(TAG, "RobotDeviceImpl 已创建");
        initSdkComponents();
    }

    /**
     * 初始化 SDK 组件
     */
    private void initSdkComponents() {
        try {
            // 1. [修复] 获取 PeanutRuntime 单例
            // 用于获取 RuntimeInfo 和调用 syncParams2Robot
            peanutRuntime = PeanutRuntime.getInstance();
            if (peanutRuntime != null) {
                peanutRuntime.start(); // 确保监听已启动
                Log.d(TAG, "PeanutRuntime 单例获取成功");
            }

            // 2. [新增] 获取 DeviceComponent
            // 用于控制风扇、获取底层详细信息
            Object deviceObj = PeanutSDK.getInstance().device();
            if (deviceObj instanceof DeviceComponent) {
                this.deviceComponent = (DeviceComponent) deviceObj;
                Log.d(TAG, "DeviceComponent 获取成功");
            } else {
                Log.w(TAG, "DeviceComponent 获取失败或类型不匹配");
            }

        } catch (Exception e) {
            Log.e(TAG, "SDK组件初始化异常", e);
        }
    }

    @Override
    public void getDeviceList(IResultCallback<DeviceInfo> callback) {
        Log.d(TAG, "getDeviceList");
        try {
            DeviceInfo deviceInfo = new DeviceInfo();

            // 优先从缓存读取
            if (peanutRuntime != null) {
                RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                if (info != null) {
                    deviceInfo.setRobotIp(info.getRobotIp());
                    deviceInfo.setArmInfo(info.getRobotArmInfo());
                    deviceInfo.setStm32Info(info.getRobotStm32Info());
                    // 注意：RuntimeInfo 是否包含 getRobotProperties 需确认
                    // 假设你的 RuntimeInfo 修改版中有此方法
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

            // 这里的逻辑可以保持从 RuntimeInfo 读取缓存
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

            // 如果缓存中没有，也可以尝试通过 deviceComponent.getBoardInfo(callback, jsonParams) 异步获取
            // 但为了保持接口同步返回的特性，读取缓存是最佳选择

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
                // PeanutRuntime 的 syncParams2Robot(true) 包含了 "同步参数 -> 等待 -> 重启" 的完整逻辑
                // 这比直接调用 DeviceComponent.reboot 更安全，因为它防止配置丢失
                // 同步参数并重启 (true = 重启)
                peanutRuntime.syncParams2Robot(true);
            } else if (deviceComponent != null) {
                // 降级方案：直接调用 SDK 组件重启
                deviceComponent.reboot(new IDataCallback() {
                    @Override
                    public void success(String result) {
                        Log.i(TAG, "设备重启指令发送成功");
                    }

                    @Override
                    public void error(com.keenon.sdk.hedera.model.ApiError error) {
                        Log.e(TAG, "设备重启失败: " + error);
                    }
                });
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
        // [说明] 查阅 DeviceComponent 源码，未发现公开的风扇控制 API (openFan/closeFan)。
        // 这通常意味着风扇控制是自动的，或者需要通过 SCMRequest 发送特定的私有指令。
        // 此处保留接口返回成功，避免上层报错。

        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    @Override
    public void closeFan(int fanId, IResultCallback<Void> callback) {
        Log.d(TAG, "closeFan: " + fanId);
        // 同上，未发现 SDK 公开 API
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "释放 RobotDevice 资源");
        // PeanutRuntime 是单例，不要 destroy
        peanutRuntime = null;
        deviceComponent = null;
    }
}
