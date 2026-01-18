package com.weigao.robot.control.service;

import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.IStateCallback;
import com.weigao.robot.control.model.RobotState;

/**
 * 机器人状态服务接口
 * <p>
 * 对齐SDK {@code PeanutRuntime}，提供机器人运行时状态监控和控制功能。
 * </p>
 */
public interface IRobotStateService {

    // ==================== 工作模式常量 ====================

    /** 工作模式：待机 */
    int WORK_MODE_STANDBY = 0;

    /** 工作模式：导航 */
    int WORK_MODE_NAVIGATION = 1;

    /** 工作模式：充电 */
    int WORK_MODE_CHARGING = 2;

    /** 工作模式：手动控制 */
    int WORK_MODE_MANUAL = 3;

    // ==================== 状态查询 ====================

    /**
     * 获取当前机器人状态
     *
     * @param callback 结果回调
     */
    void getRobotState(IResultCallback<RobotState> callback);

    /**
     * 获取当前电量
     *
     * @param callback 结果回调，返回电量百分比（0-100）
     */
    void getBatteryLevel(IResultCallback<Integer> callback);

    /**
     * 获取当前位置
     *
     * @param callback 结果回调，返回包含x、y坐标的位置信息
     */
    void getCurrentLocation(IResultCallback<RobotState.LocationInfo> callback);

    /**
     * 查询急停按钮是否被按下
     *
     * @param callback 结果回调
     */
    void isScramButtonPressed(IResultCallback<Boolean> callback);

    /**
     * 查询电机状态
     *
     * @param callback 结果回调
     */
    void getMotorStatus(IResultCallback<Integer> callback);

    // ==================== 设备控制 ====================

    /**
     * 设置工作模式
     *
     * @param mode     工作模式，参见 {@link #WORK_MODE_STANDBY} 等常量
     * @param callback 结果回调
     */
    void setWorkMode(int mode, IResultCallback<Void> callback);

    /**
     * 设置急停按钮是否启用
     *
     * @param enabled  true=启用，false=禁用
     * @param callback 结果回调
     */
    void setEmergencyEnabled(boolean enabled, IResultCallback<Void> callback);

    /**
     * 设置电机使能
     *
     * @param enabled  true=启用，false=禁用
     * @param callback 结果回调
     */
    void setMotorEnabled(boolean enabled, IResultCallback<Void> callback);

    /**
     * 同步参数配置到机器人
     *
     * @param needReboot 是否需要重启生效
     * @param callback   结果回调
     */
    void syncParams(boolean needReboot, IResultCallback<Void> callback);

    /**
     * 重启机器人
     *
     * @param callback 结果回调
     */
    void reboot(IResultCallback<Void> callback);

    /**
     * 执行开机定位（SLAM定位）
     *
     * @param callback 结果回调
     */
    void performLocalization(IResultCallback<Void> callback);

    // ==================== 设备信息查询 ====================

    /**
     * 获取总里程数
     * <p>
     * 对齐SDK {@code RuntimeInfo.getTotalOdo()}。
     * </p>
     *
     * @param callback 结果回调，返回总里程（单位：米）
     */
    void getTotalOdometer(IResultCallback<Double> callback);

    /**
     * 获取机器人IP地址
     * <p>
     * 对齐SDK {@code RuntimeInfo.getRobotIp()}。
     * </p>
     *
     * @param callback 结果回调
     */
    void getRobotIp(IResultCallback<String> callback);

    /**
     * 获取算法板信息
     * <p>
     * 对齐SDK {@code RuntimeInfo.getRobotArmInfo()}。
     * </p>
     *
     * @param callback 结果回调
     */
    void getRobotArmInfo(IResultCallback<String> callback);

    /**
     * 获取运动板信息
     * <p>
     * 对齐SDK {@code RuntimeInfo.getRobotStm32Info()}。
     * </p>
     *
     * @param callback 结果回调
     */
    void getRobotStm32Info(IResultCallback<String> callback);

    /**
     * 获取所有目标点列表
     * <p>
     * 对齐SDK {@code RuntimeInfo.getDestList()}。
     * </p>
     *
     * @param callback 结果回调，返回目标点列表的JSON字符串
     */
    void getDestinationList(IResultCallback<String> callback);

    /**
     * 获取参数配置
     * <p>
     * 对齐SDK {@code RuntimeInfo.getRobotProperties()}。
     * </p>
     *
     * @param callback 结果回调，返回配置参数的JSON字符串
     */
    void getRobotProperties(IResultCallback<String> callback);

    // ==================== 回调注册 ====================

    /**
     * 注册状态回调
     * <p>
     * 注册后可接收机器人状态变化、位置变化、电量变化等实时通知。
     * 调用方需在适当时机（如Activity销毁时）调用 {@link #unregisterCallback(IStateCallback)}
     * 以避免内存泄漏。
     * </p>
     *
     * @param callback 状态回调
     */
    void registerCallback(IStateCallback callback);

    /**
     * 注销状态回调
     *
     * @param callback 状态回调
     */
    void unregisterCallback(IStateCallback callback);
}
