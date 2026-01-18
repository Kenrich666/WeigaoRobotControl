package com.weigao.robot.control.utils;

import android.util.Log;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.ErrorCode;

public class ErrorHandler {
    private static final String TAG = "ErrorHandler";

    public static ApiError handleException(Exception e) {
        Log.e(TAG, "Exception occurred", e);
        return new ApiError(ErrorCode.ERROR_UNKNOWN, e.getMessage(), ApiError.ErrorType.UNKNOWN_ERROR, e);
    }

    public static ApiError createNetworkError(String message) {
        Log.e(TAG, "Network error: " + message);
        return new ApiError(ErrorCode.ERROR_NETWORK, message, ApiError.ErrorType.NETWORK_ERROR);
    }

    public static ApiError createDeviceError(String message) {
        Log.e(TAG, "Device error: " + message);
        return new ApiError(ErrorCode.ERROR_DEVICE, message, ApiError.ErrorType.DEVICE_ERROR);
    }

    public static ApiError createTimeoutError(String message) {
        Log.e(TAG, "Timeout error: " + message);
        return new ApiError(ErrorCode.ERROR_TIMEOUT, message, ApiError.ErrorType.TIMEOUT_ERROR);
    }

    public static ApiError createValidationError(String message) {
        Log.e(TAG, "Validation error: " + message);
        return new ApiError(ErrorCode.ERROR_VALIDATION, message, ApiError.ErrorType.VALIDATION_ERROR);
    }

    public static void logError(ApiError error) {
        Log.e(TAG, "API Error: " + error.toString());
    }
}
