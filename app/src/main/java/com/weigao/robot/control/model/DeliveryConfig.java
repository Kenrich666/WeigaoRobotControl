package com.weigao.robot.control.model;

import java.io.Serializable;

/**
 * 配送配置模型
 * <p>
 * 满足需求书第5章"配送模式"和第7章"循环配送设置"要求。
 * </p>
 */
public class DeliveryConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否显示最近使用点位 */
    private boolean showRecentPoints;

    /** 配送模式 */
    private DeliveryMode deliveryMode;

    /** 目标点停留时长是否启用（false=一直停留在目标点） */
    private boolean stayDurationEnabled;

    /** 目标点停留时长（秒） */
    private int stayDuration;

    /** 行走暂停时长（秒） */
    private int pauseDuration;

    /** 配送行走速度（cm/s） */
    private int deliverySpeed;

    /** 返航行走速度（cm/s） */
    private int returnSpeed;

    /** 慢速出发 */
    private boolean slowStart;

    /** 循环次数 */
    private int loopCount;

    /** 循环运行时长（秒） */
    private int loopDuration;

    /**
     * 配送模式枚举
     * <p>
     * 对应需求书第5章"配送模式"中的"选择配送方式"设置：
     * <ul>
     * <li>{@link #SINGLE_FLOOR_SINGLE_POINT} - 单层单点（默认模式）</li>
     * <li>{@link #SINGLE_FLOOR_MULTI_POINT} - 一层多点：每个楼层支持选择多个目标点</li>
     * <li>{@link #MULTI_FLOOR} - 多层配送：支持跨楼层配送</li>
     * <li>{@link #MULTI_POINT_NO_FLOOR} - 多点配送：支持无楼层配送</li>
     * </ul>
     * </p>
     */
    public enum DeliveryMode {
        /** 单层单点（默认模式） */
        SINGLE_FLOOR_SINGLE_POINT,

        /** 一层多点：每个楼层支持选择多个目标点（对应需求书"一层多点"） */
        SINGLE_FLOOR_MULTI_POINT,

        /** 多层配送：支持跨楼层配送 */
        MULTI_FLOOR,

        /** 多点配送：支持无楼层配送（对应需求书"多点配送"） */
        MULTI_POINT_NO_FLOOR
    }

    public boolean isShowRecentPoints() {
        return showRecentPoints;
    }

    public void setShowRecentPoints(boolean showRecentPoints) {
        this.showRecentPoints = showRecentPoints;
    }

    public DeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(DeliveryMode deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public boolean isStayDurationEnabled() {
        return stayDurationEnabled;
    }

    public void setStayDurationEnabled(boolean stayDurationEnabled) {
        this.stayDurationEnabled = stayDurationEnabled;
    }

    public int getStayDuration() {
        return stayDuration;
    }

    public void setStayDuration(int stayDuration) {
        this.stayDuration = stayDuration;
    }

    public int getPauseDuration() {
        return pauseDuration;
    }

    public void setPauseDuration(int pauseDuration) {
        this.pauseDuration = pauseDuration;
    }

    public int getDeliverySpeed() {
        return deliverySpeed;
    }

    public void setDeliverySpeed(int deliverySpeed) {
        this.deliverySpeed = deliverySpeed;
    }

    public int getReturnSpeed() {
        return returnSpeed;
    }

    public void setReturnSpeed(int returnSpeed) {
        this.returnSpeed = returnSpeed;
    }

    public boolean isSlowStart() {
        return slowStart;
    }

    public void setSlowStart(boolean slowStart) {
        this.slowStart = slowStart;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
    }

    public int getLoopDuration() {
        return loopDuration;
    }

    public void setLoopDuration(int loopDuration) {
        this.loopDuration = loopDuration;
    }
}
