package com.weigao.robot.control.service;

import android.content.Context;
import android.util.Log;

import com.weigao.robot.control.service.impl.AudioServiceImpl;
import com.weigao.robot.control.service.impl.ChargerServiceImpl;
import com.weigao.robot.control.service.impl.ChargingRuntimeBridge;
import com.weigao.robot.control.service.impl.DoorServiceImpl;
import com.weigao.robot.control.service.impl.NavigationServiceImpl;
import com.weigao.robot.control.service.impl.RemoteCallServiceImpl;
import com.weigao.robot.control.service.impl.RobotStateServiceImpl;
import com.weigao.robot.control.service.impl.SecurityServiceImpl;
import com.weigao.robot.control.service.impl.StabilizedDoorServiceImpl;
import com.weigao.robot.control.service.impl.TimingServiceImpl;

/**
 * Central service locator for the application.
 */
public class ServiceManager {

    private static final String TAG = "ServiceManager";

    private static volatile ServiceManager instance;

    private Context context;
    private boolean initialized = false;

    private volatile INavigationService navigationService;
    private volatile IDoorService doorService;
    private volatile IChargerService chargerService;
    private volatile ISecurityService securityService;
    private volatile ITimingService timingService;
    private volatile IAudioService audioService;
    private volatile IRobotStateService robotStateService;
    private volatile IRemoteCallService remoteCallService;
    private volatile ChargingRuntimeBridge chargingRuntimeBridge;

    private final Object navigationLock = new Object();
    private final Object doorLock = new Object();
    private final Object chargerLock = new Object();
    private final Object securityLock = new Object();
    private final Object timingLock = new Object();
    private final Object audioLock = new Object();
    private final Object robotStateLock = new Object();
    private final Object remoteCallLock = new Object();
    private final Object chargingRuntimeBridgeLock = new Object();

    private ServiceManager() {
    }

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

    public void initialize(Context context) {
        if (initialized) {
            Log.w(TAG, "ServiceManager already initialized, skip");
            return;
        }

        this.context = context.getApplicationContext();
        this.initialized = true;
        Log.i(TAG, "ServiceManager initialized");
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Context getContext() {
        return context;
    }

    public INavigationService getNavigationService() {
        checkInitialized();
        if (navigationService == null) {
            synchronized (navigationLock) {
                if (navigationService == null) {
                    navigationService = new NavigationServiceImpl(context);
                    Log.d(TAG, "NavigationService created");
                }
            }
        }
        return navigationService;
    }

    public IDoorService getDoorService() {
        checkInitialized();
        if (doorService == null) {
            synchronized (doorLock) {
                if (doorService == null) {
                    doorService = new StabilizedDoorServiceImpl(context);
                    Log.d(TAG, "DoorService created");
                }
            }
        }
        return doorService;
    }

    public IChargerService getChargerService() {
        checkInitialized();
        if (chargerService == null) {
            synchronized (chargerLock) {
                if (chargerService == null) {
                    chargerService = new ChargerServiceImpl(getChargingRuntimeBridge());
                    Log.d(TAG, "ChargerService created");
                }
            }
        }
        return chargerService;
    }

    public ISecurityService getSecurityService() {
        checkInitialized();
        if (securityService == null) {
            synchronized (securityLock) {
                if (securityService == null) {
                    securityService = new SecurityServiceImpl(context);
                    Log.d(TAG, "SecurityService created");
                }
            }
        }
        return securityService;
    }

    public ITimingService getTimingService() {
        checkInitialized();
        if (timingService == null) {
            synchronized (timingLock) {
                if (timingService == null) {
                    timingService = new TimingServiceImpl();
                    Log.d(TAG, "TimingService created");
                }
            }
        }
        return timingService;
    }

    public IAudioService getAudioService() {
        checkInitialized();
        if (audioService == null) {
            synchronized (audioLock) {
                if (audioService == null) {
                    audioService = new AudioServiceImpl(context);
                    Log.d(TAG, "AudioService created");
                }
            }
        }
        return audioService;
    }

    public IRobotStateService getRobotStateService() {
        checkInitialized();
        if (robotStateService == null) {
            synchronized (robotStateLock) {
                if (robotStateService == null) {
                    robotStateService = new RobotStateServiceImpl(context, getChargingRuntimeBridge());
                    Log.d(TAG, "RobotStateService created");
                }
            }
        }
        return robotStateService;
    }

    public IRemoteCallService getRemoteCallService() {
        checkInitialized();
        if (remoteCallService == null) {
            synchronized (remoteCallLock) {
                if (remoteCallService == null) {
                    remoteCallService = new RemoteCallServiceImpl(context, getNavigationService());
                    Log.d(TAG, "RemoteCallService created");
                }
            }
        }
        return remoteCallService;
    }

    public void release() {
        Log.i(TAG, "Releasing all services");

        if (remoteCallService != null) {
            try {
                remoteCallService.release();
            } catch (Exception e) {
                Log.e(TAG, "Release RemoteCallService failed", e);
            }
            remoteCallService = null;
        }

        if (timingService != null) {
            try {
                timingService.release();
            } catch (Exception e) {
                Log.e(TAG, "Release TimingService failed", e);
            }
            timingService = null;
        }

        if (securityService != null) {
            try {
                securityService.release();
            } catch (Exception e) {
                Log.e(TAG, "Release SecurityService failed", e);
            }
            securityService = null;
        }

        if (robotStateService != null) {
            try {
                robotStateService.release();
            } catch (Exception e) {
                Log.e(TAG, "Release RobotStateService failed", e);
            }
            robotStateService = null;
        }

        if (chargerService != null) {
            try {
                chargerService.release();
            } catch (Exception e) {
                Log.e(TAG, "Release ChargerService failed", e);
            }
            chargerService = null;
        }

        if (audioService != null) {
            try {
                audioService.release();
            } catch (Exception e) {
                Log.e(TAG, "Release AudioService failed", e);
            }
            audioService = null;
        }

        if (chargingRuntimeBridge != null) {
            try {
                chargingRuntimeBridge.release();
            } catch (Exception e) {
                Log.e(TAG, "Release ChargingRuntimeBridge failed", e);
            }
            chargingRuntimeBridge = null;
        }

        if (doorService != null) {
            try {
                doorService.release();
            } catch (Exception e) {
                Log.e(TAG, "Release DoorService failed", e);
            }
            doorService = null;
        }

        if (navigationService != null) {
            try {
                navigationService.release();
            } catch (Exception e) {
                Log.e(TAG, "Release NavigationService failed", e);
            }
            navigationService = null;
        }

        initialized = false;
        context = null;
        Log.i(TAG, "All services released");
    }

    private ChargingRuntimeBridge getChargingRuntimeBridge() {
        checkInitialized();
        if (chargingRuntimeBridge == null) {
            synchronized (chargingRuntimeBridgeLock) {
                if (chargingRuntimeBridge == null) {
                    chargingRuntimeBridge = new ChargingRuntimeBridge(context);
                    Log.d(TAG, "ChargingRuntimeBridge created");
                }
            }
        }
        return chargingRuntimeBridge;
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("ServiceManager not initialized");
        }
    }
}
