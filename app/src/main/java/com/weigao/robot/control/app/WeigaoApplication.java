package com.weigao.robot.control.app;

import android.app.Application;
import android.util.Log;

import com.keenon.sdk.external.PeanutSDK;


public class WeigaoApplication extends Application {
    private static final String TAG = "WeigaoApp";

    @Override
    public void onCreate() {
        super.onCreate();
        initRobotSDK();
    }

    private void initRobotSDK() {
        // 初始化 SDK
        PeanutSDK.getInstance().init(this, errorCode -> {
            // errorCode = 0 表示成功
            Log.i(TAG, "SDK Init Result: " + errorCode);
        });
    }
}