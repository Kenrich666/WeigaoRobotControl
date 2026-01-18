package com.weigao.robot.control.core.device;

import com.weigao.robot.control.model.MotorInfo;
import com.weigao.robot.control.callback.IResultCallback;

public interface IMotorController {
    void getStatus(IResultCallback<MotorInfo> callback);

    void getHealth(IResultCallback<MotorInfo> callback);

    void enableMotor(boolean enabled, IResultCallback<Void> callback);

    void setSpeed(int speed, IResultCallback<Void> callback);

    void stopMotor(IResultCallback<Void> callback);
}
