package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.INavigationCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.INavigationService;
import com.weigao.robot.control.service.IRemoteCallService;

import java.util.ArrayList;
import java.util.Collections;
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

    private final INavigationService navigationService;
    private final Handler mainHandler;
    /**
     * 回调列表（线程安全）
     */
    private final List<IRemoteCallCallback> callbacks = new CopyOnWriteArrayList<>();

    /**
     * 远程呼叫功能是否启用
     */
    private boolean remoteCallEnabled = false;

    /**
     * 是否正在执行远程呼叫任务
     */
    private boolean remoteCallActive = false;

    /**
     * 当前呼叫任务的类型
     */
    private int currentCallType = 0;

    /**
     * 到达后停留时长是否启用
     */
    private boolean arrivalStayDurationEnabled = true;

    /**
     * 到达后停留时长（秒）
     */
    private int arrivalStayDuration = 60;

    /**
     * 默认返回点ID (通常应从配置获取，这里暂定为 0 或 1)
     */
    private static final int DEFAULT_RETURN_POINT_ID = 1;
    /**
     * 倒计时任务
     */
    private final Runnable returnTask = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "停留时间结束，准备返回");
            performReturn();
        }
    };

    /**
     * 构造函数
     *
     * @param context           上下文
     * @param navigationService 导航服务实例 (必须注入)
     */
    public RemoteCallServiceImpl(Context context, INavigationService navigationService) {
        this.context = context.getApplicationContext();
        this.navigationService = navigationService;
        this.mainHandler = new Handler(Looper.getMainLooper());

        // 注册导航回调以监听到达事件
        if (this.navigationService != null) {
            this.navigationService.registerCallback(navigationCallback);
        }

        Log.d(TAG, "RemoteCallServiceImpl 已创建");
    }

    // ==================== 导航回调监听 ====================
    private final INavigationCallback navigationCallback = new INavigationCallback() {
        @Override
        public void onStateChanged(int state, int scheduleStatus) {
            // Navigation.STATE_DESTINATION = 3 (到达目的地)
            // 需根据实际 SDK Navigation 常量值确认，通常 3 代表到达
            if (state == 3 && remoteCallActive) {
                Log.i(TAG, "远程呼叫：已到达目标点，开始处理停留逻辑");
                handleArrival();
            }
        }

        @Override
        public void onRoutePrepared(List<NavigationNode> nodes) {
            Log.d(TAG, "导航路径规划完成，节点数：" + (nodes != null ? nodes.size() : 0));
        }

        @Override
        public void onRouteNode(int index, NavigationNode node) {
            // 可以在此更新进度，暂不处理
        }

        @Override
        public void onDistanceChanged(double distance) {
            // 距离变化回调，暂不处理
        }

        @Override
        public void onError(int code, String message) {
            if (remoteCallActive) {
                Log.e(TAG, "远程呼叫导航出错: " + message);
                notifyCallbacksResult(false, "导航异常: " + message);
                // 注意：导航出错通常意味着任务中断，这里根据业务需求决定是否重置 active 状态
//                 remoteCallActive = false;
            }
        }

        @Override
        public void onNavigationError(int code) {
            Log.e(TAG, "onNavigationError: " + code);
            if (remoteCallActive) {
                notifyCallbacksResult(false, "导航故障(码:" + code + ")");
            }
        }
    };

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
        Log.d(TAG, "handleRemoteCall: target=" + targetPointId + ", type=" + callType);

        if (!remoteCallEnabled) {
            notifyError(callback, -1, "远程呼叫功能未启用");
            return;
        }

        if (navigationService == null) {
            notifyError(callback, -1, "导航服务不可用");
            return;
        }

        if (remoteCallActive) {
            notifyError(callback, -1, "当前已有远程呼叫任务正在进行");
            return;
        }

        // 1. 更新状态
        remoteCallActive = true;
        currentCallType = callType;

        // 2. 通知回调：收到呼叫 (模拟来源为 API，若是物理按钮触发应在外部调用此方法前处理)
        notifyCallbacksReceived("API", targetPointId, callType);

        // 3. 执行导航到目标点
        List<Integer> targets = new ArrayList<>();
        targets.add(targetPointId);

        navigationService.setTargets(targets, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "目标点设置成功，开始导航");
                navigationService.start(callback); // 将启动结果透传给调用者
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "设置目标点失败");
                remoteCallActive = false;
                if (callback != null) callback.onError(error);
            }
        });
    }

    @Override
    public void cancelRemoteCall(IResultCallback<Void> callback) {
        Log.d(TAG, "cancelRemoteCall");

        // 停止倒计时
        mainHandler.removeCallbacks(returnTask);

        // 如果正在导航，停止导航
        if (remoteCallActive && navigationService != null) {
            navigationService.stop(null);
        }

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
    // ==================== 内部逻辑：到达与返回 ====================
    /**
     * 处理到达目标点后的逻辑
     */
    private void handleArrival() {
        if (!arrivalStayDurationEnabled) {
            Log.i(TAG, "停留时长未启用，保持在目标点");
            // 任务算完成还是挂起？根据接口"倒计时结束后前往返回点"，
            // 如果不启用倒计时，可能意味着一直等待直到人工干预或取消。
            // 这里我们发送一个结果通知，但不自动返回。
            notifyCallbacksResult(true, "已到达目标点，无自动返回");
            return;
        }

        Log.i(TAG, "启动返回倒计时: " + arrivalStayDuration + "秒");
        // 发送通知
        notifyCallbacksResult(true, "已到达，" + arrivalStayDuration + "秒后返回");

        // 启动定时器
        mainHandler.removeCallbacks(returnTask);
        mainHandler.postDelayed(returnTask, arrivalStayDuration * 1000L);
    }

    /**
     * 执行返回动作
     */
    private void performReturn() {
        if (navigationService == null) return;

        Log.d(TAG, "开始返回至: " + DEFAULT_RETURN_POINT_ID);
        List<Integer> returnTarget = Collections.singletonList(DEFAULT_RETURN_POINT_ID);

        navigationService.setTargets(returnTarget, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                navigationService.start(new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.i(TAG, "返回导航已启动");
                        // 注意：此时任务仍视为 Active，直到返回到达。
                        // 但为简化逻辑，通常在启动返回后，本次呼叫交互逻辑即算闭环，
                        // 或者可以重置 active 状态让其变为普通导航。
                        // 这里我们选择在启动返回后结束呼叫任务状态。
                        remoteCallActive = false;
                    }

                    @Override
                    public void onError(ApiError error) {
                        Log.e(TAG, "启动返回导航失败: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "设置返回点失败: " + error.getMessage());
            }
        });
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
        mainHandler.removeCallbacksAndMessages(null);
        if (navigationService != null) {
            navigationService.unregisterCallback(navigationCallback);
        }
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
            callback.onError(new ApiError(code, message));
        }
    }
}
