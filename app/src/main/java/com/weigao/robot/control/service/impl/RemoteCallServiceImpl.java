package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.util.Log;

import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.service.IRemoteCallService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 远程呼叫服务实现类
 * <p>
 * 实现远程呼叫功能，支持呼叫循环、呼叫到达、呼叫回收等操作。
 * </p>
 */
public class RemoteCallServiceImpl implements IRemoteCallService {

    private static final String TAG = "RemoteCallServiceImpl";

    private final Context context;

    /** 回调列表（线程安全） */
    private final List<IRemoteCallCallback> callbacks = new CopyOnWriteArrayList<>();

    /** 远程呼叫功能是否启用 */
    private boolean remoteCallEnabled = false;

    /** 是否正在执行远程呼叫任务 */
    private boolean remoteCallActive = false;

    /** 到达后停留时长是否启用 */
    private boolean arrivalStayDurationEnabled = true;

    /** 到达后停留时长（秒） */
    private int arrivalStayDuration = 60;

    public RemoteCallServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        Log.d(TAG, "RemoteCallServiceImpl 已创建");
    }

    // ==================== 远程呼叫启用控制 ====================

    @Override
    public void setRemoteCallEnabled(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "setRemoteCallEnabled: " + enabled);
        this.remoteCallEnabled = enabled;
        notifySuccess(callback);
    }

    @Override
    public void isRemoteCallEnabled(IResultCallback<Boolean> callback) {
        if (callback != null) {
            callback.onSuccess(remoteCallEnabled);
        }
    }

    // ==================== 远程呼叫操作 ====================

    @Override
    public void handleRemoteCall(int targetPointId, int callType, IResultCallback<Void> callback) {
        Log.d(TAG, "handleRemoteCall: targetPointId=" + targetPointId + ", callType=" + callType);
        if (!remoteCallEnabled) {
            notifyError(callback, -1, "远程呼叫功能未启用");
            return;
        }

        remoteCallActive = true;

        // TODO: 根据呼叫类型执行相应操作
        // CALL_TYPE_LOOP: 导航到目标点后进行循环配送
        // CALL_TYPE_ARRIVE: 仅导航到目标点
        // CALL_TYPE_RECOVERY: 导航到目标点进行物品回收

        notifySuccess(callback);
    }

    @Override
    public void cancelRemoteCall(IResultCallback<Void> callback) {
        Log.d(TAG, "cancelRemoteCall");
        remoteCallActive = false;
        notifyCallbacksCancelled();
        notifySuccess(callback);
    }

    @Override
    public void isRemoteCallActive(IResultCallback<Boolean> callback) {
        if (callback != null) {
            callback.onSuccess(remoteCallActive);
        }
    }

    // ==================== 到达后停留设置 ====================

    @Override
    public void setArrivalStayDurationEnabled(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "setArrivalStayDurationEnabled: " + enabled);
        this.arrivalStayDurationEnabled = enabled;
        notifySuccess(callback);
    }

    @Override
    public void setArrivalStayDuration(int durationSeconds, IResultCallback<Void> callback) {
        Log.d(TAG, "setArrivalStayDuration: " + durationSeconds);
        this.arrivalStayDuration = durationSeconds;
        notifySuccess(callback);
    }

    @Override
    public void getArrivalStayDuration(IResultCallback<Integer> callback) {
        if (callback != null) {
            callback.onSuccess(arrivalStayDuration);
        }
    }

    @Override
    public void isArrivalStayDurationEnabled(IResultCallback<Boolean> callback) {
        if (callback != null) {
            callback.onSuccess(arrivalStayDurationEnabled);
        }
    }

    // ==================== 回调注册 ====================

    @Override
    public void registerCallback(IRemoteCallCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
            Log.d(TAG, "回调已注册，当前数量：" + callbacks.size());
        }
    }

    @Override
    public void unregisterCallback(IRemoteCallCallback callback) {
        if (callbacks.remove(callback)) {
            Log.d(TAG, "回调已注销，当前数量：" + callbacks.size());
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "释放 RemoteCallService 资源");
        callbacks.clear();
        remoteCallActive = false;
    }

    // ==================== 回调分发 ====================

    private void notifyCallbacksReceived(String sourceType, int targetPointId, int callType) {
        for (IRemoteCallCallback callback : callbacks) {
            try {
                callback.onRemoteCallReceived(sourceType, targetPointId, callType);
            } catch (Exception e) {
                Log.e(TAG, "回调 onRemoteCallReceived 异常", e);
            }
        }
    }

    private void notifyCallbacksResult(boolean success, String message) {
        for (IRemoteCallCallback callback : callbacks) {
            try {
                callback.onRemoteCallResult(success, message);
            } catch (Exception e) {
                Log.e(TAG, "回调 onRemoteCallResult 异常", e);
            }
        }
    }

    private void notifyCallbacksCancelled() {
        for (IRemoteCallCallback callback : callbacks) {
            try {
                callback.onRemoteCallCancelled();
            } catch (Exception e) {
                Log.e(TAG, "回调 onRemoteCallCancelled 异常", e);
            }
        }
    }

    // ==================== 辅助方法 ====================

    private void notifySuccess(IResultCallback<Void> callback) {
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    private void notifyError(IResultCallback<?> callback, int code, String message) {
        if (callback != null) {
            callback.onError(new com.weigao.robot.control.callback.ApiError(code, message));
        }
    }
}
