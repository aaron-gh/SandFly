/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.utils;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class NodeOverrides {

  public interface Source {
    @Nullable Integer role(AccessibilityNodeInfoCompat node);

    @Nullable CharSequence state(AccessibilityNodeInfoCompat node);

    boolean hidden(AccessibilityNodeInfoCompat node);

    @Nullable Boolean heading(AccessibilityNodeInfoCompat node);

    boolean grouped(AccessibilityNodeInfoCompat node);

    @Nullable Order order(AccessibilityNodeInfoCompat node);
  }

  public interface Order {
    boolean isBefore();

    boolean isTarget(AccessibilityNodeInfoCompat node);
  }

  private static volatile @Nullable Source source;

  private NodeOverrides() {}

  public static void set(@Nullable Source newSource) {
    source = newSource;
  }

  static @Nullable Integer role(AccessibilityNodeInfoCompat node) {
    Source current = source;
    return current == null ? null : current.role(node);
  }

  static @Nullable CharSequence state(AccessibilityNodeInfoCompat node) {
    Source current = source;
    return current == null ? null : current.state(node);
  }

  static boolean hidden(AccessibilityNodeInfoCompat node) {
    Source current = source;
    return current != null && current.hidden(node);
  }

  static @Nullable Boolean heading(AccessibilityNodeInfoCompat node) {
    Source current = source;
    return current == null ? null : current.heading(node);
  }

  static boolean grouped(AccessibilityNodeInfoCompat node) {
    Source current = source;
    return current != null && current.grouped(node);
  }

  public static @Nullable Order order(AccessibilityNodeInfoCompat node) {
    Source current = source;
    return current == null ? null : current.order(node);
  }
}
