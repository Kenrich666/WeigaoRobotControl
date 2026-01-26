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

        SeekBar seekBar = view.findViewById(R.id.seekbar_led_brightness);
        TextView brightnessValue = view.findViewById(R.id.tv_brightness_value);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                brightnessValue.setText(String.format("%d%%", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        return view;
    }
}
