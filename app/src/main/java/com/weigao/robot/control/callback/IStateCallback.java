package com.weigao.robot.control.callback;

import com.weigao.robot.control.model.RobotState;

public interface IStateCallback {
    void onStateChanged(RobotState newState);

    void onLocationChanged(double x, double y);

    void onBatteryLevelChanged(int level);

    void onScramButtonPressed(boolean pressed);
}
