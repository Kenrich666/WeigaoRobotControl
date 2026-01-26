package com.weigao.robot.control.model;

import java.io.Serializable;
import java.util.List;

public class CircularRoute implements Serializable {
    private String name;
    private List<NavigationNode> nodes;
    private int loopCount;

    public CircularRoute() {
    }

    public CircularRoute(String name, List<NavigationNode> nodes, int loopCount) {
        this.name = name;
        this.nodes = nodes;
        this.loopCount = loopCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<NavigationNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<NavigationNode> nodes) {
        this.nodes = nodes;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
    }
}
