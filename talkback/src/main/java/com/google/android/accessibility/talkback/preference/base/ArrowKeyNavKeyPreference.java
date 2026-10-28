/*
 * Copyright 2026 SandFly contributors
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
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.keyboard.KeyCombo;
import com.google.android.accessibility.talkback.keyboard.KeyComboManager;
import com.google.android.accessibility.talkback.keyboard.ArrowKeyNav;
import com.google.android.accessibility.talkback.keyboard.ArrowKeyNavCommand;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.ServiceKeyEventListener;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import java.util.LinkedHashSet;

/**
 * Keys of an arrow key navigation command on the Keyboard shortcuts screen. Its dialog takes a key
 * or two keys pressed together, with or without modifiers.
 */
public class ArrowKeyNavKeyPreference extends Preference
    implements DialogInterface.OnKeyListener, ServiceKeyEventListener {

  private @Nullable ArrowKeyNavCommand command;
  private long keys = ArrowKeyNav.UNASSIGNED;
  private final LinkedHashSet<Integer> heldKeys = new LinkedHashSet<>();
  private @Nullable KeyComboManager keyComboManager;
  private @Nullable Dialog dialog;
  private @Nullable TextView keysView;
  private @Nullable TextView errorView;

  /** Whether SandFly receives keys before the dialog, so the dialog's own listener ignores them. */
  private boolean keysFromService;

  public ArrowKeyNavKeyPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public ArrowKeyNavKeyPreference(Context context) {
    super(context);
    init();
  }

  private void init() {
    setPersistent(false);
    command = ArrowKeyNavCommand.fromPrefKey(getKey());
  }

  @Override
  public void setKey(String key) {
    super.setKey(key);
    command = ArrowKeyNavCommand.fromPrefKey(key);
  }

  /** Shows the keys again, such as after they were reset. */
  public void refresh() {
    notifyChanged();
  }

  @Override
  public CharSequence getSummary() {
    if (command == null) {
      return null;
    }
    return describe(ArrowKeyNav.keysFor(getContext(), command));
  }

  @Override
  protected void onClick() {
    super.onClick();
    if (command != null) {
      showDialog();
    }
  }

  private String describe(long keys) {
    return KeyboardShortcutDialogPreference.getKeyComboManager(getContext())
        .getArrowKeyNavKeysStringRepresentation(keys);
  }

  private void showDialog() {
    keys = ArrowKeyNav.keysFor(getContext(), command);
    heldKeys.clear();
    View view =
        LayoutInflater.from(getContext())
            .inflate(R.layout.keyboard_shortcut_material_dialog, /* root= */ null);
    keysView = view.findViewById(R.id.assigned_combination);
    errorView = view.findViewById(R.id.error_message);
    TextView instruction = view.findViewById(R.id.instruction);
    instruction.setText(R.string.arrow_key_nav_assign_instruction);

    A11yAlertDialogWrapper wrapper =
        A11yAlertDialogWrapper.materialDialogBuilder(getContext())
            .setTitle(getContext().getString(R.string.keyboard_assignment_dialog_title, getTitle()))
            .setView(view)
            .setPositiveButton(R.string.save_button_in_keycombo_assign_dialog, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.clear_button_in_keycombo_assign_dialog, null)
            .create();
    Dialog shownDialog = wrapper.getDialog();
    dialog = shownDialog;
    shownDialog.setOnShowListener(
        d -> {
          Button save = wrapper.getButton(DialogInterface.BUTTON_POSITIVE);
          if (save != null) {
            save.setOnClickListener(v -> save());
          }
          Button clear = wrapper.getButton(DialogInterface.BUTTON_NEUTRAL);
          if (clear != null) {
            clear.setOnClickListener(v -> setKeys(ArrowKeyNav.UNASSIGNED));
          }
        });
    shownDialog.setOnKeyListener(this);
    shownDialog.setOnDismissListener(d -> onDialogDismissed());

    keyComboManager = KeyboardShortcutDialogPreference.getKeyComboManager(getContext());
    keysFromService = TalkBackService.getInstance() != null;
    if (keysFromService) {
      keyComboManager.setKeyEventDelegate(this);
    }
    keyComboManager.setMatchKeyCombo(false);
    shownDialog.show();
    setKeys(keys);
  }

  private void onDialogDismissed() {
    if (keyComboManager != null) {
      if (keysFromService) {
        keyComboManager.setKeyEventDelegate(null);
      }
      keyComboManager.setMatchKeyCombo(true);
    }
    heldKeys.clear();
    dialog = null;
  }

  @Override
  public boolean onKeyEvent(KeyEvent event, EventId eventId) {
    return keysFromService && onKeyEventInternal(event);
  }

  @Override
  public boolean processWhenServiceSuspended() {
    return true;
  }

  @Override
  public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent event) {
    return !keysFromService && onKeyEventInternal(event);
  }

  private boolean onKeyEventInternal(KeyEvent event) {
    int keyCode = event.getKeyCode();
    if (dialog == null
        || keyCode == KeyEvent.KEYCODE_UNKNOWN
        || KeyEvent.isModifierKey(keyCode)
        || keyCode == KeyEvent.KEYCODE_BACK
        || keyCode == KeyEvent.KEYCODE_HOME
        || keyCode == KeyEvent.KEYCODE_CALL
        || keyCode == KeyEvent.KEYCODE_ENDCALL) {
      return false;
    }
    if (event.getAction() == KeyEvent.ACTION_UP) {
      return heldKeys.remove(keyCode);
    }
    if (event.getAction() != KeyEvent.ACTION_DOWN) {
      return false;
    }
    if (event.getRepeatCount() > 0) {
      return heldKeys.contains(keyCode);
    }

    int modifiers = event.getModifiers() & KeyCombo.KEY_EVENT_MODIFIER_MASK;
    Integer otherKey = null;
    for (int held : heldKeys) {
      if (held != keyCode) {
        otherKey = held;
      }
    }
    if (otherKey != null) {
      heldKeys.add(keyCode);
      setKeys(ArrowKeyNav.keys(modifiers, otherKey, keyCode));
      return true;
    }

    // Keys that work the dialog when pressed alone.
    if (modifiers == 0) {
      switch (keyCode) {
        case KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_ESCAPE -> {
          return false;
        }
        case KeyEvent.KEYCODE_ENTER -> {
          View dialogView = dialog.findViewById(R.id.dialog_view);
          if (dialogView == null || !dialogView.isAccessibilityFocused()) {
            // Enter activates the focused button.
            return false;
          }
          heldKeys.add(keyCode);
          save();
          return true;
        }
        case KeyEvent.KEYCODE_DEL -> {
          heldKeys.add(keyCode);
          setKeys(ArrowKeyNav.UNASSIGNED);
          return true;
        }
        default -> {}
      }
    } else if (modifiers == KeyEvent.META_SHIFT_ON && keyCode == KeyEvent.KEYCODE_TAB) {
      return false;
    } else if (modifiers == KeyEvent.META_CTRL_ON && keyCode == KeyEvent.KEYCODE_DEL) {
      heldKeys.add(keyCode);
      setKeys(command.getDefaultKeys());
      return true;
    }

    heldKeys.add(keyCode);
    setKeys(ArrowKeyNav.keys(modifiers, keyCode));
    return true;
  }

  private void setKeys(long newKeys) {
    keys = newKeys;
    if (keysView != null) {
      keysView.setText(describe(newKeys));
    }
    if (errorView == null) {
      return;
    }
    ArrowKeyNavCommand owner = ArrowKeyNav.commandFor(getContext(), newKeys);
    if (owner == null || owner == command) {
      errorView.setVisibility(View.GONE);
      return;
    }
    String message =
        getContext()
            .getString(
                R.string.already_in_use_error_message, getContext().getString(owner.getTitle()));
    errorView.setText(message);
    errorView.setVisibility(View.VISIBLE);
    PreferencesActivityUtils.announceText(message, getContext());
  }

  private void save() {
    ArrowKeyNavCommand owner = ArrowKeyNav.commandFor(getContext(), keys);
    if (owner == null || owner == command) {
      saveAndClose(/* clearedOwner= */ null);
      return;
    }
    // Keys already used by another command: confirm before moving them here.
    if (keysFromService && keyComboManager != null) {
      keyComboManager.setKeyEventDelegate(null);
    }
    A11yAlertDialogWrapper overrideDialog =
        A11yAlertDialogWrapper.materialDialogBuilder(getContext())
            .setTitle(R.string.override_keycombo)
            .setMessage(
                getContext()
                    .getString(
                        R.string.override_keycombo_message_two_params,
                        getContext().getString(owner.getTitle()),
                        getTitle()))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, (d, which) -> saveAndClose(owner))
            .create();
    overrideDialog.setOnDismissListener(
        d -> {
          if (dialog != null && keysFromService && keyComboManager != null) {
            keyComboManager.setKeyEventDelegate(this);
          }
        });
    overrideDialog.show();
  }

  private void saveAndClose(@Nullable ArrowKeyNavCommand clearedOwner) {
    if (clearedOwner != null) {
      ArrowKeyNav.save(getContext(), clearedOwner, ArrowKeyNav.UNASSIGNED);
      Preference ownerPreference = getPreferenceManager().findPreference(clearedOwner.getPrefKey());
      if (ownerPreference instanceof ArrowKeyNavKeyPreference arrowKeyNavKeyPreference) {
        arrowKeyNavKeyPreference.refresh();
      }
    }
    ArrowKeyNav.save(getContext(), command, keys);
    notifyChanged();
    if (dialog != null) {
      dialog.dismiss();
    }
  }
}
