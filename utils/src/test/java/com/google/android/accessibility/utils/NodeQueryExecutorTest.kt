/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils

import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class NodeQueryExecutorTest {
  @Test fun saturationRejectsWithoutRunningOrCancellingTheCallersFallback() {
    val executor = NodeQueryExecutor("test-queries", 2, 2)
    val entered = CountDownLatch(2)
    val release = CountDownLatch(1)
    val reads = AtomicInteger()
    try {
      repeat(2) { executor.submit { entered.countDown(); release.await(5, TimeUnit.SECONDS) } }
      assertTrue(entered.await(5, TimeUnit.SECONDS))
      val queued = List(2) { executor.submit { reads.incrementAndGet() } }
      val rejected = FutureTask { reads.incrementAndGet() }
      assertThrows(RejectedExecutionException::class.java) { executor.execute(rejected) }
      assertFalse(rejected.isCancelled)
      assertEquals(2, executor.queuedTaskCount())
      assertEquals(4, executor.trackedTaskCount())
      assertEquals(0, reads.get())
      rejected.run()
      assertEquals(1, rejected.get(1, TimeUnit.SECONDS))
      queued.forEach(executor::cancel)
      assertEquals(0, executor.queuedTaskCount())
      release.countDown()
    } finally { release.countDown(); executor.shutdown() }
  }

  @Test fun shutdownCancelsRunningAndQueuedReadsAndRestartGetsFreshWorkers() {
    val executor = NodeQueryExecutor("test-lifecycle", 1, 2)
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    val reads = AtomicInteger()
    try {
      val running = executor.submit { entered.countDown(); release.await(5, TimeUnit.SECONDS) }
      assertTrue(entered.await(5, TimeUnit.SECONDS))
      val queued = executor.submit { reads.incrementAndGet() }
      executor.shutdown()
      assertTrue(running.isCancelled)
      assertTrue(queued.isCancelled)
      assertEquals(0, executor.queuedTaskCount())
      assertEquals(0, executor.trackedTaskCount())
      assertThrows(RejectedExecutionException::class.java) { executor.submit { 1 } }
      executor.start()
      assertEquals(42, executor.submit { 42 }.get(1, TimeUnit.SECONDS))
      assertTrue(executor.coreThreadsTimeOut())
      assertEquals(0, reads.get())
    } finally { release.countDown(); executor.shutdown() }
  }
}
