```
package com.keenon.peanut.sample.bean;

import com.keenon.sdk.component.navigation.route.RouteNode;

import java.io.Serializable;

public class MyPoint implements Serializable {
  private static final long serialVersionUID = 8774101538505774364L;
  private long selectTime;
  private int duration;
  private RouteNode routeNode;
  private boolean manualControl;

  public MyPoint() {
  }

  public boolean isManualControl() {
    return manualControl;
  }

  public void setManualControl(boolean manualControl) {
    this.manualControl = manualControl;
  }

  public RouteNode getRouteNode() {
    return routeNode;
  }

  public void setRouteNode(RouteNode routeNode) {
    this.routeNode = routeNode;
  }

  public long getSelectTime() {
    return selectTime;
  }

  public void setSelectTime(long selectTime) {
    this.selectTime = selectTime;
  }

  public int getDuration() {
    return duration;
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }


  @Override
  public int hashCode() {
    int result = routeNode.getId();
    result = 31 * result + (routeNode.getName() != null ? routeNode.getName().hashCode() : 0);
    result = 31 * result + routeNode.getId();
    result = 31 * result + (int) (selectTime ^ (selectTime >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "Mypoint{" +
        ", name=" + routeNode.getName() +
        ", id=" + routeNode.getId() +
        ", selectTime=" + selectTime +
        ", node=" + routeNode +
        '}';
  }
}
```