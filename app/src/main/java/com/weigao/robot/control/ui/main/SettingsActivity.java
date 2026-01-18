package com.weigao.robot.control.ui.main;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.weigao.robot.control.R;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btn_back).setOnClickListener(this);
        findViewById(R.id.btn_basic_settings).setOnClickListener(this);
        findViewById(R.id.btn_sound_settings).setOnClickListener(this);
        findViewById(R.id.btn_scheduled_tasks).setOnClickListener(this);
        findViewById(R.id.btn_delivery_mode).setOnClickListener(this);
        findViewById(R.id.btn_notification_settings).setOnClickListener(this);
        findViewById(R.id.btn_remote_settings).setOnClickListener(this);
        findViewById(R.id.btn_pager_settings).setOnClickListener(this);
        findViewById(R.id.btn_charger_settings).setOnClickListener(this);
        findViewById(R.id.btn_wifi_network).setOnClickListener(this);
        findViewById(R.id.btn_scene_settings).setOnClickListener(this);
        findViewById(R.id.btn_about_me).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_back) {
            finish();
        } else if (id == R.id.btn_basic_settings) {
            showToast("基础设置");
        } else if (id == R.id.btn_sound_settings) {
            showToast("声音设置");
        } else if (id == R.id.btn_scheduled_tasks) {
            showToast("定时任务");
        } else if (id == R.id.btn_delivery_mode) {
            showToast("配送模式");
        } else if (id == R.id.btn_notification_settings) {
            showToast("通知铃设置");
        } else if (id == R.id.btn_remote_settings) {
            showToast("远程设置");
        } else if (id == R.id.btn_pager_settings) {
            showToast("呼叫器设置");
        } else if (id == R.id.btn_charger_settings) {
            showToast("充电器设置");
        } else if (id == R.id.btn_wifi_network) {
            showToast("wifi网络");
        } else if (id == R.id.btn_scene_settings) {
            showToast("场景设置");
        } else if (id == R.id.btn_about_me) {
            showToast("了解我");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
