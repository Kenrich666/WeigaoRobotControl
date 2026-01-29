package com.weigao.robot.control.model;

import java.io.Serializable;

/**
 * 循环配送配置模型
 * <p>
 * 满足需求书第7章"循环配送设置"要求，专门用于循环配送场景的配置。
 * 将循环配送相关的配置项从 {@link DeliveryConfig} 中独立出来，便于管理和扩展。
 * </p>
 */
public class LoopDeliveryConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 循环次数（0表示无限循环） */
    private int loopCount;

    /** 循环运行时长是否启用 */
    private boolean loopDurationEnabled;

    /** 循环运行总时长（秒） */
    private int loopDuration;

    /** 行走暂停时长（秒） */
    private int pauseDuration;

    /** 目标点停留时长（秒） */
    private int targetStayDuration;

    /** 起点停留时长（秒） */
    private int startPointStayDuration;

    /** 循环行走速度（cm/s） */
    private int loopSpeed;

    /** 循环配送背景音乐路径 */
    private String loopMusicPath;



    // ==================== Getters and Setters ====================

    public int getLoopCount() {
        return loopCount;
    }

    public void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
    }

    public boolean isLoopDurationEnabled() {
        return loopDurationEnabled;
    }

    public void setLoopDurationEnabled(boolean loopDurationEnabled) {
        this.loopDurationEnabled = loopDurationEnabled;
    }

    public int getLoopDuration() {
        return loopDuration;
    }

    public void setLoopDuration(int loopDuration) {
        this.loopDuration = loopDuration;
    }

    public int getPauseDuration() {
        return pauseDuration;
    }

    public void setPauseDuration(int pauseDuration) {
        this.pauseDuration = pauseDuration;
    }

    public int getTargetStayDuration() {
        return targetStayDuration;
    }

    public void setTargetStayDuration(int targetStayDuration) {
        this.targetStayDuration = targetStayDuration;
    }

    public int getStartPointStayDuration() {
        return startPointStayDuration;
    }

    public void setStartPointStayDuration(int startPointStayDuration) {
        this.startPointStayDuration = startPointStayDuration;
    }

    public int getLoopSpeed() {
        return loopSpeed;
    }

    public void setLoopSpeed(int loopSpeed) {
        this.loopSpeed = loopSpeed;
    }

    public String getLoopMusicPath() {
        return loopMusicPath;
    }

    public void setLoopMusicPath(String loopMusicPath) {
        this.loopMusicPath = loopMusicPath;
    }



    @Override
    public String toString() {
        return "LoopDeliveryConfig{" +
                "loopCount=" + loopCount +
                ", loopDuration=" + loopDuration +
                ", loopSpeed=" + loopSpeed +
                '}';
    }
}
