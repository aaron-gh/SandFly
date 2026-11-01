/*
 * Copyright (C) 2020 Google Inc.
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

import static com.google.android.accessibility.talkback.preference.base.GestureListPreference.TYPE_ACTION_ITEM;
import static com.google.android.accessibility.talkback.preference.base.GestureListPreference.TYPE_TITLE;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.RecyclerView.Recycler;
import androidx.recyclerview.widget.RecyclerView.State;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.NonNull;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory;
import com.google.android.accessibility.material.preference.AccessibilitySuiteRadioButtonPreference;
import com.google.android.accessibility.talkback.preference.base.GestureListPreference.ActionItem;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A fragment contains a customized wear material list view for TalkBack supported actions.
 *
 * <p>Note: This class is only for Wear.
 */
public class GesturePreferenceFragmentCompat extends TalkbackBaseFragment {
  private static final String TAG = "GesturePreferenceFragmentCompat";

  private static final String GESTURE_TITLE = "GESTURE_TITLE";
  private static final String GESTURE_INITIAL_VALUE = "GESTURE_INITIAL_VALUE";
  private static final String GESTURE_ACTION_ITEMS = "GESTURE_ACTION_ITEMS";

  private Parcelable[] items;
  private CharSequence title;
  private String initialValue;
  private String preferenceKey;

  private final OnPreferenceChangeListener onPreferenceChangeListener =
      new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
          boolean checked = (Boolean) newValue;
          if (!checked) {
            // The selected action is clicked again and it is going to be unchecked. We should
            // return false to ignore it and don't set the value to targetGestureListPreference.
            popBackStack();
            return false;
          }

          ActionItem item = (ActionItem) items[preference.getOrder()];
          GestureListPreference.save(getContext(), preferenceKey, item.value);
          popBackStack();
          return true;
        }
      };

  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable focusSelectedPreferenceRunnable =
      () -> {
        RecyclerView recyclerView = getListView();
        if (recyclerView == null) {
          LogUtils.w(TAG, "RecyclerView has been cleared.");
          return;
        }

        ViewHolder viewHolder =
            recyclerView.findViewHolderForAdapterPosition(getSelectedPositionInAdapter());
        if (viewHolder != null) {
          viewHolder.itemView.requestFocus();
          viewHolder.itemView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }
      };
  private final Runnable scrollRunnable =
      () -> {
        RecyclerView recyclerView = getListView();
        if (recyclerView == null) {
          LogUtils.w(TAG, "RecyclerView has been cleared.");
          return;
        }

        LinearLayoutManager linearLayoutManager =
            (LinearLayoutManager) recyclerView.getLayoutManager();
        if (linearLayoutManager != null) {
          linearLayoutManager.scrollToPositionWithOffset(getSelectedPositionInAdapter(), 0);
          handler.postDelayed(focusSelectedPreferenceRunnable, 100);
        }
      };

  private int selectedPosition;

  /** Creates the fragment from given {@link GestureListPreference}. */
  @Nullable
  public static GesturePreferenceFragmentCompat create(GestureListPreference preference) {
    // Do nothing.
    return null;
  }

  public static Bundle createBundleForFragmentArguments(GestureListPreference preference) {
    Bundle bundle = new Bundle();
    bundle.putString(ARG_PREFERENCE_ROOT, preference.getKey());
    bundle.putCharSequence(GESTURE_TITLE, preference.getTitle());
    bundle.putString(GESTURE_INITIAL_VALUE, preference.getCurrentValue());
    bundle.putParcelableArray(GESTURE_ACTION_ITEMS, preference.getActionItems());
    return bundle;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (savedInstanceState == null) {
      // We post a runnable to scroll to the selected position in the 1st time.
      handler.postDelayed(scrollRunnable, 100);
    }
  }

  @NonNull
  @Override
  public LayoutManager onCreateLayoutManager() {
    return new LinearLayoutManager(requireContext()) {
      @Override
      public int getSelectionModeForAccessibility(
          @NonNull Recycler recycler, @NonNull State state) {
        return CollectionInfoCompat.SELECTION_MODE_SINGLE;
      }
    };
  }

  /** Pops back the fragment and restores the a11y importance attribute for the parent fragment. */
  private void popBackStack() {
    FragmentActivity activity = getActivity();
    if (activity == null) {
      return;
    }
    FragmentManager fragmentManager = getParentFragmentManager();
    fragmentManager.popBackStackImmediate();
    if (fragmentManager.getBackStackEntryCount() == 0) {
      activity.finish();
    }
  }

  @Override
  protected CharSequence getTitle() {
    return title;
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    Bundle bundle = getArguments();
    if (bundle == null) {
      throw new IllegalArgumentException();
    }
    title = bundle.getCharSequence(GESTURE_TITLE);
    initialValue = bundle.getString(GESTURE_INITIAL_VALUE);
    items = bundle.getParcelableArray(GESTURE_ACTION_ITEMS);
    preferenceKey = rootKey;

    PreferenceSettingsUtils.setPreferenceScreen(this, createPreferenceScreen());
  }

  private PreferenceScreen createPreferenceScreen() {
    PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(getContext());
    Context context = getContext();

    AccessibilitySuitePreferenceCategory category = null;
    ActionItem item;
    for (int order = 0; order < items.length; order++) {
      item = (ActionItem) items[order];
      switch (item.viewType) {
        case TYPE_TITLE -> {
          category = new AccessibilitySuitePreferenceCategory(getContext());
          category.setTitle(item.text);
          preferenceScreen.addPreference(category);
        }
        case TYPE_ACTION_ITEM -> {
          AccessibilitySuiteRadioButtonPreference radioButtonPreference =
              new AccessibilitySuiteRadioButtonPreference(context);
          radioButtonPreference.setTitle(item.text);
          boolean checked = TextUtils.equals(item.value, initialValue);
          radioButtonPreference.setChecked(checked);
          radioButtonPreference.setOnPreferenceChangeListener(onPreferenceChangeListener);
          radioButtonPreference.setOrder(order);
          radioButtonPreference.setSingleLineTitle(false);
          if (category == null) {
            // We create a category without title to add the beginning preference (e.g., "Tap to
            // assign").
            category = new AccessibilitySuitePreferenceCategory(getContext());
            preferenceScreen.addPreference(category);
          }
          category.addPreference(radioButtonPreference);
          if (checked) {
            selectedPosition = order;
          }
        }
        default -> {}
      }
    }

    return preferenceScreen;
  }

  private int getSelectedPositionInAdapter() {
    // In the WearPreferenceFragment, the ConcatAdapter has 2 views (the id/title and the
    // divider) before the 1st action item view. Hence, we add 2 to the position parameter.
    return selectedPosition + 2;
  }
}
