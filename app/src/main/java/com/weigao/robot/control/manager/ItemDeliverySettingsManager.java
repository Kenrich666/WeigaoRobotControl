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
    private static final String SETTINGS_DIR = "WeigaoRobot/settings";
    private static final String SETTINGS_FILE = "item_delivery_settings.json";

    private static final String KEY_SPEED = "delivery_speed";
    private static final String KEY_RETURN_SPEED = "return_speed";
    // Default speed in cm/s
    private static final int DEFAULT_SPEED = 50;

    private static ItemDeliverySettingsManager instance;
    private int deliverySpeed = DEFAULT_SPEED;
    private int returnSpeed = DEFAULT_SPEED;

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

    private void loadSettings() {
        File file = new File(Environment.getExternalStorageDirectory(), SETTINGS_DIR + "/" + SETTINGS_FILE);
        if (!file.exists()) {
            saveSettings();
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
            Log.e(TAG, "Failed to load item delivery settings", e);
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
            Log.e(TAG, "Failed to save item delivery settings", e);
        }
    }
}
