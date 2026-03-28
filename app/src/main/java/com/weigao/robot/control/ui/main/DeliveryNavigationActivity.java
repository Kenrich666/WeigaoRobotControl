package com.weigao.robot.control.ui.main;

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
import com.weigao.robot.control.manager.ItemDeliveryManager;
import com.weigao.robot.control.manager.LowBatteryAutoChargeManager;
import com.weigao.robot.control.manager.TaskExecutionStateManager;
import com.weigao.robot.control.manager.WorkScheduleService;
import com.weigao.robot.control.model.ItemDeliveryRecord;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.INavigationService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.impl.ProjectionDoorService;
import com.weigao.robot.control.manager.AppSettingsManager;
import com.weigao.robot.control.app.WeigaoApplication;

import android.content.Intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配送导航界面
 * <p>
 * 参考 SampleApp NavigationActivity 实现，集成真实的导航功能
 * </p>
 */
public class DeliveryNavigationActivity extends AppCompatActivity implements INavigationCallback {

    private static final String TAG = "DeliveryNavActivity";

    private static final int REQUEST_CODE_CONFIRM_RECEIPT = 1001;
    private static final int REQUEST_CODE_END_NAVIGATION_PASSWORD = 1002;
    private static final int DOOR_CLOSE_CHECK_RETRY_COUNT = 8;
    private static final long DOOR_CLOSE_CHECK_INTERVAL_MS = 800L;
    private static final long DOOR_RESUME_DELAY_MS = 3000L;
    private TextView tvStatus, currentTaskTextView, tvHint;
    private Button btnPauseEnd, btnContinue, btnDoorToggle;
    private LinearLayout llPauseControls;
    private View rootLayout;

    private List<Map.Entry<Integer, NavigationNode>> deliveryTasks;
    // 存储配对关系，提升为成员变量以便在后续操作中修改
    private HashMap<Integer, NavigationNode> pairings;

    private int currentTaskIndex = 0;
    private boolean isPaused = false;
    private TextView tvCountdown;

    // 导航服务
    private long lastPauseTime = 0;
    private int pauseRetryCount = 0;
    private boolean isNavigating = false;

    private GestureDetector gestureDetector;
    private android.app.Dialog doorOperationDialog;

    /**
     * 导航服务
     */
    private INavigationService navigationService;
    private com.weigao.robot.control.service.IAudioService audioService; // Audio Service
    private IDoorService doorService;
    private boolean isDoorActionInProgress = false;

    /**
     * 当前导航的目标点列表
     */
    private List<NavigationNode> targetNodes;

    /**
     * 开始导航
     */
    private boolean hasRunningStateReceived = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_navigation);
        Log.d(TAG, "【导航】进入 DeliveryNavigationActivity.onCreate()");

        Log.d(TAG, "【Activity】onCreate - 初始化配送导航界面");

        // 初始化视图
        initViews();

        // 获取导航服务
        navigationService = ServiceManager.getInstance().getNavigationService();
        audioService = ServiceManager.getInstance().getAudioService(); // Init Audio
        if (navigationService == null) {
            Log.e(TAG, "【错误】无法获取导航服务");
            Toast.makeText(this, "导航服务未初始化", Toast.LENGTH_SHORT).show();
            cancelActiveTask();
            finish();
            return;
        }

        // 注册导航回调
        navigationService.registerCallback(this);
        doorService = ServiceManager.getInstance().getDoorService();
        if (doorService != null) {
            doorService.registerCallback(doorCallback);
        }
        Log.d(TAG, "【导航服务】已注册回调监听器");

        // 获取配送任务
        pairings = (HashMap<Integer, NavigationNode>) getIntent()
                .getSerializableExtra("pairings");
        Log.d(TAG, "【导航】收到配送任务 pairings=" + (pairings != null ? pairings.size() : 0));

        if (pairings == null || pairings.isEmpty()) {
            Log.w(TAG, "【警告】没有配送任务");
            Toast.makeText(this, "没有配送任务", Toast.LENGTH_SHORT).show();
            cancelActiveTask();
            finish();
            return;
        }

        // 按楼层排序任务
        deliveryTasks = new ArrayList<>(pairings.entrySet());
        Collections.sort(deliveryTasks,
                (o1, o2) -> Integer.compare(getLayerNumber(o1.getKey()), getLayerNumber(o2.getKey())));

        Log.d(TAG, "【配送任务】共 " + deliveryTasks.size() + " 个任务");

        // 准备导航目标点ID列表
        Log.d(TAG, "【导航】deliveryTasks 已排序，taskCount=" + deliveryTasks.size());
        prepareNavigationTargets();

        // 更新任务文本
        updateTaskText();

        // 设置手势检测器
        setupGestureDetector();

        // 设置按钮监听器
        setupButtons();

        // 开始导航
        startNavigation();

        // 如果启用了投影灯开关门，使用单例服务
        if (AppSettingsManager.getInstance()
                .isProjectionDoorEnabled(com.weigao.robot.control.manager.ProjectionDoorMode.ITEM)) {
            ProjectionDoorService.getInstance().setDoorActionListener(this::showDoorOperationDialog);
            // 任务刚开始，机器人即将移动，先不开灯（STATE_RUNNING会处理）
            Log.d(TAG, "【投影灯】功能已启用");
        }
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        currentTaskTextView = findViewById(R.id.current_task_textview);
        tvHint = findViewById(R.id.tv_hint);
        llPauseControls = findViewById(R.id.ll_pause_controls);
        tvHint = findViewById(R.id.tv_hint);
        tvCountdown = findViewById(R.id.tv_countdown);

        btnPauseEnd = findViewById(R.id.btn_pause_end);
        btnContinue = findViewById(R.id.btn_continue);
        btnDoorToggle = findViewById(R.id.btn_toggle_door);
        rootLayout = findViewById(R.id.root_layout);
    }

    /**
     * 准备导航目标点列表
     * <p>
     * 从 deliveryTasks 中提取唯一的 NavigationNode，合并同一目的地的任务
     * </p>
     */
    // 新增：按唯一目标点分组的任务列表
    private List<List<Map.Entry<Integer, NavigationNode>>> uniqueDeliveryTasks;
    private int currentUniqueTargetIndex = 0;

    /**
     * 准备导航目标点列表
     * <p>
     * 从 deliveryTasks 中提取唯一的 NavigationNode，并构建 uniqueDeliveryTasks
     * </p>
     */
    private void prepareNavigationTargets() {
        targetNodes = new ArrayList<>();
        uniqueDeliveryTasks = new ArrayList<>();
        List<Integer> addedIds = new ArrayList<>();

        for (Map.Entry<Integer, NavigationNode> task : deliveryTasks) {
            NavigationNode node = task.getValue();
            if (node != null) {
                int nodeId = node.getId();
                if (!addedIds.contains(nodeId)) {
                    // 新的唯一目标点
                    targetNodes.add(node);
                    addedIds.add(nodeId);

                    // 创建新的任务组
                    List<Map.Entry<Integer, NavigationNode>> taskGroup = new ArrayList<>();
                    taskGroup.add(task);
                    uniqueDeliveryTasks.add(taskGroup);

                    Log.d(TAG, "【目标点】添加唯一目标点: id=" + nodeId + ", name=" + node.getName());
                } else {
                    // 已存在的目标点，找到对应的任务组并添加
                    int index = addedIds.indexOf(nodeId);
                    if (index != -1 && index < uniqueDeliveryTasks.size()) {
                        uniqueDeliveryTasks.get(index).add(task);
                        Log.d(TAG, "【目标点】将任务合并到现有目标点: id=" + nodeId);
                    }
                }
            } else {
                Log.w(TAG, "【警告】任务节点为空");
            }
        }
        Log.d(TAG, "【目标点】合并后目标点数量: " + targetNodes.size());
    }

    /**
     * 设置手势检测器（双击暂停）
     */
    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isNavigating && !isPaused && currentTaskIndex < deliveryTasks.size()) {
                    Log.d(TAG, "【用户操作】双击屏幕，暂停导航");
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

    /**
     * 设置按钮监听器
     */
    private void setupButtons() {
        btnPauseEnd.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.weigao.robot.control.ui.auth.PasswordActivity.class);
            startActivityForResult(intent, REQUEST_CODE_END_NAVIGATION_PASSWORD);
        });

        btnContinue.setOnClickListener(v -> resumeNavigation());

        if (btnDoorToggle != null) {
            btnDoorToggle.setOnClickListener(v -> toggleDoorState());
        }
    }

    /**
     * 开始导航
     */
    private void startNavigation() {
        Log.d(TAG, "【导航】startNavigation() 被调用，targetCount=" + (targetNodes != null ? targetNodes.size() : 0));
        if (targetNodes == null || targetNodes.isEmpty()) {
            Log.e(TAG, "【错误】目标点列表为空，无法开始导航");
            Toast.makeText(this, "目标点列表为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 重置运行状态标志，防止粘性到达事件
        hasRunningStateReceived = false;

        Log.d(TAG, "【导航控制】开始导航，目标点数量: " + targetNodes.size());
        isNavigating = true;

        // 提取目标ID列表
        List<Integer> targetIds = new ArrayList<>();
        for (NavigationNode node : targetNodes) {
            targetIds.add(node.getId());
        }
        Log.d(TAG, "【导航】开始导航，targetIds=" + targetIds);

        // 设置导航目标点（使用 setTargets 传递 ID 列表）
        navigationService.setTargets(targetIds, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "【导航控制】设置目标点成功");
                // 播放背景音乐
                Log.d(TAG, "【导航】setTargets() 成功，准备播放语音并设置速度");
                playBackgroundMusic();
                // 播报语音
                playConfiguredVoice(false);

                // 设置导航速度
                int speed = com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance()
                        .getDeliverySpeed();

                Log.d(TAG, "【导航】读取配送速度配置 speed=" + speed);
                navigationService.setSpeed(speed, new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "【导航控制】设置速度成功");
                        // 准备导航路线
                        Log.d(TAG, "【导航】setSpeed() 成功，准备调用 prepare()");
                        navigationService.prepare(new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "【导航】prepare() 调用成功，等待 onRoutePrepared()");
                                Log.d(TAG, "【导航控制】准备路线成功");
                            }

                            @Override
                            public void onError(ApiError error) {
                                Log.e(TAG, "【导航】prepare() 失败: " + error.getMessage());
                                Log.e(TAG, "【导航控制】准备路线失败: " + error.getMessage());
                                runOnUiThread(() -> {
                                    Toast.makeText(DeliveryNavigationActivity.this,
                                            "准备路线失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    cancelActiveTask();
                                    finish();
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        Log.e(TAG, "【导航】setSpeed() 失败: " + error.getMessage());
                        Log.e(TAG, "【导航控制】设置速度失败: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "【导航】setTargets() 失败: " + error.getMessage());
                Log.e(TAG, "【导航控制】设置目标点失败: " + error.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(DeliveryNavigationActivity.this,
                            "设置目标点失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    cancelActiveTask();
                    finish();
                });
            }
        });
    }

    /**
     * 暂停导航
     */
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
                    tvStatus.setText("\u5df2\u6682\u505c");
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
                runOnUiThread(() -> Toast.makeText(DeliveryNavigationActivity.this,
                        "\u6682\u505c\u5931\u8d25: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private boolean waitingForNext = false;

    private void resumeNavigation() {
        stopAutoResumeTimer();
        if (!isPaused || isDoorActionInProgress) {
            return;
        }
        setPauseActionButtonsEnabled(false);
        ensureDoorsClosedBeforeResume(this::resumeNavigationInternal);
    }

    private void resumeNavigationInternal() {
        playConfiguredVoice(false);

        if (waitingForNext) {
            waitingForNext = false;
            if (AppSettingsManager.getInstance()
                    .isProjectionDoorEnabled(com.weigao.robot.control.manager.ProjectionDoorMode.ITEM)) {
                ProjectionDoorService.getInstance().pauseForMovement();
            }
            navigationService.pilotNext(new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    runOnUiThread(() -> {
                        isPaused = false;
                        tvStatus.setText("\u914d\u9001\u4e2d");
                        llPauseControls.setVisibility(View.GONE);
                        if (btnDoorToggle != null) {
                            btnDoorToggle.setVisibility(View.GONE);
                        }
                        setPauseActionButtonsEnabled(true);
                        tvHint.setVisibility(View.VISIBLE);
                    });
                }

                @Override
                public void onError(ApiError error) {
                    runOnUiThread(() -> {
                        setPauseActionButtonsEnabled(true);
                        updateDoorToggleButton();
                        Toast.makeText(DeliveryNavigationActivity.this,
                                "\u7ee7\u7eed\u5bfc\u822a\u5931\u8d25: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            navigationService.start(new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    runOnUiThread(() -> {
                        isPaused = false;
                        tvStatus.setText("\u914d\u9001\u4e2d");
                        llPauseControls.setVisibility(View.GONE);
                        if (btnDoorToggle != null) {
                            btnDoorToggle.setVisibility(View.GONE);
                        }
                        setPauseActionButtonsEnabled(true);
                        tvHint.setVisibility(View.VISIBLE);
                    });
                }

                @Override
                public void onError(ApiError error) {
                    runOnUiThread(() -> {
                        setPauseActionButtonsEnabled(true);
                        updateDoorToggleButton();
                        Toast.makeText(DeliveryNavigationActivity.this,
                                "\u7ee7\u7eed\u5bfc\u822a\u5931\u8d25: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
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
                            Toast.makeText(DeliveryNavigationActivity.this,
                                    Boolean.TRUE.equals(allClosed) ? "\u5df2\u6253\u5f00\u6240\u6709\u8231\u95e8" : "\u5df2\u5173\u95ed\u6240\u6709\u8231\u95e8",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        handleDoorActionError("\u8231\u95e8\u64cd\u4f5c\u5931\u8d25", error);
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
                handleDoorActionError("\u8231\u95e8\u72b6\u6001\u67e5\u8be2\u5931\u8d25", error);
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
                        handleDoorActionError("\u7ee7\u7eed\u524d\u5173\u95e8\u5931\u8d25", error);
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                handleDoorActionError("\u7ee7\u7eed\u524d\u5173\u95e8\u5931\u8d25", error);
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
                Toast.makeText(DeliveryNavigationActivity.this,
                        "\u7ee7\u7eed\u524d\u5173\u95e8\u5931\u8d25", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(DeliveryNavigationActivity.this, prefix + detail, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateDoorToggleButton() {
        if (btnDoorToggle == null) {
            return;
        }
        if (doorService == null) {
            btnDoorToggle.setText("\u5f00\u95e8");
            return;
        }

        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                runOnUiThread(() -> btnDoorToggle.setText(
                        Boolean.TRUE.equals(allClosed) ? "\u5f00\u95e8" : "\u5173\u95e8"));
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> btnDoorToggle.setText("\u5f00\u95e8"));
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

    /**
     * 停止导航
     */
    private void stopNavigation() {
        isNavigating = false;
        navigationService.stop(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "【导航控制】停止成功");
                stopBackgroundMusic();
                audioService.stopVoice(null);
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "【导航控制】停止失败: " + error.getMessage());
            }
        });
    }

    /**
     * 更新任务文本
     */
    /**
     * 更新任务文本
     */
    private boolean isMissionFinished = false;

    /**
     * 更新任务文本
     */
    private void updateTaskText() {
        runOnUiThread(() -> {
            if (isMissionFinished)
                return;

            if (currentUniqueTargetIndex < targetNodes.size()) {
                NavigationNode currentNode = targetNodes.get(currentUniqueTargetIndex);
                String pointText = currentNode.getName();
                int totalUniqueTasks = targetNodes.size();

                currentTaskTextView.setText(String.format("正在前往：%s (第 %d/%d 个)",
                        pointText, currentUniqueTargetIndex + 1, totalUniqueTasks));
                Log.d(TAG, "【UI更新】当前任务: " + pointText + " (" + (currentUniqueTargetIndex + 1) + "/" + totalUniqueTasks
                        + ")");

            } else {
                showMissionCompletedUI();
            }
        });
    }

    private void showMissionCompletedUI() {
        isMissionFinished = true;
        currentTaskTextView.setText("\u6240\u6709\u4efb\u52a1\u5df2\u5b8c\u6210");
        tvStatus.setText("\u914d\u9001\u5b8c\u6210");
        tvHint.setVisibility(View.GONE);
        llPauseControls.setVisibility(View.VISIBLE);
        btnContinue.setVisibility(View.GONE);
        if (btnDoorToggle != null) {
            btnDoorToggle.setVisibility(View.GONE);
        }
        btnPauseEnd.setText("\u8fd4\u56de\u9996\u9875");
        rootLayout.setOnTouchListener(null);
        isNavigating = false;
        Log.d(TAG, "mission completed");
    }

    private boolean handoffToLowBatteryAutoChargeIfNeeded() {
        if (!LowBatteryAutoChargeManager.getInstance().hasPendingTaskCompletionAutoCharge()) {
            return false;
        }
        TaskExecutionStateManager.getInstance().finishTask();
        LowBatteryAutoChargeManager.getInstance().onTaskCompletedAndReadyForPrompt();
        LowBatteryAutoChargeManager.getInstance().maybeShowPendingDialog();
        return true;
    }

    private void cancelActiveTask() {
        TaskExecutionStateManager.getInstance().cancelTask();
        LowBatteryAutoChargeManager.getInstance().onTaskCancelled();
    }

    /**
     * 获取楼层编号
     */
    private int getLayerNumber(int buttonId) {
        if (buttonId == R.id.l1_button)
            return 1;
        if (buttonId == R.id.l2_button)
            return 2;
        if (buttonId == R.id.l3_button)
            return 3;
        return 0;
    }

    // ==================== INavigationCallback 接口实现 ====================

    @Override
    public void onStateChanged(int state, int schedule) {
        Log.d(TAG, "onStateChanged state=" + state + ", schedule=" + schedule);
        runOnUiThread(() -> {
            switch (state) {
                case Navigation.STATE_RUNNING:
                    hasRunningStateReceived = true;
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
                            Toast.makeText(DeliveryNavigationActivity.this,
                                    "\u6682\u505c\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5", Toast.LENGTH_SHORT).show();
                        } else {
                            return;
                        }
                    }

                    tvStatus.setText("\u914d\u9001\u4e2d");
                    if (!isMissionFinished) {
                        llPauseControls.setVisibility(View.GONE);
                        if (btnDoorToggle != null) {
                            btnDoorToggle.setVisibility(View.GONE);
                        }
                        setPauseActionButtonsEnabled(true);
                        tvHint.setVisibility(View.VISIBLE);
                    }

                    if (AppSettingsManager.getInstance()
                            .isProjectionDoorEnabled(com.weigao.robot.control.manager.ProjectionDoorMode.ITEM)) {
                        ProjectionDoorService.getInstance().pauseForMovement();
                    }
                    break;

                case Navigation.STATE_DESTINATION:
                    if (hasRunningStateReceived) {
                        Toast.makeText(this, "\u5df2\u5230\u8fbe\u76ee\u6807\u70b9", Toast.LENGTH_SHORT).show();
                        playConfiguredVoice(true);
                        handleArrival();
                        hasRunningStateReceived = false;
                    }
                    break;

                case Navigation.STATE_COLLISION:
                case Navigation.STATE_BLOCKED:
                    Toast.makeText(this, "\u9047\u5230\u969c\u788d\u7269\uff0c\u6b63\u5728\u907f\u969c", Toast.LENGTH_SHORT).show();
                    break;

                case Navigation.STATE_BLOCKING:
                    Toast.makeText(this, "\u963b\u6321\u8d85\u65f6\uff0c\u8bf7\u68c0\u67e5\u8def\u5f84", Toast.LENGTH_SHORT).show();
                    if (currentUniqueTargetIndex < targetNodes.size()) {
                        ItemDeliveryManager.getInstance().recordPointArrival(
                                targetNodes.get(currentUniqueTargetIndex).getName(),
                                ItemDeliveryRecord.STATUS_NAV_FAILED);
                    }
                    break;

                case Navigation.STATE_ERROR:
                    Toast.makeText(this, "\u5bfc\u822a\u51fa\u73b0\u9519\u8bef", Toast.LENGTH_SHORT).show();
                    if (currentUniqueTargetIndex < targetNodes.size()) {
                        ItemDeliveryManager.getInstance().recordPointArrival(
                                targetNodes.get(currentUniqueTargetIndex).getName(),
                                ItemDeliveryRecord.STATUS_NAV_FAILED);
                    }
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public void onRouteNode(int index, NavigationNode node) {
        Log.d(TAG, "【导航回调】onRouteNode - index: " + index + ", node: " + (node != null ? node.toString() : "null"));
        // SDK返回的index对应的是setTargets传入列表的索引，即unique目标的索引
        currentUniqueTargetIndex = index;
        runOnUiThread(() -> updateTaskText());
    }

    @Override
    public void onRoutePrepared(List<NavigationNode> nodes) {
        Log.d(TAG, "【导航】收到 onRoutePrepared()，节点数=" + (nodes != null ? nodes.size() : 0) + "，即将调用 start()");
        Log.d(TAG, "【导航回调】onRoutePrepared - 路线准备完成，节点数: " + (nodes != null ? nodes.size() : 0));
        // 路线准备完成，自动开始导航
        navigationService.start(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "【导航】start() 调用成功，等待 STATE_RUNNING");
                Log.d(TAG, "【导航控制】自动开始导航成功");
                runOnUiThread(() -> {
                    tvStatus.setText("配送中");
                    Toast.makeText(DeliveryNavigationActivity.this, "开始导航", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "【导航】start() 失败: " + error.getMessage());
                Log.e(TAG, "【导航控制】自动开始导航失败: " + error.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(DeliveryNavigationActivity.this,
                            "开始导航失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onDistanceChanged(double distance) {
        // 剩余距离变化，可以在这里显示剩余距离
        // Log.d(TAG, "【导航回调】剩余距离: " + distance + " 米");
    }

    @Override
    public void onNavigationError(int errorCode) {
        Log.e(TAG, "【导航】onNavigationError(), errorCode=" + errorCode);
        Log.e(TAG, "【导航回调】导航错误，错误码: " + errorCode);
        runOnUiThread(() -> {
            Toast.makeText(this, "导航错误，错误码: " + errorCode, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(int errorCode, String message) {
        Log.e(TAG, "【导航】导航通用错误回调 errorCode=" + errorCode + ", message=" + message);
        Log.e(TAG, "【导航回调】错误 - 错误码: " + errorCode + ", 消息: " + message);
        runOnUiThread(() -> {
            Toast.makeText(this, "错误: " + message, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 处理到达目标点
     */
    private void handleArrival() {

        Log.d(TAG, "【到达处理】当前唯一目标索引: " + currentUniqueTargetIndex + ", 总唯一目标数: " + targetNodes.size());

        // 无论是否是最后一个点，都跳转确认页面供用户取货
        waitingForNext = true;

        // 检查是否启用了投影灯开关门功能
        if (AppSettingsManager.getInstance()
                .isProjectionDoorEnabled(com.weigao.robot.control.manager.ProjectionDoorMode.ITEM)) {
            Log.d(TAG, "【投影灯】到达目标点，恢复投影灯检测");
            ProjectionDoorService.getInstance().resumeAfterMovement();
        }

        // 跳转到 ConfirmReceiptActivity
        Intent intent = new Intent(this, ConfirmReceiptActivity.class);

        // 传递配对信息(用于显示需要取货的层)
        HashMap<Integer, NavigationNode> pairings = (HashMap<Integer, NavigationNode>) getIntent()
                .getSerializableExtra("pairings");
        if (pairings != null) {
            intent.putExtra("pairings", pairings);
        }

        // 传递当前站点信息
        if (currentUniqueTargetIndex < targetNodes.size()) {
            NavigationNode currentNode = targetNodes.get(currentUniqueTargetIndex);
            intent.putExtra("current_node", currentNode);
        }

        startActivityForResult(intent, REQUEST_CODE_CONFIRM_RECEIPT);
    }

    // ==================== 投影灯开关门功能 ====================

    /**
     * 显示开关门操作提示弹窗（3秒后自动消失）
     *
     * @param isOpening true=开门中，false=关门中
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
    public void finish() {
        Intent returnIntent = new Intent();
        if (pairings != null) {
            returnIntent.putExtra("remaining_pairings", pairings);
        }
        setResult(RESULT_OK, returnIntent);
        super.finish();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CONFIRM_RECEIPT) {
            // 离开到达页面，立即关闭投影灯，防止移动过程中灯亮
            if (AppSettingsManager.getInstance()
                    .isProjectionDoorEnabled(com.weigao.robot.control.manager.ProjectionDoorMode.ITEM)) {
                ProjectionDoorService.getInstance().pauseForMovement();
            }

            if (resultCode == RESULT_OK) {
                Log.d(TAG, "【ConfirmReceipt】返回确认，用户已完成收货");
                waitingForNext = false;

                // 用户已确认收货，从配对关系中移除对应的层级
                if (pairings != null && currentUniqueTargetIndex < uniqueDeliveryTasks.size()) {
                    List<Map.Entry<Integer, NavigationNode>> finishedTasks = uniqueDeliveryTasks
                            .get(currentUniqueTargetIndex);
                    for (Map.Entry<Integer, NavigationNode> task : finishedTasks) {
                        pairings.remove(task.getKey());
                    }
                    Log.d(TAG, "【数据更新】已移除当前站点所有层级的配对关系");
                }

                // 判断是否还有下一站
                if (currentUniqueTargetIndex < targetNodes.size() - 1) {
                    Log.d(TAG, "【导航控制】还有下一站，前往下一站");
                    navigationService.pilotNext(new IResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Log.d(TAG, "【导航控制】前往下一点成功");
                            runOnUiThread(() -> tvStatus.setText("配送中"));
                        }

                        @Override
                        public void onError(ApiError error) {
                            Log.e(TAG, "【导航控制】前往下一点失败: " + error.getMessage());
                            runOnUiThread(() -> Toast.makeText(DeliveryNavigationActivity.this,
                                    "跳转失败: " + error.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
                } else {
                    Log.d(TAG, "【导航控制】最后一站完成，检查是否需要返回原点");
                    // 标记配送任务完成
                    currentUniqueTargetIndex = targetNodes.size();

                    if (handoffToLowBatteryAutoChargeIfNeeded()) {
                        Log.d(TAG, "【导航控制】低电量待回充，任务完成后停留当前页面");
                        updateTaskText();
                        return;
                    }

                    // 检查是否有原点数据，跳转到返航页面
                    if (DeliveryActivity.originPoints != null && !DeliveryActivity.originPoints.isEmpty()) {
                        if (WorkScheduleService.getInstance().hasDeferredWorkEnd()) {
                            TaskExecutionStateManager.getInstance().finishTask();
                            if (WorkScheduleService.getInstance().executeDeferredActionIfIdle()) {
                                finish();
                                return;
                            }
                        }
                        Log.d(TAG, "【导航控制】配送完成，跳转到返航页面");
                        Intent intent = new Intent(DeliveryNavigationActivity.this, ReturnActivity.class);
                        intent.putExtra("return_source_mode", 1); // 1: Delivery
                        startActivity(intent);
                        finish();
                    } else {
                        TaskExecutionStateManager.getInstance().finishTask();
                        if (WorkScheduleService.getInstance().executeDeferredActionIfIdle()) {
                            finish();
                            return;
                        }
                        Log.d(TAG, "【导航控制】无原点数据，直接结束");
                        updateTaskText();
                    }
                }
            } else {
                // 处理确认收货页面非正常返回的情况（超时、取消、异常等）
                Log.w(TAG, "【ConfirmReceipt】返回非OK状态，resultCode=" + resultCode + "，可能是超时或用户取消");
                waitingForNext = false;

                // 记录当前站点为失败状态
                if (currentUniqueTargetIndex < targetNodes.size()) {
                    NavigationNode currentNode = targetNodes.get(currentUniqueTargetIndex);
                    ItemDeliveryManager.getInstance().recordPointArrival(currentNode.getName(),
                            ItemDeliveryRecord.STATUS_FAILED_TIMEOUT);
                    Log.d(TAG, "【数据更新】记录站点 " + currentNode.getName() + " 为超时失败");
                }

                // 从配对关系中移除对应的层级（即使失败也要移除，避免重复尝试）
                if (pairings != null && currentUniqueTargetIndex < uniqueDeliveryTasks.size()) {
                    List<Map.Entry<Integer, NavigationNode>> finishedTasks = uniqueDeliveryTasks
                            .get(currentUniqueTargetIndex);
                    for (Map.Entry<Integer, NavigationNode> task : finishedTasks) {
                        pairings.remove(task.getKey());
                    }
                    Log.d(TAG, "【数据更新】已移除失败站点的配对关系");
                }

                // 继续执行后续任务
                if (currentUniqueTargetIndex < targetNodes.size() - 1) {
                    Log.d(TAG, "【导航控制】当前站点失败，继续前往下一站");
                    navigationService.pilotNext(new IResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Log.d(TAG, "【导航控制】前往下一点成功");
                            runOnUiThread(() -> {
                                tvStatus.setText("配送中");
                                Toast.makeText(DeliveryNavigationActivity.this,
                                        "上一站点超时，继续下一站", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(ApiError error) {
                            Log.e(TAG, "【导航控制】前往下一点失败: " + error.getMessage());
                            runOnUiThread(() -> Toast.makeText(DeliveryNavigationActivity.this,
                                    "跳转失败: " + error.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
                } else {
                    Log.d(TAG, "【导航控制】最后一站失败，检查是否需要返回原点");
                    // 标记配送任务完成
                    currentUniqueTargetIndex = targetNodes.size();

                    if (handoffToLowBatteryAutoChargeIfNeeded()) {
                        Log.d(TAG, "【导航控制】低电量待回充，最后一站失败后停留当前页面");
                        updateTaskText();
                        return;
                    }

                    // 检查是否有原点数据，跳转到返航页面
                    if (DeliveryActivity.originPoints != null && !DeliveryActivity.originPoints.isEmpty()) {
                        if (WorkScheduleService.getInstance().hasDeferredWorkEnd()) {
                            TaskExecutionStateManager.getInstance().finishTask();
                            if (WorkScheduleService.getInstance().executeDeferredActionIfIdle()) {
                                finish();
                                return;
                            }
                        }
                        Log.d(TAG, "【导航控制】最后一站失败，但仍跳转到返航页面");
                        Intent intent = new Intent(DeliveryNavigationActivity.this, ReturnActivity.class);
                        intent.putExtra("return_source_mode", 1); // 1: Delivery
                        startActivity(intent);
                        finish();
                    } else {
                        TaskExecutionStateManager.getInstance().finishTask();
                        if (WorkScheduleService.getInstance().executeDeferredActionIfIdle()) {
                            finish();
                            return;
                        }
                        Log.d(TAG, "【导航控制】无原点数据，直接结束");
                        updateTaskText();
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_END_NAVIGATION_PASSWORD) {
            // 无论验证是否成功，都不需要显式停止密码计时器了
            if (resultCode == RESULT_OK) {
                performEndNavigation();
            }
        }
    }

    private void performEndNavigation() {
        Log.d(TAG, "【操作】密码验证通过，执行结束导航");
        // 如果配送任务尚未完成就被手动结束，记录为取消状态
        if (!isMissionFinished && currentUniqueTargetIndex < targetNodes.size()) {
            NavigationNode currentNode = targetNodes.get(currentUniqueTargetIndex);
            ItemDeliveryManager.getInstance().recordPointArrival(currentNode.getName(),
                    ItemDeliveryRecord.STATUS_CANCELLED);
        }

        cancelActiveTask();
        stopNavigation();
        finish();
    }

    // ==================== Audio Helper Methods ====================

    private void playBackgroundMusic() {
        if (audioService != null) {
            audioService.getAudioConfig(new IResultCallback<com.weigao.robot.control.model.AudioConfig>() {
                @Override
                public void onSuccess(com.weigao.robot.control.model.AudioConfig config) {
                    if (config != null && config.isDeliveryMusicEnabled()
                            && !android.text.TextUtils.isEmpty(config.getDeliveryMusicPath())) {
                        audioService.playBackgroundMusic(config.getDeliveryMusicPath(), true, null);
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
                    if (config != null && config.isDeliveryVoiceEnabled()) {
                        String path = isArrival ? config.getDeliveryArrivalVoicePath()
                                : config.getDeliveryNavigatingVoicePath();
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
            runOnUiThread(DeliveryNavigationActivity.this::updateDoorToggleButton);
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
