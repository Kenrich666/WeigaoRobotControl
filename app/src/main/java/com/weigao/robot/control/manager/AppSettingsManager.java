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
 * Manager for Global App settings persistence.
 * Stores settings in a JSON file on external storage to survive reinstall.
 * 普通物品配送历史记录 (ItemDeliveryManager)
 * 路径: /sdcard/WeigaoRobot/history/item_delivery_history.json
 * 
 * 循环配送历史记录 (CircularDeliveryHistoryManager)
 * 路径: /sdcard/WeigaoRobot/history/delivery_history.json
 * 
 * 密码及安全配置 (SecurityServiceImpl)
 * 路径: /sdcard/WeigaoRobot/config/security_config.json
 * 
 * 循环配送路线设置 (CircularDeliveryActivity)
 * 路径: /sdcard/WeigaoRobot/routes/circular_routes.json
 * 
 * 应用全局设置（如全屏开关）
 * 路径: /sdcard/WeigaoRobot/config/app_settings.json
 * 
 */
public class AppSettingsManager {
    private static final String TAG = "AppSettingsMgr";
    private static final String SETTINGS_DIR = "WeigaoRobot/config";
    private static final String SETTINGS_FILE = "app_settings.json";

    private static final String KEY_FULLSCREEN = "is_fullscreen";
    private static final String KEY_PROJECTION_DOOR = "projection_door_enabled";
    private static final String KEY_ITEM_PROJECTION_DOOR = "item_projection_door_enabled";
    private static final String KEY_CIRCULAR_PROJECTION_DOOR = "circular_projection_door_enabled";
    private static final String KEY_HOSPITAL_PROJECTION_DOOR = "hospital_projection_door_enabled";
    private static final String KEY_PASSWORD_VERIFICATION = "password_verification_enabled";

    private static AppSettingsManager instance;
    private boolean isFullScreen = true;
    private boolean itemProjectionDoorEnabled = false;
    private boolean circularProjectionDoorEnabled = false;
    private boolean hospitalProjectionDoorEnabled = false;
    private boolean passwordVerificationEnabled = true;

    private AppSettingsManager() {
        // Delayed loading? Or just handle exception in loadSettings.
        // If constructed before permission, loadSettings fails but that is OK.
        // It will just default to false for fullscreen.
        try {
            loadSettings();
        } catch (Exception e) {
            Log.e(TAG, "Init load failed: " + e.getMessage());
        }
    }

    public static synchronized AppSettingsManager getInstance() {
        if (instance == null) {
            instance = new AppSettingsManager();
        }
        return instance;
    }

    public boolean isFullScreen() {
        return isFullScreen;
    }

    public void setFullScreen(boolean fullScreen) {
        this.isFullScreen = fullScreen;
        saveSettings();
    }

    public boolean isProjectionDoorEnabled() {
        return isAnyProjectionDoorEnabled();
    }

    public void setProjectionDoorEnabled(boolean enabled) {
        this.itemProjectionDoorEnabled = enabled;
        this.circularProjectionDoorEnabled = enabled;
        this.hospitalProjectionDoorEnabled = enabled;
        saveSettings();
    }

    public boolean isProjectionDoorEnabled(ProjectionDoorMode mode) {
        switch (mode) {
            case ITEM:
                return itemProjectionDoorEnabled;
            case CIRCULAR:
                return circularProjectionDoorEnabled;
            case HOSPITAL:
                return hospitalProjectionDoorEnabled;
            default:
                return false;
        }
    }

    public void setProjectionDoorEnabled(ProjectionDoorMode mode, boolean enabled) {
        switch (mode) {
            case ITEM:
                itemProjectionDoorEnabled = enabled;
                break;
            case CIRCULAR:
                circularProjectionDoorEnabled = enabled;
                break;
            case HOSPITAL:
                hospitalProjectionDoorEnabled = enabled;
                break;
            default:
                return;
        }
        saveSettings();
    }

    public boolean isAnyProjectionDoorEnabled() {
        return itemProjectionDoorEnabled || circularProjectionDoorEnabled || hospitalProjectionDoorEnabled;
    }

    public boolean isPasswordVerificationEnabled() {
        return passwordVerificationEnabled;
    }

    public void setPasswordVerificationEnabled(boolean enabled) {
        this.passwordVerificationEnabled = enabled;
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
        try {
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
                    boolean legacyProjectionDoorEnabled = json.optBoolean(KEY_PROJECTION_DOOR, false);
                    boolean missingProjectionModeKey = !json.has(KEY_ITEM_PROJECTION_DOOR)
                            || !json.has(KEY_CIRCULAR_PROJECTION_DOOR)
                            || !json.has(KEY_HOSPITAL_PROJECTION_DOOR);

                    this.isFullScreen = json.optBoolean(KEY_FULLSCREEN, true);
                    this.itemProjectionDoorEnabled = json.has(KEY_ITEM_PROJECTION_DOOR)
                            ? json.optBoolean(KEY_ITEM_PROJECTION_DOOR, false)
                            : legacyProjectionDoorEnabled;
                    this.circularProjectionDoorEnabled = json.has(KEY_CIRCULAR_PROJECTION_DOOR)
                            ? json.optBoolean(KEY_CIRCULAR_PROJECTION_DOOR, false)
                            : legacyProjectionDoorEnabled;
                    this.hospitalProjectionDoorEnabled = json.has(KEY_HOSPITAL_PROJECTION_DOOR)
                            ? json.optBoolean(KEY_HOSPITAL_PROJECTION_DOOR, false)
                            : legacyProjectionDoorEnabled;
                    this.passwordVerificationEnabled = json.optBoolean(KEY_PASSWORD_VERIFICATION, true);

                    if (missingProjectionModeKey) {
                        saveSettings();
                    }
                }
            }
        } catch (IOException | JSONException e) {
            if (e.getMessage() != null
                    && (e.getMessage().contains("EACCES") || e.getMessage().contains("Permission denied"))) {
                Log.w(TAG, "Permissions missing - unable to load app settings, using defaults.");
            } else {
                Log.e(TAG, "Failed to load app settings", e);
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
            json.put(KEY_FULLSCREEN, isFullScreen);
            json.put(KEY_ITEM_PROJECTION_DOOR, itemProjectionDoorEnabled);
            json.put(KEY_CIRCULAR_PROJECTION_DOOR, circularProjectionDoorEnabled);
            json.put(KEY_HOSPITAL_PROJECTION_DOOR, hospitalProjectionDoorEnabled);
            json.put(KEY_PASSWORD_VERIFICATION, passwordVerificationEnabled);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toString());
            }
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Failed to save app settings", e);
        }
    }
}
