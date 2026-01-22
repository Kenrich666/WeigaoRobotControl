package com.weigao.robot.control.ui.main.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.weigao.robot.control.R;

public class SceneSettingsFragment extends Fragment {

    private LinearLayout layoutMapSelection;
    private LinearLayout layoutOptionsContainer;
    private LinearLayout layoutDishExit;
    private LinearLayout layoutDishwashingArea;
    private LinearLayout layoutMappingTool;
    
    private TextView tvMapSelectionValue;
    private TextView tvDishExitValue;
    private TextView tvDishwashingAreaValue;
    
    private String selectedMap = null;
    private String selectedDishExit = null;
    private String selectedDishwashingArea = null;
    
    // 模拟地图数据
    private final String[] availableMaps = {"一楼地图", "二楼地图", "三楼地图", "餐厅地图"};
    
    // 模拟各种口的数据（这些会根据选择的地图动态加载）
    private String[] availableExits;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scene_settings, container, false);
        
        initViews(view);
        setupClickListeners();
        
        return view;
    }
    
    private void initViews(View view) {
        // Initialize layouts
        layoutMapSelection = view.findViewById(R.id.layout_map_selection);
        layoutOptionsContainer = view.findViewById(R.id.layout_options_container);
        layoutDishExit = view.findViewById(R.id.layout_dish_exit);
        layoutDishwashingArea = view.findViewById(R.id.layout_dishwashing_area);
        layoutMappingTool = view.findViewById(R.id.layout_mapping_tool);
        
        // Initialize value TextViews
        tvMapSelectionValue = view.findViewById(R.id.tv_map_selection_value);
        tvDishExitValue = view.findViewById(R.id.tv_dish_exit_value);
        tvDishwashingAreaValue = view.findViewById(R.id.tv_dishwashing_area_value);
    }
    
    private void setupClickListeners() {
        // Map Selection - Must be selected first
        layoutMapSelection.setOnClickListener(v -> showMapSelectionDialog());
        
        // Dish Exit Selection
        layoutDishExit.setOnClickListener(v -> {
            if (selectedMap == null) {
                Toast.makeText(getContext(), "请先选择地图", Toast.LENGTH_SHORT).show();
                return;
            }
            showExitSelectionDialog("出菜口选择", selectedValue -> {
                selectedDishExit = selectedValue;
                tvDishExitValue.setText(selectedValue);
                tvDishExitValue.setTextColor(0xFF666666);
            });
        });
        
        // Dishwashing Area Selection
        layoutDishwashingArea.setOnClickListener(v -> {
            if (selectedMap == null) {
                Toast.makeText(getContext(), "请先选择地图", Toast.LENGTH_SHORT).show();
                return;
            }
            showExitSelectionDialog("洗碗间选择", selectedValue -> {
                selectedDishwashingArea = selectedValue;
                tvDishwashingAreaValue.setText(selectedValue);
                tvDishwashingAreaValue.setTextColor(0xFF666666);
            });
        });
        
        // Mapping Tool
        layoutMappingTool.setOnClickListener(v -> {
            Toast.makeText(getContext(), "打开建图工具", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to mapping tool screen
        });
    }
    
    /**
     * 显示地图选择对话框
     */
    private void showMapSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("选择地图");
        
        builder.setItems(availableMaps, (dialog, which) -> {
            selectedMap = availableMaps[which];
            tvMapSelectionValue.setText(selectedMap);
            tvMapSelectionValue.setTextColor(0xFF666666);
            
            // 加载该地图对应的出口数据
            loadExitsForMap(selectedMap);
            
            // 显示其他选项
            layoutOptionsContainer.setVisibility(View.VISIBLE);
            
            // 重置已选择的值
            resetSelections();
            
            Toast.makeText(getContext(), "已选择地图: " + selectedMap, Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    /**
     * 显示出口选择对话框
     */
    private void showExitSelectionDialog(String title, OnSelectionListener listener) {
        if (availableExits == null || availableExits.length == 0) {
            Toast.makeText(getContext(), "该地图暂无可用出口", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title);
        
        builder.setItems(availableExits, (dialog, which) -> {
            String selectedValue = availableExits[which];
            listener.onSelected(selectedValue);
            Toast.makeText(getContext(), "已选择: " + selectedValue, Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    /**
     * 根据选择的地图加载对应的出口数据
     */
    private void loadExitsForMap(String mapName) {
        // 模拟根据地图加载不同的出口
        // 实际应用中，这里应该从服务器或数据库获取数据
        availableExits = new String[]{
            "出餐口A", "出餐口B", "出餐口C", 
            "后厨入口", "员工通道", "主入口"
        };
    }
    
    /**
     * 重置所有选择
     */
    private void resetSelections() {
        selectedDishExit = null;
        selectedDishwashingArea = null;
        
        tvDishExitValue.setText("请选择");
        tvDishExitValue.setTextColor(0xFF999999);
        
        tvDishwashingAreaValue.setText("请选择");
        tvDishwashingAreaValue.setTextColor(0xFF999999);
    }
    
    /**
     * 选择监听器接口
     */
    private interface OnSelectionListener {
        void onSelected(String value);
    }
}
