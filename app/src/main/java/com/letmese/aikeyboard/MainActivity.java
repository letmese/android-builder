package com.letmese.aikeyboard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(64, 64, 64, 64);

        TextView title = new TextView(this);
        title.setText("AI Keyboard ⌨️");
        title.setTextSize(28);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("1. Tap Enable below\n2. Turn on \"AI Keyboard\"\n3. Switch keyboards (spacebar hold or ⌘) and type!\n\nTap the blue ⌘AI key to fix grammar with AI.");
        sub.setTextSize(15);
        sub.setPadding(0, 32, 0, 48);
        root.addView(sub);

        Button enable = new Button(this);
        enable.setText("Enable keyboard");
        enable.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        root.addView(enable);

        setContentView(root);
    }
}
