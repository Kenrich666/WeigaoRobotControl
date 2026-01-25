package com.weigao.robot.control.ui.main;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.weigao.robot.control.R;
// 返航页面

public class ReturnActivity extends AppCompatActivity {
//  回到原点
    private LinearLayout llControls;
    private TextView tvHint;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return);

        // 1. 初始化控件
        View rootLayout = findViewById(R.id.root_layout);
        llControls = findViewById(R.id.ll_controls);
        tvHint = findViewById(R.id.tv_hint);
        Button btnContinue = findViewById(R.id.btn_continue);
        Button btnEnd = findViewById(R.id.btn_end);

        // 2. 初始化手势检测器（监听双击）
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // 双击屏幕：显示控制按钮，隐藏提示文字
                showControls(true);
                return true;
            }
        });

        // 3. 为根布局设置触摸监听
        rootLayout.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true; // 消费掉事件，确保后续触摸能被捕捉
        });

        // 4. 按钮点击事件
        btnContinue.setOnClickListener(v -> {
            // 点击“继续”：隐藏按钮，恢复双击提示
            showControls(false);
        });

        btnEnd.setOnClickListener(v -> {
            // 点击“结束”：根据需求关闭页面或停止任务
            finish();
        });
    }

    /**
     * 切换显示状态
     * @param showVisible 是否显示按钮
     */
    private void showControls(boolean showVisible) {
        if (showVisible) {
            llControls.setVisibility(View.VISIBLE);
            tvHint.setVisibility(View.GONE);
        } else {
            llControls.setVisibility(View.GONE);
            tvHint.setVisibility(View.VISIBLE);
        }
    }
}