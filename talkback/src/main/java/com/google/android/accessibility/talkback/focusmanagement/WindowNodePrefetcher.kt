/*
 * Copyright 2026 Backtalk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.talkback.focusmanagement

import android.os.Build
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.WebInterfaceUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Reads the nodes of a window from the app on several threads at once, before the reading order is
 * built from them on the main thread.
 *
 * Building the order asks the app for one node at a time. Each request waits until the app's main
 * thread has finished what it is doing, and an app that redraws on every frame, such as One UI's
 * notification shade with five or more silent notifications, answers each one only after its next
 * frame: about 8 ms at 120 Hz, or over half a second for the shade. Requests from several threads
 * arrive together and are answered after the same frame, and each answer carries up to 50 nodes.
 * Android drops the extra nodes of an ordinary request when the next request has already been sent,
 * which during a build it always has, so these requests ask for them to be sent with the answer.
 *
 * The nodes land in Android's node cache. Events clear that cache on the main thread, so they stay
 * there until the build on the main thread is done.
 */
object WindowNodePrefetcher {
  private const val TAG = "BacktalkPrefetch"

  private const val THREADS = 8

  /** How long the main thread waits for the nodes before it reads the rest itself. */
  private const val MAX_WAIT_MS = 300L

  /** Stops a window with a very large or looped tree. */
  private const val MAX_NODES = 5000

  private const val FLAGS =
    AccessibilityNodeInfo.FLAG_PREFETCH_SIBLINGS or
      AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_HYBRID or
      AccessibilityNodeInfo.FLAG_PREFETCH_UNINTERRUPTIBLE

  /** Android keeps a request client for every thread that asks for nodes, so the threads stay. */
  private val executor by lazy {
    val number = AtomicInteger()
    Executors.newFixedThreadPool(THREADS) { runnable ->
      Thread(runnable, "$TAG-${number.incrementAndGet()}").apply { isDaemon = true }
    }
  }

  /** Threads reading nodes now. All of them can still be waiting for an app that stopped answering. */
  private val running = AtomicInteger()

  /**
   * Reads the nodes below [root] into Android's node cache, waiting at most [MAX_WAIT_MS]. Does
   * nothing for a window of [ownPackage]: its requests are answered on the main thread, which is
   * waiting here. Does nothing either while every thread is still waiting for an earlier window,
   * since none of them could start on this one.
   */
  @JvmStatic
  fun prefetch(root: AccessibilityNodeInfoCompat, ownPackage: String) {
    if (
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        TextUtils.equals(root.packageName, ownPackage) ||
        running.get() >= THREADS
    ) {
      return
    }
    val start = SystemClock.uptimeMillis()
    val walk = Walk(start + MAX_WAIT_MS)
    walk.submit(root.unwrap())
    walk.await()
    if (LogUtils.shouldLog(Log.DEBUG)) {
      Log.d(TAG, "Read ${walk.nodes.get()} nodes in ${SystemClock.uptimeMillis() - start} ms")
    }
  }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private class Walk(private val deadline: Long) {
    private val lock = ReentrantLock()
    private val finished = lock.newCondition()
    private var pending = 0
    @Volatile private var stopped = false
    /** Nodes compare by window and node ID. */
    private val seen = ConcurrentHashMap.newKeySet<AccessibilityNodeInfo>()
    val nodes = AtomicInteger()

    fun submit(node: AccessibilityNodeInfo) {
      lock.withLock { pending++ }
      try {
        executor.execute {
          running.incrementAndGet()
          try {
            readChildren(node)
          } catch (e: RuntimeException) {
            Log.w(TAG, "Could not read nodes", e)
          } finally {
            running.decrementAndGet()
            done()
          }
        }
      } catch (e: RejectedExecutionException) {
        done()
      }
    }

    private fun done() {
      lock.withLock {
        pending--
        if (pending == 0) {
          finished.signalAll()
        }
      }
    }

    private fun readChildren(node: AccessibilityNodeInfo) {
      for (i in 0 until node.childCount) {
        if (stopped) {
          return
        }
        val child = node.getChild(i, FLAGS) ?: continue
        if (nodes.incrementAndGet() >= MAX_NODES) {
          stopped = true
          return
        }
        // The order is not built inside web content, which is read through its own actions.
        if (
          child.childCount > 0 &&
            seen.add(child) &&
            !WebInterfaceUtils.supportsWebActions(AccessibilityNodeInfoCompat.wrap(child))
        ) {
          submit(child)
        }
      }
    }

    fun await() {
      lock.withLock {
        while (pending > 0) {
          val left = deadline - SystemClock.uptimeMillis()
          if (left <= 0) {
            break
          }
          finished.await(left, TimeUnit.MILLISECONDS)
        }
      }
      stopped = true
    }
  }
}
