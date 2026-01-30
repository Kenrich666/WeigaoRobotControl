package com.weigao.robot.control.ui.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.weigao.robot.control.R;

import java.util.ArrayList;
import java.util.List;
// 密码验证
public class PasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private StringBuilder currentPassword = new StringBuilder();
    private int maxPasswordLength = 6; // Default 6
    private static final String DEFAULT_PASSWORD = "1234"; // Placeholder

    private TextView tvTitle;
    private List<ImageView> dots = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_input);

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

        // Initial state
        updateDotsVisibility();
    }

    private void setupListeners() {
        // Keypad listeners
        int[] btnIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int id : btnIds) {
            View btn = findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(this);
            }
        }
        
        // Function buttons
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
            String digit = v.getTag().toString();
            handleInput(digit);
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
                if (i < currentPassword.length()) {
                    dots.get(i).setImageResource(R.drawable.icon_dot_filled);
                } else {
                    dots.get(i).setImageResource(R.drawable.icon_dot_empty);
                }
            }
        }
    }

    private void updateDotsVisibility() {
        for (int i = 0; i < dots.size(); i++) {
            if (i < maxPasswordLength) {
                dots.get(i).setVisibility(View.VISIBLE);
            } else {
                dots.get(i).setVisibility(View.GONE);
            }
        }
    }

    private void initPasswordSettings() {
        // Ensure standard 6-digit length to match SecurityService default
        maxPasswordLength = 6;
        updateDotsVisibility();
    }

    private void verifyPassword() {
        String input = currentPassword.toString();
        
        com.weigao.robot.control.service.ISecurityService securityService = 
                com.weigao.robot.control.service.ServiceManager.getInstance().getSecurityService();

        if (securityService != null) {
            securityService.verifyPassword(input, new com.weigao.robot.control.callback.IResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean isValid) {
                    runOnUiThread(() -> {
                        if (isValid) {
                            Toast.makeText(PasswordActivity.this, "密码正确", Toast.LENGTH_SHORT).show();
                            
                            // Check for target intent to launch
                            Intent targetIntent = (Intent) getIntent().getParcelableExtra("target_intent");
                            if (targetIntent != null) {
                                startActivity(targetIntent);
                            }
                            
                            setResult(RESULT_OK);
                            finish();
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
        } else {
             // Fallback if service is not available (though it should be)
             Toast.makeText(this, "服务不可用", Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePasswordError() {
        Toast.makeText(PasswordActivity.this, "密码错误", Toast.LENGTH_SHORT).show();
        // Clear input after a short delay
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
