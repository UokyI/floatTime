package com.floattime.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class CountdownActivity extends AppCompatActivity {

    public static final String EXTRA_CANCEL = "extra_cancel";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppLocale.wrap(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_CANCEL, false)) {
            // 取消信号：finish 已存在的实例并关闭
            finish();
            return;
        }

        setContentView(R.layout.dialog_countdown);

        EditText editTaskName = findViewById(R.id.editTaskName);
        EditText editMinutes = findViewById(R.id.editMinutes);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnStart = findViewById(R.id.btnStart);

        btnCancel.setOnClickListener(v -> finish());

        btnStart.setOnClickListener(v -> {
            String name = editTaskName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                editTaskName.setError(getString(R.string.error_task_name));
                return;
            }
            String minStr = editMinutes.getText().toString().trim();
            int minutes;
            try {
                minutes = Integer.parseInt(minStr);
            } catch (NumberFormatException e) {
                minutes = 5;
            }
            if (minutes <= 0) {
                editMinutes.setError(getString(R.string.error_minutes));
                return;
            }
            FloatingService.startCountdownFromActivity(this, name, minutes);
            finish();
        });
    }
}
