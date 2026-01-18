package com.weigao.robot.control.service;

import com.weigao.robot.control.model.DeliveryTask;
import com.weigao.robot.control.model.DeliveryConfig;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.IProgressCallback;

public interface IDeliveryService {
    void startStandardDelivery(DeliveryTask task, IResultCallback<String> callback);

    void startLoopDelivery(DeliveryTask task, IResultCallback<String> callback);

    void startRecoveryDelivery(DeliveryTask task, IResultCallback<String> callback);

    void startSlowDelivery(DeliveryTask task, IResultCallback<String> callback);

    void pauseDelivery(String taskId, IResultCallback<Void> callback);

    void resumeDelivery(String taskId, IResultCallback<Void> callback);

    void cancelDelivery(String taskId, IResultCallback<Void> callback);

    void getDeliveryStatus(String taskId, IResultCallback<DeliveryTask> callback);

    void updateDeliveryConfig(DeliveryConfig config, IResultCallback<Void> callback);

    void getDeliveryConfig(IResultCallback<DeliveryConfig> callback);
}
