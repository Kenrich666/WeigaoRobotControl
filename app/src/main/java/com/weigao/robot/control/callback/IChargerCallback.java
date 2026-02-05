package com.weigao.robot.control.callback;

import com.weigao.robot.control.model.ChargerInfo;

/**
 * 充电状态回调接口
 * <p>
 * 对齐SDK {@code Charger.Listener}，用于接收充电相关的状态变化和事件通知。
 * </p>
 */
public interface IChargerCallback {

    /**
     * 充电信息变化
     * <p>
     * 当电量变化或充电事件发生时回调。
     * </p>
     *
     * @param event       充电事件码，参见SDK充电事件定义（40001-40021）
     * @param chargerInfo 充电信息
     */
    void onChargerInfoChanged(int event, ChargerInfo chargerInfo);

    /**
     * 充电状态变化
     *
     * @param status 充电状态码
     */
    void onChargerStatusChanged(int status);

    /**
     * 充电过程中发生错误
     *
     * @param errorCode 错误码，参见SDK电源错误码（203300-203317）
     */
    void onChargerError(int errorCode);

    /**
     * 消毒倒计时更新
     * <p>
     * 每秒回调一次，通知剩余消毒时间。
     * </p>
     *
     * @param remainingMillis 剩余时间（毫秒）
     */
    default void onDisinfectionTimerTick(long remainingMillis) {
    }

    /**
     * 消毒完成
     * <p>
     * 当消毒倒计时结束或充电停止时回调。
     * </p>
     */
    default void onDisinfectionComplete() {
    }
}
