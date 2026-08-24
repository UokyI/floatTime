package com.floattime.app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;

public class ColorPickerDialog extends Dialog {

    public interface OnColorPicked {
        void onPicked(int color);
    }

    private final int initialColor;
    private final OnColorPicked callback;

    public ColorPickerDialog(@NonNull Context c, int initialColor, OnColorPicked callback) {
        super(c);
        this.initialColor = initialColor;
        this.callback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_color_picker);

        HsvColorPickerView picker = findViewById(R.id.hsvPicker);
        picker.setColor(initialColor);

        findViewById(R.id.btnColorCancel).setOnClickListener(v -> dismiss());
        findViewById(R.id.btnColorConfirm).setOnClickListener(v -> {
            if (callback != null) callback.onPicked(picker.getColor());
            dismiss();
        });
    }
}
