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
 * Manager for Circular Delivery settings persistence.
 * Stores settings in a JSON file on external storage.
 */
public class CircularDeliverySettingsManager {
    private static final String TAG = "CircularSettingsMgr";
    private static final String SETTINGS_DIR = "WeigaoRobot/config";
    private static final String SETTINGS_FILE = "circular_settings.json";

    private static final String KEY_SPEED = "delivery_speed";
    private static final String KEY_RETURN_SPEED = "return_speed";
    // Default speed in cm/s
    private static final int DEFAULT_SPEED = 50;

    private static CircularDeliverySettingsManager instance;
    private int deliverySpeed = DEFAULT_SPEED;
    private int returnSpeed = DEFAULT_SPEED;

    private CircularDeliverySettingsManager() {
        loadSettings();
    }

    public static synchronized CircularDeliverySettingsManager getInstance() {
        if (instance == null) {
            instance = new CircularDeliverySettingsManager();
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
                this.deliverySpeed = json.optInt(KEY_SPEED, DEFAULT_SPEED);
                this.returnSpeed = json.optInt(KEY_RETURN_SPEED, DEFAULT_SPEED);
            }
        } catch (IOException | JSONException e) {
            if (e.getMessage() != null && (e.getMessage().contains("EACCES") || e.getMessage().contains("Permission denied"))) {
                Log.w(TAG, "Permissions missing - unable to load circular settings, using defaults.");
            } else {
                Log.e(TAG, "Failed to load circular settings", e);
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

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toString());
            }
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Failed to save circular settings", e);
        }
    }
}
