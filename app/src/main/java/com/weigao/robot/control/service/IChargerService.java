package com.weigao.robot.control.service;

import com.keenon.sdk.component.charger.PeanutCharger;
import com.weigao.robot.control.callback.IResultCallback;

/**
 * Charging action service aligned with {@code PeanutCharger}.
 * State reads are exposed by {@link IRobotStateService}.
 */
public interface IChargerService {

    int CHARGE_ACTION_AUTO = PeanutCharger.CHARGE_ACTION_AUTO;
    int CHARGE_ACTION_MANUAL = PeanutCharger.CHARGE_ACTION_MANUAL;
    int CHARGE_ACTION_ADAPTER = PeanutCharger.CHARGE_ACTION_ADAPTER;
    int CHARGE_ACTION_STOP = PeanutCharger.CHARGE_ACTION_STOP;

    void startAutoCharge(IResultCallback<Void> callback);

    void startManualCharge(IResultCallback<Void> callback);

    void startAdapterCharge(IResultCallback<Void> callback);

    void stopCharge(IResultCallback<Void> callback);

    void performAction(int action, IResultCallback<Void> callback);

    void setChargePile(int pileId, IResultCallback<Void> callback);

    void release();
}
