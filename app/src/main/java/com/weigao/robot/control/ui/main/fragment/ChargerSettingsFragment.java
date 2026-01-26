package com.weigao.robot.control.ui.main.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
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
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IChargerCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.SdkErrorCode;
import com.weigao.robot.control.model.ChargerInfo;
import com.weigao.robot.control.service.IChargerService;
import com.weigao.robot.control.service.ServiceManager;
// 使用到的函数：
// 1. ServiceManager.getInstance().getChargerService(): 获取充电服务实例
// 2. IChargerService.registerCallback(IChargerCallback): 注册充电状态监听 (回调 onChargerInfoChanged)
// 3. IChargerService.unregisterCallback(IChargerCallback): 注销监听
// 4. IChargerService.getChargerInfo(IResultCallback<ChargerInfo>): 主动获取当前电量和充电状态
// 5. IChargerService.startAutoCharge(IResultCallback<Void>): 下发自动回充指令
// 6. IChargerService.startManualCharge(IResultCallback<Void>): 下发手动充电指令
// 7. IChargerService.stopCharge(IResultCallback<Void>): 下发停止充电指令

// 涉及的 Model：
// - ChargerInfo: 包含 power (电量百分比), isCharging (是否正在充电) 等状态信息
/**
 * 充电设置页面 Fragment
 * 负责展示电池状态、充电进度，并提供自动回充、手动充电、停止充电等操作功能。
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
    private ProgressBar batteryProgressBackground;
    private TextView tvChargingStatus;

    private IChargerService chargerService;


    private int currentBatteryLevel = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_charger_settings, container, false);
        
        // 获取充电服务实例
        chargerService = ServiceManager.getInstance().getChargerService();

        // 初始化 UI 控件
        initViews(view);
        // 设置按钮点击监听器
        setupListeners();
        // 初始假数据更新，真实数据将在 onResume (IChargerCallback) 中刷新
        updateBatteryUI(null);
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 页面可见时注册回调，并主动刷新一次状态
        if (chargerService != null) {
            chargerService.registerCallback(chargerCallback);
            refreshBatteryStatus();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 页面暂停时取消注册，避免内存泄漏以及不必要的后台 UI 更新
        if (chargerService != null) {
            chargerService.unregisterCallback(chargerCallback);
        }
    }

    // 充电服务回调接口实现，用于监听底层充电状态变化
    private final IChargerCallback chargerCallback = new IChargerCallback() {
        @Override
        public void onChargerInfoChanged(int event, ChargerInfo chargerInfo) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (chargerInfo != null) {
                        updateBatteryUI(chargerInfo);
                    }
                });
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

    /**
     * 主动获取最新的电池和充电状态
     */
    private void refreshBatteryStatus() {
        if (chargerService == null) return;
        
        chargerService.getChargerInfo(new IResultCallback<ChargerInfo>() {
            @Override
            public void onSuccess(ChargerInfo result) {
                if (getActivity() != null && result != null) {
                    getActivity().runOnUiThread(() -> {
                        updateBatteryUI(result);
                    });
                }
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "获取充电信息失败: " + error.getMessage());
            }
        });
    }

    /**
     * 初始化所有 View 控件
     */
    private void initViews(View view) {
        batteryIcon = view.findViewById(R.id.batteryIcon);
        batteryPercentage = view.findViewById(R.id.batteryPercentage);
        batteryProgress = view.findViewById(R.id.batteryProgress);
        batteryProgressBackground = view.findViewById(R.id.batteryProgressBackground);

        chargeNowButton = view.findViewById(R.id.chargeNowButton);
        manualChargeButton = view.findViewById(R.id.manualChargeButton);
        stopChargeButton = view.findViewById(R.id.stopChargeButton);
        helpText = view.findViewById(R.id.helpText);
        tvChargingStatus = view.findViewById(R.id.tvChargingStatus);
    }

    private void setupListeners() {
        // "立即充电" (自动回充) 按钮点击事件
        chargeNowButton.setOnClickListener(v -> {
            if (chargerService != null) {
                // 下发自动回充指令
                chargerService.startAutoCharge(new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "开始自动回充任务", Toast.LENGTH_SHORT).show();
                                // 更新状态显示
                                tvChargingStatus.setText("正在前往充电桩...");
                                tvChargingStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                                tvChargingStatus.setVisibility(View.VISIBLE);
                            });
                        }
                    }

                    @Override
                    public void onError(ApiError error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> 
                                Toast.makeText(getContext(), "启动回充失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                });
            }
        });

        // "帮助说明" 点击事件
        helpText.setOnClickListener(v -> {
            // 提示用户手动充电的操作方式
            Toast.makeText(getContext(), "请手动将机器人推至充电桩接触弹片", Toast.LENGTH_SHORT).show();
        });

        // "手动充电" 按钮点击事件
        manualChargeButton.setOnClickListener(v -> {
            if (chargerService != null) {
                // 下发手动充电指令 (通常用于告知系统开始进行手动接触匹配)
                chargerService.startManualCharge(new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "开始手动充电匹配", Toast.LENGTH_SHORT).show();
                                // 更新状态显示
                                tvChargingStatus.setText("正在匹配充电桩...");
                                tvChargingStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                                tvChargingStatus.setVisibility(View.VISIBLE);
                            });
                        }
                    }

                    @Override
                    public void onError(ApiError error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> 
                                Toast.makeText(getContext(), "启动手动充电失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                });
            }
        });

        // "停止充电" 按钮点击事件
        stopChargeButton.setOnClickListener(v -> {
            if (chargerService != null) {
                // 下发停止充电指令
                chargerService.stopCharge(new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "已停止充电", Toast.LENGTH_SHORT).show();
                                // 更新状态显示
                                tvChargingStatus.setText("充电已停止");
                                tvChargingStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                                tvChargingStatus.setVisibility(View.VISIBLE);
                            });
                        }
                    }

                    @Override
                    public void onError(ApiError error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> 
                                Toast.makeText(getContext(), "停止充电失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                });
            }
        });


    }



    /**
     * 更新电池 UI 显示：包括百分比、进度条颜色、充电状态按钮等
     * @param info 充电信息对象
     */
    private void updateBatteryUI(ChargerInfo info) {
        int batteryLevel = info != null ? info.getPower() : 0;
        boolean isCharging = info != null ? info.isCharging() : false;
        int event = info != null ? info.getEvent() : 0;

        currentBatteryLevel = batteryLevel;
        batteryPercentage.setText(batteryLevel + "%");
        
        // 更新进度条
        batteryProgress.setProgress(batteryLevel);
        
        // 根据电量动态设置颜色：
        // < 20%: 红色
        // < 50%: 橙色
        // >= 50%: 绿色
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

        if (isCharging) {
            // 充电中禁用“自动回充”和“手动充电”按钮
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
            
            // 显示详细状态或未充电
            String statusText = SdkErrorCode.getEventDescription(event);
            if (statusText.isEmpty()) {
                statusText = "未充电";
            }
            tvChargingStatus.setText(statusText);
            
            // 如果是正在导航或匹配中，显示为黄色或特定颜色
            if (event == SdkErrorCode.CHARGER_EVENT_ARRIVE_PILE || 
                event == SdkErrorCode.CHARGER_EVENT_RETRY_GO_PILE ||
                event == SdkErrorCode.CHARGER_EVENT_RETRY_ARRIVE_PILE) {
                 tvChargingStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            } else {
                 tvChargingStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
            }
            tvChargingStatus.setVisibility(View.VISIBLE);
        }
    }


}
