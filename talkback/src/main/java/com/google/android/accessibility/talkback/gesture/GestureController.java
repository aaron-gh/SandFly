/*
 * Copyright (C) 2015 Google Inc.
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

package com.google.android.accessibility.talkback.gesture;

import static android.accessibilityservice.AccessibilityService.GESTURE_DOUBLE_TAP_AND_HOLD;
import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS;
import static com.google.android.accessibility.talkback.Feedback.AdjustVolume.Action.DECREASE_VOLUME;
import static com.google.android.accessibility.talkback.Feedback.AdjustVolume.Action.INCREASE_VOLUME;
import static com.google.android.accessibility.talkback.Feedback.AdjustVolume.StreamType.STREAM_TYPE_ACCESSIBILITY;
import static com.google.android.accessibility.talkback.Feedback.BrailleDisplay.Action.TOGGLE_BRAILLE_DISPLAY_ON_OR_OFF;
import static com.google.android.accessibility.talkback.Feedback.ContinuousRead.Action.START_AT_CURSOR;
import static com.google.android.accessibility.talkback.Feedback.ContinuousRead.Action.START_AT_CURSOR_BY_READING_CONTROL;
import static com.google.android.accessibility.talkback.Feedback.ContinuousRead.Action.START_AT_TOP;
import static com.google.android.accessibility.talkback.Feedback.DimScreen.Action.BRIGHTEN;
import static com.google.android.accessibility.talkback.Feedback.DimScreen.Action.DIM;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.CLEAR_SELECTION;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.APPEND_COPY;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.COPY;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.CURSOR_TO_BEGINNING;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.CURSOR_TO_END;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.CUT;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.END_SELECT;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.INSERT;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.PASTE;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.CLEAR_TEXT;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.SELECT_ALL;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.START_SELECT;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.CLICK_CURRENT;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.DOUBLE_CLICK_CURRENT;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.LONG_CLICK_CURRENT;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.TOUCH_CLICK_CURRENT;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.CLICK_NODE;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.NEXT_GRANULARITY;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.NEXT_PAGE;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.PREVIOUS_GRANULARITY;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.PREVIOUS_PAGE;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.SCROLL_DOWN;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.SCROLL_LEFT;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.SCROLL_RIGHT;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.SCROLL_UP;
import static com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.PASSTHROUGH_CONFIRM_DIALOG;
import static com.google.android.accessibility.talkback.Feedback.Speech.Action.COPY_LAST;
import static com.google.android.accessibility.talkback.Feedback.Speech.Action.REPEAT_LAST;
import static com.google.android.accessibility.talkback.Feedback.Speech.Action.REPEAT_LAST_SLOWER;
import static com.google.android.accessibility.talkback.Feedback.Speech.Action.SPELL_LAST;
import static com.google.android.accessibility.talkback.Feedback.Speech.Action.TOGGLE_VOICE_FEEDBACK;
import static com.google.android.accessibility.talkback.Feedback.UniversalSearch.Action.CANCEL_SEARCH;
import static com.google.android.accessibility.talkback.Feedback.UniversalSearch.Action.TOGGLE_SEARCH;
import static com.google.android.accessibility.talkback.Feedback.VoiceRecognition.Action.START_LISTENING_IF_SCREEN_NOT_LOCKED;
import static com.google.android.accessibility.talkback.actor.SystemActionPerformer.GLOBAL_ACTION_ACCESSIBILITY_BUTTON;
import static com.google.android.accessibility.talkback.actor.SystemActionPerformer.GLOBAL_ACTION_ACCESSIBILITY_BUTTON_CHOOSER;
import static com.google.android.accessibility.talkback.actor.SystemActionPerformer.GLOBAL_ACTION_KEYCODE_HEADSETHOOK;
import static com.google.android.accessibility.talkback.actor.TalkBackUIActor.Type.GESTURE_OR_KEYBOARD_ACTION_OVERLAY;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.BRAILLE_COMMAND_PROFILE;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.CONTEXT;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.CUSTOM_ACTION;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.EDITING;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.GESTURE_PROFILE;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.LANGUAGE;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.SETTINGS_PROFILE;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.SPLIT_SCREEN;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.TEXT_ENTITIES;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.VOICE_PROFILE;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.ANNOUNCE_REAL_ACTION;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.UNKNOWN_ANNOUNCEMENT;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;
import static com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_FAKED_SPLIT_TYPING_AND_HOLD;
import static com.google.android.accessibility.utils.input.CursorGranularity.DEFAULT;
import static com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_TOUCH;
import static com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_LEFT;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_RIGHT;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.FingerprintGestureController;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.KeyEvent;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.recognition.PictureDescriptionActivity;
import com.google.android.accessibility.talkback.report.AiResponseReport;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Feedback.GeminiRequest;
import com.google.android.accessibility.talkback.Feedback.TalkBackUI;
import com.google.android.accessibility.talkback.Feedback.TalkBackUI.Item;
import com.google.android.accessibility.talkback.Feedback.TriggerIntent.Action;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.commands.AppGestureFragment;
import com.google.android.accessibility.talkback.commands.ButtonCommands;
import com.google.android.accessibility.talkback.commands.Commands;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.ContinuousCopy;
import com.google.android.accessibility.talkback.actor.ImageSharer;
import com.google.android.accessibility.talkback.actor.SpeechRateAndPitchActor;
import com.google.android.accessibility.talkback.actor.SystemActionPerformer;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.backup.SettingsProfiles;
import com.google.android.accessibility.talkback.commands.CommandMenus;
import com.google.android.accessibility.talkback.commands.MenuCommands;
import com.google.android.accessibility.talkback.contextmenu.ListMenuManager;
import com.google.android.accessibility.talkback.dialog.WordSelectionDialog;
import com.google.android.accessibility.talkback.eventprocessor.ProcessorPhoneticLetters;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor;
import com.google.android.accessibility.talkback.focusmanagement.NavigationTarget;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenStateMonitor;
import com.google.android.accessibility.talkback.blackberry.BlackBerryMode;
import com.google.android.accessibility.talkback.blackberry.BlackBerryModeDialog;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping.TalkbackAction;
import com.google.android.accessibility.talkback.interpreters.AccessibilityFocusInterpreter;
import com.google.android.accessibility.talkback.macros.MacroRecorder;
import com.google.android.accessibility.talkback.macros.MacroRunner;
import com.google.android.accessibility.talkback.macros.Macros;
import com.google.android.accessibility.talkback.menurules.RuleTextFormatting;
import com.google.android.accessibility.talkback.monitor.BatteryMonitor;
import com.google.android.accessibility.talkback.monitor.VolumeMonitor;
import com.google.android.accessibility.talkback.monitor.VolumeMonitor.VolumeChangedListener;
import com.google.android.accessibility.talkback.notifications.NotificationReader;
import com.google.android.accessibility.talkback.notifications.SpokenSchedules;
import com.google.android.accessibility.talkback.quicksettings.TileCommands;
import com.google.android.accessibility.talkback.recognition.LiveRecognition;
import com.google.android.accessibility.talkback.scripting.ItemInspector;
import com.google.android.accessibility.talkback.scripting.ScriptCommands;
import com.google.android.accessibility.talkback.scripting.Scripts;
import com.google.android.accessibility.talkback.calls.AnswerCall;
import com.google.android.accessibility.talkback.calls.CallerAnnouncer;
import com.google.android.accessibility.talkback.calls.EndCall;
import com.google.android.accessibility.talkback.actor.voicecommands.VoiceEditing;
import com.google.android.accessibility.talkback.dictation.AiDictation;
import com.google.android.accessibility.talkback.directtouch.DirectTouchController;
import com.google.android.accessibility.talkback.doubletaptaps.DoubleTapTapsApps;
import com.google.android.accessibility.talkback.clipboard.ClipboardHistoryOverlay;
import com.google.android.accessibility.talkback.speechhistory.SpeechHistoryFragment;
import com.google.android.accessibility.talkback.granularediting.CharacterCount;
import com.google.android.accessibility.talkback.granularediting.TextFieldContents;
import com.google.android.accessibility.talkback.granularediting.GranularEditing;
import com.google.android.accessibility.talkback.applauncher.BrailleScreenInput;
import com.google.android.accessibility.talkback.applauncher.BrailleScreenInputHost;
import com.google.android.accessibility.talkback.braillecommands.BrailleCommands;
import com.google.android.accessibility.talkback.handwriting.HandwritingAppLauncher;
import com.google.android.accessibility.talkback.handwriting.HandwritingCommand;
import com.google.android.accessibility.talkback.sandflyai.SandflyAi;
import com.google.android.accessibility.talkback.speech.LanguageSwitchCommands;
import com.google.android.accessibility.talkback.selector.ReadingControlLists;
import com.google.android.accessibility.talkback.translation.TranslateCommand;
import com.google.android.accessibility.talkback.selector.ReadingControlCommands;
import com.google.android.accessibility.talkback.selector.SelectorController;
import com.google.android.accessibility.talkback.speech.VoiceProfileNames;
import com.google.android.accessibility.talkback.selector.SelectorController.AnnounceType;
import com.google.android.accessibility.talkback.selector.SelectorController.Setting;
import com.google.android.accessibility.talkback.selector.SwipeValueDirection;
import com.google.android.accessibility.talkback.status.StatusReader;
import com.google.android.accessibility.talkback.trainingcommon.TrainingActivity;
import com.google.android.accessibility.talkback.trainingcommon.content.TutorialGestureUses;
import com.google.android.accessibility.talkback.trainer.GestureTrainerHooks;
import com.google.android.accessibility.talkback.tutorial.TutorialHooks;
import com.google.android.accessibility.talkback.utils.DateTimeUtils;
import com.google.android.accessibility.talkback.utils.DebugProperties;
import com.google.android.accessibility.talkback.utils.FocusIndicatorUtils;
import com.google.android.accessibility.talkback.utils.GeminiCommandUtils;
import com.google.android.accessibility.talkback.keyboard.CharacterChooser;
import com.google.android.accessibility.talkback.keyboard.KeyComboManager;
import com.google.android.accessibility.talkback.keyboard.SandFlyKeyboardHost;
import com.google.android.accessibility.talkback.labeling.LabelDialogManager;
import com.google.android.accessibility.talkback.keyboard.NextKeyboard;
import com.google.android.accessibility.talkback.overview.PreviousApp;
import com.google.android.accessibility.talkback.overview.RecentAppSteps;
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.monitor.RecentUserInput;
import com.google.android.accessibility.talkback.touchlock.TouchLock;
import com.google.android.accessibility.utils.output.OutputMute;
import com.google.android.accessibility.utils.KeyboardUtils;
import com.google.android.accessibility.utils.labeling.Label;
import com.google.android.accessibility.utils.labeling.LabelManager;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.TreeDebug;
import com.google.android.accessibility.utils.TreeDebugRenderer;
import com.google.android.accessibility.utils.WindowUtils;
import com.google.android.accessibility.utils.gestures.GestureConfiguration;
import com.google.android.accessibility.utils.input.CursorGranularity;
import com.google.android.accessibility.utils.input.ScrollEventInterpreter;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.GestureSpeechTiming;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.accessibility.utils.output.SpeechControllerImpl;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Class to handle incoming gestures to TalkBack. Make sure tutorial still works TODO: Map
 * actions to ints, and store in a map that changes with prefs
 */
public class GestureController {

  private static final String LOG_TAG = "GestureController";

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Member variables

  private final TalkBackService service;
  private final Pipeline.FeedbackReturner pipeline;
  private final ActorState actorState;
  private final ListMenuManager menuManager;
  private final SelectorController selectorController;
  private final AccessibilityFocusMonitor accessibilityFocusMonitor;
  private final AccessibilityFocusInterpreter accessibilityFocusInterpreter;
  private final GestureShortcutMapping gestureShortcutMapping;
  private final TalkBackAnalytics analytics;
  private final VolumeMonitor volumeMonitor;
  private final BatteryMonitor batteryMonitor;
  private final StatusReader statusReader;
  private final SpeechRateAndPitchActor speechRateAndPitchActor;
  private final SpeechControllerImpl speaker;
  private final ScreenStateMonitor.State screenState;
  private final ProcessorPhoneticLetters processorPhoneticLetters;
  private final ListEdgeScroller listEdgeScroller;
  private final PullToRefresh pullToRefresh;
  private final SwipeUp swipeUp;
  private final HandwritingCommand handwritingCommand;
  private final TranslateCommand translateCommand;
  private final HandwritingAppLauncher handwritingAppLauncher;
  private final BrailleScreenInput brailleScreenInput;
  private final AiDictation dictation;
  // For Speak position. Set once the service has made them.
  private @Nullable ScrollEventInterpreter scrollEventInterpreter;
  private @Nullable TextTraversalTracker textTraversalTracker;

  private final @NonNull Map<Integer, Integer> captureGestureIdToAnnouncements = new HashMap<>();
  // Whether an open training page, such as Practice gestures, takes back-and-forth or angle
  // gestures, so that swipes wait for the lift as if those gestures had commands.
  private static volatile boolean trainingTakesTwoDirectionSwipes;
  // Whether an open training page takes any gesture, so that multi-finger taps wait for a further
  // tap as if the gestures that go on after them had commands.
  private static volatile boolean trainingTakesGestures;
  private final @NonNull Map<Integer, Integer> captureFingerprintGestureIdToAnnouncements =
      new HashMap<>();
  // The gesture whose action is being performed, or NO_GESTURE.
  private int performingGestureId = NO_GESTURE;
  private static final int NO_GESTURE = -1;

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Constructor methods

  public GestureController(
      TalkBackService service,
      Pipeline.FeedbackReturner pipeline,
      ActorState actorState,
      ListMenuManager menuManager,
      SelectorController selectorController,
      AccessibilityFocusMonitor accessibilityFocusMonitor,
      AccessibilityFocusInterpreter accessibilityFocusInterpreter,
      GestureShortcutMapping gestureShortcutMapping,
      TalkBackAnalytics analytics,
      VolumeMonitor volumeMonitor,
      BatteryMonitor batteryMonitor,
      StatusReader statusReader,
      SpeechRateAndPitchActor speechRateAndPitchActor,
      SpeechControllerImpl speaker,
      ScreenStateMonitor.State screenState,
      ProcessorPhoneticLetters processorPhoneticLetters) {
    if (pipeline == null) {
      throw new IllegalStateException();
    }
    if (menuManager == null) {
      throw new IllegalStateException();
    }
    if (selectorController == null) {
      throw new IllegalStateException();
    }

    this.pipeline = pipeline;
    this.actorState = actorState;
    this.menuManager = menuManager;
    this.service = service;
    this.selectorController = selectorController;
    this.accessibilityFocusMonitor = accessibilityFocusMonitor;
    this.accessibilityFocusInterpreter = accessibilityFocusInterpreter;
    this.gestureShortcutMapping = gestureShortcutMapping;
    this.analytics = analytics;
    this.volumeMonitor = volumeMonitor;
    this.batteryMonitor = batteryMonitor;
    this.statusReader = statusReader;
    this.speechRateAndPitchActor = speechRateAndPitchActor;
    this.speaker = speaker;
    this.screenState = screenState;
    this.processorPhoneticLetters = processorPhoneticLetters;
    listEdgeScroller = new ListEdgeScroller(service, pipeline, accessibilityFocusMonitor);
    pullToRefresh = new PullToRefresh(service, pipeline, accessibilityFocusMonitor);
    swipeUp = new SwipeUp(service);
    handwritingCommand = new HandwritingCommand(service, pipeline, actorState, selectorController);
    handwritingAppLauncher = new HandwritingAppLauncher(service, pipeline, actorState);
    brailleScreenInput = new BrailleScreenInput(service, pipeline, actorState, new BrailleHost());
    translateCommand = new TranslateCommand(service);
    dictation = new AiDictation(service, pipeline);
  }

  /** Stops what gestures started that is still going on. */
  public void shutdown() {
    listEdgeScroller.cancel();
    pullToRefresh.cancel();
    swipeUp.cancel();
    SidewaysSwipe.cancel();
    handwritingCommand.shutdown();
    handwritingAppLauncher.shutdown();
    brailleScreenInput.shutdown();
    translateCommand.shutdown();
    // Before dictation, whose shutdown answers the recording of voice editing.
    VoiceEditing.shutdown();
    dictation.shutdown();
    QuickTasks.stop();
  }

  /**
   * Stops scrolling to the top or bottom, and swipes still waiting, when the user touches the
   * screen or presses a key.
   */
  public void onUserInputStarted() {
    listEdgeScroller.cancel();
    pullToRefresh.cancel();
    swipeUp.cancel();
    SidewaysSwipe.cancel();
    selectorController.cancelPageScroll();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Screen-swipe-shortcut methods

  /**
   * Maps fingerprint gesture Id to TalkBack action.
   *
   * @param gesture Fingerprint gesture Id
   * @return Mapped action shortcut
   */
  private String actionFromFingerprintGesture(int gesture) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
    return switch (gesture) {
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP ->
          GestureLayout.read(
              service,
              prefs,
              service.getString(R.string.pref_shortcut_fingerprint_up_key),
              service.getString(R.string.pref_shortcut_fingerprint_up_default));
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN ->
          GestureLayout.read(
              service,
              prefs,
              service.getString(R.string.pref_shortcut_fingerprint_down_key),
              service.getString(R.string.pref_shortcut_fingerprint_down_default));
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_LEFT ->
          GestureLayout.read(
              service,
              prefs,
              service.getString(R.string.pref_shortcut_fingerprint_left_key),
              service.getString(R.string.pref_shortcut_fingerprint_left_default));
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_RIGHT ->
          GestureLayout.read(
              service,
              prefs,
              service.getString(R.string.pref_shortcut_fingerprint_right_key),
              service.getString(R.string.pref_shortcut_fingerprint_right_default));
      default -> service.getString(R.string.shortcut_value_unassigned);
    };
  }

  public void performAction(String action, EventId eventId) {
    if (isInputOverlayShown() && !runsOverInputOverlay(action)) {
      // A window would open over the braille keyboard, braille input or handwriting.
      LogUtils.d(LOG_TAG, "Not run over an input overlay: [%s]", action);
      pipeline.returnFeedback(eventId, Feedback.sound(R.raw.complete));
      return;
    }
    runAction(action, eventId);
  }

  /** Whether the on-screen braille keyboard, braille input or a handwriting overlay is shown. */
  private boolean isInputOverlayShown() {
    return service.isOnScreenBrailleKeyboardShown()
        || brailleScreenInput.isShowing()
        || handwritingCommand.isShowing()
        || handwritingAppLauncher.isShowing();
  }

  private @Nullable Set<String> commandsOverInputOverlay;

  /** Whether {@code action} runs while an input overlay is shown: it opens no window. */
  private boolean runsOverInputOverlay(String action) {
    if (commandsOverInputOverlay == null) {
      Set<String> commands = new HashSet<>();
      commands.add(service.getString(R.string.switch_gesture_set));
      for (int id : COMMANDS_OVER_INPUT_OVERLAY) {
        commands.add(service.getString(id));
      }
      commandsOverInputOverlay = commands;
    }
    return commandsOverInputOverlay.contains(action)
        || NavigationGestureActions.isAction(action)
        || ReadingControlCommands.isSelection(action)
        || ReadingControlLists.isCommand(action)
        || ReadingControlCommands.settingOf(action) != null
        || GestureProfiles.isAction(action)
        || VoiceProfileNames.isCommand(action)
        || BrailleCommands.isAction(action);
  }

  /** Commands that open no window, so they run over the braille keyboard and handwriting. */
  private static final int[] COMMANDS_OVER_INPUT_OVERLAY = {
    R.string.shortcut_value_unassigned,
    R.string.shortcut_value_previous,
    R.string.shortcut_value_next,
    R.string.shortcut_value_move_to_item_below,
    R.string.shortcut_value_move_to_item_above,
    R.string.shortcut_value_move_to_item_left,
    R.string.shortcut_value_move_to_item_right,
    R.string.shortcut_value_first_in_screen,
    R.string.shortcut_value_last_in_screen,
    R.string.shortcut_value_next_granularity,
    R.string.shortcut_value_next_by_navigation_setting,
    R.string.shortcut_value_previous_by_navigation_setting,
    R.string.shortcut_value_previous_granularity,
    R.string.shortcut_value_prev_container,
    R.string.shortcut_value_next_container,
    R.string.shortcut_value_previous_window,
    R.string.shortcut_value_next_window,
    R.string.shortcut_value_scroll_back,
    R.string.shortcut_value_scroll_forward,
    R.string.shortcut_value_scroll_to_top,
    R.string.shortcut_value_scroll_to_bottom,
    R.string.shortcut_value_scroll_up,
    R.string.shortcut_value_scroll_down,
    R.string.shortcut_value_scroll_left,
    R.string.shortcut_value_scroll_right,
    R.string.shortcut_value_refresh,
    R.string.shortcut_value_toggle_browse_mode,
    R.string.shortcut_value_toggle_arrow_key_nav,
    R.string.shortcut_value_toggle_touch_lock,
    R.string.shortcut_value_row_up,
    R.string.shortcut_value_row_down,
    R.string.shortcut_value_column_left,
    R.string.shortcut_value_column_right,
    R.string.shortcut_value_read_from_top,
    R.string.shortcut_value_read_from_current,
    R.string.shortcut_value_read_from_current_by_reading_control,
    R.string.shortcut_value_perform_click_action,
    R.string.shortcut_value_perform_double_click_action,
    R.string.shortcut_value_touch_click_action,
    R.string.shortcut_value_activate_confirm_button,
    R.string.shortcut_value_activate_cancel_button,
    R.string.shortcut_value_perform_long_click_action,
    R.string.shortcut_value_action_setting_activate_current_action,
    R.string.shortcut_value_select_next_setting,
    R.string.shortcut_value_select_previous_setting,
    R.string.shortcut_value_select_first_setting,
    R.string.shortcut_value_selected_setting_next_action,
    R.string.shortcut_value_selected_setting_previous_action,
    R.string.shortcut_value_next_reading_control_list,
    R.string.shortcut_value_previous_reading_control_list,
    R.string.shortcut_value_back,
    R.string.shortcut_value_home,
    R.string.shortcut_value_lock_screen,
    R.string.shortcut_value_take_screenshot,
    R.string.shortcut_value_answer_call,
    R.string.shortcut_value_end_call,
    R.string.shortcut_value_media_control,
    R.string.shortcut_value_dictate_or_media_control,
    R.string.shortcut_value_dictate,
    R.string.shortcut_value_voice_editing,
    R.string.shortcut_value_increase_volume,
    R.string.shortcut_value_decrease_volume,
    R.string.shortcut_value_increase_media_volume,
    R.string.shortcut_value_decrease_media_volume,
    R.string.shortcut_value_next_track,
    R.string.shortcut_value_previous_track,
    R.string.shortcut_value_rewind,
    R.string.shortcut_value_fast_forward,
    R.string.shortcut_value_read_notifications,
    R.string.shortcut_value_toggle_notification_speech,
    R.string.shortcut_value_toggle_time_announcements,
    R.string.shortcut_value_read_last_notification,
    R.string.shortcut_value_stop_notification,
    R.string.shortcut_value_start_selection_mode,
    R.string.shortcut_value_move_cursor_to_beginning,
    R.string.shortcut_value_move_cursor_to_end,
    R.string.shortcut_value_select_all,
    R.string.shortcut_value_clear_selection,
    R.string.shortcut_value_copy,
    R.string.shortcut_value_add_to_clipboard,
    R.string.shortcut_value_cut,
    R.string.shortcut_value_paste,
    R.string.shortcut_value_copy_last_spoken_phrase,
    R.string.shortcut_value_continuous_copy,
    R.string.shortcut_value_submit_text,
    R.string.shortcut_value_new_line,
    R.string.shortcut_value_clear_text,
    R.string.shortcut_value_character_count,
    R.string.shortcut_value_read_text_field,
    R.string.shortcut_value_split_typing,
    R.string.shortcut_value_braille_keyboard,
    // Braille commands and app search show over the braille keyboard on purpose.
    R.string.shortcut_value_braille_commands,
    R.string.shortcut_value_braille_app_name,
    R.string.shortcut_value_braille_find_text,
    R.string.shortcut_value_braille_find_next_text,
    R.string.shortcut_value_braille_find_previous_text,
    R.string.shortcut_value_toggle_braille_display,
    R.string.shortcut_value_pause_or_resume_feedback,
    R.string.shortcut_value_toggle_voice_feedback,
    R.string.shortcut_value_control_telling_time,
    R.string.shortcut_value_control_text_formatting,
    R.string.shortcut_value_describe_text_formatting,
    R.string.shortcut_value_announce_current_time_and_date,
    R.string.shortcut_value_announce_battery_state,
    R.string.shortcut_value_say_caller,
    R.string.shortcut_value_announce_status,
    R.string.shortcut_value_announce_current_title,
    R.string.shortcut_value_announce_phonetic_pronunciation,
    R.string.shortcut_value_speak_position,
    R.string.shortcut_value_speak_coordinates,
    R.string.shortcut_value_copy_item_details,
    R.string.shortcut_value_copy_screen_tree,
    R.string.shortcut_value_repeat_last_spoken_phrase,
    R.string.shortcut_value_repeat_last_spoken_phrase_slower,
    R.string.shortcut_value_spell_last_spoken_phrase,
    R.string.shortcut_value_translate_last_spoken_phrase,
    R.string.shortcut_value_toggle_scripts,
    R.string.shortcut_value_toggle_focus_indicator,
    R.string.shortcut_value_toggle_focus_text_inside_items,
    R.string.shortcut_value_toggle_direct_touch_app,
    R.string.shortcut_value_toggle_double_tap_taps_app,
    R.string.shortcut_value_toggle_color_inversion,
    R.string.shortcut_value_toggle_auto_translation,
    R.string.shortcut_value_toggle_equalizer,
    R.string.shortcut_value_toggle_switch_languages,
    R.string.shortcut_value_toggle_identify_languages_from_words,
    R.string.shortcut_value_speech_pitch_decrease,
    R.string.shortcut_value_speech_pitch_increase,
    R.string.shortcut_value_speech_rate_decrease,
    R.string.shortcut_value_speech_rate_increase,
    R.string.shortcut_value_previous_voice_profile,
    R.string.shortcut_value_next_voice_profile,
    R.string.shortcut_value_previous_gesture_profile,
    R.string.shortcut_value_next_gesture_profile,
    R.string.shortcut_value_previous_settings_profile,
    R.string.shortcut_value_next_settings_profile,
    R.string.shortcut_value_toggle_blackberry_mode,
    R.string.shortcut_value_quick_tasks,
    R.string.shortcut_value_previous_braille_command_profile,
    R.string.shortcut_value_next_braille_command_profile,
  };

  /** Runs {@code action}, over an input overlay as well. */
  private void runAction(String action, EventId eventId) {
    RecentUserInput.mark();
    MacroRecorder.onCommand(service, action);
    NotificationReader.onUserAction();
    boolean defaultItemNavigation =
        action.equals(service.getString(R.string.shortcut_value_next))
            || action.equals(service.getString(R.string.shortcut_value_previous));
    // Next/previous item explicitly use DEFAULT, independent of the selected reading control.
    // Any command that can use that control still resolves its pending context before it runs.
    if (defaultItemNavigation) selectorController.applyPendingSwitchForDefaultNavigation();
    else selectorController.applyPendingSwitch();
    Performance.getInstance().onPerformAction(eventId, action);
    TalkbackAction actionEvent = gestureShortcutMapping.getActionEvent(action);
    if (actionEvent != null) {
      analytics.onShortcutActionEvent(actionEvent);
    }

    maybeInterruptAllFeedback(action);
    boolean continuousCopyAction =
        action.equals(service.getString(R.string.shortcut_value_continuous_copy));
    if (!continuousCopyAction) {
      // Another command ends a collection, as a touch or key does.
      service.stopContinuousCopy();
    }
    if (action.equals(service.getString(R.string.shortcut_value_dictate))
        || action.equals(service.getString(R.string.shortcut_value_dictate_or_media_control))
        || action.equals(service.getString(R.string.shortcut_value_voice_commands))) {
      // These record speech too, which voice editing does between its commands.
      VoiceEditing.end();
    }
    boolean result = true;
    if (action.equals(service.getString(R.string.switch_gesture_set))) {
      speak(
          service.getString(
              R.string.switch_gesture_set_to,
              gestureShortcutMapping.switchGestureSet(/* isNext= */ true)));
    } else if (action.equals(service.getString(R.string.shortcut_value_split_typing))) {
      accessibilityFocusInterpreter.performSplitTap(EVENT_ID_UNTRACKED);
    } else if (action.equals(service.getString(R.string.shortcut_value_unassigned))) {
      // Do Nothing
    } else if (action.equals(service.getString(R.string.shortcut_value_previous))) {
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.focusDirection(SEARCH_FOCUS_BACKWARD)
                  // Sets granularity to default because "Previous item" action always moves at
                  // default granularity.
                  .setGranularity(DEFAULT)
                  .setInputMode(INPUT_MODE_TOUCH)
                  .setDefaultToInputFocus(true)
                  .setScroll(true)
                  .setWrap(true));
    } else if (action.equals(service.getString(R.string.shortcut_value_next))) {
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.focusDirection(SEARCH_FOCUS_FORWARD)
                  // Sets granularity to default because "Next item" action always moves at default
                  // granularity.
                  .setGranularity(DEFAULT)
                  .setInputMode(INPUT_MODE_TOUCH)
                  .setDefaultToInputFocus(true)
                  .setScroll(true)
                  .setWrap(true));
    } else if (action.equals(service.getString(R.string.shortcut_value_next_by_navigation_setting))
        || action.equals(
            service.getString(R.string.shortcut_value_previous_by_navigation_setting))) {
      // By the navigation setting chosen, such as words, as the keyboard's Next by navigation
      // setting moves.
      boolean next =
          action.equals(service.getString(R.string.shortcut_value_next_by_navigation_setting));
      Feedback.FocusDirection.Builder builder =
          Feedback.focusDirection(next ? SEARCH_FOCUS_FORWARD : SEARCH_FOCUS_BACKWARD)
              .setInputMode(INPUT_MODE_TOUCH)
              .setDefaultToInputFocus(true)
              .setScroll(true)
              .setWrap(true);
      CursorGranularity granularity =
          actorState.getDirectionNavigation().getCurrentGranularity();
      if (granularity == CursorGranularity.CONTAINER) {
        builder.setToContainer(true);
      } else if (granularity == CursorGranularity.WINDOWS) {
        builder.setToWindow(true);
      }
      result = pipeline.returnFeedback(eventId, builder);
    } else if (action.equals(service.getString(R.string.shortcut_value_move_to_item_below))) {
      selectorController.moveVertically(eventId, /* isNext= */ true);
    } else if (action.equals(service.getString(R.string.shortcut_value_move_to_item_above))) {
      selectorController.moveVertically(eventId, /* isNext= */ false);
    } else if (action.equals(service.getString(R.string.shortcut_value_move_to_item_left))
        || action.equals(service.getString(R.string.shortcut_value_move_to_item_right))) {
      // The nearest item on screen in that direction, as a D-pad moves.
      boolean left = action.equals(service.getString(R.string.shortcut_value_move_to_item_left));
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.focusDirection(left ? SEARCH_FOCUS_LEFT : SEARCH_FOCUS_RIGHT)
                  .setGranularity(DEFAULT)
                  .setInputMode(INPUT_MODE_TOUCH)
                  .setDefaultToInputFocus(true)
                  .setScroll(true));
    } else if (OpenAppActions.isAction(action)) {
      result = OpenAppActions.open(service, action);
    } else if (AppShortcutActions.isAction(action)) {
      AppShortcutActions.open(
          service,
          action,
          opened -> {
            if (opened != AppShortcutActions.Opened.YES) {
              pipeline.returnFeedback(
                  EVENT_ID_UNTRACKED,
                  Feedback.requestedSpeech(
                      service.getString(
                          opened == AppShortcutActions.Opened.NEEDS_SHIZUKU
                              ? R.string.app_shortcut_needs_shizuku
                              : R.string.app_shortcut_not_opened)));
              pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.complete));
            }
            return kotlin.Unit.INSTANCE;
          });
    } else if (AccessibilityShortcutActions.isAction(action)) {
      AccessibilityShortcutActions.run(
          service,
          action,
          ran -> {
            if (!ran) {
              pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.complete));
            }
            return kotlin.Unit.INSTANCE;
          });
    } else if (TileCommands.isAction(action)) {
      TileCommands.run(
          service,
          action,
          text -> {
            speak(text);
            return kotlin.Unit.INSTANCE;
          });
    } else if (Commands.keyboardActionOf(service, action) != null) {
      KeyComboManager keyComboManager = service.getKeyComboManager();
      result =
          keyComboManager != null
              && keyComboManager.performKeyboardAction(
                  Commands.keyboardActionOf(service, action), eventId);
    } else if (NavigationGestureActions.isAction(action)) {
      result =
          NavigationGestureActions.perform(
              action,
              pipeline,
              actorState.getDirectionNavigation().hasNavigableWebContent(),
              actorState.getDirectionNavigation().isSelectionModeActive(),
              eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_scroll_back))) {
      // At the top of a list, scrolling back pulls it down to refresh, as two fingers do.
      result =
          pullToRefresh.pullIfAtTop(eventId)
              || pipeline.returnFeedback(eventId, Feedback.focusDirection(PREVIOUS_PAGE));
    } else if (action.equals(service.getString(R.string.shortcut_value_scroll_forward))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusDirection(NEXT_PAGE));
    } else if (action.equals(service.getString(R.string.shortcut_value_scroll_up))) {
      // Scroll up moves the content up, toward the end of the list.
      result = pipeline.returnFeedback(eventId, Feedback.focusDirection(SCROLL_UP));
    } else if (action.equals(service.getString(R.string.shortcut_value_scroll_down))) {
      // Scroll down moves the content down, toward the top, where it pulls the list to refresh.
      result =
          pullToRefresh.pullIfAtTop(eventId)
              || pipeline.returnFeedback(eventId, Feedback.focusDirection(SCROLL_DOWN));
    } else if (action.equals(service.getString(R.string.shortcut_value_scroll_left))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusDirection(SCROLL_LEFT));
    } else if (action.equals(service.getString(R.string.shortcut_value_scroll_right))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusDirection(SCROLL_RIGHT));
    } else if (action.equals(service.getString(R.string.shortcut_value_first_in_screen))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusTop(INPUT_MODE_TOUCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_last_in_screen))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusBottom(INPUT_MODE_TOUCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_write_reading_control))) {
      handwritingCommand.show();
      result = true;
    } else if (action.equals(service.getString(R.string.shortcut_value_write_app_name))) {
      handwritingAppLauncher.show();
      result = true;
    } else if (action.equals(service.getString(R.string.shortcut_value_braille_app_name))) {
      brailleScreenInput.showApps();
      result = true;
    } else if (action.equals(service.getString(R.string.shortcut_value_braille_commands))) {
      brailleScreenInput.showCommands();
      result = true;
    } else if (action.equals(service.getString(R.string.shortcut_value_braille_find_text))) {
      brailleScreenInput.find(/* forward= */ true, /* wrap= */ true, /* newText= */ true);
    } else if (action.equals(service.getString(R.string.shortcut_value_braille_find_next_text))) {
      brailleScreenInput.find(/* forward= */ true, /* wrap= */ false, /* newText= */ false);
    } else if (action.equals(
        service.getString(R.string.shortcut_value_braille_find_previous_text))) {
      brailleScreenInput.find(/* forward= */ false, /* wrap= */ false, /* newText= */ false);
    } else if (action.equals(service.getString(R.string.shortcut_value_braille_command_profiles))) {
      result =
          menuManager.showMenu(
              BRAILLE_COMMAND_PROFILE, eventId, R.string.no_braille_command_profiles);
    } else if (action.equals(
        service.getString(R.string.shortcut_value_previous_braille_command_profile))) {
      changeBrailleCommandProfile(/* next= */ false);
    } else if (action.equals(
        service.getString(R.string.shortcut_value_next_braille_command_profile))) {
      changeBrailleCommandProfile(/* next= */ true);
    } else if (BrailleCommands.isAction(action)) {
      @Nullable String name =
          BrailleCommands.select(
              service,
              SharedPreferencesUtils.getSharedPreferences(service),
              BrailleCommands.idOf(action));
      result = name != null;
      if (name != null) {
        speak(name);
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_scroll_to_top))) {
      result = listEdgeScroller.scroll(/* toStart= */ true, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_scroll_to_bottom))) {
      result = listEdgeScroller.scroll(/* toStart= */ false, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_browse_mode))) {
      KeyComboManager keyComboManager = service.getKeyComboManager();
      result = keyComboManager != null && keyComboManager.toggleBrowseModeByUser();
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_arrow_key_nav))) {
      KeyComboManager keyComboManager = service.getKeyComboManager();
      if (keyComboManager != null) {
        keyComboManager.toggleArrowKeyNav();
      }
      result = keyComboManager != null;
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_touch_lock))) {
      result = TouchLock.toggle();
    } else if (action.equals(service.getString(R.string.shortcut_value_search_emoji))
        || action.equals(service.getString(R.string.shortcut_value_choose_symbol))) {
      result =
          SandFlyKeyboardHost.openChooser(
              action.equals(service.getString(R.string.shortcut_value_search_emoji))
                  ? CharacterChooser.Kind.EMOJI
                  : CharacterChooser.Kind.SYMBOL);
      if (!result) {
        SandFlyKeyboardHost.speak(service.getString(R.string.sandfly_keyboard_needed));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_editing_mode))) {
      result = SandFlyKeyboardHost.toggleEditingMode();
      if (!result) {
        SandFlyKeyboardHost.speak(service.getString(R.string.sandfly_keyboard_needed));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_spell_password_field))) {
      // Only the SandFly keyboard has the text of a password field.
      result = SandFlyKeyboardHost.spellPasswordField();
      if (!result) {
        SandFlyKeyboardHost.speak(service.getString(R.string.sandfly_keyboard_needed));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_next_word_suggestion))
        || action.equals(service.getString(R.string.shortcut_value_previous_word_suggestion))) {
      String said =
          SandFlyKeyboardHost.stepSuggestion(
              action.equals(service.getString(R.string.shortcut_value_next_word_suggestion)));
      result = said != null;
      SandFlyKeyboardHost.speak(
          said != null ? said : service.getString(R.string.sandfly_keyboard_needed));
    } else if (action.equals(service.getString(R.string.shortcut_value_type_word_suggestion))) {
      result = SandFlyKeyboardHost.typeSuggestion();
      if (!result) {
        SandFlyKeyboardHost.speak(service.getString(R.string.sandfly_keyboard_needed));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_refresh))) {
      result = pullToRefresh.refresh(eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_media_control))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(GLOBAL_ACTION_KEYCODE_HEADSETHOOK));
    } else if (action.equals(service.getString(R.string.shortcut_value_dictate_or_media_control))) {
      // Stops a dictation that is on; starts one in a text field being edited, unless a call is
      // on, when play or pause answers or ends it.
      AccessibilityNodeInfoCompat field = null;
      if (!dictation.isActive() && !service.isPhoneCallActive()) {
        field = editingTextField();
      }
      if (dictation.isActive() || field != null) {
        result = dictation.start(field);
      } else {
        result =
            pipeline.returnFeedback(
                eventId, Feedback.systemAction(GLOBAL_ACTION_KEYCODE_HEADSETHOOK));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_increase_volume))) {
      changeAccessibilityVolume(eventId, /* decrease= */ false);
    } else if (action.equals(service.getString(R.string.shortcut_value_decrease_volume))) {
      changeAccessibilityVolume(eventId, /* decrease= */ true);
    } else if (action.equals(service.getString(R.string.shortcut_value_read_notifications))) {
      result = NotificationReader.readSummary();
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_notification_speech))) {
      NotificationReader.setSpeechOn(service, null, /* announce= */ true);
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_time_announcements))) {
      boolean on =
          SpokenSchedules.toggleClock(SharedPreferencesUtils.getSharedPreferences(service));
      speak(
          service.getString(
              on ? R.string.notification_clock_now_on : R.string.notification_clock_now_off));
    } else if (action.equals(service.getString(R.string.shortcut_value_read_last_notification))) {
      result = NotificationReader.readLastAgain();
      if (!result) {
        pipeline.returnFeedback(
            eventId,
            Feedback.requestedSpeech(service.getString(R.string.notification_no_last_notification)));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_stop_notification))) {
      NotificationReader.stop();
    } else if (action.equals(service.getString(R.string.shortcut_value_increase_media_volume))) {
      changeMediaVolume(eventId, /* decrease= */ false);
    } else if (action.equals(service.getString(R.string.shortcut_value_decrease_media_volume))) {
      changeMediaVolume(eventId, /* decrease= */ true);
    } else if (action.equals(service.getString(R.string.shortcut_value_next_track))) {
      result = dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
    } else if (action.equals(service.getString(R.string.shortcut_value_previous_track))) {
      result = dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    } else if (action.equals(service.getString(R.string.shortcut_value_rewind))) {
      result = MediaSeek.seek(service, /* forward= */ false);
    } else if (action.equals(service.getString(R.string.shortcut_value_fast_forward))) {
      result = MediaSeek.seek(service, /* forward= */ true);
    } else if (action.equals(service.getString(R.string.shortcut_value_back))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_BACK));
    } else if (action.equals(service.getString(R.string.shortcut_value_home))
        && GestureLayout.isOtherScreenReader(SharedPreferencesUtils.getSharedPreferences(service))
        && SyntheticTouches.isAvailable()
        && swipeUp.isLockScreenShown()) {
      // As on an iPhone or a BlackBerry 10 phone, Home on the lock screen unlocks it, or shows the PIN, pattern or
      // password: Android's lock screen does that for a two-finger swipe up.
      result = swipeUp.swipeUp(/* fingers= */ 2);
    } else if (action.equals(service.getString(R.string.shortcut_value_home))) {
      // Dismiss overlays such as TalkBack menu, Gemini bottom sheet dialog and screen search.
      menuManager.dismissAll();
      pipeline.returnFeedback(
          eventId, Feedback.geminiAction(GeminiRequest.Action.DISMISS_BOTTOM_SHEET));
      pipeline.returnFeedback(eventId, Feedback.universalSearch(CANCEL_SEARCH));

      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_HOME));
    } else if (action.equals(service.getString(R.string.shortcut_value_voice_commands))) {
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.voiceRecognition(
                  START_LISTENING_IF_SCREEN_NOT_LOCKED,
                  /* checkDialog= */ true,
                  gestureShortcutMapping.nodeMenuShortcut() != null
                      ? gestureShortcutMapping.nodeMenuShortcut().toString()
                      : ""));
    } else if (action.equals(service.getString(R.string.shortcut_value_overview))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_RECENTS));
    } else if (action.equals(service.getString(R.string.shortcut_value_system_overview))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(SystemActionPerformer.GLOBAL_ACTION_SYSTEM_OVERVIEW));
    } else if (action.equals(service.getString(R.string.shortcut_value_take_screenshot))) {
      result =
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
              && service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT);
    } else if (action.equals(service.getString(R.string.shortcut_value_answer_call))) {
      AnswerCall.answer(
          service,
          service.getVoiceActionMonitor().getCurrentCallState(),
          callResult ->
              onCallResult(
                  callResult, R.string.answer_call_no_call, R.string.answer_call_permission));
    } else if (action.equals(service.getString(R.string.shortcut_value_end_call))) {
      EndCall.end(
          service,
          service.getVoiceActionMonitor().getCurrentCallState(),
          callResult ->
              onCallResult(callResult, R.string.end_call_no_call, R.string.end_call_permission));
    } else if (action.equals(service.getString(R.string.shortcut_value_previous_app))) {
      result = PreviousApp.switchTo(service);
    } else if (action.equals(service.getString(R.string.shortcut_value_older_app))) {
      result = RecentAppSteps.step(service, /* older= */ true);
    } else if (action.equals(service.getString(R.string.shortcut_value_newer_app))) {
      result = RecentAppSteps.step(service, /* older= */ false);
    } else if (action.equals(service.getString(R.string.shortcut_value_notifications))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS));
    } else if (action.equals(service.getString(R.string.shortcut_value_system_notifications))) {
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.systemAction(SystemActionPerformer.GLOBAL_ACTION_SYSTEM_NOTIFICATIONS));
    } else if (action.equals(service.getString(R.string.shortcut_value_quick_settings))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS));
    } else if (action.equals(service.getString(R.string.shortcut_value_system_quick_settings))) {
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.systemAction(SystemActionPerformer.GLOBAL_ACTION_SYSTEM_QUICK_SETTINGS));
    } else if (action.equals(service.getString(R.string.shortcut_value_lock_screen))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN));
    } else if (action.equals(service.getString(R.string.shortcut_value_power_menu))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG));
    } else if (PowerCommands.isAction(service, action)) {
      PowerCommands.run(service, pipeline, action);
    } else if (action.equals(service.getString(R.string.shortcut_value_all_apps))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS));
    } else if (action.equals(service.getString(R.string.shortcut_value_talkback_breakout))) {
      result = menuManager.showMenu(CONTEXT, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_local_breakout))) {
      result = menuManager.showMenu(CONTEXT, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_show_custom_actions))) {
      result = menuManager.showMenu(CUSTOM_ACTION, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_editing))) {
      // Combines editing menu and custom action menu. If user set the gesture to editing menu, it
      // will launch custom action menu.
      result = menuManager.showMenu(CUSTOM_ACTION, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_show_language_options))) {
      result = menuManager.showMenu(LANGUAGE, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_voice_profiles))) {
      result = menuManager.showMenu(VOICE_PROFILE, eventId, R.string.no_voice_profiles);
    } else if (action.equals(service.getString(R.string.shortcut_value_split_screen))) {
      result = menuManager.showMenu(SPLIT_SCREEN, eventId, R.string.split_screen_not_active);
    } else if (action.equals(service.getString(R.string.shortcut_value_editing_menu))) {
      result = menuManager.showMenu(EDITING, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_previous_voice_profile))) {
      selectorController.changeVoiceProfile(eventId, /* isNext= */ false);
    } else if (action.equals(service.getString(R.string.shortcut_value_next_voice_profile))) {
      selectorController.changeVoiceProfile(eventId, /* isNext= */ true);
    } else if (action.equals(service.getString(R.string.shortcut_value_turn_off_sandfly))) {
      service.showDisableTalkBackDialog();
      result = true;
    } else if (action.equals(service.getString(R.string.shortcut_value_gesture_profiles))) {
      result = menuManager.showMenu(GESTURE_PROFILE, eventId, R.string.no_gesture_profiles);
    } else if (action.equals(service.getString(R.string.shortcut_value_previous_gesture_profile))) {
      selectorController.changeGestureProfile(eventId, /* isNext= */ false);
    } else if (action.equals(service.getString(R.string.shortcut_value_next_gesture_profile))) {
      selectorController.changeGestureProfile(eventId, /* isNext= */ true);
    } else if (action.equals(
        service.getString(R.string.shortcut_value_previous_reading_control_list))) {
      selectorController.changeReadingControlList(eventId, /* isNext= */ false);
    } else if (action.equals(service.getString(R.string.shortcut_value_next_reading_control_list))) {
      selectorController.changeReadingControlList(eventId, /* isNext= */ true);
    } else if (action.equals(service.getString(R.string.shortcut_value_app_gestures_for_app))) {
      result = AppGestureFragment.open(service, AppGestures.appShown());
    } else if (action.equals(service.getString(R.string.shortcut_value_settings_profiles))) {
      result = menuManager.showMenu(SETTINGS_PROFILE, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_previous_settings_profile))) {
      SettingsProfiles.switchToPrevious(
          service, said -> pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(said)));
    } else if (action.equals(service.getString(R.string.shortcut_value_next_settings_profile))) {
      SettingsProfiles.switchToNext(
          service, said -> pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(said)));
    } else if (action.equals(service.getString(R.string.shortcut_value_quick_tasks))) {
      QuickTasks.start(
          service,
          () ->
              pipeline.returnFeedback(
                  EVENT_ID_UNTRACKED, Feedback.vibration(R.array.view_clicked_pattern)));
      speak(service.getString(R.string.shortcut_quick_tasks));
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_blackberry_mode))) {
      // Turning it off is at once; turning it on asks first, as its gestures are unfamiliar to
      // most people.
      SettingsProfiles.inBackground(
          service,
          BlackBerryMode::isOn,
          on -> {
            Consumer<String> say =
                said -> pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(said));
            if (on) {
              BlackBerryMode.set(service, false, say);
            } else {
              new BlackBerryModeDialog(
                      service, pipeline, () -> BlackBerryMode.set(service, true, say))
                  .showDialog();
            }
          });
    } else if (ReadingControlCommands.isSelection(action)) {
      result =
          selectorController.selectReadingControlByKey(
              eventId, ReadingControlCommands.selectedKey(action));
    } else if (ReadingControlLists.isCommand(action)) {
      List<String> controls =
          ReadingControlLists.controls(
              service,
              SharedPreferencesUtils.getSharedPreferences(service),
              ReadingControlLists.idOf(action));
      result =
          controls != null
              && selectorController.selectInReadingControlList(
                  eventId, controls, ReadingControlLists.isNext(action));
    } else if (GestureProfiles.isAction(action)) {
      result = selectorController.selectGestureProfile(eventId, GestureProfiles.idOf(action));
    } else if (VoiceProfileNames.isCommand(action)) {
      result = selectorController.selectVoiceProfile(eventId, VoiceProfileNames.idOfCommand(action));
    } else if (action.equals(service.getString(R.string.shortcut_value_previous_granularity))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusDirection(PREVIOUS_GRANULARITY));
    } else if (action.equals(service.getString(R.string.shortcut_value_next_granularity))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusDirection(NEXT_GRANULARITY));
    } else if (action.equals(service.getString(R.string.shortcut_value_previous_window))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.previousWindow(INPUT_MODE_TOUCH).setDefaultToInputFocus(true));
    } else if (action.equals(service.getString(R.string.shortcut_value_next_window))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.nextWindow(INPUT_MODE_TOUCH).setDefaultToInputFocus(true));
    } else if (action.equals(service.getString(R.string.shortcut_value_prev_container))) {
      result = pipeline.returnFeedback(eventId, Feedback.prevContainer(INPUT_MODE_TOUCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_next_container))) {
      result = pipeline.returnFeedback(eventId, Feedback.nextContainer(INPUT_MODE_TOUCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_read_from_top))) {
      result = pipeline.returnFeedback(eventId, Feedback.continuousRead(START_AT_TOP));
    } else if (action.equals(service.getString(R.string.shortcut_value_read_from_current))) {
      result = pipeline.returnFeedback(eventId, Feedback.continuousRead(START_AT_CURSOR));
    } else if (action.equals(
        service.getString(R.string.shortcut_value_read_from_current_by_reading_control))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.continuousRead(START_AT_CURSOR_BY_READING_CONTROL));
    } else if (action.equals(service.getString(R.string.shortcut_value_print_node_tree))) {
      TreeDebugRenderer.FocusIndicatorSpec focusIndicatorSpec = null;
      if (DebugProperties.includeScreenshotWithTreeDebug(service)) {
        SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
        int color =
            FocusIndicatorUtils.getTalkBackFocusColor(service, prefs, service.getResources());
        float strokeWidth =
            FocusIndicatorUtils.getTalkBackFocusStrokeWidth(prefs, service.getResources());
        focusIndicatorSpec = new TreeDebugRenderer.FocusIndicatorSpec(color, strokeWidth);
      }
      TreeDebug.logNodeTreesOnAllDisplaysAndWriteScreenshot(service, focusIndicatorSpec);
      pipeline.returnFeedback(
          eventId, Feedback.speech(service.getString(R.string.dump_node_tree_description)));
    } else if (action.equals(service.getString(R.string.shortcut_value_print_performance_stats))) {
      Performance.getInstance().displayLabelToStats();
      Performance.getInstance().displayStatToLabelCompare();
      Performance.getInstance().displayAllEventStats();
    } else if (action.equals(
        service.getString(R.string.shortcut_value_perform_long_click_action))) {
      result = pipeline.returnFeedback(eventId, Feedback.focus(LONG_CLICK_CURRENT));
    } else if (action.equals(service.getString(R.string.shortcut_value_touch_click_action))) {
      result = pipeline.returnFeedback(eventId, Feedback.focus(TOUCH_CLICK_CURRENT));
    } else if (ButtonCommands.kindOf(service, action) != null) {
      ButtonCommands.Kind kind = ButtonCommands.kindOf(service, action);
      AccessibilityNodeInfoCompat button =
          ButtonCommands.find(service, SharedPreferencesUtils.getSharedPreferences(service), kind);
      if (button == null) {
        speak(service.getString(kind.getNoneRes()));
      } else {
        // Activating stops speech, as double tap does.
        pipeline.returnFeedback(eventId, Feedback.part().setInterruptGentle(true));
        result = pipeline.returnFeedback(eventId, Feedback.focus(CLICK_NODE).setTarget(button));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_select_previous_setting))) {
      selectorController.selectPreviousOrNextSetting(
          eventId, AnnounceType.DESCRIPTION_AND_HINT, false);
    } else if (action.equals(service.getString(R.string.shortcut_value_select_next_setting))) {
      selectorController.selectPreviousOrNextSetting(
          eventId, AnnounceType.DESCRIPTION_AND_HINT, true);
    } else if (action.equals(service.getString(R.string.shortcut_value_select_first_setting))) {
      selectorController.selectFirstSetting(eventId, AnnounceType.DESCRIPTION_AND_HINT);
    } else if (action.equals(
        service.getString(R.string.shortcut_value_selected_setting_previous_action))) {
      selectorController.adjustSelectedSetting(eventId, false, swipeIncreasesValue());
    } else if (action.equals(
        service.getString(R.string.shortcut_value_selected_setting_next_action))) {
      selectorController.adjustSelectedSetting(eventId, true, swipeIncreasesValue());
    } else if (action.equals(
            service.getString(R.string.shortcut_value_action_setting_activate_current_action))
        || action.equals(service.getString(R.string.shortcut_value_perform_click_action))) {
      // Activate does what the one-finger double tap does, from any gesture, voice command,
      // braille command or script that runs it.
      if (selectorController.activateDoubleTapSetting()) {
        return;
      } else if (SelectorController.getCurrentSetting(service).equals(Setting.ACTIONS)) {
        selectorController.activateCurrentAction(eventId);
      } else if (doubleTapMovesCursor(eventId)) {
        return;
      } else {
        result = pipeline.returnFeedback(eventId, Feedback.focus(CLICK_CURRENT));
      }
    } else if (FeatureFlagReader.enableDoubleClickKeyboard(service)
        && action.equals(service.getString(R.string.shortcut_value_perform_double_click_action))) {
      result = pipeline.returnFeedback(eventId, Feedback.focus(DOUBLE_CLICK_CURRENT));
    } else if (action.equals(service.getString(R.string.shortcut_value_screen_search))) {
      result = pipeline.returnFeedback(eventId, Feedback.universalSearch(TOGGLE_SEARCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_show_hide_screen))) {
      if (actorState.getDimScreen().isDimmingEnabled()) {
        result = pipeline.returnFeedback(eventId, Feedback.dimScreen(BRIGHTEN));
      } else {
        result = pipeline.returnFeedback(eventId, Feedback.dimScreen(DIM));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_focus_indicator))) {
      boolean shown =
          FocusIndicatorUtils.toggleFocusIndicatorShown(
              SharedPreferencesUtils.getSharedPreferences(service), service.getResources());
      speak(
          service.getString(
              shown ? R.string.focus_indicator_now_on : R.string.focus_indicator_now_off));
    } else if (action.equals(
        service.getString(R.string.shortcut_value_toggle_focus_text_inside_items))) {
      SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
      boolean on =
          !SharedPreferencesUtils.getBooleanPref(
              prefs,
              service.getResources(),
              R.string.pref_focus_text_inside_items_key,
              R.bool.pref_focus_text_inside_items_default);
      // The service applies the setting when it changes.
      SharedPreferencesUtils.putBooleanPref(
          prefs, service.getResources(), R.string.pref_focus_text_inside_items_key, on);
      speak(
          service.getString(
              on
                  ? R.string.focus_text_inside_items_now_on
                  : R.string.focus_text_inside_items_now_off));
    } else if (action.equals(service.getString(R.string.shortcut_value_pause_backtalk))) {
      if (service.getPauseController() != null) {
        service.getPauseController().requestPause();
      }
    } else if (action.equals(
        service.getString(R.string.shortcut_value_pass_through_next_gesture))) {
      result =
          pipeline.returnFeedback(eventId, Feedback.passThroughMode(PASSTHROUGH_CONFIRM_DIALOG));
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_direct_touch_app))) {
      DirectTouchController directTouch = service.getDirectTouchController();
      Boolean on = (directTouch == null) ? null : directTouch.toggleForAppInFront();
      if (on == null) {
        speak(service.getString(R.string.direct_touch_no_app));
      } else {
        speak(service.getString(on ? R.string.direct_touch_on : R.string.direct_touch_off));
      }
    } else if (action.equals(
        service.getString(R.string.shortcut_value_toggle_double_tap_taps_app))) {
      DirectTouchController directTouch = service.getDirectTouchController();
      String pkg = (directTouch == null) ? null : directTouch.appInFront();
      if (pkg == null) {
        speak(service.getString(R.string.double_tap_taps_no_app));
      } else {
        boolean on =
            DoubleTapTapsApps.toggle(SharedPreferencesUtils.getSharedPreferences(service), pkg);
        speak(service.getString(on ? R.string.double_tap_taps_on : R.string.double_tap_taps_off));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_a11y_button))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(GLOBAL_ACTION_ACCESSIBILITY_BUTTON));
    } else if (action.equals(service.getString(R.string.shortcut_value_a11y_button_long_press))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(GLOBAL_ACTION_ACCESSIBILITY_BUTTON_CHOOSER));
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_magnification))) {
      result = MagnificationToggle.toggle(service);
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_color_inversion))) {
      result =
          ColorInversionToggle.toggle(
              service, said -> pipeline.returnFeedback(eventId, Feedback.speech(said)));
    } else if (action.equals(service.getString(R.string.shortcut_value_pause_or_resume_feedback))) {
      pipeline.returnFeedback(eventId, Feedback.speech(Feedback.Speech.Action.PAUSE_OR_RESUME));
    } else if (action.equals(service.getString(R.string.shortcut_value_start_selection_mode))) {
      if (actorState.getDirectionNavigation().isSelectionModeActive()) {
        // Copying mode can be ended with nothing focused, as it goes from window to window.
        result =
            pipeline.returnFeedback(
                eventId, Feedback.edit(getSelectTextFocus(/* speakIfNone= */ false), END_SELECT));
      } else {
        AccessibilityNodeInfoCompat node = getSelectTextFocus(/* speakIfNone= */ true);
        // Editable or non-editable selectable text found.
        result = node != null && pipeline.returnFeedback(eventId, Feedback.edit(node, START_SELECT));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_move_cursor_to_beginning))) {
      AccessibilityNodeInfoCompat node = getEditTextFocus();
      if (node == null) {
        result = false;
      } else {
        pipeline.returnFeedback(eventId, Feedback.edit(node, CURSOR_TO_BEGINNING));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_move_cursor_to_end))) {
      AccessibilityNodeInfoCompat node = getEditTextFocus();
      if (node == null) {
        result = false;
      } else {
        pipeline.returnFeedback(eventId, Feedback.edit(node, CURSOR_TO_END));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_select_all))) {
      AccessibilityNodeInfoCompat node = getSelectTextFocus(/* speakIfNone= */ true);
      if (node == null) {
        result = false;
      } else {
        pipeline.returnFeedback(eventId, Feedback.edit(node, SELECT_ALL));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_granular_editing))) {
      AccessibilityNodeInfoCompat focus =
          accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ true);
      AccessibilityNodeInfoCompat editing =
          accessibilityFocusMonitor.getEditingNodeFromFocusedKeyboard(focus);
      result = GranularEditing.open(service, editing != null ? editing : focus);
    } else if (action.equals(service.getString(R.string.shortcut_value_dictate))) {
      result = dictation.start(dictationTarget());
    } else if (action.equals(service.getString(R.string.shortcut_value_voice_editing))) {
      result = VoiceEditing.toggle(dictationTarget());
    } else if (action.equals(service.getString(R.string.shortcut_value_switch_keyboard))) {
      result = NextKeyboard.switchKeyboard(service, said -> speak(said.toString()));
    } else if (action.equals(service.getString(R.string.shortcut_value_clear_selection))) {
      // The field the keyboard types into when focus is on the keyboard.
      AccessibilityNodeInfoCompat node = getSelectTextFocus(/* speakIfNone= */ false);
      result = pipeline.returnFeedback(eventId, Feedback.edit(node, CLEAR_SELECTION));
    } else if (action.equals(service.getString(R.string.shortcut_value_copy))) {
      AccessibilityNodeInfoCompat node =
          accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ true);
      result = pipeline.returnFeedback(eventId, Feedback.edit(node, COPY));
    } else if (action.equals(service.getString(R.string.shortcut_value_add_to_clipboard))) {
      AccessibilityNodeInfoCompat node =
          accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ true);
      result = pipeline.returnFeedback(eventId, Feedback.edit(node, APPEND_COPY));
    } else if (action.equals(service.getString(R.string.shortcut_value_clipboard_history))) {
      result = ClipboardHistoryOverlay.open(service);
    } else if (action.equals(service.getString(R.string.shortcut_value_cut))) {
      AccessibilityNodeInfoCompat node = getEditTextFocus();
      if (node == null) {
        result = false;
      } else {
        // Edit text found.
        result = pipeline.returnFeedback(eventId, Feedback.edit(node, CUT));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_clear_text))) {
      AccessibilityNodeInfoCompat node = getEditTextFocus();
      if (node == null) {
        result = false;
      } else {
        result = pipeline.returnFeedback(eventId, Feedback.edit(node, CLEAR_TEXT));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_character_count))) {
      AccessibilityNodeInfoCompat node = getEditTextFocus();
      if (node == null) {
        result = false;
      } else {
        speak(CharacterCount.describe(service, node));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_read_text_field))) {
      AccessibilityNodeInfoCompat field =
          TextFieldContents.field(
              accessibilityFocusMonitor.getInputFocus(),
              accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false));
      if (field == null) {
        speak(service.getString(R.string.not_editable));
      } else {
        // The text goes into the speech history, to repeat or copy, unless it is a password.
        int flags =
            FeedbackItem.FLAG_FORCE_FEEDBACK_ALL
                | (field.isPassword() ? FeedbackItem.FLAG_NO_HISTORY : 0);
        pipeline.returnFeedback(
            eventId,
            Feedback.speech(
                TextFieldContents.describe(service, field),
                SpeakOptions.create().setFlags(flags)));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_paste))) {
      AccessibilityNodeInfoCompat node = getEditTextFocus();
      if (node == null) {
        result = false;
      } else {
        // Edit text found.
        result = pipeline.returnFeedback(eventId, Feedback.edit(node, PASTE));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_voice_feedback))) {
      pipeline.returnFeedback(eventId, Feedback.speech(TOGGLE_VOICE_FEEDBACK));
    } else if (action.equals(service.getString(R.string.shortcut_value_submit_text))) {
      result = SubmitText.submit(service);
      if (!result) {
        pipeline.returnFeedback(eventId, Feedback.sound(R.raw.complete));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_new_line))) {
      AccessibilityNodeInfoCompat node = getEditTextFocus();
      if (node == null) {
        result = false;
      } else if (NewLine.typesNewLine(node)) {
        result = pipeline.returnFeedback(eventId, Feedback.edit(node, INSERT).setText("\n"));
        if (result) {
          // Only "New line", not the text of the field.
          speak(service.getString(R.string.shortcut_new_line));
        }
      } else {
        result = NewLine.performAction(service, node);
        if (!result) {
          pipeline.returnFeedback(eventId, Feedback.sound(R.raw.complete));
        }
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_copy_last_spoken_phrase))) {
      pipeline.returnFeedback(
          eventId, Feedback.part().setSpeech(Feedback.Speech.create(COPY_LAST)));
    } else if (action.equals(
        service.getString(R.string.shortcut_value_select_last_spoken_phrase_word_by_word))) {
      WordSelectionDialog.showForLastUtterance(service, pipeline, /* fromMenu= */ false);
    } else if (action.equals(service.getString(R.string.shortcut_value_copy_text_from_screen))) {
      WordSelectionDialog.showForScreen(service, pipeline);
    } else if (continuousCopyAction) {
      ContinuousCopy continuousCopy = service.getContinuousCopy();
      result = continuousCopy != null && continuousCopy.start(eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_braille_keyboard))
        && brailleScreenInput.isShowing()) {
      result = brailleScreenInput.switchToBrailleKeyboard();
    } else if (action.equals(service.getString(R.string.shortcut_value_braille_keyboard))) {
      String inputMethodInfoId =
          KeyboardUtils.getImeId(
              service, AccessibilityServiceCompatUtils.Constants.BRAILLE_KEYBOARD, /* enabledOnly= */ true);
      if (!TextUtils.isEmpty(inputMethodInfoId)) {
        result = service.getSoftKeyboardController().switchToInputMethod(inputMethodInfoId);
      } else {
        result =
            pipeline.returnFeedback(
                eventId,
                Feedback.speech(
                    service.getString(R.string.switch_to_braille_keyboard_failure_msg)));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_braille_display_settings))) {
      pipeline.returnFeedback(
          eventId, Feedback.triggerIntent(Action.TRIGGER_BRAILLE_DISPLAY_SETTINGS));
    } else if (action.equals(service.getString(R.string.shortcut_value_tutorial))) {
      pipeline.returnFeedback(eventId, Feedback.triggerIntent(Action.TRIGGER_TUTORIAL));
    } else if (action.equals(service.getString(R.string.shortcut_value_practice_gestures))) {
      pipeline.returnFeedback(eventId, Feedback.triggerIntent(Action.TRIGGER_PRACTICE_GESTURE));
    } else if (action.equals(service.getString(R.string.shortcut_value_report_gesture))) {
      result = pipeline.returnFeedback(eventId, Feedback.reportGesture());
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_braille_display))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.performBrailleDisplayAction(TOGGLE_BRAILLE_DISPLAY_ON_OR_OFF));
    } else if (action.equals(service.getString(R.string.shortcut_value_report_ai_response))) {
      result = AiResponseReport.open(service);
      if (!result) {
        pipeline.returnFeedback(
            eventId, Feedback.speech(service.getString(R.string.report_no_ai_response)));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_screen_overview))) {
      result =
          pipeline.returnFeedback(
              EVENT_ID_UNTRACKED,
              GeminiCommandUtils.feedbackForScreenOverview(
                  service,
                  accessibilityFocusMonitor.getAccessibilityFocus(
                      /* useInputFocusIfEmpty= */ false)));
    } else if (action.equals(service.getString(R.string.shortcut_value_label))) {
      result = addOrEditLabel();
    } else if (action.equals(service.getString(R.string.shortcut_value_take_picture_and_describe))) {
      service.startActivity(
          new Intent(service, PictureDescriptionActivity.class)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      result = true;
    } else if (action.equals(service.getString(R.string.shortcut_value_describe_image))) {
      result =
          pipeline.returnFeedback(
              EVENT_ID_UNTRACKED,
              GeminiCommandUtils.feedbackForDescribeImage(
                  service,
                  accessibilityFocusMonitor.getAccessibilityFocus(
                      /* useInputFocusIfEmpty= */ false),
                  actorState));
    } else if (action.equals(service.getString(R.string.shortcut_value_speak_screen_description))) {
      result =
          pipeline.returnFeedback(
              EVENT_ID_UNTRACKED,
              GeminiCommandUtils.feedbackForScreenOverview(
                  service,
                  accessibilityFocusMonitor.getAccessibilityFocus(
                      /* useInputFocusIfEmpty= */ false),
                  /* speak= */ true));
    } else if (action.equals(service.getString(R.string.shortcut_value_speak_image_description))) {
      result =
          pipeline.returnFeedback(
              EVENT_ID_UNTRACKED,
              GeminiCommandUtils.feedbackForDescribeImage(
                  service,
                  accessibilityFocusMonitor.getAccessibilityFocus(
                      /* useInputFocusIfEmpty= */ false),
                  actorState,
                  /* speak= */ true));
    } else if (action.equals(service.getString(R.string.shortcut_value_share_focused_image))
        || action.equals(service.getString(R.string.shortcut_value_share_screenshot))) {
      result =
          ImageSharer.share(
              service,
              accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false),
              /* wholeScreen= */ action.equals(
                  service.getString(R.string.shortcut_value_share_screenshot)),
              message -> pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(message)));
    } else if (action.equals(service.getString(R.string.shortcut_value_ask_about_screen))
        || action.equals(service.getString(R.string.shortcut_value_ask_about_image))) {
      result =
          pipeline.returnFeedback(
              EVENT_ID_UNTRACKED,
              GeminiCommandUtils.feedbackForAskAbout(
                  service,
                  accessibilityFocusMonitor.getAccessibilityFocus(
                      /* useInputFocusIfEmpty= */ false),
                  actorState,
                  /* wholeScreen= */ action.equals(
                      service.getString(R.string.shortcut_value_ask_about_screen))));
    } else if (action.equals(service.getString(R.string.shortcut_value_row_up))) {
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.navigateTable(SEARCH_FOCUS_BACKWARD, NavigationTarget.TARGET_ROW)
                  .setInputMode(INPUT_MODE_TOUCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_row_down))) {
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.navigateTable(SEARCH_FOCUS_FORWARD, NavigationTarget.TARGET_ROW)
                  .setInputMode(INPUT_MODE_TOUCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_column_left))) {
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.navigateTable(SEARCH_FOCUS_BACKWARD, NavigationTarget.TARGET_COLUMN)
                  .setInputMode(INPUT_MODE_TOUCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_column_right))) {
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.navigateTable(SEARCH_FOCUS_FORWARD, NavigationTarget.TARGET_COLUMN)
                  .setInputMode(INPUT_MODE_TOUCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_control_telling_time))) {
      SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
      boolean enabledTellTime =
          SharedPreferencesUtils.getBooleanPref(
              prefs,
              service.getResources(),
              R.string.pref_speak_time_key,
              R.bool.pref_tell_time_default);
      analytics.onManuallyChangeSetting(
          service.getString(R.string.pref_speak_time_key), TalkBackAnalytics.TYPE_GESTURE);
      SharedPreferencesUtils.putBooleanPref(
          prefs, service.getResources(), R.string.pref_speak_time_key, !enabledTellTime);
      pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.complete));
    } else if (action.equals(service.getString(R.string.shortcut_value_control_text_formatting))) {
      SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
      boolean enabledTextFormatting =
          SharedPreferencesUtils.getBooleanPref(
              prefs,
              service.getResources(),
              R.string.pref_formatting_inline_key,
              R.bool.pref_formatting_inline_default);
      boolean toggleTextFormatting = !enabledTextFormatting;
      analytics.onManuallyChangeSetting(
          service.getString(R.string.pref_formatting_inline_key), TalkBackAnalytics.TYPE_GESTURE);
      SharedPreferencesUtils.putBooleanPref(
          prefs, service.getResources(), R.string.pref_formatting_inline_key, toggleTextFormatting);
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.speech(
              service.getString(
                  R.string.text_formatting_state,
                  toggleTextFormatting
                      ? service.getString(R.string.value_on)
                      : service.getString(R.string.value_off))));
    } else if (action.equals(service.getString(R.string.shortcut_value_describe_text_formatting))) {
      result =
          RuleTextFormatting.describeTextFormatting(
              service,
              accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false),
              pipeline);
    } else if (FeatureFlagReader.enableAnnounceCurrentTimeAndDate(service)
        && action.equals(
            service.getString(R.string.shortcut_value_announce_current_time_and_date))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.requestedSpeech(DateTimeUtils.getCurrentTimeAndDate(service)));
    } else if (FeatureFlagReader.enableAnnounceBatteryState(service)
        && action.equals(service.getString(R.string.shortcut_value_announce_battery_state))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.requestedSpeech(batteryMonitor.getBatteryStateDescription()));
    } else if (action.equals(service.getString(R.string.shortcut_value_say_caller))) {
      CallerAnnouncer callerAnnouncer = service.getCallerAnnouncer();
      result =
          callerAnnouncer != null
              && pipeline.returnFeedback(
                  eventId, Feedback.requestedSpeech(callerAnnouncer.sayCallerText()));
    } else if (action.equals(service.getString(R.string.shortcut_value_announce_status))) {
      result =
          pipeline.returnFeedback(eventId, Feedback.requestedSpeech(statusReader.describe()));
    } else if (action.equals(service.getString(R.string.shortcut_value_speak_position))) {
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.requestedSpeech(describePosition()));
    } else if (action.equals(service.getString(R.string.shortcut_value_speak_coordinates))) {
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.requestedSpeech(
                  ItemPosition.coordinates(
                      service, accessibilityFocusMonitor.getAccessibilityFocus(false))));
    } else if (action.equals(service.getString(R.string.shortcut_value_inspect_item))) {
      AccessibilityNodeInfoCompat focused =
          accessibilityFocusMonitor.getAccessibilityFocus(false);
      @Nullable String message = ItemInspector.show(service, focused);
      result =
          message == null
              || pipeline.returnFeedback(eventId, Feedback.requestedSpeech(message));
    } else if (action.equals(service.getString(R.string.shortcut_value_copy_item_details))) {
      AccessibilityNodeInfoCompat focused =
          accessibilityFocusMonitor.getAccessibilityFocus(false);
      result =
          pipeline.returnFeedback(
              eventId, Feedback.requestedSpeech(ItemInspector.copy(service, focused)));
    } else if (action.equals(service.getString(R.string.shortcut_value_copy_screen_tree))) {
      AccessibilityNodeInfoCompat focused =
          accessibilityFocusMonitor.getAccessibilityFocus(false);
      ItemInspector.copyScreenTree(
          service,
          focused,
          message ->
              pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.requestedSpeech(message)));
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_scripts))) {
      @Nullable Boolean scriptsOn = Scripts.toggleAll();
      int message =
          scriptsOn == null
              ? R.string.scripts_unavailable
              : scriptsOn ? R.string.scripts_now_on : R.string.scripts_now_off;
      result =
          pipeline.returnFeedback(eventId, Feedback.requestedSpeech(service.getString(message)));
    } else if (FeatureFlagReader.enableAnnounceCurrentTitle(service)
        && action.equals(service.getString(R.string.shortcut_value_announce_current_title))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.requestedSpeech(screenState.getActiveWindowTitle()));
    } else if (FeatureFlagReader.enableAnnouncePhoneticPronunciation(service)
        && action.equals(
            service.getString(R.string.shortcut_value_announce_phonetic_pronunciation))) {
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.requestedSpeech(
                  processorPhoneticLetters.getPhoneticText(
                      AccessibilityNodeInfoUtils.getNodeText(
                          accessibilityFocusMonitor.getAccessibilityFocus(
                              /* useInputFocusIfEmpty= */ false)))));
    } else if (action.equals(
        service.getString(R.string.shortcut_value_repeat_last_spoken_phrase))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.part().setSpeech(Feedback.Speech.create(REPEAT_LAST)));
    } else if (action.equals(
        service.getString(R.string.shortcut_value_repeat_last_spoken_phrase_slower))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.part().setSpeech(Feedback.Speech.create(REPEAT_LAST_SLOWER)));
    } else if (action.equals(service.getString(R.string.shortcut_value_spell_last_spoken_phrase))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.part().setSpeech(Feedback.Speech.create(SPELL_LAST)));
    } else if (action.equals(service.getString(R.string.shortcut_value_speech_history))) {
      result = SpeechHistoryFragment.open(service);
    } else if (action.equals(
        service.getString(R.string.shortcut_value_translate_last_spoken_phrase))) {
      AccessibilityNodeInfoCompat focus =
          accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ true);
      AccessibilityNodeInfoCompat editing =
          accessibilityFocusMonitor.getEditingNodeFromFocusedKeyboard(focus);
      translateCommand.translateLast(editing != null ? editing : focus);
      result = true;
    } else if (ScriptCommands.isCommand(action)) {
      if (!Scripts.runCommand(action)) {
        pipeline.returnFeedback(
            eventId, Feedback.speech(service.getString(R.string.script_command_not_running)));
      }
    } else if (Macros.isCommand(action)) {
      result = MacroRunner.runCommand(action);
    } else if (CommandMenus.isCommand(action)) {
      result = menuManager.showCustomMenu(action, eventId);
    } else if (MenuCommands.isMenuOnly(service, action)) {
      result = menuManager.showMenuItemCommand(action, eventId);
    } else if (ReadingControlCommands.settingOf(action) != null) {
      selectorController.adjustSettingByCommand(
          eventId, ReadingControlCommands.settingOf(action), ReadingControlCommands.isNext(action));
    } else if (LiveRecognition.isCommand(action)) {
      result = LiveRecognition.runCommand(service, action);
    } else if (action.equals(service.getString(R.string.shortcut_value_text_entities))) {
      result = menuManager.showMenu(TEXT_ENTITIES, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_auto_translation))) {
      selectorController.switchOnOrOffAutoTranslation(eventId, /* hint= */ null);
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_equalizer))) {
      selectorController.switchEqualizerOnOrOff(eventId, /* hint= */ null);
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_switch_languages))) {
      boolean on =
          LanguageSwitchCommands.toggleSwitchLanguages(
              service, SharedPreferencesUtils.getSharedPreferences(service));
      speak(
          service.getString(
              on ? R.string.switch_languages_now_on : R.string.switch_languages_now_off));
    } else if (action.equals(
        service.getString(R.string.shortcut_value_toggle_identify_languages_from_words))) {
      boolean on =
          LanguageSwitchCommands.toggleIdentifyFromWords(
              SharedPreferencesUtils.getSharedPreferences(service));
      speak(
          service.getString(
              on
                  ? R.string.identify_languages_from_words_now_on
                  : R.string.identify_languages_from_words_now_off));
    } else if (SandflyAi.getFeatures() != null
        && action.equals(service.getString(R.string.shortcut_value_ai_agent))) {
      SandflyAi.getFeatures().startAgent(service);
      result = true;
    } else if (SandflyAi.getFeatures() != null
        && SandflyAi.getFeatures().runCommand(service, action)) {
      result = true;
    } else if (FeatureFlagReader.enableSpeechPitchKeyboard(service)
        && action.equals(service.getString(R.string.shortcut_value_speech_pitch_decrease))) {
      result = speechRateAndPitchActor.changeSpeechPitch(false);
    } else if (FeatureFlagReader.enableSpeechPitchKeyboard(service)
        && action.equals(service.getString(R.string.shortcut_value_speech_pitch_increase))) {
      result = speechRateAndPitchActor.changeSpeechPitch(true);
    } else if (FeatureFlagReader.enableSpeechRateKeyboard(service)
        && action.equals(service.getString(R.string.shortcut_value_speech_rate_decrease))) {
      selectorController.changeSpeechRate(eventId, false);
    } else if (FeatureFlagReader.enableSpeechRateKeyboard(service)
        && action.equals(service.getString(R.string.shortcut_value_speech_rate_increase))) {
      selectorController.changeSpeechRate(eventId, true);
    } else if (action.equals(service.getString(R.string.shortcut_long_press_talkback_exit))) {
      service.requestDisableTalkBackByTalkBackExitBanner();
    }

    if (!result) {
      // Show failure hint if perform action fail.
      pipeline.returnFeedback(eventId, Feedback.sound(R.raw.complete));
    }
  }

  public boolean isFingerprintGestureAssigned(int fingerprintGestureId) {
    return !TextUtils.equals(
        service.getString(R.string.shortcut_value_unassigned),
        actionFromFingerprintGesture(fingerprintGestureId));
  }

  @SuppressWarnings("NewApi")
  public void onGesture(int gestureId, EventId eventId) {
    RecentUserInput.mark();
    OutputMute.onUserInput("gesture");
    if (gestureId == GESTURE_FAKED_SPLIT_TYPING_AND_HOLD) {
      // The intended behavior is simulating the non-customizable gesture "double-tap-and-hold".
      gestureId = GESTURE_DOUBLE_TAP_AND_HOLD;
    }
    if (GestureTrainerHooks.onGesture(gestureId)) {
      return;
    }
    if (gestureHandledByTraining(gestureId, /* isFingerprintGesture= */ false)) {
      return;
    }
    if (disabledGestures.contains(gestureId)) {
      return;
    }
    if (menuManager.consumeGestureForRadialMenu()) {
      return;
    }

    if (LiveRecognition.onGesture(gestureId)) {
      return;
    }
    // Quick Tasks take a few gestures for the three seconds after the Quick Tasks command.
    String quickTask = QuickTasks.action(gestureShortcutMapping.getGestureKey(gestureId));
    if (quickTask != null) {
      performGestureAction(quickTask, eventId, gestureId);
      return;
    }
    // A gesture profile's own gestures come before scripts.
    boolean fromProfile = gestureShortcutMapping.getProfileActionKeyFromGestureId(gestureId) != null;
    String action = gestureShortcutMapping.getActionKeyFromGestureId(gestureId);
    // Rows and columns, the table reading control, always takes Swipe left and Swipe right, whatever
    // a gesture profile, an app or a script gives them.
    boolean columnSwipe =
        actorState.getDirectionNavigation().getCurrentGranularity() == CursorGranularity.ROW_COLUMN
            && (gestureId == AccessibilityService.GESTURE_SWIPE_LEFT
                || gestureId == AccessibilityService.GESTURE_SWIPE_RIGHT);
    if (columnSwipe) {
      action =
          service.getString(
              gestureId == AccessibilityService.GESTURE_SWIPE_LEFT
                  ? R.string.shortcut_value_column_left
                  : R.string.shortcut_value_column_right);
    }
    LogUtils.v(
        LOG_TAG,
        "Recognized gesture id: %d [%s] , action: [%s]",
        gestureId,
        GestureShortcutMapping.getGestureString(service, gestureId),
        action);
    if (swipeUpForScrollUp(action)) {
      return;
    }
    GestureSpeechTiming.onGesture(action);
    if (TutorialHooks.onGesture(gestureId, action)) {
      return;
    }
    String fallbackAction = action;
    int fallbackGestureId = gestureId;
    // A script can hand the gesture back later, so the fallback keeps which gesture it was.
    if (!fromProfile
        && !columnSwipe
        && !turnsScriptsOff(action)
        && Scripts.onGesture(
            gestureId,
            () -> performGestureAction(fallbackAction, eventId, fallbackGestureId))) {
      return;
    }
    performGestureAction(action, eventId, gestureId);
  }

  /**
   * While SandFly takes two-finger swipes, the lock screen and the launcher never see one, so the
   * gesture for Scroll up, Two-finger swipe up by default, sends SandFly's own swipe up there
   * instead of scrolling: two fingers on the lock screen to unlock it or show the PIN, pattern or
   * password, one finger on the launcher to open its apps list. A gesture with another command,
   * Two-finger swipe up included, runs that command. Returns whether the swipe was sent.
   */
  private boolean swipeUpForScrollUp(@Nullable String action) {
    if (action == null
        || !action.equals(service.getString(R.string.shortcut_value_scroll_up))
        || !GestureLayout.handlesTwoFingerSwipes(
            SharedPreferencesUtils.getSharedPreferences(service))) {
      return false;
    }
    boolean sent;
    if (swipeUp.isLockScreenShown()) {
      sent = swipeUp.swipeUp(/* fingers= */ 2);
    } else {
      sent = swipeUp.isHomeScreenShown() && swipeUp.swipeUp(/* fingers= */ 1);
    }
    if (sent) {
      LogUtils.i(LOG_TAG, "Scroll up sent a swipe up to the lock screen or the launcher");
    }
    return sent;
  }

  /** Opens the Add label or Edit label dialog for the focused item. */
  private boolean addOrEditLabel() {
    AccessibilityNodeInfoCompat node =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    LabelManager.State labels = actorState.getLabelManagerState();
    if (node == null || !labels.supportsLabel(node)) {
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.speech(service.getString(R.string.voice_commands_cannot_label_feedback)));
      return false;
    }
    long labelId = labels.getLabelIdForNode(node);
    return labelId == Label.NO_ID
        ? LabelDialogManager.addLabel(
            service, node.getViewIdResourceName(), /* needToRestoreFocus= */ true, pipeline)
        : LabelDialogManager.editLabel(service, labelId, /* needToRestoreFocus= */ true, pipeline);
  }

  /** Performs [action] of the gesture [gestureId], noting the gesture. */
  private void performGestureAction(String action, EventId eventId, int gestureId) {
    int previousGestureId = performingGestureId;
    performingGestureId = gestureId;
    try {
      performAction(action, eventId);
    } finally {
      performingGestureId = previousGestureId;
    }
  }

  /**
   * Whether the gesture being performed increases a value, such as a slider's, or null when no
   * straight swipe is being performed.
   */
  private @Nullable Boolean swipeIncreasesValue() {
    if (performingGestureId == NO_GESTURE) {
      return null;
    }
    return SwipeValueDirection.increases(
        performingGestureId, WindowUtils.isScreenLayoutRTL(service));
  }

  /**
   * With Double-tap moves cursor to start or end on, Activate moves the cursor of the text field
   * that accessibility focus is on, while it has input focus and the keyboard is shown, to the
   * start of its text, or to the end when it is at the start. Returns whether it moved the cursor.
   */
  private boolean doubleTapMovesCursor(EventId eventId) {
    if (!SharedPreferencesUtils.getBooleanPref(
            SharedPreferencesUtils.getSharedPreferences(service),
            service.getResources(),
            R.string.pref_double_tap_moves_cursor_key,
            R.bool.pref_double_tap_moves_cursor_default)
        || !KeyboardUtils.isSoftKeyboardActive(service)) {
      return false;
    }
    @Nullable AccessibilityNodeInfoCompat field =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (field == null
        || Role.getRole(field) != Role.ROLE_EDIT_TEXT
        || !field.isFocused()
        || field.isShowingHintText()
        || TextUtils.isEmpty(AccessibilityNodeInfoUtils.getText(field))) {
      return false;
    }
    boolean atStart = field.getTextSelectionStart() == 0 && field.getTextSelectionEnd() == 0;
    return pipeline.returnFeedback(
        eventId, Feedback.edit(field, atStart ? CURSOR_TO_END : CURSOR_TO_BEGINNING));
  }

  public void setCaptureGestureIdToAnnouncements(
      @NonNull ImmutableMap<Integer, Integer> captureGestureIdToAnnouncements,
      @NonNull ImmutableMap<Integer, Integer> captureFingerprintGestureIdToAnnouncements) {
    this.captureGestureIdToAnnouncements.clear();
    if (!captureGestureIdToAnnouncements.isEmpty()) {
      this.captureGestureIdToAnnouncements.putAll(captureGestureIdToAnnouncements);
    }

    this.captureFingerprintGestureIdToAnnouncements.clear();
    if (!captureFingerprintGestureIdToAnnouncements.isEmpty()) {
      this.captureFingerprintGestureIdToAnnouncements.putAll(
          captureFingerprintGestureIdToAnnouncements);
    }
    onTrainingOpen(/* open= */ true);
  }

  /** Sets where Speak position finds scroll positions and the cursor in text that apps move. */
  public void setPositionSources(
      ScrollEventInterpreter scrollEventInterpreter, TextTraversalTracker textTraversalTracker) {
    this.scrollEventInterpreter = scrollEventInterpreter;
    this.textTraversalTracker = textTraversalTracker;
  }

  /** Returns the position of the focused item for Speak position. */
  private String describePosition() {
    @Nullable AccessibilityNodeInfoCompat focused =
        accessibilityFocusMonitor.getAccessibilityFocus(false);
    @Nullable ScrollEventInterpreter scrolls = scrollEventInterpreter;
    @Nullable TextTraversalTracker traversals = textTraversalTracker;
    @Nullable TextCursor cursor =
        TextPosition.cursorOf(
            focused,
            actorState.getDirectionNavigation().ownTextCursor(focused),
            traversals == null ? null : traversals.cursor());
    return ItemPosition.describe(
        service,
        focused,
        cursor,
        node -> scrolls == null ? -1 : scrolls.scrollPercent(node));
  }

  /** Notes whether the training is open, whose current page may take gestures. */
  public void onTrainingOpen(boolean open) {
    boolean takes = false;
    if (open) {
      for (int gestureId : captureGestureIdToAnnouncements.keySet()) {
        if (GestureConfiguration.isTwoDirectionSwipe(gestureId)) {
          takes = true;
          break;
        }
      }
    }
    trainingTakesTwoDirectionSwipes = takes;
    trainingTakesGestures = open && !captureGestureIdToAnnouncements.isEmpty();
  }

  /**
   * Returns whether an open training page takes gestures, which then need multi-finger taps to wait
   * for a further tap.
   */
  public static boolean trainingTakesGestures() {
    return trainingTakesGestures;
  }

  /**
   * Returns whether an open training page takes back-and-forth or angle gestures, which then need
   * swipes to wait for the lift.
   */
  public static boolean trainingTakesTwoDirectionSwipes() {
    return trainingTakesTwoDirectionSwipes;
  }

  /**
   * Whether the action is the one that turns all scripts off. Scripts are never offered the gesture
   * it is assigned to, so a script that takes over gestures can't take that one.
   */
  private boolean turnsScriptsOff(@Nullable String action) {
    return service.getString(R.string.shortcut_value_toggle_scripts).equals(action);
  }

  private boolean gestureHandledByTraining(int gestureId, boolean isFingerprintGesture) {
    // TalkBack can ignore the gesture, which is handled by OnGestureListener.onCaptureGesture(), if
    // captured gesture list exists and the current window is training.
    @Nullable Integer feedbackResId =
        isFingerprintGesture
            ? captureFingerprintGestureIdToAnnouncements.get(gestureId)
            : captureGestureIdToAnnouncements.get(gestureId);
    if (feedbackResId == null || feedbackResId == UNKNOWN_ANNOUNCEMENT) {
      return false;
    }
    // Checked last: it asks the app for the window's nodes, which holds up every gesture.
    if (!isOnTrainingPage()) {
      return false;
    }

    // An announcement says what the gesture does by default. If the user gave the gesture another
    // action, say the gesture and that action instead.
    if (!isFingerprintGesture
        && feedbackResId != ANNOUNCE_REAL_ACTION
        && !TutorialGestureUses.announcementStillTrue(
            service, feedbackResId, gestureShortcutMapping.getActionKeyFromGestureId(gestureId))) {
      feedbackResId = ANNOUNCE_REAL_ACTION;
    }

    String feedbackString;
    if (feedbackResId == ANNOUNCE_REAL_ACTION) {
      SpannableStringBuilder gestureAndAction = new SpannableStringBuilder();
      if (isFingerprintGesture) {
        StringBuilderUtils.appendWithSeparator(
            gestureAndAction,
            GestureShortcutMapping.getFingerprintGestureString(service, gestureId),
            GestureShortcutMapping.getActionString(
                service, gestureShortcutMapping.getActionKeyFromFingerprintGestureId(gestureId)));
      } else {
        StringBuilderUtils.appendWithSeparator(
            gestureAndAction,
            GestureShortcutMapping.getGestureString(service, gestureId),
            GestureShortcutMapping.getActionString(
                service, gestureShortcutMapping.getActionKeyFromGestureId(gestureId)));
      }
      feedbackString = gestureAndAction.toString();
    } else {
      feedbackString = service.getString(feedbackResId);
    }
    pipeline.returnFeedback(
        /* eventId= */ null,
        Feedback.speech(
            feedbackString,
            SpeakOptions.create().setQueueMode(QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH)));
    return true;
  }

  /** Returns true if the active window is training activity. */
  private boolean isOnTrainingPage() {
    // Compares resource ID with all child nodes on the active window if navigate-up arrow is
    // shown because the first child node is the navigate-up arrow which is no resource ID.
    return WindowUtils.rootChildMatchesResId(service, TrainingActivity.ROOT_RES_ID);
  }

  /**
   * Runs the command of a back tap or a shake, named by its preference key on the Gestures screen
   * without a gesture set.
   */
  public void onAccelerometerGesture(String key, EventId eventId) {
    String unassigned = service.getString(R.string.shortcut_value_unassigned);
    String action = gestureShortcutMapping.getActionKeyFromPrefKey(key, unassigned);
    LogUtils.v(LOG_TAG, "Accelerometer gesture [%s], action: [%s]", key, action);
    if (TextUtils.isEmpty(action) || action.equals(unassigned)) {
      return;
    }
    pipeline.returnFeedback(eventId, Feedback.part().setInterruptGentle(true));
    performAction(action, eventId);
  }

  /**
   * The interruptAllFeedback inside performAction is effective only when CRM is active.. Here to
   * perform an equivalent logic(by interruptGentle) to interrupt feedback when CRM is inactive.
   */
  public void onFingerprintGesture(int fingerprintGestureId, EventId eventId) {
    if (gestureHandledByTraining(fingerprintGestureId, /* isFingerprintGesture= */ true)) {
      return;
    }
    pipeline.returnFeedback(eventId, Feedback.part().setInterruptGentle(true));
    String action = actionFromFingerprintGesture(fingerprintGestureId);

    LogUtils.v(
        LOG_TAG,
        "Recognized fingerprint gesture id: %d [%s] , action: [%s]",
        fingerprintGestureId,
        GestureShortcutMapping.getFingerprintGestureString(service, fingerprintGestureId),
        action);
    if (!turnsScriptsOff(action)
        && Scripts.onFingerprintGesture(
            fingerprintGestureId, () -> performAction(action, eventId))) {
      return;
    }
    performAction(action, eventId);
  }

  /**
   * Change the action if it's not allowed in continuous reading mode.
   *
   * @param action the action mapping from preference
   */
  private void maybeInterruptAllFeedback(String action) {
    if (action.equals(service.getString(R.string.shortcut_value_previous))
        || action.equals(service.getString(R.string.shortcut_value_next))
        || action.equals(service.getString(R.string.shortcut_value_unassigned))) {
      return;
    }
    if (!actorState.getContinuousRead().isActive()) {
      if (action.equals(service.getString(R.string.shortcut_value_pause_or_resume_feedback))
          && speaker.readyToPause()) {
        // When the pause gesture is triggered in non CRM, the FullScreenReadActor must be informed
        // to reset the pause record for pending CRM.
        service.interruptFullScreenReadActor();
      }
      return;
    }

    service.interruptAllFeedback(false);
  }

  /** Checks if users have changed gestures manually. */
  public static boolean isAnyGestureChanged(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String[] gestureKeys = context.getResources().getStringArray(R.array.pref_shortcut_keys);
    for (String key : gestureKeys) {
      if (prefs.contains(key)) {
        return true;
      }
    }
    return false;
  }

  private @Nullable AccessibilityNodeInfoCompat getSelectTextFocus(boolean speakIfNone) {
    @Nullable AccessibilityNodeInfoCompat node =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ true);
    if (AccessibilityNodeInfoUtils.isTextSelectable(node)) {
      return node;
    }
    @Nullable AccessibilityNodeInfoCompat editingNode =
        accessibilityFocusMonitor.getEditingNodeFromFocusedKeyboard(node);
    if (editingNode != null) {
      return editingNode;
    }
    // Other text is selected for copying by Backtalk; it speaks if there is none.
    if (node != null) {
      return node;
    }
    if (speakIfNone) {
      speak(service.getString(R.string.not_selectable));
    }
    return null;
  }

  /**
   * Returns the focused text field, or the text field that the keyboard in focus types into, or
   * says that there is none and returns null.
   */
  private @Nullable AccessibilityNodeInfoCompat getEditTextFocus() {
    @Nullable AccessibilityNodeInfoCompat node =
        accessibilityFocusMonitor.getNodeForEditingActions(
            accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ true));
    if (node != null) {
      return node;
    }
    speak(service.getString(R.string.not_editable));
    return null;
  }

  /** Returns the field that the keyboard in focus types into, or else the focused node. */
  private @Nullable AccessibilityNodeInfoCompat dictationTarget() {
    AccessibilityNodeInfoCompat focus =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ true);
    AccessibilityNodeInfoCompat editing =
        accessibilityFocusMonitor.getEditingNodeFromFocusedKeyboard(focus);
    return editing != null ? editing : focus;
  }

  /**
   * Returns the text field that has input focus while a keyboard is active, or null when no field
   * is in its editing state.
   */
  private @Nullable AccessibilityNodeInfoCompat editingTextField() {
    if (!KeyboardUtils.isKeyboardActive(service)) {
      return null;
    }
    AccessibilityNodeInfoCompat field = dictationTarget();
    if (Role.getRole(field) != Role.ROLE_EDIT_TEXT || !field.isFocused()) {
      field = getEditTextInputFocus();
    }
    return field != null && field.isFocused() && field.isVisibleToUser() ? field : null;
  }

  private @Nullable AccessibilityNodeInfoCompat getEditTextInputFocus() {
    @Nullable AccessibilityNodeInfoCompat node = accessibilityFocusMonitor.getInputFocus();
    if (Role.getRole(node) == Role.ROLE_EDIT_TEXT) {
      return node;
    } else {
      return null;
    }
  }

  /** Changes to the next or previous braille command profile, with Default first, and says it. */
  private void changeBrailleCommandProfile(boolean next) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
    if (!BrailleCommands.hasProfiles(prefs)) {
      speak(service.getString(R.string.no_braille_command_profiles));
      return;
    }
    @Nullable String name =
        BrailleCommands.select(service, prefs, BrailleCommands.adjacentId(prefs, next));
    speak(name != null ? name : service.getString(R.string.no_braille_command_profiles));
  }

  /** Braille commands, app search and typing on the screen. */
  public BrailleScreenInput getBrailleScreenInput() {
    return brailleScreenInput;
  }

  /** What braille commands reach here. */
  private final class BrailleHost implements BrailleScreenInputHost {
    @Override
    public void runCommand(String value) {
      // A command typed in braille runs even when it opens a window.
      runAction(value, EVENT_ID_UNTRACKED);
    }

    @Override
    public boolean canType() {
      return isBrailleKeyboardShown() || editingTextField() != null;
    }

    @Override
    public boolean isBrailleKeyboardShown() {
      return service.isBrailleKeyboardShown();
    }

    @Override
    public boolean switchToBrailleKeyboard() {
      String id =
          KeyboardUtils.getImeId(
              service,
              AccessibilityServiceCompatUtils.Constants.BRAILLE_KEYBOARD,
              /* enabledOnly= */ true);
      if (TextUtils.isEmpty(id)
          || Build.VERSION.SDK_INT < Build.VERSION_CODES.R
          || !service.getSoftKeyboardController().switchToInputMethod(id)) {
        speak(service.getString(R.string.switch_to_braille_keyboard_failure_msg));
        return false;
      }
      return true;
    }

    @Override
    public boolean findText(String text, boolean forward, boolean wrap, Runnable onWrap) {
      return service.findText(text, forward, wrap, onWrap);
    }
  }

  /**
   * Speaks the result of Answer call or End call: {@code noCall} when there is no call, {@code
   * permission} when Android asks for the permission, and the failure sound when it failed.
   */
  private void onCallResult(AnswerCall.Result callResult, int noCall, int permission) {
    switch (callResult) {
      case NO_CALL -> speak(service.getString(noCall));
      case PERMISSION_REQUESTED -> speak(service.getString(permission));
      case FAILED -> pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.complete));
      case DONE -> {}
    }
  }

  /** voice feedback function */
  private void speak(String text) {
    SpeakOptions speakOptions =
        SpeakOptions.create()
            .setFlags(
                FeedbackItem.FLAG_NO_HISTORY
                    | FeedbackItem.FLAG_FORCE_FEEDBACK_ALL);
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(text, speakOptions));
  }

  private EventId lastChangeAccessibilityEventId;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final VolumeChangedListener a11yVolumeChangedListener =
      new VolumeChangedListener() {

        @Override
        public void onVolumeChanged(int type, int value, int prevValue, int percent) {
          if (type != AudioManager.STREAM_ACCESSIBILITY) {
            return;
          }

          String volumeText =
              service.getString(
                  value < prevValue
                      ? R.string.template_accessibility_volume_change_decrease
                      : R.string.template_accessibility_volume_change_increase,
                  percent);
          TalkBackUI.Item item =
              value < prevValue
                  ? Item.ITEM_ACCESSIBILITY_VOLUME_DECREASE
                  : Item.ITEM_ACCESSIBILITY_VOLUME_INCREASE;
          announceChangedAccessibilityVolume(lastChangeAccessibilityEventId, volumeText, item);

          handler.post(() -> volumeMonitor.removeVolumeChangedListener(a11yVolumeChangedListener));
        }
      };

  private void changeAccessibilityVolume(EventId eventId, boolean decrease) {
    boolean result =
        pipeline.returnFeedback(
            eventId,
            Feedback.adjustVolume(
                decrease ? DECREASE_VOLUME : INCREASE_VOLUME, STREAM_TYPE_ACCESSIBILITY));
    if (result) {
      refreshVolumeChangedListener(eventId);
    } else {
      String volumeText =
          service.getString(
              decrease
                  ? R.string.template_volume_change_minimum
                  : R.string.template_volume_change_maximum);
      TalkBackUI.Item item =
          decrease
              ? Item.ITEM_ACCESSIBILITY_VOLUME_MINIMUM
              : Item.ITEM_ACCESSIBILITY_VOLUME_MAXIMUM;
      announceChangedAccessibilityVolume(eventId, volumeText, item);
    }
  }

  /** Changes the media volume a step and says the new volume. */
  private void changeMediaVolume(EventId eventId, boolean decrease) {
    AudioManager audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
    if (audioManager == null) {
      return;
    }
    int stream = AudioManager.STREAM_MUSIC;
    int min =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? audioManager.getStreamMinVolume(stream) : 0;
    int max = audioManager.getStreamMaxVolume(stream);
    int volume = audioManager.getStreamVolume(stream);
    String volumeText;
    if (max <= min || (decrease && volume <= min) || (!decrease && volume >= max)) {
      volumeText =
          service.getString(
              decrease
                  ? R.string.template_volume_change_minimum
                  : R.string.template_volume_change_maximum);
    } else {
      service.ignoreSelfVolumeChange(stream);
      audioManager.adjustStreamVolume(
          stream, decrease ? AudioManager.ADJUST_LOWER : AudioManager.ADJUST_RAISE, 0);
      volume = audioManager.getStreamVolume(stream);
      volumeText =
          service.getString(
              R.string.template_stream_volume_set,
              service.getString(R.string.value_stream_music),
              Math.round(100f * (volume - min) / (max - min)));
    }
    announceChangedAccessibilityVolume(
        eventId,
        volumeText,
        decrease ? Item.ITEM_ACCESSIBILITY_VOLUME_DECREASE : Item.ITEM_ACCESSIBILITY_VOLUME_INCREASE);
  }

  /** Sends a media key press to the active media session. */
  private boolean dispatchMediaKey(int keyCode) {
    AudioManager audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
    if (audioManager == null) {
      return false;
    }
    audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
    audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
    return true;
  }

  private void refreshVolumeChangedListener(EventId eventId) {
    lastChangeAccessibilityEventId = eventId;
    // Just in case that we get interrupted in VolumeChangedListener#onVolumeChanged so we failed to
    // remove this listener when the callback is finished.
    volumeMonitor.removeVolumeChangedListener(a11yVolumeChangedListener);
    volumeMonitor.addVolumeChangedListener(a11yVolumeChangedListener);
  }

  private void announceChangedAccessibilityVolume(
      EventId eventId, String volumeText, TalkBackUI.Item item) {
    pipeline.returnFeedback(
        eventId,
        Feedback.speech(
            volumeText,
            SpeakOptions.create()
                .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
                .setFlags(
                    FeedbackItem.FLAG_NO_HISTORY
                        | FeedbackItem.FLAG_FORCE_FEEDBACK_ALL
                        | FeedbackItem.FLAG_SKIP_DUPLICATE)));

    pipeline.returnFeedback(
        eventId,
        Feedback.talkBackUI(
            TalkBackUI.Action.SHOW_GESTURE_OR_KEYBOARD_ACTION_UI,
            GESTURE_OR_KEYBOARD_ACTION_OVERLAY,
            volumeText,
            /* showIcon= */ false,
            item));
  }

  private final HashSet<Integer> disabledGestures = new HashSet<>();

  public void disableGestures(int... gestures) {
    for (int gesture : gestures) {
      disabledGestures.add(gesture);
    }
  }

  public void enabledGestures(int... gestures) {
    for (int gesture : gestures) {
      disabledGestures.remove(gesture);
    }
  }
}
