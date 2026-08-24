package com.letmese.aikeyboard;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists user settings: AI mode, keyboard scale, theme. */
public class Prefs {
    private static final String NAME = "aikeyboard_prefs";
    private static final String KEY_MODE = "mode";
    private static final String KEY_SCALE = "scale";   // 0.80 .. 1.30
    private static final String KEY_THEME = "theme";  // 0 light, 1 dark

    public static AiClient.Mode getMode(Context c) {
        SharedPreferences p = c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        String m = p.getString(KEY_MODE, "GRAMMAR");
        try { return AiClient.Mode.valueOf(m); }
        catch (Exception e) { return AiClient.Mode.GRAMMAR; }
    }
    public static void setMode(Context c, AiClient.Mode m) {
        c.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString(KEY_MODE, m.name()).apply();
    }

    public static float getScale(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE).getFloat(KEY_SCALE, 1.0f);
    }
    public static void setScale(Context c, float s) {
        c.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putFloat(KEY_SCALE, s).apply();
    }

    public static boolean isDark(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean(KEY_THEME, false);
    }
    public static void setDark(Context c, boolean d) {
        c.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_THEME, d).apply();
    }
}
