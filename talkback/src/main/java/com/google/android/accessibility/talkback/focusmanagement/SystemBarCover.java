/*
 * Copyright 2026 SandFly contributors
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

package com.google.android.accessibility.talkback.focusmanagement;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityWindowInfoCompat;
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils;
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Text that a system bar window, such as the navigation bar, covers. Android reports such text as
 * not visible to the user, though the app shows it and can't scroll it out from under the bar.
 */
public final class SystemBarCover {
  /** How long the bounds of the system windows read last are used again. */
  private static final long CACHE_MS = 500;

  private static List<Rect> cachedBars = Collections.emptyList();
  private static long cachedAt = Long.MIN_VALUE;

  private SystemBarCover() {}

  /**
   * Bounds of the system windows on screen, such as the navigation and status bars. Read again at
   * most every {@link #CACHE_MS}, as one description can ask for each item in a row.
   */
  public static synchronized List<Rect> systemBarBounds(@Nullable AccessibilityService service) {
    if (service == null) {
      return Collections.emptyList();
    }
    long now = SystemClock.uptimeMillis();
    if (cachedAt != Long.MIN_VALUE && now - cachedAt < CACHE_MS) {
      return cachedBars;
    }
    List<Rect> bars = new ArrayList<>();
    for (AccessibilityWindowInfo window : AccessibilityServiceCompatUtils.getWindows(service)) {
      if (window != null
          && AccessibilityWindowInfoUtils.getType(window)
              == AccessibilityWindowInfoCompat.TYPE_SYSTEM) {
        Rect bounds = AccessibilityWindowInfoUtils.getBounds(window);
        if (bounds != null && !bounds.isEmpty()) {
          bars.add(bounds);
        }
      }
    }
    cachedBars = Collections.unmodifiableList(bars);
    cachedAt = now;
    return cachedBars;
  }

  /**
   * Returns whether {@code node}, which isn't visible to the user, lies inside {@code within}, such
   * as the list or row it is in, and under one of {@code barBounds}: a bar covers it, rather than
   * the app having hidden or scrolled it away.
   */
  public static boolean isCovered(
      AccessibilityNodeInfoCompat node, @Nullable Rect within, List<Rect> barBounds) {
    if (within == null || barBounds.isEmpty()) {
      return false;
    }
    Rect bounds = new Rect();
    node.getBoundsInScreen(bounds);
    if (bounds.isEmpty() || !within.contains(bounds)) {
      return false;
    }
    for (Rect bar : barBounds) {
      if (Rect.intersects(bar, bounds)) {
        return true;
      }
    }
    return false;
  }
}
