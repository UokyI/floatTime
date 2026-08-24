package com.floattime.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends BaseAdapter {

    private final Context ctx;
    private List<History.Item> items = new ArrayList<>();
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
    private BroadcastReceiver receiver;

    public HistoryAdapter(Context c) {
        this.ctx = c;
        reload();
        // 适配器自己监听数据变化广播，收到后自动刷新
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                reload();
            }
        };
        IntentFilter filter = new IntentFilter(HistoryAction.BROADCAST);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ctx.registerReceiver(receiver, filter);
        }
    }

    private void reload() {
        items = History.load(ctx);
        notifyDataSetChanged();
    }

    public void destroy() {
        if (receiver != null) {
            ctx.unregisterReceiver(receiver);
            receiver = null;
        }
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public History.Item getItem(int pos) { return items.get(pos); }

    @Override
    public long getItemId(int pos) { return items.get(pos).id; }

    @Override
    public View getView(int pos, View cv, ViewGroup parent) {
        if (cv == null) {
            cv = LayoutInflater.from(ctx).inflate(R.layout.item_history, parent, false);
        }
        History.Item it = items.get(pos);
        TextView task = cv.findViewById(R.id.histTask);
        TextView detail = cv.findViewById(R.id.histDetail);
        TextView status = cv.findViewById(R.id.histStatus);

        task.setText(it.task);
        detail.setText(it.minutes + "分钟 · " + dateFmt.format(new Date(it.timestamp)));
        status.setText(it.statusSymbol());
        status.setTextColor(it.statusColor());
        return cv;
    }
}
