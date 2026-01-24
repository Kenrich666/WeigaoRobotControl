```
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.sensor.map;

import com.keenon.common.utils.GsonUtil;
import com.keenon.sdk.api.MapDownloadV2Api;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.tftp.TFTPServerManager;
import java.io.File;

public final class SensorMap extends Sensor {
    private static final String TAG = "SensorMap";
    private static volatile SensorMap sInstance;
    private MapConfig mMapConfig;
    private static final int RESET = 1;
    public static final int ERROR_CODE_TFTP_FAIL = 1001;
    public static final int ERROR_CODE_DOWNLOAD_ING = 1002;
    public static final int ERROR_CODE_DOWNLOAD_UUID_REPEAT = 1003;
    public static final int ERROR_CODE_DOWNLOAD_RESTART_ROS = 1004;
    private final IDataCallback sendInfoCallBack = new IDataCallback() {
        public void success(String result) {
            MapDownloadV2Api.Bean dataBean = (MapDownloadV2Api.Bean)GsonUtil.gson2Bean(result, MapDownloadV2Api.Bean.class);
            if (dataBean != null && dataBean.getData() != null && dataBean.getData().getStatus() != 1) {
                if (dataBean.getData().getStatus() == 2) {
                    SensorMap.this.notifyEvent("_sensor.map.download.fault.event", new ErrorCodeBean(1003));
                } else if (dataBean.getData().getStatus() == 3) {
                    SensorMap.this.notifyEvent("_sensor.map.download.fault.event", new ErrorCodeBean(1004));
                }
            }

        }

        public void error(ApiError error) {
            SensorMap.this.notifyEvent("_sensor.map.download.fault.event", new ErrorCodeBean(error.code));
        }
    };
    private final IDataCallback mCallBack = new IDataCallback() {
        public void success(String result) {
            SensorMap.this.notifyEvent("_sensor.map.download.status", result);
        }

        public void error(ApiError error) {
            SensorMap.this.notifyEvent("_sensor.map.download.fault.event", new ErrorCodeBean(error.code));
        }
    };

    public static SensorMap getInstance() {
        if (sInstance == null) {
            synchronized(SensorMap.class) {
                if (sInstance == null) {
                    sInstance = new SensorMap();
                }
            }
        }

        return sInstance;
    }

    private SensorMap() {
        PeanutSDK.getInstance().subscribe("MapDownLoadStatusV2Api", this.mCallBack);
    }

    public String name() {
        return this.getClass().getSimpleName();
    }

    public void mount() {
    }

    public void unMount() {
    }

    public void release() {
        TFTPServerManager.getInstance().stop();
        PeanutSDK.getInstance().map().sendMapDownLoadAction(new IDataCallback() {
            public void success(String result) {
            }

            public void error(ApiError error) {
            }
        }, 1);
        PeanutSDK.getInstance().unSubscribe("MapDownLoadStatusV2Api", this.mCallBack);
        sInstance = null;
    }

    public void download(MapConfig config) {
        this.mMapConfig = config;
        if (this.mMapConfig == null) {
            this.log("startTftp", "mMapConfig is NULL");
        } else {
            this.log("startTftp", "mMapConfig is " + this.mMapConfig.toString());
            this.startTftp();
        }
    }

    private void startTftp() {
        if (!TFTPServerManager.getInstance().isRunning() && !TFTPServerManager.getInstance().init(this.mMapConfig.getFileDirectory())) {
            this.notifyEvent("_sensor.map.download.fault.event", new ErrorCodeBean(1001));
        }

        String uri = TFTPServerManager.getInstance().getUri() + File.separator + this.mMapConfig.getFileName();
        PeanutSDK.getInstance().map().sendDownloadInfo(this.sendInfoCallBack, this.mMapConfig.getFileMd5(), uri, this.mMapConfig.getType(), this.mMapConfig.getMapUuid());
    }
}
```