package com.weigao.robot.control.core.state;

import android.annotation.SuppressLint;
import android.os.SystemClock; // [新增] 引入 SystemClock

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
        // 使用 elapsedRealtime 防止系统时间修改导致计时错误
        timing.setStartTime(SystemClock.elapsedRealtime());
        timing.setStatus(TaskTiming.TimingStatus.RUNNING);
        timing.setPausedDuration(0);
        timing.setElapsedTime(0); // 重置已用时间
    }

    /**
     * 暂停计时
     */
    public void pause() {
        if (timing.getStatus() == TaskTiming.TimingStatus.RUNNING) {
            timing.setLastPauseTime(SystemClock.elapsedRealtime());
            timing.setStatus(TaskTiming.TimingStatus.PAUSED);
        }
    }

    /**
     * 恢复计时
     */
    public void resume() {
        if (timing.getStatus() == TaskTiming.TimingStatus.PAUSED) {
            long now = SystemClock.elapsedRealtime();
            long pausedTime = now - timing.getLastPauseTime();
            // 累加暂停时长
            timing.setPausedDuration(timing.getPausedDuration() + pausedTime);
            timing.setStatus(TaskTiming.TimingStatus.RUNNING);
        }
    }

    /**
     * 停止计时
     */
    public void stop() {
        // 防止重复停止
        if (timing.getStatus() == TaskTiming.TimingStatus.STOPPED) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        timing.setEndTime(now);

        // 如果是在暂停状态下停止，需要结算最后一段暂停时间
        if (timing.getStatus() == TaskTiming.TimingStatus.PAUSED) {
            long pausedTime = now - timing.getLastPauseTime();
            timing.setPausedDuration(timing.getPausedDuration() + pausedTime);
        }

        // 计算最终总耗时 = (结束 - 开始) - 总暂停时长
        long elapsed = timing.getEndTime() - timing.getStartTime() - timing.getPausedDuration();
        timing.setElapsedTime(Math.max(0, elapsed)); // 防止负数
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

        long now = SystemClock.elapsedRealtime();
        long currentPausedDuration = timing.getPausedDuration();

        // 如果处于暂停中，当前已流逝的暂停时间也要算进去，以便从总时长中扣除
        // 效果：(now - start) - (historyPaused + currentPausedSession) = 冻结的 active time
        if (status == TaskTiming.TimingStatus.PAUSED) {
            currentPausedDuration += now - timing.getLastPauseTime();
        }

        long elapsed = now - timing.getStartTime() - currentPausedDuration;
        return Math.max(0, elapsed);
    }

    /**
     * 获取格式化的已用时间
     *
     * @return 格式化的时间字符串（HH:mm:ss）
     */
    @SuppressLint("DefaultLocale")
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
