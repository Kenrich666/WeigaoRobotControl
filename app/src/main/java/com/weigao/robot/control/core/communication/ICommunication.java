package com.weigao.robot.control.core.communication;

import com.weigao.robot.control.callback.IResultCallback;

public interface ICommunication {
    void initialize(IResultCallback<Boolean> callback);

    void sendRequest(String endpoint, String data, IResultCallback<String> callback);

    void disconnect(IResultCallback<Void> callback);

    boolean isConnected();

    void setConnectionTimeout(int timeoutMs);

    int getConnectionTimeout();
}
