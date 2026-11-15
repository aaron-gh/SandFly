/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.focusmanagement.record

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.ConcurrentNodeQueries
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** A refresh not answered in time is unknown, not a removed ancestor. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecoveryPrepareTest {
  @Before fun start() { ConcurrentNodeQueries.start() }
  @After fun stop() { ConcurrentNodeQueries.shutdown() }

  private fun node() = AccessibilityNodeInfoCompat.wrap(
    AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication())).apply {
      packageName = "example.web"
      addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
      ReflectionHelpers.setField(this, "mWindowId", 3)
    })

  private fun deadline() = NodePathDescription.Deadline.afterSearchTimeLimit()

  @Test fun unansweredRefreshesAreLeftToTheLazyRefresh() {
    val busy = CountDownLatch(8)
    val release = CountDownLatch(1)
    try {
      // Every worker is busy, so the refreshes wait in the queue past the recovery wait.
      repeat(8) {
        assertNotNull(ConcurrentNodeQueries.readAlongsidePath(node(), Supplier {
          busy.countDown(); release.await(5, TimeUnit.SECONDS)
        }))
      }
      assertTrue(busy.await(2, TimeUnit.SECONDS))
      val ancestor = node()
      val recovery = NodePathDescription.Recovery()
      recovery.prepare(listOf(ancestor), deadline())
      assertFalse(recovery.refreshed.containsKey(ancestor))
      assertFalse(recovery.confirmed.containsKey(ancestor))
    } finally { release.countDown() }
  }

  @Test fun answeredRefreshesAreKept() {
    val ancestor = node()
    val recovery = NodePathDescription.Recovery()
    recovery.prepare(listOf(ancestor), deadline())
    assertTrue(recovery.confirmed.containsKey(ancestor))
    assertTrue(recovery.refreshed.containsKey(ancestor))
  }
}
