package com.weigao.robot.control.ui.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.keenon.sdk.component.navigation.route.RouteNode;
import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IDoorCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.manager.HospitalDeliveryManager;
import com.weigao.robot.control.manager.HospitalItemPresetManager;
import com.weigao.robot.control.manager.TaskExecutionStateManager;
import com.weigao.robot.control.manager.TaskType;
import com.weigao.robot.control.model.DoorType;
import com.weigao.robot.control.model.HospitalDeliveryTask;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.ui.auth.PasswordActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HospitalDeliveryActivity extends AppCompatActivity {
    private static final String TAG = "HospitalDeliveryAct";
    private static final int REQUEST_PASSWORD_FOR_BACK = 2101;
    private static final int REQUEST_PASSWORD_FOR_DOOR = 2102;
    private static final int REQUEST_PASSWORD_FOR_RETURN = 2103;
    private static final int REQUEST_PASSWORD_FOR_HISTORY = 2104;
    private static final int MAX_TASK_COUNT = 3;
    private static final String DISINFECTION_ROOM_EN_NAME = "disinfection";

    public static final List<NavigationNode> originPoints = new ArrayList<>();
    public static final List<NavigationNode> disinfectionPoints = new ArrayList<>();

    private final ArrayList<HospitalDeliveryTask> hospitalTasks = new ArrayList<>();
    private final List<NavigationNode> availableRoomPoints = new ArrayList<>();

    private TextView selectedItemTextView;
    private TextView selectedRoomTextView;
    private TextView taskCountTextView;
    private TextView taskEmptyTextView;
    private Button addTaskButton;
    private Button openDoorButton;
    private TaskAdapter taskAdapter;
    private String selectedItemName;
    private NavigationNode selectedRoomNode;
    private IDoorService doorService;
    private IRobotStateService robotStateService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_delivery_v2);

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
        bindTaskList();
        bindItemPresetList();
        bindPointList();
        updateDraftSummary();
        updateTaskListState();
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

        selectedItemTextView = findViewById(R.id.tv_selected_item);
        selectedRoomTextView = findViewById(R.id.tv_selected_room);
        taskCountTextView = findViewById(R.id.tv_task_count);
        taskEmptyTextView = findViewById(R.id.tv_task_empty);
        addTaskButton = findViewById(R.id.btn_add_task);
        addTaskButton.setOnClickListener(v -> addCurrentTask());

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
                            "获取舱门状态失败: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show());
                }
            });
        });

        findViewById(R.id.start_delivery_button).setOnClickListener(v -> {
            String validationMessage = validateReadyTasks();
            if (validationMessage != null) {
                Toast.makeText(this, validationMessage, Toast.LENGTH_SHORT).show();
                return;
            }
            if (originPoints.isEmpty()) {
                Toast.makeText(this, "未获取到原点，请先在地图中配置原点", Toast.LENGTH_SHORT).show();
                return;
            }
            if (disinfectionPoints.isEmpty()) {
                Toast.makeText(this, "未获取到消毒间，请先在地图中配置消毒间", Toast.LENGTH_SHORT).show();
                return;
            }

            v.setEnabled(false);
            if (robotStateService != null) {
                Log.d(TAG, "【定位】医院配送开始前发起定位校验，tasks=" + hospitalTasks.size()
                        + ", disinfectionPoints=" + disinfectionPoints.size());
                robotStateService.performLocalization(new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            if (!isActivityAlive()) {
                                Log.d(TAG, "【定位】医院配送页已结束，忽略定位成功回调");
                                return;
                            }
                            Log.d(TAG, "【定位】医院配送定位校验成功，进入关门检查");
                            checkDoorsAndStart(v);
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        runOnUiThread(() -> {
                            if (!isActivityAlive()) {
                                Log.d(TAG, "【定位】医院配送页已结束，忽略定位失败回调");
                                return;
                            }
                            Log.e(TAG, "【定位】医院配送定位校验失败: " + error.getMessage());
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

    private void bindTaskList() {
        RecyclerView taskRecyclerView = findViewById(R.id.task_recyclerview);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(hospitalTasks, task -> {
            hospitalTasks.remove(task);
            updateTaskListState();
            Toast.makeText(this, "已移除任务: " + task.getItemName(), Toast.LENGTH_SHORT).show();
        });
        taskRecyclerView.setAdapter(taskAdapter);
    }

    private void bindItemPresetList() {
        RecyclerView presetRecyclerView = findViewById(R.id.item_preset_recyclerview);
        presetRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        List<String> presetItems = HospitalItemPresetManager.getInstance().getPresetItems();
        presetRecyclerView.setAdapter(new ItemPresetAdapter(presetItems, preset -> {
            selectedItemName = preset;
            updateDraftSummary();
            Toast.makeText(this, "已选择物品: " + preset, Toast.LENGTH_SHORT).show();
        }));
    }

    private void bindPointList() {
        RecyclerView pointsRecyclerView = findViewById(R.id.points_recyclerview);
        pointsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        PointAdapter adapter = new PointAdapter(availableRoomPoints, node -> {
            selectedRoomNode = node;
            updateDraftSummary();
            Toast.makeText(this, "已选择房间: " + node.getName(), Toast.LENGTH_SHORT).show();
        });
        pointsRecyclerView.setAdapter(adapter);
        getRealPoints(availableRoomPoints, adapter);
    }

    private void updateDraftSummary() {
        selectedItemTextView.setText("物品: " + (selectedItemName == null ? "未选择" : selectedItemName));
        selectedRoomTextView.setText("房间: " + (selectedRoomNode == null ? "未选择" : selectedRoomNode.getName()));
        addTaskButton.setEnabled(selectedItemName != null
                && selectedRoomNode != null
                && hospitalTasks.size() < MAX_TASK_COUNT);
    }

    private void updateTaskListState() {
        taskAdapter.notifyDataSetChanged();
        taskCountTextView.setText("已添加 " + hospitalTasks.size() + " / " + MAX_TASK_COUNT);
        taskEmptyTextView.setVisibility(hospitalTasks.isEmpty() ? View.VISIBLE : View.GONE);
        updateDraftSummary();
    }

    private void addCurrentTask() {
        if (selectedItemName == null || selectedRoomNode == null) {
            Toast.makeText(this, "请先同时选择物品和房间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (hospitalTasks.size() >= MAX_TASK_COUNT) {
            Toast.makeText(this, "一次任务最多添加 3 个物品", Toast.LENGTH_SHORT).show();
            return;
        }

        hospitalTasks.add(new HospitalDeliveryTask(selectedItemName, cloneNode(selectedRoomNode)));
        selectedItemName = null;
        selectedRoomNode = null;
        updateTaskListState();
        Toast.makeText(this, "任务已加入待配送列表", Toast.LENGTH_SHORT).show();
    }

    @Nullable
    private String validateReadyTasks() {
        if (hospitalTasks.isEmpty()) {
            return "请至少添加 1 条配送任务";
        }
        if (hospitalTasks.size() > MAX_TASK_COUNT) {
            return "一次医院任务最多支持 3 条任务";
        }
        for (HospitalDeliveryTask task : hospitalTasks) {
            if (task.getRoomNode() == null || task.getItemName() == null || task.getItemName().trim().isEmpty()) {
                return "存在未完成的房间或物品配置";
            }
        }
        return null;
    }

    private void getRealPoints(List<NavigationNode> points, PointAdapter adapter) {
        if (robotStateService == null) {
            Log.w(TAG, "getRealPoints skipped: robotStateService is null");
            return;
        }

        Log.d(TAG, "Requesting destination list for hospital delivery");
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

                                String category;
                                if ("origin".equals(type)) {
                                    originPoints.add(node);
                                    category = "origin";
                                } else if (isDisinfectionPointByName(name)) {
                                    disinfectionPoints.add(node);
                                    category = "disinfection";
                                } else if ("normal".equals(type)) {
                                    normalPoints.add(node);
                                    category = "normal";
                                } else {
                                    category = "ignored";
                                }

                                Log.d(TAG, "Destination node parsed: id=" + id
                                        + ", name=" + name
                                        + ", type=" + type
                                        + ", floor=" + node.getFloor()
                                        + ", x=" + node.getX()
                                        + ", y=" + node.getY()
                                        + ", category=" + category);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "医院配送点位解析失败", e);
                    }

                    runOnUiThread(() -> {
                        points.clear();
                        points.addAll(normalPoints);
                        Log.d(TAG, "Destination list ready. originPoints=" + originPoints.size()
                                + ", disinfectionPoints=" + disinfectionPoints.size()
                                + ", roomPoints=" + normalPoints.size());
                        for (NavigationNode node : originPoints) {
                            Log.d(TAG, "Origin point: id=" + node.getId()
                                    + ", name=" + node.getName()
                                    + ", floor=" + node.getFloor()
                                    + ", x=" + node.getX()
                                    + ", y=" + node.getY());
                        }
                        for (NavigationNode node : disinfectionPoints) {
                            Log.d(TAG, "Disinfection point: id=" + node.getId()
                                    + ", name=" + node.getName()
                                    + ", floor=" + node.getFloor()
                                    + ", x=" + node.getX()
                                    + ", y=" + node.getY());
                        }
                        for (NavigationNode node : normalPoints) {
                            Log.d(TAG, "Normal point: id=" + node.getId()
                                    + ", name=" + node.getName()
                                    + ", floor=" + node.getFloor()
                                    + ", x=" + node.getX()
                                    + ", y=" + node.getY());
                        }
                        adapter.notifyDataSetChanged();
                    });
                }).start();
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "getDestinationList failed: " + error.getMessage());
                runOnUiThread(() -> Toast.makeText(
                        HospitalDeliveryActivity.this,
                        "获取配送点列表失败: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    private boolean isDisinfectionPointByName(String name) {
        if (name == null) {
            return false;
        }
        String normalizedName = name.trim();
        if ("\u6d88\u6bd2\u95f4".equals(normalizedName)) {
            return true;
        }
        return DISINFECTION_ROOM_EN_NAME.equalsIgnoreCase(normalizedName);
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
                            "获取舱门状态失败: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void performReturnOperation() {
        Intent intent = new Intent(this, ReturnActivity.class);
        intent.putExtra("return_source_mode", 3);
        intent.putExtra("return_speed",
                com.weigao.robot.control.manager.HospitalDeliverySettingsManager.getInstance().getReturnSpeed());
        startActivity(intent);
    }

    private void checkDoorsAndStart(View trigger) {
        Log.d(TAG, "checkDoorsAndStart called. taskCount=" + hospitalTasks.size()
                + ", disinfectionCount=" + disinfectionPoints.size());
        if (doorService == null) {
            Log.w(TAG, "doorService is null, starting delivery directly");
            startDelivery();
            trigger.setEnabled(true);
            return;
        }

        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                Log.d(TAG, "Door state before hospital start. allClosed=" + allClosed);
                runOnUiThread(() -> {
                    if (Boolean.TRUE.equals(allClosed)) {
                        startDelivery();
                        trigger.setEnabled(true);
                    } else {
                        doorService.closeAllDoors(new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "Doors closed successfully before hospital start");
                                runOnUiThread(() -> new Handler().postDelayed(() -> {
                                    startDelivery();
                                    trigger.setEnabled(true);
                                }, 5000));
                            }

                            @Override
                            public void onError(ApiError error) {
                                Log.e(TAG, "closeAllDoors before hospital start failed: " + error.getMessage());
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
                Log.e(TAG, "isAllDoorsClosed before hospital start failed: " + error.getMessage());
                runOnUiThread(() -> {
                    trigger.setEnabled(true);
                    Toast.makeText(
                            HospitalDeliveryActivity.this,
                            "获取舱门状态失败: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void startDelivery() {
        StringBuilder taskSummary = new StringBuilder();
        for (HospitalDeliveryTask task : hospitalTasks) {
            if (taskSummary.length() > 0) {
                taskSummary.append(" | ");
            }
            String roomName = task.getRoomNode() == null ? "null" : task.getRoomNode().getName();
            int roomId = task.getRoomNode() == null ? -1 : task.getRoomNode().getId();
            taskSummary.append(task.getItemName())
                    .append("->")
                    .append(roomName)
                    .append("(")
                    .append(roomId)
                    .append(")");
        }
        Log.d(TAG, "startDelivery hospital. taskCount=" + hospitalTasks.size()
                + ", disinfectionNode=" + disinfectionPoints.get(0).getName()
                + "(" + disinfectionPoints.get(0).getId() + ")"
                + ", tasks=" + taskSummary);
        HospitalDeliveryManager.getInstance().startDelivery();
        TaskExecutionStateManager.getInstance().startTask(TaskType.HOSPITAL_DELIVERY);
        Intent intent = new Intent(this, HospitalDeliveryNavigationActivity.class);
        intent.putExtra("hospital_tasks", new ArrayList<>(hospitalTasks));
        intent.putExtra("disinfection_node", disinfectionPoints.get(0));
        startActivity(intent);
    }

    private boolean isActivityAlive() {
        return !isFinishing() && !isDestroyed();
    }
    private NavigationNode cloneNode(NavigationNode node) {
        NavigationNode copy = new NavigationNode();
        copy.setId(node.getId());
        copy.setName(node.getName());
        copy.setFloor(node.getFloor());
        copy.setX(node.getX());
        copy.setY(node.getY());
        copy.setPhi(node.getPhi());
        copy.setRouteNode(node.getRouteNode());
        return copy;
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

    private static class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
        private final List<HospitalDeliveryTask> tasks;
        private final OnTaskDeleteListener listener;

        TaskAdapter(List<HospitalDeliveryTask> tasks, OnTaskDeleteListener listener) {
            this.tasks = tasks;
            this.listener = listener;
        }

        @NonNull
        @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_hospital_task, parent, false);
            return new TaskViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
            HospitalDeliveryTask task = tasks.get(position);
            holder.roomTextView.setText(task.getRoomNode() == null ? "未选择房间" : task.getRoomNode().getName());
            holder.itemTextView.setText("物品: " + task.getItemName());
            holder.layerTextView.setText("层位: " + task.getAssignedLayerLabel());
            holder.deleteButton.setOnClickListener(v -> listener.onDelete(task));
        }

        @Override
        public int getItemCount() {
            return tasks.size();
        }

        static class TaskViewHolder extends RecyclerView.ViewHolder {
            final TextView roomTextView;
            final TextView itemTextView;
            final TextView layerTextView;
            final View deleteButton;

            TaskViewHolder(View itemView) {
                super(itemView);
                roomTextView = itemView.findViewById(R.id.tv_task_room);
                itemTextView = itemView.findViewById(R.id.tv_task_item);
                layerTextView = itemView.findViewById(R.id.tv_task_layer);
                deleteButton = itemView.findViewById(R.id.btn_delete_task);
            }
        }
    }

    private interface OnTaskDeleteListener {
        void onDelete(HospitalDeliveryTask task);
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
