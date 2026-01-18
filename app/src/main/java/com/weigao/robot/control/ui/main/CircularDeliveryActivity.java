package com.weigao.robot.control.ui.main;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.weigao.robot.control.R;

public class CircularDeliveryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circular_delivery);

        String deliveryType = getIntent().getStringExtra("delivery_type");
        TextView titleText = findViewById(R.id.title_text);
        if (deliveryType != null) {
            titleText.setText(deliveryType);
        } else {
            titleText.setText("循环配送");
        }

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }
}
