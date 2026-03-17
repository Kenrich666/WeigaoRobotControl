package com.weigao.robot.control.ui.main;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;

public class PositioningActivity extends AppCompatActivity {

    private static final String TAG = "PositioningActivity";
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_positioning);

        tvStatus = findViewById(R.id.tv_status);
        Log.d(TAG, "【定位】进入开机定位页，开始执行真正定位校验");
        checkPositioning();
    }

    private void checkPositioning() {
        if (tvStatus != null) {
            tvStatus.setText("正在进行开机定位...");
        }
        Log.d(TAG, "【定位】开机定位页发起定位请求");

        IRobotStateService service = ServiceManager.getInstance().getRobotStateService();
        if (service == null) {
            Log.e(TAG, "【定位】开机定位页获取定位服务失败");
            if (tvStatus != null) {
                tvStatus.setText("服务未就绪，请稍后重试");
            }
            return;
        }

        service.performLocalization(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                runOnUiThread(() -> {
                    if (!isActivityAlive()) {
                        Log.d(TAG, "【定位】开机定位成功回调到达，但页面已销毁，忽略");
                        return;
                    }
                    Log.d(TAG, "【定位】开机定位成功，3秒后关闭定位页");
                    if (tvStatus != null) {
                        tvStatus.setText("定位成功");
                    }
                    Toast.makeText(PositioningActivity.this, "定位成功", Toast.LENGTH_SHORT).show();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (isActivityAlive()) {
                            finish();
                        }
                    }, 3000);
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> {
                    if (!isActivityAlive()) {
                        Log.d(TAG, "【定位】开机定位失败回调到达，但页面已销毁，忽略");
                        return;
                    }
                    Log.e(TAG, "【定位】开机定位失败，跳转失败页: " + error.getMessage());
                    if (tvStatus != null) {
                        tvStatus.setText("定位失败: " + error.getMessage());
                    }
                    startActivity(new Intent(PositioningActivity.this, PositioningFailedActivity.class));
                    finish();
                });
            }
        });
    }

    private boolean isActivityAlive() {
        return !isFinishing() && !isDestroyed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            com.weigao.robot.control.app.WeigaoApplication.applyFullScreen(this);
        }
    }
}
