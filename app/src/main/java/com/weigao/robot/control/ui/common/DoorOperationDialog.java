package com.weigao.robot.control.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * 舱门操作提示弹窗
 * <p>
 * 在开关门过程中显示居中提示，持续3秒后自动消失。
 * </p>
 */
public class DoorOperationDialog extends Dialog {

    private static final int DISPLAY_DURATION_MS = 3000; // 3秒

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TextView tvMessage;

    public DoorOperationDialog(@NonNull Context context) {
        super(context);

        // 移除标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 创建居中的文本视图
        tvMessage = new TextView(context);
        tvMessage.setTextSize(24);
        tvMessage.setTextColor(Color.WHITE);
        tvMessage.setGravity(Gravity.CENTER);
        tvMessage.setPadding(60, 40, 60, 40);
        tvMessage.setBackgroundColor(Color.parseColor("#CC333333"));

        setContentView(tvMessage);

        // 设置弹窗样式
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);

            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }

        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    /**
     * 显示开门提示
     */
    public void showOpening() {
        showMessage("开门中，请小心");
    }

    /**
     * 显示关门提示
     */
    public void showClosing() {
        showMessage("关门中，请小心");
    }

    /**
     * 显示消息并在3秒后自动关闭
     */
    private void showMessage(String message) {
        tvMessage.setText(message);

        if (!isShowing()) {
            show();
        }

        // 3秒后自动关闭
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::dismiss, DISPLAY_DURATION_MS);
    }

    @Override
    public void dismiss() {
        handler.removeCallbacksAndMessages(null);
        super.dismiss();
    }
}
