package com.weigao.robot.control.model;

import java.io.Serializable;

/**
 * 充电信息模型
 * <p>
 * 对齐SDK {@code ChargerInfo}。
 * </p>
 */
public class ChargerInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前电量百分比（0-100） */
    private int power;

    /** 充电事件码 */
    private int event;

    /** 充电状态码 */
    private int status;

    /** 充电桩ID */
    private int pileId;

    /** 是否正在充电 */
    private boolean isCharging;

    public ChargerInfo() {
    }

    public ChargerInfo(int power, int event) {
        this.power = power;
        this.event = event;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public int getEvent() {
        return event;
    }

    public void setEvent(int event) {
        this.event = event;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getPileId() {
        return pileId;
    }

    public void setPileId(int pileId) {
        this.pileId = pileId;
    }

    public boolean isCharging() {
        return isCharging;
    }

    public void setCharging(boolean charging) {
        isCharging = charging;
    }

    @Override
    public String toString() {
        return "ChargerInfo{" +
                "power=" + power +
                ", event=" + event +
                ", status=" + status +
                ", pileId=" + pileId +
                ", isCharging=" + isCharging +
                '}';
    }
}
