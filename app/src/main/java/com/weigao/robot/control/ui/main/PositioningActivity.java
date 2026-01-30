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

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String TAG = "PositioningActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure landscape orientation (optional if handled by manifest/theme)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_positioning);

        // Immediately start positioning (SDK should be initialized by MainActivity)
        checkPositioning();
    }



    private void checkPositioning() {
        // 获取状态展示控件
        android.widget.TextView tvStatus = findViewById(R.id.tv_status);
        if (tvStatus != null) {
            tvStatus.setText("正在进行开机定位...");
        }

        // 获取机器人状态服务
        com.weigao.robot.control.service.IRobotStateService service = 
                com.weigao.robot.control.service.ServiceManager.getInstance().getRobotStateService();
        
        if (service != null) {
            service.performLocalization(new com.weigao.robot.control.callback.IResultCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    runOnUiThread(() -> {
                        if (tvStatus != null) {
                            tvStatus.setText("定位成功");
                        }
                        android.widget.Toast.makeText(PositioningActivity.this, "定位成功", android.widget.Toast.LENGTH_SHORT).show();
                        // 定位成功，延迟3秒关闭定位页面，返回 MainActivity
                        new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 3000);
                    });
                }

                @Override
                public void onError(com.weigao.robot.control.callback.ApiError error) {
                    runOnUiThread(() -> {
                        if (tvStatus != null) {
                            tvStatus.setText("定位失败: " + error.getMessage());
                        }
                    });
                }
            });
        } else {
             if (tvStatus != null) {
                 tvStatus.setText("服务未就绪，请稍后重试");
             }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            com.weigao.robot.control.app.WeigaoApplication.applyFullScreen(this);
        }
    }
}
