package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.sdk.api.FdlCmdStatusApi;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.protopack.base.ProtoDev;
import com.keenon.sdk.sensor.calibration.SensorCalibration;
import com.keenon.sdk.sensor.light.LightConfig;
import com.keenon.sdk.sensor.light.SensorLight;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.IProjectionLightService;
import com.weigao.robot.control.service.ServiceManager;

/**
 * 投影灯服务实现类
 * <p>
 * 封装投影灯控制和脚踩检测逻辑，通过投影灯区域触发舱门开关。
 * </p>
 */
public class ProjectionLightServiceImpl implements IProjectionLightService {

    private static final String TAG = "ProjectionLightService";
    private static final String PREFS_NAME = "projection_light_prefs";
    private static final String KEY_ENABLED = "door_control_enabled";

    /** 检测超时时间 (35秒) */
    private static final int DETECTION_TIMEOUT_MS = 35 * 1000;

    /** 轮询间隔 (100ms) */
    private static final int POLL_INTERVAL_MS = 100;

    private final Context context;
    private final Handler handler;
    private final SharedPreferences prefs;

    /** 功能是否启用 */
    private boolean enabled = false;

    /** 是否正在检测 */
    private boolean isDetecting = false;

    /** 检测开始时间 */
    private long detectionStartTime;

    /** 是否首次轮询 */
    private boolean isFirstLoop = false;

    /** 当前舱门状态 (true=已打开, false=已关闭) */
    private boolean doorsOpen = false;

    /** 舱门操作监听器 */
    private OnDoorOperationListener listener;

    public ProjectionLightServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        this.handler = new Handler(Looper.getMainLooper());
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.enabled = prefs.getBoolean(KEY_ENABLED, false);
        Log.d(TAG, "ProjectionLightServiceImpl 已创建, enabled=" + enabled);
    }

    @Override
    public void controlProjectionLight(boolean isOpen) {
        Log.d(TAG, "controlProjectionLight: isOpen=" + isOpen);
        try {
            LightConfig config = new LightConfig(ProtoDev.SENSOR_SINGLE_LIGHT_1);
            config.setVer(PeanutConstants.SCM_VER_1);
            config.setType(0);
            config.setUSBDirect(true);

            if (isOpen) {
                config.getBlink().setOnTime(255);
                config.getBlink().setOffTime(0);
            } else {
                config.getBlink().setOffTime(255);
                config.getBlink().setOnTime(0);
            }

            SensorLight.getInstance().play(config);
            Log.i(TAG, "投影灯已" + (isOpen ? "开启" : "关闭"));
        } catch (Exception e) {
            Log.e(TAG, "controlProjectionLight 异常", e);
        }
    }

    @Override
    public void startDoorControlDetection() {
        if (!enabled) {
            Log.d(TAG, "功能未启用，跳过检测");
            return;
        }

        if (isDetecting) {
            Log.d(TAG, "正在检测中，跳过重复启动");
            return;
        }

        Log.i(TAG, "开始脚踩检测...");
        isDetecting = true;

        // 开启投影灯
        controlProjectionLight(true);

        // 发送开始检测指令
        SensorCalibration.getInstance().fdlCmd("auto_door_cmd", "start", new IDataCallback() {
            @Override
            public void success(String result) {
                Log.i(TAG, "检测指令发送成功: " + result);
                detectionStartTime = System.currentTimeMillis();
                isFirstLoop = true;
                // 开始轮询检测结果
                handler.post(ProjectionLightServiceImpl.this::pollDetectionResult);
            }

            @Override
            public void error(ApiError error) {
                Log.e(TAG, "检测指令发送失败: " + error.toString());
                isDetecting = false;
                controlProjectionLight(false);
            }
        });
    }

    @Override
    public void stopDoorControlDetection() {
        Log.i(TAG, "停止脚踩检测");
        isDetecting = false;
        handler.removeCallbacksAndMessages(null);
        controlProjectionLight(false);
    }

    /**
     * 轮询检测结果
     */
    private void pollDetectionResult() {
        if (!isDetecting) {
            return;
        }

        // 检查超时
        if ((System.currentTimeMillis() - detectionStartTime) > DETECTION_TIMEOUT_MS) {
            Log.w(TAG, "检测超时 (35s), 重新开始...");
            isDetecting = false;
            // 重新开始检测循环
            handler.postDelayed(this::startDoorControlDetection, 500);
            return;
        }

        // 查询状态
        SensorCalibration.getInstance().fdlCmdStatus("auto_door_cmd", new IDataCallback() {
            @Override
            public void success(String result) {
                Log.d(TAG, "检测状态: " + result);
                try {
                    FdlCmdStatusApi.FdlCmdStatusBean info = GsonUtil.gson2Bean(result,
                            FdlCmdStatusApi.FdlCmdStatusBean.class);

                    if (info != null && info.getData() != null) {
                        if (info.getData().isFinish()) {
                            // 首次轮询可能有状态延迟，跳过
                            if (isFirstLoop) {
                                isFirstLoop = false;
                                handler.postDelayed(ProjectionLightServiceImpl.this::pollDetectionResult,
                                        POLL_INTERVAL_MS);
                                return;
                            }

                            int status = info.getData().getStatus();
                            if (status == 1) {
                                // 检测成功 - 用户脚踩了投影区域
                                Log.i(TAG, "检测到脚踩！切换舱门状态");
                                toggleDoors();
                            } else if (status == 2) {
                                Log.w(TAG, "检测超时");
                            }

                            // 重新开始下一轮检测
                            isDetecting = false;
                            handler.postDelayed(ProjectionLightServiceImpl.this::startDoorControlDetection, 1000);
                        } else {
                            // 继续轮询
                            isFirstLoop = false;
                            handler.postDelayed(ProjectionLightServiceImpl.this::pollDetectionResult, POLL_INTERVAL_MS);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析检测结果异常", e);
                    handler.postDelayed(ProjectionLightServiceImpl.this::pollDetectionResult, POLL_INTERVAL_MS);
                }
            }

            @Override
            public void error(ApiError error) {
                Log.e(TAG, "查询检测状态失败: " + error.toString());
                handler.postDelayed(ProjectionLightServiceImpl.this::pollDetectionResult, POLL_INTERVAL_MS);
            }
        });
    }

    /**
     * 切换舱门状态
     */
    private void toggleDoors() {
        IDoorService doorService = ServiceManager.getInstance().getDoorService();
        boolean opening = !doorsOpen;

        // 通知监听器 - 操作开始
        if (listener != null) {
            handler.post(() -> listener.onDoorOperationStart(opening));
        }

        if (opening) {
            Log.i(TAG, "打开所有舱门");
            doorService.openAllDoors(false, new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    doorsOpen = true;
                    Log.i(TAG, "舱门已打开");
                    notifyOperationComplete(true);
                }

                @Override
                public void onError(com.weigao.robot.control.callback.ApiError error) {
                    Log.e(TAG, "打开舱门失败: " + error.getMessage());
                    notifyOperationComplete(false);
                }
            });
        } else {
            Log.i(TAG, "关闭所有舱门");
            doorService.closeAllDoors(new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    doorsOpen = false;
                    Log.i(TAG, "舱门已关闭");
                    notifyOperationComplete(true);
                }

                @Override
                public void onError(com.weigao.robot.control.callback.ApiError error) {
                    Log.e(TAG, "关闭舱门失败: " + error.getMessage());
                    notifyOperationComplete(false);
                }
            });
        }
    }

    private void notifyOperationComplete(boolean success) {
        if (listener != null) {
            handler.post(() -> listener.onDoorOperationComplete(success));
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        Log.d(TAG, "setEnabled: " + enabled);
        this.enabled = enabled;
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();

        if (enabled) {
            startDoorControlDetection();
        } else {
            stopDoorControlDetection();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setOnDoorOperationListener(OnDoorOperationListener listener) {
        this.listener = listener;
    }

    @Override
    public void release() {
        Log.d(TAG, "释放资源");
        stopDoorControlDetection();
        listener = null;
    }
}
