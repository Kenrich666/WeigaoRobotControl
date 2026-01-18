package com.weigao.robot.control.callback;

/**
 * 进度回调接口
 * <p>
 * 对齐SDK {@code IProgressCallback}，用于处理带进度反馈的异步操作。
 * </p>
 *
 * @param <T> 结果数据类型
 */
public interface IProgressCallback<T> extends IResultCallback<T> {

    /**
     * 进度更新
     *
     * @param percent     进度百分比（0-100）
     * @param description 进度描述信息（可选）
     */
    void onProgress(int percent, String description);

    /**
     * 准备就绪，即将开始发送
     *
     * @param request 请求对象（可用于取消操作等）
     */
    void onReadyToSend(Object request);

    /**
     * 进度更新（简化版本）
     *
     * @param percent 进度百分比（0-100）
     */
    default void onProgress(int percent) {
        onProgress(percent, null);
    }

    /**
     * 准备就绪（简化版本，无请求对象）
     */
    default void onReadyToSend() {
        onReadyToSend(null);
    }
}
