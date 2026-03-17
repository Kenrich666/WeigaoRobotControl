package com.weigao.robot.control.ui.main;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;

public class PositioningFailedActivity extends AppCompatActivity {

    private static final String TAG = "PositionFailedActivity";

    private IRobotStateService robotStateService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_positioning_failed);

        robotStateService = ServiceManager.getInstance().getRobotStateService();
        Log.d(TAG, "【定位】进入定位失败页");

        Button btnRetry = findViewById(R.id.btn_retry);
        Button btnBack = findViewById(R.id.btn_back);

        btnRetry.setOnClickListener(v -> handleRetry(btnRetry));
        btnBack.setOnClickListener(v -> finish());
    }

    private void handleRetry(Button btnRetry) {
        if (robotStateService == null) {
            Log.e(TAG, "【定位】失败页点击重试，但定位服务未连接");
            Toast.makeText(this, "服务未连接", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "【定位】失败页点击重试，重新发起定位");
        btnRetry.setEnabled(false);
        btnRetry.setText("正在定位...");

        robotStateService.performLocalization(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    if (!isActivityAlive()) {
                        Log.d(TAG, "【定位】失败页重试成功回调到达，但页面已销毁，忽略");
                        return;
                    }
                    Log.d(TAG, "【定位】失败页重试定位成功，1秒后关闭失败页");
                    Toast.makeText(PositioningFailedActivity.this, "定位成功", Toast.LENGTH_SHORT).show();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (!isActivityAlive()) {
                            return;
                        }
                        setResult(RESULT_OK);
                        finish();
                    }, 1000);
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> {
                    if (!isActivityAlive()) {
                        Log.d(TAG, "【定位】失败页重试失败回调到达，但页面已销毁，忽略");
                        return;
                    }
                    Log.e(TAG, "【定位】失败页重试定位失败: " + error.getMessage());
                    Toast.makeText(
                            PositioningFailedActivity.this,
                            "定位失败: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnRetry.setEnabled(true);
                    btnRetry.setText("重试");
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
