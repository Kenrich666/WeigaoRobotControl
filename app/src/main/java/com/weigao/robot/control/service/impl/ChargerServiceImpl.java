package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.keenon.sdk.component.charger.PeanutCharger;
import com.keenon.sdk.component.charger.common.Charger;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IChargerCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.SdkErrorCode;
import com.weigao.robot.control.model.ChargerInfo;
import com.weigao.robot.control.service.IChargerService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Charger service focused on charging actions, status, and events.
 */
public class ChargerServiceImpl implements IChargerService {

    private static final String TAG = "ChargerServiceImpl";

    private static final int CHARGER_STATUS_IDLE = 1;
    private static final int CHARGER_STATUS_AUTO_GOING = 2;
    private static final int CHARGER_STATUS_MANUAL_GOING = 3;
    private static final int CHARGER_STATUS_CHARGING = 4;
    private static final int CHARGER_STATUS_ADAPTER_CHARGING = 5;
    private static final int CHARGER_STATUS_CANCELLING = 6;

    private static final long INIT_STOP_DELAY_MS = 3000L;

    private final Context context;
    private final List<IChargerCallback> callbacks = new CopyOnWriteArrayList<>();
    private final ChargerInfo chargerInfo = new ChargerInfo();
    private final Handler handler;

    private PeanutCharger peanutCharger;
    private int currentPileId = 0;
    private boolean isCharging = false;
    private boolean initResetPending = true;

    public ChargerServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        this.handler = createMainHandler();
        chargerInfo.setPileId(currentPileId);
        Log.d(TAG, "ChargerServiceImpl created");
        initPeanutCharger();
    }

    private void initPeanutCharger() {
        try {
            PeanutCharger.Builder builder = new PeanutCharger.Builder()
                    .setPile(currentPileId)
                    .setListener(mChargerListener);
            peanutCharger = createPeanutCharger(builder);
            peanutCharger.execute();
            Log.d(TAG, "PeanutCharger initialized");

            initResetPending = true;
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

    private final Charger.Listener mChargerListener = new Charger.Listener() {
        @Override
        public void onChargerInfoChanged(int event,
                com.keenon.sdk.component.charger.common.ChargerInfo sdkInfo) {
            Log.d(TAG, "onChargerInfoChanged: event=" + event
                    + ", sdkPower=" + (sdkInfo != null ? sdkInfo.getPower() : "null")
                    + ", isCharging=" + isCharging);

            if (sdkInfo != null) {
                chargerInfo.setEvent(sdkInfo.getEvent());
            } else {
                chargerInfo.setEvent(event);
            }
            chargerInfo.setCharging(isCharging);
            chargerInfo.setPileId(currentPileId);
            notifyChargerInfoChanged(event, chargerInfo);
        }

        @Override
        public void onChargerStatusChanged(int status) {
            Log.d(TAG, "onChargerStatusChanged: status=" + status
                    + "(" + getStatusDesc(status) + ")"
                    + ", isCharging=" + isCharging
                    + ", initResetPending=" + initResetPending);

            if (initResetPending) {
                if (status == CHARGER_STATUS_IDLE || status == CHARGER_STATUS_CANCELLING) {
                    initResetPending = false;
                    Log.d(TAG, "Startup STOP took effect, resume normal charging status handling");
                } else if (status == CHARGER_STATUS_CHARGING
                        || status == CHARGER_STATUS_ADAPTER_CHARGING) {
                    Log.d(TAG, "Ignore stale charging status during startup reset: " + status);
                    chargerInfo.setStatus(status);
                    chargerInfo.setPileId(currentPileId);
                    notifyChargerStatusChanged(status);
                    return;
                }
            }

            boolean wasCharging = isCharging;
            isCharging = status == CHARGER_STATUS_CHARGING
                    || status == CHARGER_STATUS_ADAPTER_CHARGING;

            chargerInfo.setStatus(status);
            chargerInfo.setCharging(isCharging);
            chargerInfo.setPileId(currentPileId);

            if (isCharging != wasCharging) {
                Log.d(TAG, "Charging state changed: " + (isCharging ? "charging" : "not charging"));
                notifyChargerInfoChanged(chargerInfo.getEvent(), chargerInfo);
            }

            notifyChargerStatusChanged(status);
        }

        @Override
        public void onError(int errorCode) {
            Log.e(TAG, "Charging error: errorCode=" + errorCode);
        }
    };

    @Override
    public void startAutoCharge(IResultCallback<Void> callback) {
        Log.d(TAG, "startAutoCharge");
        performAction(CHARGE_ACTION_AUTO, callback);
    }

    @Override
    public void startManualCharge(IResultCallback<Void> callback) {
        Log.d(TAG, "startManualCharge");
        performAction(CHARGE_ACTION_MANUAL, callback);
    }

    @Override
    public void startAdapterCharge(IResultCallback<Void> callback) {
        Log.d(TAG, "startAdapterCharge");
        performAction(CHARGE_ACTION_ADAPTER, callback);
    }

    @Override
    public void stopCharge(IResultCallback<Void> callback) {
        Log.d(TAG, "stopCharge");
        performAction(CHARGE_ACTION_STOP, callback);
    }

    @Override
    public void performAction(int action, IResultCallback<Void> callback) {
        Log.d(TAG, "performAction: action=" + action);
        try {
            if (peanutCharger != null) {
                int sdkAction = switch (action) {
                    case CHARGE_ACTION_AUTO -> PeanutCharger.CHARGE_ACTION_AUTO;
                    case CHARGE_ACTION_MANUAL -> PeanutCharger.CHARGE_ACTION_MANUAL;
                    case CHARGE_ACTION_ADAPTER -> PeanutCharger.CHARGE_ACTION_ADAPTER;
                    case CHARGE_ACTION_STOP -> PeanutCharger.CHARGE_ACTION_STOP;
                    default -> action;
                };
                peanutCharger.performAction(sdkAction);
                if (action == CHARGE_ACTION_STOP) {
                    isCharging = false;
                    chargerInfo.setCharging(false);
                    chargerInfo.setStatus(CHARGER_STATUS_IDLE);
                    chargerInfo.setEvent(SdkErrorCode.CHARGER_EVENT_EXIT);
                    chargerInfo.setPileId(currentPileId);
                    notifyChargerInfoChanged(SdkErrorCode.CHARGER_EVENT_EXIT, chargerInfo);
                }
                initResetPending = false;
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "performAction failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void setChargePile(int pileId, IResultCallback<Void> callback) {
        Log.d(TAG, "setChargePile: " + pileId);
        try {
            currentPileId = pileId;
            chargerInfo.setPileId(pileId);
            if (peanutCharger != null) {
                peanutCharger.setPile(pileId);
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "setChargePile failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void getChargePile(IResultCallback<Integer> callback) {
        if (callback == null) {
            return;
        }
        try {
            int pile = currentPileId;
            if (peanutCharger != null) {
                pile = peanutCharger.getPile();
            }
            callback.onSuccess(pile);
        } catch (Exception e) {
            Log.e(TAG, "getChargePile failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void getChargerInfo(IResultCallback<ChargerInfo> callback) {
        if (callback != null) {
            callback.onSuccess(chargerInfo);
        }
    }

    @Override
    public void isCharging(IResultCallback<Boolean> callback) {
        if (callback != null) {
            callback.onSuccess(isCharging);
        }
    }

    @Override
    public void registerCallback(IChargerCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
            Log.d(TAG, "charger callback registered, size=" + callbacks.size());
        }
    }

    @Override
    public void unregisterCallback(IChargerCallback callback) {
        if (callbacks.remove(callback)) {
            Log.d(TAG, "charger callback unregistered, size=" + callbacks.size());
        }
    }

    @Override
    public void release() {
        Log.d(TAG, "release ChargerService");
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        callbacks.clear();
        if (peanutCharger != null) {
            try {
                peanutCharger.release();
            } catch (Exception e) {
                Log.e(TAG, "release peanutCharger failed", e);
            }
            peanutCharger = null;
        }
    }

    private void notifyChargerInfoChanged(int event, ChargerInfo info) {
        for (IChargerCallback callback : callbacks) {
            try {
                callback.onChargerInfoChanged(event, info);
            } catch (Exception e) {
                Log.e(TAG, "onChargerInfoChanged callback failed", e);
            }
        }
    }

    private void notifyChargerStatusChanged(int status) {
        for (IChargerCallback callback : callbacks) {
            try {
                callback.onChargerStatusChanged(status);
            } catch (Exception e) {
                Log.e(TAG, "onChargerStatusChanged callback failed", e);
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

    private Handler createMainHandler() {
        try {
            Looper mainLooper = Looper.getMainLooper();
            return mainLooper != null ? new Handler(mainLooper) : null;
        } catch (RuntimeException e) {
            Log.e(TAG, "Main looper unavailable for ChargerService", e);
            return null;
        }
    }

    protected PeanutCharger createPeanutCharger(PeanutCharger.Builder builder) {
        return builder.build();
    }

    private static String getStatusDesc(int status) {
        return switch (status) {
            case CHARGER_STATUS_IDLE -> "idle";
            case CHARGER_STATUS_AUTO_GOING -> "auto_going";
            case CHARGER_STATUS_MANUAL_GOING -> "manual_going";
            case CHARGER_STATUS_CHARGING -> "charging";
            case CHARGER_STATUS_ADAPTER_CHARGING -> "adapter_charging";
            case CHARGER_STATUS_CANCELLING -> "cancelling";
            default -> "unknown(" + status + ")";
        };
    }
}
