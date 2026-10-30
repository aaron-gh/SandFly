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

package com.google.android.accessibility.talkback.preference.base;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory;
import com.google.android.accessibility.material.preference.AccessibilitySuiteSwitchPreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.TalkBackService.TalkbackServiceStateNotifier.TalkBackServiceStateChangeListener;
import com.google.android.accessibility.talkback.commands.CommandBindings;
import com.google.android.accessibility.talkback.commands.Commands;
import com.google.android.accessibility.talkback.keyboard.KeyCombo;
import com.google.android.accessibility.talkback.keyboard.KeyComboManager;
import com.google.android.accessibility.talkback.keyboard.KeyComboModel;
import com.google.android.accessibility.talkback.keyboard.Keymap;
import com.google.android.accessibility.talkback.keyboard.SandFlyKey;
import com.google.android.accessibility.talkback.keyboard.ArrowKeyNav;
import com.google.android.accessibility.talkback.keyboard.ArrowKeys;
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.talkback.preference.TalkBackPreferenceFilter;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Panel holding a set of keyboard shortcut preferences. */
public class TalkBackKeyboardShortcutPreferenceFragment extends TalkbackBaseFragment {
  private static final String TAG = "TalkBackKeyboardShortcutPreferenceFragment";
  // android.content.res.Resources.ID_NULL was introduced in API 29, but the min SDK version of
  // TalkBack is API 26. For compatibility, define a constant here instead of using ID_NULL.
  private static final int RESOURCE_ID_NULL = 0;

  private String keymap;
  private SharedPreferences prefs;
  private PreferenceScreen resetKeymapPreference;

  public TalkBackKeyboardShortcutPreferenceFragment() {
    super(R.xml.new_key_combo_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_manage_keyboard_shortcuts);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TalkBackService.TalkbackServiceStateNotifier.getInstance()
        .registerTalkBackServiceStateChangeListener(serviceStateChangeListener);
  }

  /** Updates fragment whenever their values change. */
  private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
      (prefs, key) -> {
        if (TextUtils.equals(key, getString(R.string.pref_select_keymap_key))
            || TextUtils.equals(key, getString(R.string.pref_sandfly_key))) {
          // Refreshes key combo model after keymap or SandFly key changes.
          KeyComboManager keyComboManager = getKeyComboManager();
          keyComboManager.refreshKeyComboModel();
          keymap = getKeymap();
          updateFragment();
        }
      };

  private final DialogInterface.OnClickListener resetKeymapConfirmDialogPositive =
      (dialogInterface, i) -> {
        resetKeymap();
        dialogInterface.dismiss();

        PreferencesActivityUtils.announceText(
            getString(R.string.keycombo_menu_announce_reset_keymap), getActivity());
      };

  private final Preference.OnPreferenceClickListener resetKeymapPreferenceClickListener =
      (preference) -> {
        // Show confirm dialog.
        A11yAlertDialogWrapper.materialDialogBuilder(getActivity())
            .setTitle(getString(R.string.keycombo_menu_reset_keymap))
            .setMessage(getString(R.string.message_in_reset_keymap_confirm_dialog))
            .setPositiveButton(
                R.string.reset_button_in_reset_keymap_confirm_dialog,
                resetKeymapConfirmDialogPositive)
            .setNegativeButton(
                android.R.string.cancel,
                (DialogInterface dialogInterface, int i) -> dialogInterface.cancel())
            .create()
            .show();
        return true;
      };

  private final Preference.OnPreferenceClickListener
      viewSystemKeyboardShortcutsPreferenceClickListener =
          (preference) -> {
            if (getActivity() != null) {
              getActivity().requestShowKeyboardShortcuts();
            } else {
              LogUtils.w(TAG, "Could not resolve activity to call requestShowKeyboardShortcuts");
            }
            return true;
          };

  private final OnPreferenceChangeListener preferenceChangeListener =
      (preference, newValue) -> {
        String preferenceKeyForTriggerModifier =
            getKeyComboManager().getKeyComboModel().getPreferenceKeyForTriggerModifier();
        if (preference instanceof KeyboardShortcutDialogPreference keyboardPreference
            && newValue instanceof KeyCombo customizedKeyCombo) {
          keyboardPreference.setKeyCombo(customizedKeyCombo);
          keyboardPreference.notifyChanged();
        } else if (preference.getKey() != null
            && preference.getKey().equals(getString(R.string.pref_select_keymap_key))
            && newValue instanceof String chosenKeymap) {
          // Do nothing if keymap is the same.
          if (keymap.equals(chosenKeymap)) {
            return false;
          }

          // Announce new keymap.
          PreferencesActivityUtils.announceText(
              String.format(
                  getString(R.string.keycombo_menu_announce_active_keymap),
                  getKeymapName(chosenKeymap)),
              getActivity());
        } else if (preference.getKey() != null
            && preference.getKey().equals(preferenceKeyForTriggerModifier)
            && newValue instanceof String chosenTriggerModifier) {
          ListPreference listPreference = (ListPreference) preference;
          if (listPreference.getValue().equals(chosenTriggerModifier)) {
            return false;
          }
          PreferencesActivityUtils.announceText(
              getString(
                  R.string.keycombo_menu_announce_new_trigger_modifier,
                  listPreference.getEntries()[
                      listPreference.findIndexOfValue(chosenTriggerModifier)]),
              getActivity());
          return true;
        }
        return true;
      };

  private void resetKeymap() {
    KeyComboModel keyComboModel = getKeyComboManager().getKeyComboModel();
    for (String key : keyComboModel.getKeyComboMap().keySet()) {
      KeyCombo defaultKeyCombo = keyComboModel.getDefaultKeyCombo(key);
      // Do nothing if key combo code is not changed from default one.
      if (defaultKeyCombo.equals(keyComboModel.getKeyComboForKey(key))) {
        continue;
      }
      // Save with default key combo.
      keyComboModel.saveKeyCombo(key, defaultKeyCombo);

      // Update UI.
      if (findPreference(key) instanceof KeyboardShortcutDialogPreference preference) {
        preference.setKeyCombo(defaultKeyCombo);
        preference.notifyChanged();
      }
    }

    ArrowKeyNav.reset(getContext());
    refreshArrowKeys();
  }

  private KeyComboManager getKeyComboManager() {
    TalkBackService talkBackService = TalkBackService.getInstance();

    return talkBackService == null
        ? KeyComboManager.create(getActivity())
        : talkBackService.getKeyComboManager();
  }

  private String getKeymapName(String keymap) {
    return getString(Keymap.of(getContext(), keymap).getNameRes());
  }

  private String getKeymap() {
    KeyComboManager keyComboManager = getKeyComboManager();
    return keyComboManager.getKeymap();
  }

  private int getPreferenceResourceId() {
    return R.xml.new_key_combo_preferences;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    prefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    TalkBackService.TalkbackServiceStateNotifier.getInstance()
        .unregisterTalkBackServiceStateChangeListener(serviceStateChangeListener);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);

    keymap = getKeymap();
    int preferenceResourceId = getPreferenceResourceId();
    if (preferenceResourceId == RESOURCE_ID_NULL) {
      LogUtils.w(TAG, "Preference resource id is null, can't create preferences.");
      return;
    }
    setPreferencesFromResource(preferenceResourceId, rootKey);

    prefs = SharedPreferencesUtils.getSharedPreferences(getContext());
    prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

    updatePreference();
  }

  /** Updates Fragment when KeyComboModel changes or resets keymap. */
  private void updateFragment() {
    PreferenceSettingsUtils.setPreferencesFromResource(this, getPreferenceResourceId(), null);
    updatePreference();
  }

  /** Updates preference, including UI, by KeyComboModel. */
  private void updatePreference() {
    // By default, the preference filter is applied in TalkbackBaseFragment which is only performed
    // 1st time the fragment created. Keyboard shortcut preference group can change between default
    // key map and new key map, so need to redo the filter when the preference changed.
    TalkBackPreferenceFilter talkBackPreferenceFilter =
        new TalkBackPreferenceFilter(getActivity().getApplicationContext());
    talkBackPreferenceFilter.filterPreferences(getPreferenceScreen());

    updateSelectKeymapPreferenceWithAvailableKeymaps();
    updateSandFlyKeyPreferenceWithKeymapsKeys();

    resetKeymapPreference = findPreference(getString(R.string.pref_reset_keymap_key));
    resetKeymapPreference.setOnPreferenceClickListener(resetKeymapPreferenceClickListener);
    Preference viewSystemKeyboardShortcutsPreference =
        findPreference(getString(R.string.pref_view_system_keyboard_shortcuts_key));
    if (viewSystemKeyboardShortcutsPreference != null) {
      viewSystemKeyboardShortcutsPreference.setOnPreferenceClickListener(
          viewSystemKeyboardShortcutsPreferenceClickListener);
    }

    initPreferenceUIs(getPreferenceScreen());
    addArrowKeys();
    addCommandCategories();
    updateDialogAndResetKeymapPreference();
  }

  /**
   * Adds arrow key navigation: whether it is on, and the command each of its arrow keys runs, chosen
   * from the same list as a gesture's.
   */
  private void addArrowKeys() {
    Context context = getContext();
    if (context == null) {
      return;
    }
    ArrowKeyNav.migrate(SharedPreferencesUtils.getSharedPreferences(context));
    KeyComboManager keyComboManager = getKeyComboManager();
    PreferenceCategory category = new PreferenceCategory(context);
    category.setKey("pref_arrow_key_nav_category");
    category.setTitle(R.string.keycombo_menu_category_arrow_key_nav);
    category.setIconSpaceReserved(false);
    getPreferenceScreen().addPreference(category);
    AccessibilitySuiteSwitchPreference on = new AccessibilitySuiteSwitchPreference(context);
    on.setKey(ArrowKeyNav.onKey());
    on.setTitle(R.string.keycombo_menu_category_arrow_key_nav);
    on.setDefaultValue(false);
    on.setIconSpaceReserved(false);
    category.addPreference(on);
    for (ArrowKeys arrowKeys : ArrowKeys.values()) {
      GestureListPreference preference =
          new GestureListPreference(
              context,
              ArrowKeyNav.prefKey(arrowKeys),
              keyComboManager.getArrowKeysStringRepresentation(arrowKeys.getKeys()),
              arrowKeys.getDefaultCommand());
      preference.setIconSpaceReserved(false);
      category.addPreference(preference);
    }
  }

  /** Shows the commands the arrow keys run now, such as after a reset or the list closes. */
  private void refreshArrowKeys() {
    for (ArrowKeys arrowKeys : ArrowKeys.values()) {
      if (findPreference(ArrowKeyNav.prefKey(arrowKeys)) instanceof GestureListPreference preference) {
        preference.refresh();
      }
    }
  }

  @Override
  public void onDisplayPreferenceDialog(Preference preference) {
    if (preference instanceof GestureListPreference gesturePreference) {
      GestureListPreference.showCommandList(this, gesturePreference);
    } else {
      super.onDisplayPreferenceDialog(preference);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshArrowKeys();
  }

  /** Adds every command, by category, each with its keyboard shortcut. */
  private void addCommandCategories() {
    Context context = getContext();
    KeyComboManager keyComboManager = getKeyComboManager();
    KeyComboModel model = keyComboManager.getKeyComboModel();
    if (context == null || model == null) {
      return;
    }
    Set<String> browseModeOnlyCommands =
        keyComboManager.isBrowseModeSupported()
            ? keyComboManager.getBrowseModeOnlyCommands()
            : Set.of();
    String browseModeTitle = getString(R.string.template_keycombo_menu_browse_mode_command);
    for (Commands.Category category : Commands.categories(context)) {
      AccessibilitySuitePreferenceCategory group = new AccessibilitySuitePreferenceCategory(context);
      group.setKey("pref_keyboard_commands_" + category.getTitleRes());
      group.setTitle(category.getTitleRes());
      group.setIconSpaceReserved(false);
      getPreferenceScreen().addPreference(group);
      for (Commands.Command command : category.getCommands()) {
        String key = CommandBindings.keyboardKey(context, model, command);
        if (findPreference(key) != null) {
          // A keymap without a key of its own for this command keeps it under the command.
          continue;
        }
        CharSequence title =
            browseModeOnlyCommands.contains(key)
                ? String.format(browseModeTitle, command.getLabel())
                : command.getLabel();
        KeyboardShortcutDialogPreference preference =
            new KeyboardShortcutDialogPreference(context, key, title);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceChangeListener(preferenceChangeListener);
        if (!CommandBindings.isCustomizable(context, key)) {
          preference.setEnabled(false);
          preference.setSummary(
              getString(R.string.shortcut_not_customizable, preference.getSummary().toString()));
        }
        group.addPreference(preference);
      }
    }
    collapseCategories();
  }

  /**
   * Lists the SandFly keys the keymap offers. Where the key chosen for another keymap isn't one,
   * the keymap's own is used and named, and the choice stays for the keymaps that offer it.
   */
  private void updateSandFlyKeyPreferenceWithKeymapsKeys() {
    ListPreference preference = findPreference(getString(R.string.pref_sandfly_key));
    if (preference == null) {
      return;
    }
    Keymap current = Keymap.of(getContext(), keymap);
    List<CharSequence> entries = new ArrayList<>();
    List<CharSequence> entryValues = new ArrayList<>();
    for (SandFlyKey sandFlyKey : current.getSandFlyKeys()) {
      entries.add(getString(sandFlyKey.getNameRes()));
      entryValues.add(getString(sandFlyKey.getValueRes()));
    }
    preference.setEntries(entries.toArray(new CharSequence[0]));
    preference.setEntryValues(entryValues.toArray(new CharSequence[0]));
    SandFlyKey used = current.sandFlyKey(getContext());
    if (SandFlyKey.chosen(getContext()) != used) {
      preference.setSummary(getString(used.getNameRes()));
    }
  }

  /** Updates select keymap preference with available keymaps. */
  private void updateSelectKeymapPreferenceWithAvailableKeymaps() {
    ListPreference keymapPreference = findPreference(getString(R.string.pref_select_keymap_key));
    List<CharSequence> entries = new ArrayList<>();
    List<CharSequence> entryValues = new ArrayList<>();
    for (Keymap available : Keymap.values()) {
      entries.add(getString(available.getNameRes()));
      entryValues.add(getString(available.getValueRes()));
    }
    keymapPreference.setEntries(entries.toArray(new CharSequence[entries.size()]));
    keymapPreference.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
  }

  /**
   * Initialize preference UIs.
   *
   * @param root Root element of preference UIs.
   */
  private void initPreferenceUIs(PreferenceGroup root) {
    if (root == null) {
      return;
    }

    final KeyComboModel keyComboModel = getKeyComboManager().getKeyComboModel();
    final String preferenceKeyForTriggerModifier =
        keyComboModel.getPreferenceKeyForTriggerModifier();
    final Map<String, KeyCombo> keyComboMap = keyComboModel.getKeyComboMap();
    for (int i = root.getPreferenceCount() - 1; i >= 0; i--) {
      final Preference preference = root.getPreference(i);
      final String key = preference.getKey();
      if (key != null && preference instanceof KeyboardShortcutDialogPreference) {
        if (!keyComboMap.containsKey(key)) {
          // Hide preference of unavailable key combo on this device.
          root.removePreference(preference);
          if (root.getPreferenceCount() == 0) {
            getPreferenceScreen().removePreference(root);
          }
        } else if (key.equals(getString(R.string.keycombo_shortcut_other_toggle_browse_mode))) {
          // Disable the preference to make it not customizable.
          KeyboardShortcutDialogPreference keyShortcutPreference =
              (KeyboardShortcutDialogPreference) preference;
          keyShortcutPreference.setEnabled(false);
          keyShortcutPreference.setSummary(
              getString(
                  R.string.shortcut_not_customizable,
                  keyShortcutPreference.getSummary().toString()));
        } else {
          // Set onPreferenceChangeListener.
          preference.setOnPreferenceChangeListener(preferenceChangeListener);
        }
      } else if ((key != null && key.equals(getString(R.string.pref_select_keymap_key)))
          || (key != null && key.equals(preferenceKeyForTriggerModifier))) {
        // Set onPreferenceChangeListener.
        preference.setOnPreferenceChangeListener(preferenceChangeListener);
      } else if (preference instanceof PreferenceGroup) {
        initPreferenceUIs((PreferenceGroup) preference);
      }
    }
  }

  private void setUpDialogPreference(Consumer<KeyboardShortcutDialogPreference> consumer) {
    forEachDialogPreference(getPreferenceScreen(), consumer);
  }

  private static void forEachDialogPreference(
      PreferenceGroup group, Consumer<KeyboardShortcutDialogPreference> consumer) {
    for (int i = 0; i < group.getPreferenceCount(); i++) {
      Preference preference = group.getPreference(i);
      if (preference instanceof KeyboardShortcutDialogPreference dialogPreference) {
        consumer.accept(dialogPreference);
      } else if (preference instanceof PreferenceGroup childGroup) {
        forEachDialogPreference(childGroup, consumer);
      }
    }
  }

  private void updateDialogAndResetKeymapPreference() {
    resetKeymapPreference.setEnabled(isServiceActive());
    setUpDialogPreference(
        preference ->
            preference.setEnabled(
                isServiceActive()
                    && CommandBindings.isCustomizable(getContext(), preference.getKey())));
  }

  private boolean isServiceActive() {
    return TalkBackService.isServiceActive();
  }

  /** Enabled or disabled the dialog preference if the state of TalkbackService is changed. */
  TalkBackService.TalkbackServiceStateNotifier.TalkBackServiceStateChangeListener
      serviceStateChangeListener =
          new TalkBackServiceStateChangeListener() {
            @Override
            public void onServiceStateChange(boolean isServiceActive) {
              resetKeymapPreference.setEnabled(isServiceActive);
              setUpDialogPreference(
                  preference -> {
                    preference.setEnabled(
                        isServiceActive
                            && CommandBindings.isCustomizable(getContext(), preference.getKey()));
                    if (isServiceActive) {
                      preference.updateKeyComboManager();
                      preference.onTriggerModifierChanged();
                    }
                  });
            }
          };
}
