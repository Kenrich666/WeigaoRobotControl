package com.weigao.robot.control.service.impl;

import android.content.Context;
import android.util.Log;

import com.keenon.peanut.api.PeanutDoor;
import com.keenon.peanut.api.callback.Door;
import com.keenon.peanut.api.entity.GatingType;
import com.keenon.peanut.api.entity.GatingState;
import com.keenon.peanut.api.entity.Faults;

import com.weigao.robot.control.callback.IDoorCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.model.DoorType;
import com.weigao.robot.control.service.IDoorService;

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
    private int doorCount = 4;

    /** 脚踩灯光开关门是否启用 */
    private boolean footSwitchEnabled = false;

    /** 到达后自动离开是否启用 */
    private boolean autoLeaveEnabled = false;

    /** 是否已初始化 */
    private boolean initialized = false;

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
            peanutDoor = PeanutDoor.getInstance();
            peanutDoor.init(context);
            peanutDoor.setDoorListerner(LISTENER_TAG, mDoorListener);
            initialized = true;
            Log.d(TAG, "PeanutDoor 初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "PeanutDoor 初始化异常", e);
            initialized = false;
        }
    }

    /**
     * SDK 舱门回调
     */
    private final Door.DoorListener mDoorListener = new Door.DoorListener() {
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
            if (peanutDoor != null && isValidDoorId(doorId)) {
                peanutDoor.openDoor(doorId, single);
                notifySuccess(callback);
            } else {
                notifyError(callback, -1, "无效的舱门ID或未初始化");
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
            if (peanutDoor != null && isValidDoorId(doorId)) {
                peanutDoor.closeDoor(doorId);
                notifySuccess(callback);
            } else {
                notifyError(callback, -1, "无效的舱门ID或未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "closeDoor 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void closeAllDoors(IResultCallback<Void> callback) {
        Log.d(TAG, "closeAllDoors");
        try {
            if (peanutDoor != null) {
                peanutDoor.closeAllDoor();
            }
            notifySuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "closeAllDoors 异常", e);
            notifyError(callback, -1, e.getMessage());
        }
    }

    @Override
    public void isAllDoorsClosed(IResultCallback<Boolean> callback) {
        try {
            boolean allClosed = false;
            if (peanutDoor != null) {
                allClosed = peanutDoor.isAllDoorClose();
            }
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

    // ==================== 地面投影灯控制 ====================

    @Override
    public void setFootSwitchEnabled(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "setFootSwitchEnabled: " + enabled);
        this.footSwitchEnabled = enabled;
        // TODO: 调用 SDK 对应的方法（如果存在）
        notifySuccess(callback);
    }

    @Override
    public void setAutoLeaveEnabled(boolean enabled, IResultCallback<Void> callback) {
        Log.d(TAG, "setAutoLeaveEnabled: " + enabled);
        this.autoLeaveEnabled = enabled;
        // TODO: 调用 SDK 对应的方法（如果存在）
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
            case Four:
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
        return doorId >= 1 && doorId <= doorCount;
    }

    private void notifySuccess(IResultCallback<Void> callback) {
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    private void notifyError(IResultCallback<?> callback, int code, String message) {
        if (callback != null) {
            callback.onError(new ApiError(code, message));
        }
    }
}
