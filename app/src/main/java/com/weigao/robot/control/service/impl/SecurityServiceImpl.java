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

/**
 * 安全锁定服务实现类
 * <p>
 * 实现配送任务的安全锁定功能，包括密码验证和舱门解锁控制。
 * </p>
 */
public class SecurityServiceImpl implements ISecurityService {

    private static final String TAG = "SecurityServiceImpl";
    private static final String PREFS_NAME = "security_prefs";
    private static final String KEY_PASSWORD = "security_password";
    private static final String KEY_ENABLED = "security_enabled";
    private static final String KEY_LOCKED_STATE = "security_is_locked"; // 新增
    private static final String DEFAULT_PASSWORD = "123456";

    private final Context context;
    private final SharedPreferences prefs;

    /**
     * 安全锁定功能是否启用
     */
    private boolean securityLockEnabled;

    /**
     * 当前是否处于锁定状态
     */
    private boolean isLocked = false;

    public SecurityServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        this.securityLockEnabled = prefs.getBoolean(KEY_ENABLED, true);
        // [修复] 从 SP 读取锁定状态，防止重启后失效
        this.isLocked = prefs.getBoolean(KEY_LOCKED_STATE, false);

        Log.d(TAG, "SecurityServiceImpl 已创建");
    }

    @Override
    public void setSecurityLockEnabled(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "setSecurityLockEnabled: " + enabled);
        this.securityLockEnabled = enabled;
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
        // 如果禁用了安全锁，是否应该自动解锁？视业务而定
        if (!enabled && isLocked) {
            setLockedState(false);
        }
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
        String savedPassword = prefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD);
        boolean isValid = savedPassword.equals(password);
        if (callback != null) {
            callback.onSuccess(isValid);
        }
    }

    @Override
    public void setPassword(String oldPassword, String newPassword, IResultCallback<Void> callback) {
        Log.d(TAG, "setPassword");
        String savedPassword = prefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD);

        // 首次设置密码时 oldPassword 可为空
        if (savedPassword.equals(DEFAULT_PASSWORD) || savedPassword.equals(oldPassword)) {
            prefs.edit().putString(KEY_PASSWORD, newPassword).apply();
            notifySuccess(callback);
        } else {
            notifyError(callback, -1, "旧密码不正确");
        }
    }

    // 封装状态保存逻辑
    private void setLockedState(boolean locked) {
        this.isLocked = locked;
        prefs.edit().putBoolean(KEY_LOCKED_STATE, locked).apply();
    }

    @Override
    public void unlockDoor(int doorId, String password, IResultCallback<Void> callback) {
        Log.d(TAG, "unlockDoor: doorId=" + doorId);
        String savedPassword = prefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD);

        if (savedPassword.equals(password)) {
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
        String savedPassword = prefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD);

        if (savedPassword.equals(password)) {
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
