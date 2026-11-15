/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class BoundedNodeWalkTest {
  private data class Node(val id: Int, val children: List<Node> = emptyList())
  private fun remaining(end: Long): Long = TimeUnit.NANOSECONDS.toMillis(end - System.nanoTime()).coerceAtLeast(0)
  private fun end() = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)

  @Test fun aCompleteWalkFiltersOnTheCallerAndCopiesEveryWorkerParent() {
    val executor = NodeQueryExecutor("walk-copy", 2, 2)
    val root = Node(0, listOf(Node(1), Node(2, listOf(Node(3)))))
    val caller = Thread.currentThread()
    val copies = AtomicInteger()
    val reads = AtomicInteger()
    val end = end()
    try {
      val result = BoundedNodeWalk<Node>(executor, { remaining(end) }, { it.children.size },
        { copies.incrementAndGet(); it.copy() }, { node: Node, index ->
          assertNotSame(caller, Thread.currentThread())
          reads.incrementAndGet(); node.children[index]
        }, { assertSame(caller, Thread.currentThread()); it.id == 3 }, { true }, 2, slots = 2).run(root)
      assertTrue(result.complete)
      assertEquals(1, result.matches)
      assertEquals(3, result.found!!.id)
      assertEquals(3, reads.get())
      assertTrue(copies.get() >= reads.get() + 1)
    } finally { executor.shutdown() }
  }

  @Test fun theReadLimitReturnsAnIncompleteAnswerWithoutRewalking() {
    val executor = NodeQueryExecutor("walk-cap", 2, 2)
    val reads = AtomicInteger()
    val end = end()
    try {
      val result = BoundedNodeWalk<Node>(executor, { remaining(end) }, { it.children.size }, { it.copy() },
        { node: Node, index -> reads.incrementAndGet(); node.children[index] }, { false }, { true },
        1, slots = 2, maxReads = 4).run(Node(0, List(5000) { Node(it + 1) }))
      assertFalse(result.complete)
      assertEquals(4, reads.get())
      assertEquals(0, result.matches)
      assertEquals(0, executor.queuedTaskCount())
    } finally { executor.shutdown() }
  }

  @Test fun timeoutKeepsAnObservedMatchButDoesNotClaimACompleteSearch() {
    val executor = NodeQueryExecutor("walk-partial", 1, 2)
    val reads = AtomicInteger()
    var allowed = true
    try {
      val result = BoundedNodeWalk<Node>(executor, { if (allowed) 1000L else 0L }, { it.children.size },
        { it.copy() }, { node: Node, index -> reads.incrementAndGet(); node.children[index] },
        { allowed = false; it.id == 1 }, { false }, 2, slots = 1)
        .run(Node(0, listOf(Node(1), Node(2))))
      assertEquals(1, result.matches)
      assertFalse(result.complete)
      assertEquals(1, reads.get())
    } finally { executor.shutdown() }
  }

  @Test fun aSlowReadCanFinishAfterTimeoutWithoutSharingTheCallersParent() {
    data class Parent(var version: Int)
    val executor = NodeQueryExecutor("walk-timeout", 1, 2)
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    val finished = CountDownLatch(1)
    val observed = AtomicInteger()
    val root = Parent(1)
    val end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(50)
    try {
      val result = BoundedNodeWalk<Parent>(executor, { remaining(end) }, { 1 }, { it.copy() },
        { parent: Parent, _ ->
          entered.countDown()
          release.await(5, TimeUnit.SECONDS)
          observed.set(parent.version); finished.countDown(); null
        }, { false }, { false }, 1, slots = 1).run(root)
      assertTrue(entered.await(1, TimeUnit.SECONDS))
      assertFalse(result.complete)
      root.version = 2
      release.countDown()
      assertTrue(finished.await(2, TimeUnit.SECONDS))
      assertEquals(1, observed.get())
      assertNull(result.found)
    } finally { release.countDown(); executor.shutdown() }
  }

  @Test fun saturationUsesOnlyTheRejectedIndividualReadsOnTheCaller() {
    val executor = NodeQueryExecutor("walk-full", 1, 1)
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    val reads = AtomicInteger()
    val caller = Thread.currentThread()
    val end = end()
    try {
      executor.submit { entered.countDown(); release.await(5, TimeUnit.SECONDS) }
      assertTrue(entered.await(2, TimeUnit.SECONDS))
      executor.submit { release.await(5, TimeUnit.SECONDS) }
      val result = BoundedNodeWalk<Node>(executor, { remaining(end) }, { it.children.size }, { it.copy() },
        { node: Node, index ->
          assertSame(caller, Thread.currentThread()); reads.incrementAndGet(); node.children[index]
        }, { false }, { false }, 1, slots = 2).run(Node(0, listOf(Node(1), Node(2))))
      assertTrue(result.complete)
      assertEquals(2, reads.get())
      assertEquals(1, executor.queuedTaskCount())
    } finally { release.countDown(); executor.shutdown() }
  }

  @Test fun aFailedReadDoesNotStartASecondSynchronousWalk() {
    val executor = NodeQueryExecutor("walk-failed", 2, 2)
    val reads = AtomicInteger()
    val end = end()
    try {
      val result = BoundedNodeWalk<Node>(executor, { remaining(end) }, { it.children.size }, { it.copy() },
        { node: Node, index ->
          reads.incrementAndGet(); if (index == 0) error("provider failed") else node.children[index]
        }, { false }, { false }, 1, slots = 2).run(Node(0, listOf(Node(1), Node(2))))
      assertFalse(result.complete)
      assertEquals(2, reads.get())
    } finally { executor.shutdown() }
  }
}
