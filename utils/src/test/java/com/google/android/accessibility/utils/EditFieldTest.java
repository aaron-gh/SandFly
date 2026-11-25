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

/** Which items editing commands, such as Clear text, treat as text fields. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EditFieldTest {

  private static AccessibilityNodeInfoCompat node(String className, boolean editable, boolean web) {
    AccessibilityNodeInfo node = new AccessibilityNodeInfo();
    node.setClassName(className);
    node.setPackageName("com.example.app");
    node.setText("Some text");
    node.setEditable(editable);
    if (web) {
      node.addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT);
      node.addAction(AccessibilityNodeInfo.ACTION_PREVIOUS_HTML_ELEMENT);
    }
    return AccessibilityNodeInfoCompat.wrap(node);
  }

  @Test
  public void editText_isEditField() {
    assertTrue(
        AccessibilityNodeInfoUtils.isEditField(node("android.widget.EditText", true, false)));
    assertTrue(AccessibilityNodeInfoUtils.isEditField(node("android.widget.EditText", true, true)));
  }

  @Test
  public void editableCustomEditor_isEditField() {
    // Report #606: the Cx File Explorer text editor, a view of its own class marked editable.
    assertTrue(
        AccessibilityNodeInfoUtils.isEditField(node("com.example.app.CodeEditor", true, false)));
  }

  @Test
  public void viewThatIsNotEditable_isNoEditField() {
    assertFalse(AccessibilityNodeInfoUtils.isEditField(node("android.view.View", false, false)));
    assertFalse(
        AccessibilityNodeInfoUtils.isEditField(node("android.widget.TextView", false, false)));
  }

  @Test
  public void editableWebContent_isNoEditField() {
    // A paragraph inside a rich text editor on a web page.
    assertFalse(AccessibilityNodeInfoUtils.isEditField(node("android.view.View", true, true)));
  }

  @Test
  public void noNode_isNoEditField() {
    assertFalse(AccessibilityNodeInfoUtils.isEditField(null));
  }
}
