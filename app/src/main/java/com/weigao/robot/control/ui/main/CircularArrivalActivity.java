package com.weigao.robot.control.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IDoorCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.DoorType;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.ServiceManager;
// 循环配送的到达点位暂停页面
public class CircularArrivalActivity extends AppCompatActivity {
    private static final String TAG = "CircularArrivalActivity";

    public static final int RESULT_CONTINUE = 101;
    public static final int RESULT_RETURN_ORIGIN = 102;
    public static final int RESULT_CANCEL = 100;

    private IDoorService doorService;
    private Button btnOpenDoor;

    private static final long COUNTDOWN_TIME_MS = 30000; // 30 seconds
    private TextView tvArrivalMessage;
    private android.os.CountDownTimer countDownTimer;
    private boolean isLastPoint = false;
    private Button btnContinue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circular_arrival);

        doorService = ServiceManager.getInstance().getDoorService();
        if (doorService != null) {
            doorService.registerCallback(doorCallback);
        }

        isLastPoint = getIntent().getBooleanExtra("is_last_point", false);

        initViews();
        updateDoorButtonState();
        startCountdown();
    }

    private void initViews() {
        btnOpenDoor = findViewById(R.id.btn_open_door);
        Button btnReturnHome = findViewById(R.id.btn_return_home);
        btnContinue = findViewById(R.id.btn_continue);
        Button btnFullReturn = findViewById(R.id.btn_full_return);
        tvArrivalMessage = findViewById(R.id.tv_arrival_message);

        btnOpenDoor.setOnClickListener(v -> toggleDoor());

        btnReturnHome.setOnClickListener(v -> {
            setResult(RESULT_RETURN_ORIGIN);
            finish();
        });

        if (isLastPoint) {
            btnContinue.setEnabled(false);
            btnContinue.setAlpha(0.5f);
            btnContinue.setText("已是终点");
        }
        
        btnContinue.setOnClickListener(v -> closeDoorAndFinish(RESULT_CONTINUE));

        btnFullReturn.setOnClickListener(v -> closeDoorAndFinish(RESULT_RETURN_ORIGIN));
    }

    private void startCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new android.os.CountDownTimer(COUNTDOWN_TIME_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateMessage(millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                handleTimeout();
            }
        };
        countDownTimer.start();
    }

    private void updateMessage(long secondsLeft) {
        String baseMsg = "请尽快把货物放入机器人中";
        String extra = isLastPoint ? "\n(无人操作将自动返航: " + secondsLeft + "s)" : "\n(无人操作将继续下一站: " + secondsLeft + "s)";
        tvArrivalMessage.setText(baseMsg + extra);
    }

    private void handleTimeout() {
        if (isLastPoint) {
            // Auto return
            Toast.makeText(this, "倒计时结束，自动返航", Toast.LENGTH_SHORT).show();
            // closeDoorAndFinish(RESULT_RETURN_ORIGIN); // Or just finish with result?
            // The prompt said: "倒计时结束相当按下已装满返航到原点"
             closeDoorAndFinish(RESULT_RETURN_ORIGIN);
        } else {
            // Auto continue
            Toast.makeText(this, "倒计时结束，前往下一站", Toast.LENGTH_SHORT).show();
            closeDoorAndFinish(RESULT_CONTINUE);
        }
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            // Reset timer on user interaction
            startCountdown();
        }
        return super.dispatchTouchEvent(ev);
    }

    private void toggleDoor() {
        if (doorService == null) return;
        btnOpenDoor.setEnabled(false);
        // Reset timer as this is an action
        startCountdown();
        
        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                runOnUiThread(() -> {
                    if (allClosed) {
                        doorService.openAllDoors(false, new IResultCallback<Void>() {
                            @Override public void onSuccess(Void result) {
                                runOnUiThread(() -> {
                                    Toast.makeText(CircularArrivalActivity.this, "舱门已打开", Toast.LENGTH_SHORT).show();
                                    updateDoorButtonState();
                                });
                            }
                            @Override public void onError(ApiError error) {
                                handleDoorError("开门失败", error);
                            }
                        });
                    } else {
                        doorService.closeAllDoors(new IResultCallback<Void>() {
                            @Override public void onSuccess(Void result) {
                                runOnUiThread(() -> {
                                    Toast.makeText(CircularArrivalActivity.this, "舱门已关闭", Toast.LENGTH_SHORT).show();
                                    updateDoorButtonState();
                                });
                            }
                            @Override public void onError(ApiError error) {
                                handleDoorError("关门失败", error);
                            }
                        });
                    }
                });
            }
            @Override
            public void onError(ApiError error) {
                handleDoorError("状态查询失败", error);
            }
        });
    }

    private void handleDoorError(String msg, ApiError error) {
        runOnUiThread(() -> {
            Toast.makeText(this, msg + ": " + error.getMessage(), Toast.LENGTH_SHORT).show();
            btnOpenDoor.setEnabled(true);
        });
    }

    private void updateDoorButtonState() {
        if (doorService == null) return;
        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                runOnUiThread(() -> {
                    btnOpenDoor.setText(allClosed ? "开门" : "关门");
                    new Handler().postDelayed(() -> btnOpenDoor.setEnabled(true), 1000);
                });
            }
            @Override
            public void onError(ApiError error) {
                runOnUiThread(() -> btnOpenDoor.setEnabled(true));
            }
        });
    }

    private void closeDoorAndFinish(int resultCode) {
        if (countDownTimer != null) countDownTimer.cancel();
        
        if (doorService != null) {
            // Attempt to close door automatically before continuing
            Toast.makeText(this, "正在关闭舱门...", Toast.LENGTH_SHORT).show();
            doorService.closeAllDoors(new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    runOnUiThread(() -> {
                        setResult(resultCode);
                        finish();
                    });
                }

                @Override
                public void onError(ApiError error) {
                    runOnUiThread(() -> {
                        Toast.makeText(CircularArrivalActivity.this, "关门失败，请手动关闭", Toast.LENGTH_SHORT).show();
                        // For safety, finish anyway? Or allow user to retry?
                        // If logic requires closing doors for safety, we might get stuck here.
                        // But let's proceed to finish for flow continuity as requested.
                         setResult(resultCode);
                         finish();
                    });
                }
            });
        } else {
            setResult(resultCode);
            finish();
        }
    }

    private final IDoorCallback doorCallback = new IDoorCallback() {
        @Override
        public void onDoorStateChanged(int doorId, int state) {
            runOnUiThread(() -> {
                updateDoorButtonState();
                // Door state change is also an interaction
                startCountdown();
            });
        }
        @Override public void onDoorTypeChanged(DoorType type) {}
        @Override public void onDoorTypeSettingResult(boolean success) {}
        @Override public void onDoorError(int doorId, int errorCode) {}
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (doorService != null) {
            doorService.unregisterCallback(doorCallback);
        }
    }
}
