package com.weigao.robot.control.service.impl;

import android.util.Log;

import com.keenon.sdk.external.IDataCallback2;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.sensor.uvlamp.SensorUVLamp;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.service.IUVLampService;

/**
 * 紫外灯服务实现类
 * <p>
 * 封装 Peanut SDK 的 {@code SensorUVLamp} 组件，提供紫外灯控制功能。
 * </p>
 */
public class UVLampServiceImpl implements IUVLampService {

    private static final String TAG = "UVLampServiceImpl";

    public UVLampServiceImpl() {
        Log.d(TAG, "UVLampServiceImpl 已创建");
    }

    @Override
    public void setUVLampSwitch(int dev, boolean isOpen) {
        Log.d(TAG, "setUVLampSwitch: dev=" + dev + ", isOpen=" + isOpen);
        try {
            SensorUVLamp.getInstance().setUVLampSwitch(dev, isOpen);
            Log.i(TAG, "紫外灯 " + dev + " 已" + (isOpen ? "开启" : "关闭"));
        } catch (Exception e) {
            Log.e(TAG, "setUVLampSwitch 异常", e);
        }
    }

    @Override
    public void setAllUVLampsSwitch(boolean isOpen) {
        Log.d(TAG, "setAllUVLampsSwitch: isOpen=" + isOpen);
        try {
            // 依次控制所有 3 个紫外灯
            SensorUVLamp.getInstance().setUVLampSwitch(UV_LAMP_1, isOpen);
            SensorUVLamp.getInstance().setUVLampSwitch(UV_LAMP_2, isOpen);
            SensorUVLamp.getInstance().setUVLampSwitch(UV_LAMP_3, isOpen);
            Log.i(TAG, "所有紫外灯已" + (isOpen ? "开启" : "关闭"));
        } catch (Exception e) {
            Log.e(TAG, "setAllUVLampsSwitch 异常", e);
        }
    }

    @Override
    public void getUVLampStatus(int dev, IResultCallback<Boolean> callback) {
        Log.d(TAG, "getUVLampStatus: dev=" + dev);
        try {
            SensorUVLamp.getInstance().getUVLampSwitch(dev, new IDataCallback2<Boolean>() {
                @Override
                public void success(Boolean isOpen) {
                    Log.d(TAG, "紫外灯 " + dev + " 状态: " + (isOpen ? "开启" : "关闭"));
                    if (callback != null) {
                        callback.onSuccess(isOpen);
                    }
                }

                @Override
                public void error(ApiError error) {
                    Log.e(TAG, "获取紫外灯状态失败: " + error.toString());
                    if (callback != null) {
                        callback.onError(new com.weigao.robot.control.callback.ApiError(
                                error.getCode(), error.getMsg()));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "getUVLampStatus 异常", e);
            if (callback != null) {
                callback.onError(new com.weigao.robot.control.callback.ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public void release() {
        Log.d(TAG, "UVLampServiceImpl 资源已释放");
        // SDK SensorUVLamp 为单例，无需手动释放
    }
}
