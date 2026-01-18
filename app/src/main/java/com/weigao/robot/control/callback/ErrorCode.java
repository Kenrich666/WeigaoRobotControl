package com.weigao.robot.control.callback;

/**
 * 应用层错误码常量定义
 * <p>
 * 定义威高机器人控制系统应用层的错误码。
 * 这些错误码用于应用内部逻辑，与SDK错误码（{@link SdkErrorCode}）互补但不重叠。
 * </p>
 * <ul>
 * <li>应用层错误码范围：负数（-1 至 -999）</li>
 * <li>SDK错误码范围：正数（200000+）</li>
 * </ul>
 *
 * @see SdkErrorCode SDK层错误码定义
 */
public final class ErrorCode {

    private ErrorCode() {
        // 禁止实例化
    }

    // ==================== 通用错误码 ====================

    /** 成功 */
    public static final int SUCCESS = 0;

    /** 未知错误 */
    public static final int ERROR_UNKNOWN = -1;

    // ==================== 网络相关错误码（-100~-199） ====================

    /** 网络错误 */
    public static final int ERROR_NETWORK = -100;

    /** 网络连接超时 */
    public static final int ERROR_NETWORK_TIMEOUT = -101;

    /** 网络连接断开 */
    public static final int ERROR_NETWORK_DISCONNECTED = -102;

    // ==================== 设备相关错误码（-200~-299） ====================

    /** 设备错误 */
    public static final int ERROR_DEVICE = -200;

    /** 设备未连接 */
    public static final int ERROR_DEVICE_NOT_CONNECTED = -201;

    /** 设备通信失败 */
    public static final int ERROR_DEVICE_COMMUNICATION = -202;

    // ==================== 超时相关错误码（-300~-399） ====================

    /** 操作超时 */
    public static final int ERROR_TIMEOUT = -300;

    /** 请求超时 */
    public static final int ERROR_REQUEST_TIMEOUT = -301;

    // ==================== 验证相关错误码（-400~-499） ====================

    /** 验证错误 */
    public static final int ERROR_VALIDATION = -400;

    /** 参数无效 */
    public static final int ERROR_INVALID_PARAMETER = -401;

    /** 数据格式错误 */
    public static final int ERROR_DATA_FORMAT = -402;

    // ==================== 权限相关错误码（-500~-599） ====================

    /** 认证失败 */
    public static final int ERROR_AUTHENTICATION = -500;

    /** 权限被拒绝 */
    public static final int ERROR_PERMISSION_DENIED = -501;

    /** 未授权操作 */
    public static final int ERROR_UNAUTHORIZED = -502;

    // ==================== 任务相关错误码（-600~-699） ====================

    /** 任务未找到 */
    public static final int ERROR_TASK_NOT_FOUND = -600;

    /** 任务执行失败 */
    public static final int ERROR_TASK_FAILED = -601;

    /** 任务已取消 */
    public static final int ERROR_TASK_CANCELLED = -602;

    /** 任务已存在 */
    public static final int ERROR_TASK_EXISTS = -603;

    // ==================== 点位相关错误码（-700~-799） ====================

    /** 点位未找到 */
    public static final int ERROR_POINT_NOT_FOUND = -700;

    /** 点位不可达 */
    public static final int ERROR_POINT_UNREACHABLE = -701;

    // ==================== 舱门相关错误码（-800~-899） ====================

    /** 舱门已锁定 */
    public static final int ERROR_DOOR_LOCKED = -800;

    /** 密码错误 */
    public static final int ERROR_PASSWORD_INCORRECT = -801;

    /** 舱门操作失败 */
    public static final int ERROR_DOOR_OPERATION_FAILED = -802;

    // ==================== 安全相关错误码（-900~-999） ====================

    /** 急停按钮被按下 */
    public static final int ERROR_SCRAM_PRESSED = -900;

    /** 电量不足 */
    public static final int ERROR_BATTERY_LOW = -901;

    /** 安全锁定中 */
    public static final int ERROR_SECURITY_LOCKED = -902;

    // ==================== 工具方法 ====================

    /**
     * 判断是否为成功状态
     *
     * @param code 错误码
     * @return true=成功
     */
    public static boolean isSuccess(int code) {
        return code == SUCCESS;
    }

    /**
     * 判断是否为应用层错误码
     *
     * @param code 错误码
     * @return true=应用层错误码
     */
    public static boolean isAppError(int code) {
        return code < 0 && code >= -999;
    }

    /**
     * 判断是否为SDK错误码
     *
     * @param code 错误码
     * @return true=SDK错误码
     */
    public static boolean isSdkError(int code) {
        return code > 0;
    }

    /**
     * 获取错误码描述（中文）
     *
     * @param errorCode 错误码
     * @return 错误描述
     */
    public static String getErrorMessage(int errorCode) {
        // 如果是SDK错误码，委托给SdkErrorCode处理
        if (isSdkError(errorCode)) {
            return SdkErrorCode.getErrorDescription(errorCode);
        }

        switch (errorCode) {
            case SUCCESS:
                return "成功";
            case ERROR_UNKNOWN:
                return "未知错误";
            // 网络相关
            case ERROR_NETWORK:
                return "网络错误";
            case ERROR_NETWORK_TIMEOUT:
                return "网络连接超时";
            case ERROR_NETWORK_DISCONNECTED:
                return "网络连接断开";
            // 设备相关
            case ERROR_DEVICE:
                return "设备错误";
            case ERROR_DEVICE_NOT_CONNECTED:
                return "设备未连接";
            case ERROR_DEVICE_COMMUNICATION:
                return "设备通信失败";
            // 超时相关
            case ERROR_TIMEOUT:
                return "操作超时";
            case ERROR_REQUEST_TIMEOUT:
                return "请求超时";
            // 验证相关
            case ERROR_VALIDATION:
                return "验证错误";
            case ERROR_INVALID_PARAMETER:
                return "参数无效";
            case ERROR_DATA_FORMAT:
                return "数据格式错误";
            // 权限相关
            case ERROR_AUTHENTICATION:
                return "认证失败";
            case ERROR_PERMISSION_DENIED:
                return "权限被拒绝";
            case ERROR_UNAUTHORIZED:
                return "未授权操作";
            // 任务相关
            case ERROR_TASK_NOT_FOUND:
                return "任务未找到";
            case ERROR_TASK_FAILED:
                return "任务执行失败";
            case ERROR_TASK_CANCELLED:
                return "任务已取消";
            case ERROR_TASK_EXISTS:
                return "任务已存在";
            // 点位相关
            case ERROR_POINT_NOT_FOUND:
                return "点位未找到";
            case ERROR_POINT_UNREACHABLE:
                return "点位不可达";
            // 舱门相关
            case ERROR_DOOR_LOCKED:
                return "舱门已锁定";
            case ERROR_PASSWORD_INCORRECT:
                return "密码错误";
            case ERROR_DOOR_OPERATION_FAILED:
                return "舱门操作失败";
            // 安全相关
            case ERROR_SCRAM_PRESSED:
                return "急停按钮被按下";
            case ERROR_BATTERY_LOW:
                return "电量不足";
            case ERROR_SECURITY_LOCKED:
                return "安全锁定中";
            default:
                return "未知错误 (" + errorCode + ")";
        }
    }
}
