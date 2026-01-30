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
import com.weigao.robot.control.callback.INavigationCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.manager.CircularDeliveryHistoryManager;
import com.weigao.robot.control.model.CircularDeliveryRecord;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.INavigationService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.app.WeigaoApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * 循环配送导航界面
 */
public class CircularDeliveryNavigationActivity extends AppCompatActivity implements INavigationCallback {

    private static final String TAG = "CircularNavActivity";

    private TextView tvStatus, currentTaskTextView, tvHint, tvLoopCount;
    private TextView tvCountdown;
    private android.widget.ProgressBar pbProgress;
    private Button btnPauseEnd, btnContinue, btnReturnOrigin;
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

    private String routeName;
    private int loopCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circular_delivery_navigation); // Use separate layout

        initViews();



        navigationService = ServiceManager.getInstance().getNavigationService();
        audioService = ServiceManager.getInstance().getAudioService(); // Init AudioService
        if (navigationService == null) {
            Toast.makeText(this, "导航服务未初始化", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        navigationService.registerCallback(this);

        // Get Data
        ArrayList<NavigationNode> rawNodes = (ArrayList<NavigationNode>) getIntent()
                .getSerializableExtra("route_nodes");
        routeName = getIntent().getStringExtra("route_name");
        loopCount = getIntent().getIntExtra("loop_count", 1);

        if (rawNodes == null || rawNodes.isEmpty()) {
            Toast.makeText(this, "路线节点为空", Toast.LENGTH_SHORT).show();
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
            // 不停止自动恢复计时，让其继续计时 (参考 ConfirmReceiptActivity)
            Intent intent = new Intent(this, com.weigao.robot.control.ui.auth.PasswordActivity.class);
            startActivityForResult(intent, REQUEST_CODE_END_NAVIGATION_PASSWORD);
        });

        // Continue button logic
        btnContinue.setText("继续");
        btnContinue.setOnClickListener(v -> {
            if (isPaused) {
                resumeNavigation();
            } else if (isWaitingAtNode) {
                proceedToNextNode();
            }
        });

        btnReturnOrigin.setOnClickListener(v -> {
            stopNavigation();
            Intent intent = new Intent(this, ReturnActivity.class);
            intent.putExtra("return_speed",
                    com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance().getReturnSpeed());
            intent.putExtra("return_source_mode", 2); // 2 = Loop
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
                // 播放背景音乐
                playBackgroundMusic();
                // 播报语音
                playConfiguredVoice(false);

                int speed = com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance()
                        .getDeliverySpeed();
                navigationService.setSpeed(speed, new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        navigationService.prepare(new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
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
        // 1s内尽可能多发暂停指令，从第一次发计时1s
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
                    // btnReturnOrigin.setVisibility(View.VISIBLE); // Removed as per user request
                    tvHint.setVisibility(View.INVISIBLE);
                    
                    startAutoResumeTimer();

                    // pauseBackgroundMusic(); // Modified: Keep music playing during pause
                    // speak("已暂停循环配送");
                });
            }

            @Override
            public void onError(ApiError error) {
            }
        });
    }

    private void resumeNavigation() {
        stopAutoResumeTimer();
        navigationService.start(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    isPaused = false;
                    tvStatus.setText("导航中");
                    llPauseControls.setVisibility(View.GONE);
                    btnReturnOrigin.setVisibility(View.GONE);
                    tvHint.setVisibility(View.VISIBLE);

                    // resumeBackgroundMusic(); // Modified: Music is already playing
                    playConfiguredVoice(false);
                });
            }

            @Override
            public void onError(ApiError error) {
            }
        });
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
                        // Toast.makeText(CircularDeliveryNavigationActivity.this, "关门失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
                runOnUiThread(() -> Toast.makeText(CircularDeliveryNavigationActivity.this, "无法前往下一站: " + error.getMessage(), Toast.LENGTH_SHORT).show());
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
                    // Arrived
                    // pauseBackgroundMusic(); // Modified: Keep music playing at destination
                    playConfiguredVoice(true);
                    handleArrival();
                    break;
                case Navigation.STATE_PAUSED:
                    isPaused = true;
                    tvStatus.setText("已暂停");
                    llPauseControls.setVisibility(View.VISIBLE);
                    // btnReturnOrigin.setVisibility(View.VISIBLE); // Removed as per user request
                    tvHint.setVisibility(View.INVISIBLE);
                    break;
                case Navigation.STATE_COLLISION:
                case Navigation.STATE_BLOCKED:
                    // 短暂阻挡或碰撞,正在避障
                    Log.w(TAG, "【导航回调】遇到障碍物，正在避障");
                    Toast.makeText(this, "遇到障碍物，正在避障", Toast.LENGTH_SHORT).show();
                    // speak("遇到障碍物，正在避障");
                    break;

                case Navigation.STATE_BLOCKING:
                    // 长时间阻挡超时
                    Log.w(TAG, "【导航回调】阻挡超时");
                    Toast.makeText(this, "阻挡超时，请检查路径", Toast.LENGTH_SHORT).show();
                    // speak("长时间被阻挡，请检查路径");
                    
                    // 记录导航失败状态
                    if (currentRecord != null && currentTaskIndex < targetNodes.size()) {
                        currentRecord.complete("NAV_FAILED");
                        Log.d(TAG, "【记录】当前导航任务被标记为失败状态");
                    }
                    break;
                case Navigation.STATE_RUNNING:
                    if (isPaused) {
                        if (System.currentTimeMillis() - lastPauseTime > 1000) {
                            if (pauseRetryCount < 1) {
                                pauseRetryCount++;
                                lastPauseTime = System.currentTimeMillis();
                                Log.d(TAG, "暂停无效，尝试二次暂停...");
                                navigationService.pause(null);
                                return;
                            }
                            // 如果暂停后超过1秒仍收到运行状态，说明暂停失败或已被覆盖，强制同步UI
                            isPaused = false;
                            stopAutoResumeTimer();
                            Toast.makeText(CircularDeliveryNavigationActivity.this, "暂停失败，请重试", Toast.LENGTH_SHORT).show();
                        } else {
                            // 暂停指令发出不久，忽略短暂的运行状态回调（避免UI闪烁）
                            return;
                        }
                    }
                    tvStatus.setText("导航中");
                    tvHint.setText("正在前往目的地...");
                    tvHint.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    llPauseControls.setVisibility(View.GONE);
                    btnReturnOrigin.setVisibility(View.GONE);
                    tvHint.setVisibility(View.VISIBLE);
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
        // isReturning logic is handled by jumping to ReturnActivity
        /*
         * if (isReturning) {
         * Toast.makeText(this, "已返回出餐口，任务结束", Toast.LENGTH_LONG).show();
         * if (currentRecord != null) {
         * currentRecord.complete("COMPLETED");
         * CircularDeliveryHistoryManager.getInstance(this).addRecord(currentRecord);
         * }
         * finish();
         * return;
         * }
         */

        isWaitingAtNode = true;

        Intent intent = new Intent(this, CircularArrivalActivity.class);
        boolean isLastPoint = (currentTaskIndex >= targetNodes.size() - 1);
        intent.putExtra("is_last_point", isLastPoint);
        if (currentTaskIndex < targetNodes.size()) {
            intent.putExtra("current_point_name", targetNodes.get(currentTaskIndex).getName());
        }
        startActivityForResult(intent, REQUEST_CODE_ARRIVAL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ARRIVAL) {
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
                    Toast.makeText(this, "任务终止，开始返航", Toast.LENGTH_SHORT).show();
                    Intent returnIntent = new Intent(this, ReturnActivity.class);
                    returnIntent.putExtra("return_source_mode", 2);
                    startActivity(returnIntent);
                    finish();
                }
            } else if (resultCode == CircularArrivalActivity.RESULT_CANCEL || resultCode == RESULT_CANCELED) {
                // Cancelled
                Toast.makeText(this, "循环任务已取消", Toast.LENGTH_SHORT).show();
                stopNavigation();
                finish();
            }
        } else if (requestCode == REQUEST_CODE_END_NAVIGATION_PASSWORD) {
            // 无论验证是否成功，都不需要显式停止密码计时器了（因为它不存在了）
            // 也不需要重启自动恢复计时器（因为它没停过）
            if (resultCode == RESULT_OK) {
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
        if (navigationService != null) {
            stopNavigation();
            navigationService.unregisterCallback(this);
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
                    if (config != null && config.isLoopMusicEnabled() && !android.text.TextUtils.isEmpty(config.getLoopMusicPath())) {
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
                        String path = isArrival ? config.getLoopArrivalVoicePath() : config.getLoopNavigatingVoicePath();
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
        
        tvCountdown.setVisibility(View.VISIBLE);
        
        autoResumeTimer = new android.os.CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isFinishing()) {
                    tvCountdown.setText((millisUntilFinished / 1000) + "s 后自动继续");
                }
            }

            @Override
            public void onFinish() {
                if (!isFinishing() && isPaused) {
                    // 关键：如果密码页面还在显示，强制关闭它 (参考 ConfirmReceiptActivity)
                    finishActivity(REQUEST_CODE_END_NAVIGATION_PASSWORD);
                    
                    tvCountdown.setVisibility(View.GONE);
                    Toast.makeText(CircularDeliveryNavigationActivity.this, "暂停超时，自动继续", Toast.LENGTH_SHORT).show();
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
}
