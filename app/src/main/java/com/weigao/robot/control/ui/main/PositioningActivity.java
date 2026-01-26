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
        // Ensure landscape orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_positioning);

        // Request permissions first, then init SDK, then check positioning
        requestPermission();
    }

    private void requestPermission() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ||
                androidx.core.content.ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ||
                androidx.core.content.ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ||
                androidx.core.content.ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_PHONE_STATE,
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_CODE_PERMISSIONS);
        } else {
            initRobotSDK();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions,
                                           @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted && grantResults.length > 0) {
                initRobotSDK();
            } else {
                android.util.Log.e(TAG, "Permission denied. Some permissions were not granted.");
                android.widget.TextView tvStatus = findViewById(R.id.tv_status);
                if (tvStatus != null) {
                    tvStatus.setText("无法获取必要权限，定位失败");
                }
            }
        }
    }

    private void initRobotSDK() {
        android.widget.TextView tvStatus = findViewById(R.id.tv_status);
        if (tvStatus != null) {
            tvStatus.setText("正在初始化SDK...");
        }

        com.weigao.robot.control.app.WeigaoApplication app = com.weigao.robot.control.app.WeigaoApplication.getInstance();
        
        // 设置监听器以等待初始化完成
        app.setSdkInitListener(new com.weigao.robot.control.app.WeigaoApplication.SdkInitListener() {
            @Override
            public void onSdkInitSuccess() {
                runOnUiThread(() -> {
                     android.util.Log.i(TAG, "SDK初始化成功，开始定位");
                     checkPositioning();
                });
            }

            @Override
            public void onSdkInitError(int errorCode) {
                runOnUiThread(() -> {
                    if (tvStatus != null) {
                        tvStatus.setText("SDK初始化失败: " + errorCode);
                    }
                });
            }
        });

        // 尝试初始化（如果已经初始化过，上面的监听器会立即回调成功）
        app.initializeSdk();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理监听器，防止内存泄漏和回调到已销毁的页面
        com.weigao.robot.control.app.WeigaoApplication.getInstance().setSdkInitListener(null);
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
                        navigateToMain();
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
        }
    }

    private void navigateToMain() {
        // [关键修复] 跳转前立即清理监听器，防止 MainActivity 初始化 SDK 时再次触发本页面的回调
        com.weigao.robot.control.app.WeigaoApplication.getInstance().setSdkInitListener(null);
        
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
