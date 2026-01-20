package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.util.Log;

import com.keenon.peanut.api.PeanutSDK;
import com.keenon.peanut.api.component.AudioComponent;
import com.keenon.peanut.api.callback.OnAudioListener;

import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.AudioConfig;
import com.weigao.robot.control.service.IAudioService;

/**
 * 音频服务实现类
 * <p>
 * 封装 Peanut SDK 的 {@code AudioComponent}，提供音量控制和音乐配置功能。
 * </p>
 */
public class AudioServiceImpl implements IAudioService {

    private static final String TAG = "AudioServiceImpl";

    private final Context context;

    /** 当前音频配置 */
    private AudioConfig audioConfig;

    /** Peanut SDK 音频组件 */
    private AudioComponent audioComponent;

    /** 音频组件是否已初始化 */
    private boolean audioInitialized = false;

    public AudioServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        this.audioConfig = new AudioConfig();
        Log.d(TAG, "AudioServiceImpl 已创建");
        initAudioComponent();
    }

    /**
     * 初始化音频组件
     */
    private void initAudioComponent() {
        try {
            audioComponent = PeanutSDK.getInstance().audio();
            if (audioComponent != null) {
                audioComponent.initAudio(new OnAudioListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "AudioComponent 初始化成功");
                        audioInitialized = true;
                    }

                    @Override
                    public void onError(int errorCode) {
                        Log.e(TAG, "AudioComponent 初始化失败: " + errorCode);
                        audioInitialized = false;
                    }

                    @Override
                    public void onAudioData(byte[] bytes, int len) {
                        // 音频数据回调
                    }

                    @Override
                    public void onHeartbeat(int state) {
                        // 心跳回调
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "AudioComponent 初始化异常", e);
        }
    }

    // ==================== 音乐设置 ====================

    @Override
    public void setDeliveryMusic(String musicPath, IResultCallback<Void> callback) {
        Log.d(TAG, "setDeliveryMusic: " + musicPath);
        audioConfig.setDeliveryMusicPath(musicPath);
        notifySuccess(callback);
    }

    @Override
    public void setLoopMusic(String musicPath, IResultCallback<Void> callback) {
        Log.d(TAG, "setLoopMusic: " + musicPath);
        audioConfig.setLoopMusicPath(musicPath);
        notifySuccess(callback);
    }

    // ==================== 音量设置 ====================

    @Override
    public void setVoiceVolume(int volume, IResultCallback<Void> callback) {
        Log.d(TAG, "setVoiceVolume: " + volume);
        audioConfig.setVoiceVolume(clampVolume(volume));
        notifySuccess(callback);
    }

    @Override
    public void setDeliveryVolume(int volume, IResultCallback<Void> callback) {
        Log.d(TAG, "setDeliveryVolume: " + volume);
        audioConfig.setDeliveryVolume(clampVolume(volume));
        notifySuccess(callback);
    }

    @Override
    public void getVoiceVolume(IResultCallback<Integer> callback) {
        if (callback != null) {
            callback.onSuccess(audioConfig.getVoiceVolume());
        }
    }

    @Override
    public void getDeliveryVolume(IResultCallback<Integer> callback) {
        if (callback != null) {
            callback.onSuccess(audioConfig.getDeliveryVolume());
        }
    }

    // ==================== 播报频率设置 ====================

    @Override
    public void setAnnouncementFrequency(int frequency, IResultCallback<Void> callback) {
        Log.d(TAG, "setAnnouncementFrequency: " + frequency);
        audioConfig.setAnnouncementFrequency(frequency);
        notifySuccess(callback);
    }

    @Override
    public void setLoopAnnouncementFrequency(int frequency, IResultCallback<Void> callback) {
        Log.d(TAG, "setLoopAnnouncementFrequency: " + frequency);
        audioConfig.setLoopAnnouncementFrequency(frequency);
        notifySuccess(callback);
    }

    @Override
    public void getAnnouncementFrequency(IResultCallback<Integer> callback) {
        if (callback != null) {
            callback.onSuccess(audioConfig.getAnnouncementFrequency());
        }
    }

    @Override
    public void getLoopAnnouncementFrequency(IResultCallback<Integer> callback) {
        if (callback != null) {
            callback.onSuccess(audioConfig.getLoopAnnouncementFrequency());
        }
    }

    // ==================== 配置管理 ====================

    @Override
    public void getAudioConfig(IResultCallback<AudioConfig> callback) {
        if (callback != null) {
            callback.onSuccess(audioConfig);
        }
    }

    @Override
    public void updateAudioConfig(AudioConfig config, IResultCallback<Void> callback) {
        Log.d(TAG, "updateAudioConfig");
        if (config != null) {
            this.audioConfig = config;
            notifySuccess(callback);
        } else {
            notifyError(callback, -1, "配置不能为空");
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "释放 AudioService 资源");
        if (audioComponent != null && audioInitialized) {
            try {
                audioComponent.audioStop();
                Log.d(TAG, "AudioComponent 已停止");
            } catch (Exception e) {
                Log.e(TAG, "释放 AudioComponent 异常", e);
            }
        }
        audioInitialized = false;
    }

    // ==================== 辅助方法 ====================

    private int clampVolume(int volume) {
        return Math.max(0, Math.min(100, volume));
    }

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
