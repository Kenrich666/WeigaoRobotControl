package com.weigao.robot.control.service;

import com.weigao.robot.control.callback.IResultCallback;

/**
 * 安全锁定服务接口
 * <p>
 * 提供配送任务的安全锁定功能，满足需求书第1章"任务安全与权限控制功能"要求：
 * <ul>
 * <li>锁定触发：舱门关闭并启动配送任务后自动激活安全锁定状态</li>
 * <li>取物解锁：到达目的地后需输入密码才能开启舱门</li>
 * <li>中断保护：任务执行过程中强行中断需强制密码验证</li>
 * </ul>
 * </p>
 */
public interface ISecurityService {

    /**
     * 启用或禁用安全锁定功能
     *
     * @param enabled  true=启用，false=禁用
     * @param callback 结果回调
     */
    void setSecurityLockEnabled(boolean enabled, IResultCallback<Void> callback);

    /**
     * 查询安全锁定功能是否启用
     *
     * @param callback 结果回调，返回true表示已启用
     */
    void isSecurityLockEnabled(IResultCallback<Boolean> callback);

    /**
     * 查询当前是否处于锁定状态
     * <p>
     * 锁定状态：配送任务启动后自动进入锁定状态，需密码验证才能解锁。
     * </p>
     *
     * @param callback 结果回调，返回true表示当前已锁定
     */
    void isLocked(IResultCallback<Boolean> callback);

    /**
     * 验证密码
     *
     * @param password 待验证的密码
     * @param callback 结果回调，返回true表示密码正确
     */
    void verifyPassword(String password, IResultCallback<Boolean> callback);

    /**
     * 设置或修改密码
     *
     * @param oldPassword 旧密码（首次设置时传空字符串）
     * @param newPassword 新密码
     * @param callback    结果回调
     */
    void setPassword(String oldPassword, String newPassword, IResultCallback<Void> callback);

    /**
     * 使用密码解锁指定舱门
     * <p>
     * 此方法会先验证密码，验证通过后解锁并开启指定舱门。
     * </p>
     *
     * @param doorId   舱门ID
     * @param password 密码
     * @param callback 结果回调
     */
    void unlockDoor(int doorId, String password, IResultCallback<Void> callback);

    /**
     * 手动触发锁定
     * <p>
     * 通常用于配送任务开始时自动调用，也可手动触发。
     * </p>
     *
     * @param callback 结果回调
     */
    void lock(IResultCallback<Void> callback);

    /**
     * 解锁（需先验证密码）
     *
     * @param password 密码
     * @param callback 结果回调
     */
    void unlock(String password, IResultCallback<Void> callback);

    /**
     * 释放资源
     */
    void release();
}
