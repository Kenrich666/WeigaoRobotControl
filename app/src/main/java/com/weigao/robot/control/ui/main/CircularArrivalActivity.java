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
import com.weigao.robot.control.ui.auth.PasswordActivity;
import com.weigao.robot.control.app.WeigaoApplication;
// 循环配送的到达点位暂停页面
public class CircularArrivalActivity extends AppCompatActivity {
    private static final String TAG = "CircularArrivalActivity";

    public static final int RESULT_CONTINUE = 101;
    public static final int RESULT_RETURN_ORIGIN = 102;
    public static final int RESULT_CANCEL = 100;

    private static final int REQUEST_CODE_PWD_BACK = 201;
    private static final int REQUEST_CODE_PWD_OPEN_DOOR = 202;
    private static final int REQUEST_CODE_PWD_RETURN_HOME = 203;
    private static final int REQUEST_CODE_PWD_FULL_RETURN = 204;

    private IDoorService doorService;
    private Button btnOpenDoor;

    private static final long COUNTDOWN_TIME_MS = 30000; // 30 seconds
    private TextView tvArrivalMessage;
    private android.os.CountDownTimer countDownTimer;
    private boolean isLastPoint = false;
    private Button btnContinue;
    private TextView tvCurrentPoint;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circular_arrival);

        doorService = ServiceManager.getInstance().getDoorService();
        if (doorService != null) {
            doorService.registerCallback(doorCallback);
        }

        isLastPoint = getIntent().getBooleanExtra("is_last_point", false);
        String currentPointName = getIntent().getStringExtra("current_point_name");

        initViews();
        if (currentPointName != null) {
            tvCurrentPoint.setText("当前到达: " + currentPointName);
        }
        updateDoorButtonState();
        startCountdown();
    }

    private void initViews() {
        btnOpenDoor = findViewById(R.id.btn_open_door);
        tvCurrentPoint = findViewById(R.id.tv_current_point); // Added
        Button btnReturnHome = findViewById(R.id.btn_return_home);
        btnContinue = findViewById(R.id.btn_continue);
        Button btnFullReturn = findViewById(R.id.btn_full_return);
        tvArrivalMessage = findViewById(R.id.tv_arrival_message);
        Button btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(CircularArrivalActivity.this, PasswordActivity.class);
            startActivityForResult(intent, REQUEST_CODE_PWD_BACK);
        });

        btnOpenDoor.setOnClickListener(v -> toggleDoor());

        btnReturnHome.setOnClickListener(v -> {
            Intent intent = new Intent(CircularArrivalActivity.this, PasswordActivity.class);
            startActivityForResult(intent, REQUEST_CODE_PWD_RETURN_HOME);
        });

        if (isLastPoint) {
            btnContinue.setEnabled(false);
            btnContinue.setAlpha(0.5f);
            btnContinue.setText("已是终点");
        }
        
        btnContinue.setOnClickListener(v -> closeDoorAndFinish(RESULT_CONTINUE));

        btnFullReturn.setOnClickListener(v -> {
            Intent intent = new Intent(CircularArrivalActivity.this, PasswordActivity.class);
            startActivityForResult(intent, REQUEST_CODE_PWD_FULL_RETURN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // If the activity is finishing (e.g. timeout triggered), ignore the result
        if (isFinishing()) {
            return;
        }

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_PWD_BACK:
                    closeDoorAndFinish(RESULT_CANCEL);
                    break;
                case REQUEST_CODE_PWD_OPEN_DOOR:
                    performOpenDoors();
                    break;
                case REQUEST_CODE_PWD_RETURN_HOME:
                    setResult(RESULT_RETURN_ORIGIN);
                    finish();
                    break;
                case REQUEST_CODE_PWD_FULL_RETURN:
                    closeDoorAndFinish(RESULT_RETURN_ORIGIN);
                    break;
            }
        } else {
             if (requestCode == REQUEST_CODE_PWD_OPEN_DOOR) {
                 updateDoorButtonState();
             }
        }
    }

    private void startCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new android.os.CountDownTimer(COUNTDOWN_TIME_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isFinishing()) {
                    updateMessage(millisUntilFinished / 1000);
                }
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
        if (isFinishing()) return;

        // Force close any open password activities
        finishActivity(REQUEST_CODE_PWD_BACK);
        finishActivity(REQUEST_CODE_PWD_OPEN_DOOR);
        finishActivity(REQUEST_CODE_PWD_RETURN_HOME);
        finishActivity(REQUEST_CODE_PWD_FULL_RETURN);

        runOnUiThread(() -> {
            btnOpenDoor.setEnabled(false);
            btnContinue.setEnabled(false);
            
            String msg = isLastPoint ? "倒计时结束，自动返航" : "倒计时结束，前往下一站";
            Toast.makeText(CircularArrivalActivity.this, msg, Toast.LENGTH_SHORT).show();
            
            // Logic similar to processAutoDeparture in ConfirmReceiptActivity
            // Try to close doors and leave
            int resultCode = isLastPoint ? RESULT_RETURN_ORIGIN : RESULT_CONTINUE;
            
            if (doorService != null) {
                doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean allClosed) {
                         if (allClosed) {
                             closeDoorAndFinish(resultCode);
                         } else {
                             // Force close
                             doorService.closeAllDoors(new IResultCallback<Void>() {
                                 @Override
                                 public void onSuccess(Void result) {
                                     // Wait a bit then finish
                                     closeDoorAndFinish(resultCode); 
                                 }
                                 @Override
                                 public void onError(ApiError error) {
                                     // Error closing, but time is up, force leave
                                     closeDoorAndFinish(resultCode); 
                                 }
                             });
                         }
                    }
                    @Override
                    public void onError(ApiError error) {
                        closeDoorAndFinish(resultCode);
                    }
                });
            } else {
                setResult(resultCode);
                finish();
            }
        });
    }

    private void toggleDoor() {
        if (doorService == null) return;
        btnOpenDoor.setEnabled(false);
        // Do NOT reset timer here, strict 30s limit
        
        doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean allClosed) {
                runOnUiThread(() -> {
                    if (allClosed) {
                        Intent intent = new Intent(CircularArrivalActivity.this, PasswordActivity.class);
                        startActivityForResult(intent, REQUEST_CODE_PWD_OPEN_DOOR);
                    } else {
                        performCloseDoors();
                    }
                });
            }
            @Override
            public void onError(ApiError error) {
                handleDoorError("状态查询失败", error);
            }
        });
    }

    private void performOpenDoors() {
        if (doorService == null) return;
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
    }

    private void performCloseDoors() {
        if (doorService == null) return;
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
            // First check door status
            doorService.isAllDoorsClosed(new IResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean allClosed) {
                    runOnUiThread(() -> {
                        if (allClosed) {
                            // Already closed, proceed immediately
                            setResult(resultCode);
                            finish();
                        } else {
                            // Doors are open, close them
                            Toast.makeText(CircularArrivalActivity.this, "正在关闭舱门...", Toast.LENGTH_SHORT).show();
                            doorService.closeAllDoors(new IResultCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(CircularArrivalActivity.this, "舱门已关闭，5秒后继续...", Toast.LENGTH_SHORT).show();
                                        new Handler().postDelayed(() -> {
                                            setResult(resultCode);
                                            finish();
                                        }, 5000);
                                    });
                                }

                                @Override
                                public void onError(ApiError error) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(CircularArrivalActivity.this, "关门失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                        // Still finish to avoid blocking workflow
                                        setResult(resultCode);
                                        finish();
                                    });
                                }
                            });
                        }
                    });
                }

                @Override
                public void onError(ApiError error) {
                     // Check failed, try to close blindly
                     runOnUiThread(() -> {
                         // Toast.makeText(CircularArrivalActivity.this, "查询状态失败，尝试关闭舱门...", Toast.LENGTH_SHORT).show();
                         doorService.closeAllDoors(new IResultCallback<Void>() {
                             @Override
                             public void onSuccess(Void result) {
                                 runOnUiThread(() -> {
                                     Toast.makeText(CircularArrivalActivity.this, "舱门已关闭，5秒后继续...", Toast.LENGTH_SHORT).show();
                                     new Handler().postDelayed(() -> {
                                         setResult(resultCode);
                                         finish();
                                     }, 5000);
                                 });
                             }
                             @Override
                             public void onError(ApiError error) {
                                 runOnUiThread(() -> {
                                     setResult(resultCode);
                                     finish();
                                 });
                             }
                         });
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
                // removed startCountdown() to avoid resetting timer
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            WeigaoApplication.applyFullScreen(this);
        }
    }
}
