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
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.preference.ItemOrder;
import com.google.android.accessibility.talkback.recognition.LiveRecognition;
import com.google.android.accessibility.talkback.selector.SelectorController;
import com.google.android.accessibility.utils.FeatureSupport;
import java.util.List;

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
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_category_selector_menu);
  }
}
