package com.floattime.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class History {

    private static final String FILE = "history.json";
    private static final int MAX = 200;

    // 状态：0=进行中，1=完成（✓），2=取消（✗）
    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_COMPLETED = 1;
    public static final int STATUS_CANCELLED = 2;

    public static class Item {
        public long id;            // 雪花 ID
        public String task;
        public int minutes;
        public long timestamp;     // 开始时间
        public long endTime;       // 结束时间
        public int status;         // STATUS_*

        public Item(String task, int minutes, long timestamp) {
            this.id = snowflakeId();
            this.task = task;
            this.minutes = minutes;
            this.timestamp = timestamp;
            this.status = STATUS_RUNNING;
        }

        public Item() {}

        public String statusSymbol() {
            switch (status) {
                case STATUS_COMPLETED: return "✓";
                case STATUS_CANCELLED: return "✗";
                default: return "…";
            }
        }

        public int statusColor() {
            switch (status) {
                case STATUS_COMPLETED: return 0xFF4CAF50;
                case STATUS_CANCELLED: return 0xFFE57373;
                default: return 0xFF999999;
            }
        }
    }

    // 简易雪花 ID：时间戳左移 + 自增序列，保证唯一
    private static long seq = 0;
    private static long lastTs = 0;
    private static synchronized long snowflakeId() {
        long now = System.currentTimeMillis();
        if (now == lastTs) {
            seq = (seq + 1) & 0xFFF;
            if (seq == 0) {
                while (System.currentTimeMillis() == now) { /* spin */ }
                now = System.currentTimeMillis();
            }
        } else {
            seq = 0;
            lastTs = now;
        }
        return (now << 12) | seq;
    }

    public static List<Item> load(Context c) {
        List<Item> list = new ArrayList<>();
        File f = new File(c.getFilesDir(), FILE);
        if (!f.exists()) return list;
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            in.read(buf);
            JSONArray arr = new JSONArray(new String(buf, "UTF-8"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject j = arr.getJSONObject(i);
                Item it = new Item();
                it.id = j.optLong("id", 0);
                it.task = j.optString("task", "");
                it.minutes = j.optInt("minutes", 0);
                it.timestamp = j.optLong("ts", 0);
                it.endTime = j.optLong("end", 0);
                it.status = j.optInt("status", STATUS_RUNNING);
                list.add(it);
            }
        } catch (Exception ignored) {
        }
        // 最新的在最前（按开始时间倒序）
        Collections.sort(list, (a, b) -> Long.compare(b.timestamp, a.timestamp));
        return list;
    }

    // 新增一笔（开始倒计时时调用）
    public static void add(Context c, Item item) {
        List<Item> list = load(c);
        list.add(0, item);
        if (list.size() > MAX) list = new ArrayList<>(list.subList(0, MAX));
        save(c, list);
        notifyChanged(c);
    }

    // 按 ID 更新某笔记录的状态和结束时间
    public static void update(Context c, long id, int status, long endTime) {
        List<Item> list = load(c);
        for (Item it : list) {
            if (it.id == id) {
                it.status = status;
                it.endTime = endTime;
                break;
            }
        }
        save(c, list);
        notifyChanged(c);
    }

    public static void save(Context c, List<Item> list) {
        try (FileOutputStream out = new FileOutputStream(new File(c.getFilesDir(), FILE))) {
            JSONArray arr = new JSONArray();
            for (Item it : list) {
                JSONObject j = new JSONObject();
                j.put("id", it.id);
                j.put("task", it.task);
                j.put("minutes", it.minutes);
                j.put("ts", it.timestamp);
                j.put("end", it.endTime);
                j.put("status", it.status);
                arr.put(j);
            }
            out.write(arr.toString().getBytes("UTF-8"));
        } catch (Exception ignored) {
        }
    }

    public static void clear(Context c) {
        new File(c.getFilesDir(), FILE).delete();
        notifyChanged(c);
    }

    private static void notifyChanged(Context c) {
        android.content.Intent intent = new android.content.Intent(HistoryAction.BROADCAST);
        intent.setPackage(c.getPackageName());
        c.sendBroadcast(intent);
    }
}
