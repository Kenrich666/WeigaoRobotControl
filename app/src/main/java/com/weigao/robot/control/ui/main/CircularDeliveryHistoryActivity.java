package com.weigao.robot.control.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.weigao.robot.control.R;
import com.weigao.robot.control.manager.CircularDeliveryHistoryManager;
import com.weigao.robot.control.model.CircularDeliveryRecord;

import java.util.List;
import java.util.Locale;

public class CircularDeliveryHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private RecordAdapter adapter;
    private Button btnClear;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circular_delivery_history);

        recyclerView = findViewById(R.id.recycler_view);
        tvEmpty = findViewById(R.id.tv_empty);
        btnClear = findViewById(R.id.btn_clear);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize data
        loadData();

        // Setup Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        btnClear.setOnClickListener(v -> showClearDialog());
    }

    private void loadData() {
        List<CircularDeliveryRecord> records = CircularDeliveryHistoryManager.getInstance(this).getHistory();
        if (records.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            adapter = new RecordAdapter(records);
            recyclerView.setAdapter(adapter);
        }
    }

    private void showClearDialog() {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("确定要清空所有循环配送记录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    CircularDeliveryHistoryManager.getInstance(this).clearHistory();
                    loadData();
                    Toast.makeText(this, "记录已清空", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private static class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {
        private List<CircularDeliveryRecord> data;
        
        // No DateFormat needed here as CircularDeliveryRecord has getFormattedStartTime(),
        // but we might want uniform formatting. It uses getFormattedStartTime internally.

        public RecordAdapter(List<CircularDeliveryRecord> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_delivery_record_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CircularDeliveryRecord record = data.get(position);

            // ===== 头部: 路线信息 =====
            holder.layoutTaskHeader.setVisibility(View.VISIBLE);
            
            String routeInfo = String.format(Locale.getDefault(), "路线: %s", record.getRouteName());
            holder.tvTaskId.setText(routeInfo);
            holder.tvTaskTime.setText(String.format(Locale.getDefault(), "循环: %d次  |  开始: %s", 
                record.getLoopCount(), record.getFormattedStartTime()));

            // ===== 主体: 状态信息 =====
            // 使用 Model 的 getStatusText() 方法获取带 Emoji 的状态文本
            holder.tvPointName.setText(record.getStatusText());
            
            // 根据状态设置颜色
            int statusColor;
            String status = record.getStatus();
            if (CircularDeliveryRecord.STATUS_COMPLETED.equals(status)) {
                statusColor = 0xFF4CAF50; // 绿色 - 成功
            } else if (CircularDeliveryRecord.STATUS_CANCELLED.equals(status) 
                    || CircularDeliveryRecord.STATUS_ABORTED.equals(status)) {
                statusColor = 0xFF9E9E9E; // 灰色 - 取消/终止
            } else if (CircularDeliveryRecord.STATUS_NAV_FAILED.equals(status)) {
                statusColor = 0xFFF44336; // 红色 - 导航失败
            } else {
                statusColor = 0xFF666666; // 深灰色 - 未知
            }
            holder.tvPointName.setTextColor(statusColor);

            // ===== 底部: 耗时和结束时间 =====
            // 使用 Model 的 getFormattedDuration() 方法
            holder.tvDuration.setText("总耗时: " + record.getFormattedDuration());
            holder.tvDuration.setTextColor(0xFF666666);
            
            // 显示结束时间(如果已完成)
            if (record.getDurationSeconds() > 0) {
                holder.tvCreateTime.setText("结束: " + record.getFormattedEndTime());
                holder.tvCreateTime.setVisibility(View.VISIBLE);
                holder.tvCreateTime.setTextColor(0xFF666666);
            } else {
                holder.tvCreateTime.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            View layoutTaskHeader;
            // Header views - unused here but bound
            TextView tvTaskId, tvTaskTime; 

            TextView tvPointName;
            TextView tvDuration;
            TextView tvCreateTime;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                layoutTaskHeader = itemView.findViewById(R.id.layout_task_header);
                tvTaskId = itemView.findViewById(R.id.tv_task_id);
                tvTaskTime = itemView.findViewById(R.id.tv_task_time);

                tvPointName = itemView.findViewById(R.id.tv_point_name);
                tvDuration = itemView.findViewById(R.id.tv_duration);
                tvCreateTime = itemView.findViewById(R.id.tv_create_time);
            }
        }
    }
}
