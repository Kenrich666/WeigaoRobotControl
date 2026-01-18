package com.weigao.robot.control.service;

import com.weigao.robot.control.callback.IResultCallback;

/**
 * 远程呼叫服务接口
 * <p>
 * 满足需求书第8章"远程控制"要求：
 * <ul>
 * <li>远程呼叫：空闲状态下的机器人可被远程呼叫到指定点位</li>
 * <li>呼叫循环：呼叫机器到目标点进行循环，倒计时结束后前往返回点</li>
 * <li>呼叫到达：仅呼叫机器到目标点，倒计时结束后前往返回点</li>
 * <li>到达后停留时长设置</li>
 * </ul>
 * </p>
 */
public interface IRemoteCallService {

    // ==================== 呼叫类型常量 ====================

    /** 呼叫类型：呼叫循环（到达后进行循环配送） */
    int CALL_TYPE_LOOP = 1;

    /** 呼叫类型：呼叫到达（仅呼叫到目标点） */
    int CALL_TYPE_ARRIVE = 2;

    /** 呼叫类型：呼叫回收（呼叫机器人到呼叫点进行物品回收） */
    int CALL_TYPE_RECOVERY = 3;

    // ==================== 远程呼叫启用控制 ====================

    /**
     * 启用或禁用远程呼叫功能
     * <p>
     * 开启后，空闲状态下的机器人可以被以下方式远程呼叫：
     * <ul>
     * <li>擎朗机器人APP</li>
     * <li>擎朗呼叫器</li>
     * <li>API接口</li>
     * </ul>
     * </p>
     *
     * @param enabled  true=启用，false=禁用
     * @param callback 结果回调
     */
    void setRemoteCallEnabled(boolean enabled, IResultCallback<Void> callback);

    /**
     * 查询远程呼叫功能是否启用
     *
     * @param callback 结果回调，返回true表示已启用
     */
    void isRemoteCallEnabled(IResultCallback<Boolean> callback);

    // ==================== 远程呼叫操作 ====================

    /**
     * 处理远程呼叫请求
     * <p>
     * 当机器人收到远程呼叫指令时调用此方法。
     * </p>
     *
     * @param targetPointId 目标点位ID
     * @param callType      呼叫类型，参见 {@link #CALL_TYPE_LOOP} 等常量
     * @param callback      结果回调
     */
    void handleRemoteCall(int targetPointId, int callType, IResultCallback<Void> callback);

    /**
     * 取消当前远程呼叫任务
     *
     * @param callback 结果回调
     */
    void cancelRemoteCall(IResultCallback<Void> callback);

    /**
     * 查询是否正在执行远程呼叫任务
     *
     * @param callback 结果回调
     */
    void isRemoteCallActive(IResultCallback<Boolean> callback);

    // ==================== 到达后停留设置 ====================

    /**
     * 设置到达后停留时长是否启用
     * <p>
     * 开启时可设置具体时长，关闭后则一直停留在目标点。
     * </p>
     *
     * @param enabled  true=启用停留时长限制，false=一直停留
     * @param callback 结果回调
     */
    void setArrivalStayDurationEnabled(boolean enabled, IResultCallback<Void> callback);

    /**
     * 设置到达后停留时长
     * <p>
     * 停留时间结束后，机器人将自动前往返回点。
     * 仅在 {@link #setArrivalStayDurationEnabled(boolean, IResultCallback)}
     * 设置为true时有效。
     * </p>
     *
     * @param durationSeconds 停留时长，单位：秒
     * @param callback        结果回调
     */
    void setArrivalStayDuration(int durationSeconds, IResultCallback<Void> callback);

    /**
     * 获取到达后停留时长
     *
     * @param callback 结果回调，返回停留时长（秒）
     */
    void getArrivalStayDuration(IResultCallback<Integer> callback);

    /**
     * 查询到达后停留时长是否启用
     *
     * @param callback 结果回调
     */
    void isArrivalStayDurationEnabled(IResultCallback<Boolean> callback);

    // ==================== 呼叫来源监听 ====================

    /**
     * 注册远程呼叫回调
     * <p>
     * 用于接收远程呼叫请求的通知。
     * </p>
     *
     * @param callback 远程呼叫回调
     */
    void registerCallback(IRemoteCallCallback callback);

    /**
     * 注销远程呼叫回调
     *
     * @param callback 远程呼叫回调
     */
    void unregisterCallback(IRemoteCallCallback callback);

    /**
     * 远程呼叫回调接口
     */
    interface IRemoteCallCallback {

        /**
         * 收到远程呼叫请求
         *
         * @param sourceType    呼叫来源类型（如：APP、呼叫器、API）
         * @param targetPointId 目标点位ID
         * @param callType      呼叫类型
         */
        void onRemoteCallReceived(String sourceType, int targetPointId, int callType);

        /**
         * 远程呼叫任务执行结果
         *
         * @param success true=执行成功
         * @param message 结果消息
         */
        void onRemoteCallResult(boolean success, String message);

        /**
         * 远程呼叫任务被取消
         */
        void onRemoteCallCancelled();
    }
}
