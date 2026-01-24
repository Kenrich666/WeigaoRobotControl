```
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.component.navigation.arrival;

import android.os.Handler;
import android.os.Looper;
import com.keenon.common.utils.LogUtils;
import java.util.Timer;
import java.util.TimerTask;

public class DefaultArrivalControl implements ArrivalControl {
    private ArrivalControl.Listener listener;
    private boolean enable;
    private final float marginScope;
    private final int blockDelay;
    private final int scopeDelay;
    private boolean blocked;
    private boolean destinationNearby;
    private Timer scopeTimer;
    private Timer blockTimer;
    private TimerTask scopeTimerTask;
    private TimerTask blockTimerTask;

    public DefaultArrivalControl() {
        this(false, 1.0F, 5000, 10000);
    }

    public DefaultArrivalControl(boolean enable) {
        this(enable, 1.0F, 5000, 10000);
    }

    public DefaultArrivalControl(boolean enable, float marginScope, int blockDelay, int scopeDelay) {
        this.blocked = false;
        this.destinationNearby = false;
        this.enable = enable;
        this.marginScope = marginScope;
        this.blockDelay = blockDelay;
        this.scopeDelay = scopeDelay;
    }

    public void addListener(ArrivalControl.Listener listener) {
        this.listener = listener;
    }

    public void removeListener(ArrivalControl.Listener listener) {
        this.listener = null;
    }

    public void enable(boolean enable) {
        this.enable = enable;
    }

    public synchronized void notifyDistanceChanged(float distance) {
        LogUtils.d("NAVI--", "[DefaultArrivalControl][notifyDistanceChanged][enable->" + this.enable + "][destinationNearby->" + this.destinationNearby + "][blocked->" + this.blocked + "][distance->" + distance + "]");
        if (this.enable && !(distance <= 0.0F)) {
            if (this.destinationNearby) {
                if (this.marginScope <= distance) {
                    this.reset();
                }
            } else if (this.marginScope > distance) {
                this.startScopeTimer();
            }

        }
    }

    public synchronized void notifyBlocked() {
        LogUtils.d("NAVI--", "[DefaultArrivalControl][notifyBlocked][enable->" + this.enable + "][destinationNearby->" + this.destinationNearby + "][blocked->" + this.blocked + "]");
        if (this.enable && this.destinationNearby && !this.blocked) {
            this.startBlockTimer();
        }

    }

    public void release() {
        this.reset();
    }

    private void startScopeTimer() {
        this.destinationNearby = true;
        this.scopeTimer = new Timer();
        this.scopeTimerTask = new TimerTask() {
            public void run() {
                if (null != DefaultArrivalControl.this.blockTimerTask) {
                    DefaultArrivalControl.this.blockTimerTask.cancel();
                }

                LogUtils.d("NAVI--", "[DefaultArrivalControl][ScopeTimer]");
                DefaultArrivalControl.this.notifyArrived();
            }
        };
        this.scopeTimer.schedule(this.scopeTimerTask, (long)this.scopeDelay);
    }

    private void startBlockTimer() {
        this.blocked = true;
        this.blockTimer = new Timer();
        this.blockTimerTask = new TimerTask() {
            public void run() {
                DefaultArrivalControl.this.scopeTimerTask.cancel();
                LogUtils.d("NAVI--", "[DefaultArrivalControl][BlockTimer]");
                DefaultArrivalControl.this.notifyArrived();
            }
        };
        this.blockTimer.schedule(this.blockTimerTask, (long)this.blockDelay);
    }

    private void notifyArrived() {
        LogUtils.d("NAVI--", "[DefaultArrivalControl][notifyArrived]");
        if (null != this.listener) {
            (new Handler(Looper.getMainLooper())).post(new Runnable() {
                public void run() {
                    LogUtils.d("NAVI--", "[DefaultArrivalControl][notifyArrived really]");
                    DefaultArrivalControl.this.listener.onArrived();
                }
            });
        }

    }

    private void reset() {
        this.destinationNearby = false;
        this.blocked = false;
        if (null != this.blockTimer) {
            this.blockTimer.cancel();
            this.blockTimer = null;
        }

        if (null != this.blockTimerTask) {
            this.blockTimerTask.cancel();
            this.blockTimerTask = null;
        }

        if (null != this.scopeTimer) {
            this.scopeTimer.cancel();
            this.scopeTimer = null;
        }

        if (null != this.scopeTimerTask) {
            this.scopeTimerTask.cancel();
            this.scopeTimerTask = null;
        }

    }
}
```