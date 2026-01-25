```
package com.keenon.peanut.sample.chassis;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.bean.MyPoint;
import com.keenon.peanut.sample.manager.NavManager;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.sdk.component.navigation.common.Navigation;
import com.keenon.sdk.component.navigation.route.RouteNode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NavigationActivity extends BaseActivity implements Navigation.Listener {


  @BindView(R.id.tv_status)
  TextView tvStatus;
  @BindView(R.id.btn_manual_next)
  Button btnNext;
  @BindView(R.id.btn_pause)
  Button btnPause;
  @BindView(R.id.btn_resume)
  Button btnResume;

  private int timeout = 0;
  private boolean arrival;
  private int repeat = -1;
  private int speed = 80;
  private List<MyPoint> list = new ArrayList<>();
  private MyPoint curPoint;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_pilot);
    ButterKnife.bind(this);
    setButtonBack();
    btnNext.setVisibility(View.INVISIBLE);
    btnNext.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        navigatenext();
      }
    });

    btnPause.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        NavManager.getInstance().readyGo(false);
      }
    });

    btnResume.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        NavManager.getInstance().readyGo(true);
      }
    });

    timeout = getIntent().getIntExtra("timeout", 0);
    arrival = getIntent().getBooleanExtra("arrival", false);
    repeat = getIntent().getIntExtra("repeat", -1);
    speed = getIntent().getIntExtra("speed", 80);
    list = (List<MyPoint>) getIntent().getSerializableExtra("list");
    NavManager.getInstance().init(this, timeout, repeat, arrival);
    NavManager.getInstance().setSpeed(speed);
    NavManager.getInstance().setTargets(list);
    NavManager.getInstance().prepare();
    curPoint = NavManager.getInstance().getCurNode();
    tvStatus.setText(getString(R.string.go_to) + curPoint.getRouteNode().getName());

  }


  @Override
  protected void onDestroy() {
    super.onDestroy();
    NavManager.getInstance().stop();
    NavManager.getInstance().release();
  }

  @Override
  public void onStateChanged(int state, int schedule) {
    switch (state) {
      case Navigation.STATE_DESTINATION:
        arrived();
        break;
      case Navigation.STATE_COLLISION:
      case Navigation.STATE_BLOCKED:
        Toast.makeText(this, R.string.avoid_tip, Toast.LENGTH_SHORT).show();

        break;
      case Navigation.STATE_BLOCKING:
        Toast.makeText(this, R.string.block_timeout_tip, Toast.LENGTH_SHORT).show();

        break;
    }
  }

  private void arrived() {
    if (curPoint.isManualControl()) {
      btnNext.setVisibility(View.VISIBLE);
      tvStatus.setText(R.string.arrive_tip_2);
    } else {

      btnNext.setVisibility(View.INVISIBLE);
      tvStatus.setText(getString(R.string.arrive_tip_1, curPoint.getDuration() + ""));
    }
  }

  private void navigatenext() {
    if (NavManager.getInstance().getNextNode() == null) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(NavigationActivity.this, R.string.no_more_point, Toast.LENGTH_SHORT).show();
        }
      });
    } else {
      NavManager.getInstance().nextDes();
      NavManager.getInstance().readyGo(true);
      curPoint = NavManager.getInstance().getCurNode();
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          tvStatus.setText(getString(R.string.go_to) + curPoint.getRouteNode().getName());
        }
      });
    }
  }

  @Override
  public void onRouteNode(int index, RouteNode routeNode) {

  }

  @Override
  public void onRoutePrepared(RouteNode... routeNodes) {
    NavManager.getInstance().readyGo(true);
  }

  @Override
  public void onDistanceChanged(float distance) {

  }

  @Override
  public void onError(int code) {

  }

  @Override
  public void onEvent(int event) {

  }
}
```