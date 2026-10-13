/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.accessibility.material.preference;

import android.content.Context;
import android.util.AttributeSet;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The class is used to inherit the {@link PreferenceCategory} for the phone platform. A category
 * made collapsible with {@link #setCollapsible} hides its preferences until it is activated.
 */
public class AccessibilitySuitePreferenceCategory extends PreferenceCategory {
  private boolean collapsible;
  private boolean collapsed;
  private @Nullable CharSequence collapsedSummary;
  private @Nullable CharSequence expandedSummary;
  private final List<Preference> hiddenByCollapse = new ArrayList<>();

  public AccessibilitySuitePreferenceCategory(Context context) {
    super(context);
  }

  public AccessibilitySuitePreferenceCategory(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Makes the category collapsible, collapsed. The summaries describe the state. Call after the
   * preferences in the category have their final visibility.
   */
  public void setCollapsible(CharSequence collapsedSummary, CharSequence expandedSummary) {
    this.collapsedSummary = collapsedSummary;
    this.expandedSummary = expandedSummary;
    collapsible = true;
    setCollapsed(true);
  }

  public boolean isCollapsed() {
    return collapsed;
  }

  public void setCollapsed(boolean collapsed) {
    if (!collapsible || collapsed == this.collapsed) {
      return;
    }
    this.collapsed = collapsed;
    if (collapsed) {
      hiddenByCollapse.clear();
      for (int i = 0; i < getPreferenceCount(); i++) {
        Preference preference = getPreference(i);
        if (preference.isVisible()) {
          hiddenByCollapse.add(preference);
          preference.setVisible(false);
        }
      }
    } else {
      for (Preference preference : hiddenByCollapse) {
        preference.setVisible(true);
      }
      hiddenByCollapse.clear();
    }
    setSummary(collapsed ? collapsedSummary : expandedSummary);
  }

  @Override
  public boolean isEnabled() {
    return collapsible || super.isEnabled();
  }

  @Override
  public boolean isSelectable() {
    return collapsible || super.isSelectable();
  }

  @Override
  protected void onClick() {
    if (collapsible) {
      setCollapsed(!collapsed);
    } else {
      super.onClick();
    }
  }
}
