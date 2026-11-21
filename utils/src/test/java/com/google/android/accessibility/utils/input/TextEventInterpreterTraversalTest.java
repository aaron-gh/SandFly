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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
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

/**
 * Moving by word or character through an edit text that has a content description, as the message
 * fields of Signal and Messenger have (report #506).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TextEventInterpreterTraversalTest {
  private static final String TEXT = "Hello there my friend, how are you today";
  private static final String DESCRIPTION = "Message composition";

  private final List<Integer> sent = new ArrayList<>();
  private final List<CharSequence> traversed = new ArrayList<>();
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
        interpretation -> {
          TextEventInterpretation result = interpretation.interpretation;
          sent.add(result == null ? interpretation.event.getEventType() : result.getEvent());
          traversed.add(result == null ? null : result.getTraversedText());
        });
    node = new EditText(RuntimeEnvironment.getApplication()).createAccessibilityNodeInfo();
    node.setFocused(true);
    node.setEditable(true);
    node.setVisibleToUser(true);
    node.setText(TEXT);
    node.setContentDescription(DESCRIPTION);
    // The cursor is placed at the end of "friend".
    send(selection(21, 21));
    clear();
  }

  private void clear() {
    sent.clear();
    traversed.clear();
  }

  private void send(AccessibilityEvent event) {
    interpreter.interpret(event, null);
  }

  private AccessibilityEvent event(int type) {
    AccessibilityEvent event = AccessibilityEvent.obtain(type);
    shadowOf(event).setSourceNode(node);
    event.setPackageName("com.example");
    event.setEventTime(time);
    event.getText().add(TEXT);
    event.setItemCount(TEXT.length());
    return event;
  }

  private AccessibilityEvent selection(int from, int to) {
    AccessibilityEvent event = event(AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED);
    event.setFromIndex(from);
    event.setToIndex(to);
    return event;
  }

  private AccessibilityEvent wordTraversal(int from, int to) {
    AccessibilityEvent event =
        event(AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY);
    event.setFromIndex(from);
    event.setToIndex(to);
    event.setMovementGranularity(AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
    event.setAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY);
    return event;
  }

  /** A move to the next word: the framework sends the selection event, then the traversal. */
  private void nextWord(int start, int end) {
    time += 1000;
    send(selection(end, end));
    send(wordTraversal(start, end));
  }

  @Test
  public void wordMovesPastTheDescriptionLengthAreReadAsCursorMoves() {
    nextWord(23, 26);
    nextWord(27, 30);
    nextWord(31, 34);
    assertEquals(
        List.of(
            TextEventInterpretation.SELECTION_MOVE_CURSOR_NO_SELECTION,
            TextEventInterpretation.SELECTION_MOVE_CURSOR_NO_SELECTION,
            TextEventInterpretation.SELECTION_MOVE_CURSOR_NO_SELECTION),
        sent);
    assertEquals(", how", traversed.get(0).toString());
    assertEquals(" are", traversed.get(1).toString());
    assertEquals(" you", traversed.get(2).toString());
  }

  @Test
  public void aTraversalOnItsOwnReadsTheFieldText() {
    time += 1000;
    send(wordTraversal(23, 26));
    assertEquals(List.of(TextEventInterpretation.SELECTION_TEXT_TRAVERSAL), sent);
    assertEquals("how", traversed.get(0).toString());
  }

  @Test
  public void aSelectionThatGoesAwayIsReadAsCleared() {
    time += 1000;
    send(selection(15, 21));
    clear();
    time += 1000;
    send(selection(21, 21));
    assertEquals(List.of(TextEventInterpretation.SELECTION_MOVE_CURSOR_SELECTION_CLEARED), sent);
  }

  @Test
  public void traversedTextOfAFieldIsItsTextNotItsDescription() {
    assertEquals(
        TEXT,
        TextEventInterpreter.getTraversedNodeText(AccessibilityNodeInfoCompat.wrap(node))
            .toString());
  }

  @Test
  public void traversedTextOfAnythingElseIsItsDescription() {
    node.setEditable(false);
    assertEquals(
        DESCRIPTION,
        TextEventInterpreter.getTraversedNodeText(AccessibilityNodeInfoCompat.wrap(node))
            .toString());
  }
}
