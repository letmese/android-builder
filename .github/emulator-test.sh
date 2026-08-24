#!/bin/bash
# Emulator smoke-test for AI Keyboard.
set -x

APK=app/build/outputs/apk/debug/app-debug.apk

echo "=== WAITING FOR BOOT ==="
adb wait-for-device
for i in $(seq 1 60); do
  B=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
  if [ "$B" = "1" ]; then echo "BOOT COMPLETE after ${i}0s"; break; fi
  sleep 10
done

echo "=== INSTALLING APK ==="
timeout 120 adb install -r "$APK" || { echo "INSTALL FAILED/TIMED OUT"; }

echo "=== LAUNCHING MAIN ACTIVITY ==="
timeout 60 adb shell am start -W -n com.letmese.aikeyboard/.MainActivity || true
sleep 8

echo "=== PROCESS CHECK ==="
PID=$(adb shell pidof com.letmese.aikeyboard | tr -d '\r')
if [ -n "$PID" ]; then
  echo "=== APP IS RUNNING OK (pid $PID) ==="
else
  echo "=== APP DIED - CRASH DETECTED ==="
fi

# Test the IME service too
echo "=== ENABLING IME ==="
timeout 30 adb shell ime enable com.letmese.aikeyboard/.AiKeyboardService || true
timeout 30 adb shell ime set com.letmese.aikeyboard/.AiKeyboardService || true
sleep 3
timeout 30 adb shell ime list -s || true

# Open a text field and tap it to summon the keyboard
timeout 30 adb shell am start -a android.intent.action.VIEW -d "https://www.google.com" >/dev/null 2>&1 || true
sleep 5
timeout 30 adb shell input tap 400 200 || true
sleep 6

# Re-check process after keyboard use
PID2=$(adb shell pidof com.letmese.aikeyboard | tr -d '\r')
if [ -n "$PID2" ]; then
  echo "=== APP STILL ALIVE AFTER KEYBOARD USE ==="
else
  echo "=== APP DIED AFTER KEYBOARD USE ==="
fi

echo "=== CRASH LOG ==="
adb logcat -d | grep -B2 -A60 "FATAL EXCEPTION" > crash_log.txt || echo "NO FATAL EXCEPTIONS FOUND" > crash_log.txt
cat crash_log.txt
