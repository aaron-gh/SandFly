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

package com.google.android.accessibility.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.accessibility.AccessibilityNodeInfo;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Which text the app keeps a readable selection in, and which Backtalk selects for copying. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class KeepsAppSelectionTest {

  private static AccessibilityNodeInfoCompat text(boolean selectable, boolean web) {
    AccessibilityNodeInfo node = new AccessibilityNodeInfo();
    node.setClassName("android.widget.TextView");
    node.setText("People. It can be read");
    node.setTextSelectable(selectable);
    if (web) {
      node.addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT);
      node.addAction(AccessibilityNodeInfo.ACTION_PREVIOUS_HTML_ELEMENT);
    }
    return AccessibilityNodeInfoCompat.wrap(node);
  }

  @Test
  public void selectableTextInAnApp_keepsAppSelection() {
    assertTrue(AccessibilityNodeInfoUtils.keepsAppSelection(text(true, false)));
  }

  @Test
  public void selectableWebText_isSelectedByBacktalk() {
    // Current Chrome marks text on web pages selectable.
    AccessibilityNodeInfoCompat node = text(true, true);
    assertTrue(AccessibilityNodeInfoUtils.isTextSelectable(node));
    assertFalse(AccessibilityNodeInfoUtils.keepsAppSelection(node));
  }

  @Test
  public void textThatIsNotSelectable_isSelectedByBacktalk() {
    assertFalse(AccessibilityNodeInfoUtils.keepsAppSelection(text(false, false)));
    assertFalse(AccessibilityNodeInfoUtils.keepsAppSelection(text(false, true)));
  }

  @Test
  public void editableWebText_keepsAppSelection() {
    AccessibilityNodeInfoCompat node = text(false, true);
    node.setEditable(true);
    assertTrue(AccessibilityNodeInfoUtils.keepsAppSelection(node));
  }

  @Test
  public void noNode_keepsNoSelection() {
    assertFalse(AccessibilityNodeInfoUtils.keepsAppSelection(null));
  }
}
