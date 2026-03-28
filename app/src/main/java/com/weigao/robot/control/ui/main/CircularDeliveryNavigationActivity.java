package com.weigao.robot.control.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
import com.weigao.robot.control.manager.CircularDeliveryHistoryManager;
import com.weigao.robot.control.manager.LowBatteryAutoChargeManager;
import com.weigao.robot.control.manager.TaskExecutionStateManager;
import com.weigao.robot.control.manager.WorkScheduleService;
import com.weigao.robot.control.model.CircularDeliveryRecord;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.INavigationService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.impl.ProjectionDoorService;
import com.weigao.robot.control.manager.AppSettingsManager;
import com.weigao.robot.control.app.WeigaoApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * 循环配送导航界面
 */
public class CircularDeliveryNavigationActivity extends AppCompatActivity implements INavigationCallback {

    private static final String TAG = "CircularNavActivity";
    private static final int DOOR_CLOSE_CHECK_RETRY_COUNT = 8;
    private static final long DOOR_CLOSE_CHECK_INTERVAL_MS = 800L;
    private static final long DOOR_RESUME_DELAY_MS = 3000L;

    private TextView tvStatus, currentTaskTextView, tvHint, tvLoopCount;
    private TextView tvCountdown;
    private android.widget.ProgressBar pbProgress;
    private Button btnPauseEnd, btnContinue, btnReturnOrigin, btnDoorToggle;
    private LinearLayout llPauseControls;
    private View rootLayout;

    private List<NavigationNode> targetNodes;
    private int currentTaskIndex = 0;

    private boolean isPaused = false;
    private boolean isNavigating = false;
    private boolean isWaitingAtNode = false;

    private GestureDetector gestureDetector;
    private INavigationService navigationService;
    private com.weigao.robot.control.service.IAudioService audioService; // Audio Service
    private IDoorService doorService;
    private boolean isDoorActionInProgress = false;
    private android.app.Dialog doorOperationDialog;

    private String routeName;
    private int loopCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circular_delivery_navigation); // Use separate layout

        initViews();

        navigationService = ServiceManager.getInstance().getNavigationService();
        audioService = ServiceManager.getInstance().getAudioService(); // Init AudioService
        doorService = ServiceManager.getInstance().getDoorService();
        if (navigationService == null) {
            Toast.makeText(this, "导航服务未初始化", Toast.LENGTH_SHORT).show();
            cancelActiveTask();
            finish();
            return;
        }
        if (doorService != null) {
            doorService.registerCallback(doorCallback);
        }
        navigationService.registerCallback(this);

        // Get Data
        ArrayList<NavigationNode> rawNodes = (ArrayList<NavigationNode>) getIntent()
                .getSerializableExtra("route_nodes");
        routeName = getIntent().getStringExtra("route_name");
        loopCount = getIntent().getIntExtra("loop_count", 1);

        if (rawNodes == null || rawNodes.isEmpty()) {
            Toast.makeText(this, "路线节点为空", Toast.LENGTH_SHORT).show();
            cancelActiveTask();
            finish();
            return;
        }

        // Expand nodes based on loop count
        targetNodes = new ArrayList<>();
        for (int i = 0; i < loopCount; i++) {
            targetNodes.addAll(rawNodes);
        }

        // Add Return to Origin at the very end?
        // Logic: Circular delivery usually ends where it started or just stops.
        // We'll assume it stops at the last node. User can "Return" manually.

        setupGestureDetector();
        setupButtons();

        if (targetNodes.size() > 0) {
            pbProgress.setMax(targetNodes.size());
        }

        updateTaskText();
        startNavigation();

        // 如果启用了投影灯开关门，使用单例服务
        if (AppSettingsManager.getInstance()
                .isProjectionDoorEnabled(com.weigao.robot.control.manager.ProjectionDoorMode.CIRCULAR)) {
            ProjectionDoorService.getInstance().setDoorActionListener(this::showDoorOperationDialog);
            Log.d(TAG, "【投影灯】功能已启用");
        }
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        currentTaskTextView = findViewById(R.id.current_task_textview);
        tvHint = findViewById(R.id.tv_hint);
        tvLoopCount = findViewById(R.id.tv_loop_count);
        pbProgress = findViewById(R.id.pb_progress);

        llPauseControls = findViewById(R.id.ll_pause_controls);
        tvHint = findViewById(R.id.tv_hint);
        tvCountdown = findViewById(R.id.tv_countdown);

        btnPauseEnd = findViewById(R.id.btn_pause_end);
        btnContinue = findViewById(R.id.btn_continue);
        btnDoorToggle = findViewById(R.id.btn_toggle_door);
        rootLayout = findViewById(R.id.root_layout);
        btnReturnOrigin = findViewById(R.id.btn_return_origin);

        // Adjust button text for "Pause" state initially hidden
        llPauseControls.setVisibility(View.GONE);
        btnReturnOrigin.setVisibility(View.GONE);
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isNavigating && !isPaused && !isWaitingAtNode) {
                    pauseRetryCount = 0;
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
        btnPauseEnd.setText("结束导航");
        btnPauseEnd.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.weigao.robot.control.ui.auth.PasswordActivity.class);
            startActivityForResult(intent, REQUEST_CODE_END_NAVIGATION_PASSWORD);
        });

        btnContinue.setText("继续");
        btnContinue.setOnClickListener(v -> {
            if (isPaused) {
                resumeNavigation();
            } else if (isWaitingAtNode) {
                proceedToNextNode();
            }
        });

        if (btnDoorToggle != null) {
            btnDoorToggle.setOnClickListener(v -> toggleDoorState());
        }

        btnReturnOrigin.setOnClickListener(v -> {
            cancelActiveTask();
            stopNavigation();
            Intent intent = new Intent(this, ReturnActivity.class);
            intent.putExtra("return_speed",
                    com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance().getReturnSpeed());
            intent.putExtra("return_source_mode", 2);
            startActivity(intent);
            finish();
        });
    }

    private void startNavigation() {
        isNavigating = true;
        // Start Record
        currentRecord = new CircularDeliveryRecord(routeName, loopCount, System.currentTimeMillis());

        List<Integer> targetIds = new ArrayList<>();
        for (NavigationNode node : targetNodes) {
            // ...
            targetIds.add(node.getId());
        }

        navigationService.setTargets(targetIds, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "【导航控制】设置目标点成功");
                // 播放背景音乐
                playBackgroundMusic();
                // 播报语音
                playConfiguredVoice(false);

                int speed = com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance()
                        .getDeliverySpeed();
                navigationService.setSpeed(speed, new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "【导航控制】设置速度成功");
                        navigationService.prepare(new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "【导航控制】准备路线成功");
                            } // Will accept onRoutePrepared

                            @Override
                            public void onError(ApiError error) {
                                handleError("准备路线失败", error);
                            }
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        handleError("设置速度失败", error);
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                handleError("设置目标点失败", error);
            }
        });
    }

    private void handleError(String msg, ApiError error) {
        runOnUiThread(() -> Toast.makeText(this, msg + ": " + error.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void pauseNavigation() {
        final long startTime = System.currentTimeMillis();
        final android.os.Handler handler = new android.os.Handler();
        Runnable multiplePauseTask = new Runnable() {
            @Override
            public void run() {
                if (navigationService != null) {
                    navigationService.pause(null);
                }
                if (System.currentTimeMillis() - startTime < 1000) {
                    handler.postDelayed(this, 50);
                }
            }
        };
        handler.post(multiplePauseTask);

        navigationService.pause(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    isPaused = true;
                    lastPauseTime = System.currentTimeMillis();
                    tvStatus.setText("已暂停");
                    llPauseControls.setVisibility(View.VISIBLE);
                    tvHint.setVisibility(View.INVISIBLE);
                    stopAutoResumeTimer();
                    if (btnDoorToggle != null) {
                        btnDoorToggle.setVisibility(View.VISIBLE);
                    }
                    setPauseActionButtonsEnabled(true);
                    updateDoorToggleButton();
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> Toast.makeText(CircularDeliveryNavigationActivity.this,
                        "暂停失败: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void resumeNavigation() {
        stopAutoResumeTimer();
        if (!isPaused || isDoorActionInProgress) {
            return;
        }
        setPauseActionButtonsEnabled(false);
        ensureDoorsClosedBeforeResume(this::resumeNavigationInternal);
    }

    private void resumeNavigationInternal() {
        navigationService.start(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    isPaused = false;
                    tvStatus.setText("导航中");
                    llPauseControls.setVisibility(View.GONE);
                    btnReturnOrigin.setVisibility(View.GONE);
                    if (btnDoorToggle != null) {
                        btnDoorToggle.setVisibility(View.GONE);
                    }
                    setPauseActionButtonsEnabled(true);
                    tvHint.setVisibility(View.VISIBLE);
                    playConfiguredVoice(false);
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> {
                    setPauseActionButtonsEnabled(true);
                    updateDoorToggleButton();
                    Toast.makeText(CircularDeliveryNavigationActivity.this,
                            "继续导航失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void toggleDoorState() {
        if (doorService == null || btnDoorToggle == null || !isPaused || isDoorActionInProgress) {
            return;
        }

        isDoorActionInProgress = true;
        setPauseActionButtonsEnabled(false);
        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                IResultCallback<Void> actionCallback = new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            isDoorActionInProgress = false;
                            setPauseActionButtonsEnabled(true);
                            updateDoorToggleButton();
                            Toast.makeText(CircularDeliveryNavigationActivity.this,
                                    Boolean.TRUE.equals(allClosed) ? "已打开所有舱门" : "已关闭所有舱门",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        handleDoorActionError("舱门操作失败", error);
                    }
                };

                if (Boolean.TRUE.equals(allClosed)) {
                    doorService.openAllDoors(false, actionCallback);
                } else {
                    doorService.closeAllDoors(actionCallback);
                }
            }

            @Override
            public void onError(ApiError error) {
                handleDoorActionError("舱门状态查询失败", error);
            }
        });
    }

    private void ensureDoorsClosedBeforeResume(Runnable onReadyToResume) {
        if (doorService == null) {
            runOnUiThread(onReadyToResume);
            return;
        }

        isDoorActionInProgress = true;
        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                if (Boolean.TRUE.equals(allClosed)) {
                    runOnUiThread(() -> {
                        isDoorActionInProgress = false;
                        onReadyToResume.run();
                    });
                    return;
                }

                doorService.closeAllDoors(new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        verifyDoorsClosedBeforeResume(onReadyToResume, DOOR_CLOSE_CHECK_RETRY_COUNT);
                    }

                    @Override
                    public void onError(ApiError error) {
                        handleDoorActionError("继续前关门失败", error);
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                handleDoorActionError("继续前关门失败", error);
            }
        });
    }

    private void verifyDoorsClosedBeforeResume(Runnable onReadyToResume, int remainingRetries) {
        if (doorService == null) {
            runOnUiThread(() -> {
                isDoorActionInProgress = false;
                onReadyToResume.run();
            });
            return;
        }
        if (remainingRetries <= 0) {
            runOnUiThread(() -> {
                isDoorActionInProgress = false;
                setPauseActionButtonsEnabled(true);
                updateDoorToggleButton();
                Toast.makeText(CircularDeliveryNavigationActivity.this,
                        "继续前关门失败", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        new android.os.Handler().postDelayed(() -> doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                if (Boolean.TRUE.equals(allClosed)) {
                    runOnUiThread(() -> new android.os.Handler().postDelayed(() -> {
                        isDoorActionInProgress = false;
                        onReadyToResume.run();
                    }, DOOR_RESUME_DELAY_MS));
                    return;
                }
                verifyDoorsClosedBeforeResume(onReadyToResume, remainingRetries - 1);
            }

            @Override
            public void onError(ApiError error) {
                verifyDoorsClosedBeforeResume(onReadyToResume, remainingRetries - 1);
            }
        }), DOOR_CLOSE_CHECK_INTERVAL_MS);
    }

    private void handleDoorActionError(String prefix, ApiError error) {
        runOnUiThread(() -> {
            isDoorActionInProgress = false;
            setPauseActionButtonsEnabled(true);
            updateDoorToggleButton();
            String detail = error == null ? "" : ": " + error.getMessage();
            Toast.makeText(CircularDeliveryNavigationActivity.this, prefix + detail, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateDoorToggleButton() {
        if (btnDoorToggle == null) {
            return;
        }
        if (doorService == null) {
            btnDoorToggle.setText("开门");
            return;
        }

        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                runOnUiThread(() -> btnDoorToggle.setText(
                        Boolean.TRUE.equals(allClosed) ? "开门" : "关门"));
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> btnDoorToggle.setText("开门"));
            }
        });
    }

    private void setPauseActionButtonsEnabled(boolean enabled) {
        if (btnPauseEnd != null) {
            btnPauseEnd.setEnabled(enabled);
        }
        if (btnContinue != null) {
            btnContinue.setEnabled(enabled);
        }
        if (btnDoorToggle != null) {
            btnDoorToggle.setEnabled(enabled);
        }
    }

    private void proceedToNextNode() {
        // 先关门
        com.weigao.robot.control.service.IDoorService doorService = ServiceManager.getInstance().getDoorService();
        if (doorService != null) {
            // true 表示关闭所有舱门
            doorService.closeDoor(-1, new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // 门关好后，再执行前往下一站
                    runOnUiThread(() -> {
                        startPilotNext();
                    });
                }

                @Override
                public void onError(ApiError error) {
                    runOnUiThread(() -> {
                        // Toast.makeText(CircularDeliveryNavigationActivity.this, "关门失败: " +
                        // error.getMessage(), Toast.LENGTH_SHORT).show();
                        // 即使关门失败，也尝试继续导航？或者让用户决定？这里选择继续尝试导航
                        startPilotNext();
                    });
                }
            });
        } else {
            // 服务不可用直接走
            startPilotNext();
        }
    }

    private void startPilotNext() {
        // 即将移动，确保投影灯关闭
        if (AppSettingsManager.getInstance()
                .isProjectionDoorEnabled(com.weigao.robot.control.manager.ProjectionDoorMode.CIRCULAR)) {
            ProjectionDoorService.getInstance().pauseForMovement();
        }
        navigationService.pilotNext(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    isWaitingAtNode = false;
                    tvStatus.setText("导航中");
                    llPauseControls.setVisibility(View.GONE);
                    tvHint.setVisibility(View.VISIBLE);

                    // resumeBackgroundMusic(); // Modified: Music is already playing
                    playConfiguredVoice(false);
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> Toast.makeText(CircularDeliveryNavigationActivity.this,
                        "无法前往下一站: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void stopNavigation() {
        navigationService.stop(null);
        isNavigating = false;

        stopBackgroundMusic();

        // Only mark as CANCELLED if it hasn't been completed yet
        if (currentRecord != null && currentRecord.getDurationSeconds() == 0
                && !"COMPLETED".equals(currentRecord.getStatus())) {
            currentRecord.complete("CANCELLED");
            CircularDeliveryHistoryManager.getInstance(this).addRecord(currentRecord);
            currentRecord = null;
        }
    }

    private void cancelActiveTask() {
        TaskExecutionStateManager.getInstance().cancelTask();
        LowBatteryAutoChargeManager.getInstance().onTaskCancelled();
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
        isNavigating = false;
        isWaitingAtNode = false;
        tvStatus.setText("循环配送完成");
        currentTaskTextView.setText("所有任务已完成");
        llPauseControls.setVisibility(View.GONE);
        btnReturnOrigin.setVisibility(View.GONE);
        if (btnDoorToggle != null) {
            btnDoorToggle.setVisibility(View.GONE);
        }
        tvHint.setVisibility(View.VISIBLE);
        tvHint.setText("电量过低，即将自动回充");
    }

    private void updateTaskText() {
        runOnUiThread(() -> {
            if (currentTaskIndex < targetNodes.size()) {
                NavigationNode node = targetNodes.get(currentTaskIndex);

                int totalPoints = targetNodes.size();
                int nodesPerLoop = totalPoints / (loopCount > 0 ? loopCount : 1);
                if (nodesPerLoop == 0)
                    nodesPerLoop = 1;

                // 1-based index calculation
                int currentLoop = (currentTaskIndex / nodesPerLoop) + 1;
                int nodeIndexInLoop = (currentTaskIndex % nodesPerLoop) + 1;

                tvLoopCount.setText(String.format("第 %d / %d 轮", currentLoop, loopCount));

                // Progress Bar
                if (pbProgress != null) {
                    pbProgress.setProgress(currentTaskIndex + 1);
                }

                currentTaskTextView.setText(String.format("正在前往: %s (第 %d/%d 个点位)",
                        node.getName(), nodeIndexInLoop, nodesPerLoop));
            } else {
                currentTaskTextView.setText("导航结束");
                if (pbProgress != null)
                    pbProgress.setProgress(targetNodes.size());
            }
        });
    }

    // INavigationCallback Implementation
    @Override
    public void onStateChanged(int state, int schedule) {
        runOnUiThread(() -> {
            Log.d(TAG, "onStateChanged: state=" + state + ", schedule=" + schedule);
            switch (state) {
                case Navigation.STATE_DESTINATION:
                    playConfiguredVoice(true);
                    handleArrival();
                    break;
                case Navigation.STATE_PAUSED:
                    isPaused = true;
                    tvStatus.setText("已暂停");
                    llPauseControls.setVisibility(View.VISIBLE);
                    if (btnDoorToggle != null) {
                        btnDoorToggle.setVisibility(View.VISIBLE);
                    }
                    setPauseActionButtonsEnabled(true);
                    updateDoorToggleButton();
                    tvHint.setVisibility(View.INVISIBLE);
                    break;
                case Navigation.STATE_COLLISION:
                case Navigation.STATE_BLOCKED:
                    Toast.makeText(this, "遇到障碍物，正在避障", Toast.LENGTH_SHORT).show();
                    break;
                case Navigation.STATE_BLOCKING:
                    Toast.makeText(this, "阻挡超时，请检查路径", Toast.LENGTH_SHORT).show();
                    if (currentRecord != null && currentTaskIndex < targetNodes.size()) {
                        currentRecord.complete("NAV_FAILED");
                    }
                    break;
                case Navigation.STATE_RUNNING:
                    if (isPaused) {
                        if (System.currentTimeMillis() - lastPauseTime > 1000) {
                            if (pauseRetryCount < 1) {
                                pauseRetryCount++;
                                lastPauseTime = System.currentTimeMillis();
                                navigationService.pause(null);
                                return;
                            }
                            isPaused = false;
                            stopAutoResumeTimer();
                            setPauseActionButtonsEnabled(true);
                            if (btnDoorToggle != null) {
                                btnDoorToggle.setVisibility(View.GONE);
                            }
                            Toast.makeText(CircularDeliveryNavigationActivity.this,
                                    "暂停失败，请重试", Toast.LENGTH_SHORT).show();
                        } else {
                            return;
                        }
                    }
                    tvStatus.setText("导航中");
                    tvHint.setText("正在前往目的地...");
                    tvHint.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    llPauseControls.setVisibility(View.GONE);
                    btnReturnOrigin.setVisibility(View.GONE);
                    if (btnDoorToggle != null) {
                        btnDoorToggle.setVisibility(View.GONE);
                    }
                    setPauseActionButtonsEnabled(true);
                    tvHint.setVisibility(View.VISIBLE);
                    if (AppSettingsManager.getInstance()
                            .isProjectionDoorEnabled(com.weigao.robot.control.manager.ProjectionDoorMode.CIRCULAR)) {
                        ProjectionDoorService.getInstance().pauseForMovement();
                    }
                    break;
                default:
                    break;
            }
        });
    }

    private static final int REQUEST_CODE_ARRIVAL = 1001;
    private static final int REQUEST_CODE_END_NAVIGATION_PASSWORD = 1002;

    // ...

    private CircularDeliveryRecord currentRecord;
    private long lastPauseTime = 0;
    private int pauseRetryCount = 0;

    private boolean isReturning = false;

    private void handleArrival() {
        isWaitingAtNode = true;

        /*

        // 检查是否启用了投影灯开关门功能
        if (AppSettingsManager.getInstance()
                .isProjectionDoorEnabled(com.weigao.robot.control.manager.ProjectionDoorMode.CIRCULAR)) {
            Log.d(TAG, "【投影灯】到达目标点，恢复投影灯检测");
        }

        */

        Intent intent = new Intent(this, CircularArrivalActivity.class);
        boolean isLastPoint = (currentTaskIndex >= targetNodes.size() - 1);
        intent.putExtra("is_last_point", isLastPoint);
        if (currentTaskIndex < targetNodes.size()) {
            intent.putExtra("current_point_name", targetNodes.get(currentTaskIndex).getName());
        }
        startActivityForResult(intent, REQUEST_CODE_ARRIVAL);
    }

    // ==================== 投影灯开关门功能 ====================

    /**
     * 显示开关门操作提示弹窗（3秒后自动消失）
     */
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

        new android.os.Handler().postDelayed(this::dismissDoorOperationDialog, 3000);
    }

    private void dismissDoorOperationDialog() {
        if (doorOperationDialog != null && doorOperationDialog.isShowing()) {
            doorOperationDialog.dismiss();
        }
        doorOperationDialog = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ARRIVAL) {
            // 离开到达页面，立即关闭投影灯，防止移动过程中灯亮
            if (AppSettingsManager.getInstance()
                    .isProjectionDoorEnabled(com.weigao.robot.control.manager.ProjectionDoorMode.CIRCULAR)) {
                ProjectionDoorService.getInstance().pauseForMovement();
            }

            if (resultCode == CircularArrivalActivity.RESULT_CONTINUE) {
                // Check if last one
                if (currentTaskIndex >= targetNodes.size() - 1) {
                    Toast.makeText(this, "循环配送已完成，正在返回出餐口", Toast.LENGTH_SHORT).show();
                    returnToOrigin();
                } else {
                    proceedToNextNode();
                }
            } else if (resultCode == CircularArrivalActivity.RESULT_RETURN_ORIGIN) {
                // If this was the last point, it's a successful completion
                if (currentTaskIndex >= targetNodes.size() - 1) {
                    returnToOrigin();
                } else {
                    // Jump to ReturnActivity as requested (Aborted/Early return)
                    cancelActiveTask();
                    Toast.makeText(this, "任务终止，开始返航", Toast.LENGTH_SHORT).show();
                    Intent returnIntent = new Intent(this, ReturnActivity.class);
                    returnIntent.putExtra("return_source_mode", 2);
                    startActivity(returnIntent);
                    finish();
                }
            } else if (resultCode == CircularArrivalActivity.RESULT_CANCEL || resultCode == RESULT_CANCELED) {
                // Cancelled
                cancelActiveTask();
                Toast.makeText(this, "循环任务已取消", Toast.LENGTH_SHORT).show();
                stopNavigation();
                finish();
            }
        } else if (requestCode == REQUEST_CODE_END_NAVIGATION_PASSWORD) {
            // 无论验证是否成功，都不需要显式停止密码计时器了（因为它不存在了）
            // 也不需要重启自动恢复计时器（因为它没停过）
            if (resultCode == RESULT_OK) {
                cancelActiveTask();
                stopNavigation();
                finish();
            }
        }
    }

    private void returnToOrigin() {
        // Mark as COMPLETED before switching to ReturnActivity
        if (currentRecord != null) {
            currentRecord.complete("COMPLETED");
            CircularDeliveryHistoryManager.getInstance(this).addRecord(currentRecord);
            // Nullify to prevent onDestroy from marking it as cancelled
            currentRecord = null;
        }

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

        // Use ReturnActivity for consistent return logic
        Toast.makeText(this, "循环结束，开始返航", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, ReturnActivity.class);
        intent.putExtra("return_speed",
                com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance().getReturnSpeed());
        intent.putExtra("return_source_mode", 2);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRouteNode(int index, NavigationNode node) {
        currentTaskIndex = index;
        updateTaskText();
    }

    @Override
    public void onRoutePrepared(List<NavigationNode> nodes) {
        // Auto start
        navigationService.start(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    tvStatus.setText("导航中");
                    Toast.makeText(CircularDeliveryNavigationActivity.this, "开始循环配送", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(ApiError error) {
            }
        });
    }

    @Override
    public void onDistanceChanged(double distance) {
    }

    @Override
    public void onNavigationError(int errorCode) {
        runOnUiThread(() -> Toast.makeText(this, "导航错误: " + errorCode, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onError(int errorCode, String message) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoResumeTimer();
        ProjectionDoorService.getInstance().removeDoorActionListener();
        dismissDoorOperationDialog();
        if (navigationService != null) {
            stopNavigation();
            navigationService.unregisterCallback(this);
        }
        if (doorService != null) {
            doorService.unregisterCallback(doorCallback);
        }
        stopBackgroundMusic();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            WeigaoApplication.applyFullScreen(this);
        }
    }

    // ==================== Audio Helper Methods ====================

    private void playBackgroundMusic() {
        if (audioService != null) {
            audioService.getAudioConfig(new IResultCallback<com.weigao.robot.control.model.AudioConfig>() {
                @Override
                public void onSuccess(com.weigao.robot.control.model.AudioConfig config) {
                    if (config != null && config.isLoopMusicEnabled()
                            && !android.text.TextUtils.isEmpty(config.getLoopMusicPath())) {
                        Log.d(TAG, "【音频】播放循环配送背景音乐 (Loop=true)");
                        audioService.playBackgroundMusic(config.getLoopMusicPath(), true, null);
                    }
                }

                @Override
                public void onError(ApiError error) {
                }
            });
        }
    }

    private void stopBackgroundMusic() {
        if (audioService != null) {
            audioService.stopBackgroundMusic(null);
        }
    }

    private void pauseBackgroundMusic() {
        if (audioService != null) {
            audioService.pauseBackgroundMusic(null);
        }
    }

    private void resumeBackgroundMusic() {
        if (audioService != null) {
            audioService.resumeBackgroundMusic(null);
        }
    }

    private void playConfiguredVoice(boolean isArrival) {
        if (audioService != null) {
            audioService.getAudioConfig(new IResultCallback<com.weigao.robot.control.model.AudioConfig>() {
                @Override
                public void onSuccess(com.weigao.robot.control.model.AudioConfig config) {
                    if (config != null && config.isLoopVoiceEnabled()) {
                        String path = isArrival ? config.getLoopArrivalVoicePath()
                                : config.getLoopNavigatingVoicePath();
                        if (!android.text.TextUtils.isEmpty(path)) {
                            audioService.playVoice(path, null);
                        }
                    }
                }

                @Override
                public void onError(ApiError error) {
                }
            });
        }
    }

    // ==================== Auto Resume ====================
    private android.os.CountDownTimer autoResumeTimer;

    private void startAutoResumeTimer() {
        stopAutoResumeTimer();
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
            runOnUiThread(CircularDeliveryNavigationActivity.this::updateDoorToggleButton);
        }

        @Override
        public void onDoorTypeChanged(com.weigao.robot.control.model.DoorType type) {
        }

        @Override
        public void onDoorTypeSettingResult(boolean success) {
        }

        @Override
        public void onDoorError(int doorId, int errorCode) {
        }
    };
}
