package com.weigao.robot.control.ui.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IDoorCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.CircularRoute;
import com.weigao.robot.control.model.DoorType;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.model.CircularDeliveryRecord;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// 循环配送的
/**
 * 循环配送 vs 普通配送 模式对比
 *
 * ### 1. 循环配送模式 (Circular Delivery)
 * 主要用于机器人在固定的路线上进行多点循环配送（例如自助取餐、巡航等）。
 *
 * | 功能模块 | 文件名 | 功能描述 |
 * | :--- | :--- | :--- |
 * | **入口/配置** | {@link CircularDeliveryActivity} |
 * **主页面**。负责展示和管理“循环路线”列表。用户在此新建/编辑路线（设置途经点、循环次数），检查舱门状态，并启动循环任务。 |
 * | **导航中** | {@link CircularDeliveryNavigationActivity} |
 * **导航控制中心**。负责执行循环逻辑（按设定的循环次数重复跑点）。显示当前第几轮、第几个点位，处理暂停/继续/结束导航的逻辑。 |
 * | **到达处理** | {@link CircularArrivalActivity} |
 * **到达站点页面**。到达某个点位后弹出。包含一个30秒倒计时（倒计时结束自动前往下一站或返航）。提供“开门”、“继续下一站”、“结束返航”等操作。 |
 * | **历史记录** | {@link CircularDeliveryHistoryActivity} |
 * **历史记录页面**。展示循环配送的过往记录，包括路线名称、完成的循环次数、状态（完成/取消）和耗时。 |
 *
 * ---
 *
 * ### 2. 普通配送模式 (Item Delivery)
 * 主要用于点对点、多点配送物品（例如送餐给特定桌号），流程更严格（如取货需密码）。
 *
 * | 功能模块 | 文件名 | 功能描述 |
 * | :--- | :--- | :--- |
 * | **入口/配置** | {@link DeliveryActivity} |
 * **主页面**。负责舱门（层级）与点位的配对（例如L1层送到A点）。检查舱门状态，处理多层配对逻辑，启动配送任务。 |
 * | **导航中** | {@link DeliveryNavigationActivity} |
 * **导航控制中心**。负责处理配对好的任务列表。它会优化路径（合并相同目的地的任务），按顺序导航到目标点，并在途中显示配送状态。 |
 * | **到达处理** | {@link ConfirmReceiptActivity} |
 * **取货确认页面**。到达目标点后弹出。**核心区别**：这里需要验证**密码**才能开门取货。界面会高亮显示当前需要取货的层级（L1/L2/L3），并有“确认收货”流程。
 * |
 * | **到达处理** | {@link ConfirmReceiptActivity} |
 * **取货确认页面**。到达目标点后弹出。**核心区别**：这里需要验证**密码**才能开门取货。界面会高亮显示当前需要取货的层级（L1/L2/L3），并有“确认收货”流程。
 * |
 * | **历史记录** | {@link ItemDeliveryHistoryActivity} |
 * **历史记录页面**。展示单次物品配送的详细记录，精确到每个点位的到达时间、耗时以及任务ID。 |
 *
 * ---
 *
 * ### 3. 数据管理 (Data Management)
 *
 * | 模式 | 管理器类名 | 记录类名 | 用途 |
 * | :--- | :--- | :--- | :--- |
 * | **循环配送** |
 * {@link com.weigao.robot.control.manager.CircularDeliveryHistoryManager} |
 * {@link CircularDeliveryRecord} | 负责保存、读取和清空循环配送的历史记录。记录字段包括：任务名、循环数、耗时、状态等。 |
 * | **普通配送** | {@link com.weigao.robot.control.manager.ItemDeliveryManager} |
 * {@link com.weigao.robot.control.model.ItemDeliveryRecord} |
 * 负责记录单次配送的点位到达情况。记录字段包括：任务ID、点位名、到达时间、耗时、具体状态（成功/超时/失败）等。 |
 */
public class CircularDeliveryActivity extends AppCompatActivity {
    private static final String TAG = "CircularDeliveryAct";
    private static final String PREFS_NAME = "CircularRoutesPrefs";
    private static final String KEY_ROUTES = "SavedRoutes";

    private IDoorService doorService;
    private IRobotStateService robotStateService;
    private Button openDoorButton;

    private RecyclerView routesRecyclerView;
    private RouteAdapter routeAdapter;
    private List<CircularRoute> savedRoutes = new ArrayList<>();
    private List<NavigationNode> availableNodes = new ArrayList<>();

    private CircularRoute selectedRoute = null; // Currently selected route

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circular_delivery);

        // Core Services
        doorService = ServiceManager.getInstance().getDoorService();
        robotStateService = ServiceManager.getInstance().getRobotStateService();

        // 1. Header Buttons
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.return_sc_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReturnActivity.class);
            intent.putExtra("return_speed",
                    com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance().getReturnSpeed());
            startActivity(intent);
        });

        openDoorButton = findViewById(R.id.open_door_button);
        setupDoorButton();

        // History Button
        // History Button
        findViewById(R.id.history_button).setOnClickListener(v -> {
            startActivity(new Intent(this, CircularDeliveryHistoryActivity.class));
        });

        // 2. Routes List
        routesRecyclerView = findViewById(R.id.routes_recycler_view);
        routesRecyclerView.setLayoutManager(new GridLayoutManager(this, 4)); // 4 columns as per sketch
        routeAdapter = new RouteAdapter(savedRoutes, route -> {
            selectedRoute = route;
            routeAdapter.notifyDataSetChanged();
            Toast.makeText(this, "已选择: " + route.getName(), Toast.LENGTH_SHORT).show();
        });
        routesRecyclerView.setAdapter(routeAdapter);

        // Load Routes
        loadRoutesFromPrefs();
        // Load Maps Nodes (for creating new routes)
        loadMapNodes();

        // 3. New Route Button
        ImageButton newRouteBtn = findViewById(R.id.new_route_button);
        newRouteBtn.setOnClickListener(v -> openRouteEditor(null, -1));

        // 4. Start Navigation
        Button startNavBtn = findViewById(R.id.start_navigation_button);
        startNavBtn.setOnClickListener(v -> startNavigation());

        // Register door callback
        if (doorService != null) {
            doorService.registerCallback(doorCallback);
        }
    }

    private void setupDoorButton() {
        if (doorService == null)
            return;
        updateDoorButtonState();
        openDoorButton.setOnClickListener(v -> {
            openDoorButton.setEnabled(false);
            doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean allClosed) {
                    runOnUiThread(() -> {
                        if (allClosed) {
                            doorService.openAllDoors(false, new IResultCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(CircularDeliveryActivity.this, "舱门已打开", Toast.LENGTH_SHORT)
                                                .show();
                                        updateDoorButtonState();
                                    });
                                }

                                @Override
                                public void onError(ApiError error) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(CircularDeliveryActivity.this, "开门失败: " + error.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                        openDoorButton.setEnabled(true);
                                    });
                                }
                            });
                        } else {
                            doorService.closeAllDoors(new IResultCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(CircularDeliveryActivity.this, "舱门已关闭", Toast.LENGTH_SHORT)
                                                .show();
                                        updateDoorButtonState();
                                    });
                                }

                                @Override
                                public void onError(ApiError error) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(CircularDeliveryActivity.this, "关门失败: " + error.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                        openDoorButton.setEnabled(true);
                                    });
                                }
                            });
                        }
                    });
                }

                @Override
                public void onError(ApiError error) {
                    runOnUiThread(() -> openDoorButton.setEnabled(true));
                }
            });
        });
    }

    private void updateDoorButtonState() {
        if (doorService == null)
            return;
        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                runOnUiThread(() -> {
                    openDoorButton.setText(allClosed ? "开门" : "关门");
                    new Handler().postDelayed(() -> openDoorButton.setEnabled(true), 2000);
                });
            }

            @Override
            public void onError(ApiError error) {
            }
        });
    }

    private void loadMapNodes() {
        if (robotStateService == null)
            return;
        robotStateService.getDestinationList(new IResultCallback<String>() {
            @Override
            public void onSuccess(String result) {
                new Thread(() -> {
                    try {
                        availableNodes.clear();
                        JSONObject resultObj = new JSONObject(result);
                        JSONArray jsonArray = resultObj.optJSONArray("data");

                        // Clear static origin points to avoid duplicates or stale data
                        DeliveryActivity.originPoints.clear();

                        if (jsonArray != null) {
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject obj = jsonArray.getJSONObject(i);
                                String type = obj.optString("type");

                                NavigationNode node = new NavigationNode();
                                node.setId(obj.optInt("id"));
                                node.setName(obj.optString("name"));
                                node.setFloor(obj.optInt("floor"));

                                if ("normal".equals(type)) {
                                    availableNodes.add(node);
                                } else if ("origin".equals(type)) {
                                    // Ensure we have origin points for return navigation
                                    if (!DeliveryActivity.originPoints.contains(node)) {
                                        // Simple duplicate check or just clear before loading?
                                        // Since we append, maybe better to clear before loop?
                                        // But this is inside a loop inside a thread.
                                        // Let's just add it. DeliveryActivity clears it before load.
                                        // We should ideally clear it before this loop starts.
                                        DeliveryActivity.originPoints.add(node);
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse nodes", e);
                    }
                }).start();
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "Failed to load nodes: " + error.getMessage());
            }
        });
    }

    // --- Persistence ---
    private static final String ROUTES_DIR = "WeigaoRobot/routes";
    private static final String ROUTES_FILE = "circular_routes.json";

    private void saveRoutesToPrefs() {
        File dir = new File(android.os.Environment.getExternalStorageDirectory(), ROUTES_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, ROUTES_FILE);

        try {
            JSONArray array = new JSONArray();
            for (CircularRoute route : savedRoutes) {
                JSONObject obj = new JSONObject();
                obj.put("name", route.getName());
                obj.put("loop", route.getLoopCount());

                JSONArray nodesArr = new JSONArray();
                for (NavigationNode node : route.getNodes()) {
                    JSONObject n = new JSONObject();
                    n.put("id", node.getId());
                    n.put("name", node.getName());
                    // We only need minimal info to reconstruct or verify
                    nodesArr.put(n);
                }
                obj.put("nodes", nodesArr);
                array.put(obj);
            }

            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                writer.write(array.toString());
            }

        } catch (Exception e) { // Catch JSONException or IOException
            Log.e(TAG, "Error saving routes to file", e);
        }
    }

    private void loadRoutesFromPrefs() {
        File file = new File(android.os.Environment.getExternalStorageDirectory(), ROUTES_DIR + "/" + ROUTES_FILE);
        if (!file.exists())
            return;

        try {
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            JSONArray array = new JSONArray(sb.toString());
            savedRoutes.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.getString("name");
                int loop = obj.getInt("loop");
                JSONArray nodesArr = obj.getJSONArray("nodes");

                List<NavigationNode> nodes = new ArrayList<>();
                for (int j = 0; j < nodesArr.length(); j++) {
                    JSONObject n = nodesArr.getJSONObject(j);
                    NavigationNode node = new NavigationNode();
                    node.setId(n.getInt("id"));
                    node.setName(n.getString("name"));
                    nodes.add(node);
                }
                savedRoutes.add(new CircularRoute(name, nodes, loop));
            }
            if (routeAdapter != null)
                routeAdapter.notifyDataSetChanged();

        } catch (Exception e) {
            Log.e(TAG, "Error loading routes from file", e);
        }
    }

    private void startNavigation() {
        if (selectedRoute == null) {
            Toast.makeText(this, "请先选择一条循环路线", Toast.LENGTH_SHORT).show();
            return;
        }

        Button startNavBtn = findViewById(R.id.start_navigation_button);
        if (startNavBtn != null)
            startNavBtn.setEnabled(false);

        if (robotStateService != null) {
            Toast.makeText(this, "正在检查定位状态...", Toast.LENGTH_SHORT).show();
            robotStateService.performLocalization(new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    runOnUiThread(() -> checkDoorsAndProceed(startNavBtn));
                }

                @Override
                public void onError(ApiError error) {
                    runOnUiThread(() -> {
                        if (startNavBtn != null)
                            startNavBtn.setEnabled(true);
                        Intent intent = new Intent(CircularDeliveryActivity.this, PositioningFailedActivity.class);
                        startActivity(intent);
                    });
                }
            });
        } else {
            checkDoorsAndProceed(startNavBtn);
        }
    }

    private void checkDoorsAndProceed(View btn) {
        if (doorService == null) {
            proceedToNavigation();
            if (btn != null)
                btn.setEnabled(true);
            return;
        }

        // Check if doors are closed
        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                runOnUiThread(() -> {
                    if (allClosed) {
                        proceedToNavigation();
                        if (btn != null)
                            btn.setEnabled(true);
                    } else {
                        Toast.makeText(CircularDeliveryActivity.this, "舱门未关，正在自动关闭...", Toast.LENGTH_SHORT).show();
                        doorService.closeAllDoors(new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                runOnUiThread(() -> {
                                    Toast.makeText(CircularDeliveryActivity.this, "舱门已关闭", Toast.LENGTH_SHORT).show();
                                    // Update button state just in case
                                    updateDoorButtonState();
                                    proceedToNavigation();
                                    if (btn != null)
                                        btn.setEnabled(true);
                                });
                            }

                            @Override
                            public void onError(ApiError error) {
                                runOnUiThread(() -> {
                                    Toast.makeText(CircularDeliveryActivity.this, "关门失败，请手动关闭: " + error.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    if (btn != null)
                                        btn.setEnabled(true);
                                });
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> {
                    Toast.makeText(CircularDeliveryActivity.this, "检查舱门状态失败: " + error.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                    if (btn != null)
                        btn.setEnabled(true);
                });
            }
        });
    }

    private void proceedToNavigation() {
        Intent intent = new Intent(this, CircularDeliveryNavigationActivity.class);
        intent.putExtra("route_name", selectedRoute.getName());
        intent.putExtra("loop_count", selectedRoute.getLoopCount());

        ArrayList<NavigationNode> nodes = (ArrayList<NavigationNode>) selectedRoute.getNodes();
        intent.putExtra("route_nodes", nodes);

        startActivity(intent);
    }

    // Dialog/Overlay related fields
    private View createRouteOverlay;
    private EditText etRouteName;
    private EditText etLoopCount;
    private TextView tvDialogTitle;
    private NodeSelectionAdapter nodeSelectorAdapter;
    private List<NavigationNode> dialogSelectedNodes = new ArrayList<>();
    private int editingRouteIndex = -1; // -1 means creating new

    private void openRouteEditor(@Nullable CircularRoute routeToEdit, int index) {
        if (availableNodes.isEmpty()) {
            Toast.makeText(this, "未获取到点位数据，无法操作", Toast.LENGTH_SHORT).show();
            loadMapNodes();
            return;
        }

        if (createRouteOverlay == null) {
            initOverlay();
        }

        editingRouteIndex = index;
        dialogSelectedNodes.clear();

        if (routeToEdit != null) {
            // Edit Mode
            tvDialogTitle.setText("编辑路线");
            etRouteName.setText(routeToEdit.getName());
            etLoopCount.setText(String.valueOf(routeToEdit.getLoopCount()));

            // Match existing nodes with available nodes by ID to ensure equality checks
            // work
            for (NavigationNode node : routeToEdit.getNodes()) {
                for (NavigationNode avail : availableNodes) {
                    if (avail.getId() == node.getId()) {
                        dialogSelectedNodes.add(avail);
                        break;
                    }
                }
            }
        } else {
            // Create Mode
            tvDialogTitle.setText("新建路线");
            etRouteName.setText("");
            etLoopCount.setText("1");
        }

        if (nodeSelectorAdapter != null) {
            nodeSelectorAdapter.notifyDataSetChanged();
        }
        createRouteOverlay.setVisibility(View.VISIBLE);
    }

    private void initOverlay() {
        createRouteOverlay = findViewById(R.id.create_route_overlay);

        etRouteName = createRouteOverlay.findViewById(R.id.et_route_name);
        etLoopCount = createRouteOverlay.findViewById(R.id.et_loop_count);
        // Assuming the TextView for title is the first one or give it an ID.
        // In previous replace it didn't have ID. Let's try to find it dynamically or
        // assume standard layout.
        // Wait, looking at xml in thought, the title TextView didn't have an ID.
        // I'll grab it by child index? LinearLayout is 0th child of FrameLayout?
        // LinearLayout ll = (LinearLayout)
        // ((FrameLayout)createRouteOverlay).getChildAt(0);
        // tvDialogTitle = (TextView) ll.getChildAt(0);

        LinearLayout ll = (LinearLayout) ((android.widget.FrameLayout) createRouteOverlay).getChildAt(0);
        tvDialogTitle = (TextView) ll.getChildAt(0);

        RecyclerView rvNodes = createRouteOverlay.findViewById(R.id.rv_node_selection);
        Button btnConfirm = createRouteOverlay.findViewById(R.id.btn_confirm_create);
        Button btnCancel = createRouteOverlay.findViewById(R.id.btn_cancel_create);

        rvNodes.setLayoutManager(new GridLayoutManager(this, 3));
        nodeSelectorAdapter = new NodeSelectionAdapter(availableNodes, dialogSelectedNodes);
        rvNodes.setAdapter(nodeSelectorAdapter);

        btnCancel.setOnClickListener(v -> closeOverlay());

        btnConfirm.setOnClickListener(v -> handleSaveRoute());
    }

    private void closeOverlay() {
        if (createRouteOverlay != null) {
            createRouteOverlay.setVisibility(View.GONE);
        }
    }

    private void handleSaveRoute() {
        String name = etRouteName.getText().toString().trim();
        String loopStr = etLoopCount.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "请输入路线名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dialogSelectedNodes.isEmpty()) {
            Toast.makeText(this, "请至少选择一个节点", Toast.LENGTH_SHORT).show();
            return;
        }
        int loop = 1;
        try {
            loop = Integer.parseInt(loopStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的循环次数", Toast.LENGTH_SHORT).show();
            return;
        }

        CircularRoute newRoute = new CircularRoute(name, new ArrayList<>(dialogSelectedNodes), loop);

        if (editingRouteIndex >= 0 && editingRouteIndex < savedRoutes.size()) {
            // Update
            savedRoutes.set(editingRouteIndex, newRoute);
            Toast.makeText(this, "路线已更新", Toast.LENGTH_SHORT).show();
        } else {
            // Create
            savedRoutes.add(newRoute);
            Toast.makeText(this, "路线已创建", Toast.LENGTH_SHORT).show();
        }

        saveRoutesToPrefs();
        routeAdapter.notifyDataSetChanged();
        closeOverlay();
    }

    interface OnRouteClickListener {
        void onClick(CircularRoute route);
    }

    private class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.ViewHolder> {
        private List<CircularRoute> list;
        private OnRouteClickListener listener;

        public RouteAdapter(List<CircularRoute> list, OnRouteClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.widget.LinearLayout root = new android.widget.LinearLayout(parent.getContext());
            int h80dp = (int) (80 * parent.getResources().getDisplayMetrics().density);
            int m8dp = (int) (8 * parent.getResources().getDisplayMetrics().density);

            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, h80dp);
            params.setMargins(m8dp, m8dp, m8dp, m8dp);
            root.setLayoutParams(params);
            root.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            root.setBackgroundResource(R.drawable.rounded_button);
            root.setGravity(android.view.Gravity.CENTER);
            root.setClickable(true);
            root.setFocusable(true);

            TextView tv = new TextView(parent.getContext());
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(20);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            root.addView(tv);

            return new ViewHolder(root, tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CircularRoute route = list.get(position);
            holder.text.setText(route.getName());

            boolean isSelected = (selectedRoute == route);
            holder.itemView
                    .setBackgroundResource(isSelected ? R.drawable.rounded_button_selected : R.drawable.rounded_button);

            holder.itemView.setOnClickListener(v -> listener.onClick(route));

            // Updated Long Click for Edit/Delete
            holder.itemView.setOnLongClickListener(v -> {
                String[] options = { "编辑", "删除" };
                new AlertDialog.Builder(CircularDeliveryActivity.this)
                        .setTitle("操作: " + route.getName())
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                // Edit
                                openRouteEditor(route, position);
                            } else {
                                // Delete
                                confirmDelete(route, position);
                            }
                        })
                        .show();
                return true;
            });
        }

        private void confirmDelete(CircularRoute route, int position) {
            new AlertDialog.Builder(CircularDeliveryActivity.this)
                    .setTitle("删除路线")
                    .setMessage("确定要删除路线 " + route.getName() + " 吗？")
                    .setPositiveButton("确定", (d, w) -> {
                        if (position < list.size()) {
                            list.remove(position);
                            if (selectedRoute == route)
                                selectedRoute = null;
                            saveRoutesToPrefs();
                            notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;

            ViewHolder(View v, TextView tv) {
                super(v);
                text = tv;
            }
        }
    }

    private class NodeSelectionAdapter extends RecyclerView.Adapter<NodeSelectionAdapter.ViewHolder> {
        private List<NavigationNode> allNodes;
        private List<NavigationNode> selectedNodes;

        public NodeSelectionAdapter(List<NavigationNode> all, List<NavigationNode> selected) {
            this.allNodes = all;
            this.selectedNodes = selected;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Programmatically create item_node_selection.xml equivalent
            // XML was: TextView (rounded_button)

            TextView tv = new TextView(parent.getContext());
            int h50dp = (int) (50 * parent.getResources().getDisplayMetrics().density);
            int m4dp = (int) (4 * parent.getResources().getDisplayMetrics().density);

            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, h50dp);
            params.setMargins(m4dp, m4dp, m4dp, m4dp);
            tv.setLayoutParams(params);

            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(16);
            tv.setBackgroundResource(R.drawable.rounded_button);

            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NavigationNode node = allNodes.get(position);
            holder.text.setText(node.getName());

            boolean isSelected = selectedNodes.contains(node);
            holder.text
                    .setBackgroundResource(isSelected ? R.drawable.rounded_button_selected : R.drawable.rounded_button);

            holder.itemView.setOnClickListener(v -> {
                if (isSelected) {
                    selectedNodes.remove(node);
                } else {
                    selectedNodes.add(node);
                }
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() {
            return allNodes.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;

            ViewHolder(View v) {
                super(v);
                text = (TextView) v;
            }
        }
    }

    private final IDoorCallback doorCallback = new IDoorCallback() {
        @Override
        public void onDoorStateChanged(int doorId, int state) {
            runOnUiThread(() -> updateDoorButtonState());
        }

        @Override
        public void onDoorTypeChanged(DoorType type) {
        }

        @Override
        public void onDoorTypeSettingResult(boolean success) {
        }

        @Override
        public void onDoorError(int doorId, int errorCode) {
            runOnUiThread(() -> Toast.makeText(CircularDeliveryActivity.this, "舱门错误: " + errorCode, Toast.LENGTH_SHORT)
                    .show());
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (doorService != null) {
            doorService.unregisterCallback(doorCallback);
        }
    }
}
