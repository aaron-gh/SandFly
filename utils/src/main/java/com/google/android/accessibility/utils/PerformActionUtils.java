/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.libraries.accessibility.utils.log.LogUtils;

/** Used to perform an action on a AccessibilityNodeInfoCompat and log relevant information. */
public class PerformActionUtils {
  private static final String TAG = "PerformActionUtils";

  /** Whether an app may move the cursor or change the text of a field after {@code action}. */
  private static boolean changesTextOrCursor(int action) {
    return action == AccessibilityNodeInfoCompat.ACTION_CLICK
        || action == AccessibilityNodeInfoCompat.ACTION_LONG_CLICK
        || action == AccessibilityNodeInfoCompat.ACTION_SET_SELECTION
        || action == AccessibilityNodeInfoCompat.ACTION_SET_TEXT
        || action == AccessibilityNodeInfoCompat.ACTION_CUT
        || action == AccessibilityNodeInfoCompat.ACTION_PASTE
        || action == AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
        || action == AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY;
  }

  // Far in the past, without overflowing when subtracted from an event time.
  private static volatile long lastActionOutsideKeyboard = Long.MIN_VALUE / 2;

  /**
   * When SandFly last clicked an item or moved a cursor outside the keyboard, in uptime
   * milliseconds: the cursor moves and text changes that follow are SandFly's, not the keyboard's.
   */
  public static long lastActionOutsideKeyboard() {
    return lastActionOutsideKeyboard;
  }

  public static boolean performAction(
      @Nullable AccessibilityNodeInfoCompat node, int action, @Nullable EventId eventId) {
    return performAction(node, action, null /* args */, eventId);
  }

  public static boolean performAction(
      @Nullable AccessibilityNodeInfoCompat node,
      int action,
      Bundle args,
      @Nullable EventId eventId) {
    if (node == null) {
      return false;
    }

    boolean result = node.performAction(action, args);
    if (result && changesTextOrCursor(action) && !AccessibilityNodeInfoUtils.isKeyboard(node)) {
      lastActionOutsideKeyboard = SystemClock.uptimeMillis();
    }
    LogUtils.d(
        TAG,
        "perform action=%d=%s returns %s with args=%s on node=%s for event=%s",
        action,
        AccessibilityNodeInfoUtils.actionToString(action),
        result,
        args,
        node,
        eventId);

    return result;
  }

  public static boolean showOnScreen(
      @Nullable AccessibilityNodeInfoCompat node, @Nullable EventId eventId) {
    return performAction(node, AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId(), eventId);
  }

  public static boolean isNodeSupportAction(AccessibilityNodeInfoCompat node, int actionId) {
    if (node == null) {
      return false;
    }

    for (AccessibilityActionCompat action : node.getActionList()) {
      if (actionId == action.getId()) {
        return true;
      }
    }

    return false;
  }
}
