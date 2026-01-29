package com.weigao.robot.control.model;

import java.io.Serializable;

/**
 * 音频配置模型
 * <p>
 * 满足需求书第10章"声音设置"要求。
 * </p>
 */
public class AudioConfig implements Serializable {

    private static final long serialVersionUID = 1L;
    private int voiceVolume;
    private int deliveryVolume;
    private String deliveryMusicPath;
    private String loopMusicPath;

    // Item Delivery Voice Paths
    private String deliveryNavigatingVoicePath;
    private String deliveryArrivalVoicePath;

    // Loop Delivery Voice Paths
    private String loopNavigatingVoicePath;
    private String loopArrivalVoicePath;

    // Global volume settings removed frequency

    
    // 语速 (0.5x - 2.0x, 默认为 1.0)
    private float speechRate = 1.0f;

    public int getVoiceVolume() {
        return voiceVolume;
    }

    public void setVoiceVolume(int voiceVolume) {
        this.voiceVolume = voiceVolume;
    }

    public int getDeliveryVolume() {
        return deliveryVolume;
    }

    public void setDeliveryVolume(int deliveryVolume) {
        this.deliveryVolume = deliveryVolume;
    }

    public String getDeliveryMusicPath() {
        return deliveryMusicPath;
    }

    public void setDeliveryMusicPath(String deliveryMusicPath) {
        this.deliveryMusicPath = deliveryMusicPath;
    }

    public String getLoopMusicPath() {
        return loopMusicPath;
    }

    public void setLoopMusicPath(String loopMusicPath) {
        this.loopMusicPath = loopMusicPath;
    }

    public String getDeliveryNavigatingVoicePath() {
        return deliveryNavigatingVoicePath;
    }

    public void setDeliveryNavigatingVoicePath(String deliveryNavigatingVoicePath) {
        this.deliveryNavigatingVoicePath = deliveryNavigatingVoicePath;
    }

    public String getDeliveryArrivalVoicePath() {
        return deliveryArrivalVoicePath;
    }

    public void setDeliveryArrivalVoicePath(String deliveryArrivalVoicePath) {
        this.deliveryArrivalVoicePath = deliveryArrivalVoicePath;
    }

    public String getLoopNavigatingVoicePath() {
        return loopNavigatingVoicePath;
    }

    public void setLoopNavigatingVoicePath(String loopNavigatingVoicePath) {
        this.loopNavigatingVoicePath = loopNavigatingVoicePath;
    }

    public String getLoopArrivalVoicePath() {
        return loopArrivalVoicePath;
    }

    public void setLoopArrivalVoicePath(String loopArrivalVoicePath) {
        this.loopArrivalVoicePath = loopArrivalVoicePath;
    }



    private boolean isBackgroundMusicEnabled = true; // Default true
    private boolean isVoiceAnnouncementEnabled = true; // Default true



    // 语音播放间隔 (固定3s，移除配置字段)

    // 开关配置
    private boolean isDeliveryMusicEnabled = true;
    private boolean isDeliveryVoiceEnabled = true;
    private boolean isLoopMusicEnabled = true;
    private boolean isLoopVoiceEnabled = true;

    // Getters and Setters
    public boolean isDeliveryMusicEnabled() { return isDeliveryMusicEnabled; }
    public void setDeliveryMusicEnabled(boolean deliveryMusicEnabled) { isDeliveryMusicEnabled = deliveryMusicEnabled; }

    public boolean isDeliveryVoiceEnabled() { return isDeliveryVoiceEnabled; }
    public void setDeliveryVoiceEnabled(boolean deliveryVoiceEnabled) { isDeliveryVoiceEnabled = deliveryVoiceEnabled; }

    public boolean isLoopMusicEnabled() { return isLoopMusicEnabled; }
    public void setLoopMusicEnabled(boolean loopMusicEnabled) { isLoopMusicEnabled = loopMusicEnabled; }

    public boolean isLoopVoiceEnabled() { return isLoopVoiceEnabled; }
    public void setLoopVoiceEnabled(boolean loopVoiceEnabled) { isLoopVoiceEnabled = loopVoiceEnabled; }
}
