/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.android.accessibility.talkback.compositor;

import static com.google.android.accessibility.talkback.compositor.ParseTreeCreator.ENUM_VERBOSITY_DESCRIPTION_ORDER;
import static com.google.android.accessibility.talkback.compositor.roledescription.RoleDescriptionExtractor.DESC_ORDER_ROLE_NAME_STATE_POSITION;
import static com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_KEYBOARD;
import static com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_NON_ALPHABETIC_KEYBOARD;
import static com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_TOUCH;
import static com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_TV_REMOTE;
import static com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_UNKNOWN;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.StringRes;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.compositor.parsetree.ParseTree;
import com.google.android.accessibility.talkback.compositor.parsetree.ParseTree.VariableDelegate;
import com.google.android.accessibility.talkback.compositor.roledescription.RoleDescriptionExtractor.DescriptionOrder;
import com.google.android.accessibility.talkback.compositor.rule.EventTypeViewAccessibilityFocusedFeedbackRule;
import com.google.android.accessibility.talkback.compositor.rule.InputTextFeedbackRules;
import com.google.android.accessibility.talkback.compositor.rule.MagnificationStateChangedFeedbackRule;
import com.google.android.accessibility.talkback.controlsounds.ControlSounds;
import com.google.android.accessibility.talkback.eventprocessor.PhoneticLetterMode;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.focusmanagement.FocusProcessorForTapAndTouchExploration;
import com.google.android.accessibility.talkback.focusmanagement.FocusProcessorForTapAndTouchExploration.TypingMethod;
import com.google.android.accessibility.talkback.gesture.GestureHints;
import com.google.android.accessibility.talkback.keyboard.KeyComboManager;
import com.google.android.accessibility.talkback.keyboard.KeyComboModel;
import com.google.android.accessibility.talkback.selector.SelectorController;
import com.google.android.accessibility.talkback.selector.SelectorController.Setting;
import com.google.android.accessibility.talkback.utils.TalkbackFeatureSupport;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.KeyboardUtils;
import com.google.android.accessibility.utils.Logger;
import com.google.android.accessibility.utils.TimedFlags;
import com.google.android.accessibility.utils.input.WindowsDelegate;
import com.google.android.accessibility.utils.monitor.CollectionState;
import com.google.android.accessibility.utils.monitor.InputModeTracker;
import com.google.android.accessibility.utils.output.SpeechControllerImpl;
import com.google.android.apps.common.proguard.UsedByReflection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Tracks the current global state for the parse tree. */
public class GlobalVariables extends TimedFlags implements ParseTree.VariableDelegate {
  private static final String TAG = "GlobalVariables";
  // Parameters used in join statement.
  private static final CharSequence EMPTY_STRING = "";

  public static final int EVENT_SKIP_FOCUS_PROCESSING_AFTER_GRANULARITY_MOVE = 1;
  public static final int EVENT_SKIP_FOCUS_PROCESSING_AFTER_CURSOR_CONTROL = 3;

  /**
   * Indicates that the system has moved the cursor after an edit field was focused, and we want to
   * avoid providing feedback because we're about to reset the cursor.
   */
  public static final int EVENT_SKIP_SELECTION_CHANGED_AFTER_FOCUSED = 9;

  /** Indicates that we've automatically snapped text selection state and don't want feedback. */
  public static final int EVENT_SKIP_SELECTION_CHANGED_AFTER_CURSOR_RESET = 10;

  /** Used to suppress focus announcement when refocusing after an IME has closed. */
  public static final int EVENT_SKIP_FOCUS_PROCESSING_AFTER_IME_CLOSED = 13;

  // Parse tree constants.
  private static final int ENUM_INPUT_MODE = 6002;
  private static final int ENUM_COLLECTION_SELECTION_MODE = 6003;
  private static final int ENUM_READING_MENU_SETTING = 6004;

  private static final int GLOBAL_IS_KEYBOARD_ACTIVE = 6001;
  private static final int GLOBAL_IS_SELECTION_MODE_ACTIVE = 6002;
  private static final int GLOBAL_INPUT_MODE = 6003;
  private static final int GLOBAL_USE_SINGLE_TAP = 6004;
  private static final int GLOBAL_LAST_TEXT_EDIT_IS_PASSWORD = 6008;
  private static final int GLOBAL_SPEAK_PASS_SERVICE_POLICY = 6009;
  private static final int GLOBAL_ENABLE_USAGE_HINT = 6011;
  private static final int GLOBAL_ADJUSTABLE_HINT = 6012;
  private static final int GLOBAL_INTERPRET_AS_ENTRY_KEY = 6013;
  static final int GLOBAL_CURRENT_READING_MENU = 6014;
  private static final int GLOBAL_HAS_READING_MENU_ACTION_SETTING = 6015;
  private static final int GLOBAL_SUPPORT_TEXT_SUGGESTION = 6016;

  private static final int COLLECTION_ITEM_TRANSITION = 6101;
  private static final int COLLECTION_TRANSITION = 6102;
  private static final int COLLECTION_SELECTION_MODE = 6116;

  private static final int WINDOWS_LAST_WINDOW_ID = 6200;
  private static final int WINDOWS_IS_SPLIT_SCREEN_MODE = 6201;
  private static final int WINDOWS_CURRENT_WINDOW_TITLE = 6202;

  private static final int FOCUS_IS_CURRENT_FOCUS_IN_SCROLLABLE_NODE = 6300;
  private static final int FOCUS_IS_LAST_FOCUS_IN_SCROLLABLE_NODE = 6301;
  public static final int FOCUS_IS_PAGE = 6302;

  private static final int KEY_COMBO_HAS_KEY_FOR_CLICK = 6400;
  private static final int KEY_COMBO_STRING_FOR_CLICK = 6401;
  private static final int KEY_COMBO_HAS_KEY_FOR_LONG_CLICK = 6402;
  private static final int KEY_COMBO_STRING_FOR_LONG_CLICK = 6403;
  private static final int KEY_COMBO_HAS_KEY_FOR_DOUBLE_CLICK = 6404;
  private static final int KEY_COMBO_STRING_FOR_DOUBLE_CLICK = 6405;

  private static final int MAGNIFICATION_STATE_CHANGED = 6500;

  private static final int GESTURE_STRING_FOR_NODE_ACTIONS = 6600;
  private static final int GESTURE_STRING_FOR_READING_MENU_NEXT_SETTING = 6601;
  private static final int GESTURE_STRING_FOR_READING_MENU_SELECTED_SETTING_NEXT_ACTION = 6602;

  // Verbosity
  private static final int VERBOSITY_SPEAK_ROLES = 10001;
  private static final int VERBOSITY_SPEAK_COLLECTION_INFO = 10002;
  private static final int VERBOSITY_DESCRIPTION_ORDER = 10003;
  private static final int VERBOSITY_SPEAK_ELEMENT_IDS = 10004;
  private static final int VERBOSITY_SPEAK_SYSTEM_WINDOW_TITLES = 10005;

  private final Context mContext;
  private final AccessibilityService mService;
  private final InputModeTracker inputModeTracker;
  private @Nullable KeyComboManager keyComboManager;
  private final CollectionState collectionState;
  private WindowsDelegate mWindowsDelegate;
  private @Nullable SelectorController selectorController;

  /** Stores the user preferred locale changed using language switcher. */
  private @Nullable Locale userPreferredLocale;

  private boolean mUseSingleTap = false;
  private boolean supportClickableLinks = false;

  private int mLastWindowId = -1;
  private int mCurrentWindowId = -1;
  private int currentDisplayId = Display.INVALID_DISPLAY;

  private MagnificationState magnificationState;

  private boolean mSelectionModeActive;
  private boolean mLastTextEditIsPassword;

  private boolean isCurrentFocusInScrollableNode = false;
  private boolean isLastFocusInScrollableNode = false;
  private boolean isFocusPage = false;
  @TypingMethod private int typingMethod = FocusProcessorForTapAndTouchExploration.LIFT_TO_TYPE;
  private boolean isDescribingSwipeTarget = false;

  private final @Nullable GestureShortcutProvider gestureShortcutProvider;

  private @Nullable NodeMenuProvider nodeMenuProvider;

  // Defaults to true to speak usage hint.
  private boolean usageHintEnabled = true;
  // It's enabled when [Say capital] is configured.
  private boolean sayCapital = false;
  // How capital letters are told apart, one of SpeechControllerImpl's CAPITAL_LETTERS_TYPE values.
  private int capitalLetterFeedback = SpeechControllerImpl.CAPITAL_LETTERS_TYPE_SPEAK_CAP;

  private String phoneticLetterMode = PhoneticLetterMode.AFTER_LETTER_DELAY;

  // Verbosity settings
  private boolean speakRoles = true;
  private boolean speakCollectionInfo = true;

  public static final String TABLE_HEADERS_BEFORE = "before";
  public static final String TABLE_HEADERS_AFTER = "after";
  public static final String TABLE_HEADERS_OFF = "off";

  private String tableColumnHeaders = TABLE_HEADERS_AFTER;
  private boolean speakTableRowColumnNumbers = true;

  // Control sounds: whether they are heard, and the ones heard or felt for focused controls.
  private boolean controlSoundsOn = false;
  private Set<Integer> controlSounds = Collections.emptySet();
  private Set<Integer> heardControlSounds = Collections.emptySet();
  private boolean speakControlSoundRoles = false;
  // The control sound playing for the focus being described, or 0.
  private int roleSoundOfFocus = 0;
  @DescriptionOrder private int descriptionOrder = DESC_ORDER_ROLE_NAME_STATE_POSITION;
  private boolean speakElementIds = false;
  private boolean countRepeatedSymbols = false;
  private boolean speakSystemWindowTitles = true;
  private boolean textChangeRateUnlimited = false;
  private final boolean enableMediaControlHintForCall;
  private final boolean enableShortAndLongDurationsForSpecificApps;
  private volatile boolean isDndEnabled;
  private volatile boolean speakNotifications = true;
  private final boolean deviceScreenNoTouch;

  public GlobalVariables(
      AccessibilityService service,
      InputModeTracker inputModeTracker,
      CollectionState collectionState,
      @Nullable GestureShortcutProvider gestureShortcutProvider) {
    mContext = service;
    mService = service;
    this.inputModeTracker = inputModeTracker;
    this.collectionState = collectionState;
    this.gestureShortcutProvider = gestureShortcutProvider;
    enableMediaControlHintForCall = FeatureFlagReader.enableMediaControlHintForCall(service);
    enableShortAndLongDurationsForSpecificApps =
        FeatureFlagReader.enableShortAndLongDurationsForSpecificApps(service);
    deviceScreenNoTouch =
        mContext.getResources().getConfiguration().touchscreen == Configuration.TOUCHSCREEN_NOTOUCH;
  }

  public void setWindowsDelegate(WindowsDelegate delegate) {
    mWindowsDelegate = delegate;
  }

  public void setKeyComboManager(KeyComboManager keyComboManager) {
    this.keyComboManager = keyComboManager;
  }

  public void setSelectorController(SelectorController selectorController) {
    this.selectorController = selectorController;
  }

  /**
   * Gets the preferred locale for feedback.
   *
   * <p>Note: Fallback to return locale for feedback in priority order.
   *
   * <ul>
   *   <li>1. user preferred locale,
   *   <li>2. node locale,
   * </ul>
   */
  public @Nullable Locale getPreferredLocaleByNode(AccessibilityNodeInfoCompat node) {
    if (userPreferredLocale != null) {
      return userPreferredLocale;
    }
    return AccessibilityNodeInfoUtils.getLocalesByNode(node);
  }

  /** Gets the user preferred locale changed using language switcher. */
  public @Nullable Locale getUserPreferredLocale() {
    return userPreferredLocale;
  }

  /** Sets the user preferred locale changed using language switcher. */
  public void setUserPreferredLocale(Locale locale) {
    userPreferredLocale = locale;
  }

  /** Declares {@link ParseTree} variables. */
  void declareVariables(ParseTree parseTree) {

    Map<Integer, String> collectionSelectionMode = new HashMap<>();
    collectionSelectionMode.put(CollectionState.SELECTION_NONE, "none");
    collectionSelectionMode.put(CollectionState.SELECTION_SINGLE, "single");
    collectionSelectionMode.put(CollectionState.SELECTION_MULTIPLE, "multiple");

    Map<Integer, String> inputMode = new HashMap<>();
    inputMode.put(INPUT_MODE_UNKNOWN, "unknown");
    inputMode.put(INPUT_MODE_TOUCH, "touch");
    inputMode.put(INPUT_MODE_KEYBOARD, "keyboard");
    inputMode.put(INPUT_MODE_TV_REMOTE, "tv_remote");
    inputMode.put(INPUT_MODE_NON_ALPHABETIC_KEYBOARD, "non_alphabetic_keyboard");

    Map<Integer, String> readingMenuSettings = new HashMap<>();
    for (Setting setting : Setting.values()) {
      readingMenuSettings.put(setting.ordinal(), setting.name());
    }

    parseTree.addEnum(ENUM_COLLECTION_SELECTION_MODE, collectionSelectionMode);
    parseTree.addEnum(ENUM_INPUT_MODE, inputMode);
    parseTree.addEnum(ENUM_READING_MENU_SETTING, readingMenuSettings);

    // Globals
    parseTree.addEnumVariable("global.inputMode", GLOBAL_INPUT_MODE, ENUM_INPUT_MODE);
    parseTree.addBooleanVariable("global.useSingleTap", GLOBAL_USE_SINGLE_TAP);
    parseTree.addBooleanVariable(
        "global.lastTextEditIsPassword", GLOBAL_LAST_TEXT_EDIT_IS_PASSWORD);
    parseTree.addBooleanVariable(
        "global.speakPasswordsServicePolicy", GLOBAL_SPEAK_PASS_SERVICE_POLICY);
    parseTree.addBooleanVariable("global.enableUsageHint", GLOBAL_ENABLE_USAGE_HINT);
    parseTree.addStringVariable("global.adjustableHint", GLOBAL_ADJUSTABLE_HINT);
    parseTree.addBooleanVariable("global.isInterpretAsEntryKey", GLOBAL_INTERPRET_AS_ENTRY_KEY);
    parseTree.addEnumVariable(
        "global.currentReadingMenu", GLOBAL_CURRENT_READING_MENU, ENUM_READING_MENU_SETTING);
    parseTree.addBooleanVariable(
        "global.hasReadingMenuActionsSetting", GLOBAL_HAS_READING_MENU_ACTION_SETTING);
    parseTree.addBooleanVariable("global.supportTextSuggestion", GLOBAL_SUPPORT_TEXT_SUGGESTION);

    // Collection
    parseTree.addStringVariable("collection.transition", COLLECTION_TRANSITION);
    parseTree.addStringVariable("collection.itemTransition", COLLECTION_ITEM_TRANSITION);
    parseTree.addEnumVariable(
        "collection.selectionMode", COLLECTION_SELECTION_MODE, ENUM_COLLECTION_SELECTION_MODE);

    parseTree.addBooleanVariable("windows.isSplitScreenMode", WINDOWS_IS_SPLIT_SCREEN_MODE);
    parseTree.addIntegerVariable("windows.lastWindowId", WINDOWS_LAST_WINDOW_ID);
    parseTree.addStringVariable("windows.currentWindowTitle", WINDOWS_CURRENT_WINDOW_TITLE);

    parseTree.addBooleanVariable(
        "focus.isCurrentFocusInScrollableNode", FOCUS_IS_CURRENT_FOCUS_IN_SCROLLABLE_NODE);
    parseTree.addBooleanVariable(
        "focus.isLastFocusInScrollableNode", FOCUS_IS_LAST_FOCUS_IN_SCROLLABLE_NODE);
    parseTree.addBooleanVariable("focus.isPage", FOCUS_IS_PAGE);

    parseTree.addBooleanVariable("keyCombo.hasKeyForClick", KEY_COMBO_HAS_KEY_FOR_CLICK);
    parseTree.addStringVariable(
        "keyCombo.stringRepresentationForClick", KEY_COMBO_STRING_FOR_CLICK);
    parseTree.addBooleanVariable(
        "keyCombo.hasKeyForDoubleClick", KEY_COMBO_HAS_KEY_FOR_DOUBLE_CLICK);
    parseTree.addStringVariable(
        "keyCombo.stringRepresentationForDoubleClick", KEY_COMBO_STRING_FOR_DOUBLE_CLICK);
    parseTree.addBooleanVariable("keyCombo.hasKeyForLongClick", KEY_COMBO_HAS_KEY_FOR_LONG_CLICK);
    parseTree.addStringVariable(
        "keyCombo.stringRepresentationForLongClick", KEY_COMBO_STRING_FOR_LONG_CLICK);

    parseTree.addStringVariable("magnification.stateChanged", MAGNIFICATION_STATE_CHANGED);

    parseTree.addStringVariable("gesture.nodeMenuShortcut", GESTURE_STRING_FOR_NODE_ACTIONS);
    parseTree.addStringVariable(
        "gesture.readingMenuNextSettingShortcut", GESTURE_STRING_FOR_READING_MENU_NEXT_SETTING);
    parseTree.addStringVariable(
        "gesture.readingMenuSelectedSettingNextActionShortcut",
        GESTURE_STRING_FOR_READING_MENU_SELECTED_SETTING_NEXT_ACTION);

    // Verbosity
    parseTree.addBooleanVariable("verbosity.speakRole", VERBOSITY_SPEAK_ROLES);
    parseTree.addBooleanVariable("verbosity.speakCollectionInfo", VERBOSITY_SPEAK_COLLECTION_INFO);
    parseTree.addBooleanVariable(
        "verbosity.speakSystemWindowTitles", VERBOSITY_SPEAK_SYSTEM_WINDOW_TITLES);
    parseTree.addEnumVariable(
        "verbosity.descriptionOrder",
        VERBOSITY_DESCRIPTION_ORDER,
        ENUM_VERBOSITY_DESCRIPTION_ORDER);
    parseTree.addBooleanVariable("verbosity.speakElementIds", VERBOSITY_SPEAK_ELEMENT_IDS);

    // Functions
    parseTree.addFunction("conditionalPrepend", this);
    parseTree.addFunction("conditionalAppend", this);
    parseTree.addFunction("round", this);
    parseTree.addFunction("roundForProgressPercent", this);
    parseTree.addFunction("roundForProgressInt", this);
    parseTree.addFunction("spelling", this);
    parseTree.addFunction("equals", this);
    parseTree.addFunction("dedupJoin", this);
    parseTree.addFunction("prependCapital", this);
  }

  /** Updates global variables state by the event. */
  public void updateStateFromEvent(AccessibilityEvent event) {
    int eventType = event.getEventType();
    if (eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
      final AccessibilityNodeInfoCompat sourceNode =
          AccessibilityEventUtils.sourceCompat(event);
      if (sourceNode != null) {
        updateStateFromFocusedNode(sourceNode);
        currentDisplayId = AccessibilityEventUtils.getDisplayId(event);
      }
    }
  }

  /** Updates the state that follows accessibility focus, for focus that moved to {@code node}. */
  public void updateStateFromFocusedNode(AccessibilityNodeInfoCompat node) {
    final AccessibilityNodeInfoCompat scrollableNode =
        AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(
            node, AccessibilityNodeInfoUtils.FILTER_SCROLLABLE);
    isLastFocusInScrollableNode = isCurrentFocusInScrollableNode;
    isCurrentFocusInScrollableNode = (scrollableNode != null);
    isFocusPage = AccessibilityNodeInfoUtils.isPage(node);

    mLastWindowId = mCurrentWindowId;
    mCurrentWindowId = node.getWindowId();
  }

  /** The state that follows accessibility focus, from {@link #saveFocusState()}. */
  public static final class SavedFocusState {
    private final boolean isLastFocusInScrollableNode;
    private final boolean isCurrentFocusInScrollableNode;
    private final boolean isFocusPage;
    private final int lastWindowId;
    private final int currentWindowId;
    private final CollectionState.SavedState collection;
    // The title of the container that focus is in, which the focus announcement both reads, to say
    // when focus moves into or out of a container, and changes.
    private final CharSequence containerTitle;

    private SavedFocusState(GlobalVariables variables) {
      isLastFocusInScrollableNode = variables.isLastFocusInScrollableNode;
      isCurrentFocusInScrollableNode = variables.isCurrentFocusInScrollableNode;
      isFocusPage = variables.isFocusPage;
      lastWindowId = variables.mLastWindowId;
      currentWindowId = variables.mCurrentWindowId;
      collection = variables.collectionState.save();
      containerTitle = EventTypeViewAccessibilityFocusedFeedbackRule.currentContainerTitle;
    }
  }

  /**
   * Returns the state that follows accessibility focus, including the collection state, so that
   * {@link #restoreFocusState} can put it back after working out what focusing a node would say.
   */
  public SavedFocusState saveFocusState() {
    return new SavedFocusState(this);
  }

  /** Puts back the state from {@link #saveFocusState()}. */
  public void restoreFocusState(SavedFocusState saved) {
    isLastFocusInScrollableNode = saved.isLastFocusInScrollableNode;
    isCurrentFocusInScrollableNode = saved.isCurrentFocusInScrollableNode;
    isFocusPage = saved.isFocusPage;
    mLastWindowId = saved.lastWindowId;
    mCurrentWindowId = saved.currentWindowId;
    collectionState.restore(saved.collection);
    EventTypeViewAccessibilityFocusedFeedbackRule.currentContainerTitle = saved.containerTitle;
  }

  /**
   * Updates the collection state for focus that moved to {@code node}, as the focus event for it
   * would.
   */
  public void updateCollectionStateFromFocusedNode(
      AccessibilityNodeInfoCompat node, AccessibilityEvent event) {
    collectionState.updateCollectionInformation(node, event);
  }

  /** Returns whether TalkBack should announce the media control hint to answer a call. */
  public boolean getEnableMediaControlHintForCall() {
    return enableMediaControlHintForCall;
  }

  /** Returns if short and long minimum durations for specific apps are enabled. */
  public boolean getEnableShortAndLongDurationsForSpecificApps() {
    return enableShortAndLongDurationsForSpecificApps;
  }

  /** Sets if DND do-not disturb enabled. Some feedback should be silenced for DND. */
  public void setIsDndEnabled(boolean enabled) {
    isDndEnabled = enabled;
  }

  /** Returns if DND do-not disturb is enabled. Some feedback should be silenced for DND. */
  public boolean isDndEnabled() {
    return isDndEnabled;
  }

  public void setSpeakNotifications(boolean speakNotifications) {
    this.speakNotifications = speakNotifications;
  }

  public boolean getSpeakNotifications() {
    return speakNotifications;
  }

  /** Returns if TalkBack usage hint is enabled. */
  public boolean getUsageHintEnabled() {
    return usageHintEnabled;
  }

  /** Sets if TalkBack usage hint is enabled. */
  public void setUsageHintEnabled(boolean enabled) {
    usageHintEnabled = enabled;
  }

  /** Returns if TalkBack speaks phonetic letters, with or instead of the letter. */
  public boolean getSpeakPhoneticLettersEnabled() {
    return !PhoneticLetterMode.OFF.equals(phoneticLetterMode);
  }

  /**
   * Returns if what is said takes the place of the letter: the letter and its word together, or the
   * word alone. Otherwise the word, if any, follows the letter after a pause.
   */
  public boolean getPhoneticLetterTakesPlaceOfLetter() {
    return PhoneticLetterMode.AFTER_LETTER_NO_DELAY.equals(phoneticLetterMode)
        || PhoneticLetterMode.INSTEAD_OF_LETTER.equals(phoneticLetterMode);
  }

  /** Returns if the phonetic word is said in place of the letter, without the letter. */
  public boolean getSpeakPhoneticLetterInsteadOfLetter() {
    return PhoneticLetterMode.INSTEAD_OF_LETTER.equals(phoneticLetterMode);
  }

  /** Sets how phonetic letters are spoken, one of the {@link PhoneticLetterMode} values. */
  public void setPhoneticLetterMode(String mode) {
    phoneticLetterMode = mode;
  }

  /** Returns if TalkBack speaks capital letter. */
  public boolean getGlobalSayCapital() {
    return sayCapital;
  }

  /** Sets if TalkBack speaks capital letter. */
  public void setGlobalSayCapital(boolean enabled) {
    sayCapital = enabled;
  }

  /** Returns how capital letters are told apart, one of the CAPITAL_LETTERS_TYPE values. */
  public int getCapitalLetterFeedback() {
    return capitalLetterFeedback;
  }

  public void setCapitalLetterFeedback(int feedback) {
    capitalLetterFeedback = feedback;
  }

  /** Returns if TalkBack uses single-tap gesture. */
  public boolean useSingleTap() {
    return mUseSingleTap;
  }

  /** Sets if TalkBack uses single-tap gesture. */
  public void setUseSingleTap(boolean value) {
    mUseSingleTap = value;
  }

  /** Returns if supporting clickable links. */
  public boolean supportClickableLinks() {
    return supportClickableLinks;
  }

  /** Sets if supporting clickable links. */
  public void setSupportClickableLinks(boolean value) {
    supportClickableLinks = value;
  }

  /** Returns the magnification state. If the state is not set before, the return value is null. */
  public @Nullable MagnificationState getMagnificationState() {
    return magnificationState;
  }

  /** Sets magnification state when handling magnification changed. */
  public void setMagnificationState(MagnificationState state) {
    magnificationState = state;
  }

  /**
   * Returns the selection mode from the collection state. And the collection state is from the
   * current focused node.
   *
   * <p>Note: the collection's selection mode is one of:
   *
   * <ul>
   *   <li>{@link CollectionInfoCompat#SELECTION_MODE_NONE}
   *   <li>{@link CollectionInfoCompat#SELECTION_MODE_SINGLE}
   *   <li>{@link CollectionInfoCompat#SELECTION_MODE_MULTIPLE}
   * </ul>
   */
  public int getCollectionSelectionMode() {
    return collectionState.getSelectionMode();
  }

  /** Returns if the selection mode is active. */
  public boolean getSelectionModeActive() {
    return mSelectionModeActive;
  }

  /** Sets the current state of selection mode. */
  public void setSelectionModeActive(boolean value) {
    mSelectionModeActive = value;
  }

  /** Sets if the last text edit is password. */
  public void setLastTextEditIsPassword(boolean value) {
    mLastTextEditIsPassword = value;
  }

  /** Returns the last text editing field is password or not. */
  public boolean getLastTextEditIsPassword() {
    return mLastTextEditIsPassword;
  }

  /** Returns and clears the state of skip-selection state flags. */
  public boolean resettingNodeCursor() {
    return checkAndClearRecentFlag(EVENT_SKIP_SELECTION_CHANGED_AFTER_FOCUSED)
        || checkAndClearRecentFlag(EVENT_SKIP_SELECTION_CHANGED_AFTER_CURSOR_RESET);
  }

  /** Returns whether a skip focus state flag is set, without clearing it. */
  public boolean hasSkipFocusProcessing() {
    return checkRecentFlag(EVENT_SKIP_FOCUS_PROCESSING_AFTER_GRANULARITY_MOVE)
        || checkRecentFlag(EVENT_SKIP_FOCUS_PROCESSING_AFTER_CURSOR_CONTROL)
        || checkRecentFlag(EVENT_SKIP_FOCUS_PROCESSING_AFTER_IME_CLOSED);
  }

  /** Returns and clears the state of skip focus state flags. */
  public boolean resettingSkipFocusProcessing() {
    return checkAndClearRecentFlag(EVENT_SKIP_FOCUS_PROCESSING_AFTER_GRANULARITY_MOVE)
        || checkAndClearRecentFlag(EVENT_SKIP_FOCUS_PROCESSING_AFTER_CURSOR_CONTROL)
        || checkAndClearRecentFlag(EVENT_SKIP_FOCUS_PROCESSING_AFTER_IME_CLOSED);
  }

  /** Sets the typing method, which the hint decision uses. */
  public void setTypingMethod(@TypingMethod int typingMethod) {
    this.typingMethod = typingMethod;
  }

  /**
   * Returns if it interprets all keys as entry keys. It is {@code true} when the typing method is
   * {@link FocusProcessorForTapAndTouchExploration#FORCE_LIFT_TO_TYPE_ON_IME} or {@link
   * FocusProcessorForTapAndTouchExploration#LIFT_TO_TYPE_EXCEPT_ACTION_KEY}.
   */
  public boolean isInterpretAsEntryKey() {
    return FocusProcessorForTapAndTouchExploration.liftsToTypeAnyKey(typingMethod);
  }

  /**
   * Returns whether {@code node} is a keyboard key that types on lift with a typing method that
   * interprets keyboard keys as entry keys, so it needs no hint to double-tap. The keyboard's
   * action key needs a double-tap with {@link
   * FocusProcessorForTapAndTouchExploration#LIFT_TO_TYPE_EXCEPT_ACTION_KEY}, so it isn't one.
   */
  public boolean liftsToType(@Nullable AccessibilityNodeInfoCompat node) {
    return isInterpretAsEntryKey()
        && AccessibilityNodeInfoUtils.isKeyboard(node)
        && FocusProcessorForTapAndTouchExploration.liftsToType(typingMethod, node);
  }

  /**
   * Sets whether the node being described is where a swipe moves focus, described before the swipe
   * has scrolled it into view. Its children that are off screen only because of where they are,
   * below or above the edge of the list, will then be on screen when it is spoken.
   */
  public void setDescribingSwipeTarget(boolean describingSwipeTarget) {
    isDescribingSwipeTarget = describingSwipeTarget;
  }

  /** Returns whether the node being described is a swipe's target; see setDescribingSwipeTarget. */
  public boolean isDescribingSwipeTarget() {
    return isDescribingSwipeTarget;
  }

  /** Returns if TalkBack speaks collection info. */
  public boolean getSpeakCollectionInfo() {
    return speakCollectionInfo;
  }

  /** Sets if TalkBack speaks collection info. */
  public void setSpeakCollectionInfo(boolean value) {
    speakCollectionInfo = value;
  }

  public String getTableColumnHeaders() {
    return tableColumnHeaders;
  }

  public void setTableColumnHeaders(String value) {
    tableColumnHeaders = value;
  }

  public void setSpeakTableRowColumnNumbers(boolean value) {
    speakTableRowColumnNumbers = value;
  }

  public boolean getSpeakRoles() {
    return speakRoles;
  }

  public void setSpeakRoles(boolean value) {
    speakRoles = value;
  }

  /**
   * Sets whether control sounds are heard, the ones that are heard or felt as vibrations, the ones
   * of those that are heard, and whether the kind of control is still spoken when its sound
   * plays.
   */
  public void setControlSounds(
      boolean on, Set<Integer> sounds, Set<Integer> heard, boolean speakRoles) {
    controlSoundsOn = on;
    controlSounds = sounds;
    heardControlSounds = heard;
    speakControlSoundRoles = speakRoles;
  }

  /**
   * Returns whether a control sound is heard. One that is only felt leaves the focus sound in
   * place, and plays its vibration in place of the focus vibration.
   */
  public boolean isControlSoundHeard(int sound) {
    return heardControlSounds.contains(sound);
  }

  /** Returns whether control sounds are on, so focus sounds come from where the focus is. */
  public boolean areControlSoundsOn() {
    return controlSoundsOn;
  }

  /** Returns the control sound to play for focusing {@code node}, or 0 if none plays. */
  public int getControlSoundForFocus(AccessibilityNodeInfoCompat node) {
    if (controlSounds.isEmpty()) {
      return 0;
    }
    int sound = ControlSounds.soundForFocus(node);
    return controlSounds.contains(sound) ? sound : 0;
  }

  /**
   * Sets the control sound playing for the focus whose speech is being composed, or 0 after it.
   * Controls of that kind do not have their kind spoken, unless the user asked for it.
   */
  public void setRoleSoundOfFocus(int sound) {
    roleSoundOfFocus = speakControlSoundRoles ? 0 : sound;
  }

  /** Returns whether the sound playing for the focus already tells what kind of control it is. */
  public boolean isRoleSaidBySound(AccessibilityNodeInfoCompat node) {
    return roleSoundOfFocus != 0 && ControlSounds.soundForRole(node) == roleSoundOfFocus;
  }

  /** Returns if TalkBack speaks system window titles. */
  public boolean getSpeakSystemWindowTitles() {
    return speakSystemWindowTitles;
  }

  /** Sets if TalkBack speaks system window titles. */
  public void setSpeakSystemWindowTitles(boolean value) {
    speakSystemWindowTitles = value;
  }

  /** Returns if TalkBack limits text change rate. */
  public boolean getTextChangeRateUnlimited() {
    return textChangeRateUnlimited;
  }

  /** Sets if TalkBack limits text change rate. */
  public void setTextChangeRateUnlimited(boolean value) {
    textChangeRateUnlimited = value;
  }

  /** Returns description order of TalkBack feedback. */
  public int getDescriptionOrder() {
    return descriptionOrder;
  }

  /** Sets description order of TalkBack feedback. */
  public void setDescriptionOrder(@DescriptionOrder int value) {
    descriptionOrder = value;
  }

  /** Returns if TalkBack speaks element IDs. */
  public boolean getSpeakElementIds() {
    return speakElementIds;
  }

  /** Sets if TalkBack speaks element IDs. */
  public void setSpeakElementIds(boolean value) {
    speakElementIds = value;
  }

  /** Returns if TalkBack speech counts repeated symbols. */
  public boolean getCountRepeatedSymbols() {
    return countRepeatedSymbols;
  }

  /** Sets if TalkBack speech counts repeated symbols. */
  public void setCountRepeatedSymbols(boolean value) {
    countRepeatedSymbols = value;
  }

  /** Returns if either soft or hard keyboard is active. */
  public boolean isKeyBoardActive() {
    return KeyboardUtils.isKeyboardActive(mService);
  }

  /** Returns if the current focus is in scrollable node. */
  public boolean currentFocusInScrollableNode() {
    return isCurrentFocusInScrollableNode;
  }

  /** Returns if the last focus is in scrollable node. */
  public boolean lastFocusInScrollableNode() {
    return isLastFocusInScrollableNode;
  }

  /** Returns if the current focus is on page. */
  public boolean focusIsPage() {
    return isFocusPage;
  }

  /** Returns if the device is in splitc screen mode. */
  public boolean isSplitScreenMode() {
    return mWindowsDelegate != null && mWindowsDelegate.isSplitScreenMode(currentDisplayId);
  }

  /** Returns the window ID of the last {@link TYPE_VIEW_ACCESSIBILITY_FOCUSED} event. */
  public int getLastWindowId() {
    return mLastWindowId;
  }

  /** Returns the window ID of the current source node. */
  public int getCurrentWindowId() {
    return mCurrentWindowId;
  }

  /** Returns {@code true} if device screen is not touch screen. */
  public boolean isDeviceScreenNoTouch() {
    return deviceScreenNoTouch;
  }

  /** Returns the hint for screen search with no keyword yet, naming the user's gesture. */
  public CharSequence getScreenSearchNoKeywordHint() {
    return gestureShortcutProvider != null
        ? gestureShortcutProvider.screenSearchNoKeywordHint()
        : mContext.getString(R.string.screen_search_no_keyword_hint_pre_r);
  }

  /** Returns the gesture string for the node actions in TalkBack menu. */
  public CharSequence getGestureStringForNodeActions() {
    if (inputModeTracker.getInputMode() == INPUT_MODE_KEYBOARD) {
      @Nullable CharSequence keyCombo =
          getKeyComboStringRepresentation(R.string.keycombo_shortcut_other_talkback_context_menu);
      if (!TextUtils.isEmpty(keyCombo)) {
        return keyCombo;
      }
    }
    return gestureInSentence(
        gestureShortcutProvider != null ? gestureShortcutProvider.nodeMenuShortcut() : null);
  }

  /** Returns the gesture string for the node actions in reading control. */
  public CharSequence getGestureStringForNodeActionsInReadingControl() {
    if (inputModeTracker.getInputMode() == INPUT_MODE_KEYBOARD) {
      @Nullable CharSequence keyCombo =
          getKeyComboStringRepresentation(
              R.string.keycombo_shortcut_global_adjust_reading_setting_next);
      if (!TextUtils.isEmpty(keyCombo)) {
        return keyCombo;
      }
    }

    if (gestureShortcutProvider == null) {
      return "";
    }

    CharSequence readingMenuUpShortcut = gestureShortcutProvider.readingMenuUpShortcut();
    CharSequence readingMenuDownShortcut = gestureShortcutProvider.readingMenuDownShortcut();
    return TextUtils.isEmpty(readingMenuUpShortcut)
        ? (TextUtils.isEmpty(readingMenuDownShortcut)
            ? ""
            : lowerCaseFirst(readingMenuDownShortcut))
        : (TextUtils.isEmpty(readingMenuDownShortcut)
            ? lowerCaseFirst(readingMenuUpShortcut)
            : mContext.getString(
                R.string.gesture_1_or_2,
                lowerCaseFirst(readingMenuUpShortcut),
                lowerCaseFirst(readingMenuDownShortcut)));
  }

  /** Returns the global input mode. */
  public int getGlobalInputMode() {
    return inputModeTracker.getInputMode();
  }

  public CharSequence getGlobalAdjustableHint() {
    CharSequence gestureReadingMenuUp =
        gestureShortcutProvider == null ? "" : gestureShortcutProvider.readingMenuUpShortcut();
    CharSequence gestureReadingMenuDown =
        gestureShortcutProvider == null ? "" : gestureShortcutProvider.readingMenuDownShortcut();
    if (!TextUtils.isEmpty(gestureReadingMenuUp) && !TextUtils.isEmpty(gestureReadingMenuDown)) {
      return mContext.getString(
          R.string.template_hint_adjustable_2gesture, gestureReadingMenuUp, gestureReadingMenuDown);
    } else if (TextUtils.isEmpty(gestureReadingMenuUp)
        && TextUtils.isEmpty(gestureReadingMenuDown)) {
      return FormFactorUtils.isAndroidWear()
          ? ""
          : mContext.getString(
              R.string.no_adjust_setting_gesture,
              lowerCaseFirst(mContext.getString(R.string.shortcut_selected_setting_next_action)));
    } else {
      return mContext.getString(
          R.string.template_hint_adjustable_1gesture,
          TextUtils.isEmpty(gestureReadingMenuUp) ? gestureReadingMenuDown : gestureReadingMenuUp);
    }
  }

  /**
   * Returns the hint for the Wear specific adjustable node which is input-focused and consumes RSB
   * input events.
   */
  public CharSequence getWearInputFocusedAdjustableHint() {
    return mContext.getString(R.string.template_hint_wear_input_focused_adjustable);
  }

  /** Returns the gesture string to select the next setting in reading menu. */
  public CharSequence getGestureStringForReadingMenuNextSetting() {
    if (inputModeTracker.getInputMode() == INPUT_MODE_KEYBOARD) {
      @Nullable CharSequence keyCombo =
          getKeyComboStringRepresentation(
              R.string.keycombo_shortcut_global_scroll_backward_reading_menu);
      if (!TextUtils.isEmpty(keyCombo)) {
        return keyCombo;
      }
    }
    return gestureShortcutProvider != null
        ? gestureShortcutProvider.readingMenuNextSettingShortcut()
        : "";
  }

  /** Returns the gesture string to perform the next action of selected setting in reading menu. */
  public CharSequence getGestureStringForReadingMenuSelectedSettingNextAction() {
    if (inputModeTracker.getInputMode() == INPUT_MODE_KEYBOARD) {
      @Nullable CharSequence keyCombo =
          getKeyComboStringRepresentation(
              R.string.keycombo_shortcut_global_adjust_reading_setting_next);
      if (!TextUtils.isEmpty(keyCombo)) {
        return keyCombo;
      }
    }
    return gestureShortcutProvider != null ? gestureShortcutProvider.readingMenuUpShortcut() : "";
  }

  /** Returns the gesture string to perform the next action of selected setting in reading menu. */
  public CharSequence getGestureStringForActionShortcut() {
    return gestureInSentence(
        gestureShortcutProvider != null ? gestureShortcutProvider.actionsShortcut() : null);
  }

  /** Returns {@code gesture} starting in lower case to go inside a sentence, or "" if it's null. */
  private CharSequence gestureInSentence(@Nullable CharSequence gesture) {
    return TextUtils.isEmpty(gesture) ? "" : lowerCaseFirst(gesture);
  }

  /** Lower-cases the first letter of a gesture or action name, to go inside a sentence. */
  private String lowerCaseFirst(CharSequence text) {
    return GestureHints.lowerCaseFirst(mContext, text.toString());
  }

  /** Returns the gesture string to get to the next window. */
  public CharSequence getGestureForNextWindowShortcut() {
    return gestureShortcutProvider != null ? gestureShortcutProvider.nextWindowShortcut() : "";
  }

  /** Returns the gesture string to play or pause media. */
  public CharSequence getGestureStringForMediaControlShortcut() {
    return gestureShortcutProvider != null ? gestureShortcutProvider.mediaControlShortcut() : "";
  }

  public CharSequence getKeyComboStringRepresentation(@StringRes int stringRes) {
    return KeyComboManagerUtils.getKeyComboStringRepresentation(
        mContext, keyComboManager, stringRes);
  }

  public long getKeyComboCodeForKey(@StringRes int stringRes) {
    return KeyComboManagerUtils.getKeyComboCodeForKey(mContext, keyComboManager, stringRes);
  }

  /**
   * Returns the collection role description when it is transitioned to a node inside a collection.
   */
  public CharSequence getCollectionTransitionDescription() {
    return CollectionStateFeedbackUtils.getCollectionTransitionDescription(
        collectionState, mContext);
  }

  /**
   * Returns the collection item description when the collection is transitioned.
   *
   * <p>Note: The collection item description is for {@link ROLE_GRID} and {@link ROLE_LIST}.
   */
  public CharSequence getCollectionItemTransitionDescription(
      @Nullable AccessibilityNodeInfoCompat focusedNode) {
    return CollectionStateFeedbackUtils.getCollectionItemTransitionDescription(
        focusedNode, collectionState, mContext, tableColumnHeaders, speakTableRowColumnNumbers);
  }

  public int getCollectionRole() {
    return collectionState.getCollectionRole();
  }

  /** Returns if the reading menu has actions settings. */
  public boolean hasReadingMenuActionSettings() {
    return selectorController != null && selectorController.isSettingAvailable(Setting.ACTIONS);
  }

  public boolean isReadingMenuActionCurrentSettings(Context context) {
    return SelectorController.getCurrentSetting(context) == Setting.ACTIONS;
  }

  public String getReadingMenuActionSettingsDescription() {
    return selectorController == null
        ? ""
        : selectorController.getSelectorActionSettingsDescription();
  }

  /** Returns the current reading menu ordinal. */
  public int getCurrentReadingMenuOrdinal() {
    Setting setting = SelectorController.getCurrentSetting(mContext);
    return setting == null ? -1 : setting.ordinal();
  }

  /** Sets the node menu provider. */
  public void setNodeMenuProvider(NodeMenuProvider nodeMenuProvider) {
    this.nodeMenuProvider = nodeMenuProvider;
  }

  /** Returns the node menu provider. */
  public @Nullable NodeMenuProvider getNodeMenuProvider() {
    return nodeMenuProvider;
  }

  @Override
  public boolean getBoolean(int variableId) {
    return switch (variableId) {
      case GLOBAL_IS_KEYBOARD_ACTIVE ->
          // Globals
          isKeyBoardActive();
      case GLOBAL_IS_SELECTION_MODE_ACTIVE -> getSelectionModeActive();
      case GLOBAL_SPEAK_PASS_SERVICE_POLICY ->
          true; // TODO: Has removed Speak password settings.
      case GLOBAL_USE_SINGLE_TAP -> useSingleTap();
      case GLOBAL_LAST_TEXT_EDIT_IS_PASSWORD -> getLastTextEditIsPassword();
      case GLOBAL_ENABLE_USAGE_HINT -> getUsageHintEnabled();
      case GLOBAL_HAS_READING_MENU_ACTION_SETTING ->
          selectorController != null && selectorController.isSettingAvailable(Setting.ACTIONS);
      case GLOBAL_SUPPORT_TEXT_SUGGESTION -> TalkbackFeatureSupport.supportTextSuggestion();
      // Windows
      case WINDOWS_IS_SPLIT_SCREEN_MODE -> isSplitScreenMode();
      // Focus
      case FOCUS_IS_CURRENT_FOCUS_IN_SCROLLABLE_NODE -> currentFocusInScrollableNode();
      case FOCUS_IS_LAST_FOCUS_IN_SCROLLABLE_NODE -> lastFocusInScrollableNode();
      case FOCUS_IS_PAGE -> focusIsPage();
      // KeyComboManager
      case KEY_COMBO_HAS_KEY_FOR_CLICK ->
          getKeyComboCodeForKey(R.string.keycombo_shortcut_perform_click)
              != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED;
      case KEY_COMBO_HAS_KEY_FOR_DOUBLE_CLICK ->
          getKeyComboCodeForKey(R.string.keycombo_shortcut_perform_double_click)
              != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED;
      case KEY_COMBO_HAS_KEY_FOR_LONG_CLICK ->
          getKeyComboCodeForKey(R.string.keycombo_shortcut_perform_long_click)
              != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED;
      case GLOBAL_INTERPRET_AS_ENTRY_KEY -> isInterpretAsEntryKey();
      // Verbosity
      case VERBOSITY_SPEAK_ROLES -> getSpeakRoles();
      case VERBOSITY_SPEAK_COLLECTION_INFO -> getSpeakCollectionInfo();
      case VERBOSITY_SPEAK_ELEMENT_IDS -> getSpeakElementIds();
      case VERBOSITY_SPEAK_SYSTEM_WINDOW_TITLES -> getSpeakSystemWindowTitles();
      default -> false;
    };
  }

  @Override
  public int getInteger(int variableId) {
    return switch (variableId) {
      case WINDOWS_LAST_WINDOW_ID -> getLastWindowId();
      default -> 0;
    };
  }

  @Override
  public double getNumber(int variableId) {
    return 0;
  }

  @Override
  public @Nullable CharSequence getString(int variableId) {
    return switch (variableId) {
      case COLLECTION_TRANSITION -> getCollectionTransitionDescription();
      case COLLECTION_ITEM_TRANSITION ->
          getCollectionItemTransitionDescription(/* focusedNode= */ null);
      case KEY_COMBO_STRING_FOR_CLICK ->
          getKeyComboStringRepresentation(R.string.keycombo_shortcut_perform_click);
      case KEY_COMBO_STRING_FOR_LONG_CLICK ->
          getKeyComboStringRepresentation(R.string.keycombo_shortcut_perform_long_click);
      case GESTURE_STRING_FOR_NODE_ACTIONS -> getGestureStringForNodeActions();
      case GESTURE_STRING_FOR_READING_MENU_NEXT_SETTING ->
          getGestureStringForReadingMenuNextSetting();
      case GESTURE_STRING_FOR_READING_MENU_SELECTED_SETTING_NEXT_ACTION ->
          getGestureStringForReadingMenuSelectedSettingNextAction();
      case GLOBAL_ADJUSTABLE_HINT -> getGlobalAdjustableHint();
      case MAGNIFICATION_STATE_CHANGED ->
          MagnificationStateChangedFeedbackRule.getMagnificationStateChangedText(
              mContext, magnificationState);
      case WINDOWS_CURRENT_WINDOW_TITLE -> getWindowTitle(mCurrentWindowId);
      default -> "";
    };
  }

  @Override
  public int getEnum(int variableId) {
    return switch (variableId) {
      case GLOBAL_INPUT_MODE -> getGlobalInputMode();
      case VERBOSITY_DESCRIPTION_ORDER -> getDescriptionOrder();
      case COLLECTION_SELECTION_MODE -> getCollectionSelectionMode();
      case GLOBAL_CURRENT_READING_MENU -> getCurrentReadingMenuOrdinal();
      default -> 0;
    };
  }

  @Override
  public @Nullable VariableDelegate getReference(int variableId) {
    return null;
  }

  @Override
  public int getArrayLength(int variableId) {
    return 0;
  }

  @Override
  public @Nullable CharSequence getArrayStringElement(int variableId, int index) {
    return "";
  }

  @Override
  public @Nullable VariableDelegate getArrayChildElement(int variableId, int index) {
    return null;
  }

  /** Returns the window tile by the given window ID. */
  public CharSequence getWindowTitle(int windowId) {
    if (mWindowsDelegate == null) {
      return EMPTY_STRING;
    }

    CharSequence title = mWindowsDelegate.getWindowTitle(windowId);
    return title != null ? title : EMPTY_STRING;
  }

  public void dump(Logger dumpLogger) {
    dumpLogger.log(
        new StringBuilder()
            .append("Compositor state:\n ")
            .append(String.format("inputMode=%s, ", getGlobalInputMode()))
            .append(String.format("isDeviceScreenNoTouch=%s, ", isDeviceScreenNoTouch()))
            .append(String.format("sayCapital=%s, ", getGlobalSayCapital()))
            .append(String.format("speakCollectionInfo=%s, ", getSpeakCollectionInfo()))
            .append(String.format("speakElementIds=%s, ", getSpeakElementIds()))
            .append(String.format("speakRoles=%s, ", getSpeakRoles()))
            .append(String.format("speakSysWindowTitles=%s, ", getSpeakSystemWindowTitles()))
            .append(String.format("textChangeRateUnlimited=%s, ", getTextChangeRateUnlimited()))
            .append(String.format("dndEnabled=%s, ", isDndEnabled()))
            .append(String.format("speakNotifications=%s, ", getSpeakNotifications()))
            .append(String.format("usageHintEnabled=%s, ", getUsageHintEnabled()))
            .append(
                String.format(
                    "enableMediaControlHintForCall=%s, ", getEnableMediaControlHintForCall()))
            .append(
                String.format(
                    "enableShortAndLongDurationsForSpecificApps=%s, ",
                    getEnableShortAndLongDurationsForSpecificApps()))
            .append(String.format("supportClickableLinks=%s, ", supportClickableLinks()))
            .toString());

    dumpLogger.log(collectionState.toString());
    CollectionState.ListItemState listItemState = collectionState.getListItemState();
    if (listItemState != null) {
      dumpLogger.log(listItemState.toString());
    }
    CollectionState.PagerItemState pagerItemState = collectionState.getPagerItemState();
    if (pagerItemState != null) {
      dumpLogger.log(pagerItemState.toString());
    }
    CollectionState.TableItemState tableItemState = collectionState.getTableItemState();
    if (tableItemState != null) {
      dumpLogger.log(tableItemState.toString());
    }
    dumpLogger.log("");
  }

  ///////////////////////////////////////////////////////////////////////////////////////////
  // Functions callable from compositor script.

  // TODO: Replace the reflection method
  @UsedByReflection("compositor.json")
  private static CharSequence conditionalAppend(
      CharSequence conditionalText, CharSequence appendText) {
    return CompositorUtils.conditionalAppend(
        conditionalText, appendText, CompositorUtils.getSeparator());
  }

  // TODO: Replace the reflection method
  @UsedByReflection("compositor.json")
  private static CharSequence conditionalPrepend(
      CharSequence prependText, CharSequence conditionalText) {
    return CompositorUtils.conditionalPrepend(
        prependText, conditionalText, CompositorUtils.getSeparator());
  }

  @UsedByReflection("compositor.json")
  private static CharSequence dedupJoin(
      CharSequence value1, CharSequence value2, CharSequence value3) {
    return CompositorUtils.dedupJoin(value1, value2, value3);
  }

  @UsedByReflection("compositor.json")
  private CharSequence spelling(CharSequence word) {
    return InputTextFeedbackRules.spelling(word, mContext);
  }

  @UsedByReflection("compositor.json")
  private static int round(double value) {
    return (int) Math.round(value);
  }

  @UsedByReflection("compositor.json")
  private static int roundForProgressPercent(double value) {
    return AccessibilityNodeInfoUtils.roundForProgressPercent(value);
  }

  @UsedByReflection("compositor.json")
  private static int roundForProgressInt(double value) {
    return (int) (value);
  }

  @UsedByReflection("compositor.json")
  private CharSequence prependCapital(CharSequence s) {
    return sayCapital ? CompositorUtils.prependCapital(s, mContext) : s;
  }

  @UsedByReflection("compositor.json")
  private static boolean equals(CharSequence text1, CharSequence text2) {
    return TextUtils.equals(text1, text2);
  }
}
