package com.weigao.robot.control.callback;

import com.weigao.robot.control.model.DoorType;

/**
 * 舱门状态回调接口
 * <p>
 * 对齐SDK {@code DoorListener}，用于接收舱门相关的状态变化和事件通知。
 * </p>
 */
public interface IDoorCallback {

    /** 舱门状态：关闭 */
    int DOOR_STATE_CLOSED = 0;

    /** 舱门状态：打开 */
    int DOOR_STATE_OPEN = -1;

    /** 舱门状态：执行中（正在开启或关闭） */
    int DOOR_STATE_EXECUTING = 1;

    /** 舱门状态：未知 */
    int DOOR_STATE_UNKNOWN = -100;

    /**
     * 舱门状态变化
     *
     * @param doorId 舱门ID（从1开始）
     * @param state  舱门状态，参见 {@link #DOOR_STATE_CLOSED} 等常量
     */
    void onDoorStateChanged(int doorId, int state);

    /**
     * 舱门类型变化
     *
     * @param type 新的舱门类型
     */
    void onDoorTypeChanged(DoorType type);

    /**
     * 舱门类型设置结果
     *
     * @param success true=设置成功，false=设置失败
     */
    void onDoorTypeSettingResult(boolean success);

    /**
     * 舱门发生故障
     *
     * @param doorId    舱门ID，为0表示非特定舱门的错误
     * @param errorCode 错误码，参见SDK舱门错误码（200500-200503）
     */
    void onDoorError(int doorId, int errorCode);
}
