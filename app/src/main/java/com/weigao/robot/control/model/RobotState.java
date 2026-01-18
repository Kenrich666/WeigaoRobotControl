package com.weigao.robot.control.model;

import java.io.Serializable;

/**
 * 机器人状态模型
 * <p>
 * 表示机器人的运行时状态信息。
 * </p>
 */
public class RobotState implements Serializable {

    private static final long serialVersionUID = 1L;
    private int status;
    private String message;
    private LocationInfo local;
    private LocationInfo other;
    private boolean isMoving;
    private int batteryLevel;
    private boolean isScramPressed;

    /**
     * 位置信息内部类
     */
    public static class LocationInfo implements java.io.Serializable {

        private static final long serialVersionUID = 1L;

        private int id;
        private double x;
        private double y;
        private int status;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocationInfo getLocal() {
        return local;
    }

    public void setLocal(LocationInfo local) {
        this.local = local;
    }

    public LocationInfo getOther() {
        return other;
    }

    public void setOther(LocationInfo other) {
        this.other = other;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public void setMoving(boolean moving) {
        isMoving = moving;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public boolean isScramPressed() {
        return isScramPressed;
    }

    public void setScramPressed(boolean scramPressed) {
        isScramPressed = scramPressed;
    }
}
