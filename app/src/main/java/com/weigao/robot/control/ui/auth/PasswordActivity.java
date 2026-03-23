package com.weigao.robot.control.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.weigao.robot.control.R;
import com.weigao.robot.control.manager.AppSettingsManager;
import com.weigao.robot.control.service.ISecurityService;
import com.weigao.robot.control.service.ServiceManager;

import java.util.ArrayList;
import java.util.List;

public class PasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private final StringBuilder currentPassword = new StringBuilder();
    private final List<ImageView> dots = new ArrayList<>();
    private int maxPasswordLength = 6;
    private TextView tvTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_input);

        if (!AppSettingsManager.getInstance().isPasswordVerificationEnabled()) {
            handlePasswordVerified(false);
            return;
        }

        initViews();
        setupListeners();
        initPasswordSettings();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);

        dots.add(findViewById(R.id.dot_1));
        dots.add(findViewById(R.id.dot_2));
        dots.add(findViewById(R.id.dot_3));
        dots.add(findViewById(R.id.dot_4));
        dots.add(findViewById(R.id.dot_5));
        dots.add(findViewById(R.id.dot_6));

        updateDotsVisibility();
    }

    private void setupListeners() {
        int[] buttonIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int id : buttonIds) {
            View button = findViewById(id);
            if (button != null) {
                button.setOnClickListener(this);
            }
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        View btnDelete = findViewById(R.id.btn_delete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                if (currentPassword.length() > 0) {
                    currentPassword.deleteCharAt(currentPassword.length() - 1);
                    updateDots();
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() != null) {
            handleInput(v.getTag().toString());
        }
    }

    private void handleInput(String digit) {
        if (currentPassword.length() < maxPasswordLength) {
            currentPassword.append(digit);
            updateDots();

            if (currentPassword.length() == maxPasswordLength) {
                verifyPassword();
            }
        }
    }

    private void updateDots() {
        for (int i = 0; i < dots.size(); i++) {
            if (i < maxPasswordLength) {
                dots.get(i).setImageResource(
                        i < currentPassword.length() ? R.drawable.icon_dot_filled : R.drawable.icon_dot_empty);
            }
        }
    }

    private void updateDotsVisibility() {
        for (int i = 0; i < dots.size(); i++) {
            dots.get(i).setVisibility(i < maxPasswordLength ? View.VISIBLE : View.GONE);
        }
    }

    private void initPasswordSettings() {
        maxPasswordLength = 6;
        updateDotsVisibility();
    }

    private void verifyPassword() {
        String input = currentPassword.toString();
        ISecurityService securityService = ServiceManager.getInstance().getSecurityService();

        if (securityService == null) {
            Toast.makeText(this, "服务不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        securityService.verifyPassword(input, new com.weigao.robot.control.callback.IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isValid) {
                runOnUiThread(() -> {
                    if (isValid) {
                        handlePasswordVerified(true);
                    } else {
                        handlePasswordError();
                    }
                });
            }

            @Override
            public void onError(com.weigao.robot.control.callback.ApiError error) {
                runOnUiThread(() -> {
                    Toast.makeText(PasswordActivity.this, "验证出错: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    handlePasswordError();
                });
            }
        });
    }

    private void handlePasswordVerified(boolean showToast) {
        if (showToast) {
            Toast.makeText(this, "密码正确", Toast.LENGTH_SHORT).show();
        }

        Intent targetIntent = getIntent().getParcelableExtra("target_intent");
        if (targetIntent != null) {
            startActivity(targetIntent);
        }

        setResult(RESULT_OK);
        finish();
    }

    private void handlePasswordError() {
        Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            currentPassword.setLength(0);
            updateDots();
        }, 500);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            com.weigao.robot.control.app.WeigaoApplication.applyFullScreen(this);
        }
    }
}
