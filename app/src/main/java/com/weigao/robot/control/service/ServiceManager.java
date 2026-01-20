package com.weigao.robot.control.service;

import android.content.Context;
import android.util.Log;

import com.weigao.robot.control.service.impl.AudioServiceImpl;
import com.weigao.robot.control.service.impl.ChargerServiceImpl;
import com.weigao.robot.control.service.impl.DoorServiceImpl;
import com.weigao.robot.control.service.impl.NavigationServiceImpl;
import com.weigao.robot.control.service.impl.RemoteCallServiceImpl;
import com.weigao.robot.control.service.impl.RobotStateServiceImpl;
import com.weigao.robot.control.service.impl.SecurityServiceImpl;
import com.weigao.robot.control.service.impl.TimingServiceImpl;

/**
 * 服务管理器
 * <p>
 * 统一管理所有服务实例的创建、获取与释放。
 * 采用单例模式，服务实例使用懒加载机制。
 * </p>
 */
public class ServiceManager {

    private static final String TAG = "ServiceManager";

    /** 单例实例 */
    private static volatile ServiceManager instance;

    /** 应用上下文 */
    private Context context;

    /** 是否已初始化 */
    private boolean initialized = false;

    // ==================== 服务实例 ====================

    private volatile INavigationService navigationService;
    private volatile IDoorService doorService;
    private volatile IChargerService chargerService;
    private volatile ISecurityService securityService;
    private volatile ITimingService timingService;
    private volatile IAudioService audioService;
    private volatile IRobotStateService robotStateService;
    private volatile IRemoteCallService remoteCallService;

    // ==================== 锁对象 ====================

    private final Object navigationLock = new Object();
    private final Object doorLock = new Object();
    private final Object chargerLock = new Object();
    private final Object securityLock = new Object();
    private final Object timingLock = new Object();
    private final Object audioLock = new Object();
    private final Object robotStateLock = new Object();
    private final Object remoteCallLock = new Object();

    /**
     * 私有构造函数
     */
    private ServiceManager() {
    }

    /**
     * 获取单例实例
     *
     * @return ServiceManager 实例
     */
    public static ServiceManager getInstance() {
        if (instance == null) {
            synchronized (ServiceManager.class) {
                if (instance == null) {
                    instance = new ServiceManager();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化服务管理器
     * <p>
     * 应在 SDK 初始化成功后调用。
     * </p>
     *
     * @param context 应用上下文
     */
    public void initialize(Context context) {
        if (initialized) {
            Log.w(TAG, "ServiceManager 已初始化，跳过重复初始化");
            return;
        }

        this.context = context.getApplicationContext();
        this.initialized = true;
        Log.i(TAG, "ServiceManager 初始化完成");
    }

    /**
     * 检查是否已初始化
     *
     * @return true=已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取应用上下文
     *
     * @return 上下文
     */
    public Context getContext() {
        return context;
    }

    // ==================== 服务获取方法（懒加载） ====================

    /**
     * 获取导航服务
     *
     * @return 导航服务实例
     */
    public INavigationService getNavigationService() {
        checkInitialized();
        if (navigationService == null) {
            synchronized (navigationLock) {
                if (navigationService == null) {
                    navigationService = new NavigationServiceImpl(context);
                    Log.d(TAG, "NavigationService 已创建");
                }
            }
        }
        return navigationService;
    }

    /**
     * 获取舱门服务
     *
     * @return 舱门服务实例
     */
    public IDoorService getDoorService() {
        checkInitialized();
        if (doorService == null) {
            synchronized (doorLock) {
                if (doorService == null) {
                    doorService = new DoorServiceImpl(context);
                    Log.d(TAG, "DoorService 已创建");
                }
            }
        }
        return doorService;
    }

    /**
     * 获取充电服务
     *
     * @return 充电服务实例
     */
    public IChargerService getChargerService() {
        checkInitialized();
        if (chargerService == null) {
            synchronized (chargerLock) {
                if (chargerService == null) {
                    chargerService = new ChargerServiceImpl(context);
                    Log.d(TAG, "ChargerService 已创建");
                }
            }
        }
        return chargerService;
    }

    /**
     * 获取安全锁定服务
     *
     * @return 安全锁定服务实例
     */
    public ISecurityService getSecurityService() {
        checkInitialized();
        if (securityService == null) {
            synchronized (securityLock) {
                if (securityService == null) {
                    securityService = new SecurityServiceImpl(context);
                    Log.d(TAG, "SecurityService 已创建");
                }
            }
        }
        return securityService;
    }

    /**
     * 获取计时服务
     *
     * @return 计时服务实例
     */
    public ITimingService getTimingService() {
        checkInitialized();
        if (timingService == null) {
            synchronized (timingLock) {
                if (timingService == null) {
                    timingService = new TimingServiceImpl();
                    Log.d(TAG, "TimingService 已创建");
                }
            }
        }
        return timingService;
    }

    /**
     * 获取音频服务
     *
     * @return 音频服务实例
     */
    public IAudioService getAudioService() {
        checkInitialized();
        if (audioService == null) {
            synchronized (audioLock) {
                if (audioService == null) {
                    audioService = new AudioServiceImpl(context);
                    Log.d(TAG, "AudioService 已创建");
                }
            }
        }
        return audioService;
    }

    /**
     * 获取机器人状态服务
     *
     * @return 机器人状态服务实例
     */
    public IRobotStateService getRobotStateService() {
        checkInitialized();
        if (robotStateService == null) {
            synchronized (robotStateLock) {
                if (robotStateService == null) {
                    robotStateService = new RobotStateServiceImpl(context);
                    Log.d(TAG, "RobotStateService 已创建");
                }
            }
        }
        return robotStateService;
    }

    /**
     * 获取远程呼叫服务
     *
     * @return 远程呼叫服务实例
     */
    public IRemoteCallService getRemoteCallService() {
        checkInitialized();
        if (remoteCallService == null) {
            synchronized (remoteCallLock) {
                if (remoteCallService == null) {
                    remoteCallService = new RemoteCallServiceImpl(context);
                    Log.d(TAG, "RemoteCallService 已创建");
                }
            }
        }
        return remoteCallService;
    }

    // ==================== 生命周期管理 ====================

    /**
     * 释放所有服务资源
     */
    public void release() {
        Log.i(TAG, "开始释放所有服务资源...");

        // 释放导航服务
        if (navigationService != null) {
            try {
                navigationService.release();
            } catch (Exception e) {
                Log.e(TAG, "释放 NavigationService 失败", e);
            }
            navigationService = null;
        }

        // 释放舱门服务
        if (doorService != null) {
            try {
                doorService.release();
            } catch (Exception e) {
                Log.e(TAG, "释放 DoorService 失败", e);
            }
            doorService = null;
        }

        // 释放充电服务
        if (chargerService != null) {
            try {
                chargerService.release();
            } catch (Exception e) {
                Log.e(TAG, "释放 ChargerService 失败", e);
            }
            chargerService = null;
        }

        // 释放安全锁定服务
        if (securityService != null) {
            try {
                securityService.release();
            } catch (Exception e) {
                Log.e(TAG, "释放 SecurityService 失败", e);
            }
            securityService = null;
        }

        // 释放计时服务
        if (timingService != null) {
            try {
                timingService.release();
            } catch (Exception e) {
                Log.e(TAG, "释放 TimingService 失败", e);
            }
            timingService = null;
        }

        // 释放音频服务
        if (audioService != null) {
            try {
                audioService.release();
            } catch (Exception e) {
                Log.e(TAG, "释放 AudioService 失败", e);
            }
            audioService = null;
        }

        // 释放机器人状态服务
        if (robotStateService != null) {
            try {
                robotStateService.release();
            } catch (Exception e) {
                Log.e(TAG, "释放 RobotStateService 失败", e);
            }
            robotStateService = null;
        }

        // 释放远程呼叫服务
        if (remoteCallService != null) {
            try {
                remoteCallService.release();
            } catch (Exception e) {
                Log.e(TAG, "释放 RemoteCallService 失败", e);
            }
            remoteCallService = null;
        }

        initialized = false;
        context = null;
        Log.i(TAG, "所有服务资源已释放");
    }

    /**
     * 检查是否已初始化
     *
     * @throws IllegalStateException 未初始化时抛出
     */
    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("ServiceManager 未初始化，请先调用 initialize() 方法");
        }
    }
}
