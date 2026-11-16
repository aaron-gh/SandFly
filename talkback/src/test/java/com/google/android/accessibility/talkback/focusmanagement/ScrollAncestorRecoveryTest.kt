/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */

package com.google.android.accessibility.talkback.focusmanagement

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** A row can still be visible when the independently focused control inside it has left. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScrollAncestorRecoveryTest {
  private var nextId = 1L

  private inner class Node(label: String?, bounds: Rect) :
    AccessibilityNodeInfoCompat(AccessibilityNodeInfo.obtain().apply {
      ReflectionHelpers.setField(this, "mSourceNodeId", nextId++)
      className = "android.view.View"
      text = label
      isVisibleToUser = true
      isFocusable = label != null
      isClickable = label != null
      setBoundsInScreen(bounds)
    }) {
    val children = mutableListOf<AccessibilityNodeInfoCompat>()
    var parentNode: Node? = null
    var valid = true
    var parentReads = 0

    override fun refresh() = valid
    override fun getParent(): AccessibilityNodeInfoCompat? {
      parentReads++
      return parentNode
    }
    override fun getChildCount() = children.size
    override fun getChild(index: Int): AccessibilityNodeInfoCompat = children[index]

    fun add(vararg nodes: Node): Node {
      nodes.forEach { it.parentNode = this; children.add(it) }
      return this
    }
  }

  private val filter = Filter.node { it.isClickable }

  private inner class Fixture {
    val oldRow = Node("Old row", Rect(0, 0, 100, 5))
    val wrapper = Node(null, Rect(0, 0, 20, 5))
    val vanished = Node("Photo", Rect(0, 0, 20, 0)).apply {
      valid = false
      isVisibleToUser = false
      parentNode = wrapper
    }
    val nextRow = Node("Next row", Rect(0, 5, 100, 100))
    val list = Node(null, Rect(0, 0, 100, 100)).add(oldRow.add(wrapper), nextRow)

    fun recover(forward: Boolean = true, pivot: Node? = vanished): AccessibilityNodeInfoCompat? =
      FocusProcessorForLogicalNavigation.findFocusAfterInvalidPivot(
        OrderedTraversalStrategy(list), list,
        if (forward) SEARCH_FOCUS_FORWARD else SEARCH_FOCUS_BACKWARD, filter, pivot,
      )
  }

  @Test
  fun forwardRecoveryDoesNotReturnToTheMissingChildsParentRow() {
    val f = Fixture()
    assertEquals(f.nextRow, f.recover())
  }

  @Test
  fun aRemovedChildStillInTheCachedOrderCannotBeFocusedAgain() {
    val f = Fixture()
    f.vanished.isVisibleToUser = true // A cached visible flag survives a failed refresh.
    f.wrapper.add(f.vanished)
    assertEquals(f.nextRow, f.recover())
  }

  @Test
  fun backwardRecoverySkipsTheRemovedChildButKeepsItsParent() {
    val f = Fixture()
    f.list.children.remove(f.nextRow)
    f.vanished.isVisibleToUser = true
    f.wrapper.add(f.vanished)
    assertEquals(f.oldRow, f.recover(forward = false))
  }

  @Test
  fun backwardRecoveryCannotReturnToTheRemovedRowsOwnPhoto() {
    val f = Fixture()
    f.list.children.clear()
    f.list.add(f.nextRow, f.oldRow)
    f.vanished.isVisibleToUser = true
    f.wrapper.add(f.vanished)
    f.oldRow.valid = false
    assertEquals(f.nextRow, f.recover(forward = false, pivot = f.oldRow))
  }

  @Test
  fun laterControlsInsideTheOldRowRemainReachable() {
    val f = Fixture()
    val later = Node("Later control", Rect(20, 0, 100, 5))
    f.oldRow.add(later)
    assertEquals(later, f.recover())
  }

  @Test
  fun backwardRecoveryCanStillReturnToTheParent() {
    val f = Fixture()
    f.list.children.remove(f.nextRow)
    assertEquals(f.oldRow, f.recover(forward = false))
  }

  @Test
  fun removingTheWholeOldItemStillSelectsTheFirstNewItem() {
    val f = Fixture()
    f.list.children.remove(f.oldRow)
    assertEquals(f.nextRow, f.recover())
  }

  @Test
  fun recoveryWithoutThePreviousChildKeepsTheExistingEdgeChoice() {
    val f = Fixture()
    assertEquals(f.oldRow, f.recover(pivot = null))
  }

  @Test
  fun aWebContainerKeepsItsExistingFallback() {
    val f = Fixture()
    f.list.addAction(AccessibilityActionCompat.ACTION_NEXT_HTML_ELEMENT)
    // Native traversal intentionally leaves this container's contents to web navigation.
    assertNull(f.recover())
    assertEquals(0, f.vanished.parentReads)
  }

  @Test
  fun earlyScrollRecoveryUsesTheSameAncestorGuard() {
    val f = Fixture()
    assertEquals(
      f.nextRow,
      FocusProcessorForLogicalNavigation.searchFocusRecoveringMissingPivot(
        OrderedTraversalStrategy(f.list), f.list, f.vanished, SEARCH_FOCUS_FORWARD,
        filter, false, f.list.children,
      ),
    )
  }

  @Test
  fun aValidChildContinuesToItsNextPart() {
    val f = Fixture()
    f.vanished.valid = true
    f.vanished.isVisibleToUser = true
    f.vanished.setBoundsInScreen(Rect(0, 0, 20, 5))
    f.wrapper.add(f.vanished)
    val later = Node("Later control", Rect(20, 0, 100, 5))
    f.oldRow.add(later)
    assertEquals(
      later,
      FocusProcessorForLogicalNavigation.searchFocusRecoveringMissingPivot(
        OrderedTraversalStrategy(f.list), f.list, f.vanished, SEARCH_FOCUS_FORWARD,
        filter, false, f.list.children,
      ),
    )
  }
}
