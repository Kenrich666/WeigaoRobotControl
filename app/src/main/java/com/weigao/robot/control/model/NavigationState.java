package com.weigao.robot.control.model;

/**
 * 导航状态常量定义
 * <p>
 * 对齐SDK {@code Navigation} 状态码定义。
 * </p>
 */
public final class NavigationState {

    private NavigationState() {
        // 禁止实例化
    }

    /** 空闲状态 */
    public static final int STATE_IDLE = 0;

    /** 路线已准备就绪 */
    public static final int STATE_PREPARED = 1;

    /** 正在导航中 */
    public static final int STATE_RUNNING = 2;

    /** 已到达目的地 */
    public static final int STATE_DESTINATION = 3;

    /** 导航已暂停 */
    public static final int STATE_PAUSED = 4;

    /** 发生碰撞 */
    public static final int STATE_COLLISION = 5;

    /** 被障碍物阻挡 */
    public static final int STATE_BLOCKED = 6;

    /** 导航已停止 */
    public static final int STATE_STOPPED = 7;

    /** 导航出错 */
    public static final int STATE_ERROR = 8;

    /** 正在处理阻挡 */
    public static final int STATE_BLOCKING = 9;

    /** 导航任务结束 */
    public static final int STATE_END = 10;

    /**
     * 获取状态描述文字
     *
     * @param state 状态码
     * @return 状态描述
     */
    public static String getStateDescription(int state) {
        switch (state) {
            case STATE_IDLE:
                return "空闲";
            case STATE_PREPARED:
                return "已准备";
            case STATE_RUNNING:
                return "导航中";
            case STATE_DESTINATION:
                return "已到达";
            case STATE_PAUSED:
                return "已暂停";
            case STATE_COLLISION:
                return "碰撞";
            case STATE_BLOCKED:
                return "阻挡";
            case STATE_STOPPED:
                return "已停止";
            case STATE_ERROR:
                return "错误";
            case STATE_BLOCKING:
                return "处理阻挡中";
            case STATE_END:
                return "已结束";
            default:
                return "未知状态";
        }
    }

    /**
     * 判断是否为终态（导航已结束，无需继续监听）
     * <p>
     * 终态包括：已到达、已停止、错误、已结束。
     * </p>
     *
     * @param state 状态码
     * @return true=终态
     */
    public static boolean isTerminalState(int state) {
        return state == STATE_DESTINATION ||
                state == STATE_STOPPED ||
                state == STATE_ERROR ||
                state == STATE_END;
    }

    /**
     * 判断是否为活动状态（导航正在进行中）
     * <p>
     * 活动状态包括：导航中、处理阻挡中。
     * </p>
     *
     * @param state 状态码
     * @return true=活动状态
     */
    public static boolean isActiveState(int state) {
        return state == STATE_RUNNING || state == STATE_BLOCKING;
    }

    /**
     * 判断是否为异常状态（需要关注或处理）
     * <p>
     * 异常状态包括：碰撞、阻挡、错误。
     * </p>
     *
     * @param state 状态码
     * @return true=异常状态
     */
    public static boolean isAbnormalState(int state) {
        return state == STATE_COLLISION ||
                state == STATE_BLOCKED ||
                state == STATE_ERROR;
    }
}
