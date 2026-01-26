package com.weigao.robot.control.ui.main;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.weigao.robot.control.R;
import com.weigao.robot.control.ui.main.fragment.AboutMeFragment;
import com.weigao.robot.control.ui.main.fragment.BasicSettingsFragment;
import com.weigao.robot.control.ui.main.fragment.ChargerSettingsFragment;
import com.weigao.robot.control.ui.main.fragment.DeliveryModeFragment;
import com.weigao.robot.control.ui.main.fragment.NotificationSettingsFragment;
import com.weigao.robot.control.ui.main.fragment.PagerSettingsFragment;
import com.weigao.robot.control.ui.main.fragment.RemoteSettingsFragment;
import com.weigao.robot.control.ui.main.fragment.SceneSettingsFragment;
import com.weigao.robot.control.ui.main.fragment.SoundSettingsFragment;
import com.weigao.robot.control.ui.main.fragment.ScheduledTasksFragment;
import com.weigao.robot.control.ui.main.fragment.WifiNetworkFragment;

import java.util.ArrayList;
import java.util.List;
//设置页面
public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private final List<MaterialButton> menuButtons = new ArrayList<>();
    private MaterialButton selectedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btn_back).setOnClickListener(this);

        menuButtons.add(findViewById(R.id.btn_basic_settings));
        menuButtons.add(findViewById(R.id.btn_sound_settings));
        menuButtons.add(findViewById(R.id.btn_scheduled_tasks));
        menuButtons.add(findViewById(R.id.btn_delivery_mode));
        menuButtons.add(findViewById(R.id.btn_notification_settings));
        menuButtons.add(findViewById(R.id.btn_remote_settings));
        menuButtons.add(findViewById(R.id.btn_pager_settings));
        menuButtons.add(findViewById(R.id.btn_charger_settings));
        menuButtons.add(findViewById(R.id.btn_wifi_network));
        menuButtons.add(findViewById(R.id.btn_scene_settings));
        menuButtons.add(findViewById(R.id.btn_about_me));

        for (MaterialButton button : menuButtons) {
            button.setOnClickListener(this);
        }

        // Default to showing basic settings
        showFragment(new BasicSettingsFragment());
        // Default to selecting basic settings button
        setSelectedButton(findViewById(R.id.btn_basic_settings));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_back) {
            finish();
        } else {
            if (v instanceof MaterialButton) {
                setSelectedButton((MaterialButton) v);
            }
            if (id == R.id.btn_basic_settings) {
//               基础设置
                showFragment(new BasicSettingsFragment());
            } else if (id == R.id.btn_sound_settings) {
//                声音设置
                showFragment(new SoundSettingsFragment());
            } else if (id == R.id.btn_scheduled_tasks) {
//                定时任务
                showFragment(new ScheduledTasksFragment());
            } else if (id == R.id.btn_delivery_mode) {
//                配送任务
                showFragment(new DeliveryModeFragment());
            } else if (id == R.id.btn_notification_settings) {
//                通知铃设置
                showFragment(new NotificationSettingsFragment());
            } else if (id == R.id.btn_remote_settings) {
//                远程设置
                showFragment(new RemoteSettingsFragment());
            } else if (id == R.id.btn_pager_settings) {
//                呼叫器设置
                showFragment(new PagerSettingsFragment());
            } else if (id == R.id.btn_charger_settings) {
//                充电器设置
                showFragment(new ChargerSettingsFragment());
            } else if (id == R.id.btn_wifi_network) {
//                wifi设置
                showFragment(new WifiNetworkFragment());
            } else if (id == R.id.btn_scene_settings) {
//                场景设置
                showFragment(new SceneSettingsFragment());
            } else if (id == R.id.btn_about_me) {
//                了解我
                showFragment(new AboutMeFragment());
            }
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_content, fragment)
                .commit();
    }

    private void setSelectedButton(MaterialButton button) {
        if (selectedButton != null) {
            selectedButton.setBackgroundColor(Color.parseColor("#2196F3")); // unselected color
        }
        button.setBackgroundColor(Color.parseColor("#0D47A1")); // selected color
        selectedButton = button;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
