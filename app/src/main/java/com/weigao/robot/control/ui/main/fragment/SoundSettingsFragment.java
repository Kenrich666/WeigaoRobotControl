package com.weigao.robot.control.ui.main.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    private Spinner spinnerDeliveryMusic;
    private Spinner spinnerLoopMusic;
    private Button btnSelectDeliveryMusic;
    private Button btnSelectLoopMusic;
    private EditText etAnnouncementFreq;
    private EditText etLoopAnnouncementFreq;

    private static final String MUSIC_DIR = "WeigaoRobot/music";
    private static final int REQUEST_CODE_PICK_DELIVERY = 2001;
    private static final int REQUEST_CODE_PICK_LOOP = 2002;
    private static final int REQUEST_CODE_PERMISSION = 3001;

    private List<String> musicFiles = new ArrayList<>();
    private ArrayAdapter<String> deliveryAdapter;
    private ArrayAdapter<String> loopAdapter;

    // Switch references
    private Switch switchBgMusic;
    private Switch switchVoiceAnnouncement;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化服务
        if (getContext() != null) {
            audioService = new AudioServiceImpl(getContext());
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sound_settings, container, false);
        initView(view);
        initListener();

        // 检查权限，如果有权限则加载数据，否则请求权限
        if (checkPermission()) {
            loadData();
        } else {
            requestPermission();
        }
        return view;
    }

    private View btnPlayDeliveryMusic;
    private View btnStopDeliveryMusic;
    private View btnPlayLoopMusic;
    private View btnStopLoopMusic;
    private View btnTestSpeak;

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop any playing music when leaving settings? Or keep playing?
        // Usually settings preview stops on exit.
        if (audioService != null) {
            audioService.stopBackgroundMusic(null);
            ((AudioServiceImpl) audioService).release();
        }
    }

    private void initView(View view) {
        seekDeliveryVolume = view.findViewById(R.id.seekbar_delivery_volume);
        tvDeliveryVolumeValue = view.findViewById(R.id.tv_delivery_volume_value);

        seekVoiceVolume = view.findViewById(R.id.seekbar_voice_volume);
        tvVoiceVolumeValue = view.findViewById(R.id.tv_voice_volume_value);

        spinnerDeliveryMusic = view.findViewById(R.id.spinner_delivery_music);
        spinnerLoopMusic = view.findViewById(R.id.spinner_loop_music);
        btnSelectDeliveryMusic = view.findViewById(R.id.btn_select_delivery_music);
        btnSelectLoopMusic = view.findViewById(R.id.btn_select_loop_music);

        etAnnouncementFreq = view.findViewById(R.id.et_announcement_freq);
        etLoopAnnouncementFreq = view.findViewById(R.id.et_loop_announcement_freq);

        switchBgMusic = view.findViewById(R.id.switch_bg_music);
        switchVoiceAnnouncement = view.findViewById(R.id.switch_voice_announcement);

        // New Buttons
        btnPlayDeliveryMusic = view.findViewById(R.id.btn_play_delivery_music);
        btnStopDeliveryMusic = view.findViewById(R.id.btn_stop_delivery_music);
        btnPlayLoopMusic = view.findViewById(R.id.btn_play_loop_music);
        btnStopLoopMusic = view.findViewById(R.id.btn_stop_loop_music);
        btnTestSpeak = view.findViewById(R.id.btn_test_speak);
    }

    private void initListener() {
        // ... existing listeners ...
        // 配送音量监听
        seekDeliveryVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvDeliveryVolumeValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

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
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                audioService.setVoiceVolume(seekBar.getProgress(), new SimpleCallback("语音音量"));
            }
        });

        // Music Spinners
        refreshMusicList();
        deliveryAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        deliveryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDeliveryMusic.setAdapter(deliveryAdapter);
        updateAdapter(deliveryAdapter);

        loopAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        loopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLoopMusic.setAdapter(loopAdapter);
        updateAdapter(loopAdapter);

        spinnerDeliveryMusic.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String fileName = deliveryAdapter.getItem(position);
                if ("(无音乐文件)".equals(fileName))
                    return;
                String fullPath = getFullPath(fileName);
                audioService.setDeliveryMusic(fullPath, new SimpleCallback("配送音乐路径"));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinnerLoopMusic.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String fileName = loopAdapter.getItem(position);
                if ("(无音乐文件)".equals(fileName))
                    return;
                String fullPath = getFullPath(fileName);
                audioService.setLoopMusic(fullPath, new SimpleCallback("循环音乐路径"));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnSelectDeliveryMusic.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_DELIVERY));
        btnSelectLoopMusic.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_LOOP));

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

        // Background Music Switch Listener
        switchBgMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            audioService.setBackgroundMusicEnabled(isChecked, new SimpleCallback("背景音乐开关"));
        });

        // Voice Announcement Switch Listener
        switchVoiceAnnouncement.setOnCheckedChangeListener((buttonView, isChecked) -> {
            audioService.setVoiceAnnouncementEnabled(isChecked, new SimpleCallback("语音播报开关"));
        });

        // Button listeners
        btnPlayDeliveryMusic.setOnClickListener(v -> {
            String fileName = (String) spinnerDeliveryMusic.getSelectedItem();
            if (TextUtils.isEmpty(fileName) || "(无音乐文件)".equals(fileName)) {
                showToast("请选择配送音乐");
                return;
            }
            // 试听时强制开启背景音乐开关
            if (!switchBgMusic.isChecked()) {
                switchBgMusic.setChecked(true); // Listener will trigger service update
                showToast("已自动开启背景音乐开关");
            }
            audioService.playBackgroundMusic(getFullPath(fileName), false, new SimpleCallback("播放配送音乐"));
        });

        btnStopDeliveryMusic.setOnClickListener(v -> audioService.stopBackgroundMusic(new SimpleCallback("停止播放")));

        btnPlayLoopMusic.setOnClickListener(v -> {
            String fileName = (String) spinnerLoopMusic.getSelectedItem();
            if (TextUtils.isEmpty(fileName) || "(无音乐文件)".equals(fileName)) {
                showToast("请选择循环音乐");
                return;
            }
            // 试听时强制开启背景音乐开关
            if (!switchBgMusic.isChecked()) {
                switchBgMusic.setChecked(true);
                showToast("已自动开启背景音乐开关");
            }
            audioService.playBackgroundMusic(getFullPath(fileName), true, new SimpleCallback("播放循环音乐"));
        });

        btnStopLoopMusic.setOnClickListener(v -> audioService.stopBackgroundMusic(new SimpleCallback("停止播放")));

        btnTestSpeak.setOnClickListener(v -> audioService.speak("这是一个语音播报测试，欢迎使用威高机器人", new SimpleCallback("语音测试")));
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
        if (audioService == null)
            return;
        // 初始化加载数据：
        audioService.getAudioConfig(new IResultCallback<AudioConfig>() {
            @Override
            public void onSuccess(AudioConfig config) {
                if (getActivity() == null)
                    return;
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
        if (config == null)
            return;

        seekDeliveryVolume.setProgress(config.getDeliveryVolume());
        tvDeliveryVolumeValue.setText(String.valueOf(config.getDeliveryVolume()));

        seekVoiceVolume.setProgress(config.getVoiceVolume());
        tvVoiceVolumeValue.setText(String.valueOf(config.getVoiceVolume()));

        updateSpinnerSelection(spinnerDeliveryMusic, config.getDeliveryMusicPath());
        updateSpinnerSelection(spinnerLoopMusic, config.getLoopMusicPath());

        etAnnouncementFreq.setText(String.valueOf(config.getAnnouncementFrequency()));
        etLoopAnnouncementFreq.setText(String.valueOf(config.getLoopAnnouncementFrequency()));

        // Update Switches without triggering listeners if possible
        // Ideally remove listener, set checked, add listener
        // Or simple setChecked and let callback run (redundant but acceptable)
        switchBgMusic.setOnCheckedChangeListener(null);
        switchBgMusic.setChecked(config.isBackgroundMusicEnabled());
        switchBgMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            audioService.setBackgroundMusicEnabled(isChecked, new SimpleCallback("背景音乐开关"));
        });

        switchVoiceAnnouncement.setOnCheckedChangeListener(null);
        switchVoiceAnnouncement.setChecked(config.isVoiceAnnouncementEnabled());
        switchVoiceAnnouncement.setOnCheckedChangeListener((buttonView, isChecked) -> {
            audioService.setVoiceAnnouncementEnabled(isChecked, new SimpleCallback("语音播报开关"));
        });
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

    // ==================== Music Selection Helpers ====================

    private File getMusicDir() {
        // 使用应用私有目录，绕过 Android 10+ 的分区存储限制，确保读写成功
        // 路径通常为: /sdcard/Android/data/com.weigao.robot.control/files/music/
        File dir = getContext().getExternalFilesDir("music");
        if (dir == null) {
            // 极少情况，回退到内部存储
            dir = new File(getContext().getFilesDir(), "music");
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private void refreshMusicList() {
        musicFiles.clear();
        File dir = getMusicDir();
        Log.d(TAG, "Refreshing music list from: " + dir.getAbsolutePath());

        File[] files = dir.listFiles();
        if (files != null) {
            Log.d(TAG, "Found " + files.length + " files/dirs in music dir.");
            for (File file : files) {
                Log.d(TAG, "Checking file: " + file.getName() + ", IsFile: " + file.isFile());
                if (file.isFile() && isMusicFile(file.getName())) {
                    musicFiles.add(file.getName());
                    Log.d(TAG, "Added music file: " + file.getName());
                } else {
                    Log.d(TAG, "Ignored file: " + file.getName());
                }
            }
        } else {
            Log.e(TAG, "Music dir listFiles() returned null! Exists: " + dir.exists() + ", CanRead: " + dir.canRead());
        }
        Log.d(TAG, "Final music list size: " + musicFiles.size());
    }

    private boolean isMusicFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".ogg");
    }

    private String getFullPath(String fileName) {
        return new File(getMusicDir(), fileName).getAbsolutePath();
    }

    private void openFilePicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择音乐文件"), requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            handlePickedFile(uri, requestCode);
        }
    }

    private void handlePickedFile(Uri uri, int requestCode) {
        try {
            String fileName = getFileName(uri);
            // 如果文件名没有后缀，尝试从MIME类型推断，或者默认添加.mp3
            if (!fileName.contains(".")) {
                // 简单处理：默认添加mp3后缀，或者提示用户
                Log.w(TAG, "File has no extension, appending .mp3: " + fileName);
                fileName += ".mp3";
            }

            File destFile = new File(getMusicDir(), fileName);

            // 检查文件是否已存在，防止重复导入
            if (destFile.exists()) {
                Log.d(TAG, "File already exists: " + fileName);
                showToast("文件已存在，直接选中");
            } else {
                Log.d(TAG, "Copying URI to: " + destFile.getAbsolutePath());
                copyUriToFile(uri, destFile);

                if (destFile.exists()) {
                    Log.d(TAG, "File copy successful. Size: " + destFile.length());
                    showToast("已导入音乐文件: " + fileName);
                } else {
                    Log.e(TAG, "File copy seems failed, file does not exist after copy.");
                    showToast("导入异常：文件不存在");
                    return;
                }
            }

            // Refresh list from filesystem
            refreshMusicList();

            // Sync adapters (this will remove placeholder if list < 0 -> list > 0)
            updateAdapter(deliveryAdapter);
            updateAdapter(loopAdapter);

            // Select the new file
            // Note: Adapter content is synced with musicFiles now.
            // If placeholder was removed, indices are direct mapping to musicFiles.
            int position = musicFiles.indexOf(fileName);

            // Debug Log
            Log.d(TAG,
                    "Imported file: " + fileName + ", New Position: " + position + ", List Size: " + musicFiles.size());
            // Log known files for debugging
            for (int i = 0; i < musicFiles.size(); i++) {
                Log.d(TAG, "List[" + i + "]: " + musicFiles.get(i));
            }

            if (position >= 0) {
                if (requestCode == REQUEST_CODE_PICK_DELIVERY) {
                    spinnerDeliveryMusic.setSelection(position);
                } else {
                    spinnerLoopMusic.setSelection(position);
                }
            } else {
                Log.e(TAG, "Failed to find imported file '" + fileName + "' in refreshed list.");
                showToast("更新列表失败，未找到文件: " + fileName);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to copy music file", e);
            showToast("导入失败: " + e.getMessage());
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContext().getContentResolver().query(uri, null, null, null,
                    null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting filename from content URI", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void copyUriToFile(Uri uri, File dest) throws Exception {
        try (InputStream is = getContext().getContentResolver().openInputStream(uri);
                OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    private void updateSpinnerSelection(Spinner spinner, String path) {
        if (TextUtils.isEmpty(path))
            return;
        File file = new File(path);
        String name = file.getName();
        int position = musicFiles.indexOf(name);
        if (position >= 0) {
            spinner.setSelection(position);
        }
    }

    private void updateAdapter(ArrayAdapter<String> adapter) {
        if (adapter == null)
            return;
        adapter.clear();
        if (musicFiles.isEmpty()) {
            adapter.add("(无音乐文件)");
        } else {
            adapter.addAll(musicFiles);
        }
        adapter.notifyDataSetChanged();
    }

    // ==================== Permissions ====================

    private boolean checkPermission() {
        if (getContext() == null)
            return false;
        int read = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        int write = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (getActivity() == null)
            return;
        ActivityCompat.requestPermissions(getActivity(),
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CODE_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限授予后，刷新列表并加载数据
                refreshMusicList();
                updateAdapter(deliveryAdapter);
                updateAdapter(loopAdapter);
                loadData();
            } else {
                showToast("未授予存储权限，无法读取音乐文件");
            }
        }
    }
}
