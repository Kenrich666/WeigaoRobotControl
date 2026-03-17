package com.weigao.robot.control.ui.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.weigao.robot.control.R;
import com.weigao.robot.control.app.WeigaoApplication;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.manager.HospitalDeliveryManager;
import com.weigao.robot.control.manager.ItemDeliveryManager;
import com.weigao.robot.control.model.HospitalDeliveryTask;
import com.weigao.robot.control.model.ItemDeliveryRecord;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.ServiceManager;
import com.weigao.robot.control.ui.auth.PasswordActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfirmReceiptActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmReceiptActivity";
    private static final int REQUEST_CODE_VERIFY_PASSWORD = 2001;

    private TextView tvPointName;
    private TextView tvLayersList;
    private TextView tvCountdown;
    private TextView tvArrivalDuration;
    private TextView tvItemsList;
    private TextView layerL1;
    private TextView layerL2;
    private TextView layerL3;
    private Button btnOpenCabin;

    private IDoorService doorService;
    private boolean isConfirmState = false;
    private CountDownTimer departureTimer;
    private HashMap<Integer, NavigationNode> pairings;
    private ArrayList<HospitalDeliveryTask> hospitalTasks;
    private NavigationNode currentNode;
    private String recordMode = "item";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        recordMode = getIntent().getStringExtra("record_mode");
        if (recordMode == null || recordMode.isEmpty()) {
            recordMode = "item";
        }
        super.onCreate(savedInstanceState);
        setContentView("hospital".equals(recordMode)
                ? R.layout.activity_confirm_receipt_hospital
                : R.layout.activity_confirm_receipt);

        initViews();
        initData();
        updateUI();
        setupListeners();
        startDepartureTimer();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                doorBroadcastReceiver,
                new IntentFilter("com.weigao.robot.DOOR_STATE_CHANGED"));
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
        tvItemsList = findViewById(R.id.tv_items_list);
    }

    @SuppressWarnings("unchecked")
    private void initData() {
        doorService = ServiceManager.getInstance().getDoorService();
        try {
            pairings = (HashMap<Integer, NavigationNode>) getIntent().getSerializableExtra("pairings");
            hospitalTasks = (ArrayList<HospitalDeliveryTask>) getIntent().getSerializableExtra("hospital_tasks");
            currentNode = (NavigationNode) getIntent().getSerializableExtra("current_node");
        } catch (Exception e) {
            Log.e(TAG, "获取Intent数据失败", e);
        }
        if (pairings == null) {
            pairings = new HashMap<>();
        }
        if (hospitalTasks == null) {
            hospitalTasks = new ArrayList<>();
        }
    }

    private void updateUI() {
        if (currentNode != null) {
            tvPointName.setText(currentNode.getName());
        } else {
            tvPointName.setText("未知点位");
        }

        resetLayerHighlights();
        List<Integer> targetLayers = new ArrayList<>();
        List<String> itemLines = new ArrayList<>();

        if ("hospital".equals(recordMode)) {
            for (HospitalDeliveryTask task : hospitalTasks) {
                NavigationNode roomNode = task.getRoomNode();
                if (roomNode == null || currentNode == null || roomNode.getId() != currentNode.getId()) {
                    continue;
                }
                if (task.hasAssignedLayer()) {
                    targetLayers.add(task.getAssignedLayer());
                    highlightLayer(task.getAssignedLayer());
                    itemLines.add(task.getAssignedLayerLabel() + " - " + task.getItemName());
                } else {
                    itemLines.add("未分配 - " + task.getItemName());
                }
            }
        } else if (pairings != null && currentNode != null) {
            for (Map.Entry<Integer, NavigationNode> entry : pairings.entrySet()) {
                NavigationNode node = entry.getValue();
                if (node != null && node.getId() == currentNode.getId()) {
                    int layerNum = getLayerNumber(entry.getKey());
                    if (layerNum > 0) {
                        targetLayers.add(layerNum);
                        highlightLayer(layerNum);
                    }
                }
            }
        }

        Collections.sort(targetLayers);
        StringBuilder layersText = new StringBuilder();
        for (Integer layer : targetLayers) {
            if (layersText.length() > 0) {
                layersText.append(" ");
            }
            layersText.append("L").append(layer);
        }
        tvLayersList.setText(layersText.length() == 0 ? "未分配层位" : layersText.toString());

        if (tvItemsList != null) {
            if (itemLines.isEmpty()) {
                tvItemsList.setText("请打开舱门后取物。");
            } else {
                tvItemsList.setText(android.text.TextUtils.join("\n", itemLines));
            }
        }
    }

    private void resetLayerHighlights() {
        int textColor = ContextCompat.getColor(this, R.color.medical_text_primary);
        layerL1.setBackgroundResource(R.drawable.item_point_style);
        layerL2.setBackgroundResource(R.drawable.item_point_style);
        layerL3.setBackgroundResource(R.drawable.item_point_style);
        layerL1.setTextColor(textColor);
        layerL2.setTextColor(textColor);
        layerL3.setTextColor(textColor);
    }

    private int getLayerNumber(int id) {
        if (id == R.id.l1_button) return 1;
        if (id == R.id.l2_button) return 2;
        if (id == R.id.l3_button) return 3;
        if (id == 1 || id == 2 || id == 3) return id;
        return 0;
    }

    private void highlightLayer(int layer) {
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
            default:
                break;
        }
    }

    private void setupListeners() {
        btnOpenCabin.setOnClickListener(v -> {
            if (doorService == null) {
                Toast.makeText(this, "舱门服务未连接", Toast.LENGTH_SHORT).show();
                return;
            }

            btnOpenCabin.setEnabled(false);
            if (!isConfirmState) {
                Toast.makeText(this, "请先验证密码", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, PasswordActivity.class);
                startActivityForResult(intent, REQUEST_CODE_VERIFY_PASSWORD);
            } else {
                if (departureTimer != null) {
                    departureTimer.cancel();
                    tvCountdown.setVisibility(View.INVISIBLE);
                }
                attemptCloseAndLeave(true);
            }
        });
    }

    private void startDepartureTimer() {
        if (departureTimer != null) {
            departureTimer.cancel();
        }

        int stayDurationSeconds;
        if ("hospital".equals(recordMode)) {
            stayDurationSeconds = com.weigao.robot.control.manager.HospitalDeliverySettingsManager.getInstance()
                    .getArrivalStayDuration();
        } else {
            stayDurationSeconds = com.weigao.robot.control.manager.ItemDeliverySettingsManager.getInstance()
                    .getArrivalStayDuration();
        }
        long stayDurationMs = stayDurationSeconds * 1000L;

        departureTimer = new CountDownTimer(stayDurationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isFinishing()) {
                    tvCountdown.setText((millisUntilFinished / 1000) + "s 后自动离开");
                }
            }

            @Override
            public void onFinish() {
                processAutoDeparture();
            }
        }.start();
    }

    private void processAutoDeparture() {
        if (isFinishing()) return;
        finishActivity(REQUEST_CODE_VERIFY_PASSWORD);

        runOnUiThread(() -> {
            btnOpenCabin.setEnabled(false);
            tvCountdown.setText("正在离开...");
            if (!isConfirmState) {
                String pointName = currentNode != null ? currentNode.getName() : "未知点位";
                recordArrival(pointName, ItemDeliveryRecord.STATUS_FAILED_TIMEOUT);
            }
        });

        if (doorService != null) {
            doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean allClosed) {
                    if (allClosed) {
                        finishWithSuccess();
                    } else {
                        runOnUiThread(() -> Toast.makeText(ConfirmReceiptActivity.this, "超时自动关门...", Toast.LENGTH_SHORT).show());
                        attemptCloseAndLeave(false);
                    }
                }

                @Override
                public void onError(ApiError error) {
                    attemptCloseAndLeave(false);
                }
            });
        } else {
            finishWithSuccess();
        }
    }

    private void attemptCloseAndLeave(boolean isManual) {
        runOnUiThread(() -> Toast.makeText(this, "正在关闭舱门...", Toast.LENGTH_SHORT).show());

        doorService.closeAllDoors(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> checkDoorClosedRecursive(10));
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> {
                    Toast.makeText(ConfirmReceiptActivity.this, "关门指令失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    if (isManual) {
                        btnOpenCabin.setEnabled(true);
                    } else {
                        btnOpenCabin.setEnabled(false);
                        tvCountdown.setText("关门异常，5s 后离场...");
                        new android.os.Handler().postDelayed(() -> {
                            if (!isFinishing()) {
                                finishWithSuccess();
                            }
                        }, 5000);
                    }
                });
            }
        });
    }

    private void checkDoorClosedRecursive(int remainingRetries) {
        if (isFinishing() || doorService == null) return;
        if (remainingRetries <= 0) {
            runOnUiThread(() -> {
                Toast.makeText(ConfirmReceiptActivity.this, "关门检测超时，5s 后自动离场", Toast.LENGTH_LONG).show();
                btnOpenCabin.setEnabled(false);
                tvCountdown.setText("检测超时，即将离场...");
                new android.os.Handler().postDelayed(() -> {
                    if (!isFinishing()) {
                        finishWithSuccess();
                    }
                }, 5000);
            });
            return;
        }

        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                runOnUiThread(() -> {
                    if (allClosed) {
                        Toast.makeText(ConfirmReceiptActivity.this, "舱门已关闭，5s 后离场...", Toast.LENGTH_SHORT).show();
                        tvCountdown.setVisibility(View.VISIBLE);
                        tvCountdown.setText("准备离场...");
                        new android.os.Handler().postDelayed(ConfirmReceiptActivity.this::finishWithSuccess, 5000);
                    } else {
                        new android.os.Handler().postDelayed(() -> checkDoorClosedRecursive(remainingRetries - 1), 1000);
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> new android.os.Handler().postDelayed(() -> checkDoorClosedRecursive(remainingRetries - 1), 1000));
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
        if (departureTimer != null) {
            departureTimer.cancel();
            departureTimer = null;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(doorBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (isFinishing()) {
            return;
        }
        if (requestCode == REQUEST_CODE_VERIFY_PASSWORD && resultCode == RESULT_OK) {
            performOpenCabin();
        } else if (requestCode == REQUEST_CODE_VERIFY_PASSWORD) {
            btnOpenCabin.setEnabled(true);
        }
    }

    private void performOpenCabin() {
        Toast.makeText(this, "正在打开舱门...", Toast.LENGTH_SHORT).show();
        doorService.openAllDoors(false, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    Toast.makeText(ConfirmReceiptActivity.this, "舱门已打开", Toast.LENGTH_SHORT).show();
                    btnOpenCabin.setText("确认收货");
                    isConfirmState = true;
                    String pointName = currentNode != null ? currentNode.getName() : "位置点位";
                    ItemDeliveryRecord record = recordArrival(pointName, ItemDeliveryRecord.STATUS_SUCCESS);
                    if (record != null) {
                        tvArrivalDuration.setText("到达耗时: " + record.getFormattedDuration());
                        tvArrivalDuration.setVisibility(View.VISIBLE);
                    }
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
                    Toast.makeText(ConfirmReceiptActivity.this, "开门失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    btnOpenCabin.setEnabled(true);
                    String pointName = currentNode != null ? currentNode.getName() : "位置点位";
                    recordArrival(pointName, ItemDeliveryRecord.STATUS_FAILED_HARDWARE);
                });
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            WeigaoApplication.applyFullScreen(this);
        }
    }

    private final BroadcastReceiver doorBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isClosing = intent.getBooleanExtra("is_closing", false);
            if (isClosing) {
                if (departureTimer != null) {
                    departureTimer.cancel();
                }
                if (!isConfirmState) {
                    String pointName = currentNode != null ? currentNode.getName() : "未知点位";
                    recordArrival(pointName, ItemDeliveryRecord.STATUS_SUCCESS);
                }
                runOnUiThread(() -> {
                    btnOpenCabin.setEnabled(false);
                    btnOpenCabin.setText("舱门已关闭");
                    tvCountdown.setText("舱门已关闭，即将离场...");
                    new android.os.Handler().postDelayed(() -> {
                        if (!isFinishing()) {
                            finishWithSuccess();
                        }
                    }, 3000);
                });
            } else {
                runOnUiThread(() -> {
                    if (!isConfirmState) {
                        btnOpenCabin.setText("确认收货");
                        isConfirmState = true;
                        String pointName = currentNode != null ? currentNode.getName() : "未知点位";
                        ItemDeliveryRecord record = recordArrival(pointName, ItemDeliveryRecord.STATUS_SUCCESS);
                        if (record != null) {
                            tvArrivalDuration.setText("到达耗时: " + record.getFormattedDuration());
                            tvArrivalDuration.setVisibility(View.VISIBLE);
                        }
                        new android.os.Handler().postDelayed(() -> {
                            if (!isFinishing()) {
                                btnOpenCabin.setEnabled(true);
                            }
                        }, 3000);
                    }
                });
            }
        }
    };

    private ItemDeliveryRecord recordArrival(String pointName, int status) {
        if ("hospital".equals(recordMode)) {
            HospitalDeliveryManager.getInstance().recordPointArrival(
                    pointName,
                    mapHospitalStatus(status),
                    com.weigao.robot.control.model.HospitalDeliveryRecord.STAGE_ROOM);
            return null;
        }
        return ItemDeliveryManager.getInstance().recordPointArrival(pointName, status);
    }

    private int mapHospitalStatus(int itemStatus) {
        switch (itemStatus) {
            case ItemDeliveryRecord.STATUS_SUCCESS:
                return com.weigao.robot.control.model.HospitalDeliveryRecord.STATUS_SUCCESS;
            case ItemDeliveryRecord.STATUS_FAILED_TIMEOUT:
                return com.weigao.robot.control.model.HospitalDeliveryRecord.STATUS_FAILED_TIMEOUT;
            case ItemDeliveryRecord.STATUS_FAILED_HARDWARE:
                return com.weigao.robot.control.model.HospitalDeliveryRecord.STATUS_FAILED_HARDWARE;
            case ItemDeliveryRecord.STATUS_NAV_FAILED:
                return com.weigao.robot.control.model.HospitalDeliveryRecord.STATUS_NAV_FAILED;
            case ItemDeliveryRecord.STATUS_CANCELLED:
            default:
                return com.weigao.robot.control.model.HospitalDeliveryRecord.STATUS_CANCELLED;
        }
    }
}
