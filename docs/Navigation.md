```
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.component.navigation.common;

import com.keenon.sdk.component.navigation.NavigationImpl;
import com.keenon.sdk.component.navigation.arrival.ArrivalControl;
import com.keenon.sdk.component.navigation.route.RouteLine;
import com.keenon.sdk.component.navigation.route.RouteNode;

public interface Navigation {
    int STATE_IDLE = 0;
    int STATE_PREPARED = 1;
    int STATE_RUNNING = 2;
    int STATE_DESTINATION = 3;
    int STATE_PAUSED = 4;
    int STATE_COLLISION = 5;
    int STATE_BLOCKED = 6;
    int STATE_STOPPED = 7;
    int STATE_ERROR = 8;
    int STATE_BLOCKING = 9;
    int STATE_END = 10;
    int FORWARD = 1;
    int BACKWARD = 2;
    int LEFT = 3;
    int RIGHT = 4;
    int EVENT_SET_TAG_SUCCESS = 100;

    void addListener(Listener var1);

    void removeListener(Listener var1);

    void prepare(RouteLine var1);

    void setPilotWhenReady(boolean var1);

    void setTaskControl(boolean var1);

    void pilotNext();

    void stop();

    void manual(int var1);

    void setSpeed(int var1);

    void setStableMode(int var1);

    void setSportMode(int var1);

    void setBlockTimeout(int var1);

    RouteNode[] getRouteNodes();

    RouteNode getCurrentNode();

    RouteNode getNextNode();

    int getCurrentPosition();

    void release();

    void skipTo(int var1);

    void cancelArriveControl();

    void resetNewTargets();

    boolean isLastRepeat();

    void arrivalControlEnable(boolean var1);

    RouteLine getRouteLine();

    public static final class Factory {
        private Factory() {
        }

        public static Navigation newInstance(ArrivalControl arrivalControl) {
            return NavigationImpl.getInstance(arrivalControl);
        }
    }

    public interface Listener {
        void onStateChanged(int var1, int var2);

        void onRouteNode(int var1, RouteNode var2);

        void onRoutePrepared(RouteNode... var1);

        void onDistanceChanged(float var1);

        void onError(int var1);

        void onEvent(int var1);
    }
}
```