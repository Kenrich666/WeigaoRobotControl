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

 * </p>
 */
public class SoundSettingsFragment extends Fragment {

    private static final String TAG = "SoundSettingsFragment";
    private IAudioService audioService;

    // Volume
    private SeekBar seekDeliveryVolume;
    private TextView tvDeliveryVolumeValue;
    private SeekBar seekVoiceVolume;
    private TextView tvVoiceVolumeValue;

    // Delivery Mode
    private Spinner spinnerDeliveryMusic;
    private Button btnSelectDeliveryMusic;
    private Spinner spinnerDeliveryNavVoice;
    private Button btnSelectDeliveryNavVoice;
    private Spinner spinnerDeliveryArrivalVoice;
    private Button btnSelectDeliveryArrivalVoice;


    // Loop Mode
    private Spinner spinnerLoopMusic;
    private Button btnSelectLoopMusic;
    private Spinner spinnerLoopNavVoice;
    private Button btnSelectLoopNavVoice;
    private Spinner spinnerLoopArrivalVoice;
    private Button btnSelectLoopArrivalVoice;


    // Independent Switches
    private Switch switchDeliveryMusic;
    private Switch switchDeliveryVoice;
    private Switch switchLoopMusic;
    private Switch switchLoopVoice;
    
    // Test States
    private boolean isDeliveryNavTestLooping = false;
    private boolean isLoopNavTestLooping = false;

    // Control Buttons
    private View btnPlayDeliveryMusic;
    private View btnStopDeliveryMusic;
    private View btnTestDeliveryVoice;
    private View btnTestDeliveryArrival; // New
    private View btnPlayLoopMusic;
    private View btnTestLoopVoice;
    private View btnTestLoopArrival; // New
    private View btnStopLoopMusic;

    private static final String MUSIC_DIR = "WeigaoRobot/audio";

    // Request Codes
    private static final int REQUEST_CODE_PICK_DELIVERY_MUSIC = 2001;
    private static final int REQUEST_CODE_PICK_DELIVERY_NAV_VOICE = 2002;
    private static final int REQUEST_CODE_PICK_DELIVERY_ARRIVAL_VOICE = 2003;

    private static final int REQUEST_CODE_PICK_LOOP_MUSIC = 2004;
    private static final int REQUEST_CODE_PICK_LOOP_NAV_VOICE = 2005;
    private static final int REQUEST_CODE_PICK_LOOP_ARRIVAL_VOICE = 2006;

    private static final int REQUEST_CODE_PERMISSION = 3001;

    // Data
    private List<String> musicFiles = new ArrayList<>();
    
    // Adapters
    private ArrayAdapter<String> deliveryMusicAdapter;
    private ArrayAdapter<String> deliveryNavVoiceAdapter;
    private ArrayAdapter<String> deliveryArrivalVoiceAdapter;
    
    private ArrayAdapter<String> loopMusicAdapter;
    private ArrayAdapter<String> loopNavVoiceAdapter;
    private ArrayAdapter<String> loopArrivalVoiceAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        if (checkPermission()) {
            loadData();
        } else {
            requestPermission();
        }
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioService != null) {
            audioService.stopBackgroundMusic(null);
            audioService.stopVoice(null);
            ((AudioServiceImpl) audioService).release();
        }
    }

    private void initView(View view) {
        // Volume
        seekDeliveryVolume = view.findViewById(R.id.seekbar_delivery_volume);
        tvDeliveryVolumeValue = view.findViewById(R.id.tv_delivery_volume_value);
        seekVoiceVolume = view.findViewById(R.id.seekbar_voice_volume);
        tvVoiceVolumeValue = view.findViewById(R.id.tv_voice_volume_value);

        // Settings - Delivery
        spinnerDeliveryMusic = view.findViewById(R.id.spinner_delivery_music);
        btnSelectDeliveryMusic = view.findViewById(R.id.btn_select_delivery_music);
        spinnerDeliveryNavVoice = view.findViewById(R.id.spinner_delivery_nav_voice);
        btnSelectDeliveryNavVoice = view.findViewById(R.id.btn_select_delivery_nav_voice);
        spinnerDeliveryArrivalVoice = view.findViewById(R.id.spinner_delivery_arrival_voice);
        btnSelectDeliveryArrivalVoice = view.findViewById(R.id.btn_select_delivery_arrival_voice);


        // Settings - Loop
        spinnerLoopMusic = view.findViewById(R.id.spinner_loop_music);
        btnSelectLoopMusic = view.findViewById(R.id.btn_select_loop_music);
        spinnerLoopNavVoice = view.findViewById(R.id.spinner_loop_nav_voice);
        btnSelectLoopNavVoice = view.findViewById(R.id.btn_select_loop_nav_voice);
        spinnerLoopArrivalVoice = view.findViewById(R.id.spinner_loop_arrival_voice);
        btnSelectLoopArrivalVoice = view.findViewById(R.id.btn_select_loop_arrival_voice);


        // Switches
        switchDeliveryMusic = view.findViewById(R.id.switch_delivery_music);
        switchDeliveryVoice = view.findViewById(R.id.switch_delivery_voice);
        switchLoopMusic = view.findViewById(R.id.switch_loop_music);
        switchLoopVoice = view.findViewById(R.id.switch_loop_voice);

        // Controls
        btnPlayDeliveryMusic = view.findViewById(R.id.btn_play_delivery_music);
        btnStopDeliveryMusic = view.findViewById(R.id.btn_stop_bg_music); 
        
        btnTestDeliveryVoice = view.findViewById(R.id.btn_test_delivery_voice);
        btnTestDeliveryArrival = view.findViewById(R.id.btn_test_delivery_arrival); 
        
        btnPlayLoopMusic = view.findViewById(R.id.btn_play_loop_music);
        btnTestLoopVoice = view.findViewById(R.id.btn_test_loop_voice);
        btnTestLoopArrival = view.findViewById(R.id.btn_test_loop_arrival);
    }

    private void initListener() {
        // Volume Listeners
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

        // Initialize Adapters
        refreshMusicList();
        deliveryMusicAdapter = createAdapter();
        deliveryNavVoiceAdapter = createAdapter();
        deliveryArrivalVoiceAdapter = createAdapter();
        
        loopMusicAdapter = createAdapter();
        loopNavVoiceAdapter = createAdapter();
        loopArrivalVoiceAdapter = createAdapter();

        spinnerDeliveryMusic.setAdapter(deliveryMusicAdapter);
        spinnerDeliveryNavVoice.setAdapter(deliveryNavVoiceAdapter);
        spinnerDeliveryArrivalVoice.setAdapter(deliveryArrivalVoiceAdapter);
        
        spinnerLoopMusic.setAdapter(loopMusicAdapter);
        spinnerLoopNavVoice.setAdapter(loopNavVoiceAdapter);
        spinnerLoopArrivalVoice.setAdapter(loopArrivalVoiceAdapter);

        // Spinner Listeners
        setupSpinnerListener(spinnerDeliveryMusic, deliveryMusicAdapter, path -> 
            audioService.setDeliveryMusic(path, new SimpleCallback("配置保存")));
            
        setupSpinnerListener(spinnerDeliveryNavVoice, deliveryNavVoiceAdapter, path -> {
             // 需要 AudioService 提供 set 方法，或者直接更新 config 对象
             // 由于 IAudioService 接口主要暴露 setMusic 等，建议通过 updateAudioConfig 全量更新或扩展接口
             // 这里为了方便，我们直接获取 config -> modify -> update
             updateConfigPartial(c -> c.setDeliveryNavigatingVoicePath(path));
        });
        
        setupSpinnerListener(spinnerDeliveryArrivalVoice, deliveryArrivalVoiceAdapter, path -> 
             updateConfigPartial(c -> c.setDeliveryArrivalVoicePath(path)));

        setupSpinnerListener(spinnerLoopMusic, loopMusicAdapter, path -> 
            audioService.setLoopMusic(path, new SimpleCallback("配置保存")));

        setupSpinnerListener(spinnerLoopNavVoice, loopNavVoiceAdapter, path -> 
             updateConfigPartial(c -> c.setLoopNavigatingVoicePath(path)));

        setupSpinnerListener(spinnerLoopArrivalVoice, loopArrivalVoiceAdapter, path -> 
             updateConfigPartial(c -> c.setLoopArrivalVoicePath(path)));

        // File Select Buttons
        btnSelectDeliveryMusic.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_DELIVERY_MUSIC));
        btnSelectDeliveryNavVoice.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_DELIVERY_NAV_VOICE));
        btnSelectDeliveryArrivalVoice.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_DELIVERY_ARRIVAL_VOICE));
        
        btnSelectLoopMusic.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_LOOP_MUSIC));
        btnSelectLoopNavVoice.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_LOOP_NAV_VOICE));
        btnSelectLoopArrivalVoice.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_LOOP_ARRIVAL_VOICE));




        // Switches
        switchDeliveryMusic.setOnCheckedChangeListener((v, isChecked) -> 
            updateConfigPartial(c -> c.setDeliveryMusicEnabled(isChecked)));
        switchDeliveryVoice.setOnCheckedChangeListener((v, isChecked) -> 
            updateConfigPartial(c -> c.setDeliveryVoiceEnabled(isChecked)));
        switchLoopMusic.setOnCheckedChangeListener((v, isChecked) -> 
            updateConfigPartial(c -> c.setLoopMusicEnabled(isChecked)));
        switchLoopVoice.setOnCheckedChangeListener((v, isChecked) -> 
            updateConfigPartial(c -> c.setLoopVoiceEnabled(isChecked)));

        // Test Buttons
        btnPlayDeliveryMusic.setOnClickListener(v -> playMusicFromSpinner(spinnerDeliveryMusic));
        btnPlayLoopMusic.setOnClickListener(v -> playMusicFromSpinner(spinnerLoopMusic));
        
        if (btnStopDeliveryMusic != null) {
            btnStopDeliveryMusic.setOnClickListener(v -> {
                // Reset test loop states
                isDeliveryNavTestLooping = false;
                isLoopNavTestLooping = false;
                audioService.stopBackgroundMusic(null);
                audioService.stopVoice(null);
            });
        }
        
        if (btnTestDeliveryVoice != null) {
            btnTestDeliveryVoice.setOnClickListener(v -> {
                 // Test play the selected Navigating voice (Delivery) - LOOP
                 String path = getPathFromSpinner(spinnerDeliveryNavVoice);
                 if (path != null) {
                     if (isDeliveryNavTestLooping) {
                         showToast("正在测试中...");
                         return;
                     }
                     showToast("开始行驶语音仿真循环");
                     isDeliveryNavTestLooping = true;
                     playNavLoop(path, true);
                 } else {
                     showToast("请先选择行驶语音文件");
                 }
            });
        }
        
        if (btnTestDeliveryArrival != null) {
            btnTestDeliveryArrival.setOnClickListener(v -> {
                 // Test play the selected Arrival voice (Delivery)
                 String path = getPathFromSpinner(spinnerDeliveryArrivalVoice);
                 if (path != null) {
                     audioService.playVoice(path, new SimpleCallback("播放到达语音"));
                 } else {
                     showToast("请先选择到达语音文件");
                 }
            });
        }
        
        if (btnTestLoopVoice != null) {
            btnTestLoopVoice.setOnClickListener(v -> {
                 // Test play the selected Navigating voice (Loop) - LOOP
                 String path = getPathFromSpinner(spinnerLoopNavVoice);
                 if (path != null) {
                     if (isLoopNavTestLooping) {
                         showToast("正在测试中...");
                         return;
                     }
                     showToast("开始行驶语音仿真循环");
                     isLoopNavTestLooping = true;
                     playNavLoop(path, false);
                 } else {
                     showToast("请先选择行驶语音文件");
                 }
            });
        }

        if (btnTestLoopArrival != null) {
            btnTestLoopArrival.setOnClickListener(v -> {
                 // Test play the selected Arrival voice (Loop)
                 String path = getPathFromSpinner(spinnerLoopArrivalVoice);
                 if (path != null) {
                     audioService.playVoice(path, new SimpleCallback("播放到达语音"));
                 } else {
                     showToast("请先选择到达语音文件");
                 }
            });
        }
    }
    
    // Helper to update config via lambda
    private interface ConfigMutator {
        void mutate(AudioConfig config);
    }
    
    private void updateConfigPartial(ConfigMutator mutator) {
         audioService.getAudioConfig(new IResultCallback<AudioConfig>() {
             @Override
             public void onSuccess(AudioConfig config) {
                 if (config == null) return;
                 mutator.mutate(config);
                 audioService.updateAudioConfig(config, new SimpleCallback("设置已更新")); // Needs IAudioServiceImpl definition or general update
                 // Note: IAudioService defined updateAudioConfig in previous step context check?
                 // Checking IAudioService in context... yes it has updateAudioConfig.
             }
             @Override
             public void onError(ApiError error) {
                 Log.e(TAG, "Config Update Failed: " + error.getMessage());
             }
         });
    }

    private void playMusicFromSpinner(Spinner spinner) {
        String path = getPathFromSpinner(spinner);
        if (path == null) {
            showToast("请选择文件");
            return;
        }
        if (!switchDeliveryMusic.isChecked() && !switchLoopMusic.isChecked()) {
             // For test purposes, we might want to auto-enable. Which one?
             // Since this is a generic helper, let's just warn or enable the one corresponding to the spinner?
             // But we don't know which spinner unless passed.
             // Assume caller handles enabling or just play anyway (Service might block if we check flags inside service).
             // Service check was removed for music in plan, but let's see.
             // Reverting logic: "Automatic enable" logic is tricky with split switches.
             // Let's just play.
        }
        // Force loop false for testing? Or true? Usually test is just listen.
        audioService.playBackgroundMusic(path, false, new SimpleCallback("试听播放"));
    }

    private String getPathFromSpinner(Spinner spinner) {
        Object item = spinner.getSelectedItem();
        if (item == null) return null;
        String name = (String) item;
        if ("(无音乐文件)".equals(name)) return null;
        return getFullPath(name);
    }

    private ArrayAdapter<String> createAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        updateAdapterData(adapter);
        return adapter;
    }

    private interface PathConsumer {
        void consume(String fullPath);
    }

    private void setupSpinnerListener(Spinner spinner, ArrayAdapter<String> adapter, PathConsumer consumer) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String fileName = adapter.getItem(position);
                if ("(无音乐文件)".equals(fileName)) return;
                consumer.consume(getFullPath(fileName));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private interface IntSetter { void set(int value); }
    private void saveIntSettingFromEditText(EditText editText, IntSetter setter) {
        String input = editText.getText().toString().trim();
        if (!TextUtils.isEmpty(input)) {
            try {
                setter.set(Integer.parseInt(input));
            } catch (NumberFormatException e) {
                showToast("无效数字");
            }
        }
    }

    private void loadData() {
        if (audioService == null) return;
        audioService.getAudioConfig(new IResultCallback<AudioConfig>() {
            @Override
            public void onSuccess(AudioConfig config) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateUI(config));
                }
            }
            @Override
            public void onError(ApiError error) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> showToast("加载配置失败"));
            }
        });
    }

    private void updateUI(AudioConfig config) {
        if (config == null) return;
        
        seekDeliveryVolume.setProgress(config.getDeliveryVolume());
        tvDeliveryVolumeValue.setText(String.valueOf(config.getDeliveryVolume()));
        seekVoiceVolume.setProgress(config.getVoiceVolume());
        tvVoiceVolumeValue.setText(String.valueOf(config.getVoiceVolume()));

        updateSpinnerSelection(spinnerDeliveryMusic, config.getDeliveryMusicPath());
        updateSpinnerSelection(spinnerDeliveryNavVoice, config.getDeliveryNavigatingVoicePath());
        updateSpinnerSelection(spinnerDeliveryArrivalVoice, config.getDeliveryArrivalVoicePath());
        
        updateSpinnerSelection(spinnerLoopMusic, config.getLoopMusicPath());
        updateSpinnerSelection(spinnerLoopNavVoice, config.getLoopNavigatingVoicePath());
        updateSpinnerSelection(spinnerLoopArrivalVoice, config.getLoopArrivalVoicePath());


        
        // Block listeners to prevent loops?
        // Simple way: rely on state diff or just set. current implementation is idempotent enough.
        switchDeliveryMusic.setChecked(config.isDeliveryMusicEnabled());
        switchDeliveryVoice.setChecked(config.isDeliveryVoiceEnabled());
        switchLoopMusic.setChecked(config.isLoopMusicEnabled());
        switchLoopVoice.setChecked(config.isLoopVoiceEnabled());
    }

    private void playNavLoop(String path, boolean isDelivery) {
        audioService.playVoice(path, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Playback started successfully (or delayed)
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "playNavLoop failed: " + error.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("播放失败: " + error.getMessage());
                        // Stop looping state on error
                        if (isDelivery) {
                            isDeliveryNavTestLooping = false;
                        } else {
                            isLoopNavTestLooping = false;
                        }
                    });
                }
            }
        }, () -> {
            if (getActivity() == null) return;
            boolean looping = isDelivery ? isDeliveryNavTestLooping : isLoopNavTestLooping;
            if (looping) {
                // Recursively play again
                getActivity().runOnUiThread(() -> playNavLoop(path, isDelivery));
            }
        });
    }

    // ==================== Helpers ==================== //

    private File getMusicDir() {
        File dir = new File(Environment.getExternalStorageDirectory(), MUSIC_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private void refreshMusicList() {
        musicFiles.clear();
        File dir = getMusicDir();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && isMusicFile(f.getName())) {
                    musicFiles.add(f.getName());
                }
            }
        }
    }

    private boolean isMusicFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".ogg")
            || lower.endsWith(".flac") || lower.endsWith(".aac") || lower.endsWith(".m4a") || lower.endsWith(".wma");
    }

    private String getFullPath(String fileName) {
        return new File(getMusicDir(), fileName).getAbsolutePath();
    }
    
    private void updateAdapterData(ArrayAdapter<String> adapter) {
        if (adapter == null) return;
        adapter.clear();
        if (musicFiles.isEmpty()) {
            adapter.add("(无音乐文件)");
        } else {
            adapter.addAll(musicFiles);
        }
        adapter.notifyDataSetChanged();
    }
    
    private void updateSpinnerSelection(Spinner spinner, String path) {
        if (TextUtils.isEmpty(path)) return;
        File file = new File(path);
        String name = file.getName();
        int idx = musicFiles.indexOf(name);
        if (idx >= 0) spinner.setSelection(idx);
    }

    private void openFilePicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择音频文件"), requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            handlePickedFile(data.getData(), requestCode);
        }
    }
    
    private void handlePickedFile(Uri uri, int requestCode) {
        try {
            String fileName = getFileName(uri);
            if (!fileName.contains(".")) fileName += ".mp3"; // default fallback
            
            File dest = new File(getMusicDir(), fileName);
            if (!dest.exists()) {
                copyUriToFile(uri, dest);
            } else {
                showToast("文件已存在: " + fileName);
            }
            
            refreshMusicList();
            
            // Update all adapters
            updateAdapterData(deliveryMusicAdapter);
            updateAdapterData(deliveryNavVoiceAdapter);
            updateAdapterData(deliveryArrivalVoiceAdapter);
            updateAdapterData(loopMusicAdapter);
            updateAdapterData(loopNavVoiceAdapter);
            updateAdapterData(loopArrivalVoiceAdapter);
            
            // Select in the spinner that initiated this
            int idx = musicFiles.indexOf(fileName);
            if (idx >= 0) {
                Spinner target = null;
                if (requestCode == REQUEST_CODE_PICK_DELIVERY_MUSIC) target = spinnerDeliveryMusic;
                else if (requestCode == REQUEST_CODE_PICK_DELIVERY_NAV_VOICE) target = spinnerDeliveryNavVoice;
                else if (requestCode == REQUEST_CODE_PICK_DELIVERY_ARRIVAL_VOICE) target = spinnerDeliveryArrivalVoice;
                else if (requestCode == REQUEST_CODE_PICK_LOOP_MUSIC) target = spinnerLoopMusic;
                else if (requestCode == REQUEST_CODE_PICK_LOOP_NAV_VOICE) target = spinnerLoopNavVoice;
                else if (requestCode == REQUEST_CODE_PICK_LOOP_ARRIVAL_VOICE) target = spinnerLoopArrivalVoice;
                
                if (target != null) target.setSelection(idx);
            }
            showToast("已导入: " + fileName);
            
        } catch (Exception e) {
            Log.e(TAG, "Import failed", e);
            showToast("导入失败");
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                     int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                     if (idx != -1) result = cursor.getString(idx);
                }
            } catch (Exception e) {}
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private void copyUriToFile(Uri uri, File dest) throws Exception {
        try (InputStream is = getContext().getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
        }
    }

    private boolean checkPermission() {
        if (getContext() == null) return false;
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (getActivity() != null) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadData();
        }
    }

    private void showToast(String msg) {
        if (getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private class SimpleCallback implements IResultCallback<Void> {
        private String tag;
        SimpleCallback(String t) { tag = t; }
        @Override public void onSuccess(Void result) { Log.d(TAG, tag + " Success"); }
        @Override public void onError(ApiError e) { 
             Log.e(TAG, tag + " Failed: " + (e!=null?e.getMessage():"null")); 
             if(getActivity()!=null) getActivity().runOnUiThread(()->showToast(tag + " 失败"));
        }
    }
}
