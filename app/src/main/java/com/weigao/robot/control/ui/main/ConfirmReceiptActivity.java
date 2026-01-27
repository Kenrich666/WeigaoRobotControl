package com.weigao.robot.control.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.weigao.robot.control.R;
import com.weigao.robot.control.model.NavigationNode;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.manager.ItemDeliveryManager;
import android.content.Intent;
import android.os.CountDownTimer;
import com.weigao.robot.control.ui.auth.PasswordActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import androidx.appcompat.app.AlertDialog;
import com.weigao.robot.control.model.ItemDeliveryRecord;

/**
 * 到达点位确认收货页面
 * 显示到达的点位名称，并指示需要取走的层级（L1, L2, L3）
 */
public class ConfirmReceiptActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmReceiptActivity";

    private TextView tvPointName;
    private TextView tvLayersList;
    private TextView tvCountdown;
    private TextView tvArrivalDuration;
    private TextView layerL1, layerL2, layerL3;
    private Button btnOpenCabin;
    private AlertDialog durationDialog;

    // 舱门服务
    private IDoorService doorService;
    // 确认关闭状态标记
    private boolean isConfirmState = false;
    private CountDownTimer departureTimer;
    private static final int REQUEST_CODE_VERIFY_PASSWORD = 2001;

    // 存储配对关系：层级 -> 导航点
    // TODO：配对信息占位符，跳转该页面需要附带配对信息，将pairings赋值即可
    private HashMap<Integer, NavigationNode> pairings;
    // 当前到达的导航点
    private NavigationNode currentNode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_receipt);

        initViews();
        initData();
        updateUI();

        setupListeners();
        startDepartureTimer();
    }

    private void initViews() {
        tvPointName = findViewById(R.id.tv_point_name);
        tvLayersList = findViewById(R.id.tv_layers_list);
        tvCountdown = findViewById(R.id.tv_countdown);

        layerL1 = findViewById(R.id.layer_l1);
        layerL2 = findViewById(R.id.layer_l2);
        layerL3 = findViewById(R.id.layer_l3);

        btnOpenCabin = findViewById(R.id.btn_open_cabin);
        tvArrivalDuration = findViewById(R.id.tv_arrival_duration);
    }

    private void initData() {
        // 获取舱门服务
        doorService = ServiceManager.getInstance().getDoorService();

        // 获取传递的数据
        try {
            pairings = (HashMap<Integer, NavigationNode>) getIntent().getSerializableExtra("pairings");
            currentNode = (NavigationNode) getIntent().getSerializableExtra("current_node");
        } catch (Exception e) {
            Log.e(TAG, "获取Intent数据失败", e);
        }

        if (pairings == null) {
            pairings = new HashMap<>();
            Log.w(TAG, "pairings为空");
        }

        if (currentNode == null) {
            Log.w(TAG, "currentNode为空，使用测试数据");
            // 仅用于测试/预览，实际应由上个页面传入
            // currentNode = new NavigationNode();
            // currentNode.setName("测试点位");
            // currentNode.setId(1);
        }
    }

    private void updateUI() {
        if (currentNode != null) {
            tvPointName.setText(currentNode.getName());
        } else {
            tvPointName.setText("未知点位");
        }

        List<Integer> targetLayers = new ArrayList<>();

        // 遍历配对关系，找出所有对应当前点位的层级
        if (pairings != null && currentNode != null) {
            for (Map.Entry<Integer, NavigationNode> entry : pairings.entrySet()) {
                NavigationNode node = entry.getValue();
                // 比较ID或名称，这里假设ID唯一
                if (node != null && node.getId() == currentNode.getId()) {
                    // 将资源ID转换为层号 (1, 2, 3)
                    int layerNum = getLayerNumber(entry.getKey());
                    if (layerNum > 0) {
                        targetLayers.add(layerNum);
                    }
                }
            }
        }

        // 排序层级
        Collections.sort(targetLayers);

        // 构建显示的层级字符串
        StringBuilder layersText = new StringBuilder();
        for (Integer layer : targetLayers) {
            layersText.append("L").append(layer).append(" ");

            // 高亮显示的层级
            highlightLayer(layer);
        }

        if (targetLayers.isEmpty()) {
            tvLayersList.setText("");
        } else {
            tvLayersList.setText(layersText.toString().trim());
        }
    }

    /**
     * 将按钮ID转换为层号
     */
    private int getLayerNumber(int id) {
        if (id == R.id.l1_button)
            return 1;
        if (id == R.id.l2_button)
            return 2;
        if (id == R.id.l3_button)
            return 3;
        // 兼容处理：如果已经是层号
        if (id == 1 || id == 2 || id == 3)
            return id;
        return 0;
    }

    /**
     * 高亮指定层级
     */
    private void highlightLayer(int layer) {
        // 将对应的层级背景设置为高亮色（蓝色圆角），文字设为白色
        int whiteColor = ContextCompat.getColor(this, android.R.color.white);

        switch (layer) {
            case 1:
                layerL1.setBackgroundResource(R.drawable.blue_rounded_button);
                layerL1.setTextColor(whiteColor);
                break;
            case 2:
                layerL2.setBackgroundResource(R.drawable.blue_rounded_button);
                layerL2.setTextColor(whiteColor);
                break;
            case 3:
                layerL3.setBackgroundResource(R.drawable.blue_rounded_button);
                layerL3.setTextColor(whiteColor);
                break;
        }
    }

    private void setupListeners() {
        btnOpenCabin.setOnClickListener(v -> {
            if (doorService == null) {
                Toast.makeText(this, "舱门服务未连接", Toast.LENGTH_SHORT).show();
                return;
            }

            // 禁用按钮防止重复点击
            btnOpenCabin.setEnabled(false);

            if (!isConfirmState) {
                // 1. 当前为"打开舱门"操作，需先验证密码
                Toast.makeText(this, "请先验证密码", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, PasswordActivity.class);
                startActivityForResult(intent, REQUEST_CODE_VERIFY_PASSWORD);
            } else {
                // 2. 当前为"确认收货"操作（关闭舱门）
                // 用户确认收货，立刻取消自动离场倒计时
                if (departureTimer != null) {
                    departureTimer.cancel();
                    tvCountdown.setVisibility(android.view.View.INVISIBLE);
                }

                attemptCloseAndLeave(true);
            }
        });
    }

    /**
     * 启动离场倒计时 (30秒)
     */
    private void startDepartureTimer() {
        if (departureTimer != null) {
            departureTimer.cancel();
        }

        departureTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isFinishing()) {
                    tvCountdown.setText((millisUntilFinished / 1000) + "s 后自动离开");
                }
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "【倒计时】30秒结束，自动离场");
                processAutoDeparture();
            }
        }.start();
    }

    /**
     * 处理超时自动离场
     */
    private void processAutoDeparture() {
        if (isFinishing())
            return;

        runOnUiThread(() -> {
            btnOpenCabin.setEnabled(false);
            tvCountdown.setText("正在离开...");

            // 如果门没开就自动离场，记录为超时失败
            if (!isConfirmState) {
                String pointName = currentNode != null ? currentNode.getName() : "未知点位";
                ItemDeliveryManager.getInstance().recordPointArrival(pointName,
                        ItemDeliveryRecord.STATUS_FAILED_TIMEOUT);
            }
        });

        if (doorService != null) {
            // 先检查状态，如果已关闭直接走，否则尝试关闭
            doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean allClosed) {
                    if (allClosed) {
                        finishWithSuccess();
                    } else {
                        runOnUiThread(() -> Toast.makeText(ConfirmReceiptActivity.this, "超时自动关门...", Toast.LENGTH_SHORT)
                                .show());
                        attemptCloseAndLeave(false);
                    }
                }

                @Override
                public void onError(ApiError error) {
                    // 查询失败，也尝试关闭保底
                    attemptCloseAndLeave(false);
                }
            });
        } else {
            finishWithSuccess();
        }
    }

    /**
     * 尝试关闭舱门并离开
     * 
     * @param isManual 是否是手动触发
     */
    private void attemptCloseAndLeave(boolean isManual) {
        runOnUiThread(() -> Toast.makeText(this, "正在关闭舱门...", Toast.LENGTH_SHORT).show());

        doorService.closeAllDoors(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    // 指令下发成功，开始轮询检查舱门是否真正关闭
                    // 只有真正关闭后才允许离开（防止移动时门还开着）
                    checkDoorClosedRecursive(10); // 10次重试，每次1秒
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> {
                    Toast.makeText(ConfirmReceiptActivity.this, "关门指令失败: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // 如果是手动，允许重试；如果是自动，可能需要人工干预
                    btnOpenCabin.setEnabled(true);
                    if (!isManual) {
                        tvCountdown.setText("关门异常");
                    }
                });
            }
        });
    }

    /**
     * 递归检查舱门是否关闭
     */
    private void checkDoorClosedRecursive(int remainingRetries) {
        if (isFinishing() || doorService == null)
            return;

        if (remainingRetries <= 0) {
            runOnUiThread(() -> {
                Toast.makeText(ConfirmReceiptActivity.this, "关门检测超时，请检查舱门状态", Toast.LENGTH_LONG).show();
                btnOpenCabin.setEnabled(true);
                tvCountdown.setText("关门超时");
            });
            return;
        }

        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                runOnUiThread(() -> {
                    if (allClosed) {
                        Toast.makeText(ConfirmReceiptActivity.this, "舱门已关闭，5秒后离场...", Toast.LENGTH_SHORT).show();
                        tvCountdown.setVisibility(android.view.View.VISIBLE);
                        tvCountdown.setText("准备离场...");

                        // 增加5秒延迟，确保门完全关闭后再移动
                        new android.os.Handler().postDelayed(() -> {
                            finishWithSuccess();
                        }, 5000);
                    } else {
                        // 未关闭，等待1秒后重试
                        new android.os.Handler().postDelayed(() -> {
                            checkDoorClosedRecursive(remainingRetries - 1);
                        }, 1000);
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                // 查询出错也重试
                runOnUiThread(() -> {
                    new android.os.Handler().postDelayed(() -> {
                        checkDoorClosedRecursive(remainingRetries - 1);
                    }, 1000);
                });
            }
        });
    }

    private void finishWithSuccess() {
        runOnUiThread(() -> {
            setResult(RESULT_OK);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        if (durationDialog != null && durationDialog.isShowing()) {
            durationDialog.dismiss();
        }
        if (departureTimer != null) {
            departureTimer.cancel();
            departureTimer = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_VERIFY_PASSWORD && resultCode == RESULT_OK) {
            // 密码验证通过，执行开门
            performOpenCabin();
        } else if (requestCode == REQUEST_CODE_VERIFY_PASSWORD) {
            // 验证未通过或取消，恢复按钮状态
            btnOpenCabin.setEnabled(true);
        }
    }

    /**
     * 执行开门操作
     */
    private void performOpenCabin() {
        Toast.makeText(this, "正在打开舱门...", Toast.LENGTH_SHORT).show();
        doorService.openAllDoors(false, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    Toast.makeText(ConfirmReceiptActivity.this, "舱门已打开", Toast.LENGTH_SHORT).show();
                    // 切换按钮状态和文本
                    btnOpenCabin.setText("确认收货");
                    isConfirmState = true;

                    // 记录到达时间 (成功)
                    String pointName = currentNode != null ? currentNode.getName() : "位置点位";
                    ItemDeliveryRecord record = ItemDeliveryManager.getInstance().recordPointArrival(pointName,
                            ItemDeliveryRecord.STATUS_SUCCESS);

                    if (record != null) {
                        tvArrivalDuration.setText("到达耗时: " + record.getFormattedDuration());
                        tvArrivalDuration.setVisibility(View.VISIBLE);
                        showDurationDialog(record.getFormattedDuration());
                    }

                    // 防止连击，延迟3秒启用确认收货按钮
                    new android.os.Handler().postDelayed(() -> {
                        if (!isFinishing()) {
                            btnOpenCabin.setEnabled(true);
                        }
                    }, 3000);
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> {
                    Toast.makeText(ConfirmReceiptActivity.this, "开门失败: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnOpenCabin.setEnabled(true);

                    // 记录开门硬件失败
                    String pointName = currentNode != null ? currentNode.getName() : "位置点位";
                    ItemDeliveryManager.getInstance().recordPointArrival(pointName,
                            ItemDeliveryRecord.STATUS_FAILED_HARDWARE);
                });
            }
        });
    }

    private void showDurationDialog(String duration) {
        if (isFinishing())
            return;

        // 如果已经有显示的对话框，先关闭
        if (durationDialog != null && durationDialog.isShowing()) {
            durationDialog.dismiss();
        }

        durationDialog = new AlertDialog.Builder(this)
                .setTitle("配送记录")
                .setMessage("本次配送到达时长: " + duration)
                .setPositiveButton("确定", null)
                .setCancelable(false)
                .show();
    }
}
