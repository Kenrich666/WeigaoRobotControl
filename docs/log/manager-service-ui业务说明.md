# manager、service、ui 目录业务说明

生成时间：2026-03-11

本文档面向 `app/src/main/java/com/weigao/robot/control/manager`、`service`、`ui` 三个目录，说明每个文件在机器人控制业务中的职责、它参与的流程以及和其他模块的关系。

## 1. manager 目录

### `AppSettingsManager.java`
- 全局应用配置管理器。
- 负责持久化全屏显示、投影灯开门等应用级开关。
- 配置落盘到外部存储，确保卸载重装后仍可复用同类配置思路。
- 主要被 `MainActivity`、导航页、设置页读取，用来决定界面形态和投影门联动行为。

### `CircularDeliveryHistoryManager.java`
- 循环配送历史记录管理器。
- 负责保存、读取、清空循环配送任务的执行记录。
- 主要服务于循环配送历史页，帮助追踪每条路线的执行结果、时间和次数。

### `CircularDeliverySettingsManager.java`
- 循环配送参数管理器。
- 负责保存循环配送模式下的配送速度、返航速度等参数。
- 主要被 `CircularDeliveryActivity`、`CircularDeliveryNavigationActivity`、`ReturnActivity` 使用。

### `GlobalScramManager.java`
- 全局急停状态管理器。
- 监听机器人运行状态里的急停按钮状态，并结合 Activity 生命周期决定是否弹出全局急停提示。
- 急停触发时会尝试停止充电等危险动作，属于全局安全兜底模块。

### `ItemDeliveryManager.java`
- 物品配送过程记录管理器。
- 负责在多层货柜配送场景中记录到点、签收、状态变化等历史信息。
- `DeliveryActivity` 和配送导航流程会依赖它写入记录，历史页依赖它读取记录。

### `ItemDeliverySettingsManager.java`
- 物品配送参数管理器。
- 负责保存普通配送模式下的配送速度、返航速度等参数。
- 是基础设置页和普通配送导航页之间的参数桥梁。

### `SoundSettingsManager.java`
- 音频配置管理器。
- 负责保存背景音乐、到点语音、循环导航语音、音量等声音相关配置。
- `SoundSettingsFragment` 负责编辑它，配送/返航/循环导航页负责读取它并驱动 `AudioService` 播放。

### `UVDisinfectionManager.java`
- 紫外消杀流程管理器。
- 负责监听充电状态，在满足条件时启动紫外灯消杀倒计时，结束或中断时关闭紫外灯。
- 同时会处理门状态、投影灯、充电联动等安全条件，是充电场景下的附加业务控制器。
- `ChargerSettingsFragment` 通过它展示当前消杀状态和剩余时间。

### `package-info.java`
- `manager` 包说明文件。
- 不承载业务逻辑，主要用于包级注释、文档组织和代码结构说明。

## 2. service 目录

### `ServiceManager.java`
- 全局服务注册与获取入口。
- 采用单例和懒加载方式统一创建 `NavigationService`、`DoorService`、`ChargerService`、`SecurityService`、`TimingService`、`AudioService`、`RobotStateService`、`RemoteCallService`。
- 是 UI 层访问业务服务的核心门面，避免页面直接 new 具体实现。

### `IAudioService.java`
- 音频服务接口。
- 抽象背景音乐、语音播报、停止播放、获取/更新音频配置等能力。
- 供声音设置页和配送/返航页解耦调用。

### `IChargerService.java`
- 充电服务接口。
- 抽象电量查询、充电状态监听、开始回桩、停止充电、充电信息读取等能力。
- 供充电设置页、消杀管理器、安全控制等模块使用。

### `IDoorService.java`
- 舱门服务接口。
- 抽象开门、关门、查询门状态、批量控制、门状态回调注册等能力。
- 是普通配送、循环到点、返航前安全检查等场景的关键服务。

### `INavigationService.java`
- 导航服务接口。
- 抽象目标点设置、路径准备、开始/暂停/停止导航、跳点、速度设置、到点控制、手动控制、路线状态查询等能力。
- 是配送、循环配送、返航、远程呼叫等所有移动类业务的核心接口。

### `IRemoteCallService.java`
- 远程呼叫服务接口。
- 定义远程呼叫开关、呼叫任务接收/取消、停留时长、回调通知等能力。
- 业务目标是支持机器人被远程叫到指定点位，并在到达后停留或自动返回。

### `IRobotStateService.java`
- 机器人运行状态服务接口。
- 抽象电量、位置、急停、电机、工作模式、重启、定位、设备信息读取等能力。
- 主要服务于定位校验、状态同步、故障检测和页面展示。

### `ISecurityService.java`
- 安全服务接口。
- 抽象密码校验、安全锁开关、锁定/解锁等能力。
- 为设置入口、开门鉴权、敏感操作鉴权提供统一安全逻辑。

### `ITimingService.java`
- 任务计时服务接口。
- 定义开始、暂停、恢复、停止计时以及历史记录查询能力。
- 用于需要统计任务时长的业务场景，并和门状态做联动控制。

### `impl/AudioServiceImpl.java`
- 音频服务实现。
- 负责真正管理媒体播放、语音播报和音频配置读取/保存。
- 被导航页、返航页、声音设置页直接通过接口使用。

### `impl/ChargerServiceImpl.java`
- 充电服务实现。
- 对接底层 SDK 读取充电信息、监听充电状态、执行充电相关控制。
- 是充电设置页、紫外消杀、异常安全处理的实际执行者。

### `impl/DoorServiceImpl.java`
- 舱门服务实现。
- 对接底层门控能力，负责门状态查询、单门或全门开关以及门事件分发。
- 物品配送、循环到点、返航前检查、投影门联动都依赖它。

### `impl/NavigationServiceImpl.java`
- 导航服务实现。
- 封装 Peanut 导航 SDK，负责设置目标点、准备路线、控制导航状态并向上层派发导航回调。
- 是所有移动业务流程的核心执行层。

### `impl/ProjectionDoorService.java`
- 投影灯开门辅助服务。
- 负责在开启投影门功能时，把投影/灯光事件和门操作流程串联起来。
- 主要给导航页和到点页提供门动作提示与联动能力。

### `impl/RemoteCallServiceImpl.java`
- 远程呼叫服务实现。
- 在远程呼叫启用后，负责接收呼叫任务、设置导航目标、处理到点停留、倒计时返回和结果回调。
- 本质上是基于 `INavigationService` 组合出来的“被叫到点”业务流程。

### `impl/RobotStateServiceImpl.java`
- 机器人状态服务实现。
- 对接运行时 SDK 监听心跳、健康状态、位置、电量、急停，并提供重启、定位、参数同步等操作。
- 是所有状态感知型功能的底层实现。

### `impl/SecurityServiceImpl.java`
- 安全服务实现。
- 负责密码配置持久化、旧配置迁移、密码验证、安全锁启停和解锁门操作。
- `PasswordActivity`、设置入口、门控敏感操作都会依赖它。

### `impl/TimingServiceImpl.java`
- 计时服务实现。
- 负责维护任务计时控制器、历史记录，并支持“开门自动停止计时”等业务规则。
- 适合配送任务统计、过程耗时分析等场景。

## 3. ui 目录

### 3.1 auth 子目录

### `auth/PasswordActivity.java`
- 密码输入与校验页面。
- 用于设置入口、返回首页、开门、结束任务等敏感操作的鉴权。
- 校验逻辑走 `ISecurityService`，校验成功后跳转到目标页面或放行目标操作。

### `auth/package-info.java`
- `ui.auth` 包说明文件。
- 不承载业务逻辑。

### 3.2 main 子目录

### `main/MainActivity.java`
- 应用主入口页面。
- 负责申请权限、初始化 SDK、准备默认配置、创建基础目录，并把用户导向“设置”“普通配送”“循环配送”三条主业务入口。
- 也是应用启动阶段的配置装载中心。

### `main/SettingsActivity.java`
- 设置中心容器页。
- 左侧菜单切换不同设置 Fragment，本身不处理具体业务逻辑。
- 负责组织基础设置、声音、定时任务、配送模式、充电、Wi-Fi、场景等设置页面。

### `main/DeliveryActivity.java`
- 普通物品配送任务编排页。
- 支持为 L1/L2/L3 等货层绑定目标点位，处理开门、返航、历史查看、开始配送前定位和关门检查。
- 是“选层位 -> 绑点位 -> 开始配送”的任务配置入口。

### `main/DeliveryNavigationActivity.java`
- 普通配送执行页。
- 负责根据货层绑定结果依次导航到目标点，支持双击暂停、继续、结束任务、自动恢复、到点开门/签收、背景音乐与语音播报。
- 是普通配送的核心执行界面。

### `main/ConfirmReceiptActivity.java`
- 普通配送到点后的签收确认页。
- 负责等待取货/确认签收，并决定继续下一点还是结束当前配送流程。
- 通常与门控操作、导航继续逻辑紧密耦合。

### `main/ReturnActivity.java`
- 返航执行页。
- 在配送结束或循环任务退出后，负责检查门状态、关闭舱门、设置返航目标点并执行返航导航。
- 同时支持暂停、继续、结束返航、返航音频播报等逻辑。

### `main/PositioningActivity.java`
- 定位检查页。
- 在任务正式开始前触发定位流程，用于确认机器人当前定位可用。
- 成功则进入后续流程，失败则跳转失败页或中断任务。

### `main/PositioningFailedActivity.java`
- 定位失败提示页。
- 当定位失败时给出重试入口和错误提示，防止在定位异常时继续执行导航任务。

### `main/ItemDeliveryHistoryActivity.java`
- 普通配送历史页。
- 读取 `ItemDeliveryManager` 里的配送记录并展示，支持清空历史。
- 用于回溯以往配送任务的到点与状态信息。

### `main/CircularDeliveryActivity.java`
- 循环配送任务配置页。
- 支持读取地图点位、编辑循环路线、配置路线节点、保存本地路线并开始循环导航。
- 是“编辑路线 -> 保存路线 -> 启动循环配送”的业务入口。

### `main/CircularDeliveryNavigationActivity.java`
- 循环配送执行页。
- 负责按路线和循环次数执行导航，支持暂停/继续、节点等待、手动返航、背景音乐/语音播报，并在结束后写入循环历史。
- 是循环配送模式的核心执行界面。

### `main/CircularArrivalActivity.java`
- 循环配送到点停留页。
- 在循环配送到达节点后负责倒计时、门操作、继续下一站或超时自动处理。
- 相当于循环模式下的“到点交互页”。

### `main/CircularDeliveryHistoryActivity.java`
- 循环配送历史页。
- 展示 `CircularDeliveryHistoryManager` 中的路线执行记录，支持清空。
- 用于查看历史路线名称、执行时间、循环次数和结果。

### `main/ReturnActivity.java`
- 返航页。
- 负责配送或循环业务结束后的安全返航，是多业务公用的收尾页面。

### `main/AboutMeActivity` 
- 当前目录下没有该文件，关于页实际由 `AboutMeFragment.java` 承担。

### 3.3 main/fragment 子目录

### `main/fragment/AboutMeFragment.java`
- 关于页面 Fragment。
- 一般用于展示应用、设备或团队相关说明，业务权重较低。

### `main/fragment/BasicSettingsFragment.java`
- 基础设置页。
- 负责配置普通配送和循环配送的配送速度、返航速度、全屏开关等基础参数。
- 会把配置写入 `ItemDeliverySettingsManager`、`CircularDeliverySettingsManager`、`AppSettingsManager`，是运行参数的主要编辑入口。

### `main/fragment/ChargerSettingsFragment.java`
- 充电与消杀设置页。
- 负责展示电池/充电状态、执行开始充电或停止充电、开启或关闭紫外消杀，并监听充电事件刷新界面。
- 同时承担“充电业务 + 消杀业务”联动控制台的角色。


### `main/fragment/DeliveryModeFragment.java`
- 配送模式参数展示页。
- 通过滑块展示或调整不同模式下的速度、距离等参数。
- 当前更像静态或演示型设置页，业务落地程度低于 `BasicSettingsFragment`。

### `main/fragment/NotificationSettingsFragment.java`
- 通知铃声设置占位页。
- 当前主要承担界面占位作用，后续可扩展系统通知或任务提醒策略。

### `main/fragment/PagerSettingsFragment.java`
- 呼叫器设置占位页。
- 预留给呼叫器相关配置，当前未承载实质业务逻辑。

### `main/fragment/RemoteSettingsFragment.java`
- 远程设置占位页。
- 预留给远程呼叫、远程控制类配置，当前仅提供界面壳子。

### `main/fragment/SceneSettingsFragment.java`
- 场景设置页。
- 负责场景地图、出口点位等业务参数的选择，用于把配送业务绑定到具体地图场景。
- 更偏向部署配置和运行环境配置。

### `main/fragment/ScheduledTasksFragment.java`
- 定时任务设置页。
- 支持编辑开始时间、周期、任务卡片等内容，当前主要是本地 UI 级任务管理和演示数据。
- 后续可扩展为真正的定时配送或定时消杀调度入口。

### `main/fragment/SoundSettingsFragment.java`
- 声音设置页。
- 负责音量、背景音乐、导航音频、语音文件的选择、导入和试听，并把结果保存到 `SoundSettingsManager`/`AudioService`。
- 是所有音频类业务配置的主入口。

### `main/fragment/WifiNetworkFragment.java`
- Wi-Fi 网络设置页。
- 负责 Wi-Fi 开关交互、网络列表点击、密码输入和连接动作的 UI 流程。
- 更偏向系统接入配置，而非机器人核心动作控制。

### 3.4 settings / task 子目录

### `settings/package-info.java`
- `ui.settings` 包说明文件。
- 当前无实际业务实现。

### `task/package-info.java`
- `ui.task` 包说明文件。
- 当前无实际业务实现。

## 4. 业务关系总览

### 4.1 普通配送链路
- `MainActivity` 进入 `DeliveryActivity`。
- `DeliveryActivity` 完成层位与目标点绑定，做定位和关门检查。
- `DeliveryNavigationActivity` 执行逐点导航。
- 到点后进入 `ConfirmReceiptActivity` 做签收或继续。
- 任务结束后进入 `ReturnActivity` 执行返航。
- 历史数据由 `ItemDeliveryManager` 管理，在 `ItemDeliveryHistoryActivity` 查看。

### 4.2 循环配送链路
- `MainActivity` 进入 `CircularDeliveryActivity`。
- `CircularDeliveryActivity` 编辑路线和节点。
- `CircularDeliveryNavigationActivity` 执行循环导航。
- 每站到点后进入 `CircularArrivalActivity` 处理停留、开门和超时。
- 结束后可进入 `ReturnActivity` 返航。
- 历史数据由 `CircularDeliveryHistoryManager` 管理，在 `CircularDeliveryHistoryActivity` 查看。

### 4.3 设置与底层能力链路
- `SettingsActivity` 组织各类设置 Fragment。
- Fragment 通过 `AppSettingsManager`、`ItemDeliverySettingsManager`、`CircularDeliverySettingsManager`、`SoundSettingsManager` 等持久化参数。
- UI 层通过 `ServiceManager` 获取 `NavigationService`、`DoorService`、`ChargerService`、`RobotStateService`、`SecurityService`、`AudioService` 等底层能力。
- 各服务实现层统一封装 Peanut SDK 和机器人硬件控制，避免页面直接依赖底层 SDK。

## 5. 当前代码结构特点
- `manager` 偏本地配置、历史记录和全局业务状态管理。
- `service` 偏硬件能力和 SDK 封装，是 UI 调用底层能力的统一入口。
- `ui` 偏业务流程编排和交互承载，普通配送、循环配送、返航、设置等业务都在这里闭环。
- 部分设置页目前还是占位实现，后续如果继续扩展远程呼叫、通知、呼叫器能力，可以优先在对应 Fragment 和 `IRemoteCallService` 方向继续落地。
