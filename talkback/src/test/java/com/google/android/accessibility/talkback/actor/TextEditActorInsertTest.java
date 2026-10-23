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
import static org.junit.Assert.assertTrue;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.Performance.EventId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
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
    TestService service = Robolectric.buildService(TestService.class).get();
    TextEditActor actor = new TextEditActor(service, null, null, null, null, null, null) {
      @Override
      public boolean moveCursor(AccessibilityNodeInfoCompat node, int position, EventId eventId) {
        assertEquals(cursor, position);
        return true;
      }
    };
    AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
    node.setClassName("android.widget.EditText");
    node.setEditable(true);
    node.setText(original);
    node.setShowingHintText(showingHint);
    node.setTextSelection(start, end);
    String[] written = {null};
    Shadows.shadowOf(node).setOnPerformActionListener((action, args) -> {
      assertEquals(AccessibilityNodeInfo.ACTION_SET_TEXT, action);
      written[0] = args.getCharSequence(
          AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE).toString();
      return true;
    });
    assertTrue(actor.insert(AccessibilityNodeInfoCompat.wrap(node), "Did you book that lunch?", null));
    assertEquals(expected, written[0]);
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

  @Test
  public void insertionReplacesOnlyTheSelection() {
    checkInsertion("Hi old bye", false, 3, 6, "Hi Did you book that lunch? bye", 27);
  }
}
