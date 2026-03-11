package com.weigao.robot.control.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.weigao.robot.control.model.HospitalDeliveryRecord;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HospitalDeliveryManager {
    private static final String TAG = "HospitalDeliveryMgr";
    private static final String PREF_NAME = "hospital_delivery_records";
    private static final String KEY_RECORDS = "records";
    private static final String HISTORY_DIR = "WeigaoRobot/history";
    private static final String HISTORY_FILE = "hospital_delivery_history.json";
    private static final int MAX_HISTORY_SIZE = 100;

    private static HospitalDeliveryManager instance;

    private Context context;
    private long currentTripStartTime = 0L;
    private String currentTaskId;
    private List<HospitalDeliveryRecord> records = new ArrayList<>();

    private HospitalDeliveryManager() {
    }

    public static HospitalDeliveryManager getInstance() {
        if (instance == null) {
            synchronized (HospitalDeliveryManager.class) {
                if (instance == null) {
                    instance = new HospitalDeliveryManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        loadRecords();
    }

    public void startDelivery() {
        currentTripStartTime = System.currentTimeMillis();
        currentTaskId = java.util.UUID.randomUUID().toString();
        Log.i(TAG, "Start hospital delivery timing at: " + currentTripStartTime + ", task: " + currentTaskId);
    }

    public HospitalDeliveryRecord recordPointArrival(String pointName, int status, int stage) {
        if (currentTripStartTime == 0L) {
            Log.w(TAG, "Trip start time not set, skip hospital delivery record");
            return null;
        }

        long endTime = System.currentTimeMillis();
        String taskId = currentTaskId != null ? currentTaskId : "unknown-" + currentTripStartTime;
        HospitalDeliveryRecord record = new HospitalDeliveryRecord(taskId, currentTripStartTime, endTime, pointName, status, stage);
        records.add(0, record);
        saveRecords();
        return record;
    }

    public List<HospitalDeliveryRecord> getAllRecords() {
        return new ArrayList<>(records);
    }

    public void clearRecords() {
        records.clear();
        saveRecords();
        if (context != null) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        }
    }

    private void loadRecords() {
        File file = new File(android.os.Environment.getExternalStorageDirectory(), HISTORY_DIR + "/" + HISTORY_FILE);
        if (!file.exists()) {
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
                Type type = new TypeToken<List<HospitalDeliveryRecord>>() {}.getType();
                List<HospitalDeliveryRecord> loaded = new Gson().fromJson(sb.toString(), type);
                if (loaded != null) {
                    records = loaded;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading hospital delivery records", e);
        }
    }

    private void loadFromLegacyPrefs() {
        if (context == null) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_RECORDS, "");
        if (!json.isEmpty()) {
            try {
                Type type = new TypeToken<List<HospitalDeliveryRecord>>() {}.getType();
                List<HospitalDeliveryRecord> loaded = new Gson().fromJson(json, type);
                if (loaded != null) {
                    records = loaded;
                    saveRecords();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading legacy hospital delivery records", e);
            }
        }
    }

    private void saveRecords() {
        if (records.size() > MAX_HISTORY_SIZE) {
            records = new ArrayList<>(records.subList(0, MAX_HISTORY_SIZE));
        }

        File dir = new File(android.os.Environment.getExternalStorageDirectory(), HISTORY_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, HISTORY_FILE);

        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write(new Gson().toJson(records));
        } catch (Exception e) {
            Log.e(TAG, "Error saving hospital delivery records", e);
        }
    }
}
