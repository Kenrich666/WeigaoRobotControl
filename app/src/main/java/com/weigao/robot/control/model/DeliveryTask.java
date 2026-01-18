package com.weigao.robot.control.model;

import java.io.Serializable;
import java.util.List;

/**
 * 配送任务模型
 * <p>
 * 表示一个配送任务的完整信息，包括任务类型、目标点列表、配置和状态。
 * </p>
 */
public class DeliveryTask implements Serializable {

    private static final long serialVersionUID = 1L;
    private String taskId;
    private DeliveryType type;
    private List<PointInfo> points;
    private DeliveryConfig config;
    private DeliveryStatus status;
    private String currentPointId;
    private int progress;
    private long startTime;
    private long endTime;

    public enum DeliveryType {
        STANDARD,
        LOOP,
        RECOVERY,
        SLOW
    }

    public enum DeliveryStatus {
        PENDING,
        RUNNING,
        PAUSED,
        COMPLETED,
        CANCELLED,
        FAILED
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public DeliveryType getType() {
        return type;
    }

    public void setType(DeliveryType type) {
        this.type = type;
    }

    public List<PointInfo> getPoints() {
        return points;
    }

    public void setPoints(List<PointInfo> points) {
        this.points = points;
    }

    public DeliveryConfig getConfig() {
        return config;
    }

    public void setConfig(DeliveryConfig config) {
        this.config = config;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public String getCurrentPointId() {
        return currentPointId;
    }

    public void setCurrentPointId(String currentPointId) {
        this.currentPointId = currentPointId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
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
}
