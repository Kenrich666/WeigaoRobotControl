package com.weigao.robot.control.model;

import java.io.Serializable;
import java.util.UUID;

public class HospitalDeliveryTask implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int UNASSIGNED_LAYER = -1;

    private String taskId;
    private String itemName;
    private NavigationNode roomNode;
    private int assignedLayer = UNASSIGNED_LAYER;

    public HospitalDeliveryTask() {
        this.taskId = UUID.randomUUID().toString();
    }

    public HospitalDeliveryTask(String itemName, NavigationNode roomNode) {
        this();
        this.itemName = itemName;
        this.roomNode = roomNode;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public NavigationNode getRoomNode() {
        return roomNode;
    }

    public void setRoomNode(NavigationNode roomNode) {
        this.roomNode = roomNode;
    }

    public int getAssignedLayer() {
        return assignedLayer;
    }

    public void setAssignedLayer(int assignedLayer) {
        this.assignedLayer = assignedLayer;
    }

    public boolean hasAssignedLayer() {
        return assignedLayer > 0;
    }

    public String getAssignedLayerLabel() {
        return hasAssignedLayer() ? "L" + assignedLayer : "\u672a\u5206\u914d";
    }
}
