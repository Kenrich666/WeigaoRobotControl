package com.weigao.robot.control.core.data;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import com.weigao.robot.control.model.DeviceInfo;
import com.weigao.robot.control.model.MotorInfo;
import com.weigao.robot.control.model.PointInfo;

/**
 * 数据处理器实现类
 * <p>
 * 提供 JSON 数据的序列化/反序列化功能，用于处理 SDK 与应用层之间的数据转换。
 * </p>
 */
public class DataProcessorImpl implements IDataProcessor {

    private static final String TAG = "DataProcessorImpl";

    private final Gson gson;

    public DataProcessorImpl() {
        this.gson = new GsonBuilder()
                .setLenient()
                .create();
        Log.d(TAG, "DataProcessorImpl 已创建");
    }

    @Override
    public <T> T parseResponse(String jsonResponse, Class<T> clazz) {
        Log.d(TAG, "parseResponse: clazz=" + clazz.getSimpleName());
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            Log.w(TAG, "parseResponse: jsonResponse 为空");
            return null;
        }

        try {
            return gson.fromJson(jsonResponse, clazz);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "parseResponse 解析异常", e);
            return null;
        }
    }

    @Override
    public String serializeRequest(Object request) {
        Log.d(TAG, "serializeRequest");
        if (request == null) {
            return "{}";
        }

        try {
            return gson.toJson(request);
        } catch (Exception e) {
            Log.e(TAG, "serializeRequest 序列化异常", e);
            return "{}";
        }
    }

    @Override
    public boolean validateData(String data) {
        Log.d(TAG, "validateData");
        if (data == null || data.isEmpty()) {
            return false;
        }

        // 基本 JSON 格式验证
        String trimmed = data.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return true;
        }
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return true;
        }

        return false;
    }

    @Override
    public DeviceInfo parseDeviceInfo(String jsonData) {
        Log.d(TAG, "parseDeviceInfo");
        return parseResponse(jsonData, DeviceInfo.class);
    }

    @Override
    public MotorInfo parseMotorInfo(String jsonData) {
        Log.d(TAG, "parseMotorInfo");
        return parseResponse(jsonData, MotorInfo.class);
    }

    @Override
    public PointInfo parsePointInfo(String jsonData) {
        Log.d(TAG, "parsePointInfo");
        return parseResponse(jsonData, PointInfo.class);
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "释放 DataProcessor 资源");
        // Gson 无需特殊释放
    }
}
