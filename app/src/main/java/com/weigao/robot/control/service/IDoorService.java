package com.weigao.robot.control.service;

import com.weigao.robot.control.callback.IDoorCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.DoorType;

/**
 * 舱门服务接口
 * <p>
 * 对齐SDK {@code PeanutDoor}，提供舱门控制功能。
 * </p>
 */
public interface IDoorService {

    /**
     * 打开指定舱门
     *
     * @param doorId   舱门ID（从1开始，按照从左到右、从上到下的顺序）
     * @param single   是否独占模式：true=单独打开此舱门（其他舱门自动关闭），false=仅开此门不影响其他
     * @param callback 结果回调
     */
    void openDoor(int doorId, boolean single, IResultCallback<Void> callback);

    /**
     * 关闭指定舱门
     *
     * @param doorId   舱门ID
     * @param callback 结果回调
     */
    void closeDoor(int doorId, IResultCallback<Void> callback);

    /**
     * 打开所有舱门
     *
     * @param single   是否独占模式（此参数在打开所有门时通常为 false）
     * @param callback 结果回调
     */
    void openAllDoors(boolean single, IResultCallback<Void> callback);

    /**
     * 关闭所有舱门
     *
     * @param callback 结果回调
     */
    void closeAllDoors(IResultCallback<Void> callback);

    /**
     * 查询所有舱门是否都已关闭
     *
     * @param callback 结果回调，返回true表示所有舱门已关闭
     */
    void isAllDoorsClosed(IResultCallback<Boolean> callback);

    /**
     * 获取指定舱门状态
     *
     * @param doorId   舱门ID
     * @param callback 结果回调，返回舱门状态码
     */
    void getDoorState(int doorId, IResultCallback<Integer> callback);

    /**
     * 获取所有舱门状态
     *
     * @param callback 结果回调，返回所有舱门的状态数组
     */
    void getAllDoorStates(IResultCallback<int[]> callback);

    /**
     * 设置舱门类型
     * <p>
     * 注意：调用前请先通过 {@link #supportDoorTypeSetting(IResultCallback)} 检查硬件是否支持此功能。
     * </p>
     *
     * @param type     舱门类型
     * @param callback 结果回调
     */
    void setDoorType(DoorType type, IResultCallback<Void> callback);

    /**
     * 获取当前舱门类型
     *
     * @param callback 结果回调
     */
    void getDoorType(IResultCallback<DoorType> callback);

    /**
     * 检查硬件是否支持舱门类型设置功能
     *
     * @param callback 结果回调，返回true表示支持
     */
    void supportDoorTypeSetting(IResultCallback<Boolean> callback);

    /**
     * 获取舱门控制固件版本号
     *
     * @param callback 结果回调
     */
    void getDoorVersion(IResultCallback<String> callback);

    /**
     * 设置脚踩灯光开关门功能是否启用
     *
     * @param enabled  true=启用，false=禁用
     * @param callback 结果回调
     */
    void setFootSwitchEnabled(boolean enabled, IResultCallback<Void> callback);

    /**
     * 设置到达后脚踩灯光自动离开功能是否启用
     *
     * @param enabled  true=启用，false=禁用
     * @param callback 结果回调
     */
    void setAutoLeaveEnabled(boolean enabled, IResultCallback<Void> callback);

    /**
     * 注册舱门状态回调
     *
     * @param callback 舱门回调
     */
    void registerCallback(IDoorCallback callback);

    /**
     * 注销舱门状态回调
     *
     * @param callback 舱门回调
     */
    void unregisterCallback(IDoorCallback callback);

    /**
     * 释放资源
     */
    void release();
}
