package com.weigao.robot.control.ui.main.fragment;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.weigao.robot.control.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
//暂时不开发
public class ScheduledTasksFragment extends Fragment {

    private DrawerLayout drawerLayout;
    private LinearLayout tasksContainer;
    private View taskItemTemplate;
    private List<ScheduledTask> taskList;

    // Drawer views
    private EditText etTaskName;
    private Spinner spinnerMode;
    private Button btnStartTime, btnEndTime;
    private CheckBox cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday;
    private Button btnSave, btnCancel;

    private String selectedStartTime = "";
    private String selectedEndTime = "";
    private ScheduledTask editingTask = null;
    private View editingTaskView = null;
    private TextView drawerTitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scheduled_tasks, container, false);

        initViews(view);
        setupDrawer();
        loadMockData();

        return view;
    }

    private void initViews(View view) {
        drawerLayout = view.findViewById(R.id.drawer_layout);
        tasksContainer = view.findViewById(R.id.ll_tasks_container);
        taskItemTemplate = view.findViewById(R.id.task_item_template);
        FloatingActionButton fabNewTask = view.findViewById(R.id.fab_new_task);

        // Drawer views
        View navView = view.findViewById(R.id.nav_view);
        drawerTitle = navView.findViewById(R.id.drawer_title);
        etTaskName = navView.findViewById(R.id.et_task_name);
        spinnerMode = navView.findViewById(R.id.spinner_mode);
        btnStartTime = navView.findViewById(R.id.btn_start_time);
        btnEndTime = navView.findViewById(R.id.btn_end_time);
        cbMonday = navView.findViewById(R.id.cb_monday);
        cbTuesday = navView.findViewById(R.id.cb_tuesday);
        cbWednesday = navView.findViewById(R.id.cb_wednesday);
        cbThursday = navView.findViewById(R.id.cb_thursday);
        cbFriday = navView.findViewById(R.id.cb_friday);
        cbSaturday = navView.findViewById(R.id.cb_saturday);
        cbSunday = navView.findViewById(R.id.cb_sunday);
        btnSave = navView.findViewById(R.id.btn_save);
        btnCancel = navView.findViewById(R.id.btn_cancel);

        taskList = new ArrayList<>();

        fabNewTask.setOnClickListener(v -> openDrawer());
    }

    private void setupDrawer() {
        // Setup mode spinner
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"自动模式", "手动模式", "定时模式", "智能模式"}
        );
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMode.setAdapter(modeAdapter);

        // Time pickers
        btnStartTime.setOnClickListener(v -> showTimePicker(true));
        btnEndTime.setOnClickListener(v -> showTimePicker(false));

        // Save and cancel buttons
        btnSave.setOnClickListener(v -> saveTask());
        btnCancel.setOnClickListener(v -> closeDrawer());
    }

    private void showTimePicker(boolean isStartTime) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, selectedHour, selectedMinute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    if (isStartTime) {
                        selectedStartTime = time;
                        btnStartTime.setText("开始时间: " + time);
                    } else {
                        selectedEndTime = time;
                        btnEndTime.setText("结束时间: " + time);
                    }
                },
                hour,
                minute,
                true
        );
        timePickerDialog.show();
    }

    private void saveTask() {
        String taskName = etTaskName.getText().toString().trim();
        if (taskName.isEmpty()) {
            Toast.makeText(requireContext(), "请输入任务名称", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedStartTime.isEmpty() || selectedEndTime.isEmpty()) {
            Toast.makeText(requireContext(), "请选择开始和结束时间", Toast.LENGTH_SHORT).show();
            return;
        }

        String mode = spinnerMode.getSelectedItem().toString();
        String timeRange = selectedStartTime + " - " + selectedEndTime;
        String repeat = getSelectedDays();

        if (editingTask != null) {
            // Update existing task
            editingTask.name = taskName;
            editingTask.mode = mode;
            editingTask.timeRange = timeRange;
            editingTask.repeat = repeat;
            updateTaskView(editingTaskView, editingTask);
            Toast.makeText(requireContext(), "任务已更新", Toast.LENGTH_SHORT).show();
        } else {
            // Create new task
            ScheduledTask task = new ScheduledTask(taskName, mode, timeRange, repeat);
            taskList.add(task);
            addTaskView(task);
            Toast.makeText(requireContext(), "任务已保存", Toast.LENGTH_SHORT).show();
        }

        resetDrawerFields();
        closeDrawer();
    }

    private void addTaskView(ScheduledTask task) {
        // Clone the template
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View taskView = inflater.inflate(R.layout.fragment_scheduled_tasks, tasksContainer, false);
        View taskCard = taskView.findViewById(R.id.task_item_template);
        
        // Remove from parent if it has one
        if (taskCard.getParent() != null) {
            ((ViewGroup) taskCard.getParent()).removeView(taskCard);
        }
        
        taskCard.setVisibility(View.VISIBLE);

        // Set task data
        updateTaskView(taskCard, task);

        // Setup buttons
        Button btnEdit = taskCard.findViewById(R.id.btn_edit_task);
        Button btnDelete = taskCard.findViewById(R.id.btn_delete_task);

        btnEdit.setOnClickListener(v -> editTask(task, taskCard));
        btnDelete.setOnClickListener(v -> deleteTask(task, taskCard));

        // Add to container
        tasksContainer.addView(taskCard);
    }

    private void updateTaskView(View taskCard, ScheduledTask task) {
        TextView tvTaskName = taskCard.findViewById(R.id.tv_task_name);
        TextView tvTaskMode = taskCard.findViewById(R.id.tv_task_mode);
        TextView tvTaskTime = taskCard.findViewById(R.id.tv_task_time);
        TextView tvTaskRepeat = taskCard.findViewById(R.id.tv_task_repeat);

        tvTaskName.setText(task.name);
        tvTaskMode.setText("模式: " + task.mode);
        tvTaskTime.setText("时间: " + task.timeRange);
        tvTaskRepeat.setText("重复: " + task.repeat);
    }

    private void editTask(ScheduledTask task, View taskCard) {
        editingTask = task;
        editingTaskView = taskCard;
        
        // Update drawer title
        drawerTitle.setText("编辑任务");
        
        // Fill form with task data
        etTaskName.setText(task.name);
        
        // Set mode
        String[] modes = {"自动模式", "手动模式", "定时模式", "智能模式"};
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(task.mode)) {
                spinnerMode.setSelection(i);
                break;
            }
        }
        
        // Set times
        String[] times = task.timeRange.split(" - ");
        if (times.length == 2) {
            selectedStartTime = times[0];
            selectedEndTime = times[1];
            btnStartTime.setText("开始时间: " + selectedStartTime);
            btnEndTime.setText("结束时间: " + selectedEndTime);
        }
        
        // Set repeat days
        cbMonday.setChecked(task.repeat.contains("周一"));
        cbTuesday.setChecked(task.repeat.contains("周二"));
        cbWednesday.setChecked(task.repeat.contains("周三"));
        cbThursday.setChecked(task.repeat.contains("周四"));
        cbFriday.setChecked(task.repeat.contains("周五"));
        cbSaturday.setChecked(task.repeat.contains("周六"));
        cbSunday.setChecked(task.repeat.contains("周日"));
        
        openDrawer();
    }

    private void deleteTask(ScheduledTask task, View taskCard) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("删除任务")
            .setMessage("确定要删除任务 \"" + task.name + "\" 吗？")
            .setPositiveButton("删除", (dialog, which) -> {
                taskList.remove(task);
                tasksContainer.removeView(taskCard);
                Toast.makeText(requireContext(), "任务已删除", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private String getSelectedDays() {
        List<String> days = new ArrayList<>();
        if (cbMonday.isChecked()) days.add("周一");
        if (cbTuesday.isChecked()) days.add("周二");
        if (cbWednesday.isChecked()) days.add("周三");
        if (cbThursday.isChecked()) days.add("周四");
        if (cbFriday.isChecked()) days.add("周五");
        if (cbSaturday.isChecked()) days.add("周六");
        if (cbSunday.isChecked()) days.add("周日");

        if (days.isEmpty()) {
            return "不重复";
        }
        return String.join(", ", days);
    }

    private void resetDrawerFields() {
        drawerTitle.setText("新建任务");
        etTaskName.setText("");
        spinnerMode.setSelection(0);
        btnStartTime.setText("选择开始时间");
        btnEndTime.setText("选择结束时间");
        selectedStartTime = "";
        selectedEndTime = "";
        cbMonday.setChecked(false);
        cbTuesday.setChecked(false);
        cbWednesday.setChecked(false);
        cbThursday.setChecked(false);
        cbFriday.setChecked(false);
        cbSaturday.setChecked(false);
        cbSunday.setChecked(false);
        editingTask = null;
        editingTaskView = null;
    }

    private void openDrawer() {
        drawerLayout.openDrawer(androidx.core.view.GravityCompat.END);
    }

    private void closeDrawer() {
        drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END);
    }

    private void loadMockData() {
        taskList.add(new ScheduledTask("早晨清洁", "自动模式", "08:00 - 09:00", "周一, 周三, 周五"));
        taskList.add(new ScheduledTask("午间巡逻", "定时模式", "12:00 - 13:00", "周一, 周二, 周三, 周四, 周五"));
        taskList.add(new ScheduledTask("晚间检查", "智能模式", "18:00 - 19:00", "每天"));
        
        for (ScheduledTask task : taskList) {
            addTaskView(task);
        }
    }

    // Task model class
    private static class ScheduledTask {
        String name;
        String mode;
        String timeRange;
        String repeat;

        ScheduledTask(String name, String mode, String timeRange, String repeat) {
            this.name = name;
            this.mode = mode;
            this.timeRange = timeRange;
            this.repeat = repeat;
        }
    }
}

