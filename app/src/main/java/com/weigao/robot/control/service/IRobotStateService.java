package com.weigao.robot.control.service;

import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.IStateCallback;
import com.weigao.robot.control.model.ChargingState;
import com.weigao.robot.control.model.RobotState;

/**
 * Robot runtime state service aligned with {@code PeanutRuntime}.
 */
public interface IRobotStateService {

    int WORK_MODE_STANDBY = 0;
    int WORK_MODE_NAVIGATION = 1;
    int WORK_MODE_CHARGING = 2;
    int WORK_MODE_MANUAL = 3;

    void getRobotState(IResultCallback<RobotState> callback);

    void getBatteryLevel(IResultCallback<Integer> callback);

    void getChargingState(IResultCallback<ChargingState> callback);

    void getCurrentLocation(IResultCallback<RobotState.LocationInfo> callback);

    void isScramButtonPressed(IResultCallback<Boolean> callback);

    void getMotorStatus(IResultCallback<Integer> callback);

    void setWorkMode(int mode, IResultCallback<Void> callback);

    void setEmergencyEnabled(boolean enabled, IResultCallback<Void> callback);

    void setMotorEnabled(boolean enabled, IResultCallback<Void> callback);

    void syncParams(boolean needReboot, IResultCallback<Void> callback);

    void reboot(IResultCallback<Void> callback);

    void performLocalization(IResultCallback<Void> callback);

    void getTotalOdometer(IResultCallback<Double> callback);

    void getRobotIp(IResultCallback<String> callback);

    void getRobotArmInfo(IResultCallback<String> callback);

    void getRobotStm32Info(IResultCallback<String> callback);

    void getDestinationList(IResultCallback<String> callback);

    void getRobotProperties(IResultCallback<String> callback);

    void registerCallback(IStateCallback callback);

    void unregisterCallback(IStateCallback callback);

    void release();
}
