package com.weigao.robot.control.ui.main;

import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.weigao.robot.control.R;

import java.util.ArrayList;
import java.util.List;

public class DeliveryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        创建页面时候的调用
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery);

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // 在 onCreate 中修改 RecyclerView 的配置
        RecyclerView pointsRecyclerView = findViewById(R.id.points_recyclerview);

        // 使用网格布局，每行 3 个点位（可根据屏幕宽度动态调整）
        pointsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        // 模拟从后端获取不确定数量的点位
        List<String> points = new ArrayList<>();
        int mockDataCount = (int) (Math.random() * 20) + 5; // 随机生成 5-25 个点位
        for (int i = 1; i <= mockDataCount; i++) {
            points.add("点位 " + i);
        }

        PointAdapter adapter = new PointAdapter(points);
        pointsRecyclerView.setAdapter(adapter);
    }

    private static class PointAdapter extends RecyclerView.Adapter<PointAdapter.PointViewHolder> {

        private final List<String> mPoints;

        PointAdapter(List<String> points) {
            mPoints = points;
        }

        @NonNull
        @Override
        public PointViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Button button = new Button(parent.getContext());
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            // Convert 8dp to pixels for margins
            int margin = (int) (8 * parent.getResources().getDisplayMetrics().density);
            params.setMargins(margin, margin, margin, margin);
            button.setLayoutParams(params);
            button.setBackgroundResource(R.drawable.item_point_style);
            button.setTextColor(Color.BLACK); // Set a contrasting text color
            return new PointViewHolder(button);
        }

        @Override
        public void onBindViewHolder(@NonNull PointViewHolder holder, int position) {
            holder.bind(mPoints.get(position));
        }

        @Override
        public int getItemCount() {
            return mPoints.size();
        }

        static class PointViewHolder extends RecyclerView.ViewHolder {
            Button pointButton;

            PointViewHolder(@NonNull Button button) {
                super(button);
                pointButton = button;
            }

            void bind(String pointText) {
                pointButton.setText(pointText);
            }
        }
    }
}
