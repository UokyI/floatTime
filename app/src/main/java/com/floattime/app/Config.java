package com.floattime.app;

import android.content.Context;
import android.graphics.Color;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Config {

    private static final String FILE = "config.json";

    // 默认值 = 当前设定
    public static final int DEF_IDLE_C = 0xFFFBFCC7;
    public static final int DEF_IDLE_D = 0xFFF6C8C0;
    public static final int DEF_IDLE_G = 0xFFF0A89F;
    public static final int DEF_ACT_C  = 0xFF95C5AC;
    public static final int DEF_ACT_D  = 0xFF7BB496;
    public static final int DEF_ACT_G  = 0xFF5A9A7C;
    public static final int DEF_SIZE   = 94;
    public static final int DEF_TEXT_SIZE = 15;
    // 提醒方式：0=静默 1=震动 2=轻度提醒(清脆音效) 3=闪动(风浪)
    public static final int DEF_REMIND = 3;

    public int idleC, idleD, idleG;
    public int actC, actD, actG;
    public int sizeDp;
    public int textSizeSp;
    public int remind;

    public Config() {
        idleC = DEF_IDLE_C; idleD = DEF_IDLE_D; idleG = DEF_IDLE_G;
        actC  = DEF_ACT_C;  actD  = DEF_ACT_D;  actG  = DEF_ACT_G;
        sizeDp = DEF_SIZE;
        textSizeSp = DEF_TEXT_SIZE;
        remind = DEF_REMIND;
    }

    public static Config load(Context c) {
        Config cfg = new Config();
        File f = new File(c.getFilesDir(), FILE);
        if (!f.exists()) return cfg;
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            in.read(buf);
            JSONObject j = new JSONObject(new String(buf, "UTF-8"));
            cfg.idleC = j.optInt("idleC", DEF_IDLE_C);
            cfg.idleD = j.optInt("idleD", DEF_IDLE_D);
            cfg.idleG = j.optInt("idleG", DEF_IDLE_G);
            cfg.actC  = j.optInt("actC",  DEF_ACT_C);
            cfg.actD  = j.optInt("actD",  DEF_ACT_D);
            cfg.actG  = j.optInt("actG",  DEF_ACT_G);
            cfg.sizeDp = j.optInt("sizeDp", DEF_SIZE);
            cfg.textSizeSp = j.optInt("textSizeSp", DEF_TEXT_SIZE);
            cfg.remind = j.optInt("remind", DEF_REMIND);
        } catch (Exception ignored) {
        }
        return cfg;
    }

    public void save(Context c) {
        try (FileOutputStream out = new FileOutputStream(new File(c.getFilesDir(), FILE))) {
            out.write(toJsonString().getBytes("UTF-8"));
        } catch (IOException ignored) {
        }
    }

    private String toJsonString() {
        try {
            return toJson().toString(2);
        } catch (Exception e) {
            return "{}";
        }
    }

    public JSONObject toJson() {
        JSONObject j = new JSONObject();
        try {
            j.put("idleC", idleC); j.put("idleD", idleD); j.put("idleG", idleG);
            j.put("actC", actC);   j.put("actD", actD);   j.put("actG", actG);
            j.put("sizeDp", sizeDp);
            j.put("textSizeSp", textSizeSp);
            j.put("remind", remind);
        } catch (Exception ignored) {
        }
        return j;
    }

    public static Config fromJsonString(String s) throws Exception {
        JSONObject j = new JSONObject(s);
        Config cfg = new Config();
        cfg.idleC = j.optInt("idleC", DEF_IDLE_C);
        cfg.idleD = j.optInt("idleD", DEF_IDLE_D);
        cfg.idleG = j.optInt("idleG", DEF_IDLE_G);
        cfg.actC  = j.optInt("actC",  DEF_ACT_C);
        cfg.actD  = j.optInt("actD",  DEF_ACT_D);
        cfg.actG  = j.optInt("actG",  DEF_ACT_G);
        cfg.sizeDp = j.optInt("sizeDp", DEF_SIZE);
        cfg.textSizeSp = j.optInt("textSizeSp", DEF_TEXT_SIZE);
        cfg.remind = j.optInt("remind", DEF_REMIND);
        return cfg;
    }

    public void reset() {
        idleC = DEF_IDLE_C; idleD = DEF_IDLE_D; idleG = DEF_IDLE_G;
        actC  = DEF_ACT_C;  actD  = DEF_ACT_D;  actG  = DEF_ACT_G;
        sizeDp = DEF_SIZE;
        textSizeSp = DEF_TEXT_SIZE;
        remind = DEF_REMIND;
    }

    // 带颜色的格式化 JSON（用于“查看配置”弹窗，显示 hex 颜色）
    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"idle\": {\n");
        sb.append("    \"core\": \"").append(hex(idleC)).append("\",\n");
        sb.append("    \"deep\": \"").append(hex(idleD)).append("\",\n");
        sb.append("    \"glow\": \"").append(hex(idleG)).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"active\": {\n");
        sb.append("    \"core\": \"").append(hex(actC)).append("\",\n");
        sb.append("    \"deep\": \"").append(hex(actD)).append("\",\n");
        sb.append("    \"glow\": \"").append(hex(actG)).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"sizeDp\": ").append(sizeDp).append(",\n");
        sb.append("  \"textSizeSp\": ").append(textSizeSp).append(",\n");
        sb.append("  \"remind\": ").append(remind).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String hex(int c) {
        return String.format("#%06X", c & 0xFFFFFF);
    }

    // 从 pretty 格式（含 hex 颜色）解析
    public static Config fromPrettyString(String s) throws Exception {
        // 简化处理：解析 hex 颜色和数字
        Config cfg = new Config();
        cfg.idleC = parseHex(s, "idle", "core", DEF_IDLE_C);
        cfg.idleD = parseHex(s, "idle", "deep", DEF_IDLE_D);
        cfg.idleG = parseHex(s, "idle", "glow", DEF_IDLE_G);
        cfg.actC  = parseHex(s, "active", "core", DEF_ACT_C);
        cfg.actD  = parseHex(s, "active", "deep", DEF_ACT_D);
        cfg.actG  = parseHex(s, "active", "glow", DEF_ACT_G);
        // sizeDp
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "\"sizeDp\"\\s*:\\s*(\\d+)").matcher(s);
        if (m.find()) cfg.sizeDp = Integer.parseInt(m.group(1));
        java.util.regex.Matcher m1b = java.util.regex.Pattern.compile(
                "\"textSizeSp\"\\s*:\\s*(\\d+)").matcher(s);
        if (m1b.find()) cfg.textSizeSp = Integer.parseInt(m1b.group(1));
        java.util.regex.Matcher m2 = java.util.regex.Pattern.compile(
                "\"remind\"\\s*:\\s*(\\d+)").matcher(s);
        if (m2.find()) cfg.remind = Integer.parseInt(m2.group(1));
        return cfg;
    }

    private static int parseHex(String s, String group, String key, int def) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "\"" + group + "\"[\\s\\S]*?\"" + key + "\"\\s*:\\s*\"(#?[0-9A-Fa-f]+)\"").matcher(s);
        if (m.find()) {
            String h = m.group(1);
            if (h.startsWith("#")) h = h.substring(1);
            try {
                return 0xFF000000 | Integer.parseInt(h, 16);
            } catch (Exception e) {
                return def;
            }
        }
        return def;
    }
}
