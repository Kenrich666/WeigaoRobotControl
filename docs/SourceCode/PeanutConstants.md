```
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.common.constant;

import android.os.Environment;

public class PeanutConstants {
    public static final String TAG_API = "API--";
    public static final String TAG_SDK = "SDK--";
    public static final String TAG_UTIL = "UTIL--";
    public static final String TAG_TFTP = "TFTP--";
    public static final String TAG_CONFIG = "CONFIG--";
    public static final String TAG_CHARGE = "CHARGE--";
    public static final String TAG_NAVI = "NAVI--";
    public static final String TAG_RUNTIME = "RUNTIME--";
    public static final String TAG_EMOTION = "EMOTION--";
    public static final String TAG_DISINFECT = "DISINFECT--";
    public static final String TAG_IOT = "IOT--";
    public static final String TAG_THREAD = "PeanutThread: ";
    public static final String TAG_DOOR = "DOOR--";
    public static final String TAG_TRANSPORT = "TRANSPORT--";
    public static final String TAG_GRAVITY = "GRAVITY--";
    public static final String TAG_SENSOR = "SENSOR--";
    public static final String TAG_DEVICE_NODE = "DEVICE_NODE--";
    public static final String API_PACKAGE = "com.keenon.sdk.api.";
    public static final String API_PACKAGE_V2 = "com.keenon.sdk.api2.";
    public static final String API_PACKAGE_PROXY = "com.keenon.sdk.apiProxy.";
    public static final int API_SUCCESS_CODE = 0;
    public static final int API_SUCCESS_STATUS = 0;
    public static final String API_SUCCESS_MESSAGE = "success";
    public static final int API_FAIL_CODE = 1;
    public static final int API_FAIL_STATUS = 1;
    public static final String API_FAIL_MESSAGE = "error";
    public static final String SCM_VER_1 = "v1.0";
    public static final String SCM_VER_2 = "v2.0";
    public static final String SCM_VER_3 = "v3.0";
    public static String AUTH_ID = "";
    public static String AUTH_SECRET = "";
    public static String PATH_PARAMS_PROPERTIES = Environment.getExternalStorageDirectory() + "/peanut/params";
    public static String PATH_TFTP_SERVER = Environment.getExternalStorageDirectory() + "/peanut/server/tftp";
    public static int LOG_LEVEL = 4;
    public static boolean LOG_ENABLE = true;
    public static boolean LOGBACK_ENABLE = false;
    public static boolean LOGBACK_PRINT_LOGCAT_ENABLE = false;
    public static boolean LOGBACK_WRITE_FILE_ENABLE = false;
    public static boolean LOG_WRITE_FILE_ENABLE = false;
    public static int LOG_MAX_SIZE = 4194304;
    public static int LOG_CACHE_COUNT = 100;
    public static String LOG_PATH = null;
    public static boolean UM_LOGBACK_ENABLE = true;
    public static final String COAP_PROTOCOL = "coap://";
    public static final int COAP_PORT = 5683;
    public static final String HTTP_PROTOCOL = "http://";
    public static final int HTTP_PORT = 34569;
    public static final long DEFAULT_INTERVAL_OBSERVE = 5000L;
    public static final String WS_PROTOCOL = "ws://";
    public static final int WS_PORT = 12386;
    public static final String TFTP_PROTOCOL = "tftp://";
    public static final int TFTP_PORT = 9527;
    public static final String LOCAL_LINK_PROXY = "127.0.0.1";
    public static final String REMOTE_LINK_PROXY = "192.168.64.20";
    public static final String LOCAL_ETHER_IP = "192.168.64.10";
    public static final int TRANSCEIVER_ANDROID_SERIAL_PORT = 1;
    public static final int TRANSCEIVER_ANDROID_NETWORK = 2;
    public static final String PARAMETERS_SERIAL_PORT = "serial_port";
    public static final String COM_PREFIX = "/dev/";
    public static final String COM1 = "/dev/ttyS1";
    public static final String COM2 = "/dev/ttyS2";
    public static final String COM3 = "/dev/ttyS3";
    public static final String COM3_USB0 = "/dev/ttyUSB0";
    public static final String COM_USB_CTL = "/dev/ttyUSBDoorCtl";
    public static final int COM3RATE = 921600;
    public static final int DEFAULT_RATE = 115200;
    public static final long CONNECTION_TIMEOUT = 60000L;
    public static final long REQUEST_TIMEOUT = 10000L;
    public static final long SERIAL_TIME_OUT = 60000L;
    public static int NETWORK_TIMEOUT = 30;
    public static final String TFTP_SERVER_PATH = "tftpPath";
    public static final String TFTP_SERVER_IP = "tftpIP";
    public static final String[] SDK_PERMISSION_LIST = new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE", "android.permission.READ_PHONE_STATE"};
    public static final String SENSOR_CONFIG = "sensors.json";

    public static enum BoardType {
        DEFAULT,
        ROBOT,
        EMOTION,
        DOOR,
        LIGHT;
    }

    public static enum LinkType {
        DEFAULT,
        COM,
        COM_COAP,
        COAP,
        HTTP;
    }

    public @interface SerialProtocolVer {
        String VER_1 = "v1.0";
        String VER_2 = "v2.0";
    }
}
```