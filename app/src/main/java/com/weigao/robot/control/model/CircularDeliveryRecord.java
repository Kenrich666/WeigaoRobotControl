package com.weigao.robot.control.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 循环配送记录
 * 记录整个循环配送任务的基本信息和完成状态
 */
public class CircularDeliveryRecord {
    // 状态常量
    public static final String STATUS_COMPLETED = "COMPLETED";   // 完成所有循环
    public static final String STATUS_CANCELLED = "CANCELLED";   // 用户主动取消
    public static final String STATUS_NAV_FAILED = "NAV_FAILED"; // 导航失败
    public static final String STATUS_ABORTED = "ABORTED";       // 异常终止
    
    private String routeName;
    private long startTime; // Unix timestamp in millis
    private long endTime;   // Unix timestamp in millis
    private long durationSeconds;
    private int loopCount;
    private String status;

    public CircularDeliveryRecord(String routeName, int loopCount, long startTime) {
        this.routeName = routeName;
        this.loopCount = loopCount;
        this.startTime = startTime;
    }

    public void complete(String status) {
        this.endTime = System.currentTimeMillis();
        this.status = status;
        this.durationSeconds = (endTime - startTime) / 1000;
    }

    public String getRouteName() {
        return routeName;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }
    
    public String getFormattedStartTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(startTime));
    }

    public String getFormattedEndTime() {
        if (endTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(endTime));
        }
        return "未完成";
    }

    public int getLoopCount() {
        return loopCount;
    }

    public String getStatus() {
        return status;
    }
    
    /**
     * 获取格式化的状态文本
     * @return 中文状态描述
     */
    public String getStatusText() {
        if (STATUS_COMPLETED.equals(status)) {
            return "✅ 配送完成";
        } else if (STATUS_CANCELLED.equals(status) || STATUS_ABORTED.equals(status)) {
            return "⏸️ 已取消";
        } else if (STATUS_NAV_FAILED.equals(status)) {
            return "❌ 导航失败";
        }
        return "❓ 未知状态";
    }
    
    /**
     * 获取格式化的耗时
     * @return HH:mm:ss 格式的耗时字符串
     */
    public String getFormattedDuration() {
        if (durationSeconds > 0) {
            long hours = durationSeconds / 3600;
            long minutes = (durationSeconds % 3600) / 60;
            long seconds = durationSeconds % 60;
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return "计算中...";
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("routeName", routeName);
            obj.put("startTime", startTime);
            obj.put("endTime", endTime);
            obj.put("duration", durationSeconds);
            obj.put("loopCount", loopCount);
            obj.put("status", status);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static CircularDeliveryRecord fromJson(JSONObject obj) {
        // Implementation for reading back if needed
        CircularDeliveryRecord record = new CircularDeliveryRecord(
            obj.optString("routeName"),
            obj.optInt("loopCount"),
            obj.optLong("startTime")
        );
        record.endTime = obj.optLong("endTime");
        record.durationSeconds = obj.optLong("duration");
        record.status = obj.optString("status");
        return record;
    }
}
