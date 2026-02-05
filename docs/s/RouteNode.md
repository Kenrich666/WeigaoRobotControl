```java
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.component.navigation.route;

import java.io.Serializable;

public class RouteNode implements Serializable {
    private int index;
    private String name;
    private Integer id;
    private String queueName;
    private int queue;
    private boolean isRouteLastNode;
    private boolean isRouteFirstNode;
    private boolean isLastRouteMember;
    private Integer type;
    private Location location = new Location();
    private NavigationInfo navigationInfo = new NavigationInfo();
    private int duration;
    private int queueId;
    private int queueLoop;

    public boolean isRouteFirstNode() {
        return this.isRouteFirstNode;
    }

    public void setRouteFirstNode(boolean routeFirstNode) {
        this.isRouteFirstNode = routeFirstNode;
    }

    public boolean isLastRouteMember() {
        return this.isLastRouteMember;
    }

    public void setLastRouteMember(boolean lastRouteMember) {
        this.isLastRouteMember = lastRouteMember;
    }

    public boolean isRouteLastNode() {
        return this.isRouteLastNode;
    }

    public void setRouteLastNode(boolean routeLastNode) {
        this.isRouteLastNode = routeLastNode;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getQueue() {
        return this.queue;
    }

    public void setQueue(int queue) {
        this.queue = queue;
    }

    public String getQueueName() {
        return this.queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getType() {
        return this.type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public NavigationInfo getNavigationInfo() {
        return this.navigationInfo;
    }

    public void setNavigationInfo(NavigationInfo navigationInfo) {
        this.navigationInfo = navigationInfo;
    }

    public int getQueueLoop() {
        return this.queueLoop;
    }

    public void setQueueLoop(int queueLoop) {
        this.queueLoop = queueLoop;
    }

    public int getQueueId() {
        return this.queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public String toString() {
        return "RouteNode{name='" + this.name + '\'' + ", id=" + this.id + ", routeLastNode=" + this.isRouteLastNode + ", routeFirstNode=" + this.isRouteFirstNode + ", isLastRouteMember=" + this.isLastRouteMember + '}';
    }

    private class Location implements Serializable {
        private float x;
        private float y;
        private float phi;

        private Location() {
        }

        public float getX() {
            return this.x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return this.y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public float getPhi() {
            return this.phi;
        }

        public void setPhi(float phi) {
            this.phi = phi;
        }
    }

    public class NavigationInfo implements Serializable {
        private Integer status;
        private Integer schedule;
        private String destinationDesc;
        private Float totalDistance;
        private Float remainDistance;
        private Float totalTime;
        private Float remainTime;

        public Integer getStatus() {
            return this.status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public Integer getSchedule() {
            return this.schedule;
        }

        public void setSchedule(Integer schedule) {
            this.schedule = schedule;
        }

        public String getDestinationDesc() {
            return this.destinationDesc;
        }

        public void setDestinationDesc(String destinationDesc) {
            this.destinationDesc = destinationDesc;
        }

        public Float getTotalDistance() {
            return this.totalDistance;
        }

        public void setTotalDistance(Float totalDistance) {
            this.totalDistance = totalDistance;
        }

        public Float getRemainDistance() {
            return this.remainDistance;
        }

        public void setRemainDistance(Float remainDistance) {
            this.remainDistance = remainDistance;
        }

        public Float getTotalTime() {
            return this.totalTime;
        }

        public void setTotalTime(Float totalTime) {
            this.totalTime = totalTime;
        }

        public Float getRemainTime() {
            return this.remainTime;
        }

        public void setRemainTime(Float remainTime) {
            this.remainTime = remainTime;
        }

        public String toString() {
            return "NavigationInfo{status=" + this.status + ", schedule=" + this.schedule + ", destinationDesc='" + this.destinationDesc + '\'' + ", totalDistance=" + this.totalDistance + ", remainDistance=" + this.remainDistance + ", totalTime=" + this.totalTime + ", remainTime=" + this.remainTime + '}';
        }
    }
}

```