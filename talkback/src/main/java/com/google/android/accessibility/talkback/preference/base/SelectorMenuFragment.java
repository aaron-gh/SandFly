/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.content.SharedPreferences;
import android.text.TextUtils;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.preference.ItemOrder;
import com.google.android.accessibility.talkback.recognition.LiveRecognition;
import com.google.android.accessibility.talkback.selector.AiActions;
import com.google.android.accessibility.talkback.selector.SelectorController;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Reading controls: the controls in the list, in order, and those that aren't. */
public class SelectorMenuFragment extends ItemListFragment {

  // Prevents from runtime exception that will occur in some cases during state restore if the
  // no-argument constructor is not available.
  public SelectorMenuFragment() {
    super(R.xml.selector_menu_preferences);
  }

  @Override
  protected String getOrderKey() {
    return ItemOrder.READING_CONTROLS_KEY;
  }

  @Override
  protected int getNotInListTitle() {
    return R.string.item_list_not_in_reading_controls;
  }

  @Override
  protected List<String> defaultOrder(Context context) {
    return SelectorController.defaultOrderKeys(context);
  }

  @Override
  protected void hideUnavailable(PreferenceScreen screen) {
    if (!LiveRecognition.isSupported(requireContext())
        || !FeatureSupport.doesServiceHandleDoubleTap()) {
      Preference liveRecognition =
          screen.findPreference(getString(R.string.pref_selector_live_recognition_key));
      if (liveRecognition != null) {
        liveRecognition.setVisible(false);
      }
    }
    if (!FeatureSupport.doesServiceHandleDoubleTap()) {
      Preference textEntities =
          screen.findPreference(getString(R.string.pref_selector_text_entities_key));
      if (textEntities != null) {
        textEntities.setVisible(false);
      }
    }
    List<AiActions.Action> aiActions =
        FeatureSupport.doesServiceHandleDoubleTap()
            ? AiActions.all(requireContext())
            : List.of();
    Preference aiActionsControl =
        screen.findPreference(getString(R.string.pref_selector_ai_actions_key));
    if (aiActionsControl != null && aiActions.isEmpty()) {
      aiActionsControl.setVisible(false);
    }
    MultiSelectListPreference included =
        screen.findPreference(getString(R.string.pref_ai_actions_included_key));
    if (included != null) {
      if (aiActions.isEmpty()) {
        included.setVisible(false);
      } else {
        setUpIncludedAiActions(included, aiActions);
      }
    }
  }

  /**
   * Lists [actions] in the AI actions to include setting. Until the user chooses, all are included
   * and nothing is saved, so that actions added later are included too.
   */
  private void setUpIncludedAiActions(
      MultiSelectListPreference preference, List<AiActions.Action> actions) {
    Context context = requireContext();
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    CharSequence[] labels = new CharSequence[actions.size()];
    CharSequence[] values = new CharSequence[actions.size()];
    for (int i = 0; i < actions.size(); i++) {
      labels[i] = actions.get(i).getLabel();
      values[i] = actions.get(i).getValue();
    }
    preference.setPersistent(false);
    preference.setEntries(labels);
    preference.setEntryValues(values);
    @Nullable Set<String> chosen = AiActions.includedValues(prefs, context);
    Set<String> shown = new HashSet<>();
    for (AiActions.Action action : actions) {
      if (chosen == null || chosen.contains(action.getValue())) {
        shown.add(action.getValue());
      }
    }
    preference.setValues(shown);
    preference.setSummary(includedSummary(actions, shown));
    preference.setOnPreferenceChangeListener(
        (changed, newValue) -> {
          @SuppressWarnings("unchecked")
          Set<String> chosenValues = new HashSet<>((Set<String>) newValue);
          prefs.edit().putStringSet(preference.getKey(), chosenValues).apply();
          preference.setSummary(includedSummary(actions, chosenValues));
          return true;
        });
  }

  private static String includedSummary(List<AiActions.Action> actions, Set<String> values) {
    List<String> labels = new ArrayList<>();
    for (AiActions.Action action : actions) {
      if (values.contains(action.getValue())) {
        labels.add(action.getLabel());
      }
    }
    return TextUtils.join(", ", labels);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_category_selector_menu);
  }
}
