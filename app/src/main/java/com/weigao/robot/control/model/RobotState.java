package com.weigao.robot.control.model;

import java.io.Serializable;

/**
 * Unified robot runtime state.
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
    private int workMode;
    private int motorStatus;
    private boolean scramButtonPressed;
    private LocationInfo location;
    private ChargingState chargingState;

    public static class LocationInfo implements Serializable {

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

    public int getWorkMode() {
        return workMode;
    }

    public void setWorkMode(int workMode) {
        this.workMode = workMode;
    }

    public int getMotorStatus() {
        return motorStatus;
    }

    public void setMotorStatus(int motorStatus) {
        this.motorStatus = motorStatus;
    }

    public boolean isScramButtonPressed() {
        return scramButtonPressed;
    }

    public void setScramButtonPressed(boolean pressed) {
        this.scramButtonPressed = pressed;
    }

    public LocationInfo getLocation() {
        return location;
    }

    public void setLocation(LocationInfo location) {
        this.location = location;
    }

    public ChargingState getChargingState() {
        return chargingState;
    }

    public void setChargingState(ChargingState chargingState) {
        this.chargingState = chargingState;
    }
}
