package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.util.Log;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.service.IChargerService;

/**
 * Charging action executor backed by {@link ChargingRuntimeBridge}.
 */
public class ChargerServiceImpl implements IChargerService {

    private static final String TAG = "ChargerServiceImpl";

    private final ChargingRuntimeBridge chargingRuntimeBridge;
    private final boolean ownsChargingRuntimeBridge;

    public ChargerServiceImpl(Context context) {
        this(new ChargingRuntimeBridge(context), true);
    }

    public ChargerServiceImpl(ChargingRuntimeBridge chargingRuntimeBridge) {
        this(chargingRuntimeBridge, false);
    }

    private ChargerServiceImpl(ChargingRuntimeBridge chargingRuntimeBridge, boolean ownsChargingRuntimeBridge) {
        this.chargingRuntimeBridge = chargingRuntimeBridge;
        this.ownsChargingRuntimeBridge = ownsChargingRuntimeBridge;
        Log.d(TAG, "ChargerServiceImpl created");
    }

    @Override
    public void startAutoCharge(IResultCallback<Void> callback) {
        performAction(CHARGE_ACTION_AUTO, callback);
    }

    @Override
    public void startManualCharge(IResultCallback<Void> callback) {
        performAction(CHARGE_ACTION_MANUAL, callback);
    }

    @Override
    public void startAdapterCharge(IResultCallback<Void> callback) {
        performAction(CHARGE_ACTION_ADAPTER, callback);
    }

    @Override
    public void stopCharge(IResultCallback<Void> callback) {
        performAction(CHARGE_ACTION_STOP, callback);
    }

    @Override
    public void performAction(int action, IResultCallback<Void> callback) {
        try {
            chargingRuntimeBridge.performAction(action);
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "performAction failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void setChargePile(int pileId, IResultCallback<Void> callback) {
        try {
            chargingRuntimeBridge.setPile(pileId);
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "setChargePile failed", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void release() {
        Log.d(TAG, "release ChargerService");
        if (ownsChargingRuntimeBridge) {
            chargingRuntimeBridge.release();
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
}
