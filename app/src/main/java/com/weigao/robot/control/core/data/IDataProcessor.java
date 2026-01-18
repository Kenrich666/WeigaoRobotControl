package com.weigao.robot.control.core.data;

import com.weigao.robot.control.model.DeviceInfo;
import com.weigao.robot.control.model.MotorInfo;
import com.weigao.robot.control.model.PointInfo;
import com.weigao.robot.control.callback.IResultCallback;

public interface IDataProcessor {
    <T> T parseResponse(String jsonResponse, Class<T> clazz);

    String serializeRequest(Object request);

    boolean validateData(String data);

    DeviceInfo parseDeviceInfo(String jsonData);

    MotorInfo parseMotorInfo(String jsonData);

    PointInfo parsePointInfo(String jsonData);
}
