package com.weigao.robot.control.ui.auth;

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

import java.util.ArrayList;
import java.util.List;
// 密码验证
public class PasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private StringBuilder currentPassword = new StringBuilder();
    private int maxPasswordLength = 4; // Default 4
    private static final int SUPER_PASSWORD_LENGTH = 6;
    private static final String DEFAULT_PASSWORD = "1234"; // Placeholder
    private static final String SUPER_PASSWORD = "123456"; // Placeholder

    private TextView tvTitle;
    private List<ImageView> dots = new ArrayList<>();
    
    // Secret trigger logic
    private int titleClickCount = 0;
    private long lastTitleClickTime = 0;
    private static final int CLICK_THRESHOLD = 8;
    private static final long CLICK_TIME_WINDOW = 1000; // 1 second window to chain clicks? No, 500ms between clicks is better.

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_input);

        initViews();
        setupListeners();
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
        findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            if (currentPassword.length() > 0) {
                currentPassword.deleteCharAt(currentPassword.length() - 1);
                updateDots();
            } else {
                finish(); // Close activity
            }
        });
        


        // Secret trigger on Title
        tvTitle.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTitleClickTime < 500) {
                titleClickCount++;
            } else {
                titleClickCount = 1;
            }
            lastTitleClickTime = currentTime;

            if (titleClickCount >= CLICK_THRESHOLD) {
                toggleSuperPasswordMode();
                titleClickCount = 0;
            }
        });
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

    private void verifyPassword() {
        String input = currentPassword.toString();
        boolean isValid = false;
        
        if (maxPasswordLength == 4) {
            isValid = input.equals(DEFAULT_PASSWORD);
        } else {
            isValid = input.equals(SUPER_PASSWORD);
        }

        if (isValid) {
            Toast.makeText(this, "Password Correct!", Toast.LENGTH_SHORT).show();
            // TODO: Proceed to next screen or unlock
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Wrong Password", Toast.LENGTH_SHORT).show();
            // Shake animation or visual feedback could be added here
            
            // Clear input after a short delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                currentPassword.setLength(0);
                updateDots();
            }, 500);
        }
    }

    private void toggleSuperPasswordMode() {
        if (maxPasswordLength == 4) {
            maxPasswordLength = SUPER_PASSWORD_LENGTH;
            tvTitle.setText("输入超级密码");
            Toast.makeText(this, "Switched to Super Password Mode", Toast.LENGTH_SHORT).show();
        } else {
            // Optional: Toggle back? Usually hidden modes don't toggle back easily or irrelevant.
            // Let's keep it one way or toggle back for testing.
             maxPasswordLength = 4;
             tvTitle.setText("输入密码");
        }
        currentPassword.setLength(0);
        updateDotsVisibility();
        updateDots();
    }
}
