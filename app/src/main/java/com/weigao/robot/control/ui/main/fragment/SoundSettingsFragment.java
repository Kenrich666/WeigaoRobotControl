package com.weigao.robot.control.ui.main.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.AudioConfig;
import com.weigao.robot.control.service.IAudioService;
import com.weigao.robot.control.service.impl.AudioServiceImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class SoundSettingsFragment extends Fragment {

    private enum AudioMode {
        DELIVERY,
        LOOP,
        HOSPITAL
    }

    private static final String TAG = "SoundSettingsFragment";
    private static final String MUSIC_DIR = "WeigaoRobot/audio";
    private static final String EMPTY_AUDIO_LABEL = "(无音频文件)";

    private static final int REQUEST_CODE_PICK_DELIVERY_MUSIC = 2001;
    private static final int REQUEST_CODE_PICK_DELIVERY_NAV_VOICE = 2002;
    private static final int REQUEST_CODE_PICK_DELIVERY_ARRIVAL_VOICE = 2003;
    private static final int REQUEST_CODE_PICK_LOOP_MUSIC = 2004;
    private static final int REQUEST_CODE_PICK_LOOP_NAV_VOICE = 2005;
    private static final int REQUEST_CODE_PICK_LOOP_ARRIVAL_VOICE = 2006;
    private static final int REQUEST_CODE_PICK_HOSPITAL_MUSIC = 2007;
    private static final int REQUEST_CODE_PICK_HOSPITAL_NAV_VOICE = 2008;
    private static final int REQUEST_CODE_PICK_HOSPITAL_ARRIVAL_VOICE = 2009;
    private static final int REQUEST_CODE_PERMISSION = 3001;

    private IAudioService audioService;

    private SeekBar seekDeliveryVolume;
    private TextView tvDeliveryVolumeValue;
    private SeekBar seekVoiceVolume;
    private TextView tvVoiceVolumeValue;

    private Spinner spinnerDeliveryMusic;
    private Button btnSelectDeliveryMusic;
    private Spinner spinnerDeliveryNavVoice;
    private Button btnSelectDeliveryNavVoice;
    private Spinner spinnerDeliveryArrivalVoice;
    private Button btnSelectDeliveryArrivalVoice;

    private Spinner spinnerLoopMusic;
    private Button btnSelectLoopMusic;
    private Spinner spinnerLoopNavVoice;
    private Button btnSelectLoopNavVoice;
    private Spinner spinnerLoopArrivalVoice;
    private Button btnSelectLoopArrivalVoice;

    private Spinner spinnerHospitalMusic;
    private Button btnSelectHospitalMusic;
    private Spinner spinnerHospitalNavVoice;
    private Button btnSelectHospitalNavVoice;
    private Spinner spinnerHospitalArrivalVoice;
    private Button btnSelectHospitalArrivalVoice;

    private Switch switchDeliveryMusic;
    private Switch switchDeliveryVoice;
    private Switch switchLoopMusic;
    private Switch switchLoopVoice;
    private Switch switchHospitalMusic;
    private Switch switchHospitalVoice;

    private View btnPlayDeliveryMusic;
    private View btnTestDeliveryVoice;
    private View btnTestDeliveryArrival;
    private View btnPlayLoopMusic;
    private View btnTestLoopVoice;
    private View btnTestLoopArrival;
    private View btnPlayHospitalMusic;
    private View btnTestHospitalVoice;
    private View btnTestHospitalArrival;
    private View btnStopAllPlayback;

    private final List<String> musicFiles = new ArrayList<>();

    private ArrayAdapter<String> deliveryMusicAdapter;
    private ArrayAdapter<String> deliveryNavVoiceAdapter;
    private ArrayAdapter<String> deliveryArrivalVoiceAdapter;
    private ArrayAdapter<String> loopMusicAdapter;
    private ArrayAdapter<String> loopNavVoiceAdapter;
    private ArrayAdapter<String> loopArrivalVoiceAdapter;
    private ArrayAdapter<String> hospitalMusicAdapter;
    private ArrayAdapter<String> hospitalNavVoiceAdapter;
    private ArrayAdapter<String> hospitalArrivalVoiceAdapter;

    private AudioMode activeNavLoopMode;

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
        activeNavLoopMode = null;
        if (audioService != null) {
            audioService.stopBackgroundMusic(null);
            audioService.stopVoice(null);
            ((AudioServiceImpl) audioService).release();
        }
    }

    private void initView(View view) {
        seekDeliveryVolume = view.findViewById(R.id.seekbar_delivery_volume);
        tvDeliveryVolumeValue = view.findViewById(R.id.tv_delivery_volume_value);
        seekVoiceVolume = view.findViewById(R.id.seekbar_voice_volume);
        tvVoiceVolumeValue = view.findViewById(R.id.tv_voice_volume_value);

        spinnerDeliveryMusic = view.findViewById(R.id.spinner_delivery_music);
        btnSelectDeliveryMusic = view.findViewById(R.id.btn_select_delivery_music);
        spinnerDeliveryNavVoice = view.findViewById(R.id.spinner_delivery_nav_voice);
        btnSelectDeliveryNavVoice = view.findViewById(R.id.btn_select_delivery_nav_voice);
        spinnerDeliveryArrivalVoice = view.findViewById(R.id.spinner_delivery_arrival_voice);
        btnSelectDeliveryArrivalVoice = view.findViewById(R.id.btn_select_delivery_arrival_voice);

        spinnerLoopMusic = view.findViewById(R.id.spinner_loop_music);
        btnSelectLoopMusic = view.findViewById(R.id.btn_select_loop_music);
        spinnerLoopNavVoice = view.findViewById(R.id.spinner_loop_nav_voice);
        btnSelectLoopNavVoice = view.findViewById(R.id.btn_select_loop_nav_voice);
        spinnerLoopArrivalVoice = view.findViewById(R.id.spinner_loop_arrival_voice);
        btnSelectLoopArrivalVoice = view.findViewById(R.id.btn_select_loop_arrival_voice);

        spinnerHospitalMusic = view.findViewById(R.id.spinner_hospital_music);
        btnSelectHospitalMusic = view.findViewById(R.id.btn_select_hospital_music);
        spinnerHospitalNavVoice = view.findViewById(R.id.spinner_hospital_nav_voice);
        btnSelectHospitalNavVoice = view.findViewById(R.id.btn_select_hospital_nav_voice);
        spinnerHospitalArrivalVoice = view.findViewById(R.id.spinner_hospital_arrival_voice);
        btnSelectHospitalArrivalVoice = view.findViewById(R.id.btn_select_hospital_arrival_voice);

        switchDeliveryMusic = view.findViewById(R.id.switch_delivery_music);
        switchDeliveryVoice = view.findViewById(R.id.switch_delivery_voice);
        switchLoopMusic = view.findViewById(R.id.switch_loop_music);
        switchLoopVoice = view.findViewById(R.id.switch_loop_voice);
        switchHospitalMusic = view.findViewById(R.id.switch_hospital_music);
        switchHospitalVoice = view.findViewById(R.id.switch_hospital_voice);

        btnPlayDeliveryMusic = view.findViewById(R.id.btn_play_delivery_music);
        btnTestDeliveryVoice = view.findViewById(R.id.btn_test_delivery_voice);
        btnTestDeliveryArrival = view.findViewById(R.id.btn_test_delivery_arrival);
        btnPlayLoopMusic = view.findViewById(R.id.btn_play_loop_music);
        btnTestLoopVoice = view.findViewById(R.id.btn_test_loop_voice);
        btnTestLoopArrival = view.findViewById(R.id.btn_test_loop_arrival);
        btnPlayHospitalMusic = view.findViewById(R.id.btn_play_hospital_music);
        btnTestHospitalVoice = view.findViewById(R.id.btn_test_hospital_voice);
        btnTestHospitalArrival = view.findViewById(R.id.btn_test_hospital_arrival);
        btnStopAllPlayback = view.findViewById(R.id.btn_stop_bg_music);
    }

    private void initListener() {
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

        refreshMusicList();
        initAdapters();
        bindSpinners();
        bindFilePickers();
        bindSwitches();
        bindTestButtons();
    }

    private void initAdapters() {
        deliveryMusicAdapter = createAdapter();
        deliveryNavVoiceAdapter = createAdapter();
        deliveryArrivalVoiceAdapter = createAdapter();
        loopMusicAdapter = createAdapter();
        loopNavVoiceAdapter = createAdapter();
        loopArrivalVoiceAdapter = createAdapter();
        hospitalMusicAdapter = createAdapter();
        hospitalNavVoiceAdapter = createAdapter();
        hospitalArrivalVoiceAdapter = createAdapter();

        spinnerDeliveryMusic.setAdapter(deliveryMusicAdapter);
        spinnerDeliveryNavVoice.setAdapter(deliveryNavVoiceAdapter);
        spinnerDeliveryArrivalVoice.setAdapter(deliveryArrivalVoiceAdapter);
        spinnerLoopMusic.setAdapter(loopMusicAdapter);
        spinnerLoopNavVoice.setAdapter(loopNavVoiceAdapter);
        spinnerLoopArrivalVoice.setAdapter(loopArrivalVoiceAdapter);
        spinnerHospitalMusic.setAdapter(hospitalMusicAdapter);
        spinnerHospitalNavVoice.setAdapter(hospitalNavVoiceAdapter);
        spinnerHospitalArrivalVoice.setAdapter(hospitalArrivalVoiceAdapter);
    }

    private void bindSpinners() {
        setupSpinnerListener(spinnerDeliveryMusic, deliveryMusicAdapter,
                path -> audioService.setDeliveryMusic(path, new SimpleCallback("配置保存")));
        setupSpinnerListener(spinnerDeliveryNavVoice, deliveryNavVoiceAdapter,
                path -> updateConfigPartial(config -> config.setDeliveryNavigatingVoicePath(path)));
        setupSpinnerListener(spinnerDeliveryArrivalVoice, deliveryArrivalVoiceAdapter,
                path -> updateConfigPartial(config -> config.setDeliveryArrivalVoicePath(path)));

        setupSpinnerListener(spinnerLoopMusic, loopMusicAdapter,
                path -> audioService.setLoopMusic(path, new SimpleCallback("配置保存")));
        setupSpinnerListener(spinnerLoopNavVoice, loopNavVoiceAdapter,
                path -> updateConfigPartial(config -> config.setLoopNavigatingVoicePath(path)));
        setupSpinnerListener(spinnerLoopArrivalVoice, loopArrivalVoiceAdapter,
                path -> updateConfigPartial(config -> config.setLoopArrivalVoicePath(path)));

        setupSpinnerListener(spinnerHospitalMusic, hospitalMusicAdapter,
                path -> audioService.setHospitalMusic(path, new SimpleCallback("配置保存")));
        setupSpinnerListener(spinnerHospitalNavVoice, hospitalNavVoiceAdapter,
                path -> updateConfigPartial(config -> config.setHospitalNavigatingVoicePath(path)));
        setupSpinnerListener(spinnerHospitalArrivalVoice, hospitalArrivalVoiceAdapter,
                path -> updateConfigPartial(config -> config.setHospitalArrivalVoicePath(path)));
    }

    private void bindFilePickers() {
        btnSelectDeliveryMusic.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_DELIVERY_MUSIC));
        btnSelectDeliveryNavVoice.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_DELIVERY_NAV_VOICE));
        btnSelectDeliveryArrivalVoice.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_DELIVERY_ARRIVAL_VOICE));

        btnSelectLoopMusic.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_LOOP_MUSIC));
        btnSelectLoopNavVoice.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_LOOP_NAV_VOICE));
        btnSelectLoopArrivalVoice.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_LOOP_ARRIVAL_VOICE));

        btnSelectHospitalMusic.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_HOSPITAL_MUSIC));
        btnSelectHospitalNavVoice.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_HOSPITAL_NAV_VOICE));
        btnSelectHospitalArrivalVoice.setOnClickListener(v -> openFilePicker(REQUEST_CODE_PICK_HOSPITAL_ARRIVAL_VOICE));
    }

    private void bindSwitches() {
        switchDeliveryMusic.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateConfigPartial(config -> config.setDeliveryMusicEnabled(isChecked)));
        switchDeliveryVoice.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateConfigPartial(config -> config.setDeliveryVoiceEnabled(isChecked)));
        switchLoopMusic.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateConfigPartial(config -> config.setLoopMusicEnabled(isChecked)));
        switchLoopVoice.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateConfigPartial(config -> config.setLoopVoiceEnabled(isChecked)));
        switchHospitalMusic.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateConfigPartial(config -> config.setHospitalMusicEnabled(isChecked)));
        switchHospitalVoice.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateConfigPartial(config -> config.setHospitalVoiceEnabled(isChecked)));
    }

    private void bindTestButtons() {
        btnPlayDeliveryMusic.setOnClickListener(v -> playMusicFromSpinner(spinnerDeliveryMusic));
        btnPlayLoopMusic.setOnClickListener(v -> playMusicFromSpinner(spinnerLoopMusic));
        btnPlayHospitalMusic.setOnClickListener(v -> playMusicFromSpinner(spinnerHospitalMusic));

        btnTestDeliveryVoice.setOnClickListener(v -> startNavLoopTest(spinnerDeliveryNavVoice, AudioMode.DELIVERY));
        btnTestLoopVoice.setOnClickListener(v -> startNavLoopTest(spinnerLoopNavVoice, AudioMode.LOOP));
        btnTestHospitalVoice.setOnClickListener(v -> startNavLoopTest(spinnerHospitalNavVoice, AudioMode.HOSPITAL));

        btnTestDeliveryArrival.setOnClickListener(v -> playArrivalVoice(spinnerDeliveryArrivalVoice));
        btnTestLoopArrival.setOnClickListener(v -> playArrivalVoice(spinnerLoopArrivalVoice));
        btnTestHospitalArrival.setOnClickListener(v -> playArrivalVoice(spinnerHospitalArrivalVoice));

        if (btnStopAllPlayback != null) {
            btnStopAllPlayback.setOnClickListener(v -> {
                activeNavLoopMode = null;
                audioService.stopBackgroundMusic(null);
                audioService.stopVoice(null);
            });
        }
    }

    private void loadData() {
        if (audioService == null) {
            return;
        }
        audioService.getAudioConfig(new IResultCallback<AudioConfig>() {
            @Override
            public void onSuccess(AudioConfig config) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateUI(config));
                }
            }

            @Override
            public void onError(ApiError error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> showToast("加载配置失败"));
                }
            }
        });
    }

    private void updateUI(AudioConfig config) {
        if (config == null) {
            return;
        }

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
        updateSpinnerSelection(spinnerHospitalMusic, config.getHospitalMusicPath());
        updateSpinnerSelection(spinnerHospitalNavVoice, config.getHospitalNavigatingVoicePath());
        updateSpinnerSelection(spinnerHospitalArrivalVoice, config.getHospitalArrivalVoicePath());

        switchDeliveryMusic.setChecked(config.isDeliveryMusicEnabled());
        switchDeliveryVoice.setChecked(config.isDeliveryVoiceEnabled());
        switchLoopMusic.setChecked(config.isLoopMusicEnabled());
        switchLoopVoice.setChecked(config.isLoopVoiceEnabled());
        switchHospitalMusic.setChecked(config.isHospitalMusicEnabled());
        switchHospitalVoice.setChecked(config.isHospitalVoiceEnabled());
    }

    private void playMusicFromSpinner(Spinner spinner) {
        String path = getPathFromSpinner(spinner);
        if (path == null) {
            showToast("请选择文件");
            return;
        }
        audioService.playBackgroundMusic(path, false, new SimpleCallback("试听播放"));
    }

    private void playArrivalVoice(Spinner spinner) {
        String path = getPathFromSpinner(spinner);
        if (path == null) {
            showToast("请先选择到达语音文件");
            return;
        }
        activeNavLoopMode = null;
        audioService.playVoice(path, new SimpleCallback("播放到达语音"));
    }

    private void startNavLoopTest(Spinner spinner, AudioMode mode) {
        String path = getPathFromSpinner(spinner);
        if (path == null) {
            showToast("请先选择行驶语音文件");
            return;
        }
        if (activeNavLoopMode == mode) {
            showToast("正在测试中...");
            return;
        }

        activeNavLoopMode = mode;
        audioService.stopVoice(null);
        showToast("开始行驶语音仿真循环");
        playNavLoop(path, mode);
    }

    private void playNavLoop(String path, AudioMode mode) {
        audioService.playVoice(path, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "playNavLoop failed: " + error.getMessage());
                if (activeNavLoopMode == mode) {
                    activeNavLoopMode = null;
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> showToast("播放失败: " + error.getMessage()));
                }
            }
        }, () -> {
            if (getActivity() == null || activeNavLoopMode != mode) {
                return;
            }
            getActivity().runOnUiThread(() -> playNavLoop(path, mode));
        });
    }

    private interface ConfigMutator {
        void mutate(AudioConfig config);
    }

    private void updateConfigPartial(ConfigMutator mutator) {
        audioService.getAudioConfig(new IResultCallback<AudioConfig>() {
            @Override
            public void onSuccess(AudioConfig config) {
                if (config == null) {
                    return;
                }
                mutator.mutate(config);
                audioService.updateAudioConfig(config, new SimpleCallback("设置已更新"));
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "Config update failed: " + error.getMessage());
            }
        });
    }

    private ArrayAdapter<String> createAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
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
                if (EMPTY_AUDIO_LABEL.equals(fileName)) {
                    return;
                }
                consumer.consume(getFullPath(fileName));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private String getPathFromSpinner(Spinner spinner) {
        Object item = spinner.getSelectedItem();
        if (!(item instanceof String)) {
            return null;
        }
        String name = (String) item;
        if (EMPTY_AUDIO_LABEL.equals(name)) {
            return null;
        }
        return getFullPath(name);
    }

    private File getMusicDir() {
        File dir = new File(Environment.getExternalStorageDirectory(), MUSIC_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private void refreshMusicList() {
        musicFiles.clear();
        File[] files = getMusicDir().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile() && isMusicFile(file.getName())) {
                musicFiles.add(file.getName());
            }
        }
    }

    private boolean isMusicFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".mp3")
                || lower.endsWith(".wav")
                || lower.endsWith(".ogg")
                || lower.endsWith(".flac")
                || lower.endsWith(".aac")
                || lower.endsWith(".m4a")
                || lower.endsWith(".wma");
    }

    private String getFullPath(String fileName) {
        return new File(getMusicDir(), fileName).getAbsolutePath();
    }

    private void updateAdapterData(ArrayAdapter<String> adapter) {
        if (adapter == null) {
            return;
        }
        adapter.clear();
        if (musicFiles.isEmpty()) {
            adapter.add(EMPTY_AUDIO_LABEL);
        } else {
            adapter.addAll(musicFiles);
        }
        adapter.notifyDataSetChanged();
    }

    private void refreshAllAdapters() {
        updateAdapterData(deliveryMusicAdapter);
        updateAdapterData(deliveryNavVoiceAdapter);
        updateAdapterData(deliveryArrivalVoiceAdapter);
        updateAdapterData(loopMusicAdapter);
        updateAdapterData(loopNavVoiceAdapter);
        updateAdapterData(loopArrivalVoiceAdapter);
        updateAdapterData(hospitalMusicAdapter);
        updateAdapterData(hospitalNavVoiceAdapter);
        updateAdapterData(hospitalArrivalVoiceAdapter);
    }

    private void updateSpinnerSelection(Spinner spinner, String path) {
        if (spinner == null || TextUtils.isEmpty(path)) {
            return;
        }
        String name = new File(path).getName();
        int idx = musicFiles.indexOf(name);
        if (idx >= 0) {
            spinner.setSelection(idx);
        }
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
            if (!fileName.contains(".")) {
                fileName += ".mp3";
            }

            File dest = new File(getMusicDir(), fileName);
            if (!dest.exists()) {
                copyUriToFile(uri, dest);
            } else {
                showToast("文件已存在: " + fileName);
            }

            refreshMusicList();
            refreshAllAdapters();

            int idx = musicFiles.indexOf(fileName);
            if (idx >= 0) {
                Spinner target = getTargetSpinnerForRequest(requestCode);
                if (target != null) {
                    target.setSelection(idx);
                }
            }
            showToast("已导入: " + fileName);
        } catch (Exception e) {
            Log.e(TAG, "Import failed", e);
            showToast("导入失败");
        }
    }

    private Spinner getTargetSpinnerForRequest(int requestCode) {
        switch (requestCode) {
            case REQUEST_CODE_PICK_DELIVERY_MUSIC:
                return spinnerDeliveryMusic;
            case REQUEST_CODE_PICK_DELIVERY_NAV_VOICE:
                return spinnerDeliveryNavVoice;
            case REQUEST_CODE_PICK_DELIVERY_ARRIVAL_VOICE:
                return spinnerDeliveryArrivalVoice;
            case REQUEST_CODE_PICK_LOOP_MUSIC:
                return spinnerLoopMusic;
            case REQUEST_CODE_PICK_LOOP_NAV_VOICE:
                return spinnerLoopNavVoice;
            case REQUEST_CODE_PICK_LOOP_ARRIVAL_VOICE:
                return spinnerLoopArrivalVoice;
            case REQUEST_CODE_PICK_HOSPITAL_MUSIC:
                return spinnerHospitalMusic;
            case REQUEST_CODE_PICK_HOSPITAL_NAV_VOICE:
                return spinnerHospitalNavVoice;
            case REQUEST_CODE_PICK_HOSPITAL_ARRIVAL_VOICE:
                return spinnerHospitalArrivalVoice;
            default:
                return null;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme()) && getContext() != null) {
            try (android.database.Cursor cursor = getContext().getContentResolver()
                    .query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) {
                        result = cursor.getString(idx);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to resolve display name", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result == null) {
                return "imported_audio.mp3";
            }
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void copyUriToFile(Uri uri, File dest) throws Exception {
        if (getContext() == null) {
            throw new IllegalStateException("Context is null");
        }
        try (InputStream is = getContext().getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(dest)) {
            if (is == null) {
                throw new IllegalStateException("InputStream is null");
            }
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
        }
    }

    private boolean checkPermission() {
        if (getContext() == null) {
            return false;
        }
        return ContextCompat.checkSelfPermission(
                getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (getActivity() != null) {
            ActivityCompat.requestPermissions(
                    getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadData();
        }
    }

    private void showToast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    private class SimpleCallback implements IResultCallback<Void> {
        private final String tag;

        SimpleCallback(String tag) {
            this.tag = tag;
        }

        @Override
        public void onSuccess(Void result) {
            Log.d(TAG, tag + " success");
        }

        @Override
        public void onError(ApiError e) {
            Log.e(TAG, tag + " failed: " + (e != null ? e.getMessage() : "null"));
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> showToast(tag + "失败"));
            }
        }
    }
}
