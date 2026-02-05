```
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.sensor.light;

import com.keenon.common.utils.ByteUtils;
import com.keenon.sdk.api.LightDisplayApi;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.scmIot.bean.request.LEDMultiColorConfig;
import com.keenon.sdk.scmIot.bean.request.LEDMultiColorConfigV2;
import com.keenon.sdk.scmIot.bean.request.LEDMultiColorConfigV3;
import com.keenon.sdk.scmIot.bean.request.LEDRGBConfig;
import com.keenon.sdk.scmIot.bean.request.LEDSingleColorConfig;
import com.keenon.sdk.sensor.common.Sensor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.elements.util.NamedThreadFactory;

public class SensorLight extends Sensor {
    private static volatile SensorLight sInstance;
    private ScheduledExecutorService scheduledThreadPool = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("V3HeartBeat#"));
    private volatile boolean isStartScheduled = false;
    private volatile boolean isHeartBeatStart = false;
    private int heartBeatDev = -1;

    public void setHeartBeatDev(int heartBeatDev) {
        this.heartBeatDev = heartBeatDev;
    }

    public static SensorLight getInstance() {
        if (sInstance == null) {
            synchronized(SensorLight.class) {
                if (sInstance == null) {
                    sInstance = new SensorLight();
                }
            }
        }

        return sInstance;
    }

    public String name() {
        return SensorLight.class.getSimpleName();
    }

    public void mount() {
        this.log("start");
    }

    public void unMount() {
    }

    public void release() {
    }

    public synchronized void onStartHeartBeatV3() {
        this.isHeartBeatStart = true;
        if (!this.isStartScheduled) {
            this.isStartScheduled = true;
            this.scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    if (SensorLight.this.isHeartBeatStart) {
                        SensorLight.this.playHeartBeatV3();
                    }

                }
            }, 0L, 2L, TimeUnit.SECONDS);
        }
    }

    public synchronized void onStopHeartBeatV3() {
        this.isHeartBeatStart = false;
    }

    public void displayLight(int lightId, Integer intensity, int mode) {
        (new LightDisplayApi()).send(new IDataCallback() {
            public void success(String result) {
                SensorLight.this.notifyEvent("_sensor.light.warning.ack", result);
            }

            public void error(ApiError error) {
                SensorLight.this.notifyEvent("_sensor.request.failed", error);
            }
        }, lightId, intensity, mode);
    }

    public void play(LightConfig lightConfig) {
        if (lightConfig != null) {
            if (lightConfig.getType() == 0) {
                this.playSingleColor(lightConfig);
            } else {
                this.playMultiColor(lightConfig);
            }

        }
    }

    private void playSingleColor(LightConfig lightConfig) {
        this.log("playSingleColor", lightConfig.toString());
        LEDSingleColorConfig inputBean = new LEDSingleColorConfig();
        inputBean.setOnTime((short)lightConfig.getBlink().getOnTime());
        inputBean.setOffTime((short)lightConfig.getBlink().getOffTime());
        inputBean.setRepeat((byte)lightConfig.getBlink().getRepeat());
        SCMRequest request = new SCMRequest();
        request.setDev(lightConfig.getDevId());
        request.setTopic(2);
        request.setType(2);
        request.setCmd(6);
        request.setParams(inputBean);
        request.setSerialDirect(lightConfig.isSerialDirect(), lightConfig.getSerialProtocolVersion());
        request.setUSBDirect(lightConfig.isUSBDirect());
        SCMIoTSender.sendRequest(request, new IDataCallback() {
            public void success(String result) {
                SensorLight.this.notifyEvent("_sensor.light.playSingle.ack", result);
            }

            public void error(ApiError error) {
                SensorLight.this.notifyEvent("_sensor.request.failed", error);
            }
        });
    }

    private void playMultiColor(LightConfig lightConfig) {
        this.log("playMultiColor", lightConfig.toString());
        if (lightConfig.getVer().equals("v1.0")) {
            this.playMultiColorV1(lightConfig);
        } else if (lightConfig.getVer().equals("v2.0")) {
            this.playMultiColorV2(lightConfig);
        } else if (lightConfig.getVer().equals("v3.0")) {
            this.playMultiColorV3(lightConfig);
        }

    }

    private void playMultiColorV1(LightConfig lightConfig) {
        this.log("playMultiColorV1");
        LEDMultiColorConfig config = new LEDMultiColorConfig();
        config.setNum((byte)lightConfig.getBeads().getNum());
        config.setMode((byte)lightConfig.getEffect().getId());
        config.setTime(lightConfig.getEffect().getTime());
        SCMRequest request = new SCMRequest();
        request.setDev(lightConfig.getDevId());
        request.setTopic(3);
        request.setType(2);
        request.setCmd(6);
        request.setParams(config);
        request.setSerialDirect(lightConfig.isSerialDirect(), lightConfig.getSerialProtocolVersion());
        request.setUSBDirect(lightConfig.isUSBDirect());
        SCMIoTSender.sendRequest(request, new IDataCallback() {
            public void success(String result) {
                SensorLight.this.notifyEvent("_sensor.light.playMulti.v1.ack", result);
            }

            public void error(ApiError error) {
                SensorLight.this.notifyEvent("_sensor.request.failed", error);
            }
        });
    }

    private void playMultiColorV2(LightConfig lightConfig) {
        this.log("playMultiColorV2");
        LEDMultiColorConfigV2 config = new LEDMultiColorConfigV2();
        config.setNum((short)lightConfig.getBeads().getNum());
        config.setHead((short)lightConfig.getBeads().getHead());
        config.setMode((short)lightConfig.getEffect().getId());
        config.setTime((short)((int)Math.floor((double)(lightConfig.getEffect().getTime() / 100))));
        SCMRequest request = new SCMRequest();
        request.setDev(lightConfig.getDevId());
        request.setTopic(11);
        request.setType(2);
        request.setCmd(6);
        request.setParams(config);
        if (!lightConfig.isTrans() && !this.isTrans()) {
            request.setSerialDirect(true, lightConfig.getSerialProtocolVersion());
        } else {
            request.setSerialDirect(false, lightConfig.getSerialProtocolVersion());
            request.setTrans(lightConfig.isTrans() || this.isTrans());
        }

        request.setUSBDirect(lightConfig.isUSBDirect() || this.isUsbDirect());
        SCMIoTSender.sendRequest(request, new IDataCallback() {
            public void success(String result) {
                SensorLight.this.notifyEvent("_sensor.light.playMulti.v2.ack", result);
            }

            public void error(ApiError error) {
                SensorLight.this.notifyEvent("_sensor.request.failed", error);
            }
        });
    }

    private void playMultiColorV3(LightConfig lightConfig) {
        this.log("playMultiColorV3");
        LEDMultiColorConfigV3 config = new LEDMultiColorConfigV3();
        config.setHead((short)lightConfig.getBeads().getHead());
        config.setNum((short)lightConfig.getBeads().getNum());
        config.setRgbs(lightConfig.getBeads().getBytes());
        SCMRequest request = new SCMRequest();
        request.setDev(lightConfig.getDevId());
        request.setTopic(22);
        request.setType(2);
        request.setCmd(6);
        request.setParams(config);
        if (!this.isUsbDirect() && !this.isSerialDirect() && !this.isTrans()) {
            request.setUSBDirect(true);
        } else {
            request.setUSBDirect(this.isUsbDirect());
            request.setTrans(this.isTrans());
            request.setSerialDirect(this.isSerialDirect());
        }

        SCMIoTSender.sendRequest(request, new IDataCallback() {
            public void success(String result) {
                SensorLight.this.notifyEvent("_sensor.light.playMulti.v3.ack", result);
            }

            public void error(ApiError error) {
                SensorLight.this.notifyEvent("_sensor.request.failed", error);
            }
        });
    }

    private void playHeartBeatV3() {
        this.log("playHeartBeatV3");
        SCMRequest request = new SCMRequest();
        if (this.heartBeatDev == -1) {
            request.setDev(6);
        } else {
            request.setDev(this.heartBeatDev);
        }

        request.setTopic(23);
        request.setType(2);
        request.setCmd(6);
        if (!this.isUsbDirect() && !this.isSerialDirect() && !this.isTrans()) {
            request.setUSBDirect(true);
        } else {
            request.setUSBDirect(this.isUsbDirect());
            request.setTrans(this.isTrans());
            request.setSerialDirect(this.isSerialDirect());
        }

        SCMIoTSender.sendRequest(request, new IDataCallback() {
            public void success(String result) {
                SensorLight.this.notifyEvent("_sensor.light.heartBeat.v3.ack", result);
            }

            public void error(ApiError error) {
                SensorLight.this.notifyEvent("_sensor.request.failed", error);
            }
        });
    }

    public void setRGB(LightConfig lightConfig) {
        this.log("setRGB", lightConfig.toString());
        if (lightConfig.getVer().equals("v1.0")) {
            this.setRGBV1(lightConfig);
        } else if (lightConfig.getVer().equals("v2.0")) {
            this.setRGBV2(lightConfig);
        }

    }

    private void setRGBV1(LightConfig lightConfig) {
        this.log("setRGBV1");
        byte[] rgb = new byte[]{(byte)lightConfig.getColor().getMask(), (byte)lightConfig.getColor().getR(), (byte)lightConfig.getColor().getG(), (byte)lightConfig.getColor().getB()};
        LEDRGBConfig inputBean = new LEDRGBConfig();
        inputBean.setRgb(ByteUtils.bytesToInt2(rgb, 0));
        SCMRequest request = new SCMRequest();
        request.setDev(lightConfig.getDevId());
        request.setTopic(3);
        request.setType(0);
        request.setCmd(1);
        request.setParams(inputBean);
        request.setCon(false);
        SCMIoTSender.sendRequest(request, new IDataCallback() {
            public void success(String result) {
                SensorLight.this.notifyEvent("_sensor.light.setRGB.v1.ack", result);
            }

            public void error(ApiError error) {
                SensorLight.this.notifyEvent("_sensor.request.failed", error);
            }
        });
    }

    private void setRGBV2(LightConfig lightConfig) {
        this.log("setRGBV2");
        byte[] rgb = new byte[]{(byte)lightConfig.getColor().getMask(), (byte)lightConfig.getColor().getR(), (byte)lightConfig.getColor().getG(), (byte)lightConfig.getColor().getB()};
        LEDRGBConfig inputBean = new LEDRGBConfig();
        inputBean.setRgb(ByteUtils.bytesToInt2(rgb, 0));
        SCMRequest request = new SCMRequest();
        request.setDev(lightConfig.getDevId());
        request.setTopic(11);
        request.setType(0);
        request.setCmd(1);
        request.setParams(inputBean);
        request.setSerialDirect(true);
        SCMIoTSender.sendRequest(request, new IDataCallback() {
            public void success(String result) {
                SensorLight.this.notifyEvent("_sensor.light.setRGB.v2.ack", result);
            }

            public void error(ApiError error) {
                SensorLight.this.notifyEvent("_sensor.request.failed", error);
            }
        });
    }
}
```