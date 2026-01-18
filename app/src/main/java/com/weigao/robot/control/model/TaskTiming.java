package com.weigao.robot.control.model;

import java.io.Serializable;

/**
 * 作业计时模型
 * <p>
 * 满足需求书第2章"作业过程计量功能"要求：
 * <ul>
 * <li>计时起始点：配送开始时刻</li>
 * <li>计时终止点：机器人到达目的地并再次打开舱门取用物品时刻</li>
 * </ul>
 * </p>
 */
public class TaskTiming implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联的任务ID */
    private String taskId;

    /** 计时开始时间（Unix时间戳，毫秒） */
    private long startTime;

    /** 计时结束时间（Unix时间戳，毫秒），0表示尚未结束 */
    private long endTime;

    /** 已用时间（毫秒） */
    private long elapsedTime;

    /** 计时状态 */
    private TimingStatus status;

    /** 暂停累计时间（毫秒） */
    private long pausedDuration;

    /** 最后一次暂停开始时间 */
    private long lastPauseTime;

    /**
     * 计时状态枚举
     */
    public enum TimingStatus {
        /** 尚未开始 */
        NOT_STARTED,
        /** 计时中 */
        RUNNING,
        /** 已暂停 */
        PAUSED,
        /** 已停止/完成 */
        STOPPED
    }

    public TaskTiming() {
        this.status = TimingStatus.NOT_STARTED;
    }

    public TaskTiming(String taskId) {
        this.taskId = taskId;
        this.status = TimingStatus.NOT_STARTED;
    }

    // Getters and Setters

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public TimingStatus getStatus() {
        return status;
    }

    public void setStatus(TimingStatus status) {
        this.status = status;
    }

    public long getPausedDuration() {
        return pausedDuration;
    }

    public void setPausedDuration(long pausedDuration) {
        this.pausedDuration = pausedDuration;
    }

    public long getLastPauseTime() {
        return lastPauseTime;
    }

    public void setLastPauseTime(long lastPauseTime) {
        this.lastPauseTime = lastPauseTime;
    }

    @Override
    public String toString() {
        return "TaskTiming{" +
                "taskId='" + taskId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", elapsedTime=" + elapsedTime +
                ", status=" + status +
                '}';
    }
}
