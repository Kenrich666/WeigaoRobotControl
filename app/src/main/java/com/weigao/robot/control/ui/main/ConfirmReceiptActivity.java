package com.weigao.robot.control.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.weigao.robot.control.R;
import com.weigao.robot.control.model.NavigationNode;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.ServiceManager;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 到达点位确认收货页面
 * 显示到达的点位名称，并指示需要取走的层级（L1, L2, L3）
 */
public class ConfirmReceiptActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmReceiptActivity";

    private TextView tvPointName;
    private TextView tvLayersList;
    private TextView layerL1, layerL2, layerL3;
    private Button btnOpenCabin;
    
    // 舱门服务
    private IDoorService doorService;
    // 确认关闭状态标记
    private boolean isConfirmState = false;

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
    }

    private void initViews() {
        tvPointName = findViewById(R.id.tv_point_name);
        tvLayersList = findViewById(R.id.tv_layers_list);
        
        layerL1 = findViewById(R.id.layer_l1);
        layerL2 = findViewById(R.id.layer_l2);
        layerL3 = findViewById(R.id.layer_l3);

        btnOpenCabin = findViewById(R.id.btn_open_cabin);
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
                    targetLayers.add(entry.getKey());
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
     * 高亮指定层级
     */
    private void highlightLayer(int layer) {
        // 将对应的层级背景设置为高亮色（蓝色圆角），文字设为白色
        switch (layer) {
            case 1:
                layerL1.setBackgroundResource(R.drawable.blue_rounded_button);
                layerL1.setTextColor(getResources().getColor(android.R.color.white, null));
                break;
            case 2:
                layerL2.setBackgroundResource(R.drawable.blue_rounded_button);
                layerL2.setTextColor(getResources().getColor(android.R.color.white, null));
                break;
            case 3:
                layerL3.setBackgroundResource(R.drawable.blue_rounded_button);
                layerL3.setTextColor(getResources().getColor(android.R.color.white, null));
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
                // 1. 当前为"打开舱门"操作
                Toast.makeText(this, "正在打开舱门...", Toast.LENGTH_SHORT).show();
                doorService.openAllDoors(false, new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            Toast.makeText(ConfirmReceiptActivity.this, "舱门已打开", Toast.LENGTH_SHORT).show();
                            // 切换按钮状态和文本
                            btnOpenCabin.setText("确认收货");
                            btnOpenCabin.setEnabled(true);
                            isConfirmState = true;
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        runOnUiThread(() -> {
                            Toast.makeText(ConfirmReceiptActivity.this, "开门失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            btnOpenCabin.setEnabled(true);
                        });
                    }
                });
            } else {
                // 2. 当前为"确认收货"操作（关闭舱门）
                Toast.makeText(this, "正在关闭舱门...", Toast.LENGTH_SHORT).show();
                doorService.closeAllDoors(new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            Toast.makeText(ConfirmReceiptActivity.this, "舱门已关闭", Toast.LENGTH_SHORT).show();
                            
                            // 跳转到配送导航页面
                            Intent intent = new Intent(ConfirmReceiptActivity.this, DeliveryNavigationActivity.class);
                            if (pairings != null) {
                                intent.putExtra("pairings", pairings);
                            }
                            // 在跳转时添加了 FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP 标志，确保返回导航页面时不重复创建 Activity 实例，维持导航状态。
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        runOnUiThread(() -> {
                            Toast.makeText(ConfirmReceiptActivity.this, "关门失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            btnOpenCabin.setEnabled(true);
                        });
                    }
                });
            }
        });
    }
}
