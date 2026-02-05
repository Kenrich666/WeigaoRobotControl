```
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.sensor.calibration;

import com.keenon.sdk.api.AntiFallCalibrationApi;
import com.keenon.sdk.api.AntiFallStatusApi;
import com.keenon.sdk.api.FdlCmdApi;
import com.keenon.sdk.api.FdlCmdStatusApi;
import com.keenon.sdk.api.RobotDevicePostCheckApi;
import com.keenon.sdk.api.RobotDevicePostCheckStatusApi;
import com.keenon.sdk.api.RobotDevicePostCheckStatusV2Api;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.IDataCallback2;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.sensor.common.Sensor;
import java.util.HashMap;

public class SensorCalibration extends Sensor {
    private static volatile SensorCalibration sInstance;

    public static SensorCalibration getInstance() {
        if (sInstance == null) {
            synchronized(SensorCalibration.class) {
                if (sInstance == null) {
                    sInstance = new SensorCalibration();
                }
            }
        }

        return sInstance;
    }

    public String name() {
        return SensorCalibration.class.getSimpleName();
    }

    public void mount() {
    }

    public void unMount() {
    }

    public void release() {
    }

    public void runAntiFallCalibration(IDataCallback callback, int cmd, String set) {
        HashMap<String, Object> params = new HashMap();
        params.put("cmd", cmd);
        params.put("set", set == null ? "" : set);
        (new AntiFallCalibrationApi()).send(callback, params);
    }

    public void modifyFdlSwitch(final IDataCallback2<Boolean> callback, boolean isOpen) {
        HashMap<String, Object> params = new HashMap();
        params.put("cmd", 5);
        params.put("set", isOpen ? "function on" : "function off");
        (new AntiFallCalibrationApi()).send(new IDataCallback() {
            public void success(String result) {
                (new FdlSwitchStatusHelper(2)).getFdlSwitchStatus(callback);
            }

            public void error(ApiError error) {
                if (null != callback) {
                    callback.error(error);
                }

            }
        }, params);
    }

    public void getFdlSwitch(final IDataCallback2<Boolean> callback) {
        HashMap<String, Object> params = new HashMap();
        params.put("cmd", 5);
        params.put("set", "function status");
        (new AntiFallCalibrationApi()).send(new IDataCallback() {
            public void success(String result) {
                (new FdlSwitchStatusHelper(1)).getFdlSwitchStatus(callback);
            }

            public void error(ApiError error) {
                if (null != callback) {
                    callback.error(error);
                }

            }
        }, params);
    }

    public void getAntiFallStatus(IDataCallback callback) {
        (new AntiFallStatusApi()).send(callback);
    }

    public void runDevicesCalibration(IDataCallback callBack, SensorParamsBean params) {
        RobotDevicePostCheckApi.RobotCheckParams mParams = new RobotDevicePostCheckApi.RobotCheckParams();
        mParams.type = params.type;
        mParams.standard = params.standard;
        mParams.offset = params.offset;
        mParams.count = params.count;
        (new RobotDevicePostCheckApi()).send(callBack, mParams);
    }

    public void getDevicesCalibrationStatusCompat(final IDataCallback callback, final String type) {
        this.getDevicesCalibrationStatusV2(new IDataCallback() {
            public void success(String result) {
                if (null != callback) {
                    callback.success(result);
                }

            }

            public void error(ApiError error) {
                SensorCalibration.this.getDevicesCalibrationStatus(callback, type);
            }
        }, type);
    }

    public void getDevicesCalibrationStatus(IDataCallback callback, String type) {
        (new RobotDevicePostCheckStatusApi()).send(callback, type);
    }

    public void getDevicesCalibrationStatusV2(IDataCallback callback, String type) {
        (new RobotDevicePostCheckStatusV2Api()).send(callback, type);
    }

    public void fdlCmd(String type, String action, IDataCallback callback) {
        (new FdlCmdApi()).send(type, action, callback);
    }

    public void fdlCmdStatus(String type, IDataCallback callback) {
        (new FdlCmdStatusApi()).send(type, callback);
    }
}
```