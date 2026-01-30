package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.AudioConfig;
import com.weigao.robot.control.service.IAudioService;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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
    private static final String CONFIG_DIR = "WeigaoRobot/config";
    private static final String AUDIO_DIR = "WeigaoRobot/audio";

    private final Context context;
    private final AudioManager audioManager;
    private AudioConfig audioConfig;

    // Components
    private MediaPlayer mediaPlayer; // Background Music
    private MediaPlayer voiceMediaPlayer; // Voice Announcement
    private String currentMusicPath; // Track currently playing music

    public AudioServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // 0. 部署默认资源 (如果不存在)
        deployDefaultAssets();

        // 1. 初始化配置 (从 Manager 获取)
        this.audioConfig = com.weigao.robot.control.manager.SoundSettingsManager.getInstance().getAudioConfig();
        
        // 检查配置是否初始化
        if (!audioConfig.isInitialized()) {
             initDefaultConfig();
             com.weigao.robot.control.manager.SoundSettingsManager.getInstance().saveAudioConfig(audioConfig);
        }
        
        Log.d(TAG, "AudioServiceImpl (No TTS, Audio Files) created");
    }

    private void deployDefaultAssets() {
        // 尝试从 raw 资源复制到 app 目录
        copyRawResourceToFile(com.weigao.robot.control.R.raw.default_music, "default_music.mp3");
        copyRawResourceToFile(com.weigao.robot.control.R.raw.default_navigating, "default_navigating.mp3");
        copyRawResourceToFile(com.weigao.robot.control.R.raw.default_arrival, "default_arrival.mp3");
    }

    private void copyRawResourceToFile(int resId, String filename) {
        try {
            File dest = new File(android.os.Environment.getExternalStorageDirectory(), AUDIO_DIR + "/" + filename);
            if (!dest.exists()) {
                // Ensure dir exists
                File dir = dest.getParentFile();
                if (dir != null && !dir.exists()) dir.mkdirs();
                
                Log.d(TAG, "Copying default asset " + filename + " to " + dest.getAbsolutePath());
                java.io.InputStream in = context.getResources().openRawResource(resId);
                java.io.FileOutputStream out = new java.io.FileOutputStream(dest);
                byte[] buff = new byte[1024];
                int read = 0;
                while ((read = in.read(buff)) > 0) {
                    out.write(buff, 0, read);
                }
                in.close();
                out.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy default asset: " + filename, e);
        }
    }



    private void initDefaultConfig() {
        // Default volumes
        audioConfig.setVoiceVolume(80);
        audioConfig.setDeliveryVolume(50);
        

        
        // Default switches
        audioConfig.setDeliveryMusicEnabled(true);
        audioConfig.setDeliveryVoiceEnabled(true);
        audioConfig.setLoopMusicEnabled(true);
        audioConfig.setLoopVoiceEnabled(true);
        
        // Default paths if available
        // Default paths if available
        File musicDir = new File(android.os.Environment.getExternalStorageDirectory(), AUDIO_DIR);
        if (!musicDir.exists()) musicDir.mkdirs();
        
        if (musicDir != null) {
             audioConfig.setDeliveryMusicPath(new File(musicDir, "default_music.mp3").getAbsolutePath());
             audioConfig.setLoopMusicPath(new File(musicDir, "default_music.mp3").getAbsolutePath());
             
             audioConfig.setDeliveryNavigatingVoicePath(new File(musicDir, "default_navigating.mp3").getAbsolutePath());
             audioConfig.setDeliveryArrivalVoicePath(new File(musicDir, "default_arrival.mp3").getAbsolutePath());
             
             audioConfig.setLoopNavigatingVoicePath(new File(musicDir, "default_navigating.mp3").getAbsolutePath());
             audioConfig.setLoopArrivalVoicePath(new File(musicDir, "default_arrival.mp3").getAbsolutePath());
        }
        
        // Mark as initialized
        audioConfig.setInitialized(true);
    }

    // ==================== 音乐设置 ====================

    @Override
    public void setDeliveryMusic(String musicPath, IResultCallback<Void> callback) {
        audioConfig.setDeliveryMusicPath(musicPath);
        com.weigao.robot.control.manager.SoundSettingsManager.getInstance().saveAudioConfig(audioConfig);
        notifySuccess(callback);
    }

    @Override
    public void setLoopMusic(String musicPath, IResultCallback<Void> callback) {
        audioConfig.setLoopMusicPath(musicPath);
        com.weigao.robot.control.manager.SoundSettingsManager.getInstance().saveAudioConfig(audioConfig);
        notifySuccess(callback);
    }

    // Global setters removed

    // ==================== 播放控制 ====================

    @Override
    public void playBackgroundMusic(String musicPath, boolean loop, IResultCallback<Void> callback) {
        if (TextUtils.isEmpty(musicPath)) {
            notifyError(callback, -1, "音乐路径为空");
            return;
        }

        // Logic moved to caller (Activity) to avoid ambiguity when paths are identical
        
        // Idempotency check: If same music is already playing, do nothing.
        if (mediaPlayer != null && mediaPlayer.isPlaying() && TextUtils.equals(musicPath, currentMusicPath)) {
            Log.d(TAG, "Music already playing: " + musicPath + ", ignoring request to restart.");
            notifySuccess(callback);
            return;
        }
        
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();

            File file = new File(musicPath);
            Log.d(TAG, "Preparing to play: " + musicPath + ", Exists: " + file.exists() + ", Size: " + file.length());

            if (file.exists()) {
                mediaPlayer.setDataSource(musicPath);
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
            });

            Log.d(TAG, "Calling prepareAsync...");
            mediaPlayer.prepareAsync();

            // Speed logic removed

            notifySuccess(callback);
            
            // Update current track
            currentMusicPath = musicPath;

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
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            currentMusicPath = null;
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
        com.weigao.robot.control.manager.SoundSettingsManager.getInstance().saveAudioConfig(audioConfig);
        
        if (voiceMediaPlayer != null) {
            float v = val / 100.0f;
            voiceMediaPlayer.setVolume(v, v);
        }
        notifySuccess(callback);
    }

    @Override
    public void setDeliveryVolume(int volume, IResultCallback<Void> callback) {
        int val = clampVolume(volume);
        audioConfig.setDeliveryVolume(val);
        com.weigao.robot.control.manager.SoundSettingsManager.getInstance().saveAudioConfig(audioConfig);

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

    private long lastVoiceEndTime = 0;
    private android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pendingVoiceRunnable;

    @Override
    public void playVoice(String voicePath, IResultCallback<Void> callback) {
        playVoice(voicePath, callback, null);
    }

    @Override
    public void playVoice(String voicePath, IResultCallback<Void> callback, Runnable onComplete) {
        // Interval Logic for Navigation Voices
        // Note: Switches are checked by caller.

        // Cancel previous pending play request
        if (pendingVoiceRunnable != null) {
            handler.removeCallbacks(pendingVoiceRunnable);
            pendingVoiceRunnable = null;
        }

        // Check if this is a Navigation Voice
        boolean isNavVoice = isNavigationVoice(voicePath);
        
        // Only apply interval logic for Navigation Voices
        if (isNavVoice) {
            long now = System.currentTimeMillis();
            // Fixed interval: 3 seconds
            long intervalMs = 3000L;
            long diff = now - lastVoiceEndTime;

            if (diff < intervalMs) {
                long delay = intervalMs - diff;
                Log.d(TAG, "Nav Voice interval enforced. Delaying: " + delay + "ms");

                pendingVoiceRunnable = () -> playVoiceInternal(voicePath, callback, onComplete);
                handler.postDelayed(pendingVoiceRunnable, delay);
                return;
            }
        }
        
        playVoiceInternal(voicePath, callback, onComplete);
    }

    private boolean isNavigationVoice(String path) {
        if (TextUtils.isEmpty(path) || audioConfig == null) return false;
        return TextUtils.equals(path, audioConfig.getDeliveryNavigatingVoicePath()) ||
               TextUtils.equals(path, audioConfig.getLoopNavigatingVoicePath());
    }

    private void playVoiceInternal(String voicePath, IResultCallback<Void> callback) {
        playVoiceInternal(voicePath, callback, null);
    }

    private void playVoiceInternal(String voicePath, IResultCallback<Void> callback, Runnable onComplete) {
        if (TextUtils.isEmpty(voicePath)) {
             notifyError(callback, -1, "语音路径为空");
            return;
        }

        try {
            if (voiceMediaPlayer != null) {
                if (voiceMediaPlayer.isPlaying()) {
                    voiceMediaPlayer.stop();
                }
                voiceMediaPlayer.reset();
            } else {
                voiceMediaPlayer = new MediaPlayer();
            }

            File file = new File(voicePath);
            if (!file.exists()) {
                Log.e(TAG, "Voice file not found: " + voicePath);
                notifyError(callback, -2, "语音文件不存在");
                return;
            }

            voiceMediaPlayer.setDataSource(voicePath);
            voiceMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            voiceMediaPlayer.setLooping(false);

            float vol = audioConfig.getVoiceVolume() / 100.0f;
            voiceMediaPlayer.setVolume(vol, vol);

            voiceMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "VoiceMediaPlayer Error: " + what);
                return true;
            });
            
            voiceMediaPlayer.setOnCompletionListener(mp -> {
                 Log.d(TAG, "Voice playback finished");
                 // Only update timestamp for Navigation voices to enforce interval
                 if (isNavigationVoice(voicePath)) {
                     lastVoiceEndTime = System.currentTimeMillis();
                 }
                 if (onComplete != null) {
                     onComplete.run();
                 }
            });

            voiceMediaPlayer.prepare();
            
            // Speed logic removed
            
            voiceMediaPlayer.start();
            notifySuccess(callback);

        } catch (Exception e) {
            Log.e(TAG, "Play voice failed", e);
            notifyError(callback, -3, e.getMessage());
        }
    }

    @Override
    public void stopVoice(IResultCallback<Void> callback) {
        try {
            // Cancel any pending voice
            if (pendingVoiceRunnable != null) {
                handler.removeCallbacks(pendingVoiceRunnable);
                pendingVoiceRunnable = null;
            }

            if (voiceMediaPlayer != null) {
                if (voiceMediaPlayer.isPlaying()) {
                    voiceMediaPlayer.stop();
                }
                voiceMediaPlayer.reset();
            }
            notifySuccess(callback);
        } catch (Exception e) {
            notifyError(callback, -1, e.getMessage());
        }
    }



    @Override
    public void getAudioConfig(IResultCallback<AudioConfig> callback) {
        if (callback != null)
            callback.onSuccess(audioConfig);
    }

    @Override
    public void updateAudioConfig(AudioConfig config, IResultCallback<Void> callback) {
        if (config != null) {
            this.audioConfig = config;
            com.weigao.robot.control.manager.SoundSettingsManager.getInstance().saveAudioConfig(audioConfig);
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
            currentMusicPath = null;
        }
        if (voiceMediaPlayer != null) {
            try {
                if (voiceMediaPlayer.isPlaying()) voiceMediaPlayer.stop();
                voiceMediaPlayer.release();
            } catch(Exception e) {}
            voiceMediaPlayer = null;
        }
        Log.d(TAG, "AudioService Released");
    }

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
