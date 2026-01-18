package com.weigao.robot.control.service;

import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.TaskTiming;

import java.util.List;

/**
 * 作业计时服务接口
 * <p>
 * 满足需求书第2章"作业过程计量功能"要求：
 * <ul>
 * <li>计时起始点：配送开始时刻</li>
 * <li>计时终止点：机器人到达目的地并再次打开舱门取用物品时刻</li>
 * </ul>
 * </p>
 */
public interface ITimingService {

    /**
     * 开始计时
     * <p>
     * 在配送任务开始时调用，记录计时起始点。
     * </p>
     *
     * @param taskId   关联的任务ID
     * @param callback 结果回调
     */
    void startTiming(String taskId, IResultCallback<Void> callback);

    /**
     * 停止计时
     * <p>
     * 在舱门打开取物时调用，记录计时终止点并返回完整计时信息。
     * </p>
     *
     * @param taskId   关联的任务ID
     * @param callback 结果回调，返回完整的计时信息
     */
    void stopTiming(String taskId, IResultCallback<TaskTiming> callback);

    /**
     * 暂停计时
     *
     * @param taskId   关联的任务ID
     * @param callback 结果回调
     */
    void pauseTiming(String taskId, IResultCallback<Void> callback);

    /**
     * 恢复计时
     *
     * @param taskId   关联的任务ID
     * @param callback 结果回调
     */
    void resumeTiming(String taskId, IResultCallback<Void> callback);

    /**
     * 获取当前计时信息
     *
     * @param taskId   关联的任务ID
     * @param callback 结果回调，返回当前计时信息（包含实时已用时间）
     */
    void getCurrentTiming(String taskId, IResultCallback<TaskTiming> callback);

    /**
     * 获取计时历史记录
     *
     * @param taskId   关联的任务ID，传null获取所有历史
     * @param callback 结果回调，返回计时历史列表
     */
    void getTimingHistory(String taskId, IResultCallback<List<TaskTiming>> callback);

    /**
     * 清除计时历史
     *
     * @param taskId   关联的任务ID，传null清除所有历史
     * @param callback 结果回调
     */
    void clearTimingHistory(String taskId, IResultCallback<Void> callback);

    /**
     * 当舱门打开时自动停止对应任务的计时
     * <p>
     * 根据需求书要求，"再次打开舱门取用物品"时应自动停止计时。
     * 调用此方法启用该功能。
     * </p>
     *
     * @param enabled  是否启用
     * @param callback 结果回调
     */
    void setAutoStopOnDoorOpen(boolean enabled, IResultCallback<Void> callback);
}
