package com.letmese.aikeyboard;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;

/** Launches the settings activity from within the keyboard service. */
public class IntentHelper {
    public static void launchSettings(InputMethodService svc) {
        Intent i = new Intent(svc, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        svc.startActivity(i);
    }
}
