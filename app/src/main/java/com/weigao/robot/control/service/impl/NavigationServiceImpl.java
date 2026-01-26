package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.util.Log;

import com.keenon.sdk.component.navigation.PeanutNavigation;
import com.keenon.sdk.component.navigation.common.Navigation;
import com.keenon.sdk.component.navigation.route.RouteNode;

import com.weigao.robot.control.callback.INavigationCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.INavigationService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 导航服务实现类
 * <p>
 * 封装 Peanut SDK 的 {@code PeanutNavigation} 组件，提供导航控制功能。
 * </p>
 */
public class NavigationServiceImpl implements INavigationService {

    private static final String TAG = "NavigationServiceImpl";

    private final Context context;

    /** 回调列表（线程安全） */
    private final List<INavigationCallback> callbacks = new CopyOnWriteArrayList<>();

    /** Peanut SDK 导航组件 */
    private PeanutNavigation peanutNavigation;

    /** 导航目标点列表 */
    // [优化] 使用线程安全的 List
    private List<NavigationNode> targetNodes = new CopyOnWriteArrayList<>();
    /** 当前目标点索引 */
    private int currentPosition = 0;

    /** 导航速度（cm/s） */
    private int speed = 40;

    /** 路线策略 */
    private int routePolicy = POLICY_FIXED;

    /** 阻挡超时（ms） */
    private int blockingTimeout = 30000;

    /** 循环次数 */
    private int repeatCount = 0;

    /** 是否自动循环 */
    private boolean autoRepeat = false;

    /** 近似到达控制是否启用 */
    private boolean arrivalControlEnabled = true;

    public NavigationServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        Log.d(TAG, "NavigationServiceImpl 已创建");
    }

    /**
     * SDK 导航回调
     */
    private final Navigation.Listener mNavigationListener = new Navigation.Listener() {
        @Override
        public void onStateChanged(int state, int schedule) {
            String stateStr = getStateString(state);
            Log.d(TAG, "【导航回调】状态变化: " + stateStr + " (state=" + state + "), schedule=" + schedule);
            notifyStateChanged(state, schedule);
        }

        @Override
        public void onRouteNode(int index, RouteNode routeNode) {
            Log.d(TAG, "【导航回调】到达路线节点，索引: " + index + ", 点位ID: " + (routeNode != null ? routeNode.getId() : "null"));
            currentPosition = index;
            NavigationNode node = convertToNavigationNode(routeNode);
            notifyRouteNode(index, node);
        }

        @Override
        public void onRoutePrepared(RouteNode... routeNodes) {
            Log.d(TAG, "【导航回调】路线准备完成，点位数量: " + (routeNodes != null ? routeNodes.length : 0));
            // 更新目标点列表
            targetNodes.clear();
            List<NavigationNode> nodeList = new ArrayList<>();
            if (routeNodes != null) {
                for (RouteNode rn : routeNodes) {
                    NavigationNode node = convertToNavigationNode(rn);
                    targetNodes.add(node);
                    nodeList.add(node);
                    Log.d(TAG, "【导航回调】路线点位: " + node.toString());
                }
            }
            // 通知回调监听器
            notifyRoutePrepared(nodeList);
        }

        @Override
        public void onDistanceChanged(float distance) {
            notifyDistanceChanged((double) distance);
        }

        @Override
        public void onError(int code) {
            Log.e(TAG, "onError: " + code);
            notifyError(code, "导航错误");
        }

        @Override
        public void onEvent(int event) {
            Log.d(TAG, "onEvent: " + event);
            // 处理导航事件
        }
    };

    // ==================== 导航控制 ====================

    @Override
    public void setTargets(List<Integer> targetIds, IResultCallback<Void> callback) {
        Log.d(TAG, "【设置目标】目标点ID列表: " + targetIds);
        try {
            Integer[] targets = null;
            if (targetIds != null && !targetIds.isEmpty()) {
                targets = targetIds.toArray(new Integer[0]);
            }

            // 更新 NavigationNode 列表（仅用于本地记录和回调）
            targetNodes.clear();
            if (targetIds != null) {
                for (Integer id : targetIds) {
                    NavigationNode node = new NavigationNode();
                    node.setId(id);
                    // 尝试从缓存或 SDK 获取名称/坐标？目前仅记录 ID
                    targetNodes.add(node);
                }
            }

            if (peanutNavigation != null) {
                // 复用现有实例
                Log.d(TAG, "【设置目标】复用现有 PeanutNavigation 实例");
                peanutNavigation.stop();
                if (targets != null) {
                    peanutNavigation.setTargets(targets);
                }
            } else {
                // 创建新实例
                PeanutNavigation.Builder builder = new PeanutNavigation.Builder()
                        .setListener(mNavigationListener)
                        .setBlockingTimeOut(blockingTimeout)
                        .setRoutePolicy(routePolicy)
                        .enableDefaultArrival(true)
                        // .setArrivalControl(true, 1.0f, 5000, 10000)
                        .enableAutoRepeat(autoRepeat);

                if (repeatCount > 0) {
                    builder.setRepeatCount(repeatCount);
                }

                if (targets != null) {
                    builder.setTargets(targets);
                }

                peanutNavigation = createPeanutNavigation(builder);
                Log.d(TAG, "【设置目标】创建新 PeanutNavigation 实例");
            }
            currentPosition = 0;
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "setTargets 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void setTargetNodes(List<NavigationNode> targets, IResultCallback<Void> callback) {
        Log.d(TAG, "【设置目标】setTargetNodes: count=" + (targets != null ? targets.size() : 0));
        try {
            List<Integer> targetIds = new ArrayList<>();
            // 更新本地 targetNodes 缓存，保留详细信息
            targetNodes.clear();

            if (targets != null && !targets.isEmpty()) {
                for (NavigationNode node : targets) {
                    targetNodes.add(node);
                    targetIds.add(node.getId());
                    Log.d(TAG, "【目标点】添加目标 ID: " + node.getId() + ", name=" + node.getName());
                }
            }

            Integer[] targetIdArray = targetIds.toArray(new Integer[0]);

            if (peanutNavigation != null) {
                Log.d(TAG, "【设置目标】复用实例 (setTargetNodes)");
                peanutNavigation.stop();
                peanutNavigation.setTargets(targetIdArray);
            } else {
                Log.d(TAG, "【设置目标】新建实例 (setTargetNodes)");
                PeanutNavigation.Builder builder = new PeanutNavigation.Builder()
                        .setListener(mNavigationListener)
                        .setBlockingTimeOut(blockingTimeout)
                        .setRoutePolicy(routePolicy)
                        .enableDefaultArrival(true)
                        .enableAutoRepeat(autoRepeat);

                if (repeatCount > 0) {
                    builder.setRepeatCount(repeatCount);
                }

                builder.setTargets(targetIdArray);
                peanutNavigation = createPeanutNavigation(builder);
            }

            currentPosition = 0;
            notifySuccess(callback);

        } catch (Exception e) {
            Log.e(TAG, "setTargetNodes 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void prepare(IResultCallback<Void> callback) {
        Log.d(TAG, "【准备导航】开始规划路线...");
        try {
            if (peanutNavigation != null) {
                peanutNavigation.prepare();
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "prepare 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void start(IResultCallback<Void> callback) {
        Log.d(TAG, "【开始导航】启动导航...");
        try {
            if (peanutNavigation != null) {
                peanutNavigation.setPilotWhenReady(true);
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "start 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void pause(IResultCallback<Void> callback) {
        Log.d(TAG, "【暂停导航】暂停中...");
        try {
            if (peanutNavigation != null) {
                peanutNavigation.setPilotWhenReady(false);
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "pause 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void stop(IResultCallback<Void> callback) {
        Log.d(TAG, "【停止导航】停止导航...");
        try {
            if (peanutNavigation != null) {
                peanutNavigation.stop();
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "stop 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void pilotNext(IResultCallback<Void> callback) {
        Log.d(TAG, "【前往下一点】开始前往下一个目标点...");
        try {
            if (peanutNavigation != null) {
                peanutNavigation.pilotNext();
            }
            if (currentPosition < targetNodes.size() - 1) {
                currentPosition++;
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "pilotNext 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void skipTo(int index, IResultCallback<Void> callback) {
        Log.d(TAG, "skipTo: " + index);
        try {
            if (peanutNavigation != null) {
                peanutNavigation.skipTo(index);
            }
            if (index >= 0 && index < targetNodes.size()) {
                currentPosition = index;
                notifySuccess(callback);
            } else {
                notifyError(callback, -1, "索引越界");
            }
        } catch (Exception e) {
            Log.e(TAG, "skipTo 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    // ==================== 参数设置 ====================

    @Override
    public void setSpeed(int speed, IResultCallback<Void> callback) {
        Log.d(TAG, "setSpeed: " + speed);
        this.speed = speed;
        try {
            if (peanutNavigation != null) {
                peanutNavigation.setSpeed(speed);
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "setSpeed 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void setRoutePolicy(int policy, IResultCallback<Void> callback) {
        Log.d(TAG, "setRoutePolicy: " + policy);
        this.routePolicy = policy;
        notifySuccess(callback);
    }

    @Override
    public void setBlockingTimeout(int timeout, IResultCallback<Void> callback) {
        Log.d(TAG, "setBlockingTimeout: " + timeout);
        this.blockingTimeout = timeout;
        notifySuccess(callback);
    }

    @Override
    public void setRepeatCount(int count, IResultCallback<Void> callback) {
        Log.d(TAG, "setRepeatCount: " + count);
        this.repeatCount = count;
        notifySuccess(callback);
    }

    @Override
    public void setAutoRepeat(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "setAutoRepeat: " + enabled);
        this.autoRepeat = enabled;
        notifySuccess(callback);
    }

    @Override
    public void setArrivalControlEnabled(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "setArrivalControlEnabled: " + enabled);
        this.arrivalControlEnabled = enabled;
        try {
            if (peanutNavigation != null) {
                peanutNavigation.setArrivalControlEnable(enabled);
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "setArrivalControlEnabled 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void cancelArrivalControl(IResultCallback<Void> callback) {
        Log.d(TAG, "cancelArrivalControl");
        try {
            if (peanutNavigation != null) {
                peanutNavigation.cancelArrivalControl();
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "cancelArrivalControl 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    // ==================== 手动控制 ====================

    @Override
    public void manual(int direction, IResultCallback<Void> callback) {
        Log.d(TAG, "manual: direction=" + direction);
        try {
            if (peanutNavigation != null) {
                peanutNavigation.manual(direction);
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "manual 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    // ==================== 状态查询 ====================

    @Override
    public void getRouteNodes(IResultCallback<List<NavigationNode>> callback) {
        if (callback != null) {
            callback.onSuccess(new ArrayList<>(targetNodes));
        }
    }

    @Override
    public void getCurrentNode(IResultCallback<NavigationNode> callback) {
        if (callback != null) {
            if (!targetNodes.isEmpty() && currentPosition < targetNodes.size()) {
                callback.onSuccess(targetNodes.get(currentPosition));
            } else if (peanutNavigation != null) {
                RouteNode rn = peanutNavigation.getCurrentNode();
                callback.onSuccess(convertToNavigationNode(rn));
            } else {
                callback.onSuccess(null);
            }
        }
    }

    @Override
    public void getNextNode(IResultCallback<NavigationNode> callback) {
        if (callback != null) {
            int nextIndex = currentPosition + 1;
            if (!targetNodes.isEmpty() && nextIndex < targetNodes.size()) {
                callback.onSuccess(targetNodes.get(nextIndex));
            } else if (peanutNavigation != null) {
                RouteNode rn = peanutNavigation.getNextNode();
                callback.onSuccess(convertToNavigationNode(rn));
            } else {
                callback.onSuccess(null);
            }
        }
    }

    @Override
    public void getCurrentPosition(IResultCallback<Integer> callback) {
        if (callback != null) {
            if (peanutNavigation != null) {
                callback.onSuccess(peanutNavigation.getCurrentPosition());
            } else {
                callback.onSuccess(currentPosition);
            }
        }
    }

    @Override
    public void isLastNode(IResultCallback<Boolean> callback) {
        if (callback != null) {
            if (peanutNavigation != null) {
                callback.onSuccess(peanutNavigation.isLastNode());
            } else {
                callback.onSuccess(currentPosition >= targetNodes.size() - 1);
            }
        }
    }

    @Override
    public void isLastRepeat(IResultCallback<Boolean> callback) {
        if (callback != null) {
            if (peanutNavigation != null) {
                callback.onSuccess(peanutNavigation.isLastRepeat());
            } else {
                callback.onSuccess(true);
            }
        }
    }

    // ==================== 回调注册 ====================

    @Override
    public void registerCallback(INavigationCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
            Log.d(TAG, "回调已注册，当前数量：" + callbacks.size());
        }
    }

    @Override
    public void unregisterCallback(INavigationCallback callback) {
        if (callbacks.remove(callback)) {
            Log.d(TAG, "回调已注销，当前数量：" + callbacks.size());
        }
    }

    @Override
    public void release() {
        Log.d(TAG, "释放 NavigationService 资源");
        callbacks.clear();
        targetNodes.clear();
        if (peanutNavigation != null) {
            try {
                peanutNavigation.stop(); // 先停止
                peanutNavigation.release(); // 再释放
            } catch (Exception e) {
                Log.e(TAG, "释放 peanutNavigation 异常", e);
            }
            peanutNavigation = null;
        }
    }

    // ==================== 回调分发 ====================

    private void notifyStateChanged(int state, int scheduleStatus) {
        for (INavigationCallback callback : callbacks) {
            try {
                callback.onStateChanged(state, scheduleStatus);
            } catch (Exception e) {
                Log.e(TAG, "回调 onStateChanged 异常", e);
            }
        }
    }

    private void notifyRouteNode(int index, NavigationNode node) {
        for (INavigationCallback callback : callbacks) {
            try {
                callback.onRouteNode(index, node);
            } catch (Exception e) {
                Log.e(TAG, "回调 onRouteNode 异常", e);
            }
        }
    }

    private void notifyRoutePrepared(List<NavigationNode> nodes) {
        Log.d(TAG, "【回调分发】通知路线准备完成，监听器数量: " + callbacks.size());
        for (INavigationCallback callback : callbacks) {
            try {
                callback.onRoutePrepared(nodes);
            } catch (Exception e) {
                Log.e(TAG, "回调 onRoutePrepared 异常", e);
            }
        }
    }

    private void notifyDistanceChanged(double distance) {
        for (INavigationCallback callback : callbacks) {
            try {
                callback.onDistanceChanged(distance);
            } catch (Exception e) {
                Log.e(TAG, "回调 onDistanceChanged 异常", e);
            }
        }
    }

    private void notifyError(int code, String message) {
        for (INavigationCallback callback : callbacks) {
            try {
                callback.onError(code, message);
            } catch (Exception e) {
                Log.e(TAG, "回调 onError 异常", e);
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 将 SDK RouteNode 转换为 NavigationNode
     * <p>
     * 注意: RouteNode 的坐标信息存储在其内部 Location 对象中，
     * 需要通过 getLocation() 获取后再获取 x, y, phi 坐标。
     * </p>
     */
    private NavigationNode convertToNavigationNode(RouteNode routeNode) {
        if (routeNode == null) {
            return null;
        }

        // 防御性初始化，防止 SDK 内部空指针
        initNavigationInfo(routeNode);

        NavigationNode node = new NavigationNode();
        node.setId(routeNode.getId());
        node.setName(routeNode.getName());
        node.setRouteNode(routeNode);
        return node;
    }

    /**
     * 防御性初始化 NavigationInfo，防止 SDK 内部空指针
     */
    private void initNavigationInfo(RouteNode routeNode) {
        try {
            if (routeNode != null && routeNode.getNavigationInfo() != null) {
                // [关键修复] 设置为最大值而不是 0，防止 SDK 误判为已到达目的地
                // SDK 规划路线成功后会自动更新这些值
                // [关键修复] 设置为 99999f (非 0 且非 MAX_VALUE)，防止 SDK 误判为已到达或溢出
                // SDK 规划路线成功后会自动更新这些值
                routeNode.getNavigationInfo().setTotalDistance(99999f);
                routeNode.getNavigationInfo().setRemainDistance(99999f);
                routeNode.getNavigationInfo().setTotalTime(99999f);
                routeNode.getNavigationInfo().setRemainTime(99999f);
            }
        } catch (Exception e) {
            Log.w(TAG, "initNavigationInfo 失败", e);
        }
    }

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

    /**
     * 将导航状态码转换为中文描述
     */
    private String getStateString(int state) {
        switch (state) {
            case Navigation.STATE_IDLE:
                return "空闲";
            case Navigation.STATE_PREPARED:
                return "已准备";
            case Navigation.STATE_RUNNING:
                return "导航中";
            case Navigation.STATE_DESTINATION:
                return "已到达目标点";
            case Navigation.STATE_PAUSED:
                return "已暂停";
            case Navigation.STATE_BLOCKED:
                return "被阻挡";
            case Navigation.STATE_BLOCKING:
                return "阻挡超时";
            case Navigation.STATE_COLLISION:
                return "碰撞";
            case Navigation.STATE_STOPPED:
                return "已停止";
            case Navigation.STATE_ERROR:
                return "错误";
            case Navigation.STATE_END:
                return "已结束";
            default:
                return "未知状态";
        }
    }

    /**
     * 创建 PeanutNavigation 实例的工厂方法。
     * <p>
     * 设计为 protected 以便在测试子类中重写并注入 Mock 对象。
     * </p>
     */
    protected PeanutNavigation createPeanutNavigation(PeanutNavigation.Builder builder) {
        return builder.build();
    }
}
