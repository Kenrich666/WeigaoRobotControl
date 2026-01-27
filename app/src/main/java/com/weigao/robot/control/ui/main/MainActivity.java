package com.weigao.robot.control.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.keenon.sdk.external.PeanutSDK;
import com.weigao.robot.control.R;
import com.weigao.robot.control.ui.auth.PasswordActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btn_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent targetIntent = new Intent(MainActivity.this, SettingsActivity.class);
                Intent passwordIntent = new Intent(MainActivity.this, PasswordActivity.class);
                passwordIntent.putExtra("target_intent", targetIntent);
                startActivity(passwordIntent);
            }
        });

        findViewById(R.id.btn_item_delivery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent targetIntent = new Intent(MainActivity.this, DeliveryActivity.class);
                targetIntent.putExtra("delivery_type", "物品配送");

                Intent passwordIntent = new Intent(MainActivity.this, PasswordActivity.class);
                passwordIntent.putExtra("target_intent", targetIntent);
                startActivity(passwordIntent);
            }
        });

        findViewById(R.id.btn_loop_delivery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent targetIntent = new Intent(MainActivity.this, CircularDeliveryActivity.class);
                targetIntent.putExtra("delivery_type", "循环配送");

                Intent passwordIntent = new Intent(MainActivity.this, PasswordActivity.class);
                passwordIntent.putExtra("target_intent", targetIntent);
                startActivity(passwordIntent);
            }
        });

        requestPermission();
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_CODE_PERMISSIONS);
        } else {
            Log.i(TAG, "Permissions already granted.");
            initRobotSDK();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted && grantResults.length > 0) {
                Log.i(TAG, "Permissions granted.");
                initRobotSDK();
            } else {
                Log.e(TAG, "Permission denied. Some permissions were not granted.");
                // 可以添加弹窗提示用户手动开启权限
            }
        }
    }

    private void initRobotSDK() {
        // 权限已获取，初始化设置管理器
        com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance();
        com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance();
        // 尝试初始化 App 设置（如果之前失败）
        try {
            com.weigao.robot.control.manager.AppSettingsManager.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Failed to init AppSettingsManager", e);
        }

        com.weigao.robot.control.app.WeigaoApplication app = com.weigao.robot.control.app.WeigaoApplication
                .getInstance();

        app.setSdkInitListener(new com.weigao.robot.control.app.WeigaoApplication.SdkInitListener() {
            @Override
            public void onSdkInitSuccess() {
                runOnUiThread(() -> {
                    Log.i(TAG, "SDK初始化成功，弹出定位窗口");
                    Intent intent = new Intent(MainActivity.this, PositioningActivity.class);
                    startActivity(intent);
                });
            }

            @Override
            public void onSdkInitError(int errorCode) {
                runOnUiThread(() -> {
                    android.widget.Toast
                            .makeText(MainActivity.this, "SDK初始化失败: " + errorCode, android.widget.Toast.LENGTH_LONG)
                            .show();
                });
            }
        });

        app.initializeSdk();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        com.weigao.robot.control.app.WeigaoApplication.getInstance().setSdkInitListener(null);
    }
}
