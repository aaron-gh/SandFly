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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.Performance.EventId;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Clear selection in an edit field collapses the app's selection and ends selection mode. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TextEditActorClearSelectionTest {
  public static class TestService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
  }

  private final List<Feedback.Part> parts = new ArrayList<>();
  private final List<Integer> cursorMoves = new ArrayList<>();
  private TestService service;

  private TextEditActor actor() {
    service = Robolectric.buildService(TestService.class).get();
    TextEditActor actor =
        new TextEditActor(service, null, null, null, null, null) {
          @Override
          public boolean moveCursor(
              AccessibilityNodeInfoCompat node, int position, EventId eventId) {
            // Selection mode is already off when the cursor moves.
            assertTrue(parts.isEmpty() || parts.get(parts.size() - 1).focusDirection() != null);
            cursorMoves.add(position);
            return true;
          }
        };
    actor.setPipeline(
        feedback -> {
          parts.addAll(feedback.failovers());
          return true;
        });
    return actor;
  }

  private static AccessibilityNodeInfoCompat editField(String text, int start, int end) {
    AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
    node.setClassName("android.widget.EditText");
    node.setEditable(true);
    node.setText(text);
    node.setTextSelection(start, end);
    node.addAction(AccessibilityNodeInfo.ACTION_SET_SELECTION);
    return AccessibilityNodeInfoCompat.wrap(node);
  }

  private String spoken() {
    for (Feedback.Part part : parts) {
      if (part.speech() != null && part.speech().text() != null) {
        return part.speech().text().toString();
      }
    }
    return null;
  }

  private boolean endedSelectionMode() {
    for (Feedback.Part part : parts) {
      if (part.focusDirection() != null
          && part.focusDirection().action()
              == Feedback.FocusDirection.Action.SELECTION_MODE_OFF) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void selectionInSelectionModeCollapsesAtItsEnd() {
    TextEditActor actor = actor();
    assertTrue(actor.clearAppSelection(editField("Hello world", 2, 7), true, null));
    assertEquals(List.of(7), cursorMoves);
    assertTrue(endedSelectionMode());
    assertEquals(service.getString(R.string.notification_type_selection_cleared), spoken());
  }

  @Test
  public void backwardSelectionCollapsesAtItsEnd() {
    TextEditActor actor = actor();
    assertTrue(actor.clearAppSelection(editField("Hello world", 9, 3), false, null));
    assertEquals(List.of(9), cursorMoves);
    assertFalse(endedSelectionMode());
    assertEquals(service.getString(R.string.notification_type_selection_cleared), spoken());
  }

  @Test
  public void selectionModeWithoutSelectionEndsSelectionMode() {
    TextEditActor actor = actor();
    assertTrue(actor.clearAppSelection(editField("Hello world", 4, 4), true, null));
    assertTrue(cursorMoves.isEmpty());
    assertTrue(endedSelectionMode());
    assertEquals(service.getString(R.string.notification_type_selection_mode_off), spoken());
  }

  @Test
  public void noSelectionAndNoSelectionModeDoesNothing() {
    TextEditActor actor = actor();
    assertFalse(actor.clearAppSelection(editField("Hello world", 4, 4), false, null));
    assertTrue(cursorMoves.isEmpty());
    assertTrue(parts.isEmpty());
  }

  @Test
  public void noNodeDoesNothing() {
    TextEditActor actor = actor();
    assertFalse(actor.clearAppSelection(null, true, null));
    assertTrue(parts.isEmpty());
  }
}
