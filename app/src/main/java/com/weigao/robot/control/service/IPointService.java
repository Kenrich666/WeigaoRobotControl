package com.weigao.robot.control.service;

import com.weigao.robot.control.model.PointInfo;
import com.weigao.robot.control.callback.IResultCallback;

import java.util.List;

public interface IPointService {
    void getAllPoints(IResultCallback<List<PointInfo>> callback);

    void getRecentPoints(IResultCallback<List<PointInfo>> callback);

    void getPointsByFloor(int floor, IResultCallback<List<PointInfo>> callback);

    void addPoint(PointInfo point, IResultCallback<String> callback);

    void deletePoint(String pointId, IResultCallback<Void> callback);

    void updatePoint(PointInfo point, IResultCallback<Void> callback);

    void setReturnPoint(String pointId, IResultCallback<Void> callback);

    void setStandbyPoint(String pointId, IResultCallback<Void> callback);

    void getReturnPoint(IResultCallback<PointInfo> callback);

    void getStandbyPoint(IResultCallback<PointInfo> callback);
}
