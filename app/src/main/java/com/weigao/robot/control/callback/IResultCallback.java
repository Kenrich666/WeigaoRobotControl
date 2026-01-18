package com.weigao.robot.control.callback;

public interface IResultCallback<T> {
    void onSuccess(T result);

    void onError(ApiError error);
}
