package com.tyler.SayHello;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private EditText nameText;
    private TextView textGreeting;
    private Button buttonSayHello;

    // SensorManager variables
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private TextView sensorValueText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Link your XML elements to Java variables
        nameText = findViewById(R.id.nameText);
        textGreeting = findViewById(R.id.textGreeting);
        buttonSayHello = findViewById(R.id.buttonSayHello);
        sensorValueText = findViewById(R.id.sensorValueText);

        // 1. Start with the button disabled
        buttonSayHello.setEnabled(false);

        // 2. Dynamically enable/disable button based on text input
        nameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    buttonSayHello.setEnabled(false);
                } else {
                    buttonSayHello.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 3. Initialize SensorManager and target the light sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        if (lightSensor == null) {
            sensorValueText.setText("Light sensor not available.");
        }
    }

    // 4. Public function triggered by the XML android:onClick="SayHello"
    public void SayHello(View view) {
        String enteredName = nameText.getText().toString().trim();

        if (!TextUtils.isEmpty(enteredName)) {
            textGreeting.setText("Hello " + enteredName);
        } else {
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float currentValue = event.values[0];
            sensorValueText.setText("Light Level: " + currentValue + " lx");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Required interface callback
    }
}