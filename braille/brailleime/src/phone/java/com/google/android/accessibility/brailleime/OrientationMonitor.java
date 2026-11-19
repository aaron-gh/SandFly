/*
 * Copyright 2020 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.brailleime;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import com.google.common.annotations.VisibleForTesting;

/**
 * Monitors the orientation changed event even when device auto rotate is locked.
 *
 * <p>OrientationMonitor is a singleton. The calling sequence is as below.
 *
 * <p>1. Call {@link #init(Context)} <b>once</b> to make sure OrientationMonitor has been
 * initialized.
 *
 * <p>2. Call {@link #enable()} to start monitor orientation changed events from system.
 */
public class OrientationMonitor {
  /** Screen orientations. */
  public enum Orientation {
    UNKNOWN(-1),
    PORTRAIT(0),
    LANDSCAPE(90),
    REVERSE_PORTRAIT(180),
    REVERSE_LANDSCAPE(270),
    ;

    private final int degree;

    Orientation(int degree) {
      this.degree = degree;
    }

    public int getDegree() {
      return degree;
    }
  }

  /** Callback register in {@link OrientationMonitor}. */
  public interface Callback {
    void onOrientationChanged(Orientation orientation);
  }

  private static OrientationMonitor singleton;
  private Callback orientationMonitorCallback;
  private boolean enabled;
  private Orientation currentOrientation = Orientation.UNKNOWN;
  @Nullable private SensorManager sensors;
  @Nullable private Sensor accelerometer;

  /**
   * How far past the edge between two orientations a reading must be, in degrees, before the
   * orientation changes, so that a device held near the edge does not flicker between them.
   */
  @VisibleForTesting static final int HYSTERESIS_DEGREES = 5;

  /**
   * How far from lying flat a device must be tilted before a reading can change the orientation,
   * as tan² of the angle: 40 degrees. Tilted less, a small sideways roll swings the reading a long
   * way, as when picking the device up by one edge or holding it low to read.
   */
  private static final float MIN_TILT_TAN_SQUARED = 0.704f;

  private final SensorEventListener sensorListener =
      new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
          float[] values = event.values;
          if (values.length < 3) {
            return;
          }
          Orientation newOrientation =
              nextOrientation(currentOrientation, defaultPortrait, values[0], values[1], values[2]);
          if (newOrientation != currentOrientation) {
            currentOrientation = newOrientation;
            if (orientationMonitorCallback != null) {
              orientationMonitorCallback.onOrientationChanged(newOrientation);
            }
          }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
      };
  private Context context;
  // Whether the device's natural orientation is portrait, read once and again when a display
  // changes, as a foldable folds or unfolds, rather than at every reading.
  private boolean defaultPortrait = true;
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

  public static OrientationMonitor getInstance() {
    if (singleton == null) {
      throw new IllegalStateException("You forget to call init()!");
    }
    return singleton;
  }

  /** Whether {@link #init} has been called. */
  public static boolean isInitialized() {
    return singleton != null;
  }

  public static void init(Context context) {
    if (singleton == null) {
      singleton = new OrientationMonitor();
      singleton.context = context.getApplicationContext();
      singleton.sensors = singleton.context.getSystemService(SensorManager.class);
      singleton.accelerometer =
          singleton.sensors == null
              ? null
              : singleton.sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }
  }

  /**
   * The orientation after an accelerometer reading, from {@code current}. Lying flat reads as it
   * did from {@link android.view.OrientationEventListener}. Tilted less than 40 degrees from flat,
   * or within {@link #HYSTERESIS_DEGREES} of the edge of the current orientation, it stays as it
   * is.
   */
  @VisibleForTesting
  static Orientation nextOrientation(
      Orientation current, boolean defaultPortrait, float x, float y, float z) {
    int degrees = HeldOrientationTracker.orientationDegrees(x, y, z);
    if (degrees == HeldOrientationTracker.ORIENTATION_UNKNOWN) {
      return toOrientation(defaultPortrait, degrees);
    }
    if (x * x + y * y < z * z * MIN_TILT_TAN_SQUARED) {
      return current;
    }
    Orientation next = toOrientation(defaultPortrait, degrees);
    if (next == current || current == Orientation.UNKNOWN) {
      return next;
    }
    int fromCenter =
        Math.floorMod(degrees - next.getDegree() - (defaultPortrait ? 0 : 90), 360);
    fromCenter = Math.min(fromCenter, 360 - fromCenter);
    return fromCenter <= 45 - HYSTERESIS_DEGREES ? next : current;
  }

  /**
   * Converts a reading as from {@link android.view.OrientationEventListener} to the nearest
   * orientation, on a device whose natural orientation is portrait or not.
   */
  static Orientation toOrientation(boolean defaultPortrait, int degree) {
    if (degree < 0) {
      // Lying flat.
      return defaultPortrait ? Orientation.PORTRAIT : Orientation.UNKNOWN;
    }
    degree = defaultPortrait ? degree : Math.floorMod(degree - 90, 360);
    for (Orientation orientation : Orientation.values()) {
      if (orientation != Orientation.UNKNOWN
          && Math.abs(degree - orientation.getDegree()) <= 45) {
        return orientation;
      }
    }
    // Special case for portrait when the degrees is close to 360.
    if (Math.abs(degree - 360) <= 45) {
      return Orientation.PORTRAIT;
    }
    return Orientation.UNKNOWN;
  }

  public void enable() {
    if (enabled) {
      return;
    }
    enabled = true;
    if (context != null) {
      defaultPortrait = Utils.isDeviceDefaultPortrait(context);
      context
          .getSystemService(DisplayManager.class)
          .registerDisplayListener(displayListener, new Handler(Looper.getMainLooper()));
    }
    if (sensors != null && accelerometer != null) {
      sensors.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
  }

  public void disable() {
    if (!enabled) {
      return;
    }
    enabled = false;
    if (context != null) {
      context.getSystemService(DisplayManager.class).unregisterDisplayListener(displayListener);
    }
    if (sensors != null) {
      sensors.unregisterListener(sensorListener);
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Orientation getCurrentOrientation() {
    return currentOrientation;
  }

  public void registerCallback(Callback orientationMonitorCallback) {
    this.orientationMonitorCallback = orientationMonitorCallback;
  }

  public void unregisterCallback() {
    orientationMonitorCallback = null;
  }

  /** Whether a callback is registered, as while the braille keyboard is shown. */
  public boolean hasCallback() {
    return orientationMonitorCallback != null;
  }
}
