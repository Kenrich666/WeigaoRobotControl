```
package com.keenon.peanut.sample.manager;

import android.util.Log;

import com.keenon.peanut.sample.bean.MyPoint;
import com.keenon.sdk.component.navigation.PeanutNavigation;
import com.keenon.sdk.component.navigation.common.Navigation;
import com.keenon.sdk.component.navigation.route.RouteNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class NavManager implements Navigation.Listener {
  private static final String TAG = "NavManager";
  private static volatile NavManager mInstance;
  private List<RouteNode> nodeList = new ArrayList<>();
  private List<Navigation.Listener> listeners = new CopyOnWriteArrayList<>();
  private PeanutNavigation mPeanutNavigation;
  private List<MyPoint> targets = new ArrayList<>();

  private NavManager() {
  }

  public static NavManager getInstance() {
    if (mInstance == null) {
      synchronized (NavManager.class) {
        mInstance = new NavManager();
      }
    }
    return mInstance;
  }


  /**
   * @param listener
   * @param timeOut       If timeout > 0 ,when the robot blocking for <timeOut> seconds , robot will callback  state<Navigation.STATE_BLOCKING> for you by the
   *                      listener ,you can do what you want to do,for example ,you can skip the tartget you are leading to  or ga back to orgin point;
   *                      If timeout = 0 ,do not use the timeout policy
   *                      Duration is : 0 ~ 300
   * @param repeatTime    Example:if you setTargets a-b-c , repeatTime = 2 ,it will be a-b-c-a-b-c ;
   *                      -1 is infinite
   * @param arrivalEnable true: open the arrival policy ,it means when the robot blocked within 1meter from the target point for 5seconds ,it will
   *                      be  recognized as destinationed ! callback is <Navigation.STATE_DESTINATION>,too.
   */
  public void init(Navigation.Listener listener, int timeOut, int repeatTime, boolean arrivalEnable) {
    listeners.clear();
    setListener(listener);
    if (mPeanutNavigation == null) {

      PeanutNavigation.Builder builder = new PeanutNavigation.Builder()
          .setListener(this)
          .enableDefaultArrival(arrivalEnable)
          .setRepeatCount(repeatTime)
          .setBlockingTimeOut(timeOut);


      mPeanutNavigation = builder.build();
      Log.d(TAG, "init , mPeanutNavigation init  ");
    }
  }

  public MyPoint getCurNode() {
    if (mPeanutNavigation.getCurrentNode() == null) {
      return null;
    }
    int indexCur = mPeanutNavigation.getCurrentPosition();
    Log.d(TAG, "indexCur : " + indexCur);
    if (indexCur >= 0 && indexCur < targets.size()) {
      return targets.get(indexCur);
    }
    return null;
  }

  public MyPoint getNextNode() {
    if (mPeanutNavigation.getNextNode() == null) {
      return null;
    }
    int indexNext = mPeanutNavigation.getCurrentPosition() + 1;
    Log.d(TAG, "indeNex : " + indexNext);
    if (indexNext >= 0 && targets != null && targets.size() != 0) {
      return targets.get(indexNext % targets.size());
    }
    return null;
  }


  public List<MyPoint> getTargets() {
    return targets;
  }


  public void setTargets(List<MyPoint> targets) {
    Log.d(TAG, "setTargets" + targets);
    this.targets.clear();
    this.targets.addAll(targets);
    nodeList.clear();
    for (int i = 0; i < this.targets.size(); i++) {
      if (this.targets.get(i).getRouteNode() == null) {
        Log.d(TAG, "routeNode ==  " + nodeList);
      } else {
        Log.d(TAG, "nodelist.add  " + nodeList);
        nodeList.add(this.targets.get(i).getRouteNode());
      }
    }
    if (mPeanutNavigation != null) {
      Log.d(TAG, "set nodelist : " + nodeList);
      mPeanutNavigation.setTargets(nodeList);
    }
  }


  public void prepare() {
    mPeanutNavigation.prepare();
  }

  /**
   * @param ready true ： start
   *              false ： pause
   */
  public void readyGo(boolean ready) {
    if (mPeanutNavigation != null) {
      mPeanutNavigation.setPilotWhenReady(ready);
    }
  }

  public void stop() {
    if (mPeanutNavigation != null) {
      mPeanutNavigation.stop();
    }
  }

  /**
   * go to the next point
   */
  public void nextDes() {
    mPeanutNavigation.pilotNext();
  }

  /**
   * @param speed 20 ~100
   */
  public void setSpeed(int speed) {
    mPeanutNavigation.setSpeed(speed);
  }

  public void setListener(Navigation.Listener listener) {
    if (listener == null) {
      return;
    }
    listeners.add(listener);
  }

  public void removeListener(Navigation.Listener listener) {
    listeners.remove(listener);
  }

  public void release() {
    targets.clear();
    listeners.clear();
    if (mPeanutNavigation != null) {
      mPeanutNavigation.release();
    }
    mInstance = null;
  }


  public void setArrivalControlEnable(boolean enable) {
    mPeanutNavigation.setArrivalControlEnable(enable);
  }

  public boolean isLastRepeat() {
    return mPeanutNavigation.isLastRepeat();
  }

  @Override
  public void onStateChanged(int state, int schedule) {
    for (Navigation.Listener listener : listeners) {
      if (listener != null) {
        listener.onStateChanged(state, schedule);
      }
    }
  }

  @Override
  public void onRouteNode(int index, RouteNode routeNode) {
    Log.d(TAG, "onRouteNode ---->index : " + index + ",routeNode : " + routeNode.toString());
    for (Navigation.Listener listener : listeners) {
      if (listener != null) {
        listener.onRouteNode(index, routeNode);
      }
    }
  }

  @Override
  public void onRoutePrepared(RouteNode... routeNodes) {
    Log.d(TAG, "onRoutePrepared length: " + routeNodes.length);
    for (Navigation.Listener listener : listeners) {
      listener.onRoutePrepared(routeNodes);
    }
  }

  @Override
  public void onDistanceChanged(float distance) {
    Log.d(TAG, "onDistanceChanged : " + distance);
    for (Navigation.Listener listener : listeners) {
      listener.onDistanceChanged(distance);
    }
  }

  @Override
  public void onError(int code) {
    Log.d(TAG, "onError : " + code);
    for (Navigation.Listener listener : listeners) {
      listener.onError(code);
    }
  }

  @Override
  public void onEvent(int event) {

  }

}
```