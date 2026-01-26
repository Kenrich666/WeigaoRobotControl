package com.weigao.robot.control.ui.main.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.AudioConfig;
import com.weigao.robot.control.service.IAudioService;
import com.weigao.robot.control.service.impl.AudioServiceImpl;

/**
 * 声音设置 Fragment
 * <p>
 * 页面功能：
 * 集成 AudioService，显示并控制音频配置的所有参数。
 * </p>
 * <p>
 * 涉及 Model:
 * 1. AudioConfig: 音频配置数据模型
 * </p>
 * <p>
 * 涉及 Service 方法 (IAudioService):
 * 1. getAudioConfig: 获取当前配置
 * 2. setDeliveryVolume: 设置配送音量
 * 3. setVoiceVolume: 设置语音音量
 * 4. setDeliveryMusic: 设置配送音乐路径
 * 5. setLoopMusic: 设置循环音乐路径
 * 6. setAnnouncementFrequency: 设置播报频率
 * 7. setLoopAnnouncementFrequency: 设置循环播报频率
 * </p>
 */
public class SoundSettingsFragment extends Fragment {

    private static final String TAG = "SoundSettingsFragment";
    private IAudioService audioService;

    // View references
    private SeekBar seekDeliveryVolume;
    private TextView tvDeliveryVolumeValue;
    private SeekBar seekVoiceVolume;
    private TextView tvVoiceVolumeValue;
    private EditText etDeliveryMusicPath;
    private EditText etLoopMusicPath;
    private EditText etAnnouncementFreq;
    private EditText etLoopAnnouncementFreq;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化服务
        if (getContext() != null) {
            audioService = new AudioServiceImpl(getContext());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sound_settings, container, false);
        initView(view);
        initListener();
        loadData();
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioService instanceof AudioServiceImpl) {
            ((AudioServiceImpl) audioService).release();
        }
    }

    private void initView(View view) {
        seekDeliveryVolume = view.findViewById(R.id.seekbar_delivery_volume);
        tvDeliveryVolumeValue = view.findViewById(R.id.tv_delivery_volume_value);
        
        seekVoiceVolume = view.findViewById(R.id.seekbar_voice_volume);
        tvVoiceVolumeValue = view.findViewById(R.id.tv_voice_volume_value);
        
        etDeliveryMusicPath = view.findViewById(R.id.et_delivery_music_path);
        etLoopMusicPath = view.findViewById(R.id.et_loop_music_path);
        
        etAnnouncementFreq = view.findViewById(R.id.et_announcement_freq);
        etLoopAnnouncementFreq = view.findViewById(R.id.et_loop_announcement_freq);
    }

    private void initListener() {
        // 配送音量监听
        seekDeliveryVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvDeliveryVolumeValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                audioService.setDeliveryVolume(seekBar.getProgress(), new SimpleCallback("配送音量"));
            }
        });

        // 语音音量监听
        seekVoiceVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVoiceVolumeValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                audioService.setVoiceVolume(seekBar.getProgress(), new SimpleCallback("语音音量"));
            }
        });

        // 配送音乐路径监听 (失去焦点时保存)
        etDeliveryMusicPath.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String path = etDeliveryMusicPath.getText().toString().trim();
                audioService.setDeliveryMusic(path, new SimpleCallback("配送音乐路径"));
            }
        });

        // 循环音乐路径监听
        etLoopMusicPath.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String path = etLoopMusicPath.getText().toString().trim();
                audioService.setLoopMusic(path, new SimpleCallback("循环音乐路径"));
            }
        });

        // 播报频率监听
        etAnnouncementFreq.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveIntSettingFromEditText(etAnnouncementFreq, new IntSetter() {
                    @Override
                    public void set(int value) {
                        audioService.setAnnouncementFrequency(value, new SimpleCallback("播报频率"));
                    }
                });
            }
        });

        // 循环播报频率监听
        etLoopAnnouncementFreq.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveIntSettingFromEditText(etLoopAnnouncementFreq, new IntSetter() {
                    @Override
                    public void set(int value) {
                        audioService.setLoopAnnouncementFrequency(value, new SimpleCallback("循环播报频率"));
                    }
                });
            }
        });
    }

    private interface IntSetter {
        void set(int value);
    }

    private void saveIntSettingFromEditText(EditText editText, IntSetter setter) {
        String input = editText.getText().toString().trim();
        if (!TextUtils.isEmpty(input)) {
            try {
                int value = Integer.parseInt(input);
                setter.set(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid number format", e);
                showToast("请输入有效数字");
            }
        }
    }

    private void loadData() {
        if (audioService == null) return;
        // 初始化加载数据：
        audioService.getAudioConfig(new IResultCallback<AudioConfig>() {
            @Override
            public void onSuccess(AudioConfig config) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> updateUI(config));
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "获取配置失败: " + error.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> showToast("获取配置失败"));
                }
            }
        });
    }

    private void updateUI(AudioConfig config) {
        if (config == null) return;

        seekDeliveryVolume.setProgress(config.getDeliveryVolume());
        tvDeliveryVolumeValue.setText(String.valueOf(config.getDeliveryVolume()));

        seekVoiceVolume.setProgress(config.getVoiceVolume());
        tvVoiceVolumeValue.setText(String.valueOf(config.getVoiceVolume()));

        etDeliveryMusicPath.setText(config.getDeliveryMusicPath());
        etLoopMusicPath.setText(config.getLoopMusicPath());

        etAnnouncementFreq.setText(String.valueOf(config.getAnnouncementFrequency()));
        etLoopAnnouncementFreq.setText(String.valueOf(config.getLoopAnnouncementFrequency()));
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    // 简易回调实现，用于简单的成功/失败提示
    private class SimpleCallback implements IResultCallback<Void> {
        private final String actionName;

        public SimpleCallback(String actionName) {
            this.actionName = actionName;
        }

        @Override
        public void onSuccess(Void result) {
            Log.d(TAG, actionName + " 成功");
            // 可选：成功时不打扰用户，或者显示Toast
            // runOnUiThread(() -> showToast(actionName + " 已保存"));
        }

        @Override
        public void onError(ApiError error) {
            Log.e(TAG, actionName + " 失败: " + error.getMessage());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> showToast(actionName + " 失败"));
            }
        }
    }
}
