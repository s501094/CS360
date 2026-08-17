package com.tyler.weighttracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private static final String PREF_NAME = "WeightTrackerPrefs";
    private static final String KEY_USER = "USERNAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedUser = prefs.getString(KEY_USER, null);

        if (savedUser != null) {
            // Auto-login straight to Dashboard
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            intent.putExtra("USERNAME", savedUser);
            startActivity(intent);
            finish();
            return; // Stop rendering the login screen
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText usernameInput = findViewById(R.id.usernameEditText);
        EditText passwordInput = findViewById(R.id.passwordEditText);
        Button loginButton = findViewById(R.id.loginButton);
        Button createAccountButton = findViewById(R.id.createAccountButton);

        // login button verification
        loginButton.setOnClickListener(v -> {
            String user = usernameInput.getText().toString().trim();
            String pass = passwordInput.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(MainActivity.this, "Fill out all fields", Toast.LENGTH_SHORT).show();
            } else if (dbHelper.checkUser(user, pass)) {
                // Save session memory
                prefs.edit().putString(KEY_USER, user).apply();

                Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                intent.putExtra("USERNAME", user);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(MainActivity.this, "Invalid credentials or account doesn't exist", Toast.LENGTH_SHORT).show();
            }
        });

        // create account button action
        createAccountButton.setOnClickListener(v -> {
            String user = usernameInput.getText().toString().trim();
            String pass = passwordInput.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(MainActivity.this, "Enter username and password to create account", Toast.LENGTH_SHORT).show();
            } else {
                boolean success = dbHelper.registerUser(user, pass);
                if (success) {
                    // Save session memory
                    prefs.edit().putString(KEY_USER, user).apply();

                    Toast.makeText(MainActivity.this, "Account created! Now configuring settings...", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, NotificationsActivity.class);
                    intent.putExtra("USERNAME", user);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "Error creating account", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}