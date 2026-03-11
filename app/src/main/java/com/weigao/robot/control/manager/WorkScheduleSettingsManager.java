package com.weigao.robot.control.manager;

import android.os.Environment;
import android.util.Log;

import com.weigao.robot.control.model.WorkSchedule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作时段设置管理器
 * <p>
 * 管理工作时段的持久化存储。支持多个工作时段，每个时段有独立的启用开关、
 * 上下班时间和工作日期配置。
 * </p>
 * 存储路径: /sdcard/WeigaoRobot/config/work_schedule_settings.json
 */
public class WorkScheduleSettingsManager {

    private static final String TAG = "WorkScheduleMgr";
    private static final String SETTINGS_DIR = "WeigaoRobot/config";
    private static final String SETTINGS_FILE = "work_schedule_settings.json";

    /** 最大支持的工作时段数量 */
    public static final int MAX_SCHEDULES = 10;

    private static WorkScheduleSettingsManager instance;
    private List<WorkSchedule> schedules;

    private WorkScheduleSettingsManager() {
        schedules = new ArrayList<>();
        loadSettings();
    }

    public static synchronized WorkScheduleSettingsManager getInstance() {
        if (instance == null) {
            instance = new WorkScheduleSettingsManager();
        }
        return instance;
    }

    /**
     * 获取所有工作时段
     */
    public List<WorkSchedule> getSchedules() {
        return new ArrayList<>(schedules);
    }

    /**
     * 获取指定索引的工作时段
     */
    public WorkSchedule getSchedule(int index) {
        if (index >= 0 && index < schedules.size()) {
            return schedules.get(index);
        }
        return new WorkSchedule();
    }

    /**
     * 更新指定索引的工作时段并保存
     */
    public void updateSchedule(int index, WorkSchedule schedule) {
        if (index >= 0 && index < schedules.size()) {
            schedules.set(index, schedule);
            saveSettings();
        }
    }

    /**
     * 添加一个新的工作时段
     * @return 新时段的索引，如果已达上限则返回 -1
     */
    public int addSchedule() {
        if (schedules.size() >= MAX_SCHEDULES) {
            return -1;
        }
        schedules.add(new WorkSchedule());
        saveSettings();
        return schedules.size() - 1;
    }

    /**
     * 删除指定索引的工作时段
     */
    public void removeSchedule(int index) {
        if (index >= 0 && index < schedules.size()) {
            schedules.remove(index);
            saveSettings();
        }
    }

    /**
     * 获取时段数量
     */
    public int getScheduleCount() {
        return schedules.size();
    }

    /**
     * 获取所有已启用的工作时段
     */
    public List<WorkSchedule> getEnabledSchedules() {
        List<WorkSchedule> enabled = new ArrayList<>();
        for (WorkSchedule schedule : schedules) {
            if (schedule.isEnabled()) {
                enabled.add(schedule);
            }
        }
        return enabled;
    }

    /**
     * 重新加载设置。权限获取后调用。
     */
    public void reloadSettings() {
        loadSettings();
        File file = new File(Environment.getExternalStorageDirectory(), SETTINGS_DIR + "/" + SETTINGS_FILE);
        if (!file.exists()) {
            saveSettings();
        }
    }



    private void loadSettings() {
        File file = new File(Environment.getExternalStorageDirectory(), SETTINGS_DIR + "/" + SETTINGS_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            if (sb.length() > 0) {
                JSONObject json = new JSONObject(sb.toString());
                JSONArray schedulesArray = json.optJSONArray("schedules");
                if (schedulesArray != null) {
                    schedules.clear();
                    for (int i = 0; i < schedulesArray.length(); i++) {
                        JSONObject scheduleJson = schedulesArray.optJSONObject(i);
                        if (scheduleJson != null) {
                            schedules.add(WorkSchedule.fromJson(scheduleJson));
                        }
                    }
                }
            }
        } catch (IOException | JSONException e) {
            if (e.getMessage() != null && (e.getMessage().contains("EACCES") || e.getMessage().contains("Permission denied"))) {
                Log.w(TAG, "Permissions missing - unable to load work schedule settings, using defaults.");
            } else {
                Log.e(TAG, "Failed to load work schedule settings", e);
            }
        }
    }

    private void saveSettings() {
        File dir = new File(Environment.getExternalStorageDirectory(), SETTINGS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, SETTINGS_FILE);

        try {
            JSONObject json = new JSONObject();
            JSONArray schedulesArray = new JSONArray();
            for (WorkSchedule schedule : schedules) {
                schedulesArray.put(schedule.toJson());
            }
            json.put("schedules", schedulesArray);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toString());
            }
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Failed to save work schedule settings", e);
        }
    }
}
