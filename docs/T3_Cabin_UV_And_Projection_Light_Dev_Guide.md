# T3 仓内紫外灯与投影灯开发指南

本文档基于 `Robot SDK .x T 补充文档` 及源码 `SensorUVLamp`、`SensorLight` 等类库整理，旨在指导开发者如何在 T3 机器人上进行仓内紫外灯控制、投影灯控制及投影检测功能的开发。

## 一、 仓内紫外灯控制 (Cabin UV Lamp)

仓内紫外灯主要用于消毒或其他业务场景。SDK 提供了 `SensorUVLamp` 类来管理紫外灯的开关和状态查询。

### 1.1 关键类与接口

*   **控制类**: `com.keenon.sdk.sensor.uvlamp.SensorUVLamp`
*   **常量类**: `com.keenon.sdk.scmIot.protopack.base.ProtoDev`
*   **设备 ID**:
    *   `ProtoDev.SENSOR_UV_LAMP_1` (44)
    *   `ProtoDev.SENSOR_UV_LAMP_2` (45)
    *   `ProtoDev.SENSOR_UV_LAMP_3` (46)

### 1.2 控制紫外灯开关

使用 `setUVLampSwitch` 方法控制指定紫外灯的开关状态。

**方法签名**:
```java
public void setUVLampSwitch(int dev, boolean isOpen)
```
*   `dev`: 紫外灯设备 ID (如 `ProtoDev.SENSOR_UV_LAMP_1`)
*   `isOpen`: `true` 开启，`false` 关闭

**示例代码**:

```java
import com.keenon.sdk.scmIot.protopack.base.ProtoDev;
import com.keenon.sdk.sensor.uvlamp.SensorUVLamp;

// 开启 1 号紫外灯
SensorUVLamp.getInstance().setUVLampSwitch(ProtoDev.SENSOR_UV_LAMP_1, true);

// 关闭 1 号紫外灯
SensorUVLamp.getInstance().setUVLampSwitch(ProtoDev.SENSOR_UV_LAMP_1, false);

// 同时开启多个紫外灯
SensorUVLamp.getInstance().setUVLampSwitch(ProtoDev.SENSOR_UV_LAMP_2, true);
SensorUVLamp.getInstance().setUVLampSwitch(ProtoDev.SENSOR_UV_LAMP_3, true);
```

### 1.3 查询紫外灯状态

使用 `getUVLampSwitch` 方法异步获取当前紫外灯的开关状态。

**方法签名**:
```java
public void getUVLampSwitch(int dev, final IDataCallback2<Boolean> callback)
```

**示例代码**:

```java
import com.keenon.sdk.external.IDataCallback2;
import com.keenon.sdk.hedera.model.ApiError;

SensorUVLamp.getInstance().getUVLampSwitch(ProtoDev.SENSOR_UV_LAMP_1, new IDataCallback2<Boolean>() {
    @Override
    public void success(Boolean isOpen) {
        // isOpen 为 true 表示开启，false 表示关闭
        Log.d("UVLamp", "Lamp 1 is " + (isOpen ? "On" : "Off"));
    }

    @Override
    public void error(ApiError error) {
        Log.e("UVLamp", "Get status failed: " + error.toString());
    }
});
```

---

## 二、 投影灯控制 (Projection Light)

投影灯控制较为通用，使用 `SensorLight` 类配合 `LightConfig` 配置对象来实现。

### 2.1 关键类与接口

*   **控制类**: `com.keenon.sdk.sensor.light.SensorLight`
*   **配置类**: `com.keenon.sdk.sensor.light.LightConfig`
*   **常量类**: `com.keenon.sdk.scmIot.protopack.base.ProtoDev`
*   **常量类**: `com.keenon.common.constant.PeanutConstants`
*   **设备 ID**:
    *   `ProtoDev.SENSOR_SINGLE_LIGHT_1` (33) - 通常用于投影灯
    *   `ProtoDev.SENSOR_SINGLE_LIGHT_2` (34)
    *   其他需根据具体机器配置确认

### 2.2 控制逻辑

控制投影灯的核心在于构造一个 `LightConfig` 对象，设置其类型、版本、以及闪烁参数(`Blink`)，然后调用 `SensorLight.getInstance().play(config)`。

*   **Type**: 设置为 `0` (通常代表单色/开关型灯光)。
*   **Version**: 设置为 `PeanutConstants.SCM_VER_1`。
*   **USBDirect**: 设置为 `true` (根据示例代码)。
*   **开关控制**:
    *   **开启**: `config.getBlink().setOnTime(255)`
    *   **关闭**: `config.getBlink().setOffTime(255)`

### 2.3 示例代码

```java
import com.keenon.common.constant.PeanutConstants;
import com.keenon.sdk.scmIot.protopack.base.ProtoDev;
import com.keenon.sdk.sensor.light.LightConfig;
import com.keenon.sdk.sensor.light.SensorLight;

/**
 * 控制投影灯开关
 * @param isOpen true 开启, false 关闭
 */
public void controlProjectionLight(boolean isOpen) {
    // 1. 创建配置对象，传入设备ID
    LightConfig config = new LightConfig(ProtoDev.SENSOR_SINGLE_LIGHT_1);

    // 2. 设置基本参数
    config.setVer(PeanutConstants.SCM_VER_1);
    config.setType(0); // 0 代表简单控制类型
    config.setUSBDirect(true); // 直连模式

    // 3. 设置开关状态参数
    if (isOpen) {
        // 设置开启时间 255 (常亮逻辑)
        config.getBlink().setOnTime(255);
        // 清除关闭时间设置 (可选，视具体逻辑而定，建议保持默认或清零)
        config.getBlink().setOffTime(0); 
    } else {
        // 设置关闭时间 255 (常灭逻辑)
        config.getBlink().setOffTime(255);
        // 清除开启时间
        config.getBlink().setOnTime(0);
    }

    // 4. 下发指令
    SensorLight.getInstance().play(config);
}
```

---

## 三、 投影检测 (Projection Detection)

投影检测用于检测投影灯硬件是否工作正常（如是否能投射图案、自动门指令检测等），依赖底层校准接口 `SensorCalibration` 发送 `fdlCmd` 指令。

**注意**: 该过程是异步且需要轮询结果的。

### 3.1 关键类

*   `com.keenon.sdk.sensor.calibration.SensorCalibration`
*   `com.keenon.sdk.api.FdlCmdStatusApi`
*   `com.keenon.sdk.external.IDataCallback`

### 3.2 检测流程

1.  **发送开始指令**: 调用 `SensorCalibration.getInstance().fdlCmd("auto_door_cmd", "start", ...)`。
2.  **轮询状态**: 成功发送开始指令后，进入循环检测状态。
3.  **检查结果**: 调用 `SensorCalibration.getInstance().fdlCmdStatus("auto_door_cmd", ...)` 获取当前状态。
    *   检查返回数据的 `status` 字段：
        *   `1`: 检测成功 (Finish)
        *   `2`: 检测超时 (Timeout)
        *   其他: 检测中 (Running)
4.  **超时处理**: 建议设置约 35 秒的总超时时间，防止无限轮询。

### 3.3 详细实现示例

```java
import android.os.Handler;
import android.util.Log;
import com.keenon.common.utils.GsonUtil;
import com.keenon.sdk.api.FdlCmdStatusApi;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.sensor.calibration.SensorCalibration;

public class ProjectionDetector {
    private static final String TAG = "ProjectionDetector";
    private long mStartLoopTime;
    private boolean mIsFirstLoop = false;
    private Handler mHandler = new Handler();
    private static final int TIMEOUT_MS = 35 * 1000; // 35秒超时

    /**
     * 开始投影检测
     */
    public void startCheck() {
        Log.i(TAG, "开始投影检测...");
        SensorCalibration.getInstance().fdlCmd("auto_door_cmd", "start", new IDataCallback() {
            @Override
            public void success(String result) {
                Log.i(TAG, "Command start success: " + result);
                mStartLoopTime = System.currentTimeMillis();
                mIsFirstLoop = true;
                // 开始轮询结果
                loopFdlCmdResult();
            }

            @Override
            public void error(ApiError error) {
                Log.e(TAG, "Command start failed: " + error.toString());
            }
        });
    }

    /**
     * 轮询检测结果
     */
    private void loopFdlCmdResult() {
        // 1. 检查总超时
        if ((System.currentTimeMillis() - mStartLoopTime) > TIMEOUT_MS) {
            Log.e(TAG, "Detection Timeout (35s)!");
            return;
        }

        // 2. 查询状态
        SensorCalibration.getInstance().fdlCmdStatus("auto_door_cmd", new IDataCallback() {
            @Override
            public void success(String result) {
                Log.d(TAG, "Status check: " + result);
                FdlCmdStatusApi.FdlCmdStatusBean info = GsonUtil.gson2Bean(result, FdlCmdStatusApi.FdlCmdStatusBean.class);
                
                if (info != null && info.getData() != null) {
                    // 如果流程结束 (isFinish 为 true)
                    if (info.getData().isFinish()) {
                        // 首次轮询由于状态同步延迟，可能需要跳过一次
                        if (mIsFirstLoop) {
                            mIsFirstLoop = false;
                            loopFdlCmdResult(); // 再次查询
                            return;
                        }

                        // 判断最终状态
                        int status = info.getData().getStatus();
                        if (status == 1) {
                            Log.i(TAG, "检测成功！(Success)");
                        } else if (status == 2) {
                            Log.w(TAG, "检测超时/失败 (Timeout/Fail)");
                        } else {
                            Log.d(TAG, "检测中... (Still running logic if finished but status not final)");
                        }
                    } else {
                        // 流程未结束，继续延时轮询
                        mIsFirstLoop = false;
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                loopFdlCmdResult();
                            }
                        }, 100); // 100ms 后再次查询
                    }
                } else {
                    Log.e(TAG, "Data Error: Result parsed null");
                }
            }

            @Override
            public void error(ApiError error) {
                Log.e(TAG, "Status check error: " + error.toString());
            }
        });
    }
}
```

## 四、 总结

1.  **紫外灯**：使用 `SensorUVLamp` 单例，通过 `setUVLampSwitch` 直接控制。
2.  **投影灯**：使用 `SensorLight` 单例，需构建 `LightConfig` (Ver 1.0, Type 0)，通过 `getBlink().setOnTime(255)` / `setOffTime(255)` 控制开关。
3.  **投影检测**：涉及 `SensorCalibration` 的命令交互，由于是耗时操作，需要实现异步轮询机制来获取最终结果。
