package com.weigao.robot.control.core.device;

import com.weigao.robot.control.model.DeviceInfo;
import com.weigao.robot.control.callback.IResultCallback;

public interface IRobotDevice {
    void getDeviceList(IResultCallback<DeviceInfo> callback);

    void getBoardInfo(String board, IResultCallback<DeviceInfo> callback);

    void getConfig(IResultCallback<String> callback);

    void updateConfig(String params, IResultCallback<String> callback);

    void reboot(IResultCallback<Void> callback);

    void getScramButtonStatus(IResultCallback<Boolean> callback);

    void openFan(int fanId, IResultCallback<Void> callback);

    void closeFan(int fanId, IResultCallback<Void> callback);
}
