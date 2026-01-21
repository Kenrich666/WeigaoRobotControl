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

    /** 工作模式 */
    private int workMode;
    /** 电机状态 */
    private int motorStatus;
    /** 急停按钮是否按下 */
    private boolean scramButtonPressed;
    /** 当前位置 */
    private LocationInfo location;

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

    // ==================== 新增的 getter/setter 方法 ====================

    /**
     * 获取工作模式
     * 
     * @return 工作模式码
     */
    public int getWorkMode() {
        return workMode;
    }

    /**
     * 设置工作模式
     * 
     * @param workMode 工作模式码
     */
    public void setWorkMode(int workMode) {
        this.workMode = workMode;
    }

    /**
     * 获取电机状态
     * 
     * @return 电机状态码
     */
    public int getMotorStatus() {
        return motorStatus;
    }

    /**
     * 设置电机状态
     * 
     * @param motorStatus 电机状态码
     */
    public void setMotorStatus(int motorStatus) {
        this.motorStatus = motorStatus;
    }

    /**
     * 获取急停按钮是否按下
     * 
     * @return 是否按下
     */
    public boolean isScramButtonPressed() {
        return scramButtonPressed;
    }

    /**
     * 设置急停按钮状态
     * 
     * @param pressed 是否按下
     */
    public void setScramButtonPressed(boolean pressed) {
        this.scramButtonPressed = pressed;
    }

    /**
     * 获取当前位置信息
     * 
     * @return 位置信息
     */
    public LocationInfo getLocation() {
        return location;
    }

    /**
     * 设置当前位置信息
     * 
     * @param location 位置信息
     */
    public void setLocation(LocationInfo location) {
        this.location = location;
    }
}
