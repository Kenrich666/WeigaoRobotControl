package com.weigao.robot.control.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.weigao.robot.control.model.ItemDeliveryRecord;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 物品配送记录管理器
 * 负责记录开始时间、生成记录并持久化
 */
public class ItemDeliveryManager {
    private static final String TAG = "ItemDeliveryManager";
    private static final String PREF_NAME = "item_delivery_records";
    private static final String KEY_RECORDS = "records";

    private static ItemDeliveryManager instance;
    private Context context;
    private long currentTripStartTime = 0;
    private String currentTaskId;
    private List<ItemDeliveryRecord> records;

    private ItemDeliveryManager() {
        records = new ArrayList<>();
    }

    public static ItemDeliveryManager getInstance() {
        if (instance == null) {
            synchronized (ItemDeliveryManager.class) {
                if (instance == null) {
                    instance = new ItemDeliveryManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        loadRecords();
    }

    /**
     * 开始一次新的配送任务，记录出发时间
     */
    public void startDelivery() {
        this.currentTripStartTime = System.currentTimeMillis();
        this.currentTaskId = java.util.UUID.randomUUID().toString();
        Log.i(TAG, "Start delivery timing at: " + currentTripStartTime + ", task: " + currentTaskId);
    }

    /**
     * 到达点位并成功开门，记录数据
     * 
     * @param pointName 点位名称
     */
    public ItemDeliveryRecord recordPointArrival(String pointName) {
        return recordPointArrival(pointName, ItemDeliveryRecord.STATUS_SUCCESS);
    }

    public ItemDeliveryRecord recordPointArrival(String pointName, int status) {
        if (currentTripStartTime == 0) {
            Log.w(TAG, "Trip start time not set, using current time as approximation or ignoring.");
            return null;
        }

        long endTime = System.currentTimeMillis();
        // 如果是从旧版本升级或异常情况没有TaskId，生成一个占位符
        String taskId = currentTaskId != null ? currentTaskId : "unknown-" + currentTripStartTime;
        ItemDeliveryRecord record = new ItemDeliveryRecord(taskId, currentTripStartTime, endTime, pointName, status);

        // 添加到内存列表
        records.add(0, record); // 最新记录在最前

        // 持久化保存
        saveRecords();

        Log.i(TAG, "Recorded arrival at " + pointName + ", status: " + status + ", duration: "
                + record.getFormattedDuration());
        return record;
    }

    /**
     * 获取所有记录
     */
    public List<ItemDeliveryRecord> getAllRecords() {
        return new ArrayList<>(records);
    }

    private static final String HISTORY_DIR = "WeigaoRobot/history";
    private static final String HISTORY_FILE = "item_delivery_history.json";
    
    // Limits the history to avoid overflow, e.g., last 100 records
    private static final int MAX_HISTORY_SIZE = 100;

    private void loadRecords() {
        File file = new File(android.os.Environment.getExternalStorageDirectory(), HISTORY_DIR + "/" + HISTORY_FILE);
        if (!file.exists()) {
            // Try migrate from legacy SharedPreferences if file doesn't exist
            loadFromLegacyPrefs();
            return;
        }

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            if (sb.length() > 0) {
                Type type = new TypeToken<List<ItemDeliveryRecord>>() {}.getType();
                List<ItemDeliveryRecord> loaded = new Gson().fromJson(sb.toString(), type);
                if (loaded != null) {
                    records = loaded;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading records from file", e);
        }
    }
    
    private void loadFromLegacyPrefs() {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_RECORDS, "");
        if (!json.isEmpty()) {
            try {
                Type type = new TypeToken<List<ItemDeliveryRecord>>() {}.getType();
                List<ItemDeliveryRecord> loaded = new Gson().fromJson(json, type);
                if (loaded != null) {
                    records = loaded;
                    // Immediately save to new file format
                    saveRecords();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading legacy records", e);
            }
        }
    }

    private void saveRecords() {
        // Enforce limit
        if (records.size() > MAX_HISTORY_SIZE) {
            records = new ArrayList<>(records.subList(0, MAX_HISTORY_SIZE));
        }

        File dir = new File(android.os.Environment.getExternalStorageDirectory(), HISTORY_DIR);
        if (!dir.exists()) {
             dir.mkdirs();
        }
        File file = new File(dir, HISTORY_FILE);

        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            String json = new Gson().toJson(records);
            writer.write(json);
        } catch (Exception e) {
            Log.e(TAG, "Error saving records to file", e);
        }
    }

    /**
     * 清空所有记录
     */
    public void clearRecords() {
        records.clear();
        saveRecords(); // Save empty list to file
        // Also clear legacy if present
        if (context != null) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        }
    }
}
