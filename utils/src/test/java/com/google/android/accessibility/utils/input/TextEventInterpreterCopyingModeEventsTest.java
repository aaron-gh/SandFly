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
import static org.robolectric.Shadows.shadowOf;

import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.monitor.InputModeTracker;
import com.google.android.accessibility.utils.output.ActorStateProvider;
import com.google.android.accessibility.utils.output.CopySelection;
import com.google.android.accessibility.utils.output.SelectionStateReader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Interpreting real traversal events in copying mode, through {@code interpret()}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TextEventInterpreterCopyingModeEventsTest {
  private static final String TEXT = "Hello, world. Next";

  private final CopySelection selection = new CopySelection();
  private boolean selectionModeActive;
  private TextEventInterpreter interpreter;
  private TextView view;

  @Before
  public void setUp() {
    ActorStateProvider actorState =
        new ActorStateProvider() {
          @Override
          public @NonNull SelectionStateReader selectionState() {
            return new SelectionStateReader() {
              @Override
              public boolean isSelectionModeActive() {
                return selectionModeActive;
              }

              @Override
              public CopySelection copySelection() {
                return selection;
              }
            };
          }
        };
    TextEventHistory history = new TextEventHistory();
    TextCursorTracker tracker = new TextCursorTracker();
    interpreter =
        new TextEventInterpreter(
            RuntimeEnvironment.getApplication(),
            tracker,
            new InputModeTracker(),
            history,
            actorState,
            /* voiceActionDelegate= */ null,
            /* voiceDictationDelegate= */ null,
            new TextEventFilter(RuntimeEnvironment.getApplication(), tracker, history),
            /* cacheAndDropFrequentEvents= */ false,
            /* readLineWhenMoveToAdjacentLineByUpDownKey= */ false);
    view = new TextView(RuntimeEnvironment.getApplication());
    view.setText(TEXT);
    selectionModeActive = true;
    selection.start();
  }

  private AccessibilityEvent traversal(int from, int to, boolean forward, boolean password) {
    AccessibilityEvent event =
        AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY);
    shadowOf(event).setSourceNode(view.createAccessibilityNodeInfo());
    event.setFromIndex(from);
    event.setToIndex(to);
    event.setItemCount(TEXT.length());
    event.setMovementGranularity(AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD);
    event.setAction(
        forward
            ? AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
            : AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY);
    event.setPassword(password);
    return event;
  }

  private TextEventInterpretation interpret(AccessibilityEvent event) {
    return interpreter.interpret(event, false, false);
  }

  @Test
  public void aWordMoveIsDescribedByWhatItSelected() {
    TextEventInterpretation result = interpret(traversal(0, 5, true, false));
    assertEquals(TextEventInterpretation.SELECTION_MOVE_CURSOR_WITH_SELECTION, result.getEvent());
    assertEquals("Hello", result.getSelectedText().toString());
    assertEquals("", result.getDeselectedText().toString());
  }

  @Test
  public void movesOverPunctuationBuildOneRangeAndMovingBackDeselects() {
    interpret(traversal(0, 5, true, false));
    interpret(traversal(7, 12, true, false));
    assertEquals("Hello, world", selection.selectedText().toString());
    TextEventInterpretation back = interpret(traversal(7, 12, false, false));
    assertEquals("world", back.getDeselectedText().toString());
    assertEquals("Hello,", selection.selectedText().toString());
  }

  @Test
  public void anEventWhenSelectionModeIsOffIsAnOrdinaryTraversal() {
    selectionModeActive = false;
    TextEventInterpretation result = interpret(traversal(0, 5, true, false));
    assertEquals(TextEventInterpretation.SELECTION_TEXT_TRAVERSAL, result.getEvent());
    assertEquals("Hello", result.getTraversedText().toString());
    assertEquals(false, selection.hasContent());
  }

  @Test
  public void anEventWhenCopyingModeIsNotOpenIsAnOrdinaryTraversal() {
    selection.abandon();
    TextEventInterpretation result = interpret(traversal(0, 5, true, false));
    assertEquals(TextEventInterpretation.SELECTION_TEXT_TRAVERSAL, result.getEvent());
    assertEquals(false, selection.hasContent());
  }

  @Test
  public void aPasswordEventIsNotSelected() {
    TextEventInterpretation result = interpret(traversal(0, 5, true, true));
    assertEquals(false, selection.hasContent());
    assertEquals(false, result.getEvent() == TextEventInterpretation.SELECTION_MOVE_CURSOR_WITH_SELECTION);
  }

  @Test
  public void anEventOverNoTextIsNotSelected() {
    view.setText("");
    AccessibilityEvent event = traversal(0, 0, true, false);
    event.setItemCount(0);
    interpret(event);
    assertEquals(false, selection.hasContent());
  }
}
