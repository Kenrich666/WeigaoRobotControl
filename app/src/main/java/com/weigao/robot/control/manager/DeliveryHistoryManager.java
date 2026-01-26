package com.weigao.robot.control.manager;

import android.content.Context;
import android.util.Log;

import com.weigao.robot.control.model.DeliveryRecord;

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

public class DeliveryHistoryManager {
    private static final String TAG = "DeliveryHistoryManager";
    private static final String HISTORY_DIR = "WeigaoRobot/history";
    private static final String HISTORY_FILE = "delivery_history.json";
    
    // Limits the history to avoid overflow, e.g., last 100 records
    private static final int MAX_HISTORY_SIZE = 100; 

    private static DeliveryHistoryManager instance;
    private Context context;
    private List<DeliveryRecord> historyList;

    private DeliveryHistoryManager(Context context) {
        this.context = context.getApplicationContext();
        this.historyList = new ArrayList<>();
        loadHistory();
    }

    public static synchronized DeliveryHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new DeliveryHistoryManager(context);
        }
        return instance;
    }

    public void addRecord(DeliveryRecord record) {
        historyList.add(0, record); // Add to top
        if (historyList.size() > MAX_HISTORY_SIZE) {
            historyList.remove(historyList.size() - 1);
        }
        saveHistory();
    }

    public List<DeliveryRecord> getHistory() {
        return new ArrayList<>(historyList);
    }

    private void saveHistory() {
        // Use External Storage to survive uninstall
        File dir = new File(android.os.Environment.getExternalStorageDirectory(), HISTORY_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, HISTORY_FILE);

        JSONArray array = new JSONArray();
        for (DeliveryRecord record : historyList) {
            array.put(record.toJson());
        }
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(array.toString());
        } catch (IOException e) {
            Log.e(TAG, "Failed to save history to file", e);
        }
    }

    private void loadHistory() {
        File file = new File(android.os.Environment.getExternalStorageDirectory(), HISTORY_DIR + "/" + HISTORY_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JSONArray array = new JSONArray(sb.toString());
            historyList.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                historyList.add(DeliveryRecord.fromJson(obj));
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to load history from file", e);
        }
    }
}
