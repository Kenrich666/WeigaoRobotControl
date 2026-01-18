package com.weigao.robot.control.callback;

public interface IProgressCallback<T> extends IResultCallback<T> {
    void onProgress(int percent);

    void onReadyToSend();
}
