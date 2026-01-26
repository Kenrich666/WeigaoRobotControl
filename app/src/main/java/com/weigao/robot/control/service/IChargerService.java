package com.weigao.robot.control.service;

import com.keenon.sdk.component.charger.PeanutCharger;
import com.weigao.robot.control.callback.IChargerCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.ChargerInfo;

/**
 * 充电服务接口
 * <p>
 * 对齐SDK {@code PeanutCharger}，提供机器人充电控制功能。
 * </p>
 */
public interface IChargerService {

    // ==================== 充电动作常量 ====================
    // 注意：以下常量值需与SDK PeanutCharger.CHARGE_ACTION_* 保持一致
    // SDK参考：PeanutCharger.performAction(int action)

    /** 充电动作：自动充电（机器人自动导航到充电桩） */
    int CHARGE_ACTION_AUTO = PeanutCharger.CHARGE_ACTION_AUTO;

    /** 充电动作：手动充电（机器人已在充电桩附近时使用） */
    int CHARGE_ACTION_MANUAL = PeanutCharger.CHARGE_ACTION_MANUAL;

    /** 充电动作：适配器充电（使用外接适配器） */
    int CHARGE_ACTION_ADAPTER = PeanutCharger.CHARGE_ACTION_ADAPTER;

    /** 充电动作：停止充电 */
    int CHARGE_ACTION_STOP = PeanutCharger.CHARGE_ACTION_STOP;

    /**
     * 开始自动充电
     * <p>
     * 机器人会自动导航到充电桩并开始充电。
     * </p>
     *
     * @param callback 结果回调
     */
    void startAutoCharge(IResultCallback<Void> callback);

    /**
     * 开始手动充电
     * <p>
     * 机器人已在充电桩附近时，手动开始充电匹配。
     * </p>
     *
     * @param callback 结果回调
     */
    void startManualCharge(IResultCallback<Void> callback);

    /**
     * 开始适配器充电
     * <p>
     * 使用外接适配器充电。
     * </p>
     *
     * @param callback 结果回调
     */
    void startAdapterCharge(IResultCallback<Void> callback);

    /**
     * 停止充电
     *
     * @param callback 结果回调
     */
    void stopCharge(IResultCallback<Void> callback);

    /**
     * 执行充电动作
     *
     * @param action   充电动作，参见 {@link #CHARGE_ACTION_AUTO} 等常量
     * @param callback 结果回调
     */
    void performAction(int action, IResultCallback<Void> callback);

    /**
     * 设置充电桩ID
     *
     * @param pileId   充电桩ID
     * @param callback 结果回调
     */
    void setChargePile(int pileId, IResultCallback<Void> callback);

    /**
     * 获取当前充电桩ID
     *
     * @param callback 结果回调
     */
    void getChargePile(IResultCallback<Integer> callback);

    /**
     * 获取当前充电信息
     *
     * @param callback 结果回调
     */
    void getChargerInfo(IResultCallback<ChargerInfo> callback);

    /**
     * 获取当前电量
     *
     * @param callback 结果回调，返回电量百分比（0-100）
     */
    void getBatteryLevel(IResultCallback<Integer> callback);

    /**
     * 查询是否正在充电
     *
     * @param callback 结果回调
     */
    void isCharging(IResultCallback<Boolean> callback);

    /**
     * 注册充电状态回调
     *
     * @param callback 充电回调
     */
    void registerCallback(IChargerCallback callback);

    /**
     * 注销充电状态回调
     *
     * @param callback 充电回调
     */
    void unregisterCallback(IChargerCallback callback);

    /**
     * 释放资源
     * <p>
     * 对齐SDK {@code PeanutCharger.release()}。
     * 注意：释放后将无法接收电量更新通知，内置的电量订阅也会被取消。
     * 正常情况下不需要调用，仅在确实需要释放资源时使用。
     * </p>
     */
    void release();
}
