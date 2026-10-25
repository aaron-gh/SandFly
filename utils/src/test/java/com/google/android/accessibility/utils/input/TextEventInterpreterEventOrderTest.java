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

package com.google.android.accessibility.utils.input;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import com.google.android.accessibility.utils.monitor.InputModeTracker;
import com.google.android.accessibility.utils.output.ActorStateProvider;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Text fields that send the selection event of an edit before or after its text change event. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TextEventInterpreterEventOrderTest {
  private final List<Integer> sent = new ArrayList<>();
  private TextEventInterpreter interpreter;
  private AccessibilityNodeInfo node;
  private long time = 1000;

  @Before
  public void setUp() {
    TextEventHistory history = new TextEventHistory();
    TextCursorTracker tracker = new TextCursorTracker();
    interpreter =
        new TextEventInterpreter(
            RuntimeEnvironment.getApplication(),
            tracker,
            new InputModeTracker(),
            history,
            new ActorStateProvider() {},
            /* voiceActionDelegate= */ null,
            /* voiceDictationDelegate= */ null,
            new TextEventFilter(RuntimeEnvironment.getApplication(), tracker, history),
            /* cacheAndDropFrequentEvents= */ false,
            /* readLineWhenMoveToAdjacentLineByUpDownKey= */ false);
    interpreter.addListener(
        interpretation ->
            sent.add(
                interpretation.interpretation == null
                    ? interpretation.event.getEventType()
                    : interpretation.interpretation.getEvent()));
    node = new EditText(RuntimeEnvironment.getApplication()).createAccessibilityNodeInfo();
    node.setFocused(true);
    node.setEditable(true);
    node.setVisibleToUser(true);
    // The cursor is placed in the field.
    send(selection("cm", 2));
    sent.clear();
  }

  private void send(AccessibilityEvent event) {
    interpreter.interpret(event, null);
  }

  private AccessibilityEvent event(int type, String text) {
    AccessibilityEvent event = AccessibilityEvent.obtain(type);
    shadowOf(event).setSourceNode(node);
    event.setPackageName("com.example");
    event.setEventTime(time);
    event.getText().add(text);
    event.setItemCount(text.length());
    return event;
  }

  private AccessibilityEvent selection(String text, int cursor) {
    AccessibilityEvent event = event(AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED, text);
    event.setFromIndex(cursor);
    event.setToIndex(cursor);
    return event;
  }

  private AccessibilityEvent textChange(String before, String text, int from, int added, int removed) {
    AccessibilityEvent event = event(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED, text);
    event.setBeforeText(before);
    event.setFromIndex(from);
    event.setAddedCount(added);
    event.setRemovedCount(removed);
    return event;
  }

  @Test
  public void selectionAfterTheTextChangeIsDropped() {
    send(textChange("cm", "cm ", 2, 1, 0));
    send(selection("cm ", 3));
    assertEquals(List.of(TextEventInterpretation.TEXT_ADD), sent);
  }

  @Test
  public void selectionBeforeTheTextChangeIsDropped() {
    send(selection("cm ", 3));
    send(textChange("cm", "cm ", 2, 1, 0));
    assertEquals(List.of(TextEventInterpretation.TEXT_ADD), sent);
  }

  @Test
  public void deletionWithTheSelectionFirstIsReadAsADeletion() {
    send(selection("", 0));
    send(textChange("cm", "", 0, 0, 2));
    assertEquals(1, sent.size());
    assertEquals(false, sent.get(0) == TextEventInterpretation.SELECTION_MOVE_CURSOR_NO_SELECTION);
  }

  @Test
  public void aCursorMoveSoonAfterIsStillRead() {
    send(selection("cm ", 3));
    send(textChange("cm", "cm ", 2, 1, 0));
    time += 50;
    send(selection("cm ", 2));
    assertEquals(
        List.of(
            TextEventInterpretation.TEXT_ADD,
            TextEventInterpretation.SELECTION_MOVE_CURSOR_NO_SELECTION),
        sent);
  }

  /** A selection event in an empty field, which reports its hint as its text. */
  private AccessibilityEvent hintSelection(String hint) {
    AccessibilityEvent event = selection(hint, 0);
    event.setItemCount(0);
    return event;
  }

  @Test
  public void firstCharacterInAFieldWithAHintIsReadOnce() {
    send(textChange("cm", "Name", 0, 0, 2));
    send(hintSelection("Name"));
    sent.clear();
    time += 1000;
    send(selection("n", 1));
    send(textChange("", "n", 0, 1, 0));
    assertEquals(List.of(TextEventInterpretation.TEXT_ADD), sent);
  }

  @Test
  public void firstWordInAFieldWithAHintIsReadOnce() {
    send(textChange("cm", "Name", 0, 0, 2));
    send(hintSelection("Name"));
    sent.clear();
    time += 1000;
    send(selection("hello", 5));
    send(textChange("", "hello", 0, 5, 0));
    assertEquals(List.of(TextEventInterpretation.TEXT_ADD), sent);
  }

  @Test
  public void aCursorMoveIsRead() {
    send(selection("cm", 1));
    assertEquals(List.of(TextEventInterpretation.SELECTION_MOVE_CURSOR_NO_SELECTION), sent);
  }
}
