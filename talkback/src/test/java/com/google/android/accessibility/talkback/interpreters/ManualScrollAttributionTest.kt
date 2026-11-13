/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.interpreters

import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.input.ScrollEventInterpreter
import com.google.android.accessibility.utils.monitor.TouchMonitor
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ManualScrollAttributionTest {
  @Before fun start() {
    FormFactorUtils.initialize(RuntimeEnvironment.getApplication())
    ScrollEventInterpreter.resetNavigation()
  }
  @After fun stop() { ScrollEventInterpreter.resetNavigation() }

  @Test fun noRecentTouchDoesNotMakeAWebScrollAnAutomaticNavigationScroll() {
    val context = RuntimeEnvironment.getApplication()
    val interpreter = AccessibilityFocusInterpreter(context, null, null, TouchMonitor(), null)
    val web = AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo.obtain(View(context)).apply {
      addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
    })
    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
      eventTime = SystemClock.uptimeMillis()
      org.robolectric.Shadows.shadowOf(this).setWindowId(12)
    }
    assertFalse(interpreter.shouldIgnoreManualScroll(event, web))
    ScrollEventInterpreter.onWebNavigation(12, event.eventTime)
    assertTrue(interpreter.shouldIgnoreManualScroll(event, web))
    ScrollEventInterpreter.resetNavigation()
    assertFalse(interpreter.shouldIgnoreManualScroll(event, web))
  }
}
