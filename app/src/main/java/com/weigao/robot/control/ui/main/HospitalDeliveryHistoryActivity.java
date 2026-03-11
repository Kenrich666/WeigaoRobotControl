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
import com.weigao.robot.control.manager.HospitalDeliveryManager;
import com.weigao.robot.control.model.HospitalDeliveryRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HospitalDeliveryHistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private Button btnClear;
    private RecordAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_delivery_history);

        // 历史记录由 HospitalDeliveryManager 负责文件持久化，这里进入页面时先加载一次。
        HospitalDeliveryManager.getInstance().init(this);

        recyclerView = findViewById(R.id.recycler_view);
        tvEmpty = findViewById(R.id.tv_empty);
        btnClear = findViewById(R.id.btn_clear);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnClear.setOnClickListener(v -> showClearDialog());
        loadData();
    }

    private void loadData() {
        List<HospitalDeliveryRecord> records = HospitalDeliveryManager.getInstance().getAllRecords();
        if (records.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        recyclerView.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        adapter = new RecordAdapter(records);
        recyclerView.setAdapter(adapter);
    }

    private void showClearDialog() {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("确定要清空所有医院配送记录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    HospitalDeliveryManager.getInstance().clearRecords();
                    loadData();
                    Toast.makeText(this, "记录已清空", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            com.weigao.robot.control.app.WeigaoApplication.applyFullScreen(this);
        }
    }

    private static class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {
        private final List<HospitalDeliveryRecord> data;
        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        RecordAdapter(List<HospitalDeliveryRecord> data) {
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
            HospitalDeliveryRecord record = data.get(position);

            boolean showHeader = position == 0;
            if (position > 0) {
                HospitalDeliveryRecord prevRecord = data.get(position - 1);
                String currTaskId = record.getTaskId();
                String prevTaskId = prevRecord.getTaskId();
                showHeader = currTaskId != null ? !currTaskId.equals(prevTaskId)
                        : record.getStartTime() != prevRecord.getStartTime();
            }

            if (showHeader) {
                holder.layoutTaskHeader.setVisibility(View.VISIBLE);
                String taskIdDisplay = record.getTaskId() != null ? record.getTaskId() : "N/A";
                if (taskIdDisplay.length() > 8) {
                    taskIdDisplay = taskIdDisplay.substring(0, 8) + "...";
                }
                holder.tvTaskId.setText("任务ID: " + taskIdDisplay);
                holder.tvTaskTime.setText("出发时间: " + dateFormat.format(new Date(record.getStartTime())));
            } else {
                holder.layoutTaskHeader.setVisibility(View.GONE);
            }

            holder.tvPointName.setText(buildRecordTitle(record));
            holder.tvDuration.setText(getStatusLabel(record.getStatus()) + " | 耗时: " + record.getFormattedDuration());
            holder.tvDuration.setTextColor(getStatusColor(record.getStatus()));
            holder.tvCreateTime.setText("时间: " + record.getCreateTime());
        }

        private String buildRecordTitle(HospitalDeliveryRecord record) {
            String pointName = record.getPointName();
            if (pointName == null || pointName.trim().isEmpty()) {
                pointName = "未命名点位";
            }

            String normalizedPointName = pointName.trim();
            switch (record.getStage()) {
                case HospitalDeliveryRecord.STAGE_DISINFECTION:
                    if (containsAny(normalizedPointName, "消毒", "清洗")) {
                        return normalizedPointName;
                    }
                    return "消毒间 - " + normalizedPointName;
                case HospitalDeliveryRecord.STAGE_ROOM:
                    return "房间 - " + normalizedPointName;
                case HospitalDeliveryRecord.STAGE_RETURN:
                    if (containsAny(normalizedPointName, "原点", "起点", "返航")) {
                        return "返航至 " + normalizedPointName;
                    }
                    return "返航 - " + normalizedPointName;
                default:
                    return normalizedPointName;
            }
        }

        private boolean containsAny(String text, String... keywords) {
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }

        private String getStageLabel(int stage) {
            switch (stage) {
                case HospitalDeliveryRecord.STAGE_DISINFECTION:
                    return "消毒间";
                case HospitalDeliveryRecord.STAGE_ROOM:
                    return "房间";
                case HospitalDeliveryRecord.STAGE_RETURN:
                    return "返航";
                default:
                    return "阶段";
            }
        }

        private String getStatusLabel(int status) {
            switch (status) {
                case HospitalDeliveryRecord.STATUS_SUCCESS:
                    return "配送成功";
                case HospitalDeliveryRecord.STATUS_FAILED_TIMEOUT:
                    return "配送失败(超时)";
                case HospitalDeliveryRecord.STATUS_FAILED_HARDWARE:
                    return "配送失败(硬件错误)";
                case HospitalDeliveryRecord.STATUS_NAV_FAILED:
                    return "配送失败(导航异常)";
                case HospitalDeliveryRecord.STATUS_CANCELLED:
                    return "配送取消";
                default:
                    return "未知状态";
            }
        }

        private int getStatusColor(int status) {
            switch (status) {
                case HospitalDeliveryRecord.STATUS_SUCCESS:
                    return 0xFF4CAF50;
                case HospitalDeliveryRecord.STATUS_FAILED_TIMEOUT:
                case HospitalDeliveryRecord.STATUS_FAILED_HARDWARE:
                case HospitalDeliveryRecord.STATUS_NAV_FAILED:
                    return 0xFFF44336;
                case HospitalDeliveryRecord.STATUS_CANCELLED:
                    return 0xFF9E9E9E;
                default:
                    return 0xFF333333;
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final View layoutTaskHeader;
            final TextView tvTaskId;
            final TextView tvTaskTime;
            final TextView tvPointName;
            final TextView tvDuration;
            final TextView tvCreateTime;

            ViewHolder(@NonNull View itemView) {
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
