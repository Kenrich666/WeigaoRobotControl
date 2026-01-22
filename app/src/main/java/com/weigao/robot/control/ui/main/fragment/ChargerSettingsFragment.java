package com.weigao.robot.control.ui.main.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.weigao.robot.control.R;

public class ChargerSettingsFragment extends Fragment {

    private ImageView batteryIcon;
    private TextView batteryPercentage;
    private TextView estimatedTime;
    private Button chargeNowButton;
    private TextView helpText;
    private RelativeLayout powerSavingModeLayout;
    private TextView currentModeValue;
    private ProgressBar batteryProgress;
    private ProgressBar batteryProgressBackground;

    // Power saving modes
    private static final String[] POWER_MODES = {
        "标准模式",
        "省电模式",
        "深度省电",
        "超级省电",
        "性能模式"
    };
    
    private static final String[] POWER_MODES_DESC = {
        "正常性能，不限制功能",
        "降低性能，延长续航约30%",
        "大幅降低性能，延长续航约50%",
        "最低性能，最大续航约70%",
        "最高性能，续航时间较短"
    };

    private int currentPowerModeIndex = 2; // Default: 深度省电

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_charger_settings, container, false);
        
        initViews(view);
        setupListeners();
        updateBatteryStatus();
        
        return view;
    }

    private void initViews(View view) {
        batteryIcon = view.findViewById(R.id.batteryIcon);
        batteryPercentage = view.findViewById(R.id.batteryPercentage);
        batteryProgress = view.findViewById(R.id.batteryProgress);
        batteryProgressBackground = view.findViewById(R.id.batteryProgressBackground);
        estimatedTime = view.findViewById(R.id.estimatedTime);
        chargeNowButton = view.findViewById(R.id.chargeNowButton);
        helpText = view.findViewById(R.id.helpText);
        powerSavingModeLayout = view.findViewById(R.id.powerSavingModeLayout);
        currentModeValue = view.findViewById(R.id.currentModeValue);
        
        // Set initial power mode
        currentModeValue.setText(POWER_MODES[currentPowerModeIndex]);
    }

    private void setupListeners() {
        // Charge Now Button Click
        chargeNowButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "开始充电...", Toast.LENGTH_SHORT).show();
            // TODO: Implement charging logic
        });

        // Help Text Click
        helpText.setOnClickListener(v -> {
            Toast.makeText(getContext(), "手动推至充电桩", Toast.LENGTH_SHORT).show();
            // TODO: Show manual charging instructions
        });

        // Power Saving Mode Click - Show Dialog
        powerSavingModeLayout.setOnClickListener(v -> {
            showPowerModeDialog();
        });
    }

    private void showPowerModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("选择省电模式");
        
        // Create custom view for dialog with descriptions
        String[] displayItems = new String[POWER_MODES.length];
        for (int i = 0; i < POWER_MODES.length; i++) {
            displayItems[i] = POWER_MODES[i] + "\n" + POWER_MODES_DESC[i];
        }
        
        builder.setSingleChoiceItems(displayItems, currentPowerModeIndex, (dialog, which) -> {
            currentPowerModeIndex = which;
            currentModeValue.setText(POWER_MODES[which]);
            Toast.makeText(getContext(), "已切换到: " + POWER_MODES[which], Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            
            // Update battery estimation based on mode
            updateBatteryEstimation();
        });
        
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateBatteryStatus() {
        // Simulated battery data - replace with actual battery status
        int batteryLevel = 79;
        int hours = 12;
        int minutes = 38;
        
        batteryPercentage.setText(batteryLevel + "%");
        estimatedTime.setText(hours + "小时" + minutes + "分钟");
        batteryProgress.setProgress(batteryLevel);
        
        // Update battery icon and progress color based on level
        int color;
        if (batteryLevel < 20) {
            color = getResources().getColor(android.R.color.holo_red_dark);
        } else if (batteryLevel < 50) {
            color = getResources().getColor(android.R.color.holo_orange_dark);
        } else {
            color = getResources().getColor(android.R.color.holo_green_light);
        }
        
        batteryIcon.setColorFilter(color);
        batteryProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private void updateBatteryEstimation() {
        // Adjust battery estimation based on power mode
        int baseHours = 12;
        int baseMinutes = 38;
        
        switch (currentPowerModeIndex) {
            case 0: // 标准模式
                // No change
                break;
            case 1: // 省电模式
                baseHours = (int)(baseHours * 1.3);
                break;
            case 2: // 深度省电
                baseHours = (int)(baseHours * 1.5);
                break;
            case 3: // 超级省电
                baseHours = (int)(baseHours * 1.7);
                break;
            case 4: // 性能模式
                baseHours = (int)(baseHours * 0.8);
                break;
        }
        
        estimatedTime.setText(baseHours + "小时" + baseMinutes + "分钟");
    }

    // Method to update battery status from external source
    public void setBatteryStatus(int percentage, int hours, int minutes) {
        if (batteryPercentage != null) {
            batteryPercentage.setText(percentage + "%");
            batteryProgress.setProgress(percentage);
        }
        if (estimatedTime != null) {
            estimatedTime.setText(hours + "小时" + minutes + "分钟");
        }
    }

    // Method to update power saving mode
    public void setPowerSavingMode(String mode) {
        if (currentModeValue != null) {
            currentModeValue.setText(mode);
        }
    }
}
