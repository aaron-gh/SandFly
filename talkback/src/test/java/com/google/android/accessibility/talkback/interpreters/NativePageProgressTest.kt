/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.interpreters

import android.graphics.Rect
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.AccessibilityEventUtils
import com.google.android.accessibility.utils.output.ScrollActionRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NativePageProgressTest {
  private val node = AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication())).apply {
    setBoundsInWindow(Rect(0, 40, 1080, 2094))
  }
  private val list = AccessibilityNodeInfoCompat.wrap(node)
  private val record = ScrollActionRecord(
    1, null, list, ScrollActionRecord.ACTION_SCROLL_COMMAND, 1, ScrollActionRecord.FOCUS,
  )
  private val progress = ReflectionHelpers.callConstructor(
    AutoScrollInterpreter::class.java.declaredClasses.single { it.simpleName == "NativePageProgress" },
    ClassParameter.from(ScrollActionRecord::class.java, record),
  )

  private fun read(deltaY: Int, deltaX: Int = 0): Boolean =
    ReflectionHelpers.callInstanceMethod(
      progress, "read",
      ClassParameter.from(AccessibilityNodeInfoCompat::class.java, list),
      ClassParameter.from(Int::class.javaPrimitiveType, deltaX),
      ClassParameter.from(Int::class.javaPrimitiveType, deltaY),
    )

  @Test fun aForwardPageCompletesOnTheEventThatReachesItsViewportHeight() {
    assertFalse(read(1300))
    assertFalse(read(749))
    assertTrue(read(5))
  }

  @Test fun aBackwardPageAlsoCompletesAtItsFullDistance() {
    assertFalse(read(-1600))
    assertFalse(read(-453))
    assertTrue(read(-1))
  }

  @Test fun aMixedDirectionOrUnknownMovementCannotCompleteThePage() {
    assertFalse(read(1300))
    assertFalse(read(-10))
    assertFalse(read(AccessibilityEventUtils.DELTA_UNDEFINED))
    assertFalse(read(754, deltaX = 5))
    assertFalse(read(754))
  }

  @Test fun aViewportResizeInvalidatesTheMeasuredPage() {
    assertFalse(read(1300))
    node.setBoundsInWindow(Rect(0, 40, 1080, 2050))
    assertFalse(read(710))
    assertFalse(read(44))
  }
}
