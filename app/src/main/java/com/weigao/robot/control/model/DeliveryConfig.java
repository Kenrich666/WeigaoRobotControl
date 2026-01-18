package com.weigao.robot.control.model;

public class DeliveryConfig {
    private boolean showRecentPoints;
    private DeliveryMode deliveryMode;
    private int stayDuration;
    private int pauseDuration;
    private int deliverySpeed;
    private int returnSpeed;
    private boolean slowStart;
    private int loopCount;
    private int loopDuration;

    public enum DeliveryMode {
        SINGLE_FLOOR_SINGLE_POINT,
        SINGLE_FLOOR_MULTI_POINT,
        MULTI_FLOOR,
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
