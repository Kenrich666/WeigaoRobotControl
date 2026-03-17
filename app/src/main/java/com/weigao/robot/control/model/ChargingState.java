package com.weigao.robot.control.model;

import java.io.Serializable;

/**
 * Unified charging state exposed by {@code IRobotStateService}.
 */
public class ChargingState implements Serializable {

    private static final long serialVersionUID = 1L;

    private int status;
    private int event;
    private int pileId;
    private boolean isCharging;

    public ChargingState() {
    }

    public ChargingState(ChargingState other) {
        if (other != null) {
            this.status = other.status;
            this.event = other.event;
            this.pileId = other.pileId;
            this.isCharging = other.isCharging;
        }
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getEvent() {
        return event;
    }

    public void setEvent(int event) {
        this.event = event;
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
        return "ChargingState{"
                + "status=" + status
                + ", event=" + event
                + ", pileId=" + pileId
                + ", isCharging=" + isCharging
                + '}';
    }
}
