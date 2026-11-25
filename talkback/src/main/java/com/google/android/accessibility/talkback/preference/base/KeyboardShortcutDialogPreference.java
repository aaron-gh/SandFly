/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.android.accessibility.talkback.preference.base;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.commands.Commands;
import com.google.android.accessibility.talkback.keyboard.KeyCombo;
import com.google.android.accessibility.talkback.keyboard.KeyComboManager;
import com.google.android.accessibility.talkback.keyboard.KeyComboModel;
import com.google.android.accessibility.talkback.keyboard.KeyPresses;
import com.google.android.accessibility.talkback.keyboard.NewKeyComboModel;
import com.google.android.accessibility.talkback.keyboard.SandFlyKey;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.ServiceKeyEventListener;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import com.google.android.accessibility.utils.material.MaterialComponentUtils;
import com.google.common.collect.ImmutableSet;
import java.util.function.Consumer;

/**
 * A {@link Preference} which contains two dialogs, setUpKeyComboDialog and keyAlreadyInUseDialog.
 * SetUpKeyComboDialog is for all keyboard combo assigned key, it provides a customized dialog for
 * combo assigned key setting. KeyAlreadyInUseDialog is for warning users that the input key
 * combination is already in use.
 */
public class KeyboardShortcutDialogPreference extends Preference
    implements DialogInterface.OnKeyListener, ServiceKeyEventListener {
  /** The longest time between the SandFly key's two presses that records it pressed twice. */
  private static final long SANDFLY_KEY_TWICE_MS = 500;
  // No Enter press that saved is waiting for its key up.
  private static final long NO_SAVING_ENTER = -1;


  private static final int KEY_EVENT_SOURCE_ACTIVITY = 0;
  private static final int KEY_EVENT_SOURCE_ACCESSIBILITY_SERVICE = 1;
  private static final String ANDROID_SCHEME = "http://schemas.android.com/apk/res/android";
  private static final String SUMMARY_ATTRIBUTE = "summary";

  private KeyComboManager keyComboManager;
  private int keyEventSource = KEY_EVENT_SOURCE_ACTIVITY;
  private KeyCombo tempKeyCombo =
      new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          KeyComboModel.NO_PREFIX_KEY_CODE,
          KeyComboModel.KEY_COMBO_CODE_UNASSIGNED,
          /* triggerModifierUsed= */ false);

  private A11yAlertDialogWrapper keyAlreadyInUseDialog;
  private Dialog setUpKeyComboDialog;
  private TextView keyAssignmentView;
  private TextView errorTextView;
  private TextView instructionText;
  private boolean supportedSequenceKeyInfra;
  private boolean triggerModifierUp;
  // Whether onKeyEventInternal consumes the key event that processKeyEvent doesn't process.
  private boolean consumeKeyEvent;
  // The down time of the Enter press that saved, so only that press's key up is consumed too. The
  // key up may go elsewhere once the dialog closes or the "Key already in use" dialog opens.
  private long savingEnterDownTime = NO_SAVING_ENTER;
  // When the SandFly key alone was last pressed in the dialog, to record it pressed twice.
  private long sandFlyKeyDownTime;
  private String summary;

  /** Defines the state of the sequence key mode. */
  @IntDef({STATE_PREFIX_KEY_NOT_PRESSED, STATE_PREFIX_KEY_PRESSED})
  @Retention(RetentionPolicy.SOURCE)
  public @interface SequenceKeyModeState {}

  private static final int STATE_PREFIX_KEY_NOT_PRESSED = 0;
  private static final int STATE_PREFIX_KEY_PRESSED = 1;
  @SequenceKeyModeState private int currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
  private static final ImmutableSet<Integer> INVALID_SEQUENCE_KEYCOMBO_FINAL_KEYS =
      ImmutableSet.of(KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_ENTER);

  public KeyboardShortcutDialogPreference(
      Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(attrs);
  }

  public KeyboardShortcutDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(attrs);
  }

  public KeyboardShortcutDialogPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
  }

  public KeyboardShortcutDialogPreference(Context context) {
    super(context);
    init(null);
  }

  /** Creates the preference for the keyboard shortcut kept under {@code key}. */
  public KeyboardShortcutDialogPreference(Context context, String key, CharSequence title) {
    super(context);
    setKey(key);
    setTitle(title);
    init(null);
  }

  /** Returns the name of the command whose keyboard shortcut is kept under {@code key}. */
  private CharSequence titleOf(String key) {
    Preference preference =
        getPreferenceManager() == null ? null : getPreferenceManager().findPreference(key);
    if (preference != null && preference.getTitle() != null) {
      return preference.getTitle();
    }
    CharSequence title = Commands.labelOfKeyboardKey(getContext(), key);
    return title == null ? key : title;
  }

  static KeyComboManager getKeyComboManager(Context context) {
    KeyComboManager keyComboManager;
    if (TalkBackService.getInstance() != null) {
      keyComboManager = TalkBackService.getInstance().getKeyComboManager();
    } else {
      keyComboManager = KeyComboManager.create(context);
    }

    return keyComboManager;
  }

  private void init(@Nullable AttributeSet attrs) {
    if (attrs != null) {
      int summaryResId = attrs.getAttributeResourceValue(ANDROID_SCHEME, SUMMARY_ATTRIBUTE, 0);
      if (summaryResId != 0) {
        summary = getContext().getString(summaryResId);
      }
    }
    setPersistent(true);
    updateKeyComboManager();
    supportedSequenceKeyInfra = keyComboManager.isSequenceKeyInfraSupported();
    setTemporaryKeyCombo(getKeyComboForKey(getKey()));
  }

  @Nullable private Runnable keyComboChangeListener;

  // Takes the keys recorded in place of the keymap, for keyboard shortcuts kept elsewhere.
  @Nullable private Consumer<KeyCombo> recordListener;

  /** Whether the shortcut being set works only while a text field is being edited. */
  private boolean whileEditing;

  @Nullable private CheckBox whileEditingBox;
  /** Whether the shortcut being set works only in browse mode, in the keyboard phone keymaps. */
  private boolean inBrowseMode;
  @Nullable private CheckBox inBrowseModeBox;

  /**
   * Makes the dialog give the keys recorded to {@code listener}, which keeps them, instead of
   * saving them in the keymap as the shortcut of this preference's key.
   */
  public void setRecordListener(@Nullable Consumer<KeyCombo> listener) {
    recordListener = listener;
  }

  /** Shows the dialog that records keys. */
  public void showRecordDialog() {
    showSetUpKeyComboDialog();
  }

  /** Sets what runs after this or another shortcut changes through this preference's dialogs. */
  public void setKeyComboChangeListener(@Nullable Runnable listener) {
    keyComboChangeListener = listener;
  }

  public void updateKeyComboManager() {
    keyComboManager = getKeyComboManager(getContext());
    throwExceptionIfKeyComboManagerIsNull();
    supportedSequenceKeyInfra = keyComboManager.isSequenceKeyInfraSupported();
  }

  public void onTriggerModifierChanged() {
    setTemporaryKeyCombo(getKeyComboForKey(getKey()));

    // Ensures it's updated through the preference framework without caching any class members.
    notifyChanged();
  }

  public void setKeyCombo(KeyCombo keyCombo) {
    setTemporaryKeyCombo(keyCombo);
  }

  @Override
  public void setSummary(@Nullable CharSequence summary) {
    super.setSummary(summary);
    if (TextUtils.equals(summary, this.summary)) {
      return;
    }
    this.summary = (summary == null) ? "" : summary.toString();
  }

  @Override
  public CharSequence getSummary() {
    return TextUtils.isEmpty(summary)
        ? keyComboManager.getKeyComboStringRepresentation(this.tempKeyCombo)
        : summary;
  }

  @Override
  public void notifyChanged() {
    super.notifyChanged();
  }

  @Override
  protected void onClick() {
    super.onClick();
    showSetUpKeyComboDialog();
  }

  @Override
  protected void onPrepareForRemoval() {
    super.onPrepareForRemoval();
  }

  // Presses being recorded, for keymaps that take presses.
  private static final long RECORD_TWICE_MS = 500;
  private final android.os.Handler pressHandler =
      new android.os.Handler(android.os.Looper.getMainLooper());
  private final java.util.Set<Integer> pressKeysDown = new java.util.HashSet<>();
  private int pressModifiers;
  private boolean pressOnlyModifiers = true;
  private boolean pressHeld;
  private @Nullable KeyCombo lastRecorded;
  private long lastRecordedTime;
  private @Nullable Runnable pendingEnterSave;
  private final Runnable pressHoldCheck = this::onPressHeld;
  // A key other than a modifier being recorded, which held down records the key held.
  private @Nullable KeyCombo pressKeyHeld;
  private int pressKeyHeldCode;
  private final Runnable pressKeyHoldCheck =
      () -> {
        if (pressKeyHeld != null && pressKeysDown.contains(pressKeyHeldCode)) {
          record(
              pressKeyHeld.withPressType(KeyCombo.HELD_DOWN),
              /* allowTwice= */ false,
              android.os.SystemClock.uptimeMillis());
        }
      };

  /** Clears current temporary key combo code. */
  private void clearTemporaryKeyCombo() {
    tempKeyCombo =
        new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            KeyComboModel.NO_PREFIX_KEY_CODE,
            KeyComboModel.KEY_COMBO_CODE_UNASSIGNED,
            /* triggerModifierUsed= */ false);
  }

  /** Sets a temporary KeyCombo object. */
  private void setTemporaryKeyCombo(long keyComboCode) {
    // Create a KeyCombo object without trigger modifier to extract modifiers, prefix key code,
    // and key code from the given `keyComboCode`. Note that `triggerModifierUsed` in this object
    // won't be used and must not be used in this function.
    KeyCombo keyCombo = new KeyCombo(keyComboCode, /* triggerModifierUsed= */ false);
    int triggerModifier = getTriggerModifier();
    // The SandFly key is used only when all of it is pressed: Ctrl alone isn't Ctrl + Alt.
    boolean triggerModifierUsed =
        triggerModifier != KeyComboModel.NO_MODIFIER
            && (keyCombo.getModifiers() & triggerModifier) == triggerModifier;
    this.tempKeyCombo =
        new KeyCombo(
            keyCombo.getModifiers() & ~(triggerModifierUsed ? triggerModifier : 0),
            keyCombo.getPrefixKeyCode(),
            keyCombo.getKeyCode(),
            triggerModifierUsed);
    applyWhileEditing();
  }

  /** Sets temporary key combo. */
  private void setTemporaryKeyCombo(KeyCombo keyCombo) {
    this.tempKeyCombo = keyCombo;
    applyWhileEditing();
  }

  /**
   * Marks the shortcut being set as working only while editing, or only in browse mode, as the
   * check boxes say.
   */
  private void applyWhileEditing() {
    if (tempKeyCombo.getKeyComboCode() != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED) {
      tempKeyCombo = tempKeyCombo.withWhileEditing(whileEditing).withInBrowseMode(inBrowseMode);
    }
  }

  private int getKeyEventSourceForCurrentKeyComboModel() {
    int triggerModifier = getTriggerModifier();

    if (triggerModifier == KeyComboModel.NO_MODIFIER) {
      return KEY_EVENT_SOURCE_ACTIVITY;
    } else {
      return KEY_EVENT_SOURCE_ACCESSIBILITY_SERVICE;
    }
  }

  private int getTriggerModifier() {
    return keyComboManager.getKeyComboModel().getTriggerModifier();
  }

  /** Returns whether [event] has no modifiers, a held SandFly key such as Insert among them. */
  private boolean hasNoModifiers(KeyEvent event) {
    return keyComboManager.modifiersOf(event) == KeyComboModel.NO_MODIFIER;
  }

  /** Returns whether [event] releases the SandFly key, or a key of it. */
  private boolean isSandFlyKeyRelease(KeyEvent event) {
    if (event.getAction() != KeyEvent.ACTION_UP) {
      return false;
    }
    @Nullable SandFlyKey sandFlyKey = keyComboManager.getSandFlyKey();
    if (sandFlyKey == null) {
      return false;
    }
    return sandFlyKey.hasKey(event.getKeyCode());
  }

  private void setKeyEventSource(int keyEventSource) {
    if (this.keyEventSource == keyEventSource) {
      return;
    }

    this.keyEventSource = keyEventSource;

    if (keyEventSource == KEY_EVENT_SOURCE_ACCESSIBILITY_SERVICE) {
      keyComboManager.setKeyEventDelegate(this);
    } else {
      keyComboManager.setKeyEventDelegate(null);
    }
  }

  /** Handles key combo when fragment closes. */
  private void onSetUpKeyComboDialogClosed() {
    setTemporaryKeyCombo(getKeyComboForKey(getKey()));
    pressHandler.removeCallbacksAndMessages(null);
    pendingEnterSave = null;
    pressKeysDown.clear();
    lastRecorded = null;

    keyComboManager.setMatchKeyCombo(true);
    setKeyEventSource(KEY_EVENT_SOURCE_ACTIVITY);
  }

  /**
   * Registers the key event listener to receive key event.
   *
   * @param dialog Dialog receives key event.
   */
  private void registerDialogKeyEvent(Dialog dialog) {
    if (dialog == null) {
      return;
    }

    dialog.setOnKeyListener(this);
    setKeyEventSource(getKeyEventSourceForCurrentKeyComboModel());
  }

  @Override
  public boolean onKeyEvent(KeyEvent event, EventId eventId) {
    if (keyEventSource != KEY_EVENT_SOURCE_ACCESSIBILITY_SERVICE) {
      return false;
    }
    // A dialog that closed without letting go gives the keys back.
    if (setUpKeyComboDialog == null || !setUpKeyComboDialog.isShowing()) {
      setKeyEventSource(KEY_EVENT_SOURCE_ACTIVITY);
      keyComboManager.setMatchKeyCombo(true);
      return false;
    }

    return onKeyEventInternal(event);
  }

  @Override
  public boolean processWhenServiceSuspended() {
    return true;
  }

  @Override
  public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
    if (keyEventSource != KEY_EVENT_SOURCE_ACTIVITY) {
      return false;
    }

    return onKeyEventInternal(event);
  }

  private boolean onKeyEventInternal(KeyEvent event) {
    if (recordsPresses()) {
      return recordPress(event);
    }
    if (!processKeyEvent(event)) {
      boolean consume = consumeKeyEvent;
      consumeKeyEvent = false;
      return consume;
    }

    // The SandFly key pressed twice, alone, turns browse mode on and off.
    @Nullable SandFlyKey sandFlyKey = keyComboManager.getSandFlyKey();
    // Key downs only: the release of the first press isn't a second press. Meta and Alt count too,
    // which are modifiers rather than held keys.
    if (canTakeSandFlyKeyPressedTwice()
        && sandFlyKey != null
        && sandFlyKey.getCanBePressedTwice()
        && event.getAction() == KeyEvent.ACTION_DOWN
        && (sandFlyKey.isHeldKey()
            ? sandFlyKey.isKey(event)
            : sandFlyKey.hasKey(event.getKeyCode()))
        && event.getRepeatCount() == 0) {
      if (event.getEventTime() - sandFlyKeyDownTime < SANDFLY_KEY_TWICE_MS) {
        sandFlyKeyDownTime = 0;
        this.tempKeyCombo = KeyCombo.sandFlyKeyPressedTwice();
        if (supportedSequenceKeyInfra) {
          currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
        }
        updateKeyAssignmentText();
        return true;
      }
      sandFlyKeyDownTime = event.getEventTime();
    } else if (event.getAction() == KeyEvent.ACTION_DOWN) {
      sandFlyKeyDownTime = 0;
    }

    if (event.getKeyCode() == KeyEvent.KEYCODE_DEL && hasNoModifiers(event)) {
      clearTemporaryKeyCombo();
      if (supportedSequenceKeyInfra) {
        currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
      }
      updateKeyAssignmentText();
      return true;
    }

    if (supportedSequenceKeyInfra
        && keyComboManager.modifiersOf(event) == KeyEvent.META_CTRL_ON
        && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
      this.tempKeyCombo = keyComboManager.getKeyComboModel().getDefaultKeyCombo(getKey());
      // The default may work only while editing, or everywhere.
      if (whileEditingBox != null) {
        whileEditingBox.setChecked(tempKeyCombo.isWhileEditing());
      }
      if (inBrowseModeBox != null) {
        inBrowseModeBox.setChecked(tempKeyCombo.isInBrowseMode());
      }
      currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
      updateKeyAssignmentText();
      return true;
    }

    if (supportedSequenceKeyInfra) {
      int currentKeyCode = event.getKeyCode();
      int eventEffectiveModifiers = keyComboManager.modifiersOf(event);
      int triggerModifier = keyComboManager.getKeyComboModel().getTriggerModifier();
      if (currentSequenceKeyState == STATE_PREFIX_KEY_NOT_PRESSED) {
        boolean onlyTriggerModifierPressed = eventEffectiveModifiers == triggerModifier;
        boolean pressedPrefixKeyCombo =
            onlyTriggerModifierPressed
                && KeyCombo.SEQUENCE_KEY_COMBO_CODE_PREFIX_KEYS.contains(currentKeyCode);

        if (pressedPrefixKeyCombo) {
          this.tempKeyCombo =
              new KeyCombo(
                  KeyComboModel.NO_MODIFIER,
                  currentKeyCode,
                  KeyComboModel.KEY_COMBO_CODE_UNASSIGNED,
                  /* triggerModifierUsed= */ true);
          currentSequenceKeyState = STATE_PREFIX_KEY_PRESSED;
          triggerModifierUp = false;
        } else {
          setTemporaryKeyCombo(keyComboManager.getKeyComboCodeWithSandFlyKey(event));
        }
        updateKeyAssignmentText();
        return true;
      } else if (currentSequenceKeyState == STATE_PREFIX_KEY_PRESSED) {
        if (currentKeyCode == KeyEvent.KEYCODE_UNKNOWN) {
          clearTemporaryKeyCombo();
          currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
          updateKeyAssignmentText();
          return true;
        }

        if (KeyEvent.isModifierKey(currentKeyCode)) {
          showError(
              errorTextView,
              getContext()
                  .getString(
                      R.string.invalid_modifier_key_as_third_key_message,
                      keyComboManager.getTriggerKeyUserFacingName(
                          keyComboManager.getKeyComboModel().getTriggerModifier())));
          return true;
        }

        boolean triggerIsActive = eventEffectiveModifiers == triggerModifier;
        // The combined condition for completing a sequence key assignment.
        boolean isCompletingWithoutTriggerModifier =
            eventEffectiveModifiers == KeyComboModel.NO_MODIFIER && triggerModifierUp;
        boolean isCompletingWithTriggerModifierHeld =
            triggerIsActive
                && !triggerModifierUp
                && this.tempKeyCombo.getPrefixKeyCode() != KeyComboModel.NO_PREFIX_KEY_CODE;

        if (KeyCombo.SEQUENCE_KEY_COMBO_CODE_PREFIX_KEYS.contains(currentKeyCode)
            && triggerIsActive
            && triggerModifierUp) {
          // Scenario 1: Restart sequence with a new prefix key (e.g., Action+E -> Action+R).
          this.tempKeyCombo =
              new KeyCombo(
                  KeyComboModel.NO_MODIFIER,
                  currentKeyCode,
                  KeyComboModel.KEY_COMBO_CODE_UNASSIGNED,
                  /* triggerModifierUsed= */ true);
          triggerModifierUp = false;
        } else if (isCompletingWithoutTriggerModifier || isCompletingWithTriggerModifierHeld) {
          // Scenarios 2 & 3 Combined: Complete the sequence key assignment.
          this.tempKeyCombo =
              new KeyCombo(
                  KeyComboModel.NO_MODIFIER,
                  this.tempKeyCombo.getPrefixKeyCode(),
                  currentKeyCode,
                  /* triggerModifierUsed= */ true);
          currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
        } else {
          // Scenario 4: Abort the sequence key assignment and treat the keyevent as a normal key
          // combo.
          setTemporaryKeyCombo(keyComboManager.getKeyComboCodeWithSandFlyKey(event));
          currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
        }
        updateKeyAssignmentText();
      }
    } else {
      setTemporaryKeyCombo(keyComboManager.getKeyComboCodeWithSandFlyKey(event));
      updateKeyAssignmentText();
    }

    return true;
  }

  private boolean processKeyEvent(KeyEvent event) {
    if (event == null) {
      return false;
    }

    if (event.getRepeatCount() > 1) {
      return false;
    }

    // Check if a key up event is trigger modifier.
    if (isSandFlyKeyRelease(event)) {
      triggerModifierUp = true;
    }

    //noinspection SimplifiableIfStatement
    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
        || event.getKeyCode() == KeyEvent.KEYCODE_HOME
        || event.getKeyCode() == KeyEvent.KEYCODE_CALL
        || event.getKeyCode() == KeyEvent.KEYCODE_ENDCALL) {
      return false;
    }

    // Let the system handle the shift + tab key event and the tab key event when sequence key infra
    // is supported, unless a held SandFly key such as Insert makes it a shortcut.
    if (supportedSequenceKeyInfra
        && (keyComboManager.modifiersOf(event) & KeyComboModel.HELD_KEY_MODIFIER) == 0) {
      if (event.getKeyCode() == KeyEvent.KEYCODE_TAB
          && (event.getModifiers() & KeyEvent.META_SHIFT_ON) != 0) {
        return false;
      }
      if (event.getKeyCode() == KeyEvent.KEYCODE_TAB) {
        return false;
      }
    }

    // Uses Enter key to confirm the key combo change. If keyAlreadyInUseDialog shows up, it will
    // not process the button click function since onKey() will receive enter key twice.
    // keyAlreadyInUseDialog will be null when this dialog dismisses. If the sequence key infra
    // is supported, it won't save invalid key combo and only save valid key combo when the dialog
    // view is accessibility focused and the event action is ACTION_DOWN.
    // Enter that saves is consumed, down and up, so it doesn't also press the focused button,
    // such as Clear shortcut under the "Key already in use" dialog.
    if (hasNoModifiers(event) && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
      if (event.getAction() == KeyEvent.ACTION_UP) {
        consumeKeyEvent = event.getDownTime() == savingEnterDownTime;
        if (consumeKeyEvent) {
          savingEnterDownTime = NO_SAVING_ENTER;
        }
        return false;
      }
      if (keyAlreadyInUseDialog == null) {
        LinearLayout dialogView = setUpKeyComboDialog.findViewById(R.id.dialog_view);
        boolean enterPressedInDialogView =
            dialogView != null && dialogView.isAccessibilityFocused();
        if (supportedSequenceKeyInfra && !enterPressedInDialogView) {
          return false;
        }
        consumeKeyEvent = true;
        savingEnterDownTime = event.getDownTime();
        if (shouldAnnounceInvalidShortcutMessage()) {
          return false;
        }
        processSaveButtonClickListener();
        return false;
      }
    }

    // Enter and Esc are used to accept/dismiss dialogs. However, the default shortcuts
    // involve Enter and Esc (with modifiers), so we should only trap Enter and Esc without
    // modifiers.
    boolean isDialogNavigation =
        event.getKeyCode() == KeyEvent.KEYCODE_ENTER
            || event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE;
    if (isDialogNavigation && hasNoModifiers(event)) {
      return false;
    }

    return event.getAction() == KeyEvent.ACTION_DOWN;
  }

  /**
   * Whether the keymap takes presses as Key Mapper does, as the keyboard phone keymap does: keys
   * without the SandFly key, modifier keys pressed on their own, either pressed twice, and modifier
   * keys held down.
   */
  private boolean recordsPresses() {
    return keyComboManager.getKeyComboModel() instanceof NewKeyComboModel model
        && model.plainKeys();
  }

  /**
   * Records the press [event] belongs to, for keymaps that take presses. Enter alone saves, a moment
   * later, unless it is pressed again, which records Enter twice; Backspace alone clears.
   */
  private boolean recordPress(KeyEvent event) {
    int keyCode = event.getKeyCode();
    if (keyCode == KeyEvent.KEYCODE_BACK
        || keyCode == KeyEvent.KEYCODE_HOME
        || keyCode == KeyEvent.KEYCODE_CALL
        || keyCode == KeyEvent.KEYCODE_ENDCALL
        || keyCode == KeyEvent.KEYCODE_POWER
        || keyCode == KeyEvent.KEYCODE_VOLUME_UP
        || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        || keyAlreadyInUseDialog != null) {
      return false;
    }
    boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
    if (event.getAction() == KeyEvent.ACTION_MULTIPLE || event.getRepeatCount() > 0) {
      return true;
    }
    boolean alone = pressKeysDown.isEmpty() && hasNoModifiers(event);
    if (keyCode == KeyEvent.KEYCODE_ENTER && alone) {
      if (!down) {
        return true;
      }
      if (pendingEnterSave != null) {
        pressHandler.removeCallbacks(pendingEnterSave);
        pendingEnterSave = null;
        record(
            new KeyCombo(
                KeyComboModel.NO_MODIFIER,
                KeyComboModel.NO_PREFIX_KEY_CODE,
                KeyEvent.KEYCODE_ENTER,
                /* triggerModifierUsed= */ false)
                .withPressType(KeyCombo.PRESSED_TWICE),
            /* allowTwice= */ false,
            event.getEventTime());
      } else {
        pendingEnterSave =
            () -> {
              pendingEnterSave = null;
              if (!shouldAnnounceInvalidShortcutMessage()) {
                processSaveButtonClickListener();
              }
            };
        pressHandler.postDelayed(pendingEnterSave, KeyPresses.TWICE_MS);
      }
      return true;
    }
    if (keyCode == KeyEvent.KEYCODE_DEL && alone) {
      if (down) {
        clearTemporaryKeyCombo();
        lastRecorded = null;
        updateKeyAssignmentText();
      }
      return true;
    }
    @Nullable SandFlyKey sandFlyKey = keyComboManager.getSandFlyKey();
    int trigger = getTriggerModifier();
    if (down) {
      if (pressKeysDown.isEmpty()) {
        pressModifiers = 0;
        pressOnlyModifiers = true;
        pressHeld = false;
      }
      pressKeysDown.add(keyCode);
      int bit = KeyPresses.modifierBitOf(event, sandFlyKey);
      if (bit != 0) {
        if (pressOnlyModifiers) {
          pressModifiers |= bit;
          pressHandler.removeCallbacks(pressHoldCheck);
          pressHandler.postDelayed(pressHoldCheck, KeyPresses.HOLD_MS);
        }
        return true;
      }
      pressOnlyModifiers = false;
      pressHandler.removeCallbacks(pressHoldCheck);
      boolean heldDown =
          sandFlyKey != null
              && sandFlyKey.isHeldKey()
              && pressKeysDown.contains(sandFlyKey.getKeyCode());
      KeyCombo combo = KeyPresses.keyComboOf(event, sandFlyKey, heldDown, trigger);
      record(combo, /* allowTwice= */ true, event.getEventTime());
      // Held down, it is the key held.
      pressHandler.removeCallbacks(pressKeyHoldCheck);
      pressKeyHeld = combo;
      pressKeyHeldCode = keyCode;
      pressHandler.postDelayed(pressKeyHoldCheck, KeyPresses.HOLD_MS);
      return true;
    }
    pressKeysDown.remove(keyCode);
    if (keyCode == pressKeyHeldCode) {
      pressHandler.removeCallbacks(pressKeyHoldCheck);
      pressKeyHeldCode = 0;
    }
    if (pressKeysDown.isEmpty()) {
      pressHandler.removeCallbacks(pressHoldCheck);
      if (pressOnlyModifiers && !pressHeld && pressModifiers != 0) {
        record(
            KeyPresses.comboOf(pressModifiers, trigger),
            /* allowTwice= */ true,
            event.getEventTime());
      }
    }
    return true;
  }

  /** Records modifier keys held down on their own. */
  private void onPressHeld() {
    if (!pressOnlyModifiers || pressKeysDown.isEmpty() || pressModifiers == 0) {
      return;
    }
    pressHeld = true;
    record(
        KeyPresses.comboOf(pressModifiers, getTriggerModifier())
            .withPressType(KeyCombo.HELD_DOWN),
        /* allowTwice= */ false,
        android.os.SystemClock.uptimeMillis());
  }

  /** Shows [combo] as the new shortcut, or it pressed twice where it was the last one too. */
  private void record(KeyCombo combo, boolean allowTwice, long time) {
    if (allowTwice
        && combo.equals(lastRecorded)
        && time - lastRecordedTime < RECORD_TWICE_MS) {
      lastRecorded = null;
      setTemporaryKeyCombo(combo.withPressType(KeyCombo.PRESSED_TWICE));
    } else {
      lastRecorded = allowTwice ? combo : null;
      lastRecordedTime = time;
      setTemporaryKeyCombo(combo);
    }
    updateKeyAssignmentText();
  }

  /**
   * Takes key presses again for the shortcut being recorded, only while its dialog shows. Key
   * already in use closes after the shortcut dialog when it saves; taking keys for a closed dialog
   * would keep every key from SandFly and the apps until SandFly restarts.
   */
  private void takeKeysWhileDialogShows() {
    if (setUpKeyComboDialog != null && setUpKeyComboDialog.isShowing()) {
      setKeyEventSource(getKeyEventSourceForCurrentKeyComboModel());
    }
  }

  /** Shows dialog if there is duplicate key assigned. */
  private void showOverrideKeyComboDialog(final String key) {
    CharSequence currentAction = titleOf(key);
    CharSequence newAction = getTitle();
    setKeyEventSource(KEY_EVENT_SOURCE_ACTIVITY);
    showOverrideKeyComboDialog(
        currentAction,
        newAction,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
              takeKeysWhileDialogShows();
              return;
            }

            saveKeyCombo();
            clearKeyComboCode(key);
            notifyListener(key, getKeyComboForKey(key));
            if (keyAlreadyInUseDialog != null) {
              keyAlreadyInUseDialog.dismiss();
            }
          }
        });
  }

  private void showOverrideKeyComboDialog(
      CharSequence currentAction,
      CharSequence newAction,
      final DialogInterface.OnClickListener clickListener) {
    String message =
        getContext()
            .getString(R.string.override_keycombo_message_two_params, currentAction, newAction);
    A11yAlertDialogWrapper.Builder builder =
        A11yAlertDialogWrapper.materialDialogBuilder(getContext());
    keyAlreadyInUseDialog =
        builder
            .setTitle(R.string.override_keycombo)
            .setMessage(message)
            .setNegativeButton(
                android.R.string.cancel,
                (dialog, which) -> {
                  dialog.dismiss();
                  clickListener.onClick(dialog, which);
                })
            .setPositiveButton(
                android.R.string.ok,
                (dialog, which) -> {
                  saveKeyCombo();
                  clickListener.onClick(dialog, which);
                  if (setUpKeyComboDialog != null && setUpKeyComboDialog.isShowing()) {
                    setUpKeyComboDialog.dismiss();
                  }
                })
            .create();
    keyAlreadyInUseDialog.setOnDismissListener(
        (dialog) -> {
          keyAlreadyInUseDialog = null;
          takeKeysWhileDialogShows();
        });
    keyAlreadyInUseDialog.show();
  }

  /** Saves key code to keyComboManager and notifies the listeners. */
  private void saveKeyCombo() {
    KeyCombo newKeyCombo = this.tempKeyCombo;
    keyComboManager.getKeyComboModel().saveKeyCombo(getKey(), newKeyCombo);
    notifyListener(getKey(), newKeyCombo);
  }

  /** Clears key combo code for the given key in keyComboManager. */
  private void clearKeyComboCode(String key) {
    keyComboManager.getKeyComboModel().clearKeyCombo(key);
  }

  private void notifyListener(String key, Object newValue) {
    if (keyComboChangeListener != null) {
      keyComboChangeListener.run();
    }
    Preference preference = getPreferenceManager().findPreference(key);
    if (!(preference instanceof KeyboardShortcutDialogPreference)) {
      return;
    }

    OnPreferenceChangeListener listener = preference.getOnPreferenceChangeListener();
    if (listener != null) {
      listener.onPreferenceChange(preference, newValue);
    }
  }

  /** Shows the dialog that sets the shortcut, as a tap on the preference does. */
  void showSetUpKeyComboDialog() {
    savingEnterDownTime = NO_SAVING_ENTER;
    if (supportedSequenceKeyInfra) {
      CharSequence title = getTitle();
      A11yAlertDialogWrapper wrapper =
          A11yAlertDialogWrapper.materialDialogBuilder(getContext())
              .setTitle(getContext().getString(R.string.keyboard_assignment_dialog_title, title))
              .setView(getSetUpKeyComboDialogView())
              .setPositiveButton(R.string.save_button_in_keycombo_assign_dialog, null)
              .setNegativeButton(android.R.string.cancel, null)
              .setNeutralButton(R.string.clear_button_in_keycombo_assign_dialog, null)
              .create();
      // Get the inner Dialog.
      setUpKeyComboDialog = wrapper.getDialog();
      setUpDialogListeners(wrapper);
    } else {
      setUpKeyComboDialog =
          MaterialComponentUtils.alertDialogBuilder(getContext())
              .setView(getSetUpKeyComboDialogView())
              .create();
      setUpDialogListeners(/* wrapper= */ null);
    }

    setUpKeyComboDialog.setOnDismissListener(
        keyAlreadyInUseDialog -> onSetUpKeyComboDialogClosed());
    setUpKeyComboDialog.show();
  }

  private void setUpDialogListeners(A11yAlertDialogWrapper wrapper) {
    setUpKeyComboDialog.setOnShowListener(
        (dialogInterface) -> {
          Button saveButton = null;
          Button clearButton = null;
          if (wrapper != null) {
            saveButton = wrapper.getButton(DialogInterface.BUTTON_POSITIVE);
            clearButton = wrapper.getButton(DialogInterface.BUTTON_NEUTRAL);
          } else {
            saveButton =
                ((AlertDialog) setUpKeyComboDialog).getButton(DialogInterface.BUTTON_POSITIVE);
          }

          if (saveButton != null) {
            saveButton.setOnClickListener(
                v -> {
                  if (shouldAnnounceInvalidShortcutMessage()) {
                    return;
                  }
                  processSaveButtonClickListener();
                });
            saveButton.setFocusableInTouchMode(true);
          }

          if (clearButton != null) {
            clearButton.setOnClickListener(
                v -> {
                  clearTemporaryKeyCombo();
                  updateKeyAssignmentText();
                  currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
                });
            clearButton.setFocusableInTouchMode(true);
          }
          registerDialogKeyEvent(setUpKeyComboDialog);
        });
  }

  /**
   * When the sequence key infra is supported and set up key combo dialog exists, announces the
   * invalid shortcut message when the shortcut is invalid or the {@code currentSequenceKeyState} is
   * STATE_PREFIX_KEY_PRESSED.
   */
  private boolean shouldAnnounceInvalidShortcutMessage() {
    if (!supportedSequenceKeyInfra || setUpKeyComboDialog == null) {
      return false;
    }

    if (currentSequenceKeyState == STATE_PREFIX_KEY_PRESSED) {
      if (errorTextView != null) {
        showError(
            errorTextView,
            getContext()
                .getString(
                    R.string.keycombo_sequence_key_assignment_instruction,
                    keyComboManager.getTriggerKeyUserFacingName(
                        keyComboManager.getKeyComboModel().getTriggerModifier())));
      }
      return true;
    }
    if (!isValidKeyCombo(this.tempKeyCombo)) {
      PreferencesActivityUtils.announceText(
          getContext().getString(R.string.invalid_shortcut_message), getContext());
      return true;
    }

    return false;
  }

  /** Processes when save button is clicked. */
  private void processSaveButtonClickListener() {
    if (!isValidKeyCombo(this.tempKeyCombo)) {
      instructionText.setTextColor(Color.RED);
      PreferencesActivityUtils.announceText(instructionText.getText().toString(), getContext());
      return;
    }

    if (recordListener != null) {
      recordListener.accept(this.tempKeyCombo);
      if (setUpKeyComboDialog != null) {
        setUpKeyComboDialog.dismiss();
      }
      return;
    }

    String key = getKeyForKeyCombo(this.tempKeyCombo);
    if (key == null) {
      saveKeyCombo();
      notifyChanged();
    } else if (!key.equals(getKey())) {
      showOverrideKeyComboDialog(key);
      return;
    }
    if (setUpKeyComboDialog != null) {
      setUpKeyComboDialog.dismiss();
    }
  }

  private KeyCombo getKeyComboForKey(String key) {
    throwExceptionIfKeyComboManagerIsNull();
    final KeyComboModel keyComboModel = keyComboManager.getKeyComboModel();
    final KeyCombo keyCombo = keyComboModel.getKeyComboForKey(key);
    return keyCombo != null && keyCombo.getKeyComboCode() != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED
        ? keyCombo
        : new KeyCombo(KeyComboModel.KEY_COMBO_CODE_UNASSIGNED, /* triggerModifierUsed= */ false);
  }

  @Nullable
  private String getKeyForKeyCombo(KeyCombo keyCombo) {
    throwExceptionIfKeyComboManagerIsNull();
    KeyComboModel keyComboModel = keyComboManager.getKeyComboModel();
    return keyComboModel.getKeyForKeyCombo(keyCombo);
  }

  private View getSetUpKeyComboDialogView() {
    final View dialogView =
        LayoutInflater.from(getContext())
            .inflate(
                supportedSequenceKeyInfra
                    ? R.layout.keyboard_shortcut_material_dialog
                    : R.layout.keyboard_shortcut_dialog,
                /* root= */ null);
    String key = getKey();

    updateKeyComboManager();

    KeyCombo saved = getKeyComboForKey(key);
    whileEditing = saved.isWhileEditing();
    inBrowseMode = saved.isInBrowseMode();
    setTemporaryKeyCombo(saved);
    keyAssignmentView = (TextView) dialogView.findViewById(R.id.assigned_combination);
    instructionText = (TextView) dialogView.findViewById(R.id.instruction);
    instructionText.setText(
        supportedSequenceKeyInfra
            ? getContext()
                .getString(
                    R.string.keycombo_assign_dialog_new_instruction,
                    keyComboManager.getTriggerKeyUserFacingName(
                        keyComboManager.getKeyComboModel().getTriggerModifier()))
            : keyComboManager.getKeyComboModel().getDescriptionOfEligibleKeyCombo());

    keyAssignmentView.setText(keyComboManager.getKeyComboStringRepresentation(this.tempKeyCombo));

    if (!supportedSequenceKeyInfra) {
      View clear = dialogView.findViewById(R.id.clear);
      clear.setOnClickListener(
          (View v) -> {
            instructionText.setTextColor(Color.BLACK);
            clearTemporaryKeyCombo();
            updateKeyAssignmentText();
          });
    }

    keyComboManager.setMatchKeyCombo(false);

    whileEditingBox = null;
    inBrowseModeBox = null;
    if (recordListener != null) {
      return dialogView;
    }
    // Shortcuts of the keymap can work only while a text field is being edited, taking their keys
    // from it.
    whileEditingBox = new CheckBox(getContext());
    whileEditingBox.setText(R.string.keycombo_only_while_editing);
    whileEditingBox.setChecked(whileEditing);
    whileEditingBox.setOnCheckedChangeListener(
        (box, checked) -> {
          whileEditing = checked;
          if (checked && inBrowseModeBox != null) {
            inBrowseModeBox.setChecked(false);
          }
          applyWhileEditing();
          keyAssignmentView.setText(
              keyComboManager.getKeyComboStringRepresentation(this.tempKeyCombo));
        });
    LinearLayout layout = new LinearLayout(getContext());
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.addView(dialogView);
    int padding = (int) (24 * getContext().getResources().getDisplayMetrics().density);
    whileEditingBox.setPadding(padding / 4, 0, padding, 0);
    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    params.setMarginStart(padding);
    layout.addView(whileEditingBox, params);
    // In the keyboard phone keymaps, single keys can have a shortcut in browse mode as well.
    if (keyComboManager.getKeyComboModel() instanceof NewKeyComboModel model && model.plainKeys()) {
      inBrowseModeBox = new CheckBox(getContext());
      inBrowseModeBox.setText(R.string.keycombo_only_in_browse_mode);
      inBrowseModeBox.setChecked(inBrowseMode);
      inBrowseModeBox.setOnCheckedChangeListener(
          (box, checked) -> {
            inBrowseMode = checked;
            if (checked && whileEditingBox != null) {
              whileEditingBox.setChecked(false);
            }
            applyWhileEditing();
            keyAssignmentView.setText(
                keyComboManager.getKeyComboStringRepresentation(this.tempKeyCombo));
          });
      inBrowseModeBox.setPadding(padding / 4, 0, padding, 0);
      LinearLayout.LayoutParams browseModeParams = new LinearLayout.LayoutParams(params);
      layout.addView(inBrowseModeBox, browseModeParams);
    }
    return layout;
  }

  /** Updates key assignment by getting the summary of th preference. */
  private void updateKeyAssignmentText() {
    keyAssignmentView.setText(getSummary());
    if (supportedSequenceKeyInfra) {
      updateErrorTextView();
    }
  }

  private void updateErrorTextView() {
    errorTextView = setUpKeyComboDialog.findViewById(R.id.error_message);
    if (errorTextView == null) {
      return;
    }

    // If the shortcut is invalid and the user doesn't enter sequence key mode, show the error
    // message.
    if (!isValidKeyCombo(this.tempKeyCombo)
        && currentSequenceKeyState != STATE_PREFIX_KEY_PRESSED) {
      showError(errorTextView, getContext().getString(R.string.invalid_shortcut_message));
      return;
    }

    // If the shortcut is unassigned, the error message should be hidden.
    if (this.tempKeyCombo.getKeyComboCode() == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED) {
      errorTextView.setVisibility(View.GONE);
      return;
    }

    String key = getKeyForKeyCombo(this.tempKeyCombo);
    if (key == null) {
      // This is a new valid shortcut.
      errorTextView.setVisibility(View.GONE);
      return;
    }
    if (!key.equals(getKey())) {
      // This shortcut has been assigned to another action.
      handleDuplicateShortcut(errorTextView, key);
      return;
    }

    // If reassign the same shortcut to the same action, the error message
    // should be hidden.
    errorTextView.setVisibility(View.GONE);
  }

  /**
   * If sequence key infra flag is supported, the key combo code is valid in the following cases:
   *
   * <ul>
   *   <li>If user doesn't enter sequence key mode, it is a valid non-sequence key combo.
   *   <li>If user enters sequence key mode, it is a sequence key combo. It has the same valid
   *       pre-requisites as the non-sequence key combo and its key code should not be an invalid
   *       final key.
   * </ul>
   *
   * If sequence key infra is not supported, the key combo code is valid if it is a valid
   * non-sequence.
   */
  private boolean isValidKeyCombo(KeyCombo keyCombo) {
    String key = getKey();
    if (keyCombo.isSandFlyKeyPressedTwice()) {
      return canTakeSandFlyKeyPressedTwice();
    }
    boolean eligible = keyComboManager.getKeyComboModel().isEligibleKeyCombo(keyCombo);
    if (!eligible) {
      // A command's default keys are eligible even without the SandFly key, such as browse mode's
      // letters and the arrow keys of the NVDA keymap. `isEligibleKeyCombo()` returns false only
      // when a key combo is assigned but the SandFly key is not used.
      eligible = keyCombo.equals(keyComboManager.getKeyComboModel().getDefaultKeyCombo(key));
    }

    boolean validNonSequenceKeyCombo =
        keyCombo.getKeyComboCode() != KeyComboModel.KEY_COMBO_CODE_INVALID && eligible;
    boolean sequenceKeyCombo =
        supportedSequenceKeyInfra
            && (keyCombo.getPrefixKeyCode() != KeyComboModel.NO_PREFIX_KEY_CODE);
    if (sequenceKeyCombo) {
      return validNonSequenceKeyCombo
          && !INVALID_SEQUENCE_KEYCOMBO_FINAL_KEYS.contains(keyCombo.getKeyCode());
    }
    return validNonSequenceKeyCombo;
  }

  /** Returns whether this is browse mode's shortcut and its SandFly key can be pressed twice. */
  private boolean canTakeSandFlyKeyPressedTwice() {
    @Nullable SandFlyKey sandFlyKey = keyComboManager.getSandFlyKey();
    return sandFlyKey != null
        && sandFlyKey.getCanBePressedTwice()
        && getContext()
            .getString(R.string.keycombo_shortcut_other_toggle_browse_mode_extra)
            .equals(getKey());
  }

  /** Handles duplicate shortcut if the shortcut is already assigned to another action. */
  private void handleDuplicateShortcut(TextView errorTextView, String conflictingKey) {
    CharSequence currentAction = titleOf(conflictingKey);
    String message = getContext().getString(R.string.already_in_use_error_message, currentAction);
    showError(errorTextView, message);
  }

  /** Sets up the error text view and shows error message in the error text view. */
  private void showError(TextView errorTextView, String errorMessage) {
    errorTextView.setText(errorMessage);
    errorTextView.setVisibility(View.VISIBLE);
    PreferencesActivityUtils.announceText(errorMessage, getContext());
  }

  private void throwExceptionIfKeyComboManagerIsNull() {
    if (keyComboManager == null) {
      throw new IllegalStateException(
          "KeyboardShortcutDialogPreference should never appear on systems where KeyComboManager is"
              + " unavailable");
    }
  }
}
