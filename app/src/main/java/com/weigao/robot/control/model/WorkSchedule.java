package com.weigao.robot.control.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 工作时段数据模型
 * <p>
 * 表示一个可配置的工作时段，包含启用开关、上下班时间和工作日期。
 * </p>
 */
public class WorkSchedule {

    /** 时段是否启用 */
    private boolean enabled;

    /** 上班时间，格式 HH:mm */
    private String startTime;

    /** 下班时间，格式 HH:mm */
    private String endTime;

    /**
     * 工作日期，长度7的boolean数组
     * 索引 0=周一, 1=周二, 2=周三, 3=周四, 4=周五, 5=周六, 6=周日
     */
    private boolean[] workDays;

    public WorkSchedule() {
        this.enabled = false;
        this.startTime = "08:00";
        this.endTime = "17:00";
        this.workDays = new boolean[]{true, true, true, true, true, false, false}; // 默认周一至周五
    }

    public WorkSchedule(boolean enabled, String startTime, String endTime, boolean[] workDays) {
        this.enabled = enabled;
        this.startTime = startTime;
        this.endTime = endTime;
        this.workDays = workDays != null ? workDays : new boolean[7];
    }

    // ============ Getters & Setters ============

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public boolean[] getWorkDays() {
        return workDays;
    }

    public void setWorkDays(boolean[] workDays) {
        this.workDays = workDays;
    }

    /**
     * 获取上班时间的小时部分
     */
    public int getStartHour() {
        return parseHour(startTime);
    }

    /**
     * 获取上班时间的分钟部分
     */
    public int getStartMinute() {
        return parseMinute(startTime);
    }

    /**
     * 获取下班时间的小时部分
     */
    public int getEndHour() {
        return parseHour(endTime);
    }

    /**
     * 获取下班时间的分钟部分
     */
    public int getEndMinute() {
        return parseMinute(endTime);
    }

    /**
     * 获取工作日期的中文简称描述
     */
    public String getWorkDaysDescription() {
        String[] dayNames = {"一", "二", "三", "四", "五", "六", "日"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (workDays[i]) {
                if (sb.length() > 0) sb.append("、");
                sb.append("周").append(dayNames[i]);
            }
        }
        return sb.length() > 0 ? sb.toString() : "未选择";
    }

    // ============ JSON 序列化 ============

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("enabled", enabled);
        json.put("start_time", startTime);
        json.put("end_time", endTime);

        JSONArray daysArray = new JSONArray();
        for (boolean day : workDays) {
            daysArray.put(day);
        }
        json.put("work_days", daysArray);
        return json;
    }

    public static WorkSchedule fromJson(JSONObject json) {
        WorkSchedule schedule = new WorkSchedule();
        schedule.enabled = json.optBoolean("enabled", false);
        schedule.startTime = json.optString("start_time", "08:00");
        schedule.endTime = json.optString("end_time", "17:00");

        JSONArray daysArray = json.optJSONArray("work_days");
        if (daysArray != null) {
            boolean[] days = new boolean[7];
            for (int i = 0; i < Math.min(7, daysArray.length()); i++) {
                days[i] = daysArray.optBoolean(i, false);
            }
            schedule.workDays = days;
        }
        return schedule;
    }

    // ============ 私有辅助方法 ============

    private int parseHour(String time) {
        try {
            return Integer.parseInt(time.split(":")[0]);
        } catch (Exception e) {
            return 8;
        }
    }

    private int parseMinute(String time) {
        try {
            return Integer.parseInt(time.split(":")[1]);
        } catch (Exception e) {
            return 0;
        }
    }
}
