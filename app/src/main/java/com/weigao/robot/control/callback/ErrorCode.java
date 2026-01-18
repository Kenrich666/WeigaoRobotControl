package com.weigao.robot.control.callback;

public class ErrorCode {
    public static final int SUCCESS = 0;
    public static final int ERROR_UNKNOWN = -1;
    public static final int ERROR_NETWORK = -100;
    public static final int ERROR_DEVICE = -200;
    public static final int ERROR_TIMEOUT = -300;
    public static final int ERROR_VALIDATION = -400;
    public static final int ERROR_AUTHENTICATION = -500;
    public static final int ERROR_PERMISSION_DENIED = -501;
    public static final int ERROR_TASK_NOT_FOUND = -600;
    public static final int ERROR_TASK_FAILED = -601;
    public static final int ERROR_POINT_NOT_FOUND = -700;
    public static final int ERROR_DOOR_LOCKED = -800;
    public static final int ERROR_PASSWORD_INCORRECT = -801;
    public static final int ERROR_SCRAM_PRESSED = -900;
    public static final int ERROR_BATTERY_LOW = -901;

    public static String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SUCCESS:
                return "Success";
            case ERROR_NETWORK:
                return "Network error";
            case ERROR_DEVICE:
                return "Device error";
            case ERROR_TIMEOUT:
                return "Timeout error";
            case ERROR_VALIDATION:
                return "Validation error";
            case ERROR_AUTHENTICATION:
                return "Authentication failed";
            case ERROR_PERMISSION_DENIED:
                return "Permission denied";
            case ERROR_TASK_NOT_FOUND:
                return "Task not found";
            case ERROR_TASK_FAILED:
                return "Task failed";
            case ERROR_POINT_NOT_FOUND:
                return "Point not found";
            case ERROR_DOOR_LOCKED:
                return "Door locked";
            case ERROR_PASSWORD_INCORRECT:
                return "Password incorrect";
            case ERROR_SCRAM_PRESSED:
                return "Scram button pressed";
            case ERROR_BATTERY_LOW:
                return "Battery low";
            default:
                return "Unknown error";
        }
    }
}
