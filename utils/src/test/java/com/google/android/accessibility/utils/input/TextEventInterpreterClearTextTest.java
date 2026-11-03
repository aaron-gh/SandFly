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
import static org.junit.Assert.assertFalse;
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

/** Text removed by SandFly's clear text action, which announces it itself. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TextEventInterpreterClearTextTest {
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

  private void sendClear(long time) {
    AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
    shadowOf(event).setSourceNode(node);
    event.setPackageName("com.example");
    event.setEventTime(time);
    event.getText().add("");
    event.setBeforeText("hello");
    event.setFromIndex(0);
    event.setAddedCount(0);
    event.setRemovedCount(5);
    interpreter.interpret(event, null);
  }

  @Test
  public void textClearedDuringTheClearTextActionIsMarked() {
    actionHistory.beforeClearText();
    sendClear(SystemClock.uptimeMillis());
    actionHistory.afterClearText();
    assertEquals(1, sent.size());
    assertEquals(TextEventInterpretation.TEXT_CLEAR, sent.get(0).getEvent());
    assertTrue(sent.get(0).getIsClearTextAction());
  }

  @Test
  public void textClearedOtherwiseIsNotMarked() {
    actionHistory.beforeClearText();
    actionHistory.afterClearText();
    sendClear(SystemClock.uptimeMillis() + 1000);
    assertEquals(1, sent.size());
    assertEquals(TextEventInterpretation.TEXT_CLEAR, sent.get(0).getEvent());
    assertFalse(sent.get(0).getIsClearTextAction());
  }
}
