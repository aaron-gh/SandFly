/*
 * Copyright (C) 2014 Google Inc.
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

package com.google.android.accessibility.utils;

import static android.view.accessibility.AccessibilityNodeInfo.FOCUS_ACCESSIBILITY;
import static android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.IntDef;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Functions to find focus.
 *
 * <p>NOTE: To give a consistent behaviour, this code should be kept in sync with the relevant
 * subset of code in the {@code CursorController} class in TalkBack.
 */
public class FocusFinder {

  private static final String TAG = "FocusFinder";
  private final AccessibilityService service;

  /** Screen focus types in accessibility. */
  @IntDef({FOCUS_INPUT, FOCUS_ACCESSIBILITY})
  @Retention(RetentionPolicy.SOURCE)
  public @interface FocusType {}

  public FocusFinder(AccessibilityService service) {
    this.service = service;
  }

  /** Returns the view that has accessibility-focus. */
  public @Nullable AccessibilityNodeInfoCompat findAccessibilityFocus() {
    return getAccessibilityFocusNode(service, false);
  }

  /** Finds the view that has the specified focus type. The type is defined in {@link FocusType}. */
  public @Nullable AccessibilityNodeInfoCompat findFocusCompat(@FocusType int focusType) {
    switch (focusType) {
      case FOCUS_ACCESSIBILITY -> {
        return FocusFinder.getAccessibilityFocusNode(service, false);
      }
      case FOCUS_INPUT -> {
        return AccessibilityNodeInfoUtils.toCompat(
            service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT));
      }
      default -> {}
    }

    return null;
  }

  /**
   * Returns the view that has accessibility focus. Android's cached copy of the view is checked
   * with the app, as it may be stale, unless {@code isCurrent} accepts the copy as it is.
   */
  public @Nullable AccessibilityNodeInfoCompat findAccessibilityFocus(
      Filter<AccessibilityNodeInfoCompat> isCurrent) {
    return getAccessibilityFocusNode(service, /* fallbackOnRoot= */ false, isCurrent);
  }

  /**
   * Returns the accessibility focus by calling {@link AccessibilityService#findFocus(int)}. If no
   * focus is found, it allows to return the root node of the active window.
   *
   * @param fallbackOnRoot true for returning the root node if no focus is found.
   */
  public static @Nullable AccessibilityNodeInfoCompat getAccessibilityFocusNode(
      AccessibilityService service, boolean fallbackOnRoot) {
    return getAccessibilityFocusNode(service, fallbackOnRoot, /* isCurrent= */ null);
  }

  private static @Nullable AccessibilityNodeInfoCompat getAccessibilityFocusNode(
      AccessibilityService service,
      boolean fallbackOnRoot,
      @Nullable Filter<AccessibilityNodeInfoCompat> isCurrent) {
    AccessibilityNodeInfo ret = null;
    AccessibilityNodeInfo focused = service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
    if (focused == null) {
      // Find the focused node from the root of the active window, as a alternative method if
      // couldn't find the focused node by AccessibilityService.
      AccessibilityNodeInfo root = service.getRootInActiveWindow();
      if (root != null) {
        focused = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
      }
    }

    if (focused != null) {
      // If the focused node is from WebView, we still need to return it even though it's not
      // visible to user. REFERTO for details.
      if (focused.isVisibleToUser()
          || WebInterfaceUtils.isWebContainer(AccessibilityNodeInfoUtils.toCompat(focused))) {
        ret = focused;
        focused = null;
      }
    }

    boolean fromFocus = ret != null;
    if (ret == null && fallbackOnRoot) {
      ret = service.getRootInActiveWindow();
      if (ret == null) {
        LogUtils.e(TAG, "No current window root");
      }
    }

    if (ret != null) {
      AccessibilityNodeInfoCompat node = AccessibilityNodeInfoUtils.toCompat(ret);
      // When AccessibilityNodeProvider is used, the returned node may be stale.
      boolean refreshed = (isCurrent != null && isCurrent.accept(node)) || ret.refresh();
      if (fromFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
          && WebInterfaceUtils.supportsWebActions(node)
          && (!refreshed || !ret.isAccessibilityFocused())) {
        // The app has moved focus, but its event can still be behind another swipe on our
        // main thread. Refresh succeeding only proves this old item exists. Reusing it would
        // ask the browser for the same next item twice and consume a swipe without advancing.
        service.clearCachedSubtree(ret);
        AccessibilityNodeInfo current = service.findFocus(FOCUS_ACCESSIBILITY);
        LogUtils.d(TAG, "Re-read stale web focus in window %d old=%d new=%d changed=%s currentFocused=%s oldExists=%s",
            ret.getWindowId(), ret.hashCode(), current == null ? 0 : current.hashCode(),
            current != null && !ret.equals(current), current != null && current.isAccessibilityFocused(), refreshed);
        // Browsers can clear focus while keeping the old item alive. It remains the
        // navigation pivot in that case, as before; only a removed item is discarded.
        if (current == null) return refreshed ? node : null;
        AccessibilityNodeInfoCompat currentNode = AccessibilityNodeInfoCompat.wrap(current);
        // A replacement still follows the visibility rule used for the first result above.
        return current.isVisibleToUser() || WebInterfaceUtils.isWebContainer(currentNode)
            ? currentNode : null;
      }
      if (!refreshed) return null;
      return node;
    }

    return null;
  }
}
