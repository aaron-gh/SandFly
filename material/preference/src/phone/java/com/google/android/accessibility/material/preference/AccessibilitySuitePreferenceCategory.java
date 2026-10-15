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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The class is used to inherit the {@link PreferenceCategory} for the phone platform. A category
 * made collapsible with {@link #setCollapsible} hides its preferences until it is activated. The
 * state is exposed to accessibility services as an expand and collapse action, a button role and a
 * state description, and is kept per category key.
 */
public class AccessibilitySuitePreferenceCategory extends PreferenceCategory {
  private static final String STATE_FILE = "collapsible_categories";

  private boolean collapsible;
  private boolean collapsed;
  private boolean enabledByCaller = true;
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
   * Makes the category collapsible. It starts collapsed unless the user expanded it before. The
   * summaries describe the state. Call after the preferences in the category have their final
   * visibility.
   */
  public void setCollapsible(CharSequence collapsedSummary, CharSequence expandedSummary) {
    this.collapsedSummary = collapsedSummary;
    this.expandedSummary = expandedSummary;
    collapsible = true;
    collapsed = !loadExpanded();
    if (collapsed) {
      applyCollapsed();
    }
    updateSummary();
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
      applyCollapsed();
    } else {
      for (Preference preference : hiddenByCollapse) {
        preference.setVisible(true);
      }
      hiddenByCollapse.clear();
    }
    saveExpanded(!collapsed);
    updateSummary();
  }

  /** Hides the visible preferences. Preferences shown again while collapsed are hidden again. */
  private void applyCollapsed() {
    for (int i = 0; i < getPreferenceCount(); i++) {
      Preference preference = getPreference(i);
      if (preference.isVisible()) {
        if (!hiddenByCollapse.contains(preference)) {
          hiddenByCollapse.add(preference);
        }
        preference.setVisible(false);
      }
    }
  }

  private boolean hasVisibleChildren() {
    for (int i = 0; i < getPreferenceCount(); i++) {
      if (getPreference(i).isVisible()) {
        return true;
      }
    }
    return false;
  }

  /** Whether the category has anything to expand or collapse. */
  private boolean isToggleable() {
    return collapsible && (!hiddenByCollapse.isEmpty() || hasVisibleChildren());
  }

  private void updateSummary() {
    setSummary(collapsed ? collapsedSummary : expandedSummary);
  }

  private @Nullable SharedPreferences stateStore() {
    return getKey() == null
        ? null
        : getContext().getSharedPreferences(STATE_FILE, Context.MODE_PRIVATE);
  }

  private boolean loadExpanded() {
    SharedPreferences store = stateStore();
    return store != null && store.getBoolean(getKey(), false);
  }

  private void saveExpanded(boolean expanded) {
    SharedPreferences store = stateStore();
    if (store != null) {
      store.edit().putBoolean(getKey(), expanded).apply();
    }
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {
    super.onBindViewHolder(holder);
    View view = holder.itemView;
    if (!collapsible) {
      return;
    }
    if (collapsed) {
      // Hides preferences shown while the category was collapsed.
      applyCollapsed();
    }
    // The state is spoken as the state description, not as the summary text.
    View summaryView = holder.findViewById(android.R.id.summary);
    if (summaryView != null) {
      summaryView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    // The list installs its own delegate after binding, so wrap that one once it is in place.
    view.post(
        () -> {
          AccessibilityDelegateCompat current = ViewCompat.getAccessibilityDelegate(view);
          if (current instanceof CategoryDelegate) {
            current = ((CategoryDelegate) current).inner;
          }
          ViewCompat.setAccessibilityDelegate(view, new CategoryDelegate(current));
        });
  }

  /** Exposes the button role, the expanded state and the expand and collapse actions. */
  private class CategoryDelegate extends AccessibilityDelegateCompat {
    private final @Nullable AccessibilityDelegateCompat inner;

    CategoryDelegate(@Nullable AccessibilityDelegateCompat inner) {
      this.inner = inner;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
      if (inner != null) {
        inner.onInitializeAccessibilityNodeInfo(host, info);
      } else {
        super.onInitializeAccessibilityNodeInfo(host, info);
      }
      info.setClassName(Button.class.getName());
      if (!isToggleable()) {
        return;
      }
      info.addAction(
          collapsed
              ? AccessibilityActionCompat.ACTION_EXPAND
              : AccessibilityActionCompat.ACTION_COLLAPSE);
      if (Build.VERSION.SDK_INT >= 36) {
        info.unwrap()
            .setExpandedState(
                collapsed
                    ? AccessibilityNodeInfo.EXPANDED_STATE_COLLAPSED
                    : AccessibilityNodeInfo.EXPANDED_STATE_FULL);
      }
    }

    @Override
    public boolean performAccessibilityAction(View host, int action, @Nullable Bundle args) {
      if (isToggleable()
          && (action == AccessibilityNodeInfoCompat.ACTION_EXPAND
              || action == AccessibilityNodeInfoCompat.ACTION_COLLAPSE)) {
        setCollapsed(action == AccessibilityNodeInfoCompat.ACTION_COLLAPSE);
        return true;
      }
      return inner != null
          ? inner.performAccessibilityAction(host, action, args)
          : super.performAccessibilityAction(host, action, args);
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    enabledByCaller = enabled;
    super.setEnabled(enabled);
  }

  @Override
  public boolean isEnabled() {
    return collapsible ? enabledByCaller : super.isEnabled();
  }

  @Override
  public boolean isSelectable() {
    return isToggleable() ? enabledByCaller : super.isSelectable();
  }

  @Override
  protected void onClick() {
    if (isToggleable()) {
      setCollapsed(!collapsed);
    } else {
      super.onClick();
    }
  }
}
