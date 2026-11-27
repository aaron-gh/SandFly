package com.google.android.accessibility.utils.input

import android.view.accessibility.AccessibilityNodeInfo
import com.google.android.accessibility.utils.FormFactorUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CoveredFieldEchoTest {
  @Before
  fun initFormFactor() {
    FormFactorUtils.initialize(RuntimeEnvironment.getApplication())
  }

  @Test
  fun focusedFieldUnderAPopupKeepsItsEcho() {
    assertTrue(TextEventInterpreter.isCoveredFocusedField(node(focused = true, editable = true)))
  }

  @Test
  fun unfocusedFieldIsDropped() {
    assertFalse(TextEventInterpreter.isCoveredFocusedField(node(focused = false, editable = true)))
  }

  @Test
  fun focusedViewThatIsNotAFieldIsDropped() {
    assertFalse(TextEventInterpreter.isCoveredFocusedField(node(focused = true, editable = false)))
  }

  private fun node(focused: Boolean, editable: Boolean): AccessibilityNodeInfo {
    val node = AccessibilityNodeInfo()
    node.isFocusable = true
    node.isFocused = focused
    node.isEditable = editable
    node.isVisibleToUser = false
    return node
  }
}
