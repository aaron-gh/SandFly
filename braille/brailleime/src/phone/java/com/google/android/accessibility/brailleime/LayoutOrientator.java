/*
 * Copyright 2019 Google Inc.
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

import static android.content.Context.SENSOR_SERVICE;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.google.android.accessibility.braille.common.TouchDots;
import java.util.Optional;

/** Auto detects whether the device is tabletop or screen away. */
public class LayoutOrientator {
  private final Context context;
  private final SensorManager sensorManager;
  private final LayoutOrientatorCallback callback;
  private Optional<TouchDots> autoModeLayout;
  private boolean uprightFacingUser;
  private boolean heldUprightInScreenAway;

  /** Callback for clients of this class. */
  public interface LayoutOrientatorCallback {
    boolean useSensorsToDetectLayout();

    void onDetectionChanged(boolean isTabletop, boolean firstChangedEvent);

    /**
     * Whether the device, held up as it is now, faces the user rather than facing away. Then it
     * uses the tabletop layout, as the user types on the front of the screen. Called for every
     * sensor reading while the device is upright, so it must be quick.
     */
    default boolean uprightFacesUser() {
      return false;
    }
  }

  public LayoutOrientator(Context context, LayoutOrientatorCallback layoutOrientatorCallback) {
    this.context = context;
    this.callback = layoutOrientatorCallback;
    this.autoModeLayout = Optional.empty();
    this.sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
  }

  /** Starts listening to sensors that inform the orientation detection */
  public void startIfNeeded() {
    if (callback.useSensorsToDetectLayout()) {
      Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
      sensorManager.registerListener(
          sensorEventListener, accelerometer, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
    }
  }

  /** Stops listening to sensors that inform the orientation detection */
  public void stop() {
    sensorManager.unregisterListener(sensorEventListener);
    autoModeLayout = Optional.empty();
    uprightFacingUser = false;
    heldUprightInScreenAway = false;
  }

  /** Whether the tabletop layout detected is for a device held up facing the user, not flat. */
  public boolean isUprightFacingUser() {
    return uprightFacingUser;
  }

  /**
   * Whether the device has been held upright since the layout last switched to screen-away, so
   * that typing in screen-away mode is not typing on a device held nearly flat and tilted.
   */
  public boolean wasHeldUprightInScreenAway() {
    return heldUprightInScreenAway;
  }

  /** Returns detected layout. Empty before sensor receives events. */
  public Optional<TouchDots> getDetectedLayout() {
    return autoModeLayout;
  }

  private final SensorEventListener sensorEventListener =
      new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
          float[] sensorEventValues =
              Utils.adjustAccelOrientation(
                  Utils.getDisplayRotationDegrees(context), sensorEvent.values);
          boolean isFlat = Utils.isFlat(sensorEventValues);
          boolean facing = !isFlat && callback.uprightFacesUser();
          boolean isTabletop = isFlat || facing;
          boolean firstChangedEvent = autoModeLayout.isEmpty();
          TouchDots newLayout = isTabletop ? TouchDots.TABLETOP : TouchDots.SCREEN_AWAY;
          boolean shouldChange =
              firstChangedEvent
                  || autoModeLayout.get() != newLayout
                  || facing != uprightFacingUser;
          if (isTabletop) {
            heldUprightInScreenAway = false;
          } else if (Utils.isUpright(sensorEventValues)) {
            heldUprightInScreenAway = true;
          }
          autoModeLayout = Optional.of(newLayout);
          uprightFacingUser = facing;
          if (shouldChange) {
            callback.onDetectionChanged(isTabletop, firstChangedEvent);
          }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {}
      };
}
