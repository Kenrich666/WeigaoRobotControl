```
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.component.runtime;

public class RuntimeInfo {
    private int workMode;
    private int syncStatus;
    private int power;
    private Double totalOdo;
    private boolean emergencyEnable;
    private boolean emergencyOpen;
    private int motorStatus;
    private String robotArmInfo;
    private String robotStm32Info;
    private String robotIp;
    private String robotProperties;
    private String destList;

    public int getWorkMode() {
        return this.workMode;
    }

    public void setWorkMode(int workMode) {
        this.workMode = workMode;
    }

    public int getSyncStatus() {
        return this.syncStatus;
    }

    public void setSyncStatus(int syncStatus) {
        this.syncStatus = syncStatus;
    }

    public int getPower() {
        return this.power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public Double getTotalOdo() {
        return null == this.totalOdo ? new Double((double)0.0F) : this.totalOdo;
    }

    public void setTotalOdo(Double totalOdo) {
        this.totalOdo = totalOdo;
    }

    public boolean isEmergencyEnable() {
        return this.emergencyEnable;
    }

    public void setEmergencyEnable(boolean emergencyEnable) {
        this.emergencyEnable = emergencyEnable;
    }

    public boolean isEmergencyOpen() {
        return this.emergencyOpen;
    }

    public void setEmergencyOpen(boolean emergencyOpen) {
        this.emergencyOpen = emergencyOpen;
    }

    public int getMotorStatus() {
        return this.motorStatus;
    }

    public void setMotorStatus(int motorStatus) {
        this.motorStatus = motorStatus;
    }

    public String getRobotArmInfo() {
        return this.robotArmInfo;
    }

    public void setRobotArmInfo(String robotArmInfo) {
        this.robotArmInfo = robotArmInfo;
    }

    public String getRobotStm32Info() {
        return this.robotStm32Info;
    }

    public void setRobotStm32Info(String robotStm32Info) {
        this.robotStm32Info = robotStm32Info;
    }

    public String getRobotIp() {
        return this.robotIp;
    }

    public void setRobotIp(String robotIp) {
        this.robotIp = robotIp;
    }

    public String getRobotProperties() {
        return this.robotProperties;
    }

    public void setRobotProperties(String robotProperties) {
        this.robotProperties = robotProperties;
    }

    public String getDestList() {
        return this.destList;
    }

    public void setDestList(String destList) {
        this.destList = destList;
    }
}
```