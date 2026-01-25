package com.weigao.robot.control.model;

import com.keenon.sdk.component.navigation.route.RouteNode;
import java.io.Serializable;


/**
 * 导航节点数据模型
 * <p>
 * 对应SDK {@code RouteNode}，表示导航路线中的一个目标点。
 * </p>
 */
public class NavigationNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标点ID */
    private int id;

    /** 目标点名称 */
    private String name;

    /** X坐标，单位：米 */
    private double x;

    /** Y坐标，单位：米 */
    private double y;

    /** 航向角，单位：弧度 */
    private double phi;

    /** 楼层 */
    private int floor;

    /** SDK 原始路线节点 */
    private RouteNode routeNode;

    /** 是否为中间点 */
    private boolean isWaypoint;


    public NavigationNode() {
    }

    public NavigationNode(int id, String name, double x, double y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.routeNode = new RouteNode();
        this.routeNode.setId(id);
        this.routeNode.setName(name);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public double getPhi() {
        return phi;
    }

    public void setPhi(double phi) {
        this.phi = phi;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public boolean isWaypoint() {
        return isWaypoint;
    }

    public void setWaypoint(boolean waypoint) {
        isWaypoint = waypoint;
    }

    public RouteNode getRouteNode() {
        return routeNode;
    }

    public void setRouteNode(RouteNode routeNode) {
        this.routeNode = routeNode;
    }


    @Override
    public String toString() {
        return "NavigationNode{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", floor=" + floor +
                '}';
    }
}
