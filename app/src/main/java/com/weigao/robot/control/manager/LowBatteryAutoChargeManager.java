package com.weigao.robot.control.manager;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.weigao.robot.control.R;
import com.weigao.robot.control.app.WeigaoApplication;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.IStateCallback;
import com.weigao.robot.control.callback.SdkErrorCode;
import com.weigao.robot.control.model.ChargingState;
import com.weigao.robot.control.model.RobotState;
import com.weigao.robot.control.service.IChargerService;
import com.weigao.robot.control.service.INavigationService;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.ui.main.MainActivity;
import com.weigao.robot.control.ui.main.PositioningActivity;

import java.lang.ref.WeakReference;

/**
 * 全局低电量自动回充管理器。
 */
public class LowBatteryAutoChargeManager implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "LowBatteryAutoCharge";
    private static final long COUNTDOWN_MS = 15_000L;
    private static final long CHARGE_CONNECTED_DIALOG_MS = 3_000L;

    private static final LowBatteryAutoChargeManager INSTANCE = new LowBatteryAutoChargeManager();

    private WeakReference<Activity> currentActivityRef;
    private Dialog confirmationDialog;
    private Dialog chargeConnectedDialog;
    private TextView countdownTextView;
    private CountDownTimer countdownTimer;
    private Handler mainHandler;
    private final Runnable navigateToMainRunnable = this::navigateToMainPage;

    private Application application;
    private boolean lifecycleRegistered;
    private boolean stateCallbackRegistered;
    private boolean pendingAfterTaskCompletion;
    private boolean confirmationPending;
    private boolean suppressUntilRecovery;
    private boolean autoChargeInProgress;
    private int currentBatteryLevel = -1;
    private boolean currentCharging;
    private boolean chargingStateInitialized;

    private LowBatteryAutoChargeManager() {
    }

    public static LowBatteryAutoChargeManager getInstance() {
        return INSTANCE;
    }

    public synchronized void init(Application application) {
        if (lifecycleRegistered) {
            return;
        }
        this.application = application;
        application.registerActivityLifecycleCallbacks(this);
        lifecycleRegistered = true;
    }

    public synchronized void connectService() {
        if (stateCallbackRegistered) {
            return;
        }

        try {
            IRobotStateService robotStateService = ServiceManager.getInstance().getRobotStateService();
            if (robotStateService == null) {
                Log.w(TAG, "RobotStateService unavailable, skip low battery auto charge init.");
                return;
            }
            refreshCurrentChargingState(robotStateService);
            refreshCurrentBatteryLevel(robotStateService);
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect services for low battery auto charge", e);
        }
    }

    public synchronized void release() {
        dismissConfirmationDialogInternal(true);
        dismissChargeConnectedDialog(true);
        if (mainHandler != null) {
            mainHandler.removeCallbacks(navigateToMainRunnable);
        }

        if (stateCallbackRegistered) {
            try {
                IRobotStateService robotStateService = ServiceManager.getInstance().getRobotStateService();
                if (robotStateService != null) {
                    robotStateService.unregisterCallback(stateCallback);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to unregister low battery state callback", e);
            }
        }

        if (application != null && lifecycleRegistered) {
            application.unregisterActivityLifecycleCallbacks(this);
        }

        application = null;
        lifecycleRegistered = false;
        stateCallbackRegistered = false;
        pendingAfterTaskCompletion = false;
        confirmationPending = false;
        suppressUntilRecovery = false;
        autoChargeInProgress = false;
        currentBatteryLevel = -1;
        currentCharging = false;
        chargingStateInitialized = false;
        currentActivityRef = null;
        mainHandler = null;
    }

    public synchronized boolean hasPendingTaskCompletionAutoCharge() {
        return pendingAfterTaskCompletion;
    }

    public synchronized void onTaskCompletedAndReadyForPrompt() {
        pendingAfterTaskCompletion = false;
        evaluateAutoChargeState();
    }

    public synchronized void onTaskCancelled() {
        pendingAfterTaskCompletion = false;
    }

    public void maybeShowPendingDialog() {
        Activity activity = getCurrentActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> maybeShowConfirmationDialog(activity));
        }
    }

    private void refreshCurrentChargingState(IRobotStateService robotStateService) {
        robotStateService.getChargingState(new IResultCallback<ChargingState>() {
            @Override
            public void onSuccess(ChargingState result) {
                if (result != null) {
                    initializeChargingState(result);
                    handleChargingStateChanged(result);
                }
                registerStateCallback(robotStateService);
            }

            @Override
            public void onError(ApiError error) {
                Log.w(TAG, "Failed to refresh charging state: " + error.getMessage());
                registerStateCallback(robotStateService);
            }
        });
    }

    private void refreshCurrentBatteryLevel(IRobotStateService robotStateService) {
        robotStateService.getBatteryLevel(new IResultCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                if (result != null) {
                    handleBatteryLevelChanged(result);
                }
                registerStateCallback(robotStateService);
            }

            @Override
            public void onError(ApiError error) {
                Log.w(TAG, "Failed to refresh battery level: " + error.getMessage());
                registerStateCallback(robotStateService);
            }
        });
    }

    private synchronized void registerStateCallback(IRobotStateService robotStateService) {
        if (stateCallbackRegistered) {
            return;
        }
        robotStateService.registerCallback(stateCallback);
        stateCallbackRegistered = true;
    }

    private synchronized void initializeChargingState(ChargingState chargingState) {
        if (chargingStateInitialized || chargingState == null) {
            return;
        }
        currentCharging = chargingState.isCharging();
        chargingStateInitialized = true;
    }

    private final IStateCallback stateCallback = new IStateCallback() {
        @Override
        public void onStateChanged(RobotState newState) {
        }

        @Override
        public void onLocationChanged(double x, double y) {
        }

        @Override
        public void onBatteryLevelChanged(int level) {
            handleBatteryLevelChanged(level);
        }

        @Override
        public void onChargingStateChanged(ChargingState chargingState) {
            handleChargingStateChanged(chargingState);
        }

        @Override
        public void onScramButtonPressed(boolean pressed) {
        }
    };

    private synchronized void handleBatteryLevelChanged(int batteryLevel) {
        currentBatteryLevel = batteryLevel;
        evaluateAutoChargeState();
    }

    private synchronized void handleChargingStateChanged(ChargingState chargingState) {
        if (chargingState == null) {
            return;
        }
        updateChargingState(chargingState.isCharging());
        if (autoChargeInProgress
                && SdkErrorCode.isChargerError(chargingState.getEvent())
                && !chargingState.isCharging()) {
            autoChargeInProgress = false;
            Activity activity = getCurrentActivity();
            if (activity != null) {
                activity.runOnUiThread(
                        () -> Toast.makeText(activity, "自动回充失败，请重试", Toast.LENGTH_SHORT).show());
            }
        }
        evaluateAutoChargeState();
    }

    private synchronized void evaluateAutoChargeState() {
        if (currentCharging) {
            autoChargeInProgress = true;
            pendingAfterTaskCompletion = false;
            confirmationPending = false;
            dismissConfirmationDialogInternal(false);
            return;
        }

        if (currentBatteryLevel < 0) {
            return;
        }

        if (!LowBatteryAutoChargeSettingsManager.getInstance().isEnabled()) {
            pendingAfterTaskCompletion = false;
            confirmationPending = false;
            dismissConfirmationDialogInternal(false);
            return;
        }

        int threshold = LowBatteryAutoChargeSettingsManager.getInstance().getThresholdPercent();
        if (currentBatteryLevel > threshold) {
            suppressUntilRecovery = false;
            autoChargeInProgress = false;
            pendingAfterTaskCompletion = false;
            confirmationPending = false;
            dismissConfirmationDialogInternal(false);
            return;
        }

        if (suppressUntilRecovery || autoChargeInProgress || confirmationPending) {
            return;
        }

        if (TaskExecutionStateManager.getInstance().hasActiveTask()) {
            pendingAfterTaskCompletion = true;
            return;
        }

        confirmationPending = true;
        Activity activity = getCurrentActivity();
        if (activity instanceof LowBatteryAutoChargeHost) {
            activity.runOnUiThread(((LowBatteryAutoChargeHost) activity)::handoffToLowBatteryAutoCharge);
            return;
        }

        stopNavigationIfNeeded();
        if (activity != null) {
            activity.runOnUiThread(() -> maybeShowConfirmationDialog(activity));
        }
    }

    private void updateChargingState(boolean isCharging) {
        if (!chargingStateInitialized) {
            currentCharging = isCharging;
            chargingStateInitialized = true;
            return;
        }
        boolean wasCharging = currentCharging;
        currentCharging = isCharging;
        if (currentCharging && !wasCharging) {
            showChargeConnectedDialog();
        }
    }

    private synchronized void maybeShowConfirmationDialog(Activity activity) {
        if (!confirmationPending || activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (confirmationDialog != null && confirmationDialog.isShowing()) {
            return;
        }

        dismissConfirmationDialogInternal(false);

        confirmationDialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        confirmationDialog.setContentView(R.layout.dialog_low_battery_auto_charge);
        confirmationDialog.setCancelable(false);

        if (confirmationDialog.getWindow() != null) {
            confirmationDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }

        countdownTextView = confirmationDialog.findViewById(R.id.tv_low_battery_countdown);
        confirmationDialog.findViewById(R.id.btn_charge_immediately)
                .setOnClickListener(v -> executeAutoCharge(activity));
        confirmationDialog.findViewById(R.id.btn_skip_auto_charge)
                .setOnClickListener(v -> skipCurrentAutoCharge());

        try {
            confirmationDialog.show();
            WeigaoApplication.applyFullScreen(activity);

            if (confirmationDialog.getWindow() != null) {
                try {
                    boolean fullscreen = AppSettingsManager.getInstance().isFullScreen();
                    if (fullscreen) {
                        WindowCompat.setDecorFitsSystemWindows(confirmationDialog.getWindow(), false);
                        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(
                                confirmationDialog.getWindow(), confirmationDialog.getWindow().getDecorView());
                        controller.hide(WindowInsetsCompat.Type.systemBars());
                        controller.setSystemBarsBehavior(
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to apply fullscreen for low battery dialog", e);
                }
                confirmationDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            }

            startCountdown(activity);
        } catch (Exception e) {
            Log.e(TAG, "Failed to show low battery auto charge dialog", e);
        }
    }

    private void startCountdown(Activity activity) {
        stopCountdown();
        countdownTimer = new CountDownTimer(COUNTDOWN_MS, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (countdownTextView != null) {
                    countdownTextView.setText((millisUntilFinished / 1000) + " 秒后自动回充");
                }
            }

            @Override
            public void onFinish() {
                executeAutoCharge(activity);
            }
        };
        countdownTimer.start();
    }

    private synchronized void executeAutoCharge(Activity activity) {
        if (!confirmationPending || autoChargeInProgress) {
            return;
        }

        confirmationPending = false;
        pendingAfterTaskCompletion = false;
        autoChargeInProgress = true;
        dismissConfirmationDialogInternal(false);
        stopNavigationIfNeeded();

        IChargerService chargerService = ServiceManager.getInstance().getChargerService();
        if (chargerService == null) {
            autoChargeInProgress = false;
            if (activity != null) {
                activity.runOnUiThread(
                        () -> Toast.makeText(activity, "充电服务不可用，无法自动回充", Toast.LENGTH_SHORT).show());
            }
            return;
        }

        chargerService.startAutoCharge(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (activity != null) {
                    activity.runOnUiThread(
                            () -> Toast.makeText(activity, "已启动自动回充", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onError(ApiError error) {
                autoChargeInProgress = false;
                if (activity != null) {
                    activity.runOnUiThread(() -> Toast
                            .makeText(activity, "启动自动回充失败: " + error.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private synchronized void skipCurrentAutoCharge() {
        suppressUntilRecovery = true;
        confirmationPending = false;
        pendingAfterTaskCompletion = false;
        autoChargeInProgress = false;
        dismissConfirmationDialogInternal(false);
    }

    private void stopNavigationIfNeeded() {
        try {
            INavigationService navigationService = ServiceManager.getInstance().getNavigationService();
            if (navigationService != null) {
                navigationService.stop(null);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to stop navigation before low battery auto charge", e);
        }
    }

    private synchronized void dismissConfirmationDialogInternal(boolean clearPending) {
        stopCountdown();
        if (confirmationDialog != null) {
            try {
                if (confirmationDialog.isShowing()) {
                    confirmationDialog.dismiss();
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to dismiss low battery dialog", e);
            }
        }
        confirmationDialog = null;
        countdownTextView = null;
        if (clearPending) {
            confirmationPending = false;
        }
    }

    private void showChargeConnectedDialog() {
        Activity activity = getCurrentActivity();
        if (activity == null && application instanceof WeigaoApplication) {
            activity = ((WeigaoApplication) application).getCurrentActivity();
        }
        if (activity instanceof PositioningActivity) {
            return;
        }
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            scheduleNavigateToMain();
            return;
        }
        final Activity targetActivity = activity;
        targetActivity.runOnUiThread(() -> showChargeConnectedDialog(targetActivity));
    }

    private synchronized void showChargeConnectedDialog(Activity activity) {
        dismissChargeConnectedDialog(true);
        Handler handler = getMainHandler();
        if (handler != null) {
            handler.removeCallbacks(navigateToMainRunnable);
        }

        try {
            chargeConnectedDialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
            chargeConnectedDialog.setContentView(R.layout.dialog_charge_connected);
            chargeConnectedDialog.setCancelable(false);

            if (chargeConnectedDialog.getWindow() != null) {
                chargeConnectedDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            }

            chargeConnectedDialog.show();
            WeigaoApplication.applyFullScreen(activity);

            if (chargeConnectedDialog.getWindow() != null) {
                try {
                    boolean fullscreen = AppSettingsManager.getInstance().isFullScreen();
                    if (fullscreen) {
                        WindowCompat.setDecorFitsSystemWindows(chargeConnectedDialog.getWindow(), false);
                        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(
                                chargeConnectedDialog.getWindow(), chargeConnectedDialog.getWindow().getDecorView());
                        controller.hide(WindowInsetsCompat.Type.systemBars());
                        controller.setSystemBarsBehavior(
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to apply fullscreen for charge connected dialog", e);
                }
                chargeConnectedDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show charge connected dialog", e);
        }

        scheduleNavigateToMain();
    }

    private void scheduleNavigateToMain() {
        if (application == null) {
            return;
        }
        Handler handler = getMainHandler();
        if (handler == null) {
            return;
        }
        handler.removeCallbacks(navigateToMainRunnable);
        handler.postDelayed(navigateToMainRunnable, CHARGE_CONNECTED_DIALOG_MS);
    }

    private void navigateToMainPage() {
        dismissChargeConnectedDialog(false);
        Application app = application;
        if (app == null) {
            return;
        }
        Activity currentActivity = getCurrentActivity();
        if (currentActivity instanceof MainActivity && !currentActivity.isFinishing()
                && !currentActivity.isDestroyed()) {
            return;
        }
        try {
            Intent intent = new Intent(app, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_SKIP_POSITIONING, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            app.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to main page after charging started", e);
        }
    }

    private synchronized void dismissChargeConnectedDialog(boolean cancelNavigation) {
        if (cancelNavigation && mainHandler != null) {
            mainHandler.removeCallbacks(navigateToMainRunnable);
        }
        if (chargeConnectedDialog != null) {
            try {
                if (chargeConnectedDialog.isShowing()) {
                    chargeConnectedDialog.dismiss();
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to dismiss charge connected dialog", e);
            }
        }
        chargeConnectedDialog = null;
    }

    private Handler getMainHandler() {
        if (mainHandler == null) {
            try {
                mainHandler = new Handler(Looper.getMainLooper());
            } catch (Exception e) {
                Log.w(TAG, "Failed to create main handler for charge connected dialog", e);
                return null;
            }
        }
        return mainHandler;
    }

    private void stopCountdown() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }

    private Activity getCurrentActivity() {
        if (currentActivityRef == null) {
            return null;
        }
        return currentActivityRef.get();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentActivityRef = new WeakReference<>(activity);
        if (confirmationPending) {
            activity.runOnUiThread(() -> maybeShowConfirmationDialog(activity));
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        dismissConfirmationDialogInternal(false);
        dismissChargeConnectedDialog(false);
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == activity) {
            currentActivityRef = null;
        }
    }
}
