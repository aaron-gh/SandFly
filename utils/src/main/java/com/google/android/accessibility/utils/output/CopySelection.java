/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.google.android.accessibility.utils.output;

import android.text.SpannableStringBuilder;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Text selected in copying mode. The selection is kept here, not in the app, so it works in text
 * the app does not let a selection be set in, and across items.
 *
 * <p>Only text that the text cursor was moved over is selected. Moving the text cursor within an
 * item moves the end of a range in that item. Moving to another item, or to somewhere in an item
 * other than where the cursor was left, starts another range there; the text between is not
 * selected. The ranges since copying mode was started are a part. Ending copying mode keeps the
 * part, and the next one is another part. Copying gives all the parts, in the order they were
 * selected, one range to a line, without whitespace at the ends of each range.
 */
public final class CopySelection {

  /** Text selected and text no longer selected by one move. Either may be empty. */
  public static final class Change {
    public final CharSequence selected;
    public final CharSequence deselected;

    Change(CharSequence selected, CharSequence deselected) {
      this.selected = selected;
      this.deselected = deselected;
    }
  }

  /** A range of the text of one item. */
  private static final class Range {
    final AccessibilityNodeInfoCompat node;
    final CharSequence text;
    int anchor;
    int cursor;

    Range(AccessibilityNodeInfoCompat node, CharSequence text, int anchor, int cursor) {
      this.node = node;
      this.text = text;
      this.anchor = anchor;
      this.cursor = cursor;
    }

    int start() {
      return Math.min(anchor, cursor);
    }

    int end() {
      return Math.max(anchor, cursor);
    }

    CharSequence selected() {
      return text.subSequence(start(), end());
    }
  }

  private static final String SEPARATOR = "\n";

  /** Most parts and most text kept, so that a long session cannot use up memory. */
  private static final int MAX_PARTS = 500;

  private static final int MAX_LENGTH = 1_000_000;

  /** Parts that have been ended, in the order selected. */
  private final List<CharSequence> parts = new ArrayList<>();

  private int length;

  /** Ranges of the part that is open, in the order they are on the screen. */
  private final List<Range> ranges = new ArrayList<>();

  /** The range that moving the text cursor moves the end of, if the move carries on from it. */
  private @Nullable Range current;

  private boolean active;

  /** Starts a part. Parts already selected are kept, and so is a part that is already open. */
  public void start() {
    active = true;
  }

  /** Whether moving the text cursor selects. */
  public boolean isActive() {
    return active;
  }

  /** Whether there is text to copy: a part, or a range with text in it. */
  public boolean hasContent() {
    return !parts.isEmpty() || selectedText().length() > 0;
  }

  public int partCount() {
    return parts.size();
  }

  /**
   * Selects what a move of the text cursor from {@code from} to {@code to} in {@code text} of
   * {@code node} moved over. If the move carries on from where the last one in the same item
   * ended, only whitespace being between, the last range is moved on or back; otherwise it is a
   * range of its own, after the last one if the move is forward and before it if it is backward.
   *
   * @return what the move selected and deselected, or null if copying mode is not on.
   */
  public @Nullable Change move(
      @Nullable AccessibilityNodeInfoCompat node,
      @Nullable CharSequence text,
      int from,
      int to,
      boolean forward) {
    if (!active || node == null || node.isPassword() || text == null || text.length() == 0) {
      return null;
    }
    int textLength = text.length();
    int low = clamp(Math.min(from, to), textLength);
    int high = clamp(Math.max(from, to), textLength);
    int start = forward ? low : high;
    int end = forward ? high : low;

    @Nullable Range range = current;
    if (range != null && continuesFrom(range, node, text, low, high, forward)) {
      int oldStart = range.start();
      int oldEnd = range.end();
      range.cursor = end;
      return new Change(
          difference(range.text, range.start(), range.end(), oldStart, oldEnd),
          difference(range.text, oldStart, oldEnd, range.start(), range.end()));
    }
    if (length + openLength() + (high - low) > MAX_LENGTH || ranges.size() >= MAX_PARTS) {
      return null;
    }
    range = new Range(node, text, start, end);
    int index = current == null ? ranges.size() : ranges.indexOf(current);
    ranges.add(forward || current == null ? Math.min(index + 1, ranges.size()) : index, range);
    current = range;
    return new Change(range.selected(), "");
  }

  /**
   * Selects all the text of {@code items}, in the open part. Ranges already in it in other items
   * are kept, and ranges in these items are replaced.
   *
   * @return the text selected, which is empty if there is none.
   */
  public CharSequence selectAll(
      List<AccessibilityNodeInfoCompat> items, List<CharSequence> itemTexts) {
    SpannableStringBuilder result = new SpannableStringBuilder();
    for (int i = 0; i < items.size(); i++) {
      AccessibilityNodeInfoCompat item = items.get(i);
      CharSequence text = itemTexts.get(i);
      if (text == null || text.length() == 0 || item.isPassword()) {
        continue;
      }
      for (int r = ranges.size() - 1; r >= 0; r--) {
        if (ranges.get(r).node.equals(item)) {
          ranges.remove(r);
        }
      }
      if (length + openLength() + text.length() > MAX_LENGTH || ranges.size() >= MAX_PARTS) {
        continue;
      }
      current = new Range(item, text, 0, text.length());
      ranges.add(current);
      active = true;
      appendLine(result, trim(text));
    }
    return result;
  }

  private int openLength() {
    int total = 0;
    for (Range range : ranges) {
      total += range.end() - range.start();
    }
    return total;
  }

  /** The text of the open part, one range to a line. Empty if there is none. */
  public CharSequence selectedText() {
    SpannableStringBuilder result = new SpannableStringBuilder();
    for (Range range : ranges) {
      appendLine(result, trim(range.selected()));
    }
    return result;
  }

  /**
   * Ends the open part, keeping it if it has text.
   *
   * @return the text of the part, which is empty if there is none or it could not be kept.
   */
  public CharSequence end() {
    CharSequence selected = selectedText();
    active = false;
    ranges.clear();
    current = null;
    if (selected.length() == 0
        || parts.size() >= MAX_PARTS
        || length + selected.length() > MAX_LENGTH) {
      return "";
    }
    parts.add(selected);
    length += selected.length();
    return selected;
  }

  /** Ends copying mode and drops the open part, as when selection mode is turned off. */
  public void abandon() {
    active = false;
    ranges.clear();
    current = null;
  }

  /** The parts and the open part, one range to a line. */
  public CharSequence allText() {
    SpannableStringBuilder result = new SpannableStringBuilder();
    for (CharSequence part : parts) {
      appendLine(result, part);
    }
    appendLine(result, selectedText());
    return result;
  }

  /** Drops the parts and ends copying mode. */
  public void clear() {
    active = false;
    parts.clear();
    length = 0;
    ranges.clear();
    current = null;
  }

  /**
   * Whether a move over {@code [low, high)} of {@code text} of {@code node} carries on from where
   * {@code range} ends: the same text of the same item, with the end of the range where the move
   * began or behind it, in the direction of travel. What is between was moved over, so it is
   * selected too: moving by word passes spaces and punctuation, as in "555-0100". A move whose
   * range is on the other side of the end of the range, as when the text cursor was reset, is a
   * range of its own.
   */
  private static boolean continuesFrom(
      Range range,
      AccessibilityNodeInfoCompat node,
      CharSequence text,
      int low,
      int high,
      boolean forward) {
    if (!range.node.equals(node) || !range.text.toString().contentEquals(text)) {
      return false;
    }
    return forward ? range.cursor <= high : range.cursor >= low;
  }

  /** The text without whitespace at its ends, which a move over a word's edge brings in. */
  private static CharSequence trim(CharSequence text) {
    int start = 0;
    int end = text.length();
    while (start < end && Character.isWhitespace(text.charAt(start))) {
      start++;
    }
    while (end > start && Character.isWhitespace(text.charAt(end - 1))) {
      end--;
    }
    return text.subSequence(start, end);
  }

  private static void appendLine(SpannableStringBuilder result, CharSequence text) {
    if (text.length() == 0) {
      return;
    }
    if (result.length() > 0) {
      result.append(SEPARATOR);
    }
    result.append(text);
  }

  private static int clamp(int index, int max) {
    return Math.max(0, Math.min(index, max));
  }

  /** The text in {@code [start, end)} that is not in {@code [otherStart, otherEnd)}. */
  private static CharSequence difference(
      CharSequence text, int start, int end, int otherStart, int otherEnd) {
    if (start >= end) {
      return "";
    }
    if (end <= otherStart || start >= otherEnd) {
      return text.subSequence(start, end);
    }
    if (start < otherStart) {
      return text.subSequence(start, otherStart);
    }
    if (end > otherEnd) {
      return text.subSequence(otherEnd, end);
    }
    return "";
  }
}
