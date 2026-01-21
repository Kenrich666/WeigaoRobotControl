package com.weigao.robot.control.ui.main;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.weigao.robot.control.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeliveryActivity extends AppCompatActivity {
    private Button selectedLayerButton;
    // 存储配对关系
    private final HashMap<Integer, String> pairings = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery);

        // --- 1. 基础按钮 ---
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        Button openDoorButton = findViewById(R.id.open_door_button);
        openDoorButton.setOnClickListener(v -> {
            if (openDoorButton.getText().equals("开门")) {
                openDoorButton.setText("闭门");
            } else {
                openDoorButton.setText("开门");
            }
        });

        findViewById(R.id.return_button).setOnClickListener(v ->
                startActivity(new Intent(this, ReturnActivity.class)));

        findViewById(R.id.start_delivery_button).setOnClickListener(v -> {
            if (pairings.isEmpty()) {
                Toast.makeText(this, "请至少配对一个层", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, DeliveryNavigationActivity.class);
            intent.putExtra("pairings", pairings);
            startActivity(intent);
        });

        // --- 2. 层选择与“删除”逻辑 ---
        Button l1Button = findViewById(R.id.l1_button);
        Button l2Button = findViewById(R.id.l2_button);
        Button l3Button = findViewById(R.id.l3_button);

        View.OnClickListener layerClickListener = v -> {
            Button clickedButton = (Button) v;
            int id = clickedButton.getId();

            // Case 1: The user is clicking the currently selected (purple) button.
            // This action is always "unselect".
            if (clickedButton == selectedLayerButton) {
                refreshLayerStyle(clickedButton); // Revert to green (if paired) or gray (if not).
                selectedLayerButton = null;
                return;
            }

            // Case 2: The user is clicking a paired (green) button.
            // The desired action is "un-pair".
            if (pairings.containsKey(id)) {
                // Un-pair it.
                pairings.remove(id);
                if (id == R.id.l1_button) clickedButton.setText("L1 层");
                if (id == R.id.l2_button) clickedButton.setText("L2 层");
                if (id == R.id.l3_button) clickedButton.setText("L3 层");
                Toast.makeText(this, "已删除配对", Toast.LENGTH_SHORT).show();
                refreshLayerStyle(clickedButton); // It will become gray now.

                // If another button was selected, unselect it as well.
                if (selectedLayerButton != null) {
                    refreshLayerStyle(selectedLayerButton);
                    selectedLayerButton = null;
                }
                return;
            }

            // Case 3: The user is clicking an unpaired (gray) button.
            // The action is "select for pairing".
            // First, un-select any previously selected button.
            if (selectedLayerButton != null) {
                refreshLayerStyle(selectedLayerButton);
            }
            // Then, select the new button.
            selectedLayerButton = clickedButton;
            selectedLayerButton.setBackgroundResource(R.drawable.rounded_button_selected);
        };

        l1Button.setOnClickListener(layerClickListener);
        l2Button.setOnClickListener(layerClickListener);
        l3Button.setOnClickListener(layerClickListener);

        // --- 3. 点位列表与“修改”逻辑 ---
        RecyclerView pointsRecyclerView = findViewById(R.id.points_recyclerview);
        pointsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        List<String> points = new ArrayList<>();
        for (int i = 1; i <= 15; i++) points.add("点位 " + i);

        PointAdapter adapter = new PointAdapter(points, pointText -> {
            if (selectedLayerButton == null) {
                Toast.makeText(this, "请先选择一个机器人层", Toast.LENGTH_SHORT).show();
                return;
            }

            // 配对或修改配对：直接覆盖
            pairings.put(selectedLayerButton.getId(), pointText);
            selectedLayerButton.setText(pointText);
            Toast.makeText(this, "已配对: " + pointText, Toast.LENGTH_SHORT).show();

            // 配对后立即刷新样式为绿色并取消“选中”状态
            refreshLayerStyle(selectedLayerButton);
            selectedLayerButton = null;
        });
        pointsRecyclerView.setAdapter(adapter);
    }

    /**
     * 只有在切换层级或取消选中时调用：
     * 如果有配对则显示绿色，没配对则显示原始灰色
     */
    private void refreshLayerStyle(Button button) {
        if (pairings.containsKey(button.getId())) {
            button.setBackgroundResource(R.drawable.rounded_button_paired); // 绿色
        } else {
            button.setBackgroundResource(R.drawable.rounded_button); // 灰色
        }
    }

    // --- Adapter 代码 (保持不变) ---
    private static class PointAdapter extends RecyclerView.Adapter<PointAdapter.PointViewHolder> {
        private final List<String> mPoints;
        private final OnPointClickListener mListener;

        PointAdapter(List<String> points, OnPointClickListener listener) {
            mPoints = points; mListener = listener;
        }

        @NonNull
        @Override
        public PointViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Button button = new Button(parent.getContext());
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int margin = (int) (8 * parent.getResources().getDisplayMetrics().density);
            params.setMargins(margin, margin, margin, margin);
            button.setLayoutParams(params);
            button.setBackgroundResource(R.drawable.item_point_style);
            button.setTextColor(Color.BLACK);
            return new PointViewHolder(button);
        }

        @Override
        public void onBindViewHolder(@NonNull PointViewHolder holder, int position) {
            String text = mPoints.get(position);
            holder.pointButton.setText(text);
            holder.pointButton.setOnClickListener(v -> mListener.onPointClick(text));
        }

        @Override
        public int getItemCount() { return mPoints.size(); }

        static class PointViewHolder extends RecyclerView.ViewHolder {
            Button pointButton;
            PointViewHolder(Button b) { super(b); pointButton = b; }
        }
    }

    interface OnPointClickListener {
        void onPointClick(String pointText);
    }
}
