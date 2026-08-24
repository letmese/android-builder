package com.letmese.aikeyboard;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * AI Keyboard — SwiftKey-styled QWERTY with AI modes, adaptive sizing, and settings.
 */
public class AiKeyboardService extends InputMethodService {

    private static final String[] ROWS = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
    private static final String[] NUM_ROW = {"1234567890"};

    private EditText preview;
    private final StringBuilder composing = new StringBuilder();
    private boolean shiftOn = true;
    private boolean capsLock = false;
    private LinearLayout keyArea;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private float dp;
    private float scale = 1.0f;
    private boolean dark = false;

    // ---- palette (light) ----
    private int BG_KB, BG_KEY, BG_FN, BG_PRESS, BG_ACCENT, TXT;
    private final int TXT_ON_ACCENT = 0xFFFFFFFF;

    private void applyTheme() {
        if (dark) {
            BG_KB = 0xFF1B1C1E; BG_KEY = 0xFF2A2B2E; BG_FN = 0xFF3A3B3F;
            BG_PRESS = 0xFF4A4C50; BG_ACCENT = 0xFF5C7CFA; TXT = 0xFFE8EAED;
        } else {
            BG_KB = 0xFFF2F3F5; BG_KEY = 0xFFFFFFFF; BG_FN = 0xFFD5D9DE;
            BG_PRESS = 0xFFBDC3CB; BG_ACCENT = 0xFF3D5AFE; TXT = 0xFF202124;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dp = getResources().getDisplayMetrics().density;
        scale = Prefs.getScale(this);
        dark = Prefs.isDark(this);
    }

    @Override
    public View onCreateInputView() {
        applyTheme();
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG_KB);
        int pad = (int) (3 * dp * scale);
        root.setPadding(pad, pad, pad, pad);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        root.addView(column, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        preview = new EditText(this);
        preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14 * scale);
        preview.setTextColor(TXT);
        preview.setHint("Type, then tap AI — or long-press AI for a mode");
        preview.setMaxLines(2);
        preview.setBackground(rounded(BG_FN, (int) (10 * dp)));
        preview.setPadding((int) (10*dp), (int) (6*dp), (int) (10*dp), (int) (6*dp));
        column.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        addGap(column, 4);

        keyArea = new LinearLayout(this);
        keyArea.setOrientation(LinearLayout.VERTICAL);
        column.addView(keyArea);
        renderKeyboard();
        return root;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        scale = Prefs.getScale(this); dark = Prefs.isDark(this);
        applyTheme();
        if (!capsLock) { shiftOn = true; renderKeyboard(); }
    }

    private interface KeyHandler { void onKey(); }

    private void renderKeyboard() {
        keyArea.removeAllViews();
        int keyH = (int) (50 * dp * scale);

        // number row
        LinearLayout numRow = hrow();
        for (int i = 0; i < NUM_ROW[0].length(); i++) {
            final String d = String.valueOf(NUM_ROW[0].charAt(i));
            numRow.addView(key(d, 1f, BG_FN, new KeyHandler() {
                @Override public void onKey() { commitText(d); }
            }, 15f, keyH));
        }
        keyArea.addView(numRow);
        addGap(keyArea, 2);

        for (int r = 0; r < ROWS.length; r++) {
            LinearLayout row = hrow();
            if (r == 2) row.addView(key("⇧", 1.5f, BG_FN, new KeyHandler() {
                @Override public void onKey() {
                    if (shiftOn && !capsLock) capsLock = true;
                    else if (capsLock) { capsLock = false; shiftOn = false; }
                    else shiftOn = !shiftOn;
                    renderKeyboard();
                }
            }, 18f, keyH));
            String letters = ROWS[r];
            float w = (r == 1) ? 1.15f : 1f;
            for (int i = 0; i < letters.length(); i++) {
                final String letter = String.valueOf(letters.charAt(i));
                row.addView(key(shiftOn ? letter.toUpperCase() : letter, w, BG_KEY,
                        new KeyHandler() { @Override public void onKey() { commitText(letter); } },
                        18f, keyH));
            }
            if (r == 2) row.addView(key("⌫", 1.5f, BG_FN, new KeyHandler() {
                @Override public void onKey() { doBackspace(); startBackspaceRepeat(); }
            }, 18f, keyH));
            keyArea.addView(row);
        }
        addGap(keyArea, 4);

        LinearLayout bottom = hrow();
        bottom.addView(key(",", 1f, BG_KEY, new KeyHandler() { @Override public void onKey() { commitText(","); } }, 18f, keyH));
        bottom.addView(key(".", 1f, BG_KEY, new KeyHandler() { @Override public void onKey() { commitText("."); } }, 18f, keyH));
        bottom.addView(key("space", 3.6f, BG_KEY, new KeyHandler() { @Override public void onKey() { commitText(" "); } }, 14f, keyH));
        // ⚙ opens settings; long-press AI shows mode picker
        Button gear = key("⚙", 1f, BG_FN, new KeyHandler() {
            @Override public void onKey() { openSettings(); }
        }, 16f, keyH);
        bottom.addView(gear);
        Button aiBtn = key("AI", 1.7f, BG_ACCENT, new KeyHandler() {
            @Override public void onKey() { runAi(Prefs.getMode(AiKeyboardService.this)); }
        }, 15f, keyH);
        aiBtn.setTextColor(TXT_ON_ACCENT);
        aiBtn.setTypeface(Typeface.DEFAULT_BOLD);
        aiBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { showModePicker(); return true; }
        });
        bottom.addView(aiBtn);
        Button enterBtn = key("⏎", 1.5f, BG_ACCENT, new KeyHandler() {
            @Override public void onKey() { sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER); }
        }, 15f, keyH);
        enterBtn.setTextColor(TXT_ON_ACCENT);
        bottom.addView(enterBtn);
        keyArea.addView(bottom);
    }

    private void openSettings() {
        IntentHelper.launchSettings(this);
    }

    private void showModePicker() {
        final AiClient.Mode[] modes = AiClient.Mode.values();
        CharSequence[] names = new CharSequence[modes.length];
        for (int i = 0; i < modes.length; i++) names[i] = modeLabel(modes[i]);
        new AlertDialog.Builder(this)
                .setTitle("AI mode")
                .setItems(names, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int which) {
                        Prefs.setMode(AiKeyboardService.this, modes[which]);
                        Toast.makeText(AiKeyboardService.this,
                                "AI mode: " + modeLabel(modes[which]), Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    static String modeLabel(AiClient.Mode m) {
        switch (m) {
            case GRAMMAR: return "Grammar fix";
            case FORMAL:  return "Formal";
            case CASUAL:  return "Casual";
            case SOCIAL:  return "Social post";
            case TRANSLATE: return "Translate (→EN)";
        }
        return m.name();
    }

    private LinearLayout hrow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }
    private void addGap(LinearLayout parent, int dpH) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, (int) (dpH * dp)));
    }

    private Button key(String label, float weight, int bg, KeyHandler handler, float textSizeSp, int h) {
        Button b = new Button(this);
        b.setText(label); b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp * scale);
        b.setTypeface(Typeface.DEFAULT); b.setTextColor(TXT);
        b.setGravity(Gravity.CENTER); b.setStateListAnimator(null);
        b.setPadding(0, 0, 0, 0);
        final GradientDrawable idle = rounded(bg, (int) (8 * dp));
        final GradientDrawable pressed = rounded(bg == BG_ACCENT ? darken(bg) : BG_PRESS, (int) (8 * dp));
        b.setBackground(idle);
        b.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, android.view.MotionEvent e) {
                int a = e.getActionMasked();
                if (a == android.view.MotionEvent.ACTION_DOWN) {
                    v.setBackground(pressed);
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                } else if (a == android.view.MotionEvent.ACTION_UP || a == android.view.MotionEvent.ACTION_CANCEL) {
                    v.setBackground(idle);
                }
                return false;
            }
        });
        b.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { handler.onKey(); } });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, h, weight);
        int m = (int) (2.5f * dp);
        lp.setMargins(m, m, m, m);
        b.setLayoutParams(lp);
        stopBackspaceRepeat();
        return b;
    }

    private GradientDrawable rounded(int color, int r) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(r); return d;
    }
    private int darken(int c) {
        float[] hsv = new float[3]; Color.colorToHSV(c, hsv); hsv[2] *= 0.8f; return Color.HSVToColor(hsv);
    }

    private final Runnable backspaceRepeater = new Runnable() {
        @Override public void run() { doBackspace(); mainHandler.postDelayed(this, 60); }
    };
    private void startBackspaceRepeat() { mainHandler.postDelayed(backspaceRepeater, 350); }
    private void stopBackspaceRepeat() { mainHandler.removeCallbacks(backspaceRepeater); }

    private void commitText(String s) {
        if (getCurrentInputConnection() != null) getCurrentInputConnection().commitText(s, 1);
        if (!s.equals(" ")) shiftOn = Character.isWhitespace(s.charAt(0)) || s.equals(".") || s.equals(",");
        composing.append(s);
        preview.setText(composing.toString());
        preview.setSelection(preview.getText().length());
    }
    private void doBackspace() {
        if (composing.length() > 0) {
            composing.deleteCharAt(composing.length() - 1);
            preview.setText(composing.toString()); preview.setSelection(preview.getText().length());
        }
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
    }

    private void runAi(final AiClient.Mode mode) {
        final String text = composing.toString().trim();
        if (text.isEmpty()) { Toast.makeText(this, "Type something first, then tap AI", Toast.LENGTH_SHORT).show(); return; }
        Toast.makeText(this, "AI: " + modeLabel(mode) + "…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final String result = AiClient.complete(mode, text);
                    mainHandler.post(new Runnable() {
                        @Override public void run() {
                            if (result != null && !result.trim().isEmpty()) {
                                String out = result.trim();
                                if (getCurrentInputConnection() != null && text.length() > 0) {
                                    getCurrentInputConnection().deleteSurroundingText(text.length(), 0);
                                    getCurrentInputConnection().commitText(out, 1);
                                }
                                composing.setLength(0); composing.append(out);
                                preview.setText(out); preview.setSelection(out.length());
                            } else {
                                Toast.makeText(AiKeyboardService.this, "AI returned empty — try again", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(AiKeyboardService.this, "AI error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
}
