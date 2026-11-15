/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.imagecaption

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Automatic captions for a control whose only child is a decorative drawing. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImageCaptionUtilsDecorativeChildTest {

  private val context: Context
    get() = RuntimeEnvironment.getApplication()

  private fun node() = AccessibilityNodeInfo.obtain().apply {
    className = "android.view.View"
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

  private fun needsCaption(node: AccessibilityNodeInfo) =
    ImageCaptionUtils.needImageCaptionForUnlabelledView(
      context,
      AccessibilityNodeInfoCompat.wrap(node),
    )

  @Test
  fun aControlWithADecorativeIcon_isCaptioned() {
    assertTrue(needsCaption(control(node())))
  }

  @Test
  fun aControlWithAnImportantChild_isNotCaptioned() {
    assertFalse(needsCaption(control(node().apply { isImportantForAccessibility = true })))
  }

  @Test
  fun aLargeControlWithADecorativeIcon_isNotCaptioned() {
    assertFalse(
      needsCaption(control(node()).apply { setBoundsInScreen(Rect(0, 0, 1080, 1200)) })
    )
  }
}
