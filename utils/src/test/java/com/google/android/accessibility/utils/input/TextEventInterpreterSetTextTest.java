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
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import com.google.android.accessibility.utils.monitor.InputModeTracker;
import com.google.android.accessibility.utils.output.ActorStateProvider;
import com.google.android.accessibility.utils.output.EditTextActionHistory;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Cursor events from SandFly setting a field's text, as typing by voice does: a TextView places
 * the cursor at the start and then at the end of the new text, which would otherwise be read as a
 * cursor move over the whole text.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TextEventInterpreterSetTextTest {
  private final List<TextEventInterpretation> sent = new ArrayList<>();
  private final EditTextActionHistory actionHistory = new EditTextActionHistory();
  private TextEventInterpreter interpreter;
  private AccessibilityNodeInfo node;

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
            new ActorStateProvider() {
              @Override
              public EditTextActionHistory.Provider editHistory() {
                return actionHistory.provider;
              }
            },
            /* voiceActionDelegate= */ null,
            /* voiceDictationDelegate= */ null,
            new TextEventFilter(RuntimeEnvironment.getApplication(), tracker, history),
            /* cacheAndDropFrequentEvents= */ false,
            /* readLineWhenMoveToAdjacentLineByUpDownKey= */ false);
    interpreter.addListener(
        interpretation -> {
          if (interpretation.interpretation != null) {
            sent.add(interpretation.interpretation);
          }
        });
    node = new EditText(RuntimeEnvironment.getApplication()).createAccessibilityNodeInfo();
    node.setFocused(true);
    node.setEditable(true);
    node.setVisibleToUser(true);
  }

  private void sendCursor(String text, int cursor, long time) {
    AccessibilityEvent event =
        AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED);
    shadowOf(event).setSourceNode(node);
    event.setPackageName("com.example");
    event.setEventTime(time);
    event.getText().add(text);
    event.setItemCount(text.length());
    event.setFromIndex(cursor);
    event.setToIndex(cursor);
    interpreter.interpret(event, null);
  }

  private CharSequence lastTraversed() {
    for (int i = sent.size() - 1; i >= 0; i--) {
      if (sent.get(i).getEvent() == TextEventInterpretation.SELECTION_MOVE_CURSOR_NO_SELECTION) {
        return sent.get(i).getTraversedText();
      }
    }
    return null;
  }

  @Test
  public void cursorEventsOfSettingTextAreNotReadAsMoves() {
    long now = SystemClock.uptimeMillis();
    sendCursor("Hello", 5, now - 2000);
    int before = sent.size();
    // The action's events arrive after performAction returns, with times within the action.
    actionHistory.beforeSetText();
    actionHistory.afterSetText();
    sendCursor("Hello world", 0, now);
    sendCursor("Hello world", 11, now);
    for (int i = before; i < sent.size(); i++) {
      assertEquals(TextEventInterpretation.SET_TEXT_BY_ACTION, sent.get(i).getEvent());
    }
    // The next move is read from where setting the text left the cursor.
    sendCursor("Hello world", 10, now + 2000);
    assertEquals("d", String.valueOf(lastTraversed()));
  }

  @Test
  public void cursorEventsOfSettingTextOutsideTheActionReadTheWholeText() {
    long now = SystemClock.uptimeMillis();
    sendCursor("Hello", 5, now - 2000);
    sendCursor("Hello world", 0, now);
    sendCursor("Hello world", 11, now + 10);
    assertTrue(sent.size() > 1);
    assertEquals("Hello world", String.valueOf(lastTraversed()));
  }
}
