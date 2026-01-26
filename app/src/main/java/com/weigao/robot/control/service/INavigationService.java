package com.weigao.robot.control.service;

import com.weigao.robot.control.callback.INavigationCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.NavigationNode;

import java.util.List;

/**
 * 导航服务接口
 * <p>
 * 对齐SDK {@code PeanutNavigation}，提供机器人自主导航控制功能。
 * </p>
 */
public interface INavigationService {

    // ==================== 路线规划策略 ====================

    /** 路线规划策略：自适应（根据地图自动选择最优路径） */
    int POLICY_ADAPTIVE = 1;

    /** 路线规划策略：固定路径 */
    int POLICY_FIXED = 2;

    // ==================== 手动导航方向 ====================

    /** 手动导航方向：前进 */
    int DIRECTION_FORWARD = 1;

    /** 手动导航方向：后退 */
    int DIRECTION_BACKWARD = 2;

    /** 手动导航方向：左转 */
    int DIRECTION_LEFT = 3;

    /** 手动导航方向：右转 */
    int DIRECTION_RIGHT = 4;

    // ==================== 导航控制 ====================

    /**
     * 设置目标点列表
     *
     * @param targetIds 目标点ID列表
     * @param callback  结果回调
     */
    void setTargets(List<Integer> targetIds, IResultCallback<Void> callback);

    /**
     * 设置目标点列表（使用NavigationNode）
     *
     * @param targets  目标点列表
     * @param callback 结果回调
     */
    void setTargetNodes(List<NavigationNode> targets, IResultCallback<Void> callback);

    /**
     * 准备导航（路线规划）
     * <p>
     * 调用此方法进行路线规划，规划完成后会通过回调通知。
     * </p>
     *
     * @param callback 结果回调
     */
    void prepare(IResultCallback<Void> callback);

    /**
     * 开始/继续导航
     * <p>
     * 路线准备好后调用此方法开始导航，暂停后也可调用此方法继续导航。
     * </p>
     *
     * @param callback 结果回调
     */
    void start(IResultCallback<Void> callback);

    /**
     * 暂停导航
     *
     * @param callback 结果回调
     */
    void pause(IResultCallback<Void> callback);

    /**
     * 停止导航
     *
     * @param callback 结果回调
     */
    void stop(IResultCallback<Void> callback);

    /**
     * 切换到下一个目标点并继续导航
     *
     * @param callback 结果回调
     */
    void pilotNext(IResultCallback<Void> callback);

    /**
     * 跳到指定索引的目标点
     *
     * @param index    目标点索引
     * @param callback 结果回调
     */
    void skipTo(int index, IResultCallback<Void> callback);

    // ==================== 参数设置 ====================

    /**
     * 设置导航速度
     *
     * @param speed    速度，单位：cm/s
     * @param callback 结果回调
     */
    void setSpeed(int speed, IResultCallback<Void> callback);

    /**
     * 设置路线规划策略
     *
     * @param policy   策略，参见 {@link #POLICY_ADAPTIVE} 等常量
     * @param callback 结果回调
     */
    void setRoutePolicy(int policy, IResultCallback<Void> callback);

    /**
     * 设置阻挡超时时间
     * <p>
     * 被阻挡超过此时间后，将自动判定为到达并切换到下一目标点。
     * </p>
     *
     * @param timeout  超时时间，单位：毫秒
     * @param callback 结果回调
     */
    void setBlockingTimeout(int timeout, IResultCallback<Void> callback);

    /**
     * 设置循环次数
     *
     * @param count    循环次数，0表示不循环
     * @param callback 结果回调
     */
    void setRepeatCount(int count, IResultCallback<Void> callback);

    /**
     * 启用/禁用自动循环
     *
     * @param enabled  true=启用自动循环
     * @param callback 结果回调
     */
    void setAutoRepeat(boolean enabled, IResultCallback<Void> callback);

    /**
     * 启用/禁用近似到达控制
     *
     * @param enabled  true=启用
     * @param callback 结果回调
     */
    void setArrivalControlEnabled(boolean enabled, IResultCallback<Void> callback);

    /**
     * 取消当前点位的近似到达控制
     * <p>
     * 对齐SDK {@code PeanutNavigation.cancelArrivalControl()}，
     * 用于在导航过程中取消当前点位的近似到达判定。
     * </p>
     *
     * @param callback 结果回调
     */
    void cancelArrivalControl(IResultCallback<Void> callback);

    // ==================== 手动控制 ====================

    /**
     * 手动导航控制
     * <p>
     * 用于遥控模式下的手动移动控制。
     * </p>
     *
     * @param direction 方向，参见 {@link #DIRECTION_FORWARD} 等常量
     * @param callback  结果回调
     */
    void manual(int direction, IResultCallback<Void> callback);

    // ==================== 状态查询 ====================

    /**
     * 获取当前目标点列表
     *
     * @param callback 结果回调
     */
    void getRouteNodes(IResultCallback<List<NavigationNode>> callback);

    /**
     * 获取当前导航目标点
     *
     * @param callback 结果回调
     */
    void getCurrentNode(IResultCallback<NavigationNode> callback);

    /**
     * 获取下一个导航目标点
     *
     * @param callback 结果回调
     */
    void getNextNode(IResultCallback<NavigationNode> callback);

    /**
     * 获取当前目标点在路线中的索引
     *
     * @param callback 结果回调
     */
    void getCurrentPosition(IResultCallback<Integer> callback);

    /**
     * 查询是否为最后一个目标点
     *
     * @param callback 结果回调
     */
    void isLastNode(IResultCallback<Boolean> callback);

    /**
     * 查询是否为最后一次循环
     *
     * @param callback 结果回调
     */
    void isLastRepeat(IResultCallback<Boolean> callback);

    // ==================== 回调注册 ====================

    /**
     * 注册导航状态回调
     *
     * @param callback 导航回调
     */
    void registerCallback(INavigationCallback callback);

    /**
     * 注销导航状态回调
     *
     * @param callback 导航回调
     */
    void unregisterCallback(INavigationCallback callback);

    /**
     * 释放资源
     */
    void release();
}
