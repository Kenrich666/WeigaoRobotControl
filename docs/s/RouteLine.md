```
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.component.navigation.route;

public class RouteLine implements RouteSelector.Output {
    private static final String TAG = RouteLine.class.getSimpleName();
    private RouteSelector routeSelector;
    private RouteNode[] routeNodes;
    private Listener listener;
    private boolean autoRepeat;
    private int repeatCount;

    public RouteLine(RouteNode... routeNodes) {
        this(0, routeNodes);
    }

    public RouteLine(int policy, RouteNode... routeNodes) {
        this(policy, false, 1, routeNodes);
    }

    public RouteLine(int policy, boolean autoRepeat, int repeatCount, RouteNode... routeNodes) {
        this.routeSelector = this.buildRouteSelector(policy);
        this.autoRepeat = autoRepeat;
        this.repeatCount = repeatCount;
        this.routeNodes = routeNodes;
    }

    public RouteLine(RouteSelector routeSelector, boolean autoRepeat, int repeatCount, RouteNode... routeNodes) {
        this.routeSelector = routeSelector;
        this.autoRepeat = autoRepeat;
        this.repeatCount = repeatCount;
        this.routeNodes = routeNodes;
    }

    private RouteSelector buildRouteSelector(int policy) {
        if (policy == 1) {
            return DefaultRouteSelector.newAdaptiveInstance();
        } else {
            return policy == 2 ? DefaultRouteSelector.newFixedInstance() : DefaultRouteSelector.newDefaultInstance();
        }
    }

    public void addListener(Listener listener) {
        this.listener = listener;
    }

    public void removeListener(Listener listener) {
        this.listener = null;
    }

    public void prepare() {
        this.routeSelector.selectRoute(this, this.routeNodes);
    }

    public RouteNode[] getRouteNodes() {
        return this.routeNodes;
    }

    public void setRouteNodes(RouteNode... routeNodes) {
        this.routeNodes = routeNodes;
    }

    public int getLength() {
        return this.routeNodes.length;
    }

    public boolean isAutoRepeat() {
        return this.autoRepeat;
    }

    public void setAutoRepeat(boolean autoRepeat) {
        this.autoRepeat = autoRepeat;
    }

    public int getRepeatCount() {
        return this.repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public void adaptiveRoute(RouteNode... routeNodes) {
        this.setRouteNodes(routeNodes);
        if (this.listener != null) {
            this.listener.onRoutePrepared();
        }

    }

    public void fixedRoute(RouteNode... routeNodes) {
        if (this.listener != null) {
            this.listener.onRoutePrepared();
        }

    }

    public void onError(int errorCode) {
        if (this.listener != null) {
            this.listener.onRouteError(errorCode);
        }

    }

    public interface Listener {
        void onRoutePrepared();

        void onRouteError(int var1);
    }
}
```