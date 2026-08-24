package com.letmese.aikeyboard;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * AI Keyboard — a working QWERTY input method with an AI assistant button.
 *
 * The AI feature calls a free LLM endpoint (OpenCode Zen / x-preview-f-free,
 * keyless) to rewrite/enhance the current text field. Everything runs on-device
 * except the AI call itself.
 */
public class AiKeyboardService extends InputMethodService {

    private static final String[] ROWS = {
            "qwertyuiop",
            "asdfghjkl",
            "zxcvbnm"
    };

    private EditText preview;      // shows AI result / suggestions
    private StringBuilder composing = new StringBuilder();
    private boolean shiftOn = true; // sentence start

    @Override
    public View onCreateInputView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // --- Preview strip: shows composed text + AI output ---
        preview = new EditText(this);
        preview.setTextSize(14);
        preview.setHint("Type here or tap ⌘AI to enhance…");
        preview.setMaxLines(2);
        root.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // --- Keyboard rows ---
        for (int r = 0; r < ROWS.length; r++) {
            root.addView(buildRow(ROWS[r], r));
        }

        // --- Bottom row: ?123 (visual only), space, ⌘AI, backspace, enter ---
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        addKey(bottom, "⇧", v -> { shiftOn = !shiftOn; });
        addWeightedKey(bottom, "space", 3f, v -> commit(" "));
        Button aiBtn = addWeightedKey(bottom, "⌘AI", 2f, v -> runAi());
        aiBtn.setBackgroundColor(0xFF3D5AFE);
        aiBtn.setTextColor(0xFFFFFFFF);
        addWeightedKey(bottom, "⌫", 1.5f, v -> doBackspace());
        addWeightedKey(bottom, "⏎", 1.5f, v -> sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)));
        root.addView(bottom, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (48 * getResources().getDisplayMetrics().density)));

        return root;
    }

    private View buildRow(String keys, int row) {
        LinearLayout rowLayout = new LinearLayout(this);
        float weight = (row == 2) ? 1.4f : 1f;   // bottom row wider keys
        for (int i = 0; i < keys.length(); i++) {
            final String letter = String.valueOf(keys.charAt(i));
            addWeightedKey(rowLayout, letter, weight, v -> commit(shiftOn ? letter.toUpperCase() : letter));
        }
        return rowLayout;
    }

    private void addKey(LinearLayout parent, String label, android.view.View.OnClickListener l) {
        addWeightedKey(parent, label, 1f, l);
    }

    private Button addWeightedKey(LinearLayout parent, String label, float weight,
                                  android.view.View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(v -> { l.onClick(v); });
        parent.addView(b, new LinearLayout.LayoutParams(
                0, (int) (44 * getResources().getDisplayMetrics().density), weight));
        return b;
    }

    private void commit(String s) {
        ic.commitText(s, 1);
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
        sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
    }

    /** Sends the composed text to the free AI and commits the polished result. */
    private void runAi() {
        final String text = composing.toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Type something first, then tap ⌘AI", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "AI is thinking…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String prompt = "Fix grammar and improve this text. Reply ONLY with the improved text:\n" + text;
                String result = AiClient.complete(prompt);
                if (result != null && !result.isEmpty()) {
                    final String out = result.trim();
                    runOnUiThread(() -> {
                        // Replace composed text in the target app
                        for (int i = 0; i < text.length(); i++)
                            sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                        ic.commitText(out, 1);
                        composing.setLength(0);
                        composing.append(out);
                        preview.setText(out);
                        preview.setSelection(out.length());
                    });
                    return;
                }
                runOnUiThread(() -> Toast.makeText(this, "AI returned empty — try again", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "AI error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
