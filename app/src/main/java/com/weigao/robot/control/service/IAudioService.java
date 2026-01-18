package com.weigao.robot.control.service;

import com.weigao.robot.control.model.AudioConfig;
import com.weigao.robot.control.callback.IResultCallback;

public interface IAudioService {
    void setDeliveryMusic(String musicPath, IResultCallback<Void> callback);

    void setLoopMusic(String musicPath, IResultCallback<Void> callback);

    void setVoiceVolume(int volume, IResultCallback<Void> callback);

    void setDeliveryVolume(int volume, IResultCallback<Void> callback);

    void setAnnouncementFrequency(int frequency, IResultCallback<Void> callback);

    void getVoiceVolume(IResultCallback<Integer> callback);

    void getDeliveryVolume(IResultCallback<Integer> callback);

    void getAudioConfig(IResultCallback<AudioConfig> callback);

    void updateAudioConfig(AudioConfig config, IResultCallback<Void> callback);
}
