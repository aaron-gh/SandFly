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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.google.android.accessibility.material.preference.AccessibilitySuiteDialogPreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.commands.Commands;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.collect.ImmutableList;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A {@link DialogPreference} which contains a list for all TalkBack supported actions. It works
 * with {@link GesturePreferenceFragmentCompat} to provide a customized dialog for gesture setting.
 *
 * <p><b>Use {@link #createDialogFragment()} to create the dialog fragment.<b/>
 */
public final class GestureListPreference extends AccessibilitySuiteDialogPreference {
  private static final String TAG = "GestureListPreference";

  /** Type of view style for the action list. */
  @IntDef({TYPE_TITLE, TYPE_ACTION_ITEM})
  @Retention(RetentionPolicy.SOURCE)
  private @interface ActionViewType {}

  /** A category of actions. */
  static final int TYPE_TITLE = 0;

  /** A supported action. */
  static final int TYPE_ACTION_ITEM = 1;

  // Every item in the page, contains titles and actions.
  private ImmutableList<ActionItem> items;
  private String initialValue;
  // A customized summary which is used when the preference is in disable state
  @Nullable private String summaryWhenDisabled;

  public GestureListPreference(Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    // Change preference title to multi-line style.
    setSingleLineTitle(false);
  }

  @Override
  public CharSequence getSummary() {
    return getCurrentActionText();
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    initialValue = a.getString(index);
    return initialValue;
  }

  /** Updates the summary of preference to default value. */
  void updateSummaryToDefaultValue() {
    String actionString = GestureShortcutMapping.getActionString(getContext(), initialValue);
    setSummary(actionString);
  }

  /**
   * Creates a fragment to show the available action items for this gesture preference. The style of
   * this fragment is subject to the form factor.
   *
   * <p>The base classes for the {@link GesturePreferenceFragmentCompat} are as follows:
   *
   * <ul>
   *   <li>Handset: {@link androidx.preference.PreferenceDialogFragmentCompat}
   *   <li>Auto: {@link androidx.preference.PreferenceDialogFragmentCompat}
   *   <li>Wear: {@link androidx.preference.PreferenceFragmentCompat}
   * </ul>
   */
  public GesturePreferenceFragmentCompat createDialogFragment() {
    return GesturePreferenceFragmentCompat.create(this);
  }

  /** Returns all commands by category, as {@link Commands} lists them. */
  public ActionItem[] getActionItems() {
    populateActionItems();
    return items.toArray(new ActionItem[items.size()]);
  }

  /** The setter of the preference value. */
  public void setValue(String value) {
    SharedPreferences sharedPreferences = SharedPreferencesUtils.getSharedPreferences(getContext());
    sharedPreferences.edit().putString(getKey(), value).apply();
  }

  /** The setter of the summary for disabled case. */
  public void setSummaryWhenDisabled(String summary) {
    summaryWhenDisabled = summary;
  }

  /**
   * Returns the current value of the preference. For example, "READ_FROM_TOP" if the preference is
   * pointed to the action "Read from top".
   */
  public String getCurrentValue() {
    String targetValue =
        SharedPreferencesUtils.getSharedPreferences(getContext()).getString(getKey(), initialValue);

    // returns "TALKBACK_BREAKOUT" if targetText is "LOCAL_BREAKOUT"
    if (TextUtils.equals(
        targetValue, getContext().getString(R.string.shortcut_value_local_breakout))) {
      return getContext().getString(R.string.shortcut_value_talkback_breakout);
    }
    // returns "SHOW_CUSTOM_ACTIONS" if targetText is "EDITING"
    if (TextUtils.equals(targetValue, getContext().getString(R.string.shortcut_value_editing))) {
      return getContext().getString(R.string.shortcut_value_show_custom_actions);
    }

    return targetValue;
  }

  public String getDefaultValue() {
    return initialValue;
  }

  /** Returns the text of the action for the current preference value. */
  private String getCurrentActionText() {
    if (!isEnabled() && summaryWhenDisabled != null) {
      return summaryWhenDisabled;
    }
    String value = getCurrentValue();
    if (TextUtils.equals(value, getContext().getString(R.string.shortcut_value_unassigned))) {
      return getContext().getResources().getString(R.string.shortcut_unassigned);
    }
    @Nullable String label = Commands.label(getContext(), value);
    if (label != null) {
      return label;
    }
    LogUtils.w(TAG, "%s - Can't find the value from supported action list.", getTitle());
    return getContext().getResources().getString(R.string.shortcut_unassigned);
  }

  private void populateActionItems() {
    ImmutableList.Builder<ActionItem> builder = ImmutableList.builder();
    builder.add(
        new ActionItem(
            getContext().getResources().getString(R.string.shortcut_unassigned),
            getContext().getResources().getString(R.string.shortcut_value_unassigned),
            TYPE_ACTION_ITEM));
    for (Commands.Category category : Commands.categories(getContext())) {
      builder.add(new ActionItem(getContext().getString(category.getTitleRes()), TYPE_TITLE));
      for (Commands.Command command : category.getCommands()) {
        builder.add(new ActionItem(command.getLabel(), command.getValue(), TYPE_ACTION_ITEM));
      }
    }
    items = builder.build();
  }

  /**
   * Represents a TalkBack action item. It implements {@link Parcelable} to pass the value by
   * arguments of a fragment.
   */
  public static final class ActionItem implements Parcelable {
    public static final Parcelable.Creator<ActionItem> CREATOR =
        new Parcelable.Creator<ActionItem>() {
          @Override
          public ActionItem createFromParcel(Parcel in) {
            String text = in.readString();
            String value = in.readString();
            int viewType = in.readInt();
            return new ActionItem(text, value, viewType);
          }

          @Override
          public ActionItem[] newArray(int size) {
            return new ActionItem[size];
          }
        };

    private static final String ACTION_NO_VALUE = "";
    final String text;
    final String value;
    @ActionViewType final int viewType;

    ActionItem(String text, @ActionViewType int viewType) {
      this(text, ACTION_NO_VALUE, viewType);
    }

    ActionItem(String text, String value, @ActionViewType int viewType) {
      this.text = text;
      this.value = value;
      this.viewType = viewType;
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeString(text);
      dest.writeString(value);
      dest.writeInt(viewType);
    }

    private static ImmutableList<ActionItem> createItemsFromArray(String[] texts, String[] values) {
      if (texts.length != values.length) {
        LogUtils.e(TAG, "createItemsFromArray : Length doesn't match.");
        return ImmutableList.of();
      }

      if (texts.length == 0) {
        LogUtils.e(TAG, "createItemsFromArray : Empty array");
        return ImmutableList.of();
      }

      ImmutableList.Builder<ActionItem> listBuilder = ImmutableList.builder();
      for (int i = 0; i < texts.length; ++i) {
        listBuilder.add(new ActionItem(texts[i], values[i], TYPE_ACTION_ITEM));
      }
      return listBuilder.build();
    }

    @Override
    public String toString() {
      return "ActionItem={ "
          + StringBuilderUtils.joinFields(
              StringBuilderUtils.optionalText("title", text),
              StringBuilderUtils.optionalText("value", value),
              StringBuilderUtils.optionalInt("viewType", viewType, /* defaultValue= */ -1))
          + "}";
    }
  }
}
