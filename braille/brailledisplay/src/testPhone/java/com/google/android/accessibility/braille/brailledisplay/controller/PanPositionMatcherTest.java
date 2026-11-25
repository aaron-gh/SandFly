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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class PanPositionMatcherTest {

  private static List<Integer> identity(int length) {
    List<Integer> positions = new ArrayList<>();
    for (int i = 0; i < length; i++) {
      positions.add(i);
    }
    return positions;
  }

  private static int matchUncontracted(String oldText, String newText, int start, int end) {
    return PanPositionMatcher.matchDisplayPosition(
        oldText,
        identity(oldText.length()),
        identity(oldText.length()),
        newText,
        identity(newText.length()),
        identity(newText.length()),
        start,
        end);
  }

  @Test
  public void negativeStart_returnsNoPosition() {
    assertEquals(PanPositionMatcher.NO_POSITION, matchUncontracted("abc", "abd", -1, 2));
    assertEquals(
        PanPositionMatcher.NO_POSITION, PanPositionMatcher.matchTextPosition("a", "b", -1, 0));
  }

  @Test
  public void unchangedTranslation_keepsDisplayStart() {
    // Cell 2 is inside a contraction that starts at cell 1; mapping through the text would move the
    // window back to cell 1.
    List<Integer> brailleToText = Arrays.asList(0, 1, 1, 3);
    List<Integer> textToBraille = Arrays.asList(0, 1, 1, 3);
    assertEquals(
        2,
        PanPositionMatcher.matchDisplayPosition(
            "abcd", brailleToText, textToBraille, "abcd", brailleToText, textToBraille, 2, 4));
  }

  @Test
  public void textAddedAfterWindow_keepsPosition() {
    String oldText = "First paragraph of the reply. Second paragraph.";
    String newText = oldText + " Third paragraph streamed in.";
    assertEquals(30, matchUncontracted(oldText, newText, 30, 45));
  }

  @Test
  public void textChangedAfterWindowStart_keepsPosition() {
    String oldText = "Message one. Message two, sent 1 minute ago.";
    String newText = "Message one. Message two, sent 2 minutes ago.";
    assertEquals(13, matchUncontracted(oldText, newText, 13, 30));
  }

  @Test
  public void textInsertedBeforeWindow_shiftsByInsertedLength() {
    String oldText = "Header. Body text that is being read.";
    String newText = "Header, 2 new. Body text that is being read.";
    int oldStart = oldText.indexOf("that");
    assertEquals(
        newText.indexOf("that"), matchUncontracted(oldText, newText, oldStart, oldStart + 12));
  }

  @Test
  public void textRemovedBeforeWindow_shiftsByRemovedLength() {
    String oldText = "X notification. Messenger notification. The item being read.";
    String newText = "Messenger notification. The item being read.";
    int oldStart = oldText.indexOf("item");
    assertEquals(
        newText.indexOf("item"), matchUncontracted(oldText, newText, oldStart, oldStart + 10));
  }

  @Test
  public void textChangedBeforeAndAfterWindow_findsWindowText() {
    String oldText = "3 new messages. The long reply paragraph being read. Sent now";
    String newText = "4 new messages, 1 unread. The long reply paragraph being read. Sent 1m ago";
    int oldStart = oldText.indexOf("reply");
    assertEquals(
        newText.indexOf("reply"), matchUncontracted(oldText, newText, oldStart, oldStart + 20));
  }

  @Test
  public void windowTextChangedAtItsEnd_findsFirstHalf() {
    String oldText = "A: start. The reply paragraph read now. B";
    String newText = "AA: start! The reply paragraph read later. BB";
    int oldStart = oldText.indexOf("The reply");
    int oldEnd = oldText.indexOf(". B");
    assertEquals(
        newText.indexOf("The reply"), matchUncontracted(oldText, newText, oldStart, oldEnd));
  }

  @Test
  public void windowTextRepeated_usesNearestOccurrence() {
    String oldText = "1. word word word. Then the same phrase here and the same phrase here. 9";
    String newText = "12. word word word. Then the same phrase here and the same phrase here. 10";
    int oldStart = oldText.lastIndexOf("the same phrase");
    assertEquals(
        newText.lastIndexOf("the same phrase"),
        PanPositionMatcher.matchTextPosition(oldText, newText, oldStart, oldStart + 15));
  }

  @Test
  public void windowTextGone_keepsOffsetInsideNewText() {
    String oldText = "abc first content here xyz";
    String newText = "def other words entirely uvw";
    assertEquals(10, PanPositionMatcher.matchTextPosition(oldText, newText, 10, 20));
  }

  @Test
  public void windowTextGoneAndOffsetPastNewText_returnsStart() {
    String oldText = "abc a long first content that is replaced xyz";
    String newText = "def short uvw";
    assertEquals(0, PanPositionMatcher.matchTextPosition(oldText, newText, 30, 40));
  }

  @Test
  public void shortWindowText_isNotSearched() {
    // "ab" could match anywhere; the offset is kept instead.
    String oldText = "1 ab 2";
    String newText = "33 ab 444";
    assertEquals(2, PanPositionMatcher.matchTextPosition(oldText, newText, 2, 4));
  }

  @Test
  public void contractedNewText_mapsTextPositionToCell() {
    // Old text "xx so on", one cell per character, window at "so on".
    String oldText = "xx so on";
    // New text "and so on": "and" is one cell, then one cell per character.
    String newText = "and so on";
    List<Integer> newBrailleToText = Arrays.asList(0, 3, 4, 5, 6, 7, 8);
    List<Integer> newTextToBraille = Arrays.asList(0, 0, 0, 1, 2, 3, 4, 5, 6);
    assertEquals(
        2,
        PanPositionMatcher.matchDisplayPosition(
            oldText,
            identity(oldText.length()),
            identity(oldText.length()),
            newText,
            newBrailleToText,
            newTextToBraille,
            3,
            8));
  }

  @Test
  public void sameTextNewMapping_mapsThroughText() {
    // Same text, but the old translation had no contraction.
    String text = "and so on";
    List<Integer> newBrailleToText = Arrays.asList(0, 3, 4, 5, 6, 7, 8);
    List<Integer> newTextToBraille = Arrays.asList(0, 0, 0, 1, 2, 3, 4, 5, 6);
    assertEquals(
        2,
        PanPositionMatcher.matchDisplayPosition(
            text, identity(9), identity(9), text, newBrailleToText, newTextToBraille, 4, 9));
  }

  @Test
  public void windowPastEnd_mapsToEndOfNewText() {
    String oldText = "abc";
    String newText = "abcdef";
    assertEquals(3, matchUncontracted(oldText, newText, 3, 3));
  }
}
