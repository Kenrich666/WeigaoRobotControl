package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.Log;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.AudioConfig;
import com.weigao.robot.control.service.IAudioService;
import com.google.gson.Gson;
// Actually context.getSharedPreferences is better for simple KV.

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * 音频服务实现类 (基于 Android 原生 API)
 * <p>
 * 集成 MediaPlayer 实现背景音乐播放。
 * 集成 TextToSpeech 实现语音播报。
 * 支持配置持久化。
 * </p>
 */
public class AudioServiceImpl implements IAudioService {

    private static final String TAG = "AudioServiceImpl";
    private static final String PREFS_NAME = "audio_settings";
    private static final String KEY_AUDIO_CONFIG = "audio_config_json";

    private final Context context;
    private final AudioManager audioManager;
    private AudioConfig audioConfig;
    private final SharedPreferences prefs;
    private final Gson gson;

    // Components
    private MediaPlayer mediaPlayer;
    private TextToSpeech mTTS;
    private boolean isTtsReady = false;

    public AudioServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();

        // 1. 初始化配置 (从本地读取)
        loadConfigFromPrefs();

        // 2. 初始化 TTS
        initTTS();

        // 3. 应用音量
        applySystemVolume();

        Log.d(TAG, "AudioServiceImpl (Android Native + Gson) created");
    }

    private void loadConfigFromPrefs() {
        String json = prefs.getString(KEY_AUDIO_CONFIG, "");
        if (!TextUtils.isEmpty(json)) {
            try {
                audioConfig = gson.fromJson(json, AudioConfig.class);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse AudioConfig json", e);
                // Fallback to default
                audioConfig = new AudioConfig();
                initDefaultConfig();
            }
        } else {
            // First time logic
            audioConfig = new AudioConfig();
            initDefaultConfig();
        }
    }

    private void saveConfigToPrefs() {
        try {
            String json = gson.toJson(audioConfig);
            prefs.edit().putString(KEY_AUDIO_CONFIG, json).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save AudioConfig json", e);
        }
    }

    private void initDefaultConfig() {
        if (audioManager != null) {
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int volPercent = (int) ((currentVolume / (float) maxVolume) * 100);

            audioConfig.setVoiceVolume(volPercent);
            audioConfig.setDeliveryVolume(volPercent);
        } else {
            audioConfig.setVoiceVolume(50);
            audioConfig.setDeliveryVolume(50);
        }
        audioConfig.setAnnouncementFrequency(30);
        audioConfig.setLoopAnnouncementFrequency(30);
    }

    private void initTTS() {
        try {
            mTTS = new TextToSpeech(context, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    if (mTTS == null)
                        return;
                    int result = mTTS.setLanguage(Locale.CHINESE);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "TTS Language not supported");
                        // 尝试使用美式英语作为备用？或者提示用户下载
                        // mTTS.setLanguage(Locale.US);
                    } else {
                        isTtsReady = true;
                        Log.d(TAG, "TTS Initialized successfully");
                    }

                    mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            Log.d(TAG, "TTS Start: " + utteranceId);
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            Log.d(TAG, "TTS Done: " + utteranceId);
                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.e(TAG, "TTS Error: " + utteranceId);
                        }
                    });
                } else {
                    Log.e(TAG, "TTS Init failed with status: " + status);
                    // Init failed, maybe temporary.
                    // Retry once after a delay if first time failure?
                    // For now just log.
                    isTtsReady = false;
                    // 可选：尝试重试逻辑
                    if (status == -1) { // -1 is often bind failure
                        // Maybe try again in 2 seconds?
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            Log.d(TAG, "Retrying TTS init...");
                            // Prevent infinite recursion loops if it keeps failing by not calling
                            // recursively directly effectively
                            // or just let user trigger action again.
                            // Simple retry:
                            // initTTS(); // Risk of loop
                        }, 2000);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "TTS Constructor failed", e);
        }
    }

    private void applySystemVolume() {
        if (audioManager != null) {
            // We mainly use STREAM_MUSIC. Both Voice and Music settings will affect this
            // Logic: take the larger of the two as the system master volume?
            // Or just use Music Volume for stream music.
            // Simplified: Use Music Volume (DeliveryVolume) for STREAM_MUSIC
            int vol = audioConfig.getDeliveryVolume();
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int index = (int) (max * (vol / 100.0f));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
        }
    }

    // ==================== 音乐设置 ====================

    @Override
    public void setDeliveryMusic(String musicPath, IResultCallback<Void> callback) {
        audioConfig.setDeliveryMusicPath(musicPath);
        saveConfigToPrefs();
        notifySuccess(callback);
    }

    @Override
    public void setLoopMusic(String musicPath, IResultCallback<Void> callback) {
        audioConfig.setLoopMusicPath(musicPath);
        saveConfigToPrefs();
        notifySuccess(callback);
    }

    @Override
    public void setBackgroundMusicEnabled(boolean enabled, IResultCallback<Void> callback) {
        audioConfig.setBackgroundMusicEnabled(enabled);
        saveConfigToPrefs();

        if (!enabled) {
            stopBackgroundMusic(null);
        }
        notifySuccess(callback);
    }

    @Override
    public void setVoiceAnnouncementEnabled(boolean enabled, IResultCallback<Void> callback) {
        audioConfig.setVoiceAnnouncementEnabled(enabled);
        saveConfigToPrefs();

        if (!enabled) {
            stopSpeak(null);
        }
        notifySuccess(callback);
    }

    // ==================== 播放控制 ====================

    @Override
    public void playBackgroundMusic(String musicPath, boolean loop, IResultCallback<Void> callback) {
        if (!audioConfig.isBackgroundMusicEnabled()) {
            Log.d(TAG, "背景音乐开关已关闭，忽略播放请求");
            // Opt: notify success or specific error? Success implies "request handled"
            notifySuccess(callback);
            return;
        }

        Log.d(TAG, "playBackgroundMusic: " + musicPath);
        if (TextUtils.isEmpty(musicPath)) {
            notifyError(callback, -1, "音乐路径为空");
            return;
        }

        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();

            // Handle raw assets or file path
            // For simplicity, assuming absolute file path first
            File file = new File(musicPath);
            Log.d(TAG, "Preparing to play: " + musicPath + ", Exists: " + file.exists() + ", Size: " + file.length());

            if (file.exists()) {
                mediaPlayer.setDataSource(musicPath); // This might throw
            } else {
                Log.e(TAG, "Music file not found: " + musicPath);
                notifyError(callback, -2, "音乐文件不存在: " + musicPath);
                return;
            }

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setLooping(loop);

            int deliveryVol = audioConfig.getDeliveryVolume();
            float vol = deliveryVol / 100.0f;
            Log.d(TAG, "Setting MediaPlayer volume: " + vol + " (Config: " + deliveryVol + ")");
            mediaPlayer.setVolume(vol, vol);

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer Error: what=" + what + ", extra=" + extra);
                return false;
            });

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer prepared, starting...");
                mp.start();
                if (mp.isPlaying()) {
                    Log.d(TAG, "MediaPlayer started successfully");
                } else {
                    Log.e(TAG, "MediaPlayer.start() called but isPlaying() is false");
                }
            });

            Log.d(TAG, "Calling prepareAsync...");
            mediaPlayer.prepareAsync(); // Use Async to avoid UI block and catch errors in listener
            // Note: callback success is returned immediately, but actual playback starts
            // later.
            // Ideally we should wait for prepare, but existing interface is
            // synchronous-like result.
            notifySuccess(callback);

        } catch (IOException e) {
            Log.e(TAG, "播放音乐IO异常", e);
            notifyError(callback, -3, "IO异常: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "播放音乐异常", e);
            notifyError(callback, -4, e.getMessage());
        }
    }

    @Override
    public void stopBackgroundMusic(IResultCallback<Void> callback) {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset(); // or release? better verify lifecycle.
                // We keep instance or release? let's keep it null for clean state.
                mediaPlayer.release();
                mediaPlayer = null;
            }
            notifySuccess(callback);
        } catch (Exception e) {
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void pauseBackgroundMusic(IResultCallback<Void> callback) {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
            notifySuccess(callback);
        } catch (Exception e) {
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void resumeBackgroundMusic(IResultCallback<Void> callback) {
        try {
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
            notifySuccess(callback);
        } catch (Exception e) {
            notifyError(callback, -1, e.getMessage());
        }
    }

    // ==================== 音量设置 ====================

    @Override
    public void setVoiceVolume(int volume, IResultCallback<Void> callback) {
        int val = clampVolume(volume);
        audioConfig.setVoiceVolume(val);
        saveConfigToPrefs();
        // 如果需要专门控制 TTS 音量， TextToSpeech 在 Android O+ 支持 setVolume(bundle) 但比较复杂
        // 通常 TTS 跟随系统 STREAM_MUSIC。
        // 这里我们不做 System Volume 的变更，因为 Voice Volume 往往指 TTS 的 gain
        // 但为了简单，可以在 speak 时传入 params
        notifySuccess(callback);
    }

    @Override
    public void setDeliveryVolume(int volume, IResultCallback<Void> callback) {
        int val = clampVolume(volume);
        audioConfig.setDeliveryVolume(val);
        saveConfigToPrefs();
        applySystemVolume(); // Apply to system master volume

        // Update running player if exists
        if (mediaPlayer != null) {
            float v = val / 100.0f;
            mediaPlayer.setVolume(v, v);
        }
        notifySuccess(callback);
    }

    @Override
    public void getVoiceVolume(IResultCallback<Integer> callback) {
        if (callback != null)
            callback.onSuccess(audioConfig.getVoiceVolume());
    }

    @Override
    public void getDeliveryVolume(IResultCallback<Integer> callback) {
        if (callback != null)
            callback.onSuccess(audioConfig.getDeliveryVolume());
    }

    // ==================== 语音播报 ====================

    @Override
    public void speak(String text, IResultCallback<Void> callback) {
        if (!audioConfig.isVoiceAnnouncementEnabled()) {
            Log.d(TAG, "语音播报开关已关闭，忽略播报请求");
            notifySuccess(callback);
            return;
        }

        if (!isTtsReady || mTTS == null) {
            // Attempt re-init or fail?
            notifyError(callback, -1, "TTS服务未就绪");
            return;
        }

        try {
            // Use KEY_PARAM_VOLUME if we want to differentiate from system volume
            // value is 0.0 to 1.0
            float ttsVol = audioConfig.getVoiceVolume() / 100.0f;
            android.os.Bundle params = new android.os.Bundle();
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, ttsVol);

            // QUEUE_ADD allows daisy chaining, QUEUE_FLUSH interrupts
            // Default to FLUSH for immediate response
            int code = mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, params, "ID_" + System.currentTimeMillis());

            if (code == TextToSpeech.SUCCESS) {
                notifySuccess(callback);
            } else {
                notifyError(callback, -2, "TTS Error Code: " + code);
            }
        } catch (Exception e) {
            notifyError(callback, -3, e.getMessage());
        }
    }

    @Override
    public void stopSpeak(IResultCallback<Void> callback) {
        if (mTTS != null) {
            mTTS.stop();
        }
        notifySuccess(callback);
    }

    @Override
    public void isSpeaking(IResultCallback<Boolean> callback) {
        boolean speaking = (mTTS != null && mTTS.isSpeaking());
        if (callback != null)
            callback.onSuccess(speaking);
    }

    // ==================== 播报频率设置 ====================

    @Override
    public void setAnnouncementFrequency(int frequency, IResultCallback<Void> callback) {
        audioConfig.setAnnouncementFrequency(frequency);
        saveConfigToPrefs();
        notifySuccess(callback);
    }

    @Override
    public void setLoopAnnouncementFrequency(int frequency, IResultCallback<Void> callback) {
        audioConfig.setLoopAnnouncementFrequency(frequency);
        saveConfigToPrefs();
        notifySuccess(callback);
    }

    @Override
    public void getAnnouncementFrequency(IResultCallback<Integer> callback) {
        if (callback != null)
            callback.onSuccess(audioConfig.getAnnouncementFrequency());
    }

    @Override
    public void getLoopAnnouncementFrequency(IResultCallback<Integer> callback) {
        if (callback != null)
            callback.onSuccess(audioConfig.getLoopAnnouncementFrequency());
    }

    // ==================== 配置管理 ====================

    @Override
    public void getAudioConfig(IResultCallback<AudioConfig> callback) {
        if (callback != null)
            callback.onSuccess(audioConfig);
    }

    @Override
    public void updateAudioConfig(AudioConfig config, IResultCallback<Void> callback) {
        if (config != null) {
            this.audioConfig = config;
            saveConfigToPrefs();
            // Apply volumes immediately
            setDeliveryVolume(config.getDeliveryVolume(), null);
            notifySuccess(callback);
        } else {
            notifyError(callback, -1, "Config is null");
        }
    }

    @Override
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
            mTTS = null;
        }
        Log.d(TAG, "AudioService Released");
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
            callback.onError(new ApiError(code, message));
        }
    }
}
