/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils

import android.os.Build
import android.os.SystemClock
import android.os.Trace
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

/** Independent web-node reads with bounded queues and bounded joins. */
object ConcurrentNodeQueries {
  private const val TAG = "WebNodeQueries"
  private const val THREADS = 8
  private const val PATH_WAIT_MS = 80L
  private const val TREE_WAIT_MS = 300L
  private const val FLAGS = AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_HYBRID or
    AccessibilityNodeInfo.FLAG_PREFETCH_UNINTERRUPTIBLE
  private val executor = NodeQueryExecutor(TAG, THREADS, 16)
  private val liveRegionExecutor = NodeQueryExecutor("WebLiveRegions", THREADS, 16)

  @JvmStatic fun start() { executor.start(); liveRegionExecutor.start() }
  @JvmStatic fun shutdown() { executor.shutdown(); liveRegionExecutor.shutdown() }
  @JvmStatic fun cancel(read: Future<*>) { executor.cancel(read) }

  @JvmStatic fun canRead(node: AccessibilityNodeInfoCompat) = eligible(node)

  private fun eligible(node: AccessibilityNodeInfoCompat) =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && node.windowId >= 0 &&
      !PackageManagerUtils.isTalkBackPackage(node.packageName)

  @JvmStatic
  fun <T> readAlongsidePath(node: AccessibilityNodeInfoCompat, read: Supplier<T>): Future<T>? {
    if (!eligible(node) || !WebInterfaceUtils.supportsWebActions(node)) return null
    return try { executor.submit { read.get() } } catch (_: RejectedExecutionException) { null }
  }

  /** Absent entries were not answered in this attempt; null entries are confirmed removed. */
  @JvmStatic
  fun refreshForRecovery(nodes: List<AccessibilityNodeInfoCompat>, maxWaitMs: Long): Map<AccessibilityNodeInfoCompat, AccessibilityNodeInfoCompat?> {
    if (nodes.isEmpty() || maxWaitMs <= 0 || !executor.isAccepting || !eligible(nodes[0])) return emptyMap()
    data class Answer(val node: AccessibilityNodeInfoCompat, val refreshed: AccessibilityNodeInfoCompat?)
    val pending = nodes.take(64)
    val answers = ConcurrentLinkedQueue<Answer>()
    val done = CountDownLatch(pending.size)
    val stopped = AtomicBoolean()
    val tasks = mutableListOf<Future<*>>()
    val end = SystemClock.uptimeMillis() + minOf(PATH_WAIT_MS, maxWaitMs)
    try {
      for (original in pending) {
        if (Thread.currentThread().isInterrupted || SystemClock.uptimeMillis() >= end) break
        val copy = AccessibilityNodeInfoCompat.obtain(original)
        val read = {
          try {
            if (!stopped.get() && !Thread.currentThread().isInterrupted && SystemClock.uptimeMillis() < end) {
              val value = if (copy.refresh()) copy else null
              if (!stopped.get()) answers.add(Answer(original, value))
            }
          } catch (failure: RuntimeException) { Log.w(TAG, "Could not refresh saved node", failure) }
          finally { done.countDown() }
        }
        try { tasks.add(executor.submit { read() }) }
        catch (_: RejectedExecutionException) { read() }
      }
      done.await((end - SystemClock.uptimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
    } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
    finally { stopped.set(true); tasks.forEach(executor::cancel) }
    return answers.associate { it.node to it.refreshed }
  }

  private fun walk(root: AccessibilityNodeInfoCompat, recurse: Boolean, stopAfter: Int,
    maxWaitMs: Long, flags: Int = FLAGS, descend: (AccessibilityNodeInfoCompat) -> Boolean = { true },
    accepts: (AccessibilityNodeInfoCompat) -> Boolean): BoundedNodeWalk.Result<AccessibilityNodeInfoCompat> {
    val end = SystemClock.uptimeMillis() + maxWaitMs.coerceIn(0, TREE_WAIT_MS)
    return BoundedNodeWalk<AccessibilityNodeInfoCompat>(executor, { (end - SystemClock.uptimeMillis()).coerceAtLeast(0) },
      { it.childCount }, { AccessibilityNodeInfoCompat.obtain(it) },
      { parent, index -> parent.getChild(index, flags) }, accepts,
      { recurse && descend(it) }, stopAfter).run(root)
  }

  @JvmStatic
  fun hasExactlyOneVisibleChild(parent: AccessibilityNodeInfoCompat): Boolean {
    if (!eligible(parent) || parent.childCount < THREADS || !WebInterfaceUtils.supportsWebActions(parent)) {
      return AccessibilityNodeInfoUtils.hasExactlyOneVisibleChild(parent)
    }
    val result = walk(parent, false, 2, TREE_WAIT_MS, flags = 0) { it.isVisibleToUser }
    // One answer alone cannot prove that there is no second visible child.
    return result.complete && result.matches == 1
  }

  @JvmStatic
  fun hasMatchingDescendant(root: AccessibilityNodeInfoCompat, filter: Filter<AccessibilityNodeInfoCompat>): Boolean {
    if (!eligible(root) || !WebInterfaceUtils.supportsWebActions(root)) {
      return AccessibilityNodeInfoUtils.hasMatchingDescendant(root, filter)
    }
    return walk(root, true, 1, TREE_WAIT_MS) { filter.accept(it) }.matches > 0
  }

  data class DocumentSearch(val node: AccessibilityNodeInfoCompat?, val complete: Boolean)

  @JvmStatic
  fun findDocument(root: AccessibilityNodeInfoCompat, matches: Filter<AccessibilityNodeInfoCompat>, maxWaitMs: Long): DocumentSearch? {
    if (!eligible(root)) return null
    if (maxWaitMs <= 0) return DocumentSearch(null, false)
    if (matches.accept(root)) return DocumentSearch(root, true)
    val result = walk(root, true, 1, maxWaitMs, flags = 0,
      descend = { !WebInterfaceUtils.supportsWebActions(it) }) { matches.accept(it) }
    return DocumentSearch(result.found, result.complete || result.found != null)
  }

  interface LiveRegionRead {
    fun finish(): Map<AccessibilityEvent, Boolean>
    fun cancel()
  }

  /** Only the explicitly prepared batch uses workers; unprepared events keep the normal path. */
  @JvmStatic
  fun beginLiveRegions(events: List<AccessibilityEvent>, maxDepth: Int): LiveRegionRead {
    data class Answer(val original: AccessibilityEvent, val assertive: Boolean)
    val eligible = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) emptyList()
      else events.filter { it.windowId >= 0 && !PackageManagerUtils.isTalkBackPackage(it.packageName) }
    val answers = ConcurrentLinkedQueue<Answer>()
    val stopped = AtomicBoolean()
    val tasks = mutableListOf<Future<*>>()
    val end = SystemClock.uptimeMillis() + TREE_WAIT_MS
    for (original in eligible) {
      val copy = AccessibilityEvent.obtain(original)
      try {
        tasks.add(liveRegionExecutor.submit {
          Trace.beginSection("WebNodes.liveRegion.read")
          try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !stopped.get() && !Thread.currentThread().isInterrupted && SystemClock.uptimeMillis() < end) {
              var node = copy.getSource(AccessibilityNodeInfo.FLAG_PREFETCH_ANCESTORS or
                AccessibilityNodeInfo.FLAG_PREFETCH_UNINTERRUPTIBLE)
              var depth = 0
              var result: Boolean? = null
              while (node != null && depth <= maxDepth && !stopped.get() && !Thread.currentThread().isInterrupted && SystemClock.uptimeMillis() < end) {
                val live = node.liveRegion
                if (live != View.ACCESSIBILITY_LIVE_REGION_NONE) {
                  result = live == View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE
                  break
                }
                if (depth == maxDepth) { result = false; break }
                node = node.getParent(AccessibilityNodeInfo.FLAG_PREFETCH_ANCESTORS or
                  AccessibilityNodeInfo.FLAG_PREFETCH_UNINTERRUPTIBLE)
                depth++
              }
              if (node == null) result = false
              if (!stopped.get()) result?.let { answers.add(Answer(original, it)) }
            }
          } catch (failure: RuntimeException) { Log.w(TAG, "Could not read live region", failure) }
          finally { Trace.endSection() }
        })
      } catch (_: RejectedExecutionException) {}
    }
    return object : LiveRegionRead {
      // Called on the main thread before focus speech: takes the answers already in without
      // waiting, and leaves the rest to the caller's synchronous check.
      override fun finish(): Map<AccessibilityEvent, Boolean> {
        cancel()
        val result = IdentityHashMap<AccessibilityEvent, Boolean>()
        answers.forEach { result[it.original] = it.assertive }
        return result
      }
      override fun cancel() { stopped.set(true); tasks.forEach(liveRegionExecutor::cancel) }
    }
  }
}
