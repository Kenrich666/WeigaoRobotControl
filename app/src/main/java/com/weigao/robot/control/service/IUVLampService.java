package com.weigao.robot.control.service;

import com.weigao.robot.control.callback.IResultCallback;

/**
 * 紫外灯服务接口
 * <p>
 * 封装 SDK {@code SensorUVLamp} 提供的紫外灯控制功能，
 * 主要用于机器人仓内消毒场景。
 * </p>
 */
public interface IUVLampService {

    // ==================== 设备 ID 常量 ====================
    // 参考 ProtoDev.SENSOR_UV_LAMP_*

    /** 紫外灯 1 号设备 ID */
    int UV_LAMP_1 = 44; // ProtoDev.SENSOR_UV_LAMP_1

    /** 紫外灯 2 号设备 ID */
    int UV_LAMP_2 = 45; // ProtoDev.SENSOR_UV_LAMP_2

    /** 紫外灯 3 号设备 ID */
    int UV_LAMP_3 = 46; // ProtoDev.SENSOR_UV_LAMP_3

    /**
     * 控制单个紫外灯开关
     *
     * @param dev    设备 ID，参见 {@link #UV_LAMP_1} 等常量
     * @param isOpen true 开启，false 关闭
     */
    void setUVLampSwitch(int dev, boolean isOpen);

    /**
     * 控制所有紫外灯开关
     * <p>
     * 同时开启或关闭所有 3 个紫外灯。
     * </p>
     *
     * @param isOpen true 开启，false 关闭
     */
    void setAllUVLampsSwitch(boolean isOpen);

    /**
     * 查询紫外灯状态
     *
     * @param dev      设备 ID
     * @param callback 结果回调，返回 true 表示开启，false 表示关闭
     */
    void getUVLampStatus(int dev, IResultCallback<Boolean> callback);

    /**
     * 释放资源
     */
    void release();
}
