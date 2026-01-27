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
import com.weigao.robot.control.manager.ItemDeliveryManager;
import com.weigao.robot.control.model.ItemDeliveryRecord;

import java.util.List;

public class ItemDeliveryHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private RecordAdapter adapter;
    private Button btnClear;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_delivery_history);

        recyclerView = findViewById(R.id.recycler_view);
        tvEmpty = findViewById(R.id.tv_empty);
        btnClear = findViewById(R.id.btn_clear);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 初始化数据
        loadData();

        // 设置返回按钮
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
        List<ItemDeliveryRecord> records = ItemDeliveryManager.getInstance().getAllRecords();
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
                .setMessage("确定要清空所有配送记录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    ItemDeliveryManager.getInstance().clearRecords();
                    loadData();
                    Toast.makeText(this, "记录已清空", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private static class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {
        private List<ItemDeliveryRecord> data;
        private java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault());

        public RecordAdapter(List<ItemDeliveryRecord> data) {
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
            ItemDeliveryRecord record = data.get(position);

            // 分组逻辑：如果是第一条，或者当前条目的taskId与上一条不同，则显示Header
            boolean showHeader = false;
            if (position == 0) {
                showHeader = true;
            } else {
                ItemDeliveryRecord prevRecord = data.get(position - 1);
                String currTaskId = record.getTaskId();
                String prevTaskId = prevRecord.getTaskId();
                // 假如taskId为null (旧数据), 则回退到比较startTime
                if (currTaskId != null && !currTaskId.equals(prevTaskId)) {
                    showHeader = true;
                } else if (currTaskId == null && record.getStartTime() != prevRecord.getStartTime()) {
                    showHeader = true;
                }
            }

            if (showHeader) {
                holder.layoutTaskHeader.setVisibility(View.VISIBLE);
                String taskIdDisplay = record.getTaskId() != null ? record.getTaskId() : "N/A";
                // 截取UUID前8位显示，避免太长
                if (taskIdDisplay.length() > 8) {
                    taskIdDisplay = taskIdDisplay.substring(0, 8) + "...";
                }
                holder.tvTaskId.setText("任务ID: " + taskIdDisplay);
                holder.tvTaskTime.setText("出发时间: " + dateFormat.format(new java.util.Date(record.getStartTime())));
            } else {
                holder.layoutTaskHeader.setVisibility(View.GONE);
            }

            holder.tvPointName.setText(record.getPointName());

            // 状态显示
            String statusText;
            int statusColor;
            switch (record.getStatus()) {
                case ItemDeliveryRecord.STATUS_SUCCESS:
                    statusText = "配送成功";
                    statusColor = 0xFF4CAF50; // Green
                    break;
                case ItemDeliveryRecord.STATUS_FAILED_TIMEOUT:
                    statusText = "配送失败 (超时)";
                    statusColor = 0xFFF44336; // Red
                    break;
                case ItemDeliveryRecord.STATUS_FAILED_HARDWARE:
                    statusText = "配送失败 (硬件错误)";
                    statusColor = 0xFFF44336; // Red
                    break;
                case ItemDeliveryRecord.STATUS_NAV_FAILED:
                    statusText = "配送失败 (导航异常)";
                    statusColor = 0xFFF44336; // Red
                    break;
                case ItemDeliveryRecord.STATUS_CANCELLED:
                    statusText = "配送取消";
                    statusColor = 0xFF9E9E9E; // Gray
                    break;
                default:
                    statusText = "未知状态";
                    statusColor = 0xFF333333;
            }

            holder.tvDuration.setText(statusText + " | 耗时: " + record.getFormattedDuration());
            holder.tvDuration.setTextColor(statusColor);
            holder.tvCreateTime.setText("时间: " + record.getCreateTime());
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            View layoutTaskHeader;
            TextView tvTaskId;
            TextView tvTaskTime;

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
