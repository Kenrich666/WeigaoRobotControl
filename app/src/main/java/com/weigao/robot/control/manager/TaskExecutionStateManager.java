package com.weigao.robot.control.manager;

/**
 * 统一维护当前是否存在进行中的配送任务。
 */
public class TaskExecutionStateManager {

    private static final TaskExecutionStateManager INSTANCE = new TaskExecutionStateManager();

    private TaskType activeTaskType;

    private TaskExecutionStateManager() {
    }

    public static TaskExecutionStateManager getInstance() {
        return INSTANCE;
    }

    public synchronized void startTask(TaskType taskType) {
        activeTaskType = taskType;
    }

    public synchronized void finishTask() {
        activeTaskType = null;
    }

    public synchronized void cancelTask() {
        activeTaskType = null;
    }

    public synchronized boolean hasActiveTask() {
        return activeTaskType != null;
    }

    public synchronized TaskType getActiveTaskType() {
        return activeTaskType;
    }

    synchronized void resetForTest() {
        activeTaskType = null;
    }
}
