package com.weigao.robot.control.core.device;

import android.content.Context;
import android.util.Log;

import com.keenon.peanut.api.PeanutRuntime;
import com.keenon.peanut.api.entity.RuntimeInfo;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.MotorInfo;

/**
 * 电机控制器实现类
 * <p>
 * 封装 Peanut SDK 的电机控制功能，提供电机状态查询和控制接口。
 * </p>
 */
public class MotorControllerImpl implements IMotorController {

    private static final String TAG = "MotorControllerImpl";

    private final Context context;

    /** Peanut SDK 运行时组件 */
    private PeanutRuntime peanutRuntime;

    /** 当前电机状态 */
    private MotorInfo currentMotorInfo;

    /** 电机是否启用 */
    private boolean motorEnabled = false;

    /** 当前速度 */
    private int currentSpeed = 0;

    public MotorControllerImpl(Context context) {
        this.context = context.getApplicationContext();
        this.currentMotorInfo = new MotorInfo();
        Log.d(TAG, "MotorControllerImpl 已创建");
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
    public void getStatus(IResultCallback<MotorInfo> callback) {
        Log.d(TAG, "getStatus");
        try {
            updateMotorInfo();
            if (callback != null) {
                callback.onSuccess(currentMotorInfo);
            }
        } catch (Exception e) {
            Log.e(TAG, "getStatus 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public void getHealth(IResultCallback<MotorInfo> callback) {
        Log.d(TAG, "getHealth");
        try {
            updateMotorInfo();
            // 健康状态和普通状态类似，可扩展更多健康检查
            if (callback != null) {
                callback.onSuccess(currentMotorInfo);
            }
        } catch (Exception e) {
            Log.e(TAG, "getHealth 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public void enableMotor(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "enableMotor: " + enabled);
        try {
            this.motorEnabled = enabled;
            currentMotorInfo.setEnabled(enabled);

            // TODO: 调用 SDK 电机使能接口（如果存在）
            // 目前 SDK 文档中未明确电机使能的独立接口

            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "enableMotor 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public void setSpeed(int speed, IResultCallback<Void> callback) {
        Log.d(TAG, "setSpeed: " + speed);
        try {
            this.currentSpeed = speed;
            currentMotorInfo.setSpeed(speed);

            // 速度设置通常由 PeanutNavigation 组件处理
            // 此处记录速度值供状态查询使用

            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "setSpeed 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public void stopMotor(IResultCallback<Void> callback) {
        Log.d(TAG, "stopMotor");
        try {
            this.currentSpeed = 0;
            currentMotorInfo.setSpeed(0);

            // 停止电机通常由导航组件的 stop() 方法处理

            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "stopMotor 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    /**
     * 更新电机信息
     */
    private void updateMotorInfo() {
        if (peanutRuntime != null) {
            RuntimeInfo info = peanutRuntime.getRuntimeInfo();
            if (info != null) {
                currentMotorInfo.setCode(info.getMotorStatus());
                // 根据电机状态判断是否启用
                motorEnabled = info.getMotorStatus() > 0;
                currentMotorInfo.setEnabled(motorEnabled);
            }
        }
        currentMotorInfo.setSpeed(currentSpeed);
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "释放 MotorController 资源");
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
