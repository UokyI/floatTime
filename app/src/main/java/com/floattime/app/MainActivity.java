package com.floattime.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_OVERLAY = 1001;

    private DrawerLayout drawerLayout;
    private TextView titleBar;

    private Config cfg;
    private View pageHome, pageConfig, pageHistory, pageRemind;
    private HistoryAdapter historyAdapter;

    // 首页控件
    private Button btnToggle;
    private TextView textTip;

    // 配置页控件
    private View swatchIdle, swatchActive;
    private SeekBar seekSize;
    private TextView textSize;

    private int currentPage = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        titleBar = findViewById(R.id.titleBar);

        cfg = Config.load(this);

        ((Button) findViewById(R.id.menuHome)).setText(R.string.menu_home);
        ((Button) findViewById(R.id.menuConfig)).setText(R.string.menu_config);
        ((Button) findViewById(R.id.menuHistory)).setText(R.string.menu_history);
        ((Button) findViewById(R.id.menuRemind)).setText(R.string.menu_remind);
        ((Button) findViewById(R.id.menuAdvanced)).setText(R.string.menu_advanced);
        ((Button) findViewById(R.id.menuAbout)).setText(R.string.menu_about);

        findViewById(R.id.btnMenu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        findViewById(R.id.menuHome).setOnClickListener(v -> { showPage(0); drawerLayout.closeDrawers(); });
        findViewById(R.id.menuConfig).setOnClickListener(v -> { showPage(1); drawerLayout.closeDrawers(); });
        findViewById(R.id.menuHistory).setOnClickListener(v -> { showPage(2); drawerLayout.closeDrawers(); });
        findViewById(R.id.menuRemind).setOnClickListener(v -> { showPage(3); drawerLayout.closeDrawers(); });
        findViewById(R.id.menuAdvanced).setOnClickListener(v -> { drawerLayout.closeDrawers(); showAdvanced(); });
        findViewById(R.id.menuAbout).setOnClickListener(v -> { drawerLayout.closeDrawers(); showAbout(); });

        initPages();
        showPage(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyAdapter != null) historyAdapter.destroy();
    }

    private void initPages() {
        FrameLayout fc = findViewById(R.id.contentFrame);
        LayoutInflater inf = LayoutInflater.from(this);
        pageHome = inf.inflate(R.layout.page_home, fc, false);
        pageConfig = inf.inflate(R.layout.page_config, fc, false);
        pageHistory = inf.inflate(R.layout.page_history, fc, false);
        pageRemind = inf.inflate(R.layout.page_remind, fc, false);

        // 首页
        btnToggle = pageHome.findViewById(R.id.btnToggle);
        textTip = pageHome.findViewById(R.id.textTip);
        btnToggle.setOnClickListener(v -> {
            if (!canDrawOverlays()) requestOverlayPermission();
            else toggleService();
        });

        // 配置页
        swatchIdle = pageConfig.findViewById(R.id.swatchIdle);
        swatchActive = pageConfig.findViewById(R.id.swatchActive);
        seekSize = pageConfig.findViewById(R.id.seekSize);
        textSize = pageConfig.findViewById(R.id.textSize);

        pageConfig.findViewById(R.id.btnIdleColor).setOnClickListener(v -> {
            new ColorPickerDialog(this, cfg.idleC, c -> {
                int[] sh = deriveShades(c);
                cfg.idleC = sh[0]; cfg.idleD = sh[1]; cfg.idleG = sh[2];
                cfg.save(this);
                updateSwatches();
                notifyServiceConfigChanged();
            }).show();
        });
        pageConfig.findViewById(R.id.btnActiveColor).setOnClickListener(v -> {
            new ColorPickerDialog(this, cfg.actC, c -> {
                int[] sh = deriveShades(c);
                cfg.actC = sh[0]; cfg.actD = sh[1]; cfg.actG = sh[2];
                cfg.save(this);
                updateSwatches();
                notifyServiceConfigChanged();
            }).show();
        });
        seekSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                cfg.sizeDp = Math.max(80, p);
                textSize.setText(cfg.sizeDp + "dp");
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {
                cfg.save(MainActivity.this);
                notifyServiceConfigChanged();
            }
        });

        updateSwatches();
        seekSize.setProgress(cfg.sizeDp);
        textSize.setText(cfg.sizeDp + "dp");

        // 提醒页
        RadioGroup remindGroup = pageRemind.findViewById(R.id.remindGroup);
        int remindId;
        switch (cfg.remind) {
            case 0: remindId = R.id.remindSilent; break;
            case 2: remindId = R.id.remindLight;  break;
            case 3: remindId = R.id.remindStorm;  break;
            default: remindId = R.id.remindVibrate; break;
        }
        remindGroup.check(remindId);
        remindGroup.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.remindSilent) cfg.remind = 0;
            else if (id == R.id.remindVibrate) cfg.remind = 1;
            else if (id == R.id.remindLight) cfg.remind = 2;
            else if (id == R.id.remindStorm) cfg.remind = 3;
            cfg.save(MainActivity.this);
            notifyServiceConfigChanged();
            // 点击预览
            previewRemind(cfg.remind);
        });
    }

    private void previewRemind(int remind) {
        switch (remind) {
            case 0: break;
            case 2: SoundUtil.play(0); break;
            case 3:
                // 闪动预览需要悬浮球在显示
                if (FloatingService.isRunning()) {
                    Intent intent = new Intent(this, FloatingService.class);
                    intent.setAction(FloatingService.ACTION_STORM);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
                    else startService(intent);
                }
                break;
            default: vibrateOnce(); break;
        }
    }

    private void vibrateOnce() {
        try {
            android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                v.cancel();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(android.os.VibrationEffect.createOneShot(300, 150));
                } else {
                    long[] p = {0, 300};
                    v.vibrate(p, -1);
                }
            }
        } catch (Exception ignored) {}
    }

    private void showPage(int idx) {
        currentPage = idx;
        FrameLayout fc = findViewById(R.id.contentFrame);
        fc.removeAllViews();
        View page;
        int titleRes;
        if (idx == 0) { page = pageHome; titleRes = R.string.menu_home; }
        else if (idx == 1) { page = pageConfig; titleRes = R.string.menu_config; }
        else if (idx == 2) { page = pageHistory; titleRes = R.string.menu_history; refreshHistory(); }
        else { page = pageRemind; titleRes = R.string.menu_remind; }
        fc.addView(page);
        titleBar.setText(titleRes);
    }

    private void updateSwatches() {
        swatchIdle.setBackgroundColor(cfg.idleC);
        swatchActive.setBackgroundColor(cfg.actC);
    }

    private int[] deriveShades(int core) {
        float[] hsv = new float[3];
        Color.colorToHSV(core, hsv);
        float h = hsv[0], s = hsv[1], v = hsv[2];
        int deep = Color.HSVToColor(new float[]{h, Math.min(1f, s * 1.15f), Math.max(0.55f, v * 0.82f)});
        int glow = Color.HSVToColor(new float[]{h, Math.max(0.25f, s * 0.85f), Math.min(1f, v * 1.12f)});
        return new int[]{core, deep, glow};
    }

    private void refreshHistory() {
        ListView list = pageHistory.findViewById(R.id.historyList);
        TextView empty = pageHistory.findViewById(R.id.historyEmpty);
        if (historyAdapter == null) {
            historyAdapter = new HistoryAdapter(this);
            list.setAdapter(historyAdapter);
        }
        if (historyAdapter.getCount() == 0) {
            list.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
        } else {
            list.setVisibility(View.VISIBLE);
            empty.setVisibility(View.GONE);
        }
    }

    private void showAdvanced() {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_config);
        TextView configText = d.findViewById(R.id.configText);
        configText.setText(cfg.toPrettyString());

        d.findViewById(R.id.btnCopy).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("config", cfg.toPrettyString()));
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
        });
        d.findViewById(R.id.btnPaste).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            CharSequence clip = cm.getPrimaryClip() != null ? cm.getPrimaryClip().getItemAt(0).getText() : null;
            if (clip == null) return;
            try {
                Config nc = Config.fromPrettyString(clip.toString());
                cfg = nc;
                cfg.save(this);
                configText.setText(cfg.toPrettyString());
                updateSwatches();
                seekSize.setProgress(cfg.sizeDp);
                textSize.setText(cfg.sizeDp + "dp");
                notifyServiceConfigChanged();
                Toast.makeText(this, R.string.pasted, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, R.string.paste_invalid, Toast.LENGTH_SHORT).show();
            }
        });
        d.findViewById(R.id.btnConfigClose).setOnClickListener(v -> d.dismiss());

        d.findViewById(R.id.btnReset).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.adv_reset_confirm)
                    .setPositiveButton(R.string.btn_confirm, (a, b) -> {
                        cfg.reset();
                        cfg.save(this);
                        configText.setText(cfg.toPrettyString());
                        updateSwatches();
                        seekSize.setProgress(cfg.sizeDp);
                        textSize.setText(cfg.sizeDp + "dp");
                        notifyServiceConfigChanged();
                        Toast.makeText(this, R.string.pasted, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        });
        d.show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_content)
                .setPositiveButton(R.string.btn_confirm, null)
                .show();
    }

    private void notifyServiceConfigChanged() {
        Intent intent = new Intent(this, FloatingService.class);
        intent.setAction(FloatingService.ACTION_CONFIG_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    private void refreshUi() {
        boolean running = FloatingService.isRunning();
        if (canDrawOverlays()) {
            textTip.setVisibility(View.GONE);
            btnToggle.setEnabled(true);
            btnToggle.setText(running ? R.string.stop_float : R.string.start_float);
        } else {
            textTip.setVisibility(View.VISIBLE);
            btnToggle.setEnabled(true);
            btnToggle.setText(R.string.open_permission);
        }
    }

    private boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) return Settings.canDrawOverlays(this);
        return true;
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) refreshUi();
    }

    private void toggleService() {
        Intent intent = new Intent(this, FloatingService.class);
        if (FloatingService.isRunning()) stopService(intent);
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
            else startService(intent);
        }
        refreshUi();
    }
}
