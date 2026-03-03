package com.weigao.robot.control.service.impl;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.sdk.api.FdlCmdStatusApi;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.scmIot.protopack.base.ProtoDev;
import com.keenon.sdk.sensor.calibration.SensorCalibration;
import com.keenon.sdk.sensor.light.LightConfig;
import com.keenon.sdk.sensor.light.SensorLight;

import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.app.WeigaoApplication;
import com.weigao.robot.control.R;

import android.widget.TextView;

/**
 * 投影灯检测开关门服务（单例）
 * <p>
 * 投影灯亮起期间持续进行脚踩检测，检测到脚踩后自动切换舱门状态。
 * 检测完成（成功/超时/错误）后自动重启新一轮检测，形成持续监听循环。
 * </p>
 */
public class ProjectionDoorService {

    private static final String TAG = "ProjectionDoorSvc";

    /** 轮询超时时间（毫秒） */
    private static final long LOOP_TIMEOUT_MS = 35 * 1000;

    /** 轮询间隔（毫秒） */
    private static final long LOOP_INTERVAL_MS = 100;

    /** 检测完成后重启检测的延迟（毫秒） */
    private static final long RESTART_DELAY_MS = 1000;

    // ==================== 单例 ====================

    private static volatile ProjectionDoorService instance;

    public static ProjectionDoorService getInstance() {
        if (instance == null) {
            synchronized (ProjectionDoorService.class) {
                if (instance == null) {
                    instance = new ProjectionDoorService();
                }
            }
        }
        return instance;
    }

    private ProjectionDoorService() {
    }

    // ==================== 回调接口 ====================

    /** UI 层回调（可选），用于显示弹窗等 */
    public interface OnDoorActionListener {
        /**
         * 舱门操作触发
         *
         * @param isOpening true=正在开门，false=正在关门
         */
        void onDoorAction(boolean isOpening);
    }

    // ==================== 成员变量 ====================

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long startLoopTime;
    private boolean isFirstLoop = false;
    private boolean isDetecting = false;
    private boolean isLightOn = false;
    private OnDoorActionListener doorActionListener;

    // ==================== 公开方法 ====================

    /**
     * 启动持续检测（开灯 + 循环检测）
     */
    public void startContinuousDetection() {
        Log.d(TAG, "启动持续检测");
        turnOnLight();
        startDetectionLoop();
    }

    /**
     * 停止持续检测（关灯 + 停止检测）
     */
    public void stopContinuousDetection() {
        Log.d(TAG, "停止持续检测");
        stopDetection();
        turnOffLight();
    }

    /**
     * 暂停检测（移动中调用：关灯 + 停止检测，但不清除监听器）
     */
    public void pauseForMovement() {
        Log.d(TAG, "暂停检测（移动中）");
        stopDetection();
        turnOffLight();
    }

    /**
     * 恢复检测（到达目标点调用：开灯 + 重启检测）
     */
    public void resumeAfterMovement() {
        Log.d(TAG, "恢复检测（到达目标点）");
        turnOnLight();
        startDetectionLoop();
    }

    /**
     * 设置 UI 层回调（用于显示开/关门弹窗）
     */
    public void setDoorActionListener(OnDoorActionListener listener) {
        this.doorActionListener = listener;
    }

    /**
     * 移除 UI 层回调
     */
    public void removeDoorActionListener() {
        this.doorActionListener = null;
    }

    /**
     * 强制确保投影灯关闭（应用启动/退出时调用）
     * 无论当前检测状态如何，都会停止检测并关闭灯光
     */
    public void ensureLightOff() {
        Log.d(TAG, "强制确保投影灯关闭");
        stopDetection();
        turnOffLight();
    }

    public boolean isLightOn() {
        return isLightOn;
    }

    public boolean isDetecting() {
        return isDetecting;
    }

    // ==================== 投影灯控制 ====================

    public void turnOnLight() {
        setProjectionLight(true);
        isLightOn = true;
    }

    public void turnOffLight() {
        setProjectionLight(false);
        isLightOn = false;
    }

    private void setProjectionLight(boolean isOpen) {
        try {
            LightConfig config = new LightConfig(ProtoDev.SENSOR_SINGLE_LIGHT_1);
            config.setVer(PeanutConstants.SCM_VER_1);
            config.setType(0);
            if (isOpen) {
                config.getBlink().setOnTime(255);
            } else {
                config.getBlink().setOffTime(255);
            }
            config.setUSBDirect(true);
            SensorLight.getInstance().play(config);
            Log.d(TAG, "投影灯 " + (isOpen ? "已开启" : "已关闭"));
        } catch (Exception e) {
            Log.e(TAG, "控制投影灯异常", e);
        }
    }

    // ==================== 检测循环 ====================

    /**
     * 仅停止检测轮询（不关灯）
     */
    public void stopDetection() {
        isDetecting = false;
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * 启动一轮检测
     */
    private void startDetectionLoop() {
        if (isDetecting) {
            Log.d(TAG, "检测已在运行，跳过");
            return;
        }
        isDetecting = true;
        startFdlAutoDoorCmd();
    }

    /**
     * 检测完成后自动重启（延迟 RESTART_DELAY_MS）
     */
    private void scheduleRestart() {
        if (isLightOn) {
            Log.d(TAG, "将在 " + RESTART_DELAY_MS + "ms 后重启检测");
            handler.postDelayed(() -> {
                if (isLightOn) {
                    isDetecting = false;
                    startDetectionLoop();
                }
            }, RESTART_DELAY_MS);
        }
    }

    private void startFdlAutoDoorCmd() {
        SensorCalibration.getInstance().fdlCmd("auto_door_cmd", "start", new IDataCallback() {
            @Override
            public void success(String result) {
                Log.d(TAG, "fdl/cmd auto_door_cmd start = " + result);
                startLoopTime = System.currentTimeMillis();
                isFirstLoop = true;
                loopFdlCmdResult();
            }

            @Override
            public void error(com.keenon.sdk.hedera.model.ApiError error) {
                Log.e(TAG, "fdl/cmd auto_door_cmd start error = " + error.toString());
                isDetecting = false;
                scheduleRestart();
            }
        });
    }

    private void loopFdlCmdResult() {
        if (!isDetecting)
            return;

        if ((System.currentTimeMillis() - startLoopTime) > LOOP_TIMEOUT_MS) {
            Log.d(TAG, "投影检测超时，自动重启");
            isDetecting = false;
            scheduleRestart();
            return;
        }

        SensorCalibration.getInstance().fdlCmdStatus("auto_door_cmd", new IDataCallback() {
            @Override
            public void success(String result) {
                if (!isDetecting)
                    return;
                Log.d(TAG, "fdl/checkStatus auto_door_cmd = " + result);
                FdlCmdStatusApi.FdlCmdStatusBean info = GsonUtil.gson2Bean(result,
                        FdlCmdStatusApi.FdlCmdStatusBean.class);
                if (info != null) {
                    if (info.getData().isFinish()) {
                        if (isFirstLoop) {
                            isFirstLoop = false;
                            scheduleNextPoll();
                            return;
                        }
                        if (1 == info.getData().getStatus()) {
                            // 检测成功 - 脚踩到投影灯
                            Log.d(TAG, "脚踩检测成功！执行门控制");
                            handleDoorToggle();
                            isDetecting = false;
                            scheduleRestart();
                        } else if (2 == info.getData().getStatus()) {
                            Log.d(TAG, "SDK检测超时，自动重启");
                            isDetecting = false;
                            scheduleRestart();
                        } else {
                            scheduleNextPoll();
                        }
                    } else {
                        isFirstLoop = false;
                        scheduleNextPoll();
                    }
                } else {
                    Log.e(TAG, "检测数据异常，自动重启");
                    isDetecting = false;
                    scheduleRestart();
                }
            }

            @Override
            public void error(com.keenon.sdk.hedera.model.ApiError error) {
                Log.e(TAG, "fdl/checkStatus error = " + error.toString());
                isDetecting = false;
                scheduleRestart();
            }
        });
    }

    private void scheduleNextPoll() {
        if (isDetecting) {
            handler.postDelayed(this::loopFdlCmdResult, LOOP_INTERVAL_MS);
        }
    }

    // ==================== 门控制 ====================

    /**
     * 检测到脚踩后，自动判断门状态并切换开/关
     */
    private void handleDoorToggle() {
        // 必须在主线程执行，因为 getDoorService() 初始化 PeanutDoor 需要 Looper
        handler.post(() -> {
            IDoorService doorService = ServiceManager.getInstance().getDoorService();
            if (doorService == null) {
                Log.e(TAG, "舱门服务不可用");
                return;
            }

            doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean allClosed) {
                    handler.post(() -> {
                        boolean isOpening = allClosed;
                        IResultCallback<Void> doorOpCallback = new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                // 门操作完成，发送广播通知所有页面刷新门按钮
                                handler.post(() -> {
                                    try {
                                        android.content.Intent intent = new android.content.Intent(
                                                "com.weigao.robot.DOOR_STATE_CHANGED");
                                        androidx.localbroadcastmanager.content.LocalBroadcastManager
                                                .getInstance(WeigaoApplication.getInstance())
                                                .sendBroadcast(intent);
                                        Log.d(TAG, "已发送门状态变化广播");
                                    } catch (Exception e) {
                                        Log.e(TAG, "发送广播异常", e);
                                    }
                                });
                            }

                            @Override
                            public void onError(com.weigao.robot.control.callback.ApiError error) {
                                Log.e(TAG, "门操作失败: " + error.getMessage());
                            }
                        };

                        if (isOpening) {
                            Log.d(TAG, "舱门已关闭，执行开门");
                            doorService.openAllDoors(false, doorOpCallback);
                        } else {
                            Log.d(TAG, "舱门已打开，执行关门");
                            doorService.closeAllDoors(doorOpCallback);
                        }
                        // 在当前前台 Activity 上显示弹窗
                        showDoorOperationDialog(isOpening);
                        // 也通知注册的监听器
                        if (doorActionListener != null) {
                            doorActionListener.onDoorAction(isOpening);
                        }
                    });
                }

                @Override
                public void onError(com.weigao.robot.control.callback.ApiError error) {
                    Log.e(TAG, "查询舱门状态失败: " + error.getMessage());
                }
            });
        });
    }

    // ==================== 全局弹窗 ====================

    private android.app.Dialog currentDialog;

    /**
     * 在当前前台 Activity 上显示开/关门提示弹窗（3秒后自动消失）
     */
    private void showDoorOperationDialog(boolean isOpening) {
        dismissCurrentDialog();

        android.app.Activity activity = WeigaoApplication.getInstance().getCurrentActivity();
        if (activity == null || activity.isFinishing()) {
            Log.w(TAG, "无前台 Activity，无法显示弹窗");
            return;
        }

        try {
            currentDialog = new android.app.Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
            currentDialog.setContentView(R.layout.dialog_door_operation);
            currentDialog.setCancelable(true);

            // 在 show 之前设置 Window 全屏标志
            android.view.Window window = currentDialog.getWindow();
            if (window != null) {
                window.setFlags(
                        android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                window.getDecorView().setSystemUiVisibility(
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                                | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }

            TextView tvTitle = currentDialog.findViewById(R.id.tv_door_operation_title);
            tvTitle.setText(isOpening ? "开门中" : "关门中");

            TextView tvSubtitle = currentDialog.findViewById(R.id.tv_door_operation_subtitle);
            tvSubtitle.setText("请当心");

            currentDialog.show();

            // show 之后清除 NOT_FOCUSABLE 以允许交互
            if (window != null) {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            }

            // 3秒后自动关闭
            handler.postDelayed(this::dismissCurrentDialog, 3000);
        } catch (Exception e) {
            Log.e(TAG, "显示弹窗异常", e);
        }
    }

    private void dismissCurrentDialog() {
        if (currentDialog != null && currentDialog.isShowing()) {
            try {
                currentDialog.dismiss();
            } catch (Exception e) {
                // ignore
            }
        }
        currentDialog = null;

        // 弹窗关闭后重新应用全屏设置到 Activity
        android.app.Activity activity = WeigaoApplication.getInstance().getCurrentActivity();
        if (activity != null && !activity.isFinishing()) {
            WeigaoApplication.applyFullScreen(activity);
        }
    }
}
