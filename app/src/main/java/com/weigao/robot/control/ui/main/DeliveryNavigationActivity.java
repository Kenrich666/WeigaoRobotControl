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

    private TextView tvStatus, currentTaskTextView, tvHint;
    private Button btnPauseEnd, btnContinue;
    private LinearLayout llPauseControls;
    private View rootLayout;

    private List<Map.Entry<Integer, NavigationNode>> deliveryTasks;

    private int currentTaskIndex = 0;
    private boolean isPaused = false;
    private boolean isNavigating = false;

    private GestureDetector gestureDetector;

    /** 导航服务 */
    private INavigationService navigationService;

    /** 当前导航的目标点列表 */
    private List<NavigationNode> targetNodes;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_navigation);

        Log.d(TAG, "【Activity】onCreate - 初始化配送导航界面");

        // 初始化视图
        initViews();

        // 获取导航服务
        navigationService = ServiceManager.getInstance().getNavigationService();
        if (navigationService == null) {
            Log.e(TAG, "【错误】无法获取导航服务");
            Toast.makeText(this, "导航服务未初始化", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 注册导航回调
        navigationService.registerCallback(this);
        Log.d(TAG, "【导航服务】已注册回调监听器");

        // 获取配送任务
        HashMap<Integer, NavigationNode> pairings = (HashMap<Integer, NavigationNode>) getIntent()
                .getSerializableExtra("pairings");

        if (pairings == null || pairings.isEmpty()) {
            Log.w(TAG, "【警告】没有配送任务");
            Toast.makeText(this, "没有配送任务", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 按楼层排序任务
        deliveryTasks = new ArrayList<>(pairings.entrySet());
        Collections.sort(deliveryTasks,
                (o1, o2) -> Integer.compare(getLayerNumber(o1.getKey()), getLayerNumber(o2.getKey())));

        Log.d(TAG, "【配送任务】共 " + deliveryTasks.size() + " 个任务");

        // 准备导航目标点ID列表
        prepareNavigationTargets();

        // 更新任务文本
        updateTaskText();

        // 设置手势检测器
        setupGestureDetector();

        // 设置按钮监听器
        setupButtons();

        // 开始导航
        startNavigation();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        currentTaskTextView = findViewById(R.id.current_task_textview);
        tvHint = findViewById(R.id.tv_hint);
        llPauseControls = findViewById(R.id.ll_pause_controls);
        btnPauseEnd = findViewById(R.id.btn_pause_end);
        btnContinue = findViewById(R.id.btn_continue);
        rootLayout = findViewById(R.id.root_layout);
    }

    /**
     * 准备导航目标点列表
     * <p>
     * 直接从 deliveryTasks 中提取 NavigationNode
     * </p>
     */
    private void prepareNavigationTargets() {
        targetNodes = new ArrayList<>();
        for (Map.Entry<Integer, NavigationNode> task : deliveryTasks) {
            NavigationNode node = task.getValue();
            if (node != null) {
                targetNodes.add(node);
                Log.d(TAG, "【目标点】添加目标点: id=" + node.getId() + ", name=" + node.getName());
            } else {
                Log.w(TAG, "【警告】任务节点为空");
            }
        }
        Log.d(TAG, "【目标点】目标点数量: " + targetNodes.size());
    }

    /**
     * 设置手势检测器（双击暂停）
     */
    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isNavigating && currentTaskIndex < deliveryTasks.size()) {
                    Log.d(TAG, "【用户操作】双击屏幕，暂停导航");
                    setPauseState(true);
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
            Log.d(TAG, "【用户操作】点击结束按钮");
            stopNavigation();
            finish();
        });

        btnContinue.setOnClickListener(v -> {
            Log.d(TAG, "【用户操作】点击继续按钮");
            setPauseState(false);
            resumeNavigation();
        });
    }

    /**
     * 开始导航
     */
    private void startNavigation() {
        if (targetNodes == null || targetNodes.isEmpty()) {
            Log.e(TAG, "【错误】目标点列表为空，无法开始导航");
            Toast.makeText(this, "目标点列表为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "【导航控制】开始导航，目标点数量: " + targetNodes.size());
        isNavigating = true;

        // 设置导航目标点（使用 setTargetNodes 传递完整的 NavigationNode 列表）
        navigationService.setTargetNodes(targetNodes, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "【导航控制】设置目标点成功");
                // 设置导航速度 (30 cm/s)
                navigationService.setSpeed(30, new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "【导航控制】设置速度成功");
                        // 准备导航路线
                        navigationService.prepare(new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "【导航控制】准备路线成功");
                                // 路线准备完成后会在 onRoutePrepared 回调中自动开始导航
                            }

                            @Override
                            public void onError(ApiError error) {
                                Log.e(TAG, "【导航控制】准备路线失败: " + error.getMessage());
                                runOnUiThread(() -> {
                                    Toast.makeText(DeliveryNavigationActivity.this,
                                            "准备路线失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        Log.e(TAG, "【导航控制】设置速度失败: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "【导航控制】设置目标点失败: " + error.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(DeliveryNavigationActivity.this,
                            "设置目标点失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    /**
     * 暂停导航
     */
    private void pauseNavigation() {
        navigationService.pause(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "【导航控制】暂停成功");
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "【导航控制】暂停失败: " + error.getMessage());
            }
        });
    }

    private boolean waitingForNext = false;

    /**
     * 恢复导航
     */
    private void resumeNavigation() {
        if (waitingForNext) {
            Log.d(TAG, "【导航控制】恢复导航：处于等待跳转状态，立即前往下一目标");
            waitingForNext = false;
            navigationService.pilotNext(new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "【导航控制】前往下一点成功");
                    runOnUiThread(() -> tvStatus.setText("送物中"));
                }

                @Override
                public void onError(ApiError error) {
                    Log.e(TAG, "【导航控制】前往下一点失败: " + error.getMessage());
                }
            });
        } else {
            Log.d(TAG, "【导航控制】恢复导航：继续当前路径");
            navigationService.start(new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "【导航控制】恢复成功");
                }

                @Override
                public void onError(ApiError error) {
                    Log.e(TAG, "【导航控制】恢复失败: " + error.getMessage());
                }
            });
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
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "【导航控制】停止失败: " + error.getMessage());
            }
        });
    }

    /**
     * 设置暂停状态
     */
    private void setPauseState(boolean paused) {
        isPaused = paused;
        runOnUiThread(() -> {
            if (isPaused) {
                tvStatus.setText("已暂停");
                llPauseControls.setVisibility(View.VISIBLE);
                tvHint.setVisibility(View.INVISIBLE);
                Log.d(TAG, "【UI更新】显示暂停状态");
            } else {
                tvStatus.setText("送物中");
                llPauseControls.setVisibility(View.GONE);
                tvHint.setVisibility(View.VISIBLE);
                Log.d(TAG, "【UI更新】显示导航中状态");
            }
        });
    }

    /**
     * 更新任务文本
     */
    private void updateTaskText() {
        runOnUiThread(() -> {
            if (currentTaskIndex < deliveryTasks.size()) {
                Map.Entry<Integer, NavigationNode> currentTask = deliveryTasks.get(currentTaskIndex);
                String pointText = currentTask.getValue().getName();
                int totalTasks = deliveryTasks.size();
                currentTaskTextView.setText(String.format("正在前往：%s (第 %d/%d 个)",
                        pointText, currentTaskIndex + 1, totalTasks));
                Log.d(TAG, "【UI更新】当前任务: " + pointText + " (" + (currentTaskIndex + 1) + "/" + totalTasks + ")");

            } else {
                currentTaskTextView.setText("所有任务已完成！");
                tvStatus.setText("已完成");
                tvHint.setVisibility(View.GONE);
                llPauseControls.setVisibility(View.VISIBLE);
                btnContinue.setVisibility(View.GONE);
                btnPauseEnd.setText("返回首页");
                rootLayout.setOnTouchListener(null);
                isNavigating = false;
                Log.d(TAG, "【UI更新】所有任务已完成");
            }
        });
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
        Log.d(TAG, "【导航回调】onStateChanged - state: " + state + ", schedule: " + schedule);
        runOnUiThread(() -> {
            switch (state) {
                case Navigation.STATE_DESTINATION:
                    // 到达目标点
                    Log.d(TAG, "【导航回调】已到达目标点");
                    Toast.makeText(this, "已到达目标点", Toast.LENGTH_SHORT).show();
                    handleArrival();
                    break;

                case Navigation.STATE_COLLISION:
                case Navigation.STATE_BLOCKED:
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
    public void onRouteNode(int index, NavigationNode node) {
        Log.d(TAG, "【导航回调】onRouteNode - index: " + index + ", node: " + (node != null ? node.toString() : "null"));
        currentTaskIndex = index;
        runOnUiThread(() -> updateTaskText());
    }

    @Override
    public void onRoutePrepared(List<NavigationNode> nodes) {
        Log.d(TAG, "【导航回调】onRoutePrepared - 路线准备完成，节点数: " + (nodes != null ? nodes.size() : 0));
        // 路线准备完成，自动开始导航
        navigationService.start(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "【导航控制】自动开始导航成功");
                runOnUiThread(() -> {
                    tvStatus.setText("送物中");
                    Toast.makeText(DeliveryNavigationActivity.this, "开始导航", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(ApiError error) {
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
        Log.e(TAG, "【导航回调】导航错误，错误码: " + errorCode);
        runOnUiThread(() -> {
            Toast.makeText(this, "导航错误，错误码: " + errorCode, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(int errorCode, String message) {
        Log.e(TAG, "【导航回调】错误 - 错误码: " + errorCode + ", 消息: " + message);
        runOnUiThread(() -> {
            Toast.makeText(this, "错误: " + message, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 处理到达目标点
     */
    private void handleArrival() {
        Log.d(TAG, "【到达处理】当前索引: " + currentTaskIndex + ", 总任务数: " + deliveryTasks.size());

        // 检查是否还有下一个点
        if (currentTaskIndex < deliveryTasks.size() - 1) {
            // 还有下一个点，继续导航
            Log.d(TAG, "【到达处理】还有下一个点，3秒后继续");
            // 延迟3秒后前往下一个点
            tvStatus.setText("已到达，3秒后继续");
            waitingForNext = true;
            rootLayout.postDelayed(() -> {
                if (isNavigating && !isPaused) {
                    Log.d(TAG, "【到达处理】前往下一个目标点");
                    waitingForNext = false;
                    navigationService.pilotNext(new IResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Log.d(TAG, "【导航控制】前往下一点成功");
                            runOnUiThread(() -> tvStatus.setText("送物中"));
                        }

                        @Override
                        public void onError(ApiError error) {
                            Log.e(TAG, "【导航控制】前往下一点失败: " + error.getMessage());
                        }
                    });
                } else {
                    Log.d(TAG, "【到达处理】已暂停或停止，跳过自动跳转，保持 waitingForNext=true");
                }
            }, 3000);
        } else {
            // 所有任务完成
            Log.d(TAG, "【到达处理】所有任务已完成");
            currentTaskIndex = deliveryTasks.size();
            updateTaskText();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "【Activity】onDestroy - 销毁活动");
        // 停止导航
        if (navigationService != null) {
            stopNavigation();
            // 注销回调
            navigationService.unregisterCallback(this);
            Log.d(TAG, "【导航服务】已注销回调监听器");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PASSWORD && resultCode == RESULT_OK) {
            finish();
        }
    }
}
