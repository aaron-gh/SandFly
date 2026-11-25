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

package com.google.android.accessibility.braille.brailledisplay.controller;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.List;

/**
 * Finds the display position in new content that shows the text that was at the start of the
 * Braille window in the old content.
 */
final class PanPositionMatcher {
  /** Returned when the old display position is unknown. */
  static final int NO_POSITION = -1;

  /** Shortest window text that is searched for in changed text. */
  static final int MIN_SEARCH_LENGTH = 4;

  private PanPositionMatcher() {}

  /**
   * Returns the display position in the new translation for the window that started at {@code
   * oldDisplayStart} and ended at {@code oldDisplayEnd} in the old translation, or {@link
   * #NO_POSITION} if {@code oldDisplayStart} is negative.
   */
  static int matchDisplayPosition(
      CharSequence oldText,
      List<Integer> oldBrailleToText,
      List<Integer> oldTextToBraille,
      CharSequence newText,
      List<Integer> newBrailleToText,
      List<Integer> newTextToBraille,
      int oldDisplayStart,
      int oldDisplayEnd) {
    if (oldDisplayStart < 0) {
      return NO_POSITION;
    }
    String oldString = oldText.toString();
    String newString = newText.toString();
    // An unchanged translation keeps the display position as it is. Mapping it through the text
    // positions and back can land on an earlier cell and move the display backward.
    if (oldString.equals(newString) && oldBrailleToText.equals(newBrailleToText)) {
      return oldDisplayStart;
    }
    int oldTextStart = displayToText(oldBrailleToText, oldTextToBraille, oldDisplayStart);
    int oldTextEnd =
        max(oldTextStart, displayToText(oldBrailleToText, oldTextToBraille, oldDisplayEnd));
    int newTextStart = matchTextPosition(oldString, newString, oldTextStart, oldTextEnd);
    return textToDisplay(newBrailleToText, newTextToBraille, newTextStart);
  }

  /**
   * Returns the position in {@code newText} of the text at {@code oldStart} in {@code oldText}, or
   * {@link #NO_POSITION} if {@code oldStart} is negative. {@code oldEnd} is the end of the old
   * window. In order: a position before the first change or inside the unchanged end of the text
   * is kept or shifted by the length change; otherwise the old window text is searched for, nearest
   * to the old position; otherwise the old position is kept if it is inside the new text; otherwise
   * 0.
   */
  static int matchTextPosition(String oldText, String newText, int oldStart, int oldEnd) {
    if (oldStart < 0) {
      return NO_POSITION;
    }
    int oldLength = oldText.length();
    int newLength = newText.length();
    oldStart = min(oldStart, oldLength);
    oldEnd = min(max(oldEnd, oldStart), oldLength);
    if (oldText.equals(newText)) {
      return oldStart;
    }
    int prefix = commonPrefixLength(oldText, newText);
    if (oldStart < prefix) {
      return oldStart;
    }
    int suffix = commonSuffixLength(oldText, newText, min(oldLength, newLength) - prefix);
    if (suffix > 0 && oldStart >= oldLength - suffix) {
      return oldStart + newLength - oldLength;
    }
    if (oldStart == prefix) {
      // The change starts at the start of the window.
      return oldStart;
    }
    String window = oldText.substring(oldStart, oldEnd);
    int found = findNearest(newText, window, oldStart);
    if (found < 0 && window.length() >= 2 * MIN_SEARCH_LENGTH) {
      found = findNearest(newText, window.substring(0, window.length() / 2), oldStart);
    }
    if (found >= 0) {
      return found;
    }
    return oldStart < newLength ? oldStart : 0;
  }

  /**
   * Returns the start of the occurrence of {@code needle} in {@code text} nearest to {@code near},
   * or -1 if there is none or {@code needle} is too short to identify a position.
   */
  private static int findNearest(String text, String needle, int near) {
    if (needle.trim().length() < MIN_SEARCH_LENGTH) {
      return -1;
    }
    int best = -1;
    int index = text.indexOf(needle);
    while (index >= 0) {
      if (best < 0 || abs(index - near) < abs(best - near)) {
        best = index;
      } else if (index > near) {
        // Later occurrences are farther away.
        break;
      }
      index = text.indexOf(needle, index + 1);
    }
    return best;
  }

  private static int commonPrefixLength(String a, String b) {
    int limit = min(a.length(), b.length());
    int i = 0;
    while (i < limit && a.charAt(i) == b.charAt(i)) {
      i++;
    }
    return i;
  }

  private static int commonSuffixLength(String a, String b, int limit) {
    int i = 0;
    while (i < limit && a.charAt(a.length() - 1 - i) == b.charAt(b.length() - 1 - i)) {
      i++;
    }
    return i;
  }

  /** Same mapping as {@code ContentHelper#displayToTextPosition}. */
  private static int displayToText(
      List<Integer> brailleToText, List<Integer> textToBraille, int displayPosition) {
    if (displayPosition < 0) {
      return NO_POSITION;
    } else if (displayPosition >= brailleToText.size()) {
      return textToBraille.size();
    }
    return brailleToText.get(displayPosition);
  }

  /** Same mapping as {@code ContentHelper#textToDisplayPosition}. */
  private static int textToDisplay(
      List<Integer> brailleToText, List<Integer> textToBraille, int textPosition) {
    if (textPosition < 0) {
      return NO_POSITION;
    } else if (textPosition >= textToBraille.size()) {
      return brailleToText.size();
    }
    return textToBraille.get(textPosition);
  }
}
