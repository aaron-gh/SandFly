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

package com.google.android.accessibility.talkback.macros

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.gesture.OpenAppActions
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.talkback.utils.NotificationUtils
import com.google.android.accessibility.utils.PerformActionUtils
import com.google.android.accessibility.utils.preference.BasePreferencesActivity
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Records the steps of a macro as the user does them: presses on items, by touch or with SandFly,
 * scrolls, text typed, apps opened from the launcher and some SandFly commands, such as Back.
 * [MacroRecording] makes the steps, with the repeats and conditions they imply. A notification is
 * shown while recording; opening it, or running Stop macro, opens the macro, which ends the
 * recording and offers the steps.
 *
 * Everything runs on the main thread.
 */
object MacroRecorder {
  /** The steps recorded for the macro [macroId], to be added at [at], or at the end for null. */
  class Result(val macroId: String, val at: Int?, val steps: List<Macros.Step>, val skippedPasswords: Boolean)

  private class Session(
    val macroId: String,
    val name: String,
    val at: Int?,
    val forApp: Boolean,
    val startTime: Long,
    val ownPkg: String,
    val homePkg: String?,
    val imePkg: String?,
  ) {
    val actions = ArrayList<MacroRecording.Action>()
    var started = false
    var startPkg: String? = null
    var frontPkg: String? = null
    var lastPressNode: AccessibilityNodeInfoCompat? = null
    var lastPressTime = Long.MIN_VALUE / 2
    var lastSandFlyTime = Long.MIN_VALUE / 2
    var lastSandFlyScroll = Long.MIN_VALUE / 2
    /** When the last action other than a scroll was recorded. */
    var lastActionTime = Long.MIN_VALUE / 2
    var touching = false
    var touchStart = Long.MIN_VALUE / 2
    var touchEnd = Long.MIN_VALUE / 2
    var touchCount = 0
    var scrollNode: AccessibilityNodeInfo? = null
    var scrollTouch = -1
    var scrollIndex = -1
    var scrollY = -1
    var typeNode: AccessibilityNodeInfo? = null
    /** The item SandFly is about to press, and what finds it on the screen before the press. */
    var aboutToPress: Pair<AccessibilityNodeInfoCompat, Pair<String, Macros.Field?>?>? = null
    var skippedPasswords = false
    /** Presses whose aftermath is still to be checked, by index, with the switch they pressed. */
    val pending = LinkedHashMap<Int, Pending>()
  }

  private class Pending(val checkable: AccessibilityNodeInfoCompat?, val checkedBefore: Boolean?)

  private val handler = Handler(Looper.getMainLooper())

  @Volatile private var session: Session? = null

  private val listener =
    object : PerformActionUtils.ActionListener {
      override fun onActionPerformed(node: AccessibilityNodeInfoCompat, action: Int) {
        if (Looper.myLooper() == Looper.getMainLooper()) onAction(node, action) else handler.post { onAction(node, action) }
      }

      override fun onActionPerforming(node: AccessibilityNodeInfoCompat, action: Int) {
        if (Looper.myLooper() == Looper.getMainLooper()) beforeAction(node, action)
      }
    }

  private val checkRunnable = Runnable { checkPending() }

  /** Whether steps are being recorded for the macro [macroId]. */
  @JvmStatic fun isRecording(macroId: String): Boolean = session?.macroId == macroId

  /**
   * Starts recording steps for the macro [macroId], named [name], to be added at [at]. [forApp]
   * when the macro runs in one app only, which then isn't opened first. Ends any recording before
   * it.
   */
  fun start(context: Context, macroId: String, name: String, at: Int?, forApp: Boolean) {
    val home =
      context.packageManager
        .resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
        ?.activityInfo
        ?.packageName
    val ime = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)?.substringBefore('/')
    handler.removeCallbacks(checkRunnable)
    session = Session(macroId, name, at, forApp, SystemClock.uptimeMillis(), context.packageName, home, ime)
    PerformActionUtils.addActionListener(listener)
    notify(context, macroId, name)
  }

  /** Ends the recording, and gives its steps, or null when nothing was being recorded. */
  fun stop(context: Context): Result? {
    val ended = session ?: return null
    checkPending()
    session = null
    handler.removeCallbacks(checkRunnable)
    PerformActionUtils.removeActionListener(listener)
    context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    val startApp = ended.startPkg?.let { launchValue(context, it) }
    val steps =
      MacroRecording.steps(
        ended.actions,
        ended.startTime,
        startPkg = if (ended.forApp) null else ended.startPkg,
        startApp = startApp,
        trailing = endingCommands(context),
      )
    return Result(ended.macroId, ended.at, steps, ended.skippedPasswords)
  }

  /** Opens the screen of the macro being recorded, which ends the recording. Returns false when none is. */
  fun openMacro(context: Context): Boolean {
    val current = session ?: return false
    try {
      context.startActivity(macroIntent(context, current.macroId))
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot open the macro: %s", e)
      return false
    }
    return true
  }

  /** Records the SandFly command [value] when it is one a macro step repeats. */
  @JvmStatic
  fun onCommand(context: Context, value: String) {
    val current = session ?: return
    val now = SystemClock.uptimeMillis()
    val action =
      when {
        OpenAppActions.isAction(value) -> MacroRecording.AppOpened(now, current.frontPkg, value)
        value in recordedCommands(context) -> MacroRecording.Command(now, current.frontPkg, value)
        else -> return
      }
    add(current, action)
  }

  @JvmStatic
  fun onAccessibilityEvent(service: TalkBackService, event: AccessibilityEvent) {
    val current = session ?: return
    when (event.eventType) {
      AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
        current.touching = true
        current.touchStart = SystemClock.uptimeMillis()
        current.touchCount++
      }
      AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> {
        current.touching = false
        current.touchEnd = SystemClock.uptimeMillis()
      }
      AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> onWindow(service, current, event)
      AccessibilityEvent.TYPE_VIEW_CLICKED, AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
        val source = event.source ?: return
        val long = event.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED
        onPress(current, AccessibilityNodeInfoCompat.wrap(source), long, fromSandFly = false, eventChecked = event.isChecked)
      }
      AccessibilityEvent.TYPE_VIEW_SCROLLED -> onScrollEvent(current, event)
      AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> onText(current, event)
    }
  }

  private fun onWindow(service: TalkBackService, current: Session, event: AccessibilityEvent) {
    val pkg = event.packageName?.toString() ?: return
    if (pkg == current.ownPkg || pkg == current.imePkg || pkg == MacroRecording.SYSTEM_UI) return
    if (!current.started) {
      begin(current, pkg)
      return
    }
    if (pkg == current.frontPkg) return
    current.frontPkg = pkg
    // An app opened from the launcher is opened with Open app, not by its place in the launcher.
    val last = current.actions.lastOrNull()
    if (last is MacroRecording.Press && last.inLauncher && SystemClock.uptimeMillis() - last.time < LAUNCH_MS && pkg != current.homePkg) {
      val value = launchValue(service, pkg) ?: return
      current.pending.remove(current.actions.size - 1)
      current.actions[current.actions.size - 1] = MacroRecording.AppOpened(last.time, last.pkg, value)
    }
  }

  /** Starts with the app [pkg] in front: the app shown when SandFly's settings go to the back. */
  private fun begin(current: Session, pkg: String) {
    current.started = true
    current.startPkg = pkg.takeIf { it != current.homePkg }
    current.frontPkg = pkg
  }

  /**
   * Works out what finds the item SandFly is about to press while the screen is as it was: once
   * pressed, the app can be changing it, as Settings does when a switch turns.
   */
  private fun beforeAction(node: AccessibilityNodeInfoCompat, action: Int) {
    val current = session ?: return
    if (action != AccessibilityNodeInfo.ACTION_CLICK && action != AccessibilityNodeInfo.ACTION_LONG_CLICK) return
    if (isIgnored(current, node.packageName, node.unwrap())) return
    current.aboutToPress = node to TalkBackService.getInstance()?.let { labelFinding(it, node) }
  }

  private fun onAction(node: AccessibilityNodeInfoCompat, action: Int) {
    val current = session ?: return
    when (action) {
      AccessibilityNodeInfo.ACTION_CLICK -> onPress(current, node, long = false, fromSandFly = true, eventChecked = null)
      AccessibilityNodeInfo.ACTION_LONG_CLICK -> onPress(current, node, long = true, fromSandFly = true, eventChecked = null)
      else -> scrollDirection(action)?.let { forward -> onScroll(current, node.unwrap(), forward, fromSandFly = true) }
    }
  }

  private fun scrollDirection(action: Int): Boolean? =
    when (action) {
      AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
      AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id,
      AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.id -> true
      AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,
      AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id,
      AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.id -> false
      else -> null
    }

  private fun isIgnored(current: Session, pkg: CharSequence?, node: AccessibilityNodeInfo?): Boolean {
    val name = pkg?.toString() ?: return true
    if (name == current.ownPkg || name == current.imePkg) return true
    return try {
      node?.window?.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
    } catch (e: RuntimeException) {
      false
    }
  }

  private fun onPress(
    current: Session,
    node: AccessibilityNodeInfoCompat,
    long: Boolean,
    fromSandFly: Boolean,
    eventChecked: Boolean?,
  ) {
    val now = SystemClock.uptimeMillis()
    // The app reports a press SandFly made too, on the same item or one around it.
    if (!fromSandFly && now - current.lastSandFlyTime < SAME_PRESS_MS) return
    if (node == current.lastPressNode && now - current.lastPressTime < SAME_PRESS_MS) return
    if (fromSandFly) current.lastSandFlyTime = now
    if (isIgnored(current, node.packageName, node.unwrap())) return
    val pkg = node.packageName.toString()
    if (!current.started) begin(current, pkg)
    val checked = current.aboutToPress?.takeIf { fromSandFly && it.first == node }
    current.aboutToPress = null
    val label = (if (checked != null) checked.second else TalkBackService.getInstance()?.let { labelFinding(it, node) }) ?: labelOf(node)
    val bounds = Rect().also { node.getBoundsInScreen(it) }
    // A switch or check box, or one in the row pressed, and whether it was on before the press.
    // The app has turned it already when it reports a press by touch.
    val checkable = if (node.isCheckable) node else checkableIn(node)
    @Suppress("DEPRECATION")
    val checkedBefore =
      when {
        checkable == null || long -> null
        checkable == node && !fromSandFly -> eventChecked?.let { !it } ?: !node.isChecked
        else -> checkable.isChecked
      }
    val last = current.actions.lastOrNull()
    val inScrolled =
      last is MacroRecording.Scroll &&
        current.scrollNode?.let { list -> node.unwrap() == list || ancestors(node.unwrap()).any { it == list } } == true
    val press =
      MacroRecording.Press(
        time = now,
        pkg = pkg,
        label = label?.first.orEmpty(),
        field = label?.second,
        regex = label != null,
        long = long,
        // A long press by touch, or SandFly's double-tap and hold, which passes a held touch to
        // the app, is kept as a held touch: some apps act only on that.
        held = long && !fromSandFly,
        x = bounds.centerX(),
        y = bounds.centerY(),
        inScrolled = inScrolled,
        inLauncher = pkg == current.homePkg,
        editable = node.isEditable,
      )
    current.lastPressNode = node
    current.lastPressTime = now
    if (!add(current, press)) return
    current.pending[current.actions.size - 1] = Pending(checkable, checkedBefore)
    handler.removeCallbacks(checkRunnable)
    handler.postDelayed(checkRunnable, CHECK_DELAY_MS)
  }

  private fun onScrollEvent(current: Session, event: AccessibilityEvent) {
    val now = SystemClock.uptimeMillis()
    val source = event.source ?: return
    // Only scrolls the user made, by touch or by moving through a list, not the app's own.
    val byTouch = current.touching || now - current.touchEnd < SCROLL_AFTER_TOUCH_MS
    if (!byTouch || current.touchStart < current.lastActionTime) return
    if (now - current.lastSandFlyScroll < SAME_SCROLL_MS) return
    var delta =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) event.scrollDeltaY.takeIf { it != 0 } ?: event.scrollDeltaX else 0
    val sameList = source == current.scrollNode
    if (delta == 0 && sameList && event.fromIndex >= 0 && current.scrollIndex >= 0) delta = event.fromIndex - current.scrollIndex
    if (delta == 0 && sameList && event.scrollY >= 0 && current.scrollY >= 0) delta = event.scrollY - current.scrollY
    current.scrollIndex = event.fromIndex
    current.scrollY = event.scrollY
    if (delta == 0) {
      current.scrollNode = source
      return
    }
    onScroll(current, source, delta > 0, fromSandFly = false)
  }

  private fun onScroll(current: Session, node: AccessibilityNodeInfo, forward: Boolean, fromSandFly: Boolean) {
    if (isIgnored(current, node.packageName, node)) return
    if (!current.started) begin(current, node.packageName.toString())
    val now = SystemClock.uptimeMillis()
    if (fromSandFly) current.lastSandFlyScroll = now
    val last = current.actions.lastOrNull()
    if (last is MacroRecording.Scroll && node == current.scrollNode && last.forward == forward) {
      // One more scroll: each SandFly scroll, or each touch, as a fling sends many events.
      if (fromSandFly || current.touchCount != current.scrollTouch) {
        current.actions[current.actions.size - 1] = last.copy(count = last.count + 1)
      }
      current.scrollTouch = current.touchCount
      return
    }
    val id = idOf(AccessibilityNodeInfoCompat.wrap(node))
    current.scrollNode = node
    current.scrollTouch = current.touchCount
    add(
      current,
      MacroRecording.Scroll(
        now,
        node.packageName?.toString(),
        id.orEmpty(),
        if (id != null) Macros.Field.RESOURCE_ID else null,
        forward,
        regex = id != null,
      ),
      scroll = true,
    )
  }

  private fun onText(current: Session, event: AccessibilityEvent) {
    val source = event.source ?: return
    if (!source.isEditable || isIgnored(current, source.packageName, source)) return
    if (!current.started) begin(current, source.packageName.toString())
    if (event.isPassword || source.isPassword) {
      current.skippedPasswords = true
      return
    }
    val text =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && source.isShowingHintText) ""
      else (event.text.firstOrNull() ?: source.text)?.toString().orEmpty()
    val last = current.actions.lastOrNull()
    if (last is MacroRecording.Type && source == current.typeNode) {
      // A field emptied after typing, as a search field is once searched, keeps what was typed.
      if (text.isNotEmpty() || last.text.isEmpty()) {
        current.actions[current.actions.size - 1] = last.copy(text = text.take(Macros.MAX_TEXT))
      }
      return
    }
    if (text.isEmpty()) return
    val pressed = current.lastPressNode?.unwrap()
    val afterPress =
      last is MacroRecording.Press && (pressed == source || last.editable || (pressed != null && ancestors(source).any { it == pressed }))
    val label = fieldLabel(AccessibilityNodeInfoCompat.wrap(source))
    current.typeNode = source
    add(
      current,
      MacroRecording.Type(
        SystemClock.uptimeMillis(),
        source.packageName?.toString(),
        label?.first.orEmpty(),
        label?.second,
        text.take(Macros.MAX_TEXT),
        afterPress,
        regex = label != null,
      ),
    )
  }

  /** Adds [action], unless the recording has as many as it keeps. Returns whether it did. */
  private fun add(current: Session, action: MacroRecording.Action, scroll: Boolean = false): Boolean {
    // What the presses before it left on screen is checked before this changes it further.
    if (current.pending.isNotEmpty()) checkPending()
    if (current.actions.size >= MAX_ACTIONS) return false
    current.actions += action
    if (!scroll) current.lastActionTime = action.time
    return true
  }

  /**
   * Notes, for each press not checked yet, which items pressed so far are on screen, and whether
   * the switch it pressed turned.
   */
  private fun checkPending() {
    val current = session ?: return
    if (current.pending.isEmpty()) return
    val service = TalkBackService.getInstance()
    val labels =
      current.actions
        .filterIsInstance<MacroRecording.Press>()
        .filter { it.label.isNotEmpty() && it.regex }
        .map { it.label to it.field }
        .distinct()
        .take(MAX_CHECKED_LABELS)
    val shown =
      try {
        service?.let { shownLabels(it, labels) }
      } catch (e: RuntimeException) {
        LogUtils.w(TAG, "Cannot read the windows: %s", e)
        null
      }
    for ((index, pending) in current.pending) {
      val press = current.actions.getOrNull(index) as? MacroRecording.Press ?: continue
      @Suppress("DEPRECATION")
      val turned =
        pending.checkable?.let { node ->
          val before = pending.checkedBefore ?: return@let null
          try {
            // The app can replace the item once it changes, as Settings does with its switches.
            val after = if (node.refresh()) node.isChecked else service?.let { checkedState(it, press.label, press.field) }
            if (after != null && after != before) before else null
          } catch (e: RuntimeException) {
            null
          }
        }
      current.actions[index] = press.copy(shownAfter = shown, checkedBefore = turned)
    }
    current.pending.clear()
  }

  /**
   * Whether the switch or check box an item found by [label] in [field] is, or has in the row
   * around it, is checked; null when there is none.
   */
  @Suppress("DEPRECATION")
  private fun checkedState(service: TalkBackService, label: String, field: Macros.Field?): Boolean? {
    val found = MacroRunner.firstItem(service, label, regex = true, field = field) ?: return null
    if (found.isCheckable) return found.isChecked
    var around: AccessibilityNodeInfo? = found.parent
    repeat(ROW_LEVELS) {
      val row = around ?: return null
      checkableIn(AccessibilityNodeInfoCompat.wrap(row))?.let { return it.isChecked }
      around = row.parent
    }
    return null
  }

  /**
   * What a step finds [node] by, of those [labelOf] would choose from, that finds [node] first, as
   * the step does when it runs on this screen: such as a switch's description, when the row's
   * title has the same text. Null when none of them does.
   */
  private fun labelFinding(service: TalkBackService, node: AccessibilityNodeInfoCompat): Pair<String, Macros.Field?>? {
    val target = pressTarget(node.unwrap())
    return try {
      labelChoices(node).firstOrNull { (label, field) ->
        MacroRunner.firstItem(service, label, regex = true, field = field)?.let { pressTarget(it) == target } == true
      }
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot read the windows: %s", e)
      null
    }
  }

  /** The item a step presses for [node]: it, or the nearest item around it that can be pressed. */
  private fun pressTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo =
    (sequenceOf(node) + ancestors(node).take(PRESS_LEVELS)).firstOrNull { it.isClickable && it.isEnabled } ?: node

  /** What a step can find [node] by, in the order [labelOf] prefers them, with narrower fields. */
  private fun labelChoices(node: AccessibilityNodeInfoCompat): List<Pair<String, Macros.Field?>> {
    if (node.isEditable) return listOfNotNull(fieldLabel(node))
    val choices = ArrayList<Pair<String, Macros.Field?>>()
    clean(node.text)?.let {
      choices += it to null
      choices += it to Macros.Field.TEXT
    }
    clean(node.contentDescription)?.let {
      choices += it to null
      choices += it to Macros.Field.DESCRIPTION
    }
    firstText(node)?.let { choices += it to null }
    if (node.isCheckable) {
      var parent = node.parent
      repeat(2) {
        val row = parent ?: return@repeat
        firstText(row)?.let { choices += it to null }
        parent = row.parent
      }
    }
    idOf(node)?.let { choices += it to Macros.Field.RESOURCE_ID }
    return choices.distinct()
  }

  /** The keys of [labels] that an item on screen has, as an Item on screen test finds them. */
  private fun shownLabels(service: TalkBackService, presses: List<Pair<String, Macros.Field?>>): Set<String> {
    val labels = presses.mapNotNull { (label, field) -> Macros.compilePattern(label)?.let { Triple(label, field, it) } }
    if (labels.isEmpty()) return emptySet()
    val found = HashSet<String>()
    val roots =
      service.windows
        .filter { it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY && it.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        .mapNotNull { it.root }
    var count = 0
    for (root in roots) {
      val queue = ArrayDeque<AccessibilityNodeInfo>()
      queue.add(root)
      while (queue.isNotEmpty() && count < MAX_NODES) {
        val node = queue.removeFirst()
        count++
        if (node.isVisibleToUser) {
          for ((label, field, pattern) in labels) {
            if (texts(node, field).any { pattern.matcher(it.toString().trim().take(Macros.MAX_PATTERN_INPUT)).find() }) {
              found += MacroRecording.labelKey(label, field)
            }
          }
          if (found.size == labels.size) return found
        }
        for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
      }
    }
    return found
  }

  private fun texts(node: AccessibilityNodeInfo, field: Macros.Field?): List<CharSequence> {
    val compat = AccessibilityNodeInfoCompat.wrap(node)
    return when (field) {
      null -> listOfNotNull(node.text, node.contentDescription, node.hintText, compat.stateDescription, compat.tooltipText)
      Macros.Field.TEXT -> listOfNotNull(node.text)
      Macros.Field.DESCRIPTION -> listOfNotNull(node.contentDescription)
      Macros.Field.HINT -> listOfNotNull(node.hintText)
      Macros.Field.STATE -> listOfNotNull(compat.stateDescription)
      Macros.Field.TOOLTIP -> listOfNotNull(compat.tooltipText)
      Macros.Field.RESOURCE_ID -> node.viewIdResourceName?.let { id -> listOf(id, id.substringAfter(":id/")).distinct() }.orEmpty()
    }
  }

  /**
   * What a step finds [node] by, as a regular expression for the whole text: its text or
   * description, else the first text in it, else, for a switch without text, the first text in the
   * row around it, else its resource ID. Null when it has none of them.
   */
  private fun labelOf(node: AccessibilityNodeInfoCompat): Pair<String, Macros.Field?>? = labelChoices(node).firstOrNull()

  /** What finds the edit field [node]: its hint, description or resource ID, not its text. */
  private fun fieldLabel(node: AccessibilityNodeInfoCompat): Pair<String, Macros.Field?>? {
    clean(node.hintText)?.let { return it to Macros.Field.HINT }
    clean(node.contentDescription)?.let { return it to Macros.Field.DESCRIPTION }
    return idOf(node)?.let { it to Macros.Field.RESOURCE_ID }
  }

  private fun idOf(node: AccessibilityNodeInfoCompat): String? =
    node.viewIdResourceName?.substringAfter(":id/")?.takeIf { it.isNotBlank() }?.let { MacroRecording.wholeText(it) }

  /** The first text or description of the visible items in [node], in order. */
  private fun firstText(node: AccessibilityNodeInfoCompat): String? {
    val stack = ArrayDeque<AccessibilityNodeInfoCompat>()
    for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let { stack.addLast(it) }
    var count = 0
    while (stack.isNotEmpty() && count < MAX_LABEL_NODES) {
      val item = stack.removeLast()
      count++
      if (!item.isVisibleToUser) continue
      (clean(item.contentDescription) ?: clean(item.text))?.let { return it }
      for (i in item.childCount - 1 downTo 0) item.getChild(i)?.let { stack.addLast(it) }
    }
    return null
  }

  /** The first switch or check box in [node], a row pressed, within a few levels. */
  private fun checkableIn(node: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
    val queue = ArrayDeque<AccessibilityNodeInfoCompat>()
    for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
    var count = 0
    while (queue.isNotEmpty() && count < MAX_LABEL_NODES) {
      val item = queue.removeFirst()
      count++
      if (item.isCheckable) return item
      for (i in 0 until item.childCount) item.getChild(i)?.let { queue.add(it) }
    }
    return null
  }

  /**
   * [text] as a regular expression for the whole text, or null when it is empty. A long text, or
   * one over several lines, is found by its start: its first line, at most [MAX_LABEL_CHARS].
   */
  private fun clean(text: CharSequence?): String? {
    val trimmed = text?.toString()?.trim() ?: return null
    var end = trimmed.length
    val line = trimmed.indexOfFirst { it.isWhitespace() && it != ' ' }
    if (line >= 0) end = line
    val spaces = trimmed.indexOf("  ")
    if (spaces in 0 until end) end = spaces
    end = minOf(end, MAX_LABEL_CHARS)
    val start = trimmed.substring(0, end).trimEnd()
    if (start.isEmpty()) return null
    return MacroRecording.wholeText(start, prefix = end < trimmed.length)
  }

  private fun ancestors(node: AccessibilityNodeInfo): Sequence<AccessibilityNodeInfo> =
    generateSequence(node.parent) { it.parent }.take(MAX_LEVELS)

  /** The Open app value of the app [pkg], or null when it has no launcher entry. */
  private fun launchValue(context: Context, pkg: String): String? =
    context.packageManager.getLaunchIntentForPackage(pkg)?.component?.let { OpenAppActions.valueOf(it) }

  @Volatile private var commands: Set<String>? = null

  private fun recordedCommands(context: Context): Set<String> =
    commands
      ?: listOf(
          R.string.shortcut_value_back,
          R.string.shortcut_value_home,
          R.string.shortcut_value_overview,
          R.string.shortcut_value_notifications,
          R.string.shortcut_value_quick_settings,
          R.string.shortcut_value_all_apps,
          R.string.shortcut_value_select_all,
          R.string.shortcut_value_copy,
          R.string.shortcut_value_cut,
          R.string.shortcut_value_paste,
        )
        .map { context.getString(it) }
        .toSet()
        .also { commands = it }

  /** Commands that open what ends a recording: the notifications, quick settings or recent apps. */
  private fun endingCommands(context: Context): Set<String> =
    listOf(
        R.string.shortcut_value_overview,
        R.string.shortcut_value_notifications,
        R.string.shortcut_value_quick_settings,
        R.string.shortcut_value_all_apps,
      )
      .map { context.getString(it) }
      .toSet()

  private fun macroIntent(context: Context, macroId: String): Intent =
    Intent(context, TalkBackPreferencesActivity.TalkBackSubSettings::class.java)
      // A task this opened before is brought to the front without the new intent unless it is cleared.
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      .putExtra(BasePreferencesActivity.FRAGMENT_NAME, MacroFragment::class.java.name)
      .putExtra(BasePreferencesActivity.FRAGMENT_ARGS, Bundle().apply { putString(MacroFragment.ARG_MACRO, macroId) })

  private fun notify(context: Context, macroId: String, name: String) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val pending =
      PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        macroIntent(context, macroId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val heading = context.getString(R.string.macro_recording_notification, name)
    val text = context.getString(R.string.script_builder_recording_notification_text)
    try {
      manager.notify(NOTIFICATION_ID, NotificationUtils.createNotification(context, heading, heading, text, pending))
    } catch (e: SecurityException) {
      // Notifications aren't allowed; Stop macro, or opening the macro, still ends the recording.
    }
  }

  private const val TAG = "MacroRecorder"
  private const val NOTIFICATION_ID = 0x5f_ac71
  private const val SAME_PRESS_MS = 600L
  private const val SAME_SCROLL_MS = 1_000L
  private const val SCROLL_AFTER_TOUCH_MS = 1_500L
  /** How long after a press what it changed on screen is checked. */
  private const val CHECK_DELAY_MS = 1_200L
  /** How long after a press in the launcher an app that opens counts as opened by it. */
  private const val LAUNCH_MS = 10_000L
  private const val MAX_ACTIONS = 400
  private const val MAX_NODES = 2_000
  private const val MAX_CHECKED_LABELS = 40
  private const val MAX_LABEL_NODES = 40
  private const val MAX_LABEL_CHARS = 60
  private const val MAX_LEVELS = 30
  /** How far up from an item the item a step presses is looked for, as MacroRunner does. */
  private const val PRESS_LEVELS = 6
  /** How far up from an item a row with a switch in it is looked for. */
  private const val ROW_LEVELS = 3
}
