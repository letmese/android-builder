package com.letmese.aikeyboard;

import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * AI Keyboard — a working QWERTY input method with an AI assistant button.
 * The ⌘AI key sends composed text to a free keyless LLM and commits the
 * polished result back into the target app.
 */
public class AiKeyboardService extends InputMethodService {

    private static final String[] ROWS = {
            "qwertyuiop",
            "asdfghjkl",
            "zxcvbnm"
    };

    private EditText preview;
    private final StringBuilder composing = new StringBuilder();
    private boolean shiftOn = true;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateInputView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        preview = new EditText(this);
        preview.setTextSize(14);
        preview.setHint("Type here or tap AI to enhance…");
        preview.setMaxLines(2);
        root.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        for (int r = 0; r < ROWS.length; r++) {
            root.addView(buildRow(ROWS[r], r));
        }

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        addWeightedKey(bottom, "⇧", 1f, new KeyHandler() {
            @Override public void onKey() { shiftOn = !shiftOn; }
        });
        addWeightedKey(bottom, "space", 3f, new KeyHandler() {
            @Override public void onKey() { commitText(" "); }
        });
        Button aiBtn = addWeightedKey(bottom, "AI", 2f, new KeyHandler() {
            @Override public void onKey() { runAi(); }
        });
        aiBtn.setBackgroundColor(0xFF3D5AFE);
        aiBtn.setTextColor(0xFFFFFFFF);
        addWeightedKey(bottom, "⌫", 1.5f, new KeyHandler() {
            @Override public void onKey() { doBackspace(); }
        });
        addWeightedKey(bottom, "⏎", 1.5f, new KeyHandler() {
            @Override public void onKey() {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
            }
        });
        root.addView(bottom, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (48 * getResources().getDisplayMetrics().density)));

        return root;
    }

    private interface KeyHandler { void onKey(); }

    private View buildRow(String keys, int row) {
        LinearLayout rowLayout = new LinearLayout(this);
        final float weight = (row == 2) ? 1.4f : 1f;
        for (int i = 0; i < keys.length(); i++) {
            final String letter = String.valueOf(keys.charAt(i));
            addWeightedKey(rowLayout, letter, weight, new KeyHandler() {
                @Override public void onKey() {
                    commitText(shiftOn ? letter.toUpperCase() : letter);
                }
            });
        }
        return rowLayout;
    }

    private Button addWeightedKey(LinearLayout parent, String label, float weight, final KeyHandler handler) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { handler.onKey(); }
        });
        parent.addView(b, new LinearLayout.LayoutParams(
                0, (int) (44 * getResources().getDisplayMetrics().density), weight));
        return b;
    }

    private void commitText(String s) {
        if (getCurrentInputConnection() != null) {
            getCurrentInputConnection().commitText(s, 1);
        }
        if (!s.equals(" ")) shiftOn = false;
        composing.append(s);
        preview.setText(composing.toString());
        preview.setSelection(preview.getText().length());
    }

    private void doBackspace() {
        if (composing.length() > 0) {
            composing.deleteCharAt(composing.length() - 1);
            preview.setText(composing.toString());
            preview.setSelection(preview.getText().length());
        }
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
    }

    /** Sends the composed text to the free AI and commits the polished result. */
    private void runAi() {
        final String text = composing.toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Type something first, then tap AI", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "AI is thinking…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    String prompt = "Fix grammar and improve this text. Reply ONLY with the improved text:\n" + text;
                    final String result = AiClient.complete(prompt);
                    mainHandler.post(new Runnable() {
                        @Override public void run() {
                            if (result != null && !result.trim().isEmpty()) {
                                String out = result.trim();
                                if (getCurrentInputConnection() != null && text.length() > 0) {
                                    getCurrentInputConnection().deleteSurroundingText(text.length(), 0);
                                    getCurrentInputConnection().commitText(out, 1);
                                }
                                composing.setLength(0);
                                composing.append(out);
                                preview.setText(out);
                                preview.setSelection(out.length());
                            } else {
                                Toast.makeText(AiKeyboardService.this,
                                        "AI returned empty - try again", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(AiKeyboardService.this,
                                    "AI error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
}
