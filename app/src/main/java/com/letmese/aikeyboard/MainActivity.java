package com.letmese.aikeyboard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean dark = Prefs.isDark(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.TOP);
        root.setPadding(32, 32, 32, 32);
        if (dark) root.setBackgroundColor(0xFF1B1C1E);

        title("AI Keyboard ⌨️");

        Button enable = btn("Enable keyboard", () ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        root.addView(enable);

        section("AI default mode (tap AI key uses this)");
        for (final AiClient.Mode m : AiClient.Mode.values()) {
            Button b = btn(AiKeyboardService.modeLabel(m), () -> {
                Prefs.setMode(this, m);
                Toast.makeText(this, "Default AI mode: " + AiKeyboardService.modeLabel(m), Toast.LENGTH_SHORT).show();
            });
            root.addView(b);
        }

        section("Keyboard size");
        final TextView sizeLabel = new TextView(this);
        sizeLabel.setText("Scale: " + Math.round(Prefs.getScale(this) * 100) + "%");
        root.addView(sizeLabel);
        SeekBar sb = new SeekBar(this);
        sb.setMax(50); // 0.80 .. 1.30
        sb.setProgress((int) ((Prefs.getScale(this) - 0.8f) / 0.5f * 50));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                float scale = 0.8f + (p / 50f) * 0.5f;
                sizeLabel.setText("Scale: " + Math.round(scale * 100) + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                float scale = 0.8f + (s.getProgress() / 50f) * 0.5f;
                Prefs.setScale(MainActivity.this, scale);
            }
        });
        root.addView(sb);

        section("Appearance");
        CheckBox darkCb = new CheckBox(this);
        darkCb.setText("Dark theme");
        darkCb.setChecked(dark);
        darkCb.setOnCheckedChangeListener((v, checked) -> Prefs.setDark(this, checked));
        root.addView(darkCb);

        section("Tip");
        TextView tip = new TextView(this);
        tip.setText("Inside the keyboard: tap ⚙ for settings, long-press the AI key to switch mode for one message.");
        tip.setTextSize(13);
        root.addView(tip);

        setContentView(root);
    }

    private void title(String t) {
        TextView v = new TextView(this); v.setText(t); v.setTextSize(24);
        root.addView(v);
    }
    private void section(String t) {
        TextView v = new TextView(this); v.setText(t); v.setTextSize(16);
        v.setPadding(0, 24, 0, 4); root.addView(v);
    }
    private Button btn(String label, Runnable action) {
        Button b = new Button(this); b.setText(label);
        b.setOnClickListener(v -> action.run());
        root.addView(b);
        return b;
    }
}
