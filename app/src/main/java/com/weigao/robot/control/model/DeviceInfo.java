package com.weigao.robot.control.model;

import java.io.Serializable;

/**
 * 设备信息模型
 * <p>
 * 包含机器人硬件设备信息，如版本、SN号等。
 * </p>
 */
public class DeviceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private int pid;
    private String device;
    private String sn;
    private VersionInfo version;
    private EnvInfo env;

    // SDK 相关字段
    private String robotIp;
    private String armInfo;
    private String stm32Info;
    private String properties;

    public static class VersionInfo {
        private SoftwareInfo sw;
        private HardwareInfo hw;

        public static class SoftwareInfo {
            private String number;
            private String base;
            private String md5;

            public String getNumber() {
                return number;
            }

            public void setNumber(String number) {
                this.number = number;
            }

            public String getBase() {
                return base;
            }

            public void setBase(String base) {
                this.base = base;
            }

            public String getMd5() {
                return md5;
            }

            public void setMd5(String md5) {
                this.md5 = md5;
            }
        }

        public static class HardwareInfo {
            private String number;

            public String getNumber() {
                return number;
            }

            public void setNumber(String number) {
                this.number = number;
            }
        }

        public SoftwareInfo getSw() {
            return sw;
        }

        public void setSw(SoftwareInfo sw) {
            this.sw = sw;
        }

        public HardwareInfo getHw() {
            return hw;
        }

        public void setHw(HardwareInfo hw) {
            this.hw = hw;
        }
    }

    public static class EnvInfo {
        private String number;
        private String base;
        private String md5;

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getBase() {
            return base;
        }

        public void setBase(String base) {
            this.base = base;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }
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

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public VersionInfo getVersion() {
        return version;
    }

    public void setVersion(VersionInfo version) {
        this.version = version;
    }

    public EnvInfo getEnv() {
        return env;
    }

    public void setEnv(EnvInfo env) {
        this.env = env;
    }

    // ==================== SDK 相关字段 ====================

    public String getRobotIp() {
        return robotIp;
    }

    public void setRobotIp(String robotIp) {
        this.robotIp = robotIp;
    }

    public String getArmInfo() {
        return armInfo;
    }

    public void setArmInfo(String armInfo) {
        this.armInfo = armInfo;
    }

    public String getStm32Info() {
        return stm32Info;
    }

    public void setStm32Info(String stm32Info) {
        this.stm32Info = stm32Info;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }
}
