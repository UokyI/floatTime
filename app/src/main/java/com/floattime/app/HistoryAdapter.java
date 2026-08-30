package com.floattime.app;

import android.content.Context;
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

    public interface OnItemLongClickListener {
        void onItemLongClick(History.Item item, int position);
    }

    private final Context ctx;
    private List<History.Item> items = new ArrayList<>();
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
    private OnItemLongClickListener longClickListener;

    public HistoryAdapter(Context c) {
        this.ctx = c;
        items = History.load(c);
        History.setOnChangedListener(() -> {
            items = History.load(ctx);
            notifyDataSetChanged();
        });
    }

    public void setOnItemLongClickListener(OnItemLongClickListener l) {
        this.longClickListener = l;
    }

    public void destroy() {
        History.setOnChangedListener(null);
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
        detail.setText(ctx.getString(R.string.history_detail_format, it.minutes, dateFmt.format(new Date(it.timestamp))));
        status.setText(it.statusSymbol());
        status.setTextColor(it.statusColor());

        cv.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(it, pos);
            }
            return true;
        });

        return cv;
    }
}
