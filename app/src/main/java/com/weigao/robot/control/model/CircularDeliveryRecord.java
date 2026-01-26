package com.weigao.robot.control.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

public class CircularDeliveryRecord {
    private String routeName;
    private long startTime; // Unix timestamp in millis
    private long endTime;   // Unix timestamp in millis
    private long durationSeconds;
    private int loopCount;
    private String status; // "COMPLETED", "CANCELLED", "ABORTED"

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

    public long getDurationSeconds() {
        return durationSeconds;
    }
    
    public String getFormattedStartTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(startTime));
    }

    public int getLoopCount() {
        return loopCount;
    }

    public String getStatus() {
        return status;
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
