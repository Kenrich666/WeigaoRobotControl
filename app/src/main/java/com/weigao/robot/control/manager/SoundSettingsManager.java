package com.weigao.robot.control.manager;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.weigao.robot.control.model.AudioConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Manager for Audio settings persistence.
 * Stores settings in a JSON file on external storage.
 * 
 * 路径: /sdcard/WeigaoRobot/config/audio_config.json
 */
public class SoundSettingsManager {
    private static final String TAG = "SoundSettingsManager";
    private static final String CONFIG_DIR = "WeigaoRobot/config";
    private static final String CONFIG_FILE = "audio_config.json";

    private static SoundSettingsManager instance;
    private AudioConfig audioConfig;
    private final Gson gson;

    private SoundSettingsManager() {
        this.gson = new Gson();
        loadConfig();
    }

    public static synchronized SoundSettingsManager getInstance() {
        if (instance == null) {
            instance = new SoundSettingsManager();
        }
        return instance;
    }

    public AudioConfig getAudioConfig() {
        if (audioConfig == null) {
            audioConfig = new AudioConfig();
        }
        return audioConfig;
    }

    public void saveAudioConfig(AudioConfig config) {
        if (config != null) {
            this.audioConfig = config;
            saveConfig();
        }
    }

    /**
     * Reload settings from file. Useful after permissions are granted.
     * If file does not exist (new install), it will be created with default values (if saved).
     */
    public void reloadSettings() {
        loadConfig();
    }

    private void loadConfig() {
        File dir = new File(Environment.getExternalStorageDirectory(), CONFIG_DIR);
        File file = new File(dir, CONFIG_FILE);

        if (file.exists()) {
            try {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                audioConfig = gson.fromJson(sb.toString(), AudioConfig.class);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load audio config from file", e);
                // Fallback
                audioConfig = new AudioConfig();
            }
        } else {
            // First time or missing
            audioConfig = new AudioConfig();
            // Do not save immediately here; let the caller (Service) initialize defaults first
        }
    }

    private void saveConfig() {
        File dir = new File(Environment.getExternalStorageDirectory(), CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, CONFIG_FILE);

        try {
            String json = gson.toJson(audioConfig);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save audio config", e);
        }
    }
}
