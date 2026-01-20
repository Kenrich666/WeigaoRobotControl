package com.weigao.robot.control.core.communication;

import android.content.Context;
import android.util.Log;

import com.keenon.peanut.api.PeanutSDK;
import com.keenon.peanut.api.entity.PeanutConstants;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;

/**
 * 通信层实现类
 * <p>
 * 封装 Peanut SDK 的通信功能，提供与机器人底层的 CoAP/串口通信。
 * SDK 初始化时已通过 {@code PeanutConfig} 配置了通信参数（LinkType、LinkCOM、LinkIP 等）。
 * </p>
 */
public class CommunicationImpl implements ICommunication {

    private static final String TAG = "CommunicationImpl";

    private final Context context;

    /** 连接超时时间（毫秒） */
    private int connectionTimeout = 5000;

    /** 连接状态 */
    private boolean connected = false;

    public CommunicationImpl(Context context) {
        this.context = context.getApplicationContext();
        Log.d(TAG, "CommunicationImpl 已创建");
    }

    @Override
    public void initialize(IResultCallback<Boolean> callback) {
        Log.d(TAG, "initialize");
        try {
            // SDK 通信在 PeanutSDK.init() 时已建立
            // 这里检查 SDK 是否已初始化
            boolean sdkReady = PeanutSDK.getInstance().isReady();
            connected = sdkReady;

            if (callback != null) {
                callback.onSuccess(sdkReady);
            }
        } catch (Exception e) {
            Log.e(TAG, "initialize 异常", e);
            connected = false;
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public void sendRequest(String endpoint, String data, IResultCallback<String> callback) {
        Log.d(TAG, "sendRequest: endpoint=" + endpoint);
        try {
            if (!connected) {
                if (callback != null) {
                    callback.onError(new ApiError(-1, "未连接"));
                }
                return;
            }

            // Peanut SDK 的通信由各组件内部处理
            // 如需自定义通信，可通过 SDK 提供的扩展接口实现
            // 此处返回模拟响应
            String response = "{\"status\":\"ok\",\"endpoint\":\"" + endpoint + "\"}";
            if (callback != null) {
                callback.onSuccess(response);
            }
        } catch (Exception e) {
            Log.e(TAG, "sendRequest 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public void disconnect(IResultCallback<Void> callback) {
        Log.d(TAG, "disconnect");
        try {
            // SDK 释放由 PeanutSDK.release() 处理
            connected = false;
            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "disconnect 异常", e);
            if (callback != null) {
                callback.onError(new ApiError(-1, e.getMessage()));
            }
        }
    }

    @Override
    public boolean isConnected() {
        // 检查 SDK 连接状态
        try {
            connected = PeanutSDK.getInstance().isReady();
        } catch (Exception e) {
            connected = false;
        }
        return connected;
    }

    @Override
    public void setConnectionTimeout(int timeoutMs) {
        Log.d(TAG, "setConnectionTimeout: " + timeoutMs);
        this.connectionTimeout = timeoutMs;
    }

    @Override
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "释放 Communication 资源");
        connected = false;
    }
}
