package com.weigao.robot.control.core.state;

import com.weigao.robot.control.model.TaskTiming;

/**
 * 计时控制器
 * <p>
 * 从 {@link TaskTiming} 模型类中提取的业务逻辑，负责管理计时状态。
 * 满足需求书第2章"作业过程计量功能"要求：
 * <ul>
 * <li>计时起始点：配送开始时刻</li>
 * <li>计时终止点：机器人到达目的地并再次打开舱门取用物品时刻</li>
 * </ul>
 * </p>
 */
public class TimingController {

    private final TaskTiming timing;

    /**
     * 创建计时控制器
     *
     * @param taskId 关联的任务ID
     */
    public TimingController(String taskId) {
        this.timing = new TaskTiming(taskId);
    }

    /**
     * 使用现有的TaskTiming创建控制器
     *
     * @param timing 现有的计时模型
     */
    public TimingController(TaskTiming timing) {
        this.timing = timing;
    }

    /**
     * 开始计时
     */
    public void start() {
        timing.setStartTime(System.currentTimeMillis());
        timing.setStatus(TaskTiming.TimingStatus.RUNNING);
        timing.setPausedDuration(0);
    }

    /**
     * 暂停计时
     */
    public void pause() {
        if (timing.getStatus() == TaskTiming.TimingStatus.RUNNING) {
            timing.setLastPauseTime(System.currentTimeMillis());
            timing.setStatus(TaskTiming.TimingStatus.PAUSED);
        }
    }

    /**
     * 恢复计时
     */
    public void resume() {
        if (timing.getStatus() == TaskTiming.TimingStatus.PAUSED) {
            long pausedTime = System.currentTimeMillis() - timing.getLastPauseTime();
            timing.setPausedDuration(timing.getPausedDuration() + pausedTime);
            timing.setStatus(TaskTiming.TimingStatus.RUNNING);
        }
    }

    /**
     * 停止计时
     */
    public void stop() {
        long now = System.currentTimeMillis();
        timing.setEndTime(now);

        if (timing.getStatus() == TaskTiming.TimingStatus.PAUSED) {
            long pausedTime = now - timing.getLastPauseTime();
            timing.setPausedDuration(timing.getPausedDuration() + pausedTime);
        }

        long elapsed = timing.getEndTime() - timing.getStartTime() - timing.getPausedDuration();
        timing.setElapsedTime(elapsed);
        timing.setStatus(TaskTiming.TimingStatus.STOPPED);
    }

    /**
     * 获取当前已用时间（不含暂停时间）
     *
     * @return 已用时间（毫秒）
     */
    public long getCurrentElapsedTime() {
        TaskTiming.TimingStatus status = timing.getStatus();

        if (status == TaskTiming.TimingStatus.NOT_STARTED) {
            return 0;
        }
        if (status == TaskTiming.TimingStatus.STOPPED) {
            return timing.getElapsedTime();
        }

        long currentPausedDuration = timing.getPausedDuration();
        if (status == TaskTiming.TimingStatus.PAUSED) {
            currentPausedDuration += System.currentTimeMillis() - timing.getLastPauseTime();
        }
        return System.currentTimeMillis() - timing.getStartTime() - currentPausedDuration;
    }

    /**
     * 获取格式化的已用时间
     *
     * @return 格式化的时间字符串（HH:mm:ss）
     */
    public String getFormattedElapsedTime() {
        long millis = getCurrentElapsedTime();
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        return String.format("%02d:%02d:%02d",
                hours,
                minutes % 60,
                seconds % 60);
    }

    /**
     * 获取计时状态
     *
     * @return 当前计时状态
     */
    public TaskTiming.TimingStatus getStatus() {
        return timing.getStatus();
    }

    /**
     * 获取关联的计时模型
     *
     * @return 计时模型
     */
    public TaskTiming getTiming() {
        return timing;
    }

    /**
     * 判断计时是否正在进行中
     *
     * @return true=计时中
     */
    public boolean isRunning() {
        return timing.getStatus() == TaskTiming.TimingStatus.RUNNING;
    }

    /**
     * 判断计时是否已暂停
     *
     * @return true=已暂停
     */
    public boolean isPaused() {
        return timing.getStatus() == TaskTiming.TimingStatus.PAUSED;
    }

    /**
     * 判断计时是否已停止
     *
     * @return true=已停止
     */
    public boolean isStopped() {
        return timing.getStatus() == TaskTiming.TimingStatus.STOPPED;
    }
}
