```
package com.keenon.peanut.sample.chassis;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.bean.MyPoint;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.sdk.component.navigation.route.RouteNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NavigationDemo extends BaseActivity {

  private static final int ARRIVE_STAY_DURATION = 3000;
  @BindView(R.id.text_target1)
  EditText textTarget1;
  @BindView(R.id.text_target2)
  EditText textTarget2;
  @BindView(R.id.text_target3)
  EditText textTarget3;
  @BindView(R.id.et_timeout)
  EditText etTimeOut;
  @BindView(R.id.et_repeat)
  EditText etRepeat;
  @BindView(R.id.et_speed)
  EditText etSpeed;
  @BindView(R.id.checkbox_arrival)
  CheckBox checkBoxArrival;
  @BindView(R.id.btn_navigate)
  Button btnStart;
  List<MyPoint> list = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation);
    ButterKnife.bind(this);
    setButtonBack();

    btnStart.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

        try {
          list.clear();
          addPoint(textTarget1, true);
          addPoint(textTarget2, false);
          addPoint(textTarget3, true);

          if (list.size() == 0) {
            return;
          }
          Intent intent = new Intent(NavigationDemo.this, NavigationActivity.class);
          intent.putExtra("timeout", Integer.parseInt(etTimeOut.getText().toString()));
          intent.putExtra("arrival", checkBoxArrival.isChecked());
          intent.putExtra("repeat", Integer.parseInt(etRepeat.getText().toString()));
          intent.putExtra("speed", Integer.parseInt(etSpeed.getText().toString()));
          intent.putExtra("list", (Serializable) list);

          startActivity(intent);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  private void addPoint(EditText editText, boolean manualNext) {
    if (!TextUtils.isEmpty(editText.getText().toString())) {
      MyPoint point = new MyPoint();
      point.setManualControl(manualNext);
      point.setDuration(ARRIVE_STAY_DURATION);
      RouteNode node = new RouteNode();
      node.setId(Integer.parseInt(editText.getText().toString()));
      node.setName("Point:" + editText.getText().toString());
      point.setRouteNode(node);
      list.add(point);
    }
  }
}
```