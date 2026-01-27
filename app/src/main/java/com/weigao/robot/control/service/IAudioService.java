package com.weigao.robot.control.service;

import com.weigao.robot.control.model.AudioConfig;
import com.weigao.robot.control.callback.IResultCallback;

/**
 * 音频服务接口
 * <p>
 * 满足需求书第10章"声音设置"要求，提供音量控制和音乐配置功能。
 * </p>
 */
public interface IAudioService {

    // ==================== 音乐设置 ====================

    /**
     * 设置配送背景音乐
     *
     * @param musicPath 音乐文件路径
     * @param callback  结果回调
     */
    void setDeliveryMusic(String musicPath, IResultCallback<Void> callback);

    /**
     * 设置循环配送背景音乐
     *
     * @param musicPath 音乐文件路径
     * @param callback  结果回调
     */
    void setLoopMusic(String musicPath, IResultCallback<Void> callback);

    /**
     * 设置背景音乐开关
     * 
     * @param enabled 是否启用
     */
    void setBackgroundMusicEnabled(boolean enabled, IResultCallback<Void> callback);

    /**
     * 设置语音播报开关
     * 
     * @param enabled 是否启用
     */
    void setVoiceAnnouncementEnabled(boolean enabled, IResultCallback<Void> callback);

    // ==================== 音量设置 ====================

    /**
     * 设置语音音量
     *
     * @param volume   音量值（0-100）
     * @param callback 结果回调
     */
    void setVoiceVolume(int volume, IResultCallback<Void> callback);

    /**
     * 设置配送背景音乐音量
     *
     * @param volume   音量值（0-100）
     * @param callback 结果回调
     */
    void setDeliveryVolume(int volume, IResultCallback<Void> callback);

    /**
     * 获取语音音量
     *
     * @param callback 结果回调
     */
    void getVoiceVolume(IResultCallback<Integer> callback);

    /**
     * 获取配送背景音乐音量
     *
     * @param callback 结果回调
     */
    void getDeliveryVolume(IResultCallback<Integer> callback);

    // ==================== 播报频率设置 ====================

    /**
     * 设置取物语音播报频率（标准配送）
     *
     * @param frequency 播报频率
     * @param callback  结果回调
     */
    void setAnnouncementFrequency(int frequency, IResultCallback<Void> callback);

    /**
     * 设置循环配送语音播报频率
     *
     * @param frequency 播报频率
     * @param callback  结果回调
     */
    void setLoopAnnouncementFrequency(int frequency, IResultCallback<Void> callback);

    /**
     * 获取取物语音播报频率（标准配送）
     *
     * @param callback 结果回调
     */
    void getAnnouncementFrequency(IResultCallback<Integer> callback);

    /**
     * 获取循环配送语音播报频率
     *
     * @param callback 结果回调
     */
    void getLoopAnnouncementFrequency(IResultCallback<Integer> callback);

    // ==================== 配置管理 ====================

    /**
     * 获取完整音频配置
     *
     * @param callback 结果回调
     */
    void getAudioConfig(IResultCallback<AudioConfig> callback);

    /**
     * 更新音频配置
     *
     * @param config   音频配置
     * @param callback 结果回调
     */
    void updateAudioConfig(AudioConfig config, IResultCallback<Void> callback);

    // ==================== 播放控制 ====================

    /**
     * 播放指定路径的背景音乐
     *
     * @param musicPath 音乐文件路径（本地路径或Asset路径）
     * @param loop      是否循环播放
     * @param callback  结果回调
     */
    void playBackgroundMusic(String musicPath, boolean loop, IResultCallback<Void> callback);

    /**
     * 停止播放背景音乐
     *
     * @param callback 结果回调
     */
    void stopBackgroundMusic(IResultCallback<Void> callback);

    /**
     * 暂停播放背景音乐
     *
     * @param callback 结果回调
     */
    void pauseBackgroundMusic(IResultCallback<Void> callback);

    /**
     * 恢复播放背景音乐
     *
     * @param callback 结果回调
     */
    void resumeBackgroundMusic(IResultCallback<Void> callback);

    // ==================== 语音播报 (TTS) ====================

    /**
     * 执行语音播报
     *
     * @param text     播报文本内容
     * @param callback 结果回调
     */
    void speak(String text, IResultCallback<Void> callback);

    /**
     * 停止当前语音播报
     *
     * @param callback 结果回调
     */
    void stopSpeak(IResultCallback<Void> callback);

    /**
     * 查询是否正在播报
     *
     * @param callback 结果回调
     */
    void isSpeaking(IResultCallback<Boolean> callback);

    /**
     * 释放资源
     */
    void release();
}
