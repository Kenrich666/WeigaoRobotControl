package com.weigao.robot.control.service;

/**
 * 投影灯服务接口
 * <p>
 * 提供投影灯控制和脚踩检测功能，用于通过脚踩投影区域控制舱门开关。
 * </p>
 */
public interface IProjectionLightService {

    /**
     * 投影灯操作回调
     */
    interface OnDoorOperationListener {
        /**
         * 舱门操作开始
         * 
         * @param isOpening true=正在开门, false=正在关门
         */
        void onDoorOperationStart(boolean isOpening);

        /**
         * 舱门操作完成
         * 
         * @param success 是否成功
         */
        void onDoorOperationComplete(boolean success);
    }

    /**
     * 控制投影灯开关
     *
     * @param isOpen true 开启，false 关闭
     */
    void controlProjectionLight(boolean isOpen);

    /**
     * 开始脚踩检测循环
     * <p>
     * 启动投影区域检测，当检测到用户脚踩时自动切换舱门状态
     * </p>
     */
    void startDoorControlDetection();

    /**
     * 停止脚踩检测
     */
    void stopDoorControlDetection();

    /**
     * 设置功能是否启用
     *
     * @param enabled true=启用, false=禁用
     */
    void setEnabled(boolean enabled);

    /**
     * 查询功能是否启用
     *
     * @return true=已启用
     */
    boolean isEnabled();

    /**
     * 设置舱门操作监听器
     *
     * @param listener 监听器
     */
    void setOnDoorOperationListener(OnDoorOperationListener listener);

    /**
     * 释放资源
     */
    void release();
}
