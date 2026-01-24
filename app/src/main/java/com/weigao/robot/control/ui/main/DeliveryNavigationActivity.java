package com.weigao.robot.control.ui.main;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.weigao.robot.control.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeliveryNavigationActivity extends AppCompatActivity {
    
    private static final int REQUEST_CODE_PASSWORD = 1001;

    private TextView tvStatus, currentTaskTextView, tvHint;
    private Button btnPauseEnd, btnContinue;
    private LinearLayout llPauseControls;
    private View rootLayout;

    private List<Map.Entry<Integer, String>> deliveryTasks;
    private int currentTaskIndex = 0;
    private boolean isPaused = false;

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_navigation);

        // Initialize views
        tvStatus = findViewById(R.id.tv_status);
        currentTaskTextView = findViewById(R.id.current_task_textview);
        tvHint = findViewById(R.id.tv_hint);
        llPauseControls = findViewById(R.id.ll_pause_controls);
        btnPauseEnd = findViewById(R.id.btn_pause_end);
        btnContinue = findViewById(R.id.btn_continue);
        rootLayout = findViewById(R.id.root_layout);

        // Get pairings from intent
        HashMap<Integer, String> pairings = (HashMap<Integer, String>) getIntent().getSerializableExtra("pairings");

        if (pairings == null || pairings.isEmpty()) {
            Toast.makeText(this, "没有配送任务", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Sort tasks by layer number
        deliveryTasks = new ArrayList<>(pairings.entrySet());
        Collections.sort(deliveryTasks, Comparator.comparing(entry -> getLayerNumber(entry.getKey())));

        updateTaskText();

        // Setup gesture detector for double tap
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (currentTaskIndex < deliveryTasks.size()) { // Don't pause if all tasks are complete
                    setPauseState(true);
                }
                return true;
            }
        });

        rootLayout.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true; // Consume the event to ensure gesture detection
        });

        // Button click listeners
        btnPauseEnd.setOnClickListener(v -> {
            // Check if we are in "Finished" state (text is "Return Home") or "Paused" state (text is "End Task")
            // Actually the logic uses the same button. 
            // If text is "Return Home", maybe we don't need password? 
            // The requirement says: "In the end task button... need password to finish task".
            // "Return Home" is technically finishing the navigation flow. 
            // But usually "End Task" (during delivery) is the critical one.
            // Let's check the button text or logic.
            // updateTaskText determines if it is finished.
            
            // If currentTaskIndex >= deliveryTasks.size(), it's "Return Home". 
            // If < size, it is "End Task".
            // Requirement: "After jump to finish task button... ensure password correct".
            // It likely refers to the "End" button when pausing.
            
            if (currentTaskIndex >= deliveryTasks.size()) {
                 finish(); // Just return home if already finished normally
            } else {
                 // Verify password to end task prematurely
                 android.content.Intent intent = new android.content.Intent(DeliveryNavigationActivity.this, com.weigao.robot.control.ui.auth.PasswordActivity.class);
                 startActivityForResult(intent, REQUEST_CODE_PASSWORD);
            }
        });
        btnContinue.setOnClickListener(v -> setPauseState(false));
    }

    private void setPauseState(boolean paused) {
        isPaused = paused;
        if (isPaused) {
            tvStatus.setText("已暂停");
            llPauseControls.setVisibility(View.VISIBLE);
            tvHint.setVisibility(View.INVISIBLE);
        } else {
            tvStatus.setText("送物中");
            llPauseControls.setVisibility(View.GONE);
            tvHint.setVisibility(View.VISIBLE);
        }
    }

    private void updateTaskText() {
        if (currentTaskIndex < deliveryTasks.size()) {
            Map.Entry<Integer, String> currentTask = deliveryTasks.get(currentTaskIndex);
            String pointText = currentTask.getValue();
            int totalTasks = deliveryTasks.size();
            currentTaskTextView.setText(String.format("正在前往：%s (第 %d/%d 个)",
                    pointText, currentTaskIndex + 1, totalTasks));
        } else {
            currentTaskTextView.setText("所有任务已完成！");
            tvStatus.setText("已完成");
            tvHint.setVisibility(View.GONE);
            llPauseControls.setVisibility(View.VISIBLE);
            btnContinue.setVisibility(View.GONE);
            btnPauseEnd.setText("返回首页");
            rootLayout.setOnTouchListener(null); // Disable double tap when finished
        }
    }

    private int getLayerNumber(int buttonId) {
        if (buttonId == R.id.l1_button) return 1;
        if (buttonId == R.id.l2_button) return 2;
        if (buttonId == R.id.l3_button) return 3;
        return 0; // Should not happen
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PASSWORD && resultCode == RESULT_OK) {
            finish();
        }
    }
}
