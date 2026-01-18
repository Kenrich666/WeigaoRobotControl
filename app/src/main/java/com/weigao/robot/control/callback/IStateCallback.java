package com.weigao.robot.control.callback;

import com.weigao.robot.control.model.RobotState;

/**
 * 机器人状态回调接口
 * <p>
 * 对齐SDK {@code PeanutRuntime.Listener}，用于接收机器人运行时状态变化通知。
 * </p>
 */
public interface IStateCallback {

    /**
     * 状态变化
     *
     * @param newState 新的机器人状态
     */
    void onStateChanged(RobotState newState);

    /**
     * 位置变化
     *
     * @param x X坐标，单位：米
     * @param y Y坐标，单位：米
     */
    void onLocationChanged(double x, double y);

    /**
     * 电量变化
     *
     * @param level 电量百分比（0-100）
     */
    void onBatteryLevelChanged(int level);

    /**
     * 急停按钮状态变化
     *
     * @param pressed true=按下，false=释放
     */
    void onScramButtonPressed(boolean pressed);
}
