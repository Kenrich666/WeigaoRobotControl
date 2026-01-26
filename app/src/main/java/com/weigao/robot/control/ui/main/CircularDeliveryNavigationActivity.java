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
import com.weigao.robot.control.model.DeliveryRecord;
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

    private TextView tvStatus, currentTaskTextView, tvHint;
    private Button btnPauseEnd, btnContinue;
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
        
        updateTaskText();
        startNavigation();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        currentTaskTextView = findViewById(R.id.current_task_textview);
        tvHint = findViewById(R.id.tv_hint);
        llPauseControls = findViewById(R.id.ll_pause_controls);
        btnPauseEnd = findViewById(R.id.btn_pause_end);
        btnContinue = findViewById(R.id.btn_continue);
        rootLayout = findViewById(R.id.root_layout);
        
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
        btnContinue.setText("继续前往下一站");
        btnContinue.setOnClickListener(v -> {
            if (isPaused) {
                resumeNavigation();
            } else if (isWaitingAtNode) {
                proceedToNextNode();
            }
        });
    }

    private void startNavigation() {
        isNavigating = true;
        // Start Record
        currentRecord = new DeliveryRecord(routeName, loopCount, System.currentTimeMillis());
        
        List<Integer> targetIds = new ArrayList<>();
        for (NavigationNode node : targetNodes) {
             // ...
             targetIds.add(node.getId());
        }
        
        navigationService.setTargets(targetIds, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                navigationService.setSpeed(30, new IResultCallback<Void>() {
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
                currentTaskTextView.setText( String.format("正在前往: %s (%d/%d)\n路线: %s", 
                    node.getName(), currentTaskIndex + 1, targetNodes.size(), routeName));
            } else {
                currentTaskTextView.setText("导航结束");
            }
        });
    }


    // INavigationCallback Implementation
    @Override
    public void onStateChanged(int state, int schedule) {
        runOnUiThread(() -> {
            if (state == com.keenon.sdk.component.navigation.common.Navigation.STATE_DESTINATION) {
                // Arrived
                handleArrival();
            }
        });
    }

    private static final int REQUEST_CODE_ARRIVAL = 1001;
    
    // ...

    private DeliveryRecord currentRecord;

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
        // NavigationNode currentNode = targetNodes.get(currentTaskIndex);
        
        Intent intent = new Intent(this, CircularArrivalActivity.class);
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
                // Abort and return origin
                Toast.makeText(this, "中止任务，返回出餐口", Toast.LENGTH_SHORT).show();
                returnToOrigin();
            } else {
                // Cancelled
            }
        }
    }

    private void returnToOrigin() {
        isReturning = true;
        List<NavigationNode> origins = DeliveryActivity.originPoints;
        if (origins != null && !origins.isEmpty()) {
            NavigationNode origin = origins.get(0);
            List<Integer> ids = new ArrayList<>();
            ids.add(origin.getId());
            
            navigationService.setTargets(ids, new IResultCallback<Void>() {
                @Override public void onSuccess(Void result) {
                    navigationService.prepare(new IResultCallback<Void>() {
                        @Override public void onSuccess(Void result) { } // onRoutePrepared will handle start
                        @Override public void onError(ApiError error) {
                            handleError("返航失败", error);
                        }
                    });
                }
                @Override public void onError(ApiError error) {
                    handleError("无法设定返航点", error);
                }
            });
        } else {
            Toast.makeText(this, "未找到原点，无法自动返航", Toast.LENGTH_SHORT).show();
            finish();
        }
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
