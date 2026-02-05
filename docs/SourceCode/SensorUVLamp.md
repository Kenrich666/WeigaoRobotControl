```
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.sensor.uvlamp;

import com.keenon.common.utils.GsonUtil;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.IDataCallback2;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.BaseResponse;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.scmIot.bean.response.ReportInfo;
import com.keenon.sdk.sensor.common.Sensor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorUVLamp extends Sensor {
    private static volatile SensorUVLamp sInstance;
    private Map<Integer, Integer> mUvlampStatus = new HashMap();
    private int[] mUvlampDevs = new int[]{44, 45, 46, 47, 48};
    private IDataCallback mReportCallback = new IDataCallback() {
        public void success(String result) {
            ReportInfo status = (ReportInfo)GsonUtil.gson2Bean(result, ReportInfo.class);
            if (SensorUVLamp.this.mUvlampStatus.containsKey(status.getDev())) {
                SensorUVLamp.this.mUvlampStatus.put(status.getDev(), status.getInfo());
            }

            UVLampStatusInfo statusInfo = new UVLampStatusInfo();
            List<Integer> devStatus = new ArrayList();

            for(int dev : SensorUVLamp.this.mUvlampDevs) {
                devStatus.add(SensorUVLamp.this.mUvlampStatus.get(dev));
            }

            statusInfo.setDevStatus(devStatus);
            BaseResponse<UVLampStatusInfo> response = new BaseResponse();
            response.setCode(0);
            response.setStatus(0);
            response.setTopic("UVLampFaultApi");
            response.setData(statusInfo);
            PeanutSDK.getInstance().notifySubscribeSuccess("UVLampFaultApi", GsonUtil.bean2String(response));
        }

        public void error(ApiError error) {
        }
    };

    public static SensorUVLamp getInstance() {
        if (sInstance == null) {
            synchronized(SensorUVLamp.class) {
                if (sInstance == null) {
                    sInstance = new SensorUVLamp();
                }
            }
        }

        return sInstance;
    }

    private SensorUVLamp() {
        SCMIoTSender.addSCMThingDataTransfer(24, new UVLampDataTransfer());
        this.initStatus();
    }

    public void setDevs(@SensorUVLamp.UVLampDev int... devs) {
        SCMIoTSender.removeReportDataCallback(this.mUvlampDevs);
        if (null != devs) {
            this.mUvlampDevs = devs;
            this.initStatus();
        }

    }

    private void initStatus() {
        this.mUvlampStatus.clear();
        SCMIoTSender.addReportDataCallback(this.mReportCallback, this.mUvlampDevs);

        for(int dev : this.mUvlampDevs) {
            this.mUvlampStatus.put(dev, UVLampStatus.NO_FAULT.code());
        }

    }

    public String name() {
        return this.getClass().getSimpleName();
    }

    public void mount() {
    }

    public void unMount() {
    }

    public void release() {
        SCMIoTSender.removeSCMThingDataTransfer(24);
        SCMIoTSender.removeReportDataCallback(this.mUvlampDevs);
        sInstance = null;
    }

    public void getUVLampSwitch(@SensorUVLamp.UVLampDev int dev, final IDataCallback2<Boolean> callback) {
        SCMRequest request = new SCMRequest();
        request.setDev(dev);
        request.setTopic(24);
        request.setType(0);
        request.setCmd(0);
        request.setSerialDirect(!this.isUsbDirect());
        request.setUSBDirect(this.isUsbDirect());
        SCMIoTSender.sendRequest(request, new IDataCallback() {
            public void success(String result) {
                UVLampResult lampResult = (UVLampResult)GsonUtil.gson2Bean(result, UVLampResult.class);
                if (null != callback) {
                    callback.success((lampResult.getResult() & 255) == 255);
                }

            }

            public void error(ApiError error) {
                if (null != callback) {
                    callback.error(error);
                }

            }
        });
    }

    public void setUVLampSwitch(@SensorUVLamp.UVLampDev int dev, boolean isOpen) {
        SCMRequest request = new SCMRequest();
        request.setDev(dev);
        request.setTopic(24);
        request.setType(2);
        request.setCmd(6);
        UVLampResult prams = new UVLampResult();
        prams.setResult((byte)(isOpen ? 255 : 0));
        request.setParams(prams);
        request.setSerialDirect(!this.isUsbDirect());
        request.setUSBDirect(this.isUsbDirect());
        SCMIoTSender.sendRequest(request, new IDataCallback() {
            public void success(String result) {
                SensorUVLamp.this.notifyEvent("_sensor.uvlamp.setSwitch.ack", result);
            }

            public void error(ApiError error) {
                SensorUVLamp.this.notifyEvent("_sensor.uvlamp.setSwitch.ack", error);
            }
        });
    }

    public @interface UVLampDev {
    }
}
```