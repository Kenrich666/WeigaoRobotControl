package com.weigao.robot.control.model;

import java.io.Serializable;

/**
 * 点位信息模型
 * <p>
 * 表示地图上的一个目标点信息。
 * </p>
 */
public class PointInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private int floor;
    private double x;
    private double y;
    private PointType type;
    private long lastUsedTime;

    /**
     * 点位类型枚举
     */
    public enum PointType {
        /** 目的地 */
        DESTINATION,
        /** 循环点位 */
        LOOP,
        /** 返回点 */
        RETURN,
        /** 待机点 */
        STANDBY,
        /** 回收点 */
        RECOVERY
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
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

    public PointType getType() {
        return type;
    }

    public void setType(PointType type) {
        this.type = type;
    }

    public long getLastUsedTime() {
        return lastUsedTime;
    }

    public void setLastUsedTime(long lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
    }
}
