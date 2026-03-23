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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.keenon.sdk.component.navigation.common.Navigation;
import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IDoorCallback;
import com.weigao.robot.control.callback.INavigationCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.manager.AppSettingsManager;
import com.weigao.robot.control.manager.HospitalDeliveryManager;
import com.weigao.robot.control.manager.HospitalDeliverySettingsManager;
import com.weigao.robot.control.manager.LowBatteryAutoChargeManager;
import com.weigao.robot.control.manager.TaskExecutionStateManager;
import com.weigao.robot.control.manager.WorkScheduleService;
import com.weigao.robot.control.model.DoorType;
import com.weigao.robot.control.model.HospitalDeliveryRecord;
import com.weigao.robot.control.model.HospitalDeliveryTask;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.INavigationService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.service.impl.ProjectionDoorService;
import com.weigao.robot.control.ui.auth.PasswordActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HospitalDeliveryNavigationActivity extends AppCompatActivity implements INavigationCallback {
    private static final String TAG = "HospitalDeliveryNav";
    private static final int REQUEST_CODE_CONFIRM_RECEIPT = 2201;
    private static final int REQUEST_CODE_END_PASSWORD = 2202;
    private static final int REQUEST_CODE_DOOR_PASSWORD = 2203;
    private static final int REQUEST_CODE_RETURN_PASSWORD = 2204;
    private static final int DOOR_CLOSE_CHECK_RETRY_COUNT = 8;
    private static final long DOOR_CLOSE_CHECK_INTERVAL_MS = 800L;
    private static final long NAVIGATION_START_FALLBACK_DELAY_MS = 2500L;

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
    private ArrayList<HospitalDeliveryTask> hospitalTasks;
    private NavigationNode disinfectionNode;
    private List<List<HospitalDeliveryTask>> groupedRoomTasks;
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
    private boolean hasStartCommandIssued;
    private boolean routePreparedReceived;
    private android.app.Dialog doorOperationDialog;
    private final Handler startHandler = new Handler();
    private final Runnable delayedNavigationStartRunnable = new Runnable() {
        @Override
        public void run() {
            if (routePreparedReceived || hasStartCommandIssued || isFinishing()) {
                return;
            }
            Log.d(TAG, "delayedNavigationStartRunnable executing. flowStage=" + flowStage);
            requestNavigationStart();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_delivery_navigation);

        initViews();
        initServices();
        initData();
        setupGestureDetector();
        setupButtons();
        if (isHospitalProjectionDoorEnabled()) {
            ProjectionDoorService.getInstance().setDoorActionListener(this::showDoorOperationDialog);
        }
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
        Log.d(TAG, "initServices. navigationService=" + (navigationService != null)
                + ", doorService=" + (doorService != null));
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
        hospitalTasks = (ArrayList<HospitalDeliveryTask>) getIntent().getSerializableExtra("hospital_tasks");
        disinfectionNode = (NavigationNode) getIntent().getSerializableExtra("disinfection_node");
        Log.d(TAG, "initData. taskCount=" + (hospitalTasks == null ? -1 : hospitalTasks.size())
                + ", disinfectionNode=" + (disinfectionNode == null ? "null"
                : disinfectionNode.getName() + "(" + disinfectionNode.getId() + ")"));
        if (hospitalTasks == null || hospitalTasks.isEmpty() || disinfectionNode == null) {
            Toast.makeText(this, "医院配送任务无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        groupedRoomTasks = new ArrayList<>();
        targetNodes = new ArrayList<>();
        prepareRoomTargets();
    }

    private void prepareRoomTargets() {
        groupedRoomTasks.clear();
        targetNodes.clear();
        List<Integer> addedIds = new ArrayList<>();

        for (HospitalDeliveryTask task : hospitalTasks) {
            NavigationNode node = task.getRoomNode();
            if (node == null) {
                continue;
            }
            int existingIndex = addedIds.indexOf(node.getId());
            if (existingIndex == -1) {
                addedIds.add(node.getId());
                targetNodes.add(node);
                List<HospitalDeliveryTask> group = new ArrayList<>();
                group.add(task);
                groupedRoomTasks.add(group);
            } else {
                groupedRoomTasks.get(existingIndex).add(task);
            }
        }
        Log.d(TAG, "prepareRoomTargets finished. targetCount=" + targetNodes.size()
                + ", groupedTaskCount=" + groupedRoomTasks.size());
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
                if (!hasAllLayersAssigned()) {
                    Toast.makeText(this, "请先为每个物品分配 L1/L2/L3", Toast.LENGTH_SHORT).show();
                    return;
                }
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

    private void startToDisinfection() {
        Log.d(TAG, "startToDisinfection. node=" + disinfectionNode.getName()
                + "(" + disinfectionNode.getId() + ")");
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
                () -> {
                    pauseProjectionDoorForMovementIfNeeded();
                    configureAndPrepareNavigation(Collections.singletonList(disinfectionNode), false);
                },
                "前往消毒间前关门失败");
    }

    private void startRoomNavigation() {
        Log.d(TAG, "startRoomNavigation. targetCount=" + (targetNodes == null ? -1 : targetNodes.size()));
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
                () -> {
                    pauseProjectionDoorForMovementIfNeeded();
                    configureAndPrepareNavigation(targetNodes, true);
                },
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

        Log.d(TAG, "configureAndPrepareNavigation. roomPhase=" + roomPhase
                + ", flowStage=" + flowStage
                + ", targetIds=" + targetIds);
        isNavigating = true;
        hasStartCommandIssued = false;
        routePreparedReceived = false;
        cancelDelayedNavigationStart();
        navigationService.setTargets(targetIds, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "setTargets success. roomPhase=" + roomPhase + ", targetIds=" + targetIds);
                int speed = HospitalDeliverySettingsManager.getInstance().getDeliverySpeed();
                Log.d(TAG, "setSpeed requested. speed=" + speed + ", roomPhase=" + roomPhase);
                navigationService.setSpeed(speed, new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "setSpeed success. speed=" + speed + ", roomPhase=" + roomPhase);
                        navigationService.prepare(new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "prepare success. roomPhase=" + roomPhase
                                        + ", flowStage=" + flowStage);
                                scheduleDelayedNavigationStart();
                            }

                            @Override
                            public void onError(ApiError error) {
                                Log.e(TAG, "prepare failed. roomPhase=" + roomPhase
                                        + ", error=" + error.getMessage());
                                handlePreparationError("准备路线失败", error, roomPhase);
                            }
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        Log.e(TAG, "setSpeed failed. roomPhase=" + roomPhase
                                + ", error=" + error.getMessage());
                        handlePreparationError("设置速度失败", error, roomPhase);
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "setTargets failed. roomPhase=" + roomPhase
                        + ", targetIds=" + targetIds
                        + ", error=" + error.getMessage());
                handlePreparationError("设置目标失败", error, roomPhase);
            }
        });
    }

    private void requestNavigationStart() {
        if (hasStartCommandIssued || navigationService == null) {
            Log.d(TAG, "requestNavigationStart skipped. hasStartCommandIssued=" + hasStartCommandIssued
                    + ", navigationService=" + (navigationService != null));
            return;
        }
        Log.d(TAG, "requestNavigationStart executing. flowStage=" + flowStage
                + ", currentUniqueTargetIndex=" + currentUniqueTargetIndex);
        hasStartCommandIssued = true;
        ensureDoorsClosedBeforeMove(() -> {
            pauseProjectionDoorForMovementIfNeeded();
            navigationService.start(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "navigationService.start success. flowStage=" + flowStage);
            }

            @Override
            public void onError(ApiError error) {
                hasStartCommandIssued = false;
                Log.e(TAG, "navigationService.start failed. flowStage=" + flowStage
                        + ", error=" + error.getMessage());
                runOnUiThread(() -> Toast.makeText(HospitalDeliveryNavigationActivity.this,
                        "启动导航失败: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            }
            });
        }, "启动导航前关门失败");
    }

    private void scheduleDelayedNavigationStart() {
        cancelDelayedNavigationStart();
        startHandler.postDelayed(delayedNavigationStartRunnable, NAVIGATION_START_FALLBACK_DELAY_MS);
    }

    private void cancelDelayedNavigationStart() {
        startHandler.removeCallbacks(delayedNavigationStartRunnable);
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

        ensureDoorsClosedBeforeMove(() -> {
            pauseProjectionDoorForMovementIfNeeded();
            navigationService.start(new IResultCallback<Void>() {
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
            });
        }, "继续导航前关门失败");
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
        cancelDelayedNavigationStart();
        tvCountdown.setVisibility(View.GONE);
        llPauseControls.setVisibility(View.VISIBLE);
        btnPauseEnd.setText("结束任务");
        btnContinue.setText("开始房间配送");
        if (layoutTaskSummaryPanel != null) {
            layoutTaskSummaryPanel.setVisibility(View.VISIBLE);
        }
        tvHint.setVisibility(View.GONE);
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
            pauseProjectionDoorForMovementIfNeeded();
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
        Log.d(TAG, "handleDisinfectionArrival. node=" + disinfectionNode.getName()
                + "(" + disinfectionNode.getId() + ")");
        isNavigating = false;
        isPaused = false;
        flowStage = FlowStage.WAIT_AT_DISINFECTION;
        HospitalDeliveryManager.getInstance().recordPointArrival(
                disinfectionNode.getName(),
                HospitalDeliveryRecord.STATUS_SUCCESS,
                HospitalDeliveryRecord.STAGE_DISINFECTION);
        runOnUiThread(() -> {
            resumeProjectionDoorAtPointIfNeeded();
            tvStatus.setText("消毒间等待");
            currentTaskTextView.setText("已到达消毒间：" + disinfectionNode.getName());
            populateTaskSummaryTable();
            showWaitAtDisinfectionControls();
            if (btnDoorToggle != null) {
                btnDoorToggle.setVisibility(View.VISIBLE);
                updateDoorToggleButton();
            }
            Toast.makeText(this, "已到达消毒间，请分配 L1/L2/L3 后继续", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleRoomArrival() {
        Log.d(TAG, "handleRoomArrival. currentIndex=" + currentUniqueTargetIndex
                + ", node=" + targetNodes.get(currentUniqueTargetIndex).getName()
                + "(" + targetNodes.get(currentUniqueTargetIndex).getId() + ")");
        isNavigating = false;
        isPaused = false;
        stopAutoResumeTimer();
        resumeProjectionDoorAtPointIfNeeded();
        Intent intent = new Intent(this, ConfirmReceiptActivity.class);
        intent.putExtra("hospital_tasks", hospitalTasks);
        intent.putExtra("current_node", targetNodes.get(currentUniqueTargetIndex));
        intent.putExtra("record_mode", "hospital");
        startActivityForResult(intent, REQUEST_CODE_CONFIRM_RECEIPT);
    }

    private void removeCurrentFinishedTasks() {
        if (currentUniqueTargetIndex >= groupedRoomTasks.size()) {
            return;
        }
        List<HospitalDeliveryTask> finishedTasks = groupedRoomTasks.get(currentUniqueTargetIndex);
        hospitalTasks.removeAll(finishedTasks);
    }

    private void goNextRoomOrReturn() {
        Log.d(TAG, "goNextRoomOrReturn. currentIndex=" + currentUniqueTargetIndex
                + ", targetCount=" + targetNodes.size());
        if (currentUniqueTargetIndex < targetNodes.size() - 1) {
            ensureDoorsClosedBeforeMove(() -> {
                pauseProjectionDoorForMovementIfNeeded();
                navigationService.pilotNext(new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "pilotNext success. nextIndex=" + (currentUniqueTargetIndex + 1));
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
                    Log.e(TAG, "pilotNext failed. currentIndex=" + currentUniqueTargetIndex
                            + ", error=" + error.getMessage());
                    runOnUiThread(() -> Toast.makeText(HospitalDeliveryNavigationActivity.this,
                            "继续下一房间失败: " + error.getMessage(), Toast.LENGTH_SHORT).show());
                }
                });
            }, "前往下一房间前关门失败");
        } else {
            Log.d(TAG, "All room targets finished. preparing return flow");
            ensureDoorsClosedBeforeMove(() -> {
                pauseProjectionDoorForMovementIfNeeded();
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

    private boolean hasAllLayersAssigned() {
        for (HospitalDeliveryTask task : hospitalTasks) {
            if (!task.hasAssignedLayer()) {
                return false;
            }
        }
        return true;
    }

    private boolean isLayerUsedByOtherTask(int layer, HospitalDeliveryTask currentTask) {
        for (HospitalDeliveryTask task : hospitalTasks) {
            if (task == currentTask) {
                continue;
            }
            if (task.getAssignedLayer() == layer) {
                return true;
            }
        }
        return false;
    }

    private void showLayerAssignmentDialog(HospitalDeliveryTask task) {
        String[] labels = {"L1", "L2", "L3", "清除分配"};
        new AlertDialog.Builder(this)
                .setTitle(task.getRoomNode().getName() + " - " + task.getItemName())
                .setItems(labels, (dialog, which) -> {
                    if (which == 3) {
                        task.setAssignedLayer(HospitalDeliveryTask.UNASSIGNED_LAYER);
                        populateTaskSummaryTable();
                        return;
                    }
                    int selectedLayer = which + 1;
                    if (isLayerUsedByOtherTask(selectedLayer, task)) {
                        Toast.makeText(this, "该层已分配给其他物品", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    task.setAssignedLayer(selectedLayer);
                    populateTaskSummaryTable();
                })
                .show();
    }
    private void populateTaskSummaryTable() {
        if (tableTaskSummary == null) {
            return;
        }

        tableTaskSummary.removeAllViews();
        tableTaskSummary.addView(createSummaryHeaderRow());
        for (HospitalDeliveryTask task : hospitalTasks) {
            tableTaskSummary.addView(createSummaryRow(task, flowStage == FlowStage.WAIT_AT_DISINFECTION));
        }
    }

    private TableRow createSummaryHeaderRow() {
        TableRow row = new TableRow(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(createSummaryCell("房间", params, true));
        row.addView(createSummaryCell("物品", params, true));
        row.addView(createSummaryCell("层位", params, true));
        return row;
    }

    private TableRow createSummaryRow(HospitalDeliveryTask task, boolean interactive) {
        TableRow row = new TableRow(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(createSummaryCell(task.getRoomNode() == null ? "未设置" : task.getRoomNode().getName(), params, false));
        row.addView(createSummaryCell(task.getItemName(), params, false));
        String layerLabel = task.hasAssignedLayer() ? task.getAssignedLayerLabel() : "点击分配";
        row.addView(createSummaryCell(layerLabel, params, false));
        if (interactive) {
            row.setClickable(true);
            row.setOnClickListener(v -> showLayerAssignmentDialog(task));
        }
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
        cell.setTextColor(ContextCompat.getColor(this,
                header ? R.color.medical_primary : R.color.medical_text_primary));
        if (header) {
            cell.setTypeface(cell.getTypeface(), Typeface.BOLD);
            cell.setBackgroundColor(ContextCompat.getColor(this, R.color.medical_secondary));
        } else {
            cell.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
        }
        return cell;
    }

    private void ensureDoorsClosedBeforeMove(Runnable onReadyToMove, String failureMessage) {
        if (doorService == null) {
            Log.w(TAG, "ensureDoorsClosedBeforeMove: doorService is null, continue directly. msg=" + failureMessage);
            runOnUiThread(onReadyToMove);
            return;
        }

        Log.d(TAG, "ensureDoorsClosedBeforeMove checking doors. msg=" + failureMessage);
        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                Log.d(TAG, "ensureDoorsClosedBeforeMove isAllDoorsClosed success. allClosed=" + allClosed
                        + ", msg=" + failureMessage);
                if (Boolean.TRUE.equals(allClosed)) {
                    runOnUiThread(onReadyToMove);
                    return;
                }

                doorService.closeAllDoors(new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "ensureDoorsClosedBeforeMove closeAllDoors success. msg=" + failureMessage);
                        verifyDoorsClosedThenProceed(onReadyToMove, failureMessage, DOOR_CLOSE_CHECK_RETRY_COUNT);
                    }

                    @Override
                    public void onError(ApiError error) {
                        Log.e(TAG, "ensureDoorsClosedBeforeMove closeAllDoors failed. msg=" + failureMessage
                                + ", error=" + error.getMessage());
                        runOnUiThread(() -> {
                            restoreContinueButtonIfNeeded();
                            Toast.makeText(
                                    HospitalDeliveryNavigationActivity.this,
                                    failureMessage + ": " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "ensureDoorsClosedBeforeMove isAllDoorsClosed failed. msg=" + failureMessage
                        + ", error=" + error.getMessage());
                runOnUiThread(() -> {
                    restoreContinueButtonIfNeeded();
                    Toast.makeText(
                            HospitalDeliveryNavigationActivity.this,
                            failureMessage + ": " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void verifyDoorsClosedThenProceed(Runnable onReadyToMove, String failureMessage, int remainingRetries) {
        if (doorService == null) {
            runOnUiThread(onReadyToMove);
            return;
        }
        if (remainingRetries <= 0) {
            Log.e(TAG, "verifyDoorsClosedThenProceed timeout. msg=" + failureMessage);
            runOnUiThread(() -> {
                restoreContinueButtonIfNeeded();
                Toast.makeText(
                        HospitalDeliveryNavigationActivity.this,
                        failureMessage + ": 舱门未完全关闭",
                        Toast.LENGTH_SHORT).show();
            });
            return;
        }

        new Handler().postDelayed(() -> doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                Log.d(TAG, "verifyDoorsClosedThenProceed isAllDoorsClosed success. allClosed="
                        + allClosed + ", remainingRetries=" + remainingRetries
                        + ", msg=" + failureMessage);
                if (Boolean.TRUE.equals(allClosed)) {
                    runOnUiThread(onReadyToMove);
                    return;
                }
                verifyDoorsClosedThenProceed(onReadyToMove, failureMessage, remainingRetries - 1);
            }

            @Override
            public void onError(ApiError error) {
                Log.w(TAG, "verifyDoorsClosedThenProceed isAllDoorsClosed failed. remainingRetries="
                        + remainingRetries + ", msg=" + failureMessage
                        + ", error=" + error.getMessage());
                verifyDoorsClosedThenProceed(onReadyToMove, failureMessage, remainingRetries - 1);
            }
        }), DOOR_CLOSE_CHECK_INTERVAL_MS);
    }

    private void restoreContinueButtonIfNeeded() {
        if (flowStage == FlowStage.WAIT_AT_DISINFECTION && btnContinue != null) {
            btnContinue.setEnabled(true);
        }
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

    private void showDoorOperationDialog(boolean isOpening) {
        dismissDoorOperationDialog();

        doorOperationDialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        doorOperationDialog.setContentView(R.layout.dialog_door_operation);
        doorOperationDialog.setCancelable(true);

        TextView tvTitle = doorOperationDialog.findViewById(R.id.tv_door_operation_title);
        tvTitle.setText(isOpening ? "开门中" : "关门中");

        TextView tvSubtitle = doorOperationDialog.findViewById(R.id.tv_door_operation_subtitle);
        tvSubtitle.setText("请当心");

        doorOperationDialog.show();

        new Handler().postDelayed(this::dismissDoorOperationDialog, 3000);
    }

    private void dismissDoorOperationDialog() {
        if (doorOperationDialog != null && doorOperationDialog.isShowing()) {
            doorOperationDialog.dismiss();
        }
        doorOperationDialog = null;
    }

    @Override
    public void onStateChanged(int state, int schedule) {
        Log.d(TAG, "onStateChanged. state=" + state + ", schedule=" + schedule
                + ", flowStage=" + flowStage + ", isPaused=" + isPaused
                + ", isNavigating=" + isNavigating);
        runOnUiThread(() -> {
            switch (state) {
                case Navigation.STATE_RUNNING:
                    cancelDelayedNavigationStart();
                    if (isPaused) {
                        return;
                    }
                    pauseProjectionDoorForMovementIfNeeded();
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
        Log.d(TAG, "onRouteNode. index=" + index + ", node="
                + (node == null ? "null" : node.getName() + "(" + node.getId() + ")")
                + ", flowStage=" + flowStage);
        if (flowStage == FlowStage.TO_ROOMS) {
            currentUniqueTargetIndex = index;
            runOnUiThread(this::updateRoomTaskText);
        }
    }

    @Override
    public void onRoutePrepared(List<NavigationNode> nodes) {
        routePreparedReceived = true;
        cancelDelayedNavigationStart();
        List<Integer> preparedIds = new ArrayList<>();
        if (nodes != null) {
            for (NavigationNode node : nodes) {
                if (node != null) {
                    preparedIds.add(node.getId());
                }
            }
        }
        Log.d(TAG, "onRoutePrepared. flowStage=" + flowStage + ", nodeIds=" + preparedIds);
        requestNavigationStart();
    }

    @Override
    public void onDistanceChanged(double distance) {
    }

    @Override
    public void onNavigationError(int errorCode) {
        Log.e(TAG, "onNavigationError. errorCode=" + errorCode + ", flowStage=" + flowStage
                + ", currentUniqueTargetIndex=" + currentUniqueTargetIndex);
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
        Log.e(TAG, "onError. errorCode=" + errorCode + ", message=" + message
                + ", flowStage=" + flowStage + ", isNavigating=" + isNavigating);
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
            pauseProjectionDoorForMovementIfNeeded();
            recordCurrentStageCancelled();
            if (navigationService != null) {
                navigationService.stop(null);
            }
            cancelActiveTask();
            finish();
        } else if (requestCode == REQUEST_CODE_RETURN_PASSWORD) {
            performReturnOperation();
        } else if (requestCode == REQUEST_CODE_DOOR_PASSWORD) {
            Toast.makeText(this, "当前页面未开放舱门密码操作", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoResumeTimer();
        cancelDelayedNavigationStart();
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
        ProjectionDoorService.getInstance().removeDoorActionListener();
        dismissDoorOperationDialog();
        pauseProjectionDoorForMovementIfNeeded();
    }

    private boolean isHospitalProjectionDoorEnabled() {
        return AppSettingsManager.getInstance()
                .isProjectionDoorEnabled(com.weigao.robot.control.manager.ProjectionDoorMode.HOSPITAL);
    }

    private void pauseProjectionDoorForMovementIfNeeded() {
        if (isHospitalProjectionDoorEnabled()) {
            ProjectionDoorService.getInstance().pauseForMovement();
        }
    }

    private void resumeProjectionDoorAtPointIfNeeded() {
        if (isHospitalProjectionDoorEnabled()) {
            ProjectionDoorService.getInstance().resumeAfterMovement();
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
