package com.floattime.app;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;

public class CountdownDialog extends Dialog {

    public interface OnStartListener {
        void onStart(String taskName, int minutes);
    }

    private final OnStartListener listener;
    private TextInputEditText editTaskName;
    private TextInputEditText editMinutes;

    public CountdownDialog(@NonNull Context context, @Nullable OnStartListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_countdown);

        // Service 里没有 Activity token，必须用 overlay 类型窗口，否则 show 时抛 BadTokenException
        Window window = getWindow();
        if (window != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            } else {
                window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            }
            window.setDimAmount(0.4f);
        }

        editTaskName = findViewById(R.id.editTaskName);
        editMinutes = findViewById(R.id.editMinutes);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnStart = findViewById(R.id.btnStart);

        btnCancel.setOnClickListener(v -> dismiss());

        btnStart.setOnClickListener(v -> {
            String name = getText(editTaskName).trim();
            if (TextUtils.isEmpty(name)) {
                editTaskName.setError(getContext().getString(R.string.error_task_name));
                return;
            }
            String minStr = getText(editMinutes).trim();
            int minutes;
            try {
                minutes = Integer.parseInt(minStr);
            } catch (NumberFormatException e) {
                minutes = 5;
            }
            if (minutes <= 0) {
                editMinutes.setError(getContext().getString(R.string.error_minutes));
                return;
            }
            if (listener != null) {
                listener.onStart(name, minutes);
            }
            dismiss();
        });
    }

    private String getText(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString() : "";
    }
}
