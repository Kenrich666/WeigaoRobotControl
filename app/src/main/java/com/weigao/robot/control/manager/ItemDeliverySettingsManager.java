package com.weigao.robot.control.manager;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Manager for Item Delivery settings persistence.
 * Stores settings in a JSON file on external storage.
 */
public class ItemDeliverySettingsManager {
    private static final String TAG = "ItemSettingsMgr";
    private static final String SETTINGS_DIR = "WeigaoRobot/config";
    private static final String SETTINGS_FILE = "item_delivery_settings.json";

    private static final String KEY_SPEED = "delivery_speed";
    private static final String KEY_RETURN_SPEED = "return_speed";
    private static final String KEY_ARRIVAL_STAY_DURATION = "arrival_stay_duration";
    private static final String KEY_ARRIVAL_STAY_ENABLED = "item_arrival_stay_enabled";
    private static final String KEY_RETURN_POINT_ID = "return_point_id";
    private static final String KEY_RETURN_POINT_NAME = "return_point_name";
    // Default speed in cm/s
    private static final int DEFAULT_SPEED = 50;
    // 默认取物停留时间（秒）
    private static final int DEFAULT_ARRIVAL_STAY_DURATION = 30;

    private static ItemDeliverySettingsManager instance;
    private int deliverySpeed = DEFAULT_SPEED;
    private int returnSpeed = DEFAULT_SPEED;
    private int arrivalStayDuration = DEFAULT_ARRIVAL_STAY_DURATION;
    private boolean arrivalStayEnabled = false;
    private int returnPointId = -1;
    private String returnPointName = "";

    private ItemDeliverySettingsManager() {
        loadSettings();
    }

    public static synchronized ItemDeliverySettingsManager getInstance() {
        if (instance == null) {
            instance = new ItemDeliverySettingsManager();
        }
        return instance;
    }

    public int getDeliverySpeed() {
        return deliverySpeed;
    }



    public void setDeliverySpeed(int speed) {
        this.deliverySpeed = speed;
        saveSettings();
    }

    public int getReturnSpeed() {
        return returnSpeed;
    }

    public void setReturnSpeed(int speed) {
        this.returnSpeed = speed;
        saveSettings();
    }

    /**
     * 获取取物停留时间（秒）
     */
    public int getArrivalStayDuration() {
        return arrivalStayDuration;
    }

    /**
     * 设置取物停留时间（秒）
     */
    public void setArrivalStayDuration(int duration) {
        this.arrivalStayDuration = duration;
        saveSettings();
    }

    public boolean isArrivalStayEnabled() {
        return arrivalStayEnabled;
    }

    public void setArrivalStayEnabled(boolean enabled) {
        this.arrivalStayEnabled = enabled;
        saveSettings();
    }

    public int getReturnPointId() {
        return returnPointId;
    }

    public String getReturnPointName() {
        return returnPointName == null ? "" : returnPointName;
    }

    public void setReturnPoint(int pointId, String pointName) {
        this.returnPointId = pointId;
        this.returnPointName = pointName == null ? "" : pointName;
        saveSettings();
    }

    /**
     * Reload settings from file. Useful after permissions are granted.
     * If file does not exist (new install), it will be created with default values.
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
                boolean missingArrivalStayEnabled = !json.has(KEY_ARRIVAL_STAY_ENABLED);
                this.deliverySpeed = json.optInt(KEY_SPEED, DEFAULT_SPEED);
                this.returnSpeed = json.optInt(KEY_RETURN_SPEED, DEFAULT_SPEED);
                this.arrivalStayDuration = json.optInt(KEY_ARRIVAL_STAY_DURATION, DEFAULT_ARRIVAL_STAY_DURATION);
                this.arrivalStayEnabled = json.optBoolean(KEY_ARRIVAL_STAY_ENABLED, false);
                this.returnPointId = json.optInt(KEY_RETURN_POINT_ID, -1);
                this.returnPointName = json.optString(KEY_RETURN_POINT_NAME, "");
                if (missingArrivalStayEnabled) {
                    saveSettings();
                }
            }
        } catch (IOException | JSONException e) {
            if (e.getMessage() != null && (e.getMessage().contains("EACCES") || e.getMessage().contains("Permission denied"))) {
                Log.w(TAG, "Permissions missing - unable to load item delivery settings, using defaults.");
            } else {
                Log.e(TAG, "Failed to load item delivery settings", e);
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
            json.put(KEY_SPEED, deliverySpeed);
            json.put(KEY_RETURN_SPEED, returnSpeed);
            json.put(KEY_ARRIVAL_STAY_DURATION, arrivalStayDuration);
            json.put(KEY_ARRIVAL_STAY_ENABLED, arrivalStayEnabled);
            json.put(KEY_RETURN_POINT_ID, returnPointId);
            json.put(KEY_RETURN_POINT_NAME, returnPointName);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toString());
            }
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Failed to save item delivery settings", e);
        }
    }
}
