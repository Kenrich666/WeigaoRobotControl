package com.weigao.robot.control.service.impl;

import android.util.Log;

import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.core.state.TimingController;
import com.weigao.robot.control.model.DoorType;
import com.weigao.robot.control.model.TaskTiming;
import com.weigao.robot.control.service.IDoorService; // 假设存在
import com.weigao.robot.control.service.ITimingService;
import com.weigao.robot.control.service.ServiceManager; // 用于获取DoorService

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.weigao.robot.control.callback.IDoorCallback;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.ServiceManager;

/**
 * 作业计时服务实现类
 * <p>
 * 封装 {@link TimingController}，提供任务计时功能。
 * </p>
 */
public class TimingServiceImpl implements ITimingService {

    private static final String TAG = "TimingServiceImpl";

    /**
     * 计时控制器映射（taskId -> TimingController）
     */
    private final Map<String, TimingController> controllers = new ConcurrentHashMap<>();
    /**
     * 计时历史记录
     */
    private final List<TaskTiming> timingHistory = new CopyOnWriteArrayList<>();

    /**
     * 舱门打开时是否自动停止计时
     */
    private volatile boolean autoStopOnDoorOpen = true;
    // 持有 DoorService 引用用于注册监听
    private IDoorService doorService;

    public TimingServiceImpl() {
        Log.d(TAG, "TimingServiceImpl 已创建");
        initDoorListener();
    }

    // 添加舱门回调监听对象
    private final IDoorCallback doorStateListener = new IDoorCallback() {
        @Override
        public void onDoorStateChanged(int doorId, int state) {
            // SDK 舱门状态定义通常为：1=Opened, 2=Closed, 3=Opening, 4=Closing
            // 请根据实际 IDoorCallback 常量定义调整，此处假设 1 为 OPENED
            final int STATE_OPENED = 1;

            if (state == STATE_OPENED && autoStopOnDoorOpen) {
                Log.i(TAG, "监测到舱门[" + doorId + "]打开，且自动停止开关已开启 -> 停止所有计时");
                stopAllTimings();
            }
        }

        @Override
        public void onDoorTypeChanged(DoorType type) {
            // 不需要处理
        }

        // [修复] 补全 onDoorTypeSettingResult 方法以解决编译错误
        @Override
        public void onDoorTypeSettingResult(boolean success) {
            // 不需要处理，仅为满足接口实现
        }

        @Override
        public void onDoorError(int doorId, int errorCode) {
            // 不需要处理
        }
    };

    /**
     * 初始化舱门监听
     */
    private void initDoorListener() {
        try {
            IDoorService doorService = ServiceManager.getInstance().getDoorService();
            if (doorService != null) {
                doorService.registerCallback(doorStateListener);
                Log.d(TAG, "舱门状态监听已注册");
            } else {
                Log.w(TAG, "DoorService 未初始化，无法自动停止计时");
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化舱门监听失败", e);
        }
    }

    @Override
    public void startTiming(String taskId, IResultCallback<Void> callback) {
        Log.d(TAG, "startTiming: " + taskId);
        if (taskId == null || taskId.isEmpty()) {
            notifyError(callback, -1, "任务ID不能为空");
            return;
        }

        // [修复] 防止覆盖正在运行的任务
        if (controllers.containsKey(taskId)) {
            Log.w(TAG, "任务 " + taskId + " 正在计时中，将重启计时");
            TimingController oldController = controllers.get(taskId);
            if (oldController != null) oldController.stop();
        }

        TimingController controller = new TimingController(taskId);
        controller.start();
        controllers.put(taskId, controller);
        notifySuccess(callback);
    }

    @Override
    public void stopTiming(String taskId, IResultCallback<TaskTiming> callback) {
        Log.d(TAG, "stopTiming: " + taskId);
        TimingController controller = controllers.remove(taskId); // 原子操作：获取并移除
        if (controller != null) {
            controller.stop();
            TaskTiming timing = controller.getTiming();
            timingHistory.add(timing);
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
            // removeIf 需要 API 24+，如果低于此版本需改用迭代器
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                timingHistory.removeIf(timing -> taskId.equals(timing.getTaskId()));
            } else {
                // 兼容低版本写法
                List<TaskTiming> toRemove = new ArrayList<>();
                for (TaskTiming t : timingHistory) {
                    if (taskId.equals(t.getTaskId())) toRemove.add(t);
                }
                timingHistory.removeAll(toRemove);
            }
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
     * 内部方法：停止所有计时（供舱门监听调用）
     */
    private void stopAllTimings() {
        for (String taskId : controllers.keySet()) {
            // 这里仅仅是停止并移除，不一定需要回调给上层，或者可以通过广播/事件总线通知
            TimingController controller = controllers.remove(taskId);
            if (controller != null) {
                controller.stop();
                timingHistory.add(controller.getTiming());
            }
        }
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
        // [新增] 注销监听，防止内存泄漏
        try {
            IDoorService doorService = ServiceManager.getInstance().getDoorService();
            if (doorService != null) {
                doorService.unregisterCallback(doorStateListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "注销舱门监听失败", e);
        }
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
