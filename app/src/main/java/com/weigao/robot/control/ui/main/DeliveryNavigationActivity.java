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
import com.weigao.robot.control.manager.ItemDeliveryManager;
import com.weigao.robot.control.model.ItemDeliveryRecord;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.INavigationService;
import com.weigao.robot.control.service.ServiceManager;

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

    private TextView tvStatus, currentTaskTextView, tvHint;
    private Button btnPauseEnd, btnContinue;
    private LinearLayout llPauseControls;
    private View rootLayout;

    private List<Map.Entry<Integer, NavigationNode>> deliveryTasks;
    // 存储配对关系，提升为成员变量以便在后续操作中修改
    private HashMap<Integer, NavigationNode> pairings;

    private int currentTaskIndex = 0;
    private boolean isPaused = false;
    private boolean isNavigating = false;
    // 是否正在返回原点
    private boolean isReturning = false;

    private GestureDetector gestureDetector;

    /**
     * 导航服务
     */
    private INavigationService navigationService;
    private com.weigao.robot.control.service.IAudioService audioService; // Audio Service

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

        Log.d(TAG, "【Activity】onCreate - 初始化配送导航界面");

        // 初始化视图
        initViews();

        // 获取导航服务
        navigationService = ServiceManager.getInstance().getNavigationService();
        audioService = ServiceManager.getInstance().getAudioService(); // Init Audio
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
        pairings = (HashMap<Integer, NavigationNode>) getIntent()
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

            // 如果配送任务尚未完成就被手动结束，记录为取消状态
            if (!isMissionFinished && !isReturning && currentUniqueTargetIndex < targetNodes.size()) {
                NavigationNode currentNode = targetNodes.get(currentUniqueTargetIndex);
                ItemDeliveryManager.getInstance().recordPointArrival(currentNode.getName(),
                        ItemDeliveryRecord.STATUS_CANCELLED);
            }

            Intent returnIntent = new Intent();
            if (pairings != null) {
                returnIntent.putExtra("remaining_pairings", pairings);
            }
            setResult(RESULT_OK, returnIntent);
            stopNavigation();
            finish();
        });

        // "完成取物" / "继续" 按钮逻辑复用
        // 若在到达等待状态，执行 processDeparture
        btnContinue.setOnClickListener(v -> {
            Log.d(TAG, "【用户操作】点击继续/完成取物按钮");
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

        // 重置运行状态标志，防止粘性到达事件
        hasRunningStateReceived = false;

        Log.d(TAG, "【导航控制】开始导航，目标点数量: " + targetNodes.size());
        isNavigating = true;

        // 提取目标ID列表
        List<Integer> targetIds = new ArrayList<>();
        for (NavigationNode node : targetNodes) {
            targetIds.add(node.getId());
        }

        // 设置导航目标点（使用 setTargets 传递 ID 列表）
        navigationService.setTargets(targetIds, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "【导航控制】设置目标点成功");
                // 播放背景音乐
                playBackgroundMusic();
                // 播报语音
                speak("开始配送任务");

                // 设置导航速度
                int speed = isReturning
                        ? com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance().getReturnSpeed()
                        : com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance().getDeliverySpeed();

                navigationService.setSpeed(speed, new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "【导航控制】设置速度成功");
                        // 准备导航路线
                        navigationService.prepare(new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "【导航控制】准备路线成功");
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
                pauseBackgroundMusic();
                speak("已暂停配送");
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
        // 重置标志位，因为恢复或跳转都会重新进入运行状态
        hasRunningStateReceived = false;

        // 恢复背景音乐
        resumeBackgroundMusic();
        speak("继续配送");

        if (waitingForNext) {
            Log.d(TAG, "【导航控制】恢复导航：处于等待跳转状态，立即前往下一目标");
            waitingForNext = false;
            navigationService.pilotNext(new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "【导航控制】前往下一点成功");
                    runOnUiThread(() -> tvStatus.setText("配送中"));
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
                stopBackgroundMusic();
                speak("停止配送");
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
                tvStatus.setText("配送中");
                llPauseControls.setVisibility(View.GONE);
                tvHint.setVisibility(View.VISIBLE);
                Log.d(TAG, "【UI更新】显示导航中状态");
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

            if (isReturning) {
                String targetName = (targetNodes != null && !targetNodes.isEmpty()) ? targetNodes.get(0).getName()
                        : "原点";
                currentTaskTextView.setText("所有任务已完成，正在返回：" + targetName);
                tvStatus.setText("返航中");
                return;
            }

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
        currentTaskTextView.setText("所有任务已完成！");
        tvStatus.setText("配送完成");
        tvHint.setVisibility(View.GONE);
        llPauseControls.setVisibility(View.VISIBLE);
        btnContinue.setVisibility(View.GONE);
        btnPauseEnd.setText("返回首页");
        rootLayout.setOnTouchListener(null);
        isNavigating = false;
        Log.d(TAG, "【UI更新】所有任务已完成");
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
                case Navigation.STATE_RUNNING:
                    Log.d(TAG, "【导航回调】正在运行中");
                    hasRunningStateReceived = true;
                    // 如果不是暂停状态，确保显示为配送中
                    if (!isPaused && !tvStatus.getText().toString().equals("配送中") && !isReturning) {
                        tvStatus.setText("配送中");
                    }
                    if (isReturning && !tvStatus.getText().toString().equals("返航中") && !isMissionFinished) {
                        tvStatus.setText("返航中");
                    }
                    if (schedule > 0 && schedule % 5 == 0) { // 防止过于频繁
                        // speak("配送中，请避让"); 也许太吵了
                    }
                    break;

                case Navigation.STATE_DESTINATION:
                    // 到达目标点
                    if (hasRunningStateReceived) {
                        Log.d(TAG, "【导航回调】已到达目标点");
                        Toast.makeText(this, "已到达目标点", Toast.LENGTH_SHORT).show();

                        pauseBackgroundMusic();
                        speak("已到达目的地");

                        handleArrival();
                        // 重置标志，防止重复触发，且等待下一段运行
                        hasRunningStateReceived = false;
                    } else {
                        Log.w(TAG, "【导航回调】忽略无效的到达状态(未经历运行阶段)");
                    }
                    break;

                case Navigation.STATE_COLLISION:
                case Navigation.STATE_BLOCKED:
                    Log.w(TAG, "【导航回调】遇到障碍物，正在避障");
                    Toast.makeText(this, "遇到障碍物，正在避障", Toast.LENGTH_SHORT).show();
                    speak("遇到障碍物，正在避障");
                    break;

                case Navigation.STATE_BLOCKING:
                    Log.w(TAG, "【导航回调】阻挡超时");
                    Toast.makeText(this, "阻挡超时，请检查路径", Toast.LENGTH_SHORT).show();
                    speak("长时间被阻挡，请检查路径");
                    if (currentUniqueTargetIndex < targetNodes.size()) {
                        ItemDeliveryManager.getInstance().recordPointArrival(
                                targetNodes.get(currentUniqueTargetIndex).getName(),
                                ItemDeliveryRecord.STATUS_NAV_FAILED);
                    }
                    break;

                case Navigation.STATE_ERROR:
                    Log.e(TAG, "【导航回调】导航错误");
                    Toast.makeText(this, "导航出现错误", Toast.LENGTH_SHORT).show();
                    speak("导航出现错误");
                    if (currentUniqueTargetIndex < targetNodes.size()) {
                        ItemDeliveryManager.getInstance().recordPointArrival(
                                targetNodes.get(currentUniqueTargetIndex).getName(),
                                ItemDeliveryRecord.STATUS_NAV_FAILED);
                    }
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
        Log.d(TAG, "【导航回调】onRoutePrepared - 路线准备完成，节点数: " + (nodes != null ? nodes.size() : 0));
        // 路线准备完成，自动开始导航
        navigationService.start(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "【导航控制】自动开始导航成功");
                runOnUiThread(() -> {
                    tvStatus.setText("配送中");
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
        if (isReturning) {
            Log.d(TAG, "【到达处理】已回到原点，任务彻底结束");
            runOnUiThread(() -> {
                Toast.makeText(this, "已回到原点", Toast.LENGTH_LONG).show();
                showMissionCompletedUI();
            });
            return;
        }

        Log.d(TAG, "【到达处理】当前唯一目标索引: " + currentUniqueTargetIndex + ", 总唯一目标数: " + targetNodes.size());

        // 无论是否是最后一个点，都跳转确认页面供用户取货
        waitingForNext = true;

        // 跳转到 ConfirmReceiptActivity
        Intent intent = new Intent(this, ConfirmReceiptActivity.class);

        // 传递配对信息(用于显示需要取货的层)
        // ConfirmReceiptActivity 会根据当前节点ID自动筛选出对应的所有层级，所以传完整的pairings即可
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
        stopBackgroundMusic();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CONFIRM_RECEIPT) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "【ConfirmReceipt】返回确认");
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

                    // 检查是否有原点数据
                    if (DeliveryActivity.originPoints != null && !DeliveryActivity.originPoints.isEmpty()) {
                        Log.d(TAG, "【导航控制】找到原点，开始自动返回");
                        NavigationNode originNode = DeliveryActivity.originPoints.get(0);

                        // 设置返回模式
                        isReturning = true;

                        // 重置目标点为原点
                        targetNodes = new ArrayList<>();
                        targetNodes.add(originNode);

                        // 更新UI并开始导航
                        updateTaskText();
                        startNavigation();
                    } else {
                        Log.d(TAG, "【导航控制】无原点数据，直接结束");
                        updateTaskText();
                    }
                }
            } else {
                Log.d(TAG, "【ConfirmReceipt】返回非OK，可能取消或异常");
                // 视需求处理，暂时保持等待或恢复
            }
        }
    }

    // ==================== Audio Helper Methods ====================

    private void playBackgroundMusic() {
        if (audioService != null) {
            audioService.getAudioConfig(new IResultCallback<com.weigao.robot.control.model.AudioConfig>() {
                @Override
                public void onSuccess(com.weigao.robot.control.model.AudioConfig config) {
                    if (config != null && !android.text.TextUtils.isEmpty(config.getDeliveryMusicPath())) {
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

    private void speak(String text) {
        if (audioService != null) {
            audioService.speak(text, null);
        }
    }
}
