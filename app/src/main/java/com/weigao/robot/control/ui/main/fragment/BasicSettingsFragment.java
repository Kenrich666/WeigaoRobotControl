package com.weigao.robot.control.ui.main.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.weigao.robot.control.service.impl.ProjectionDoorService;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.service.impl.ProjectionDoorService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private final List<NavigationNode> availableReturnPoints = new ArrayList<>();
    private NavigationNode defaultOriginPoint;
    private TextView itemReturnPointValueView;
    private TextView circularReturnPointValueView;
    private TextView hospitalReturnPointValueView;

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

        itemReturnPointValueView = view.findViewById(R.id.tv_item_return_point_setting_value);
        circularReturnPointValueView = view.findViewById(R.id.tv_circular_return_point_setting_value);
        hospitalReturnPointValueView = view.findViewById(R.id.tv_hospital_return_point_setting_value);
        TextView hospitalReturnPointValue = hospitalReturnPointValueView;
        View itemReturnPointLayout = view.findViewById(R.id.layout_item_return_point_setting);
        View circularReturnPointLayout = view.findViewById(R.id.layout_circular_return_point_setting);
        View hospitalReturnPointLayout = view.findViewById(R.id.layout_hospital_return_point_setting);
        updateItemReturnPointText(itemReturnPointValueView);
        updateCircularReturnPointText(circularReturnPointValueView);
        updateHospitalReturnPointText(hospitalReturnPointValue);
        itemReturnPointLayout.setOnClickListener(v -> showItemReturnPointDialog(itemReturnPointValueView));
        circularReturnPointLayout.setOnClickListener(v -> showCircularReturnPointDialog(circularReturnPointValueView));
        hospitalReturnPointLayout.setOnClickListener(v -> showHospitalReturnPointDialog(hospitalReturnPointValue));
        loadHospitalOriginPoints(hospitalReturnPointValue);

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
        androidx.appcompat.widget.SwitchCompat switchItemArrivalStayEnabled =
                view.findViewById(R.id.switch_item_arrival_stay_enabled);

        int currentItemStay = com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance()
                .getArrivalStayDuration();
        currentItemStay = clampStay(currentItemStay);
        itemArrivalStaySeekBar.setMax(STAY_PROGRESS_MAX);
        itemArrivalStaySeekBar.setProgress(toStaySeekBarProgress(currentItemStay));
        itemArrivalStayValue.setText(String.format("%d 秒", currentItemStay));

        switchItemArrivalStayEnabled.setChecked(
                com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance()
                        .isArrivalStayEnabled());
        switchItemArrivalStayEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }
            com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance()
                    .setArrivalStayEnabled(isChecked);
        });

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

        SeekBar hospitalArrivalStaySeekBar = view.findViewById(R.id.seekbar_hospital_arrival_stay);
        TextView hospitalArrivalStayValue = view.findViewById(R.id.tv_hospital_arrival_stay_value);
        androidx.appcompat.widget.SwitchCompat switchHospitalArrivalStayEnabled =
                view.findViewById(R.id.switch_hospital_arrival_stay_enabled);

        int currentHospitalStay = com.weigao.robot.control.manager.HospitalDeliverySettingsManager.getInstance()
                .getArrivalStayDuration();
        currentHospitalStay = clampStay(currentHospitalStay);
        hospitalArrivalStaySeekBar.setMax(STAY_PROGRESS_MAX);
        hospitalArrivalStaySeekBar.setProgress(toStaySeekBarProgress(currentHospitalStay));
        hospitalArrivalStayValue.setText(String.format("%d s", currentHospitalStay));

        switchHospitalArrivalStayEnabled.setChecked(
                com.weigao.robot.control.manager.HospitalDeliverySettingsManager.getInstance()
                        .isArrivalStayEnabled());
        switchHospitalArrivalStayEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }
            com.weigao.robot.control.manager.HospitalDeliverySettingsManager.getInstance()
                    .setArrivalStayEnabled(isChecked);
        });

        hospitalArrivalStaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int stay = toStayValue(progress);
                hospitalArrivalStayValue.setText(String.format("%d s", stay));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int stay = toStayValue(seekBar.getProgress());
                com.weigao.robot.control.manager.HospitalDeliverySettingsManager.getInstance()
                        .setArrivalStayDuration(stay);
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
        androidx.appcompat.widget.SwitchCompat switchCircularArrivalStayEnabled =
                view.findViewById(R.id.switch_circular_arrival_stay_enabled);

        int currentCircularStay = com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance()
                .getArrivalStayDuration();
        currentCircularStay = clampStay(currentCircularStay);
        circularArrivalStaySeekBar.setMax(STAY_PROGRESS_MAX);
        circularArrivalStaySeekBar.setProgress(toStaySeekBarProgress(currentCircularStay));
        circularArrivalStayValue.setText(String.format("%d 秒", currentCircularStay));

        switchCircularArrivalStayEnabled.setChecked(
                com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance()
                        .isArrivalStayEnabled());
        switchCircularArrivalStayEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }
            com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance()
                    .setArrivalStayEnabled(isChecked);
        });

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
        androidx.appcompat.widget.SwitchCompat switchPasswordVerification =
                view.findViewById(R.id.switch_password_verification);
        switchPasswordVerification.setChecked(settingsManager.isPasswordVerificationEnabled());
        switchPasswordVerification.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setPasswordVerificationEnabled(isChecked));

        androidx.appcompat.widget.SwitchCompat switchItemProjectionDoor =
                view.findViewById(R.id.switch_item_projection_door);
        androidx.appcompat.widget.SwitchCompat switchCircularProjectionDoor =
                view.findViewById(R.id.switch_circular_projection_door);
        androidx.appcompat.widget.SwitchCompat switchHospitalProjectionDoor =
                view.findViewById(R.id.switch_hospital_projection_door);

        // 首次初始化优先反映硬件真实状态
        switchItemProjectionDoor.setChecked(
                settingsManager.isProjectionDoorEnabled(com.weigao.robot.control.manager.ProjectionDoorMode.ITEM));

        switchCircularProjectionDoor.setChecked(
                settingsManager.isProjectionDoorEnabled(com.weigao.robot.control.manager.ProjectionDoorMode.CIRCULAR));
        switchHospitalProjectionDoor.setChecked(
                settingsManager.isProjectionDoorEnabled(com.weigao.robot.control.manager.ProjectionDoorMode.HOSPITAL));

        switchItemProjectionDoor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed())
                return; // 仅响应当用户主动点击，防止回调循环
            settingsManager.setProjectionDoorEnabled(
                    com.weigao.robot.control.manager.ProjectionDoorMode.ITEM, isChecked);
            // 立即控制投影灯和检测
            if (!settingsManager.isAnyProjectionDoorEnabled()) {
                ProjectionDoorService.getInstance().stopContinuousDetection();
            }
        });

        // 轮询检查投影灯实际状态，实现 实际状态 -> UI 的双向绑定同步
        switchCircularProjectionDoor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }
            settingsManager.setProjectionDoorEnabled(
                    com.weigao.robot.control.manager.ProjectionDoorMode.CIRCULAR, isChecked);
            if (!settingsManager.isAnyProjectionDoorEnabled()) {
                ProjectionDoorService.getInstance().stopContinuousDetection();
            }
        });
        switchHospitalProjectionDoor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }
            settingsManager.setProjectionDoorEnabled(
                    com.weigao.robot.control.manager.ProjectionDoorMode.HOSPITAL, isChecked);
            if (!settingsManager.isAnyProjectionDoorEnabled()) {
                ProjectionDoorService.getInstance().stopContinuousDetection();
            }
        });

        return view;
    }

    private void loadHospitalOriginPoints(TextView valueView) {
        IRobotStateService robotStateService = ServiceManager.getInstance().getRobotStateService();
        if (robotStateService == null) {
            return;
        }

        robotStateService.getDestinationList(new IResultCallback<String>() {
            @Override
            public void onSuccess(String result) {
                new Thread(() -> {
                    List<NavigationNode> loadedPoints = new ArrayList<>();
                    NavigationNode loadedOriginPoint = null;
                    try {
                        JSONObject resultObj = new JSONObject(result);
                        JSONArray jsonArray = resultObj.optJSONArray("data");
                        if (jsonArray != null) {
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject obj = jsonArray.getJSONObject(i);
                                NavigationNode node = new NavigationNode();
                                int id = obj.optInt("id");
                                String name = obj.optString("name");
                                String type = obj.optString("type");
                                if (name == null || name.trim().isEmpty()) {
                                    name = String.valueOf(id);
                                }
                                node.setId(id);
                                node.setName(name);
                                node.setFloor(obj.optInt("floor"));
                                loadedPoints.add(node);
                                if (loadedOriginPoint == null && "origin".equals(type)) {
                                    loadedOriginPoint = node;
                                }
                            }
                        }
                    } catch (JSONException e) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> Toast.makeText(
                                    getContext(),
                                    "返航点位解析失败",
                                    Toast.LENGTH_SHORT).show());
                        }
                    }

                    if (getActivity() != null) {
                        final NavigationNode finalOriginPoint = loadedOriginPoint;
                        getActivity().runOnUiThread(() -> {
                            availableReturnPoints.clear();
                            availableReturnPoints.addAll(loadedPoints);
                            defaultOriginPoint = finalOriginPoint;
                            ensureDefaultReturnPoints();
                            updateItemReturnPointText(itemReturnPointValueView);
                            updateCircularReturnPointText(circularReturnPointValueView);
                            updateHospitalReturnPointText(valueView);
                        });
                    }
                }).start();
            }

            @Override
            public void onError(ApiError error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(
                            getContext(),
                            "获取返航点位失败: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void ensureDefaultReturnPoints() {
        if (defaultOriginPoint == null) {
            return;
        }

        com.weigao.robot.control.manager.ItemDeliverySettingsManager itemSettings =
                com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance();
        if (itemSettings.getReturnPointId() == -1) {
            itemSettings.setReturnPoint(defaultOriginPoint.getId(), defaultOriginPoint.getName());
        }

        com.weigao.robot.control.manager.CircularDeliverySettingsManager circularSettings =
                com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance();
        if (circularSettings.getReturnPointId() == -1) {
            circularSettings.setReturnPoint(defaultOriginPoint.getId(), defaultOriginPoint.getName());
        }

        com.weigao.robot.control.manager.HospitalDeliverySettingsManager hospitalSettings =
                com.weigao.robot.control.manager.HospitalDeliverySettingsManager.getInstance();
        if (hospitalSettings.getReturnPointId() == -1) {
            hospitalSettings.setReturnPoint(defaultOriginPoint.getId(), defaultOriginPoint.getName());
        }
    }

    private void showHospitalReturnPointDialog(TextView valueView) {
        if (getContext() == null) {
            return;
        }
        if (availableReturnPoints.isEmpty()) {
            Toast.makeText(getContext(), "暂无可选返航点位，请先确认地图已配置点位", Toast.LENGTH_SHORT).show();
            loadHospitalOriginPoints(valueView);
            return;
        }

        String[] names = new String[availableReturnPoints.size()];
        int checkedItem = -1;
        int currentId = com.weigao.robot.control.manager.HospitalDeliverySettingsManager.getInstance().getReturnPointId();
        for (int i = 0; i < availableReturnPoints.size(); i++) {
            NavigationNode node = availableReturnPoints.get(i);
            names[i] = node.getName();
            if (node.getId() == currentId) {
                checkedItem = i;
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle("选择医院配送返航点位")
                .setSingleChoiceItems(names, checkedItem, (dialog, which) -> {
                    NavigationNode selectedNode = availableReturnPoints.get(which);
                    com.weigao.robot.control.manager.HospitalDeliverySettingsManager.getInstance()
                            .setReturnPoint(selectedNode.getId(), selectedNode.getName());
                    updateHospitalReturnPointText(valueView);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showItemReturnPointDialog(TextView valueView) {
        if (getContext() == null) {
            return;
        }
        if (availableReturnPoints.isEmpty()) {
            Toast.makeText(getContext(), "暂无可选返航点位，请先确认地图已配置点位", Toast.LENGTH_SHORT).show();
            loadHospitalOriginPoints(hospitalReturnPointValueView);
            return;
        }

        String[] names = new String[availableReturnPoints.size()];
        int checkedItem = -1;
        int currentId = com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance().getReturnPointId();
        for (int i = 0; i < availableReturnPoints.size(); i++) {
            NavigationNode node = availableReturnPoints.get(i);
            names[i] = node.getName();
            if (node.getId() == currentId) {
                checkedItem = i;
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle("选择物品配送返航点位")
                .setSingleChoiceItems(names, checkedItem, (dialog, which) -> {
                    NavigationNode selectedNode = availableReturnPoints.get(which);
                    com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance()
                            .setReturnPoint(selectedNode.getId(), selectedNode.getName());
                    updateItemReturnPointText(valueView);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showCircularReturnPointDialog(TextView valueView) {
        if (getContext() == null) {
            return;
        }
        if (availableReturnPoints.isEmpty()) {
            Toast.makeText(getContext(), "暂无可选返航点位，请先确认地图已配置点位", Toast.LENGTH_SHORT).show();
            loadHospitalOriginPoints(hospitalReturnPointValueView);
            return;
        }

        String[] names = new String[availableReturnPoints.size()];
        int checkedItem = -1;
        int currentId = com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance().getReturnPointId();
        for (int i = 0; i < availableReturnPoints.size(); i++) {
            NavigationNode node = availableReturnPoints.get(i);
            names[i] = node.getName();
            if (node.getId() == currentId) {
                checkedItem = i;
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle("选择循环配送返航点位")
                .setSingleChoiceItems(names, checkedItem, (dialog, which) -> {
                    NavigationNode selectedNode = availableReturnPoints.get(which);
                    com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance()
                            .setReturnPoint(selectedNode.getId(), selectedNode.getName());
                    updateCircularReturnPointText(valueView);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateHospitalReturnPointText(TextView valueView) {
        if (valueView == null) {
            return;
        }

        com.weigao.robot.control.manager.HospitalDeliverySettingsManager settingsManager =
                com.weigao.robot.control.manager.HospitalDeliverySettingsManager.getInstance();
        int currentId = settingsManager.getReturnPointId();
        String currentName = settingsManager.getReturnPointName();

        if (currentId != -1) {
            for (NavigationNode node : availableReturnPoints) {
                if (node.getId() == currentId) {
                    valueView.setText(node.getName());
                    return;
                }
            }
        }

        if (!TextUtils.isEmpty(currentName)) {
            valueView.setText(currentName);
        } else {
            valueView.setText("未设置");
        }
    }

    private void updateItemReturnPointText(TextView valueView) {
        if (valueView == null) {
            return;
        }

        com.weigao.robot.control.manager.ItemDeliverySettingsManager settingsManager =
                com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance();
        int currentId = settingsManager.getReturnPointId();
        String currentName = settingsManager.getReturnPointName();

        if (currentId != -1) {
            for (NavigationNode node : availableReturnPoints) {
                if (node.getId() == currentId) {
                    valueView.setText(node.getName());
                    return;
                }
            }
        }

        if (!TextUtils.isEmpty(currentName)) {
            valueView.setText(currentName);
        } else {
            valueView.setText("未设置");
        }
    }

    private void updateCircularReturnPointText(TextView valueView) {
        if (valueView == null) {
            return;
        }

        com.weigao.robot.control.manager.CircularDeliverySettingsManager settingsManager =
                com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance();
        int currentId = settingsManager.getReturnPointId();
        String currentName = settingsManager.getReturnPointName();

        if (currentId != -1) {
            for (NavigationNode node : availableReturnPoints) {
                if (node.getId() == currentId) {
                    valueView.setText(node.getName());
                    return;
                }
            }
        }

        if (!TextUtils.isEmpty(currentName)) {
            valueView.setText(currentName);
        } else {
            valueView.setText("未设置");
        }
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
