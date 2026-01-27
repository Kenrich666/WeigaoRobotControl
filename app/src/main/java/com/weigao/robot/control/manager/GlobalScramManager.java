package com.weigao.robot.control.manager;

import android.app.Activity;
import android.app.Application;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.weigao.robot.control.callback.IStateCallback;
import com.weigao.robot.control.model.RobotState;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;

import java.lang.ref.WeakReference;

/**
 * 全局急停状态管理器
 * <p>
 * 负责监听全局急停按钮状态，并在任何 Activity 之上弹出急停警告对话框。
 * 通过 Application.ActivityLifecycleCallbacks 追踪当前最上层的 Activity。
 * </p>
 */
public class GlobalScramManager implements Application.ActivityLifecycleCallbacks, IStateCallback {

    private static final String TAG = "GlobalScramManager";

    private static GlobalScramManager instance;
    private WeakReference<Activity> currentActivityRef;
    private AlertDialog scramDialog;
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
     * 初始化：注册 Activity 生命周期监听和服务监听
     *
     * @param application Application 实例
     */
    public void init(Application application) {
        application.registerActivityLifecycleCallbacks(this);
        
        // 尝试注册 RobotStateService 监听
        // 注意：ServiceLocator 可能还未准备好，这里放在延迟可能会更稳妥，
        // 或者依靠外部（如 WeigaoApplication）在 Service 初始化后调用 connectService
    }

    /**
     * 连接 RobotStateService
     * 应在 ServiceManager 初始化完成后调用
     */
    public void connectService() {
        try {
            IRobotStateService service = ServiceManager.getInstance().getRobotStateService();
            if (service != null) {
                service.registerCallback(this);
                Log.d(TAG, "已注册 RobotStateService 监听");
                
                // 主动查询一次状态，防止错过初始状态
                service.isScramButtonPressed(new com.weigao.robot.control.callback.IResultCallback<Boolean>() {
                   @Override
                   public void onSuccess(Boolean pressed) {
                       onScramButtonPressed(pressed);
                   }
                   
                   @Override
                   public void onError(com.weigao.robot.control.callback.ApiError error) {
                       // 忽略
                   }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "连接 RobotStateService 失败", e);
        }
    }

    // ==================== IStateCallback ====================

    @Override
    public void onStateChanged(RobotState newState) {
        if (newState != null) {
            boolean pressed = newState.isScramButtonPressed();
            // 如果状态发生变化，触发更新
            if (pressed != isScramPressed) {
                onScramButtonPressed(pressed);
            }
        }
    }

    @Override
    public void onLocationChanged(double x, double y) {
        // 不需要处理位置
    }

    @Override
    public void onBatteryLevelChanged(int level) {
        // 不需要处理电量
    }

    @Override
    public void onScramButtonPressed(boolean pressed) {
        this.isScramPressed = pressed;
        Log.d(TAG, "收到急停状态: " + pressed);
        
        // 在主线程处理 UI
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

    // ==================== UI 逻辑 ====================

    private void showScramDialog(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        // 如果对话框已经在显示且属于当前 Activity，则不处理
        if (scramDialog != null && scramDialog.isShowing()) {
            // 确保对话框依附的是当前 Context
            return;
        }
        
        // 如果之前的对话框属于旧 Activity，先关闭
        dismissScramDialog();

        Log.w(TAG, "显示急停对话框");

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("⚠️ 紧急停止");
        builder.setMessage("急停开关已按下！\n机器人已停止运行，请释放急停开关以恢复。");
        builder.setCancelable(false); // 禁止点击外部取消
        
        scramDialog = builder.create();
        
        // 设为红色警示风格（可选，视具体 UI 需求）
        // scramDialog.getWindow().setBackgroundDrawableResource(android.R.color.holo_red_dark);

        try {
            scramDialog.show();
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
                // 忽略 View not attached 异常，防止 crash
                Log.w(TAG, "关闭对话框时 View 已不依附 WindowManager (可能 Activity 已销毁)");
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
        
        // 如果当前处于急停状态，每次 Activity 恢复时都要弹窗
        // 防止用户导航到新页面或按 Home 键回来后弹窗消失
        if (isScramPressed) {
            showScramDialog(activity);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // Activity 暂停/切换时关闭对话框，防止 WindowLeaked
        // 当下一个 Activity Resume 时，如果急停仍未解除，会再次弹出
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
        // 如果是当前持有的 Activity 被销毁，且对话框在其上，需要清理引用防止泄漏
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
