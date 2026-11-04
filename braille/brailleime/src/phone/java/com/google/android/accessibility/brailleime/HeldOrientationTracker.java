/*
 * Copyright 2026 Backtalk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


package com.google.android.accessibility.brailleime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.brailleime.OrientationMonitor.Orientation;
import com.google.android.accessibility.brailleime.input.DotsOrientation;
import com.google.android.accessibility.utils.AccelerometerShare;

/**
 * Remembers how the phone was last held up, the whole time the screen is on, like auto-rotate
 * does. With auto-rotate on, a phone laid flat keeps the screen rotation it was last held in, and
 * the braille keyboard follows it. With auto-rotate off, this gives the keyboard the same memory,
 * even of how the phone was held before the keyboard opened. The braille app launcher reads it
 * too, so it listens whether or not the braille keyboard is an enabled input method.
 *
 * <p>It reads the accelerometer 10 times a second in batches, on a thread of its own. While back
 * tap reads the accelerometer, it takes back tap's readings through {@link AccelerometerShare}
 * instead: a listener of its own would get every one of back tap's 400 readings a second.
 */
public final class HeldOrientationTracker {
  /** Readings every 100 ms. */
  private static final int SAMPLING_PERIOD_US = 100_000;

  /** Readings come in batches up to 200 ms apart where the sensor can hold them. */
  private static final int MAX_REPORT_LATENCY_US = 200_000;

  /** Of back tap's readings, one every 100 ms is used. */
  private static final long SHARED_READING_INTERVAL_NS = 100_000_000L;

  /** How far a reading's time may be from now and still be in the clock of elapsed realtime. */
  private static final long MAX_READING_AGE_NS = 5_000_000_000L;

  @VisibleForTesting static final int ORIENTATION_UNKNOWN = -1;

  private static HeldOrientationTracker instance;

  /** Told on the main thread when how the device is held settles on something new. */
  @Nullable private static volatile Runnable lastHeldListener;

  private final Context context;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final HandlerThread thread = new HandlerThread("HeldOrientation");
  private final Handler handler;
  @Nullable private final SensorManager sensors;
  @Nullable private final Sensor accelerometer;

  /** Used from the reading threads and read from the main thread: guarded by itself. */
  private final HeldOrientationSettler settler = new HeldOrientationSettler();

  // Whether the device's natural orientation is portrait, read on the main thread when the screen
  // turns on and when a display changes, as a foldable folds or unfolds.
  private volatile boolean defaultPortrait = true;

  // On the thread of back tap's readings.
  private long lastSharedReadingNs = Long.MIN_VALUE;

  // On the main thread.
  private boolean stopped;
  private boolean interactive;
  private boolean readingOwn;
  private boolean readingShared;

  private final BroadcastReceiver screenReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          updateListening();
        }
      };

  private final DisplayManager.DisplayListener displayListener =
      new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {}

        @Override
        public void onDisplayRemoved(int displayId) {}

        @Override
        public void onDisplayChanged(int displayId) {
          defaultPortrait = Utils.isDeviceDefaultPortrait(context);
        }
      };

  private final Runnable shareChanged = () -> mainHandler.post(this::updateListening);

  private final SensorEventListener sensorListener =
      new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
          float[] values = event.values;
          if (values.length >= 3) {
            onReading(event.timestamp, values[0], values[1], values[2]);
          }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
      };

  private final AccelerometerShare.Reader sharedReader =
      (timeNs, x, y, z) -> {
        if (timeNs >= lastSharedReadingNs
            && timeNs - lastSharedReadingNs < SHARED_READING_INTERVAL_NS) {
          return;
        }
        lastSharedReadingNs = timeNs;
        onReading(timeNs, x, y, z);
      };

  /** Starts tracking while the screen is on. Calling it again does nothing. */
  public static void start(Context context) {
    if (instance != null) {
      return;
    }
    instance = new HeldOrientationTracker(context.getApplicationContext());
    instance.startListening();
  }

  /** Stops tracking, and forgets how the phone was held. Calling it again does nothing. */
  public static void stop() {
    if (instance == null) {
      return;
    }
    instance.stopListening();
    instance = null;
  }

  /**
   * Sets what to tell when how the device is held settles on something new, or null for nothing.
   * Setting one asks for the readings held in a batch at once.
   */
  public static void setLastHeldListener(@Nullable Runnable listener) {
    lastHeldListener = listener;
    HeldOrientationTracker tracker = instance;
    if (listener != null && tracker != null && tracker.readingOwn && tracker.sensors != null) {
      tracker.sensors.flush(tracker.sensorListener);
    }
  }

  /**
   * How the phone was last held up, or UNKNOWN if it has not been held up since tracking started.
   */
  public static Orientation getLastHeld() {
    HeldOrientationTracker tracker = instance;
    if (tracker == null) {
      return Orientation.UNKNOWN;
    }
    synchronized (tracker.settler) {
      return tracker.settler.getLastHeld();
    }
  }

  /**
   * The screen rotation, one of the {@link android.view.Surface} rotations, that auto-rotate would
   * give for how the device was last held up, or -1 if it has not been held up. Unlike auto-rotate,
   * it can be upside down.
   */
  public static int getLastHeldRotation() {
    HeldOrientationTracker tracker = instance;
    if (tracker == null) {
      return -1;
    }
    synchronized (tracker.settler) {
      return tracker.settler.getLastHeldRotation();
    }
  }

  /**
   * When the phone was last seen held up, in {@link SystemClock#uptimeMillis} time, or 0 if never.
   */
  public static long getLastHeldSeenMs() {
    HeldOrientationTracker tracker = instance;
    if (tracker == null) {
      return 0;
    }
    synchronized (tracker.settler) {
      return tracker.settler.getLastHeldSeenMs();
    }
  }

  /**
   * How far the device is turned clockwise from upright, in degrees, from an accelerometer reading,
   * or {@link #ORIENTATION_UNKNOWN} while it lies flat. The same as {@link
   * android.view.OrientationEventListener} gives.
   */
  @VisibleForTesting
  static int orientationDegrees(float x, float y, float z) {
    float upX = -x;
    float upY = -y;
    float upZ = -z;
    if ((upX * upX + upY * upY) * 4 < upZ * upZ) {
      return ORIENTATION_UNKNOWN;
    }
    int degrees = 90 - Math.round((float) Math.atan2(-upY, upX) * 57.29577957855f);
    while (degrees >= 360) {
      degrees -= 360;
    }
    while (degrees < 0) {
      degrees += 360;
    }
    return degrees;
  }

  private HeldOrientationTracker(Context context) {
    this.context = context;
    sensors = context.getSystemService(SensorManager.class);
    accelerometer = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    thread.start();
    handler = new Handler(thread.getLooper());
  }

  private void startListening() {
    IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
    filter.addAction(Intent.ACTION_SCREEN_OFF);
    context.registerReceiver(screenReceiver, filter);
    context.getSystemService(DisplayManager.class).registerDisplayListener(displayListener, mainHandler);
    AccelerometerShare.addActiveListener(shareChanged);
    updateListening();
  }

  private void stopListening() {
    stopped = true;
    context.unregisterReceiver(screenReceiver);
    context.getSystemService(DisplayManager.class).unregisterDisplayListener(displayListener);
    AccelerometerShare.removeActiveListener(shareChanged);
    AccelerometerShare.removeReader(sharedReader);
    if (readingOwn && sensors != null) {
      sensors.unregisterListener(sensorListener);
    }
    readingOwn = false;
    readingShared = false;
    thread.quitSafely();
  }

  /**
   * Reads the accelerometer only while the screen is on: through back tap's readings while it reads
   * them, otherwise with a listener of its own. Runs on the main thread.
   */
  private void updateListening() {
    if (stopped) {
      return;
    }
    boolean nowInteractive = context.getSystemService(PowerManager.class).isInteractive();
    if (nowInteractive && !interactive) {
      defaultPortrait = Utils.isDeviceDefaultPortrait(context);
    } else if (!nowInteractive && interactive) {
      synchronized (settler) {
        settler.interrupt();
      }
    }
    interactive = nowInteractive;

    boolean shared = nowInteractive && AccelerometerShare.isActive();
    boolean own = nowInteractive && !shared && accelerometer != null;
    if (own != readingOwn && sensors != null) {
      readingOwn = own;
      if (own) {
        sensors.registerListener(
            sensorListener, accelerometer, SAMPLING_PERIOD_US, MAX_REPORT_LATENCY_US, handler);
      } else {
        sensors.unregisterListener(sensorListener);
      }
    }
    if (shared != readingShared) {
      readingShared = shared;
      if (shared) {
        AccelerometerShare.addReader(sharedReader);
      } else {
        AccelerometerShare.removeReader(sharedReader);
      }
    }
  }

  /** Takes a reading, on the thread it came on: [timeNs] in the clock of sensor timestamps. */
  private void onReading(long timeNs, float x, float y, float z) {
    long nowNs = SystemClock.elapsedRealtimeNanos();
    long nowMs = SystemClock.uptimeMillis();
    long ageNs = nowNs - timeNs;
    long readingMs =
        (ageNs >= 0 && ageNs < MAX_READING_AGE_NS) ? nowMs - ageNs / 1_000_000L : nowMs;
    int degrees = orientationDegrees(x, y, z);
    boolean changed;
    synchronized (settler) {
      if (degrees == ORIENTATION_UNKNOWN) {
        // Lying flat, which keeps the last orientation, as auto-rotate does.
        changed = settler.update(Orientation.UNKNOWN, -1, readingMs);
      } else {
        changed =
            settler.update(
                OrientationMonitor.toOrientation(defaultPortrait, degrees),
                DotsOrientation.rotationForDegrees(degrees),
                readingMs);
      }
    }
    if (changed) {
      mainHandler.post(
          () -> {
            Runnable listener = lastHeldListener;
            if (listener != null) {
              listener.run();
            }
          });
    }
  }
}
