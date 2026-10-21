/*
 * Copyright 2026 SandFly contributors
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

package com.google.android.accessibility.talkback.quicksettings

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Uses the system's quick settings the way the user would, through SandFly's accessibility
 * service: it opens them, finds a tile by its name, clicks it and closes them again. This reaches
 * the tiles that only SystemUI can run, such as Hotspot, and apps' tiles without Shizuku.
 *
 * While it works, the screen reader drops accessibility events, as when paused, so nothing of the
 * system's quick settings is said. It gives up after [TIMEOUT_MS].
 *
 * It starts and calls back on the main thread, and looks through the windows on a thread of its
 * own, since each node it reads asks SystemUI.
 */
object SystemPanelClicker {
  private const val TAG = "SystemPanelClicker"
  private const val SYSTEM_UI = "com.android.systemui"
  private const val POLL_MS = 100L
  /** How long the panel has to open before SandFly turns its pages to look for a tile. */
  private const val SETTLE_MS = 700L
  private const val PAGE_MS = 450L
  private const val AFTER_CLICK_MS = 700L
  private const val CLOSE_MS = 450L
  /** How long SystemUI's events are dropped after closing, while the panel slides away. */
  private const val QUIET_AFTER_MS = 1_500L
  private const val TIMEOUT_MS = 8_000L
  private const val MAX_PAGES = 6
  private const val MAX_NODES = 3_000
  private const val MAX_CLICK_PARENTS = 4
  /** AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE, from Android 12. */
  private const val GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE = 15

  sealed class Result {
    /** The tile was clicked. [state] is what it said its state was afterwards, if it said. */
    data class Clicked(val state: CharSequence?, val checked: Boolean?) : Result()

    object NotFound : Result()

    object Failed : Result()
  }

  private val mainHandler = Handler(Looper.getMainLooper())
  @Volatile private var working = false
  private var job: Job? = null

  /** Until when SystemUI's events are dropped after the panel has closed, as it animates. */
  @Volatile private var quietUntil = 0L

  /**
   * Whether the screen reader should drop an event, because SandFly is using the system's quick
   * settings or has just closed them.
   */
  @JvmStatic
  fun shouldDrop(packageName: CharSequence?): Boolean =
    working || (SystemClock.uptimeMillis() < quietUntil && packageName == SYSTEM_UI)

  /** Clicks the tile named [label] in the system's quick settings. */
  fun click(label: String, onDone: (Result) -> Unit) {
    val target = normalize(label)
    start(onDone = { onDone(Result.Failed) }) { job, roots, settled ->
      val node = roots.firstNotNullOfOrNull { findTile(it, target) }
      when {
        node != null -> {
          val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
          job.later(AFTER_CLICK_MS) {
            node.refresh()
            val state =
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) node.stateDescription else null
            val checked = if (node.isCheckable) isChecked(node) else null
            job.finish { onDone(if (clicked) Result.Clicked(state, checked) else Result.Failed) }
          }
          true
        }
        !settled -> false
        job.turnPage(roots) -> true
        else -> {
          job.finish { onDone(Result.NotFound) }
          true
        }
      }
    }
  }

  /** The names of the tiles in the system's quick settings, on all their pages, or null. */
  fun scan(onDone: (List<String>?) -> Unit) {
    // By name without punctuation, since a tile's parts can say its name with a full stop.
    val labels = LinkedHashMap<String, String>()
    var rewound = false
    start(onDone = { onDone(null) }) { job, roots, settled ->
      if (!settled) {
        false
      } else if (!rewound) {
        // The panel can open on the page the user saw last.
        if (!job.rewind(roots)) {
          rewound = true
          job.resetPages()
          job.later(POLL_MS) { job.poll() }
        }
        true
      } else {
        val before = labels.size
        roots.forEach { root ->
          tileLabels(pager(root) ?: root).forEach { labels.putIfAbsent(normalize(it), it) }
        }
        if (labels.size > before && job.turnPage(roots)) {
          true
        } else {
          job.finish { onDone(labels.values.toList()) }
          true
        }
      }
    }
  }

  /**
   * Opens the system's quick settings, then calls [step] with their windows until it returns
   * true, which means it has done something and asked for the next step itself, or finished.
   * [settled] is whether the panel has had time to open completely.
   */
  private fun start(
    onDone: () -> Unit,
    step: (job: Job, roots: List<AccessibilityNodeInfo>, settled: Boolean) -> Boolean,
  ) {
    val service = TalkBackService.getInstance()
    if (service == null || working) {
      onDone()
      return
    }
    val thread = HandlerThread(TAG).apply { start() }
    val newJob = Job(service, Handler(thread.looper), step, onDone)
    job = newJob
    working = true
    newJob.handler.postDelayed(newJob.timeout, TIMEOUT_MS)
    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
    newJob.later(POLL_MS) { newJob.poll() }
  }

  /** One use of the system's quick settings, which runs on [handler]'s thread. */
  private class Job(
    val service: AccessibilityService,
    val handler: Handler,
    val step: (job: Job, roots: List<AccessibilityNodeInfo>, settled: Boolean) -> Boolean,
    val onFailed: () -> Unit,
  ) {
    private val started = SystemClock.uptimeMillis()
    private var pages = 0
    private var forward = true
    @Volatile private var done = false

    val timeout = Runnable {
      LogUtils.w(TAG, "The system's quick settings didn't answer in time")
      finish(onFailed)
    }

    fun poll() {
      if (done) return
      val handled =
        try {
          val roots = systemRoots(service)
          val elapsed = SystemClock.uptimeMillis() - started
          // The panel first shows a row of tiles, and then all of them, on pages.
          val settled =
            roots.isNotEmpty() &&
              elapsed >= SETTLE_MS &&
              (elapsed >= 3 * SETTLE_MS || roots.any { pager(it) != null })
          step(this, roots, settled)
        } catch (e: Throwable) {
          LogUtils.w(TAG, "Cannot use the system's quick settings: %s", e)
          finish(onFailed)
          true
        }
      if (!handled) {
        later(POLL_MS) { poll() }
      }
    }

    /**
     * Shows another page of tiles: the next ones, and once there are none, the ones before the
     * page the panel opened on, which can be any page. Returns false when there are none left.
     */
    fun turnPage(roots: List<AccessibilityNodeInfo>): Boolean {
      val pager = roots.firstNotNullOfOrNull { pager(it) } ?: return false
      while (pages < 2 * MAX_PAGES) {
        val action =
          if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
          else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        if (pager.performAction(action)) {
          pages++
          later(PAGE_MS) { poll() }
          return true
        }
        if (!forward) return false
        forward = false
      }
      return false
    }

    /** Goes back to the first page of tiles, a page at a time. Returns false once there. */
    fun rewind(roots: List<AccessibilityNodeInfo>): Boolean {
      if (pages >= MAX_PAGES) return false
      val pager = roots.firstNotNullOfOrNull { pager(it) } ?: return false
      if (!pager.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) return false
      pages++
      later(PAGE_MS) { poll() }
      return true
    }

    /** Starts counting pages again, after [rewind]. */
    fun resetPages() {
      pages = 0
      forward = true
    }

    fun later(delayMs: Long, run: () -> Unit) {
      handler.postDelayed({ if (!done) run() }, delayMs)
    }

    /**
     * Closes the system's quick settings, then runs [then] on the main thread once they have gone.
     */
    fun finish(then: () -> Unit) {
      if (done) return
      done = true
      handler.removeCallbacks(timeout)
      service.performGlobalAction(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE
        else AccessibilityService.GLOBAL_ACTION_BACK
      )
      mainHandler.postDelayed(
        {
          if (job === this) {
            job = null
            quietUntil = SystemClock.uptimeMillis() + QUIET_AFTER_MS
            working = false
          }
          handler.looper.quitSafely()
          then()
        },
        CLOSE_MS,
      )
    }
  }

  private fun systemRoots(service: AccessibilityService): List<AccessibilityNodeInfo> =
    try {
      service.windows.mapNotNull { it.root }.filter { it.packageName == SYSTEM_UI }
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot read the windows: %s", e)
      emptyList()
    }

  /** The tile whose name starts with [target], within the pager of tiles if there is one. */
  private fun findTile(root: AccessibilityNodeInfo, target: String): AccessibilityNodeInfo? {
    val within = pager(root) ?: root
    val named =
      nodes(within).firstOrNull { node ->
        listOfNotNull(node.contentDescription, node.text).any { normalize(it).startsWith(target) }
      } ?: return null
    return clickable(named)
  }

  private fun clickable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    var current: AccessibilityNodeInfo? = node
    repeat(MAX_CLICK_PARENTS + 1) {
      val candidate = current ?: return null
      if (candidate.isClickable && candidate.isEnabled) return candidate
      current = candidate.parent
    }
    return null
  }

  /**
   * The names of the clickable tiles under [root]: each one's first text, or for a tile that shows
   * only an icon, its first description up to a comma, after which tiles put their state.
   */
  private fun tileLabels(root: AccessibilityNodeInfo): List<String> =
    nodes(root)
      .filter { it.isClickable }
      .mapNotNull { tile ->
        val inside = nodes(tile).toList()
        inside.firstNotNullOfOrNull { clean(it.text) }
          ?: inside.firstNotNullOfOrNull { clean(it.contentDescription?.toString()?.substringBefore(',')) }
      }
      .toList()

  private fun clean(text: CharSequence?): String? = text?.toString()?.trim()?.takeIf { it.isNotEmpty() }

  /** The pages of tiles: the node that scrolls sideways. */
  private fun pager(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
    nodes(root).firstOrNull { node ->
      node.isScrollable &&
        node.actionList.any {
          it.id == AccessibilityAction.ACTION_SCROLL_RIGHT.id ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
              it.id == AccessibilityAction.ACTION_PAGE_RIGHT.id)
        }
    }

  /** [root] and the nodes under it, breadth first, up to a limit. */
  private fun nodes(root: AccessibilityNodeInfo): Sequence<AccessibilityNodeInfo> = sequence {
    val queue = ArrayDeque<AccessibilityNodeInfo>()
    queue.add(root)
    var count = 0
    while (queue.isNotEmpty() && count < MAX_NODES) {
      val node = queue.removeFirst()
      count++
      yield(node)
      for (i in 0 until node.childCount) {
        node.getChild(i)?.let { queue.add(it) }
      }
    }
  }

  @Suppress("DEPRECATION") // getChecked() needs Android 16.
  private fun isChecked(node: AccessibilityNodeInfo): Boolean = node.isChecked

  /** A name without case, spaces, line breaks or punctuation, as tiles differ in those. */
  fun normalize(text: CharSequence): String = text.toString().lowercase().filter { it.isLetterOrDigit() }
}
