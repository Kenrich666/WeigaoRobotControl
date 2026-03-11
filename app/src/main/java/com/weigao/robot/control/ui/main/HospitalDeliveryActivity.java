package com.weigao.robot.control.ui.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.keenon.sdk.component.navigation.route.RouteNode;
import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IDoorCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.manager.HospitalDeliveryManager;
import com.weigao.robot.control.manager.HospitalItemPresetManager;
import com.weigao.robot.control.model.DoorType;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.ui.auth.PasswordActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HospitalDeliveryActivity extends AppCompatActivity {
    private static final String TAG = "HospitalDeliveryAct";
    private static final int REQUEST_PASSWORD_FOR_BACK = 2101;
    private static final int REQUEST_PASSWORD_FOR_DOOR = 2102;
    private static final int REQUEST_PASSWORD_FOR_RETURN = 2103;
    private static final int REQUEST_PASSWORD_FOR_HISTORY = 2104;
    private static final String[] ITEM_PRESETS = {
            "胃镜",
            "肠镜"
    };

    public static final List<NavigationNode> originPoints = new ArrayList<>();
    public static final List<NavigationNode> disinfectionPoints = new ArrayList<>();

    private final HashMap<Integer, NavigationNode> pairings = new HashMap<>();
    private final HashMap<Integer, String> layerItems = new HashMap<>();
    private Button selectedLayerButton;
    private Button openDoorButton;
    private Button clearSelectedLayerButton;
    private TextView selectedLayerSummaryTextView;
    private TextView selectedLayerDetailTextView;
    private IDoorService doorService;
    private IRobotStateService robotStateService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_delivery);

        HospitalDeliveryManager.getInstance().init(this);
        doorService = ServiceManager.getInstance().getDoorService();
        robotStateService = ServiceManager.getInstance().getRobotStateService();

        if (doorService != null) {
            doorService.registerCallback(doorCallback);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
                doorBroadcastReceiver,
                new IntentFilter("com.weigao.robot.DOOR_STATE_CHANGED"));

        bindCommonActions();
        bindLayerSelection();
        bindItemPresetList();
        bindPointList();
        resetPairingState();
        updateAllLayerButtons();
        updateDoorButtonState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindItemPresetList();
    }

    private void bindCommonActions() {
        findViewById(R.id.back_button).setOnClickListener(v -> startActivityForResult(
                new Intent(this, PasswordActivity.class), REQUEST_PASSWORD_FOR_BACK));
        findViewById(R.id.history_button).setOnClickListener(v -> startActivityForResult(
                new Intent(this, PasswordActivity.class), REQUEST_PASSWORD_FOR_HISTORY));
        findViewById(R.id.return_button).setOnClickListener(v -> startActivityForResult(
                new Intent(this, PasswordActivity.class), REQUEST_PASSWORD_FOR_RETURN));
        selectedLayerSummaryTextView = findViewById(R.id.tv_selected_layer_summary);
        selectedLayerDetailTextView = findViewById(R.id.tv_selected_layer_detail);
        clearSelectedLayerButton = findViewById(R.id.btn_clear_selected_layer);
        clearSelectedLayerButton.setOnClickListener(v -> {
            if (selectedLayerButton == null) {
                Toast.makeText(this, "请先选择要清空的货层", Toast.LENGTH_SHORT).show();
                return;
            }
            int layerId = selectedLayerButton.getId();
            if (!hasLayerConfig(layerId)) {
                Toast.makeText(this, getLayerLabel(layerId) + "暂无可清空的配置", Toast.LENGTH_SHORT).show();
                return;
            }
            clearLayerConfig(layerId);
            Toast.makeText(this, getLayerLabel(layerId) + "已清空房间和物品", Toast.LENGTH_SHORT).show();
        });

        openDoorButton = findViewById(R.id.open_door_button);
        openDoorButton.setOnClickListener(v -> {
            if (doorService == null) {
                return;
            }
            doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean allClosed) {
                    runOnUiThread(() -> {
                        if (Boolean.TRUE.equals(allClosed)) {
                            startActivityForResult(
                                    new Intent(HospitalDeliveryActivity.this, PasswordActivity.class),
                                    REQUEST_PASSWORD_FOR_DOOR);
                        } else {
                            performDoorOperation();
                        }
                    });
                }

                @Override
                public void onError(ApiError error) {
                    runOnUiThread(() -> Toast.makeText(
                            HospitalDeliveryActivity.this,
                            "查询舱门状态失败: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show());
                }
            });
        });

        findViewById(R.id.start_delivery_button).setOnClickListener(v -> {
            String validationMessage = validateReadyLayers();
            if (validationMessage != null) {
                Toast.makeText(this, validationMessage, Toast.LENGTH_SHORT).show();
                return;
            }
            if (originPoints.isEmpty()) {
                Toast.makeText(this, "未获取到原点，请先在地图中配置", Toast.LENGTH_SHORT).show();
                return;
            }
            if (disinfectionPoints.isEmpty()) {
                Toast.makeText(this, "未获取到消毒间，请先在地图中配置", Toast.LENGTH_SHORT).show();
                return;
            }

            v.setEnabled(false);
            if (robotStateService != null) {
                robotStateService.performLocalization(new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> checkDoorsAndStart(v));
                    }

                    @Override
                    public void onError(ApiError error) {
                        runOnUiThread(() -> {
                            v.setEnabled(true);
                            startActivity(new Intent(
                                    HospitalDeliveryActivity.this,
                                    PositioningFailedActivity.class));
                        });
                    }
                });
            } else {
                checkDoorsAndStart(v);
            }
        });
    }

    private void bindLayerSelection() {
        Button l1Button = findViewById(R.id.l1_button);
        Button l2Button = findViewById(R.id.l2_button);
        Button l3Button = findViewById(R.id.l3_button);

        View.OnClickListener layerClickListener = v -> {
            Button clickedButton = (Button) v;
            if (clickedButton == selectedLayerButton) {
                clearLayerSelection();
                return;
            }
            selectLayer(clickedButton);
        };

        View.OnLongClickListener layerLongClickListener = v -> {
            Button clickedButton = (Button) v;
            int layerId = clickedButton.getId();
            if (!hasLayerConfig(layerId)) {
                Toast.makeText(this, getLayerLabel(layerId) + "暂无可清空的配置", Toast.LENGTH_SHORT).show();
                return true;
            }
            clearLayerConfig(layerId);
            Toast.makeText(this, getLayerLabel(layerId) + "已清空房间和物品", Toast.LENGTH_SHORT).show();
            return true;
        };

        l1Button.setOnClickListener(layerClickListener);
        l2Button.setOnClickListener(layerClickListener);
        l3Button.setOnClickListener(layerClickListener);

        l1Button.setOnLongClickListener(layerLongClickListener);
        l2Button.setOnLongClickListener(layerLongClickListener);
        l3Button.setOnLongClickListener(layerLongClickListener);
    }

    private void bindItemPresetList() {
        RecyclerView presetRecyclerView = findViewById(R.id.item_preset_recyclerview);
        presetRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        List<String> presetItems = HospitalItemPresetManager.getInstance().getPresetItems();

        presetRecyclerView.setAdapter(new ItemPresetAdapter(presetItems, preset -> {
            if (selectedLayerButton == null) {
                Toast.makeText(this, "请先点击左侧货层，再选择预设物品", Toast.LENGTH_SHORT).show();
                return;
            }

            int layerId = selectedLayerButton.getId();
            layerItems.put(layerId, preset);
            updateAllLayerButtons();
            Toast.makeText(
                    this,
                    getLayerLabel(layerId) + "已设置物品: " + preset + buildNextStepHint(layerId),
                    Toast.LENGTH_SHORT).show();
        }));
    }

    private void bindPointList() {
        RecyclerView pointsRecyclerView = findViewById(R.id.points_recyclerview);
        pointsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        List<NavigationNode> points = new ArrayList<>();
        PointAdapter adapter = new PointAdapter(points, node -> {
            if (selectedLayerButton == null) {
                Toast.makeText(this, "请先点击左侧货层，再选择房间", Toast.LENGTH_SHORT).show();
                return;
            }

            int layerId = selectedLayerButton.getId();
            pairings.put(layerId, node);
            updateAllLayerButtons();
            Toast.makeText(
                    this,
                    getLayerLabel(layerId) + "已绑定房间: " + node.getName() + buildNextStepHint(layerId),
                    Toast.LENGTH_SHORT).show();
        });
        pointsRecyclerView.setAdapter(adapter);
        getRealPoints(points, adapter);
    }

    @Nullable
    private String validateReadyLayers() {
        int[] layerIds = {R.id.l1_button, R.id.l2_button, R.id.l3_button};
        int completeLayerCount = 0;
        for (int layerId : layerIds) {
            boolean hasRoom = pairings.containsKey(layerId);
            String itemName = layerItems.get(layerId);
            boolean hasItem = itemName != null && !itemName.trim().isEmpty();
            if (hasRoom && hasItem) {
                completeLayerCount++;
                continue;
            }
            if (hasRoom || hasItem) {
                return "请为已配置货层同时设置物品和房间";
            }
        }
        if (completeLayerCount == 0) {
            return "请至少为一个货层同时设置物品和房间";
        }
        return null;
    }

    private void getRealPoints(List<NavigationNode> points, PointAdapter adapter) {
        if (robotStateService == null) {
            return;
        }

        robotStateService.getDestinationList(new IResultCallback<String>() {
            @Override
            public void onSuccess(String result) {
                new Thread(() -> {
                    List<NavigationNode> normalPoints = new ArrayList<>();
                    originPoints.clear();
                    disinfectionPoints.clear();

                    try {
                        JSONObject resultObj = new JSONObject(result);
                        JSONArray jsonArray = resultObj.optJSONArray("data");
                        if (jsonArray != null) {
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject obj = jsonArray.getJSONObject(i);
                                NavigationNode node = new NavigationNode();
                                int id = obj.optInt("id");
                                String name = obj.optString("name");
                                String type = obj.optString("type");
                                if (name.isEmpty()) {
                                    name = String.valueOf(id);
                                }
                                node.setId(id);
                                node.setName(name);
                                node.setFloor(obj.optInt("floor"));

                                JSONObject pose = obj.optJSONObject("pose");
                                if (pose != null) {
                                    JSONObject position = pose.optJSONObject("position");
                                    if (position != null) {
                                        node.setX(position.optDouble("x"));
                                        node.setY(position.optDouble("y"));
                                    }
                                }

                                RouteNode routeNode = new RouteNode();
                                routeNode.setId(id);
                                routeNode.setName(name);
                                node.setRouteNode(routeNode);

                                if ("origin".equals(type)) {
                                    originPoints.add(node);
                                } else if (isDisinfectionPoint(name, type)) {
                                    disinfectionPoints.add(node);
                                } else if ("normal".equals(type)) {
                                    normalPoints.add(node);
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "医院配送点位解析失败", e);
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
                runOnUiThread(() -> Toast.makeText(
                        HospitalDeliveryActivity.this,
                        "获取点位失败: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateAllLayerButtons() {
        updateLayerButton(findViewById(R.id.l1_button));
        updateLayerButton(findViewById(R.id.l2_button));
        updateLayerButton(findViewById(R.id.l3_button));
        updateSelectionPanel();
    }

    private void resetPairingState() {
        pairings.clear();
        layerItems.clear();
        selectedLayerButton = null;
    }

    private void updateLayerButton(Button button) {
        updateLayerButtonText(button);
        refreshLayerStyle(button);
    }

    private void updateLayerButtonText(Button button) {
        int layerId = button.getId();
        String itemName = layerItems.get(layerId);
        NavigationNode node = pairings.get(layerId);

        StringBuilder textBuilder = new StringBuilder(getLayerLabel(layerId));
        if (itemName != null && !itemName.isEmpty() && node != null && node.getName() != null && !node.getName().isEmpty()) {
            textBuilder.append("  已配置");
        } else if (itemName != null && !itemName.isEmpty()) {
            textBuilder.append("  已选物品");
        } else if (node != null && node.getName() != null && !node.getName().isEmpty()) {
            textBuilder.append("  已选房间");
        }

        button.setText(textBuilder.toString());
    }

    private String getLayerLabel(int buttonId) {
        if (buttonId == R.id.l1_button) {
            return "L1 层";
        }
        if (buttonId == R.id.l2_button) {
            return "L2 层";
        }
        return "L3 层";
    }

    private void refreshLayerStyle(Button button) {
        if (button == selectedLayerButton) {
            button.setBackgroundResource(R.drawable.bg_shelf_selected);
            button.setTextColor(ContextCompat.getColor(this, R.color.medical_primary));
        } else if (hasLayerConfig(button.getId())) {
            button.setBackgroundResource(R.drawable.bg_shelf_paired);
            button.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            button.setBackgroundResource(R.drawable.selector_shelf_item);
            button.setTextColor(ContextCompat.getColor(this, R.color.medical_text_primary));
        }
    }

    private void selectLayer(Button button) {
        selectedLayerButton = button;
        updateAllLayerButtons();
        Toast.makeText(this, buildLayerSelectionMessage(button.getId()), Toast.LENGTH_SHORT).show();
    }

    private void clearLayerSelection() {
        selectedLayerButton = null;
        updateAllLayerButtons();
    }

    private void clearLayerConfig(int layerId) {
        pairings.remove(layerId);
        layerItems.remove(layerId);
        if (selectedLayerButton != null && selectedLayerButton.getId() == layerId) {
            selectedLayerButton = null;
        }
        updateAllLayerButtons();
    }

    private boolean hasLayerConfig(int layerId) {
        return pairings.containsKey(layerId) || layerItems.containsKey(layerId);
    }

    private String buildLayerSelectionMessage(int layerId) {
        String itemName = layerItems.get(layerId);
        NavigationNode node = pairings.get(layerId);
        if ((itemName == null || itemName.trim().isEmpty()) && node == null) {
            return getLayerLabel(layerId) + "已选中，请继续选择物品和房间";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(getLayerLabel(layerId)).append("编辑中");
        if (itemName != null && !itemName.trim().isEmpty()) {
            builder.append(" | 物品: ").append(itemName);
        }
        if (node != null && node.getName() != null && !node.getName().isEmpty()) {
            builder.append(" | 房间: ").append(node.getName());
        }
        builder.append(" | 长按可清空");
        return builder.toString();
    }

    private String buildNextStepHint(int layerId) {
        boolean hasRoom = pairings.containsKey(layerId);
        String itemName = layerItems.get(layerId);
        boolean hasItem = itemName != null && !itemName.trim().isEmpty();
        if (hasRoom && hasItem) {
            return "，当前层已配置完成";
        }
        if (!hasItem) {
            return "，请继续选择物品";
        }
        return "，请继续选择房间";
    }

    private void updateSelectionPanel() {
        if (selectedLayerSummaryTextView == null || selectedLayerDetailTextView == null || clearSelectedLayerButton == null) {
            return;
        }

        if (selectedLayerButton == null) {
            selectedLayerSummaryTextView.setText("未选择货层");
            selectedLayerDetailTextView.setText("请先点击左侧货层，再配置该层的物品和房间。");
            clearSelectedLayerButton.setEnabled(false);
            return;
        }

        int layerId = selectedLayerButton.getId();
        selectedLayerSummaryTextView.setText(getLayerLabel(layerId));

        String itemName = layerItems.get(layerId);
        NavigationNode node = pairings.get(layerId);
        StringBuilder detailBuilder = new StringBuilder();
        detailBuilder.append("物品：")
                .append(itemName == null || itemName.trim().isEmpty() ? "未设置" : itemName)
                .append("\n房间：")
                .append(node != null && node.getName() != null && !node.getName().isEmpty() ? node.getName() : "未设置");

        if (!hasLayerConfig(layerId)) {
            detailBuilder.append("\n下一步：先选物品，再选房间。");
        } else {
            detailBuilder.append("\n说明：重新选择会直接覆盖当前配置。");
        }

        selectedLayerDetailTextView.setText(detailBuilder.toString());
        clearSelectedLayerButton.setEnabled(hasLayerConfig(layerId));
    }

    private boolean isDisinfectionPoint(String name, String type) {
        if ("disinfection".equals(type)) {
            return true;
        }
        if (name == null) {
            return false;
        }
        String normalizedName = name.trim();
        return normalizedName.contains("消毒间") || normalizedName.contains("消毒");
    }

    private void updateDoorButtonState() {
        if (openDoorButton == null || doorService == null) {
            return;
        }
        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                runOnUiThread(() -> {
                    openDoorButton.setText(Boolean.TRUE.equals(allClosed) ? "手动开门" : "关闭舱门");
                    new Handler().postDelayed(() -> openDoorButton.setEnabled(true), 1500);
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> openDoorButton.setEnabled(true));
            }
        });
    }

    private void performDoorOperation() {
        if (doorService == null) {
            return;
        }
        openDoorButton.setEnabled(false);
        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                if (Boolean.TRUE.equals(allClosed)) {
                    doorService.openAllDoors(false, new IResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            runOnUiThread(() -> {
                                updateDoorButtonState();
                                Toast.makeText(HospitalDeliveryActivity.this, "舱门已打开", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(ApiError error) {
                            runOnUiThread(() -> {
                                openDoorButton.setEnabled(true);
                                Toast.makeText(
                                        HospitalDeliveryActivity.this,
                                        "开门失败: " + error.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                } else {
                    doorService.closeAllDoors(new IResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            runOnUiThread(() -> {
                                updateDoorButtonState();
                                Toast.makeText(HospitalDeliveryActivity.this, "舱门已关闭", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(ApiError error) {
                            runOnUiThread(() -> {
                                openDoorButton.setEnabled(true);
                                Toast.makeText(
                                        HospitalDeliveryActivity.this,
                                        "关门失败: " + error.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                }
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> {
                    openDoorButton.setEnabled(true);
                    Toast.makeText(
                            HospitalDeliveryActivity.this,
                            "查询舱门状态失败: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void performReturnOperation() {
        Intent intent = new Intent(this, ReturnActivity.class);
        intent.putExtra("return_source_mode", 3);
        intent.putExtra("return_speed", com.weigao.robot.control.manager.HospitalDeliverySettingsManager.getInstance().getReturnSpeed());
        startActivity(intent);
    }

    private void checkDoorsAndStart(View trigger) {
        if (doorService == null) {
            startDelivery();
            trigger.setEnabled(true);
            return;
        }

        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                runOnUiThread(() -> {
                    if (Boolean.TRUE.equals(allClosed)) {
                        startDelivery();
                        trigger.setEnabled(true);
                    } else {
                        doorService.closeAllDoors(new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                runOnUiThread(() -> new Handler().postDelayed(() -> {
                                    startDelivery();
                                    trigger.setEnabled(true);
                                }, 5000));
                            }

                            @Override
                            public void onError(ApiError error) {
                                runOnUiThread(() -> {
                                    trigger.setEnabled(true);
                                    Toast.makeText(
                                            HospitalDeliveryActivity.this,
                                            "自动关门失败: " + error.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> {
                    trigger.setEnabled(true);
                    Toast.makeText(
                            HospitalDeliveryActivity.this,
                            "查询舱门状态失败: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void startDelivery() {
        HospitalDeliveryManager.getInstance().startDelivery();
        Intent intent = new Intent(this, HospitalDeliveryNavigationActivity.class);
        intent.putExtra("pairings", pairings);
        intent.putExtra("layer_items", layerItems);
        intent.putExtra("disinfection_node", disinfectionPoints.get(0));
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_PASSWORD_FOR_BACK:
                finish();
                break;
            case REQUEST_PASSWORD_FOR_DOOR:
                performDoorOperation();
                break;
            case REQUEST_PASSWORD_FOR_RETURN:
                performReturnOperation();
                break;
            case REQUEST_PASSWORD_FOR_HISTORY:
                startActivity(new Intent(this, HospitalDeliveryHistoryActivity.class));
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (doorService != null) {
            doorService.unregisterCallback(doorCallback);
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(doorBroadcastReceiver);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            com.weigao.robot.control.app.WeigaoApplication.applyFullScreen(this);
        }
    }

    private final IDoorCallback doorCallback = new IDoorCallback() {
        @Override
        public void onDoorStateChanged(int doorId, int state) {
            runOnUiThread(HospitalDeliveryActivity.this::updateDoorButtonState);
        }

        @Override
        public void onDoorTypeChanged(DoorType type) {
        }

        @Override
        public void onDoorTypeSettingResult(boolean success) {
        }

        @Override
        public void onDoorError(int doorId, int errorCode) {
        }
    };

    private final BroadcastReceiver doorBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateDoorButtonState();
        }
    };

    private static class PointAdapter extends RecyclerView.Adapter<PointAdapter.PointViewHolder> {
        private final List<NavigationNode> points;
        private final OnPointClickListener listener;

        PointAdapter(List<NavigationNode> points, OnPointClickListener listener) {
            this.points = points;
            this.listener = listener;
        }

        @NonNull
        @Override
        public PointViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PointViewHolder(buildGridButton(parent));
        }

        @Override
        public void onBindViewHolder(@NonNull PointViewHolder holder, int position) {
            NavigationNode node = points.get(position);
            holder.pointButton.setText(node.getName());
            holder.pointButton.setOnClickListener(v -> listener.onPointClick(node));
        }

        @Override
        public int getItemCount() {
            return points.size();
        }

        static class PointViewHolder extends RecyclerView.ViewHolder {
            final Button pointButton;

            PointViewHolder(Button button) {
                super(button);
                this.pointButton = button;
            }
        }
    }

    private interface OnPointClickListener {
        void onPointClick(NavigationNode node);
    }

    private static class ItemPresetAdapter extends RecyclerView.Adapter<ItemPresetAdapter.ItemPresetViewHolder> {
        private final List<String> presets;
        private final OnPresetClickListener listener;

        ItemPresetAdapter(List<String> presets, OnPresetClickListener listener) {
            this.presets = presets;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ItemPresetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ItemPresetViewHolder(buildGridButton(parent));
        }

        @Override
        public void onBindViewHolder(@NonNull ItemPresetViewHolder holder, int position) {
            String preset = presets.get(position);
            holder.button.setText(preset);
            holder.button.setOnClickListener(v -> listener.onPresetClick(preset));
        }

        @Override
        public int getItemCount() {
            return presets.size();
        }

        static class ItemPresetViewHolder extends RecyclerView.ViewHolder {
            final Button button;

            ItemPresetViewHolder(Button button) {
                super(button);
                this.button = button;
            }
        }
    }

    private interface OnPresetClickListener {
        void onPresetClick(String preset);
    }

    private static Button buildGridButton(ViewGroup parent) {
        Button button = new Button(parent.getContext());
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) (8 * parent.getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin, margin, margin);
        button.setLayoutParams(params);
        button.setBackgroundResource(R.drawable.item_point_style);
        button.setTextColor(Color.BLACK);
        return button;
    }
}
