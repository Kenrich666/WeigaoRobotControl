package com.weigao.robot.control.callback;

import com.weigao.robot.control.model.ChargingState;
import com.weigao.robot.control.model.RobotState;

/**
 * Runtime state callback aligned with {@code PeanutRuntime.Listener}.
 */
public interface IStateCallback {

    void onStateChanged(RobotState newState);

    void onLocationChanged(double x, double y);

    void onBatteryLevelChanged(int level);

    void onChargingStateChanged(ChargingState chargingState);

    void onScramButtonPressed(boolean pressed);
}
