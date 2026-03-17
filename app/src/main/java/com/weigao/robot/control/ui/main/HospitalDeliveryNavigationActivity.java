package com.weigao.robot.control.ui.main;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.keenon.sdk.component.navigation.common.Navigation;
import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IDoorCallback;
import com.weigao.robot.control.callback.INavigationCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.manager.HospitalDeliveryManager;
import com.weigao.robot.control.manager.HospitalDeliverySettingsManager;
import com.weigao.robot.control.manager.LowBatteryAutoChargeManager;
import com.weigao.robot.control.manager.TaskExecutionStateManager;
import com.weigao.robot.control.manager.WorkScheduleService;
import com.weigao.robot.control.model.DoorType;
import com.weigao.robot.control.model.HospitalDeliveryRecord;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.INavigationService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.ui.auth.PasswordActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HospitalDeliveryNavigationActivity extends AppCompatActivity implements INavigationCallback {
    private static final String TAG = "HospitalDeliveryNav";
    private static final int REQUEST_CODE_CONFIRM_RECEIPT = 2201;
    private static final int REQUEST_CODE_END_PASSWORD = 2202;
    private static final int REQUEST_CODE_DOOR_PASSWORD = 2203;
    private static final int REQUEST_CODE_RETURN_PASSWORD = 2204;

    private enum FlowStage {
        TO_DISINFECTION,
        WAIT_AT_DISINFECTION,
        TO_ROOMS
    }

    private TextView tvStatus;
    private TextView currentTaskTextView;
    private TextView tvHint;
    private TextView tvCountdown;
    private LinearLayout llPauseControls;
    private LinearLayout layoutTaskSummaryPanel;
    private TableLayout tableTaskSummary;
    private Button btnPauseEnd;
    private Button btnReturn;
    private Button btnDoorToggle;
    private Button btnContinue;
    private View rootLayout;

    private INavigationService navigationService;
    private IDoorService doorService;
    private HashMap<Integer, NavigationNode> pairings;
    private HashMap<Integer, String> layerItems;
    private NavigationNode disinfectionNode;
    private List<Map.Entry<Integer, NavigationNode>> deliveryTasks;
    private List<List<Map.Entry<Integer, NavigationNode>>> uniqueDeliveryTasks;
    private List<NavigationNode> targetNodes;
    private int currentUniqueTargetIndex = 0;
    private boolean isNavigating = false;
    private boolean isMissionFinished = false;
    private boolean isPaused = false;
    private boolean isReturning = false;
    private FlowStage flowStage = FlowStage.TO_DISINFECTION;
    private GestureDetector gestureDetector;
    private CountDownTimer autoResumeTimer;
    private boolean handoffToLowBatteryAutoCharge;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_delivery_navigation);

        initViews();
        initServices();
        initData();
        setupGestureDetector();
        setupButtons();
        startToDisinfection();
    }

    private void initViews() {
        rootLayout = findViewById(R.id.root_layout);
        tvStatus = findViewById(R.id.tv_status);
        currentTaskTextView = findViewById(R.id.current_task_textview);
        tvHint = findViewById(R.id.tv_hint);
        tvCountdown = findViewById(R.id.tv_countdown);
        llPauseControls = findViewById(R.id.ll_pause_controls);
        layoutTaskSummaryPanel = findViewById(R.id.layout_task_summary_panel);
        tableTaskSummary = findViewById(R.id.table_task_summary);
        btnPauseEnd = findViewById(R.id.btn_pause_end);
        btnReturn = findViewById(R.id.btn_return);
        btnDoorToggle = findViewById(R.id.btn_toggle_door);
        btnContinue = findViewById(R.id.btn_continue);
    }

    private void initServices() {
        navigationService = ServiceManager.getInstance().getNavigationService();
        doorService = ServiceManager.getInstance().getDoorService();
        if (navigationService == null) {
            Toast.makeText(this, "导航服务未初始化", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        navigationService.registerCallback(this);
        if (doorService != null) {
            doorService.registerCallback(doorCallback);
        }
    }

    @SuppressWarnings("unchecked")
    private void initData() {
        pairings = (HashMap<Integer, NavigationNode>) getIntent().getSerializableExtra("pairings");
        layerItems = (HashMap<Integer, String>) getIntent().getSerializableExtra("layer_items");
        disinfectionNode = (NavigationNode) getIntent().getSerializableExtra("disinfection_node");
        if (pairings == null || pairings.isEmpty() || disinfectionNode == null) {
            Toast.makeText(this, "医院配送任务无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (layerItems == null) {
            layerItems = new HashMap<>();
        }

        deliveryTasks = new ArrayList<>(pairings.entrySet());
        Collections.sort(deliveryTasks,
                (o1, o2) -> Integer.compare(getLayerNumber(o1.getKey()), getLayerNumber(o2.getKey())));
        prepareRoomTargets();
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isNavigating && !isPaused && flowStage != FlowStage.WAIT_AT_DISINFECTION) {
                    pauseNavigation();
                }
                return true;
            }
        });

        rootLayout.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void setupButtons() {
        btnPauseEnd.setOnClickListener(v -> startActivityForResult(
                new Intent(this, PasswordActivity.class), REQUEST_CODE_END_PASSWORD));

        btnContinue.setOnClickListener(v -> {
            if (flowStage == FlowStage.WAIT_AT_DISINFECTION) {
                checkDoorsAndStartRoomNavigation();
            } else if (isPaused) {
                resumeNavigation();
            }
        });

        if (btnDoorToggle != null) {
            btnDoorToggle.setOnClickListener(v -> toggleDoorState());
            updateDoorToggleButton();
        }

        if (btnReturn != null) {
            btnReturn.setVisibility(View.GONE);
        }
        if (btnDoorToggle != null) {
            btnDoorToggle.setVisibility(View.GONE);
        }
    }

    private void prepareRoomTargets() {
        uniqueDeliveryTasks = new ArrayList<>();
        targetNodes = new ArrayList<>();
        List<Integer> addedIds = new ArrayList<>();

        for (Map.Entry<Integer, NavigationNode> task : deliveryTasks) {
            NavigationNode node = task.getValue();
            if (node == null) {
                continue;
            }
            int nodeId = node.getId();
            int existingIndex = addedIds.indexOf(nodeId);
            if (existingIndex == -1) {
                addedIds.add(nodeId);
                targetNodes.add(node);
                List<Map.Entry<Integer, NavigationNode>> group = new ArrayList<>();
                group.add(task);
                uniqueDeliveryTasks.add(group);
            } else {
                uniqueDeliveryTasks.get(existingIndex).add(task);
            }
        }
    }

    private void startToDisinfection() {
        flowStage = FlowStage.TO_DISINFECTION;
        isPaused = false;
        stopAutoResumeTimer();
        hideTaskSummaryPanel();
        if (btnDoorToggle != null) {
            btnDoorToggle.setVisibility(View.GONE);
        }
        tvStatus.setText("前往消毒间");
        currentTaskTextView.setText("正在前往消毒间：" + disinfectionNode.getName());
        tvHint.setVisibility(View.VISIBLE);
        tvHint.setText("双击屏幕可暂停导航");
        showPauseControls(false);
        ensureDoorsClosedBeforeMove(
                () -> configureAndPrepareNavigation(Collections.singletonList(disinfectionNode), false),
                "前往消毒间前关门失败");
    }

    private void startRoomNavigation() {
        if (targetNodes == null || targetNodes.isEmpty()) {
            Toast.makeText(this, "没有可配送的房间目标", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        flowStage = FlowStage.TO_ROOMS;
        currentUniqueTargetIndex = 0;
        isPaused = false;
        stopAutoResumeTimer();
        hideTaskSummaryPanel();
        tvStatus.setText("医院配送中");
        tvHint.setVisibility(View.VISIBLE);
        tvHint.setText("双击屏幕可暂停导航");
        showPauseControls(false);
        updateRoomTaskText();
        ensureDoorsClosedBeforeMove(
                () -> configureAndPrepareNavigation(targetNodes, true),
                "继续配送前关门失败");
    }

    private void checkDoorsAndStartRoomNavigation() {
        btnContinue.setEnabled(false);
        ensureDoorsClosedBeforeMove(() -> {
            btnContinue.setEnabled(true);
            startRoomNavigation();
        }, "继续导航前关门失败");
    }

    private void configureAndPrepareNavigation(List<NavigationNode> nodes, boolean roomPhase) {
        List<Integer> targetIds = new ArrayList<>();
        for (NavigationNode node : nodes) {
            targetIds.add(node.getId());
        }

        isNavigating = true;
        navigationService.setTargets(targetIds, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                int speed = HospitalDeliverySettingsManager.getInstance().getDeliverySpeed();
                navigationService.setSpeed(speed, new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        navigationService.prepare(new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "Navigation prepared, roomPhase=" + roomPhase);
                            }

                            @Override
                            public void onError(ApiError error) {
                                handlePreparationError("准备路线失败", error, roomPhase);
                            }
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        handlePreparationError("设置速度失败", error, roomPhase);
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                handlePreparationError("设置目标失败", error, roomPhase);
            }
        });
    }

    private void handlePreparationError(String label, ApiError error, boolean roomPhase) {
        if (roomPhase && currentUniqueTargetIndex < targetNodes.size()) {
            HospitalDeliveryManager.getInstance().recordPointArrival(
                    targetNodes.get(currentUniqueTargetIndex).getName(),
                    HospitalDeliveryRecord.STATUS_NAV_FAILED,
                    HospitalDeliveryRecord.STAGE_ROOM);
        } else if (!roomPhase && disinfectionNode != null) {
            HospitalDeliveryManager.getInstance().recordPointArrival(
                    disinfectionNode.getName(),
                    HospitalDeliveryRecord.STATUS_NAV_FAILED,
                    HospitalDeliveryRecord.STAGE_DISINFECTION);
        }
        runOnUiThread(() -> {
            cancelActiveTask();
            Toast.makeText(this, label + ": " + error.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private boolean handoffToLowBatteryAutoChargeIfNeeded() {
        if (!LowBatteryAutoChargeManager.getInstance().hasPendingTaskCompletionAutoCharge()) {
            return false;
        }
        TaskExecutionStateManager.getInstance().finishTask();
        LowBatteryAutoChargeManager.getInstance().onTaskCompletedAndReadyForPrompt();
        showCompletedForLowBatteryAutoCharge();
        LowBatteryAutoChargeManager.getInstance().maybeShowPendingDialog();
        return true;
    }

    private void showCompletedForLowBatteryAutoCharge() {
        isMissionFinished = true;
        isNavigating = false;
        isPaused = false;
        handoffToLowBatteryAutoCharge = true;
        stopAutoResumeTimer();
        hideTaskSummaryPanel();
        if (btnDoorToggle != null) {
            btnDoorToggle.setVisibility(View.GONE);
        }
        llPauseControls.setVisibility(View.GONE);
        currentTaskTextView.setText("房间配送已完成");
        tvStatus.setText("医院配送完成");
        tvHint.setVisibility(View.VISIBLE);
        tvHint.setText("电量过低，即将自动回充");
        rootLayout.setOnTouchListener(null);
    }

    private void cancelActiveTask() {
        TaskExecutionStateManager.getInstance().cancelTask();
        LowBatteryAutoChargeManager.getInstance().onTaskCancelled();
    }

    private void pauseNavigation() {
        if (!isNavigating || isPaused) {
            return;
        }

        navigationService.pause(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    isPaused = true;
                    tvStatus.setText("已暂停");
                    showPauseControls(true);
                    startAutoResumeTimer();
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> Toast.makeText(
                        HospitalDeliveryNavigationActivity.this,
                        "暂停失败: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void resumeNavigation() {
        stopAutoResumeTimer();
        if (!isPaused) {
            return;
        }

        ensureDoorsClosedBeforeMove(() -> navigationService.start(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    isPaused = false;
                    tvStatus.setText(flowStage == FlowStage.TO_DISINFECTION ? "前往消毒间" : "医院配送中");
                    tvHint.setVisibility(View.VISIBLE);
                    tvHint.setText("双击屏幕可暂停导航");
                    showPauseControls(false);
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> Toast.makeText(
                        HospitalDeliveryNavigationActivity.this,
                        "继续导航失败: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        }), "继续导航前关门失败");
    }

    private void showPauseControls(boolean visible) {
        llPauseControls.setVisibility(visible ? View.VISIBLE : View.GONE);
        hideTaskSummaryPanel();
        if (btnDoorToggle != null) {
            btnDoorToggle.setVisibility(View.GONE);
        }
        if (visible) {
            btnPauseEnd.setText("结束任务");
            btnContinue.setText("继续导航");
            tvHint.setVisibility(View.VISIBLE);
            tvHint.setText("双击屏幕可暂停导航");
        } else {
            stopAutoResumeTimer();
        }
    }

    private void showWaitAtDisinfectionControls() {
        stopAutoResumeTimer();
        tvCountdown.setVisibility(View.GONE);
        llPauseControls.setVisibility(View.VISIBLE);
        btnPauseEnd.setText("结束任务");
        btnContinue.setText("继续导航");
        if (layoutTaskSummaryPanel != null) {
            layoutTaskSummaryPanel.setVisibility(View.VISIBLE);
        }
        tvHint.setVisibility(View.VISIBLE);
        tvHint.setText("消毒完成后点击继续导航");
    }

    private void hideTaskSummaryPanel() {
        if (layoutTaskSummaryPanel != null) {
            layoutTaskSummaryPanel.setVisibility(View.GONE);
        }
    }

    private void performReturnOperation() {
        if (isReturning) {
            return;
        }
        isReturning = true;
        ensureDoorsClosedBeforeMove(() -> {
            stopAutoResumeTimer();
            recordCurrentStageCancelled();
            if (navigationService != null) {
                navigationService.stop(null);
            }
            isNavigating = false;
            isPaused = false;
            isMissionFinished = true;

            Intent intent = new Intent(this, ReturnActivity.class);
            intent.putExtra("return_source_mode", 3);
            intent.putExtra("return_speed", HospitalDeliverySettingsManager.getInstance().getReturnSpeed());
            startActivity(intent);
            finish();
        }, "返航前关门失败");
    }

    private void recordCurrentStageCancelled() {
        if (flowStage == FlowStage.WAIT_AT_DISINFECTION || flowStage == FlowStage.TO_DISINFECTION) {
            if (disinfectionNode != null) {
                HospitalDeliveryManager.getInstance().recordPointArrival(
                        disinfectionNode.getName(),
                        HospitalDeliveryRecord.STATUS_CANCELLED,
                        HospitalDeliveryRecord.STAGE_DISINFECTION);
            }
            return;
        }
        if (flowStage == FlowStage.TO_ROOMS && currentUniqueTargetIndex < targetNodes.size()) {
            HospitalDeliveryManager.getInstance().recordPointArrival(
                    targetNodes.get(currentUniqueTargetIndex).getName(),
                    HospitalDeliveryRecord.STATUS_CANCELLED,
                    HospitalDeliveryRecord.STAGE_ROOM);
        }
    }

    private void updateRoomTaskText() {
        if (isMissionFinished) {
            return;
        }
        if (currentUniqueTargetIndex < targetNodes.size()) {
            NavigationNode currentNode = targetNodes.get(currentUniqueTargetIndex);
            currentTaskTextView.setText("正在前往房间：" + currentNode.getName()
                    + " (" + (currentUniqueTargetIndex + 1) + "/" + targetNodes.size() + ")");
        } else {
            currentTaskTextView.setText("房间配送已完成");
        }
    }

    private void handleDisinfectionArrival() {
        isNavigating = false;
        isPaused = false;
        flowStage = FlowStage.WAIT_AT_DISINFECTION;
        HospitalDeliveryManager.getInstance().recordPointArrival(
                disinfectionNode.getName(),
                HospitalDeliveryRecord.STATUS_SUCCESS,
                HospitalDeliveryRecord.STAGE_DISINFECTION);
        runOnUiThread(() -> {
            tvStatus.setText("消毒间等待");
            currentTaskTextView.setText("已到达消毒间：" + disinfectionNode.getName());
            populateTaskSummaryTable();
            showWaitAtDisinfectionControls();
            if (btnDoorToggle != null) {
                btnDoorToggle.setVisibility(View.VISIBLE);
                updateDoorToggleButton();
            }
            Toast.makeText(this, "已到达消毒间，等待继续导航", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleRoomArrival() {
        isNavigating = false;
        isPaused = false;
        stopAutoResumeTimer();
        Intent intent = new Intent(this, ConfirmReceiptActivity.class);
        intent.putExtra("pairings", pairings);
        intent.putExtra("current_node", targetNodes.get(currentUniqueTargetIndex));
        intent.putExtra("record_mode", "hospital");
        startActivityForResult(intent, REQUEST_CODE_CONFIRM_RECEIPT);
    }

    private void removeCurrentFinishedTasks() {
        if (pairings != null && currentUniqueTargetIndex < uniqueDeliveryTasks.size()) {
            List<Map.Entry<Integer, NavigationNode>> finishedTasks = uniqueDeliveryTasks.get(currentUniqueTargetIndex);
            for (Map.Entry<Integer, NavigationNode> task : finishedTasks) {
                pairings.remove(task.getKey());
            }
        }
    }

    private void goNextRoomOrReturn() {
        if (currentUniqueTargetIndex < targetNodes.size() - 1) {
            ensureDoorsClosedBeforeMove(() -> navigationService.pilotNext(new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    isNavigating = true;
                    isPaused = false;
                    runOnUiThread(() -> {
                        tvStatus.setText("医院配送中");
                        tvHint.setVisibility(View.VISIBLE);
                        tvHint.setText("双击屏幕可暂停导航");
                        showPauseControls(false);
                    });
                }

                @Override
                public void onError(ApiError error) {
                    runOnUiThread(() -> Toast.makeText(HospitalDeliveryNavigationActivity.this,
                            "继续下一房间失败: " + error.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }), "前往下一房间前关门失败");
        } else {
            ensureDoorsClosedBeforeMove(() -> {
                isMissionFinished = true;
                if (handoffToLowBatteryAutoChargeIfNeeded()) {
                    Toast.makeText(this, "电量过低，即将自动回充", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (WorkScheduleService.getInstance().hasDeferredWorkEnd()) {
                    TaskExecutionStateManager.getInstance().finishTask();
                    if (WorkScheduleService.getInstance().executeDeferredActionIfIdle()) {
                        finish();
                        return;
                    }
                }
                if (!HospitalDeliveryActivity.originPoints.isEmpty()) {
                    HospitalDeliveryManager.getInstance().recordPointArrival(
                            HospitalDeliveryActivity.originPoints.get(0).getName(),
                            HospitalDeliveryRecord.STATUS_SUCCESS,
                            HospitalDeliveryRecord.STAGE_RETURN);
                }
                Intent intent = new Intent(this, ReturnActivity.class);
                intent.putExtra("return_source_mode", 3);
                intent.putExtra("return_speed", HospitalDeliverySettingsManager.getInstance().getReturnSpeed());
                startActivity(intent);
                finish();
            }, "返航前关门失败");
        }
    }

    private void ensureDoorsClosedBeforeMove(Runnable onReadyToMove, String failureMessage) {
        if (doorService == null) {
            runOnUiThread(onReadyToMove);
            return;
        }

        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                if (Boolean.TRUE.equals(allClosed)) {
                    runOnUiThread(onReadyToMove);
                    return;
                }

                doorService.closeAllDoors(new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> new Handler().postDelayed(onReadyToMove, 1500));
                    }

                    @Override
                    public void onError(ApiError error) {
                        runOnUiThread(() -> Toast.makeText(
                                HospitalDeliveryNavigationActivity.this,
                                failureMessage + ": " + error.getMessage(),
                                Toast.LENGTH_SHORT).show());
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> Toast.makeText(
                        HospitalDeliveryNavigationActivity.this,
                        failureMessage + ": " + error.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void toggleDoorState() {
        if (doorService == null || btnDoorToggle == null) {
            return;
        }

        btnDoorToggle.setEnabled(false);
        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                IResultCallback<Void> callback = new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            btnDoorToggle.setEnabled(true);
                            updateDoorToggleButton();
                            Toast.makeText(
                                    HospitalDeliveryNavigationActivity.this,
                                    Boolean.TRUE.equals(allClosed) ? "舱门已打开" : "舱门已关闭",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        handleDoorToggleError(error);
                    }
                };

                if (Boolean.TRUE.equals(allClosed)) {
                    doorService.openAllDoors(false, callback);
                } else {
                    doorService.closeAllDoors(callback);
                }
            }

            @Override
            public void onError(ApiError error) {
                handleDoorToggleError(error);
            }
        });
    }

    private void handleDoorToggleError(ApiError error) {
        runOnUiThread(() -> {
            if (btnDoorToggle != null) {
                btnDoorToggle.setEnabled(true);
                btnDoorToggle.setText("舱门控制");
            }
            Toast.makeText(
                    HospitalDeliveryNavigationActivity.this,
                    "舱门操作失败: " + error.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void updateDoorToggleButton() {
        if (doorService == null || btnDoorToggle == null) {
            return;
        }

        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                runOnUiThread(() -> btnDoorToggle.setText(
                        Boolean.TRUE.equals(allClosed) ? "打开舱门" : "关闭舱门"));
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> btnDoorToggle.setText("舱门控制"));
            }
        });
    }

    private int getLayerNumber(int id) {
        if (id == R.id.l1_button) {
            return 1;
        }
        if (id == R.id.l2_button) {
            return 2;
        }
        if (id == R.id.l3_button) {
            return 3;
        }
        if (id == 1 || id == 2 || id == 3) {
            return id;
        }
        return 0;
    }

    private String getLayerLabel(int id) {
        int layerNumber = getLayerNumber(id);
        return layerNumber > 0 ? "L" + layerNumber : "未知层";
    }

    private void populateTaskSummaryTable() {
        if (tableTaskSummary == null) {
            return;
        }

        tableTaskSummary.removeAllViews();
        tableTaskSummary.addView(createSummaryRow("层", "物品", "房间", true));

        if (deliveryTasks == null || deliveryTasks.isEmpty()) {
            tableTaskSummary.addView(createSummaryRow("-", "未设置", "未设置", false));
            return;
        }

        for (Map.Entry<Integer, NavigationNode> task : deliveryTasks) {
            int layerId = task.getKey();
            NavigationNode node = task.getValue();
            String itemName = layerItems.get(layerId);
            tableTaskSummary.addView(createSummaryRow(
                    getLayerLabel(layerId),
                    itemName == null || itemName.trim().isEmpty() ? "未设置物品" : itemName,
                    node != null && node.getName() != null ? node.getName() : "未设置房间",
                    false));
        }
    }

    private TableRow createSummaryRow(String layer, String item, String room, boolean header) {
        TableRow row = new TableRow(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(createSummaryCell(layer, params, header));
        row.addView(createSummaryCell(item, params, header));
        row.addView(createSummaryCell(room, params, header));
        return row;
    }

    private TextView createSummaryCell(String text, TableRow.LayoutParams params, boolean header) {
        TextView cell = new TextView(this);
        cell.setLayoutParams(params);
        int horizontal = (int) (12 * getResources().getDisplayMetrics().density);
        int vertical = (int) (10 * getResources().getDisplayMetrics().density);
        cell.setPadding(horizontal, vertical, horizontal, vertical);
        cell.setText(text);
        cell.setMaxLines(2);
        cell.setTextSize(header ? 18 : 16);
        cell.setTextColor(getColor(header ? R.color.medical_primary : R.color.medical_text_primary));
        if (header) {
            cell.setTypeface(cell.getTypeface(), Typeface.BOLD);
            cell.setBackgroundColor(getColor(R.color.medical_secondary));
        } else {
            cell.setBackgroundColor(getColor(R.color.white));
        }
        return cell;
    }

    @Override
    public void onStateChanged(int state, int schedule) {
        runOnUiThread(() -> {
            switch (state) {
                case Navigation.STATE_RUNNING:
                    if (isPaused) {
                        return;
                    }
                    tvStatus.setText(flowStage == FlowStage.TO_DISINFECTION ? "前往消毒间" : "医院配送中");
                    break;
                case Navigation.STATE_PAUSED:
                    isPaused = true;
                    break;
                case Navigation.STATE_DESTINATION:
                    if (flowStage == FlowStage.TO_DISINFECTION) {
                        handleDisinfectionArrival();
                    } else if (flowStage == FlowStage.TO_ROOMS) {
                        handleRoomArrival();
                    }
                    break;
                case Navigation.STATE_BLOCKED:
                case Navigation.STATE_COLLISION:
                    Toast.makeText(this, "遇到障碍物，正在避障", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public void onRouteNode(int index, NavigationNode node) {
        if (flowStage == FlowStage.TO_ROOMS) {
            currentUniqueTargetIndex = index;
            runOnUiThread(this::updateRoomTaskText);
        }
    }

    @Override
    public void onRoutePrepared(List<NavigationNode> nodes) {
        ensureDoorsClosedBeforeMove(() -> navigationService.start(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Hospital delivery navigation started");
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> Toast.makeText(HospitalDeliveryNavigationActivity.this,
                        "启动导航失败: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }), "启动导航前关门失败");
    }

    @Override
    public void onDistanceChanged(double distance) {
    }

    @Override
    public void onNavigationError(int errorCode) {
        if (flowStage == FlowStage.TO_ROOMS && currentUniqueTargetIndex < targetNodes.size()) {
            HospitalDeliveryManager.getInstance().recordPointArrival(
                    targetNodes.get(currentUniqueTargetIndex).getName(),
                    HospitalDeliveryRecord.STATUS_NAV_FAILED,
                    HospitalDeliveryRecord.STAGE_ROOM);
        } else if ((flowStage == FlowStage.TO_DISINFECTION || flowStage == FlowStage.WAIT_AT_DISINFECTION)
                && disinfectionNode != null) {
            HospitalDeliveryManager.getInstance().recordPointArrival(
                    disinfectionNode.getName(),
                    HospitalDeliveryRecord.STATUS_NAV_FAILED,
                    HospitalDeliveryRecord.STAGE_DISINFECTION);
        }
    }

    @Override
    public void onError(int errorCode, String message) {
        Log.e(TAG, "Navigation generic error: " + errorCode + ", " + message);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CONFIRM_RECEIPT) {
            if (resultCode == RESULT_OK) {
                removeCurrentFinishedTasks();
                goNextRoomOrReturn();
            }
            return;
        }
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_CODE_END_PASSWORD) {
            recordCurrentStageCancelled();
            if (navigationService != null) {
                navigationService.stop(null);
            }
            cancelActiveTask();
            finish();
        } else if (requestCode == REQUEST_CODE_RETURN_PASSWORD) {
            performReturnOperation();
        } else if (requestCode == REQUEST_CODE_DOOR_PASSWORD) {
            Toast.makeText(this, "当前页面未开放舱门操作", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoResumeTimer();
        if (isFinishing() && !isMissionFinished && !isReturning && !handoffToLowBatteryAutoCharge) {
            cancelActiveTask();
        }
        if (navigationService != null) {
            navigationService.stop(null);
            navigationService.unregisterCallback(this);
        }
        if (doorService != null) {
            doorService.unregisterCallback(doorCallback);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            com.weigao.robot.control.app.WeigaoApplication.applyFullScreen(this);
        }
    }

    private void startAutoResumeTimer() {
        stopAutoResumeTimer();
        tvCountdown.setVisibility(View.VISIBLE);
        autoResumeTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isFinishing()) {
                    tvCountdown.setText((millisUntilFinished / 1000) + "s 后自动继续");
                }
            }

            @Override
            public void onFinish() {
                if (!isFinishing() && isPaused) {
                    finishActivity(REQUEST_CODE_END_PASSWORD);
                    tvCountdown.setVisibility(View.GONE);
                    Toast.makeText(HospitalDeliveryNavigationActivity.this,
                            "暂停超时，自动继续导航", Toast.LENGTH_SHORT).show();
                    resumeNavigation();
                }
            }
        };
        autoResumeTimer.start();
    }

    private void stopAutoResumeTimer() {
        if (autoResumeTimer != null) {
            autoResumeTimer.cancel();
            autoResumeTimer = null;
        }
        if (tvCountdown != null) {
            tvCountdown.setVisibility(View.GONE);
        }
    }

    private final IDoorCallback doorCallback = new IDoorCallback() {
        @Override
        public void onDoorStateChanged(int doorId, int state) {
            runOnUiThread(HospitalDeliveryNavigationActivity.this::updateDoorToggleButton);
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
}

