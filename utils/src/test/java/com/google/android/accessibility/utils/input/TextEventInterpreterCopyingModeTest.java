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

package com.google.android.accessibility.utils.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.TextView;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.output.CopySelection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Moving the text cursor in copying mode. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TextEventInterpreterCopyingModeTest {
  private static final String TEXT = "Hello big world";

  private final CopySelection selection = new CopySelection();
  private final TextEventInterpretation interpretation =
      new TextEventInterpretation(TextEventInterpretation.SELECTION_TEXT_TRAVERSAL);
  private AccessibilityNodeInfoCompat node;

  @Before
  public void setUp() {
    View view = new TextView(RuntimeEnvironment.getApplication());
    node = AccessibilityNodeInfoCompat.obtain(view);
    selection.start();
  }

  private boolean move(int from, int to, boolean forward, boolean password) {
    return TextEventInterpreter.interpretCopyingModeMove(
        interpretation, selection, node, TEXT, "ignored", from, to, forward, password);
  }

  @Test
  public void aForwardMoveIsDescribedByWhatItSelected() {
    assertTrue(move(0, 5, true, false));
    assertEquals(
        TextEventInterpretation.SELECTION_MOVE_CURSOR_WITH_SELECTION, interpretation.getEvent());
    assertEquals("Hello", interpretation.getSelectedText().toString());
    assertEquals("", interpretation.getDeselectedText().toString());
  }

  @Test
  public void aBackwardMoveAfterAForwardOneIsDescribedByWhatItDeselected() {
    move(0, 5, true, false);
    move(6, 9, true, false);
    assertTrue(move(6, 9, false, false));
    assertEquals("", interpretation.getSelectedText().toString());
    assertEquals("big", interpretation.getDeselectedText().toString());
    assertEquals("Hello", selection.selectedText().toString());
  }

  @Test
  public void directionDecidesWhichEndOfAMoveStartsTheRange() {
    // A backward move over "big" selects it, with the cursor at its start.
    move(6, 9, false, false);
    assertEquals("big", selection.selectedText().toString());
    // Carrying on backward over "Hello " continues from that start.
    move(0, 5, false, false);
    assertEquals("Hello big", selection.selectedText().toString());
  }

  @Test
  public void theViewTextIsUsedBeforeTheEventText() {
    assertTrue(move(0, 5, true, false));
    assertEquals("Hello", selection.selectedText().toString());
  }

  @Test
  public void theEventTextIsUsedWhenTheViewHasNone() {
    assertTrue(
        TextEventInterpreter.interpretCopyingModeMove(
            interpretation, selection, node, "", TEXT, 6, 9, true, false));
    assertEquals("big", selection.selectedText().toString());
  }

  @Test
  public void aPasswordIsNotSelected() {
    assertFalse(move(0, 5, true, true));
    assertFalse(selection.hasContent());
    assertEquals(
        TextEventInterpretation.SELECTION_TEXT_TRAVERSAL, interpretation.getEvent());
  }

  @Test
  public void aMoveIsNotDescribedWhenCopyingModeIsOff() {
    selection.abandon();
    assertFalse(move(0, 5, true, false));
    assertEquals(
        TextEventInterpretation.SELECTION_TEXT_TRAVERSAL, interpretation.getEvent());
  }

  @Test
  public void aMoveIsNotDescribedWithNoCopySelection() {
    assertFalse(
        TextEventInterpreter.interpretCopyingModeMove(
            interpretation, null, node, TEXT, TEXT, 0, 5, true, false));
  }
}
