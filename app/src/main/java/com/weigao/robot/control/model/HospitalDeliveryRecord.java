package com.weigao.robot.control.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class HospitalDeliveryRecord implements Serializable {
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_FAILED_TIMEOUT = 1;
    public static final int STATUS_FAILED_HARDWARE = 2;
    public static final int STATUS_NAV_FAILED = 3;
    public static final int STATUS_CANCELLED = 4;

    public static final int STAGE_DISINFECTION = 0;
    public static final int STAGE_ROOM = 1;
    public static final int STAGE_RETURN = 2;

    private final String id;
    private final String taskId;
    private final long startTime;
    private final long endTime;
    private final String pointName;
    private final long duration;
    private final String formattedDuration;
    private final String createTime;
    private final int status;
    private final int stage;

    public HospitalDeliveryRecord(String taskId, long startTime, long endTime, String pointName, int status, int stage) {
        this.id = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.pointName = pointName;
        this.status = status;
        this.stage = stage;
        this.duration = endTime - startTime;
        this.formattedDuration = formatDuration(this.duration);
        this.createTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(endTime));
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long absSeconds = Math.abs(seconds);
        String positive = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                absSeconds / 3600, (absSeconds % 3600) / 60, absSeconds % 60);
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

    public int getStage() {
        return stage;
    }
}
