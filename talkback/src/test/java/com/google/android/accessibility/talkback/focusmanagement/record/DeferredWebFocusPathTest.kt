/*
 * Copyright 2026 SandFly contributors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0
 */
package com.google.android.accessibility.talkback.focusmanagement.record

import android.view.View
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** The history's action metadata is immediate; copies retain one complete recovery snapshot. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeferredWebFocusPathTest {
  private fun identityAt(record: FocusActionRecord, time: Long): NodePathDescription =
    ReflectionHelpers.callInstanceMethod(record, "getAncestorIdentityPath",
      ReflectionHelpers.ClassParameter.from(NodePathDescription.Deadline::class.java,
        NodePathDescription.Deadline(time)))
  private fun node() = AccessibilityNodeInfoCompat(
    AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication()))
  ).apply {
    text = "focused text"
    packageName = "example.web"
    className = "android.view.View"
    addAction(AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT)
    ReflectionHelpers.setField(unwrap(), "mSealed", true)
  }

  @Test fun slowPathDoesNotDelayActionMetadataOrCopiesAndIsReadOnce() {
    val executor = Executors.newSingleThreadExecutor()
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    val reads = AtomicInteger()
    val node = node()
    val expected = NodePathDescription.obtainWithoutSiblingContext(node)
    val pathNode = AccessibilityNodeInfoCompat.obtain(node)
    val info = FocusActionInfo.builder().setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION).build()
    try {
      val record = FocusActionRecord(node, info, 1234, executor) {
        reads.incrementAndGet()
        entered.countDown()
        assertTrue(release.await(5, TimeUnit.SECONDS))
        NodePathDescription.obtainWithoutSiblingContext(pathNode)
      }
      assertTrue(entered.await(5, TimeUnit.SECONDS))
      val copy = FocusActionRecord.copy(record)!!
      assertSame(info, record.extraInfo)
      assertEquals(1234, copy.actionTime)
      assertTrue(record.readyAncestorNodes.isEmpty())
      assertTrue(copy.readyAncestorNodes.isEmpty())

      release.countDown()
      assertEquals(expected, record.ancestorIdentityPath)
      assertSame(record.ancestorIdentityPath, copy.ancestorIdentityPath)
      assertSame(record.nodePathDescription, copy.nodePathDescription)
      assertEquals(record, copy)
      assertEquals(record.hashCode(), copy.hashCode())
      assertEquals(1, reads.get())
      assertFalse(record.readyAncestorNodes.isEmpty())
    } finally {
      release.countDown()
      executor.shutdownNow()
    }
  }

  @Test fun aFullWorkerQueueUsesTheOriginalSynchronousCaptureInsteadOfDroppingHistory() {
    val node = node()
    val record = FocusActionRecord(node, FocusActionInfo.builder().build(), 1000,
      { throw RejectedExecutionException("busy") }) {
      NodePathDescription.obtainWithoutSiblingContext(node)
    }
    assertEquals(NodePathDescription.obtainWithoutSiblingContext(node), record.ancestorIdentityPath)
    assertFalse(record.readyAncestorNodes.isEmpty())
  }

  @Test fun workerFailureKeepsMetadataAndCopiesUsableWithoutThrowingFromEquality() {
    val record = FocusActionRecord(node(), FocusActionInfo.builder().build(), 1000,
      { it.run() }) { error("provider failure") }
    val copy = FocusActionRecord.copy(record)!!
    val hash = record.hashCode()
    assertEquals(0, record.ancestorIdentityPath.size())
    assertEquals(record, copy)
    assertEquals(hash, copy.hashCode())
  }

  @Test fun anExpiredConsumerBudgetCancelsAQueuedCaptureAndPinsItsFallback() {
    val work = mutableListOf<Runnable>()
    var reads = 0
    val focused = node()
    val record = FocusActionRecord(focused, FocusActionInfo.builder().build(), 1000,
      { work.add(it) }) { reads++; NodePathDescription.obtainWithoutSiblingContext(focused) }
    val copy = FocusActionRecord.copy(record)!!
    val hash = record.hashCode()
    assertEquals(0, reads)
    assertEquals(record, copy)
    val path = identityAt(record, SystemClock.uptimeMillis())
    assertEquals(0, path.size())
    work.single().run()
    assertEquals(0, reads)
    assertSame(path, copy.ancestorIdentityPath)
    assertEquals(hash, record.hashCode())
  }

  @Test fun aLateWorkerCannotReplaceTheFallbackAfterAnExpiredConsumerBudget() {
    val executor = Executors.newSingleThreadExecutor()
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    val finished = CountDownLatch(1)
    val focused = node()
    val expected = NodePathDescription.obtainWithoutSiblingContext(focused)
    try {
      val record = FocusActionRecord(focused, FocusActionInfo.builder().build(), 1000, executor) {
        entered.countDown(); release.await(5, TimeUnit.SECONDS); finished.countDown(); expected
      }
      assertTrue(entered.await(2, TimeUnit.SECONDS))
      val fallback = identityAt(record, SystemClock.uptimeMillis())
      assertEquals(0, fallback.size())
      release.countDown()
      assertTrue(finished.await(2, TimeUnit.SECONDS))
      assertSame(fallback, record.ancestorIdentityPath)
    } finally { release.countDown(); executor.shutdownNow() }
  }
}
