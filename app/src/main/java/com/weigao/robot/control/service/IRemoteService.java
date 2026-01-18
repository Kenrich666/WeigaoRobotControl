package com.weigao.robot.control.service;

import com.weigao.robot.control.model.PointInfo;
import com.weigao.robot.control.callback.IResultCallback;

public interface IRemoteService {
    void enableRemoteCall(boolean enabled, IResultCallback<Void> callback);

    void callLoop(PointInfo targetPoint, int duration, IResultCallback<String> callback);

    void callArrival(PointInfo targetPoint, int duration, IResultCallback<String> callback);

    void callRecovery(IResultCallback<String> callback);

    void setStayDuration(int durationSeconds, IResultCallback<Void> callback);

    void getStayDuration(IResultCallback<Integer> callback);

    void isRemoteCallEnabled(IResultCallback<Boolean> callback);
}
