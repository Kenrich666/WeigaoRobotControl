package com.weigao.robot.control.ui.main;

import android.os.Bundle;
import android.os.Handler;
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
import com.weigao.robot.control.service.IDoorService;
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
    private IDoorService doorService;
    private boolean isNavigating = false;
    private boolean isPaused = false;
    private long lastPauseTime = 0;
    private int pauseRetryCount = 0;
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
        doorService = ServiceManager.getInstance().getDoorService();
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
                pauseRetryCount = 0;
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
     * 开始返航导航 (包含舱门检查)
     */
    private void startReturnNavigation() {
        if (isNavigating)
            return;

        if (doorService == null) {
            performReturnNavigation();
            return;
        }

        // 检查舱门状态
        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                runOnUiThread(() -> {
                    if (allClosed != null && allClosed) {
                        // 已关闭，直接开始
                        performReturnNavigation();
                    } else {
                        // 未关闭，先关门
                        Toast.makeText(ReturnActivity.this, "检测到舱门未关，正在关闭...", Toast.LENGTH_SHORT).show();
                        doorService.closeAllDoors(new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                runOnUiThread(() -> {
                                    Toast.makeText(ReturnActivity.this, "舱门已关闭，5秒后开始返航...", Toast.LENGTH_SHORT).show();
                                    // 增加5秒等待时间
                                    new Handler().postDelayed(() -> performReturnNavigation(), 5000);
                                });
                            }

                            @Override
                            public void onError(ApiError error) {
                                runOnUiThread(() -> {
                                    Toast.makeText(ReturnActivity.this, "自动关门失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> {
                    Toast.makeText(ReturnActivity.this, "查询舱门状态失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    // 查询失败也尝试开始返航? 为了安全起见，或者如果只是传感器错误，还是应该让用户决定?
                    // 参考DeliveryActivity，这里只是Toast。
                    // 但由于这里是自动流程，如果在这里卡住，机器人就停了。
                    // 我们可以尝试直接开始，或者就停在这里。
                    // 假设为了健壮性，查询不到状态就不自动处理舱门，直接尝试返航（或者不返航）。
                    // 这里选择直接尝试返航，防止因舱门服务异常导致无法返航。
                    performReturnNavigation();
                });
            }
        });
    }

    /**
     * 执行实际的返航逻辑
     */
    private void performReturnNavigation() {
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
                isPaused = true;
                lastPauseTime = System.currentTimeMillis();
                
                // 发送第二次暂停指令，确保命令生效 (Double Check)
                new android.os.Handler().postDelayed(() -> {
                    if (navigationService != null) navigationService.pause(null);
                }, 150);

                if (audioService != null) audioService.pauseBackgroundMusic(null);
                runOnUiThread(() -> {
                    showControls(true);
                    tvStatus.setText("已暂停");
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "暂停失败: " + error.getMessage());
            }
        });
    }

    /**
     * 恢复导航
     */
    private void resumeNavigation() {
        if (!isNavigating || !isPaused)
            return;

        navigationService.start(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                isPaused = false;
                if (audioService != null) audioService.resumeBackgroundMusic(null);
                playReturnVoice(false); // Resuming voice
                runOnUiThread(() -> {
                    showControls(false);
                    tvStatus.setText("返航中");
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "恢复失败: " + error.getMessage());
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
        runOnUiThread(() -> {
            switch (state) {
                // 判断到达
                case Navigation.STATE_DESTINATION:
                    if (audioService != null) {
                        audioService.stopBackgroundMusic(null);
                        playReturnVoice(true);
                    }
                    Toast.makeText(this, "已回到目标点", Toast.LENGTH_SHORT).show();
                    
                    // 延迟5秒后关闭Activity，确保到达语音播放完成
                    rootLayout.postDelayed(() -> {
                        finish();
                    }, 5000);
                    break;
                case Navigation.STATE_BLOCKED:
                case Navigation.STATE_COLLISION:
                    Toast.makeText(this, "遇到障碍物，正在避障", Toast.LENGTH_SHORT).show();
                    if (audioService != null) {
                        // audioService.speak("遇到障碍物", null);
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
                            // 暂停失败或失效，强制恢复 UI 状态
                            isPaused = false;
                            showControls(false);
                            tvStatus.setText("返航中");
                            Toast.makeText(ReturnActivity.this, "暂停失败，请重试", Toast.LENGTH_SHORT).show();
                        } else {
                            return;
                        }
                    }
                    tvStatus.setText("返航中");
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
        if (navigationService != null) {
            // 退出页面时确保停止导航
            if (isNavigating) {
                navigationService.stop(null);
            }
            navigationService.unregisterCallback(this);
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            com.weigao.robot.control.app.WeigaoApplication.applyFullScreen(this);
        }
    }
}