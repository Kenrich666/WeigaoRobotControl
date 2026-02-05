```
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.component.navigation.arrival;

public interface ArrivalControl {
    float DEFAULT_MARGIN_SCOPE = 1.0F;
    int DEFAULT_BLOCK_DELAY = 5000;
    int DEFAULT_SCOPE_DELAY = 10000;

    void addListener(Listener var1);

    void removeListener(Listener var1);

    void enable(boolean var1);

    void notifyDistanceChanged(float var1);

    void notifyBlocked();

    void release();

    public interface Listener {
        void onArrived();
    }
}
```