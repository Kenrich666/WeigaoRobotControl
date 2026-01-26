package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.ISecurityService;
// 假设您有这样一个接口，如果还没定义，请使用 ServiceManager 方式并做好判空
import com.weigao.robot.control.service.ServiceManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * 安全锁定服务实现类
 * <p>
 * 实现配送任务的安全锁定功能，包括密码验证和舱门解锁控制。
 * </p>
 */
public class SecurityServiceImpl implements ISecurityService {

    private static final String TAG = "SecurityServiceImpl";
    private static final String PREFS_NAME = "security_prefs";
    
    // File Persistence
    private static final String CONFIG_DIR = "WeigaoRobot/config";
    private static final String CONFIG_FILE = "security_config.json";

    private static final String DEFAULT_PASSWORD = "123456";

    private final Context context;

    // In-memory state
    private boolean securityLockEnabled = true;
    private boolean isLocked = false;
    private String currentPassword = DEFAULT_PASSWORD;
    private boolean isPasswordSet = false;

    public SecurityServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        
        // Load config from file (external storage)
        // If file doesn't exist, it will attempt to migrate from legacy SharedPreferences
        loadConfig();
        
        Log.d(TAG, "SecurityServiceImpl 已创建");
    }

    private void loadConfig() {
        File dir = new File(android.os.Environment.getExternalStorageDirectory(), CONFIG_DIR);
        File file = new File(dir, CONFIG_FILE);

        if (file.exists()) {
            // Case 1: File exists, load normally
            try {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                JSONObject obj = new JSONObject(sb.toString());
                this.securityLockEnabled = obj.optBoolean("enabled", true);
                this.isLocked = obj.optBoolean("locked", false);
                this.currentPassword = obj.optString("password", DEFAULT_PASSWORD);
                this.isPasswordSet = obj.optBoolean("passwordSet", false);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load security config from file", e);
            }
        } else {
            // Case 2: File missing, try migration from legacy SharedPreferences
            migrateFromLegacyPrefs();
            // Create the file immediately
            saveConfig();
        }
    }

    /**
     * One-time migration: Read old SP data if file doesn't exist
     */
    private void migrateFromLegacyPrefs() {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.securityLockEnabled = sp.getBoolean("security_enabled", true);
        this.isLocked = sp.getBoolean("security_is_locked", false);
        this.currentPassword = sp.getString("security_password", DEFAULT_PASSWORD);
        this.isPasswordSet = sp.getBoolean("security_password_set", false);
        Log.i(TAG, "Migrated legacy settings to file storage");
    }

    private void saveConfig() {
        File dir = new File(android.os.Environment.getExternalStorageDirectory(), CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, CONFIG_FILE);

        try {
            JSONObject obj = new JSONObject();
            obj.put("enabled", securityLockEnabled);
            obj.put("locked", isLocked);
            obj.put("password", currentPassword);
            obj.put("passwordSet", isPasswordSet);
            
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(obj.toString());
            }
            // No redundant write to SharedPreferences
        } catch (Exception e) {
            Log.e(TAG, "Failed to save security config", e);
        }
    }

    @Override
    public void setSecurityLockEnabled(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "setSecurityLockEnabled: " + enabled);
        this.securityLockEnabled = enabled;
        // 如果禁用了安全锁，是否应该自动解锁？视业务而定
        if (!enabled && isLocked) {
            this.isLocked = false;
        }
        saveConfig();
        notifySuccess(callback);
    }

    @Override
    public void isSecurityLockEnabled(IResultCallback<Boolean> callback) {
        if (callback != null) {
            callback.onSuccess(securityLockEnabled);
        }
    }

    @Override
    public void isLocked(IResultCallback<Boolean> callback) {
        if (callback != null) {
            callback.onSuccess(isLocked);
        }
    }

    @Override
    public void verifyPassword(String password, IResultCallback<Boolean> callback) {
        Log.d(TAG, "verifyPassword");
        // Verify against in-memory state (whch is synced with file)
        boolean isValid = currentPassword.equals(password);
        if (callback != null) {
            callback.onSuccess(isValid);
        }
    }

    @Override
    public void setPassword(String oldPassword, String newPassword, IResultCallback<Void> callback) {
        Log.d(TAG, "setPassword");
        
        // Check old password
        // If not set yet, allow optional old password or specific logic
        boolean isCorrect = isPasswordSet ? currentPassword.equals(oldPassword) : true;
        // Or if user provided oldPassword match, it's fine too. 
        // Logic from before: if (isFirstTime || savedPassword.equals(oldPassword))
        
        if (!isPasswordSet || currentPassword.equals(oldPassword)) {
            this.currentPassword = newPassword;
            this.isPasswordSet = true;
            saveConfig();
            notifySuccess(callback);
        } else {
            notifyError(callback, -1, "旧密码不正确");
        }
    }

    // 封装状态保存逻辑
    private void setLockedState(boolean locked) {
        this.isLocked = locked;
        saveConfig();
    }

    @Override
    public void unlockDoor(int doorId, String password, IResultCallback<Void> callback) {
        Log.d(TAG, "unlockDoor: doorId=" + doorId);
        if (currentPassword.equals(password)) {
            setLockedState(false);
            // 调用 DoorService 打开指定舱门
            try {
                IDoorService doorService = ServiceManager.getInstance().getDoorService();

                if (doorService != null) {
                    doorService.openDoor(doorId, false, new IResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            notifySuccess(callback);
                        }

                        @Override
                        public void onError(ApiError error) {
                            // 虽然开门失败，但密码正确，逻辑上“已解锁”是合理的
                            notifyError(callback, error.getCode(), error.getMessage());
                        }
                    });
                } else {
                    notifyError(callback, -1, "舱门服务未初始化");
                }
            } catch (Exception e) {
                Log.e(TAG, "打开舱门异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        } else {
            notifyError(callback, -1, "密码不正确");
        }
    }

    @Override
    public void lock(IResultCallback<Void> callback) {
        Log.d(TAG, "lock");
        setLockedState(true); // [修复] 使用带持久化的方法
        notifySuccess(callback);
    }

    @Override
    public void unlock(String password, IResultCallback<Void> callback) {
        Log.d(TAG, "unlock");
        if (currentPassword.equals(password)) {
            setLockedState(false); // [修复] 使用带持久化的方法
            notifySuccess(callback);
        } else {
            notifyError(callback, -1, "密码不正确");
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "释放 SecurityService 资源");
    }

    // ==================== 辅助方法 ====================

    private void notifySuccess(IResultCallback<Void> callback) {
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    private void notifyError(IResultCallback<?> callback, int code, String message) {
        if (callback != null) {
            callback.onError(new com.weigao.robot.control.callback.ApiError(code, message));
        }
    }
}
