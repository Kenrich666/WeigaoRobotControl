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
import com.weigao.robot.control.callback.INavigationCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.INavigationService;
import com.weigao.robot.control.service.ServiceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 返航页面
 * 负责控制机器人返回充电桩或原点
 */
public class ReturnActivity extends AppCompatActivity implements INavigationCallback {

    private static final String TAG = "ReturnActivity";

    // 控件
    private LinearLayout llControls;
    private TextView tvHint;
    private TextView tvStatus;
    private GestureDetector gestureDetector;
    private View rootLayout;

    // 导航服务
    private INavigationService navigationService;
    private boolean isNavigating = false;
    private boolean isPaused = false;
    private boolean hasRunningStateReceived = false; // 防止粘性到达事件
    private com.weigao.robot.control.service.IAudioService audioService;
    private int sourceMode = 1; // 1: Delivery, 2: Loop

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return);

        initViews();
        initService();
        setupGesture();
        setupButtons();

        // 获取源模式
        sourceMode = getIntent().getIntExtra("return_source_mode", 1);

        // 延迟一点启动，给UI渲染时间
        rootLayout.postDelayed(this::startReturnNavigation, 500);
    }

    private void initViews() {
        rootLayout = findViewById(R.id.root_layout);
        llControls = findViewById(R.id.ll_controls);
        tvHint = findViewById(R.id.tv_hint);
        tvStatus = findViewById(R.id.tv_status);
        tvStatus.setText("返航中");
    }

    private void initService() {
        navigationService = ServiceManager.getInstance().getNavigationService();
        audioService = ServiceManager.getInstance().getAudioService();
        if (navigationService != null) {
            navigationService.registerCallback(this);
        } else {
            Toast.makeText(this, "导航服务不可用", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupGesture() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // 双击暂停
                pauseNavigation();
                return true;
            }
        });

        rootLayout.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void setupButtons() {
        Button btnContinue = findViewById(R.id.btn_continue);
        Button btnEnd = findViewById(R.id.btn_end);

        btnContinue.setOnClickListener(v -> {
            // 继续导航
            resumeNavigation();
        });

        btnEnd.setOnClickListener(v -> {
            // 结束并停止
            stopNavigation();
        });
    }

    /**
     * 开始返航导航
     */
    private void startReturnNavigation() {
        if (isNavigating)
            return;

        // 确定目标点：直接返回原点
        NavigationNode targetNode = null;
        if (DeliveryActivity.originPoints != null && !DeliveryActivity.originPoints.isEmpty()) {
            targetNode = DeliveryActivity.originPoints.get(0);
            Log.d(TAG, "选择返航目标: 原点 - " + targetNode.getName());
        }

        if (targetNode == null) {
            Toast.makeText(this, "未找到返航点（原点）", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        final int targetId = targetNode.getId();
        isNavigating = true;

        List<Integer> targetIds = new ArrayList<>();
        targetIds.add(targetId);

        // 重置运行状态标志，防止粘性到达事件
        hasRunningStateReceived = false;

        // 1. 设置目标
        navigationService.setTargets(targetIds, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // 播报
                // 播报和音乐
                playReturnMusic();
                playReturnVoice(false);

                // 2. 设置速度
                int speed = getIntent().getIntExtra("return_speed", -1);
                if (speed == -1) {
                    speed = com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance()
                            .getReturnSpeed();
                }
                navigationService.setSpeed(speed, new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // 3. 准备路径
                        navigationService.prepare(new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "路径准备成功，等待onRoutePrepared回调开始导航");
                            }

                            @Override
                            public void onError(ApiError error) {
                                handleError("路径准备失败", error);
                            }
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        handleError("速度设置失败", error);
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                handleError("目标点设置失败", error);
            }
        });
    }

    /**
     * 暂停导航
     */
    private void pauseNavigation() {
        if (!isNavigating || isPaused)
            return;

        navigationService.pause(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "【导航控制】暂停成功 - 机器人已停止");
                isPaused = true;
                // if (audioService != null) audioService.pauseBackgroundMusic(null); // Modified: Keep music playing during pause
                runOnUiThread(() -> {
                    showControls(true);
                    tvStatus.setText("已暂停");
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "【导航控制】暂停失败: " + error.getMessage());
                // 关键修复: 暂停失败时回滚UI状态
                runOnUiThread(() -> {
                    isPaused = false;
                    showControls(false);
                    Toast.makeText(ReturnActivity.this,
                            "暂停失败，机器人继续运行", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 恢复导航
     */
    private void resumeNavigation() {
        if (!isNavigating || !isPaused)
            return;

        // 重置标志位，因为恢复会重新进入运行状态
        hasRunningStateReceived = false;

        navigationService.start(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "【导航控制】恢复成功");
                isPaused = false;
                // if (audioService != null) audioService.resumeBackgroundMusic(null); // Modified: Music is already playing
                playReturnVoice(false); // Resuming voice
                runOnUiThread(() -> {
                    showControls(false);
                    tvStatus.setText("返航中");
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "【导航控制】恢复失败: " + error.getMessage());
                runOnUiThread(() -> Toast
                        .makeText(ReturnActivity.this, "恢复失败: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * 停止导航并退出
     */
    private void stopNavigation() {
        navigationService.stop(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                isNavigating = false;
                if (audioService != null) {
                    audioService.stopBackgroundMusic(null);
                    audioService.stopVoice(null);
                }
                runOnUiThread(ReturnActivity.this::finish);
            }

            @Override
            public void onError(ApiError error) {
                // 即使出错也强制退出
                runOnUiThread(ReturnActivity.this::finish);
            }
        });
    }

    private void handleError(String msg, ApiError error) {
        String detail = msg + ": " + (error != null ? error.getMessage() : "Unknown");
        Log.e(TAG, detail);
        runOnUiThread(() -> {
            Toast.makeText(this, detail, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void showControls(boolean showVisible) {
        if (showVisible) {
            llControls.setVisibility(View.VISIBLE);
            tvHint.setVisibility(View.GONE);
        } else {
            llControls.setVisibility(View.GONE);
            tvHint.setVisibility(View.VISIBLE);
        }
    }

    // ==================== Navigation Callbacks ====================

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
                        Log.w(TAG, "【状态同步】棅测到状态不一致: UI显示暂停但机器人运行中，自动修正UI状态");
                        isPaused = false;
                        showControls(false);
                        Toast.makeText(this, "棅测到暂停失败，机器人继续运行", Toast.LENGTH_SHORT).show();
                    }

                    // 如果不是暂停状态，确保显示为返航中
                    if (!isPaused && !tvStatus.getText().toString().equals("返航中")) {
                        tvStatus.setText("返航中");
                    }

                    // 持续播报行驶中语音（AudioServiceImpl会自动处理3秒间隔，不会重复播放）
                    playReturnVoice(false);
                    break;

                case Navigation.STATE_DESTINATION:
                    // 到达目标点
                    if (hasRunningStateReceived) {
                        Log.d(TAG, "【导航回调】已到达目标点");
                        // if (audioService != null) audioService.stopBackgroundMusic(null); // Modified: Keep music playing at destination
                        playReturnVoice(true);
                        Toast.makeText(this, "已回到目标点", Toast.LENGTH_SHORT).show();

                        // 重置标志，防止重复触发
                        hasRunningStateReceived = false;

                        // 延迟一点再退出，让用户看到提示
                        rootLayout.postDelayed(() -> {
                            if (audioService != null) {
                                audioService.stopBackgroundMusic(null);
                                audioService.stopVoice(null);
                            }
                            finish();
                        }, 2000);
                    } else {
                        Log.w(TAG, "【导航回调】忽略无效的到达状态(未经历运行阶段)");
                    }
                    break;

                case Navigation.STATE_BLOCKED:
                case Navigation.STATE_COLLISION:
                    Log.w(TAG, "【导航回调】遇到障碍物，正在避障");
                    Toast.makeText(this, "遇到障碍物，正在避障", Toast.LENGTH_SHORT).show();
                    break;

                case Navigation.STATE_BLOCKING:
                    Log.w(TAG, "【导航回调】阻挡超时");
                    Toast.makeText(this, "阻挡超时，请检查路径", Toast.LENGTH_SHORT).show();
                    break;

                case Navigation.STATE_ERROR:
                    Log.e(TAG, "【导航回调】导航错误");
                    Toast.makeText(this, "导航出现错误", Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    @Override
    public void onRoutePrepared(List<NavigationNode> nodes) {
        // 路径准备完成后，自动开始
        navigationService.start(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "自动开始导航成功");
            }

            @Override
            public void onError(ApiError error) {
                handleError("启动导航失败", error);
            }
        });
    }

    @Override
    public void onRouteNode(int index, NavigationNode node) {
        // 返航通常只有一个点，不太关注中间点
    }

    @Override
    public void onDistanceChanged(double distance) {
        // 可选：显示剩余距离
    }

    @Override
    public void onNavigationError(int errorCode) {
        Log.e(TAG, "导航错误码: " + errorCode);
    }

    @Override
    public void onError(int errorCode, String message) {
        Log.e(TAG, "通用错误: " + errorCode + ", " + message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "【Activity】onDestroy - 销毁活动");
        if (navigationService != null) {
            // 退出页面时确保停止导航
            if (isNavigating) {
                navigationService.stop(null);
            }
            navigationService.unregisterCallback(this);
            Log.d(TAG, "【导航服务】已注销回调监听器");
        }
        if (audioService != null) {
            audioService.stopBackgroundMusic(null);
            audioService.stopVoice(null);
        }
    }

    private void playReturnMusic() {
        if (audioService != null) {
            audioService.getAudioConfig(new IResultCallback<com.weigao.robot.control.model.AudioConfig>() {
                @Override
                public void onSuccess(com.weigao.robot.control.model.AudioConfig config) {
                    if (config != null) {
                        boolean enabled = (sourceMode == 2) ? config.isLoopMusicEnabled() : config.isDeliveryMusicEnabled();
                        String path = (sourceMode == 2) ? config.getLoopMusicPath() : config.getDeliveryMusicPath();
                        
                        if (enabled && !android.text.TextUtils.isEmpty(path)) {
                            audioService.playBackgroundMusic(path, true, null);
                        }
                    }
                }
                @Override public void onError(ApiError e) {}
            });
        }
    }

    private void playReturnVoice(boolean isArrival) {
        if (audioService != null) {
            audioService.getAudioConfig(new IResultCallback<com.weigao.robot.control.model.AudioConfig>() {
                @Override
                public void onSuccess(com.weigao.robot.control.model.AudioConfig config) {
                    if (config != null) {
                        boolean enabled = (sourceMode == 2) ? config.isLoopVoiceEnabled() : config.isDeliveryVoiceEnabled();
                        if (enabled) {
                            String path;
                            if (sourceMode == 2) {
                                path = isArrival ? config.getLoopArrivalVoicePath() : config.getLoopNavigatingVoicePath();
                            } else {
                                path = isArrival ? config.getDeliveryArrivalVoicePath() : config.getDeliveryNavigatingVoicePath();
                            }
                            if (!android.text.TextUtils.isEmpty(path)) {
                                audioService.playVoice(path, null);
                            }
                        }
                    }
                }
                @Override public void onError(ApiError e) {}
            });
        }
    }
}