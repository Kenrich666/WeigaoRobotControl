package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds a small settle window for all-door operations so rapid manual toggles
 * are serialized instead of being sent to hardware too early and getting lost.
 */
public class StabilizedDoorServiceImpl extends DoorServiceImpl {

    private static final String TAG = "StabilizedDoorService";
    private static final long ALL_DOOR_SETTLE_MS = 1200L;
    private static final int ERROR_OPERATION_REPLACED = -3;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object allDoorLock = new Object();

    private boolean allDoorOperationInProgress = false;
    private boolean currentTargetOpen = false;
    private boolean currentSingleMode = false;
    private Boolean queuedTargetOpen = null;
    private boolean queuedSingleMode = false;
    private Boolean stableAllDoorsClosed = null;
    private Runnable completionRunnable;

    private final List<IResultCallback<Void>> currentCallbacks = new ArrayList<>();
    private final List<IResultCallback<Void>> queuedCallbacks = new ArrayList<>();

    public StabilizedDoorServiceImpl(Context context) {
        super(context);
    }

    @Override
    public void openAllDoors(boolean single, IResultCallback<Void> callback) {
        requestAllDoorOperation(true, single, callback);
    }

    @Override
    public void closeAllDoors(IResultCallback<Void> callback) {
        requestAllDoorOperation(false, false, callback);
    }

    @Override
    public void isAllDoorsClosed(IResultCallback<Boolean> callback) {
        if (callback == null) {
            return;
        }
        boolean returnCached;
        boolean cachedValue = false;
        synchronized (allDoorLock) {
            if (allDoorOperationInProgress) {
                callback.onSuccess(false);
                return;
            }
            returnCached = stableAllDoorsClosed != null;
            if (returnCached) {
                cachedValue = stableAllDoorsClosed;
            }
        }
        if (returnCached) {
            callback.onSuccess(cachedValue);
            return;
        }
        super.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                synchronized (allDoorLock) {
                    stableAllDoorsClosed = result;
                }
                callback.onSuccess(result);
            }

            @Override
            public void onError(ApiError error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void release() {
        synchronized (allDoorLock) {
            cancelCompletionLocked();
            allDoorOperationInProgress = false;
            queuedTargetOpen = null;
            currentCallbacks.clear();
            queuedCallbacks.clear();
        }
        super.release();
    }

    private void requestAllDoorOperation(boolean targetOpen, boolean single, IResultCallback<Void> callback) {
        List<IResultCallback<Void>> supersededCallbacks = null;
        boolean shouldDispatch = false;
        boolean queuedOnly = false;

        synchronized (allDoorLock) {
            if (!allDoorOperationInProgress
                    && stableAllDoorsClosed != null
                    && stableAllDoorsClosed == !targetOpen) {
                notifySuccess(callback);
                return;
            }

            if (allDoorOperationInProgress) {
                if (currentTargetOpen == targetOpen) {
                    addCallback(currentCallbacks, callback);
                    return;
                }

                if (queuedTargetOpen != null && queuedTargetOpen == targetOpen) {
                    addCallback(queuedCallbacks, callback);
                    return;
                }

                if (queuedTargetOpen != null) {
                    supersededCallbacks = new ArrayList<>(queuedCallbacks);
                    queuedCallbacks.clear();
                }

                queuedTargetOpen = targetOpen;
                queuedSingleMode = single;
                addCallback(queuedCallbacks, callback);
                queuedOnly = true;
            } else {
                allDoorOperationInProgress = true;
                currentTargetOpen = targetOpen;
                currentSingleMode = single;
                addCallback(currentCallbacks, callback);
                shouldDispatch = true;
            }
        }

        if (supersededCallbacks != null && !supersededCallbacks.isEmpty()) {
            notifyCallbacksError(supersededCallbacks, ERROR_OPERATION_REPLACED, "舱门操作已更新，请重试最新指令");
        }

        if (queuedOnly) {
            return;
        }

        if (shouldDispatch) {
            dispatchAllDoorOperation(targetOpen, single);
        }
    }

    private void dispatchAllDoorOperation(boolean targetOpen, boolean single) {
        IResultCallback<Void> settleCallback = new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                scheduleCompletion(targetOpen);
            }

            @Override
            public void onError(ApiError error) {
                failAllDoorOperation(error);
            }
        };

        if (targetOpen) {
            super.openAllDoors(single, settleCallback);
        } else {
            super.closeAllDoors(settleCallback);
        }
    }

    private void scheduleCompletion(boolean targetOpen) {
        Runnable runnable = () -> finishAllDoorOperation(targetOpen);
        synchronized (allDoorLock) {
            cancelCompletionLocked();
            completionRunnable = runnable;
        }
        mainHandler.postDelayed(runnable, ALL_DOOR_SETTLE_MS);
    }

    private void finishAllDoorOperation(boolean targetOpen) {
        List<IResultCallback<Void>> completedCallbacks;
        List<IResultCallback<Void>> redundantQueuedCallbacks = null;
        Boolean nextTarget = null;
        boolean nextSingleMode = false;

        synchronized (allDoorLock) {
            if (!allDoorOperationInProgress || currentTargetOpen != targetOpen) {
                return;
            }

            cancelCompletionLocked();
            stableAllDoorsClosed = !targetOpen;
            allDoorOperationInProgress = false;

            completedCallbacks = new ArrayList<>(currentCallbacks);
            currentCallbacks.clear();

            if (queuedTargetOpen != null) {
                if (queuedTargetOpen == targetOpen) {
                    redundantQueuedCallbacks = new ArrayList<>(queuedCallbacks);
                    queuedCallbacks.clear();
                    queuedTargetOpen = null;
                } else {
                    nextTarget = queuedTargetOpen;
                    nextSingleMode = queuedSingleMode;
                    queuedTargetOpen = null;
                    allDoorOperationInProgress = true;
                    currentTargetOpen = nextTarget;
                    currentSingleMode = nextSingleMode;
                    currentCallbacks.addAll(queuedCallbacks);
                    queuedCallbacks.clear();
                }
            }
        }

        notifyCallbacksSuccess(completedCallbacks);

        if (redundantQueuedCallbacks != null && !redundantQueuedCallbacks.isEmpty()) {
            notifyCallbacksSuccess(redundantQueuedCallbacks);
        }

        if (nextTarget != null) {
            dispatchAllDoorOperation(nextTarget, nextSingleMode);
        }
    }

    private void failAllDoorOperation(ApiError error) {
        List<IResultCallback<Void>> failedCallbacks;
        List<IResultCallback<Void>> failedQueuedCallbacks;

        synchronized (allDoorLock) {
            cancelCompletionLocked();
            allDoorOperationInProgress = false;
            queuedTargetOpen = null;
            failedCallbacks = new ArrayList<>(currentCallbacks);
            failedQueuedCallbacks = new ArrayList<>(queuedCallbacks);
            currentCallbacks.clear();
            queuedCallbacks.clear();
        }

        notifyCallbacksError(failedCallbacks, error);
        notifyCallbacksError(failedQueuedCallbacks, error);
    }

    private void cancelCompletionLocked() {
        if (completionRunnable != null) {
            mainHandler.removeCallbacks(completionRunnable);
            completionRunnable = null;
        }
    }

    private void addCallback(List<IResultCallback<Void>> callbacks, IResultCallback<Void> callback) {
        if (callback != null) {
            callbacks.add(callback);
        }
    }

    private void notifyCallbacksSuccess(List<IResultCallback<Void>> callbacks) {
        for (IResultCallback<Void> callback : callbacks) {
            notifySuccess(callback);
        }
    }

    private void notifyCallbacksError(List<IResultCallback<Void>> callbacks, ApiError error) {
        for (IResultCallback<Void> callback : callbacks) {
            if (callback != null) {
                callback.onError(error);
            }
        }
    }

    private void notifyCallbacksError(List<IResultCallback<Void>> callbacks, int code, String message) {
        ApiError error = new ApiError(code, message);
        notifyCallbacksError(callbacks, error);
    }

    private void notifySuccess(IResultCallback<Void> callback) {
        if (callback != null) {
            callback.onSuccess(null);
        }
    }
}
