package com.weigao.robot.control.core.state;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.weigao.robot.control.callback.IStateCallback;
import com.weigao.robot.control.model.RobotState;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 状态管理器实现类
 * <p>
 * 管理机器人状态的存储、更新和分发，支持状态持久化和监听器注册。
 * </p>
 */
public class StateManagerImpl implements IStateManager {

    private static final String TAG = "StateManagerImpl";
    private static final String PREFS_NAME = "robot_state_prefs";
    private static final String KEY_STATE = "robot_state";

    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;

    /** 当前机器人状态 */
    private RobotState currentState;

    /** 状态监听器列表（线程安全） */
    private final List<IStateCallback> stateListeners = new CopyOnWriteArrayList<>();

    public StateManagerImpl(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new GsonBuilder().create();
        this.currentState = new RobotState();
        Log.d(TAG, "StateManagerImpl 已创建");

        // 尝试加载持久化的状态
        loadState();
    }

    @Override
    public RobotState getCurrentState() {
        return currentState;
    }

    @Override
    public void updateState(RobotState state) {
        Log.d(TAG, "updateState");
        if (state != null) {
            this.currentState = state;
            // 通知所有监听器
            notifyStateChanged(state);
        }
    }

    @Override
    public void registerStateListener(IStateCallback listener) {
        if (listener != null && !stateListeners.contains(listener)) {
            stateListeners.add(listener);
            Log.d(TAG, "状态监听器已注册，当前数量：" + stateListeners.size());
        }
    }

    @Override
    public void unregisterStateListener(IStateCallback listener) {
        if (stateListeners.remove(listener)) {
            Log.d(TAG, "状态监听器已注销，当前数量：" + stateListeners.size());
        }
    }

    @Override
    public void clearStateListeners() {
        stateListeners.clear();
        Log.d(TAG, "所有状态监听器已清除");
    }

    @Override
    public void saveState() {
        Log.d(TAG, "saveState");
        try {
            String stateJson = gson.toJson(currentState);
            prefs.edit().putString(KEY_STATE, stateJson).apply();
            Log.d(TAG, "状态已保存");
        } catch (Exception e) {
            Log.e(TAG, "saveState 异常", e);
        }
    }

    @Override
    public void loadState() {
        Log.d(TAG, "loadState");
        try {
            String stateJson = prefs.getString(KEY_STATE, null);
            if (stateJson != null) {
                RobotState loadedState = gson.fromJson(stateJson, RobotState.class);
                if (loadedState != null) {
                    this.currentState = loadedState;
                    Log.d(TAG, "状态已加载");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "loadState 异常", e);
            // 加载失败时使用默认状态
            this.currentState = new RobotState();
        }
    }

    /**
     * 通知所有监听器状态变化
     */
    private void notifyStateChanged(RobotState state) {
        for (IStateCallback listener : stateListeners) {
            try {
                listener.onStateChanged(state);
            } catch (Exception e) {
                Log.e(TAG, "通知状态变化异常", e);
            }
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "释放 StateManager 资源");
        // 保存状态
        saveState();
        // 清除监听器
        clearStateListeners();
    }
}
