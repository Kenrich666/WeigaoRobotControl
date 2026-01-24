```
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.component.navigation;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import androidx.annotation.NonNull;
import com.keenon.common.error.PeanutError;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.PeanutMainThreadExecutor;
import com.keenon.sdk.api.NavigationStatusApi;
import com.keenon.sdk.component.navigation.arrival.ArrivalControl;
import com.keenon.sdk.component.navigation.common.Navigation;
import com.keenon.sdk.component.navigation.common.NavigationFsm;
import com.keenon.sdk.component.navigation.route.RouteLine;
import com.keenon.sdk.component.navigation.route.RouteNode;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import java.util.concurrent.CopyOnWriteArraySet;

public class NavigationImpl implements Navigation, Handler.Callback, RouteLine.Listener, NavigationFsm.Listener, ArrivalControl.Listener {
    private static final String TAG = "[NavigationImpl]";
    private static final int MSG_PREPARE = 1;
    private static final int MSG_SET_PILOT_WHEN_READY = 2;
    private static final int MSG_DO_SOME_WORK = 3;
    private static final int MSG_DESTINATION = 4;
    private static final int MSG_STOP = 5;
    private static final int MSG_RESET = 6;
    private static final int MSG_RELEASE = 7;
    private static final int MSG_BLOCKED = 8;
    private static final int MSG_MANUAL = 9;
    private static final int MSG_SET_BLOCK_TIMEOUT = 10;
    private static final int REPEAT_INFINITE = -1;
    private static volatile NavigationImpl sInstance;
    private final Handler handler;
    private final HandlerThread navigationThread;
    private final CopyOnWriteArraySet<Navigation.Listener> listeners;
    private NavigationFsm navigationFsm;
    private ArrivalControl arrivalControl;
    private RouteLine routeLine;
    private RouteNode routeNodeRunning;
    private boolean released;
    private boolean pilotWhenReady;
    private boolean isArrivalEffect;
    private boolean isTakeControl;
    private int state;
    private int index;
    private int repeat;
    private int schedule;
    private PeanutMainThreadExecutor mainThreadExecutor;
    private IDataCallback mStatusCallBack = new IDataCallback() {
        public void success(String response) {
            NavigationStatusApi.Bean data = (NavigationStatusApi.Bean)GsonUtil.gson2Bean(response, NavigationStatusApi.Bean.class);
            LogUtils.d("[NavigationImpl]", "[mStatusCallBack-onSuccess][bean = " + response + "]");
            if (data != null && data.getData() != null) {
                NavigationImpl.this.schedule = data.getData().getSchedule();
                NavigationImpl.this.handleRawStatus(data.getData().getStatus());
                if (NavigationImpl.this.routeNodeRunning.getNavigationInfo().getRemainDistance() == null || !NavigationImpl.this.routeNodeRunning.getNavigationInfo().getRemainDistance().equals(data.getData().getRemain_length())) {
                    NavigationImpl.this.arrivalControl.notifyDistanceChanged(data.getData().getRemain_length());
                }

                NavigationImpl.this.updateNavigationInfo(data.getData());
            } else {
                NavigationImpl.this.notifyError(203172);
            }
        }

        public void error(ApiError error) {
            LogUtils.e("[NavigationImpl]", "[navigation-status][error][" + error.toString() + "]");
            NavigationImpl.this.notifyError(error.getCode());
        }
    };

    NavigationImpl(ArrivalControl arrivalControl) {
        if (arrivalControl == null) {
            this.notifyError(203409);
            throw new RuntimeException("到达策略为空");
        } else {
            this.pilotWhenReady = false;
            this.index = 0;
            this.repeat = 0;
            this.state = 0;
            this.listeners = new CopyOnWriteArraySet();
            this.arrivalControl = arrivalControl;
            this.routeNodeRunning = new RouteNode();
            this.navigationFsm = new NavigationFsm();
            this.navigationFsm.addListener(this);
            arrivalControl.addListener(this);
            this.navigationThread = new HandlerThread("NavigationImpl:Handler", -16);
            this.navigationThread.start();
            this.handler = new Handler(this.navigationThread.getLooper(), this);
            this.mainThreadExecutor = PeanutSDK.getInstance().getMainThreadExecutor();
        }
    }

    public static NavigationImpl getInstance(ArrivalControl arrivalControl) {
        if (sInstance == null) {
            synchronized(NavigationImpl.class) {
                if (sInstance == null) {
                    sInstance = new NavigationImpl(arrivalControl);
                }
            }
        }

        return sInstance;
    }

    public void addListener(Navigation.Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Navigation.Listener listener) {
        this.listeners.remove(listener);
    }

    public void prepare(RouteLine routeLine) {
        if (routeLine != null && routeLine.getLength() != 0) {
            this.routeLine = routeLine;
            this.routeLine.addListener(this);
            this.prepareInternal(routeLine);
        } else {
            this.notifyError(203405);
        }
    }

    public void setPilotWhenReady(boolean pilotWhenReady) {
        if (this.pilotWhenReady != pilotWhenReady) {
            this.pilotWhenReady = pilotWhenReady;
            LogUtils.d("NAVI--", "[NavigationImpl][setPilotWhenReady][pilotWhenReady->" + pilotWhenReady + "]");
            this.handler.obtainMessage(2, pilotWhenReady ? 1 : 0, 0).sendToTarget();
        }

    }

    public void setTaskControl(boolean isTaskControl) {
        this.isTakeControl = isTaskControl;
        LogUtils.d("NAVI--", "[NavigationImpl][setTaskControl][isTaskControl->" + isTaskControl + "]");
    }

    public void pilotNext() {
        LogUtils.d("NAVI--", "[NavigationImpl][pilotNext]");
        this.prepareNextRouteNode();
    }

    public RouteNode[] getRouteNodes() {
        return this.routeLine.getRouteNodes();
    }

    public RouteNode getCurrentNode() {
        return this.routeNodeRunning;
    }

    public RouteLine getRouteLine() {
        return this.routeLine;
    }

    public RouteNode getNextNode() {
        LogUtils.d("[NavigationImpl]", "getNextNode :  repeat = " + this.repeat + " , index = " + this.index);
        if (!this.isInfiniteRepeat() && this.repeat >= this.routeLine.getRepeatCount()) {
            return null;
        } else {
            return this.isLastRepeat() && this.index + 1 >= this.routeLine.getLength() ? null : this.routeLine.getRouteNodes()[(this.index + 1) % this.routeLine.getLength()];
        }
    }

    private boolean isInfiniteRepeat() {
        return this.routeLine.getRepeatCount() == -1;
    }

    public int getCurrentPosition() {
        return this.index;
    }

    public void stop() {
        this.notifyFsm(7);
    }

    public void manual(int direction) {
        this.handler.obtainMessage(9, direction, 0).sendToTarget();
    }

    public void setSpeed(int speed) {
        this.setSpeedInternal(speed);
    }

    public void setStableMode(int mode) {
        this.setStableModeInternal(mode);
    }

    public void setSportMode(int mode) {
        this.setSportModeInternal(mode);
    }

    public void setBlockTimeout(int timeout) {
        this.handler.obtainMessage(10, timeout).sendToTarget();
    }

    public synchronized void release() {
        if (!this.released) {
            this.handler.sendEmptyMessage(7);
        }
    }

    public void skipTo(int index) {
        this.index = index;
    }

    public void onArrived() {
        LogUtils.d("NAVI--", "[NavigationImpl][onArrived][近似到达]");
        this.isArrivalEffect = true;
        this.notifyFsm(3);
    }

    public void onRoutePrepared() {
        this.notifyRoutePrepared();
    }

    public void onRouteError(int code) {
        LogUtils.d("NAVI--", "[NavigationImpl][onRouteError][error " + code + "]");
        this.notifyError(code);
    }

    public void onStateSafely(int state) {
        this.setState(state);
    }

    public boolean handleMessage(@NonNull Message msg) {
        try {
            switch (msg.what) {
                case 1:
                    this.prepareInternal((RouteLine)msg.obj);
                    return true;
                case 2:
                    this.setPlayWhenReadyInternal(msg.arg1 != 0);
                    return true;
                case 3:
                    this.doSomeWork();
                    return true;
                case 4:
                    this.arrivalInternal();
                    return true;
                case 5:
                    this.stopInternal();
                    return true;
                case 6:
                    this.resetInternal();
                    return true;
                case 7:
                    this.releaseInternal();
                    return true;
                case 8:
                    this.blockInternal();
                    return true;
                case 9:
                    this.manualInternal(msg.arg1);
                    return true;
                case 10:
                    this.setBlockTimeoutInternal((Integer)msg.obj);
                    return true;
                default:
                    return false;
            }
        } catch (RuntimeException e) {
            LogUtils.e("NAVI--", "[NavigationImpl]", e);
            this.resetInternal();
            return true;
        }
    }

    private void setState(int state) {
        if (this.state != state) {
            this.state = state;
            this.handleState(state);
            this.notifyStateChanged(state);
        }

    }

    private void handleState(int state) {
        switch (state) {
            case 0:
            case 2:
            case 4:
            default:
                break;
            case 1:
                this.handler.sendEmptyMessage(3);
                break;
            case 3:
                this.handler.sendEmptyMessage(4);
                break;
            case 5:
            case 6:
                this.handler.sendEmptyMessage(8);
                break;
            case 7:
                this.handler.sendEmptyMessage(5);
        }

    }

    private synchronized void notifyStateChanged(final int state) {
        for(final Navigation.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() {
                public void run() {
                    listener.onStateChanged(state, NavigationImpl.this.schedule);
                }
            });
        }

    }

    private synchronized void notifyNodeChanged() {
        for(final Navigation.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() {
                public void run() {
                    listener.onRouteNode(NavigationImpl.this.index, NavigationImpl.this.routeNodeRunning);
                }
            });
        }

    }

    private void notifyRoutePrepared() {
        for(final Navigation.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() {
                public void run() {
                    listener.onRoutePrepared(NavigationImpl.this.getRouteNodes());
                }
            });
        }

    }

    private synchronized void notifyDistanceChanged(final float distance) {
        for(final Navigation.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() {
                public void run() {
                    listener.onDistanceChanged(distance);
                }
            });
        }

    }

    private void notifyError(final int error) {
        this.resetInternal();

        for(final Navigation.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() {
                public void run() {
                    listener.onError(error);
                }
            });
        }

    }

    private void notifyEvent(final int event) {
        for(final Navigation.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() {
                public void run() {
                    listener.onEvent(event);
                }
            });
        }

    }

    private void notifyFsm(int event) {
        if (null != this.navigationFsm) {
            this.navigationFsm.process(event);
        } else {
            this.notifyError(203407);
        }

    }

    private void prepareInternal(RouteLine routeLine) {
        LogUtils.d("NAVI--", "[NavigationImpl][prepareInternal]");
        routeLine.prepare();
    }

    private void setPlayWhenReadyInternal(boolean pilotWhenReady) {
        LogUtils.d("NAVI--", "[NavigationImpl][doSomeWork][setPlayWhenReadyInternal->" + pilotWhenReady + "]");
        if (!pilotWhenReady) {
            if (this.state == 2 || this.state == 1) {
                this.motionPause();
            }
        } else if (this.state == 0) {
            this.notifyFsm(1);
        } else {
            this.motionContinue();
        }

    }

    private void doSomeWork() {
        LogUtils.d("NAVI--", "[NavigationImpl][doSomeWork][pilotWhenReady->" + this.pilotWhenReady + "]");
        if (this.pilotWhenReady) {
            this.prepareRouteNode();
            PeanutSDK.getInstance().subscribe("NavigationStatusApi", this.mStatusCallBack);
            this.navigate();
        } else {
            LogUtils.d("NAVI--", "[NavigationImpl][doSomeWork][pilotWhenReady not set]");
        }

    }

    private void prepareRouteNode() {
        if (this.index == Integer.MIN_VALUE) {
            this.index = 0;
        }

        if (this.repeat == Integer.MIN_VALUE) {
            this.repeat = 0;
        }

        this.isArrivalEffect = false;

        try {
            this.routeNodeRunning = this.routeLine.getRouteNodes()[this.index];
            this.notifyNodeChanged();
        } catch (IndexOutOfBoundsException e) {
            this.notifyError(PeanutError.getExceptionCode(e));
        }

    }

    private void prepareNextRouteNode() {
        if (this.repeat >= this.routeLine.getRepeatCount() && !this.isInfiniteRepeat()) {
            this.notifyError(203411);
        } else {
            ++this.index;
            if (this.index >= this.routeLine.getLength()) {
                this.index %= this.routeLine.getLength();
                if (!this.isInfiniteRepeat()) {
                    ++this.repeat;
                }
            }

            LogUtils.d("[NavigationImpl]", "index : " + this.index + "  repeat :" + this.repeat);
            this.routeNodeRunning = this.routeLine.getRouteNodes()[this.index];
            this.pilotWhenReady = true;
            this.notifyFsm(1);
        }
    }

    private void navigate() {
        PeanutSDK.getInstance().navigation().setTarget(new IDataCallback() {
            public void success(String response) {
                LogUtils.d("NAVI--", "[NavigationImpl][navigate-onSuccess][bean = " + response + "]");
                ApiData data = (ApiData)GsonUtil.gson2Bean(response, ApiData.class);
                if (null != data) {
                    if (data.getCode() == 0) {
                        NavigationImpl.this.notifyEvent(100);
                    } else if (data.getCode() == 35) {
                        NavigationImpl.this.notifyError(203405);
                    } else if (data.getCode() == 81) {
                        NavigationImpl.this.notifyError(203406);
                    } else if (data.getCode() == 19) {
                        NavigationImpl.this.notifyError(203408);
                    }
                }

            }

            public void error(ApiError error) {
                NavigationImpl.this.notifyError(error.getCode());
            }
        }, this.routeNodeRunning.getId(), this.isTakeControl ? 1 : 0);
    }

    private void arrivalInternal() {
        LogUtils.d("NAVI--", "[NavigationImpl][arrivalInternal]");
        this.arrivalSafely();
        this.notifyFsm(0);
    }

    private void arrivalSafely() {
        if (this.isArrivalEffect) {
            this.motionStop();
        }

        this.motionReset();
        this.arrivalControl.release();
    }

    private void setSpeedInternal(int speed) {
        LogUtils.d("NAVI--", "[NavigationImpl][setSpeedInternal][" + speed + "]");
        PeanutSDK.getInstance().navigation().setSpeed(new IDataCallback() {
            public void success(String result) {
            }

            public void error(ApiError error) {
            }
        }, speed);
    }

    private void setStableModeInternal(int mode) {
        LogUtils.d("NAVI--", "[NavigationImpl][setModeInternal][" + mode + "]");
        PeanutSDK.getInstance().navigation().setStableMode((IDataCallback)null, mode);
    }

    private void setSportModeInternal(int mode) {
        LogUtils.d("NAVI--", "[NavigationImpl][setSportModeInternal][" + mode + "]");
        PeanutSDK.getInstance().navigation().setSportMode((IDataCallback)null, mode);
    }

    private void setBlockTimeoutInternal(int timeout) {
        LogUtils.d("NAVI--", "[NavigationImpl][setBlockTimeoutInternal][" + timeout + "]");
        PeanutSDK.getInstance().navigation().setBlockTimeout(new IDataCallback() {
            public void success(String result) {
                LogUtils.d("NAVI--", "[NavigationImpl][setBlockTimeoutInternal Success]");
            }

            public void error(ApiError error) {
                LogUtils.e("NAVI--", "[NavigationImpl][setBlockTimeoutInternal Error][" + error.toString() + "]");
            }
        }, timeout);
    }

    private void blockInternal() {
        this.arrivalControl.notifyBlocked();
    }

    private void stopInternal() {
        LogUtils.d("NAVI--", "[NavigationImpl][stopInternal]");
        this.motionStop();
    }

    private void manualInternal(int direction) {
        PeanutSDK.getInstance().motor().hrc((IDataCallback)null, false);
        switch (direction) {
            case 1:
                PeanutSDK.getInstance().motor().forward((IDataCallback)null);
                break;
            case 2:
                PeanutSDK.getInstance().motor().backward((IDataCallback)null);
                break;
            case 3:
                PeanutSDK.getInstance().motor().turnLeft((IDataCallback)null);
                break;
            case 4:
                PeanutSDK.getInstance().motor().turnRight((IDataCallback)null);
        }

    }

    private void releaseInternal() {
        LogUtils.d("NAVI--", "[NavigationImpl][releaseInternal]");
        this.resetInternal();
        this.setState(0);

        while(!this.released) {
            try {
                this.wait();
            } catch (InterruptedException var4) {
                Thread.currentThread().interrupt();
            }
        }

        this.navigationThread.quit();
        synchronized(this) {
            this.released = true;
            this.notifyAll();
        }
    }

    private void resetInternal() {
        LogUtils.d("NAVI--", "[NavigationImpl][resetInternal]");
        PeanutSDK.getInstance().unSubscribe("NavigationStatusApi", this.mStatusCallBack);
        this.handler.removeCallbacksAndMessages((Object)null);
        this.arrivalSafely();
        this.pilotWhenReady = false;
        this.isTakeControl = false;
        this.index = 0;
        this.repeat = 0;
    }

    private void updateNavigationInfo(NavigationStatusApi.Bean.DataBean data) {
        if (this.routeNodeRunning.getNavigationInfo().getRemainDistance() > 0.0F && this.routeNodeRunning.getNavigationInfo().getRemainDistance() != data.getRemain_length()) {
            this.notifyDistanceChanged(data.getRemain_length());
        }

        this.routeNodeRunning.getNavigationInfo().setStatus(data.getStatus());
        this.routeNodeRunning.getNavigationInfo().setSchedule(data.getSchedule());
        this.routeNodeRunning.getNavigationInfo().setDestinationDesc(data.getDesc());
        this.routeNodeRunning.getNavigationInfo().setRemainDistance(data.getRemain_length());
        this.routeNodeRunning.getNavigationInfo().setRemainTime(data.getRemain_time());
        this.routeNodeRunning.getNavigationInfo().setTotalDistance(data.getTotal_length());
        this.routeNodeRunning.getNavigationInfo().setTotalTime(data.getTotal_time());
        this.notifyNodeChanged();
    }

    private void motionPause() {
        PeanutSDK.getInstance().navigation().pause(new IDataCallback() {
            public void success(String response) {
                ApiData data = (ApiData)GsonUtil.gson2Bean(response, ApiData.class);
                if (null != data && data.getStatus() == 0) {
                    LogUtils.d("[NavigationImpl]", "[motionPause][success]");
                    NavigationImpl.this.arrivalControl.release();
                    NavigationImpl.this.notifyFsm(4);
                }

            }

            public void error(ApiError error) {
                NavigationImpl.this.notifyError(error.getCode());
            }
        });
    }

    private void motionContinue() {
        PeanutSDK.getInstance().navigation().resume(new IDataCallback() {
            public void success(String response) {
                ApiData data = (ApiData)GsonUtil.gson2Bean(response, ApiData.class);
                if (null != data && data.getStatus() == 0) {
                    LogUtils.d("[NavigationImpl]", "[motionContinue][success]");
                    NavigationImpl.this.notifyFsm(2);
                }

            }

            public void error(ApiError error) {
                NavigationImpl.this.notifyError(error.getCode());
            }
        });
    }

    private void motionStop() {
        PeanutSDK.getInstance().navigation().stop(new IDataCallback() {
            public void success(String response) {
                ApiData data = (ApiData)GsonUtil.gson2Bean(response, ApiData.class);
                if (null != data && data.getStatus() == 0) {
                    LogUtils.d("[NavigationImpl]", "[motionStop][success]");
                }

            }

            public void error(ApiError error) {
                NavigationImpl.this.notifyError(error.getCode());
            }
        });
    }

    private void motionReset() {
        PeanutSDK.getInstance().navigation().reset(new IDataCallback() {
            public void success(String response) {
                ApiData data = (ApiData)GsonUtil.gson2Bean(response, ApiData.class);
                if (null != data && data.getStatus() == 0) {
                    LogUtils.d("[NavigationImpl]", "[motionReset][success]");
                }

            }

            public void error(ApiError error) {
                LogUtils.e("[NavigationImpl]", "[motionReset][error][" + error.toString() + "]");
            }
        });
    }

    private void handleRawStatus(int status) {
        switch (status) {
            case 25:
            case 66:
                this.notifyFsm(6);
                break;
            case 26:
            case 67:
                this.notifyFsm(5);
                break;
            case 68:
                this.notifyFsm(3);
                break;
            case 69:
                this.notifyFsm(10);
                break;
            case 73:
                if (this.state != 1) {
                    this.notifyFsm(0);
                }
                break;
            case 79:
                this.notifyFsm(9);
                break;
            case 82:
                this.notifyFsm(2);
        }

    }

    public void cancelArriveControl() {
        this.arrivalControl.release();
    }

    public void resetNewTargets() {
        this.resetInternal();
    }

    public boolean isLastRepeat() {
        if (this.isInfiniteRepeat()) {
            return false;
        } else {
            return this.repeat + 1 >= this.routeLine.getRepeatCount();
        }
    }

    public void arrivalControlEnable(boolean enable) {
        this.arrivalControl.enable(enable);
    }
}
```