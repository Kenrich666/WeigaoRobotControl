# Peanut SDKV1.3.4开发文档

# 一、概述



适配多种机型配置，开放机器人底层能力，封装功能组件，简化场景实现。

 

**
**

 

# 二、SDK集成指南

## 1. 简介



本文档主要用于指导开发者使用SDK开发运行在Peanut机器人上的Android应用程序。

 

·     集成之前请仔细阅读[准备工作](#25970172)，以便于你了解集成前所需要做的准备。

·     如果您想了解如何运行样例工程Peanut Sample，请查看[Sample运行](#91af3a32)。

·     如果您想了解怎样把SDK集成的自己应用中,请查看[SDK集成](#88ab823e)。

·     如果您想了解SDK的代码简单集成使用，请查看[SDK初始化](#fd5a4bcf)；如您想更详细的了解SDK中的方法使
 用，请查看《API列表》。

·     如果集成开发过程中遇到错误，请参照《常量字段值-错误码》。

## 2. 准备工作



·     准备一台Peanut机器人。

·     准备一根公对公双头USB线，建议2~3米。

·     联系技术支持，布置现场机器人运行环境，并从技术支持获取一份《机器人运行目标点位置表》。

·     联系销售申请离线鉴权信息。

## 3. 集成流程



开发者需要从平台申请开发相关资源，包括Android SDK、示例、开发文档、技术支持等。

## 3.1. 获取sdk



获取SDK发布文件，SDK文件通常是以peanut-sdk-x.x.x.zip命名的压缩文件，其中x.x.x代表SDK版本，以实际版本为准。解压后，可得到如下文件目录：

![img](file:///C:/Users/kenrich/AppData/Local/Temp/msohtmlclip1/01/clip_image002.gif)

文件目录说明如下：

·     doc是本文档所在的目录。

·     javadoc是。

·     libs是SDK库文件。

·     sample是包含SDK基本功能的演示样例程序，可直接用Android Studio打开并编译。

## 3.2. Sample运行



**3.2.1.** **步骤1：机器人开机**

 

打开机器人电源，等待机器人开机完毕。

 

**3.2.2.** **步骤2：准备机器人软件运行环境**

 

·     如果机器人启动完毕后直接进入机器人的默认应用程序，请联系技术支持人员，获知如何退出自启动的程序，并取消机器人默认应用程序的开机启动与进程守护。

·     检查并强行停止默认应用程序。具体步骤为，回到Android桌面，点击【设置】、【应用】，选择默认的Peanut程序，点击【强行停止】。这一步骤是为了防止默认应用程序与Peanut Sample程序同时与机器人内部通讯造成通讯错误。

 

**3.2.3.** **步骤3：准备机器人硬件调试环境**

 

·     打开机器人腰部后半部分腰带，打开方法为按住两侧卡扣并弹出，用力掰下中部卡扣，取下腰带后可看到右侧USB接口。此USB接口可以设置为HOST模式与DEVICE模式。设置为HOST模式时，可使用U盘。设置为DEVICE模式时，可使用ADB调试Android应用程序。

·     设置USB为DEVICE模式。具体步骤为，回到Android桌面，点出【设置】、【开发者选项】、【USB模式切换】，选中【Device模式】。

·     使用双头USB线连接装电脑与机器人腰部的USB接口（不同机型位置不同，可咨询技术支持）。

![img](file:///C:/Users/kenrich/AppData/Local/Temp/msohtmlclip1/01/clip_image004.jpg)

**3.2.4.** **步骤4：连接PC端调试环境并运行**

 

·     打开Android Studio。Sample程序在Android Studio开发并测试通过。

·     打开SDK文件目录下的Sample工程。

·     编译并运行。Sample程序会运行在机器人上。

 

**3.3. SDK****集成**



**3.3.1.** **添加库文件**

 

将SDK目录中libs目录下的aar文件拷贝至Android工程module的libs目录

 

**3.3.2.** **配置build.gradle**

 

·     android 配置packagingOptions

 

packagingOptions {

  pickFirst 'about.html'

  pickFirst 'edl-v10.html'

  pickFirst 'epl-v10.html'

  pickFirst 'notice.html'

  pickFirst 'META-INF/legal/LICENSE'

  pickFirst 'META-INF/legal/NOTICE.md'

  pickFirst 'META-INF/legal/3rd-party/cc0-legalcode.html'

  pickFirst 'META-INF/legal/3rd-party/BSD-3-Clause-LICENSE.txt'

  pickFirst 'META-INF/legal/3rd-party/APACHE-LICENSE-2.0.txt'

  pickFirst 'META-INF/legal/3rd-party/MIT-license.html'

  pickFirst 'META-INF/legal/3rd-party/CDDL+GPL-1.1.txt'

}

 

·     确保对libs/peanut-sdk-xxx.aar的依赖：

 

dependencies {

  implementation fileTree(dir: 'libs', include: ['*.aar'])

implementation 'com.alibaba:fastjson:1.2.68'

implementation 'com.google.code.gson:gson:2.8.6'

implementation 'org.slf4j:slf4j-api:1.7.25'

  ...

}

 

**3.3.3.** **配置AndroidManifest.xml**

 

配置SDK所需的权限

 

<uses-permission android:name="android.permission.INTERNET" />

<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<uses-permission android:name="android.permission.READ_PHONE_STATE" />

 

至此，开发者可以在自己的应用程序中调用Peanut SDK提供的功能。

 

**3.4. SDK****初始化**



**3.4.1.** **启动**

PeanutSDK.getInstance().init(this.getApplicationContext(), mErrorListener);

**3.4.2.** **释放**

 

应用退出时，需要调用SDK的释放，避免资源占用。

 

PeanutSDK.getInstance().release();

 

**3.4.3.** **详细配置**

 

PeanutConfig.getConfig() 

  //设置连接协议

  .setLinkType(PeanutConstants.LinkType.COM_COAP);

  //设置连接主板串口端口

  .setLinkCOM(PeanutConstants.COM1);

  //设置连接服务IP

  .setLinkIP(PeanutConstants.LOCAL_LINK_PROXY);

  //设置连接服务端口

  .setLinkPort(5683);

  //设置灯板串口端口

  .setEmotionLinkCOM(PeanutConstants.COM2);

  //设置门控板串口端口

  .setDoorLinkCOM(PeanutConstants.COM2);

  //设置门数量

  .setDoorNum(1);

  //设置连接超时

  .setConnectionTimeout(PeanutConstants.COAP_TIME_OUT);

  //是否开启日志

  .enableLog(true)

  //设置日志级别

.setLogLevel(Log.DEBUG)

//设置离线鉴权的AppId

.setAppId("xx")

//设置离线鉴权的Secret

.setSecret("xx");

PeanutSDK.getInstance().init(this.getApplicationContext(), config, mErrorListener);

 

//SDK初始化后回调的错误码，0代表成功，其他故障情况（参考常量字段值-错误码）

private PeanutSDK.InitListener mErrorListener = new PeanutSDK.ErrorListener() {

 

  @Override

  public void onInit(int errorCode) {

​    Log.d(TAG, "onInit:" + errorCode);

  }

};

 

**3.5.** **参数说明**



**3.5.1** **通信协议**

 

RobotSDKConfig可以设置通信协议、机器IP、端口、串口等。

 

public enum LinkType {

  DEFAULT,

  COM,

  COM_COAP,

  COAP,

  HTTP

}

 

| **协议值** | **协议描述**       |
| ---------- | ------------------ |
| DEFAULT    | 保留字段           |
| COM        | 串口协议           |
| COM_COAP   | Coap代理，串口协议 |
| COAP       | Coap协议           |
| HTTP       | Http协议           |

 

**3.5.2.** **串口端口**

 public static final String COM1 = "/dev/ttyS1";

 public static final String COM2 = "/dev/ttyS2";

**3.5.3.** **通信服务地址**

 public static final String LOCAL_LINK_PROXY = "127.0.0.1";

 public static final String REMOTE_LINK_PROXY = "192.168.64.20";

| **服务IP**        | **适用条件** |
| ----------------- | ------------ |
| LOCAL_LINK_PROXY  | 立体视觉定位 |
| REMOTE_LINK_PROXY | 激光雷达定位 |

## 4. 帮助



**遇到集成问题可先参考最新版本的文档及Sample示例。**

**遇到环境配置无法解决请先与技术支持沟通确认部署是否正确。**

**遇到功能使用问题请按照问题反馈模板联系技术。**

 

# 三、组件说明

## 1.设备运行时组件

### 1.1. 使用介绍

组件用于设备连接，且在连接后，进行自检、设备各部件状态同步、参数同步、设备信息获取及同步，过滤掉不合理数据及无变化的订阅上报。

public class RuntimeInfo {

 //工作模式

 private int workMode;

 

 //同步状态

 private int syncStatus;

 

 //电量

 private int power;

  

 //总里程

 private Double totalOdo;

 

 //急停使能

 private boolean emergencyEnable;

 

 //急停开关

 private boolean emergencyOpen;

 

 //电机状态

 private int motorStatus;

 

 //算法板信息

 private String robotArmInfo;

 

 //运动板信息

 private String robotStm32Info;

 

 //robot IP地址

 private String robotIp;

 

 //参数配置

 private String robotProperties;

 

 //所有目标点

 private String destList;

}

  PeanutRuntime.getInstance().start(new PeanutRuntime.Listener() {

   @Override

   public void onEvent(int event, Object obj) {

​    Log.d(TAG, "onEvent:" + event + ", content: " + obj);

   }

 

   @Override

   public void onHealth(Object content) {

​    Log.d(TAG, "onHealth:" + content);

   }

 

   @Override

   public void onHeartbeat(Object content) {

​    Log.d(TAG, "onHeartbeat:" + content);

   }

  });

### 1.2. 功能API

| **限定符和类型**     | **方法和说明**                                               | **关联常量**    |
| -------------------- | ------------------------------------------------------------ | --------------- |
| static PeanutRuntime | getInstance()  获取单例。                                    |                 |
| RuntimeInfo          | getRuntimeInfo()  获取运行时信息，包含工作模式、电量、里程数、版本信息、IP、机型配置、急停按钮状态、电机状态等。 |                 |
| void                 | start()  启动，建立通信连接，开始数据订阅、配置同步、状态自检等。 |                 |
| void                 | registerListener(Listener listener)  注册回调，可以同步事件及状态信息。 |                 |
| void                 | removeListener(Listener  listener)  注销回调。               |                 |
| void                 | setEmergencyEnable(final  boolean enable)  设置急停按钮是否启用。 |                 |
| void                 | setIP(String ip, int port)  设置服务地址，用于多机协作场景。 |                 |
| void                 | setWorkMode(final int mode)  设置工作模式。                  | 常量 - 工作模式 |
| void                 | setTime(final long timestamp)  设置robot 时间                |                 |
| void                 | syncParams2Robot(boolean  needReboot)  同步机型配置，是否重启生效。 |                 |
| void                 | location()  开机定位，用于SLAM定位。                         |                 |
| void                 | getPath()  获取位置路线信息                                  |                 |
| void                 | destory()  销毁。                                            |                 |

### 1.3. Listener

| **限定符和类型** | **方法和说明**                                             | **关联常量**    |
| ---------------- | ---------------------------------------------------------- | --------------- |
| void             | onEvent(int event, Object obj)  返回事件码及相关信息数据。 | 事件 - 设备运行 |
| void             | onHealth(Object content)  健康状态回调。                   |                 |
| void             | onHeartbeat(Object content)  心跳状态回调。                |                 |

 public interface Listener {

  void onEvent(int event, Object obj);

 

  void onHealth(Object content);

 

  void onHeartbeat(Object content);

 }

 



 

## 2. 导航组件



### 2.1. 使用介绍

组件用于机器人自主导航控制。可以设置路线规划策略、到达策略、目的地列表、循环次数等，开始导航后可以控制导航的暂停、恢复、停止，导航过程中可以同步上报导航状态及导航信息。

### 2.2. 功能API



#### 2.2.1. PeanutNavigation.Builder类

 

| **限定符和类型** | **方法和说明**                                               | **备注**            |
| ---------------- | ------------------------------------------------------------ | ------------------- |
| pulic            | Builder()  构造方法。                                        |                     |
| Builder          | setTargets(Integer... targets)  设置目的地列表，整型。       |                     |
| Builder          | enableDefaultArrival(boolean enable)  启用默认到达策略。     |                     |
| Builder          | setRoutePolicy(int policy)  设置路线规划策略。               | 常量 - 路线规划策略 |
| Builder          | enableAutoRepeat(boolean autoRepeat)  启用自动循环。         |                     |
| Builder          | setRepeatCount(int count)  设置循环次数。                    | 目标点导航任务循环  |
| Builder          | setArrivalControl(ArrivalControl arrivalControl)  设置自定义到达策略。 |                     |
| Builder          | setArrivalControl(boolean enable, float marginScope, int blockDelay, int  scopeDelay)  设置默认到达策略。 |                     |
| Builder          | setRouteSelector(RouteSelector routeSelector)  设置自定义路线规划策略 |                     |
| Builder          | setListener(Navigation.Listener listener)  设置回调          |                     |
| Builder          | setBlockingTimeOut(int timeout)  设置阻挡超时，超时后默认到达，切换到下一个任务，单位ms | 仅适用激光定位      |
| PeanutNavigation | build()  创建方法                                            |                     |

 

#### 2.2.2. PeanutNavigation API

 

| **限定符和类型** | **方法和说明**                                               | **关联常量**        |
| ---------------- | ------------------------------------------------------------ | ------------------- |
| pulic            | PeanutNavigation(ArrivalControl arrivalControl, RouteLine routeLine,  Navigation.Listener listener, int blockingTimeout)  构造方法。 |                     |
| void             | setTargets(Integer... targets)  设置目标点列表，参数为目标点ID列表，ID来自地图。 |                     |
| void             | setTargets(List<RouteNode> targets)  设置目标点列表，参数为RouteNode列表。 |                     |
| void             | prepare()  准备，路线规划。                                  |                     |
| void             | setPilotWhenReady(boolean pilotWhenReady)  自主导航，当准备好后，控制开始、暂停。 |                     |
| void             | setSpeed(int speed)  设置导航运动速度，单位cm/s。            |                     |
| void             | pilotNext()  切换下一个目标点并开始导航。                    |                     |
| void             | stop()  停止导航。                                           |                     |
| void             | release()  释放资源。                                        |                     |
| void             | manual(int direction)  手动导航。                            | 常量 - 手动导航方向 |
| RouteNode[]      | getRouteNodes()  获取当前目标点列表。                        |                     |
| RouteNode        | getCurrentNode()  获取当前导航目标点。                       |                     |
| RouteNode        | getNextNode()  获取下一个导航目标点。                        |                     |
| boolean          | isLastNode()  是否最后一个导航目标点。                       |                     |
| int              | getCurrentPosition()  获取当前导航目标点的index。            |                     |
| void             | skipTo(@NonNull int node)  跳刀指定下标的目标点处。          |                     |
| void             | cancelArrivalControl()  当前点位导航取消近似到达。           |                     |
| void             | setArrivalControlEnable(boolean enable)  设置近似到达是否启用。 |                     |
| boolean          | isLastRepeat()  是否为最后一次循环                           |                     |
| void             | addListener(Navigation.Listener listener)  添加回调          |                     |
| void             | removeListerner(Navigation.Listener listener)  移除回调      |                     |

### 2.3. 自定义策略



#### 2.3.1. 到达策略

开发者根据自身需求自行实现ArrivalControl接口。到达判定后调用Listener的onArrived()方法。

public interface ArrivalControl {

 public interface Listener {

  void onArrived();

 }

 

 public static final float DEFAULT_MARGIN_SCOPE = 1.0f;

 public static final int DEFAULT_BLOCK_DELAY = 5 * 1000;

 public static final int DEFAULT_SCOPE_DELAY = 10 * 1000;

 

 public void addListener(Listener listener);

 

 public void removeListener(Listener listener);

 

 public void enable(boolean enable);

 

 public void notifyDistanceChanged(float distance);

 

 public void notifyBlocked();

 

 public void release();

}

#### 2.3.2. 路线规划

开发者根据自身需求自行实现路线规划。

public interface RouteSelector {

 interface Output {

  void adaptiveRoute(RouteNode... routeNodes);

 

  void fixedRoute(RouteNode... routeNodes);

 

  void onError(int errorCode);

 }

 

 public static final int DEFAULT_LIMIT = 5;

 

 public void selectRoute(Output output, RouteNode... routeNodes);

}

**单次路径规划的最大限制为5个。**

### 2.4. Listener



 

| **限定符和类型** | **方法和说明**                                               | **关联常量**  |
| ---------------- | ------------------------------------------------------------ | ------------- |
| void             | onStateChanged(int state ，int schedule)  返回导航状态码。  参数：  state：导航状态。  schedule：是否调度中。 | 常量-导航状态 |
| void             | onRouteNode(int index, RouteNode routeNode)  目标点更新。    |               |
| void             | onRoutePrepared(RouteNode... routeNodes)  路线准备就绪。     |               |
| void             | onDistanceChanged(float  distance)  当前目标点所剩距离。     |               |
| void             | onError(int code)  故障。                                    | 错误码        |

 public interface Navigation {

 

 final class Factory {

  private Factory() {

  }

 

  public static Navigation newInstance(ArrivalControl arrivalControl) {

   return NavigationImpl.getInstance(arrivalControl);

  }

 }

 

 public interface Listener {

  void onStateChanged(int state, int schedule);

 

  void onRouteNode(int index, RouteNode routeNode);

 

  void onRoutePrepared(RouteNode... routeNodes);

 

  void onDistanceChanged(float distance);

 

  void onError(int code);

 }

 

 public static final int STATE_IDLE = 0;

 public static final int STATE_PREPARED = 1;

 public static final int STATE_RUNNING = 2;

 public static final int STATE_DESTINATION = 3;

 public static final int STATE_PAUSED = 4;

 public static final int STATE_COLLISION = 5;

 public static final int STATE_BLOCKED = 6;

 public static final int STATE_STOPPED = 7;

 public static final int STATE_ERROR = 8;

 public static final int STATE_BLOCKING = 9;

 public static final int STATE_END = 10;

 

 public static final int FORWARD = 1;

 public static final int BACKWARD = 2;

 public static final int LEFT = 3;

 public static final int RIGHT = 4;

 

 

 public void addListener(Listener listener);

 

 public void removeListener(Listener listener);

 

 public void prepare(RouteLine routeLine);

 

 public void setPilotWhenReady(boolean pilotWhenReady);

 

 public void pilotNext();

 

 public void stop();

 

 public void manual(int direction);

 

 public void setSpeed(int speed);

 

 public void setBlockTimeout(int timeout);

 

 public RouteNode[] getRouteNodes();

 

 public RouteNode getCurrentNode();

 

 public RouteNode getNextNode();

 

 public int getCurrentPosition();

 

 public void release();

 

 public void skipTo(int index);

 

 public void cancelArriveControl();

 

 void resetNewTargets();

 

 boolean isLastRepeat();

 

 void arrivalControlEnable(boolean enable);

}

 

 



 

## 3. 电源组件



### 3.1. 使用介绍

组件用于机器人充电控制和监听电量。可以设置机器人 自动充电，手动充电，适配器充电等模式。机器人会同步上报充电事件和电量变化。



PeanutCharger mPeanutCharger;

 

//充电回调

 Charger.Listener listener=new Charger.Listener() {

  @Override

  public void onChargerInfoChanged(int event, ChargerInfo chargerInfo) {

   PrintLnLog.d(ChargerDemo2.this, tvApiLog, svApiLog, sb, "event = " + event+

​       " Power = " + chargerInfo.getPower()+" ChargeEvent = " + chargerInfo.getEvent());

  }

 

  @Override

  public void onChargerStatusChanged(int status) {

   PrintLnLog.d(ChargerDemo2.this, tvApiLog, svApiLog, sb, "status = " + status);

  }

 

  @Override

  public void onError(int errorCode) {

   PrintLnLog.d(ChargerDemo2.this, tvApiLog, svApiLog, sb, "errorCode = " + errorCode);

  }

 };

 

 

 

 //初始化

 mPeanutCharger = new PeanutCharger.Builder()

   .setListener(listener)

   .build();

 mPeanutCharger.execute();

 

//充电指令控制

 case R.id.btn_manual_charge:

  mPeanutCharger.performAction(PeanutCharger.CHARGE_ACTION_MANUAL);

  break;

 case R.id.btn_auto_charge:

  mPeanutCharger.performAction(PeanutCharger.CHARGE_ACTION_AUTO);

  break;

 case R.id.btn_stop_charge:

  mPeanutCharger.performAction(PeanutCharger.CHARGE_ACTION_STOP);

  break;

 case R.id.btn_adapter_charge:

  mPeanutCharger.performAction(PeanutCharger.CHARGE_ACTION_ADAPTER);

  break;

 

 

//释放

//正常情况不需要释放，内置订阅电量，释放后无法获取电量

if (mPeanutCharger != null) {

  mPeanutCharger.cancel();

}

 

### 3.2. 功能API 



#### 3.2.1. Builder类

| **限定符和类型** | **方法和说明**                                     | **关联常量** |
| ---------------- | -------------------------------------------------- | ------------ |
| public           | Builder()  构造方法。                              |              |
| Builder          | setPile(int pile)  设置充电桩 Id。                 |              |
| Builder          | setListener(Charger.Listener listener)  设置回调。 |              |
| PeanutCharger    | build()  创建方法。                                |              |

#### 3.2.2 API

| **限定符和类型** | 方法和说明                                                   | **关联常量**        |
| ---------------- | ------------------------------------------------------------ | ------------------- |
| public           | PeanutCharger(int pile, Charger.Listener listener,,int maxRetry,long  timeRetry)  构造方法。 |                     |
| void             | setPile(int pile)  设置充电桩。                              |                     |
| void             | execute()  开始执行。                                        |                     |
| void             | performAction(int action)  控制充电。                        | 常量 - 电源充电指令 |
| void             | release()  释放资源。                                        |                     |
| int              | getPile()  获取电桩ID。                                      |                     |

### 3.3. Listener



| **限定符和类型** | 方法和说明                                                   | **关联常量**        |
| ---------------- | ------------------------------------------------------------ | ------------------- |
| void             | onChargerInfoChanged(int event, ChargerInfo chargerInfo)  返回电源上报事件和电量信息。 | 事件 - 电源充电事件 |
| void             | void onChargerStatusChanged(int status);  充电状态发生改变时候返回当前状态。 | 常量 - 电源状态     |
| void             | onError(int errorCode)  故障。                               | 错误码              |

 

 public interface Listener {

  void onChargerInfoChanged(int event, ChargerInfo chargerInfo);

 

  void onChargerStatusChanged(int status);

 

  void onError(int errorCode);

 }

 

public class ChargerInfo implements Serializable {

 int power;

 int event;

 

 public int getPower() {

  return power;

 }

 

 public int getEvent() {

  return event;

 }

 

 public void setPower(int power) {

  this.power = power;

 }

 

 public void setEvent(int event) {

  this.event = event;

 }

 

 

 @Override

 public String toString() {

  return "ChargerInfo{" +

​      "power=" + power +

​      ", event=" + event +

​      '}';

 }

}

 

## 4. 音频组件



### 4.1. 使用介绍

组件通过T8设备麦克风硬件获取音频流数据。

// Step1 获取音频组件

AudioComponent component = PeanutSDK.getInstance().audio(); 

// Step2 初始化音频组件

Component.initAudio(mAudioListener);

 

private OnAudioListener mAudioListener = new OnAudioListener() {

  @Override

  public void onSuccess() {

   // 初始化成功，可以做一些业务逻辑处理

  }

 

  @Override

  public void onError(int errorCode) {

   // 初始化失败: -100 麦克风硬件不存在, -200 麦克风硬件打开失败

  }

 

  @Override

  public void onAudioData(byte[] bytes, int len) {

   // 音频数据回调

  }

 

  @Override

  public void onHeartbeat(int state) {}

};

### 4.2. 功能Api

| **限定符和类型** | **方法和说明**                                               | **关联常量** |
| ---------------- | ------------------------------------------------------------ | ------------ |
| Int              | audioStart()   开始音频数据上报，0执行成功；  通过OnAudioListener.onAudioData()方法往上报。 |              |
| Int              | audioStop()   停止音频数据上报，0 执行成功。                 |              |
| boolean          | isInitialized()   是否已初始化。                             |              |
| void             | setAudioGainEnable(boolean enable)  音频增益开关，默认关。   |              |
| void             | setAudioGainValue(float value)  音频增益值设置。             |              |
| byte[]           | gainAudioData(byte[] data)  音频增益数据换算。               |              |

### 4.3. Listener

| **限定符和类型** | **方法和说明**                                          | **关联常量**                                     |
| ---------------- | ------------------------------------------------------- | ------------------------------------------------ |
| void             | onSuccess()  麦克风拾音初始化成功。                     |                                                  |
| void             | onError(int errorCode)  麦克风拾音初始化失败。          | ERROR_DEVICE_NOT_FOUND  ERROR_DEVICE_OPEN_FAILED |
| void             | onAudioData(byte[] bytes, int  len)  麦克风音频数据回调 |                                                  |
| void             | onHeartbeat(int state)  麦克风拾音心跳                  |                                                  |

 // 麦克风音频回调接口 

public interface OnAudioListener {

​     // 麦克风硬件不存在

​     int ERROR_DEVICE_NOT_FOUND = -100;

​     // 麦克风硬件打开失败

​     int ERROR_DEVICE_OPEN_FAILED = -200;

 

​     void onSuccess();

​     void onError(int errorCode);

​     void onAudioData(byte[] bytes, int len);

​     void onHeartbeat(int state);

}

 

## 5.舱门组件



### 5.1基本功能介绍

#### 5.1.1 使用前初始化

PeanutDoor.getInstance().init(getApplicationContext());

 

//使用结束需释放资源：

PeanutDoor.getInstance().release() ;

#### 5.1.2 监听状态

public interface DoorListener {

//返回某ID舱门错误 doorId为空则为非指定门的错误

 void onFault(Faults type, int doorId); 

//监听特定ID舱门的开、关状态

 void onStateChange(int floorId, int state);

//监听舱门类型改变 

 void onTypeChange(GatingType gatingType);

//监听舱门设置是否成功

 void onTypeSetting(boolean success);

//错误码

 void onError(int errorCode);

}

**
 5.1.3** 舱门控制&舱门类型说明

基于目前结构设计原因，暂定四种类型： 四舱，双舱 ，T型舱，倒T型舱

 

mPeanutDoor.setDoorType(Door.SET_TYPE_FOUR, TAG, this);

*常量 - 舱门设置类型*

| **数值**                | 说明           |
| ----------------------- | -------------- |
| SET_TYPE_FOUR(101)      | 设置为四舱     |
| SET_TYPE_DOUBLE(102)    | 设置为双舱     |
| SET_TYPE_THREE(103)     | 设置为T舱      |
| SET_TYPE_THREE_REV(104) | 设置为倒T舱    |
| SET_TYPE_AUTO(105)      | 设置为自动识别 |

舱门开关控制：

 

//single true ：单独打开指定舱门（其他全关闭）  false ：互不影响

 public void openDoor(int doorId, boolean single) {

  mPeanutDoor.openDoor(doorId, single);

 }

 

 //关闭指定舱门

 public void closeDoor(int doorId) {

  mPeanutDoor.closeDoor(doorId);

 }

**
 5.1.4** **其他功能**

 

 // 关闭所有舱门

 public void closeAllDoor() {

  mPeanutDoor.closeAllDoor();

 }

  // 判断所有舱门是否都关闭

 public boolean isAllDoorClose() {

  return mPeanutDoor.isAllDoorClose();

 }

 //获取舱门固件版本号

 public String getDoorVersion() {

  return mPeanutDoor.getDoorVersion();

 }

 //判断是否支持手动更改舱门类型功能

 public boolean supportDoorTypeSetting() {

  return mPeanutDoor.supportDoorTypeSetting();

 }

### 5.2 API汇总

 

| **api**                                                      | 说明                             |
| ------------------------------------------------------------ | -------------------------------- |
| void setDoorListerner(String tag, DoorListener gatingFaultListener) | 设置监听                         |
| void removeFloorListener(String tag);                        | 移除监听                         |
| int getDoorType();                                           | 获取舱门类型                     |
| GatingState getDoorState(int  doorId);                       | 获取指定id舱门的状态             |
| void openDoor(int doorId, boolean single);                   | 控制开门                         |
| void closeDoor(int doorId);                                  | 控制关门                         |
| void closeAllDoor();                                         | 控制关闭所有舱门                 |
| boolean isAllDoorClose();                                    | 判断多油门是否都关闭             |
| void setDoorType(int gatingType, String tag, DoorListener doorListener); | 设置舱门类型                     |
| String getDoorVersion();                                     | 获取舱门控制版本                 |
| boolean supportDoorTypeSetting();                            | 判断硬件是否支持舱门类型更改功能 |
| void release();                                              | 释放资源                         |

***补充说明：\***

gatingType ：

| GatingType(enum) | TypeId (int) | 类型  |
| ---------------- | ------------ | ----- |
| Four             | 0            | 四舱  |
| DOUBLE           | 1            | 双舱  |
| THREE            | 2            | T字型 |
| THREE_REVERSE    | 3            | 倒T型 |

***doorId\******：\***

***根据从左到右，从上到下的顺序规则，从1开始依次递增\***

![img](file:///C:/Users/kenrich/AppData/Local/Temp/msohtmlclip1/01/clip_image006.jpg)

### 5.3注意事项

使用设置舱门类型功能前需判断舱门固件是否支持舱门类型设置功能，否则可能会出现设置不成功或没反应的情况。

可通过supportDoorTypeSetting() 方法来判断。

 

## 6.Ros地图文件操作组件



### 1.1. 使用介绍

组件用于与机器人Ros 系统中地图文件导入导出，将A机器地图导出后，再用U盘将导出的PeanutDB.zip 文件复制至B机器Sdcard 目录下，进行导入操作。

```
//导入地图
MapManager.getInstance().onImportToRos();
//导出地图
MapManager.getInstance().onExportToAndroid();
 
```

### 1.2. 功能API 



### 1.2.1. Builder类

| 限定符和类型 | 方法和说明                                                   |
| ------------ | ------------------------------------------------------------ |
| public       | addListen(MapListen  listen)添加回调                         |
| public       | onImportToRos()  导入地图至 Ros //默认读取根目录文件         |
| public       | onExportToAndroid()  导出地图至 android Sdcard //默认根目录  |
| public       | onImportToRos(String  filePath) 导入地图至 Ros //指定目录文件 |
| public       | onExportToAndroid(String  savePath) 导出地图 //指定导出文件目录 |

### 1.3. Listener



| 限定符和类型 | 方法和说明          | 关联常量 |
| ------------ | ------------------- | -------- |
| void         | onResult(int  code) |          |

### 1.3.1 回调状态

| 状态码 | 说明         |
| ------ | ------------ |
| 1001   | 导入地图成功 |
| 1002   | 导出地图成功 |
| 2001   | 导入地图失败 |
| 2002   | 导出地图失败 |

 

## 6.T3 舱门控制



### 1.1. 使用介绍

控制T3 机器人舱门开关操作。

```
//打开指定舱门
SensorDoor.getInstance().setDoorSwitch(doorID,true)
//关闭指定舱门
SensorDoor.getInstance().setDoorSwitch(doorID,false)
```

### 1.2. 功能API 



### 1.2.1. 舱门控制

| 限定符和类型 | 方法和说明                                            |
| ------------ | ----------------------------------------------------- |
| public       | SensorDoor.getInstance().setDoorSwitch(doorID,isOpen) |

### 1.2.2 舱门ID

| ID                     |      |
| ---------------------- | ---- |
| ProtoDev.SENSOR_DOOR_1 | 22   |
| ProtoDev.SENSOR_DOOR_2 | 23   |

### 1.2.3 回调状态

| 舱门状态码 | 说明             |
| ---------- | ---------------- |
| -1         | 打开状态         |
| 0          | 关闭状态         |
| 1          | 打开或关闭执行中 |

 

 

 

# 四. 通用接口定义

## 4.1. 回调方法

 

·     IDataCallback是数据接口。

public interface IDataCallback {

 

 void success(String result);

 

 void error(ApiError error);

}

·     IProgressCallback接口表示处理文件的时候可以使用这个返回进度。

public interface IProgressCallback extends IDataCallback {

 

 void readyToSend(Request request);

 

 void progress(int percent);

}



## 4.2. 接口数据定义

#### 4.2.1. 返回成功

**success****方法中的"result"以json字符串格式返回数据**，具体如下：

{ 

  “status”: 0, 

  “code”: 0, 

  “msg”: "success",

  “data”: {} 

}

| status | code | msg     | data | 备注                       |
| ------ | ---- | ------- | ---- | -------------------------- |
| 0      | 0    | success | json | 请求成功，以json的返回数据 |
| 1      | -1   | error   | json | 请求错误，以json返回数据。 |

#### 4.2.2. 返回失败

**error****方法中的"ApiError"格式**

| code | msg                | 备注       |
| ---- | ------------------ | ---------- |
| 19   | Method Not Allowed | 方法不支持 |
| 21   | Empty Data         | 空数据     |



## 4.3. 接口调用方式

PeanutSDK.getInstance().xxx()



## 4.4. 电机接口

获取电机的状态信息及电机锁使能控制。

PeanutSDK.getInstance().motor().xxx()

### 4.4.1. 获取电机状态

 

/**

 \* Get status of motor.

 *

 \* @param callBack the callback after request

 */

public void getStatus(IRobotCallBack callBack)

 

返回结果

{

  "data": {

​    "desc": "MOTOR_STATUS_LOCKED",

​    "status": 255

  },

  "code": 0,

  "msg": "success",

  "status": 0

}

 

### 4.4.2. 设置电机是否启用

 

/**

 \* Set motor enable.

 *

 \* @param callBack the callback after request

 \* @param enable  on or off

 */

public void enable(IRobotCallBack callBack, int enable)

 

返回结果

{

  "code": 0,

  "msg": "success",

  "status": 0

}

 

### 4.4.3. 设置HRC是否启用

 

/**

 \* Set HRC enable.

 *

 \* @param callBack the callback after request

 \* @param enable  on or off

 */

public void hrc(IRobotCallBack callBack, boolean enable)

 

返回结果

{

  "code": 0,

  "msg": "success",

  "status": 0

}

 

### 4.4.4. 获取电机健康状态

 

/**

 \* Get health status of motor.

 *

 \* @param callBack the callback after request

 */

public void getHealth(IRobotCallBack callBack)

 

返回结果

{

  "data": [

​    {

​      "code": 0,

​      "desc": "",

​      "position": 0

​    }

  ],

  "code": 0,

  "msg": "success",

  "status": 0

}



**4.5.** **设备信息接口**

获取机器人硬件设备信息。

PeanutSDK.getInstance().device().xxx()

**4.5.1.** **获取设备列表**

 

/**

 \* Get the device list.

 *

 \* @param callBack the callback after request

 */

public void getList(IDataListener callBack)

 

返回结果

{

  "data": [

​    {

​      "id": 1,

​      "name": "robot-arm",

​      "pid": 4

​    },

​    {

​      "id": 2,

​      "name": "robot-stm32",

​      "pid": 0

​    },

​    {

​      "id": 3,

​      "name": "motor",

​      "pid": 1736260526

​    },

​    {

​      "id": 6,

​      "name": "power",

​      "pid": 1134105942

​    }

  ],

  "code": 0,

  "msg": "success",

  "status": 0

}

 

**4.5.2.** **获取设备版本信息**

 

/**

 \* Get the detail information of board.

 *

 \* @param callBack the callback after request

 \* @param board   the board

 */

public void getBoardInfo(IRobotCallBack callBack, String board)

 

返回结果

{

  "code": 0,

  "data": {

​    "device": "robot-arm",

​    "sn": "",

​    "version": {

​      "sw": {

​        "number": "1.0.14beta",

​        "base": "linux-armv7-robot-v1.0.14-gf4fbb90-release.zip",

​        "md5": "9205b9033196ba81"

​      },

​      "hw": {

​        "number": "1.0.0"

​      },

​      "env": {

​        "number": "Linux firefly 3.10.0",

​        "base": "linux-rk3288-os-update-20180410.img",

​        "md5": "9d40e1329780d62e"

​      }

​    }

  },

  "msg": "success",

  "status": 0

}

 

**4.5.3.** **重启设备**

 

/**

 \* Reboot the robot.

 *

 \* @param callBack the callback after request

 */

public void reboot(IRobotCallBack callBack)

 

返回结果

{

  "code": 0,

  "msg": "success",

  "status": 0

}

 

**4.5.4.** **获取配置属性**

 

/**

 \* Get the configuration of device.

 *

 \* @param callBack the callback after request

 */

public void getConfig(IRobotCallBack callBack)

 

返回结果

{

  "data": {

​    "params": xxx  //map<string, string>

  },

  "code": 0,

  "msg": "success",

  "status": 0

}

 

**4.5.5.** **更新配置属性**

说明：此配置下发后Ros 会存文件保存，当Ros 升级或替换后配置会丢失，需要重新下发，也可多次下发。

 

/**

 \* Set the configuration with params.

 *

 \* @param callBack the callback after request

 \* @param params  the param

 */

public void updateConfigCompat(IRobotCallBack callBack, String params)

使用方法：

​     

 

PeanutRuntime.getInstance().getPath();

​    Map<String, String> params = new HashMap<>();

​    params.put("schedulePathWaitTime","20");//调度等待时长 单位（秒）

​    String jsonString = new JSONObject(params).toString();

PeanutSDK.getInstance().device().updateConfigCompat(callback,jsonString)

返回结果：

{

  "data": {

​    "schedulePathWaitTime": "20"，

  },

  "code": 0,

  "msg": "success",

  "status": 0

}

 

**4.5.6.** **设置急停按钮是否启用**

 

 /**

  \* Turn on W3 fan

  \* @param callBack

  \* @param fanId 0,1,2,3

  */

 public void openFan(IDataCallback callBack, int fanId)

 

 /**

  \* Turn off W3 fan

  \* @param callBack

  \* @param fanId 0,1,2,3

  */

 public void closeFan(IDataCallback callBack, int fanId)

 

PeanutSDK.getInstance().door().openFan(null,DOOR_ID_2);//打开指定风扇

PeanutSDK.getInstance().door().closeFan(null,DOOR_ID_2);//关闭指定风扇

 

**4.5.7.** **获取急停按钮状态**

 

/**

 \* Get status of the scram button.

 *

 \* @param callBack the callback after request

 */

public void getScramButtonStatus(IRobotCallBack callBack)

 

返回结果

{

  "data": {

​    "info": false

  },

  "code": 0,

  "msg": "success",

  "status": 0

}

 

**4.5.8. W3** **机器舱内风扇控制**

 

 \* @param callBack the callback after request

 */

public void openFan(IDataCallback callBack, int fanId)

public void closeFan(IDataCallback callBack, int fanId)

 

//调用

PeanutSDK.getInstance().door().closeFan()

PeanutSDK.getInstance().door().openFan()

 

 



**4.7.** **调度接口**

PeanutSDK.getInstance().schedule().xxx()

**4.7.1.** **获取调度状态**

 

/**

 \* Get status of motor.

 *

 \* @param callBack the callback after request

 */

public void getStatus(IRobotCallBack callBack)

**1.****等待**

{

  "status": 0,

  "code": 0,

  "msg": "success",

  "data": {

​    "status": 1,

​    "msg": "stop",

​    "data": {

​      "local": {

​        "id": 1,

​        "x": 1.23,

​        "y": 2.34

​      },

​      "other": {

​        "id": 2,

​        "status": 0,

​        "x": 1.23,

​        "y": 2.34,

​        "path": [

​          {

​            "x": 1.23,

​            "y": 2.34

​          },

​          {

​            "x": 1.23,

​            "y": 2.34

​          },

​          {

​            "x": 1.23,

​            "y": 2.34

​          },

​          {

​            "x": 1.23,

​            "y": 2.34

​          }

​        ]

​      },

​      "distance": 5.23,

​      "time": 20

​    }

  }

}

| **字段**          | **实例值** | **有效值** | **说明**                     |
| ----------------- | ---------- | ---------- | ---------------------------- |
| status            | 1          | 0,1,2,3    | 当前机器人的状态，被调度暂停 |
| data-local-id     | 1          |            | 当前机器人的调度ID           |
| data-local-x      | 1.23       |            | 当前机器人的位置x, 单位米    |
| data-local-y      | 2.34       |            | 当前机器人的位置y, 单位米    |
| data-other-id     | 1          |            | 另一台机器人的调度ID         |
| data-other-status | 0          | 0,1,2,3    | 另一台机器人的状态，正常运动 |
| data-other-x      | 1.23       |            | 另一台机器人的位置x, 单位米  |
| data-other-y      | 2.34       |            | 另一台机器人的位置y, 单位米  |
| data-other-path   |            |            | 另一台机器人的路线           |
| data-distance     | 5.23       |            | 路线冲突的剩余距离, 单位米   |
| data-time         | 20         |            | 路线冲突的剩余时间, 单位秒   |

**2.****避让**

{

  "status": 0,

  "code": 0,

  "msg": "success",

  "data": {

​    "status": 2,

​    "msg": "avoid",

​    "data": {

​      "local": {

​        "id": 1,

​        "x": 1.23,

​        "y": 2.34

​      },

​      "other": {

​        "id": 2,

​        "status": 1,

​        "x": 1.23,

​        "y": 2.34,

​        "path": [

​          {

​            "x": 1.23,

​            "y": 2.34

​          },

​          {

​            "x": 1.23,

​            "y": 2.34

​          },

​          {

​            "x": 1.23,

​            "y": 2.34

​          },

​          {

​            "x": 1.23,

​            "y": 2.34

​          }

​        ]

​      },

​      "distance": 5.23,

​      "time": 20

​    }

  }

}

| **字段**          | **实例值** | **有效值** | **说明**                       |
| ----------------- | ---------- | ---------- | ------------------------------ |
| status            | 2          | 0,1,2,3    | 当前机器人的状态，被调度去避让 |
| data-local-id     | 1          |            | 当前机器人的调度ID             |
| data-local-x      | 1.23       |            | 当前机器人的位置x, 单位米      |
| data-local-y      | 2.34       |            | 当前机器人的位置y, 单位米      |
| data-other-id     | 1          |            | 另一台机器人的调度ID           |
| data-other-status | 1          | 0,1,2,3    | 另一台机器人的状态，被调度暂停 |
| data-other-x      | 1.23       |            | 另一台机器人的位置x, 单位米    |
| data-other-y      | 2.34       |            | 另一台机器人的位置y, 单位米    |
| data-other-path   |            |            | 另一台机器人的路线             |
| data-distance     | 5.23       |            | 路线冲突的剩余距离, 单位米     |
| data-time         | 20         |            | 路线冲突的剩余时间, 单位秒     |

**3.****原点**

{

  "status": 0,

  "code": 0,

  "msg": "success",

  "data": {

​    "status": 3,

​    "msg": "origin",

​    "data": {

​      "local": {

​        "id": 1,

​        "x": 1.23,

​        "y": 2.34

​      },

​      "from": {

​        "id": 60001,

​        "x": 1.23,

​        "y": 2.34

​      },

​      "to": {

​        "id": 60002,

​        "x": 3.23,

​        "y": 4.34

​      }

​    }

  }

}

| **字段**      | **实例值** | **有效值** |                            |
| ------------- | ---------- | ---------- | -------------------------- |
| status        | 3          | 0,1,2,3    | 当前机器人的状态，调整原点 |
| data-local-id | 1          |            | 当前机器人的调度ID         |
| data-local-x  | 1.23       |            | 当前机器人的位置x, 单位米  |
| data-local-y  | 2.34       |            | 当前机器人的位置y, 单位米  |
| data-from-id  | 60001      |            | 当前原点ID                 |
| data-from-x   |            |            | 当前原点的位置x, 单位米    |
| data-from-y   |            |            | 当前原点的位置y, 单位米    |
| data-to-id    | 60002      |            | 调整后原点ID               |
| data-to-x     |            |            | 调整后原点的位置x, 单位米  |
| data-to-y     |            |            | 调整后原点的位置y, 单位米  |

**4.7.2.** **获取调度在线状态**

 

/**

 \* Get status of motor.

 *

 \* @param callBack the callback after request

 */

public void getLiveStatus(IRobotCallBack callBack)

 

{

  "status": 0,

  "code": 0,

  "msg": "success",

  "data": {

​    "local": {

​      "id": 1,

​      "status": 1,

​      "x": 1.23,

​      "y": 2.34,

​      "connection": [

​        1,

​        3,

​        2,

​        3,

​        3

​      ]

​    },

​    "other": [

​      {

​        "id": 2,

​        "status": 0,

​        "x": 1.23,

​        "y": 2.34,

​        "connection": [

​          1,

​          3,

​          2,

​          3,

​          3

​        ]

​      },

​      {

​        "id": 3,

​        "status": 1,

​        "x": 1.23,

​        "y": 2.34,

​        "connection": [

​          1,

​          3,

​          2,

​          3,

​          3

​        ]

​      },

​      {

​        "id": 4,

​        "status": 2,

​        "x": 1.23,

​        "y": 2.34,

​        "connection": [

​          1,

​          3,

​          2,

​          3,

​          3

​        ]

​      },

​      {

​        "id": 5,

​        "status": 3,

​        "x": 1.23,

​        "y": 2.34,

​        "connection": [

​          1,

​          3,

​          2,

​          3,

​          3

​        ]

​      }

​    ]

  }

}

| **字段**         | **实例值** | **有效值** | **说明**                                                     |
| ---------------- | ---------- | ---------- | ------------------------------------------------------------ |
| other-y          | 2.34       |            | 机器人的位置y, 单位米                                        |
| other-x          | 1.23       |            | 机器人的位置x, 单位米                                        |
| other-status     | 1          | 0,1,2,3    | 机器人的调度状态                                             |
| other-id         | 2          |            | 机器人的调度ID                                               |
| local-y          | 2.34       |            | 当前机器人的位置y, 单位米                                    |
| local-x          | 1.23       |            | 当前机器人的位置x, 单位米                                    |
| local-status     | 1          | 0,1,2,3    | 当前机器人的调度状态                                         |
| local-id         | 1          |            | 当前机器人的调度ID                                           |
| local-connection | 1,3,2,3,3  | 0,1,2,3    | 0-无连接 3-信号强；有5个数字，依次表示与编号为1的机器人的连接强度，与编号为2的，... |
| local-connection | 1,3,2,3,3  | 0,1,2,3    | 0-无连接 3-信号强；有5个数字，依次表示与编号为1的机器人的连接强度，与编号为2的，... |

**4.7.3.** **调度暂停**

 

/**

 \* Get status of motor.

 *

 \* @param callBack the callback after request

 */

public void pause(IRobotCallBack callBack)

**4.7.4.** **调度恢复**

 

/**

 \* Get status of motor.

 *

 \* @param callBack the callback after request

 */

public void resume(IRobotCallBack callBack)



**4.9.** **订阅接口**

**4.9.1.** **开始订阅接口**

指定主题创建订阅，返回数据中的topic即为订阅的主题。

/**

 \* Subscribe the topic and return data in callback.

 *

 \* @param topic the topic

 \* @param callBack the callback after request

 */

public void subscribe(String topic, IRobotCallBack callBack)

**4.9.2.** **取消订阅接口**

指定主题取消订阅

/**

 \* Cancel subscribed topic.

 *

 \* @param topic the topic

 */

public void unSubscribe(String topic)



**4.10.** **订阅主题列表**

| 主题              | 描述             |
| ----------------- | ---------------- |
| BATTERY_STATUS    | 电源状态         |
| BUTTON_STATUS     | 急停按钮状态     |
| ELEVATOR_FORWARD  | 电梯请求         |
| ELEVATOR_STATUS   | 电梯状态         |
| GUIDE_STATUS      | 导引点状态       |
| MOTOR_STATUS      | 电机状态         |
| NAVIGATION_STATUS | 导航状态         |
| RUNTIME_HEALTH    | 健康状态         |
| RUNTIME_HEARTBEAT | 心跳状态         |
| RUNTIME_OBJECT    | 移动物体状态     |
| SENSOR_COLLISION  | 碰撞条状态       |
| SCHEDULE_LIVE     | 调度在线状态     |
| SCHEDULE_STATUS   | 调度状态         |
| UPDATE_STATUS     | 升级状态         |
| VENDOR_MATCH      | 售货机匹配状态   |
| NAVIGATION_PATH   | 位置路线信息上报 |



**4.10.1.** **查询电源状态**

 

TopicName.BATTERY_STATUS

 

返回结果

{

  "data": {

​    "event": 0,

​    "power": 83

  },

  "code": 0,

  "msg": "success",

  "status": 0,

  "topic":"BatteryStatusApi"

}

 

**4.10.2.** **查询急停按钮状态**

 

TopicName.BUTTON_STATUS

 

返回结果

{

  "data": {

​    "info": false

  },

  "code": 0,

  "msg": "success",

  "status": 0,

  "topic": "ButtonStatusApi"

}

 

**4.10.3.** **查询电机状态**

 

TopicName.MOTOR_STATUS

 

返回结果

{

  "data": {

​    "desc": "MOTOR_STATUS_LOCKED",

​    "status": 255

  },

  "code": 0,

  "msg": "success",

  "status": 0,

  "topic":"MotorStatusApi"

}

 

**4.10.4.** **查询导引点信息**

 

TopicName.GUIDE_STATUS

 

返回结果

{

}

 

**4.10.5.** **查询导航状态**

 

TopicName.NAVIGATION_STATUS

 

返回结果

{

  "data": {

​    "desc": "idle",

​    "dst": 1,

​    "remain_length": 50.238235,

​    "remain_time": 100.476471,

​    "schedule": 0,

​    "status": 82,

​    "total_length": 61.702114,

​    "total_time": 123.404228

  },

  "code": 0,

  "msg": "success",

  "status": 0,

  "topic":"NavigationStatusApi"

}

 

**4.10.6.** **查询健康状态**

 

TopicName.RUNTIME_HEALTH

 

返回结果

{

  "data": [

​    {

​      "code": 513,

​      "desc": "CODE_ERROR_GAZER_NO_DATA",

​      "pose": {

​        "phi": -7.47E-4,

​        "x": 1.32E-4,

​        "y": -4.916307E-8

​      }

​    }

  ],

  "code": 0,

  "msg": "success",

  "status": 0,

  "topic": "RuntimeHealthApi"

}

 

**4.10.7.** **查询心跳状态**

 

TopicName.RUNTIME_HEARTBEAT

 

返回结果

{

  "data": {

​    "stm32": {

​      "desc": "stm32 online",

​      "status": 16

​    },

​    "sync": {

​      "desc": "init",

​      "status": 1

​    },

​    "motor": {

​      "status": 255,

​      "desc": "MOTOR_STATUS_LOCK"

​    },

​    "mode": {

​      "status": 16,

​      "desc": "WORK_STATUS_AUTOMODE_IDLE"

​    },

​    "time": 414754,

​    "timestamp": 14499885

  },

  "code": 0,

  "msg": "success",

  "status": 0,

  "topic": "RuntimeHeartbeatApi"

}

 

**4.10.8.** **查询移动物体信息**

 

TopicName.RUNTIME_OBJECT

 

返回结果

{

  "data": {

​    "exist": false,

​    "x": 0.0,

​    "y": 0.0

  },

  "code": 0,

  "msg": "success",

  "status": 0,

  "topic": "RuntimeObjectApi"

}

 

**4.10.9.** **查询碰撞条状态**

 

TopicName.SENSOR_COLLISION

 

返回结果

{

  "data": {

​    "back": true,

​    "front": false,

​    "left": false,

​    "mid-left": false,

​    "mid-right": false,

​    "right": false,

​    "state": 16

  },

  "code": 0,

  "msg": "success",

  "status": 0,

  "topic": "SensorCollisionApi"

}

 

**4.10.10.** **查询升级状态**

 

TopicName.UPDATE_STATUS

 

返回结果

{

}

**4.10.11.** **乘梯状态**

TopicName.ELEVATOR_STATUS

 

返回结果

{

 "code": 0,

 "data": {

  "curFloor": 2,

  "dstFloor": 1,

  "elevatorId": 6304,

  "errorCode": 0,

  "retryTimes": 0,

  "status": 20,

  "taskNo": "124956750"

 },

 "msg": "success",

 "status": 0}

| 事件码(status)                                | 事件描述                         | ros上报条件                                                  | app使用策略                                                  |
| --------------------------------------------- | -------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 0  (PROCESS_NO_EVENT)                         | 退出乘梯                         | 退出乘梯状态机重置为IDLE                                     | 无                                                           |
| 1  (PROCESS_ARRIVE_ELEVATOR_DOOR)             | 到达候梯点                       | 到达候梯点                                                   | 1、主任务：新增乘梯子任务  2、查询网络状态并上报给后台  3、记录乘梯日志，乘梯结束时上报后台  4、修改交互界面：“等待电梯”  5、匹配errorCode并记录事件 |
| 2  (PROCESS_WAIT_ELEVATOR_TIMEOUT)            | 等待电梯到达超时                 | 等待电梯到达超时                                             | 1、修改交互界面：“等待电梯”  2、匹配errorCode并记录事件      |
| 3  (PROCESS_GOING_INTO_BLOCK)                 | 进梯阻挡                         | 进梯遇到障碍物                                               | 1、修改交互界面：“进梯中”  2、匹配errorCode并记录事件        |
| 4  (PROCESS_GO_INTO_SUCCESS)                  | 进梯完成                         | 到达梯内点 或 梯内点无法到达但已经在梯内                     | 1、记录事件：乘梯中状态  2、查询网络状态并上报给后台  3、修改交互界面：“乘梯中” |
| 5  (PROCESS_ARRIVE_START_FLOOR)               | 电梯到达起始楼层                 | 电梯到达起始楼层                                             |                                                              |
| 6  (PROCESS_ARRIVE_DEST_FLOOR)                | 机器到达目标楼层                 | 机器到达目标楼层                                             |                                                              |
| 7  (PROCESS_GET_OUT_SUCCESS)                  | 出梯完成                         | 出梯完成                                                     |                                                              |
| 8  (PROCESS_GET_OUT_BLOCK)                    | 出梯阻挡                         | 出梯遇到障碍物                                               |                                                              |
| 9  (PROCESS_START_GETOUT_ELEVATOR)            | 开始出梯                         | 开始出梯                                                     |                                                              |
| 10  (PROCESS_GOING_OUT_TIMEOUT)               | 出梯错层或出梯超时               | 出梯错层或出梯超时                                           |                                                              |
| 11  (PROCESS_GOING_INTO_FAILED_WAIT_NEXT)     | 进梯失败                         | 梯内拥挤导致进梯失败                                         |                                                              |
| 15  (PROCESS_START_GOING_INTO_ELEVATOR)       | 开始进梯                         | 开始进梯                                                     |                                                              |
| 17  (PROCESS_TASK_FINISH)                     | 乘梯完成                         | 出梯完成，结束乘梯任务                                       |                                                              |
| 18  (PROCESS_TASK_FAIL)                       | 乘梯失败                         | 1.    电梯12min未到达起始楼层且只有一部电梯，乘梯失败；  2.    3轮外呼超时且只有一部电梯，乘梯失败；  3.    在梯内12min电梯未到达目标楼层，乘梯失败；  4.    3轮内呼超时乘梯失败； |                                                              |
| 19  (PROCESS_TASK_CANCEL)                     | 取消乘梯                         | 1.    跨楼层地标点重定位，取消乘梯；  2.    梯外候梯时收到当前楼层新任务，取消乘梯；  3.    在当前楼层出梯时收到当前楼层新任务，取消乘梯；  4.    app下发取消乘梯； |                                                              |
| 20  (PROCESS_GET_ELEVATOR_ACCESS)             | 获取使用权成功                   | 获取使用权成功                                               |                                                              |
| 21  (PROCESS_ARRIVE_QUEUE)                    | 到达排队点                       | 到达排队点                                                   |                                                              |
| 22  (PROCESS_ARRIVE_OBSTACLE)                 | 到达避障点                       | 到达避障点                                                   |                                                              |
| 24  (PROCESS_CALL_ELEVATOR_OUTDOOR_4G)        | 4G外呼成功                       | 4G外呼成功                                                   |                                                              |
| 25  (PROCESS_CALL_ELEVATOR_OUTDOOR_LORA)      | lora外呼成功                     | lora外呼成功                                                 |                                                              |
| 26  (PROCESS_CALL_ELEVATOR_INDOOR_4G)         | 4G内呼成功                       | 4G内呼成功                                                   |                                                              |
| 27  (PROCESS_CALL_ELEVATOR_INDOOR_LORA)       | lora内呼成功                     | lora内呼成功                                                 |                                                              |
| 28  (QUERY_ELEVATOR_GROUP_SUCCESS)            | 查询电梯分组成功(第三方电梯使用) | 查询电梯分组成功(第三方电梯使用)                             |                                                              |
| 29  (PROCESS_QUERY_ELEVATOR_ONLINE_STATE_4G)  | 上报主从机在线状态               | 后台返回主从机在线状态                                       |                                                              |
| 30  (PROCESS_GET_ELEVATOR_ACCESS_FAIL_LORA)   | lora获取使用权失败               | 超时 或者 lora返回获取失败原因                               |                                                              |
| 31  (PROCESS_GET_ELEVATOR_ACCESS_FAIL_4G)     | 4G获取使用权失败                 | 超时 或者 4G返回获取失败原因                                 |                                                              |
| 32  (PROCESS_CALL_ELEVATOR_OUTDOOR_FAIL_LORA) | lora外呼失败                     | 超时 或者 lora返回外呼失败原因                               |                                                              |
| 33  (PROCESS_CALL_ELEVATOR_OUTDOOR_FAIL_4G)   | 4G外呼失败                       | 超时 或者 4G返回外呼失败原因                                 |                                                              |
| 34  (PROCESS_CALL_ELEVATOR_INDOOR_FAIL_4G)    | 4G内呼失败                       | 超时 或者 4G返回内呼失败原因                                 |                                                              |
| 35  (PROCESS_CALL_ELEVATOR_INDOOR_FAIL_LORA)  | lora内呼失败                     | 超时 或者 lora返回内呼失败原因                               |                                                              |
| 36  (PROCESS_START_GET_ELEVATOR_ACCESS)       | 开始获取使用权                   | 开始获取使用权                                               |                                                              |
|                                               |                                  |                                                              |                                                              |

 

#  

# 五、常量字段值

## 5.1错误码



**错误信息类** 应用层可通过此类获取错误描述信息，以及具体的错误码。

| **错误码** | **说明**                               | **解决方案** |
| ---------- | -------------------------------------- | ------------ |
| 203 000    | 初始化参数Context为空                  |              |
| 203 001    | 初始化Component为空                    |              |
| 203 002    | 初始化权限授权失败                     |              |
|            |                                        |              |
| 203 020    | 授权                                   |              |
| 203 021    | 授权验证失败                           |              |
| 203 022    | 授权参数为空                           |              |
| 203 023    | 授权secret不匹配                       |              |
| 203 024    | 授权证书错误                           |              |
| 203 025    | 授权机器不匹配                         |              |
| 203 026    | 授权应用不匹配                         |              |
| 203 027    | 授权AppId不匹配                        |              |
| 203 028    | 授权指纹信息错误                       |              |
| 203 029    | 授权时间非法                           |              |
| 203 030    | 授权指纹编码异常                       |              |
| 203 031    | 授权指纹解码异常                       |              |
| 203 032    | 授权过期                               |              |
|            |                                        |              |
|            |                                        |              |
| 203 040    | Properties                             |              |
|            |                                        |              |
| 203 060    | Other                                  |              |
|            |                                        |              |
| 203 100    | Serial                                 |              |
| 203 101    | 连接超时，通讯断开                     |              |
| 203 102    | 连接恢复，通讯正常                     |              |
|            |                                        |              |
| 203 150    |                                        |              |
| 203 151    | Coap的onError                          |              |
| 203 152    | Coap返回数据为空                       |              |
|            |                                        |              |
| 203  170   | Coap                                   |              |
| 203  171   | Coap 请求Error                         |              |
| 203 172    | Coap 请求返回数据为空                  |              |
| 203 173    | Coap 请求超时                          |              |
|            |                                        |              |
|            |                                        |              |
|            |                                        |              |
| 203 200    | Socket                                 |              |
|            |                                        |              |
| 203 300    | 电源                                   |              |
| 203 301    | 去充电超时                             |              |
| 203 302    | 目标点不存在                           |              |
| 203 303    | 路线不存在                             |              |
| 203 304    | 匹配充电桩失败 没有匹配到一次          |              |
| 203 305    | 匹配充电桩超时 匹配到至少一次          |              |
| 203 306    | 没有标签数据超时                       |              |
| 203 307    | 没有激光数据超时                       |              |
| 203 308    | 没有stm32数据超时                      |              |
| 203 309    | 开始匹配充电桩60秒内未进入充电状态超时 |              |
| 203 310    | 没有5V                                 |              |
| 203 311    | 充电意外断开                           |              |
| 203 312    | 放弃充电，超过最大匹配次数             |              |
| 203 313    | 未设置fsm                              |              |
| 203 314    | 断开重试次数达到限制                   |              |
| 203 315    | 订阅电源api 失败                       |              |
| 203 316    | 检测到5V，但没检测到电流超时           |              |
| 203 317    | 去自动充电失败                         |              |
|            |                                        |              |
| 203 350    | 电机                                   |              |
|            |                                        |              |
| 203 400    | 导航                                   |              |
| 203 401    | 路线规划失败，目标点不存在             |              |
| 203 402    | 路线规划失败，路线不存在               |              |
| 203 403    | 路线规划失败，机器人位置丢失           |              |
| 203 404    | 路线规划失败，最优算法超过限制5个      |              |
| 203 405    | 自主导航失败，没有目标点               |              |
| 203 406    | 自主导航失败，没有路线                 |              |
| 203 407    | 自主导航失败，没有路线                 |              |
| 203 408    | 自主导航失败，充电中                   |              |
| 203 409    | 自主导航失败，到达策略为空             |              |
| 203 410    | 自主导航失败，机器未空闲               |              |
| 203 411    | 自主导航失败，超过设置的循环次数       |              |
|            |                                        |              |
| 203  550   | 文件传输                               |              |
|            |                                        |              |
| 203  551   | 传输超时                               |              |
| 203  552   | 压缩失败                               |              |
| 203  553   | 压缩路径无效                           |              |
| 203  554   | 输入文件参数为空                       |              |
| 203  555   | Robot 校验md5 失败                     |              |
| 203  557   | 上传文件获取失败                       |              |
| 203  558   | 压缩包为空                             |              |
| 203  559   | 上传信息获取失败                       |              |
| 203  560   | 解压文件无效                           |              |
| 203  561   | APP 检验md5 失败                       |              |
|            |                                        |              |
| 200 500    | 舱门                                   |              |
| 200 501    | 舱门类型获取失败/未知                  |              |
| 200 502    | 舱门ID非法或不存在                     |              |
| 200 503    | 对应舱门的状态获取失败                 |              |

## 5.2事件 - 设备运行



| **数值** | **事件说明**           | **数据类型及说明**                 |
| -------- | ---------------------- | ---------------------------------- |
| 10000    | 指令执行失败           | null                               |
| 10001    | 配置更新重启开始启动   | String，文本描述                   |
| 10002    | 配置更新重启开始初始化 | String，文本描述                   |
| 10003    | 配置更新重启已准备就绪 | String，文本描述                   |
| 10004    | 配置更新失败           | String，文本描述，更新失败的配置项 |
| 10005    | 配置更新成功           |                                    |
| 10006    | ping成功               |                                    |
| 10007    | ping失败               |                                    |
| 10010    | 工作模式变化           | int，工作模式数值                  |
| 10011    | 电量变化               | int  电量数值                      |
| 10012    | Robot IP变化           | String，IP地址                     |
| 10013    | 紧急按钮状态变化       | boolean，true：开启，false：关闭   |
| 10014    | 电机锁定状态变化       | int，电机状态                      |
| 10015    | 总里程变化             | double，总里程数                   |
| 10016    | 定位成功               |                                    |
| 10017    | Robot 同步时间成功     |                                    |
| 10018    | 获取位置路线信息成功   | Json String                        |

 

## 5.3事件 - 电源充电事件

| 数值  | 事件说明                                     | **数据类型及说明** |
| ----- | -------------------------------------------- | ------------------ |
| 40001 | 到达充电桩                                   | int                |
| 40002 | 匹配充电桩失败 没有匹配到一次                | int                |
| 40003 | 匹配充电桩超时 匹配到至少一次                | int                |
| 40004 | 没有标签 超时                                | int                |
| 40005 | 没有激光 超时                                | int                |
| 40006 | 没有stm32 超时                               | int                |
| 40007 | 从开始匹配充电桩计时60秒内未进入充电状态超时 | int                |
| 40008 | 没有5v                                       | int                |
| 40009 | 检测到5V，但没检测到电流超时                 | int                |
| 40010 | 意外断开                                     | int                |
| 40011 | 正在充电                                     | int                |
| 40012 | 放弃充电，超过最大匹配次数                   | int                |
| 40013 | 重新匹配充电桩，去充电点(目标点)             | int                |
| 40014 | 重新匹配充电桩，到达充电点                   | int                |
| 40015 | 未定义                                       | int                |
| 40016 | 未定义                                       | int                |
| 40017 | 未定义                                       | int                |
| 40018 | 未定义                                       | int                |
| 40019 | 未定义                                       | int                |
| 40020 | 退出充电                                     | int                |
| 40021 | 未定义                                       | int                |
| 40022 | 未定义                                       | int                |
| 40023 | 充电超时                                     | int                |

 

## 5.4常量 - 工作模式



定义了机器人可以切换的工作模式。所属ApiConstants类。

| **常量字段**    | **说明** |
| --------------- | -------- |
| AUTO            | 默认     |
| BUILD_MAP       | 建图模式 |
| MFG_TEST        | 测试模式 |
| CHARGER         | 充电模式 |
| MANUAL          | 手动模式 |
| APAPTER_CHARGER | 线充模式 |
| SELF_TEST       | 自检模式 |

## 5.5常量 - 导航状态



定义了机器人自主导航时的状态。

| **数值** | **说明**         |
| -------- | ---------------- |
| 0        | 空闲             |
| 1        | 已准备，路线规划 |
| 2        | 行进中           |
| 3        | 已到达           |
| 4        | 暂停             |
| 5        | 碰撞             |
| 6        | 阻挡             |
| 7        | 停止             |
| 8        | 故障             |
| 9        | 阻挡中           |
| 10       | 结束             |

## 5.6常量 - 路线规划策略



定义了机器人路线规划的策略。所属PeanutNavigation类。

| **常量字段**    | **说明**             |
| --------------- | -------------------- |
| POLICY_ADAPTIVE | 最优路线             |
| POLICY_FIXED    | 固定路线，目标点顺序 |

## 5.7常量 - 手动导航方向



定义了机器人手动控制时的运动方向。所属Navigation类。

| **常量字段** | **说明** |
| ------------ | -------- |
| FORWARD      | 前进     |
| BACKWARD     | 后退     |
| LEFT         | 左转     |
| RIGHT        | 右转     |

## 5.8常量 - 电源状态



| 数值 | 说明                 |
| ---- | -------------------- |
| 1    | 空闲                 |
| 2    | 自动去充电中         |
| 3    | 手动去充电中         |
| 4    | 充电中               |
| 5    | 适配器充电中         |
| 6    | 正在取消充电中的状态 |

## 5.9常量 - 电源充电指令



定义机器人触发充电指令。所属PeanutCharger类。

 

| **常量字段**          | **说明**   |
| --------------------- | ---------- |
| CHARGE_ACTION_AUTO    | 自动充电   |
| CHARGE_ACTION_MANUAL  | 手动充电   |
| CHARGE_ACTION_ADAPTER | 适配器充电 |
| CHARGE_ACTION_STOP    | 停止充电   |

## 5.10常量 - 电机状态



定义了电机的状态。

| **数值** | **说明**       |
| -------- | -------------- |
| 16       | 按钮解锁       |
| 32       | app解锁        |
| 49       | 右电机通信错误 |
| 50       | 左电机通信错误 |
| 51       | 右电机内部错误 |
| 52       | 左电机内部错误 |
| 255      | 锁定           |

# 六、FAQ

## 常见问题



**Android****系统界面无底部菜单栏，无法操作**

点击【设置】、【显示】，取消隐藏状态栏的勾选

**无法正常获取数据，控制机器失败，可以控制表情、舱门等，常规机型**

检查Android是否有送餐应用在运行，如果有请强制停止退出应用，再次重试

 

**无法正常获取数据，控制机器失败，可以控制表情、舱门等，G1、W3机型**

纯激光机型，需要设置通信协议为REMOTE_LINK_PROXY，详见SDK初始化

 

 

 