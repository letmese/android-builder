package com.letmese.myapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText("Hello! Your app factory is working.");
        tv.setTextSize(22);
        tv.setPadding(48, 96, 48, 48);
        setContentView(tv);
    }
}
