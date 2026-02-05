```
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.sensor.light;

import com.keenon.common.constant.PeanutConstants.SerialProtocolVer;
import java.util.List;

public class LightConfig {
    private int devId;
    private String desc;
    private int type;
    private String ver;
    private Blink blink;
    private Bead beads;
    private Color color;
    private boolean isUSBDirect;
    private boolean isSerialDirect;
    private String serialProtocolVersion;
    private boolean isTrans;
    private List<Effect> supportedEffect;
    private Effect effect;
    private List<Color> colors;

    public LightConfig(int devId) {
        this(devId, "", 1, 0);
    }

    public LightConfig(int devId, String desc, int type, int beadsNum) {
        this(devId, desc, type, beadsNum, (List)null);
    }

    public LightConfig(int devId, String desc, int type, int beadsNum, List<Effect> supportedEffect) {
        this.devId = devId;
        this.desc = desc;
        this.type = type;
        this.ver = "v2.0";
        this.beads = new Bead(beadsNum);
        this.supportedEffect = supportedEffect;
        this.color = new Color();
        this.effect = new Effect();
        this.blink = new Blink();
        this.serialProtocolVersion = "v2.0";
    }

    public int getDevId() {
        return this.devId;
    }

    public void setDevId(int devId) {
        this.devId = devId;
    }

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getVer() {
        return this.ver;
    }

    public void setVer(String ver) {
        this.ver = ver;
    }

    public Bead getBeads() {
        return this.beads;
    }

    public void setBeads(Bead beads) {
        this.beads = beads;
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public List<Effect> getSupportedEffect() {
        return this.supportedEffect;
    }

    public void setSupportedEffect(List<Effect> supportedEffect) {
        this.supportedEffect = supportedEffect;
    }

    public Effect getEffect() {
        return this.effect;
    }

    public void setEffect(Effect effect) {
        this.effect = effect;
    }

    public Blink getBlink() {
        return this.blink;
    }

    public void setBlink(Blink blink) {
        this.blink = blink;
    }

    public List<Color> getColors() {
        return this.colors;
    }

    public void setColors(List<Color> colors) {
        this.colors = colors;
    }

    public boolean isUSBDirect() {
        return this.isUSBDirect;
    }

    public void setUSBDirect(boolean USBDirect) {
        this.isUSBDirect = USBDirect;
    }

    public boolean isSerialDirect() {
        return this.isSerialDirect;
    }

    public boolean isTrans() {
        return this.isTrans;
    }

    public void setTrans(boolean trans) {
        this.isTrans = trans;
    }

    @SerialProtocolVer
    public String getSerialProtocolVersion() {
        return this.serialProtocolVersion;
    }

    public void setSerialDirect(boolean serialDirect) {
        this.setSerialDirect(serialDirect, "v2.0");
    }

    public void setSerialDirect(boolean isDirect, @SerialProtocolVer String protocolVersion) {
        this.isSerialDirect = isDirect;
        this.serialProtocolVersion = protocolVersion;
    }

    public String toString() {
        return "LightConfig{devId=" + this.devId + ", desc='" + this.desc + '\'' + ", type=" + this.type + ", ver='" + this.ver + '\'' + ", blink=" + this.blink + ", beads=" + this.beads + ", color=" + this.color + ", supportedEffect=" + this.supportedEffect + ", effect=" + this.effect + '}';
    }

    public class Bead {
        private int num;
        private int head;
        private byte[] bytes;

        public Bead() {
            this.bytes = new byte[0];
        }

        public Bead(int num) {
            this(num, 0);
        }

        public Bead(int num, int head) {
            this.bytes = new byte[0];
            this.num = num;
            this.head = head;
        }

        public byte[] getBytes() {
            return this.bytes;
        }

        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        public int getNum() {
            return this.num;
        }

        public void setNum(int num) {
            this.num = num;
        }

        public int getHead() {
            return this.head;
        }

        public void setHead(int head) {
            this.head = head;
        }

        public String toString() {
            return "Bead{num=" + this.num + ", head=" + this.head + '}';
        }
    }

    public static class Effect {
        private int id;
        private int time;
        private String desc;

        public Effect() {
        }

        public Effect(int id, int time, String desc) {
            this.id = id;
            this.time = time;
            this.desc = desc;
        }

        public int getId() {
            return this.id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getTime() {
            return this.time;
        }

        public void setTime(int time) {
            this.time = time;
        }

        public String getDesc() {
            return this.desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public String toString() {
            return "Effect{id=" + this.id + ", time=" + this.time + ", desc='" + this.desc + '\'' + '}';
        }
    }

    public class Color {
        int mask;
        int r;
        int g;
        int b;

        public Color() {
        }

        public Color(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public Color(int mask, int r, int g, int b) {
            this.mask = mask;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public int getMask() {
            return this.mask;
        }

        public void setMask(int mask) {
            this.mask = mask;
        }

        public int getR() {
            return this.r;
        }

        public void setR(int r) {
            this.r = r;
        }

        public int getG() {
            return this.g;
        }

        public void setG(int g) {
            this.g = g;
        }

        public int getB() {
            return this.b;
        }

        public void setB(int b) {
            this.b = b;
        }

        public String toString() {
            return "Color{mask=" + this.mask + ", r=" + this.r + ", g=" + this.g + ", b=" + this.b + '}';
        }
    }

    public class Blink {
        int onTime;
        int offTime;
        int repeat;

        public Blink() {
        }

        public Blink(int onTime, int offTime, int repeat) {
            this.onTime = onTime;
            this.offTime = offTime;
            this.repeat = repeat;
        }

        public int getOnTime() {
            return this.onTime;
        }

        public void setOnTime(int onTime) {
            this.onTime = onTime;
        }

        public int getOffTime() {
            return this.offTime;
        }

        public void setOffTime(int offTime) {
            this.offTime = offTime;
        }

        public int getRepeat() {
            return this.repeat;
        }

        public void setRepeat(int repeat) {
            this.repeat = repeat;
        }

        public String toString() {
            return "Blink{onTime=" + this.onTime + ", offTime=" + this.offTime + ", repeat=" + this.repeat + '}';
        }
    }
}
```