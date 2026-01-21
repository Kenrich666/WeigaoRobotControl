package com.weigao.robot.control.core.communication;

import android.content.Context;
import android.util.Log;

// 引入 SCM 发送器相关类 (参考 DeviceComponent.md)
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.external.IDataCallback;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;

/**
 * 通信层实现类
 * <p>
 * 封装 Peanut SDK 的通信功能。
 * 目前主要用于监控 SDK 连接状态，并提供通用 SCM 指令发送通道。
 * 该类似乎有点多余。
 * </p>
 */
public class CommunicationImpl implements ICommunication {

    private static final String TAG = "CommunicationImpl";

    private final Context context;

    /** 连接超时时间（毫秒） */
    private int connectionTimeout = 5000;

    /** * 本地连接状态标记
     * 注意：PeanutSDK 内部维护心跳，此处状态最好通过 SDK 获取或监听
     */
    private volatile boolean connected = false;

    public CommunicationImpl(Context context) {
        this.context = context.getApplicationContext();
        Log.d(TAG, "CommunicationImpl 已创建");
    }

    @Override
    public void initialize(IResultCallback<Boolean> callback) {
        Log.d(TAG, "initialize");
        // PeanutSDK 通常在 Application 中已初始化
        // 这里可以检查 SDK 是否就绪
        try {
            // 简单检查：如果 SDK 单例不为空且 context 已注入，认为初始化阶段通过
            // 更严谨的做法是调用 SDK 的 Link 状态查询接口（如果存在）
            connected = (PeanutSDK.getInstance() != null);

            if (callback != null) {
                callback.onSuccess(connected);
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

        // 警告：如果该方法旨在发送通用 SCM 指令，需要解析 data 为 SCMRequest 对象
        // 由于 data 是 String，这里假设它是 JSON 或者是某种约定格式
        // 下面是一个使用 SCMIoTSender 的示例框架：

        /*
        try {
            // 示例：构造一个 SCMRequest (具体参数需根据 endpoint 和 data 解析)
            SCMRequest request = new SCMRequest();
            // request.setDev(...);
            // request.setTopic(...);
            // request.setParams(data);

            SCMIoTSender.sendRequest(request, new IDataCallback() {
                @Override
                public void success(String result) {
                    if (callback != null) callback.onSuccess(result);
                }

                @Override
                public void error(com.keenon.sdk.hedera.model.ApiError error) {
                     if (callback != null) callback.onError(new ApiError(error.getCode(), error.getMsg()));
                }
            });
        } catch (Exception e) {
             // ...
        }
        */

        // 现状保持：如果您的业务层目前不需要发底层指令，保持 Mock 并在日志中警告即可
        if (!connected) {
            if (callback != null) callback.onError(new ApiError(-1, "SDK 未连接"));
            return;
        }

        Log.w(TAG, "sendRequest: 当前为模拟实现，未实际发送数据至底层");
        String response = "{\"status\":\"ok\",\"endpoint\":\"" + endpoint + "\",\"warning\":\"mock_response\"}";
        if (callback != null) {
            callback.onSuccess(response);
        }
    }

    @Override
    public void disconnect(IResultCallback<Void> callback) {
        Log.d(TAG, "disconnect");
        // 仅仅标记本地状态，不调用 PeanutSDK.release()，以免影响其他服务
        connected = false;
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void setConnectionTimeout(int timeoutMs) {
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
        // 同样，不要销毁全局 SDK
    }
}