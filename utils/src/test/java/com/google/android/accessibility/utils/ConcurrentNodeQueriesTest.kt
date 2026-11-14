/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConcurrentNodeQueriesTest {
  @Before fun start() { ConcurrentNodeQueries.start() }
  @After fun stop() { ConcurrentNodeQueries.shutdown() }

  private fun node() = AccessibilityNodeInfoCompat.wrap(
    AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication())).apply {
      packageName = "example.web"
      addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
      ReflectionHelpers.setField(this, "mWindowId", 3)
    })

  @Test fun anExpiredBudgetStartsNoRefreshOrDocumentRead() {
    val node = node()
    val filters = AtomicInteger()
    assertTrue(ConcurrentNodeQueries.refreshForRecovery(listOf(node), 0).isEmpty())
    val result = ConcurrentNodeQueries.findDocument(node, object : Filter<AccessibilityNodeInfoCompat>() {
      override fun accept(node: AccessibilityNodeInfoCompat): Boolean { filters.incrementAndGet(); return true }
    }, 0)!!
    assertFalse(result.complete)
    assertNull(result.node)
    assertEquals(0, filters.get())
  }

  @Test fun shutdownCancelsAnInFlightMetadataReadAndRestartAllowsNewReads() {
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    try {
      val read = ConcurrentNodeQueries.readAlongsidePath(node(), Supplier {
        entered.countDown(); release.await(5, TimeUnit.SECONDS); "old"
      })!!
      assertTrue(entered.await(2, TimeUnit.SECONDS))
      ConcurrentNodeQueries.shutdown()
      assertTrue(read.isCancelled)
      assertThrows(CancellationException::class.java) { read.get(1, TimeUnit.SECONDS) }
      assertNull(ConcurrentNodeQueries.readAlongsidePath(node(), Supplier { "stopped" }))
      ConcurrentNodeQueries.start()
      assertEquals("new", ConcurrentNodeQueries.readAlongsidePath(node(), Supplier { "new" })!!.get(1, TimeUnit.SECONDS))
    } finally { release.countDown() }
  }

  @Test fun cancellingAnEmptyPreparedBatchIsSafeAndDoesNotWaitForWorkers() {
    val read = ConcurrentNodeQueries.beginLiveRegions(emptyList(), 5)
    read.cancel()
    assertTrue(read.finish().isEmpty())
  }

  @Test @Config(sdk = [32]) fun unsupportedAndroidVersionsKeepOptionalReadsOnTheNormalPath() {
    assertFalse(ConcurrentNodeQueries.canRead(node()))
    assertNull(ConcurrentNodeQueries.readAlongsidePath(node(), Supplier { error("Unexpected worker") }))
    assertTrue(ConcurrentNodeQueries.beginLiveRegions(emptyList(), 8).finish().isEmpty())
  }
}
