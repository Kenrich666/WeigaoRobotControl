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
 * 路径: /sdcard/WeigaoRobot/settings/app_settings.json
 * 
 */
public class AppSettingsManager {
    private static final String TAG = "AppSettingsMgr";
    private static final String SETTINGS_DIR = "WeigaoRobot/settings";
    private static final String SETTINGS_FILE = "app_settings.json";

    private static final String KEY_FULLSCREEN = "is_fullscreen";

    private static AppSettingsManager instance;
    private boolean isFullScreen = false;

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
                    this.isFullScreen = json.optBoolean(KEY_FULLSCREEN, false);
                }
            }
        } catch (Exception e) {
            // Permission denied usually throws FileNotFoundException (EACCES)
            Log.e(TAG, "Failed to load app settings (likely due to permissions): " + e.getMessage());
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

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toString());
            }
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Failed to save app settings", e);
        }
    }
}
