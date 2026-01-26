//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.component.runtime;

import com.keenon.common.external.PeanutConfig;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.PeanutMainThreadExecutor;
import com.keenon.common.utils.StringUtils;
import com.keenon.sdk.api.BatteryStatusApi;
import com.keenon.sdk.api.ButtonStatusApi;
import com.keenon.sdk.api.DeviceInfoApi;
import com.keenon.sdk.api.MotorStatusApi;
import com.keenon.sdk.api.NavigationAllApi;
import com.keenon.sdk.api.NavigationDestPoseApi;
import com.keenon.sdk.api.PositionRequestApi;
import com.keenon.sdk.api.RuntimeHeartbeatApi;
import com.keenon.sdk.api.RuntimeOdoApi;
import com.keenon.sdk.api.RuntimeRkIPApi;
import com.keenon.sdk.config.PeanutProperties;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

public class PeanutRuntime {
    private static final String TAG = "[PeanutRuntime]";
    private static volatile PeanutRuntime sInstance;
    private ScheduledExecutorService mScheduledThreadPool = Executors.newScheduledThreadPool(5);
    private volatile RuntimeInfo mRuntimeInfo;
    private final List<Listener> mListeners = new ArrayList();
    private int mWorkMode;
    private boolean mStarted;
    private PeanutMainThreadExecutor mainThreadExecutor = PeanutSDK.getInstance().getMainThreadExecutor();
    private IDataCallback mWorkModeCallback = new IDataCallback() {
        public void success(String response) {
            ApiData data = (ApiData)GsonUtil.gson2Bean(response, ApiData.class);
            if (null != data && data.getStatus() == 0) {
                LogUtils.d("RUNTIME--", "[PeanutRuntime][setWorkMode][切换工作模式成功： " + PeanutRuntime.this.mWorkMode + "]");
                if (PeanutRuntime.this.mWorkMode != PeanutRuntime.this.mRuntimeInfo.getWorkMode()) {
                    PeanutRuntime.this.mRuntimeInfo.setWorkMode(PeanutRuntime.this.mWorkMode);
                    PeanutRuntime.this.notifyEvent(10010, PeanutRuntime.this.mWorkMode);
                }
            }

        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][set work mode][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mSetEmergencyCallback = new IDataCallback() {
        public void success(String response) {
            ApiData data = (ApiData)GsonUtil.gson2Bean(response, ApiData.class);
            if (null != data && data.getStatus() == 0) {
                LogUtils.d("RUNTIME--", "[PeanutRuntime][setEmergency]");
            }

        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][set emergency][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mEmergencyInfoCallback = new IDataCallback() {
        public void success(String response) {
            ButtonStatusApi.Bean data = (ButtonStatusApi.Bean)GsonUtil.gson2Bean(response, ButtonStatusApi.Bean.class);
            if (null != data && null != data.getData()) {
                boolean state = data.getData().isInfo();
                if (state != PeanutRuntime.this.mRuntimeInfo.isEmergencyOpen()) {
                    PeanutRuntime.this.mRuntimeInfo.setEmergencyOpen(state);
                    PeanutRuntime.this.notifyEvent(10013, data.getData().isInfo());
                }
            }

        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][get emergency status][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mMotorInfoCallback = new IDataCallback() {
        public void success(String response) {
            MotorStatusApi.Bean data = (MotorStatusApi.Bean)GsonUtil.gson2Bean(response, MotorStatusApi.Bean.class);
            if (null != data && null != data.getData()) {
                int motorStatus = data.getData().getStatus();
                if (motorStatus > -1 && motorStatus != PeanutRuntime.this.mRuntimeInfo.getMotorStatus()) {
                    PeanutRuntime.this.notifyEvent(10014, motorStatus);
                    PeanutRuntime.this.mRuntimeInfo.setMotorStatus(motorStatus);
                }
            }

        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][get motor status][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mRobotIPCallback = new IDataCallback() {
        public void success(String response) {
            RuntimeRkIPApi.Bean data = (RuntimeRkIPApi.Bean)GsonUtil.gson2Bean(response, RuntimeRkIPApi.Bean.class);
            if (null != data && data.getStatus() == 0) {
                String ip = data.getData().getIp();
                if (StringUtils.isNotEmpty(ip) && !ip.equals(PeanutRuntime.this.mRuntimeInfo.getRobotIp())) {
                    PeanutRuntime.this.mRuntimeInfo.setRobotIp(ip);
                    PeanutRuntime.this.notifyEvent(10012, ip);
                }
            }

        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][get ip addr][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mArmInfoCallback = new IDataCallback() {
        public void success(String response) {
            DeviceInfoApi.Bean data = (DeviceInfoApi.Bean)GsonUtil.gson2Bean(response, DeviceInfoApi.Bean.class);
            if (null != data && data.getStatus() == 0) {
                LogUtils.d("RUNTIME--", "[PeanutRuntime][获取arm板信息][" + response + "]");
                PeanutRuntime.this.mRuntimeInfo.setRobotArmInfo(response);
            }

        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][get arm info][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mStm32InfoCallback = new IDataCallback() {
        public void success(String response) {
            DeviceInfoApi.Bean data = (DeviceInfoApi.Bean)GsonUtil.gson2Bean(response, DeviceInfoApi.Bean.class);
            if (null != data && data.getStatus() == 0) {
                LogUtils.d("RUNTIME--", "[PeanutRuntime][获取stm32板信息][" + response + "]");
                PeanutRuntime.this.mRuntimeInfo.setRobotStm32Info(response);
            }

        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][get stm32 info][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mPowerCallback = new IDataCallback() {
        public void success(String response) {
            BatteryStatusApi.Bean data = (BatteryStatusApi.Bean)GsonUtil.gson2Bean(response, BatteryStatusApi.Bean.class);
            LogUtils.d("RUNTIME--", "[PeanutRuntime][电量查询回调][" + response + "]");
            if (null != data && null != data.getData()) {
                int dumpPower = data.getData().getPower();
                int power;
                if (dumpPower <= 0) {
                    power = 1;
                } else if (dumpPower > 100) {
                    power = 100;
                } else {
                    power = dumpPower;
                }

                if (power != PeanutRuntime.this.mRuntimeInfo.getPower()) {
                    PeanutRuntime.this.mRuntimeInfo.setPower(power);
                    PeanutRuntime.this.notifyEvent(10011, power);
                }
            }

        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][get power status][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mOdoCallback = new IDataCallback() {
        public void success(String response) {
            LogUtils.d("RUNTIME--", "[PeanutRuntime][odo][response = " + response + "]");
            RuntimeOdoApi.Bean data = (RuntimeOdoApi.Bean)GsonUtil.gson2Bean(response, RuntimeOdoApi.Bean.class);
            if (null != data && null != data.getData()) {
                Double odo = data.getData().getOdo();
                LogUtils.d("RUNTIME--", "[PeanutRuntime][odo:" + odo + "]");
                if (odo.compareTo(new Double((double)0.0F)) > 0 && odo.compareTo(PeanutRuntime.this.mRuntimeInfo.getTotalOdo()) > 0) {
                    PeanutRuntime.this.mRuntimeInfo.setTotalOdo(odo);
                    PeanutRuntime.this.notifyEvent(10015, odo);
                }
            }

        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][get odo status][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mHealthCallback = new IDataCallback() {
        public void success(String response) {
            LogUtils.d("RUNTIME--", "[PeanutRuntime][health][response = " + response + "]");
            PeanutRuntime.this.notifyHealth(response);
        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][get health status][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mHeartbeatCallback = new IDataCallback() {
        public void success(String response) {
            LogUtils.d("RUNTIME--", "[PeanutRuntime][heartbeat][response = " + response + "]");
            RuntimeHeartbeatApi.Bean data = (RuntimeHeartbeatApi.Bean)GsonUtil.gson2Bean(response, RuntimeHeartbeatApi.Bean.class);
            if (null != data && null != data.getData()) {
                int syncStatus = data.getData().getSync() == null ? -1 : data.getData().getSync().getStatus();
                if (PeanutRuntime.this.mRuntimeInfo.getSyncStatus() == -1) {
                    PeanutRuntime.this.mRuntimeInfo.setSyncStatus(syncStatus);
                } else if (syncStatus != -1 && syncStatus != PeanutRuntime.this.mRuntimeInfo.getSyncStatus()) {
                    LogUtils.i("[PeanutRuntime]", "robot reboot, current status: " + syncStatus + ", previous status: " + PeanutRuntime.this.mRuntimeInfo.getSyncStatus());
                    if (syncStatus == 0) {
                        PeanutRuntime.this.notifyEvent(10001, "robot startup");
                    } else if (syncStatus == 1) {
                        PeanutRuntime.this.notifyEvent(10002, "robot init");
                        PeanutRuntime.this.start();
                    } else if (syncStatus == 2) {
                        PeanutRuntime.this.notifyEvent(10003, "robot ready");
                    }

                    PeanutRuntime.this.mRuntimeInfo.setSyncStatus(syncStatus);
                }
            }

            PeanutRuntime.this.notifyHeartbeat(response);
        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][get heartbeat status][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mSyncParamsCallback = new IDataCallback() {
        public void success(String response) {
            LogUtils.d("RUNTIME--", "syncParams2Robot success " + response);
            ApiData data = (ApiData)GsonUtil.gson2Bean(response, ApiData.class);
            if (data.getCode() == -1) {
                PeanutRuntime.this.notifyEvent(10004, response);
            } else {
                PeanutRuntime.this.notifyEvent(10005, (Object)null);
            }

        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][sync params][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mLocationCallback = new IDataCallback() {
        public void success(String response) {
            LogUtils.d("RUNTIME--", "location success " + response);
            PositionRequestApi.Bean data = (PositionRequestApi.Bean)GsonUtil.gson2Bean(response, PositionRequestApi.Bean.class);
            if (data.isSuccess() && data.getData() != null) {
                PeanutRuntime.this.notifyEvent(10016, data.getData().getStatus());
            }

        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][set location][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mSetTimeCallback = new IDataCallback() {
        public void success(String response) {
            LogUtils.d("RUNTIME--", "set time success " + response);
            PositionRequestApi.Bean data = (PositionRequestApi.Bean)GsonUtil.gson2Bean(response, PositionRequestApi.Bean.class);
            if (data.isSuccess() && data.getData() != null) {
                PeanutRuntime.this.notifyEvent(10017, data.getData().getStatus());
            }

        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][set time][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mGetPathCallback = new IDataCallback() {
        public void success(String response) {
            LogUtils.d("RUNTIME--", "get path success " + response);
            PeanutRuntime.this.notifyEvent(10018, response);
        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][get path][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mRobotDestListCallback = new IDataCallback() {
        public void success(String response) {
            NavigationAllApi.Bean data = (NavigationAllApi.Bean)GsonUtil.gson2Bean(response, NavigationAllApi.Bean.class);
            if (null != data && data.getStatus() == 0) {
                PeanutRuntime.this.mRuntimeInfo.setDestList(response);
            }

        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][get dest list][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };
    private IDataCallback mRosDestListCallback = new IDataCallback() {
        public void success(String response) {
            NavigationDestPoseApi.Bean data = (NavigationDestPoseApi.Bean)GsonUtil.gson2Bean(response, NavigationDestPoseApi.Bean.class);
            if (null != data && data.getStatus() == 0) {
                PeanutRuntime.this.mRuntimeInfo.setDestList(response);
            }

        }

        public void error(ApiError error) {
            LogUtils.e("RUNTIME--", "[PeanutRuntime][get dest list][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, (Object)null);
        }
    };

    private PeanutRuntime() {
    }

    public static PeanutRuntime getInstance() {
        if (sInstance == null) {
            synchronized(PeanutRuntime.class) {
                if (sInstance == null) {
                    sInstance = new PeanutRuntime();
                }
            }
        }

        return sInstance;
    }

    public void registerListener(Listener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.mListeners.remove(listener);
    }

    public void start(Listener listener) {
        this.registerListener(listener);
        this.start();
    }

    public synchronized void start() {
        if (this.mStarted) {
            LogUtils.d("RUNTIME--", "[runtime already start.]");
        } else {
            this.initData();
            this.initState();
            this.initScheduledTask();
            this.initSubscribeTask();
            this.mStarted = true;
        }
    }

    public void destroy() {
        LogUtils.d("RUNTIME--", "[destroy]");
        this.stopSubscribe();
        this.mScheduledThreadPool.shutdown();
        this.mainThreadExecutor.release();
        this.mListeners.clear();
        this.mRuntimeInfo = null;
        this.mStarted = false;
        sInstance = null;
    }

    public RuntimeInfo getRuntimeInfo() {
        return this.mRuntimeInfo;
    }

    public void setIP(String ip, int port) {
        LogUtils.i("RUNTIME--", "[setIP]ip： " + ip + ", port: " + port + "]");
        PeanutConfig.getConfig().setLinkIP(ip);
        PeanutConfig.getConfig().setLinkPort(port);
    }

    private void initData() {
        this.mRuntimeInfo = new RuntimeInfo();
        this.mRuntimeInfo.setPower(1);
        this.mRuntimeInfo.setWorkMode(-1);
        this.mRuntimeInfo.setSyncStatus(-1);
        this.mRuntimeInfo.setMotorStatus(-1);
        this.mRuntimeInfo.setEmergencyOpen(false);
    }

    private void initState() {
        this.setWorkMode(1);
        this.queryPower();
        this.queryIpAddress();
        this.queryRobotArm();
        this.queryRobotStm32();
        this.queryDestList();
        this.syncHeartBeat();
        this.syncParams2Robot(false);
        if (PeanutProperties.getValue("app.jerk.enable", 1) != 1) {
            this.setEmergencyEnable(false);
        } else {
            this.setEmergencyEnable(true);
            this.queryLockButtonInfo();
            this.queryMotorStatus();
        }

    }

    private void initScheduledTask() {
        this.syncRobotIP();
        this.syncRobotOdo();
        this.syncRobotHeartBeat();
        this.syncPower();
    }

    private void syncRobotIP() {
        this.mScheduledThreadPool.scheduleAtFixedRate(new Runnable() {
            public void run() {
                PeanutRuntime.this.queryIpAddress();
            }
        }, 0L, 60L, TimeUnit.SECONDS);
    }

    private void syncRobotOdo() {
        this.mScheduledThreadPool.scheduleAtFixedRate(new Runnable() {
            public void run() {
                PeanutRuntime.this.queryOdo();
            }
        }, 0L, 3L, TimeUnit.SECONDS);
    }

    private void syncRobotHeartBeat() {
        this.mScheduledThreadPool.scheduleAtFixedRate(new Runnable() {
            public void run() {
                PeanutRuntime.this.queryHearBeat();
            }
        }, 0L, 5L, TimeUnit.SECONDS);
    }

    private void syncPower() {
        this.mScheduledThreadPool.scheduleAtFixedRate(new Runnable() {
            public void run() {
                PeanutRuntime.this.queryPower();
            }
        }, 0L, 60L, TimeUnit.SECONDS);
    }

    private void initSubscribeTask() {
        this.stopSubscribe();
        this.startSubscribe();
    }

    private void startSubscribe() {
        PeanutSDK.getInstance().subscribe("ButtonStatusApi", this.mEmergencyInfoCallback);
        PeanutSDK.getInstance().subscribe("MotorStatusApi", this.mMotorInfoCallback);
        PeanutSDK.getInstance().subscribe("RuntimeHealthApi", this.mHealthCallback);
    }

    private void stopSubscribe() {
        PeanutSDK.getInstance().unSubscribe("ButtonStatusApi", this.mEmergencyInfoCallback);
        PeanutSDK.getInstance().unSubscribe("MotorStatusApi", this.mMotorInfoCallback);
        PeanutSDK.getInstance().unSubscribe("RuntimeHealthApi", this.mHealthCallback);
    }

    private void queryHearBeat() {
        LogUtils.d("RUNTIME--", "[PeanutRuntime][queryHearBeat]");
        PeanutSDK.getInstance().runtime().getHeartBeat(this.mHeartbeatCallback);
    }

    private void queryPower() {
        LogUtils.d("RUNTIME--", "[PeanutRuntime][queryPower]");
        PeanutSDK.getInstance().battery().getStatus(this.mPowerCallback);
    }

    private void queryOdo() {
        LogUtils.d("RUNTIME--", "[PeanutRuntime][queryOdo]");
        PeanutSDK.getInstance().runtime().getOdo(this.mOdoCallback);
    }

    private void queryIpAddress() {
        LogUtils.d("RUNTIME--", "[PeanutRuntime][queryIpAddress]");
        PeanutSDK.getInstance().runtime().getIPAddress(this.mRobotIPCallback);
    }

    private void queryRobotArm() {
        String robotArm = "{\"id\": 1,\"name\": \"robot-arm\"}";
        PeanutSDK.getInstance().device().getBoardInfo(this.mArmInfoCallback, robotArm);
    }

    private void queryRobotStm32() {
        String robotStm32 = "{\"id\": 2,\"name\": \"robot-stm32\"}";
        PeanutSDK.getInstance().device().getBoardInfo(this.mStm32InfoCallback, robotStm32);
    }

    public void setEmergencyEnable(boolean enable) {
        PeanutSDK.getInstance().device().setButtonEnable(this.mSetEmergencyCallback, enable);
    }

    private void queryLockButtonInfo() {
        PeanutSDK.getInstance().device().getScramButtonStatus(this.mEmergencyInfoCallback);
    }

    private void queryMotorStatus() {
        PeanutSDK.getInstance().motor().getStatus(this.mMotorInfoCallback);
    }

    private void queryDestList() {
        PeanutSDK.getInstance().navigation().getAllTargets(this.mRobotDestListCallback);
        PeanutSDK.getInstance().navigation().getAllDestPose(this.mRosDestListCallback);
    }

    public void setWorkMode(int mode) {
        this.mWorkMode = mode;
        PeanutSDK.getInstance().runtime().setMode(this.mWorkModeCallback, this.mWorkMode);
    }

    public void setTime(long timestamp) {
        PeanutSDK.getInstance().runtime().setTime(this.mSetTimeCallback, timestamp);
    }

    public void getPath() {
        PeanutSDK.getInstance().runtime().getPath(this.mGetPathCallback);
    }

    public void syncParams2Robot(boolean needReboot) {
        HashMap<String, String> params = PeanutProperties.loadRobotParams();
        params.put("AppRestart", needReboot ? "0" : "1");
        PeanutSDK.getInstance().device().updateConfig(this.mSyncParamsCallback, (new JSONObject(params)).toString());
    }

    public void syncParams2RobotV2(boolean needReboot) {
        HashMap<String, String> params = PeanutProperties.loadRobotParams();
        params.put("AppRestart", needReboot ? "0" : "1");
        PeanutSDK.getInstance().device().updateConfigCompat(this.mSyncParamsCallback, (new JSONObject(params)).toString());
    }

    public void location() {
        PeanutSDK.getInstance().runtime().location(this.mLocationCallback);
    }

    private void syncHeartBeat() {
        PeanutSDK.getInstance().runtime().sync((IDataCallback)null, System.currentTimeMillis());
    }

    private void notifyEvent(final int event, final Object object) {
        for(final Listener listener : this.mListeners) {
            LogUtils.d("RUNTIME--", "[PeanutRuntime][notifyEvent][event： " + event + "]");
            this.mainThreadExecutor.execute(new Runnable() {
                public void run() {
                    listener.onEvent(event, object);
                }
            });
        }

    }

    private void notifyHealth(final Object object) {
        for(final Listener listener : this.mListeners) {
            this.mainThreadExecutor.execute(new Runnable() {
                public void run() {
                    listener.onHealth(object);
                }
            });
        }

    }

    private void notifyHeartbeat(final Object object) {
        for(final Listener listener : this.mListeners) {
            this.mainThreadExecutor.execute(new Runnable() {
                public void run() {
                    listener.onHeartbeat(object);
                }
            });
        }

    }

    public interface Listener {
        void onEvent(int var1, Object var2);

        void onHealth(Object var1);

        void onHeartbeat(Object var1);
    }
}
