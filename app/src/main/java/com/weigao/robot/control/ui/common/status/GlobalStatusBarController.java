package com.weigao.robot.control.ui.common.status;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.IStateCallback;
import com.weigao.robot.control.model.RobotState;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Map;
import java.util.WeakHashMap;

public final class GlobalStatusBarController {
    private static final int WIFI_LEVEL_COUNT = 5;
    private static final Map<Activity, GlobalStatusBarController> INSTANCES = new WeakHashMap<>();

    public static synchronized void attach(Activity activity) {
        if (activity == null) {
            return;
        }
        GlobalStatusBarController controller = INSTANCES.get(activity);
        if (controller == null) {
            controller = new GlobalStatusBarController(activity);
            INSTANCES.put(activity, controller);
        }
        controller.attachInternal();
    }

    public static synchronized void detach(Activity activity) {
        GlobalStatusBarController controller = INSTANCES.remove(activity);
        if (controller != null) {
            controller.detachInternal();
        }
    }

    public static synchronized void refreshNow(Activity activity) {
        GlobalStatusBarController controller = INSTANCES.get(activity);
        if (controller != null) {
            controller.refreshInternal();
        }
    }

    private final WeakReference<Activity> activityRef;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeTicker = new Runnable() {
        @Override
        public void run() {
            updateTime();
            scheduleNextTick();
        }
    };
    private final IStateCallback stateCallback = new IStateCallback() {
        @Override
        public void onStateChanged(RobotState newState) {
        }

        @Override
        public void onLocationChanged(double x, double y) {
        }

        @Override
        public void onBatteryLevelChanged(int level) {
            mainHandler.post(() -> updateBattery(level));
        }

        @Override
        public void onScramButtonPressed(boolean pressed) {
        }
    };
    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWifiState();
        }
    };

    private View statusBarView;
    private TextView timeTextView;
    private TextView batteryTextView;
    private TextView wifiTextView;
    private ImageView batteryIconView;
    private ImageView wifiIconView;
    private View targetRootView;
    private int originalRootPaddingTop;
    private boolean receiverRegistered;
    private boolean batteryCallbackRegistered;
    private boolean attached;
    private IRobotStateService robotStateService;

    private GlobalStatusBarController(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
    }

    private void attachInternal() {
        Activity activity = activityRef.get();
        if (activity == null) {
            return;
        }

        if (attached) {
            refreshInternal();
            return;
        }

        FrameLayout content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) {
            return;
        }

        if (statusBarView == null) {
            statusBarView = LayoutInflater.from(activity).inflate(R.layout.view_global_status_bar, content, false);
            timeTextView = statusBarView.findViewById(R.id.tv_status_time);
            batteryTextView = statusBarView.findViewById(R.id.tv_status_battery);
            wifiTextView = statusBarView.findViewById(R.id.tv_status_wifi);
            batteryIconView = statusBarView.findViewById(R.id.iv_status_battery);
            wifiIconView = statusBarView.findViewById(R.id.iv_status_wifi);

            int height = activity.getResources().getDimensionPixelSize(R.dimen.global_status_bar_height);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    height,
                    Gravity.TOP);
            content.addView(statusBarView, params);
        } else if (statusBarView.getParent() == null) {
            int height = activity.getResources().getDimensionPixelSize(R.dimen.global_status_bar_height);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    height,
                    Gravity.TOP);
            content.addView(statusBarView, params);
        }

        targetRootView = findTargetRootView(content);
        if (targetRootView != null) {
            originalRootPaddingTop = targetRootView.getPaddingTop();
            int extraTop = activity.getResources().getDimensionPixelSize(R.dimen.global_status_bar_height);
            targetRootView.setPadding(
                    targetRootView.getPaddingLeft(),
                    originalRootPaddingTop + extraTop,
                    targetRootView.getPaddingRight(),
                    targetRootView.getPaddingBottom());
        }

        statusBarView.bringToFront();
        statusBarView.setVisibility(View.VISIBLE);
        statusBarView.setTranslationZ(1000f);
        startTimeTicker();
        ensureBatteryBinding();
        registerWifiReceiver();
        attached = true;
        refreshInternal();
    }

    private void detachInternal() {
        stopTimeTicker();
        unregisterWifiReceiver();
        unregisterBatteryBinding();

        if (targetRootView != null) {
            targetRootView.setPadding(
                    targetRootView.getPaddingLeft(),
                    originalRootPaddingTop,
                    targetRootView.getPaddingRight(),
                    targetRootView.getPaddingBottom());
            targetRootView = null;
        }

        if (statusBarView != null) {
            ViewParent parent = statusBarView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(statusBarView);
            }
        }
        attached = false;
    }

    private void refreshInternal() {
        updateTime();
        ensureBatteryBinding();
        requestBatteryLevel();
        updateWifiState();
    }

    private void startTimeTicker() {
        stopTimeTicker();
        timeTicker.run();
    }

    private void stopTimeTicker() {
        mainHandler.removeCallbacks(timeTicker);
    }

    private void scheduleNextTick() {
        long now = System.currentTimeMillis();
        long nextDelay = 60_000L - (now % 60_000L);
        mainHandler.postDelayed(timeTicker, nextDelay);
    }

    private void updateTime() {
        if (timeTextView == null) {
            return;
        }
        CharSequence formatted = DateFormat.format("HH:mm", new Date());
        timeTextView.setText(formatted);
    }

    private void ensureBatteryBinding() {
        if (batteryCallbackRegistered) {
            return;
        }
        try {
            robotStateService = ServiceManager.getInstance().getRobotStateService();
        } catch (Exception ignored) {
            robotStateService = null;
        }
        if (robotStateService != null) {
            robotStateService.registerCallback(stateCallback);
            batteryCallbackRegistered = true;
        }
    }

    private void unregisterBatteryBinding() {
        if (batteryCallbackRegistered && robotStateService != null) {
            robotStateService.unregisterCallback(stateCallback);
        }
        batteryCallbackRegistered = false;
        robotStateService = null;
    }

    private void requestBatteryLevel() {
        if (robotStateService == null) {
            updateBattery(0);
            return;
        }
        robotStateService.getBatteryLevel(new IResultCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                mainHandler.post(() -> updateBattery(result != null ? result : 0));
            }

            @Override
            public void onError(ApiError error) {
                mainHandler.post(() -> updateBattery(0));
            }
        });
    }

    private void updateBattery(int level) {
        Activity activity = activityRef.get();
        if (activity == null) {
            return;
        }
        int safeLevel = Math.max(0, Math.min(100, level));
        if (batteryTextView != null) {
            batteryTextView.setText(safeLevel + "%");
        }
        if (batteryIconView != null) {
            int tint;
            if (safeLevel <= 20) {
                tint = ContextCompat.getColor(activity, R.color.medical_error);
            } else if (safeLevel <= 50) {
                tint = ContextCompat.getColor(activity, R.color.medical_warning);
            } else {
                tint = ContextCompat.getColor(activity, R.color.medical_text_primary);
            }
            batteryIconView.setImageResource(R.drawable.ic_status_battery);
            batteryIconView.setColorFilter(tint);
        }
    }

    private void registerWifiReceiver() {
        Activity activity = activityRef.get();
        if (activity == null || receiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(activity, wifiReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        } else {
            activity.registerReceiver(wifiReceiver, filter);
        }
        receiverRegistered = true;
    }

    private void unregisterWifiReceiver() {
        Activity activity = activityRef.get();
        if (activity != null && receiverRegistered) {
            try {
                activity.unregisterReceiver(wifiReceiver);
            } catch (Exception ignored) {
            }
        }
        receiverRegistered = false;
    }

    private void updateWifiState() {
        Activity activity = activityRef.get();
        if (activity == null) {
            return;
        }
        Context context = activity;
        WifiStatus status = resolveWifiStatus(context);
        if (wifiIconView != null) {
            wifiIconView.setImageResource(status.connected ? R.drawable.ic_status_wifi : R.drawable.ic_status_wifi_off);
            wifiIconView.setColorFilter(ContextCompat.getColor(context,
                    status.connected ? R.color.medical_text_primary : R.color.medical_text_secondary));
        }
        if (wifiTextView != null) {
            wifiTextView.setText(status.connected ? (status.level + "/" + (WIFI_LEVEL_COUNT - 1)) : "--");
        }
    }

    private WifiStatus resolveWifiStatus(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiManager =
                (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (connectivityManager == null || wifiManager == null || !isWifiConnected(connectivityManager)) {
            return new WifiStatus(false, 0);
        }

        try {
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info == null || info.getNetworkId() == -1) {
                return new WifiStatus(true, WIFI_LEVEL_COUNT - 1);
            }
            int level = WifiManager.calculateSignalLevel(info.getRssi(), WIFI_LEVEL_COUNT);
            return new WifiStatus(true, Math.max(0, Math.min(WIFI_LEVEL_COUNT - 1, level)));
        } catch (Exception ignored) {
            return new WifiStatus(true, WIFI_LEVEL_COUNT - 1);
        }
    }

    private boolean isWifiConnected(ConnectivityManager connectivityManager) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI;
    }

    private View findTargetRootView(FrameLayout content) {
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            if (child != statusBarView) {
                return child;
            }
        }
        return null;
    }

    private static final class WifiStatus {
        final boolean connected;
        final int level;

        WifiStatus(boolean connected, int level) {
            this.connected = connected;
            this.level = level;
        }
    }
}
