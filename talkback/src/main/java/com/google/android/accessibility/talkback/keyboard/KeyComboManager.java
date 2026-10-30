/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.google.android.accessibility.talkback.keyboard;

import static android.content.res.Configuration.HARDKEYBOARDHIDDEN_NO;
import static android.content.res.Configuration.HARDKEYBOARDHIDDEN_UNDEFINED;
import static android.content.res.Configuration.KEYBOARDHIDDEN_YES;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.BROWSE_MODE_TURNED_OFF_BY_SMART_BROWSE_MODE;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.BROWSE_MODE_TURNED_OFF_BY_USER;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.BROWSE_MODE_TURNED_OFF_ON_SERVICE_STOP;
import static com.google.android.accessibility.talkback.keyboard.KeyCombo.EXACT_MATCH;
import static com.google.android.accessibility.talkback.keyboard.KeyCombo.KEY_EVENT_MODIFIER_MASK;
import static com.google.android.accessibility.talkback.keyboard.KeyCombo.PARTIAL_MATCH;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;
import static com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH;
import static java.lang.Math.max;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.gesture.AppGestures;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.commands.Commands;
import com.google.android.accessibility.talkback.actor.DirectionNavigationActor;
import com.google.android.accessibility.talkback.actor.FullScreenReadActor;
import com.google.android.accessibility.talkback.actor.SpeechRateAndPitchActor;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.BrowseModeLogTrigger;
import com.google.android.accessibility.talkback.contextmenu.ListMenuManager;
import com.google.android.accessibility.talkback.eventprocessor.ProcessorPhoneticLetters;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenStateMonitor;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping;
import com.google.android.accessibility.talkback.monitor.BatteryMonitor;
import com.google.android.accessibility.talkback.scripting.Scripts;
import com.google.android.accessibility.talkback.selector.SelectorController;
import com.google.android.accessibility.talkback.trainingcommon.TrainingActivity;
import com.google.android.accessibility.talkback.tutorial.TutorialHooks;
import com.google.android.accessibility.utils.AccessibilityEventListener;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.ServiceKeyEventListener;
import com.google.android.accessibility.utils.ServiceStateListener;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.WindowUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import com.google.android.accessibility.utils.monitor.CollectionState;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Manages state related to detecting key combinations. */
public class KeyComboManager
    implements ServiceKeyEventListener, ServiceStateListener, AccessibilityEventListener {
  private static final String TAG = "KeyComboManager";
  public static final int KEYMAP_DEFAULT = R.string.new_keymap_entry_value;

  /** These KeyEvent can works independently due to handle by input frameworks. */
  private static final ImmutableSet<Integer> KEY_EVENT_INDEPENDENT_FUNCTIONAL_KEYS =
      ImmutableSet.of(
          KeyEvent.KEYCODE_DPAD_UP,
          KeyEvent.KEYCODE_DPAD_DOWN,
          KeyEvent.KEYCODE_DPAD_LEFT,
          KeyEvent.KEYCODE_DPAD_RIGHT,
          KeyEvent.KEYCODE_ENTER);

  public static final String CONCATENATION_STR = " + ";
  private static final String KEYCODE_PREFIX = "KEYCODE_";

  /** When user has pressed same key twice less than this interval, we handle them as double tap. */
  @VisibleForTesting static final Duration TIME_TO_DETECT_DOUBLE_TAP = Duration.ofMillis(300);

  /**
   * Speak options for interruptive speech feedback. This is used to interrupt any existing,
   * interruptible speech feedback.
   */
  @VisibleForTesting
  static final SpeakOptions INTERRUPTIVE_SPEAK_OPTIONS =
      SpeakOptions.create()
          .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
          .setFlags(
              FeedbackItem.FLAG_NO_HISTORY
                  | FeedbackItem.FLAG_FORCE_FEEDBACK_ALL
                  | FeedbackItem.FLAG_SKIP_DUPLICATE);

  /** Speak options for speech feedback that is uninterruptible by new speech. */
  @VisibleForTesting
  static final SpeakOptions UNINTERRUPTIBLE_BY_NEW_SPEECH_SPEAK_OPTIONS =
      SpeakOptions.create()
          .setQueueMode(SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH)
          .setFlags(
              FeedbackItem.FLAG_NO_HISTORY
                  | FeedbackItem.FLAG_FORCE_FEEDBACK_ALL
                  | FeedbackItem.FLAG_SKIP_DUPLICATE);

  /**
   * Time to wait for a key after the prefix key combo has been pressed. If no key is pressed within
   * this interval, exit the sequence key mode.
   */
  private static final Duration SEQUENCE_KEY_MODE_TIMEOUT = Duration.ofSeconds(3);
  // The removed Default keymap's value of the keymap preference.
  private static final String DEFAULT_KEYMAP_VALUE = "default_keymap";

  /** Returns keyComboCode that represent keyEvent. */
  public static long getKeyComboCode(KeyEvent keyEvent) {
    if (keyEvent == null) {
      return KeyComboModel.KEY_COMBO_CODE_UNASSIGNED;
    }

    int modifier = keyEvent.getModifiers() & KEY_EVENT_MODIFIER_MASK;
    return KeyCombo.computeKeyComboCode(modifier, getConvertedKeyCode(keyEvent));
  }

  /** Returns keyComboCode that represents [keyEvent], with the SandFly key while it's held. */
  public long getKeyComboCodeWithSandFlyKey(KeyEvent keyEvent) {
    return KeyCombo.computeKeyComboCode(modifiersOf(keyEvent), getConvertedKeyCode(keyEvent));
  }

  /**
   * Returns converted key code. This method converts the following key events. - Convert
   * KEYCODE_HOME with meta to KEYCODE_ENTER. - Convert KEYCODE_BACK with meta to KEYCODE_DEL.
   *
   * @param event Key event to be converted.
   * @return Converted key code.
   */
  public static int getConvertedKeyCode(KeyEvent event) {
    // We care only when meta key is pressed with.
    if ((event.getModifiers() & KeyEvent.META_META_ON) == 0) {
      return event.getKeyCode();
    }

    if (event.getKeyCode() == KeyEvent.KEYCODE_HOME) {
      return KeyEvent.KEYCODE_ENTER;
    } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
      return KeyEvent.KEYCODE_DEL;
    } else {
      return event.getKeyCode();
    }
  }

  /** Whether the user performed a combo during the current interaction. */
  private boolean performedCombo;

  /** Whether the user may be performing a combo and we should intercept keys. */
  private boolean hasPartialMatch;

  private final Set<Integer> currentKeysDown = new HashSet<>();
  private final Set<Integer> passedKeys = new HashSet<>();
  /** Keys whose key down a script took, so that their key up is not passed on either. */
  private final Set<Integer> scriptKeys = new HashSet<>();
  private final Set<Integer> keyComboPassThroughPressedKeys = new HashSet<>();

  /** Commands only work in Browse Mode. Built during initialization. */
  private final ImmutableSet<String> browseModeOnlyCommands;

  private KeyCombo currentKeyCombo = new KeyCombo();
  private long currentKeyComboTime = 0;
  private KeyCombo previousKeyCombo = new KeyCombo();
  private long previousKeyComboTime = 0;
  private int maxKeysDown = 0;

  private final Context context;
  private SharedPreferences sharedPreferences;
  private Pipeline.FeedbackReturner pipeline;
  private AccessibilityFocusMonitor accessibilityFocusMonitor;
  private boolean matchKeyCombo = true;
  private boolean browseModeEnabled = false;
  private long browseModeEnabledTimeMills = 0;

  /**
   * Whether the user turned browse mode off in the web content that has focus. Smart browse mode
   * then leaves it off, so that arrow keys keep going to the page, until the user turns it on or
   * focus leaves the web content.
   */
  private boolean browseModeTurnedOffByUser = false;

  /** The app that accessibility focus was last in. */
  private @Nullable String focusedPackage;
  private boolean captureTrainingCombos = false;
  private KeyComboModel keyComboModel;
  private int serviceState = SERVICE_STATE_INACTIVE;
  private ServiceKeyEventListener keyEventDelegate;
  private KeyComboMapper keyComboMapper;
  private TalkBackAnalytics analytics;
  private int hardwareKeyboardStatus = HARDKEYBOARDHIDDEN_UNDEFINED;
  private A11yAlertDialogWrapper updateModifierKeysDialog;
  // Whether a SandFly key that Android doesn't treat as a modifier, such as Insert, is down.
  private boolean sandFlyKeyHeld;
  private KeyComboPassThroughState keyComboPassThroughState = KeyComboPassThroughState.INACTIVE;
  private ScheduledExecutorService scheduler;
  private Future<Boolean> scheduleToOpenSearchApp;
  private final ArrowKeyNavKeyHandler arrowKeyNavKeyHandler;

  private enum KeyComboPassThroughState {
    // KeyComboPassThrough is not enabled and TalkBack will act on key events as normal.
    INACTIVE,
    // User has pressed the keys assigned to the KeyComboPassThrough shortcut, but has not yet
    // released the keys.
    PENDING_PASS_THROUGH_SHORTCUT_RELEASE,
    // KeyComboPassThrough is enabled and TalkBack will pass the next key events through to the
    // system.
    ACTIVE
  }

  public static KeyComboManager create(Context context) {
    return new KeyComboManager(context);
  }

  private KeyComboManager(Context context) {
    this.context = context;
    sharedPreferences = SharedPreferencesUtils.getSharedPreferences(context);
    initializeDefaultPreferenceValues();
    keyComboModel = createKeyComboModel();
    arrowKeyNavKeyHandler =
        new ArrowKeyNavKeyHandler(
            context,
            new ArrowKeyNavKeyHandler.Callbacks() {
              @Override
              public void perform(String command) {
                if (TutorialHooks.onKeys(/* keys= */ null, command)) {
                  return;
                }
                performCommandFromKeyboard(
                    command,
                    Performance.getInstance()
                        .onKeyComboEventReceived(
                            TalkBackPhysicalKeyboardShortcut.ACTION_UNKNOWN
                                .getKeyboardShortcutOrdinal()));
              }

              @Override
              public boolean isEditingText() {
                return isEditingTextWithAccessibilityFocus();
              }
            });
    // Create a set of commands that only work in Browse Mode.
    browseModeOnlyCommands =
        FeatureFlagReader.enableBrowseMode(context)
            ? ImmutableSet.copyOf(initializeBrowseModeOnlyCommands())
            : ImmutableSet.of();
  }

  // TODO: KeyComboManager would be separated into KeyComboManager
  // and KeyComboModelManager
  public KeyComboManager(
      AccessibilityService accessibilityService,
      Pipeline.FeedbackReturner pipeline,
      ActorState actorState,
      SelectorController selectorController,
      SpeechRateAndPitchActor speechRateAndPitchActor,
      ListMenuManager menuManager,
      FullScreenReadActor fullScreenReadActor,
      TalkBackAnalytics analytics,
      DirectionNavigationActor.StateReader stateReader,
      BatteryMonitor batteryMonitor,
      ScreenStateMonitor.State screenState,
      AccessibilityFocusMonitor accessibilityFocusMonitor,
      ProcessorPhoneticLetters processorPhoneticLetters,
      CollectionState collectionState,
      GestureShortcutMapping gestureShortcutMapping) {
    this(accessibilityService);
    this.analytics = analytics;
    hardwareKeyboardStatus =
        accessibilityService.getResources().getConfiguration().hardKeyboardHidden;
    this.pipeline = pipeline;
    this.accessibilityFocusMonitor = accessibilityFocusMonitor;
    keyComboMapper =
        new KeyComboMapper(
            accessibilityService,
            pipeline,
            actorState,
            selectorController,
            speechRateAndPitchActor,
            menuManager,
            fullScreenReadActor,
            stateReader,
            batteryMonitor,
            screenState,
            accessibilityFocusMonitor,
            processorPhoneticLetters,
            collectionState,
            gestureShortcutMapping);
    this.pipeline = pipeline;
    this.browseModeEnabled = false;
    this.scheduler = Executors.newScheduledThreadPool(1);
  }

  /** Runs a command by its value, as a gesture assigned to it would. */
  public interface CommandPerformer {
    void perform(String commandValue, @Nullable EventId eventId);
  }

  @Nullable private CommandPerformer commandPerformer;

  /** Sets what runs commands whose keyboard shortcuts are kept under {@link Commands} keys. */
  public void setCommandPerformer(@Nullable CommandPerformer commandPerformer) {
    this.commandPerformer = commandPerformer;
  }

  /** Performs a keyboard action for a gesture or another input. Returns whether it worked. */
  public boolean performKeyboardAction(
      TalkBackPhysicalKeyboardShortcut action, @Nullable EventId eventId) {
    if (keyComboMapper == null) {
      return false;
    }
    interrupt(action);
    return keyComboMapper.performKeyComboAction(action, eventId);
  }

  /** Terminates instances to prevent leakage. */
  public void shutdown() {
    if (hardwareKeyboardStatus == HARDKEYBOARDHIDDEN_NO) {
      analytics.onKeymapTypeUsed(keyComboModel);
      analytics.onModifierKeyUsed(keyComboModel.getTriggerModifier());
    }
    browseModeTurnedOffByUser = false;
    if (browseModeEnabled) {
      browseModeEnabled = false;
      // Record a Browse Mode log if Browse Mode is on when TalkBack is being turned off.
      recordBrowseModeStateWhenOff(BROWSE_MODE_TURNED_OFF_ON_SERVICE_STOP);
    }
    dismissUpdateModifierKeysDialog();
  }

  @Override
  public void onServiceStateChanged(int newState) {
    // Unfortunately, key events are lost when the TalkBackService becomes active. If a key-down
    // occurs that triggers TalkBack to resume, the corresponding key-up event will not be
    // sent, causing the partially-matched key history to become inconsistent.
    // The following method will cause the key history to be reset.
    setMatchKeyCombo(matchKeyCombo);
    arrowKeyNavKeyHandler.reset();
    // The up event of a held SandFly key may be lost the same way.
    sandFlyKeyHeld = false;
    serviceState = newState;
  }

  /**
   * Handles incoming key events. May intercept keys if the user seems to be performing a key combo.
   *
   * @param event The key event.
   * @return {@code true} if the key was intercepted.
   */
  @Override
  public boolean onKeyEvent(KeyEvent event, EventId eventId) {
    trackSandFlyKey(event);
    if (keyEventDelegate != null) {
      if (keyEventDelegate.onKeyEvent(event, eventId)) {
        arrowKeyNavKeyHandler.onKeyEventConsumedElsewhere(event);
        return true;
      }
    }

    // Arrow keys pressed with a held SandFly key are SandFly key shortcuts, not arrow key
    // navigation; the held key itself is a modifier to arrow key navigation.
    if (!isHeldSandFlyKey(event)
        && arrowKeyNavKeyHandler.onKeyEvent(
            event,
            /* canAct= */ serviceState == SERVICE_STATE_ACTIVE
                && matchKeyCombo
                && keyComboMapper != null
                && keyComboPassThroughState != KeyComboPassThroughState.ACTIVE
                && !sandFlyKeyHeld)) {
      return true;
    }

    if (!hasPartialMatch && !performedCombo && !matchKeyCombo) {
      return false;
    }

    return switch (event.getAction()) {
      case KeyEvent.ACTION_DOWN -> onKeyDown(event);
      case KeyEvent.ACTION_MULTIPLE -> hasPartialMatch;
      case KeyEvent.ACTION_UP -> onKeyUp(event);
      default -> false;
    };
  }

  @Override
  public boolean processWhenServiceSuspended() {
    return true;
  }

  @Override
  public int getEventTypes() {
    return AccessibilityEvent.TYPE_VIEW_FOCUSED
        | AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
  }

  /**
   * Handles incoming accessibility events. {@link KeyComboManager} handles input focus changed and
   * accessibility focus changed events only when the smart browse mode is enabled and the browse
   * mode is supported.
   *
   * @param event The accessibility event.
   * @param eventId The event ID.
   */
  @Override
  public void onAccessibilityEvent(AccessibilityEvent event, EventId eventId) {
    if (isBrowseModeSupported()
        && event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
      applyRememberedBrowseMode(event);
    }
    if (!KeyComboManagerHelper.isSmartBrowseModeEnabled(context) || !isBrowseModeSupported()) {
      LogUtils.d(TAG, "Smart browse mode is not enabled, or browse mode is not supported");
      return;
    }

    switch (event.getEventType()) {
      case AccessibilityEvent.TYPE_VIEW_FOCUSED ->
          // Handle input focus changed event
          handleViewInputFocusedEvent(event, eventId);
      case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED ->
          // Handle accessibility focus changed event
          handleViewAccessibilityFocusedEvent(event, eventId);
      default -> {}
    }
  }

  /**
   * Sets delegate for key events. If it's set, it can listen and consume key events before
   * KeyComboManager does. Sets null to remove current one.
   */
  public void setKeyEventDelegate(ServiceKeyEventListener delegate) {
    keyEventDelegate = delegate;
  }

  /**
   * Returns keymap by reading preference.
   *
   * @return key map. Returns default key map as default.
   */
  public String getKeymap() {
    return sharedPreferences.getString(
        context.getString(R.string.pref_select_keymap_key), context.getString(KEYMAP_DEFAULT));
  }

  /** Refreshes key combo model after key map changes. */
  public void refreshKeyComboModel() {
    keyComboModel = createKeyComboModel();
    sandFlyKeyHeld = false;
  }

  /** Returns key combo model. */
  public KeyComboModel getKeyComboModel() {
    return keyComboModel;
  }

  /** Set whether to process keycombo */
  public void setMatchKeyCombo(boolean value) {
    matchKeyCombo = value;
  }

  public void setCaptureTrainingCombos(boolean value) {
    captureTrainingCombos = value;
  }

  /** Returns user friendly string representations of key combo */
  public String getKeyComboStringRepresentation(KeyCombo keyCombo) {
    if (keyCombo.getKeyComboCode() == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED) {
      return context.getString(R.string.keycombo_unassigned);
    }
    // Browse mode's toggle is written as the SandFly key's Meta key with no key after it.
    @Nullable SandFlyKey sandFlyKey = getSandFlyKey();
    if (sandFlyKey != null
        && keyCombo.isTriggerModifierUsed()
        && keyCombo.getPrefixKeyCode() == KeyEvent.KEYCODE_META_RIGHT
        && keyCombo.getKeyCode() == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED) {
      return context.getString(
          R.string.sandfly_keyboard_tutorial_key_twice, context.getString(sandFlyKey.getNameRes()));
    }

    int triggerModifier = keyComboModel.getTriggerModifier();
    boolean sequenceKeyCombo =
        isSequenceKeyInfraSupported()
            && (keyCombo.getPrefixKeyCode() != KeyComboModel.NO_PREFIX_KEY_CODE);

    StringBuilder assignedShortcut = new StringBuilder();
    // Append the SandFly key if key combo code contains it.
    if (keyCombo.isTriggerModifierUsed()) {
      // The SandFly key looked up above.
      if (sandFlyKey != null) {
        assignedShortcut.append(context.getString(sandFlyKey.getNameRes()));
      } else {
        appendModifiers(triggerModifier, assignedShortcut);
      }
    }
    // Append modifier except trigger modifier.
    appendModifiers(keyCombo.getModifiers(), assignedShortcut);
    // Append prefix key code if it's in sequenced key mode.
    if (sequenceKeyCombo) {
      appendKey(keyCombo.getPrefixKeyCode(), assignedShortcut);
    }
    // Append key code.
    appendKey(keyCombo.getKeyCode(), assignedShortcut);

    return assignedShortcut.toString();
  }

  /** Notifies configuration changed. */
  public void onConfigurationChanged(Configuration newConfig) {
    if (newConfig.hardKeyboardHidden != hardwareKeyboardStatus) {
      hardwareKeyboardStatus = newConfig.hardKeyboardHidden;
      // A keyboard that went away can't send the up event of a held SandFly key.
      sandFlyKeyHeld = false;
      if (serviceState == SERVICE_STATE_ACTIVE) {
        if (hardwareKeyboardStatus == KEYBOARDHIDDEN_YES) {
          analytics.onKeymapTypeUsed(keyComboModel);
          analytics.onModifierKeyUsed(keyComboModel.getTriggerModifier());
        }
      }
    }
  }

  /** Returns true if browse mode is supported and it's using the new key combo model. */
  public boolean isBrowseModeSupported() {
    return FeatureFlagReader.enableBrowseMode(context) && keyComboModel instanceof NewKeyComboModel;
  }

  /**
   * Returns true if the sequence key infra is supported.
   *
   * <p>The sequence key infra is supported if the feature flag is enabled and the keymap uses
   * shortcuts of two keys one after the other.
   */
  public boolean isSequenceKeyInfraSupported() {
    return FeatureFlagReader.enableSequenceKeyInfra(context)
        && keyComboModel instanceof NewKeyComboModel model
        && model.usesSequences();
  }

  /** Returns true if the given key code is a meta key. */
  public boolean isMetaKey(int keyCode) {
    return keyCode == KeyEvent.KEYCODE_META_LEFT || keyCode == KeyEvent.KEYCODE_META_RIGHT;
  }

  /** Returns the user facing name of the given modifier. */
  public String getTriggerKeyUserFacingName(int modifier) {
    @Nullable SandFlyKey sandFlyKey = getSandFlyKey();
    if (sandFlyKey != null && sandFlyKey.getModifiers() == modifier) {
      return context.getString(sandFlyKey.getNameRes());
    }
    return switch (modifier) {
      case KeyEvent.META_ALT_ON -> context.getString(R.string.keycombo_key_modifier_alt);
      case KeyEvent.META_META_ON -> context.getString(R.string.keycombo_key_modifier_meta);
      default -> "";
    };
  }

  /** Returns true if browse mode is supported and enabled. */
  public boolean isBrowseModeEnabled() {
    return isBrowseModeSupported() && browseModeEnabled;
  }

  /**
   * Sets whether browse mode is enabled, when the user turns it on or off. It creates an
   * interruptive speech feedback.
   */
  public void setBrowseModeEnabled(boolean enabled) {
    browseModeTurnedOffByUser = !enabled;
    setBrowseModeEnabledInternal(enabled, /* makeSpeechFeedbackInterruptive= */ true);
    TutorialHooks.onBrowseMode(isBrowseModeEnabled());
    if (focusedPackage != null && isBrowseModeSupported()) {
      BrowseModeApps.remember(context, focusedPackage, enabled);
    }
  }

  /**
   * Turns browse mode on or off, as the user asks with a gesture or a shortcut other than Meta
   * twice. Returns false when browse mode isn't available.
   */
  public boolean toggleBrowseModeByUser() {
    if (!isBrowseModeSupported()) {
      return false;
    }
    setBrowseModeEnabled(!isBrowseModeEnabled());
    if (!browseModeEnabled) {
      recordBrowseModeStateWhenOff(BROWSE_MODE_TURNED_OFF_BY_USER);
    }
    return true;
  }

  /** Turns arrow key navigation on or off. */
  public void toggleArrowKeyNav() {
    boolean on = !ArrowKeyNav.isOn(context);
    ArrowKeyNav.setOn(context, on);
    pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Feedback.speech(
            context.getString(on ? R.string.arrow_key_nav_on : R.string.arrow_key_nav_off),
            INTERRUPTIVE_SPEAK_OPTIONS));
  }

  /**
   * Runs the command saved as {@code command}, as a key or gesture assigned to it would, for
   * callers outside the keyboard such as the SandFly Pro agent.
   */
  public void performCommand(String command) {
    performCommandFromKeyboard(command, EVENT_ID_UNTRACKED);
  }

  /**
   * Runs the command saved as {@code command} for a key press: by its keyboard action when it has
   * one, otherwise as a gesture would run it.
   */
  private void performCommandFromKeyboard(String command, @Nullable EventId eventId) {
    @Nullable
    TalkBackPhysicalKeyboardShortcut action = Commands.keyboardActionFor(context, command);
    if (action != null) {
      performKeyboardAction(action, eventId);
    } else if (commandPerformer != null) {
      commandPerformer.perform(command, eventId);
    }
  }

  /** Whether SandFly focus is on the text field being edited. */
  private boolean isEditingTextWithAccessibilityFocus() {
    if (accessibilityFocusMonitor == null) {
      return false;
    }
    AccessibilityNodeInfoCompat inputFocus = accessibilityFocusMonitor.getInputFocus();
    return inputFocus != null
        && inputFocus.isFocused()
        && inputFocus.isEditable()
        && inputFocus.equals(
            accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false));
  }

  /** Returns how arrow keys are written, such as "Arrow Up + Arrow Down". */
  public String getArrowKeysStringRepresentation(long keys) {
    StringBuilder sb = new StringBuilder();
    appendModifiers(KeyCombo.extractModifiers(keys), sb);
    appendKey(KeyCombo.extractPrefixKeyCode(keys), sb);
    appendKey(KeyCombo.extractKeyCode(keys), sb);
    return sb.toString();
  }

  /**
   * When focus moves to another app, sets browse mode as the user last left it there, if
   * Remember browse mode in each app is on. In an app where the user hasn't turned it on or off,
   * browse mode is off, unless smart browse mode turns it on in web content. Focus in SandFly's
   * own windows, the keyboard or the system's windows leaves it as it is.
   */
  private void applyRememberedBrowseMode(AccessibilityEvent event) {
    CharSequence packageName = event.getPackageName();
    String app = packageName == null ? null : packageName.toString();
    if (app == null || app.equals(focusedPackage) || app.equals(context.getPackageName())) {
      return;
    }
    AccessibilityNodeInfoCompat source = AccessibilityNodeInfoUtils.toCompat(event.getSource());
    if (source == null
        || AccessibilityNodeInfoUtils.getWindowType(source)
            != AccessibilityWindowInfo.TYPE_APPLICATION) {
      return;
    }
    focusedPackage = app;
    if (!BrowseModeApps.isOn(context)) {
      return;
    }
    Boolean remembered = BrowseModeApps.remembered(context, app);
    LogUtils.i(TAG, "Browse mode remembered in %s: %s", app, remembered);
    browseModeTurnedOffByUser = Boolean.FALSE.equals(remembered);
    if (remembered == null
        && KeyComboManagerHelper.isSmartBrowseModeEnabled(context)
        && AccessibilityNodeInfoUtils.isSelfOrAncestorRoleWebView(source)) {
      // Smart browse mode turns it on for this web content.
      return;
    }
    setBrowseModeEnabledInternal(
        Boolean.TRUE.equals(remembered), /* makeSpeechFeedbackInterruptive= */ false);
  }

  /**
   * Sets whether browse mode is enabled.
   *
   * @param enabled Whether to enable or disable browse mode.
   * @param makeSpeechFeedbackInterruptive Whether to make the speech feedback interruptive.
   */
  private void setBrowseModeEnabledInternal(
      boolean enabled, boolean makeSpeechFeedbackInterruptive) {
    if (!isBrowseModeSupported() || browseModeEnabled == enabled) {
      return;
    }

    browseModeEnabled = enabled;
    String browseModeStateDescription;
    int browseModeSoundResId;
    if (browseModeEnabled) {
      browseModeStateDescription = context.getString(R.string.browse_mode_on_hint);
      browseModeSoundResId = R.raw.browse_mode_on_v4_2;
      // Start the timer to track the browse mode enabled time.
      browseModeEnabledTimeMills = SystemClock.uptimeMillis();
    } else {
      browseModeStateDescription = context.getString(R.string.browse_mode_off_hint);
      browseModeSoundResId = R.raw.browse_mode_off_v4_2;
    }
    if (isOnTrainingPage()) {
      // Specify the trigger modifier used in the hint.
      SpannableStringBuilder browseModeHint = new SpannableStringBuilder();
      int triggerModifier = keyComboModel.getTriggerModifier();
      StringBuilderUtils.appendWithSeparator(
          browseModeHint,
          context.getString(
              R.string.browse_mode_double_trigger_modifier,
              getTriggerKeyUserFacingName(triggerModifier)),
          browseModeStateDescription);
      // If it's on the training page, make the speech feedback announce interruptive.
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.part()
              .sound(browseModeSoundResId)
              .speech(browseModeHint.toString(), INTERRUPTIVE_SPEAK_OPTIONS));
    } else {
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.part()
              .sound(browseModeSoundResId)
              .speech(
                  browseModeStateDescription,
                  makeSpeechFeedbackInterruptive
                      ? INTERRUPTIVE_SPEAK_OPTIONS
                      : UNINTERRUPTIBLE_BY_NEW_SPEECH_SPEAK_OPTIONS));
    }
  }

  /**
   * Returns the set of commands that are only available in browse mode. The set stores their key
   * strings.
   */
  /**
   * Returns the commands whose shortcuts work only in browse mode: the ones browse mode has, and
   * ones the keymap gives keys without the SandFly key, such as Orca's Alt + Shift + H.
   */
  public ImmutableSet<String> getBrowseModeOnlyCommands() {
    ImmutableSet.Builder<String> commands = ImmutableSet.<String>builder().addAll(browseModeOnlyCommands);
    if (keyComboModel != null) {
      for (String key : keyComboModel.getKeyComboMap().keySet()) {
        KeyCombo keyCombo = keyComboModel.getDefaultKeyCombo(key);
        if (keyCombo.getKeyComboCode() != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED
            && !keyCombo.isTriggerModifierUsed()) {
          commands.add(key);
        }
      }
    }
    return commands.build();
  }


  /**
   * Saves the SandFly keymap for new users and for users of the classic keymap, and keeps the
   * removed Default keymap as a custom keymap for the users who chose it.
   */
  private void initializeDefaultPreferenceValues() {
    String key = context.getString(R.string.pref_select_keymap_key);
    String keymapValue = sharedPreferences.getString(key, null);
    if (DEFAULT_KEYMAP_VALUE.equals(keymapValue)) {
      keymapValue = keepDefaultKeymapAsCustomKeymap().getValue();
      sharedPreferences.edit().putString(key, keymapValue).apply();
    }
    Keymap keymap = Keymap.of(context, keymapValue);
    if (CustomKeymaps.of(context, keymapValue) == null) {
      String value = context.getString(keymap.getValueRes());
      if (!value.equals(keymapValue)) {
        sharedPreferences.edit().putString(key, value).apply();
      }
    }
  }

  private ImmutableSet<String> initializeBrowseModeOnlyCommands() {
    // TODO: b/380361706 - Move all properties of a command into a single structure/object instead
    // of separate sets.
    ImmutableSet.Builder<String> commands = ImmutableSet.builder();
    commands.addAll(
        ImmutableSet.of(
            // Reading granularity commands
            context.getString(R.string.keycombo_shortcut_navigate_next_character),
            context.getString(R.string.keycombo_shortcut_navigate_previous_character),
            context.getString(R.string.keycombo_shortcut_navigate_next_word),
            context.getString(R.string.keycombo_shortcut_navigate_previous_word),
            context.getString(R.string.keycombo_shortcut_navigate_next_line),
            context.getString(R.string.keycombo_shortcut_navigate_previous_line),
            context.getString(R.string.keycombo_shortcut_navigate_next_paragraph),
            context.getString(R.string.keycombo_shortcut_navigate_previous_paragraph),
            // Jump commands
            context.getString(R.string.keycombo_shortcut_navigate_next_aria_landmark),
            context.getString(R.string.keycombo_shortcut_navigate_previous_aria_landmark),
            context.getString(R.string.keycombo_shortcut_navigate_next_button),
            context.getString(R.string.keycombo_shortcut_navigate_previous_button),
            context.getString(R.string.keycombo_shortcut_navigate_next_checkbox),
            context.getString(R.string.keycombo_shortcut_navigate_previous_checkbox),
            context.getString(R.string.keycombo_shortcut_navigate_next_combobox),
            context.getString(R.string.keycombo_shortcut_navigate_previous_combobox),
            context.getString(R.string.keycombo_shortcut_navigate_next_control),
            context.getString(R.string.keycombo_shortcut_navigate_previous_control),
            context.getString(R.string.keycombo_shortcut_navigate_next_edit_field),
            context.getString(R.string.keycombo_shortcut_navigate_previous_edit_field),
            context.getString(R.string.keycombo_shortcut_navigate_next_graphic),
            context.getString(R.string.keycombo_shortcut_navigate_previous_graphic),
            context.getString(R.string.keycombo_shortcut_navigate_next_heading),
            context.getString(R.string.keycombo_shortcut_navigate_previous_heading),
            context.getString(R.string.keycombo_shortcut_navigate_next_heading_1),
            context.getString(R.string.keycombo_shortcut_navigate_previous_heading_1),
            context.getString(R.string.keycombo_shortcut_navigate_next_heading_2),
            context.getString(R.string.keycombo_shortcut_navigate_previous_heading_2),
            context.getString(R.string.keycombo_shortcut_navigate_next_heading_3),
            context.getString(R.string.keycombo_shortcut_navigate_previous_heading_3),
            context.getString(R.string.keycombo_shortcut_navigate_next_heading_4),
            context.getString(R.string.keycombo_shortcut_navigate_previous_heading_4),
            context.getString(R.string.keycombo_shortcut_navigate_next_heading_5),
            context.getString(R.string.keycombo_shortcut_navigate_previous_heading_5),
            context.getString(R.string.keycombo_shortcut_navigate_next_heading_6),
            context.getString(R.string.keycombo_shortcut_navigate_previous_heading_6),
            context.getString(R.string.keycombo_shortcut_navigate_next_link),
            context.getString(R.string.keycombo_shortcut_navigate_previous_link),
            context.getString(R.string.keycombo_shortcut_navigate_next_list),
            context.getString(R.string.keycombo_shortcut_navigate_previous_list),
            context.getString(R.string.keycombo_shortcut_navigate_next_list_item),
            context.getString(R.string.keycombo_shortcut_navigate_previous_list_item),
            context.getString(R.string.keycombo_shortcut_navigate_next_radio),
            context.getString(R.string.keycombo_shortcut_navigate_previous_radio),
            context.getString(R.string.keycombo_shortcut_navigate_next_table),
            context.getString(R.string.keycombo_shortcut_navigate_previous_table),
            context.getString(R.string.keycombo_shortcut_navigate_next_unvisited_link),
            context.getString(R.string.keycombo_shortcut_navigate_previous_unvisited_link),
            context.getString(R.string.keycombo_shortcut_navigate_next_visited_link),
            context.getString(R.string.keycombo_shortcut_navigate_previous_visited_link),
            // Navigation commands
            context.getString(R.string.keycombo_shortcut_navigate_next_column),
            context.getString(R.string.keycombo_shortcut_navigate_previous_column),
            context.getString(R.string.keycombo_shortcut_navigate_next_column_bounds),
            context.getString(R.string.keycombo_shortcut_navigate_previous_column_bounds),
            context.getString(R.string.keycombo_shortcut_navigate_next_row),
            context.getString(R.string.keycombo_shortcut_navigate_previous_row),
            context.getString(R.string.keycombo_shortcut_navigate_next_row_bounds),
            context.getString(R.string.keycombo_shortcut_navigate_previous_row_bounds),
            context.getString(R.string.keycombo_shortcut_navigate_next_table_bounds),
            context.getString(R.string.keycombo_shortcut_navigate_previous_table_bounds)));
    // Table header reading commands.
    if (FeatureFlagReader.enableAnnounceTableHeader(context)) {
      commands.addAll(
          ImmutableSet.of(
              context.getString(R.string.keycombo_shortcut_other_announce_column_header),
              context.getString(R.string.keycombo_shortcut_other_announce_row_header)));
    }
    // Reading the address works outside browse mode too, as JAWS's Insert + A does.
    return commands.build();
  }

  /** Creates key combo model by keymap key and return it. */
  private KeyComboModel createKeyComboModel() {
    @Nullable CustomKeymap customKeymap = CustomKeymaps.of(context, getKeymap());
    if (customKeymap != null) {
      return new NewKeyComboModel(context, customKeymap);
    }
    return new NewKeyComboModel(context, Keymap.of(context, getKeymap()));
  }

  /** Appends modifier. */
  private void appendModifiers(int modifier, StringBuilder sb) {
    appendModifier(
        modifier, KeyEvent.META_ALT_ON, context.getString(R.string.keycombo_key_modifier_alt), sb);
    appendModifier(
        modifier,
        KeyEvent.META_SHIFT_ON,
        context.getString(R.string.keycombo_key_modifier_shift),
        sb);
    appendModifier(
        modifier,
        KeyEvent.META_CTRL_ON,
        context.getString(R.string.keycombo_key_modifier_ctrl),
        sb);
    appendModifier(
        modifier,
        KeyEvent.META_META_ON,
        context.getString(R.string.keycombo_key_modifier_meta),
        sb);
  }

  /** Appends string representation of target modifier if modifier contains it. */
  private void appendModifier(
      int modifier, int targetModifier, String stringRepresentation, StringBuilder sb) {
    if ((modifier & targetModifier) != 0) {
      appendPlusSignIfNotEmpty(sb);
      sb.append(stringRepresentation);
    }
  }

  /** Appends string representation of target key code. */
  private void appendKey(int keyCode, StringBuilder sb) {
    if (keyCode <= 0 || KeyEvent.isModifierKey(keyCode)) {
      return;
    }
    // A held SandFly key is named with the SandFly key.
    @Nullable SandFlyKey sandFlyKey = getSandFlyKey();
    if (sandFlyKey != null && sandFlyKey.isHeldKey() && keyCode == sandFlyKey.getKeyCode()) {
      return;
    }

    appendPlusSignIfNotEmpty(sb);
    switch (keyCode) {
      case KeyEvent.KEYCODE_DPAD_RIGHT ->
          sb.append(context.getString(R.string.keycombo_key_arrow_right));
      case KeyEvent.KEYCODE_DPAD_LEFT ->
          sb.append(context.getString(R.string.keycombo_key_arrow_left));
      case KeyEvent.KEYCODE_DPAD_UP -> sb.append(context.getString(R.string.keycombo_key_arrow_up));
      case KeyEvent.KEYCODE_DPAD_DOWN ->
          sb.append(context.getString(R.string.keycombo_key_arrow_down));
      case KeyEvent.KEYCODE_DEL -> sb.append(context.getString(R.string.keycombo_key_backspace));
      default -> {
        String keyCodeString = KeyEvent.keyCodeToString(keyCode);
        if (keyCodeString != null) {
          String keyCodeNoPrefix;
          if (keyCodeString.startsWith(KEYCODE_PREFIX)) {
            keyCodeNoPrefix = keyCodeString.substring(KEYCODE_PREFIX.length());
          } else {
            keyCodeNoPrefix = keyCodeString;
          }
          sb.append(keyCodeNoPrefix.replace('_', ' '));
        }
      }
    }
  }

  private void appendPlusSignIfNotEmpty(StringBuilder sb) {
    if (sb.length() > 0) {
      sb.append(CONCATENATION_STR);
    }
  }

  /** Returns true if the active window is training activity. */
  private boolean isOnTrainingPage() {
    // Compares resource ID with all child nodes on the active window if navigate-up arrow is
    // shown because the first child node is the navigate-up arrow which is no resource ID.
    AccessibilityNodeInfoCompat root = accessibilityFocusMonitor.getAccessibilityFocus(false);
    if (root == null) {
      return false;
    }

    return WindowUtils.rootChildMatchesResId(context, root.unwrap(), TrainingActivity.ROOT_RES_ID)
        && captureTrainingCombos;
  }

  private boolean onKeyDown(KeyEvent event) {
    if (serviceState != SERVICE_STATE_ACTIVE) {
      return false;
    }

    currentKeysDown.add(event.getKeyCode());
    currentKeyComboTime = event.getDownTime();

    if (isKeyComboPassThroughFeatureAvailable()) {
      // If no modifier is pressed, we know that the user for sure is not performing a shortcut and
      // passthrough should be inactive. So clear the set of pressed keys in case dangling key downs
      // didn't get a key up to ensure the state is clean.
      boolean modifierKeyPressed = isAnyModifierKeyPressed(event);
      if (!modifierKeyPressed) {
        keyComboPassThroughPressedKeys.clear();
      }

      // When KeyComboPassThroughState is enabled key downs are not consumed by TalkBack.
      if (keyComboPassThroughState == KeyComboPassThroughState.ACTIVE) {
        passedKeys.addAll(currentKeysDown);
        keyComboPassThroughPressedKeys.add(event.getKeyCode());
        return false;
      }
    }

    // A held SandFly key is SandFly's alone. Pressed twice, it turns browse mode on or off, as
    // Meta does.
    @Nullable SandFlyKey sandFlyKey = getSandFlyKey();
    if (sandFlyKey != null && sandFlyKey.isKey(event)) {
      if (event.getRepeatCount() == 0) {
        currentKeyCombo =
            new KeyCombo(
                KeyComboModel.NO_MODIFIER,
                KeyComboModel.NO_PREFIX_KEY_CODE,
                event.getKeyCode(),
                /* triggerModifierUsed= */ true);
        if (isBrowseModeSupported()
            && isBrowseModeToggleAssigned()
            && isEqualToLastKeyComboAndDurationLessThanThreshold()) {
          setBrowseModeEnabled(!browseModeEnabled);
          if (!browseModeEnabled) {
            recordBrowseModeStateWhenOff(BROWSE_MODE_TURNED_OFF_BY_USER);
          }
        }
      }
      hasPartialMatch = true;
      return true;
    }

    // Android's own shortcuts with the Meta key, which the tutorial can take while it teaches them.
    if (event.getRepeatCount() == 0
        && (event.getModifiers() & KEY_EVENT_MODIFIER_MASK) == KeyEvent.META_META_ON
        && TutorialHooks.SYSTEM_KEY_CODES.contains(event.getKeyCode())
        && TutorialHooks.onSystemKeys(event.getKeyCode())) {
      performedCombo = true;
      return true;
    }

    // Check modifier.
    int triggerModifier = keyComboModel.getTriggerModifier();
    int pressedKeyCode = event.getKeyCode();
    event = convertMetaKeyCombo(triggerModifier, event);
    boolean hasModifier = triggerModifier != KeyComboModel.NO_MODIFIER;
    int modifiers = modifiersOf(event);
    boolean pressedTriggerModifier =
        hasModifier && ((triggerModifier & modifiers) == triggerModifier);
    // Only a pressed SandFly key leaves the modifiers, so Ctrl alone stays Ctrl where the SandFly key
    // is Ctrl + Alt.
    int otherModifiers = modifiers & ~(pressedTriggerModifier ? triggerModifier : 0);
    boolean startedSequenceKeyCombo =
        isSequenceKeyInfraSupported()
            && previousKeyCombo.getPrefixKeyCode() != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED;
    boolean enabledBrowseMode = isBrowseModeEnabled();
    int scriptModifiers = otherModifiers;
    KeyCombo scriptKeyCombo =
        new KeyCombo(
            scriptModifiers,
            KeyComboModel.NO_PREFIX_KEY_CODE,
            getConvertedKeyCode(event),
            pressedTriggerModifier);
    if (Scripts.onKeys(
        scriptModifiers,
        getConvertedKeyCode(event),
        pressedTriggerModifier,
        /* repeated= */ event.getRepeatCount() > 0,
        () -> matchAndPerformKeyCombo(scriptKeyCombo, triggerModifier))) {
      scriptKeys.add(pressedKeyCode);
      performedCombo = true;
      hasPartialMatch = false;
      return true;
    }
    // Trigger modifier needs to be pressed for TalkBack to handle the key event unless:
    // * one of the prefix key combos in the sequenced key combos was pressed, or
    // * Browse mode is on.
    boolean triggerModifierNotPressedWhenNeeded =
        hasModifier && !pressedTriggerModifier && !startedSequenceKeyCombo && !enabledBrowseMode;
    if (triggerModifierNotPressedWhenNeeded) {
      // A custom keymap's shortcuts without the SandFly key work anywhere. Modifier keys pressed on
      // their own always go to the app.
      if (isCustomKeymap() && !KeyEvent.isModifierKey(event.getKeyCode())) {
        KeyCombo plainKeyCombo =
            new KeyCombo(
                otherModifiers,
                KeyComboModel.NO_PREFIX_KEY_CODE,
                getConvertedKeyCode(event),
                /* triggerModifierUsed= */ false);
        currentKeyCombo = plainKeyCombo;
        performedCombo = matchAndPerformKeyCombo(plainKeyCombo, triggerModifier);
        hasPartialMatch = false;
        if (performedCombo) {
          return true;
        }
      }
      // TalkBack may not handle the key event in this case. Add the current keys to the passed
      // keys set to avoid the key event being handled by TalkBack when not needed.
      passedKeys.addAll(currentKeysDown);
      return false;
    }

    if (startedSequenceKeyCombo) {
      // If the prefix key code has been stored in `previousKeyComboCode` when entering the
      // sequence key mode, check whether the time limit has been reached.
      if (currentKeyComboTime - previousKeyComboTime > SEQUENCE_KEY_MODE_TIMEOUT.toMillis()) {
        // The time limit has been reached. Reset the previous key combo code and continue
        // handling the key event without the sequence key mode.
        previousKeyCombo = new KeyCombo();
      } else {
        // The prefix key code has been stored in `previousKeyComboCode` when entering the
        // sequence key mode. Create and process a sequenced key combo.
        KeyCombo sequencedKeyComboCode =
            new KeyCombo(
                previousKeyCombo.getModifiers(),
                previousKeyCombo.getPrefixKeyCode(),
                getConvertedKeyCode(event),
                previousKeyCombo.isTriggerModifierUsed());
        performedCombo = matchAndPerformKeyCombo(sequencedKeyComboCode, triggerModifier);
        hasPartialMatch = false;
        // No matter whether the key combo is performed or not, we should return true to consume
        // the key event in the sequenced key combo mode.
        previousKeyCombo = new KeyCombo();
        return true;
      }
    }

    currentKeyCombo =
        new KeyCombo(
            otherModifiers,
            KeyComboModel.NO_PREFIX_KEY_CODE,
            getConvertedKeyCode(event),
            pressedTriggerModifier);
    // If the current set of keys is a partial combo, consume the event.
    hasPartialMatch = false;

    performedCombo = matchAndPerformKeyCombo(currentKeyCombo, triggerModifier);
    if (performedCombo) {
      return true;
    }

    if (previousKeyCombo.equals(currentKeyCombo)
        && currentKeyComboTime - previousKeyComboTime < TIME_TO_DETECT_DOUBLE_TAP.toMillis()
        && currentKeyCombo.getModifiers() == KeyComboModel.NO_MODIFIER
        && currentKeyCombo.getPrefixKeyCode() == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED
        && sandFlyKey != null
        && sandFlyKey.getCanBePressedTwice()
        && sandFlyKey.hasKey(currentKeyCombo.getKeyCode())) {
      if (isBrowseModeSupported() && isBrowseModeToggleAssigned()) {
        // If `isBrowseModeSupported()`, cancel the executor for opening the search app and toggle
        // Browse Mode if the user has pressed search key (meta key) twice within
        // `TIME_TO_DETECT_DOUBLE_TAP`. This cancellation needs to be done in `onKeyDown()` to
        // handle the case where the user double-presses and holds the search key; in this case, the
        // search app shouldn't be opened and Browse Mode should be toggled on/off.
        // When the user explicitly toggles browse mode, make the speech feedback interruptive.
        cancelOpenSearchAppExecutor();
        setBrowseModeEnabled(!browseModeEnabled);
        // Record a Browse Mode log when it's toggled off by the user.
        if (!browseModeEnabled) {
          recordBrowseModeStateWhenOff(BROWSE_MODE_TURNED_OFF_BY_USER);
        }
      } else if (isMetaKey(currentKeyCombo.getKeyCode())) {
        // If not `isBrowseModeSupported()`, do not handle key event if user has pressed search key
        // (meta key) twice to open search app. Reset `currentKeyCombo` to not open search app again
        // with following search key event.
        currentKeyCombo = new KeyCombo();
        passedKeys.addAll(currentKeysDown);
        return false;
      }
    }

    if (!hasPartialMatch) {
      passedKeys.addAll(currentKeysDown);
    }
    return hasPartialMatch;
  }

  private void announcePerformedShortcutAndAction(
      TalkBackPhysicalKeyboardShortcut action, KeyCombo keyCombo) {
    SpannableStringBuilder gestureAndAction = new SpannableStringBuilder();
    StringBuilderUtils.appendWithSeparator(
        gestureAndAction,
        getKeyComboStringRepresentation(keyCombo),
        action.getDescription(context.getResources()));
    pipeline.returnFeedback(
        /* eventId= */ null,
        Feedback.speech(
            gestureAndAction.toString(),
            SpeakOptions.create().setQueueMode(QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH)));
  }

  private boolean matchAndPerformKeyCombo(KeyCombo keyCombo, int triggerModifier) {
    // Check if it's a sequenced key combo.
    if (isSequenceKeyInfraSupported()) {
      if (keyCombo.isSequenceKeyPrefixCombo()) {
        // If the prefix key combo is matched, store the key code in the prefix key code field of
        // `previousKeyCombo` to indicate that the sequence key mode is entered.
        previousKeyCombo =
            new KeyCombo(
                keyCombo.getModifiers(),
                keyCombo.getKeyCode(),
                KeyComboModel.KEY_COMBO_CODE_UNASSIGNED,
                keyCombo.isTriggerModifierUsed());
        // Reset the previous key combo time to the current key combo time to handle the case that
        // the user doesn't press a key after the prefix key combo within the time limit.
        previousKeyComboTime = currentKeyComboTime;
        hasPartialMatch = true;
        return true;
      }
    }

    // The keyboard shortcuts of the app shown come before the keymap's.
    for (kotlin.Pair<KeyCombo, String> appKey : AppGestures.keys()) {
      final int match = keyCombo.matchWith(triggerModifier, appKey.getFirst());
      if (match == EXACT_MATCH) {
        String commandValue = appKey.getSecond();
        // Unassigned in the app: the keys go to the app.
        if (commandPerformer == null
            || context.getString(R.string.shortcut_value_unassigned).equals(commandValue)) {
          return false;
        }
        performCommandFromKeyboard(
            commandValue,
            Performance.getInstance()
                .onKeyComboEventReceived(
                    TalkBackPhysicalKeyboardShortcut.ACTION_UNKNOWN.getKeyboardShortcutOrdinal()));
        performedCombo = true;
        return true;
      }
      if (match == PARTIAL_MATCH) {
        hasPartialMatch = true;
      }
    }

    for (Map.Entry<String, KeyCombo> entry : keyComboModel.getKeyComboMap().entrySet()) {
      final String key = entry.getKey();
      final KeyCombo targetKeyCombo = entry.getValue();
      final int match = keyCombo.matchWith(triggerModifier, targetKeyCombo);
      if (isBrowseModeSupported() && !browseModeEnabled && browseModeOnlyCommands.contains(key)) {
        continue;
      }
      if (match == EXACT_MATCH) {
        @Nullable String commandValue = Commands.valueOfShortcutKey(key);
        if (commandValue != null && commandPerformer == null) {
          continue;
        }
        if (TutorialHooks.onKeys(
            getKeyComboStringRepresentation(targetKeyCombo),
            commandValue != null
                ? commandValue
                : Commands.valueOfKeyboardAction(
                    context,
                    TalkBackPhysicalKeyboardShortcut.getActionFromKey(
                        context.getResources(), key)))) {
          performedCombo = true;
          return true;
        }
        if (commandValue != null) {
          // A command without a keyboard action of its own runs as a gesture would run it.
          if (commandPerformer == null) {
            continue;
          }
          performCommandFromKeyboard(
              commandValue,
              Performance.getInstance()
                  .onKeyComboEventReceived(
                      TalkBackPhysicalKeyboardShortcut.ACTION_UNKNOWN
                          .getKeyboardShortcutOrdinal()));
          performedCombo = true;
          return true;
        }
        TalkBackPhysicalKeyboardShortcut action =
            TalkBackPhysicalKeyboardShortcut.getActionFromKey(context.getResources(), key);
        EventId eventId =
            Performance.getInstance().onKeyComboEventReceived(action.getKeyboardShortcutOrdinal());

        // Checks interrupt events if matches key combos. To prevent interrupting actions generated
        // by key combos, we should send interrupt events before performing key combos.
        interrupt(action);

        if (isKeyComboPassThroughFeatureAvailable()) {
          // If the action is the assigned KeyComboPassThroughOnce action shortcut, set the
          // state to pending so KeyComboPassThroughOnce is enabled on the next key up.
          if (action == TalkBackPhysicalKeyboardShortcut.TOGGLE_KEY_COMBO_PASS_THROUGH_ONCE) {
            setKeyComboPassThroughState(
                KeyComboPassThroughState.PENDING_PASS_THROUGH_SHORTCUT_RELEASE);
          }
        }

        // Checks if the key combo is handled by training, if so, return true to consume the key
        // event.
        if (FeatureFlagReader.enableKeyboardLearnModePage(context)
            && isOnTrainingPage()
            && !isBasicLinearNavigation(key)) {
          announcePerformedShortcutAndAction(action, keyCombo);
          return true;
        }

        performedCombo = keyComboMapper.performKeyComboAction(action, eventId);
        analytics.onKeyboardShortcutUsed(
            action, keyComboModel.getTriggerModifier(), keyCombo.getKeyComboCode());
        return true;
      }

      if (match == PARTIAL_MATCH) {
        hasPartialMatch = true;
      }
    }

    return false;
  }

  private boolean isBasicLinearNavigation(String key) {
    return key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_default))
        || key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_default));
  }

  /**
   * Android converts META_META_ON + KEYCODE_ENTER and META_META_ON + KEYCODE_DEL to KEYCODE_HOME
   * and KEYCODE_BACK without META_META_ON. We recover it to the original event and add META_META_ON
   * to this key event to satisfy trigger modifier condition.
   */
  private KeyEvent convertMetaKeyCombo(int triggerModifier, KeyEvent keyEvent) {
    int keyCode = keyEvent.getKeyCode();
    if (triggerModifier == KeyEvent.META_META_ON
        && (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_BACK)
        && (currentKeysDown.contains(KeyEvent.KEYCODE_META_LEFT)
            || currentKeysDown.contains(KeyEvent.KEYCODE_META_RIGHT))) {

      // The converted KeyEvent has no value of metaState, we should add META_META_ON to satisfy
      // trigger modifier condition.
      int metaState = keyEvent.getMetaState() | KeyEvent.META_META_ON;
      switch (keyCode) {
        case KeyEvent.KEYCODE_HOME -> keyCode = KeyEvent.KEYCODE_ENTER;
        case KeyEvent.KEYCODE_BACK -> keyCode = KeyEvent.KEYCODE_DEL;
        default -> {}
      }
      return new KeyEvent(
          keyEvent.getDownTime(),
          keyEvent.getEventTime(),
          keyEvent.getAction(),
          keyCode,
          keyEvent.getRepeatCount(),
          metaState);
    }
    return keyEvent;
  }

  private void setKeyComboPassThroughState(KeyComboPassThroughState nextState) {
    if (keyComboPassThroughState == nextState) {
      return;
    }

    boolean activatingPassThrough = nextState == KeyComboPassThroughState.ACTIVE;
    boolean deactivatingPassThrough = keyComboPassThroughState == KeyComboPassThroughState.ACTIVE;
    if (activatingPassThrough) {
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.part()
              .sound(R.raw.chime_up)
              .speech(
                  context.getString(R.string.hint_pass_through_mode_on_for_keyboard_shortcuts),
                  INTERRUPTIVE_SPEAK_OPTIONS));
    } else if (deactivatingPassThrough) {
      pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.chime_down));
    }
    keyComboPassThroughState = nextState;
  }

  /** Returns the SandFly key of the keymap. */
  public @Nullable SandFlyKey getSandFlyKey() {
    return keyComboModel instanceof NewKeyComboModel model ? model.getSandFlyKey() : null;
  }

  /**
   * Keeps the removed Default keymap, with its shortcuts, changed or not, as a custom keymap named
   * Custom, and its modifier, Alt or Meta, as the SandFly key. Returns the custom keymap.
   */
  private CustomKeymap keepDefaultKeymapAsCustomKeymap() {
    SandFlyKey sandFlyKey =
        "trigger_modifier_meta"
                .equals(sharedPreferences.getString("pref_default_keymap_trigger_modifier", null))
            ? SandFlyKey.META
            : SandFlyKey.ALT;
    KeyComboPersister from = new KeyComboPersister(context, "default_key_combo_model");
    Map<String, KeyCombo> shortcuts = new HashMap<>();
    for (String key : from.keysStartingWith("")) {
      shortcuts.put(key, from.getKeyCombo(key));
    }
    return CustomKeymaps.create(
        context,
        context.getString(R.string.keycombo_custom_keymap_default_name),
        /* usesSequences= */ false,
        sandFlyKey,
        shortcuts);
  }

  /** Returns whether the keymap in use is one the user made. */
  public boolean isCustomKeymap() {
    return keyComboModel instanceof NewKeyComboModel model && model.getCustomKeymap() != null;
  }

  /** Returns whether the keymap turns browse mode on and off with the SandFly key pressed twice. */
  private boolean isBrowseModeToggleAssigned() {
    return keyComboModel
            .getKeyComboForKey(context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode))
            .getKeyComboCode()
        != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED;
  }

  /** Returns whether [event] presses or releases the SandFly key where it's a held key. */
  private boolean isHeldSandFlyKey(KeyEvent event) {
    @Nullable SandFlyKey sandFlyKey = getSandFlyKey();
    return sandFlyKey != null
        && sandFlyKey.isHeldKey()
        && event.getKeyCode() == sandFlyKey.getKeyCode();
  }

  /**
   * Notes whether a held SandFly key, such as Insert, is down. Callers that see key events before
   * this manager, such as the pause controller, call it too.
   */
  public void trackSandFlyKey(KeyEvent event) {
    @Nullable SandFlyKey sandFlyKey = getSandFlyKey();
    if (sandFlyKey == null || event.getKeyCode() != sandFlyKey.getKeyCode()) {
      return;
    }
    if (event.getAction() == KeyEvent.ACTION_UP) {
      sandFlyKeyHeld = false;
    } else if (event.getAction() == KeyEvent.ACTION_DOWN && sandFlyKey.isKey(event)) {
      sandFlyKeyHeld = true;
    }
  }

  /** Returns the modifiers of [event], with the SandFly key's bit while a held SandFly key is down. */
  public int modifiersOf(KeyEvent event) {
    int modifiers = event.getModifiers() & KEY_EVENT_MODIFIER_MASK;
    return sandFlyKeyHeld ? modifiers | KeyComboModel.HELD_KEY_MODIFIER : modifiers;
  }

  /** Returns whether the SandFly key is down while [event] happens. */
  public boolean isSandFlyKeyPressed(KeyEvent event) {
    int triggerModifier = keyComboModel.getTriggerModifier();
    return triggerModifier != KeyComboModel.NO_MODIFIER
        && (modifiersOf(event) & triggerModifier) == triggerModifier;
  }

  private boolean isAnyModifierKeyPressed(KeyEvent event) {
    // Check if any of the modifiers, a held SandFly key among them, are set.
    return modifiersOf(event) != 0;
  }

  private boolean isKeyComboPassThroughFeatureAvailable() {
    return FeatureFlagReader.enableToggleKeyComboPassThroughOnce(context)
        && getKeyComboModel() instanceof NewKeyComboModel;
  }

  /**
   * Notifies the {@link KeyComboMapper} whether should interrupt or not by checking the action
   * enum.
   *
   * @param performedAction the action generating from key combos.
   */
  private void interrupt(TalkBackPhysicalKeyboardShortcut performedAction) {
    if (keyComboMapper != null) {
      keyComboMapper.interruptByKeyCombo(performedAction);
    }
  }

  private void openSearchAppAfterDelay() {
    scheduleToOpenSearchApp =
        scheduler.schedule(
            () -> {
              try {
                return pipeline.returnFeedback(
                    EVENT_ID_UNTRACKED,
                    Feedback.systemAction(
                        AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS));
              } catch (RuntimeException e) {
                LogUtils.e(TAG, "Error in openSearchAppAfterDelay(): " + e.getMessage());
                return false;
              }
            },
            // if the user doesn't press the search key again within this time, open the search app.
            TIME_TO_DETECT_DOUBLE_TAP.toMillis(),
            TimeUnit.MILLISECONDS);
  }

  private void cancelOpenSearchAppExecutor() {
    if (scheduleToOpenSearchApp != null) {
      scheduleToOpenSearchApp.cancel(false);
    }
  }

  private boolean onKeyUp(KeyEvent event) {
    if (currentKeysDown.contains(event.getKeyCode())) {
      maxKeysDown = max(maxKeysDown, currentKeysDown.size());
      if (maxKeysDown == 1 && KEY_EVENT_INDEPENDENT_FUNCTIONAL_KEYS.contains(event.getKeyCode())) {
        analytics.onKeyboardShortcutUsed(
            TalkBackPhysicalKeyboardShortcut.getActionFromInputFrameworkAction(event.getKeyCode()),
            KeyCombo.extractModifiers(getKeyComboCode(event)),
            getKeyComboCode(event));
      }
      currentKeysDown.remove(event.getKeyCode());
    }
    boolean takenByScript = scriptKeys.remove(event.getKeyCode());
    boolean passed = passedKeys.remove(event.getKeyCode()) && !takenByScript;

    // If `isBrowseModeSupported()`, schedule to open the search app if the user has pressed search
    // key (meta key) once.
    if (isBrowseModeSupported()
        && maxKeysDown == 1
        && getSandFlyKey() == SandFlyKey.META
        && isMetaKey(event.getKeyCode())
        && !isEqualToLastKeyComboAndDurationLessThanThreshold()) {
      // Schedule to open search app if it's not already scheduled.
      if (scheduleToOpenSearchApp == null
          || scheduleToOpenSearchApp.isCancelled()
          || scheduleToOpenSearchApp.isDone()) {
        openSearchAppAfterDelay();
      }
    }

    if (isKeyComboPassThroughFeatureAvailable()) {
      if (keyComboPassThroughState != KeyComboPassThroughState.INACTIVE) {
        // Remove the key from the set of pressed keys to detect when the full key combo is
        // released.
        keyComboPassThroughPressedKeys.remove(event.getKeyCode());

        if (keyComboPassThroughState
                == KeyComboPassThroughState.PENDING_PASS_THROUGH_SHORTCUT_RELEASE
            && !isAnyModifierKeyPressed(event)
            && currentKeysDown.isEmpty()) {
          // After the shortcut key combo is released (including all modifiers), set the state to
          // active to await the next key
          // combo to pass through.
          setKeyComboPassThroughState(KeyComboPassThroughState.ACTIVE);
        } else if (keyComboPassThroughState == KeyComboPassThroughState.ACTIVE
            && keyComboPassThroughPressedKeys.isEmpty()) {
          // After the full key combo is released, set the state to inactive to disable pass
          // through.
          setKeyComboPassThroughState(KeyComboPassThroughState.INACTIVE);
        }
      }
    }

    if (currentKeysDown.isEmpty()
        && previousKeyCombo.getPrefixKeyCode() == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED) {
      if (!performedCombo) {
        interrupt(TalkBackPhysicalKeyboardShortcut.ACTION_UNKNOWN);
      }
      // The interaction is over, reset the state.
      performedCombo = false;
      hasPartialMatch = false;
      previousKeyCombo = currentKeyCombo;
      previousKeyComboTime = currentKeyComboTime;
      currentKeyCombo = new KeyCombo();
      currentKeyComboTime = 0;
      maxKeysDown = 0;
      passedKeys.clear();
      scriptKeys.clear();
    }

    return !passed;
  }

  private void dismissUpdateModifierKeysDialog() {
    if (updateModifierKeysDialog != null && updateModifierKeysDialog.isShowing()) {
      updateModifierKeysDialog.dismiss();
    }
  }

  private void recordBrowseModeStateWhenOff(@BrowseModeLogTrigger int trigger) {
    if (browseModeEnabledTimeMills == 0) {
      LogUtils.w(TAG, "browseModeEnabledTimeMills is 0");
      return;
    }

    analytics.onBrowseModeStopped(
        Duration.ofMillis(SystemClock.uptimeMillis() - browseModeEnabledTimeMills), trigger);
    browseModeEnabledTimeMills = 0;
  }

  private boolean isEqualToLastKeyComboAndDurationLessThanThreshold() {
    return previousKeyCombo.equals(currentKeyCombo)
        && currentKeyComboTime - previousKeyComboTime < TIME_TO_DETECT_DOUBLE_TAP.toMillis();
  }

  // TODO: b/443789728 - Use `eventId` to track the performance of smart browse mode.
  private void handleViewAccessibilityFocusedEvent(AccessibilityEvent event, EventId eventId) {
    AccessibilityNodeInfoCompat sourceNode = AccessibilityNodeInfoUtils.toCompat(event.getSource());
    if (sourceNode == null) {
      // Invalid TYPE_VIEW_ACCESSIBILITY_FOCUSED event.
      return;
    }
    // Smart browse mode turns on browse mode when an a11y focused node in a WebView. Do nothing if
    // the a11y focused node is not in a WebView or the a11y focused node falls into the conditions
    // to turn off browse mode.
    if (!AccessibilityNodeInfoUtils.isSelfOrAncestorRoleWebView(sourceNode)) {
      // Focus left the web content, so the user's choice there no longer applies, unless the user
      // turned browse mode off for this whole app.
      browseModeTurnedOffByUser =
          focusedPackage != null
              && BrowseModeApps.isOn(context)
              && Boolean.FALSE.equals(BrowseModeApps.remembered(context, focusedPackage));
      return;
    }
    // The user turned browse mode off here, such as to use a control's arrow keys. Moving focus
    // with those keys must not turn it back on.
    if (browseModeTurnedOffByUser
        || KeyComboManagerHelper.shouldTurnOffBrowseMode(
            sourceNode, accessibilityFocusMonitor.getInputFocus())) {
      return;
    }

    // When smart browse mode turns on browse mode, don't make speech feedback interruptive.
    setBrowseModeEnabledInternal(true, /* makeSpeechFeedbackInterruptive= */ false);
  }

  // TODO: b/443789728 - Use `eventId` to track the performance of smart browse mode.
  private void handleViewInputFocusedEvent(AccessibilityEvent event, EventId eventId) {
    AccessibilityNodeInfoCompat sourceNode = AccessibilityNodeInfoUtils.toCompat(event.getSource());
    if (sourceNode == null) {
      // Invalid TYPE_VIEW_FOCUSED event.
      return;
    }

    if (KeyComboManagerHelper.shouldTurnOffBrowseMode(sourceNode)) {
      // When smart browse mode turns off browse mode, don't make speech feedback interruptive.
      setBrowseModeEnabledInternal(false, /* makeSpeechFeedbackInterruptive= */ false);
      // Record a Browse Mode log when it's toggled off by smart browse mode.
      recordBrowseModeStateWhenOff(BROWSE_MODE_TURNED_OFF_BY_SMART_BROWSE_MODE);
      return;
    }
  }

  @VisibleForTesting
  public void testing_setKeyComboMapper(KeyComboMapper keyComboMapper) {
    this.keyComboMapper = keyComboMapper;
  }

  @VisibleForTesting
  public void testing_setKeyComboModel(KeyComboModel keyComboModel) {
    this.keyComboModel = keyComboModel;
  }

  @VisibleForTesting
  public void testing_setScheduledExecutorService(ScheduledExecutorService scheduler) {
    this.scheduler = scheduler;
  }

  @VisibleForTesting
  public boolean testing_isSequenceKeyMode() {
    return previousKeyCombo.getPrefixKeyCode() != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED;
  }

  @VisibleForTesting
  public ImmutableSet<String> testing_getBrowseModeOnlyCommands() {
    return browseModeOnlyCommands;
  }
}
