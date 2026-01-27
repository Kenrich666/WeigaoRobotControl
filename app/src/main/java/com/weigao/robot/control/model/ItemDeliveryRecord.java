package com.weigao.robot.control.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * 物品配送记录实体类
 */
public class ItemDeliveryRecord implements Serializable {
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_FAILED_TIMEOUT = 1;
    public static final int STATUS_FAILED_HARDWARE = 2;
    public static final int STATUS_NAV_FAILED = 3;
    public static final int STATUS_CANCELLED = 4;

    private String id;
    private String taskId; // 任务ID，用于分组
    private long startTime;
    private long endTime;
    private String pointName;
    private long duration;
    private String formattedDuration;
    private String createTime;
    private int status; // 配送状态

    public ItemDeliveryRecord(String taskId, long startTime, long endTime, String pointName, int status) {
        this.id = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.pointName = pointName;
        this.status = status;
        this.duration = endTime - startTime;
        this.formattedDuration = formatDuration(this.duration);
        this.createTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(endTime));
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long absSeconds = Math.abs(seconds);
        String positive = String.format(Locale.getDefault(),
                "%02d:%02d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60);
        return seconds < 0 ? "-" + positive : positive;
    }

    public String getId() {
        return id;
    }

    public String getTaskId() {
        return taskId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getPointName() {
        return pointName;
    }

    public long getDuration() {
        return duration;
    }

    public String getFormattedDuration() {
        return formattedDuration;
    }

    public String getCreateTime() {
        return createTime;
    }

    public int getStatus() {
        return status;
    }
}
