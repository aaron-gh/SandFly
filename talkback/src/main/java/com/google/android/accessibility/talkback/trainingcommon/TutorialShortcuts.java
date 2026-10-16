/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.trainingcommon;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.gesture.GestureHints;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping;
import com.google.android.accessibility.talkback.keyboard.KeyCombo;
import com.google.android.accessibility.talkback.keyboard.KeyComboManager;
import com.google.android.accessibility.talkback.keyboard.KeyComboModel;
import com.google.android.accessibility.talkback.keyboard.NewKeyComboModel;
import com.google.android.accessibility.utils.WindowUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The action of each gesture and the keys of each keyboard shortcut, which the service sends to the
 * tutorial with its gesture data, so that tutorial texts can be checked against the user's settings.
 */
public final class TutorialShortcuts {

  private static final String TAG = "TutorialShortcuts";

  /** Followed by a gesture ID, the key of that gesture's action. */
  private static final String GESTURE_ACTION_PREFIX = "gesture_action:";

  /** Whether the keymap is the enhanced one with the Action key, as the keyboard tutorial says. */
  private static final String KEYMAP_AS_WRITTEN = "keymap_as_written";

  /** Followed by a keyboard shortcut's key, the text of its keys. */
  private static final String KEY_COMBO_TEXT_PREFIX = "key_combo_text:";

  /** Followed by a keyboard shortcut's key, whether it has its default keys. */
  private static final String KEY_COMBO_DEFAULT_PREFIX = "key_combo_default:";

  private final Map<Integer, String> gestureIdToAction = new HashMap<>();
  private final Map<String, String> keyComboTexts = new HashMap<>();
  private final Map<String, Boolean> keyComboDefaults = new HashMap<>();
  private boolean keymapAsWritten = true;
  private boolean received;
  // Texts drawn before the service sent its gestures and shortcuts, as written.
  private final Set<Integer> textsShownEarly = new HashSet<>();

  /** Adds the action of each gesture in {@code mapping} and the keys of each shortcut to data. */
  public static void put(
      Bundle data, GestureShortcutMapping mapping, @Nullable KeyComboManager keyComboManager) {
    mapping
        .getGestureActions()
        .forEach((gestureId, action) -> data.putString(GESTURE_ACTION_PREFIX + gestureId, action));

    @Nullable KeyComboModel model =
        (keyComboManager == null) ? null : keyComboManager.getKeyComboModel();
    if (model == null) {
      return;
    }
    data.putBoolean(
        KEYMAP_AS_WRITTEN,
        model instanceof NewKeyComboModel && model.getTriggerModifier() == KeyEvent.META_META_ON);
    for (String key : model.getKeyComboMap().keySet()) {
      KeyCombo combo = model.getKeyComboForKey(key);
      data.putString(
          KEY_COMBO_TEXT_PREFIX + key, keyComboManager.getKeyComboStringRepresentation(combo));
      data.putBoolean(KEY_COMBO_DEFAULT_PREFIX + key, combo.equals(model.getDefaultKeyCombo(key)));
    }
  }

  /** Forgets the gestures and shortcuts, before the ones the service sent are read. */
  public void clear() {
    received = true;
    gestureIdToAction.clear();
    keyComboTexts.clear();
    keyComboDefaults.clear();
    keymapAsWritten = true;
  }

  /** Reads the entry {@code key} of {@code data}. Returns false if it isn't one of these. */
  public boolean read(Bundle data, String key) {
    if (key.startsWith(GESTURE_ACTION_PREFIX)) {
      try {
        int gestureId = Integer.parseInt(key.substring(GESTURE_ACTION_PREFIX.length()));
        gestureIdToAction.put(gestureId, data.getString(key));
      } catch (NumberFormatException e) {
        LogUtils.w(TAG, "Bad gesture key %s", key);
      }
      return true;
    }
    if (key.startsWith(KEY_COMBO_TEXT_PREFIX)) {
      keyComboTexts.put(key.substring(KEY_COMBO_TEXT_PREFIX.length()), data.getString(key));
      return true;
    }
    if (key.startsWith(KEY_COMBO_DEFAULT_PREFIX)) {
      keyComboDefaults.put(key.substring(KEY_COMBO_DEFAULT_PREFIX.length()), data.getBoolean(key));
      return true;
    }
    if (key.equals(KEYMAP_AS_WRITTEN)) {
      keymapAsWritten = data.getBoolean(key, true);
      return true;
    }
    return false;
  }

  /** Notes that the text {@code textResId}, which names gestures or keys, is being shown. */
  public void noteTextShown(@StringRes int textResId) {
    if (!received) {
      textsShownEarly.add(textResId);
    }
  }

  /**
   * Returns the texts that were shown before the service sent its gestures and shortcuts, and
   * forgets them.
   */
  public ImmutableSet<Integer> takeTextsShownEarly() {
    ImmutableSet<Integer> texts = ImmutableSet.copyOf(textsShownEarly);
    textsShownEarly.clear();
    return texts;
  }

  /**
   * Returns whether the gesture {@code gestureId}, as a left-to-right text names it, does the
   * action {@code actionKey}. Returns true before the service has sent its gestures, so that texts
   * naming the default gestures show.
   */
  public boolean isGestureAssigned(Context context, int gestureId, @StringRes int actionKey) {
    if (gestureIdToAction.isEmpty()) {
      return true;
    }
    int laidOut = GestureHints.asLaidOut(gestureId, WindowUtils.isScreenLayoutRTL(context));
    return TextUtils.equals(gestureIdToAction.get(laidOut), context.getString(actionKey));
  }

  /**
   * Returns whether the keyboard shortcut {@code key} has the keys the keyboard tutorial names: the
   * enhanced keymap's default keys with the Action key. Returns true before the service has sent
   * its shortcuts.
   */
  public boolean isKeyComboAsWritten(String key) {
    if (keyComboDefaults.isEmpty()) {
      return true;
    }
    return keymapAsWritten && Boolean.TRUE.equals(keyComboDefaults.get(key));
  }

  /** Returns the text of the keys of the keyboard shortcut {@code key}, or null if it has none. */
  public @Nullable String getKeyComboText(String key) {
    return keyComboTexts.get(key);
  }

  /** Sets the action of a gesture, as the service would send it. */
  @VisibleForTesting
  public void putGestureAction(int gestureId, String action) {
    gestureIdToAction.put(gestureId, action);
  }

  /** Sets the keys of a keyboard shortcut, as the service would send them. */
  @VisibleForTesting
  public void putKeyCombo(String key, String text, boolean isDefault, boolean keymapAsWritten) {
    keyComboTexts.put(key, text);
    keyComboDefaults.put(key, isDefault);
    this.keymapAsWritten = keymapAsWritten;
  }
}
