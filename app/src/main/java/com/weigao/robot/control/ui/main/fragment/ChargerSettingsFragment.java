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
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.IStateCallback;
import com.weigao.robot.control.callback.SdkErrorCode;
import com.weigao.robot.control.manager.LowBatteryAutoChargeSettingsManager;
import com.weigao.robot.control.manager.UVDisinfectionManager;
import com.weigao.robot.control.manager.WorkScheduleService;
import com.weigao.robot.control.manager.WorkScheduleSettingsManager;
import com.weigao.robot.control.manager.WorkScheduleValidator;
import com.weigao.robot.control.model.ChargingState;
import com.weigao.robot.control.model.RobotState;
import com.weigao.robot.control.model.WorkSchedule;
import com.weigao.robot.control.service.IChargerService;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;

import java.util.List;

/**
 * 闂傚倷鑳堕…鍫㈡崲閹版澘绠犻柟鐗堟緲閺嬩線鏌涘┑鍕姕濠殿垰銈搁弻娑㈠箻濡も偓閹冲繘鎮楅鐔剁箚闁绘劦浜滈埀顒侇殜瀹曟垿鏁愭径濠勫姦?
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
    private IRobotStateService robotStateService;
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
        robotStateService = ServiceManager.getInstance().getRobotStateService();
        initViews(view);
        setupListeners();
        loadLowBatterySettings();
        updateBatteryLevelUI(0);
        updateChargingStateUI(null);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (robotStateService != null) {
            robotStateService.registerCallback(stateCallback);
            refreshChargingStatus();
            refreshBatteryLevel();
        }
        UVDisinfectionManager.getInstance().setStateChangeListener(uvStateListener);
        loadLowBatterySettings();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (robotStateService != null) {
            robotStateService.unregisterCallback(stateCallback);
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
                tvUvStatus.setText("\u7d2b\u5916\u706f\u6d88\u6bd2\u4e2d");
                int minutes = (int) (remainingMs / 1000 / 60);
                int seconds = (int) (remainingMs / 1000 % 60);
                tvUvCountdown.setText(String.format("\u5269\u4f59 %02d:%02d", minutes, seconds));
            } else {
                tvUvStatus.setText("\u6d88\u6bd2\u5df2\u5b8c\u6210");
                tvUvCountdown.setText("");
                new android.os.Handler().postDelayed(() -> {
                    if (cardUvDisinfection != null) {
                        cardUvDisinfection.setVisibility(View.GONE);
                    }
                }, 2000);
            }
        });
    };

    private final IStateCallback stateCallback = new IStateCallback() {
        @Override
        public void onStateChanged(RobotState newState) {
        }

        @Override
        public void onLocationChanged(double x, double y) {
        }

        @Override
        public void onBatteryLevelChanged(int level) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> updateBatteryLevelUI(level));
            }
        }

        @Override
        public void onChargingStateChanged(ChargingState chargingState) {
            if (getActivity() != null && chargingState != null) {
                getActivity().runOnUiThread(() -> updateChargingStateUI(chargingState));
            }
        }

        @Override
        public void onScramButtonPressed(boolean pressed) {
        }
    };

    private void refreshChargingStatus() {
        if (robotStateService == null) {
            return;
        }

        robotStateService.getChargingState(new IResultCallback<ChargingState>() {
            @Override
            public void onSuccess(ChargingState result) {
                if (getActivity() != null && result != null) {
                    getActivity().runOnUiThread(() -> updateChargingStateUI(result));
                }
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "\u83b7\u53d6\u5145\u7535\u72b6\u6001\u5931\u8d25: " + error.getMessage());
            }
        });
    }

    private void refreshBatteryLevel() {
        if (robotStateService == null) {
            return;
        }

        robotStateService.getBatteryLevel(new IResultCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                if (getActivity() != null && result != null) {
                    getActivity().runOnUiThread(() -> updateBatteryLevelUI(result));
                }
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "\u83b7\u53d6\u7535\u91cf\u5931\u8d25: " + error.getMessage());
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
                "\u5982\u679c\u81ea\u52a8\u56de\u5145\u5931\u8d25\uff0c\u8bf7\u624b\u52a8\u5c06\u673a\u5668\u4eba\u63a8\u5165\u5145\u7535\u6869",
                Toast.LENGTH_SHORT).show());

        btnStopUv.setOnClickListener(v -> {
            UVDisinfectionManager.getInstance().stopDisinfection();
            Toast.makeText(getContext(), "\u5df2\u624b\u52a8\u505c\u6b62\u7d2b\u5916\u706f\u6d88\u6bd2", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getContext(), "\u5f00\u59cb\u81ea\u52a8\u56de\u5145\u4efb\u52a1", Toast.LENGTH_SHORT).show();
                        tvChargingStatus.setText("\\u5145\\u7535\\u5df2\\u505c\\u6b62");
                        tvChargingStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                        tvChargingStatus.setVisibility(View.VISIBLE);
                    });
                }
            }

            @Override
            public void onError(ApiError error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast
                            .makeText(getContext(), "\u542f\u52a8\u81ea\u52a8\u56de\u5145\u5931\u8d25: " + error.getMessage(), Toast.LENGTH_SHORT).show());
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
                        Toast.makeText(getContext(), "\u5f00\u59cb\u624b\u52a8\u5145\u7535\u5339\u914d", Toast.LENGTH_SHORT).show();
                        tvChargingStatus.setText("\\u5145\\u7535\\u5df2\\u505c\\u6b62");
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
                            "\u542f\u52a8\u624b\u52a8\u5145\u7535\u5931\u8d25: " + error.getMessage(),
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
                        Toast.makeText(getContext(), "\u5df2\u505c\u6b62\u5145\u7535", Toast.LENGTH_SHORT).show();
                        tvChargingStatus.setText("\u5145\u7535\u5df2\u505c\u6b62");
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
                            "\u505c\u6b62\u5145\u7535\u5931\u8d25: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void updateBatteryLevelUI(int batteryLevel) {
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
    }

    private void updateChargingStateUI(@Nullable ChargingState info) {
        boolean isCharging = info != null && info.isCharging();
        int event = info != null ? info.getEvent() : 0;

        if (isCharging) {
            chargeNowButton.setEnabled(false);
            chargeNowButton.setText("\u5145\u7535\u4e2d");
            manualChargeButton.setEnabled(false);
            tvChargingStatus.setText("\u6b63\u5728\u5145\u7535");
            tvChargingStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            tvChargingStatus.setVisibility(View.VISIBLE);
        } else {
            chargeNowButton.setEnabled(true);
            chargeNowButton.setText("\u81ea\u52a8\u56de\u5145 (Auto)");
            manualChargeButton.setEnabled(true);

            String statusText = SdkErrorCode.isChargerError(event) ? SdkErrorCode.getErrorDescription(event) : SdkErrorCode.getEventDescription(event);
            if (statusText.isEmpty()) {
                statusText = "\u672a\u5145\u7535";
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
                Toast.makeText(getContext(), "\u5df2\u8fbe\u5230\u6700\u5927\u65f6\u6bb5\u6570\u91cf", Toast.LENGTH_SHORT).show();
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
            bindScheduleItemView(itemView, schedule);
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
        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }

            int currentIndex = scheduleContainer.indexOfChild(itemView);
            if (currentIndex < 0) {
                return;
            }

            WorkSchedule candidate = copySchedule(manager.getSchedule(currentIndex));
            candidate.setEnabled(isChecked);
            applyScheduleChange(itemView, currentIndex, candidate);
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
        tvStartTime.setOnClickListener(v -> {
            int currentIndex = scheduleContainer.indexOfChild(itemView);
            if (currentIndex < 0) {
                return;
            }
            WorkSchedule schedule = manager.getSchedule(currentIndex);

            showTimePicker(schedule.getStartHour(), schedule.getStartMinute(), (hour, minute) -> {
                WorkSchedule candidate = copySchedule(manager.getSchedule(currentIndex));
                candidate.setStartTime(String.format("%02d:%02d", hour, minute));
                applyScheduleChange(itemView, currentIndex, candidate);
            });
        });

        TextView tvEndTime = itemView.findViewById(R.id.tv_end_time);
        tvEndTime.setOnClickListener(v -> {
            int currentIndex = scheduleContainer.indexOfChild(itemView);
            if (currentIndex < 0) {
                return;
            }
            WorkSchedule schedule = manager.getSchedule(currentIndex);

            showTimePicker(schedule.getEndHour(), schedule.getEndMinute(), (hour, minute) -> {
                WorkSchedule candidate = copySchedule(manager.getSchedule(currentIndex));
                candidate.setEndTime(String.format("%02d:%02d", hour, minute));
                applyScheduleChange(itemView, currentIndex, candidate);
            });
        });

        boolean[] workDays = initialSchedule.getWorkDays();
        for (int d = 0; d < DAY_BUTTON_IDS.length; d++) {
            MaterialButton dayBtn = itemView.findViewById(DAY_BUTTON_IDS[d]);
            if (dayBtn == null) {
                continue;
            }

            final int dayIndex = d;
            boolean selected = workDays != null && d < workDays.length && workDays[d];
            updateDayButtonStyle(dayBtn, selected);
            dayBtn.setOnClickListener(v -> {
                int currentIndex = scheduleContainer.indexOfChild(itemView);
                if (currentIndex < 0) {
                    return;
                }
                WorkSchedule schedule = manager.getSchedule(currentIndex);

                WorkSchedule candidate = copySchedule(schedule);
                boolean[] days = candidate.getWorkDays();
                days[dayIndex] = !days[dayIndex];
                candidate.setWorkDays(days);
                applyScheduleChange(itemView, currentIndex, candidate);
            });
        }

        bindScheduleItemView(itemView, initialSchedule);
        scheduleContainer.addView(itemView);
    }

    private boolean applyScheduleChange(View itemView, int scheduleIndex, WorkSchedule candidate) {
        WorkScheduleSettingsManager manager = WorkScheduleSettingsManager.getInstance();
        WorkScheduleValidator.ValidationResult result = WorkScheduleValidator.validate(
                manager.getSchedules(), scheduleIndex, candidate);
        if (result != WorkScheduleValidator.ValidationResult.VALID) {
            bindScheduleItemView(itemView, manager.getSchedule(scheduleIndex));
            showScheduleValidationToast(result);
            return false;
        }

        manager.updateSchedule(scheduleIndex, candidate);
        bindScheduleItemView(itemView, candidate);
        WorkScheduleService.getInstance().rescheduleAll();
        return true;
    }

    private void bindScheduleItemView(View itemView, WorkSchedule schedule) {
        if (itemView == null || schedule == null) {
            return;
        }

        SwitchCompat switchEnabled = itemView.findViewById(R.id.switch_schedule_enabled);
        if (switchEnabled != null && switchEnabled.isChecked() != schedule.isEnabled()) {
            switchEnabled.setChecked(schedule.isEnabled());
        }

        TextView tvStartTime = itemView.findViewById(R.id.tv_start_time);
        if (tvStartTime != null) {
            tvStartTime.setText(schedule.getStartTime());
        }

        TextView tvEndTime = itemView.findViewById(R.id.tv_end_time);
        if (tvEndTime != null) {
            tvEndTime.setText(schedule.getEndTime());
        }

        View llMainContent = itemView.findViewById(R.id.ll_main_content);
        if (llMainContent != null) {
            updateScheduleItemAlpha(llMainContent, schedule.isEnabled());
        }

        boolean[] workDays = schedule.getWorkDays();
        for (int d = 0; d < DAY_BUTTON_IDS.length; d++) {
            MaterialButton dayBtn = itemView.findViewById(DAY_BUTTON_IDS[d]);
            if (dayBtn != null) {
                boolean selected = workDays != null && d < workDays.length && workDays[d];
                updateDayButtonStyle(dayBtn, selected);
            }
        }
    }

    private WorkSchedule copySchedule(WorkSchedule source) {
        boolean[] workDays = source.getWorkDays();
        return new WorkSchedule(
                source.isEnabled(),
                source.getStartTime(),
                source.getEndTime(),
                workDays != null ? workDays.clone() : null);
    }

    private void showScheduleValidationToast(WorkScheduleValidator.ValidationResult result) {
        if (getContext() == null) {
            return;
        }

        String message;
        if (result == WorkScheduleValidator.ValidationResult.NO_WORK_DAYS) {
            message = "至少选择一个工作日";
        } else if (result == WorkScheduleValidator.ValidationResult.INVALID_TIME_RANGE) {
            message = "开始时间必须早于结束时间";
        } else if (result == WorkScheduleValidator.ValidationResult.OVERLAP_WITH_ENABLED_SCHEDULE) {
            message = "与已启用时间段冲突";
        } else {
            return;
        }

        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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


