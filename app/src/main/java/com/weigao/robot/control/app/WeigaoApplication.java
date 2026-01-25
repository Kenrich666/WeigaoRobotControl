package com.weigao.robot.control.app;

import android.app.Application;
import android.util.Log;

import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;

import com.weigao.robot.control.service.ServiceManager;

/**
 * 威高机器人控制系统应用入口
 * <p>
 * 负责 Peanut SDK 初始化与生命周期管理。
 * </p>
 */
public class WeigaoApplication extends Application {

    private static final String TAG = "WeigaoApplication";

    /**
     * 应用实例
     */
    private static WeigaoApplication instance;

    /**
     * SDK 是否已初始化
     */
    private boolean sdkInitialized = false;

    /**
     * SDK 初始化监听器
     */
    private SdkInitListener sdkInitListener;

    // ==================== 配置参数（可根据实际情况修改） ====================

    /**
     * 离线鉴权 AppId（需从销售申请）
     */
    private static final String APP_ID = "d8b123262fa3463e835fc15392e07b60";

    /**
     * 离线鉴权 Secret（需从销售申请）
     */
    private static final String APP_SECRET = "nPlQERTP4qIvwJ5MT16y/dDQlY4DRvx/0qahVJEzuYkJRFSQoJM6CZtGLebwINKLAx/kACtCq7UBvt1QCODovm2gq7dsXAK48NrqiEj8bNDBhl/HV12geRHoXVo8pNCKUWHvfPMjy0I/XYP54J8bZYwJS7gRXJoFiDqwPsXMZYs=";

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "WeigaoApplication onCreate");

        // 移除 onCreate 中的初始化，改为由 MainActivity 请求权限后调用
        // initializeSdk();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.i(TAG, "WeigaoApplication onTerminate");

        // 释放资源
        release();
    }

    /**
     * 获取应用实例
     *
     * @return 应用实例
     */
    public static WeigaoApplication getInstance() {
        return instance;
    }

    /**
     * 初始化 Peanut SDK
     * <p>
     * 注意：必须在获取 READ_EXTERNAL_STORAGE 权限后调用
     * </p>
     */
    public void initializeSdk() {
        Log.i(TAG, "开始初始化 Peanut SDK...");

        try {
            // 配置 SDK 参数
            PeanutConfig.getConfig()
                    // 设置通信协议：CoAP (参考 SampleApp)
                    .setLinkType(PeanutConstants.LinkType.COM_COAP)
                    .setLinkCOM(PeanutConstants.COM1)
                    // 设置服务地址
                    .setLinkIP(PeanutConstants.REMOTE_LINK_PROXY)
                    .setLinkPort(5683)
                    .setEmotionLinkCOM(PeanutConstants.COM2)
                    .setDoorLinkCOM(PeanutConstants.COM2)
                    .setConnectionTimeout(PeanutConstants.CONNECTION_TIMEOUT)
                    // 开启日志
                    .enableLog(false)
                    // 设置日志级别
                    .setLogLevel(Log.DEBUG)
                    .setAppId(APP_ID)
                    .setSecret(APP_SECRET)
                    .enableUMLog(false);

            // 初始化 SDK
            PeanutSDK.getInstance().init(getApplicationContext(), mInitListener);

        } catch (Exception e) {
            Log.e(TAG, "SDK 初始化异常", e);
            sdkInitialized = false;
            if (sdkInitListener != null) {
                sdkInitListener.onSdkInitError(-1);
            }
        }
    }

    /**
     * SDK 初始化回调
     */
    private final PeanutSDK.ErrorListener mInitListener = new PeanutSDK.ErrorListener() {
        @Override
        public void onInit(int errorCode) {
            if (errorCode == PeanutSDK.SDK_INIT_SUCCESS) {
                sdkInitialized = true;
                Log.i(TAG, "Peanut SDK 初始化成功");

                // 初始化服务管理器
                ServiceManager.getInstance().initialize(getApplicationContext());

                // 启动 PeanutRuntime (参考 SampleApp)
                PeanutRuntime.getInstance().start(new PeanutRuntime.Listener() {
                    @Override
                    public void onEvent(int event, Object obj) {
                        Log.d(TAG, "PeanutRuntime onEvent: " + event + ", obj: " + obj);
                    }

                    @Override
                    public void onHealth(Object content) {
                        Log.d(TAG, "PeanutRuntime onHealth: " + content);
                    }

                    @Override
                    public void onHeartbeat(Object content) {
                        // 心跳日志可能会很频繁，视情况开启
                        // Log.d(TAG, "PeanutRuntime onHeartbeat: " + content);
                    }
                });

                // 通知监听器
                if (sdkInitListener != null) {
                    sdkInitListener.onSdkInitSuccess();
                }
            } else {
                sdkInitialized = false;
                Log.e(TAG, "Peanut SDK 初始化失败，错误码：" + errorCode);

                // 通知监听器
                if (sdkInitListener != null) {
                    sdkInitListener.onSdkInitError(errorCode);
                }
            }
        }
    };

    /**
     * 释放 SDK 及服务资源
     */
    public void release() {
        Log.i(TAG, "开始释放资源...");

        // 释放服务管理器
        ServiceManager.getInstance().release();

        // 释放 SDK 资源
        try {
            PeanutSDK.getInstance().release();
        } catch (Exception e) {
            Log.e(TAG, "SDK 释放异常", e);
        }

        sdkInitialized = false;
        Log.i(TAG, "资源释放完成");
    }

    /**
     * 检查 SDK 是否已初始化
     *
     * @return true=已初始化
     */
    public boolean isSdkInitialized() {
        return sdkInitialized;
    }

    /**
     * 设置 SDK 初始化监听器
     *
     * @param listener 监听器
     */
    public void setSdkInitListener(SdkInitListener listener) {
        this.sdkInitListener = listener;

        // 如果 SDK 已初始化，立即回调
        if (sdkInitialized && listener != null) {
            listener.onSdkInitSuccess();
        }
    }

    /**
     * SDK 初始化监听器接口
     */
    public interface SdkInitListener {
        /**
         * SDK 初始化成功
         */
        void onSdkInitSuccess();

        /**
         * SDK 初始化失败
         *
         * @param errorCode 错误码
         */
        void onSdkInitError(int errorCode);
    }
}
