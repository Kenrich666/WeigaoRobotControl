package com.weigao.robot.control.callback;

/**
 * 结果回调接口
 * <p>
 * 通用异步操作结果回调，所有服务方法均使用此接口返回结果。
 * </p>
 *
 * @param <T> 结果数据类型
 */
public interface IResultCallback<T> {

    /**
     * 操作成功
     *
     * @param result 结果数据
     */
    void onSuccess(T result);

    /**
     * 操作失败
     *
     * @param error 错误信息
     */
    void onError(ApiError error);
}
