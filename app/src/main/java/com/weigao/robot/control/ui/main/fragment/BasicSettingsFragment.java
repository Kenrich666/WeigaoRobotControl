package com.weigao.robot.control.ui.main.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import com.weigao.robot.control.service.impl.ProjectionDoorService;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.weigao.robot.control.R;

import java.util.ArrayList;
import java.util.List;
// 如果是新机（首次运行）： APP 启动时会检查本地存储是否存在配置文件。如果不存在（!file.exists()），会立即调用 
// saveSettings()
// ，将代码中定义的默认值（例如 50 cm/s）写入到本地 JSON 文件中。
// 如果是重启 APP（或再次运行）： APP 启动时会发现本地文件已经存在，就会直接读取文件中的数值，从而保留用户之前修改过的设置。
// 在这两个设置管理器文件中，定义的四个速度的默认值都是 50 cm/s：

// 1. 普通配送 (ItemDeliverySettingsManager.java):

// 配送速度 (delivery_speed): 默认值为 50 (cm/s)
// 返航速度 (return_speed): 默认值为 50 (cm/s)
// 2. 循环配送 (CircularDeliverySettingsManager.java):

// 配送速度 (delivery_speed): 默认值为 50 (cm/s)
// 返航速度 (return_speed): 默认值为 50 (cm/s)

// 全屏设置 AppSettingsManager：已添加逻辑，当检测到本地配置文件（/sdcard/WeigaoRobot/config/app_settings.json）不存在时，立即将代码中的默认配置（非全屏）写入本地文件。
// 密码设置 SecurityServiceImpl 检查，如果不修改默认逻辑，它在文件不存在时会尝试从 SharedPreferences 迁移
// （若是新机则读取代码默认值），并立即调用 saveConfig() 将这些默认值（如密码 123456）写入本地文件 (/sdcard/WeigaoRobot/config/security_config.json)。
public class BasicSettingsFragment extends Fragment {

    private static final int SPEED_MIN = 10;
    private static final int SPEED_MAX = 80;
    private static final int SPEED_PROGRESS_MAX = SPEED_MAX - SPEED_MIN;

    // 取物停留时间范围（秒）
    private static final int STAY_MIN = 10;
    private static final int STAY_MAX = 120;
    private static final int STAY_PROGRESS_MAX = STAY_MAX - STAY_MIN;

    private DrawerLayout drawerLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_basic_settings, container, false);

        drawerLayout = view.findViewById(R.id.drawer_layout);
        View changePasswordView = view.findViewById(R.id.layout_change_password);
        changePasswordView.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        View closeDrawerButton = view.findViewById(R.id.btn_close_drawer);
        closeDrawerButton.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.END));

        EditText etCurrent = view.findViewById(R.id.et_current_password);
        EditText etNew = view.findViewById(R.id.et_new_password);
        View submitPasswordButton = view.findViewById(R.id.btn_submit_password);

        submitPasswordButton.setOnClickListener(v -> {
            String currentInput = etCurrent.getText().toString().trim();
            String newInput = etNew.getText().toString().trim();

            if (TextUtils.isEmpty(currentInput) || TextUtils.isEmpty(newInput)) {
                Toast.makeText(getContext(), "请输入完整密码", Toast.LENGTH_SHORT).show();
                return;
            }

            // Limit to exact 6 digits
            if (newInput.length() != 6) {
                Toast.makeText(getContext(), "新密码长度需为6位数字", Toast.LENGTH_SHORT).show();
                return;
            }

            com.weigao.robot.control.service.ISecurityService securityService = com.weigao.robot.control.service.ServiceManager
                    .getInstance().getSecurityService();

            if (securityService != null) {
                securityService.setPassword(currentInput, newInput,
                        new com.weigao.robot.control.callback.IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "密码已修改", Toast.LENGTH_SHORT).show();
                                        // Clear inputs
                                        etCurrent.setText("");
                                        etNew.setText("");
                                        drawerLayout.closeDrawer(GravityCompat.END);
                                    });
                                }
                            }

                            @Override
                            public void onError(com.weigao.robot.control.callback.ApiError error) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "修改失败: " + error.getMessage(), Toast.LENGTH_SHORT)
                                                .show();
                                    });
                                }
                            }
                        });
            } else {
                Toast.makeText(getContext(), "安全服务不可用", Toast.LENGTH_SHORT).show();
            }
        });

        // --- 1. 普通配送速度设置 ---
        EditText hospitalPresetEditText = view.findViewById(R.id.et_hospital_item_presets);
        View saveHospitalPresetsButton = view.findViewById(R.id.btn_save_hospital_item_presets);

        hospitalPresetEditText.setText(
                com.weigao.robot.control.manager.HospitalItemPresetManager.getInstance().getPresetItemsText());

        saveHospitalPresetsButton.setOnClickListener(v -> {
            List<String> presetItems = parsePresetItems(hospitalPresetEditText.getText().toString());
            if (presetItems.isEmpty()) {
                Toast.makeText(getContext(), "请至少保留一个预设物品", Toast.LENGTH_SHORT).show();
                return;
            }
            com.weigao.robot.control.manager.HospitalItemPresetManager.getInstance().savePresetItems(presetItems);
            hospitalPresetEditText.setText(
                    com.weigao.robot.control.manager.HospitalItemPresetManager.getInstance().getPresetItemsText());
            Toast.makeText(getContext(), "医院预设物品已保存", Toast.LENGTH_SHORT).show();
        });

        SeekBar itemSpeedSeekBar = view.findViewById(R.id.seekbar_item_delivery_speed);
        TextView itemSpeedValue = view.findViewById(R.id.tv_item_delivery_speed_value);

        int currentItemSpeed = com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance()
                .getDeliverySpeed();
        currentItemSpeed = clampSpeed(currentItemSpeed);
        itemSpeedSeekBar.setMax(SPEED_PROGRESS_MAX);
        itemSpeedSeekBar.setProgress(toSeekBarProgress(currentItemSpeed));
        itemSpeedValue.setText(String.format("%d cm/s", currentItemSpeed));

        itemSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int speed = toSpeedValue(progress);
                itemSpeedValue.setText(String.format("%d cm/s", speed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int speed = toSpeedValue(seekBar.getProgress());
                com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance().setDeliverySpeed(speed);
            }
        });

        // 普通配送返航速度
        SeekBar itemReturnSeekBar = view.findViewById(R.id.seekbar_item_return_speed);
        TextView itemReturnSpeedValue = view.findViewById(R.id.tv_item_return_speed_value);

        int currentItemReturnSpeed = com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance()
                .getReturnSpeed();
        currentItemReturnSpeed = clampSpeed(currentItemReturnSpeed);
        itemReturnSeekBar.setMax(SPEED_PROGRESS_MAX);
        itemReturnSeekBar.setProgress(toSeekBarProgress(currentItemReturnSpeed));
        itemReturnSpeedValue.setText(String.format("%d cm/s", currentItemReturnSpeed));

        itemReturnSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int speed = toSpeedValue(progress);
                itemReturnSpeedValue.setText(String.format("%d cm/s", speed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int speed = toSpeedValue(seekBar.getProgress());
                com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance().setReturnSpeed(speed);
            }
        });

        // --- 物品配送取物停留时间设置 ---
        SeekBar itemArrivalStaySeekBar = view.findViewById(R.id.seekbar_item_arrival_stay);
        TextView itemArrivalStayValue = view.findViewById(R.id.tv_item_arrival_stay_value);

        int currentItemStay = com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance()
                .getArrivalStayDuration();
        currentItemStay = clampStay(currentItemStay);
        itemArrivalStaySeekBar.setMax(STAY_PROGRESS_MAX);
        itemArrivalStaySeekBar.setProgress(toStaySeekBarProgress(currentItemStay));
        itemArrivalStayValue.setText(String.format("%d 秒", currentItemStay));

        itemArrivalStaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int stay = toStayValue(progress);
                itemArrivalStayValue.setText(String.format("%d 秒", stay));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int stay = toStayValue(seekBar.getProgress());
                com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance().setArrivalStayDuration(stay);
            }
        });

        // --- 2. 循环配送速度设置 ---

        SeekBar hospitalDeliverySeekBar = view.findViewById(R.id.seekbar_hospital_delivery_speed);
        TextView hospitalDeliveryValue = view.findViewById(R.id.tv_hospital_delivery_speed_value);

        int currentHospitalDeliverySpeed = com.weigao.robot.control.manager.HospitalDeliverySettingsManager
                .getInstance().getDeliverySpeed();
        currentHospitalDeliverySpeed = clampSpeed(currentHospitalDeliverySpeed);
        hospitalDeliverySeekBar.setMax(SPEED_PROGRESS_MAX);
        hospitalDeliverySeekBar.setProgress(toSeekBarProgress(currentHospitalDeliverySpeed));
        hospitalDeliveryValue.setText(String.format("%d cm/s", currentHospitalDeliverySpeed));

        hospitalDeliverySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int speed = toSpeedValue(progress);
                hospitalDeliveryValue.setText(String.format("%d cm/s", speed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int speed = toSpeedValue(seekBar.getProgress());
                com.weigao.robot.control.manager.HospitalDeliverySettingsManager.getInstance()
                        .setDeliverySpeed(speed);
            }
        });

        SeekBar hospitalReturnSeekBar = view.findViewById(R.id.seekbar_hospital_return_speed);
        TextView hospitalReturnValue = view.findViewById(R.id.tv_hospital_return_speed_value);

        int currentHospitalReturnSpeed = com.weigao.robot.control.manager.HospitalDeliverySettingsManager
                .getInstance().getReturnSpeed();
        currentHospitalReturnSpeed = clampSpeed(currentHospitalReturnSpeed);
        hospitalReturnSeekBar.setMax(SPEED_PROGRESS_MAX);
        hospitalReturnSeekBar.setProgress(toSeekBarProgress(currentHospitalReturnSpeed));
        hospitalReturnValue.setText(String.format("%d cm/s", currentHospitalReturnSpeed));

        hospitalReturnSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int speed = toSpeedValue(progress);
                hospitalReturnValue.setText(String.format("%d cm/s", speed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int speed = toSpeedValue(seekBar.getProgress());
                com.weigao.robot.control.manager.HospitalDeliverySettingsManager.getInstance()
                        .setReturnSpeed(speed);
            }
        });

        SeekBar seekBar = view.findViewById(R.id.seekbar_circular_speed);
        TextView speedValue = view.findViewById(R.id.tv_speed_value);

        int currentSpeed = com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance()
                .getDeliverySpeed();
        currentSpeed = clampSpeed(currentSpeed);
        seekBar.setMax(SPEED_PROGRESS_MAX);
        seekBar.setProgress(toSeekBarProgress(currentSpeed));
        speedValue.setText(String.format("%d cm/s", currentSpeed));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int speed = toSpeedValue(progress);
                speedValue.setText(String.format("%d cm/s", speed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = toSpeedValue(seekBar.getProgress());
                com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance()
                        .setDeliverySpeed(progress);
            }
        });

        // Return Speed Slider
        SeekBar returnSeekBar = view.findViewById(R.id.seekbar_return_speed);
        TextView returnSpeedValue = view.findViewById(R.id.tv_return_speed_value);

        int currentReturnSpeed = com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance()
                .getReturnSpeed();
        currentReturnSpeed = clampSpeed(currentReturnSpeed);
        returnSeekBar.setMax(SPEED_PROGRESS_MAX);
        returnSeekBar.setProgress(toSeekBarProgress(currentReturnSpeed));
        returnSpeedValue.setText(String.format("%d cm/s", currentReturnSpeed));

        returnSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int speed = toSpeedValue(progress);
                returnSpeedValue.setText(String.format("%d cm/s", speed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int speed = toSpeedValue(seekBar.getProgress());
                com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance().setReturnSpeed(speed);
            }
        });

        // --- 循环配送取物停留时间设置 ---
        SeekBar circularArrivalStaySeekBar = view.findViewById(R.id.seekbar_circular_arrival_stay);
        TextView circularArrivalStayValue = view.findViewById(R.id.tv_circular_arrival_stay_value);

        int currentCircularStay = com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance()
                .getArrivalStayDuration();
        currentCircularStay = clampStay(currentCircularStay);
        circularArrivalStaySeekBar.setMax(STAY_PROGRESS_MAX);
        circularArrivalStaySeekBar.setProgress(toStaySeekBarProgress(currentCircularStay));
        circularArrivalStayValue.setText(String.format("%d 秒", currentCircularStay));

        circularArrivalStaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int stay = toStayValue(progress);
                circularArrivalStayValue.setText(String.format("%d 秒", stay));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int stay = toStayValue(seekBar.getProgress());
                com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance().setArrivalStayDuration(stay);
            }
        });

        // Fullscreen Switch
        androidx.appcompat.widget.SwitchCompat switchFullscreen = view.findViewById(R.id.switch_fullscreen);
        // Use AppSettingsManager instead of SharedPreferences
        com.weigao.robot.control.manager.AppSettingsManager settingsManager = com.weigao.robot.control.manager.AppSettingsManager
                .getInstance();
        boolean isFullscreen = settingsManager.isFullScreen();
        switchFullscreen.setChecked(isFullscreen);

        switchFullscreen.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsManager.setFullScreen(isChecked);
            applyFullScreen(isChecked);
        });

        // 脚踩投影灯开关门 Switch
        androidx.appcompat.widget.SwitchCompat switchProjectionDoor = view.findViewById(R.id.switch_projection_door);

        // 首次初始化优先反映硬件真实状态
        boolean actualStatus = ProjectionDoorService.getInstance().isLightOn()
                || ProjectionDoorService.getInstance().isDetecting();
        switchProjectionDoor.setChecked(actualStatus);

        switchProjectionDoor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed())
                return; // 仅响应当用户主动点击，防止回调循环
            settingsManager.setProjectionDoorEnabled(isChecked);
            // 立即控制投影灯和检测
            if (isChecked) {
                ProjectionDoorService.getInstance().startContinuousDetection();
            } else {
                ProjectionDoorService.getInstance().stopContinuousDetection();
            }
        });

        // 轮询检查投影灯实际状态，实现 实际状态 -> UI 的双向绑定同步
        Runnable syncRunnable = new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null || getActivity().isFinishing() || !isVisible()
                        || switchProjectionDoor == null)
                    return;
                boolean currentActual = ProjectionDoorService.getInstance().isLightOn()
                        || ProjectionDoorService.getInstance().isDetecting();
                if (switchProjectionDoor.isChecked() != currentActual) {
                    switchProjectionDoor.setChecked(currentActual);
                }
                switchProjectionDoor.postDelayed(this, 1000);
            }
        };
        switchProjectionDoor.postDelayed(syncRunnable, 1000);

        return view;
    }

    private int clampSpeed(int speed) {
        return Math.max(SPEED_MIN, Math.min(SPEED_MAX, speed));
    }

    private int toSeekBarProgress(int speed) {
        return clampSpeed(speed) - SPEED_MIN;
    }

    private int toSpeedValue(int progress) {
        return clampSpeed(progress + SPEED_MIN);
    }

    private List<String> parsePresetItems(String rawText) {
        List<String> items = new ArrayList<>();
        if (rawText == null) {
            return items;
        }

        String[] lines = rawText.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !items.contains(trimmed)) {
                items.add(trimmed);
            }
        }
        return items;
    }

    // 取物停留时间转换方法
    private int clampStay(int stay) {
        return Math.max(STAY_MIN, Math.min(STAY_MAX, stay));
    }

    private int toStaySeekBarProgress(int stay) {
        return clampStay(stay) - STAY_MIN;
    }

    private int toStayValue(int progress) {
        return clampStay(progress + STAY_MIN);
    }

    private void applyFullScreen(boolean enable) {
        if (getActivity() == null)
            return;
        android.view.Window window = getActivity().getWindow();
        if (enable) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }
}
