package com.weigao.robot.control.ui.main.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.weigao.robot.control.R;

public class DeliveryModeFragment extends Fragment {

    private SeekBar seekbarMealPickupDuration;
    private TextView tvMealPickupDuration;
    private SeekBar seekbarWalkingPauseDuration;
    private TextView tvWalkingPauseDuration;
    private SeekBar seekbarDeliverySpeed;
    private TextView tvDeliverySpeed;
    private SeekBar seekbarReturnSpeed;
    private TextView tvReturnSpeed;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery_mode, container, false);
        
        initViews(view);
        setupListeners();
        
        return view;
    }

    private void initViews(View view) {
        seekbarMealPickupDuration = view.findViewById(R.id.seekbar_meal_pickup_duration);
        tvMealPickupDuration = view.findViewById(R.id.tv_meal_pickup_duration);
        
        seekbarWalkingPauseDuration = view.findViewById(R.id.seekbar_walking_pause_duration);
        tvWalkingPauseDuration = view.findViewById(R.id.tv_walking_pause_duration);
        
        seekbarDeliverySpeed = view.findViewById(R.id.seekbar_delivery_speed);
        tvDeliverySpeed = view.findViewById(R.id.tv_delivery_speed);
        
        seekbarReturnSpeed = view.findViewById(R.id.seekbar_return_speed);
        tvReturnSpeed = view.findViewById(R.id.tv_return_speed);
    }

    private void setupListeners() {
        // 取餐停留时长: 0-300秒，步进1秒
        seekbarMealPickupDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMealPickupDuration.setText(progress + "秒");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 行走暂停时长: 1-30秒，步进1秒
        seekbarWalkingPauseDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // progress范围是0-29，实际值是1-30
                int actualValue = progress + 1;
                tvWalkingPauseDuration.setText(actualValue + "秒");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 送餐速度: 10-80 cm/s，步进1 cm/s
        seekbarDeliverySpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // progress范围是0-70，实际值是10-80
                int actualValue = progress + 10;
                tvDeliverySpeed.setText(actualValue + "cm/s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 返航速度: 10-80 cm/s，步进1 cm/s
        seekbarReturnSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // progress范围是0-70，实际值是10-80
                int actualValue = progress + 10;
                tvReturnSpeed.setText(actualValue + "cm/s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
}
