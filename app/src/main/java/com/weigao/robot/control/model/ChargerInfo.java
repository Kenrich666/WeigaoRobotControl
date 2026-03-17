package com.weigao.robot.control.model;

import java.io.Serializable;

/**
 * Charge-domain state returned by {@code IChargerService}.
 */
public class ChargerInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private int event;
    private int status;
    private int pileId;
    private boolean isCharging;

    public ChargerInfo() {
    }

    public ChargerInfo(int event) {
        this.event = event;
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
        return "ChargerInfo{"
                + "event=" + event
                + ", status=" + status
                + ", pileId=" + pileId
                + ", isCharging=" + isCharging
                + '}';
    }
}
