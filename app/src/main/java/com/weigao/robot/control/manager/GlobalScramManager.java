package com.weigao.robot.control.manager;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.weigao.robot.control.R;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;

import com.weigao.robot.control.callback.IStateCallback;
import com.weigao.robot.control.model.ChargingState;
import com.weigao.robot.control.model.RobotState;
import com.weigao.robot.control.service.IChargerService;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;

import java.lang.ref.WeakReference;

/**
 * 鍏ㄥ眬鎬ュ仠鐘舵€佺鐞嗗櫒
 * <p>
 * 璐熻矗鐩戝惉鍏ㄥ眬鎬ュ仠鎸夐挳鐘舵€侊紝骞跺湪浠讳綍 Activity 涔嬩笂寮瑰嚭鎬ュ仠璀﹀憡瀵硅瘽妗嗐€?
 * 閫氳繃 Application.ActivityLifecycleCallbacks 杩借釜褰撳墠鏈€涓婂眰鐨?Activity銆?
 * </p>
 */
public class GlobalScramManager implements Application.ActivityLifecycleCallbacks, IStateCallback {

    private static final String TAG = "GlobalScramManager";

    private static GlobalScramManager instance;
    private WeakReference<Activity> currentActivityRef;
    private Dialog scramDialog;
    private boolean isScramPressed = false;

    private GlobalScramManager() {
    }

    public static synchronized GlobalScramManager getInstance() {
        if (instance == null) {
            instance = new GlobalScramManager();
        }
        return instance;
    }

    /**
     * 鍒濆鍖栵細娉ㄥ唽 Activity 鐢熷懡鍛ㄦ湡鐩戝惉鍜屾湇鍔＄洃鍚?
     *
     * @param application Application 瀹炰緥
     */
    public void init(Application application) {
        application.registerActivityLifecycleCallbacks(this);

        // 灏濊瘯娉ㄥ唽 RobotStateService 鐩戝惉
        // 娉ㄦ剰锛歋erviceLocator 鍙兘杩樻湭鍑嗗濂斤紝杩欓噷鏀惧湪寤惰繜鍙兘浼氭洿绋冲Ε锛?
        // 鎴栬€呬緷闈犲閮紙濡?WeigaoApplication锛夊湪 Service 鍒濆鍖栧悗璋冪敤 connectService
    }

    /**
     * 杩炴帴 RobotStateService
     * 搴斿湪 ServiceManager 鍒濆鍖栧畬鎴愬悗璋冪敤
     */
    public void connectService() {
        try {
            IRobotStateService service = ServiceManager.getInstance().getRobotStateService();
            if (service != null) {
                service.registerCallback(this);
                Log.d(TAG, "宸叉敞鍐?RobotStateService 鐩戝惉");

                // 涓诲姩鏌ヨ涓€娆＄姸鎬侊紝闃叉閿欒繃鍒濆鐘舵€?
                service.isScramButtonPressed(new com.weigao.robot.control.callback.IResultCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean pressed) {
                        onScramButtonPressed(pressed);
                    }

                    @Override
                    public void onError(com.weigao.robot.control.callback.ApiError error) {
                        // 蹇界暐
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "杩炴帴 RobotStateService 澶辫触", e);
        }
    }

    // ==================== IStateCallback ====================

    @Override
    public void onStateChanged(RobotState newState) {
        if (newState != null) {
            boolean pressed = newState.isScramButtonPressed();
            // 濡傛灉鐘舵€佸彂鐢熷彉鍖栵紝瑙﹀彂鏇存柊
            if (pressed != isScramPressed) {
                onScramButtonPressed(pressed);
            }
        }
    }

    @Override
    public void onLocationChanged(double x, double y) {
        // 涓嶉渶瑕佸鐞嗕綅缃?
    }

    @Override
    public void onBatteryLevelChanged(int level) {
        // 涓嶉渶瑕佸鐞嗙數閲?
    }

    @Override
    public void onChargingStateChanged(ChargingState chargingState) {
        // no-op
    }

    @Override
    public void onScramButtonPressed(boolean pressed) {
        this.isScramPressed = pressed;
        Log.d(TAG, "鏀跺埌鎬ュ仠鐘舵€? " + pressed);

        // 鍦ㄤ富绾跨▼澶勭悊 UI
        if (pressed) {
            stopChargingOnScram();
        }
        Activity activity = getCurrentActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                if (pressed) {
                    showScramDialog(activity);
                } else {
                    dismissScramDialog();
                }
            });
        }
    }

    // ==================== UI 閫昏緫 ====================

    private void stopChargingOnScram() {
        try {
            IChargerService chargerService = ServiceManager.getInstance().getChargerService();
            if (chargerService == null) {
                return;
            }
            chargerService.stopCharge(new IResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Emergency stop triggered charge stop");
                }

                @Override
                public void onError(ApiError error) {
                    Log.w(TAG, "Emergency stop charge stop failed: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Emergency stop charge stop exception", e);
        }
    }

    private void showScramDialog(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        // 濡傛灉瀵硅瘽妗嗗凡缁忓湪鏄剧ず涓斿睘浜庡綋鍓?Activity锛屽垯涓嶅鐞?
        if (scramDialog != null && scramDialog.isShowing()) {
            // 纭繚瀵硅瘽妗嗕緷闄勭殑鏄綋鍓?Context
            return;
        }

        // 濡傛灉涔嬪墠鐨勫璇濇灞炰簬鏃?Activity锛屽厛鍏抽棴
        dismissScramDialog();

        Log.w(TAG, "显示急停对话框");

        scramDialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        scramDialog.setContentView(R.layout.dialog_emergency_stop);
        scramDialog.setCancelable(false); // 绂佹鐐瑰嚮澶栭儴鍙栨秷

        if (scramDialog.getWindow() != null) {
            // 寮瑰嚭鏃舵殏涓嶈幏鍙栫劍鐐癸紝閬垮厤鐢变簬鐒︾偣鍒囨崲瀵艰嚧绯荤粺鐘舵€佹爮/瀵艰埅鏍忓脊鍥?
            scramDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }

        try {
            scramDialog.show();

            // 纭繚褰撳墠 Activity 缁存寔鍏ㄥ睆鐘舵€?
            com.weigao.robot.control.app.WeigaoApplication.applyFullScreen(activity);

            if (scramDialog.getWindow() != null) {
                // 浣?Dialog 鏈韩涔熻繘鍏ユ矇娴稿紡鍏ㄥ睆锛岄€傞厤绯荤粺鐨?WindowInsets
                try {
                    boolean isFullscreen = AppSettingsManager.getInstance().isFullScreen();
                    if (isFullscreen) {
                        WindowCompat.setDecorFitsSystemWindows(scramDialog.getWindow(), false);
                        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(
                                scramDialog.getWindow(), scramDialog.getWindow().getDecorView());
                        controller.hide(WindowInsetsCompat.Type.systemBars());
                        controller.setSystemBarsBehavior(
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "璁剧疆 Dialog 鍏ㄥ睆澶辫触", e);
                }

                // 鎭㈠鐒︾偣锛屽厑璁哥偣鍑?
                scramDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "显示对话框失败", e);
        }
    }

    private void dismissScramDialog() {
        if (scramDialog != null) {
            try {
                if (scramDialog.isShowing()) {
                    scramDialog.dismiss();
                }
            } catch (IllegalArgumentException e) {
                // 蹇界暐 View not attached 寮傚父锛岄槻姝?crash
                Log.w(TAG, "鍏抽棴瀵硅瘽妗嗘椂 View 宸蹭笉渚濋檮 WindowManager (鍙兘 Activity 宸查攢姣?");
            } catch (Exception e) {
                Log.e(TAG, "关闭对话框失败", e);
            } finally {
                scramDialog = null;
            }
        }
    }

    private Activity getCurrentActivity() {
        if (currentActivityRef != null) {
            return currentActivityRef.get();
        }
        return null;
    }

    // ==================== Lifecycle Callbacks ====================

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentActivityRef = new WeakReference<>(activity);

        // 濡傛灉褰撳墠澶勪簬鎬ュ仠鐘舵€侊紝姣忔 Activity 鎭㈠鏃堕兘瑕佸脊绐?
        // 闃叉鐢ㄦ埛瀵艰埅鍒版柊椤甸潰鎴栨寜 Home 閿洖鏉ュ悗寮圭獥娑堝け
        if (isScramPressed) {
            showScramDialog(activity);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // Activity 鏆傚仠/鍒囨崲鏃跺叧闂璇濇锛岄槻姝?WindowLeaked
        // 褰撲笅涓€涓?Activity Resume 鏃讹紝濡傛灉鎬ュ仠浠嶆湭瑙ｉ櫎锛屼細鍐嶆寮瑰嚭
        dismissScramDialog();
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        // 濡傛灉鏄綋鍓嶆寔鏈夌殑 Activity 琚攢姣侊紝涓斿璇濇鍦ㄥ叾涓婏紝闇€瑕佹竻鐞嗗紩鐢ㄩ槻姝㈡硠婕?
        if (scramDialog != null && cleanupActivityRef(activity)) {
            try {
                scramDialog.dismiss();
            } catch (Exception e) {
                // ignore
            }
            scramDialog = null;
        }
    }

    private boolean cleanupActivityRef(Activity activity) {
        Activity current = getCurrentActivity();
        return current == activity;
    }
}

