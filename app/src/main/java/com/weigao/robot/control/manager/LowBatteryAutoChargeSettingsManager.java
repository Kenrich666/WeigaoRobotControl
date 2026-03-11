package com.weigao.robot.control.manager;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 低电量自动回充设置持久化管理器。
 */
public class LowBatteryAutoChargeSettingsManager {

    private static final String TAG = "LowBatteryChargeMgr";
    private static final String SETTINGS_DIR = "WeigaoRobot/config";
    private static final String SETTINGS_FILE = "low_battery_auto_charge_settings.json";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_THRESHOLD_PERCENT = "thresholdPercent";
    private static final Pattern ENABLED_PATTERN = Pattern
            .compile("\"" + KEY_ENABLED + "\"\\s*:\\s*(true|false)");
    private static final Pattern THRESHOLD_PATTERN = Pattern
            .compile("\"" + KEY_THRESHOLD_PERCENT + "\"\\s*:\\s*(-?\\d+)");

    private static LowBatteryAutoChargeSettingsManager instance;

    private final File storageRoot;
    private boolean enabled = false;
    private int thresholdPercent = 20;

    LowBatteryAutoChargeSettingsManager(File storageRoot) {
        this.storageRoot = storageRoot;
        loadSettings();
    }

    private LowBatteryAutoChargeSettingsManager() {
        this(Environment.getExternalStorageDirectory());
    }

    public static synchronized LowBatteryAutoChargeSettingsManager getInstance() {
        if (instance == null) {
            instance = new LowBatteryAutoChargeSettingsManager();
        }
        return instance;
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveSettings();
    }

    public synchronized int getThresholdPercent() {
        return thresholdPercent;
    }

    public synchronized void setThresholdPercent(int thresholdPercent) {
        this.thresholdPercent = clampThreshold(thresholdPercent);
        saveSettings();
    }

    public synchronized void update(boolean enabled, int thresholdPercent) {
        this.enabled = enabled;
        this.thresholdPercent = clampThreshold(thresholdPercent);
        saveSettings();
    }

    public synchronized void reloadSettings() {
        loadSettings();
        File file = getSettingsFile();
        if (!file.exists()) {
            saveSettings();
        }
    }

    private int clampThreshold(int thresholdPercent) {
        return Math.max(1, Math.min(100, thresholdPercent));
    }

    private File getSettingsDirectory() {
        return new File(storageRoot, SETTINGS_DIR);
    }

    private File getSettingsFile() {
        return new File(getSettingsDirectory(), SETTINGS_FILE);
    }

    private synchronized void loadSettings() {
        File file = getSettingsFile();
        if (!file.exists()) {
            enabled = false;
            thresholdPercent = 20;
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            if (builder.length() == 0) {
                enabled = false;
                thresholdPercent = 20;
                return;
            }

            String raw = builder.toString();
            Matcher enabledMatcher = ENABLED_PATTERN.matcher(raw);
            enabled = enabledMatcher.find() && Boolean.parseBoolean(enabledMatcher.group(1));

            Matcher thresholdMatcher = THRESHOLD_PATTERN.matcher(raw);
            int parsedThreshold = 20;
            if (thresholdMatcher.find()) {
                try {
                    parsedThreshold = Integer.parseInt(thresholdMatcher.group(1));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid threshold in low battery auto charge settings, using default.", e);
                }
            }
            thresholdPercent = clampThreshold(parsedThreshold);
        } catch (IOException e) {
            if (e.getMessage() != null
                    && (e.getMessage().contains("EACCES") || e.getMessage().contains("Permission denied"))) {
                Log.w(TAG, "Permissions missing - unable to load low battery auto charge settings, using defaults.");
            } else {
                Log.e(TAG, "Failed to load low battery auto charge settings", e);
            }
            enabled = false;
            thresholdPercent = 20;
        }
    }

    private synchronized void saveSettings() {
        File dir = getSettingsDirectory();
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "Failed to create settings directory: " + dir.getAbsolutePath());
        }
        File file = getSettingsFile();

        try {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("{\"" + KEY_ENABLED + "\":" + enabled
                        + ",\"" + KEY_THRESHOLD_PERCENT + "\":" + thresholdPercent + "}");
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to save low battery auto charge settings", e);
        }
    }

    static synchronized void resetSingletonForTest() {
        instance = null;
    }
}
