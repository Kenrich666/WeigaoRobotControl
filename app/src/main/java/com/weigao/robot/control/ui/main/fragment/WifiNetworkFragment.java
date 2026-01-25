package com.weigao.robot.control.ui.main.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.weigao.robot.control.R;
//暂时不开发
public class WifiNetworkFragment extends Fragment {

    private SwitchCompat switchWifi;
    private LinearLayout layoutCurrentWifi;
    private TextView tvCurrentWifiName;
    private TextView tvConnectionStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wifi_network, container, false);
        
        initViews(view);
        setupWifiToggle();
        setupWifiItemClickListeners(view);
        
        return view;
    }

    private void initViews(View view) {
        switchWifi = view.findViewById(R.id.switch_wifi);
        layoutCurrentWifi = view.findViewById(R.id.layout_current_wifi);
        tvCurrentWifiName = view.findViewById(R.id.tv_current_wifi_name);
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status);
    }

    private void setupWifiToggle() {
        switchWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                layoutCurrentWifi.setVisibility(View.VISIBLE);
                findViewById(R.id.wifi_item_1).setVisibility(View.VISIBLE);
                findViewById(R.id.wifi_item_2).setVisibility(View.VISIBLE);
                findViewById(R.id.wifi_item_3).setVisibility(View.VISIBLE);
                findViewById(R.id.wifi_item_4).setVisibility(View.VISIBLE);
                findViewById(R.id.wifi_item_5).setVisibility(View.VISIBLE);
                findViewById(R.id.wifi_item_6).setVisibility(View.VISIBLE);
                findViewById(R.id.wifi_item_7).setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "WiFi已开启", Toast.LENGTH_SHORT).show();
            } else {
                layoutCurrentWifi.setVisibility(View.GONE);
                findViewById(R.id.wifi_item_1).setVisibility(View.GONE);
                findViewById(R.id.wifi_item_2).setVisibility(View.GONE);
                findViewById(R.id.wifi_item_3).setVisibility(View.GONE);
                findViewById(R.id.wifi_item_4).setVisibility(View.GONE);
                findViewById(R.id.wifi_item_5).setVisibility(View.GONE);
                findViewById(R.id.wifi_item_6).setVisibility(View.GONE);
                findViewById(R.id.wifi_item_7).setVisibility(View.GONE);
                Toast.makeText(getContext(), "WiFi已关闭", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupWifiItemClickListeners(View view) {
        // WiFi Item 1: HUAWEI-YP
        view.findViewById(R.id.wifi_item_1).setOnClickListener(v -> 
            showPasswordDialog("HUAWEI-YP")
        );

        // WiFi Item 2: IGIP
        view.findViewById(R.id.wifi_item_2).setOnClickListener(v -> 
            showPasswordDialog("IGIP")
        );

        // WiFi Item 3: eduroam
        view.findViewById(R.id.wifi_item_3).setOnClickListener(v -> 
            showPasswordDialog("eduroam")
        );

        // WiFi Item 4: sdu_guest
        view.findViewById(R.id.wifi_item_4).setOnClickListener(v -> 
            showPasswordDialog("sdu_guest")
        );

        // WiFi Item 5: sdu_net
        view.findViewById(R.id.wifi_item_5).setOnClickListener(v -> 
            showPasswordDialog("sdu_net")
        );

        // WiFi Item 6: HP-Print-CC-LaserJet Pro MFP
        view.findViewById(R.id.wifi_item_6).setOnClickListener(v -> 
            showPasswordDialog("HP-Print-CC-LaserJet Pro MFP")
        );

        // WiFi Item 7: HUAWEI-G326dyj
        view.findViewById(R.id.wifi_item_7).setOnClickListener(v -> 
            showPasswordDialog("HUAWEI-G326dyj")
        );
    }

    private void showPasswordDialog(String wifiName) {
        if (getContext() == null) return;

        // Create password input dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("连接到 " + wifiName);
        
        // Create EditText for password input
        final EditText input = new EditText(getContext());
        input.setHint("请输入密码");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        
        // Add padding to EditText
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(50, 20, 50, 20);
        input.setLayoutParams(params);
        container.addView(input);
        
        builder.setView(container);

        // Set up buttons
        builder.setPositiveButton("连接", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(getContext(), "密码不能为空", Toast.LENGTH_SHORT).show();
            } else {
                connectToWifi(wifiName, password);
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> {
            dialog.cancel();
        });

        builder.show();
    }

    private void connectToWifi(String wifiName, String password) {
        // Simulate WiFi connection
        Toast.makeText(getContext(), "正在连接到 " + wifiName + "...", Toast.LENGTH_SHORT).show();
        
        // Update the current connected WiFi display
        tvCurrentWifiName.setText(wifiName);
        tvConnectionStatus.setText("连接成功");
        
        // Show success message
        Toast.makeText(getContext(), "已成功连接到 " + wifiName, Toast.LENGTH_LONG).show();
    }

    private View findViewById(int id) {
        return getView() != null ? getView().findViewById(id) : null;
    }
}
