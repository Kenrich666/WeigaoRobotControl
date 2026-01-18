package com.weigao.robot.control.callback;

/**
 * SDK错误码常量定义
 * <p>
 * 包含Peanut SDK定义的各类错误码，便于与SDK返回的错误码进行匹配。
 * 错误码来源：Peanut SDKV1.3.4开发文档 - 常量字段值-错误码
 * </p>
 */
public final class SdkErrorCode {

    private SdkErrorCode() {
        // 禁止实例化
    }

    // ==================== 通用错误码 ====================

    /** 成功 */
    public static final int SUCCESS = 0;

    /** 方法不支持 */
    public static final int ERROR_METHOD_NOT_ALLOWED = 19;

    /** 空数据 */
    public static final int ERROR_EMPTY_DATA = 21;

    // ==================== 电源/充电错误码（203300-203317） ====================

    /** 充电错误：未知错误 */
    public static final int CHARGER_ERROR_UNKNOWN = 203300;

    /** 充电错误：充电桩未找到 */
    public static final int CHARGER_ERROR_PILE_NOT_FOUND = 203301;

    /** 充电错误：导航到充电桩失败 */
    public static final int CHARGER_ERROR_NAVIGATION_FAILED = 203302;

    /** 充电错误：充电对接失败 */
    public static final int CHARGER_ERROR_DOCKING_FAILED = 203303;

    /** 充电错误：充电中断 */
    public static final int CHARGER_ERROR_INTERRUPTED = 203304;

    /** 充电错误：电池异常 */
    public static final int CHARGER_ERROR_BATTERY_ABNORMAL = 203305;

    /** 充电错误：充电桩异常 */
    public static final int CHARGER_ERROR_PILE_ABNORMAL = 203306;

    /** 充电错误：充电超时 */
    public static final int CHARGER_ERROR_TIMEOUT = 203307;

    // ==================== 导航错误码（203400-203411） ====================

    /** 导航错误：未知错误 */
    public static final int NAVIGATION_ERROR_UNKNOWN = 203400;

    /** 导航错误：路径规划失败 */
    public static final int NAVIGATION_ERROR_PATH_PLANNING_FAILED = 203401;

    /** 导航错误：目标点不存在 */
    public static final int NAVIGATION_ERROR_TARGET_NOT_FOUND = 203402;

    /** 导航错误：定位丢失 */
    public static final int NAVIGATION_ERROR_LOCALIZATION_LOST = 203403;

    /** 导航错误：被阻挡超时 */
    public static final int NAVIGATION_ERROR_BLOCKED_TIMEOUT = 203404;

    /** 导航错误：发生碰撞 */
    public static final int NAVIGATION_ERROR_COLLISION = 203405;

    /** 导航错误：电量不足 */
    public static final int NAVIGATION_ERROR_LOW_BATTERY = 203406;

    /** 导航错误：急停按下 */
    public static final int NAVIGATION_ERROR_SCRAM_PRESSED = 203407;

    /** 导航错误：电机锁定 */
    public static final int NAVIGATION_ERROR_MOTOR_LOCKED = 203408;

    /** 导航错误：地图未加载 */
    public static final int NAVIGATION_ERROR_MAP_NOT_LOADED = 203409;

    /** 导航错误：导航被取消 */
    public static final int NAVIGATION_ERROR_CANCELED = 203410;

    /** 导航错误：导航超时 */
    public static final int NAVIGATION_ERROR_TIMEOUT = 203411;

    // ==================== 舱门错误码（200500-200503） ====================

    /** 舱门错误：未知错误 */
    public static final int DOOR_ERROR_UNKNOWN = 200500;

    /** 舱门错误：舱门卡住 */
    public static final int DOOR_ERROR_STUCK = 200501;

    /** 舱门错误：舱门传感器异常 */
    public static final int DOOR_ERROR_SENSOR_ABNORMAL = 200502;

    /** 舱门错误：舱门控制板通信失败 */
    public static final int DOOR_ERROR_COMMUNICATION_FAILED = 200503;

    // ==================== 充电事件码（40001-40021） ====================
    // 参考SDK文档 5.3节 - 事件-电源充电事件

    /** 充电事件：到达充电桩 */
    public static final int CHARGER_EVENT_ARRIVE_PILE = 40001;

    /** 充电事件：匹配充电桩失败（没有匹配到一次） */
    public static final int CHARGER_EVENT_MATCH_FAILED = 40002;

    /** 充电事件：匹配充电桩超时（匹配到至少一次） */
    public static final int CHARGER_EVENT_MATCH_TIMEOUT = 40003;

    /** 充电事件：没有标签超时 */
    public static final int CHARGER_EVENT_NO_TAG_TIMEOUT = 40004;

    /** 充电事件：没有激光超时 */
    public static final int CHARGER_EVENT_NO_LASER_TIMEOUT = 40005;

    /** 充电事件：没有stm32超时 */
    public static final int CHARGER_EVENT_NO_STM32_TIMEOUT = 40006;

    /** 充电事件：开始匹配后60秒未进入充电状态超时 */
    public static final int CHARGER_EVENT_CHARGING_TIMEOUT = 40007;

    /** 充电事件：没有5V */
    public static final int CHARGER_EVENT_NO_5V = 40008;

    /** 充电事件：检测到5V但没检测到电流超时 */
    public static final int CHARGER_EVENT_NO_CURRENT_TIMEOUT = 40009;

    /** 充电事件：意外断开 */
    public static final int CHARGER_EVENT_UNEXPECTED_DISCONNECT = 40010;

    /** 充电事件：正在充电 */
    public static final int CHARGER_EVENT_CHARGING = 40011;

    /** 充电事件：放弃充电，超过最大匹配次数 */
    public static final int CHARGER_EVENT_GIVE_UP = 40012;

    /** 充电事件：重新匹配充电桩，去充电点 */
    public static final int CHARGER_EVENT_RETRY_GO_PILE = 40013;

    /** 充电事件：重新匹配充电桩，到达充电点 */
    public static final int CHARGER_EVENT_RETRY_ARRIVE_PILE = 40014;

    /** 充电事件：退出充电 */
    public static final int CHARGER_EVENT_EXIT = 40020;

    // ==================== 工具方法 ====================

    /**
     * 判断是否为充电相关错误码
     *
     * @param code 错误码
     * @return true=充电错误
     */
    public static boolean isChargerError(int code) {
        return code >= 203300 && code <= 203317;
    }

    /**
     * 判断是否为导航相关错误码
     *
     * @param code 错误码
     * @return true=导航错误
     */
    public static boolean isNavigationError(int code) {
        return code >= 203400 && code <= 203411;
    }

    /**
     * 判断是否为舱门相关错误码
     *
     * @param code 错误码
     * @return true=舱门错误
     */
    public static boolean isDoorError(int code) {
        return code >= 200500 && code <= 200503;
    }

    /**
     * 获取错误码描述
     *
     * @param code 错误码
     * @return 错误描述
     */
    public static String getErrorDescription(int code) {
        switch (code) {
            case SUCCESS:
                return "成功";
            case ERROR_METHOD_NOT_ALLOWED:
                return "方法不支持";
            case ERROR_EMPTY_DATA:
                return "空数据";
            // 充电错误
            case CHARGER_ERROR_UNKNOWN:
                return "充电：未知错误";
            case CHARGER_ERROR_PILE_NOT_FOUND:
                return "充电：充电桩未找到";
            case CHARGER_ERROR_NAVIGATION_FAILED:
                return "充电：导航到充电桩失败";
            case CHARGER_ERROR_DOCKING_FAILED:
                return "充电：充电对接失败";
            case CHARGER_ERROR_INTERRUPTED:
                return "充电：充电中断";
            case CHARGER_ERROR_BATTERY_ABNORMAL:
                return "充电：电池异常";
            case CHARGER_ERROR_PILE_ABNORMAL:
                return "充电：充电桩异常";
            case CHARGER_ERROR_TIMEOUT:
                return "充电：充电超时";
            // 导航错误
            case NAVIGATION_ERROR_UNKNOWN:
                return "导航：未知错误";
            case NAVIGATION_ERROR_PATH_PLANNING_FAILED:
                return "导航：路径规划失败";
            case NAVIGATION_ERROR_TARGET_NOT_FOUND:
                return "导航：目标点不存在";
            case NAVIGATION_ERROR_LOCALIZATION_LOST:
                return "导航：定位丢失";
            case NAVIGATION_ERROR_BLOCKED_TIMEOUT:
                return "导航：被阻挡超时";
            case NAVIGATION_ERROR_COLLISION:
                return "导航：发生碰撞";
            case NAVIGATION_ERROR_LOW_BATTERY:
                return "导航：电量不足";
            case NAVIGATION_ERROR_SCRAM_PRESSED:
                return "导航：急停按下";
            case NAVIGATION_ERROR_MOTOR_LOCKED:
                return "导航：电机锁定";
            case NAVIGATION_ERROR_MAP_NOT_LOADED:
                return "导航：地图未加载";
            case NAVIGATION_ERROR_CANCELED:
                return "导航：导航被取消";
            case NAVIGATION_ERROR_TIMEOUT:
                return "导航：导航超时";
            // 舱门错误
            case DOOR_ERROR_UNKNOWN:
                return "舱门：未知错误";
            case DOOR_ERROR_STUCK:
                return "舱门：舱门卡住";
            case DOOR_ERROR_SENSOR_ABNORMAL:
                return "舱门：传感器异常";
            case DOOR_ERROR_COMMUNICATION_FAILED:
                return "舱门：控制板通信失败";
            default:
                return "未知错误 (" + code + ")";
        }
    }
}
