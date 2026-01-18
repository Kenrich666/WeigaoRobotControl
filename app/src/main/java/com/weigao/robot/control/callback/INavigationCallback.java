package com.weigao.robot.control.callback;

import com.weigao.robot.control.model.NavigationNode;

import java.util.List;

/**
 * 导航状态回调接口
 * <p>
 * 对齐SDK {@code Navigation.Listener}，用于接收导航过程中的状态变化和事件通知。
 * </p>
 */
public interface INavigationCallback {

    /**
     * 导航状态发生变化
     *
     * @param state    导航状态码，参见
     *                 {@link com.weigao.robot.control.model.NavigationState}
     * @param schedule 调度状态，0=未调度，非0=调度中
     */
    void onStateChanged(int state, int schedule);

    /**
     * 当前导航目标点更新
     *
     * @param index 目标点在路线中的索引
     * @param node  目标点信息
     */
    void onRouteNode(int index, NavigationNode node);

    /**
     * 路线规划完成，准备就绪
     *
     * @param nodes 规划好的目标点列表
     */
    void onRoutePrepared(List<NavigationNode> nodes);

    /**
     * 到当前目标点的剩余距离变化
     *
     * @param distance 剩余距离，单位：米
     */
    void onDistanceChanged(float distance);

    /**
     * 导航过程中发生错误
     *
     * @param errorCode 错误码，参见SDK错误码定义（203400-203411）
     */
    void onNavigationError(int errorCode);
}
