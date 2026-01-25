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
                        Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_PHONE_STATE
                    },
                    REQUEST_CODE_PERMISSIONS);
        } else {
            initRobotSDK();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 简单判断，实际应检查每个权限
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initRobotSDK();
            } else {
                Log.e(TAG, "Permission denied: READ_EXTERNAL_STORAGE");
                // 可以添加弹窗提示用户手动开启权限
            }
        }
    }

    private void initRobotSDK() {
        // 使用 WeigaoApplication 进行统一初始化（包含配置参数）
        com.weigao.robot.control.app.WeigaoApplication.getInstance().initializeSdk();
    }
}
