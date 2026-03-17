package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.keenon.sdk.component.charger.PeanutCharger;
import com.keenon.sdk.component.charger.common.Charger;
import com.weigao.robot.control.callback.SdkErrorCode;
import com.weigao.robot.control.model.ChargingState;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Shared PeanutCharger adapter used by both runtime reads and charging actions.
 */
public class ChargingRuntimeBridge {

    public interface Listener {
        void onChargingStateChanged(ChargingState chargingState);
    }

    private static final String TAG = "ChargingRuntimeBridge";

    static final int CHARGER_STATUS_IDLE = 1;
    static final int CHARGER_STATUS_AUTO_GOING = 2;
    static final int CHARGER_STATUS_MANUAL_GOING = 3;
    static final int CHARGER_STATUS_CHARGING = 4;
    static final int CHARGER_STATUS_ADAPTER_CHARGING = 5;
    static final int CHARGER_STATUS_CANCELLING = 6;

    private static final long INIT_STOP_DELAY_MS = 3000L;

    private final Handler handler;
    private final Object stateLock = new Object();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final ChargingState currentState = new ChargingState();

    private PeanutCharger peanutCharger;
    private boolean initResetPending = true;

    public ChargingRuntimeBridge(Context context) {
        this.handler = createMainHandler();
        initPeanutCharger();
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public ChargingState getCurrentState() {
        synchronized (stateLock) {
            return new ChargingState(currentState);
        }
    }

    public void performAction(int action) {
        if (peanutCharger == null) {
            throw new IllegalStateException("PeanutCharger unavailable");
        }
        peanutCharger.performAction(action);
        if (action == PeanutCharger.CHARGE_ACTION_STOP) {
            synchronized (stateLock) {
                currentState.setCharging(false);
                currentState.setStatus(CHARGER_STATUS_IDLE);
                currentState.setEvent(SdkErrorCode.CHARGER_EVENT_EXIT);
            }
            notifyStateChanged();
        }
        initResetPending = false;
    }

    public void setPile(int pileId) {
        synchronized (stateLock) {
            currentState.setPileId(pileId);
        }
        if (peanutCharger != null) {
            peanutCharger.setPile(pileId);
        }
        notifyStateChanged();
    }

    public void release() {
        listeners.clear();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (peanutCharger != null) {
            try {
                peanutCharger.release();
            } catch (Exception e) {
                Log.e(TAG, "release peanutCharger failed", e);
            }
            peanutCharger = null;
        }
    }

    protected PeanutCharger createPeanutCharger(PeanutCharger.Builder builder) {
        return builder.build();
    }

    private void initPeanutCharger() {
        try {
            PeanutCharger.Builder builder = new PeanutCharger.Builder()
                    .setPile(currentState.getPileId())
                    .setListener(chargerListener);
            peanutCharger = createPeanutCharger(builder);
            peanutCharger.execute();
            initResetPending = true;
            Log.d(TAG, "PeanutCharger initialized");

            if (handler != null) {
                handler.postDelayed(() -> {
                    if (peanutCharger != null && initResetPending) {
                        Log.d(TAG, "Send startup STOP to clear stale charging state");
                        peanutCharger.performAction(PeanutCharger.CHARGE_ACTION_STOP);
                    }
                }, INIT_STOP_DELAY_MS);
            }
        } catch (Exception e) {
            Log.e(TAG, "PeanutCharger init failed", e);
        }
    }

    private final Charger.Listener chargerListener = new Charger.Listener() {
        @Override
        public void onChargerInfoChanged(int event, com.keenon.sdk.component.charger.common.ChargerInfo sdkInfo) {
            synchronized (stateLock) {
                currentState.setEvent(sdkInfo != null ? sdkInfo.getEvent() : event);
            }
            notifyStateChanged();
        }

        @Override
        public void onChargerStatusChanged(int status) {
            Log.d(TAG, "onChargerStatusChanged: status=" + status
                    + ", isCharging=" + isChargingStatus(status)
                    + ", initResetPending=" + initResetPending);

            if (initResetPending) {
                if (status == CHARGER_STATUS_IDLE || status == CHARGER_STATUS_CANCELLING) {
                    initResetPending = false;
                    Log.d(TAG, "Startup STOP took effect");
                } else if (status == CHARGER_STATUS_CHARGING || status == CHARGER_STATUS_ADAPTER_CHARGING) {
                    Log.d(TAG, "Ignore stale charging status during startup reset: " + status);
                    return;
                }
            }

            synchronized (stateLock) {
                currentState.setStatus(status);
                currentState.setCharging(isChargingStatus(status));
            }
            notifyStateChanged();
        }

        @Override
        public void onError(int errorCode) {
            Log.e(TAG, "Charging error: errorCode=" + errorCode);
            synchronized (stateLock) {
                currentState.setEvent(errorCode);
            }
            notifyStateChanged();
        }
    };

    private boolean isChargingStatus(int status) {
        return status == CHARGER_STATUS_CHARGING || status == CHARGER_STATUS_ADAPTER_CHARGING;
    }

    private void notifyStateChanged() {
        ChargingState snapshot = getCurrentState();
        for (Listener listener : listeners) {
            try {
                listener.onChargingStateChanged(new ChargingState(snapshot));
            } catch (Exception e) {
                Log.e(TAG, "charging state callback failed", e);
            }
        }
    }

    private Handler createMainHandler() {
        try {
            Looper mainLooper = Looper.getMainLooper();
            return mainLooper != null ? new Handler(mainLooper) : null;
        } catch (RuntimeException e) {
            Log.e(TAG, "Main looper unavailable for ChargingRuntimeBridge", e);
            return null;
        }
    }
}
