/*
 * Copyright 2015 Google Inc.
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

package com.google.android.accessibility.talkback.actor;

import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowMetrics;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.DimmingOverlayView;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Feedback.Part;
import com.google.android.accessibility.talkback.Feedback.Speech;
import com.google.android.accessibility.talkback.Feedback.Speech.Action;
import com.google.android.accessibility.talkback.Pipeline.FeedbackReturner;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.compositor.Compositor;
import com.google.android.accessibility.talkback.compositor.Compositor.Flavor;
import com.google.android.accessibility.talkback.gesture.GestureHints;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping;
import com.google.android.accessibility.talkback.monitor.DeviceConfigurationMonitor.OnConfigurationChangedListener;
import com.google.android.accessibility.talkback.utils.FocusIndicatorUtils;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.monitor.ScreenMonitor;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.accessibility.utils.widget.DialogUtils;
import java.util.concurrent.TimeUnit;

/** Manages UI and state related to dimming the screen */
public class DimScreenActor implements OnConfigurationChangedListener {

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Constants

  private static final float MAX_DIM_AMOUNT = 0.9f;
  private static final float MIN_BRIGHTNESS = 0.1f;

  private static final int START_DIMMING_MESSAGE = 1;
  private static final int UPDATE_TIMER_MESSAGE = 2;

  private static final int INSTRUCTION_VISIBLE_SECONDS = 180;

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Inner class for actor-state reader

  /** Read-only interface for actor-state data. */
  public class State {
    public boolean isDimmingEnabled() {
      return DimScreenActor.this.isDimmingEnabled();
    }

    public boolean isInstructionDisplayed() {
      return DimScreenActor.this.isInstructionDisplayed();
    }
  }

  public final State state = new State();

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Member data

  private final AccessibilityService service;
  private final InitSearchScreenOverlay initSearchScreenOverlay;
  private FeedbackReturner pipeline;
  private final SharedPreferences prefs;
  private final DimScreenNotifier dimScreenNotifier;
  private boolean isDimmed;
  private boolean stopFeedback;
  private WindowManager windowManager;
  @Nullable private LayoutParams viewParams;
  @Nullable private DimmingOverlayView view;
  @Nullable private DimScreenDialog dimScreenDialog;
  private int currentInstructionVisibleTime;
  private boolean isInstructionDisplayed;
  private final GestureShortcutMapping gestureShortcutMapping;

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Methods

  private final Handler dimmingHandler =
      new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
          switch (message.what) {
            case START_DIMMING_MESSAGE -> {
              currentInstructionVisibleTime = INSTRUCTION_VISIBLE_SECONDS;
              isInstructionDisplayed = true;
              sendEmptyMessage(UPDATE_TIMER_MESSAGE);
            }
            case UPDATE_TIMER_MESSAGE -> {
              currentInstructionVisibleTime--;
              if (currentInstructionVisibleTime > 0) {
                updateText(currentInstructionVisibleTime);
                sendEmptyMessageDelayed(UPDATE_TIMER_MESSAGE, TimeUnit.SECONDS.toMillis(1));
              } else {
                hideInstructionAndTurnOnDimming();
              }
            }
            default -> {}
          }
        }
      };

  /**
   * Checks if dim/brighten screen is supported by the platform.
   *
   * <p>Note: This function just checks API restrictions. Availability of dim/brighten screen is
   * also dependent on the current device lock state.
   *
   * @see #isSupported(TalkBackService).
   */
  public static boolean isSupportedbyPlatform(TalkBackService service) {
    // Screen dimming is disabled on Jasper because there is no easy way to exit this mode since
    // Talkback cannot capture volume button key presses on this platform.
    @Flavor int compositorFlavor = service.getCompositorFlavor();
    return compositorFlavor != Compositor.FLAVOR_JASPER;
  }

  /**
   * Decides whether to support dim and brighten screen functionality. Dim/brighten screen support
   * depends on API level and current device lock state.
   */
  public static boolean isSupported(TalkBackService service) {
    if (!isSupportedbyPlatform(service)) {
      return false;
    }
    return !ScreenMonitor.isDeviceLocked(service);
  }

  public static boolean isDimScreenEnabled(Context context, SharedPreferences prefs) {
    return SharedPreferencesUtils.getBooleanPref(
        prefs,
        context.getResources(),
        R.string.pref_dim_when_talkback_enabled_key,
        R.bool.pref_dim_when_talkback_enabled_default);
  }

  public DimScreenActor(
      TalkBackService service,
      InitSearchScreenOverlay initSearchScreenOverlay,
      GestureShortcutMapping gestureShortcutMapping,
      DimScreenNotifier dimScreenNotifier) {
    this.service = service;
    this.initSearchScreenOverlay = initSearchScreenOverlay;
    this.gestureShortcutMapping = gestureShortcutMapping;
    this.dimScreenNotifier = dimScreenNotifier;
    prefs = SharedPreferencesUtils.getSharedPreferences(service);
    stopFeedback = false;

    dimScreenDialog = new DimScreenDialog(service, this);
  }

  public void setPipeline(FeedbackReturner pipeline) {
    this.pipeline = pipeline;
  }

  public boolean isDimmingEnabled() {
    return isDimScreenEnabled(service, prefs);
  }

  /**
   * Whether the curtain covers the screen now. The setting can be on while it doesn't, such as
   * while SandFly is paused.
   */
  public boolean isScreenHidden() {
    return isDimmed;
  }

  /**
   * Turn on screen dimming without setting the shared preference. Used by {@link #resume()} and
   * {@link DimScreenDialog}
   */
  public void makeScreenDim() {
    makeScreenDim(SpeechController.QUEUE_MODE_INTERRUPT);
  }

  private void makeScreenDim(int queueMode) {

    initSearchScreenOverlay.initOverlay();

    if (!isDimmed) {
      isDimmed = true;
      initView();
      addExitInstructionView();
      FocusIndicatorUtils.setAccessibilityFocusAppearance(
          service,
          service
              .getResources()
              .getDimensionPixelSize(R.dimen.accessibility_focus_highlight_stroke_width),
          Color.BLACK);
      if (!getShouldShowDialogPref()) {
        hideInstructionAndTurnOnDimming();
      } else {
        startDimmingCount();
      }
      dimScreenNotifier.onScreenDim();
      announceFeedbackAndUsageHintForScreenDimmed(queueMode);
      requestBrightnessPermission();
      return;
    }

    announceFeedbackAndUsageHintForScreenDimmed(queueMode);
  }

  /**
   * The first time the screen is hidden without permission to modify system settings, asks for it,
   * so that the brightness can be lowered while the screen is hidden.
   */
  private void requestBrightnessPermission() {
    boolean opened =
        HiddenScreenBrightness.requestPermissionOnce(
            service,
            () -> {
              if (isDimmed && !isInstructionDisplayed) {
                HiddenScreenBrightness.lower(service);
              }
            });
    if (opened && pipeline != null) {
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.speech(
              service.getString(R.string.brightness_needs_permission),
              announcementOptions(SpeechController.QUEUE_MODE_QUEUE)));
    }
  }

  private void initView() {
    if (viewParams == null || view == null) {
      windowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);

      viewParams = new LayoutParams();
      viewParams.type = DialogUtils.getDialogType();
      viewParams.flags |= LayoutParams.FLAG_NOT_FOCUSABLE;
      viewParams.flags |= LayoutParams.FLAG_NOT_TOUCHABLE;
      viewParams.flags |= LayoutParams.FLAG_FULLSCREEN;
      viewParams.flags |= LayoutParams.FLAG_LAYOUT_NO_LIMITS;
      viewParams.flags |= LayoutParams.FLAG_LAYOUT_IN_SCREEN;
      // Without a gravity the curtain is centred in a frame that leaves out the navigation bar, so
      // half the bar's height stayed uncovered at the bottom. The 1-pixel gap is at the bottom edge.
      viewParams.gravity = Gravity.TOP | Gravity.START;
      viewParams.x = 0;
      viewParams.y = 0;
      // Cover the camera cutout too, so the status bar doesn't show beside it.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Lay out over the status and navigation bars.
        viewParams.setFitInsetsTypes(0);
        viewParams.setFitInsetsSides(0);
        viewParams.layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        viewParams.layoutInDisplayCutoutMode =
            LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
      }
      viewParams.flags &= ~LayoutParams.FLAG_TURN_SCREEN_ON;
      viewParams.flags &= ~LayoutParams.FLAG_KEEP_SCREEN_ON;
      // Not OPAQUE: the window manager ignores FLAG_KEEP_SCREEN_ON on windows behind an opaque
      // window that fills the display. Before Android 15 the size it compares against excludes the
      // navigation bar and cutout, so the gap below doesn't stop an opaque curtain from filling it,
      // and a video under the curtain no longer kept the screen on. The view still draws black.
      viewParams.format = PixelFormat.TRANSLUCENT;

      view = new DimmingOverlayView(service);
      view.setTimerLimit(INSTRUCTION_VISIBLE_SECONDS);
    }

    view.setInstruction(exitInstruction());

    initCurtainSize();
  }

  private void initCurtainSize() {
    if (viewParams == null) {
      return;
    }

    int width;
    int height;

    if (FeatureSupport.supportWindowMetrics()) {
      WindowMetrics windowMetrics = windowManager.getCurrentWindowMetrics();

      // The curtain lays out into the camera cutout, so it starts at the top of the display, and the
      // bounds already include the cutout. Adding its height as well covered the pixel left below.
      width = windowMetrics.getBounds().width();
      height = windowMetrics.getBounds().height();
    } else {
      Point point = new Point();
      windowManager.getDefaultDisplay().getRealSize(point);

      width = point.x;
      height = point.y;
    }

    // b/404312072: Hiding the entire screen disables the wake lock. TB avoids this by leaving a
    // small portion uncovered.
    height -= 1;

    viewParams.width = width;
    viewParams.height = height;
  }

  private void addExitInstructionView() {
    viewParams.dimAmount = MAX_DIM_AMOUNT;
    viewParams.screenBrightness = LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
    viewParams.buttonBrightness = MIN_BRIGHTNESS;
    windowManager.addView(view, viewParams);
    view.showText();
  }

  private void startDimmingCount() {
    dimmingHandler.sendEmptyMessage(START_DIMMING_MESSAGE);
  }

  private void updateText(int secondsLeft) {
    view.updateSecondsText(secondsLeft);
  }

  private void hideInstructionAndTurnOnDimming() {
    viewParams.dimAmount = MAX_DIM_AMOUNT;
    viewParams.screenBrightness = MIN_BRIGHTNESS;
    viewParams.buttonBrightness = viewParams.screenBrightness;
    isInstructionDisplayed = false;
    view.hideText();
    updateView();
    HiddenScreenBrightness.lower(service);
  }

  public boolean isInstructionDisplayed() {
    return isInstructionDisplayed;
  }

  private void updateView() {
    if (isDimmed) {
      windowManager.removeViewImmediate(view);
      windowManager.addView(view, viewParams);
    }
  }

  public void resume() {
    if (isDimmingEnabled()) {
      makeScreenDim();
    } else {
      // Restores the brightness that SandFly lowered if it stopped while the screen was hidden.
      HiddenScreenBrightness.restore(service);
    }
  }

  /**
   * Hides the screen again after a pause if the setting is on. "Screen hidden" is queued after
   * "resumed".
   */
  public void resumeAfterPause() {
    if (isDimmingEnabled()) {
      makeScreenDim(SpeechController.QUEUE_MODE_QUEUE);
    }
  }

  public void suspend() {
    makeScreenBright();
    if (dimScreenDialog != null) {
      dimScreenDialog.cancelDialog();
    }
  }

  public void shutdown() {
    stopFeedback = true;
    suspend();
    viewParams = null;
    view = null;
  }

  /** Turns off screen dimming without setting the shared preference. */
  private void makeScreenBright() {
    boolean wasDimmed = isDimmed;
    if (isDimmed) {
      isDimmed = false;
      isInstructionDisplayed = false;

      windowManager.removeViewImmediate(view);
      dimmingHandler.removeMessages(START_DIMMING_MESSAGE);
      dimmingHandler.removeMessages(UPDATE_TIMER_MESSAGE);

      dimScreenNotifier.onScreenBright();
    }
    HiddenScreenBrightness.restore(service);
    // Not to feedback since this actor is shut down.
    if (!stopFeedback) {
      Part.Builder feedback =
          Feedback.speech(
              service.getString(R.string.screen_brightness_restored),
              announcementOptions(SpeechController.QUEUE_MODE_INTERRUPT));
      // The sound plays only when the curtain was up, as the screen is then shown again.
      if (wasDimmed) {
        feedback.setSound(Feedback.Sound.create(R.raw.screen_shown));
      }
      pipeline.returnFeedback(EVENT_ID_UNTRACKED, feedback);
    }
  }

  public void disableDimming() {
    makeScreenBright();
    SharedPreferencesUtils.putBooleanPref(
        prefs, service.getResources(), R.string.pref_dim_when_talkback_enabled_key, false);
    FocusIndicatorUtils.applyFocusAppearancePreference(service, prefs, service.getResources());
  }

  public boolean getShouldShowDialogPref() {
    return (dimScreenDialog == null) || dimScreenDialog.getShouldShowDialogPref();
  }

  public boolean enableDimmingAndShowConfirmDialog() {
    if (isDimmingEnabled()) {
      announceFeedbackAndUsageHintForScreenDimmed();
      return false;
    }

    return (dimScreenDialog == null) ? false : dimScreenDialog.showDialogThenDimScreen();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    if (isDimmed) {
      windowManager.removeViewImmediate(view);
    }
    view = null;
    initView();
    if (isDimmed) {
      if (isInstructionDisplayed) {
        view.showText();
      } else {
        view.hideText();
      }
      windowManager.addView(view, viewParams);
    }
  }

  private void announceFeedbackAndUsageHintForScreenDimmed() {
    announceFeedbackAndUsageHintForScreenDimmed(SpeechController.QUEUE_MODE_INTERRUPT);
  }

  private void announceFeedbackAndUsageHintForScreenDimmed(int queueMode) {
    pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Part.builder()
            .setSpeech(
                Speech.builder()
                    .setAction(Action.SPEAK)
                    .setText(service.getString(R.string.screen_dimmed))
                    .setOptions(announcementOptions(queueMode))
                    .build())
            .setSound(Feedback.Sound.create(R.raw.screen_hidden)));
    // Users who turned off the confirmation dialog already know how to show the screen again.
    if (!getShouldShowDialogPref()) {
      return;
    }
    // The prompt for exiting Hide Screen mode. Need to separate it as a single Speech, instead of a
    // hint, because it will always be interrupted by window changed event if Hide Screen is
    // triggered from TalkBack menu or the confirm dialog.
    pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Part.builder()
            .setSpeech(
                Speech.builder()
                    .setAction(Action.SPEAK)
                    .setText(exitInstruction())
                    .setOptions(announcementOptions(SpeechController.QUEUE_MODE_QUEUE))
                    .build()));
  }

  /**
   * How to show the screen again, with the user's gestures: from the Backtalk menu if a gesture
   * opens it, or else with the gesture for hiding and showing the screen, or else where gestures
   * are assigned.
   */
  private String exitInstruction() {
    String menuGesture =
        GestureHints.gestureInSentence(
            service,
            gestureShortcutMapping,
            service.getString(R.string.shortcut_value_talkback_breakout));
    if (menuGesture != null) {
      return service.getString(
          R.string.screen_dimming_exit_instruction_line2,
          menuGesture,
          service.getString(R.string.shortcut_disable_dimming));
    }
    return GestureHints.actionLine(
        service, gestureShortcutMapping, R.string.shortcut_value_show_hide_screen);
  }

  /**
   * Options for saying that the screen was hidden or shown. The user asked for it, so it is said
   * even over other audio, or Backtalk's own speech, which would otherwise silence it. Without
   * that, hiding or showing the screen while something played went unannounced.
   */
  private static SpeakOptions announcementOptions(int queueMode) {
    return SpeakOptions.create()
        .setQueueMode(queueMode)
        .setFlags(
            FeedbackItem.FLAG_FORCE_FEEDBACK_ALL);
  }

  @VisibleForTesting
  @Nullable
  CharSequence getInstruction() {
    return view == null ? null : view.getInstruction();
  }

  /** Notifies the listener for the status changed of screen. */
  public interface DimScreenNotifier {
    /** Notifies if the screen is hiding. */
    void onScreenDim();

    /** Notifies if the screen is showing. */
    void onScreenBright();
  }

  /**
   * Init search-screen overlay early before dimming overlay to make search-screen overlay window
   * order beneath dimming overlay.
   */
  public interface InitSearchScreenOverlay {
    void initOverlay();
  }
}
