package com.weigao.robot.control.service;

import com.weigao.robot.control.callback.IResultCallback;

public interface IDoorService {
    void openDoor(int doorId, IResultCallback<Void> callback);

    void closeDoor(int doorId, IResultCallback<Void> callback);

    void verifyPassword(int doorId, String password, IResultCallback<Boolean> callback);

    void setFootSwitchEnabled(boolean enabled, IResultCallback<Void> callback);

    void setAutoLeaveEnabled(boolean enabled, IResultCallback<Void> callback);

    void getDoorStatus(int doorId, IResultCallback<Boolean> callback);

    void getAllDoorsStatus(IResultCallback<Boolean[]> callback);
}
