package com.google.android.marvin.talkback;

import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.support.wearable.input.WearableButtons;
import android.support.wearable.input.WearableButtons.ButtonInfo;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import com.google.android.accessibility.talkback.DumpComponent;
import com.google.android.accessibility.talkback.MotionEventController;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.VibrationWatchIntegrator;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.Logger;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.List;
import kotlin.Unit;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Wear-specific changes to TalkBackService. */
public class TalkBackService extends com.google.android.accessibility.talkback.TalkBackService {
  private static final String TAG = "TalkBackServiceWear";
  private static final int NUM_STEM_BUTTONS_REQUIRED = 3;
  private static final boolean SUPPORT_VOLUME_CHANGE_BY_WEAR_KEY = false;
  private @Nullable Pair<Integer, Integer> volumeButtons;

  // A controller to intercept and handle motion events if needed.
  private @Nullable MotionEventController motionEventController;
  private @Nullable VibrationWatchIntegrator vibrationWatchIntegrator;

  @Override
  public void onCreate() {
    super.onCreate();
    if (SUPPORT_VOLUME_CHANGE_BY_WEAR_KEY) {
      volumeButtons = getVolumeStemButtons(this);
    }
  }

  @Override
  protected void onServiceConnected() {
    checkAudioOutput();
    super.onServiceConnected();
    setSupportClickableLinks(false);
  }

  @Override
  protected void onTalkbackInitialized(Pipeline pipeline) {
    if (FeatureFlagReader.supportRsbScrolling(this)
        && FeatureSupport.supportMotionEventSources()
        && FeatureSupport.supportsRotaryEncoder()) {
      motionEventController = new MotionEventController(this, pipeline.getFeedbackReturner());
      addEventListener(motionEventController);
    }
    if (isSystemApp(getApplicationInfo())) {
      vibrationWatchIntegrator = new VibrationWatchIntegrator(this, gestureController);
    }
  }

  private static boolean isSystemApp(ApplicationInfo applicationInfo) {
    return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
        || (applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
  }

  @Override
  protected void shutdownInfrastructure() {
    super.shutdownInfrastructure();
    if (motionEventController != null) {
      motionEventController.shutdown();
      motionEventController = null;
    }
    if (vibrationWatchIntegrator != null) {
      vibrationWatchIntegrator.onShutDown();
      vibrationWatchIntegrator = null;
    }
  }

  /** Check whether the device has a audio output available */
  private void checkAudioOutput() {
    PackageManager packageManager = getPackageManager();
    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
      AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
      for (AudioDeviceInfo device : devices) {
        switch (device.getType()) {
          case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ->
              LogUtils.v(TAG, "Wear System with built-in speaker.");
          case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ->
              LogUtils.v(TAG, "Wear System with Bluetooth audio.");
          default -> {}
        }
      }
    }
  }

  /** Transform the volume up and down buttons, if possible. */
  @Override
  protected boolean onKeyEventInternal(KeyEvent keyEvent) {
    // Do Wear specific transformations.
    if (SUPPORT_VOLUME_CHANGE_BY_WEAR_KEY) {
      int keyCode = keyEvent.getKeyCode();
      if (keyCode == KeyEvent.KEYCODE_STEM_1 || keyCode == KeyEvent.KEYCODE_STEM_2) {
        if (volumeButtons.first == keyCode) {
          keyEvent = copyKeyEventWithNewCode(keyEvent, KeyEvent.KEYCODE_VOLUME_UP);
        } else if (volumeButtons.second == keyCode) {
          keyEvent = copyKeyEventWithNewCode(keyEvent, KeyEvent.KEYCODE_VOLUME_DOWN);
        }
      }
    }
    return super.onKeyEventInternal(keyEvent);
  }

  @Override
  public void onMotionEvent(@NonNull MotionEvent event) {
    super.onMotionEvent(event);
    if (motionEventController != null) {
      motionEventController.onMotionEvent(event);
    }
  }

  /** Return volume up and down buttons */
  private Pair<Integer, Integer> getVolumeStemButtons(Context context) {
    Pair<Integer, Integer> volumeButtons = new Pair<>(KEYCODE_VOLUME_UP, KEYCODE_VOLUME_DOWN);
    // Check that this is a watch, based on BUILD rules it should be.
    if (!FormFactorUtils.isAndroidWear()) {
      return volumeButtons;
    }
    if (WearableButtons.getButtonCount(context) != NUM_STEM_BUTTONS_REQUIRED) {
      return volumeButtons;
    }
    ButtonInfo keyCodeStem1Info = WearableButtons.getButtonInfo(context, KeyEvent.KEYCODE_STEM_1);
    ButtonInfo keyCodeStem2Info = WearableButtons.getButtonInfo(context, KeyEvent.KEYCODE_STEM_2);
    if (keyCodeStem1Info == null || keyCodeStem2Info == null) {
      return volumeButtons;
    }
    float x1 = keyCodeStem1Info.getX();
    float x2 = keyCodeStem2Info.getX();
    float y1 = keyCodeStem1Info.getY();
    float y2 = keyCodeStem2Info.getY();
    // Check the X-position is on the same side.
    double center = context.getResources().getDisplayMetrics().widthPixels * 0.5;
    if (!(x1 > center && x2 > center) || (x1 < center && x2 < center)) {
      return volumeButtons;
    }
    // Check the Y-position to determine the top and bottom button.
    if (y1 > y2) {
      return new Pair<>(KeyEvent.KEYCODE_STEM_2, KeyEvent.KEYCODE_STEM_1);
    } else {
      return new Pair<>(KeyEvent.KEYCODE_STEM_1, KeyEvent.KEYCODE_STEM_2);
    }
  }

  /** Change only the Code and keep all the other values in KeyEvent. */
  private KeyEvent copyKeyEventWithNewCode(KeyEvent sourceEvent, int newCode) {
    return new KeyEvent(
        sourceEvent.getDownTime(),
        sourceEvent.getEventTime(),
        sourceEvent.getAction(),
        newCode,
        sourceEvent.getRepeatCount(),
        sourceEvent.getMetaState(),
        sourceEvent.getDeviceId(),
        sourceEvent.getScanCode(),
        sourceEvent.getFlags(),
        sourceEvent.getSource());
  }

  final DumpComponent componentMotionEventController =
      new DumpComponent(
          "motion_event_controller",
          "dump the current state in the MotionEventController",
          dumpLogger -> {
            dumpMotionEventController(dumpLogger);
            return Unit.INSTANCE;
          });

  private void dumpMotionEventController(Logger dumpLogger) {
    if (motionEventController != null) {
      motionEventController.dump(dumpLogger);
    }
  }

  @Override
  protected void inflateDumpComponents(List<DumpComponent> outDumpComponentList) {
    outDumpComponentList.add(componentMotionEventController);
    super.inflateDumpComponents(outDumpComponentList);
  }
}
