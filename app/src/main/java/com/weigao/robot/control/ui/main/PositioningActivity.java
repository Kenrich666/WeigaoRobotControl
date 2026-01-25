package com.weigao.robot.control.ui.main;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.weigao.robot.control.R;
//定位页面
public class PositioningActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure landscape orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_positioning);

        // Start positioning check
        checkPositioning();
    }

    private void checkPositioning() {
        // TODO: 定位逻辑占位符



        // 定位成功后跳转主页面（模拟延迟2s）
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToMain, 2000);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
