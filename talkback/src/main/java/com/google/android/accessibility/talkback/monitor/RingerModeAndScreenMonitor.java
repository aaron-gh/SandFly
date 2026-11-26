/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.google.android.accessibility.talkback.monitor;

import static androidx.core.content.ContextCompat.RECEIVER_EXPORTED;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.Display;
import androidx.core.content.ContextCompat;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline.FeedbackReturner;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.audio.AudioDeviceRouter;
import com.google.android.accessibility.talkback.clipboard.ClipboardHistoryOverlay;
import com.google.android.accessibility.talkback.contextmenu.ListMenuManager;
import com.google.android.accessibility.talkback.controller.TelevisionNavigationController;
import com.google.android.accessibility.talkback.feedbackpolicy.AppSpeechFilter;
import com.google.android.accessibility.talkback.notificationlist.NotificationListService;
import com.google.android.accessibility.talkback.pause.PauseController;
import com.google.android.accessibility.talkback.status.StatusReader;
import com.google.android.accessibility.talkback.status.StatusItem;
import com.google.android.accessibility.talkback.utils.DateTimeUtils;
import com.google.android.accessibility.talkback.utils.DateTimeUtils.TimeFeedbackFormat;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.broadcast.SameThreadBroadcastReceiver;
import com.google.android.accessibility.utils.monitor.DisplayMonitor;
import com.google.android.accessibility.utils.monitor.DisplayMonitor.DisplayStateChangedListener;
import com.google.android.accessibility.utils.output.FailoverTextToSpeech;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.checkerframework.checker.nullness.qual.Nullable;

// TODO: Refactor this class into two separate receivers
// with listener interfaces. This will remove the need to hold dependencies
// and call into other classes.
/** {@link BroadcastReceiver} for receiving updates for our context - device state */
public class RingerModeAndScreenMonitor extends SameThreadBroadcastReceiver
    implements DisplayStateChangedListener {

  private static final String TAG = "RingerModeAndScreenMon";

  /** The intent filter to match phone and screen state changes. */
  private static final IntentFilter STATE_CHANGE_FILTER = new IntentFilter();

  static {
    STATE_CHANGE_FILTER.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
    STATE_CHANGE_FILTER.addAction(Intent.ACTION_SCREEN_ON);
    STATE_CHANGE_FILTER.addAction(Intent.ACTION_SCREEN_OFF);
    STATE_CHANGE_FILTER.addAction(Intent.ACTION_USER_PRESENT);
  }

  private final TalkBackService service;
  private final FeedbackReturner pipeline;
  private final TelevisionNavigationController televisionNavigationController;
  private final AudioManager audioManager;
  private final PowerManager powerManager;
  private final ListMenuManager menuManager;
  private final CallStateMonitor callStateMonitor;
  private final Set<DialogInterface> openDialogs = new HashSet<>();
  private final boolean isWatch;
  @Nullable private final StatusReader statusReader;

  private final ScreenOnRecorder screenOnRecorder = new ScreenOnRecorder();

  /**
   * The items the last screen-on readout read, and when, in {@link SystemClock#uptimeMillis()}, so
   * that unlocking right after does not read them again. Main thread only.
   */
  private List<ScreenReadoutItem> screenOnReadItems = new ArrayList<>();

  private long screenOnReadAt;

  /** The current ringer mode. */
  private int ringerMode = AudioManager.RINGER_MODE_NORMAL;

  /** The time format of time feedback. */
  @TimeFeedbackFormat private int timeFormat = DateTimeUtils.TIME_FEEDBACK_FORMAT_UNDEFINED;

  // The system will send a screen on or screen off broadcast whenever the interactive state of the
  // device changes.
  private boolean isInteractive;
  // Records the default display state.
  private boolean defaultDisplayOn;
  private final DisplayMonitor displayMonitor;
  private boolean enabledTellingTime = true;

  /** The list containing screen changed listeners from other function callback. */
  private final List<ScreenChangedListener> screenChangedListeners = new CopyOnWriteArrayList<>();

  /** The list containing device unlocked listeners from other function callback. */
  private final List<DeviceUnlockedListener> deviceUnlockedListeners = new CopyOnWriteArrayList<>();

  private boolean monitoring = false;

  private final ExecutorService executor;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final WakeAnnouncementRecovery wakeAnnouncementRecovery;
  private final DisplayManager displayManager;

  /**
   * Whether the screen-on readout was spoken for another screen in this wake, so that the default
   * display turning on right after, such as when a flip phone is opened from its cover screen, does
   * not speak it again. Main thread only.
   */
  private boolean otherDisplayReadoutSpoken;

  @Override
  public void onDisplayStateChanged(boolean displayOn) {
    final boolean preDisplayOn = defaultDisplayOn;
    defaultDisplayOn = displayOn;
    if (!displayOn) {
      wakeAnnouncementRecovery.cancel();
    }
    if (defaultDisplayOn && !preDisplayOn) {
      screenOnRecorder.onScreenOnSignal(SystemClock.uptimeMillis());
      if (otherDisplayReadoutSpoken) {
        // Another screen already spoke the readout in this wake.
        otherDisplayReadoutSpoken = false;
        return;
      }
      // When the display turns on, we announce the current time and the current ringer
      // state when phone is idle.
      speakForDisplayOn(EVENT_ID_UNTRACKED);
    }
  }

  /** Creates a new instance. */
  public RingerModeAndScreenMonitor(
      ListMenuManager menuManager,
      FeedbackReturner pipeline,
      CallStateMonitor callStateMonitor,
      DisplayMonitor displayMonitor,
      @Nullable StatusReader statusReader,
      TalkBackService service) {
    if (menuManager == null) {
      throw new IllegalStateException();
    }
    if (pipeline == null) {
      throw new IllegalStateException();
    }

    this.service = service;
    this.menuManager = menuManager;
    this.pipeline = pipeline;
    this.callStateMonitor = callStateMonitor;
    this.displayMonitor = displayMonitor;
    televisionNavigationController = service.getTelevisionNavigationController();

    audioManager = (AudioManager) service.getSystemService(Service.AUDIO_SERVICE);
    // Otherwise the ringer mode stays normal until it changes after Backtalk starts.
    ringerMode = audioManager.getRingerMode();
    this.statusReader = statusReader;
    powerManager = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
    // noinspection deprecation
    isInteractive = powerManager.isInteractive();
    isWatch = FormFactorUtils.isAndroidWear();
    displayManager = (DisplayManager) service.getSystemService(Context.DISPLAY_SERVICE);
    executor = Executors.newSingleThreadExecutor();
    wakeAnnouncementRecovery =
        new WakeAnnouncementRecovery(
            mainHandler,
            () ->
                isWatch
                    && monitoring
                    && defaultDisplayOn
                    // The display can wake before SCREEN_ON updates isInteractive.
                    && ((PowerManager) service.getSystemService(Context.POWER_SERVICE))
                        .isInteractive()
                    && enabledTellingTime
                    && TalkBackService.isServiceActive()
                    && !PauseController.isPaused()
                    && isIdle(),
            () -> speakForDisplayOn(EVENT_ID_UNTRACKED, /* allowRecovery= */ false));
  }

  public void startMonitoring(Context context) {
    if (!monitoring) {
      ContextCompat.registerReceiver(context, this, STATE_CHANGE_FILTER, RECEIVER_EXPORTED);
      updateScreenState();
      displayMonitor.addDisplayStateChangedListener(this);
      monitoring = true;
    }
  }

  public void stopMonitoring(Context context) {
    wakeAnnouncementRecovery.cancel();
    if (monitoring) {
      context.unregisterReceiver(this);
      displayMonitor.removeDisplayStateChangedListener(this);
      monitoring = false;
    }
  }

  public void clearListeners() {
    screenChangedListeners.clear();
    deviceUnlockedListeners.clear();
  }

  @Override
  public void onReceiveIntent(Intent intent) {
    String action = intent.getAction();
    if (action == null) {
      return;
    }
    // The screen state is kept while SandFly is suspended too, so that it is right when SandFly
    // resumes.
    if (!TalkBackService.isServiceActive()) {
      if (Intent.ACTION_SCREEN_ON.equals(action)) {
        isInteractive = true;
      } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
        isInteractive = false;
      }
      return;
    }

    EventId eventId = EVENT_ID_UNTRACKED; // Frequently not user-initiated.

    switch (action) {
      case AudioManager.RINGER_MODE_CHANGED_ACTION ->
          handleRingerModeChanged(
              intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL));
      case Intent.ACTION_SCREEN_ON -> {
        isInteractive = true;
        screenOnRecorder.onScreenOnSignal(SystemClock.uptimeMillis());
        AudioDeviceRouter router = AudioDeviceRouter.getInstance();
        if (router != null) {
          router.ensureRouting();
          router.scheduleReassertion();
        }
        handleScreenOn(eventId);
        if (!defaultDisplayOn && isOtherDisplayOn()) {
          // Another screen, such as a flip phone's cover screen, woke instead of the default
          // display, which speaks the readout when it turns on.
          LogUtils.d(TAG, "Screen on with another display on; speaking the screen-on readout");
          speakForDisplayOn(eventId);
          otherDisplayReadoutSpoken = true;
        }
      }
      case Intent.ACTION_SCREEN_OFF -> {
        isInteractive = false;
        wakeAnnouncementRecovery.cancel();
        otherDisplayReadoutSpoken = false;
        screenOnReadItems = new ArrayList<>();
        screenOnRecorder.onScreenOff();
        AudioDeviceRouter router = AudioDeviceRouter.getInstance();
        if (router != null) {
          router.ensureRouting();
          router.scheduleReassertion();
        }
        handleScreenOff(eventId);
      }
      case Intent.ACTION_USER_PRESENT -> {
        AudioDeviceRouter router = AudioDeviceRouter.getInstance();
        if (router != null) {
          router.ensureRouting();
          router.scheduleReassertion();
        }
        handleDeviceUnlocked(eventId);
      }
    }
  }

  /**
   * Whether a screen other than the default display is on, such as a flip phone's cover screen.
   * Private displays, which apps make for themselves, such as a projection to a car, don't count.
   */
  private boolean isOtherDisplayOn() {
    for (Display display : displayManager.getDisplays()) {
      if (display.getDisplayId() != Display.DEFAULT_DISPLAY
          && (display.getFlags() & Display.FLAG_PRIVATE) == 0
          && display.getState() == Display.STATE_ON) {
        return true;
      }
    }
    return false;
  }

  public void updateScreenState() {
    // noinspection deprecation
    isInteractive =
        ((PowerManager) service.getSystemService(Context.POWER_SERVICE)).isInteractive();
  }

  /** When the screen last turned on, in {@link SystemClock#uptimeMillis()}, or 0. */
  public long getScreenOnUptime() {
    return screenOnRecorder.getScreenOnUptime();
  }

  public boolean isInteractive() {
    // SCREEN_ON can come late or not at all, which kept volume key presses off with the screen on,
    // so Android's own state is read while the screen is taken to be off.
    if (!isInteractive && powerManager.isInteractive()) {
      isInteractive = true;
    }
    return isInteractive;
  }

  /**
   * The state of the default {@link Display} is on or not.
   *
   * <p>Note: The value returned by {@link PowerManager#isInteractive()} only indicates whether the
   * device is in an interactive state which may have nothing to do with the screen being on or off.
   * To determine the actual state of the screen, use {@link Display#getState()}.
   */
  public boolean isDefaultDisplayOn() {
    return defaultDisplayOn;
  }

  /** Enables or disables telling time feature while waking up the display. */
  public void enableTellingTime(boolean enabled) {
    enabledTellingTime = enabled;
    if (!enabled) {
      wakeAnnouncementRecovery.cancel();
    }
    LogUtils.d(TAG, "enabledTellingTime=" + enabledTellingTime);
  }

  /** Handles when the device is unlocked. Speaks "Device unlocked" and the items chosen for it. */
  private void handleDeviceUnlocked(EventId eventId) {
    if (AppSpeechFilter.leavesOutReadouts()) {
      // Spoken while an assistant records, it would become part of what the user asks.
      LogUtils.i(TAG, "Unlock readout left out while an assistant or call is in use");
    } else if (isIdle()) {
      if (isWatch) {
        // REFERTO: Use chime instead of speech on watches, since it happens on every
        // screen wake whether or not the screen lock is enabled.
        pipeline.returnFeedback(eventId, Feedback.sound(R.raw.volume_beep));
      } else {
        SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
        final SpannableStringBuilder ttsText = new SpannableStringBuilder();
        if (ScreenAnnouncementSettings.isOn(prefs, ScreenAnnouncementSettings.SAY_UNLOCKED)) {
          ttsText.append(service.getString(R.string.value_device_unlocked));
        }
        // With face or fingerprint unlock, the screen-on readout has just read some of them.
        appendReadout(
            ttsText,
            ScreenAnnouncementSettings.withoutRepeats(
                readoutItems(prefs, ScreenEvent.UNLOCK),
                screenOnReadItems,
                screenOnReadAt,
                SystemClock.uptimeMillis()));
        SpeakOptions speakOptions =
            SpeakOptions.create()
                .setQueueMode(
                    SpeechController.QUEUE_MODE_INTERRUPT_AND_UNINTERRUPTIBLE_BY_NEW_SPEECH)
                .setFlags(FeedbackItem.FLAG_SKIP_DUPLICATE | overMediaFlag(prefs));
        if (ttsText.length() > 0) {
          pipeline.returnFeedback(eventId, Feedback.speech(ttsText, speakOptions));
        }
      }
    }
    for (DeviceUnlockedListener deviceUnlockedListener : deviceUnlockedListeners) {
      deviceUnlockedListener.onDeviceUnlocked();
    }
  }

  private void handleScreenOffInBackgroundThread(EventId eventId) {
    // REFERTO: Do not have any screen off message and any chime for Android Wear.
    // The volume of the screen-off sound, which plays only while the ringer rings, or -1 for none.
    float soundVolume = -1;
    if (!isWatch && ringerMode == AudioManager.RINGER_MODE_NORMAL) {
      // Gets the volume of SandFly's sounds, which use the speech volume.
      final float talkbackVolume =
          getStreamVolume(FailoverTextToSpeech.getSpeechAudioStream(service));
      if ((talkbackVolume > 0)
          && (audioManager.isWiredHeadsetOn() || audioManager.isBluetoothA2dpOn())) {
        // TODO: refactor the following lines.
        // Play the ringer beep on the default (music) stream to avoid
        // issues with ringer audio (e.g. no speech on ICS and
        // interruption of music on JB). Adjust playback volume to
        // compensate for music volume.
        final float ringVolume = getStreamVolume(AudioManager.STREAM_RING);
        soundVolume = Math.min(1.0f, (ringVolume / talkbackVolume));
      } else {
        soundVolume = 1.0f;
      }
    }

    // The rest uses the speech queue, the event queues and the status, which only the main thread
    // may use.
    final float screenOffSoundVolume = soundVolume;
    mainHandler.post(
        () -> {
          if (isInteractive) {
            // The screen came back on meanwhile, so it is too late to say it went off.
            return;
          }
          // Always reset the television remote mode to the standard (navigate) mode on screen off.
          if (televisionNavigationController != null) {
            televisionNavigationController.resetToNavigateMode();
          }

          // Stop queued speech and events. AccessibilityEventProcessor will block new events.
          service.clearQueues();

          if (isWatch) {
            return;
          }
          // Sent after clearing the queues, and uninterruptible, so that neither they nor the lock
          // screen or always-on display appearing right after the screen turns off cut it off.
          final SpannableStringBuilder ttsText = screenOffText();
          SpeakOptions speakOptions =
              SpeakOptions.create()
                  .setQueueMode(
                      SpeechController.QUEUE_MODE_INTERRUPT_AND_UNINTERRUPTIBLE_BY_NEW_SPEECH)
                  .setFlags(
                      FeedbackItem.FLAG_NO_HISTORY
                          | overMediaFlag(SharedPreferencesUtils.getSharedPreferences(service)));
          Feedback.Part.@Nullable Builder feedback = null;
          if (screenOffSoundVolume >= 0) {
            // Normally we'll play the volume beep on the ring stream.
            feedback =
                Feedback.part()
                    .setSound(Feedback.Sound.create(R.raw.screen_off, 1.0f, screenOffSoundVolume));
            if (ttsText.length() > 0) {
              feedback.speech(ttsText, speakOptions);
            }
          } else if (ttsText.length() > 0) {
            feedback = Feedback.speech(ttsText, speakOptions);
          }
          if (feedback != null) {
            pipeline.returnFeedback(eventId, feedback);
          }
        });
  }

  /** The flag that speaks an announcement during media playback while the setting is on. */
  private static int overMediaFlag(SharedPreferences prefs) {
    return ScreenAnnouncementSettings.isOn(prefs, ScreenAnnouncementSettings.OVER_MEDIA)
        ? FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
        : 0;
  }

  /** "Screen off", the ringer mode and the items chosen for the screen turning off. */
  private SpannableStringBuilder screenOffText() {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
    final SpannableStringBuilder ttsText = new SpannableStringBuilder();
    if (ScreenAnnouncementSettings.isOn(prefs, ScreenAnnouncementSettings.SAY_SCREEN_OFF)) {
      ttsText.append(service.getString(R.string.value_screen_off));
    }
    // Only announce the ringer state and the status if we're not in a call.
    if (isIdle()) {
      if (ScreenAnnouncementSettings.screenOffRingerMode(prefs)) {
        appendRingerStateAnnouncement(ttsText);
      }
      appendReadout(ttsText, readoutItems(prefs, ScreenEvent.SCREEN_OFF));
    }
    return ttsText;
  }

  /** Handles when the screen is turned off. Announces "screen off". */
  @SuppressWarnings("deprecation")
  private void handleScreenOff(EventId eventId) {
    menuManager.dismissAll();
    ClipboardHistoryOverlay.close();

    // Iterate over a copy because dialog dismiss handlers might try to unregister dialogs.
    List<DialogInterface> openDialogsCopy = new ArrayList<>(openDialogs);
    for (DialogInterface dialog : openDialogsCopy) {
      dialog.cancel();
    }
    openDialogs.clear();

    for (ScreenChangedListener screenChangedListener : screenChangedListeners) {
      if (screenChangedListener != null) {
        screenChangedListener.onScreenChanged(isInteractive, eventId);
      }
    }

    // Move non-ui tasks to background thread
    executor.execute(() -> handleScreenOffInBackgroundThread(eventId));
  }

  /** Handles when the screen is interactive. */
  private void handleScreenOn(EventId eventId) {
    if (!isWatch && ringerMode == AudioManager.RINGER_MODE_NORMAL) {
      final float volume;
      final float talkbackVolume =
          getStreamVolume(FailoverTextToSpeech.getSpeechAudioStream(service));
      if ((talkbackVolume > 0)
          && (audioManager.isWiredHeadsetOn() || audioManager.isBluetoothA2dpOn())) {
        final float ringVolume = getStreamVolume(AudioManager.STREAM_RING);
        volume = Math.min(1.0f, (ringVolume / talkbackVolume));
      } else {
        volume = 1.0f;
      }
      pipeline.returnFeedback(
          eventId,
          Feedback.part().setSound(Feedback.Sound.create(R.raw.screen_on, 1.0f, volume)));
    }
    for (ScreenChangedListener screenChangedListener : screenChangedListeners) {
      if (screenChangedListener != null) {
        screenChangedListener.onScreenChanged(isInteractive, eventId);
      }
    }
  }

  /**
   * Handles when the display is turned on. Announces the current time and the current ringer state
   * when phone is idle.
   */
  private void speakForDisplayOn(EventId eventId) {
    speakForDisplayOn(eventId, /* allowRecovery= */ true);
  }

  /** The system's return to the watch face can interrupt the announcement already sent to TTS. */
  public void onSystemInterrupt(Runnable interrupt) {
    if (isWatch) {
      wakeAnnouncementRecovery.onSystemInterrupt(interrupt);
    } else {
      interrupt.run();
    }
  }

  private void speakForDisplayOn(EventId eventId, boolean allowRecovery) {
    if (AppSpeechFilter.leavesOutReadouts()) {
      // An assistant woken by its hotword turns the screen on and records what the user says.
      LogUtils.i(TAG, "Screen-on readout left out while an assistant or call is in use");
      return;
    }
    final SpannableStringBuilder ttsText = new SpannableStringBuilder();

    if (isIdle()) {
      // Need the old version to support version older than JB_MR1
      // noinspection deprecation
      if (Settings.Secure.getInt(
              service.getContentResolver(), Settings.Secure.DEVICE_PROVISIONED, 0)
          != 0) {
        List<ScreenReadoutItem> items =
            readoutItems(
                SharedPreferencesUtils.getSharedPreferences(service), ScreenEvent.SCREEN_ON);
        appendReadout(ttsText, items);
        screenOnReadItems = items;
        screenOnReadAt = SystemClock.uptimeMillis();
      } else {
        // Device is not ready, just speak screen on
        ttsText.append(service.getString(R.string.value_screen_on));
      }
    }

    SpeakOptions speakOptions =
        SpeakOptions.create()
            // Uses QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH so that time announcement does not
            // interrupt DeviceUnlocked speech and REFERTO won't be interrupted by the
            // following selected text.
            .setQueueMode(SpeechController.QUEUE_MODE_GLOBALLY_UNINTERRUPTED)
            .setFlags(FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE);

    if (isWatch && enabledTellingTime && allowRecovery && ttsText.length() > 0) {
      speakOptions.setCompletedAction(wakeAnnouncementRecovery.track());
    }
    pipeline.returnFeedback(eventId, Feedback.speech(ttsText, speakOptions));
  }

  /** The items chosen for {@code event}, in the order they are read. */
  private List<ScreenReadoutItem> readoutItems(SharedPreferences prefs, ScreenEvent event) {
    return ScreenAnnouncementSettings.readoutItems(
        prefs,
        event,
        enabledTellingTime,
        statusReader != null && statusReader.isCharging(),
        statusReader != null ? statusReader.batteryLevel() : -1);
  }

  /**
   * Appends {@code items}, which are in the order {@link ScreenAnnouncementSettings#readoutItems}
   * gives, with the same wording at the screen turning off or on and the phone being unlocked.
   */
  private void appendReadout(SpannableStringBuilder builder, List<ScreenReadoutItem> items) {
    List<StatusItem> status = new ArrayList<>();
    for (ScreenReadoutItem item : items) {
      if (item == ScreenReadoutItem.TIME) {
        StringBuilderUtils.appendWithSeparator(
            builder, DateTimeUtils.getScreenOnTime(service, timeFormat));
      } else if (item == ScreenReadoutItem.NOTIFICATION_COUNT) {
        appendNotificationCount(builder);
      } else if (item.getStatus() != null) {
        status.add(item.getStatus());
      }
    }
    appendStatusAnnouncement(builder, status);
  }

  /**
   * Appends how many notifications there are, if there are any and SandFly's notification listener
   * is connected, which needs notification access.
   */
  private void appendNotificationCount(SpannableStringBuilder builder) {
    Integer count = NotificationListService.countedNotifications(service);
    if (count == null || count <= 0) {
      return;
    }
    // A full stop after the time, so that they do not run together.
    if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '.') {
      builder.append('.');
    }
    StringBuilderUtils.appendWithSeparator(
        builder,
        service
                .getResources()
                .getQuantityString(R.plurals.template_screen_on_notification_count, count, count)
            + ".");
  }

  /**
   * Appends the status {@code items}. They are spoken with the time, in one announcement that the
   * lock screen taking focus cannot interrupt.
   */
  private void appendStatusAnnouncement(SpannableStringBuilder builder, List<StatusItem> items) {
    if (statusReader == null || items.isEmpty()) {
      return;
    }
    // The screen turning off or on and unlocking are not requests, so do not ask for permission.
    String status = statusReader.describe(items, /* askForLocation= */ false);
    if (status.isEmpty()) {
      return;
    }
    // A full stop after the time, and after the status, so that they do not run together.
    if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '.') {
      builder.append('.');
    }
    StringBuilderUtils.appendWithSeparator(builder, status.endsWith(".") ? status : status + ".");
  }

  /**
   * Return current phone's call state is idle or not.
   *
   * @return true when phone is idle
   */
  private boolean isIdle() {
    return callStateMonitor != null
        && callStateMonitor.getCurrentCallState() == TelephonyManager.CALL_STATE_IDLE;
  }

  /** Handles when the ringer mode (ex. volume) changes. Announces the current ringer state. */
  private void handleRingerModeChanged(int ringerMode) {
    this.ringerMode = ringerMode;
  }

  /**
   * Appends the ringer state announcement to a {@link StringBuilder}.
   *
   * @param builder The string to append to.
   */
  private void appendRingerStateAnnouncement(SpannableStringBuilder builder) {
    if (callStateMonitor == null) {
      return;
    }

    final String announcement;

    switch (ringerMode) {
      case AudioManager.RINGER_MODE_SILENT ->
          announcement = service.getString(R.string.value_ringer_silent);
      case AudioManager.RINGER_MODE_VIBRATE ->
          announcement = service.getString(R.string.value_ringer_vibrate);
      case AudioManager.RINGER_MODE_NORMAL -> {
        return;
      }
      default -> {
        LogUtils.e(TAG, "Unknown ringer mode: %d", ringerMode);
        return;
      }
    }

    StringBuilderUtils.appendWithSeparator(builder, announcement);
  }

  /**
   * Returns the volume a stream as a fraction of its maximum volume.
   *
   * @param streamType The stream type for which to return the volume.
   * @return The stream volume as a fraction of its maximum volume.
   */
  private float getStreamVolume(int streamType) {
    final int currentVolume = audioManager.getStreamVolume(streamType);
    final int maxVolume = audioManager.getStreamMaxVolume(streamType);
    return (currentVolume / (float) maxVolume);
  }

  /** Registers a dialog to be auto-cancelled when the screen turns off. */
  public void registerDialog(DialogInterface dialog) {
    openDialogs.add(dialog);
  }

  /** Removes a dialog from the list of dialogs to be auto-cancelled. */
  public void unregisterDialog(DialogInterface dialog) {
    openDialogs.remove(dialog);
  }

  /** Listener interface to callback when screen on/off. */
  public interface ScreenChangedListener {
    void onScreenChanged(boolean isInteractive, EventId eventId);
  }

  /** Add listener which will be called when received screen on/off intent. */
  public void addScreenChangedListener(ScreenChangedListener listener) {
    screenChangedListeners.add(listener);
  }

  /** Listener interface to callback when device is unlocked. */
  public interface DeviceUnlockedListener {
    void onDeviceUnlocked();
  }

  /** Adds listener which will be called when device is unlocked. */
  public void addDeviceUnlockedListener(DeviceUnlockedListener listener) {
    deviceUnlockedListeners.add(listener);
  }

  /** Sets time feedback format. */
  public void setTimeFeedbackFormat(@TimeFeedbackFormat int value) {
    timeFormat = value;
  }

  /** Converts the time feedback preference string value to the variable. */
  @TimeFeedbackFormat
  public static int prefValueToTimeFeedbackFormat(Resources resources, String value) {
    if (TextUtils.equals(
        value, resources.getString(R.string.pref_time_feedback_format_values_12_hour))) {
      return DateTimeUtils.TIME_FEEDBACK_FORMAT_12_HOURS;
    } else if (TextUtils.equals(
        value, resources.getString(R.string.pref_time_feedback_format_values_24_hour))) {
      return DateTimeUtils.TIME_FEEDBACK_FORMAT_24_HOURS;
    }

    return DateTimeUtils.TIME_FEEDBACK_FORMAT_UNDEFINED;
  }
}
