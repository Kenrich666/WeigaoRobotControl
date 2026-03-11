package com.weigao.robot.control.manager;

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
 * Manager for Hospital Delivery settings persistence.
 * Stores settings in a JSON file on external storage.
 */
public class HospitalDeliverySettingsManager {
    private static final String TAG = "HospitalSettingsMgr";
    private static final String SETTINGS_DIR = "WeigaoRobot/config";
    private static final String SETTINGS_FILE = "hospital_delivery_settings.json";

    private static final String KEY_SPEED = "delivery_speed";
    private static final String KEY_RETURN_SPEED = "return_speed";
    private static final String KEY_ARRIVAL_STAY_DURATION = "arrival_stay_duration";
    private static final String KEY_RETURN_POINT_ID = "return_point_id";
    private static final String KEY_RETURN_POINT_NAME = "return_point_name";
    private static final int DEFAULT_SPEED = 50;
    private static final int DEFAULT_ARRIVAL_STAY_DURATION = 30;

    private static HospitalDeliverySettingsManager instance;
    private int deliverySpeed = DEFAULT_SPEED;
    private int returnSpeed = DEFAULT_SPEED;
    private int arrivalStayDuration = DEFAULT_ARRIVAL_STAY_DURATION;
    private int returnPointId = -1;
    private String returnPointName = "";

    private HospitalDeliverySettingsManager() {
        loadSettings();
    }

    public static synchronized HospitalDeliverySettingsManager getInstance() {
        if (instance == null) {
            instance = new HospitalDeliverySettingsManager();
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

    public int getArrivalStayDuration() {
        return arrivalStayDuration;
    }

    public void setArrivalStayDuration(int duration) {
        this.arrivalStayDuration = duration;
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
                this.deliverySpeed = json.optInt(KEY_SPEED, DEFAULT_SPEED);
                this.returnSpeed = json.optInt(KEY_RETURN_SPEED, DEFAULT_SPEED);
                this.arrivalStayDuration = json.optInt(KEY_ARRIVAL_STAY_DURATION, DEFAULT_ARRIVAL_STAY_DURATION);
                this.returnPointId = json.optInt(KEY_RETURN_POINT_ID, -1);
                this.returnPointName = json.optString(KEY_RETURN_POINT_NAME, "");
            }
        } catch (IOException | JSONException e) {
            if (e.getMessage() != null && (e.getMessage().contains("EACCES") || e.getMessage().contains("Permission denied"))) {
                Log.w(TAG, "Permissions missing - unable to load hospital delivery settings, using defaults.");
            } else {
                Log.e(TAG, "Failed to load hospital delivery settings", e);
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
            json.put(KEY_RETURN_POINT_ID, returnPointId);
            json.put(KEY_RETURN_POINT_NAME, returnPointName);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toString());
            }
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Failed to save hospital delivery settings", e);
        }
    }
}
