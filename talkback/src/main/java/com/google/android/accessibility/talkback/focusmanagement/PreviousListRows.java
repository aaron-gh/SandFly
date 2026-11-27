/*
 * Copyright 2026 SandFly contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.accessibility.talkback.focusmanagement;

import android.graphics.Rect;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Recently exposed rows of an indexed list. Used only to choose where to bring a row into view. */
final class PreviousListRows {
  private static final int MAX_ROWS = 48;
  private static final int MAX_VISIBLE_ROWS = 18;
  private static final long READ_BUDGET_MS = 8;
  private static final long MAX_AGE_MS = 30_000;

  private @Nullable AccessibilityNodeInfoCompat list;
  private final Rect viewport = new Rect();
  private final LinkedHashMap<Integer, Integer> heights = new LinkedHashMap<>();
  private int rowCount;
  private long lastSeen;

  @SuppressWarnings("deprecation") // The screen bounds clip partly visible rows.
  void remember(AccessibilityNodeInfoCompat list, int rowCount, Rect bounds) {
    if (!appliesTo(list, rowCount, bounds)) {
      this.list = AccessibilityNodeInfoCompat.obtain(list);
      this.rowCount = rowCount;
      viewport.set(bounds);
      heights.clear();
    }
    lastSeen = SystemClock.uptimeMillis();
    long deadline = lastSeen + READ_BUDGET_MS;
    Rect size = new Rect();
    Rect visible = new Rect();
    int children = Math.min(list.getChildCount(), MAX_VISIBLE_ROWS);
    for (int i = 0; i < children && SystemClock.uptimeMillis() <= deadline; i++) {
      AccessibilityNodeInfoCompat child = list.getChild(i);
      if (child == null || !child.isVisibleToUser()) {
        continue;
      }
      CollectionItemInfoCompat item = child.getCollectionItemInfo();
      if (item == null || item.getColumnIndex() != 0 || item.getRowSpan() != 1
          || item.getRowIndex() < 0 || item.getRowIndex() >= rowCount) {
        continue;
      }
      child.getBoundsInParent(size);
      child.getBoundsInScreen(visible);
      int row = item.getRowIndex();
      heights.remove(row);
      if (size.isEmpty() || visible.isEmpty() || !bounds.contains(visible)
          || size.width() != visible.width() || size.height() >= bounds.height()
          || visible.height() > size.height()) {
        continue;
      }
      heights.put(row, size.height());
      if (heights.size() > MAX_ROWS) {
        heights.remove(heights.keySet().iterator().next());
      }
    }
  }

  int anchorFor(AccessibilityNodeInfoCompat list, int rowCount, Rect bounds, int wantedRow) {
    if (!appliesTo(list, rowCount, bounds)) {
      return -1;
    }
    return previousViewportAnchor(wantedRow, bounds.height(), heights);
  }

  private boolean appliesTo(AccessibilityNodeInfoCompat list, int rowCount, Rect bounds) {
    return this.list != null && this.list.equals(list) && this.rowCount == rowCount
        && viewport.equals(bounds) && SystemClock.uptimeMillis() - lastSeen <= MAX_AGE_MS;
  }

  @VisibleForTesting
  static int previousViewportAnchor(int wantedRow, int height,
      Map<Integer, Integer> rowHeights) {
    int remaining = height;
    int anchor = -1;
    for (int row = wantedRow; row >= 0; row--) {
      Integer size = rowHeights.get(row);
      if (size == null || size <= 0 || size >= remaining) {
        break;
      }
      anchor = row;
      remaining -= size;
    }
    return anchor;
  }
}
