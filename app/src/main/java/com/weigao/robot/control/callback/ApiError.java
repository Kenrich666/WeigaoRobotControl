package com.weigao.robot.control.callback;

public class ApiError {
    private int code;
    private String message;
    private ErrorType type;
    private Throwable cause;

    public enum ErrorType {
        NETWORK_ERROR,
        DEVICE_ERROR,
        VALIDATION_ERROR,
        TIMEOUT_ERROR,
        UNKNOWN_ERROR
    }

    public ApiError(int code, String message) {
        this.code = code;
        this.message = message;
        this.type = ErrorType.UNKNOWN_ERROR;
    }

    public ApiError(int code, String message, ErrorType type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }

    public ApiError(int code, String message, ErrorType type, Throwable cause) {
        this.code = code;
        this.message = message;
        this.type = type;
        this.cause = cause;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ErrorType getType() {
        return type;
    }

    public void setType(ErrorType type) {
        this.type = type;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    @Override
    public String toString() {
        return "ApiError{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", type=" + type +
                ", cause=" + cause +
                '}';
    }
}
