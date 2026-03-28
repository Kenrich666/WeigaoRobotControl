package com.weigao.robot.control.manager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.keenon.sdk.component.navigation.route.RouteNode;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.INavigationCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.NavigationState;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.model.WorkSchedule;
import com.weigao.robot.control.service.IChargerService;
import com.weigao.robot.control.service.INavigationService;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.ui.main.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 工作时段调度服务
 * <p>
 * 根据工作时段设置，在上班时间自动导航到原点，下班时间自动回充电桩充电。
 * 使用 AlarmManager 实现精确定时触发。
 * </p>
 */
public class WorkScheduleService {

    private static final String TAG = "WorkScheduleService";

    private static final String ACTION_WORK_START = "com.weigao.robot.WORK_SCHEDULE_START";
    private static final String ACTION_WORK_END = "com.weigao.robot.WORK_SCHEDULE_END";
    private static final String EXTRA_SCHEDULE_INDEX = "schedule_index";

    // PendingIntent 请求码基准值，避免与其他业务冲突
    private static final int REQUEST_CODE_BASE_START = 5000;
    private static final int REQUEST_CODE_BASE_END = 5100;

    private static WorkScheduleService instance;

    private enum DeferredAction {
        NONE,
        WORK_START,
        WORK_END
    }

    private Context context;
    private AlarmManager alarmManager;
    private boolean isRegistered = false;
    private boolean pendingReturnToMainAfterWorkStart;
    private DeferredAction deferredAction = DeferredAction.NONE;
    private int deferredScheduleIndex = -1;

    private final INavigationCallback workStartNavigationCallback = new INavigationCallback() {
        @Override
        public void onStateChanged(int state, int schedule) {
            if (!pendingReturnToMainAfterWorkStart) {
                return;
            }

            if (state == NavigationState.STATE_DESTINATION) {
                pendingReturnToMainAfterWorkStart = false;
                unregisterWorkStartNavigationCallback();
                navigateToMainPage();
            } else if (state == NavigationState.STATE_STOPPED
                    || state == NavigationState.STATE_ERROR
                    || state == NavigationState.STATE_END) {
                pendingReturnToMainAfterWorkStart = false;
                unregisterWorkStartNavigationCallback();
            }
        }

        @Override
        public void onRouteNode(int index, NavigationNode node) {
        }

        @Override
        public void onRoutePrepared(List<NavigationNode> nodes) {
        }

        @Override
        public void onDistanceChanged(double distance) {
        }

        @Override
        public void onNavigationError(int errorCode) {
            pendingReturnToMainAfterWorkStart = false;
            unregisterWorkStartNavigationCallback();
        }

        @Override
        public void onError(int errorCode, String message) {
            pendingReturnToMainAfterWorkStart = false;
            unregisterWorkStartNavigationCallback();
        }
    };

    private WorkScheduleService() {
    }

    public static synchronized WorkScheduleService getInstance() {
        if (instance == null) {
            instance = new WorkScheduleService();
        }
        return instance;
    }

    /**
     * 初始化调度服务
     * 应在 SDK 初始化成功后调用
     */
    public void init(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);

        // 注册广播接收器
        if (!isRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_WORK_START);
            filter.addAction(ACTION_WORK_END);
            ContextCompat.registerReceiver(this.context, scheduleReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            isRegistered = true;
        }

        // 设置所有闹钟
        rescheduleAll();
        Log.i(TAG, "工作时段调度服务已初始化");
    }

    /**
     * 重新调度所有工作时段闹钟
     * 在设置变更后调用此方法
     */
    public void rescheduleAll() {
        if (context == null || alarmManager == null) {
            Log.w(TAG, "调度服务未初始化");
            return;
        }

        // 先取消所有现有闹钟
        cancelAllAlarms();

        // 重新设置
        List<WorkSchedule> schedules = WorkScheduleSettingsManager.getInstance().getSchedules();
        for (int i = 0; i < schedules.size(); i++) {
            WorkSchedule schedule = schedules.get(i);
            if (schedule.isEnabled()) {
                scheduleAlarm(i, schedule);
                Log.i(TAG, "时段 " + (i + 1) + " 已启用调度: " +
                        schedule.getStartTime() + " ~ " + schedule.getEndTime() +
                        " [" + schedule.getWorkDaysDescription() + "]");
            }
        }
    }

    /**
     * 为指定时段设置闹钟
     */
    private void scheduleAlarm(int index, WorkSchedule schedule) {
        // 设置上班闹钟
        scheduleNextAlarm(index, schedule.getStartHour(), schedule.getStartMinute(),
                schedule.getWorkDays(), ACTION_WORK_START, REQUEST_CODE_BASE_START + index);

        // 设置下班闹钟
        scheduleNextAlarm(index, schedule.getEndHour(), schedule.getEndMinute(),
                schedule.getWorkDays(), ACTION_WORK_END, REQUEST_CODE_BASE_END + index);
    }

    /**
     * 计算下一次触发时间并设置闹钟
     */
    private void scheduleNextAlarm(int scheduleIndex, int hour, int minute,
                                    boolean[] workDays, String action, int requestCode) {
        long nextTriggerTime = calculateNextTriggerTime(hour, minute, workDays);
        if (nextTriggerTime < 0) {
            Log.w(TAG, "无法计算下一次触发时间，action=" + action);
            return;
        }

        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_SCHEDULE_INDEX, scheduleIndex);
        intent.setPackage(context.getPackageName());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 使用 setExactAndAllowWhileIdle 确保在低电量模式下也能触发
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent);
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nextTriggerTime);
        Log.d(TAG, "闹钟已设置: action=" + action + ", 触发时间=" +
                String.format("%tF %tT", cal, cal));
    }

    /**
     * 计算下一次触发时间
     * @return 毫秒时间戳，如果没有可用的工作日则返回 -1
     */
    private long calculateNextTriggerTime(int hour, int minute, boolean[] workDays) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        // 检查未来7天内的工作日
        for (int daysAhead = 0; daysAhead < 7; daysAhead++) {
            Calendar candidate = (Calendar) target.clone();
            candidate.add(Calendar.DAY_OF_YEAR, daysAhead);

            // Calendar.DAY_OF_WEEK: 1=周日, 2=周一, ..., 7=周六
            int dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK);
            // 转换为 workDays 索引: 0=周一, 1=周二, ..., 6=周日
            int workDayIndex = dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek - 2;

            if (workDayIndex >= 0 && workDayIndex < 7 && workDays[workDayIndex]) {
                if (candidate.after(now)) {
                    return candidate.getTimeInMillis();
                }
            }
        }

        return -1;
    }

    /**
     * 取消所有闹钟
     */
    private void cancelAllAlarms() {
        for (int i = 0; i < WorkScheduleSettingsManager.MAX_SCHEDULES; i++) {
            cancelAlarm(REQUEST_CODE_BASE_START + i, ACTION_WORK_START);
            cancelAlarm(REQUEST_CODE_BASE_END + i, ACTION_WORK_END);
        }
    }

    private void cancelAlarm(int requestCode, String action) {
        Intent intent = new Intent(action);
        intent.setPackage(context.getPackageName());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }

    /**
     * 广播接收器：处理上班/下班闹钟触发
     */
    private final BroadcastReceiver scheduleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            String action = intent.getAction();
            int scheduleIndex = intent.getIntExtra(EXTRA_SCHEDULE_INDEX, -1);
            Log.i(TAG, "收到工作时段广播: action=" + action + ", index=" + scheduleIndex);

            if (ACTION_WORK_START.equals(action)) {
                handleWorkStart(scheduleIndex);
            } else if (ACTION_WORK_END.equals(action)) {
                handleWorkEnd(scheduleIndex);
            }

            // 重新调度下一次闹钟（因为 setExact 是一次性的）
            rescheduleAll();
        }
    };

    /**
     * 处理上班时间到达 → 导航到原点
     */
    private void handleWorkStart(int scheduleIndex) {
        if (deferActionIfBusy(DeferredAction.WORK_START, scheduleIndex)) {
            return;
        }

        executeWorkStart(scheduleIndex);
    }

    private void executeWorkStart(int scheduleIndex) {
        Log.i(TAG, "执行上班动作: 时段 " + (scheduleIndex + 1) + " -> 自动前往原点");
        Log.i(TAG, "【上班时间】时段 " + (scheduleIndex + 1) + " 触发 → 自动前往原点");

        IChargerService chargerService = ServiceManager.getInstance().getChargerService();
        if (chargerService != null) {
            // 先停止充电
            chargerService.stopCharge(new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "停止充电成功，准备导航到原点");
                    navigateToOrigin();
                }

                @Override
                public void onError(ApiError error) {
                    Log.w(TAG, "停止充电失败（可能不在充电中）: " + error.getMessage());
                    // 仍然尝试导航
                    navigateToOrigin();
                }
            });
        } else {
            navigateToOrigin();
        }
    }

    /**
     * 导航到原点
     */
    private void navigateToOrigin() {
        INavigationService navigationService = ServiceManager.getInstance().getNavigationService();
        if (navigationService == null) {
            Log.e(TAG, "Navigation service unavailable, cannot go to origin.");
            return;
        }

        loadOriginNode(new IResultCallback<NavigationNode>() {
            @Override
            public void onSuccess(NavigationNode originNode) {
                if (originNode == null) {
                    Log.e(TAG, "Origin node not found.");
                    return;
                }

                List<NavigationNode> targets = new ArrayList<>();
                targets.add(originNode);

                final String nodeName = originNode.getName();
                registerWorkStartNavigationCallback(navigationService);
                navigationService.setTargetNodes(targets, new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        navigationService.prepare(new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                navigationService.start(new IResultCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        Log.i(TAG, "Work schedule navigation to origin started: " + nodeName);
                                    }

                                    @Override
                                    public void onError(ApiError error) {
                                        pendingReturnToMainAfterWorkStart = false;
                                        unregisterWorkStartNavigationCallback();
                                        Log.e(TAG, "Failed to start navigation to origin: " + error.getMessage());
                                    }
                                });
                            }

                            @Override
                            public void onError(ApiError error) {
                                pendingReturnToMainAfterWorkStart = false;
                                unregisterWorkStartNavigationCallback();
                                Log.e(TAG, "Failed to prepare navigation to origin: " + error.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        pendingReturnToMainAfterWorkStart = false;
                        unregisterWorkStartNavigationCallback();
                        Log.e(TAG, "Failed to set origin navigation target: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "Failed to load origin node: " + error.getMessage());
            }
        });
    }

    private void loadOriginNode(IResultCallback<NavigationNode> callback) {
        IRobotStateService robotStateService = ServiceManager.getInstance().getRobotStateService();
        if (robotStateService == null) {
            callback.onError(new ApiError(-1, "robotStateService unavailable"));
            return;
        }

        robotStateService.getDestinationList(new IResultCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    callback.onSuccess(parseOriginNode(result));
                } catch (JSONException e) {
                    callback.onError(new ApiError(-1, e.getMessage()));
                }
            }

            @Override
            public void onError(ApiError error) {
                callback.onError(error);
            }
        });
    }

    private NavigationNode parseOriginNode(String result) throws JSONException {
        if (result == null || result.isEmpty()) {
            return null;
        }

        JSONObject resultObj = new JSONObject(result);
        JSONArray jsonArray = resultObj.optJSONArray("data");
        if (jsonArray == null) {
            return null;
        }

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.optJSONObject(i);
            if (obj == null || !"origin".equals(obj.optString("type"))) {
                continue;
            }
            return buildNavigationNode(obj);
        }

        return null;
    }

    private NavigationNode buildNavigationNode(JSONObject obj) {
        NavigationNode node = new NavigationNode();
        int id = obj.optInt("id");
        String name = obj.optString("name");
        if (name.isEmpty()) {
            name = String.valueOf(id);
        }

        node.setId(id);
        node.setName(name);
        node.setFloor(obj.optInt("floor"));

        JSONObject pose = obj.optJSONObject("pose");
        if (pose != null) {
            JSONObject position = pose.optJSONObject("position");
            if (position != null) {
                node.setX(position.optDouble("x"));
                node.setY(position.optDouble("y"));
            }
            JSONObject orientation = pose.optJSONObject("orientation");
            if (orientation != null) {
                double w = orientation.optDouble("w");
                double x = orientation.optDouble("x");
                double y = orientation.optDouble("y");
                double z = orientation.optDouble("z");
                double phi = Math.atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z));
                node.setPhi(phi);
            }
        }

        RouteNode routeNode = new RouteNode();
        routeNode.setId(id);
        routeNode.setName(name);
        if (routeNode.getNavigationInfo() != null) {
            routeNode.getNavigationInfo().setTotalDistance(99999f);
            routeNode.getNavigationInfo().setRemainDistance(99999f);
            routeNode.getNavigationInfo().setTotalTime(99999f);
            routeNode.getNavigationInfo().setRemainTime(99999f);
        }
        node.setRouteNode(routeNode);
        return node;
    }

    private void registerWorkStartNavigationCallback(INavigationService navigationService) {
        unregisterWorkStartNavigationCallback();
        pendingReturnToMainAfterWorkStart = true;
        navigationService.registerCallback(workStartNavigationCallback);
    }

    private void unregisterWorkStartNavigationCallback() {
        INavigationService navigationService = ServiceManager.getInstance().getNavigationService();
        if (navigationService != null) {
            navigationService.unregisterCallback(workStartNavigationCallback);
        }
    }

    private void navigateToMainPage() {
        if (context == null) {
            return;
        }

        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_SKIP_POSITIONING, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to main page after work start arrival", e);
        }
    }

    private void handleWorkEnd(int scheduleIndex) {
        if (deferActionIfBusy(DeferredAction.WORK_END, scheduleIndex)) {
            return;
        }

        executeWorkEnd(scheduleIndex);
    }

    private void executeWorkEnd(int scheduleIndex) {
        Log.i(TAG, "执行下班动作: 时段 " + (scheduleIndex + 1) + " -> 自动回充电桩");
        Log.i(TAG, "【下班时间】时段 " + (scheduleIndex + 1) + " 触发 → 自动回充电桩");

        IChargerService chargerService = ServiceManager.getInstance().getChargerService();
        if (chargerService != null) {
            chargerService.startAutoCharge(new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.i(TAG, "已启动自动回充任务");
                }

                @Override
                public void onError(ApiError error) {
                    Log.e(TAG, "启动自动回充失败: " + error.getMessage());
                }
            });
        } else {
            Log.e(TAG, "充电服务不可用，无法自动回充");
        }
    }

    /**
     * 释放资源
     */
    private boolean deferActionIfBusy(DeferredAction action, int scheduleIndex) {
        if (!TaskExecutionStateManager.getInstance().hasActiveTask()) {
            return false;
        }

        synchronized (this) {
            // Keep only one deferred action. A later trigger replaces the previous one.
            deferredAction = action;
            deferredScheduleIndex = scheduleIndex;
        }
        Log.i(TAG, "检测到任务执行中，挂起工作时段动作: action=" + action + ", schedule=" + (scheduleIndex + 1));
        return true;
    }

    public synchronized boolean hasDeferredWorkEnd() {
        return deferredAction == DeferredAction.WORK_END;
    }

    public boolean executeDeferredActionIfIdle() {
        DeferredAction actionToRun;
        int scheduleIndexToRun;

        synchronized (this) {
            if (deferredAction == DeferredAction.NONE || TaskExecutionStateManager.getInstance().hasActiveTask()) {
                return false;
            }
            actionToRun = deferredAction;
            scheduleIndexToRun = deferredScheduleIndex;
            deferredAction = DeferredAction.NONE;
            deferredScheduleIndex = -1;
        }

        Log.i(TAG, "任务结束，执行挂起的工作时段动作: action=" + actionToRun
                + ", schedule=" + (scheduleIndexToRun + 1));
        if (actionToRun == DeferredAction.WORK_START) {
            executeWorkStart(scheduleIndexToRun);
        } else if (actionToRun == DeferredAction.WORK_END) {
            executeWorkEnd(scheduleIndexToRun);
        }
        return true;
    }

    public void release() {
        if (context != null && isRegistered) {
            try {
                context.unregisterReceiver(scheduleReceiver);
            } catch (Exception e) {
                Log.w(TAG, "注销广播失败", e);
            }
            isRegistered = false;
        }
        cancelAllAlarms();
        deferredAction = DeferredAction.NONE;
        deferredScheduleIndex = -1;
        pendingReturnToMainAfterWorkStart = false;
    }
}
