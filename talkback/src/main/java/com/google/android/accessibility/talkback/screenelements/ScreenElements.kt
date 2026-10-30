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

package com.google.android.accessibility.talkback.screenelements

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.os.HandlerCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.LOCK_PASS_THROUGH
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * An element that an app draws without exposing it to accessibility services, found by other
 * means, such as SandFly Pro's screen recognition.
 *
 * @param bounds where the element is on screen, in pixels.
 */
data class ScreenElement(
  val label: String,
  val role: Role,
  val bounds: Rect,
  val state: String? = null,
) {
  enum class Role(val className: String) {
    BUTTON("android.widget.Button"),
    TEXT("android.widget.TextView"),
    HEADING("android.widget.TextView"),
    IMAGE("android.widget.ImageView"),
    TEXT_FIELD("android.widget.EditText"),
    CHECK_BOX("android.widget.CheckBox"),
    SWITCH("android.widget.Switch"),
    SLIDER("android.widget.SeekBar"),
    TAB("android.widget.TabWidget"),
    LINK("android.widget.Button"),
  }

  val isActionable: Boolean
    get() = role != Role.TEXT && role != Role.HEADING && role != Role.IMAGE
}

/**
 * The elements shown for the current screen, with the labels given to the app's own unlabelled
 * nodes. Elements are read, found by touch and navigated like the app's nodes; swipes visit them
 * between the app's nodes, after the node each is anchored to (see [setOrder]). Main thread only.
 */
object ScreenElements {
  private const val TAG = "ScreenElements"

  /** Why the screen may have changed under the elements. */
  enum class Change {
    /** Another app or the system took the screen: the elements are gone. */
    WINDOW,
    /**
     * The app with the elements reported a change, or an element was tapped: the elements stay
     * until a screenshot shows that the screen changed.
     */
    CONTENT,
  }

  interface Listener {
    /**
     * Called on the main thread once events stop for [SETTLE_MS], or every [MAX_WAIT_MS] while an
     * app keeps sending them.
     */
    fun onScreenChanged(packageName: String, change: Change)
  }

  /** Told about screen changes, for recognising new screens. */
  @JvmStatic var listener: Listener? = null

  private var windows: ScreenElementWindows? = null
  private val handler = Handler(Looper.getMainLooper())

  private var hostWindowId = -1
  private var hostPackage: String? = null
  private val labels = HashMap<AccessibilityNodeInfoCompat, CharSequence>()

  /** Accessibility window IDs of the element windows, by element index. */
  private val elementWindowIds = HashMap<Int, Int>()

  /** The anchor of each element, once [setOrder] has worked it out. */
  private var anchors: List<AccessibilityNodeInfoCompat?>? = null
  private var firstAppNode: AccessibilityNodeInfoCompat? = null

  /** The host window's nodes in reading order, from [setOrder]. */
  private var appNodes: List<AccessibilityNodeInfoCompat> = emptyList()

  /** Where the focused element was, while accessibility focus is on one. */
  private var focusedBounds: Rect? = null

  private var pendingPackage: String? = null
  private var pendingChange: Change? = null
  private var pendingSince = 0L
  private val notify = Runnable {
    val pkg = pendingPackage
    val change = pendingChange
    pendingPackage = null
    pendingChange = null
    if (pkg != null && change != null) listener?.onScreenChanged(pkg, change)
  }

  @JvmStatic
  fun attach(service: AccessibilityService, pipeline: () -> Pipeline.FeedbackReturner?) {
    windows = ScreenElementWindows(service, pipeline) { scheduleChange(hostPackage, Change.CONTENT) }
  }

  @JvmStatic
  fun detach() {
    clear()
    windows = null
    listener = null
    handler.removeCallbacksAndMessages(null)
  }

  @JvmStatic
  val isShowing: Boolean
    get() = windows?.isShowing() == true || labels.isNotEmpty()

  /** The window whose nodes the elements belong to, or -1. */
  @JvmStatic
  val hostWindow: Int
    get() = hostWindowId

  /**
   * Shows [elements] over the window [windowId] of [packageName], in reading order, and the
   * [labels] for its nodes, in place of what was shown before. With [keepFocus], focus that was on
   * an element goes to the new element at the same place.
   */
  @JvmStatic
  fun show(
    windowId: Int,
    packageName: String,
    elements: List<ScreenElement>,
    labels: Map<AccessibilityNodeInfoCompat, CharSequence>,
    keepFocus: Boolean,
  ) {
    val focused = if (keepFocus) focusedBounds else null
    clear()
    hostWindowId = windowId
    hostPackage = packageName
    this.labels.putAll(labels)
    val current = windows ?: return
    val sorted = inReadingOrder(elements)
    current.show(sorted)
    LogUtils.d(TAG, "Showing %d elements and %d labels over window %d of %s", elements.size, labels.size, windowId, packageName)
    if (focused != null) {
      // By the windows shown, which leave out elements that couldn't be.
      val index = current.elements.indexOfFirst { it.bounds.contains(focused.centerX(), focused.centerY()) }
      // The new windows take a moment to reach accessibility services.
      if (index >= 0) {
        HandlerCompat.postDelayed(handler, { current.focus(index) }, REFOCUS_TOKEN, REFOCUS_DELAY_MS)
      }
    }
  }

  /** Removes the elements, logging [reason]. */
  @JvmStatic
  fun clear(reason: String) {
    if (isShowing) LogUtils.d(TAG, "Removing %d elements: %s", windows?.elements?.size ?: 0, reason)
    clear()
  }

  @JvmStatic
  fun clear() {
    handler.removeCallbacksAndMessages(REFOCUS_TOKEN)
    windows?.clear()
    labels.clear()
    elementWindowIds.clear()
    anchors = null
    firstAppNode = null
    appNodes = emptyList()
    hostWindowId = -1
  }

  /** The label given to the app's [node], or null. */
  @JvmStatic
  fun label(node: AccessibilityNodeInfoCompat): CharSequence? =
    if (labels.isEmpty()) null else labels[node]

  @JvmStatic
  fun isElementWindow(windowId: Int): Boolean {
    if (windows?.isShowing() != true || windowId < 0) return false
    if (elementWindowIds.containsValue(windowId)) return true
    findWindows()
    return elementWindowIds.containsValue(windowId)
  }

  /** The index of the element [node] is, or -1 when it isn't one. */
  @JvmStatic
  fun indexOf(node: AccessibilityNodeInfoCompat): Int {
    if (!isElementWindow(node.windowId)) return -1
    return elementWindowIds.entries.firstOrNull { it.value == node.windowId }?.key ?: -1
  }

  /** The node of the element at [index], or null when its window has gone. */
  @JvmStatic
  fun nodeOf(index: Int): AccessibilityNodeInfoCompat? {
    val service = windows?.service ?: return null
    if (!elementWindowIds.containsKey(index)) findWindows()
    val id = elementWindowIds[index] ?: return null
    val window = service.windows.firstOrNull { it.id == id } ?: return null
    return window.root?.let { AccessibilityNodeInfoCompat.wrap(it) }
  }

  /** Matches each element window to its element by its bounds. */
  private fun findWindows() {
    val current = windows ?: return
    val elements = current.elements
    val bounds = Rect()
    for (window in current.service.windows) {
      if (window.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) continue
      if (window.title?.toString() != ScreenElementWindows.WINDOW_TITLE) continue
      window.getBoundsInScreen(bounds)
      val index = elements.indices.firstOrNull { i ->
        !elementWindowIds.containsKey(i) && elements[i].bounds == bounds
      } ?: continue
      elementWindowIds[index] = window.id
    }
  }

  /**
   * Whether [node] is an app node with nothing of its own to read that the elements cover, such as a
   * game's full-screen view: a node without children that holds the centres of two or more
   * elements. Touch skips it, and swipes go to its elements instead.
   */
  @JvmStatic
  fun isCoveredHost(node: AccessibilityNodeInfoCompat): Boolean {
    val elements = windows?.elements ?: return false
    if (elements.size < 2 || node.windowId != hostWindowId || node.childCount > 0) return false
    val bounds = Rect().also(node::getBoundsInScreen)
    return elements.count { bounds.contains(it.bounds.centerX(), it.bounds.centerY()) } >= 2
  }

  /** Whether the window with the elements is still the app window in front. */
  private fun hostInFront(): Boolean {
    val service = windows?.service ?: return true
    val apps = service.windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
    val front = apps.firstOrNull { it.isActive } ?: apps.maxByOrNull { it.layer } ?: return true
    return front.id == hostWindowId
  }

  /** Whether the elements still need their anchors: call [setOrder] before navigating. */
  @JvmStatic
  val needsOrder: Boolean
    get() = windows?.isShowing() == true && anchors == null

  /**
   * Anchors each element to one of [nodes], the host window's nodes in reading order: the smallest
   * node that holds the element's centre, or else the last node before it on screen (rows top to
   * bottom, left to right in a row). Elements before every node have no anchor.
   */
  @JvmStatic
  fun setOrder(nodes: List<AccessibilityNodeInfoCompat>) {
    val elements = windows?.elements ?: return
    val nodeBounds = nodes.map { Rect().also(it::getBoundsInScreen) }
    anchors = elements.map { element -> nodes.getOrNull(anchorIndex(element.bounds, nodeBounds)) }
    firstAppNode = nodes.firstOrNull()
    appNodes = nodes
    LogUtils.d(
      TAG,
      "Anchors among %d nodes: %s",
      nodes.size,
      elements.indices.joinToString { i -> "${elements[i].label} -> ${anchors?.get(i)?.let(::describe)}" },
    )
  }

  private fun describe(node: AccessibilityNodeInfoCompat): String {
    val bounds = Rect().also(node::getBoundsInScreen)
    val name = node.contentDescription ?: node.text ?: node.className
    return "$name $bounds children=${node.childCount}"
  }

  /**
   * [elements] sorted as they are read: rows from top to bottom, left to right within a row. Two
   * elements are in one row when they overlap vertically by at least half the smaller one's height.
   */
  internal fun inReadingOrder(elements: List<ScreenElement>): List<ScreenElement> {
    // Rows are made by walking from the top, since comparing elements pair by pair isn't
    // transitive and sorting with such a comparison can throw.
    val rows = mutableListOf<MutableList<ScreenElement>>()
    var rowTop = 0
    var rowBottom = 0
    for (element in elements.sortedBy { it.bounds.top }) {
      val b = element.bounds
      val row = rows.lastOrNull()
      val overlap = minOf(rowBottom, b.bottom) - maxOf(rowTop, b.top)
      if (row != null && overlap * 2 >= minOf(rowBottom - rowTop, b.height())) {
        row += element
        rowBottom = maxOf(rowBottom, b.bottom)
      } else {
        rows += mutableListOf(element)
        rowTop = b.top
        rowBottom = b.bottom
      }
    }
    return rows.flatMap { row -> row.sortedBy { it.bounds.left } }
  }

  /** The index in [nodes] of the anchor of an element with [bounds], or -1. */
  internal fun anchorIndex(bounds: Rect, nodes: List<Rect>): Int {
    val x = bounds.centerX()
    val y = bounds.centerY()
    var holder = -1
    var holderArea = Long.MAX_VALUE
    for ((i, b) in nodes.withIndex()) {
      if (b.contains(x, y)) {
        val area = b.width().toLong() * b.height()
        if (area < holderArea) {
          holder = i
          holderArea = area
        }
      }
    }
    if (holder >= 0) return holder
    var before = -1
    for ((i, b) in nodes.withIndex()) {
      val sameRow = b.top < bounds.bottom && b.bottom > bounds.top
      val precedes = b.bottom <= bounds.top || (sameRow && b.centerX() < x)
      if (precedes && (before < 0 || readsAfter(b, nodes[before]))) before = i
    }
    return before
  }

  /** Whether [a] comes after [b] in reading order. */
  private fun readsAfter(a: Rect, b: Rect): Boolean =
    if (a.top < b.bottom && a.bottom > b.top) a.left > b.left else a.top > b.top

  private fun anchorOf(index: Int): AccessibilityNodeInfoCompat? = anchors?.getOrNull(index)

  /** The elements anchored to [node] (null: before every node), in reading order. */
  private fun group(node: AccessibilityNodeInfoCompat?): List<Int> {
    val all = anchors ?: return emptyList()
    return all.indices.filter { all[it] == node }
  }

  /** The element a forward swipe from app [node] goes to, or null for the app's next node. */
  @JvmStatic
  fun firstAfter(node: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? =
    group(node).firstOrNull()?.let(::nodeOf)

  /**
   * The element a backward swipe goes to instead of app node [target], or null. Elements anchored
   * to the target come between it and where the swipe started.
   */
  @JvmStatic
  fun lastBefore(target: AccessibilityNodeInfoCompat?): AccessibilityNodeInfoCompat? =
    group(target).lastOrNull()?.let(::nodeOf)

  /** Whether [node] is the host window's first node, before which unanchored elements come. */
  @JvmStatic
  fun isFirstAppNode(node: AccessibilityNodeInfoCompat): Boolean = firstAppNode == node

  /**
   * Where a swipe from the element at [index] goes: the next or previous element of its group, or
   * null when the swipe leaves the group (see [anchorOf] and [firstAppNodeOrNull]).
   */
  @JvmStatic
  fun neighbour(index: Int, forward: Boolean): AccessibilityNodeInfoCompat? {
    val members = group(anchorOf(index))
    val at = members.indexOf(index)
    val next = if (forward) members.getOrNull(at + 1) else members.getOrNull(at - 1)
    return next?.let(::nodeOf)
  }

  @JvmStatic fun anchor(index: Int): AccessibilityNodeInfoCompat? = anchorOf(index)

  /**
   * Where a swipe goes from a view the elements cover, once past its elements: the next app node, or
   * going backward the previous one or the last element anchored to it. Null at either end.
   */
  @JvmStatic
  fun pastCoveredHost(host: AccessibilityNodeInfoCompat, forward: Boolean): AccessibilityNodeInfoCompat? {
    val at = appNodes.indexOf(host)
    if (at < 0) return null
    val next = appNodes.getOrNull(if (forward) at + 1 else at - 1) ?: return null
    return if (forward) next else lastBefore(next) ?: next
  }

  @JvmStatic fun firstAppNodeOrNull(): AccessibilityNodeInfoCompat? = firstAppNode

  /**
   * Clears the elements when the screen under them changes, and tells [listener]. Called for every
   * event before SandFly handles it.
   */
  @JvmStatic
  fun onAccessibilityEvent(event: AccessibilityEvent) {
    val showing = isShowing
    if (!showing && listener == null) return
    val pkg = event.packageName?.toString() ?: return
    val own = windows?.service?.packageName
    when (event.eventType) {
      AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> {
        focusedBounds =
          if (showing && isElementWindow(event.windowId)) {
            val index = elementWindowIds.entries.firstOrNull { it.value == event.windowId }?.key
            index?.let { windows?.elements?.getOrNull(it)?.bounds }
          } else {
            null
          }
      }
      AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
        if (showing && !hostInFront()) clear("window $hostWindowId no longer in front")
      }
      AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
        // SandFly's own screens, such as its settings, cover the app; its menu and dialogs don't.
        if (showing && !hostInFront()) {
          clear("window $hostWindowId no longer in front")
          if (pkg != own) scheduleChange(pkg, Change.WINDOW)
          return
        }
        if (pkg == own) return
        // Apps, games above all, report state changes of their own window all the time, so those
        // only lead to a check of the screen.
        if (showing && pkg == hostPackage) {
          scheduleChange(pkg, Change.CONTENT)
          return
        }
        if (showing) clear("window of $pkg")
        scheduleChange(pkg, Change.WINDOW)
      }
      AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
        if (showing && event.windowId == hostWindowId) scheduleChange(pkg, Change.CONTENT)
      }
      AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
        if (
          showing &&
            event.windowId == hostWindowId &&
            (event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) != 0
        ) {
          scheduleChange(pkg, Change.CONTENT)
        }
      }
    }
  }

  private fun scheduleChange(pkg: String?, change: Change) {
    if (pkg == null || listener == null) return
    val now = SystemClock.uptimeMillis()
    if (pendingChange == null) pendingSince = now
    // A window change outweighs a content change that is still waiting.
    if (pendingChange != Change.WINDOW || pendingPackage != pkg) pendingChange = change
    pendingPackage = pkg
    handler.removeCallbacks(notify)
    // An app that never stops sending events still gets its screen checked.
    val delay = minOf(SETTLE_MS, pendingSince + MAX_WAIT_MS - now).coerceAtLeast(0)
    handler.postDelayed(notify, delay)
  }

  private const val SETTLE_MS = 800L
  private const val MAX_WAIT_MS = 3000L
  private const val REFOCUS_DELAY_MS = 300L
  private val REFOCUS_TOKEN = Any()
}

/**
 * Shows [ScreenElement]s as accessibility nodes: each in a transparent accessibility overlay
 * window at the element's bounds. Android reports only touchable windows to accessibility services,
 * so the windows are touchable, which also lets touch exploration find them. A click on one taps
 * the screen at its centre, with the windows made untouchable for the tap so that it reaches the
 * app.
 *
 * @param onTapped called once the app has had time to respond to a tap.
 */
class ScreenElementWindows(
  val service: AccessibilityService,
  private val pipeline: () -> Pipeline.FeedbackReturner?,
  private val onTapped: () -> Unit,
) {
  private val windowManager = service.getSystemService(WindowManager::class.java)
  private val handler = Handler(Looper.getMainLooper())
  private val shown = ArrayList<Pair<ElementView, WindowManager.LayoutParams>>()
  /** Whether a tap has locked touch pass-through over the screen and not ended. */
  private var tapping = false

  /** The elements on screen, in reading order. */
  val elements: List<ScreenElement>
    get() = shown.map { it.first.element }

  fun show(elements: List<ScreenElement>) {
    clear()
    for (element in elements) {
      if (element.bounds.isEmpty) continue
      val view = ElementView(service, element)
      val params = layoutParams(element.bounds)
      try {
        windowManager.addView(view, params)
        shown.add(view to params)
      } catch (e: RuntimeException) {
        LogUtils.e(TAG, "Could not show element %s: %s", element.label, e)
      }
    }
  }

  fun clear() {
    handler.removeCallbacksAndMessages(null)
    // The tap's end, which unlocks touch pass-through, was just dropped with the handler's work.
    if (tapping) {
      tapping = false
      pipeline()?.returnFeedback(EVENT_ID_UNTRACKED, Feedback.passThroughMode(LOCK_PASS_THROUGH, null))
    }
    for ((view, _) in shown) {
      try {
        windowManager.removeViewImmediate(view)
      } catch (e: IllegalArgumentException) {
        LogUtils.w(TAG, "Element window was not attached: %s", e)
      }
    }
    shown.clear()
  }

  fun isShowing(): Boolean = shown.isNotEmpty()

  /** Puts accessibility focus on the element at [index]. */
  fun focus(index: Int) {
    shown.getOrNull(index)?.first?.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
  }

  private fun layoutParams(bounds: Rect): WindowManager.LayoutParams =
    WindowManager.LayoutParams().apply {
      type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
      format = PixelFormat.TRANSLUCENT
      flags =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
      gravity = Gravity.TOP or Gravity.START
      x = bounds.left
      y = bounds.top
      width = bounds.width()
      height = bounds.height()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        fitInsetsTypes = 0
      }
      title = WINDOW_TITLE
    }

  /** Makes every element window untouchable, or touchable again. */
  private fun setTouchable(touchable: Boolean) {
    for ((view, params) in shown) {
      params.flags =
        if (touchable) {
          params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
          params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
      try {
        windowManager.updateViewLayout(view, params)
      } catch (e: IllegalArgumentException) {
        LogUtils.w(TAG, "Element window was not attached: %s", e)
      }
    }
  }

  /** Taps the screen at the centre of [view]'s element, or holds there when [long] is set. */
  private fun tap(view: ElementView, long: Boolean): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
    val bounds = view.element.bounds
    val path = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
    val duration =
      if (long) ViewConfiguration.getLongPressTimeout() * 2L else ViewConfiguration.getTapTimeout().toLong()
    val gesture =
      GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        .build()
    setTouchable(false)
    val metrics = service.resources.displayMetrics
    tapping = true
    pipeline()?.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.passThroughMode(LOCK_PASS_THROUGH, Region(0, 0, metrics.widthPixels, metrics.heightPixels)),
    )
    val ended =
      object : AccessibilityService.GestureResultCallback() {
        override fun onCompleted(description: GestureDescription) = afterTap(view)

        override fun onCancelled(description: GestureDescription) = afterTap(view)
      }
    // Android applies the pass-through region and the window flags a moment later.
    handler.postDelayed(
      { if (!service.dispatchGesture(gesture, ended, handler)) afterTap(view) },
      PASS_THROUGH_WAIT_MS,
    )
    return true
  }

  private fun afterTap(view: ElementView) {
    tapping = false
    pipeline()?.returnFeedback(EVENT_ID_UNTRACKED, Feedback.passThroughMode(LOCK_PASS_THROUGH, null))
    // The app gets time to respond. If the screen changed, whoever showed the elements replaces
    // them; until then, focus returns to the element that was tapped.
    handler.postDelayed(
      {
        if (shown.any { it.first === view }) {
          setTouchable(true)
          handler.postDelayed(
            { view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null) },
            REFOCUS_DELAY_MS,
          )
          onTapped()
        }
      },
      SETTLE_MS,
    )
  }

  private inner class ElementView(context: Context, val element: ScreenElement) : View(context) {
    init {
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
      contentDescription = element.label
      isFocusable = true
      if (element.isActionable) {
        isClickable = true
        isLongClickable = true
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && element.role == ScreenElement.Role.HEADING) {
        isAccessibilityHeading = true
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && element.state != null) {
        stateDescription = element.state
      }
    }

    override fun getAccessibilityClassName(): CharSequence = element.role.className

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean =
      when (action) {
        AccessibilityNodeInfo.ACTION_CLICK -> tap(this, long = false)
        AccessibilityNodeInfo.ACTION_LONG_CLICK -> tap(this, long = true)
        else -> super.performAccessibilityAction(action, arguments)
      }
  }

  companion object {
    private const val TAG = "ScreenElements"

    /** The title of every element window. */
    const val WINDOW_TITLE = "SandFly screen elements"

    private const val PASS_THROUGH_WAIT_MS = 350L
    private const val SETTLE_MS = 600L
    private const val REFOCUS_DELAY_MS = 150L
  }
}
