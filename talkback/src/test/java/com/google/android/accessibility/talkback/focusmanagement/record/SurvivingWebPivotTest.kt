/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.focusmanagement.record

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SurvivingWebPivotTest {
  private class Node(id: String, document: Boolean = false) : AccessibilityNodeInfoCompat(
    AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication()))) {
    var owner: Node? = null
    init {
      uniqueId = id
      packageName = "example.web"
      text = id
      addAction(ACTION_NEXT_HTML_ELEMENT)
      if (document) extras.putString("AccessibilityNodeInfo.chromeRole", "rootWebArea")
      shadowOf(unwrap()).setRefreshReturnValue(true)
      ReflectionHelpers.setField(unwrap(), "mSealed", true)
    }
    override fun getParent(): AccessibilityNodeInfoCompat? = owner
    override fun getParent(flags: Int): AccessibilityNodeInfoCompat? = owner
  }

  private fun record(leaf: Node) = FocusActionRecord(leaf,
    FocusActionInfo.builder().setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION).build(), 100,
    { it.run() }) { NodePathDescription.obtainWithoutSiblingContext(leaf) }

  @Test fun anExistingItemInTheSameDocumentCanAnchorNavigationWithoutFocus() {
    val root = Node("document", document = true)
    val leaf = Node("item").apply { owner = root }
    val record = record(leaf)
    val surviving = record.getSurvivingWebPivot(root)!!
    assertEquals("item", surviving.uniqueId)
    assertFalse(surviving.isAccessibilityFocused)
  }

  @Test fun anotherDocumentNeverRestoresTheOldItem() {
    val root = Node("old-document", document = true)
    val leaf = Node("item").apply { owner = root }
    assertNull(record(leaf).getSurvivingWebPivot(Node("new-document", document = true)))
  }

  @Test fun anItemInANestedDocumentCanUseThatDocumentAsItsInputRoot() {
    val outer = Node("outer", document = true)
    val inner = Node("inner", document = true).apply { owner = outer }
    val leaf = Node("item").apply { owner = inner }
    val record = record(leaf)
    assertEquals("item", record.getSurvivingWebPivot(inner)!!.uniqueId)
    assertEquals("item", record.getSurvivingWebPivot(outer)!!.uniqueId)
  }

  @Test fun aRemovedItemDoesNotBecomeAPivot() {
    val root = Node("document", document = true)
    val leaf = Node("item").apply { owner = root }
    val record = record(leaf)
    shadowOf(leaf.unwrap()).setRefreshReturnValue(false)
    assertNull(record.getSurvivingWebPivot(root))
  }

  @Test fun anOrdinaryInputFocusDoesNotUseTheWebFallback() {
    val root = Node("document", document = true)
    val leaf = Node("item").apply { owner = root }
    assertNull(record(leaf).getSurvivingWebPivot(Node("native-input")))
  }
}
