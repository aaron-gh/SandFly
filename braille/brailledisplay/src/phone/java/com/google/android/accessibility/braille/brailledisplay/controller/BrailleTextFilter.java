/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.braille.brailledisplay.controller;

import androidx.annotation.Nullable;

/**
 * Changes the text shown on a braille display before it is translated, such as with the entries of
 * the braille dictionary. Text being typed is shown as it is.
 */
public final class BrailleTextFilter {

  /** Returns the text to show instead of {@code text}, or null to show it as it is. */
  public interface Filter {
    @Nullable
    CharSequence apply(CharSequence text);
  }

  @Nullable private static volatile Filter filter;

  private BrailleTextFilter() {}

  /** Sets the filter, or null for none. */
  public static void set(@Nullable Filter newFilter) {
    filter = newFilter;
  }

  /** Returns {@code text} as the filter changes it. */
  static CharSequence apply(CharSequence text) {
    Filter current = filter;
    if (current == null || text == null) {
      return text;
    }
    try {
      CharSequence changed = current.apply(text);
      return changed == null ? text : changed;
    } catch (RuntimeException e) {
      return text;
    }
  }
}
