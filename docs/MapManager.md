```
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.sensor.map;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import androidx.annotation.NonNull;
import com.keenon.common.utils.FileUtil;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.api.MapDownLoadStatusV2Api;
import com.keenon.sdk.api.SystemStartUpApi;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.sensor.common.Event;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorObserver;
import java.io.File;
import java.io.IOException;

public class MapManager {
    private static final String TAG = "MapManager";
    private static volatile MapManager sInstance;
    public static final int COPY_TO_ROS_SUCCESS = 1001;
    public static final int COPY_TO_ANDROID_SUCCESS = 1002;
    public static final int COPY_TO_ROS_FAIL = 2001;
    public static final int COPY_TO_ANDROID_FAIL = 2002;
    public static final String SDCARD;
    private static final String ROS_DB_PATH;
    public static final String ROS_DS_FILE_NAME = "peanut.db";
    public static final String ROS_DS_FILE_ZIP_NAME = "PeanutDB.zip";
    private static final String DEFAULT_ZIP_PATH;
    private static final String DEFAULT_FILE_PATH;
    private static final long DELAY_TIME = 5000L;
    private static final long TIMEOUT = 180000L;
    private static final int CODE_TIMEOUT = 101;
    private static final int CODE_DELAY = 102;
    private MapListen mapListen;
    private Handler mHandler;
    private HandlerThread mHandlerThread = new HandlerThread("#MapManager");
    private SensorObserver sensorObserver = new SensorObserver() {
        public void onUpdate(Event event, Sensor sensor) {
            LogUtils.d("MapManager", "onUpdate Event " + event.toString());
            switch (event.getName()) {
                case "_sensor.map.download.fault.event":
                    ErrorCodeBean bean = (ErrorCodeBean)GsonUtil.gson2Bean(GsonUtil.bean2String(event.getData()), ErrorCodeBean.class);
                    MapManager.this.handleMapEvent(bean.getError());
                    break;
                case "_sensor.map.download.status":
                    LogUtils.d("MapManager", "onUpdate Event _sensor.map.download.status");
                    MapDownLoadStatusV2Api.Bean statusBean = (MapDownLoadStatusV2Api.Bean)GsonUtil.gson2Bean(event.getData().toString(), MapDownLoadStatusV2Api.Bean.class);
                    MapManager.this.handleMapStatus(statusBean);
            }

        }
    };

    public static MapManager getInstance() {
        if (sInstance == null) {
            synchronized(MapManager.class) {
                if (sInstance == null) {
                    sInstance = new MapManager();
                }
            }
        }

        return sInstance;
    }

    private MapManager() {
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper()) {
            public void handleMessage(@NonNull Message msg) {
                int what = msg.what;
                if (101 == what) {
                    MapManager.this.notifyFail();
                } else if (102 == what) {
                    MapManager.this.onLoopRebootState();
                }

            }
        };
    }

    public void addListen(MapListen listen) {
        this.mapListen = listen;
    }

    public void onImportToRos(String filePath) {
        LogUtils.d("MapManager", "onImportToRos");
        this.mHandler.sendEmptyMessageDelayed(101, 180000L);
        if (filePath != null && !filePath.isEmpty()) {
            File file = new File(filePath);
            if (!file.exists()) {
                LogUtils.e("MapManager", "file isEmpty Path : " + filePath, new NullPointerException());
                this.notifyFail();
            } else {
                MapConfig mapConfig = new MapConfig();
                mapConfig.setFileName("PeanutDB.zip");
                mapConfig.setFileDirectory(filePath.replace("PeanutDB.zip", ""));
                mapConfig.setType("database");
                mapConfig.setFileMd5(FileUtil.getFileMD5(file));
                mapConfig.setMapUuid(mapConfig.getFileMd5());
                if (!SensorMap.getInstance().contains(this.sensorObserver)) {
                    SensorMap.getInstance().addObserver(this.sensorObserver);
                }

                SensorMap.getInstance().download(mapConfig);
            }
        } else {
            LogUtils.e("MapManager", "filePath isEmpty", new NullPointerException());
            this.notifyFail();
        }
    }

    public void onImportToRos() {
        this.onImportToRos(DEFAULT_ZIP_PATH);
    }

    public void onExportToAndroid(String savePath) {
        LogUtils.d("MapManager", "onExportToAndroid");
        if (savePath != null && !savePath.isEmpty()) {
            File parentFolder = (new File(savePath)).getParentFile();
            if (parentFolder != null) {
                parentFolder.mkdirs();
            }

            (new Thread(() -> {
                try {
                    FileUtil.copyFile(ROS_DB_PATH, savePath);
                    String zipPath = savePath.replace("peanut.db", "PeanutDB.zip");
                    FileUtil.compressFile(savePath, zipPath);
                    FileUtil.deleteFile(new File(savePath));
                    this.notifyState(1002);
                } catch (IOException e) {
                    this.notifyState(2002);
                    e.printStackTrace();
                    LogUtils.e("MapManager", "[onCopyToAndroid]" + e.getMessage());
                }

            })).start();
        } else {
            LogUtils.e("MapManager", "SavePath isEmpty", new NullPointerException());
            this.notifyState(2002);
        }
    }

    public void onExportToAndroid() {
        this.onExportToAndroid(DEFAULT_FILE_PATH);
    }

    private void onStartReboot() {
        LogUtils.d("MapManager", "onStartReboot");
        PeanutSDK.getInstance().device().reboot(new IDataCallback() {
            public void success(String result) {
                LogUtils.d("MapManager", "onStartReboot success");
            }

            public void error(ApiError error) {
                LogUtils.d("MapManager", "onStartReboot error");
                MapManager.this.notifyFail();
            }
        });
        this.onLoopRebootState();
    }

    private void onLoopRebootState() {
        LogUtils.d("MapManager", "onLoopRebootState");
        PeanutSDK.getInstance().runtime().getSystemStartUpState(new IDataCallback() {
            public void success(String result) {
                LogUtils.d("MapManager", "onLoopRebootState result :" + result);
                SystemStartUpApi.Bean bean = (SystemStartUpApi.Bean)GsonUtil.gson2Bean(result, SystemStartUpApi.Bean.class);
                if (bean.data != null && bean.data.status == 1) {
                    MapManager.this.notifyState(1001);
                    if (MapManager.this.mHandler != null) {
                        MapManager.this.mHandler.removeCallbacksAndMessages((Object)null);
                    }
                } else if (MapManager.this.mHandler != null) {
                    MapManager.this.mHandler.sendEmptyMessageDelayed(102, 5000L);
                }

            }

            public void error(ApiError error) {
                if (MapManager.this.mHandler != null) {
                    MapManager.this.mHandler.sendEmptyMessageDelayed(102, 5000L);
                }

            }
        });
    }

    private void notifyFail() {
        this.notifyState(2001);
        if (this.mHandler != null) {
            this.mHandler.removeCallbacksAndMessages((Object)null);
        }

    }

    private void handleMapEvent(int code) {
        LogUtils.d("MapManager", "handleMapEvent code :" + code);
        if (1004 == code) {
            this.onStartReboot();
        } else if (1001 == code) {
            this.notifyFail();
        }

    }

    private void handleMapStatus(MapDownLoadStatusV2Api.Bean bean) {
        LogUtils.d("MapManager", "handleMapStatus");
        MapDownLoadStatusV2Api.Bean.DataBean dataBean = bean.getData();
        if (dataBean != null) {
            LogUtils.d("MapManager", "handleMapStatus status :" + dataBean.getStatus());
            if (dataBean.getStatus() == 1) {
                this.onStartReboot();
            }
        }

    }

    private void notifyState(int code) {
        LogUtils.i("MapManager", "[notifyState code :" + code + "]");
        if (this.mapListen != null) {
            this.mapListen.onResult(code);
        }

    }

    public void release() {
        LogUtils.d("MapManager", "release");
        SensorMap.getInstance().removeObserver(this.sensorObserver);
        SensorMap.getInstance().release();
        if (this.mHandler != null) {
            this.mHandler.removeCallbacks((Runnable)null);
        }

        if (this.mHandlerThread != null) {
            this.mHandlerThread.quit();
        }

        this.mHandlerThread = null;
        this.mHandler = null;
        sInstance = null;
    }

    static {
        SDCARD = Environment.getExternalStorageDirectory() + File.separator;
        ROS_DB_PATH = SDCARD + "ros" + File.separator + "config" + File.separator + "database" + File.separator + "peanut.db";
        DEFAULT_ZIP_PATH = SDCARD + "PeanutDB.zip";
        DEFAULT_FILE_PATH = SDCARD + "peanut.db";
    }

    public interface MapListen {
        void onResult(int var1);
    }
}
```