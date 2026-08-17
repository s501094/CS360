package com.tyler.weighttracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private static final SimpleDateFormat DB_DATE =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    DatabaseHelper dbHelper;
    ListView weightListView;
    EditText weightInput;
    TextView goalTextView;
    TextView goalProgressTextView;
    TextView currentWeightTextView;
    TextView trendPill;
    TextView entryCountTextView;
    ProgressBar goalProgressBar;
    View emptyStateView;
    ImageButton addButton;
    ImageButton logoutButton;
    ImageButton editGoalButton;
    String currentUser;
    SharedPreferences prefs;

    /** Newest first — the order the list renders in. */
    private final List<Entry> entries = new ArrayList<>();
    private WeightAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("WeightTrackerPrefs", MODE_PRIVATE);

        weightListView = findViewById(R.id.weightListView);
        weightInput = findViewById(R.id.weightInputEditText);
        goalTextView = findViewById(R.id.goalWeightTextView);
        goalProgressTextView = findViewById(R.id.goalProgressTextView);
        currentWeightTextView = findViewById(R.id.currentWeightTextView);
        trendPill = findViewById(R.id.trendPill);
        entryCountTextView = findViewById(R.id.entryCountTextView);
        goalProgressBar = findViewById(R.id.goalProgressBar);
        emptyStateView = findViewById(R.id.emptyStateView);
        addButton = findViewById(R.id.addWeightButton);
        logoutButton = findViewById(R.id.logoutButton);
        editGoalButton = findViewById(R.id.editGoalButton);

        currentUser = getIntent().getStringExtra("USERNAME");
        if (currentUser == null) {
            currentUser = "default_user";
        }

        adapter = new WeightAdapter();
        weightListView.setAdapter(adapter);
        weightListView.setEmptyView(emptyStateView);

        loadWeightLogs();

        // Logout
        logoutButton.setOnClickListener(v -> {
            prefs.edit().remove("USERNAME").apply(); // Keep goals, but clear active session
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Edit Target Goal
        editGoalButton.setOnClickListener(v -> {
            final EditText input = dialogInput("Target weight (lbs)");
            input.setText(prefs.getString(currentUser + "_GOAL", ""));
            input.setSelection(input.getText().length());

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Target Goal")
                    .setView(wrap(input))
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newGoal = input.getText().toString().trim();
                        if (parseWeight(newGoal) == null) {
                            return;
                        }
                        prefs.edit().putString(currentUser + "_GOAL", newGoal).apply();
                        renderStats();
                        Toast.makeText(this, "Target goal updated", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                    .show();
        });

        // Create Entry
        addButton.setOnClickListener(v -> addEntry());
        weightInput.setOnEditorActionListener((v, actionId, event) -> {
            addEntry();
            return true;
        });

        // Update & Delete Entry
        weightListView.setOnItemClickListener((parent, view, position, id) -> {
            Entry entry = entries.get(position);

            final EditText input = dialogInput("New weight (lbs)");
            input.setText(trimZero(entry.weight));
            input.setSelection(input.getText().length());

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Modify Entry")
                    .setView(wrap(input))
                    .setPositiveButton("Update", (dialog, which) -> {
                        Double newVal = parseWeight(input.getText().toString().trim());
                        if (newVal == null) {
                            return;
                        }
                        dbHelper.updateWeight(entry.id, newVal, DB_DATE.format(new Date()));
                        loadWeightLogs();
                        Toast.makeText(this, "Entry updated", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Delete", (dialog, which) -> {
                        dbHelper.deleteWeight(entry.id);
                        loadWeightLogs();
                        Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNeutralButton("Cancel", (dialog, which) -> dialog.cancel())
                    .show();
        });
    }

    private void addEntry() {
        Double weightVal = parseWeight(weightInput.getText().toString().trim());
        if (weightVal == null) {
            return;
        }
        dbHelper.addWeight(currentUser, weightVal, DB_DATE.format(new Date()));
        weightInput.setText("");
        loadWeightLogs();

        // Target Goal SMS Logic
        Double goal = parseWeight(prefs.getString(currentUser + "_GOAL", null));
        if (goal != null && weightVal <= goal) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                try {
                    @SuppressWarnings("deprecation")
                    SmsManager smsManager = SmsManager.getDefault();
                    // Added '+' and country code to fix silent emulator drops
                    smsManager.sendTextMessage("+15555215554", null, "Congrats! You hit your goal weight of " + trimZero(goal) + " lbs!", null, null);
                    Toast.makeText(this, "Goal Reached! SMS Sent", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void loadWeightLogs() {
        entries.clear();
        try (Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT id, weight, date FROM weights WHERE username = ? ORDER BY date DESC, id DESC",
                new String[]{currentUser})) {
            while (cursor.moveToNext()) {
                entries.add(new Entry(cursor.getInt(0), cursor.getDouble(1), cursor.getString(2)));
            }
        }
        adapter.notifyDataSetChanged();
        renderStats();
    }

    /** Repaints the hero card: current weight, trend vs. the previous entry, goal progress. */
    @SuppressLint("SetTextI18n")
    private void renderStats() {
        int count = entries.size();
        entryCountTextView.setText(count == 0
                ? "No entries yet"
                : count + (count == 1 ? " entry logged" : " entries logged"));

        if (count == 0) {
            currentWeightTextView.setText(R.string.dash_placeholder);
            trendPill.setVisibility(View.GONE);
        } else {
            double current = entries.get(0).weight;
            currentWeightTextView.setText(trimZero(current));

            if (count > 1) {
                double delta = current - entries.get(1).weight;
                trendPill.setVisibility(View.VISIBLE);
                paintDelta(trendPill, delta, true);
            } else {
                trendPill.setVisibility(View.GONE);
            }
        }

        Double goal = parseWeight(prefs.getString(currentUser + "_GOAL", null));
        if (goal == null) {
            goalTextView.setText(R.string.no_goal_prompt);
            goalTextView.setTextColor(color(R.color.ctp_overlay1));
            goalProgressTextView.setVisibility(View.GONE);
            goalProgressBar.setProgress(0);
            return;
        }

        goalTextView.setText("Goal  ·  " + trimZero(goal) + " lbs");
        goalTextView.setTextColor(color(R.color.ctp_green));

        if (count == 0) {
            goalProgressTextView.setVisibility(View.GONE);
            goalProgressBar.setProgress(0);
            return;
        }

        // Progress is measured from the very first log to the goal
        double start = entries.get(count - 1).weight;
        double current = entries.get(0).weight;
        double span = Math.abs(goal - start);
        int pct = span < 0.0001
                ? 100
                : (int) Math.round(Math.min(1, Math.max(0, Math.abs(current - start) / span)) * 100);

        goalProgressTextView.setVisibility(View.VISIBLE);
        goalProgressTextView.setText(pct + "%");
        goalProgressBar.setProgress(pct);
    }

    /** Colors a pill/label by direction: losing reads green, gaining reads peach. */
    @SuppressLint("SetTextI18n")
    private void paintDelta(TextView view, double delta, boolean withUnit) {
        int tint;
        int arrow;
        if (delta < -0.0001) {
            tint = color(R.color.ctp_green);
            arrow = R.drawable.ic_trend_down;
        } else if (delta > 0.0001) {
            tint = color(R.color.ctp_peach);
            arrow = R.drawable.ic_trend_up;
        } else {
            tint = color(R.color.ctp_overlay2);
            arrow = 0;
        }

        String label = String.format(Locale.getDefault(), "%.1f", Math.abs(delta));
        view.setText(withUnit ? label + " lbs" : label);
        view.setTextColor(tint);
        view.setBackgroundTintList(ColorStateList.valueOf((tint & 0x00FFFFFF) | 0x2E000000));
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(arrow, 0, 0, 0);

        // Fixed: Use TextViewCompat for backward compatibility
        TextViewCompat.setCompoundDrawableTintList(view, ColorStateList.valueOf(tint));
    }

    private int color(int resId) {
        return ContextCompat.getColor(this, resId);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private EditText dialogInput(String hint) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(hint);
        input.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        input.setTextColor(color(R.color.ctp_text));
        input.setHintTextColor(color(R.color.ctp_overlay0));
        input.setPadding(dp(18), dp(14), dp(18), dp(14));
        input.setGravity(Gravity.CENTER_VERTICAL);
        return input;
    }

    /** AlertDialog custom views sit flush against the edges without a padded wrapper. */
    private View wrap(View child) {
        FrameLayout container = new FrameLayout(this);
        container.setPadding(dp(24), dp(8), dp(24), 0);
        container.addView(child, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return container;
    }

    private static Double parseWeight(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 210.0 -> "210", 210.5 -> "210.5" */
    private static String trimZero(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    private static String prettyDate(String raw) {
        try {
            Date parsed = DB_DATE.parse(raw);
            if (parsed != null) {
                // Fixed: Instantiate Locale locally to clear static field warning
                return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(parsed);
            }
        } catch (ParseException ignored) {
            // Fall through to the raw string for anything not in yyyy-MM-dd.
        }
        return raw;
    }

    private static class Entry {
        final int id;
        final double weight;
        final String date;

        Entry(int id, double weight, String date) {
            this.id = id;
            this.weight = weight;
            this.date = date;
        }
    }

    private class WeightAdapter extends ArrayAdapter<Entry> {
        WeightAdapter() {
            super(DashboardActivity.this, R.layout.item_weight_log, entries);
        }

        @SuppressLint("SetTextI18n")
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_weight_log, parent, false);
            }

            Entry entry = entries.get(position);
            ((TextView) row.findViewById(R.id.rowWeight)).setText(trimZero(entry.weight));
            ((TextView) row.findViewById(R.id.rowDate)).setText(prettyDate(entry.date));

            TextView delta = row.findViewById(R.id.rowDelta);
            View accent = row.findViewById(R.id.rowAccent);

            // Entries are newest-first, so the comparison point is the next one down.
            boolean hasPrevious = position + 1 < entries.size();
            if (hasPrevious) {
                double diff = entry.weight - entries.get(position + 1).weight;
                delta.setVisibility(View.VISIBLE);
                paintDelta(delta, diff, false);
                accent.setBackgroundTintList(ColorStateList.valueOf(
                        diff < -0.0001 ? color(R.color.ctp_green)
                                : diff > 0.0001 ? color(R.color.ctp_peach)
                                : color(R.color.ctp_surface2)));
            } else {
                delta.setVisibility(View.GONE);
                accent.setBackgroundTintList(ColorStateList.valueOf(color(R.color.ctp_mauve)));
            }

            return row;
        }
    }
}