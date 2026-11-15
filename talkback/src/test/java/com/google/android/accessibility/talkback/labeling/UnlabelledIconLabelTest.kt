/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.labeling

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
class UnlabelledIconLabelTest {
  private val manager = StoragelessLabelManager().stateReader()

  private fun node() = AccessibilityNodeInfo.obtain().apply {
    className = "android.view.View"
    isEnabled = true
    isVisibleToUser = true
    isImportantForAccessibility = false
    setBoundsInScreen(Rect(100, 100, 200, 200))
  }

  private fun control(vararg children: AccessibilityNodeInfo) = node().apply {
    isClickable = true
    isScreenReaderFocusable = true
    isImportantForAccessibility = true
    children.forEach { shadowOf(this).addChild(it) }
  }

  private fun needsLabel(node: AccessibilityNodeInfo) =
    manager.needsLabel(AccessibilityNodeInfoCompat.wrap(node))

  @Test fun aControlWithADecorativeIconGetsTheUnlabelledAnnouncement() {
    assertTrue(needsLabel(control(node())))
  }

  @Test fun anUnlabelledLeafStillGetsTheAnnouncement() {
    assertTrue(needsLabel(control()))
  }

  @Test fun anAppLabelIsNotReplaced() {
    assertFalse(needsLabel(control(node()).apply { contentDescription = "Remove item" }))
  }

  @Test fun anEmptyParentOfAnInteractiveChildIsNotAnnouncedAsUnlabelled() {
    assertFalse(needsLabel(control(control().apply { isImportantForAccessibility = false })))
  }

  @Test fun aTextContainerIsNotAnnouncedAsUnlabelled() {
    assertFalse(needsLabel(control(node().apply { text = "Continue" })))
  }

  @Test fun aContainerWithAChildStateIsNotAnnouncedAsUnlabelled() {
    assertFalse(needsLabel(control(node().apply { stateDescription = "Expanded" })))
  }

  @Test fun anImportantChildIsNotAssumedToBeDecoration() {
    assertFalse(needsLabel(control(node().apply { isImportantForAccessibility = true })))
  }

  @Test fun aDisabledControlKeepsItsExistingAnnouncementBehavior() {
    assertFalse(needsLabel(control(node()).apply { isEnabled = false }))
  }
}
