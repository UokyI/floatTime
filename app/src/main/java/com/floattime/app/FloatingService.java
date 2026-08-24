package com.floattime.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class FloatingService extends Service {

    private static final String CHANNEL_ID = "float_time_channel";
    private static final int NOTI_ID = 0x77;
    private static volatile boolean running = false;

    public static final String ACTION_START_COUNTDOWN = "com.floattime.app.START_COUNTDOWN";
    public static final String ACTION_CONFIG_CHANGED = "com.floattime.app.CONFIG_CHANGED";
    public static final String ACTION_STORM = "com.floattime.app.STORM";
    public static final String EXTRA_TASK = "extra_task";
    public static final String EXTRA_MINUTES = "extra_minutes";

    private Config cfg;
    private History.Item pendingHistoryItem;

    public static boolean isRunning() {
        return running;
    }

    private WindowManager windowManager;
    private View rootView;
    private LiquidFloatingView liquidView;

    private WindowManager.LayoutParams layoutParams;
    private CountDownTimer countDownTimer;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long remainingMillis = 0;
    private String currentTask = "";
    private boolean counting = false;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        createNotificationChannel();
        startForeground(NOTI_ID, buildNotification("悬浮倒计时已开启"));
        showFloatingView();
    }

    private void applyConfig() {
        cfg = Config.load(this);
        if (liquidView != null) {
            liquidView.setIdleColors(cfg.idleC, cfg.idleD, cfg.idleG);
            liquidView.setActiveColors(cfg.actC, cfg.actD, cfg.actG);
        }
        if (layoutParams != null) {
            int size = dp(cfg.sizeDp);
            if (layoutParams.width != size) {
                layoutParams.width = size;
                layoutParams.height = size;
                try {
                    windowManager.updateViewLayout(rootView, layoutParams);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_START_COUNTDOWN.equals(intent.getAction())) {
                String task = intent.getStringExtra(EXTRA_TASK);
                int minutes = intent.getIntExtra(EXTRA_MINUTES, 5);
                if (task != null && minutes > 0) startCountdown(task, minutes);
            } else if (ACTION_CONFIG_CHANGED.equals(intent.getAction())) {
                applyConfig();
            } else if (ACTION_STORM.equals(intent.getAction())) {
                if (liquidView != null) liquidView.startStorm();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        removeSettingDialog();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (rootView != null && windowManager != null) {
            try {
                windowManager.removeView(rootView);
            } catch (Exception ignored) {
            }
            rootView = null;
        }
    }

    private void createNotificationChannel() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.float_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.float_channel_desc));
            nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private int layoutType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void showFloatingView() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        rootView = LayoutInflater.from(this).inflate(R.layout.view_floating, null, false);
        liquidView = rootView.findViewById(R.id.liquidView);
        cfg = Config.load(this);
        liquidView.setIdleColors(cfg.idleC, cfg.idleD, cfg.idleG);
        liquidView.setActiveColors(cfg.actC, cfg.actD, cfg.actG);

        int size = dp(cfg.sizeDp);
        layoutParams = new WindowManager.LayoutParams(
                size,
                size,
                layoutType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = 20;
        layoutParams.y = 200;

        setupTouchAndDrag();
        setupLongPress();

        try {
            windowManager.addView(rootView, layoutParams);
        } catch (Exception e) {
            stopSelf();
        }
    }

    private void setupTouchAndDrag() {
        rootView.setOnTouchListener(new View.OnTouchListener() {
            int initialX, initialY;
            float touchX, touchY;
            boolean moved;
            Runnable longPressRunnable;
            boolean longPressFired;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = layoutParams.x;
                        initialY = layoutParams.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        moved = false;
                        longPressFired = false;
                        longPressRunnable = () -> {
                            if (!moved) {
                                longPressFired = true;
                                v.performLongClick();
                            }
                        };
                        handler.postDelayed(longPressRunnable, 600);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - touchX;
                        float dy = event.getRawY() - touchY;
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            moved = true;
                            if (longPressRunnable != null) {
                                handler.removeCallbacks(longPressRunnable);
                            }
                        }
                        layoutParams.x = initialX + (int) dx;
                        layoutParams.y = initialY + (int) dy;
                        try {
                            windowManager.updateViewLayout(rootView, layoutParams);
                        } catch (Exception ignored) {
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (longPressRunnable != null) {
                            handler.removeCallbacks(longPressRunnable);
                        }
                        if (moved) {
                            snapToEdge();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void snapToEdge() {
        int screenW = Resources.getSystem().getDisplayMetrics().widthPixels;
        int viewW = rootView.getWidth();
        int targetX;
        // 判断当前中心点更靠近左边缘还是右边缘
        int centerX = layoutParams.x + viewW / 2;
        if (centerX < screenW / 2) {
            targetX = 0;           // 吸附左边
        } else {
            targetX = screenW - viewW;  // 吸附右边
        }
        animateX(layoutParams.x, targetX);
    }

    private void animateX(int fromX, int toX) {
        final int startX = fromX;
        final int endX = toX;
        final long duration = 250;
        final long startT = System.currentTimeMillis();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startT;
                float t = Math.min(1f, elapsed / (float) duration);
                // ease-out
                float eased = 1f - (1f - t) * (1f - t);
                int cur = startX + (int) ((endX - startX) * eased);
                layoutParams.x = cur;
                try {
                    windowManager.updateViewLayout(rootView, layoutParams);
                } catch (Exception ignored) {
                }
                if (t < 1f) {
                    handler.post(this);
                }
            }
        });
    }

    private void setupLongPress() {
        rootView.setOnLongClickListener(v -> {
            if (counting) {
                stopCountdown();
                SoundUtil.play(2);
                return true;
            }
            if (dialogShowing) {
                removeSettingDialog();
                SoundUtil.play(2);
                return true;
            }
            showSettingDialog();
            SoundUtil.play(0);
            return true;
        });
    }

    private boolean dialogShowing = false;
    private View dialogView;

    private void showSettingDialog() {
        if (dialogShowing || dialogView != null) return;
        try {
            dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_countdown, null, false);

            EditText editTaskName = dialogView.findViewById(R.id.editTaskName);
            EditText editMinutes = dialogView.findViewById(R.id.editMinutes);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnStart = dialogView.findViewById(R.id.btnStart);

            btnCancel.setOnClickListener(v -> {
                removeSettingDialog();
                SoundUtil.play(2);
            });
            btnStart.setOnClickListener(v -> {
                String name = editTaskName.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    editTaskName.setError("请输入任务名称");
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
                    editMinutes.setError("分钟数需大于0");
                    return;
                }
                removeSettingDialog();
                SoundUtil.play(1);
                startCountdown(name, minutes);
            });

            WindowManager.LayoutParams dlgParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType(),
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);
            dlgParams.gravity = Gravity.CENTER;
            dlgParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

            // 弹窗显示时，长按任意位置（含外部）取消弹窗
            dialogView.setLongClickable(true);
            dialogView.setOnLongClickListener(v -> {
                removeSettingDialog();
                SoundUtil.play(2);
                return true;
            });

            windowManager.addView(dialogView, dlgParams);
            dialogShowing = true;
        } catch (Exception e) {
            e.printStackTrace();
            dialogView = null;
            dialogShowing = false;
        }
    }

    private void removeSettingDialog() {
        if (dialogView != null) {
            try {
                windowManager.removeView(dialogView);
            } catch (Exception ignored) {
            }
            dialogView = null;
        }
        dialogShowing = false;
    }

    public static void startCountdownFromActivity(Context context, String taskName, int minutes) {
        Intent intent = new Intent(context, FloatingService.class);
        intent.setAction(ACTION_START_COUNTDOWN);
        intent.putExtra(EXTRA_TASK, taskName);
        intent.putExtra(EXTRA_MINUTES, minutes);
        context.startService(intent);
    }

    private void startCountdown(String taskName, int minutes) {
        currentTask = taskName;
        remainingMillis = minutes * 60_000L;
        counting = true;

        // 立即新增一笔历史记录（进行中状态）
        pendingHistoryItem = new History.Item(taskName, minutes, System.currentTimeMillis());
        History.add(this, pendingHistoryItem);

        liquidView.setActive(true);
        liquidView.setShowText(true);
        liquidView.setTopText(frontTwo(taskName));
        liquidView.setBottomText(formatTime(remainingMillis));

        refreshNotification("倒计时中：" + taskName);

        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(remainingMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                liquidView.setBottomText(formatTime(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                remainingMillis = 0;
                liquidView.setBottomText("0:00");
                counting = false;
                triggerRemind();
                refreshNotification("任务完成：" + taskName);
                if (pendingHistoryItem != null) {
                    History.update(FloatingService.this, pendingHistoryItem.id,
                            History.STATUS_COMPLETED, System.currentTimeMillis());
                    pendingHistoryItem = null;
                }
            }
        }.start();
    }

    private void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        // 更新为取消
        if (pendingHistoryItem != null) {
            History.update(this, pendingHistoryItem.id,
                    History.STATUS_CANCELLED, System.currentTimeMillis());
            pendingHistoryItem = null;
        }
        counting = false;
        remainingMillis = 0;
        currentTask = "";
        liquidView.setActive(false);
        liquidView.setShowText(false);
        liquidView.setTopText("");
        liquidView.setBottomText("");
        refreshNotification("悬浮倒计时已开启");
    }

    private String frontTwo(String s) {
        if (s == null || s.isEmpty()) return "";
        int len = s.length();
        if (len <= 2) return s;
        return s.substring(0, 2);
    }

    private String formatTime(long millis) {
        long totalSec = Math.max(0, (millis + 500) / 1000);
        long m = totalSec / 60;
        long s = totalSec % 60;
        return m + ":" + (s < 10 ? "0" + s : s);
    }

    private void triggerRemind() {
        int remind = cfg != null ? cfg.remind : Config.DEF_REMIND;
        switch (remind) {
            case 0:  // 静默
                break;
            case 2:  // 轻度提醒：清脆音效一声
                SoundUtil.play(0);
                break;
            case 3:  // 闪动：风浪动画 2s
                if (liquidView != null) liquidView.startStorm();
                break;
            default: // 震动
                vibrate();
                break;
        }
    }

    private void vibrate() {
        try {
            long[] pattern = {0, 600, 200, 600, 200, 1000};
            android.os.VibrationAttributes attrs = new android.os.VibrationAttributes.Builder()
                    .setUsage(android.os.VibrationAttributes.USAGE_NOTIFICATION)
                    .build();
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) {
                    Vibrator vib = vm.getDefaultVibrator();
                    if (vib != null && vib.hasVibrator()) {
                        vib.cancel();
                        vib.vibrate(effect, attrs);
                    }
                }
            } else {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    v.cancel();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(effect, attrs);
                    } else {
                        v.vibrate(pattern, -1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshNotification(String content) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTI_ID, buildNotification(content));
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                Resources.getSystem().getDisplayMetrics());
    }
}
