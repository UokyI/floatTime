package com.floattime.app;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;

import java.util.Locale;

/**
 * 应用内语言管理：system=跟随系统，zh=中文，en=English。
 * 通过重新 wrap Context 的方式让资源随所选语言切换。
 */
public final class AppLocale {

    public static final String SYSTEM = "system";
    public static final String ZH = "zh";
    public static final String EN = "en";

    private AppLocale() {
    }

    private static Locale systemLocale() {
        try {
            return Resources.getSystem().getConfiguration().getLocales().get(0);
        } catch (Exception e) {
            return Locale.getDefault();
        }
    }

    public static Locale localeOf(String lang) {
        if (EN.equals(lang)) return Locale.ENGLISH;
        if (ZH.equals(lang)) return Locale.CHINESE;
        return systemLocale();
    }

    public static void setDefault(String lang) {
        Locale.setDefault(localeOf(lang));
    }

    /** 返回一个按当前应用语言 wrap 的 Context（用于 Activity/Service 的 attachBaseContext）。 */
    public static Context wrap(Context base) {
        String lang = SYSTEM;
        try {
            Config cfg = Config.load(base);
            if (cfg != null && cfg.lang != null && !cfg.lang.isEmpty()) lang = cfg.lang;
        } catch (Exception ignored) {
        }
        Locale locale = localeOf(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocales(new LocaleList(locale));
        return base.createConfigurationContext(config);
    }

    /** 每次调用都取最新语言（用于 Service 在运行中切换语言后刷新字符串）。 */
    public static Context localized(Context ctx) {
        return wrap(ctx);
    }
}