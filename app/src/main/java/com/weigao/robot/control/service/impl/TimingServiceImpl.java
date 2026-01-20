package com.weigao.robot.control.service.impl;

import android.util.Log;

import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.core.state.TimingController;
import com.weigao.robot.control.model.TaskTiming;
import com.weigao.robot.control.service.ITimingService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 作业计时服务实现类
 * <p>
 * 封装 {@link TimingController}，提供任务计时功能。
 * </p>
 */
public class TimingServiceImpl implements ITimingService {

    private static final String TAG = "TimingServiceImpl";

    /** 计时控制器映射（taskId -> TimingController） */
    private final Map<String, TimingController> controllers = new HashMap<>();

    /** 计时历史记录 */
    private final List<TaskTiming> timingHistory = new ArrayList<>();

    /** 舱门打开时是否自动停止计时 */
    private boolean autoStopOnDoorOpen = true;

    public TimingServiceImpl() {
        Log.d(TAG, "TimingServiceImpl 已创建");
    }

    @Override
    public void startTiming(String taskId, IResultCallback<Void> callback) {
        Log.d(TAG, "startTiming: " + taskId);
        if (taskId == null || taskId.isEmpty()) {
            notifyError(callback, -1, "任务ID不能为空");
            return;
        }

        TimingController controller = new TimingController(taskId);
        controller.start();
        controllers.put(taskId, controller);
        notifySuccess(callback);
    }

    @Override
    public void stopTiming(String taskId, IResultCallback<TaskTiming> callback) {
        Log.d(TAG, "stopTiming: " + taskId);
        TimingController controller = controllers.get(taskId);
        if (controller != null) {
            controller.stop();
            TaskTiming timing = controller.getTiming();
            timingHistory.add(timing);
            controllers.remove(taskId);
            if (callback != null) {
                callback.onSuccess(timing);
            }
        } else {
            notifyError(callback, -1, "未找到对应的计时任务");
        }
    }

    @Override
    public void pauseTiming(String taskId, IResultCallback<Void> callback) {
        Log.d(TAG, "pauseTiming: " + taskId);
        TimingController controller = controllers.get(taskId);
        if (controller != null) {
            controller.pause();
            notifySuccess(callback);
        } else {
            notifyError(callback, -1, "未找到对应的计时任务");
        }
    }

    @Override
    public void resumeTiming(String taskId, IResultCallback<Void> callback) {
        Log.d(TAG, "resumeTiming: " + taskId);
        TimingController controller = controllers.get(taskId);
        if (controller != null) {
            controller.resume();
            notifySuccess(callback);
        } else {
            notifyError(callback, -1, "未找到对应的计时任务");
        }
    }

    @Override
    public void getCurrentTiming(String taskId, IResultCallback<TaskTiming> callback) {
        TimingController controller = controllers.get(taskId);
        if (callback != null) {
            if (controller != null) {
                callback.onSuccess(controller.getTiming());
            } else {
                notifyError(callback, -1, "未找到对应的计时任务");
            }
        }
    }

    @Override
    public void getTimingHistory(String taskId, IResultCallback<List<TaskTiming>> callback) {
        if (callback != null) {
            if (taskId == null) {
                callback.onSuccess(new ArrayList<>(timingHistory));
            } else {
                List<TaskTiming> filtered = new ArrayList<>();
                for (TaskTiming timing : timingHistory) {
                    if (taskId.equals(timing.getTaskId())) {
                        filtered.add(timing);
                    }
                }
                callback.onSuccess(filtered);
            }
        }
    }

    @Override
    public void clearTimingHistory(String taskId, IResultCallback<Void> callback) {
        Log.d(TAG, "clearTimingHistory: " + taskId);
        if (taskId == null) {
            timingHistory.clear();
        } else {
            timingHistory.removeIf(timing -> taskId.equals(timing.getTaskId()));
        }
        notifySuccess(callback);
    }

    @Override
    public void setAutoStopOnDoorOpen(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "setAutoStopOnDoorOpen: " + enabled);
        this.autoStopOnDoorOpen = enabled;
        notifySuccess(callback);
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "释放 TimingService 资源");
        // 停止所有计时
        for (TimingController controller : controllers.values()) {
            controller.stop();
        }
        controllers.clear();
    }

    // ==================== 辅助方法 ====================

    private void notifySuccess(IResultCallback<Void> callback) {
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    private void notifyError(IResultCallback<?> callback, int code, String message) {
        if (callback != null) {
            callback.onError(new com.weigao.robot.control.callback.ApiError(code, message));
        }
    }
}
