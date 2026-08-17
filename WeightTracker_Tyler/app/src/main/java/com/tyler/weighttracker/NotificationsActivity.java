package com.tyler.weighttracker;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class NotificationsActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        Switch smsSwitch = findViewById(R.id.smsPermissionSwitch);
        Button saveButton = findViewById(R.id.savePermissionsButton);
        EditText goalInput = findViewById(R.id.goalWeightEditText);

        // Request SMS Permission
        smsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
                }
            }
        });

        // Save and trigger an SMS alert if a goal was set and permissions granted
        saveButton.setOnClickListener(v -> {
            String goalStr = goalInput.getText().toString().trim();
            String currentUser = getIntent().getStringExtra("USERNAME");

            if (!goalStr.isEmpty()) {
                // Save the goal to SharedPreferences, bound to this specific user
                SharedPreferences prefs = getSharedPreferences("WeightTrackerPrefs", MODE_PRIVATE);
                prefs.edit().putString(currentUser + "_GOAL", goalStr).apply();

                if (smsSwitch.isChecked() && checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        @SuppressWarnings("deprecation")
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage("15555215554", null, "Goal set! Target weight: " + goalStr + " lbs.", null, null);
                        Toast.makeText(this, "SMS Alert Sent", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "SMS failed to send", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            Intent intent = new Intent(NotificationsActivity.this, DashboardActivity.class);
            intent.putExtra("USERNAME", currentUser);
            startActivity(intent);
            finish();
        });
    }
}