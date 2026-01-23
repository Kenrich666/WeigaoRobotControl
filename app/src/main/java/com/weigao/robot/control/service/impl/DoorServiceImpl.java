package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.util.Log;

import com.keenon.sdk.component.gating.manager.Door;
import com.keenon.sdk.component.gating.manager.PeanutDoor;
import com.keenon.sdk.component.gating.callback.DoorListener;
import com.keenon.sdk.component.gating.data.GatingType;
import com.keenon.sdk.component.gating.state.GatingState;
import com.keenon.sdk.component.gating.data.Faults;

import com.weigao.robot.control.callback.IDoorCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.model.DoorType;
import com.weigao.robot.control.service.IDoorService;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.sensor.door.SensorDoor;
import com.keenon.sdk.scmIot.protopack.base.ProtoDev;
import com.keenon.sdk.sensor.common.SensorObserver;
import com.keenon.sdk.sensor.common.Event;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorEvent;
import com.keenon.sdk.constant.TopicName;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 舱门服务实现类
 * <p>
 * 封装 Peanut SDK 的 {@code PeanutDoor} 组件，提供舱门控制功能。
 * </p>
 */
public class DoorServiceImpl implements IDoorService {

    private static final String TAG = "DoorServiceImpl";
    private static final String LISTENER_TAG = "DoorServiceImpl_Listener";

    private final Context context;

    /** 回调列表（线程安全） */
    private final List<IDoorCallback> callbacks = new CopyOnWriteArrayList<>();

    /** Peanut SDK 舱门组件 */
    private PeanutDoor peanutDoor;

    /** 舱门数量 */
    private int doorCount = 2;

    /** 脚踩灯光开关门是否启用 */
    private boolean footSwitchEnabled = false;

    /** 到达后自动离开是否启用 */
    private boolean autoLeaveEnabled = false;

    /** 是否已初始化 */
    private boolean initialized = false;

    /** 内部跟踪：所有门是否已打开 (用于 T3 SensorDoor 状态同步) */
    private boolean allDoorsOpen = false;

    public DoorServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        Log.d(TAG, "DoorServiceImpl 已创建");
        initPeanutDoor();
    }

    /**
     * 初始化 PeanutDoor
     */
    private void initPeanutDoor() {
        try {
            peanutDoor = getPeanutDoorInstance();
            // 移除 peanutDoor.init(context) 以避免权限错误 (avc: denied)
            // SDK Facade 模式下不需要应用层直接初始化底层组件

            // 参考 SampleApp，使用 setDoorType 初始化并注册监听
            peanutDoor.setDoorType(Door.SET_TYPE_AUTO, "keenon", mDoorListener);

            // T3 机器人使用 SensorDoor API
            SensorDoor.getInstance().setUSBDirect(true);

            // 注册 SensorDoor 观察者以跟踪实际舱门状态
            SensorDoor.getInstance().addObserver(mSensorDoorObserver);

            // 订阅舱门状态以获取初始状态
            PeanutSDK.getInstance().subscribe(TopicName.DOOR_SWITCH_STATUS, mDoorStatusCallback);

            initialized = true;
            Log.d(TAG, "PeanutDoor 监听器注册成功 (Type=AUTO), SensorDoor 已初始化");
        } catch (Exception e) {
            Log.e(TAG, "PeanutDoor 初始化异常", e);
            initialized = false;
        }
    }

    /**
     * SensorDoor 观察者 - 用于跟踪实际舱门状态变化
     */
    private final SensorObserver mSensorDoorObserver = new SensorObserver() {
        @Override
        public void onUpdate(Event event, Sensor sensor) {
            Log.d(TAG, "SensorDoor onUpdate: " + event.getName());
            if (SensorEvent.SET_DOOR_SWITCH_ACK.equals(event.getName())) {
                // 舱门开关操作已确认
                Log.d(TAG, "SensorDoor 操作已确认");
            }
        }
    };

    /**
     * 舱门状态订阅回调 - 用于获取初始状态和状态更新
     */
    private final IDataCallback mDoorStatusCallback = new IDataCallback() {
        @Override
        public void success(String response) {
            Log.d(TAG, "Door status update: " + response);
            // 解析响应以更新 allDoorsOpen 状态
            // 响应格式可能是 JSON，包含舱门状态信息
            try {
                // 简单解析：如果响应包含 "open" 或者状态值表示打开
                if (response != null && (response.contains("\"open\":true") || response.contains("\"status\":-1"))) {
                    allDoorsOpen = true;
                } else if (response != null
                        && (response.contains("\"open\":false") || response.contains("\"status\":0"))) {
                    allDoorsOpen = false;
                }
                Log.d(TAG, "allDoorsOpen updated to: " + allDoorsOpen);
            } catch (Exception e) {
                Log.e(TAG, "解析舱门状态异常", e);
            }
        }

        @Override
        public void error(com.keenon.sdk.hedera.model.ApiError error) {
            Log.e(TAG, "Door status error: " + error.getMsg());
        }
    };

    /**
     * SDK 舱门回调
     */
    private final DoorListener mDoorListener = new DoorListener() {
        @Override
        public void onFault(Faults type, int doorId) {
            Log.e(TAG, "onFault: type=" + type + ", doorId=" + doorId);
            // 将 Faults 类型转换为错误码
            int errorCode = type != null ? type.ordinal() + 200500 : 200500;
            notifyDoorError(doorId, errorCode);
        }

        @Override
        public void onStateChange(int doorId, int state) {
            Log.d(TAG, "onStateChange: doorId=" + doorId + ", state=" + state);
            notifyDoorStateChanged(doorId, state);
        }

        @Override
        public void onTypeChange(GatingType gatingType) {
            Log.d(TAG, "onTypeChange: " + gatingType);
            DoorType type = convertToDoorType(gatingType);
            if (type != null) {
                doorCount = type.getDoorCount();
            }
            notifyDoorTypeChanged(type);
        }

        @Override
        public void onTypeSetting(boolean success) {
            Log.d(TAG, "onTypeSetting: " + success);
        }

        @Override
        public void onError(int errorCode) {
            Log.e(TAG, "onError: " + errorCode);
        }
    };

    // ==================== 舱门控制 ====================

    @Override
    public void openDoor(int doorId, boolean single, IResultCallback<Void> callback) {
        Log.d(TAG, "openDoor: doorId=" + doorId + ", single=" + single);
        try {
            if (isValidDoorId(doorId)) {
                // T3 机器人使用 SensorDoor API
                int protoDoorId = (doorId == 0) ? ProtoDev.SENSOR_DOOR_1 : ProtoDev.SENSOR_DOOR_2;
                SensorDoor.getInstance().setDoorSwitch(protoDoorId, true);
                Log.d(TAG, "SensorDoor open: protoDoorId=" + protoDoorId);
                notifySuccess(callback);
            } else {
                notifyError(callback, -1, "无效的舱门ID");
            }
        } catch (Exception e) {
            Log.e(TAG, "openDoor 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void closeDoor(int doorId, IResultCallback<Void> callback) {
        Log.d(TAG, "closeDoor: doorId=" + doorId);
        try {
            if (isValidDoorId(doorId)) {
                // T3 机器人使用 SensorDoor API
                int protoDoorId = (doorId == 0) ? ProtoDev.SENSOR_DOOR_1 : ProtoDev.SENSOR_DOOR_2;
                SensorDoor.getInstance().setDoorSwitch(protoDoorId, false);
                Log.d(TAG, "SensorDoor close: protoDoorId=" + protoDoorId);
                notifySuccess(callback);
            } else {
                notifyError(callback, -1, "无效的舱门ID");
            }
        } catch (Exception e) {
            Log.e(TAG, "closeDoor 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void openAllDoors(boolean single, IResultCallback<Void> callback) {
        Log.d(TAG, "openAllDoors: single=" + single);
        try {
            // T3 机器人使用 SensorDoor API 打开所有舱门
            SensorDoor.getInstance().setDoorSwitch(ProtoDev.SENSOR_DOOR_1, true);
            SensorDoor.getInstance().setDoorSwitch(ProtoDev.SENSOR_DOOR_2, true);
            allDoorsOpen = true; // 更新内部状态
            Log.d(TAG, "SensorDoor openAll: DOOR_1 and DOOR_2");
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "openAllDoors 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void closeAllDoors(IResultCallback<Void> callback) {
        Log.d(TAG, "closeAllDoors");
        try {
            // T3 机器人使用 SensorDoor API 关闭所有舱门
            SensorDoor.getInstance().setDoorSwitch(ProtoDev.SENSOR_DOOR_1, false);
            SensorDoor.getInstance().setDoorSwitch(ProtoDev.SENSOR_DOOR_2, false);
            allDoorsOpen = false; // 更新内部状态
            Log.d(TAG, "SensorDoor closeAll: DOOR_1 and DOOR_2");
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "closeAllDoors 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void isAllDoorsClosed(IResultCallback<Boolean> callback) {
        try {
            // 使用内部状态，因为 SensorDoor 没有直接的状态查询方法
            boolean allClosed = !allDoorsOpen;
            Log.d(TAG, "isAllDoorsClosed: " + allClosed + " (internal state)");
            if (callback != null) {
                callback.onSuccess(allClosed);
            }
        } catch (Exception e) {
            Log.e(TAG, "isAllDoorsClosed 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    // ==================== 状态查询 ====================

    @Override
    public void getDoorState(int doorId, IResultCallback<Integer> callback) {
        if (callback != null) {
            try {
                if (peanutDoor != null && isValidDoorId(doorId)) {
                    GatingState state = peanutDoor.getDoorState(doorId);
                    int stateCode = state != null ? state.ordinal() : IDoorCallback.DOOR_STATE_UNKNOWN;
                    callback.onSuccess(stateCode);
                } else {
                    notifyError(callback, -1, "无效的舱门ID或未初始化");
                }
            } catch (Exception e) {
                Log.e(TAG, "getDoorState 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    @Override
    public void getAllDoorStates(IResultCallback<int[]> callback) {
        if (callback != null) {
            try {
                int[] states = new int[doorCount];
                if (peanutDoor != null) {
                    for (int i = 0; i < doorCount; i++) {
                        GatingState state = peanutDoor.getDoorState(i + 1);
                        states[i] = state != null ? state.ordinal() : IDoorCallback.DOOR_STATE_UNKNOWN;
                    }
                }
                callback.onSuccess(states);
            } catch (Exception e) {
                Log.e(TAG, "getAllDoorStates 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    // ==================== 舱门类型 ====================

    @Override
    public void setDoorType(DoorType type, IResultCallback<Void> callback) {
        Log.d(TAG, "setDoorType: " + type);
        try {
            if (peanutDoor != null && type != null) {
                int sdkType = convertToSdkDoorType(type);
                peanutDoor.setDoorType(sdkType, LISTENER_TAG, mDoorListener);
                doorCount = type.getDoorCount();
                notifySuccess(callback);
            } else {
                notifyError(callback, -1, "舱门类型无效或未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "setDoorType 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void getDoorType(IResultCallback<DoorType> callback) {
        if (callback != null) {
            try {
                if (peanutDoor != null) {
                    int typeId = peanutDoor.getDoorType();
                    DoorType type = convertToDoorTypeFromId(typeId);
                    callback.onSuccess(type);
                } else {
                    callback.onSuccess(DoorType.FOUR);
                }
            } catch (Exception e) {
                Log.e(TAG, "getDoorType 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    @Override
    public void supportDoorTypeSetting(IResultCallback<Boolean> callback) {
        if (callback != null) {
            try {
                boolean support = false;
                if (peanutDoor != null) {
                    support = peanutDoor.supportDoorTypeSetting();
                }
                callback.onSuccess(support);
            } catch (Exception e) {
                Log.e(TAG, "supportDoorTypeSetting 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    @Override
    public void getDoorVersion(IResultCallback<String> callback) {
        if (callback != null) {
            try {
                String version = "";
                if (peanutDoor != null) {
                    version = peanutDoor.getDoorVersion();
                }
                callback.onSuccess(version);
            } catch (Exception e) {
                Log.e(TAG, "getDoorVersion 异常", e);
                notifyError(callback, -1, e.getMessage());
            }
        }
    }

    // ==================== 其他配置 ====================

    @Override
    public void setFootSwitchEnabled(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "setFootSwitchEnabled: " + enabled);
        // SDK 文档中未提供光灯开关舱门的 API
        // 本地管理状态，应用层根据此状态处理业务逻辑
        this.footSwitchEnabled = enabled;
        notifySuccess(callback);
    }

    @Override
    public void setAutoLeaveEnabled(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "setAutoLeaveEnabled: " + enabled);
        // SDK 文档中未提供自动离开的 API
        // 本地管理状态，应用层根据此状态处理业务逻辑
        this.autoLeaveEnabled = enabled;
        notifySuccess(callback);
    }

    // ==================== 回调注册 ====================

    @Override
    public void registerCallback(IDoorCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
            Log.d(TAG, "回调已注册，当前数量：" + callbacks.size());
        }
    }

    @Override
    public void unregisterCallback(IDoorCallback callback) {
        if (callbacks.remove(callback)) {
            Log.d(TAG, "回调已注销，当前数量：" + callbacks.size());
        }
    }

    @Override
    public void release() {
        Log.d(TAG, "释放 DoorService 资源");
        callbacks.clear();
        if (peanutDoor != null) {
            try {
                peanutDoor.removeFloorListener(LISTENER_TAG);
                peanutDoor.release();
            } catch (Exception e) {
                Log.e(TAG, "释放 peanutDoor 异常", e);
            }
        }
        initialized = false;
    }

    // ==================== 回调分发 ====================

    private void notifyDoorStateChanged(int doorId, int state) {
        for (IDoorCallback callback : callbacks) {
            try {
                callback.onDoorStateChanged(doorId, state);
            } catch (Exception e) {
                Log.e(TAG, "回调 onDoorStateChanged 异常", e);
            }
        }
    }

    private void notifyDoorTypeChanged(DoorType type) {
        for (IDoorCallback callback : callbacks) {
            try {
                callback.onDoorTypeChanged(type);
            } catch (Exception e) {
                Log.e(TAG, "回调 onDoorTypeChanged 异常", e);
            }
        }
    }

    private void notifyDoorError(int doorId, int errorCode) {
        for (IDoorCallback callback : callbacks) {
            try {
                callback.onDoorError(doorId, errorCode);
            } catch (Exception e) {
                Log.e(TAG, "回调 onDoorError 异常", e);
            }
        }
    }

    // ==================== 类型转换 ====================

    private DoorType convertToDoorType(GatingType gatingType) {
        if (gatingType == null)
            return DoorType.FOUR;
        switch (gatingType) {
            case FOUR:
                return DoorType.FOUR;
            case DOUBLE:
                return DoorType.DOUBLE;
            case THREE:
                return DoorType.THREE;
            case THREE_REVERSE:
                return DoorType.THREE_REVERSE;
            default:
                return DoorType.FOUR;
        }
    }

    private DoorType convertToDoorTypeFromId(int typeId) {
        switch (typeId) {
            case 0:
                return DoorType.FOUR;
            case 1:
                return DoorType.DOUBLE;
            case 2:
                return DoorType.THREE;
            case 3:
                return DoorType.THREE_REVERSE;
            default:
                return DoorType.FOUR;
        }
    }

    private int convertToSdkDoorType(DoorType type) {
        switch (type) {
            case FOUR:
                return Door.SET_TYPE_FOUR;
            case DOUBLE:
                return Door.SET_TYPE_DOUBLE;
            case THREE:
                return Door.SET_TYPE_THREE;
            case THREE_REVERSE:
                return Door.SET_TYPE_THREE_REV;
            default:
                return Door.SET_TYPE_FOUR;
        }
    }

    // ==================== 辅助方法 ====================

    private boolean isValidDoorId(int doorId) {
        // SDK ID 是 0-based
        return doorId >= 0 && doorId < doorCount;
    }

    private void notifySuccess(IResultCallback<Void> callback) {
        if (callback != null) {
            Log.d(TAG, "notifySuccess: 执行成功");
            callback.onSuccess(null);
        }
    }

    private void notifyError(IResultCallback<?> callback, int code, String message) {
        if (callback != null) {
            Log.e(TAG, "notifyError: code=" + code + ", message=" + message);
            callback.onError(new ApiError(code, message));
        }
    }

    /**
     * 获取 PeanutDoor 单例的工厂方法。
     * <p>
     * 设计为 protected 以便在测试子类中重写并注入 Mock 对象。
     * </p>
     */
    protected PeanutDoor getPeanutDoorInstance() {
        return PeanutDoor.getInstance();
    }
}
