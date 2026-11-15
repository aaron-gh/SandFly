/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UnlabelledIconFocusTest {
  private fun node() = AccessibilityNodeInfo.obtain().apply {
    className = "android.view.View"
    packageName = "example.app"
    isVisibleToUser = true
    isImportantForAccessibility = false
    setBoundsInScreen(Rect(100, 100, 200, 200))
  }

  private fun control(vararg children: AccessibilityNodeInfo) = node().apply {
    isClickable = true
    isFocusable = true
    isScreenReaderFocusable = true
    isImportantForAccessibility = true
    addAction(AccessibilityNodeInfo.ACTION_CLICK)
    children.forEach { shadowOf(this).addChild(it) }
  }

  private fun accepts(node: AccessibilityNodeInfo) =
    AccessibilityNodeInfoUtils.shouldFocusNode(AccessibilityNodeInfoCompat.wrap(node))

  @Test fun aClickableControlWithOneDecorativeIconIsReachable() {
    assertTrue(accepts(control(node())))
  }

  @Test fun anUnlabelledLeafControlRemainsReachable() {
    assertTrue(accepts(control()))
  }

  @Test fun aLabelledControlStillUsesItsLabel() {
    assertTrue(accepts(control(node()).apply { contentDescription = "Remove item" }))
  }

  private fun hasOnlyDecorativeChild(node: AccessibilityNodeInfo) =
    AccessibilityNodeInfoUtils.isClickableWithOnlyDecorativeChild(
      AccessibilityNodeInfoCompat.wrap(node)
    )

  @Test fun textInTheChildStillMakesItsParentSpeak() {
    val parent = control(node().apply { text = "Continue" })
    assertTrue(accepts(parent))
    assertFalse(hasOnlyDecorativeChild(parent))
  }

  @Test fun stateInTheChildStillMakesItsParentSpeak() {
    val parent = control(node().apply { stateDescription = "Expanded" })
    assertTrue(accepts(parent))
    assertFalse(hasOnlyDecorativeChild(parent))
  }

  @Test fun anEmptyContainerAroundAnotherControlIsStillSkipped() {
    val child = control().apply {
      isImportantForAccessibility = false
      setBoundsInScreen(Rect(120, 120, 180, 180))
    }
    assertFalse(accepts(control(child)))
    assertTrue(accepts(child))
  }

  @Test fun anEmptyContainerAroundAFocusableChildIsStillSkipped() {
    assertFalse(accepts(control(node().apply { isFocusable = true })))
  }

  @Test fun anEmptyContainerAroundAScreenReaderFocusableChildIsStillSkipped() {
    assertFalse(accepts(control(node().apply { isScreenReaderFocusable = true })))
  }

  @Test fun anImportantEmptyChildIsNotAssumedToBeDecoration() {
    assertFalse(accepts(control(node().apply { isImportantForAccessibility = true })))
  }

  @Test fun aScrollableChildIsNotAssumedToBeDecoration() {
    assertFalse(accepts(control(node().apply { isScrollable = true })))
  }

  @Test fun multipleEmptyChildrenDoNotEnableAnExtraFocusStop() {
    assertFalse(accepts(control(node(), node())))
  }

  @Test fun anEmptySubtreeIsNotTreatedAsASingleIcon() {
    val container = node().apply { shadowOf(this).addChild(node()) }
    assertFalse(accepts(control(container)))
  }

  @Test fun aNonClickableWrapperIsStillSkipped() {
    assertFalse(accepts(control(node()).apply {
      isClickable = false
      removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
    }))
  }

  @Test fun implicitFocusableWrappersDoNotGainTheException() {
    assertFalse(accepts(control(node()).apply { isScreenReaderFocusable = false }))
  }

  @Test fun anInvisibleControlIsStillSkipped() {
    assertFalse(accepts(control(node()).apply { isVisibleToUser = false }))
  }

  @Test fun touchExplorationTargetsTheControlRatherThanItsDecorativeIcon() {
    val icon = node()
    val button = control(icon)
    val target = AccessibilityNodeInfoUtils.findFocusFromHover(AccessibilityNodeInfoCompat.wrap(icon))
    assertTrue(target == AccessibilityNodeInfoCompat.wrap(button))
  }
}
