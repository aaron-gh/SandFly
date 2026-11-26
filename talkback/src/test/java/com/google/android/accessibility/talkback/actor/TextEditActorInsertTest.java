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

package com.google.android.accessibility.talkback.actor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import android.os.SystemClock;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.output.EditTextActionHistory;
import java.util.ArrayList;
import java.util.List;
import com.google.android.accessibility.talkback.dictation.AiDictation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TextEditActorInsertTest {
  public static class TestService extends AccessibilityService {
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}
  }

  private void checkInsertion(
      String original, boolean showingHint, int start, int end, String expected, int cursor) {
    checkInsertion(original, showingHint, true, true, start, end, expected, cursor);
  }

  private void checkInsertion(
      String original, boolean showingHint, boolean selectable, boolean cursorSucceeds,
      int start, int end, String expected, int cursor) {
    TestService service = Robolectric.buildService(TestService.class).get();
    TextEditActor actor = new TextEditActor(service, new EditTextActionHistory(), null, null, null, null) {
      @Override
      public boolean moveCursor(AccessibilityNodeInfoCompat node, int position, EventId eventId) {
        assertEquals(cursor, position);
        return cursorSucceeds;
      }
    };
    AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
    node.setClassName("android.widget.EditText");
    node.setEditable(true);
    node.setText(original);
    node.setShowingHintText(showingHint);
    node.setTextSelection(start, end);
    if (selectable) node.addAction(AccessibilityNodeInfo.ACTION_SET_SELECTION);
    if (!selectable) {
      assertEquals("", AiDictation.Companion.textBeforeCursor(AccessibilityNodeInfoCompat.wrap(node)));
      assertEquals("", AiDictation.Companion.textAfterCursor(AccessibilityNodeInfoCompat.wrap(node)));
    }
    String[] written = {null};
    Shadows.shadowOf(node).setOnPerformActionListener((action, args) -> {
      assertEquals(AccessibilityNodeInfo.ACTION_SET_TEXT, action);
      written[0] = args.getCharSequence(
          AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE).toString();
      return true;
    });
    assertTrue(
        actor.insert(
            AccessibilityNodeInfoCompat.wrap(node), "Did you book that lunch?", false, null));
    assertEquals(expected, written[0]);
  }

  @Test
  public void emptyTelegramStyleFieldDoesNotOfferSelectionOrHintMetadata() {
    EditText field = new EditText(RuntimeEnvironment.getApplication()) {
      @Override
      public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (getText().length() == 0) info.setText("Message");
      }
    };
    AccessibilityNodeInfo node = field.createAccessibilityNodeInfo();
    assertEquals("Message", node.getText().toString());
    assertEquals(null, node.getHintText());
    assertFalse(node.isShowingHintText());
    assertEquals(0, node.getActions() & AccessibilityNodeInfo.ACTION_SET_SELECTION);
    assertEquals("", AiDictation.Companion.textBeforeCursor(AccessibilityNodeInfoCompat.wrap(node)));
    assertEquals("", AiDictation.Companion.textAfterCursor(AccessibilityNodeInfoCompat.wrap(node)));
  }

  @Test
  public void telegramPlaceholderWithoutHintFlagIsNotInserted() {
    checkInsertion("Message", false, false, true, 0, 0, "Did you book that lunch?", 24);
  }

  @Test
  public void successfulInsertionIsNotReportedAsFailureWhenCursorMoveFails() {
    checkInsertion("Message", false, false, false, 0, 0, "Did you book that lunch?", 24);
  }

  @Test
  public void placeholderWithoutHintTextIsNotInserted() {
    checkInsertion("Message", true, 0, 0, "Did you book that lunch?", 24);
  }

  @Test
  public void placeholderSelectionIsClampedToEmptyValue() {
    checkInsertion("Message", true, 7, 7, "Did you book that lunch?", 24);
  }

  @Test
  public void actualTextMatchingTheLabelIsPreserved() {
    checkInsertion("Message", false, 0, 0, "Did you book that lunch?Message", 24);
  }

  /**
   * Runs an insertion into "Hello" with the cursor at the end, and returns the speech it gives.
   * The text set and the cursor move both count as setting the text, so the framework's cursor
   * events from them aren't read as cursor moves over the whole text.
   */
  private List<CharSequence> insertAndCheckWindows(
      EditTextActionHistory history, boolean speak, boolean password) {
    TestService service = Robolectric.buildService(TestService.class).get();
    long[] duringMove = {-1};
    TextEditActor actor = new TextEditActor(service, history, null, null, null, null) {
      @Override
      public boolean moveCursor(AccessibilityNodeInfoCompat node, int position, EventId eventId) {
        duringMove[0] = SystemClock.uptimeMillis();
        return true;
      }
    };
    List<CharSequence> spoken = new ArrayList<>();
    actor.setPipeline(
        feedback -> {
          for (Feedback.Part part : feedback.failovers()) {
            if (part.speech() != null && part.speech().text() != null) {
              spoken.add(part.speech().text());
            }
          }
          return true;
        });
    AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
    node.setClassName("android.widget.EditText");
    node.setEditable(true);
    node.setPassword(password);
    node.setText("Hello");
    node.setTextSelection(5, 5);
    node.addAction(AccessibilityNodeInfo.ACTION_SET_SELECTION);
    Shadows.shadowOf(node).setOnPerformActionListener((action, args) -> true);
    assertTrue(actor.insert(AccessibilityNodeInfoCompat.wrap(node), " world", speak, null));
    assertTrue(duringMove[0] >= 0);
    assertTrue(history.hasSetTextActionAtTime(duringMove[0]));
    return spoken;
  }

  @Test
  public void spokenInsertionSaysOnlyTheInsertedText() {
    EditTextActionHistory history = new EditTextActionHistory();
    List<CharSequence> spoken = insertAndCheckWindows(history, true, false);
    assertEquals(1, spoken.size());
    assertEquals("world", spoken.get(0).toString().trim());
    assertTrue(history.hasSpokenEditAtTime(SystemClock.uptimeMillis()));
  }

  @Test
  public void unspokenInsertionSaysNothingAndKeepsTheAppTextChange() {
    EditTextActionHistory history = new EditTextActionHistory();
    assertTrue(insertAndCheckWindows(history, false, false).isEmpty());
    assertFalse(history.hasSpokenEditAtTime(SystemClock.uptimeMillis()));
  }

  @Test
  public void insertionIntoPasswordFieldIsNotSpoken() {
    EditTextActionHistory history = new EditTextActionHistory();
    assertTrue(insertAndCheckWindows(history, true, true).isEmpty());
    assertFalse(history.hasSpokenEditAtTime(SystemClock.uptimeMillis()));
  }

  @Test
  public void insertionReplacesOnlyTheSelection() {
    checkInsertion("Hi old bye", false, 3, 6, "Hi Did you book that lunch? bye", 27);
  }
}
