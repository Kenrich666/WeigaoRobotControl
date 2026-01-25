# Dev B 服务层 API 使用指南

> **版本**: 1.1  
> **更新日期**: 2026-01-23  
> **适用对象**: Dev B（业务流程与UI交互）

---

## 一、快速开始

### 1.1 获取 ServiceManager

```java
// 在 Activity/Fragment/ViewModel 中
ServiceManager serviceManager = ServiceManager.getInstance();
```

### 1.2 获取服务实例

```java
INavigationService navService = serviceManager.getNavigationService();
IDoorService doorService = serviceManager.getDoorService();
IChargerService chargerService = serviceManager.getChargerService();
IRobotStateService stateService = serviceManager.getRobotStateService();
IRemoteCallService remoteCallService = serviceManager.getRemoteCallService();
ISecurityService securityService = serviceManager.getSecurityService();
ITimingService timingService = serviceManager.getTimingService();
IAudioService audioService = serviceManager.getAudioService();
```

---

## 二、回调模式

```java
public interface IResultCallback<T> {
    void onSuccess(T result);
    void onError(ApiError error);  // error.getCode(), error.getMessage()
}
```

---

## 三、核心服务 API

### 3.1 INavigationService - 导航服务

#### 常量
```java
ROUTE_POLICY_ADAPTIVE = 0;    // 自适应路线
ROUTE_POLICY_FIXED = 1;        // 固定路线
DIRECTION_FORWARD = 0;         // 前进
DIRECTION_BACKWARD = 1;        // 后退
DIRECTION_LEFT = 2;            // 左转
DIRECTION_RIGHT = 3;           // 右转
```

#### 导航控制
```java
void setTargets(List<Integer> targetIds, callback);     // 设置目标点
void setTargetNodes(List<NavigationNode> targets, callback);
void prepare(callback);                                  // 准备导航
void start(callback);                                    // 开始
void pause(callback);                                    // 暂停
void stop(callback);                                     // 停止
void pilotNext(callback);                                // 下一个目标点
void skipTo(int index, callback);                        // 跳到指定索引
```

#### 参数设置
```java
void setSpeed(int speed, callback);                      // 速度 (cm/s)
void setRoutePolicy(int policy, callback);               // 路线策略
void setBlockingTimeout(int timeout, callback);          // 阻挡超时 (ms)
void setRepeatCount(int count, callback);                // 循环次数
void setAutoRepeat(boolean enabled, callback);           // 自动循环
void setArrivalControlEnabled(boolean enabled, callback);
void cancelArrivalControl(callback);                     // 取消近似到达控制
```

#### 手动控制
```java
void manual(int direction, callback);
```

#### 状态查询
```java
void getRouteNodes(IResultCallback<List<NavigationNode>> callback);
void getCurrentNode(IResultCallback<NavigationNode> callback);
void getNextNode(IResultCallback<NavigationNode> callback);
void getCurrentPosition(IResultCallback<Integer> callback);
void isLastNode(IResultCallback<Boolean> callback);
void isLastRepeat(IResultCallback<Boolean> callback);
```

#### 回调监听
```java
void registerCallback(INavigationCallback callback);
void unregisterCallback(INavigationCallback callback);
void release();

// INavigationCallback 方法
void onStateChanged(int state, int scheduleStatus);
void onRoutePrepared(List<NavigationNode> nodes);
void onRouteNode(int index, NavigationNode node);
void onDistanceChanged(double distance);
void onError(int code, String message);
void onNavigationError(int code);
```

---

### 3.2 IDoorService - 舱门服务

```java
// 开关门
void openDoor(int doorId, boolean single, callback);
void closeDoor(int doorId, callback);
void openAllDoors(boolean single, callback);
void closeAllDoors(callback);

// 状态查询
void getDoorState(int doorId, IResultCallback<Integer> callback);
void getAllDoorStates(IResultCallback<int[]> callback);
void isAllDoorsClosed(IResultCallback<Boolean> callback);

// 舱门类型
void setDoorType(DoorType type, callback);
void getDoorType(IResultCallback<DoorType> callback);
void supportDoorTypeSetting(IResultCallback<Boolean> callback);
void getDoorVersion(IResultCallback<String> callback);

// 配置
void setFootSwitchEnabled(boolean enabled, callback);   // 脚踩开关门
void setAutoLeaveEnabled(boolean enabled, callback);    // 到达后自动离开

// 回调
void registerCallback(IDoorCallback callback);
void unregisterCallback(IDoorCallback callback);
void release();
```

---

### 3.3 IChargerService - 充电服务

```java
// 常量
CHARGE_ACTION_AUTO = 0;     // 自动充电
CHARGE_ACTION_MANUAL = 1;   // 手动充电
CHARGE_ACTION_ADAPTER = 2;  // 适配充电
CHARGE_ACTION_STOP = 3;     // 停止充电

// 充电控制
void startAutoCharge(callback);
void startManualCharge(callback);
void startAdapterCharge(callback);
void stopCharge(callback);
void performAction(int action, callback);

// 充电桩设置
void setChargePile(int pileId, callback);
void getChargePile(IResultCallback<Integer> callback);

// 状态查询
void getChargerInfo(IResultCallback<ChargerInfo> callback);
void getBatteryLevel(IResultCallback<Integer> callback);
void isCharging(IResultCallback<Boolean> callback);

// 回调
void registerCallback(IChargerCallback callback);
void unregisterCallback(IChargerCallback callback);
void release();
```

---

### 3.4 IRobotStateService - 机器人状态服务

```java
// 常量
WORK_MODE_STANDBY = 0;     // 待机
WORK_MODE_NAVIGATION = 1;  // 导航

// 状态查询
void getRobotState(IResultCallback<RobotState> callback);
void getBatteryLevel(IResultCallback<Integer> callback);
void getCurrentLocation(IResultCallback<RobotState.LocationInfo> callback);
void isScramButtonPressed(IResultCallback<Boolean> callback);
void getMotorStatus(IResultCallback<Integer> callback);

// 设备控制
void setWorkMode(int mode, callback);
void setEmergencyEnabled(boolean enabled, callback);
void setMotorEnabled(boolean enabled, callback);
void syncParams(boolean needReboot, callback);
void reboot(callback);
void performLocalization(callback);

// 设备信息
void getTotalOdometer(IResultCallback<Double> callback);
void getRobotIp(IResultCallback<String> callback);
void getRobotArmInfo(IResultCallback<String> callback);
void getRobotStm32Info(IResultCallback<String> callback);
void getDestinationList(IResultCallback<String> callback);
void getRobotProperties(IResultCallback<String> callback);

// 回调
void registerCallback(IStateCallback callback);
void unregisterCallback(IStateCallback callback);
void release();
```

---

### 3.5 IRemoteCallService - 远程呼叫服务

```java
// 常量
CALL_TYPE_LOOP = 1;      // 呼叫循环
CALL_TYPE_ARRIVE = 2;    // 呼叫到达
CALL_TYPE_RECOVERY = 3;  // 呼叫回收

// 启用控制
void setRemoteCallEnabled(boolean enabled, callback);
void isRemoteCallEnabled(IResultCallback<Boolean> callback);

// 呼叫操作
void handleRemoteCall(int targetPointId, int callType, callback);
void cancelRemoteCall(callback);
void isRemoteCallActive(IResultCallback<Boolean> callback);

// 停留设置
void setArrivalStayDurationEnabled(boolean enabled, callback);
void setArrivalStayDuration(int durationSeconds, callback);
void getArrivalStayDuration(IResultCallback<Integer> callback);
void isArrivalStayDurationEnabled(IResultCallback<Boolean> callback);

// 回调
void registerCallback(IRemoteCallCallback callback);
void unregisterCallback(IRemoteCallCallback callback);
void release();

// IRemoteCallCallback
void onRemoteCallReceived(String sourceType, int targetPointId, int callType);
void onRemoteCallResult(boolean success, String message);
void onRemoteCallCancelled();
```

---

### 3.6 ISecurityService - 安全锁定服务

```java
// 功能开关
void setSecurityLockEnabled(boolean enabled, callback);
void isSecurityLockEnabled(IResultCallback<Boolean> callback);

// 锁定状态
void isLocked(IResultCallback<Boolean> callback);
void lock(callback);
void unlock(String password, callback);

// 密码管理
void verifyPassword(String password, IResultCallback<Boolean> callback);
void setPassword(String oldPassword, String newPassword, callback);

// 舱门解锁（验证密码后开门）
void unlockDoor(int doorId, String password, callback);

void release();
```

---

### 3.7 ITimingService - 计时服务

```java
// 计时控制
void startTiming(String taskId, callback);
void pauseTiming(String taskId, callback);
void resumeTiming(String taskId, callback);
void stopTiming(String taskId, IResultCallback<TaskTiming> callback);

// 查询
void getCurrentTiming(String taskId, IResultCallback<TaskTiming> callback);
void getTimingHistory(String taskId, IResultCallback<List<TaskTiming>> callback);

// 配置
void clearTimingHistory(String taskId, callback);
void setAutoStopOnDoorOpen(boolean enabled, callback);

void release();
```

---

### 3.8 IAudioService - 音频服务（暂不可用）

```java
// 音乐设置
void setDeliveryMusic(String musicPath, callback);   // 配送背景音乐
void setLoopMusic(String musicPath, callback);       // 循环配送背景音乐

// 音量设置 (0-100)
void setVoiceVolume(int volume, callback);
void setDeliveryVolume(int volume, callback);
void getVoiceVolume(IResultCallback<Integer> callback);
void getDeliveryVolume(IResultCallback<Integer> callback);

// 播报频率
void setAnnouncementFrequency(int frequency, callback);
void setLoopAnnouncementFrequency(int frequency, callback);
void getAnnouncementFrequency(IResultCallback<Integer> callback);
void getLoopAnnouncementFrequency(IResultCallback<Integer> callback);

// 配置管理
void getAudioConfig(IResultCallback<AudioConfig> callback);
void updateAudioConfig(AudioConfig config, callback);

void release();
```

---

## 四、生命周期管理

### 4.1 资源释放

```java
// Application.onTerminate() 中调用
ServiceManager.getInstance().release();
```

### 4.2 回调注销（防止内存泄漏）

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    navService.unregisterCallback(myCallback);
    // 注销所有回调
}
```

---

## 五、典型业务流程

### 5.1 配送任务流程

```java
// 1. 开门放物
doorService.openDoor(1, true, callback);

// 2. 关门启动安全锁定
doorService.closeDoor(1, callback);
securityService.lock(callback);

// 3. 开始计时
timingService.startTiming("task-001", callback);

// 4. 导航到目的地
navService.setTargets(Arrays.asList(targetId), callback);
navService.start(callback);

// 5. 到达后输入密码开门
securityService.unlockDoor(1, password, callback);

// 6. 停止计时
timingService.stopTiming("task-001", callback);
```

### 5.2 低电量自动充电

```java
stateService.registerCallback(new IStateCallback() {
    @Override
    public void onBatteryLevelChanged(int level) {
        if (level < 20) {
            chargerService.startAutoCharge(null);
        }
    }
});
```

### 5.3 声音设置

```java
audioService.setVoiceVolume(80, callback);
audioService.setDeliveryMusic("/sdcard/music/bg.mp3", callback);
```

---

## 六、常见错误码

| 错误码 | 含义 |
|--------|------|
| -1 | 通用错误 |
| 200500+ | 舱门故障 |
| 参考 `SdkErrorCode` | SDK 层错误 |

---

## 七、附录：数据模型

| 模型类 | 说明 |
|--------|------|
| `NavigationNode` | 导航节点（id, name, x, y, phi） |
| `RobotState` | 机器人状态（电量、位置、工作模式等） |
| `ChargerInfo` | 充电信息（电量、事件、状态） |
| `DoorType` | 舱门类型枚举（FOUR, DOUBLE, THREE, THREE_REVERSE） |
| `TaskTiming` | 计时信息（startTime, endTime, elapsed） |
| `AudioConfig` | 音频配置 |
| `ApiError` | 错误对象（code, message） |
