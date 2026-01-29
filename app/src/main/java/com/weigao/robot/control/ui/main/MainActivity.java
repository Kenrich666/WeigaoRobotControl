package com.weigao.robot.control.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.keenon.sdk.external.PeanutSDK;
import com.weigao.robot.control.R;
import com.weigao.robot.control.manager.AppSettingsManager;
import com.weigao.robot.control.manager.CircularDeliverySettingsManager;
import com.weigao.robot.control.manager.ItemDeliverySettingsManager;
import com.weigao.robot.control.service.ISecurityService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.ui.auth.PasswordActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/*
 * 默认配置 (Default Configuration):
 * 1. 速度 (Speed): 50 cm/s
 * 2. 全屏 (Fullscreen): false (显示系统导航栏/Display system navigation bar)
 * 3. 密码 (Password): 123456
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity onCreate - 启动应用");
        setContentView(R.layout.activity_main);
        
        // 尝试应用全屏模式（基于上次保存的设置）
        if (AppSettingsManager.getInstance().isFullScreen()) {
            applyFullScreen();
        }

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
        Log.i(TAG, "检查应用权限...");
        
        List<String> permissionsNeeded = new ArrayList<>();
        
        // 基础权限检查
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // 存储权限逻辑适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要精细化媒体权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else {
            // Android 12 及以下
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            Log.i(TAG, "请求权限: " + permissionsNeeded);
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
        } else {
            // 基础权限已获得，检查 Android 11+ 特殊存储权限
            checkManageStoragePermission();
        }
    }

    private void checkManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Log.i(TAG, "Android 11+ 需要所有文件访问权限，跳转设置...");
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                    startActivityForResult(intent, REQUEST_CODE_PERMISSIONS);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, REQUEST_CODE_PERMISSIONS);
                }
            } else {
                Log.i(TAG, "所有文件访问权限已授权");
                initRobotSDK();
            }
        } else {
            initRobotSDK();
        }
    }

    private static final int REQUEST_CODE_SETTINGS = 102;

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
               Log.i(TAG, "基础权限已授予");
               // 继续检查 Android 11+ 存储权限
               checkManageStoragePermission();
            } else {
                Log.e(TAG, "权限被拒绝，应用可能无法正常工作");
                showPermissionDeniedDialog(false);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Log.i(TAG, "所有文件访问权限设置返回：已授权");
                    initRobotSDK();
                } else {
                    Log.e(TAG, "所有文件访问权限设置返回：未授权");
                    showPermissionDeniedDialog(true);
                }
            }
        } else if (requestCode == REQUEST_CODE_SETTINGS) {
            // 从应用详情设置页返回，重新检查权限
            Log.i(TAG, "从设置页返回，重新检查权限...");
            requestPermission();
        }
    }

    private void showPermissionDeniedDialog(boolean isStorage) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("权限申请")
            .setMessage("应用需要相关权限才能正常运行。由于权限申请已被拒绝，请前往设置页面手动授予权限。")
            .setCancelable(false)
            .setPositiveButton("去设置", (dialog, which) -> {
                if (isStorage) {
                    checkManageStoragePermission();
                } else {
                    // 跳转到应用详情设置页，让用户手动开启权限
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, REQUEST_CODE_SETTINGS);
                }
            })
            .setNegativeButton("退出应用", (dialog, which) -> {
                finish();
                System.exit(0);
            })
            .show();
    }

    private void initRobotSDK() {
        Log.i(TAG, "开始初始化机器人 SDK...");
        
        // 0. 确保基础目录存在，避免 FileNotFoundException
        ensureDirectoriesExist();
        
        // 1. 重新加载所有设置
        // 必须先加载，确认文件是否存在，内存中的 Manager 会读取文件状态
        AppSettingsManager.getInstance().reloadSettings();
        ItemDeliverySettingsManager.getInstance().reloadSettings();
        CircularDeliverySettingsManager.getInstance().reloadSettings();

        com.weigao.robot.control.app.WeigaoApplication app = com.weigao.robot.control.app.WeigaoApplication
                .getInstance();

        app.setSdkInitListener(new com.weigao.robot.control.app.WeigaoApplication.SdkInitListener() {
            @Override
            public void onSdkInitSuccess() {
                runOnUiThread(() -> {
                    Log.i(TAG, "SDK 初始化成功！执行默认配置检查与写入...");
                    
                    // 2. 写入默认配置并应用（仅当配置缺失时）
                    setupDefaultConfigurations();
                    
                    Log.i(TAG, "弹出定位窗口");
                    Intent intent = new Intent(MainActivity.this, com.weigao.robot.control.ui.main.PositioningActivity.class);
                    startActivity(intent);
                });
            }

            @Override
            public void onSdkInitError(int errorCode) {
                Log.e(TAG, "SDK 初始化失败，错误码: " + errorCode);
            }
        });

        app.initializeSdk();
    }

    /**
     * 设置默认配置：全屏、速度、密码
     * 遵循原则：【读取优先】，只有当配置文件不存在时，才写入默认值。
     */
    private void setupDefaultConfigurations() {
        Log.d(TAG, "正在检查配置完整性...");
        
        // 1. 全屏配置检查
        File appSettingsFile = new File(Environment.getExternalStorageDirectory(), "WeigaoRobot/settings/app_settings.json");
        if (!appSettingsFile.exists()) {
            Log.i(TAG, "配置缺失，写入默认全屏设置: false");
            AppSettingsManager.getInstance().setFullScreen(false); // 默认显示导航栏
        } else {
            Log.i(TAG, "全屏配置已存在，跳过默认值写入");
        }
        // 无论是否是默认值，都根据当前 Manager 状态应用一次 UI
        if (AppSettingsManager.getInstance().isFullScreen()) {
            applyFullScreen();
        }

        // 2. 配送速度配置检查 (物品配送)
        File itemSettingsFile = new File(Environment.getExternalStorageDirectory(), "WeigaoRobot/settings/item_delivery_settings.json");
        if (!itemSettingsFile.exists()) {
             Log.i(TAG, "配置缺失，写入默认物品配送速度: 50");
             ItemDeliverySettingsManager.getInstance().setDeliverySpeed(50);
        }

        // 3. 配送速度配置检查 (循环配送)
        File circularSettingsFile = new File(Environment.getExternalStorageDirectory(), "WeigaoRobot/settings/circular_settings.json");
         if (!circularSettingsFile.exists()) {
             Log.i(TAG, "配置缺失，写入默认循环配送速度: 50");
             CircularDeliverySettingsManager.getInstance().setDeliverySpeed(50);
        }

        // 4. 默认密码配置检查
        // SecurityServiceImpl 内部可能有自己的逻辑，但这里我们尽量做一下初次初始化
        ISecurityService securityService = ServiceManager.getInstance().getSecurityService();
        if (securityService != null) {
            File securityConfigFile = new File(Environment.getExternalStorageDirectory(), "WeigaoRobot/config/security_config.json");
            if (!securityConfigFile.exists()) {
                Log.i(TAG, "安全配置缺失，写入默认安全锁定设置");
                securityService.setSecurityLockEnabled(true, null);
                // 默认密码通常由 Service 内部处理，这里主要确保锁功能开启
            }
        }
    }

    /**
     * 确保必要的配置目录存在
     */
    private void ensureDirectoriesExist() {
        String[] dirs = {
            "WeigaoRobot/settings",
            "WeigaoRobot/config",
            "WeigaoRobot/history",
            "WeigaoRobot/routes"
        };
        for (String dirPath : dirs) {
            File dir = new File(Environment.getExternalStorageDirectory(), dirPath);
            if (!dir.exists()) {
                boolean success = dir.mkdirs();
                Log.d(TAG, "创建目录 " + dirPath + (success ? " 成功" : " 失败"));
            }
        }
    }

    /**
     * 应用全屏 UI
     */
    private void applyFullScreen() {
        Log.d(TAG, "执行全屏 UI 适配");
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        com.weigao.robot.control.app.WeigaoApplication.getInstance().setSdkInitListener(null);
    }
}
