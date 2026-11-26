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
import android.content.pm.PackageManager;
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
import com.google.android.accessibility.talkback.TalkBackService;
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
import com.google.android.accessibility.talkback.report.EnterKeyLog;
import com.google.android.accessibility.talkback.review.ScreenReview;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Manages state related to detecting key combinations. */
public class KeyComboManager
    implements ServiceKeyEventListener, ServiceStateListener, AccessibilityEventListener {
  private static final String TAG = "KeyComboManager";

  /** Set once the keyboard phone keymap has been chosen for a keyboard phone. */
  private static final String PREF_KEYBOARD_PHONE_KEYMAP_CHOSEN = "pref_keyboard_phone_keymap_chosen";

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
   * Speak options for keyboard practice and Hold a touch on the focused item: each key interrupts
   * what the one before said, and a key pressed again is said again.
   */
  private static final SpeakOptions KEY_PRACTICE_SPEAK_OPTIONS =
      SpeakOptions.create()
          .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
          .setFlags(FeedbackItem.FLAG_NO_HISTORY | FeedbackItem.FLAG_FORCE_FEEDBACK_ALL);

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

  /**
   * Whether the user turned SandFly keyboard input off: every key goes to Android, except the keys
   * of Turn SandFly keyboard input on or off. Not kept when SandFly restarts.
   */
  private boolean keyboardInputOff;

  /** Keys whose key down went to Android while SandFly keyboard input was off. */
  private final Set<Integer> keysPassedWhileInputOff = new HashSet<>();

  /** Keys whose key down turned SandFly keyboard input on or off while it was off. */
  private final Set<Integer> keysTakenWhileInputOff = new HashSet<>();

  /** Keyboard practice, which says each key pressed and its command instead of running it. */
  private final KeyPractice keyPractice;

  /** Where keyboard practice speaks, for tests; null speaks through the pipeline. */
  private @Nullable Consumer<String> keyPracticeSpeech;

  /** Hold a touch on the focused item, which holds a touch while its keys are down or latched. */
  private final KeyTouchHold touchHold;

  /** Where Hold a touch on the focused item speaks, for tests; null speaks through the pipeline. */
  private @Nullable Consumer<String> touchHoldSpeech;

  /** The key event being handled, for commands that act on the keys that run them. */
  private @Nullable KeyEvent handlingKeyEvent;

  /** Commands only work in Browse Mode. Built during initialization. */
  private final ImmutableSet<String> browseModeOnlyCommands;

  /** Commands that move by character, word, line or paragraph. */
  private final ImmutableSet<String> textGranularityKeys;

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

  /** Letter keys that press keypad buttons, on keyboards whose letters carry numbers. */
  private final @Nullable KeypadKeys keypadKeys;

  /** The mute key: held in text fields, it dictates. */
  private final @Nullable MuteKey muteKey;

  /** Enter that sends messages. */
  private final MessageEnterKeys messageEnterKeys;

  /** Keys of shortcuts while editing that ran, whose repeats and release are taken too. */
  private final Set<Integer> editingShortcutKeys = new HashSet<>();

  /** Keys pressed while a chooser of the SandFly keyboard was open, released there too. */
  private final Set<Integer> keysPassedToChooser = new HashSet<>();

  /** Browse mode's fast search, which focuses items by their first letter or digit. */
  private @Nullable FastSearch fastSearch;
  // Whether the user ran a SandFly command or touched the screen since a key last went to the app
  // or a new screen opened. While not, and a text field has input focus, the keyboard phone keymaps'
  // single keys and Enter go to the text field.
  private boolean navigatedSinceTyping;
  // Whether a key that types went to the app since a new screen opened. A new screen with input
  // focus in a text field starts with typing, but the arrow keys go to the field only once the user
  // types.
  private boolean typedOnScreen;
  // The text field that SandFly focus moved to and that the arrow keys move past until the user
  // activates it or types into it.
  private final TextFieldActivation textFieldActivation = new TextFieldActivation();
  // Modifier keys pressed alone, keys pressed twice and modifier keys held, in the keyboard phone
  // keymap.
  private final KeyPresses keyPresses =
      new KeyPresses(
          new KeyPresses.Host() {
            @Override
            public int triggerModifier() {
              return keyComboModel.getTriggerModifier();
            }

            @Override
            public @Nullable SandFlyKey heldSandFlyKey() {
              @Nullable SandFlyKey sandFlyKey = getSandFlyKey();
              return sandFlyKey != null && sandFlyKey.isHeldKey() ? sandFlyKey : null;
            }

            @Override
            public boolean isAssigned(KeyCombo combo) {
              return serviceState == SERVICE_STATE_ACTIVE
                  && matchKeyCombo
                  && keyComboPassThroughState != KeyComboPassThroughState.ACTIVE
                  && (combo.isTriggerModifierUsed() || !isPassingKeysToApp())
                  && keyComboModel.getKeyComboMap().containsValue(inBrowseModeIfAssigned(combo));
            }

            @Override
            public boolean canHoldKeys() {
              return !isEditingTextWithAccessibilityFocus() && !isTyping();
            }

            @Override
            public void perform(KeyCombo combo) {
              boolean performed = performedCombo;
              boolean partial = hasPartialMatch;
              matchAndPerformKeyCombo(combo, keyComboModel.getTriggerModifier());
              performedCombo = performed;
              hasPartialMatch = partial;
            }
          });

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
    keypadKeys = context instanceof AccessibilityService service ? new KeypadKeys(service) : null;
    messageEnterKeys = new MessageEnterKeys(context);
    muteKey =
        context instanceof AccessibilityService service
            ? new MuteKey(
                service,
                command -> {
                  if (commandPerformer != null) {
                    commandPerformer.perform(command, EVENT_ID_UNTRACKED);
                  }
                  return kotlin.Unit.INSTANCE;
                })
            : null;
    keyPractice =
        new KeyPractice(
            context,
            this,
            text -> {
              speakForKeyPractice(text);
              return kotlin.Unit.INSTANCE;
            });
    touchHold =
        new KeyTouchHold(
            context,
            text -> {
              speakForTouchHold(text);
              return kotlin.Unit.INSTANCE;
            });
    sharedPreferences = SharedPreferencesUtils.getSharedPreferences(context);
    initializeDefaultPreferenceValues();
    keyComboModel = createKeyComboModel();
    arrowKeyNavKeyHandler =
        new ArrowKeyNavKeyHandler(
            context,
            new ArrowKeyNavKeyHandler.Callbacks() {
              @Override
              public void perform(String command) {
                onKeyboardAction(Commands.keyboardActionFor(context, command));
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
                return isEditingTextForArrowKeys();
              }
            });
    // Create a set of commands that only work in Browse Mode.
    browseModeOnlyCommands =
        FeatureFlagReader.enableBrowseMode(context)
            ? ImmutableSet.copyOf(initializeBrowseModeOnlyCommands())
            : ImmutableSet.of();
    textGranularityKeys =
        ImmutableSet.of(
            context.getString(R.string.keycombo_shortcut_navigate_next_character),
            context.getString(R.string.keycombo_shortcut_navigate_previous_character),
            context.getString(R.string.keycombo_shortcut_navigate_next_word),
            context.getString(R.string.keycombo_shortcut_navigate_previous_word),
            context.getString(R.string.keycombo_shortcut_navigate_next_line),
            context.getString(R.string.keycombo_shortcut_navigate_previous_line),
            context.getString(R.string.keycombo_shortcut_navigate_next_paragraph),
            context.getString(R.string.keycombo_shortcut_navigate_previous_paragraph));
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
    if (accessibilityService instanceof TalkBackService talkBack) {
      touchHold.setToucher(
          new KeyTouchHold.FocusToucher(
              talkBack,
              () ->
                  accessibilityFocusMonitor.getAccessibilityFocus(
                      /* useInputFocusIfEmpty= */ false)));
      fastSearch = new FastSearch(context, talkBack::focusItemStartingWith, this::sayNotFound);
    }
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
    onKeyboardAction(action);
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
    keyPractice.reset();
    touchHold.reset();
    if (browseModeEnabled) {
      browseModeEnabled = false;
      // Record a Browse Mode log if Browse Mode is on when TalkBack is being turned off.
      recordBrowseModeStateWhenOff(BROWSE_MODE_TURNED_OFF_ON_SERVICE_STOP);
    }
    dismissUpdateModifierKeysDialog();
    if (keyComboMapper != null) {
      keyComboMapper.getScreenReview().shutdown();
    }
  }

  @Override
  public void onServiceStateChanged(int newState) {
    // Unfortunately, key events are lost when the TalkBackService becomes active. If a key-down
    // occurs that triggers TalkBack to resume, the corresponding key-up event will not be
    // sent, causing the partially-matched key history to become inconsistent.
    // The following method will cause the key history to be reset.
    setMatchKeyCombo(matchKeyCombo);
    arrowKeyNavKeyHandler.reset();
    if (keyComboMapper != null) {
      keyComboMapper.getScreenReview().resetKeys();
    }
    // The up event of a held SandFly key may be lost the same way.
    sandFlyKeyHeld = false;
    if (keypadKeys != null) {
      keypadKeys.reset();
    }
    if (muteKey != null) {
      muteKey.reset();
    }
    messageEnterKeys.reset();
    keysPassedToChooser.clear();
    editingShortcutKeys.clear();
    if (fastSearch != null) {
      fastSearch.clear();
    }
    textFieldActivation.reset();
    keysPassedWhileInputOff.clear();
    keysTakenWhileInputOff.clear();
    // Keyboard practice ends, and a touch held lifts, when SandFly is suspended or stops.
    if (newState != SERVICE_STATE_ACTIVE) {
      keyPractice.reset();
      touchHold.reset();
    }
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
    @Nullable KeyEvent outer = handlingKeyEvent;
    handlingKeyEvent = event;
    try {
      return handleKeyEvent(event, eventId);
    } finally {
      handlingKeyEvent = outer;
    }
  }

  private boolean handleKeyEvent(KeyEvent event, EventId eventId) {
    trackSandFlyKey(event);
    if (event.getAction() == KeyEvent.ACTION_DOWN
        && event.getRepeatCount() == 0
        && !KeyEvent.isModifierKey(event.getKeyCode())) {
      textFieldActivation.onInput();
    }
    // A key pressed while SandFly keyboard input was off is released where it was pressed, even
    // after keyboard input is turned on again, so that the app doesn't see a key held forever.
    @Nullable Boolean whileInputOff = finishKeyPressedWhileInputOff(event);
    if (whileInputOff != null) {
      return whileInputOff;
    }
    if (keyEventDelegate != null) {
      if (keyEventDelegate.onKeyEvent(event, eventId)) {
        arrowKeyNavKeyHandler.onKeyEventConsumedElsewhere(event);
        return true;
      }
    }

    // In keyboard practice, every key pressed is said with its command, and nothing else gets it.
    boolean practiceWasOn = keyPractice.isOn();
    if (keyPractice.onKeyEvent(event)) {
      // Presses before practice don't count towards the next press after it.
      if (practiceWasOn && !keyPractice.isOn()) {
        keyPresses.clear();
      }
      return true;
    }

    // While a touch is held: the repeats of the key holding it, and Back or Escape, which lift it.
    if (touchHold.onKeyEvent(event, modifiersOf(event))) {
      return true;
    }

    if (isKeyboardInputOff()) {
      return onKeyEventWhileInputOff(event);
    }

    // The mute key held in a text field dictates; elsewhere it runs its own command, if it has one.
    // After Pass the next key, it goes to the app.
    if (muteKey != null
        && muteKey.onKeyEvent(
            event,
            serviceState == SERVICE_STATE_ACTIVE
                && keyComboPassThroughState != KeyComboPassThroughState.ACTIVE)) {
      return true;
    }

    // While the SandFly keyboard's emoji search or symbol chooser is open, every key pressed is its
    // own, up to its release; in its editing mode, every key pressed without the SandFly key; while
    // a word is typed with word suggestions on, Up and Down, and while its list is open, the keys
    // that move through it, type a suggestion and close it. Keys pressed before, such as the
    // shortcut that opened it, are released here.
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      if (keysPassedToChooser.contains(event.getKeyCode())
          || SandFlyKeyboardHost.isChoosing()
          || (SandFlyKeyboardHost.isEditingModeOn() && !isSandFlyKeyEvent(event))
          || (!isSandFlyKeyEvent(event) && SandFlyKeyboardHost.takesKey(event.getKeyCode()))) {
        keysPassedToChooser.add(event.getKeyCode());
        return false;
      }
    } else if (keysPassedToChooser.contains(event.getKeyCode())) {
      if (event.getAction() == KeyEvent.ACTION_UP) {
        keysPassedToChooser.remove(event.getKeyCode());
      }
      return false;
    }

    // Letter keys on keypads and Enter in message fields, before shortcuts. While SandFly is
    // suspended, only the releases of keys they took. Enter on a message field that SandFly focus
    // moved to activates the field instead; the Send key still sends.
    if (serviceState == SERVICE_STATE_ACTIVE || event.getAction() != KeyEvent.ACTION_DOWN) {
      boolean enter = isEnterKey(event.getKeyCode());
      if ((keypadKeys != null && keypadKeys.onKeyEvent(event, this::isEditingText))
          || messageEnterKeys.onKeyEvent(
              event,
              () -> accessibilityFocusMonitor == null ? null : accessibilityFocusMonitor.getInputFocus(),
              () -> isEditingText() && !(enter && isSandFlyFocusOnTextFieldMovedTo()))) {
        arrowKeyNavKeyHandler.onKeyEventConsumedElsewhere(event);
        return true;
      }
    }

    if (onEditingShortcut(event)) {
      arrowKeyNavKeyHandler.onKeyEventConsumedElsewhere(event);
      return true;
    }

    // In browse mode, the fast search key with a letter or digit focuses the next item starting
    // with it, with Shift the previous one.
    if (fastSearch != null && fastSearch.onKeyEvent(event, getSandFlyKey(), this::canFastSearch)) {
      navigatedSinceTyping = true;
      arrowKeyNavKeyHandler.onKeyEventConsumedElsewhere(event);
      return true;
    }

    if (plainKeys() && keyPresses.onKeyEvent(event)) {
      navigatedSinceTyping = true;
      arrowKeyNavKeyHandler.onKeyEventConsumedElsewhere(event);
      // A held SandFly key's release taken here still ends Pass the next key's own keys, so the
      // next key, such as Sym for the keyboard's symbols, goes to the app.
      if (event.getAction() == KeyEvent.ACTION_UP
          && keyComboPassThroughState
              == KeyComboPassThroughState.PENDING_PASS_THROUGH_SHORTCUT_RELEASE
          && !isAnyModifierKeyPressed(event)
          && currentKeysDown.isEmpty()) {
        setKeyComboPassThroughState(KeyComboPassThroughState.ACTIVE);
      }
      return true;
    }

    // While the review cursor is on, arrow keys and the like move it.
    @Nullable ScreenReview screenReview = keyComboMapper == null ? null : keyComboMapper.getScreenReview();
    if (screenReview != null
        && (event.getAction() == KeyEvent.ACTION_UP
            || (serviceState == SERVICE_STATE_ACTIVE
                && matchKeyCombo
                && keyComboPassThroughState != KeyComboPassThroughState.ACTIVE
                && !sandFlyKeyHeld))
        && screenReview.onKeyEvent(event)) {
      arrowKeyNavKeyHandler.onKeyEventConsumedElsewhere(event);
      return true;
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
                && !sandFlyKeyHeld
                && !isPassingKeysToApp())) {
      // Moving with the arrow keys, such as the Zinwa Q25 trackpad's, ends typing as a shortcut
      // does, so that Enter activates the item moved to.
      if (event.getAction() == KeyEvent.ACTION_DOWN) {
        navigatedSinceTyping = true;
      }
      return true;
    }

    if (!hasPartialMatch && !performedCombo && !matchKeyCombo) {
      return false;
    }

    boolean handled =
        switch (event.getAction()) {
          case KeyEvent.ACTION_DOWN -> onKeyDown(event);
          case KeyEvent.ACTION_MULTIPLE -> hasPartialMatch;
          case KeyEvent.ACTION_UP -> onKeyUp(event);
          default -> false;
        };
    // A key that went to the app, such as one typed into a message, or a SandFly shortcut.
    if (event.getAction() == KeyEvent.ACTION_DOWN
        && !KeyEvent.isModifierKey(event.getKeyCode())
        && !isEnterKey(event.getKeyCode())) {
      if (navigatedSinceTyping && !handled) {
        EnterKeyLog.note(
            "Typing again after "
                + (event.isPrintingKey()
                    ? "a printing key"
                    : KeyEvent.keyCodeToString(event.getKeyCode()))
                + " went to the app");
      }
      navigatedSinceTyping = handled;
    }
    // A key typed into the text field with input focus, Enter too, starts editing it.
    if (event.getAction() == KeyEvent.ACTION_DOWN && !handled && isTypingKey(event)) {
      typedOnScreen = true;
      textFieldActivation.onTyped();
    }
    return handled;
  }

  /**
   * The state that decides where Enter goes, for the Enter key log: whether the user is typing,
   * and the kinds and view IDs of the items with SandFly focus and input focus, without their text.
   */
  public String describeEnterState() {
    @Nullable Boolean editing = SandFlyKeyboardHost.isEditing();
    @Nullable AccessibilityNodeInfoCompat focus =
        accessibilityFocusMonitor == null
            ? null
            : accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    @Nullable AccessibilityNodeInfoCompat inputFocus =
        accessibilityFocusMonitor == null ? null : accessibilityFocusMonitor.getInputFocus();
    return String.format(
        Locale.ROOT,
        "typing %s, moved since typing %s, SandFly keyboard %s, SandFly focus %s, input focus %s,"
            + " SandFly focus on input focus %s, text field moved to and not activated %s,"
            + " keymap %s",
        yesNo(isTyping()),
        yesNo(navigatedSinceTyping),
        editing == null ? "not in use" : (editing ? "editing" : "not editing"),
        describeForLog(focus),
        describeForLog(inputFocus),
        yesNo(focus != null && focus.equals(inputFocus)),
        yesNo(isTextFieldMovedTo(focus)),
        getKeymap());
  }

  private static String yesNo(boolean value) {
    return value ? "yes" : "no";
  }

  /** The kind and view ID of {@code node}, without its text. */
  private static String describeForLog(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return "none";
    }
    return "["
        + node.getClassName()
        + " "
        + node.getViewIdResourceName()
        + (node.isEditable() ? " editable" : "")
        + (node.isFocused() ? " focused" : "")
        + (AccessibilityNodeInfoUtils.isClickable(node) ? " clickable" : "")
        + "]";
  }

  @Override
  public boolean processWhenServiceSuspended() {
    return true;
  }

  @Override
  public int getEventTypes() {
    return AccessibilityEvent.TYPE_VIEW_FOCUSED
        | AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
        // A click on a text field that SandFly focus moved to starts editing it.
        | AccessibilityEvent.TYPE_VIEW_CLICKED
        // The screen review reads the window again after these.
        | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        | AccessibilityEvent.TYPE_WINDOWS_CHANGED
        | AccessibilityEvent.TYPE_VIEW_SCROLLED
        | AccessibilityEvent.TYPE_TOUCH_INTERACTION_START;
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
    if (keyComboMapper != null) {
      keyComboMapper.getScreenReview().onAccessibilityEvent(event);
    }
    // A new screen starts with typing where a text field has input focus; a touch moves around.
    if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      if (navigatedSinceTyping) {
        EnterKeyLog.note(
            String.format(
                Locale.ROOT,
                "Typing again after a window change in %s, content change types %d",
                event.getPackageName(),
                event.getContentChangeTypes()));
      }
      navigatedSinceTyping = false;
      typedOnScreen = false;
    } else if (event.getEventType() == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START) {
      navigatedSinceTyping = true;
      textFieldActivation.onInput();
    }
    onTextFieldActivationEvent(event);
    if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_FOCUSED
        && event.getEventType() != AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
      return;
    }
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
    // Keys pressed or released while the delegate had them aren't followed.
    keyPresses.clear();
  }

  /**
   * Returns keymap by reading preference.
   *
   * @return key map. Returns default key map as default.
   */
  public String getKeymap() {
    return sharedPreferences.getString(
        context.getString(R.string.pref_select_keymap_key),
        context.getString(Keymap.defaultKeymap().getValueRes()));
  }

  /** Refreshes key combo model after key map changes. */
  public void refreshKeyComboModel() {
    keyComboModel = createKeyComboModel();
    keyPresses.clear();
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
    return getKeyComboStringRepresentation(keyCombo, keyComboModel);
  }

  /** Returns {@code keyCombo} as text, as pressed in the keymap of {@code model}. */
  public String getKeyComboStringRepresentation(KeyCombo keyCombo, KeyComboModel model) {
    if (keyCombo.getKeyComboCode() == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED) {
      return context.getString(R.string.keycombo_unassigned);
    }
    // Browse mode's toggle is written as the SandFly key's Meta key with no key after it.
    @Nullable
    SandFlyKey sandFlyKey = model instanceof NewKeyComboModel newModel ? newModel.getSandFlyKey() : null;
    if (sandFlyKey != null
        && keyCombo.isTriggerModifierUsed()
        && keyCombo.getPrefixKeyCode() == KeyEvent.KEYCODE_META_RIGHT
        && keyCombo.getKeyCode() == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED) {
      return context.getString(
          R.string.sandfly_keyboard_tutorial_key_twice, context.getString(sandFlyKey.getNameRes()));
    }

    int triggerModifier = model.getTriggerModifier();
    boolean sequenceKeyCombo =
        isSequenceKeyInfraSupported(model)
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
      appendKey(keyCombo.getPrefixKeyCode(), assignedShortcut, sandFlyKey);
    }
    // Append key code.
    appendKey(keyCombo.getKeyCode(), assignedShortcut, sandFlyKey);

    String keys = assignedShortcut.toString();
    if (keyCombo.isPressedTwice()) {
      return context.getString(R.string.sandfly_keyboard_tutorial_key_twice, keys);
    }
    if (keyCombo.isHeldDown()) {
      return context.getString(R.string.keycombo_keys_held, keys);
    }
    if (keyCombo.isWhileEditing()) {
      return context.getString(R.string.keycombo_keys_while_editing, keys);
    }
    if (keyCombo.isInBrowseMode()) {
      return context.getString(R.string.keycombo_keys_in_browse_mode, keys);
    }
    return keys;
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
    return isSequenceKeyInfraSupported(keyComboModel);
  }

  private boolean isSequenceKeyInfraSupported(KeyComboModel model) {
    return FeatureFlagReader.enableSequenceKeyInfra(context)
        && model instanceof NewKeyComboModel newModel
        && newModel.usesSequences();
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

  /**
   * Returns whether SandFly keyboard input is off, turned off by the user or by a script for the
   * app shown: every key goes to Android except the keys of Turn SandFly keyboard input on or off.
   */
  public boolean isKeyboardInputOff() {
    return keyboardInputOff || Scripts.passesAllKeys();
  }

  /**
   * Turns SandFly keyboard input off, or back on, and says which. Turning it on also ends a script's
   * request to pass all keys to the app shown, until the script asks again.
   */
  public void toggleKeyboardInput() {
    boolean off = !isKeyboardInputOff();
    keyboardInputOff = off;
    if (!off) {
      Scripts.stopPassingAllKeys();
    }
    LogUtils.i(TAG, "SandFly keyboard input %s", off ? "off" : "on");
    if (pipeline == null) {
      return;
    }
    pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Feedback.speech(
            context.getString(off ? R.string.keyboard_input_off : R.string.keyboard_input_on),
            INTERRUPTIVE_SPEAK_OPTIONS));
  }

  /**
   * Handles a key event while SandFly keyboard input is off. Only the keys of Turn SandFly keyboard
   * input on or off are taken; every other key, the SandFly key and its shortcuts too, goes to
   * Android. Keys pressed before keyboard input was turned off are released the way they were
   * pressed.
   */
  private boolean onKeyEventWhileInputOff(KeyEvent event) {
    int keyCode = event.getKeyCode();
    if (event.getAction() == KeyEvent.ACTION_UP) {
      // The review cursor and arrow key navigation release the keys they took.
      @Nullable ScreenReview screenReview =
          keyComboMapper == null ? null : keyComboMapper.getScreenReview();
      if (screenReview != null && screenReview.onKeyEvent(event)) {
        arrowKeyNavKeyHandler.onKeyEventConsumedElsewhere(event);
        return true;
      }
      if (arrowKeyNavKeyHandler.onKeyEvent(event, /* canAct= */ false)) {
        return true;
      }
      if (currentKeysDown.contains(keyCode)
          || passedKeys.contains(keyCode)
          || scriptKeys.contains(keyCode)) {
        return onKeyUp(event);
      }
      return false;
    }
    if (event.getAction() != KeyEvent.ACTION_DOWN) {
      return false;
    }
    // Held down before keyboard input was turned off: its repeats go where its key down went.
    if (currentKeysDown.contains(keyCode)) {
      return !passedKeys.contains(keyCode);
    }
    if (event.getRepeatCount() == 0
        && serviceState == SERVICE_STATE_ACTIVE
        && isKeyboardInputToggle(event)) {
      keysTakenWhileInputOff.add(keyCode);
      toggleKeyboardInput();
      return true;
    }
    keysPassedWhileInputOff.add(keyCode);
    return false;
  }

  /**
   * Returns where the key repeat or key up of a key pressed while SandFly keyboard input was off
   * goes: true when SandFly takes it, false when Android gets it, or null for other keys.
   */
  private @Nullable Boolean finishKeyPressedWhileInputOff(KeyEvent event) {
    int keyCode = event.getKeyCode();
    boolean up = event.getAction() == KeyEvent.ACTION_UP;
    boolean repeat = event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() > 0;
    if (!up && !repeat) {
      return null;
    }
    if (up ? keysTakenWhileInputOff.remove(keyCode) : keysTakenWhileInputOff.contains(keyCode)) {
      return true;
    }
    if (up ? keysPassedWhileInputOff.remove(keyCode) : keysPassedWhileInputOff.contains(keyCode)) {
      return false;
    }
    return null;
  }

  /** Whether the key down [event] completes the keys of Turn SandFly keyboard input on or off. */
  private boolean isKeyboardInputToggle(KeyEvent event) {
    if (keyComboModel == null || KeyEvent.isModifierKey(event.getKeyCode())) {
      return false;
    }
    KeyCombo target =
        keyComboModel.getKeyComboForKey(
            context.getString(R.string.keycombo_shortcut_toggle_keyboard_input));
    if (target == null || target.getKeyComboCode() == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED) {
      return false;
    }
    @Nullable SandFlyKey sandFlyKey = getSandFlyKey();
    if (sandFlyKey != null && sandFlyKey.isKey(event)) {
      return false;
    }
    int triggerModifier = keyComboModel.getTriggerModifier();
    int modifiers = modifiersOf(event);
    boolean pressedTriggerModifier =
        triggerModifier != KeyComboModel.NO_MODIFIER
            && (modifiers & triggerModifier) == triggerModifier;
    KeyCombo pressed =
        new KeyCombo(
            modifiers & ~(pressedTriggerModifier ? triggerModifier : 0),
            KeyComboModel.NO_PREFIX_KEY_CODE,
            getConvertedKeyCode(event),
            pressedTriggerModifier);
    return pressed.matchWith(triggerModifier, target) == EXACT_MATCH;
  }

  /** Returns whether the app shown gets the keys pressed without the SandFly key. */
  private boolean isPassingKeysToApp() {
    return PassKeysApps.isOn(context, focusedPackage);
  }

  /**
   * Makes the app shown get the keys pressed without the SandFly key, such as the arrow keys, or
   * stop getting them, and says which. Returns false when no app is shown.
   */
  public boolean togglePassKeysToApp() {
    @Nullable String app = focusedPackage;
    if (app == null) {
      return false;
    }
    boolean on = PassKeysApps.toggle(context, app);
    CharSequence name = app;
    try {
      PackageManager packageManager = context.getPackageManager();
      name = packageManager.getApplicationLabel(packageManager.getApplicationInfo(app, 0));
    } catch (PackageManager.NameNotFoundException e) {
      LogUtils.w(TAG, "No app %s", app);
    }
    pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Feedback.speech(
            context.getString(on ? R.string.pass_keys_to_app_on : R.string.pass_keys_to_app_off, name),
            INTERRUPTIVE_SPEAK_OPTIONS));
    return true;
  }

  /** Turns arrow key navigation on or off. */
  public void toggleArrowKeyNav() {
    if (!ArrowKeyNav.isAvailable(context)) {
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.speech(
              context.getString(R.string.arrow_key_nav_unavailable), INTERRUPTIVE_SPEAK_OPTIONS));
      return;
    }
    boolean on = !ArrowKeyNav.isOn(context);
    ArrowKeyNav.setOn(context, on);
    pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Feedback.speech(
            context.getString(on ? R.string.arrow_key_nav_on : R.string.arrow_key_nav_off),
            INTERRUPTIVE_SPEAK_OPTIONS));
  }

  /** Returns whether keyboard practice is on. */
  public boolean isKeyPracticeOn() {
    return keyPractice.isOn();
  }

  /**
   * Turns keyboard practice on or off, and says which. While it is on, each key pressed is said
   * with the command it runs, and nothing runs.
   */
  public void toggleKeyPractice() {
    keyPractice.toggle();
    LogUtils.i(TAG, "Keyboard practice %s", keyPractice.isOn() ? "on" : "off");
    // Presses before practice don't count towards the next press after it.
    if (!keyPractice.isOn()) {
      keyPresses.clear();
    }
  }

  /**
   * Runs Hold a touch on the focused item: lifts the touch held, or holds a touch on the item with
   * SandFly focus, while the keys that ran it are down, or latched when they are released at once
   * or it didn't run from keys.
   */
  public void toggleTouchHold() {
    touchHold.toggle(handlingKeyEvent);
  }

  /** Returns whether Hold a touch on the focused item holds a touch now. */
  public boolean isTouchHeld() {
    return touchHold.isHeld();
  }

  /** Returns whether the touch held is latched, so that it stays down without keys. */
  public boolean isTouchHoldLatched() {
    return touchHold.isLatched();
  }

  private void speakForTouchHold(String text) {
    if (touchHoldSpeech != null) {
      touchHoldSpeech.accept(text);
      return;
    }
    if (pipeline == null) {
      return;
    }
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(text, KEY_PRACTICE_SPEAK_OPTIONS));
  }

  private void speakForKeyPractice(String text) {
    if (keyPracticeSpeech != null) {
      keyPracticeSpeech.accept(text);
      return;
    }
    if (pipeline == null) {
      return;
    }
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(text, KEY_PRACTICE_SPEAK_OPTIONS));
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

  /**
   * Returns whether the command saved under [key] works only in browse mode. Moving by character,
   * word, line and paragraph works anywhere in keymaps where arrow keys move SandFly focus.
   */
  private boolean isBrowseModeOnly(String key) {
    return browseModeOnlyCommands.contains(key)
        && !plainKeys()
        && !(arrowsNavigate() && textGranularityKeys.contains(key));
  }

  /**
   * Returns whether the keymap is for keyboards without Ctrl, Meta or arrow keys, whose single keys
   * work outside text fields and, with the SandFly key, anywhere.
   */
  private boolean plainKeys() {
    return keyComboModel instanceof NewKeyComboModel model && model.plainKeys();
  }

  /**
   * Returns [keyCombo], pressed without the SandFly key, as the keymap's shortcut of the same keys
   * in browse mode, where browse mode is on in a keyboard phone keymap and the keys have one;
   * otherwise [keyCombo].
   */
  private KeyCombo inBrowseModeIfAssigned(KeyCombo keyCombo) {
    if (!plainKeys()
        || !isBrowseModeEnabled()
        || keyCombo.isTriggerModifierUsed()
        || keyCombo.getPrefixKeyCode() != KeyComboModel.NO_PREFIX_KEY_CODE
        || (keyCombo.getModifiers()
                & (KeyCombo.PRESS_TYPES | KeyCombo.WHILE_EDITING | KeyCombo.IN_BROWSE_MODE))
            != 0) {
      return keyCombo;
    }
    KeyCombo browseModeKeyCombo = keyCombo.withInBrowseMode(true);
    return keyComboModel.getKeyComboMap().containsValue(browseModeKeyCombo)
        ? browseModeKeyCombo
        : keyCombo;
  }

  /** Returns whether arrow keys without the SandFly key move SandFly focus, as in NVDA. */
  private boolean arrowsNavigate() {
    return keyComboModel instanceof NewKeyComboModel model && model.arrowsNavigate();
  }

  /** Returns whether [keyCombo] is, or starts, a shortcut of the keymap or of the app shown. */
  private boolean isShortcutKey(KeyCombo keyCombo, int triggerModifier) {
    for (kotlin.Pair<KeyCombo, String> appKey : AppGestures.keys()) {
      if (keyCombo.matchWith(triggerModifier, appKey.getFirst()) != KeyCombo.NO_MATCH) {
        return true;
      }
    }
    KeyCombo browseModeKeyCombo = inBrowseModeIfAssigned(keyCombo);
    if (browseModeKeyCombo != keyCombo) {
      return true;
    }
    for (Map.Entry<String, KeyCombo> entry : keyComboModel.getKeyComboMap().entrySet()) {
      // Browse mode's letters are typing while browse mode is off, as in matchAndPerformKeyCombo.
      if (isBrowseModeSupported() && !browseModeEnabled && isBrowseModeOnly(entry.getKey())) {
        continue;
      }
      if (keyCombo.matchWith(triggerModifier, entry.getValue()) != KeyCombo.NO_MATCH) {
        return true;
      }
    }
    return false;
  }

  /** Whether [event] is the SandFly key, or a key pressed with it. */
  private boolean isSandFlyKeyEvent(KeyEvent event) {
    @Nullable SandFlyKey sandFlyKey = getSandFlyKey();
    return sandFlyKeyHeld
        || isSandFlyKeyPressed(event)
        || (sandFlyKey != null && sandFlyKey.isKey(event));
  }

  /** Whether [keyCode] is Enter, Numpad Enter or the centre of a D-pad. */
  private static boolean isEnterKey(int keyCode) {
    return keyCode == KeyEvent.KEYCODE_ENTER
        || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
        || keyCode == KeyEvent.KEYCODE_DPAD_CENTER;
  }

  /** Whether SandFly focus is on an item other than the one with input focus. */
  private boolean isSandFlyFocusAwayFromInputFocus() {
    if (accessibilityFocusMonitor == null) {
      return false;
    }
    AccessibilityNodeInfoCompat focus =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    return focus != null && !focus.equals(accessibilityFocusMonitor.getInputFocus());
  }

  /**
   * Whether the user is typing: a text field has input focus, and since a key last went to the app
   * or the screen changed, the user ran no SandFly command and didn't touch the screen.
   */
  private boolean isTyping() {
    if (navigatedSinceTyping) {
      return false;
    }
    // The SandFly keyboard knows whether a text field takes what is typed, without asking the app.
    @Nullable Boolean editing = SandFlyKeyboardHost.isEditing();
    if (editing != null) {
      return editing;
    }
    if (accessibilityFocusMonitor == null) {
      return false;
    }
    AccessibilityNodeInfoCompat inputFocus = accessibilityFocusMonitor.getInputFocus();
    return inputFocus != null && inputFocus.isFocused() && inputFocus.isEditable();
  }

  /**
   * Runs the shortcut while editing that [event] presses, in a text field being edited, which then
   * doesn't get the keys. Only keys pressed without the SandFly key look anything up.
   */
  private boolean onEditingShortcut(KeyEvent event) {
    int keyCode = event.getKeyCode();
    if (event.getAction() != KeyEvent.ACTION_DOWN) {
      return event.getAction() == KeyEvent.ACTION_UP
          ? editingShortcutKeys.remove(keyCode)
          : editingShortcutKeys.contains(keyCode);
    }
    if (editingShortcutKeys.contains(keyCode)) {
      // Its repeats.
      return true;
    }
    if (serviceState != SERVICE_STATE_ACTIVE
        || !matchKeyCombo
        || keyComboModel == null
        || event.getRepeatCount() > 0
        || KeyEvent.isModifierKey(keyCode)
        || keyComboPassThroughState == KeyComboPassThroughState.ACTIVE
        || isSandFlyKeyPressed(event)
        || sandFlyKeyHeld) {
      return false;
    }
    KeyCombo keys =
        new KeyCombo(
            (event.getModifiers() & KEY_EVENT_MODIFIER_MASK) | KeyCombo.WHILE_EDITING,
            KeyComboModel.NO_PREFIX_KEY_CODE,
            getConvertedKeyCode(event),
            /* triggerModifierUsed= */ false);
    if (!keyComboModel.getKeyComboMap().containsValue(keys) || !isEditingText()) {
      return false;
    }
    boolean performed = matchAndPerformKeyCombo(keys, keyComboModel.getTriggerModifier());
    performedCombo = false;
    hasPartialMatch = false;
    if (performed) {
      editingShortcutKeys.add(keyCode);
    }
    return performed;
  }

  /**
   * Whether fast search works now: browse mode is on, keys aren't going to the app and no text
   * field is being edited, where browse mode's letters type too.
   */
  private boolean canFastSearch() {
    return serviceState == SERVICE_STATE_ACTIVE
        && matchKeyCombo
        && isBrowseModeEnabled()
        && keyComboPassThroughState != KeyComboPassThroughState.ACTIVE
        && !sandFlyKeyHeld
        && !isPassingKeysToApp()
        && !isEditingTextWithAccessibilityFocus()
        && !(plainKeys() && isTyping());
  }

  /** Says that fast search found no item. */
  private void sayNotFound() {
    if (fastSearchSpeech != null) {
      fastSearchSpeech.accept(context.getString(R.string.granular_editing_not_found));
      return;
    }
    if (pipeline != null) {
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.speech(
              context.getString(R.string.granular_editing_not_found), INTERRUPTIVE_SPEAK_OPTIONS));
    }
  }

  /** Where fast search speaks, for tests; null speaks through the pipeline. */
  private @Nullable Consumer<String> fastSearchSpeech;

  /** Sets what fast search focuses items with, and where it speaks, for tests. */
  @VisibleForTesting
  void setFastSearch(FastSearch.Searcher searcher, Consumer<String> speech) {
    fastSearchSpeech = speech;
    fastSearch = new FastSearch(context, searcher, this::sayNotFound);
  }

  /** Whether the user is typing in a text field or SandFly focus is on the one being edited. */
  private boolean isEditingText() {
    return isEditingTextWithAccessibilityFocus() || isTyping();
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

  /**
   * Whether single arrow keys go to a text field instead of arrow key navigation. SandFly focus is on
   * the text field with input focus, unless SandFly focus moved there and the field wasn't activated
   * or typed into since; or, in the keyboard phone keymaps, the user typed into the text field with
   * input focus and ran no SandFly command since, wherever SandFly focus is.
   */
  private boolean isEditingTextForArrowKeys() {
    if (plainKeys() && typedOnScreen && isTyping()) {
      LogUtils.w(TAG, "Arrow keys edit: typed into the text field with input focus");
      return true;
    }
    if (!isEditingTextWithAccessibilityFocus()) {
      return false;
    }
    @Nullable AccessibilityNodeInfoCompat focus =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (isTextFieldMovedTo(focus)) {
      LogUtils.w(
          TAG,
          "Arrow keys move on: SandFly focus moved to the text field with input focus %s, not"
              + " activated or typed into since",
          describeForLog(focus));
      return false;
    }
    LogUtils.w(
        TAG,
        "Arrow keys edit: SandFly focus on the text field with input focus %s, %s",
        describeForLog(focus),
        navigatedSinceTyping ? "activated or put there by the app" : "typed into or on a new screen");
    return true;
  }

  /** Whether SandFly focus is on a text field it moved to, not activated or typed into since. */
  private boolean isSandFlyFocusOnTextFieldMovedTo() {
    return accessibilityFocusMonitor != null
        && isTextFieldMovedTo(
            accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false));
  }

  /**
   * Whether {@code focus} is a text field that SandFly focus moved to, not activated or typed into
   * since, in a keymap where such a field waits for activation.
   */
  private boolean isTextFieldMovedTo(@Nullable AccessibilityNodeInfoCompat focus) {
    return textFieldActivation.isMovedTo(focus)
        && KeyComboManagerHelper.textFieldsWaitForActivation(context);
  }

  /** Notes a keyboard action about to run, of which Click starts editing a text field. */
  private void onKeyboardAction(@Nullable TalkBackPhysicalKeyboardShortcut action) {
    if (action == TalkBackPhysicalKeyboardShortcut.PERFORM_CLICK) {
      textFieldActivation.onActivated();
    }
  }

  /** Follows SandFly focus moving to text fields, and clicks on them. */
  private void onTextFieldActivationEvent(AccessibilityEvent event) {
    int type = event.getEventType();
    if (type != AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
        && type != AccessibilityEvent.TYPE_VIEW_CLICKED) {
      return;
    }
    @Nullable AccessibilityNodeInfoCompat source =
        AccessibilityNodeInfoUtils.toCompat(event.getSource());
    if (source == null) {
      return;
    }
    if (type == AccessibilityEvent.TYPE_VIEW_CLICKED) {
      textFieldActivation.onClicked(source);
    } else {
      textFieldActivation.onAccessibilityFocused(source, /* moving= */ navigatedSinceTyping);
    }
  }

  /** Whether {@code event} types or edits text: a printing key, Enter, Backspace or Delete. */
  private static boolean isTypingKey(KeyEvent event) {
    int keyCode = event.getKeyCode();
    return event.isPrintingKey()
        || isEnterKey(keyCode)
        || keyCode == KeyEvent.KEYCODE_DEL
        || keyCode == KeyEvent.KEYCODE_FORWARD_DEL;
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
   * ones the keymap gives keys without the SandFly key, unless such keys work anywhere in it.
   */
  public ImmutableSet<String> getBrowseModeOnlyCommands() {
    ImmutableSet.Builder<String> commands = ImmutableSet.<String>builder();
    for (String command : browseModeOnlyCommands) {
      if (isBrowseModeOnly(command)) {
        commands.add(command);
      }
    }
    if (keyComboModel != null && !arrowsNavigate()) {
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
   * Saves the SandFly keymap for new users and for users of the classic keymap, or the keyboard
   * phone keymap on keyboard phones, and keeps the removed Default keymap as a custom keymap for the
   * users who chose it.
   */
  private void initializeDefaultPreferenceValues() {
    String key = context.getString(R.string.pref_select_keymap_key);
    String keymapValue = sharedPreferences.getString(key, null);
    // Once, keyboard phones that had the SandFly keymap, which needs Meta, get the keyboard phone
    // keymap.
    if (KeyboardPhones.isKeyboardPhone()
        && !sharedPreferences.getBoolean(PREF_KEYBOARD_PHONE_KEYMAP_CHOSEN, false)) {
      if (keymapValue == null
          || (Keymap.of(context, keymapValue) == Keymap.SANDFLY
              && CustomKeymaps.of(context, keymapValue) == null
              && !DEFAULT_KEYMAP_VALUE.equals(keymapValue))) {
        keymapValue = context.getString(Keymap.KEYBOARD_PHONE.getValueRes());
        sharedPreferences.edit().putString(key, keymapValue).apply();
      }
      sharedPreferences.edit().putBoolean(PREF_KEYBOARD_PHONE_KEYMAP_CHOSEN, true).apply();
    }
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
            // Second paragraph keys, which are single letters in browse mode.
            context.getString(R.string.keycombo_shortcut_navigate_next_paragraph_extra),
            context.getString(R.string.keycombo_shortcut_navigate_previous_paragraph_extra),
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
            context.getString(R.string.keycombo_shortcut_navigate_next_link_extra),
            context.getString(R.string.keycombo_shortcut_navigate_previous_link_extra),
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
    appendModifier(
        modifier, KeyEvent.META_SYM_ON, context.getString(R.string.keycombo_key_modifier_sym), sb);
    appendModifier(
        modifier,
        KeyEvent.META_FUNCTION_ON,
        context.getString(R.string.keycombo_key_modifier_function),
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
    appendKey(keyCode, sb, getSandFlyKey());
  }

  /** Appends the name of {@code keyCode}, unless it is the held SandFly key {@code sandFlyKey}. */
  private void appendKey(int keyCode, StringBuilder sb, @Nullable SandFlyKey sandFlyKey) {
    if (keyCode <= 0 || KeyEvent.isModifierKey(keyCode) || keyCode == KeyCombo.MODIFIER_KEYS) {
      return;
    }
    // A held SandFly key is named with the SandFly key.
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
    // Where Alt is the SandFly key of a keyboard phone, Alt with a letter types a digit or symbol
    // in a text field being edited.
    if (pressedTriggerModifier
        && plainKeys()
        && sandFlyKey != null
        && sandFlyKey.getTypesWithLetters()
        && event.isPrintingKey()
        && event.getKeyCode() != KeyEvent.KEYCODE_SPACE
        && (isEditingTextWithAccessibilityFocus() || isTyping())) {
      passedKeys.addAll(currentKeysDown);
      return false;
    }
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
    // In an app that gets them, keys without the SandFly key are the app's, browse mode's too.
    if (hasModifier
        && !pressedTriggerModifier
        && !startedSequenceKeyCombo
        && isPassingKeysToApp()) {
      passedKeys.addAll(currentKeysDown);
      return false;
    }
    // Enter in the keyboard phone keymap, where a shortcut is Enter alone: after SandFly focus moved
    // to an item, it activates that item; after a key went to the app, as while typing a message,
    // it goes to the app, which may send the message. Where SandFly focus is on the text field
    // with input focus, it goes to the app, unless SandFly focus moved to that field and it wasn't
    // activated or typed into since. Numpad Enter and the centre of a D-pad are Enter here, as a
    // press of the Zinwa Q25 trackpad is.
    if (hasModifier
        && !pressedTriggerModifier
        && !startedSequenceKeyCombo
        && plainKeys()
        && isEnterKey(event.getKeyCode())) {
      KeyCombo enterKeys =
          new KeyCombo(
              otherModifiers,
              KeyComboModel.NO_PREFIX_KEY_CODE,
              KeyEvent.KEYCODE_ENTER,
              /* triggerModifierUsed= */ false);
      if (keyComboModel.getKeyComboMap().containsValue(enterKeys)) {
        if (event.getRepeatCount() > 0) {
          return enterKeys.equals(currentKeyCombo) && performedCombo;
        }
        if ((isSandFlyFocusAwayFromInputFocus() || isSandFlyFocusOnTextFieldMovedTo())
            && !isTyping()) {
          currentKeyCombo = enterKeys;
          performedCombo = matchAndPerformKeyCombo(enterKeys, triggerModifier);
          hasPartialMatch = false;
          if (performedCombo) {
            keyPresses.forgetPress();
            return true;
          }
        }
        currentKeyCombo = new KeyCombo();
        passedKeys.addAll(currentKeysDown);
        return false;
      }
    }
    // Keys without the SandFly key in keymaps whose shortcuts without it work outside browse mode.
    if (hasModifier
        && !pressedTriggerModifier
        && !startedSequenceKeyCombo
        && (isCustomKeymap() || arrowsNavigate())
        && !KeyEvent.isModifierKey(event.getKeyCode())) {
      // Enter clicks SandFly focus where arrow keys move it, unless input focus is there already.
      // In the keyboard phone keymap, Enter goes to the app unless a shortcut takes it.
      boolean enter =
          arrowsNavigate()
              && !plainKeys()
              && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                  || event.getKeyCode() == KeyEvent.KEYCODE_NUMPAD_ENTER)
              && otherModifiers == KeyComboModel.NO_MODIFIER
              && event.getRepeatCount() == 0;
      // In a text field being edited, they are the text field's, except a custom keymap's
      // shortcuts with Ctrl or Alt, such as Ctrl + Enter for Submit text, which aren't typing.
      // Custom keymaps whose arrows navigate leave text fields being edited alone, like the
      // NVDA, JAWS and Orca keymaps they come from (Ctrl + Home moves the cursor). Only keys that
      // are shortcuts wait for the focus lookups, which ask the app, so typing doesn't.
      boolean customShortcutKeys =
          isCustomKeymap()
              && !arrowsNavigate()
              && (otherModifiers & (KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON)) != 0;
      // In the keyboard phone keymaps, Sym, Fn and Ctrl aren't typing: their shortcuts work in text
      // fields too.
      boolean phoneShortcutKeys =
          plainKeys()
              && (otherModifiers
                      & (KeyEvent.META_SYM_ON | KeyEvent.META_FUNCTION_ON | KeyEvent.META_CTRL_ON))
                  != 0;
      if ((enter
              || (!customShortcutKeys
                  && !phoneShortcutKeys
                  && isShortcutKey(
                  new KeyCombo(
                      otherModifiers,
                      KeyComboModel.NO_PREFIX_KEY_CODE,
                      getConvertedKeyCode(event),
                      /* triggerModifierUsed= */ false),
                  triggerModifier)))
          && (isEditingTextWithAccessibilityFocus() || (plainKeys() && isTyping()))) {
        passedKeys.addAll(currentKeysDown);
        return false;
      }
      if (enter && isSandFlyFocusAwayFromInputFocus()) {
        currentKeyCombo =
            new KeyCombo(
                KeyComboModel.NO_MODIFIER,
                KeyComboModel.NO_PREFIX_KEY_CODE,
                event.getKeyCode(),
                /* triggerModifierUsed= */ false);
        performKeyboardAction(TalkBackPhysicalKeyboardShortcut.PERFORM_CLICK, EVENT_ID_UNTRACKED);
        performedCombo = true;
        hasPartialMatch = false;
        return true;
      }
    }
    if (triggerModifierNotPressedWhenNeeded) {
      // A custom keymap's shortcuts without the SandFly key work anywhere, as do the NVDA, JAWS and
      // Orca keymaps'. Modifier keys pressed on their own always go to the app.
      if ((isCustomKeymap() || arrowsNavigate()) && !KeyEvent.isModifierKey(event.getKeyCode())) {
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
      logKeysWithoutCommandInPractice(
          event,
          new KeyCombo(
              otherModifiers,
              KeyComboModel.NO_PREFIX_KEY_CODE,
              getConvertedKeyCode(event),
              /* triggerModifierUsed= */ false));
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
    // The SandFly key with a single-key shortcut of a keyboard phone keymap does what the key does
    // outside text fields, anywhere, and outside browse mode where browse mode gives the key another
    // shortcut.
    if (pressedTriggerModifier && plainKeys() && !KeyEvent.isModifierKey(event.getKeyCode())) {
      KeyCombo plainKeyCombo =
          new KeyCombo(
              otherModifiers,
              KeyComboModel.NO_PREFIX_KEY_CODE,
              getConvertedKeyCode(event),
              /* triggerModifierUsed= */ false);
      performedCombo =
          matchAndPerformKeyCombo(plainKeyCombo, triggerModifier, /* browseModeKeys= */ false);
      if (performedCombo) {
        return true;
      }
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
      logKeysWithoutCommandInPractice(event, currentKeyCombo);
      passedKeys.addAll(currentKeysDown);
    }
    return hasPartialMatch;
  }

  /**
   * Logs keys that run no command and go to the app while the tutorial's practice lesson is shown,
   * which says only keys that run a command.
   */
  private void logKeysWithoutCommandInPractice(KeyEvent event, KeyCombo keyCombo) {
    if (event.getRepeatCount() == 0
        && !KeyEvent.isModifierKey(event.getKeyCode())
        && TutorialHooks.isPracticing()) {
      LogUtils.i(
          TAG,
          "Keys without a command in tutorial practice, passed to the app: %s",
          getKeyComboStringRepresentation(keyCombo));
    }
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
    return matchAndPerformKeyCombo(keyCombo, triggerModifier, /* browseModeKeys= */ true);
  }

  /**
   * Runs the shortcut [keyCombo] presses. With [browseModeKeys], keys without the SandFly key run
   * the keymap's shortcut of the same keys in browse mode first while browse mode is on.
   */
  private boolean matchAndPerformKeyCombo(
      KeyCombo keyCombo, int triggerModifier, boolean browseModeKeys) {
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

    KeyCombo keymapKeyCombo = browseModeKeys ? inBrowseModeIfAssigned(keyCombo) : keyCombo;
    for (Map.Entry<String, KeyCombo> entry : keyComboModel.getKeyComboMap().entrySet()) {
      final String key = entry.getKey();
      final KeyCombo targetKeyCombo = entry.getValue();
      final int match = keymapKeyCombo.matchWith(triggerModifier, targetKeyCombo);
      if (isBrowseModeSupported() && !browseModeEnabled && isBrowseModeOnly(key)) {
        continue;
      }
      if (match == EXACT_MATCH) {
        @Nullable String commandValue = Commands.valueOfShortcutKey(key);
        if (commandValue != null && commandPerformer == null) {
          continue;
        }
        onKeyboardAction(
            commandValue != null
                ? Commands.keyboardActionFor(context, commandValue)
                : TalkBackPhysicalKeyboardShortcut.getActionFromKey(context.getResources(), key));
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
    if (pipeline == null) {
      // Not connected to the service yet.
    } else if (activatingPassThrough) {
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
        .getKeyComboForKey(
            context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode_extra))
        .isSandFlyKeyPressedTwice();
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
    if (!sandFlyKeyHeld) {
      return modifiers;
    }
    @Nullable SandFlyKey sandFlyKey = getSandFlyKey();
    if (sandFlyKey != null) {
      modifiers = sandFlyKey.withoutOwnModifiers(modifiers, event.getMetaState());
    }
    return modifiers | KeyComboModel.HELD_KEY_MODIFIER;
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
      if (maxKeysDown == 1
          && analytics != null
          && KEY_EVENT_INDEPENDENT_FUNCTIONAL_KEYS.contains(event.getKeyCode())) {
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
    // This handler only turns browse mode on. When it is already on, the checks below
    // cannot change the result, but can read input focus and many ancestors from the app.
    if (browseModeEnabled) {
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
  public void testing_setKeyPracticeSpeech(@Nullable Consumer<String> speech) {
    keyPracticeSpeech = speech;
  }

  @VisibleForTesting
  public void testing_setTouchHoldSpeech(@Nullable Consumer<String> speech) {
    touchHoldSpeech = speech;
  }

  @VisibleForTesting
  public void testing_setTouchHoldToucher(KeyTouchHold.Toucher toucher) {
    touchHold.setToucher(toucher);
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
