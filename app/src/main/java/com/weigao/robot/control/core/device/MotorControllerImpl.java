package com.weigao.robot.control.core.device;

import android.content.Context;
import android.util.Log;

// SDK 组件导入
import com.keenon.sdk.component.MotorComponent;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.component.runtime.RuntimeInfo;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;

// 项目内部接口导入
import com.weigao.robot.control.callback.ApiError; // 项目自己的 Error 类
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.MotorInfo;

/**
 * 电机控制器实现类
 */
public class MotorControllerImpl implements IMotorController {

    private static final String TAG = "MotorControllerImpl";

    private final Context context;

    /**
     * 状态管理单例 (用于获取缓存的状态)
     */
    private PeanutRuntime peanutRuntime;

    /**
     * 电机控制组件 (用于发送指令)
     */
    private MotorComponent motorComponent;

    /**
     * 本地缓存的电机状态模型
     */
    private final MotorInfo currentMotorInfo;

    public MotorControllerImpl(Context context) {
        this.context = context.getApplicationContext();
        this.currentMotorInfo = new MotorInfo();
        Log.d(TAG, "MotorControllerImpl 已创建");
        initSdkComponents();
    }

    /**
     * 初始化 SDK 组件
     */
    private void initSdkComponents() {
        try {
            // 1. 获取 PeanutRuntime 单例 (用于读状态)
            peanutRuntime = PeanutRuntime.getInstance();
            if (peanutRuntime != null) {
                // 确保 Runtime 监听已启动 (App启动时通常已调过，这里防守性调用)
                peanutRuntime.start();
            }

            // 2. 获取 MotorComponent 组件 (用于写指令)
            Object motorObj = PeanutSDK.getInstance().motor();
            if (motorObj instanceof MotorComponent) {
                this.motorComponent = (MotorComponent) motorObj;
                Log.d(TAG, "SDK MotorComponent 初始化成功");
            } else {
                Log.e(TAG, "获取 MotorComponent 失败或类型不匹配");
            }

        } catch (Exception e) {
            Log.e(TAG, "SDK组件初始化异常", e);
        }
    }

    @Override
    public void getStatus(IResultCallback<MotorInfo> callback) {
        // 优先从 PeanutRuntime 缓存读取
        if (peanutRuntime == null) {
            if (callback != null) callback.onError(new ApiError(-1, "SDK未初始化"));
            return;
        }

        try {
            RuntimeInfo info = peanutRuntime.getRuntimeInfo();
            if (info != null) {
                // 将 SDK 的 RuntimeInfo 转换为业务层的 MotorInfo
                currentMotorInfo.setCode(info.getMotorStatus());

                // 假设 status > 0 代表启用，具体根据业务定义
                currentMotorInfo.setEnabled(info.getMotorStatus() > 0);

                // RuntimeInfo 不含实时速度，沿用最后一次设置值
                // 如果需要实时速度，需调用 motorComponent.getSpeed() (异步)

                if (callback != null) {
                    callback.onSuccess(currentMotorInfo);
                }
            } else {
                if (callback != null) callback.onError(new ApiError(-2, "SDK数据未就绪"));
            }
        } catch (Exception e) {
            Log.e(TAG, "getStatus 异常", e);
            if (callback != null) callback.onError(new ApiError(-1, e.getMessage()));
        }
    }

    @Override
    public void getHealth(IResultCallback<MotorInfo> callback) {
        // 复用 getStatus，因为 RuntimeInfo 已包含基础健康状态
        getStatus(callback);
    }

    @Override
    public void enableMotor(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "enableMotor: " + enabled);
        if (motorComponent == null) {
            if (callback != null) callback.onError(new ApiError(-1, "MotorComponent未初始化"));
            return;
        }

        // 1 = Enable, 0 = Disable
        int param = enabled ? 1 : 0;

        motorComponent.enable(new IDataCallback() {
            @Override
            public void success(String response) {
                Log.d(TAG, "enableMotor success: " + response);
                currentMotorInfo.setEnabled(enabled);
                if (callback != null) callback.onSuccess(null);
            }

            @Override
            // 使用全限定名避免与 com.weigao...ApiError 冲突
            public void error(com.keenon.sdk.hedera.model.ApiError error) {
                Log.e(TAG, "enableMotor error: " + error.toString());
                if (callback != null) {
                    // 将 SDK Error 转换为项目 Error
                    callback.onError(new ApiError(error.getCode(), error.getMsg()));
                }
            }
        }, param);
    }

    @Override
    public void setSpeed(int speed, IResultCallback<Void> callback) {
        Log.d(TAG, "setSpeed: " + speed);
        if (motorComponent == null) {
            if (callback != null) callback.onError(new ApiError(-1, "MotorComponent未初始化"));
            return;
        }

        // 调用 MotorComponent.setMaxSpeed
        motorComponent.setMaxSpeed(new IDataCallback() {
            @Override
            public void success(String response) {
                Log.d(TAG, "setSpeed success: " + response);
                currentMotorInfo.setSpeed(speed);
                if (callback != null) callback.onSuccess(null);
            }

            @Override
            public void error(com.keenon.sdk.hedera.model.ApiError error) {
                Log.e(TAG, "setSpeed error: " + error.toString());
                if (callback != null) {
                    callback.onError(new ApiError(error.getCode(), error.getMsg()));
                }
            }
        }, speed);
    }

    @Override
    public void stopMotor(IResultCallback<Void> callback) {
        Log.d(TAG, "stopMotor");
        if (motorComponent == null) {
            if (callback != null) callback.onError(new ApiError(-1, "MotorComponent未初始化"));
            return;
        }

        // 使用 manual(0) 停止运动
        motorComponent.manual(new IDataCallback() {
            @Override
            public void success(String response) {
                Log.d(TAG, "stopMotor success: " + response);
                currentMotorInfo.setSpeed(0);
                if (callback != null) callback.onSuccess(null);
            }

            @Override
            public void error(com.keenon.sdk.hedera.model.ApiError error) {
                Log.e(TAG, "stopMotor error: " + error.toString());
                if (callback != null) {
                    callback.onError(new ApiError(error.getCode(), error.getMsg()));
                }
            }
        }, 0);
    }

    public void release() {
        Log.d(TAG, "释放 MotorController 资源");
        // 仅释放引用，不销毁单例
        peanutRuntime = null;
        motorComponent = null;
    }
}