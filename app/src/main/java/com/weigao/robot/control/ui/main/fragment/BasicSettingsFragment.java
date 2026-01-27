package com.weigao.robot.control.ui.main.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.weigao.robot.control.R;
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

// 全屏设置 AppSettingsManager：已添加逻辑，当检测到本地配置文件（/sdcard/WeigaoRobot/settings/app_settings.json）不存在时，立即将代码中的默认配置（非全屏）写入本地文件。
// 密码设置 SecurityServiceImpl 检查，如果不修改默认逻辑，它在文件不存在时会尝试从 SharedPreferences 迁移
// （若是新机则读取代码默认值），并立即调用 saveConfig() 将这些默认值（如密码 123456）写入本地文件 (/sdcard/WeigaoRobot/config/security_config.json)。
public class BasicSettingsFragment extends Fragment {

    private DrawerLayout drawerLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

            com.weigao.robot.control.service.ISecurityService securityService = 
                    com.weigao.robot.control.service.ServiceManager.getInstance().getSecurityService();

            if (securityService != null) {
                securityService.setPassword(currentInput, newInput, new com.weigao.robot.control.callback.IResultCallback<Void>() {
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
                                Toast.makeText(getContext(), "修改失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            } else {
                Toast.makeText(getContext(), "安全服务不可用", Toast.LENGTH_SHORT).show();
            }
        });
        
        // --- 1. 普通配送速度设置 ---
        SeekBar itemSpeedSeekBar = view.findViewById(R.id.seekbar_item_delivery_speed);
        TextView itemSpeedValue = view.findViewById(R.id.tv_item_delivery_speed_value);
        
        int currentItemSpeed = com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance().getDeliverySpeed();
        itemSpeedSeekBar.setProgress(currentItemSpeed);
        itemSpeedValue.setText(String.format("%d cm/s", currentItemSpeed));
        
        itemSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 10) progress = 10;
                itemSpeedValue.setText(String.format("%d cm/s", progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (progress < 10) progress = 10;
                com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance().setDeliverySpeed(progress);
            }
        });

        // 普通配送返航速度
        SeekBar itemReturnSeekBar = view.findViewById(R.id.seekbar_item_return_speed);
        TextView itemReturnSpeedValue = view.findViewById(R.id.tv_item_return_speed_value);

        int currentItemReturnSpeed = com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance().getReturnSpeed();
        itemReturnSeekBar.setProgress(currentItemReturnSpeed);
        itemReturnSpeedValue.setText(String.format("%d cm/s", currentItemReturnSpeed));

        itemReturnSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 10) progress = 10;
                itemReturnSpeedValue.setText(String.format("%d cm/s", progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (progress < 10) progress = 10;
                com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance().setReturnSpeed(progress);
            }
        });

        // --- 2. 循环配送速度设置 ---

        SeekBar seekBar = view.findViewById(R.id.seekbar_circular_speed);
        TextView speedValue = view.findViewById(R.id.tv_speed_value);

        int currentSpeed = com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance().getDeliverySpeed();
        seekBar.setProgress(currentSpeed);
        speedValue.setText(String.format("%d cm/s", currentSpeed));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Ensure value is at least min if necessary, though min attribute handles it in API 26+
                // If min attribute is not supported by target SDK, handle it here. Assuming API 26+ or valid min.
                if (progress < 10) progress = 10; 
                speedValue.setText(String.format("%d cm/s", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (progress < 10) progress = 10;
                com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance().setDeliverySpeed(progress);
            }
        });

        // Return Speed Slider
        SeekBar returnSeekBar = view.findViewById(R.id.seekbar_return_speed);
        TextView returnSpeedValue = view.findViewById(R.id.tv_return_speed_value);

        int currentReturnSpeed = com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance().getReturnSpeed();
        returnSeekBar.setProgress(currentReturnSpeed);
        returnSpeedValue.setText(String.format("%d cm/s", currentReturnSpeed));

        returnSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 10) progress = 10;
                returnSpeedValue.setText(String.format("%d cm/s", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (progress < 10) progress = 10;
                com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance().setReturnSpeed(progress);
            }
        });
        
        // Fullscreen Switch
        android.widget.Switch switchFullscreen = view.findViewById(R.id.switch_fullscreen);
        // Use AppSettingsManager instead of SharedPreferences
        com.weigao.robot.control.manager.AppSettingsManager settingsManager = com.weigao.robot.control.manager.AppSettingsManager.getInstance();
        boolean isFullscreen = settingsManager.isFullScreen();
        switchFullscreen.setChecked(isFullscreen);

        switchFullscreen.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsManager.setFullScreen(isChecked);
            applyFullScreen(isChecked);
        });
        
        return view;
    }

    private void applyFullScreen(boolean enable) {
        if (getActivity() == null) return;
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
