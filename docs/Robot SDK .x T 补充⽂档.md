# Robot SDK1.x T3 补充文档 

## 一、T3 仓内紫外灯控制 

通过以下代码可以控制 T3 仓内不同位置的紫外灯开关： 

```java
import com.keenon.sdk.scmIot.protopack.base.ProtoDev;
import com.keenon.sdk.sensor.uvlamp.SensorUVLamp;
```

```java
SensorUVLamp.getInstance().setUVLampSwitch(ProtoDev.SENSOR_UV_LAMP_1, isOpen);
SensorUVLamp.getInstance().setUVLampSwitch(ProtoDev.SENSOR_UV_LAMP_2, isOpen);
SensorUVLamp.getInstance().setUVLampSwitch(ProtoDev.SENSOR_UV_LAMP_3, isOpen);
```

## 二、T3 投影灯控制 

控制投影灯的示例代码如下： 

```java
import com.keenon.common.constant.PeanutConstants;
import com.keenon.sdk.scmIot.protopack.base.ProtoDev;
import com.keenon.sdk.sensor.light.LightConfig;
import com.keenon.sdk.sensor.light.SensorLight;
```

```java
    LightConfig config = new LightConfig(ProtoDev.SENSOR_SINGLE_LIGHT_1);
    config.setVer(PeanutConstants.SCM_VER_1);
    config.setType(0);
    if (isOpen) {
        config.getBlink().setOnTime(255);   // 开
    } else {
        config.getBlink().setOffTime(255);  // 关
    }
    config.setUSBDirect(true);
    SensorLight.getInstance().play(config);
```

## 三、T3 投影检测 

```java
import com.keenon.common.utils.GsonUtil;
import com.keenon.sdk.api.FdlCmdStatusApi;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.sensor.calibration.SensorCalibration;
```

```java
        /**
         *
         开启投影检测
         */
        private void checkFdlAutoDoorCmd() {
            SensorCalibration.getInstance().fdlCmd("auto_door_cmd", "start", new IDataCallback() {
                @Override
                public void success(String result) {
                    log("fdl/cmd auto_door_cmd start = " + result);
                    mStartLoopTime = System.currentTimeMillis();
                    log("检测中...");
                    mIsFirstLoop = true;
                    loopFdlCmdResult();
                }

                @Override
                public void error(ApiError error) {
                    log("fdl/cmd auto_door_cmd start = " + error.toString());
                }
            });
        }
        private long mStartLoopTime;
        private boolean mIsFirstLoop = false;
        private void loopFdlCmdResult() {
            if ((System.currentTimeMillis() - mStartLoopTime) > 35 * 1000) {
                log("fdl/- auto_door_cmd loop 3 5s timeout");
                log("循环检测超时！！！ ");
                return;
            }
            SensorCalibration.getInstance().fdlCmdStatus("auto_door_cmd", new IDataCallback() {
                @Override
                public void success(String result) {
                    log("fdl/checkStatus auto_door_cmd = " + result);
                    FdlCmdStatusApi.FdlCmdStatusBean info = GsonUtil.gson2Bean(result, FdlCmdStatusApi.FdlCmdStatusBean.class);
                    if (null != info) {
                        if (info.getData().isFinish()) {
                            if (mIsFirstLoop) {
                                mIsFirstLoop = false;
                                loopFdlCmdResult();
                                return;
                            }
                            if (1 == info.getData().getStatus()) {
                                // 检测成功
                            } else if (2 == info.getData().getStatus()) {
                                log("检测超时");
                            } else {
                                log("检测中....");
                            }
                        } else {
                            mIsFirstLoop = false;
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    loopFdlCmdResult();
                                }
                            }, 100);
                        }
                    } else {
                        log("检测数据异常");
                    }
                }
                @Override
                public void error(ApiError error){
                    Log("fdl/checkStatus auto_door_cmd = " + error.toString());
                    log("检测异常");
                }
            });
        }
```

