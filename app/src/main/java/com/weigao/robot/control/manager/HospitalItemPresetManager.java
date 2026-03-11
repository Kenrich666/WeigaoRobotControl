package com.weigao.robot.control.manager;

import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Manager for editable hospital item presets.
 */
public class HospitalItemPresetManager {
    private static final String TAG = "HospitalItemPresetMgr";
    private static final String SETTINGS_DIR = "WeigaoRobot/config";
    private static final String SETTINGS_FILE = "hospital_item_presets.json";
    private static final String KEY_PRESETS = "presets";

    private static final String[] DEFAULT_PRESETS = {"胃镜", "肠镜"};

    private static HospitalItemPresetManager instance;
    private final List<String> presetItems = new ArrayList<>();

    private HospitalItemPresetManager() {
        loadSettings();
    }

    public static synchronized HospitalItemPresetManager getInstance() {
        if (instance == null) {
            instance = new HospitalItemPresetManager();
        }
        return instance;
    }

    public synchronized List<String> getPresetItems() {
        if (presetItems.isEmpty()) {
            applyDefaultPresets();
        }
        return new ArrayList<>(presetItems);
    }

    public synchronized void savePresetItems(List<String> items) {
        presetItems.clear();
        presetItems.addAll(normalizeItems(items));
        if (presetItems.isEmpty()) {
            applyDefaultPresets();
        }
        saveSettings();
    }

    public synchronized void resetToDefault() {
        applyDefaultPresets();
        saveSettings();
    }

    public synchronized void reloadSettings() {
        loadSettings();
        File file = new File(Environment.getExternalStorageDirectory(), SETTINGS_DIR + "/" + SETTINGS_FILE);
        if (!file.exists()) {
            saveSettings();
        }
    }

    public synchronized String getPresetItemsText() {
        List<String> items = getPresetItems();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                builder.append("\n");
            }
            builder.append(items.get(i));
        }
        return builder.toString();
    }

    private void loadSettings() {
        File file = new File(Environment.getExternalStorageDirectory(), SETTINGS_DIR + "/" + SETTINGS_FILE);
        if (!file.exists()) {
            applyDefaultPresets();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            if (sb.length() == 0) {
                applyDefaultPresets();
                return;
            }

            JSONObject json = new JSONObject(sb.toString());
            JSONArray array = json.optJSONArray(KEY_PRESETS);
            List<String> loaded = new ArrayList<>();
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    String item = array.optString(i, "").trim();
                    if (!item.isEmpty()) {
                        loaded.add(item);
                    }
                }
            }
            presetItems.clear();
            presetItems.addAll(normalizeItems(loaded));
            if (presetItems.isEmpty()) {
                applyDefaultPresets();
            }
        } catch (IOException | JSONException e) {
            if (e.getMessage() != null && (e.getMessage().contains("EACCES") || e.getMessage().contains("Permission denied"))) {
                Log.w(TAG, "Permissions missing - unable to load hospital item presets, using defaults.");
            } else {
                Log.e(TAG, "Failed to load hospital item presets", e);
            }
            applyDefaultPresets();
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
            JSONArray array = new JSONArray();
            for (String item : presetItems) {
                array.put(item);
            }
            json.put(KEY_PRESETS, array);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toString());
            }
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Failed to save hospital item presets", e);
        }
    }

    private void applyDefaultPresets() {
        presetItems.clear();
        for (String item : DEFAULT_PRESETS) {
            presetItems.add(item);
        }
    }

    private List<String> normalizeItems(List<String> items) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String item : items) {
            if (item == null) {
                continue;
            }
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return new ArrayList<>(normalized);
    }
}
