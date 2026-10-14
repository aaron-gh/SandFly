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

package com.google.android.accessibility.utils.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.TextView;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Text selected in copying mode. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CopySelectionTest {
  private static final String ROW1 = "Hello big world";
  private static final String ROW2 = "Second line";
  private static final String ROW3 = "Third row here";

  private final CopySelection selection = new CopySelection();
  private AccessibilityNodeInfoCompat row1;
  private AccessibilityNodeInfoCompat row2;
  private AccessibilityNodeInfoCompat row3;

  @Before
  public void setUp() {
    row1 = node();
    row2 = node();
    row3 = node();
    selection.start();
  }

  private static AccessibilityNodeInfoCompat node() {
    View view = new TextView(RuntimeEnvironment.getApplication());
    return AccessibilityNodeInfoCompat.obtain(view);
  }

  @Test
  public void firstMoveSelectsWhatItMovedOver() {
    CopySelection.Change change = selection.move(row1, ROW1, 6, 9, true);
    assertEquals("big", change.selected.toString());
    assertEquals("", change.deselected.toString());
    assertEquals("big", selection.selectedText().toString());
  }

  @Test
  public void movingBackDeselects() {
    selection.move(row1, ROW1, 0, 5, true);
    selection.move(row1, ROW1, 6, 9, true);
    assertEquals("Hello big", selection.selectedText().toString());
    CopySelection.Change change = selection.move(row1, ROW1, 6, 9, false);
    assertEquals("", change.selected.toString());
    assertEquals("big", change.deselected.toString());
    assertEquals("Hello", selection.selectedText().toString());
  }

  @Test
  public void movingPastTheStartSelectsTheOtherWay() {
    selection.move(row1, ROW1, 6, 9, true);
    // A move over the whole line, back past the start of the range.
    CopySelection.Change change = selection.move(row1, ROW1, 0, 15, false);
    assertEquals("Hello ", change.selected.toString());
    assertEquals("big", change.deselected.toString());
    assertEquals("Hello", selection.selectedText().toString());
  }

  @Test
  public void aMoveToAnotherItemSelectsOnlyWhatItMovedOver() {
    selection.move(row1, ROW1, 6, 9, true);
    selection.move(row2, ROW2, 0, 1, true);
    selection.move(row2, ROW2, 1, 2, true);
    assertEquals("big\nSe", selection.selectedText().toString());
  }

  @Test
  public void itemsBetweenAreNotSelected() {
    selection.move(row1, ROW1, 10, 15, true);
    selection.move(row3, ROW3, 0, 5, true);
    assertEquals("world\nThird", selection.selectedText().toString());
  }

  @Test
  public void aBackwardMoveToAnotherItemGoesBeforeTheLastRange() {
    selection.move(row3, ROW3, 0, 5, false);
    selection.move(row2, ROW2, 0, 6, false);
    selection.move(row1, ROW1, 10, 15, false);
    assertEquals("world\nSecond\nThird", selection.selectedText().toString());
  }

  @Test
  public void moveWithOnlyWhitespaceBetweenCarriesOn() {
    selection.move(row1, ROW1, 0, 5, true);
    selection.move(row1, ROW1, 6, 9, true);
    selection.move(row1, ROW1, 10, 15, true);
    assertEquals("Hello big world", selection.selectedText().toString());
  }

  @Test
  public void textMovedOverBetweenTwoMovesIsSelected() {
    selection.move(row1, ROW1, 0, 5, true);
    selection.move(row1, ROW1, 10, 15, true);
    assertEquals("Hello big world", selection.selectedText().toString());
  }

  @Test
  public void movingByWordOverPunctuationIsOneRange() {
    String text = "Hello, world. Next";
    selection.move(row1, text, 0, 5, true);
    selection.move(row1, text, 7, 12, true);
    selection.move(row1, text, 14, 18, true);
    assertEquals("Hello, world. Next", selection.selectedText().toString());
    String number = "call 555-0100 now";
    selection.end();
    selection.start();
    selection.move(row2, number, 5, 8, true);
    selection.move(row2, number, 9, 13, true);
    assertEquals("Hello, world. Next\n555-0100", selection.allText().toString());
  }

  @Test
  public void aMoveBehindTheEndOfTheRangeIsARangeOfItsOwn() {
    selection.move(row1, ROW1, 6, 9, true);
    // The text cursor was reset to the start, so this forward move is behind the range.
    selection.move(row1, ROW1, 0, 5, true);
    assertEquals("big\nHello", selection.selectedText().toString());
  }

  @Test
  public void returningToAnItemElsewhereInItIsARangeOfItsOwn() {
    selection.move(row1, ROW1, 0, 5, true);
    selection.move(row2, ROW2, 0, 6, true);
    selection.move(row1, ROW1, 10, 15, false);
    assertEquals("Hello\nworld\nSecond", selection.selectedText().toString());
  }

  @Test
  public void rangesThatAreNotNextToEachOtherAreEachAPart() {
    selection.move(row1, ROW1, 6, 9, true);
    assertEquals("big", selection.end().toString());
    assertFalse(selection.isActive());
    selection.start();
    selection.move(row3, ROW3, 6, 9, true);
    assertEquals("big\nrow", selection.allText().toString());
    assertEquals(1, selection.partCount());
    assertEquals("row", selection.end().toString());
    assertEquals(2, selection.partCount());
    assertEquals("big\nrow", selection.allText().toString());
  }

  @Test
  public void aRangeWithNoTextIsNotAPart() {
    selection.move(row1, ROW1, 3, 3, true);
    assertEquals("", selection.end().toString());
    assertEquals(0, selection.partCount());
    assertFalse(selection.hasContent());
  }

  @Test
  public void selectAllTakesEveryItemGiven() {
    CharSequence all =
        selection.selectAll(Arrays.asList(row1, row2), Arrays.<CharSequence>asList(ROW1, ROW2));
    assertEquals("Hello big world\nSecond line", all.toString());
    assertTrue(selection.hasContent());
  }

  @Test
  public void clearingDropsPartsAndRanges() {
    selection.move(row1, ROW1, 0, 5, true);
    selection.end();
    selection.start();
    selection.move(row2, ROW2, 0, 6, true);
    selection.clear();
    assertFalse(selection.hasContent());
    assertFalse(selection.isActive());
    assertEquals("", selection.allText().toString());
  }

  @Test
  public void movesAreIgnoredWhenCopyingModeIsOff() {
    selection.end();
    assertNull(selection.move(row1, ROW1, 0, 5, true));
    assertNull(selection.move(null, ROW1, 0, 5, true));
  }

  @Test
  public void whitespaceAtTheEndsOfARangeIsNotCopied() {
    String text = "You can contact me at 555-0100 for assistance.";
    // A move over the space, then over the number.
    selection.move(row1, text, 21, 22, true);
    selection.move(row1, text, 22, 30, true);
    assertEquals("555-0100", selection.end().toString());
    selection.start();
    String email = "Alternatively, you can reach me at me@example.com for the same.";
    selection.move(row2, email, 34, 35, true);
    selection.move(row2, email, 35, 49, true);
    selection.move(row2, email, 49, 50, true);
    assertEquals("555-0100\nme@example.com", selection.allText().toString());
  }

  @Test
  public void abandoningDropsTheOpenPartWithoutKeepingIt() {
    selection.move(row1, ROW1, 0, 5, true);
    selection.abandon();
    assertFalse(selection.isActive());
    assertEquals(0, selection.partCount());
    assertFalse(selection.hasContent());
  }

  @Test
  public void anEndedPartIsKeptButAnAbandonedOneIsNot() {
    selection.move(row1, ROW1, 0, 5, true);
    selection.end();
    selection.start();
    selection.move(row2, ROW2, 0, 6, true);
    selection.abandon();
    assertEquals("Hello", selection.allText().toString());
  }

  @Test
  public void indexesOutsideTheTextAreClamped() {
    CopySelection.Change change = selection.move(row1, ROW1, -5, 99, true);
    assertEquals(ROW1, change.selected.toString());
  }

  @Test
  public void changedTextOfTheSameItemIsARangeOfItsOwn() {
    selection.move(row1, "Hello", 0, 5, true);
    selection.move(row1, "Hello world", 6, 11, true);
    assertEquals("Hello\nworld", selection.selectedText().toString());
  }

  @Test
  public void selectAllWithNoTextSelectsNothing() {
    CharSequence all =
        selection.selectAll(Arrays.asList(row1, row2), Arrays.<CharSequence>asList("", null));
    assertEquals("", all.toString());
    assertFalse(selection.hasContent());
  }

  @Test
  public void aMoveAfterSelectAllIsARangeOfItsOwn() {
    selection.selectAll(Arrays.asList(row1), Arrays.<CharSequence>asList(ROW1));
    selection.move(row2, ROW2, 0, 6, true);
    assertEquals("Hello big world\nSecond", selection.selectedText().toString());
  }

  @Test
  public void passwordTextIsNotSelected() {
    TextView view = new TextView(RuntimeEnvironment.getApplication());
    view.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
    AccessibilityNodeInfoCompat password =
        AccessibilityNodeInfoCompat.wrap(view.createAccessibilityNodeInfo());
    assertTrue(password.isPassword());
    assertNull(selection.move(password, "secret", 0, 6, true));
    assertEquals(
        "", selection.selectAll(Arrays.asList(password), Arrays.<CharSequence>asList("secret"))
            .toString());
    assertFalse(selection.hasContent());
  }

  @Test
  public void noMorePartsAreKeptThanTheLimit() {
    for (int i = 0; i < 500; i++) {
      selection.start();
      selection.move(row1, ROW1, 0, 5, true);
      assertEquals("Hello", selection.end().toString());
    }
    selection.start();
    selection.move(row1, ROW1, 0, 5, true);
    // Not kept, so not reported as kept.
    assertEquals("", selection.end().toString());
    assertEquals(500, selection.partCount());
  }

  @Test
  public void aRangeIsNotTakenOnceTheTextLimitIsReached() {
    String big = "x".repeat(600_000);
    assertNotNull(selection.move(row1, big, 0, 600_000, true));
    // The open part is counted, so a second range this size is over the limit.
    assertNull(selection.move(row2, big, 0, 600_000, true));
  }

  @Test
  public void startingWhileOpenKeepsTheOpenPart() {
    selection.move(row1, ROW1, 0, 5, true);
    selection.start();
    assertEquals("Hello", selection.selectedText().toString());
  }

  @Test
  public void selectAllKeepsRangesInOtherItemsAndReplacesRangesInTheItem() {
    selection.move(row1, ROW1, 0, 5, true);
    selection.move(row2, ROW2, 0, 6, true);
    CharSequence all =
        selection.selectAll(Arrays.asList(row2), Arrays.<CharSequence>asList(ROW2));
    assertEquals("Second line", all.toString());
    assertEquals("Hello\nSecond line", selection.selectedText().toString());
  }

  @Test
  public void selectAllIsLimitedByWhatIsAlreadyKept() {
    String big = "x".repeat(600_000);
    selection.move(row1, big, 0, 600_000, true);
    selection.end();
    selection.start();
    assertEquals(
        "",
        selection
            .selectAll(Arrays.asList(row2), Arrays.<CharSequence>asList(big))
            .toString());
  }
}
