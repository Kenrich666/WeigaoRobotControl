package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.component.runtime.RuntimeInfo;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.IStateCallback;
import com.weigao.robot.control.model.ChargingState;
import com.weigao.robot.control.model.RobotState;
import com.weigao.robot.control.service.IRobotStateService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RobotStateServiceImpl implements IRobotStateService {

    private static final String TAG = "RobotStateServiceImpl";
    private static final String LOG_PREFIX = "【定位】";
    private static final int EVENT_RUNTIME_ERROR = 10000;
    private static final int EVENT_LOCALIZATION_SUCCESS = 10016;
    private static final long LOCALIZATION_TIMEOUT_MS = 15_000L;
    private static final String LOCALIZATION_TIMEOUT_MESSAGE = "定位超时，请重试";
    private static final String LOCALIZATION_FAILED_MESSAGE = "定位失败，请重试";
    private static final String LOCALIZATION_SERVICE_UNAVAILABLE_MESSAGE = "定位服务未就绪，请稍后重试";
    private static final String LOCALIZATION_SERVICE_RELEASED_MESSAGE = "定位服务已释放";

    private final Handler mainHandler;
    private final List<IStateCallback> callbacks = new CopyOnWriteArrayList<>();
    private final RobotState currentState = new RobotState();
    private final Object localizationLock = new Object();
    private final List<IResultCallback<Void>> pendingLocalizationCallbacks = new ArrayList<>();
    private final ChargingRuntimeBridge chargingRuntimeBridge;
    private final boolean ownsChargingRuntimeBridge;

    private PeanutRuntime peanutRuntime;
    private int workMode = WORK_MODE_STANDBY;
    private boolean localizationInProgress = false;
    private Runnable localizationTimeoutRunnable;

    public RobotStateServiceImpl(Context context) {
        this(context, new ChargingRuntimeBridge(context), true);
    }

    public RobotStateServiceImpl(Context context, ChargingRuntimeBridge chargingRuntimeBridge) {
        this(context, chargingRuntimeBridge, false);
    }

    private RobotStateServiceImpl(
            Context context,
            ChargingRuntimeBridge chargingRuntimeBridge,
            boolean ownsChargingRuntimeBridge) {
        this.mainHandler = createMainHandler();
        this.chargingRuntimeBridge = chargingRuntimeBridge;
        this.ownsChargingRuntimeBridge = ownsChargingRuntimeBridge;
        currentState.setChargingState(chargingRuntimeBridge.getCurrentState());
        chargingRuntimeBridge.addListener(chargingStateListener);
        logLocalization("服务创建，准备初始化 PeanutRuntime");
        initPeanutRuntime();
    }

    private void initPeanutRuntime() {
        try {
            peanutRuntime = getPeanutRuntimeInstance();
            if (peanutRuntime == null) {
                logLocalizationError("PeanutRuntime 为空，无法初始化定位服务");
                return;
            }

            peanutRuntime.registerListener(runtimeListener);
            peanutRuntime.start();
            logLocalization("PeanutRuntime 初始化完成，已注册监听并启动");
            updateStateFromSdk();
        } catch (Exception e) {
            logLocalizationError("PeanutRuntime 初始化异常: " + e.getMessage());
            Log.e(TAG, "PeanutRuntime init failed", e);
        }
    }

    private final PeanutRuntime.Listener runtimeListener = new PeanutRuntime.Listener() {
        @Override
        public void onEvent(int event, Object obj) {
            if (event == EVENT_LOCALIZATION_SUCCESS) {
                logLocalization("收到定位成功事件 10016, obj=" + obj);
                completeLocalizationSuccess();
            } else if (event == EVENT_RUNTIME_ERROR && isLocalizationInProgress()) {
                logLocalizationError("定位过程中收到运行时错误事件 10000");
                completeLocalizationError(-1, LOCALIZATION_FAILED_MESSAGE);
            } else if (isLocalizationInProgress()) {
                logLocalization("定位过程中收到事件: event=" + event + ", obj=" + obj);
            }
            updateStateFromSdk();
        }

        @Override
        public void onHealth(Object content) {
            Log.d(TAG, "onHealth: " + content);
        }

        @Override
        public void onHeartbeat(Object content) {
            // RuntimeInfo is updated during heartbeat sync.
        }
    };

    private final ChargingRuntimeBridge.Listener chargingStateListener = chargingState -> {
        if (updateChargingState(chargingState)) {
            notifyChargingStateChanged(chargingState);
            notifyStateChanged(currentState);
        }
    };

    @Override
    public void registerCallback(IStateCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
            synchronized (currentState) {
                callback.onStateChanged(currentState);
                callback.onBatteryLevelChanged(currentState.getBatteryLevel());
                callback.onChargingStateChanged(copyChargingState(currentState.getChargingState()));
            }
        }
    }

    @Override
    public void unregisterCallback(IStateCallback callback) {
        callbacks.remove(callback);
    }

    @Override
    public void getRobotState(IResultCallback<RobotState> callback) {
        if (callback != null) {
            updateStateFromSdk();
            updateChargingState(chargingRuntimeBridge.getCurrentState());
            callback.onSuccess(currentState);
        }
    }

    @Override
    public void getBatteryLevel(IResultCallback<Integer> callback) {
        if (callback == null) {
            return;
        }
        try {
            int power = currentState.getBatteryLevel();
            if (peanutRuntime != null) {
                RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                if (info != null) {
                    power = info.getPower();
                    synchronized (currentState) {
                        currentState.setBatteryLevel(power);
                    }
                }
            }
            callback.onSuccess(power);
        } catch (Exception e) {
            Log.e(TAG, "getBatteryLevel failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void getChargingState(IResultCallback<ChargingState> callback) {
        if (callback == null) {
            return;
        }
        try {
            ChargingState chargingState = chargingRuntimeBridge.getCurrentState();
            updateChargingState(chargingState);
            callback.onSuccess(chargingState);
        } catch (Exception e) {
            Log.e(TAG, "getChargingState failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void getCurrentLocation(IResultCallback<RobotState.LocationInfo> callback) {
        if (callback != null) {
            callback.onSuccess(currentState.getLocation());
        }
    }

    @Override
    public void isScramButtonPressed(IResultCallback<Boolean> callback) {
        if (callback == null) {
            return;
        }
        try {
            boolean pressed = currentState.isScramButtonPressed();
            if (peanutRuntime != null) {
                RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                if (info != null) {
                    pressed = info.isEmergencyOpen();
                }
            }
            callback.onSuccess(pressed);
        } catch (Exception e) {
            Log.e(TAG, "isScramButtonPressed failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void getMotorStatus(IResultCallback<Integer> callback) {
        if (callback == null) {
            return;
        }
        try {
            int status = currentState.getMotorStatus();
            if (peanutRuntime != null) {
                RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                if (info != null) {
                    status = info.getMotorStatus();
                }
            }
            callback.onSuccess(status);
        } catch (Exception e) {
            Log.e(TAG, "getMotorStatus failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void setWorkMode(int mode, IResultCallback<Void> callback) {
        try {
            if (peanutRuntime != null) {
                peanutRuntime.setWorkMode(mode);
            }
            workMode = mode;
            synchronized (currentState) {
                currentState.setWorkMode(mode);
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "setWorkMode failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void setEmergencyEnabled(boolean enabled, IResultCallback<Void> callback) {
        try {
            if (peanutRuntime != null) {
                peanutRuntime.setEmergencyEnable(enabled);
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "setEmergencyEnabled failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void setMotorEnabled(boolean enabled, IResultCallback<Void> callback) {
        try {
            PeanutSDK.getInstance().motor().enable(new IDataCallback() {
                @Override
                public void success(String result) {
                    notifySuccess(callback);
                }

                @Override
                public void error(com.keenon.sdk.hedera.model.ApiError error) {
                    notifyError(callback, error.getCode(), error.getMsg());
                }
            }, enabled ? 1 : 0);
        } catch (Exception e) {
            Log.e(TAG, "setMotorEnabled failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void syncParams(boolean needReboot, IResultCallback<Void> callback) {
        try {
            if (peanutRuntime != null) {
                peanutRuntime.syncParams2Robot(needReboot);
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "syncParams failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void reboot(IResultCallback<Void> callback) {
        try {
            if (peanutRuntime != null) {
                peanutRuntime.syncParams2Robot(true);
                notifySuccess(callback);
                return;
            }

            PeanutSDK.getInstance().device().reboot(new IDataCallback() {
                @Override
                public void success(String result) {
                    notifySuccess(callback);
                }

                @Override
                public void error(com.keenon.sdk.hedera.model.ApiError error) {
                    notifyError(callback, error.getCode(), error.getMsg());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "reboot failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void performLocalization(IResultCallback<Void> callback) {
        boolean shouldTriggerLocalization = false;
        int pendingCount;
        try {
            synchronized (localizationLock) {
                if (callback != null) {
                    pendingLocalizationCallbacks.add(callback);
                }
                pendingCount = pendingLocalizationCallbacks.size();
                if (!localizationInProgress) {
                    localizationInProgress = true;
                    scheduleLocalizationTimeoutLocked();
                    shouldTriggerLocalization = true;
                }
            }

            logLocalization("收到定位请求，当前等待回调数=" + pendingCount
                    + "，是否新发起定位=" + shouldTriggerLocalization);

            if (peanutRuntime == null) {
                logLocalizationError("定位请求失败，PeanutRuntime 未就绪");
                completeLocalizationError(-1, LOCALIZATION_SERVICE_UNAVAILABLE_MESSAGE);
                return;
            }

            if (shouldTriggerLocalization) {
                logLocalization("调用 peanutRuntime.location() 发起真实定位");
                peanutRuntime.location();
            } else {
                logLocalization("复用进行中的定位请求，不重复调用 location()");
            }
        } catch (Exception e) {
            logLocalizationError("发起定位异常: " + e.getMessage());
            Log.e(TAG, "performLocalization failed", e);
            completeLocalizationError(-1, e.getMessage());
        }
    }

    @Override
    public void getTotalOdometer(IResultCallback<Double> callback) {
        if (callback == null) {
            return;
        }
        try {
            double odo = 0.0;
            if (peanutRuntime != null) {
                RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                if (info != null) {
                    odo = info.getTotalOdo();
                }
            }
            callback.onSuccess(odo);
        } catch (Exception e) {
            Log.e(TAG, "getTotalOdometer failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void getRobotIp(IResultCallback<String> callback) {
        if (callback == null) {
            return;
        }
        try {
            String ip = "";
            if (peanutRuntime != null) {
                RuntimeInfo info = peanutRuntime.getRuntimeInfo();
                if (info != null) {
                    ip = info.getRobotIp();
                }
            }
            callback.onSuccess(ip != null ? ip : "");
        } catch (Exception e) {
            Log.e(TAG, "getRobotIp failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void getRobotArmInfo(IResultCallback<String> callback) {
        if (callback == null) {
            return;
        }
        try {
            String info = "";
            if (peanutRuntime != null) {
                RuntimeInfo runtimeInfo = peanutRuntime.getRuntimeInfo();
                if (runtimeInfo != null) {
                    info = runtimeInfo.getRobotArmInfo();
                }
            }
            callback.onSuccess(info != null ? info : "");
        } catch (Exception e) {
            Log.e(TAG, "getRobotArmInfo failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void getRobotStm32Info(IResultCallback<String> callback) {
        if (callback == null) {
            return;
        }
        try {
            String info = "";
            if (peanutRuntime != null) {
                RuntimeInfo runtimeInfo = peanutRuntime.getRuntimeInfo();
                if (runtimeInfo != null) {
                    info = runtimeInfo.getRobotStm32Info();
                }
            }
            callback.onSuccess(info != null ? info : "");
        } catch (Exception e) {
            Log.e(TAG, "getRobotStm32Info failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void getDestinationList(IResultCallback<String> callback) {
        if (callback == null) {
            return;
        }
        try {
            String list = "[]";
            if (peanutRuntime != null) {
                RuntimeInfo runtimeInfo = peanutRuntime.getRuntimeInfo();
                if (runtimeInfo != null) {
                    list = runtimeInfo.getDestList();
                }
            }
            callback.onSuccess(list != null ? list : "[]");
        } catch (Exception e) {
            Log.e(TAG, "getDestinationList failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void getRobotProperties(IResultCallback<String> callback) {
        if (callback == null) {
            return;
        }
        try {
            String props = "{}";
            if (peanutRuntime != null) {
                RuntimeInfo runtimeInfo = peanutRuntime.getRuntimeInfo();
                if (runtimeInfo != null) {
                    props = runtimeInfo.getRobotProperties();
                }
            }
            callback.onSuccess(props != null ? props : "{}");
        } catch (Exception e) {
            Log.e(TAG, "getRobotProperties failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void release() {
        logLocalization("释放服务，清理定位状态和监听");
        callbacks.clear();
        completeLocalizationError(-1, LOCALIZATION_SERVICE_RELEASED_MESSAGE);
        chargingRuntimeBridge.removeListener(chargingStateListener);
        if (ownsChargingRuntimeBridge) {
            chargingRuntimeBridge.release();
        }
        if (peanutRuntime != null) {
            try {
                peanutRuntime.removeListener(runtimeListener);
            } catch (Exception e) {
                Log.e(TAG, "removeListener failed", e);
            }
            peanutRuntime = null;
        }
    }

    protected PeanutRuntime getPeanutRuntimeInstance() {
        return PeanutRuntime.getInstance();
    }

    private void updateStateFromSdk() {
        if (peanutRuntime == null) {
            return;
        }
        RuntimeInfo info = peanutRuntime.getRuntimeInfo();
        if (info == null) {
            return;
        }

        boolean stateChanged = false;
        synchronized (currentState) {
            int power = info.getPower();
            if (power != currentState.getBatteryLevel()) {
                currentState.setBatteryLevel(power);
                notifyBatteryLevelChanged(power);
                stateChanged = true;
            }

            int newWorkMode = info.getWorkMode();
            if (workMode != newWorkMode) {
                workMode = newWorkMode;
                currentState.setWorkMode(workMode);
                stateChanged = true;
            }

            boolean scram = info.isEmergencyOpen();
            if (scram != currentState.isScramButtonPressed()) {
                currentState.setScramButtonPressed(scram);
                stateChanged = true;
            }

            int motorStatus = info.getMotorStatus();
            if (motorStatus != currentState.getMotorStatus()) {
                currentState.setMotorStatus(motorStatus);
                stateChanged = true;
            }
        }

        if (stateChanged) {
            notifyStateChanged(currentState);
        }
    }

    private boolean updateChargingState(ChargingState chargingState) {
        synchronized (currentState) {
            ChargingState existing = currentState.getChargingState();
            if (sameChargingState(existing, chargingState)) {
                return false;
            }
            currentState.setChargingState(copyChargingState(chargingState));
            return true;
        }
    }

    private boolean sameChargingState(ChargingState first, ChargingState second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        return first.getStatus() == second.getStatus()
                && first.getEvent() == second.getEvent()
                && first.getPileId() == second.getPileId()
                && first.isCharging() == second.isCharging();
    }

    private ChargingState copyChargingState(ChargingState chargingState) {
        return chargingState == null ? null : new ChargingState(chargingState);
    }

    private void notifyStateChanged(RobotState state) {
        for (IStateCallback callback : callbacks) {
            try {
                callback.onStateChanged(state);
            } catch (Exception e) {
                Log.e(TAG, "onStateChanged callback failed", e);
            }
        }
    }

    private void notifyBatteryLevelChanged(int level) {
        for (IStateCallback callback : callbacks) {
            try {
                callback.onBatteryLevelChanged(level);
            } catch (Exception e) {
                Log.e(TAG, "onBatteryLevelChanged callback failed", e);
            }
        }
    }

    private void notifyChargingStateChanged(ChargingState chargingState) {
        ChargingState snapshot = copyChargingState(chargingState);
        for (IStateCallback callback : callbacks) {
            try {
                callback.onChargingStateChanged(snapshot);
            } catch (Exception e) {
                Log.e(TAG, "onChargingStateChanged callback failed", e);
            }
        }
    }

    private void notifySuccess(IResultCallback<Void> callback) {
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    private void notifyError(IResultCallback<?> callback, int code, String message) {
        if (callback != null) {
            callback.onError(new ApiError(code, message));
        }
    }

    private boolean isLocalizationInProgress() {
        synchronized (localizationLock) {
            return localizationInProgress;
        }
    }

    private void scheduleLocalizationTimeoutLocked() {
        if (mainHandler == null) {
            logLocalizationError("主线程 Handler 不可用，无法注册定位超时");
            return;
        }
        if (localizationTimeoutRunnable != null) {
            mainHandler.removeCallbacks(localizationTimeoutRunnable);
        }
        localizationTimeoutRunnable = () -> completeLocalizationError(-1, LOCALIZATION_TIMEOUT_MESSAGE);
        mainHandler.postDelayed(localizationTimeoutRunnable, LOCALIZATION_TIMEOUT_MS);
        logLocalization("已注册定位超时，timeoutMs=" + LOCALIZATION_TIMEOUT_MS);
    }

    private void completeLocalizationSuccess() {
        List<IResultCallback<Void>> callbacksToNotify;
        synchronized (localizationLock) {
            if (!localizationInProgress) {
                logLocalization("收到定位成功事件，但当前没有进行中的定位请求，忽略");
                return;
            }
            callbacksToNotify = new ArrayList<>(pendingLocalizationCallbacks);
            pendingLocalizationCallbacks.clear();
            localizationInProgress = false;
            cancelLocalizationTimeoutLocked();
        }
        logLocalization("定位成功，开始回调等待者，count=" + callbacksToNotify.size());
        for (IResultCallback<Void> callback : callbacksToNotify) {
            notifySuccess(callback);
        }
    }

    private void completeLocalizationError(int code, String message) {
        List<IResultCallback<Void>> callbacksToNotify;
        synchronized (localizationLock) {
            if (!localizationInProgress && pendingLocalizationCallbacks.isEmpty()) {
                logLocalization("收到定位失败收尾，但当前没有等待中的定位请求，忽略");
                return;
            }
            callbacksToNotify = new ArrayList<>(pendingLocalizationCallbacks);
            pendingLocalizationCallbacks.clear();
            localizationInProgress = false;
            cancelLocalizationTimeoutLocked();
        }
        logLocalizationError("定位失败，code=" + code + "，message=" + message
                + "，回调等待者 count=" + callbacksToNotify.size());
        for (IResultCallback<Void> callback : callbacksToNotify) {
            notifyError(callback, code, message);
        }
    }

    private void cancelLocalizationTimeoutLocked() {
        if (mainHandler == null) {
            localizationTimeoutRunnable = null;
            return;
        }
        if (localizationTimeoutRunnable != null) {
            mainHandler.removeCallbacks(localizationTimeoutRunnable);
            localizationTimeoutRunnable = null;
        }
    }

    private Handler createMainHandler() {
        try {
            Looper mainLooper = Looper.getMainLooper();
            return mainLooper != null ? new Handler(mainLooper) : null;
        } catch (RuntimeException e) {
            logLocalizationError("主线程 Looper 不可用: " + e.getMessage());
            Log.e(TAG, "Main looper unavailable, localization timeout disabled", e);
            return null;
        }
    }

    private void logLocalization(String message) {
        Log.d(TAG, LOG_PREFIX + " " + message);
    }

    private void logLocalizationError(String message) {
        Log.e(TAG, LOG_PREFIX + " " + message);
    }
}
