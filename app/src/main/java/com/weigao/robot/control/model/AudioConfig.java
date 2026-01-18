package com.weigao.robot.control.model;

public class AudioConfig {
    private int voiceVolume;
    private int deliveryVolume;
    private String deliveryMusicPath;
    private String loopMusicPath;
    private int announcementFrequency;
    private int loopAnnouncementFrequency;

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

    public int getAnnouncementFrequency() {
        return announcementFrequency;
    }

    public void setAnnouncementFrequency(int announcementFrequency) {
        this.announcementFrequency = announcementFrequency;
    }

    public int getLoopAnnouncementFrequency() {
        return loopAnnouncementFrequency;
    }

    public void setLoopAnnouncementFrequency(int loopAnnouncementFrequency) {
        this.loopAnnouncementFrequency = loopAnnouncementFrequency;
    }
}
