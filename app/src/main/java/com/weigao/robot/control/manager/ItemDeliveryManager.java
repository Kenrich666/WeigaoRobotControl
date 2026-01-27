package com.weigao.robot.control.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.weigao.robot.control.model.ItemDeliveryRecord;

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

    private void loadRecords() {
        if (context == null)
            return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_RECORDS, "");
        if (!json.isEmpty()) {
            try {
                Type type = new TypeToken<List<ItemDeliveryRecord>>() {
                }.getType();
                List<ItemDeliveryRecord> loaded = new Gson().fromJson(json, type);
                if (loaded != null) {
                    records = loaded;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading records", e);
            }
        }
    }

    private void saveRecords() {
        if (context == null)
            return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = new Gson().toJson(records);
            prefs.edit().putString(KEY_RECORDS, json).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving records", e);
        }
    }

    /**
     * 清空所有记录
     */
    public void clearRecords() {
        records.clear();
        if (context != null) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        }
    }
}
