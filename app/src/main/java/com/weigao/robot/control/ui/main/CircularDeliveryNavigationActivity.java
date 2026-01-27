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
import com.weigao.robot.control.manager.DeliveryHistoryManager;
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

    private GestureDetector gestureDetector;
    private INavigationService navigationService;
    
    private String routeName;
    private int loopCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circular_delivery_navigation); // Use separate layout

        initViews();

        navigationService = ServiceManager.getInstance().getNavigationService();
        if (navigationService == null) {
            Toast.makeText(this, "导航服务未初始化", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        navigationService.registerCallback(this);

        // Get Data
        ArrayList<NavigationNode> rawNodes = (ArrayList<NavigationNode>) getIntent().getSerializableExtra("route_nodes");
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
                int speed = com.weigao.robot.control.manager.CircularDeliverySettingsManager.getInstance().getDeliverySpeed();
                navigationService.setSpeed(speed, new IResultCallback<Void>() {
                     @Override public void onSuccess(Void result) {
                         navigationService.prepare(new IResultCallback<Void>() {
                             @Override public void onSuccess(Void result) {} // Will accept onRoutePrepared
                             @Override public void onError(ApiError error) { handleError("准备路线失败", error); }
                         });
                     }
                     @Override public void onError(ApiError error) { handleError("设置速度失败", error); }
                });
            }
            @Override
            public void onError(ApiError error) { handleError("设置目标点失败", error); }
        });
    }

    private void handleError(String msg, ApiError error) {
        runOnUiThread(() -> Toast.makeText(this, msg + ": " + error.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void pauseNavigation() {
        navigationService.pause(new IResultCallback<Void>() {
            @Override public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    isPaused = true;
                    tvStatus.setText("已暂停");
                    llPauseControls.setVisibility(View.VISIBLE);
                    btnReturnOrigin.setVisibility(View.VISIBLE);
                    tvHint.setVisibility(View.INVISIBLE);
                });
            }
            @Override public void onError(ApiError error) {}
        });
    }

    private void resumeNavigation() {
        navigationService.start(new IResultCallback<Void>() {
             @Override public void onSuccess(Void result) {
                 runOnUiThread(() -> {
                     isPaused = false;
                      tvStatus.setText("导航中");
                      llPauseControls.setVisibility(View.GONE);
                      btnReturnOrigin.setVisibility(View.GONE);
                      tvHint.setVisibility(View.VISIBLE);
                 });
             }
             @Override public void onError(ApiError error) {}
        });
    }

    private void proceedToNextNode() {
        // Pilot Next
        navigationService.pilotNext(new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    isWaitingAtNode = false;
                    tvStatus.setText("导航中");
                    llPauseControls.setVisibility(View.GONE);
                    tvHint.setVisibility(View.VISIBLE);
                });
            }
            @Override
            public void onError(ApiError error) {
                // If it fails (e.g. no more nodes), we might be done
                 runOnUiThread(() -> Toast.makeText(CircularDeliveryNavigationActivity.this, "无法前往下一站: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void stopNavigation() {
        navigationService.stop(null);
        isNavigating = false;
        if (currentRecord != null && currentRecord.getDurationSeconds() == 0) { // Not completed yet
             currentRecord.complete("CANCELLED");
             DeliveryHistoryManager.getInstance(this).addRecord(currentRecord);
             currentRecord = null;
        }
    }

    private void updateTaskText() {
        runOnUiThread(() -> {
            if (currentTaskIndex < targetNodes.size()) {
                NavigationNode node = targetNodes.get(currentTaskIndex);
                
                int totalPoints = targetNodes.size();
                int nodesPerLoop = totalPoints / (loopCount > 0 ? loopCount : 1);
                if (nodesPerLoop == 0) nodesPerLoop = 1;
                
                // 1-based index calculation
                int currentLoop = (currentTaskIndex / nodesPerLoop) + 1;
                int nodeIndexInLoop = (currentTaskIndex % nodesPerLoop) + 1;
                
                tvLoopCount.setText(String.format("第 %d / %d 轮", currentLoop, loopCount));
                
                // Progress Bar
                if (pbProgress != null) {
                    pbProgress.setProgress(currentTaskIndex + 1);
                }

                currentTaskTextView.setText( String.format("正在前往: %s (第 %d/%d 个点位)", 
                    node.getName(), nodeIndexInLoop, nodesPerLoop));
            } else {
                currentTaskTextView.setText("导航结束");
                if (pbProgress != null) pbProgress.setProgress(targetNodes.size());
            }
        });
    }


    // INavigationCallback Implementation
    @Override
    public void onStateChanged(int state, int schedule) {
        runOnUiThread(() -> {
            Log.d(TAG, "onStateChanged: state=" + state + ", schedule=" + schedule);
            switch (state) {
                case com.keenon.sdk.component.navigation.common.Navigation.STATE_DESTINATION:
                    // Arrived
                    handleArrival();
                    break;
                case 6: // STATE_BLOCKED
                    tvStatus.setText("被阻挡");
                    tvHint.setText("路径规划失败或被阻挡，请检查障碍物");
                    tvHint.setTextColor(android.graphics.Color.RED);
                    llPauseControls.setVisibility(View.VISIBLE);
                    break;
                case 2: // STATE_RUNNING
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
    
    // ...

    private CircularDeliveryRecord currentRecord;

    private boolean isReturning = false;

    private void handleArrival() {
        if (isReturning) {
            Toast.makeText(this, "已返回出餐口，任务结束", Toast.LENGTH_LONG).show();
            if (currentRecord != null) {
                currentRecord.complete("COMPLETED");
                DeliveryHistoryManager.getInstance(this).addRecord(currentRecord);
            }
            finish();
            return;
        }

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
                // Jump to ReturnActivity as requested
                Toast.makeText(this, "任务结束，开始返航", Toast.LENGTH_SHORT).show();
                // We should stop current navigation first? 
                // ReturnActivity will take over. 
                // But we should probably finish this activity.
                Intent returnIntent = new Intent(this, ReturnActivity.class);
                startActivity(returnIntent);
                finish();
            } else {
                // Cancelled or just back
            }
        }
    }

    private void returnToOrigin() {
        // Use ReturnActivity for consistent return logic
        Toast.makeText(this, "循环结束，开始返航", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, ReturnActivity.class);
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
             @Override public void onSuccess(Void result) {
                 runOnUiThread(() -> {
                     tvStatus.setText("导航中");
                     Toast.makeText(CircularDeliveryNavigationActivity.this, "开始循环配送", Toast.LENGTH_SHORT).show();
                 });
             }
             @Override public void onError(ApiError error) {}
         });
    }

    @Override public void onDistanceChanged(double distance) {}
    @Override public void onNavigationError(int errorCode) {
        runOnUiThread(() -> Toast.makeText(this, "导航错误: " + errorCode, Toast.LENGTH_SHORT).show());
    }
    @Override public void onError(int errorCode, String message) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (navigationService != null) {
            stopNavigation();
            navigationService.unregisterCallback(this);
        }
    }
}
