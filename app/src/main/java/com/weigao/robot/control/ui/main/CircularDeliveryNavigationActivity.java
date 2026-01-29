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

import java.util.ArrayList;
import java.util.List;

/**
 * 循环配送导航界面
 */
public class CircularDeliveryNavigationActivity extends AppCompatActivity implements INavigationCallback {

    private static final String TAG = "CircularNavActivity";

    private TextView tvStatus, currentTaskTextView, tvHint, tvLoopCount;
    private android.widget.ProgressBar pbProgress;
    private Button btnPauseEnd, btnContinue, btnReturnOrigin;
    private LinearLayout llPauseControls;
    private View rootLayout;

    private List<NavigationNode> targetNodes;
    private int currentTaskIndex = 0;

    private boolean isPaused = false;
    private boolean isNavigating = false;
    private boolean isWaitingAtNode = false;
    private boolean hasRunningStateReceived = false; // 防止粘性到达事件

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

        // 生成往返循环路线 (abccba 模式)
        // 例: A→B→C 循环3次 = A→B→C→B→A→B→C→B→A
        targetNodes = new ArrayList<>();
        for (int i = 0; i < loopCount; i++) {
            if (i % 2 == 0) {
                // 偶数次循环(0,2,4...): 正向 A→B→C
                targetNodes.addAll(rawNodes);
            } else {
                // 奇数次循环(1,3,5...): 反向 C→B→A
                List<NavigationNode> reverseNodes = new ArrayList<>(rawNodes);
                java.util.Collections.reverse(reverseNodes);
                targetNodes.addAll(reverseNodes);
            }
        }

        // 往返循环结束后,机器人会停在起点(偶数次)或终点(奇数次)
        // 用户可以手动点击"返回原点"按钮返航

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
        btnPauseEnd = findViewById(R.id.btn_pause_end);
        btnContinue = findViewById(R.id.btn_continue);
        rootLayout = findViewById(R.id.root_layout);
        btnReturnOrigin = findViewById(R.id.btn_return_origin);

        // Adjust button text for "Pause" state initially hidden
        llPauseControls.setVisibility(View.GONE);
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isNavigating && !isPaused && !isWaitingAtNode) {
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
            stopNavigation();
            finish();
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
        navigationService.pause(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "【导航控制】暂停成功 - 机器人已停止");
                runOnUiThread(() -> {
                    isPaused = true;
                    tvStatus.setText("已暂停");
                    llPauseControls.setVisibility(View.VISIBLE);
                    btnReturnOrigin.setVisibility(View.VISIBLE);
                    tvHint.setVisibility(View.INVISIBLE);

                    // pauseBackgroundMusic(); // Modified: Keep music playing during pause
                    // speak("已暂停循环配送");
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "【导航控制】暂停失败: " + error.getMessage());
                // 关键修复: 暂停失败时回滚UI状态
                runOnUiThread(() -> {
                    isPaused = false;
                    llPauseControls.setVisibility(View.GONE);
                    btnReturnOrigin.setVisibility(View.GONE);
                    tvHint.setVisibility(View.VISIBLE);
                    Toast.makeText(CircularDeliveryNavigationActivity.this,
                            "暂停失败，机器人继续运行", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void resumeNavigation() {
        // 重置标志位，因为恢复或跳转都会重新进入运行状态
        hasRunningStateReceived = false;

        // resumeBackgroundMusic(); // Modified: Music is already playing
        playConfiguredVoice(false);

        navigationService.start(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "【导航控制】恢复成功");
                runOnUiThread(() -> {
                    isPaused = false;
                    tvStatus.setText("导航中");
                    llPauseControls.setVisibility(View.GONE);
                    btnReturnOrigin.setVisibility(View.GONE);
                    tvHint.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "【导航控制】恢复失败: " + error.getMessage());
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
                        Toast.makeText(CircularDeliveryNavigationActivity.this, "关门失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
        // 重置标志位，因为跳转会重新进入运行状态
        hasRunningStateReceived = false;

        // resumeBackgroundMusic(); // Modified: Music is already playing
        playConfiguredVoice(false);

        navigationService.pilotNext(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "【导航控制】前往下一点成功");
                runOnUiThread(() -> {
                    isWaitingAtNode = false;
                    tvStatus.setText("导航中");
                    llPauseControls.setVisibility(View.GONE);
                    tvHint.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "【导航控制】前往下一点失败: " + error.getMessage());
                runOnUiThread(() -> Toast.makeText(CircularDeliveryNavigationActivity.this, "无法前往下一站: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void stopNavigation() {
        navigationService.stop(null);
        isNavigating = false;

        stopBackgroundMusic();
        if (audioService != null) {
            audioService.stopVoice(null); // 停止语音播报
        }

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

                // 判断当前是正向还是反向 (往返循环模式)
                // 偶数轮(0,2,4...)正向, 奇数轮(1,3,5...)反向
                boolean isForward = ((currentLoop - 1) % 2) == 0;
                String directionArrow = isForward ? "→" : "←";
                String directionText = isForward ? "正向" : "返回";

                // 显示循环轮次和方向
                tvLoopCount.setText(String.format("第 %d/%d 轮 (%s %s)", 
                    currentLoop, loopCount, directionText, directionArrow));

                // Progress Bar
                if (pbProgress != null) {
                    pbProgress.setProgress(currentTaskIndex + 1);
                }

                // 显示当前目标
                currentTaskTextView.setText(String.format("正在前往: %s (本轮第 %d/%d 个)",
                        node.getName(), nodeIndexInLoop, nodesPerLoop));
                
                // 显示下一个点位预览
                if (currentTaskIndex + 1 < targetNodes.size()) {
                    NavigationNode nextNode = targetNodes.get(currentTaskIndex + 1);
                    tvHint.setText("下一个: " + nextNode.getName());
                    tvHint.setVisibility(View.VISIBLE);
                } else {
                    // 最后一个点位
                    tvHint.setText("即将完成");
                    tvHint.setVisibility(View.VISIBLE);
                }
            } else {
                currentTaskTextView.setText("导航结束");
                tvLoopCount.setText(String.format("已完成 %d 轮", loopCount));
                if (pbProgress != null)
                    pbProgress.setProgress(targetNodes.size());
            }
        });
    }

    // INavigationCallback Implementation
    @Override
    public void onStateChanged(int state, int schedule) {
        Log.d(TAG, "【导航回调】onStateChanged - state: " + state + ", schedule: " + schedule);
        runOnUiThread(() -> {
            switch (state) {
                case Navigation.STATE_RUNNING:
                    Log.d(TAG, "【导航回调】正在运行中");
                    hasRunningStateReceived = true;

                    // 关键修复: 状态同步检查 - 如果UI显示暂停但机器人实际在运行，说明暂停失败
                    if (isPaused) {
                        Log.w(TAG, "【状态同步】检测到状态不一致: UI显示暂停但机器人运行中，自动修正UI状态");
                        isPaused = false;
                        llPauseControls.setVisibility(View.GONE);
                        btnReturnOrigin.setVisibility(View.GONE);
                        tvHint.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "检测到暂停失败，机器人继续运行", Toast.LENGTH_SHORT).show();
                    }

                    // 如果不是暂停状态，确保显示为导航中
                    if (!isPaused && !tvStatus.getText().toString().equals("导航中")) {
                        tvStatus.setText("导航中");
                    }

                    // 持续播报行驶中语音（AudioServiceImpl会自动处理3秒间隔，不会重复播放）
                    playConfiguredVoice(false);
                    break;

                case Navigation.STATE_DESTINATION:
                    // 到达目标点
                    if (hasRunningStateReceived) {
                        Log.d(TAG, "【导航回调】已到达目标点");
                        Toast.makeText(this, "已到达目标点", Toast.LENGTH_SHORT).show();

                        // pauseBackgroundMusic(); // Modified: Keep music playing at destination
                        playConfiguredVoice(true);

                        handleArrival();
                        // 重置标志，防止重复触发，且等待下一段运行
                        hasRunningStateReceived = false;
                    } else {
                        Log.w(TAG, "【导航回调】忽略无效的到达状态(未经历运行阶段)");
                    }
                    break;

                case Navigation.STATE_PAUSED:
                    isPaused = true;
                    tvStatus.setText("已暂停");
                    llPauseControls.setVisibility(View.VISIBLE);
                    btnReturnOrigin.setVisibility(View.VISIBLE);
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

                case Navigation.STATE_ERROR:
                    Log.e(TAG, "【导航回调】导航错误");
                    Toast.makeText(this, "导航出现错误", Toast.LENGTH_SHORT).show();
                    // 记录错误状态
                    if (currentRecord != null && currentTaskIndex < targetNodes.size()) {
                        currentRecord.complete("NAV_FAILED");
                        Log.d(TAG, "【记录】当前导航任务失败");
                    }
                    break;

                default:
                    break;
            }
        });
    }

    private static final int REQUEST_CODE_ARRIVAL = 1001;

    // ...

    private CircularDeliveryRecord currentRecord;

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
        Log.d(TAG, "【Activity】onDestroy - 销毁活动");
        if (navigationService != null) {
            stopNavigation();
            navigationService.unregisterCallback(this);
            Log.d(TAG, "【导航服务】已注销回调监听器");
        }
        stopBackgroundMusic();
        if (audioService != null) {
            audioService.stopVoice(null);
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
}
