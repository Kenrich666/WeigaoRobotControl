package com.weigao.robot.control.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
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

public class CircularArrivalActivity extends AppCompatActivity {
    private static final String TAG = "CircularArrivalActivity";

    public static final int RESULT_CONTINUE = 101;
    public static final int RESULT_RETURN_ORIGIN = 102;
    public static final int RESULT_CANCEL = 100;

    private IDoorService doorService;
    private Button btnOpenDoor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circular_arrival);

        doorService = ServiceManager.getInstance().getDoorService();
        if (doorService != null) {
            doorService.registerCallback(doorCallback);
        }

        initViews();
        updateDoorButtonState();
    }

    private void initViews() {
        btnOpenDoor = findViewById(R.id.btn_open_door);
        Button btnReturnHome = findViewById(R.id.btn_return_home);
        Button btnContinue = findViewById(R.id.btn_continue);
        Button btnFullReturn = findViewById(R.id.btn_full_return);

        btnOpenDoor.setOnClickListener(v -> toggleDoor());

        btnReturnHome.setOnClickListener(v -> {
            // General return home (Manual abort)
            setResult(RESULT_RETURN_ORIGIN);
            finish();
        });

        btnContinue.setOnClickListener(v -> {
            // Check door status before leaving? Usually better to auto-close.
            // For now, assume user closes or we auto-close on navigate.
            closeDoorAndFinish(RESULT_CONTINUE);
        });

        btnFullReturn.setOnClickListener(v -> {
            // "Full, return to origin"
            closeDoorAndFinish(RESULT_RETURN_ORIGIN);
        });
    }

    private void toggleDoor() {
        if (doorService == null) return;
        btnOpenDoor.setEnabled(false);
        
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
                        // If auto-close fails, do we prevent navigation? 
                        // Let's allow proceed but warn user, or force them to retry.
                        // For safety, maybe just finish anyway or stay? 
                        // Let's finish for now, assuming implementation might not have sensors.
                        // Ideally: stay and make user close.
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
            runOnUiThread(() -> updateDoorButtonState());
        }
        @Override public void onDoorTypeChanged(DoorType type) {}
        @Override public void onDoorTypeSettingResult(boolean success) {}
        @Override public void onDoorError(int doorId, int errorCode) {}
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (doorService != null) {
            doorService.unregisterCallback(doorCallback);
        }
    }
}
