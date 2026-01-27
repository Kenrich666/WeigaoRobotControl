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

            // Use Header for Route Info (mimicking TaskGrouping)
            holder.layoutTaskHeader.setVisibility(View.VISIBLE);
            
            String routeInfo = String.format(Locale.getDefault(), "路线: %s (循环 %d 次)", 
                record.getRouteName(), record.getLoopCount());
            holder.tvTaskId.setText(routeInfo);
            holder.tvTaskTime.setText("开始时间: " + record.getFormattedStartTime());

            // Body for Status and Duration
            String statusRaw = record.getStatus();
            String statusText = "未知状态";
            int statusColor = 0xFF333333; // Dark Gray

            if ("COMPLETED".equalsIgnoreCase(statusRaw)) {
                statusText = "配送完成";
                statusColor = 0xFF4CAF50; // Green
            } else if ("CANCELLED".equalsIgnoreCase(statusRaw)) {
                statusText = "已取消";
                statusColor = 0xFF9E9E9E; // Gray
            } else if ("ABORTED".equalsIgnoreCase(statusRaw)) {
                statusText = "已终止";
                statusColor = 0xFFF44336; // Red
            } else if (statusRaw == null) {
                statusText = "进行中";
                 statusColor = 0xFF2196F3; // Blue
            }

            holder.tvPointName.setText(statusText);
            holder.tvPointName.setTextColor(statusColor);

            String durationStr = "";
            if (record.getDurationSeconds() > 0) {
                long s = record.getDurationSeconds();
                durationStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
                durationStr = "总耗时: " + durationStr;
            } else {
                 durationStr = "计算中...";
            }
            holder.tvDuration.setText(durationStr);
            holder.tvDuration.setTextColor(0xFF666666); // Normal text color for duration description
            
            // Show End Time if available
            // We need to calculate end time or format it if CircularDeliveryRecord exposes it, 
            // otherwise just re-show start or hide.
            // CircularDeliveryRecord has start and duration. End = Start + Duration*1000
            if (record.getDurationSeconds() > 0) {
                 long endTime = record.getFormattedStartTime() != null ? 
                     (System.currentTimeMillis()) : 0; // Fallback? 
                 // Actually record has getStartTime(). Let's use simple math to show rough end time if needed
                 // Or just hide it if not critical. 
                 // ItemDeliveryRecord shows "Create Time" (Arrival Time).
                 // Let's show "结束时间: ..."
                 long endMs = 0;
                 // We don't have access to raw start time long here easily unless we parse or add getter. 
                 // CircularDeliveryRecord HAS getDurationSeconds.
                 // Ideally we should add getEndTime() to CircularDeliveryRecord or use what we have.
                 // Let's just use "结束于: [Calculated]" or just leave it blank if complex.
                 // Actually, ItemDeliveryRecord uses "Create Time". 
                 // Let's just put something static or formatted if possible.
                 // Re-reading CircularDeliveryRecord in previous steps... it has private long endTime.
                 // It does NOT expose getEndTime() public method in the snippet I saw (only complete sets it).
                 // It has `toJson` which uses it.
                 // Let's rely on what we have.
                 holder.tvCreateTime.setVisibility(View.GONE);
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
