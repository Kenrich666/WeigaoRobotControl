package com.weigao.robot.control.ui.main.fragment;

import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IChargerCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.SdkErrorCode;
import com.weigao.robot.control.manager.LowBatteryAutoChargeSettingsManager;
import com.weigao.robot.control.manager.UVDisinfectionManager;
import com.weigao.robot.control.manager.WorkScheduleService;
import com.weigao.robot.control.manager.WorkScheduleSettingsManager;
import com.weigao.robot.control.model.ChargerInfo;
import com.weigao.robot.control.model.WorkSchedule;
import com.weigao.robot.control.service.IChargerService;
import com.weigao.robot.control.service.ServiceManager;

import java.util.List;

/**
 * 充电设置页。
 */
public class ChargerSettingsFragment extends Fragment {

    private static final String TAG = "ChargerSettingsFragment";

    private ImageView batteryIcon;
    private TextView batteryPercentage;
    private TextView helpText;
    private Button chargeNowButton;
    private Button manualChargeButton;
    private Button stopChargeButton;
    private ProgressBar batteryProgress;
    private TextView tvChargingStatus;

    private SwitchCompat switchLowBatteryAutoCharge;
    private SeekBar seekbarLowBatteryThreshold;
    private TextView tvLowBatteryThresholdValue;

    private MaterialCardView cardUvDisinfection;
    private TextView tvUvStatus;
    private TextView tvUvCountdown;
    private Button btnStopUv;

    private IChargerService chargerService;
    private int currentBatteryLevel = 0;

    private LinearLayout scheduleContainer;
    private View btnAddSchedule;

    private static final int[] DAY_BUTTON_IDS = {
            R.id.btn_day_mon, R.id.btn_day_tue, R.id.btn_day_wed,
            R.id.btn_day_thu, R.id.btn_day_fri, R.id.btn_day_sat, R.id.btn_day_sun
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_charger_settings, container, false);

        chargerService = ServiceManager.getInstance().getChargerService();
        initViews(view);
        setupListeners();
        loadLowBatterySettings();
        updateBatteryUI(null);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (chargerService != null) {
            chargerService.registerCallback(chargerCallback);
            refreshBatteryStatus();
        }
        UVDisinfectionManager.getInstance().setStateChangeListener(uvStateListener);
        loadLowBatterySettings();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (chargerService != null) {
            chargerService.unregisterCallback(chargerCallback);
        }
        UVDisinfectionManager.getInstance().removeStateChangeListener();
    }

    private final UVDisinfectionManager.OnStateChangeListener uvStateListener = (isDisinfecting, remainingMs) -> {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        getActivity().runOnUiThread(() -> {
            if (isDisinfecting) {
                cardUvDisinfection.setVisibility(View.VISIBLE);
                tvUvStatus.setText("紫外灯消毒中");
                int minutes = (int) (remainingMs / 1000 / 60);
                int seconds = (int) (remainingMs / 1000 % 60);
                tvUvCountdown.setText(String.format("剩余 %02d:%02d", minutes, seconds));
            } else {
                tvUvStatus.setText("消毒已完成");
                tvUvCountdown.setText("");
                new android.os.Handler().postDelayed(() -> {
                    if (cardUvDisinfection != null) {
                        cardUvDisinfection.setVisibility(View.GONE);
                    }
                }, 2000);
            }
        });
    };

    private final IChargerCallback chargerCallback = new IChargerCallback() {
        @Override
        public void onChargerInfoChanged(int event, ChargerInfo chargerInfo) {
            if (getActivity() != null && chargerInfo != null) {
                getActivity().runOnUiThread(() -> updateBatteryUI(chargerInfo));
            }
        }

        @Override
        public void onChargerStatusChanged(int status) {
            Log.d(TAG, "onChargerStatusChanged: " + status);
        }

        @Override
        public void onChargerError(int errorCode) {
            String errorDesc = SdkErrorCode.getErrorDescription(errorCode);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "充电服务异常: " + errorDesc, Toast.LENGTH_SHORT).show();
                    tvChargingStatus.setText("异常: " + errorDesc);
                    tvChargingStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    tvChargingStatus.setVisibility(View.VISIBLE);
                });
            }
        }
    };

    private void refreshBatteryStatus() {
        if (chargerService == null) {
            return;
        }

        chargerService.getChargerInfo(new IResultCallback<ChargerInfo>() {
            @Override
            public void onSuccess(ChargerInfo result) {
                if (getActivity() != null && result != null) {
                    getActivity().runOnUiThread(() -> updateBatteryUI(result));
                }
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "获取充电信息失败: " + error.getMessage());
            }
        });
    }

    private void initViews(View view) {
        batteryIcon = view.findViewById(R.id.batteryIcon);
        batteryPercentage = view.findViewById(R.id.batteryPercentage);
        batteryProgress = view.findViewById(R.id.batteryProgress);
        chargeNowButton = view.findViewById(R.id.chargeNowButton);
        manualChargeButton = view.findViewById(R.id.manualChargeButton);
        stopChargeButton = view.findViewById(R.id.stopChargeButton);
        helpText = view.findViewById(R.id.helpText);
        tvChargingStatus = view.findViewById(R.id.tvChargingStatus);

        switchLowBatteryAutoCharge = view.findViewById(R.id.switch_low_battery_auto_charge);
        seekbarLowBatteryThreshold = view.findViewById(R.id.seekbar_low_battery_threshold);
        tvLowBatteryThresholdValue = view.findViewById(R.id.tv_low_battery_threshold_value);

        cardUvDisinfection = view.findViewById(R.id.card_uv_disinfection);
        tvUvStatus = view.findViewById(R.id.tv_uv_status);
        tvUvCountdown = view.findViewById(R.id.tv_uv_countdown);
        btnStopUv = view.findViewById(R.id.btn_stop_uv);

        seekbarLowBatteryThreshold.setMax(99);

        initWorkScheduleViews(view);
    }

    private void setupListeners() {
        chargeNowButton.setOnClickListener(v -> startAutoCharge());
        manualChargeButton.setOnClickListener(v -> startManualCharge());
        stopChargeButton.setOnClickListener(v -> stopCharge());

        helpText.setOnClickListener(v -> Toast.makeText(
                getContext(),
                "如果自动回充失败，请手动将机器人推入充电桩",
                Toast.LENGTH_SHORT).show());

        btnStopUv.setOnClickListener(v -> {
            UVDisinfectionManager.getInstance().stopDisinfection();
            Toast.makeText(getContext(), "已手动停止紫外灯消毒", Toast.LENGTH_SHORT).show();
        });

        switchLowBatteryAutoCharge.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }
            int threshold = getThresholdFromSeekBar();
            LowBatteryAutoChargeSettingsManager.getInstance().update(isChecked, threshold);
            updateLowBatterySettingState(isChecked, threshold);
        });

        seekbarLowBatteryThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvLowBatteryThresholdValue.setText((progress + 1) + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int threshold = getThresholdFromSeekBar();
                LowBatteryAutoChargeSettingsManager manager = LowBatteryAutoChargeSettingsManager.getInstance();
                manager.update(switchLowBatteryAutoCharge.isChecked(), threshold);
                updateLowBatterySettingState(switchLowBatteryAutoCharge.isChecked(), threshold);
            }
        });
    }

    private void loadLowBatterySettings() {
        LowBatteryAutoChargeSettingsManager manager = LowBatteryAutoChargeSettingsManager.getInstance();
        boolean enabled = manager.isEnabled();
        int threshold = manager.getThresholdPercent();

        switchLowBatteryAutoCharge.setChecked(enabled);
        seekbarLowBatteryThreshold.setProgress(threshold - 1);
        updateLowBatterySettingState(enabled, threshold);
    }

    private void updateLowBatterySettingState(boolean enabled, int threshold) {
        seekbarLowBatteryThreshold.setEnabled(enabled);
        tvLowBatteryThresholdValue.setEnabled(enabled);
        tvLowBatteryThresholdValue.setAlpha(enabled ? 1.0f : 0.5f);
        tvLowBatteryThresholdValue.setText(threshold + "%");
    }

    private int getThresholdFromSeekBar() {
        return seekbarLowBatteryThreshold.getProgress() + 1;
    }

    private void startAutoCharge() {
        if (chargerService == null) {
            return;
        }

        chargerService.startAutoCharge(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "开始自动回充任务", Toast.LENGTH_SHORT).show();
                        tvChargingStatus.setText("正在前往充电桩...");
                        tvChargingStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                        tvChargingStatus.setVisibility(View.VISIBLE);
                    });
                }
            }

            @Override
            public void onError(ApiError error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast
                            .makeText(getContext(), "启动回充失败: " + error.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void startManualCharge() {
        if (chargerService == null) {
            return;
        }

        chargerService.startManualCharge(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "开始手动充电匹配", Toast.LENGTH_SHORT).show();
                        tvChargingStatus.setText("正在匹配充电桩...");
                        tvChargingStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                        tvChargingStatus.setVisibility(View.VISIBLE);
                    });
                }
            }

            @Override
            public void onError(ApiError error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(
                            getContext(),
                            "启动手动充电失败: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void stopCharge() {
        if (chargerService == null) {
            return;
        }

        chargerService.stopCharge(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "已停止充电", Toast.LENGTH_SHORT).show();
                        tvChargingStatus.setText("充电已停止");
                        tvChargingStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        tvChargingStatus.setVisibility(View.VISIBLE);
                    });
                }
            }

            @Override
            public void onError(ApiError error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(
                            getContext(),
                            "停止充电失败: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void updateBatteryUI(@Nullable ChargerInfo info) {
        int batteryLevel = info != null ? info.getPower() : 0;
        boolean isCharging = info != null && info.isCharging();
        int event = info != null ? info.getEvent() : 0;

        currentBatteryLevel = batteryLevel;
        batteryPercentage.setText(batteryLevel + "%");
        batteryProgress.setProgress(batteryLevel);

        int color;
        if (batteryLevel < 20) {
            color = getResources().getColor(android.R.color.holo_red_dark);
        } else if (batteryLevel < 50) {
            color = getResources().getColor(android.R.color.holo_orange_dark);
        } else {
            color = getResources().getColor(android.R.color.holo_green_light);
        }

        batteryIcon.setColorFilter(color);
        batteryProgress.setProgressTintList(ColorStateList.valueOf(color));

        if (isCharging) {
            chargeNowButton.setEnabled(false);
            chargeNowButton.setText("充电中");
            manualChargeButton.setEnabled(false);

            tvChargingStatus.setText("正在充电");
            tvChargingStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            tvChargingStatus.setVisibility(View.VISIBLE);
        } else {
            chargeNowButton.setEnabled(true);
            chargeNowButton.setText("自动回充 (Auto)");
            manualChargeButton.setEnabled(true);

            String statusText = SdkErrorCode.getEventDescription(event);
            if (statusText.isEmpty()) {
                statusText = "未充电";
            }
            tvChargingStatus.setText(statusText);

            if (event == SdkErrorCode.CHARGER_EVENT_ARRIVE_PILE
                    || event == SdkErrorCode.CHARGER_EVENT_RETRY_GO_PILE
                    || event == SdkErrorCode.CHARGER_EVENT_RETRY_ARRIVE_PILE) {
                tvChargingStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            } else {
                tvChargingStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
            }
            tvChargingStatus.setVisibility(View.VISIBLE);
        }
    }

    private void initWorkScheduleViews(View rootView) {
        scheduleContainer = rootView.findViewById(R.id.schedule_container);
        btnAddSchedule = rootView.findViewById(R.id.btn_add_schedule);

        refreshAllScheduleItems();

        btnAddSchedule.setOnClickListener(v -> {
            WorkScheduleSettingsManager manager = WorkScheduleSettingsManager.getInstance();
            int newIndex = manager.addSchedule();
            if (newIndex < 0) {
                Toast.makeText(getContext(), "已达到最大时段数量", Toast.LENGTH_SHORT).show();
                return;
            }
            addScheduleItemView(manager.getSchedule(newIndex));
            WorkScheduleService.getInstance().rescheduleAll();
        });
    }

    private void refreshAllScheduleItems() {
        if (scheduleContainer == null) {
            return;
        }
        scheduleContainer.removeAllViews();

        WorkScheduleSettingsManager manager = WorkScheduleSettingsManager.getInstance();
        List<WorkSchedule> schedules = manager.getSchedules();
        for (WorkSchedule schedule : schedules) {
            addScheduleItemView(schedule);
        }
    }

    private void refreshAllScheduleItemsState() {
        if (scheduleContainer == null) {
            return;
        }

        WorkScheduleSettingsManager manager = WorkScheduleSettingsManager.getInstance();
        List<WorkSchedule> schedules = manager.getSchedules();

        for (int i = 0; i < scheduleContainer.getChildCount() && i < schedules.size(); i++) {
            View itemView = scheduleContainer.getChildAt(i);
            WorkSchedule schedule = schedules.get(i);

            SwitchCompat switchEnabled = itemView.findViewById(R.id.switch_schedule_enabled);
            if (switchEnabled != null && switchEnabled.isChecked() != schedule.isEnabled()) {
                switchEnabled.setChecked(schedule.isEnabled());
            }

            View llMainContent = itemView.findViewById(R.id.ll_main_content);
            if (llMainContent != null) {
                updateScheduleItemAlpha(llMainContent, schedule.isEnabled());
            }

            boolean[] workDays = schedule.getWorkDays();
            for (int d = 0; d < DAY_BUTTON_IDS.length; d++) {
                MaterialButton dayBtn = itemView.findViewById(DAY_BUTTON_IDS[d]);
                if (dayBtn != null) {
                    updateDayButtonStyle(dayBtn, workDays[d] && schedule.isEnabled());
                }
            }
        }
    }

    private void addScheduleItemView(WorkSchedule initialSchedule) {
        WorkScheduleSettingsManager manager = WorkScheduleSettingsManager.getInstance();
        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_work_schedule, scheduleContainer, false);

        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        itemLp.bottomMargin = (int) (12 * getResources().getDisplayMetrics().density);
        itemView.setLayoutParams(itemLp);

        HorizontalScrollView hsv = itemView.findViewById(R.id.hsv_swipe);
        View btnDelete = itemView.findViewById(R.id.btn_delete_swipe);
        View llMainContent = itemView.findViewById(R.id.ll_main_content);

        itemView.setVisibility(View.INVISIBLE);
        itemView.post(() -> {
            int containerWidth = scheduleContainer.getWidth();
            if (containerWidth > 0 && llMainContent != null) {
                ViewGroup.LayoutParams lp = llMainContent.getLayoutParams();
                lp.width = containerWidth;
                llMainContent.setLayoutParams(lp);
            }
            itemView.setVisibility(View.VISIBLE);
        });

        if (hsv != null && btnDelete != null) {
            hsv.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP
                        || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    int scrollX = hsv.getScrollX();
                    int maxScrollX = btnDelete.getWidth();
                    if (scrollX > maxScrollX / 2) {
                        hsv.post(() -> hsv.smoothScrollTo(maxScrollX, 0));
                    } else {
                        hsv.post(() -> hsv.smoothScrollTo(0, 0));
                    }
                    return true;
                }
                return false;
            });
        }

        SwitchCompat switchEnabled = itemView.findViewById(R.id.switch_schedule_enabled);
        switchEnabled.setChecked(initialSchedule.isEnabled());
        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }

            int currentIndex = scheduleContainer.indexOfChild(itemView);
            if (currentIndex < 0) {
                return;
            }

            if (isChecked) {
                disableAllSchedulesExcept(currentIndex);
            }

            WorkSchedule schedule = manager.getSchedule(currentIndex);
            schedule.setEnabled(isChecked);
            manager.updateSchedule(currentIndex, schedule);
            WorkScheduleService.getInstance().rescheduleAll();
            refreshAllScheduleItemsState();
        });

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                int currentIndex = scheduleContainer.indexOfChild(itemView);
                if (currentIndex >= 0) {
                    manager.removeSchedule(currentIndex);
                    scheduleContainer.removeView(itemView);
                    refreshAllScheduleItemsState();
                    WorkScheduleService.getInstance().rescheduleAll();
                }
            });
        }

        TextView tvStartTime = itemView.findViewById(R.id.tv_start_time);
        tvStartTime.setText(initialSchedule.getStartTime());
        tvStartTime.setOnClickListener(v -> {
            int currentIndex = scheduleContainer.indexOfChild(itemView);
            if (currentIndex < 0) {
                return;
            }
            WorkSchedule schedule = manager.getSchedule(currentIndex);
            if (!schedule.isEnabled()) {
                return;
            }

            showTimePicker(schedule.getStartHour(), schedule.getStartMinute(), (hour, minute) -> {
                String time = String.format("%02d:%02d", hour, minute);
                tvStartTime.setText(time);
                schedule.setStartTime(time);
                manager.updateSchedule(currentIndex, schedule);
                WorkScheduleService.getInstance().rescheduleAll();
            });
        });

        TextView tvEndTime = itemView.findViewById(R.id.tv_end_time);
        tvEndTime.setText(initialSchedule.getEndTime());
        tvEndTime.setOnClickListener(v -> {
            int currentIndex = scheduleContainer.indexOfChild(itemView);
            if (currentIndex < 0) {
                return;
            }
            WorkSchedule schedule = manager.getSchedule(currentIndex);
            if (!schedule.isEnabled()) {
                return;
            }

            showTimePicker(schedule.getEndHour(), schedule.getEndMinute(), (hour, minute) -> {
                String time = String.format("%02d:%02d", hour, minute);
                tvEndTime.setText(time);
                schedule.setEndTime(time);
                manager.updateSchedule(currentIndex, schedule);
                WorkScheduleService.getInstance().rescheduleAll();
            });
        });

        boolean[] workDays = initialSchedule.getWorkDays();
        for (int d = 0; d < DAY_BUTTON_IDS.length; d++) {
            MaterialButton dayBtn = itemView.findViewById(DAY_BUTTON_IDS[d]);
            if (dayBtn == null) {
                continue;
            }

            final int dayIndex = d;
            updateDayButtonStyle(dayBtn, workDays[d] && initialSchedule.isEnabled());
            dayBtn.setOnClickListener(v -> {
                int currentIndex = scheduleContainer.indexOfChild(itemView);
                if (currentIndex < 0) {
                    return;
                }
                WorkSchedule schedule = manager.getSchedule(currentIndex);
                if (!schedule.isEnabled()) {
                    return;
                }

                boolean[] days = schedule.getWorkDays();
                days[dayIndex] = !days[dayIndex];
                schedule.setWorkDays(days);
                manager.updateSchedule(currentIndex, schedule);
                updateDayButtonStyle((MaterialButton) v, days[dayIndex]);
                WorkScheduleService.getInstance().rescheduleAll();
            });
        }

        updateScheduleItemAlpha(llMainContent, initialSchedule.isEnabled());
        scheduleContainer.addView(itemView);
    }

    private void disableAllSchedulesExcept(int exceptIndex) {
        WorkScheduleSettingsManager manager = WorkScheduleSettingsManager.getInstance();
        List<WorkSchedule> schedules = manager.getSchedules();
        for (int i = 0; i < schedules.size(); i++) {
            if (i != exceptIndex && schedules.get(i).isEnabled()) {
                WorkSchedule schedule = schedules.get(i);
                schedule.setEnabled(false);
                manager.updateSchedule(i, schedule);
            }
        }
    }

    private void showTimePicker(int currentHour, int currentMinute, OnTimeSetListener listener) {
        if (getContext() == null) {
            return;
        }
        TimePickerDialog dialog = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, minute) -> listener.onTimeSet(hourOfDay, minute),
                currentHour,
                currentMinute,
                true);
        dialog.show();
    }

    private void updateDayButtonStyle(MaterialButton button, boolean selected) {
        if (selected) {
            button.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.medical_primary)));
            button.setTextColor(getResources().getColor(android.R.color.white));
            button.setStrokeColorResource(R.color.medical_primary);
        } else {
            button.setBackgroundTintList(
                    ColorStateList.valueOf(getResources().getColor(android.R.color.transparent)));
            button.setTextColor(getResources().getColor(R.color.medical_text_secondary));
            button.setStrokeColorResource(R.color.medical_divider);
        }
    }

    private void updateScheduleItemAlpha(View itemView, boolean enabled) {
        itemView.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private interface OnTimeSetListener {
        void onTimeSet(int hour, int minute);
    }
}
