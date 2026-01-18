package com.weigao.robot.control.utils;

import android.os.Handler;
import android.os.Looper;

import com.weigao.robot.control.callback.IResultCallback;

public class CallbackHelper {
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static <T> void onSuccessOnUiThread(final IResultCallback<T> callback, final T result) {
        if (callback == null) return;
        MAIN_HANDLER.post(() -> callback.onSuccess(result));
    }

    public static <T> void onErrorOnUiThread(final IResultCallback<T> callback, final com.weigao.robot.control.callback.ApiError error) {
        if (callback == null) return;
        MAIN_HANDLER.post(() -> callback.onError(error));
    }

    public static void runOnUiThread(final Runnable runnable) {
        if (runnable == null) return;
        MAIN_HANDLER.post(runnable);
    }
}
