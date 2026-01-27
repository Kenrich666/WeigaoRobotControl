package com.weigao.robot.control.ui.main;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.sensor.map.MapManager;
import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IDoorCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.DoorType;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.manager.ItemDeliveryManager;

import com.keenon.sdk.component.navigation.route.RouteNode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.weigao.robot.control.model.NavigationNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;
import android.os.Handler;

public class DeliveryActivity extends AppCompatActivity {
    private static final String TAG = "DeliveryActivity";
    private Button selectedLayerButton;
    // 存储配对关系
    private final HashMap<Integer, NavigationNode> pairings = new HashMap<>();

    /**
     * 舱门服务
     */
    private IDoorService doorService;
    /**
     * 机器人状态服务
     */
    private IRobotStateService robotStateService;

    /**
     * 开门按钮引用
     */
    private Button openDoorButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery);

        // 初始化 ItemDeliveryManager
        ItemDeliveryManager.getInstance().init(this);

        // 获取舱门服务
        doorService = ServiceManager.getInstance().getDoorService();
        // 获取机器人状态服务
        robotStateService = ServiceManager.getInstance().getRobotStateService();

        // 注册舱门状态回调
        doorService.registerCallback(doorCallback);

        // --- 1. 基础按钮 ---
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        findViewById(R.id.history_button).setOnClickListener(v -> {
            startActivity(new Intent(DeliveryActivity.this, ItemDeliveryHistoryActivity.class));
        });

        openDoorButton = findViewById(R.id.open_door_button);

        // 初始化时查询舱门状态并更新按钮文本
        updateDoorButtonState();

        openDoorButton.setOnClickListener(v -> {
            // 禁用按钮防止重复点击
            openDoorButton.setEnabled(false);
            Log.i(TAG, "点击了开/关门按钮，开始查询状态...");

            // 动态查询当前舱门状态
            doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean allClosed) {
                    runOnUiThread(() -> {
                        if (allClosed) {
                            Log.d(TAG, "当前所有舱门已关闭，准备执行开门操作");
                            // 当前门是关闭的，执行开门操作
                            doorService.openAllDoors(false, new IResultCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    runOnUiThread(() -> {
                                        Log.i(TAG, "开门成功");
                                        updateDoorButtonState();
                                        Toast.makeText(DeliveryActivity.this, "舱门已打开", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onError(ApiError error) {
                                    runOnUiThread(() -> {
                                        Log.e(TAG, "开门失败: " + error.getMessage());
                                        openDoorButton.setEnabled(true);
                                        Toast.makeText(DeliveryActivity.this,
                                                "开门失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                        } else {
                            Log.d(TAG, "当前有舱门开启，准备执行关门操作");
                            // 当前门是打开的，执行关门操作
                            doorService.closeAllDoors(new IResultCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    runOnUiThread(() -> {
                                        Log.i(TAG, "关门成功");
                                        updateDoorButtonState();
                                        Toast.makeText(DeliveryActivity.this, "舱门已关闭", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onError(ApiError error) {
                                    runOnUiThread(() -> {
                                        Log.e(TAG, "关门失败: " + error.getMessage());
                                        openDoorButton.setEnabled(true);
                                        Toast.makeText(DeliveryActivity.this,
                                                "关门失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                        }
                    });
                }

                @Override
                public void onError(ApiError error) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "查询舱门状态失败: " + error.getMessage());
                        openDoorButton.setEnabled(true);
                        Toast.makeText(DeliveryActivity.this,
                                "查询舱门状态失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        findViewById(R.id.return_button).setOnClickListener(v -> startActivity(new Intent(this, ReturnActivity.class)));

        findViewById(R.id.start_delivery_button).setOnClickListener(v -> {
            if (pairings.isEmpty()) {
                Toast.makeText(this, "请至少配对一个层", Toast.LENGTH_SHORT).show();
                return;
            }

            if (doorService == null) {
                startDelivery();
                return;
            }

            // 禁用按钮，避免重复点击
            v.setEnabled(false);

            // 检查舱门状态
            doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean allClosed) {
                    runOnUiThread(() -> {
                        if (allClosed) {
                            // 已关闭，直接开始
                            startDelivery();
                            v.setEnabled(true);
                        } else {
                            // 未关闭，先关门
                            Toast.makeText(DeliveryActivity.this, "检测到舱门未关，正在关闭...", Toast.LENGTH_SHORT).show();
                            doorService.closeAllDoors(new IResultCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(DeliveryActivity.this, "舱门已关闭，5秒后开始配送...", Toast.LENGTH_SHORT)
                                                .show();

                                        // 增加5秒等待时间
                                        new Handler().postDelayed(() -> {
                                            startDelivery();
                                            v.setEnabled(true);
                                        }, 5000);
                                    });
                                }

                                @Override
                                public void onError(ApiError error) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(DeliveryActivity.this, "自动关门失败: " + error.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                        v.setEnabled(true);
                                    });
                                }
                            });
                        }
                    });
                }

                @Override
                public void onError(ApiError error) {
                    runOnUiThread(() -> {
                        Toast.makeText(DeliveryActivity.this, "查询舱门状态失败: " + error.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                        v.setEnabled(true);
                    });
                }
            });
        });

        // --- 2. 层选择与“删除”逻辑 ---
        Button l1Button = findViewById(R.id.l1_button);
        Button l2Button = findViewById(R.id.l2_button);
        Button l3Button = findViewById(R.id.l3_button);

        View.OnClickListener layerClickListener = v -> {
            Button clickedButton = (Button) v;
            int id = clickedButton.getId();

            // Case 1: The user is clicking the currently selected (purple) button.
            // This action is always "unselect".
            if (clickedButton == selectedLayerButton) {
                refreshLayerStyle(clickedButton); // Revert to green (if paired) or gray (if not).
                selectedLayerButton = null;
                return;
            }

            // Case 2: The user is clicking a paired (green) button.
            // The desired action is "un-pair".
            if (pairings.containsKey(id)) {
                // Un-pair it.
                pairings.remove(id);
                if (id == R.id.l1_button)
                    clickedButton.setText("L1 层");
                if (id == R.id.l2_button)
                    clickedButton.setText("L2 层");
                if (id == R.id.l3_button)
                    clickedButton.setText("L3 层");
                Toast.makeText(this, "已删除配对", Toast.LENGTH_SHORT).show();
                refreshLayerStyle(clickedButton); // It will become gray now.

                // If another button was selected, unselect it as well.
                if (selectedLayerButton != null) {
                    refreshLayerStyle(selectedLayerButton);
                    selectedLayerButton = null;
                }
                return;
            }

            // Case 3: The user is clicking an unpaired (gray) button.
            // The action is "select for pairing".
            // First, un-select any previously selected button.
            if (selectedLayerButton != null) {
                refreshLayerStyle(selectedLayerButton);
            }
            // Then, select the new button.
            selectedLayerButton = clickedButton;
            selectedLayerButton.setBackgroundResource(R.drawable.rounded_button_selected);
        };

        l1Button.setOnClickListener(layerClickListener);
        l2Button.setOnClickListener(layerClickListener);
        l3Button.setOnClickListener(layerClickListener);

        // --- 3. 点位列表与“修改”逻辑 ---
        RecyclerView pointsRecyclerView = findViewById(R.id.points_recyclerview);
        pointsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        List<NavigationNode> points = new ArrayList<>();
        PointAdapter adapter = new PointAdapter(points, node -> {
            if (selectedLayerButton == null) {
                Toast.makeText(this, "请先选择一个机器人层", Toast.LENGTH_SHORT).show();
                return;
            }

            // 配对或修改配对：直接覆盖
            pairings.put(selectedLayerButton.getId(), node);
            selectedLayerButton.setText(node.getName());
            Toast.makeText(this, "已配对: " + node.getName(), Toast.LENGTH_SHORT).show();

            // 配对后立即刷新样式为绿色并取消“选中”状态
            refreshLayerStyle(selectedLayerButton);
            selectedLayerButton = null;
        });
        pointsRecyclerView.setAdapter(adapter);

        // 获取并解析真实点位
        getRealPoints(points, adapter);

    }

    /**
     * 获取真实点位列表
     */
    /**
     * 获取真实点位列表
     */
    // 存储特定类型的点位供后续使用
    public static List<NavigationNode> originPoints = new ArrayList<>();
    public static List<NavigationNode> chargePoints = new ArrayList<>();

    /**
     * 获取真实点位列表
     */
    private void getRealPoints(List<NavigationNode> points, PointAdapter adapter) {
        if (robotStateService == null)
            return;

        robotStateService.getDestinationList(new IResultCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // 在工作线程解析，UI线程更新
                new Thread(() -> {
                    List<NavigationNode> normalPoints = new ArrayList<>();
                    // 清空之前的缓存
                    originPoints.clear();
                    chargePoints.clear();

                    try {
                        if (result != null && !result.isEmpty()) {
                            // 先解析为 JSONObject
                            JSONObject resultObj = new JSONObject(result);
                            // 获取 data 数组
                            JSONArray jsonArray = resultObj.optJSONArray("data");

                            if (jsonArray != null) {
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject obj = jsonArray.getJSONObject(i);
                                    NavigationNode node = new NavigationNode();

                                    // 解析基本属性
                                    int id = obj.optInt("id");
                                    String name = obj.optString("name");
                                    String type = obj.optString("type"); // 获取点位类型

                                    if (name.isEmpty()) {
                                        name = String.valueOf(id);
                                    }
                                    node.setId(id);
                                    node.setName(name);
                                    node.setFloor(obj.optInt("floor"));

                                    // 解析 pose
                                    JSONObject pose = obj.optJSONObject("pose");
                                    if (pose != null) {
                                        JSONObject position = pose.optJSONObject("position");
                                        if (position != null) {
                                            node.setX(position.optDouble("x"));
                                            node.setY(position.optDouble("y"));
                                        }
                                        JSONObject orientation = pose.optJSONObject("orientation");
                                        if (orientation != null) {
                                            double w = orientation.optDouble("w");
                                            double x = orientation.optDouble("x");
                                            double y = orientation.optDouble("y");
                                            double z = orientation.optDouble("z");
                                            // 计算航向角 phi
                                            double phi = Math.atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z));
                                            node.setPhi(phi);
                                        }
                                    }

                                    // 填充 routeNode 用于兼容
                                    RouteNode routeNode = new RouteNode();
                                    routeNode.setId(id);
                                    routeNode.setName(name);

                                    // 初始化 NavigationInfo以防止 SDK 内部 NPE
                                    if (routeNode.getNavigationInfo() != null) {
                                        // [关键修复] 设置为 99999f，防止 SDK 误判
                                        routeNode.getNavigationInfo().setTotalDistance(99999f);
                                        routeNode.getNavigationInfo().setRemainDistance(99999f);
                                        routeNode.getNavigationInfo().setTotalTime(99999f);
                                        routeNode.getNavigationInfo().setRemainTime(99999f);
                                    }

                                    node.setRouteNode(routeNode);

                                    // 根据类型分类存储
                                    if ("origin".equals(type)) {
                                        originPoints.add(node);
                                        Log.d(TAG, "解析到原点: " + name);
                                    } else if ("charge".equals(type)) {
                                        chargePoints.add(node);
                                        Log.d(TAG, "解析到充电点: " + name);
                                    } else if ("normal".equals(type)) {
                                        // 仅 normal 类型显示在界面上
                                        normalPoints.add(node);
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "点位解析失败: " + e.getMessage());
                    }

                    // 如果列表仍为空（解析失败或无数据），保留空列表或添加提示
                    if (normalPoints.isEmpty()) {
                        Log.w(TAG, "未获取到有效普通点位");
                    }

                    runOnUiThread(() -> {
                        points.clear();
                        points.addAll(normalPoints);
                        adapter.notifyDataSetChanged();
                    });
                }).start();
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "获取点位失败: " + error.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(DeliveryActivity.this, "获取点位失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 只有在切换层级或取消选中时调用：
     * 如果有配对则显示绿色，没配对则显示原始灰色
     */
    private void refreshLayerStyle(Button button) {
        if (pairings.containsKey(button.getId())) {
            button.setBackgroundResource(R.drawable.rounded_button_paired); // 绿色
        } else {
            button.setBackgroundResource(R.drawable.rounded_button); // 灰色
        }
    }

    // --- Adapter 代码 ---
    private static class PointAdapter extends RecyclerView.Adapter<PointAdapter.PointViewHolder> {
        private final List<NavigationNode> mPoints;
        private final OnPointClickListener mListener;

        PointAdapter(List<NavigationNode> points, OnPointClickListener listener) {
            mPoints = points;
            mListener = listener;
        }

        @NonNull
        @Override
        public PointViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Button button = new Button(parent.getContext());
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int margin = (int) (8 * parent.getResources().getDisplayMetrics().density);
            params.setMargins(margin, margin, margin, margin);
            button.setLayoutParams(params);
            button.setBackgroundResource(R.drawable.item_point_style);
            button.setTextColor(Color.BLACK);
            return new PointViewHolder(button);
        }

        @Override
        public void onBindViewHolder(@NonNull PointViewHolder holder, int position) {
            NavigationNode node = mPoints.get(position);
            holder.pointButton.setText(node.getName());
            holder.pointButton.setOnClickListener(v -> mListener.onPointClick(node));
        }

        @Override
        public int getItemCount() {
            return mPoints.size();
        }

        static class PointViewHolder extends RecyclerView.ViewHolder {
            Button pointButton;

            PointViewHolder(Button b) {
                super(b);
                pointButton = b;
            }
        }
    }

    interface OnPointClickListener {
        void onPointClick(NavigationNode node);
    }

    //
    // /**
    // * 更新开门按钮的状态和文本
    // */
    private void updateDoorButtonState() {
        if (openDoorButton == null) {
            return;
        }

        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                runOnUiThread(() -> {
                    if (allClosed) {
                        openDoorButton.setText("开门");
                    } else {
                        openDoorButton.setText("关门");
                    }
                    // 延迟 5 秒后才重新启用按钮，防止连续点击
                    new Handler().postDelayed(() -> {
                        if (openDoorButton != null) {
                            openDoorButton.setEnabled(true);
                        }
                    }, 5000);
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> {
                    // 错误时也延迟启用
                    new Handler().postDelayed(() -> {
                        if (openDoorButton != null) {
                            openDoorButton.setEnabled(true);
                        }
                    }, 1500);
                });
            }
        });
    }

    /**
     * 舱门状态回调监听器
     */
    private final IDoorCallback doorCallback = new IDoorCallback() {
        @Override
        public void onDoorStateChanged(int doorId, int state) {
            // 当任何舱门状态变化时，更新按钮状态
            Log.d(TAG, "onDoorStateChanged: doorId=" + doorId + ", state=" + state);
            runOnUiThread(() -> updateDoorButtonState());
        }

        @Override
        public void onDoorTypeChanged(DoorType type) {
            // 舱门类型变化，暂不处理
        }

        @Override
        public void onDoorTypeSettingResult(boolean success) {
            // 舱门类型设置结果，暂不处理
        }

        @Override
        public void onDoorError(int doorId, int errorCode) {
            // 舱门错误，可以在这里显示错误提示
            Log.e(TAG, "onDoorError: doorId=" + doorId + ", errorCode=" + errorCode);
            runOnUiThread(() -> {
                Toast.makeText(DeliveryActivity.this,
                        "舱门错误 (ID: " + doorId + ", 错误码: " + errorCode + ")",
                        Toast.LENGTH_SHORT).show();
            });
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1002 && resultCode == RESULT_OK) {
            Log.d(TAG, "配送任务结束返回");
            if (data != null && data.hasExtra("remaining_pairings")) {
                HashMap<Integer, NavigationNode> remaining = (HashMap<Integer, NavigationNode>) data
                        .getSerializableExtra("remaining_pairings");
                pairings.clear();
                if (remaining != null) {
                    pairings.putAll(remaining);
                }
                updateUIFromPairings();
            } else {
                Log.d(TAG, "未收到剩余配对信息，重置界面");
                resetUI();
            }
        }
    }

    /**
     * 重置界面状态（清空配对信息）
     */
    private void resetUI() {
        pairings.clear();
        updateUIFromPairings();
    }

    /**
     * 根据当前的配对信息更新UI
     */
    private void updateUIFromPairings() {
        updateButtonState(findViewById(R.id.l1_button), "L1 层");
        updateButtonState(findViewById(R.id.l2_button), "L2 层");
        updateButtonState(findViewById(R.id.l3_button), "L3 层");
        selectedLayerButton = null;
    }

    private void updateButtonState(Button btn, String defaultText) {
        if (btn == null)
            return;
        if (pairings.containsKey(btn.getId())) {
            NavigationNode node = pairings.get(btn.getId());
            if (node != null) {
                btn.setText(node.getName());
            }
            refreshLayerStyle(btn);
        } else {
            btn.setText(defaultText);
            refreshLayerStyle(btn);
        }
    }

    private void startDelivery() {
        // 记录开始配送时间
        ItemDeliveryManager.getInstance().startDelivery();

        Intent intent = new Intent(this, DeliveryNavigationActivity.class);
        intent.putExtra("pairings", pairings);
        // 使用 startActivityForResult 以便在任务完成后接收通知
        startActivityForResult(intent, 1002);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销舱门状态回调
        if (doorService != null) {
            doorService.unregisterCallback(doorCallback);
        }
    }
}
