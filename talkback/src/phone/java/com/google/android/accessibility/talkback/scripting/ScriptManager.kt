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

package com.google.android.accessibility.talkback.scripting

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.focusmanagement.TraversalTreeCache
import com.google.android.accessibility.talkback.translation.Translator
import com.google.android.accessibility.utils.Role
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

class ScriptManager(
  val service: AccessibilityService,
  val feedback: ScriptFeedback,
  private val activation: ScriptActivation,
) : ScriptHost, ScriptStore.Listener {

  val store: ScriptStore = ScriptStore.get(service)
  val dialogs = ScriptDialogs(service)
  val device = ScriptDevice(service)
  val touch = ScriptTouch(service, feedback)
  val screen = ScriptScreen(service)
  /** Used on the main thread only. */
  private val translatorLazy = lazy { Translator(service) }
  val translator by translatorLazy

  private val mainHandler = Handler(Looper.getMainLooper())
  private val scriptHandler = startScriptThread()
  private val loaded = LinkedHashMap<String, ScriptRuntime>()
  private val speechHooks = ScriptSpeechHooks(scriptHandler) {
    !closed && speechStateVersion == appliedSpeechStateVersion
  }
  private val events = ScriptEventDelivery(scriptHandler) { loaded.values }
  private val actions = ScriptItemActions(scriptHandler, feedback)
  private val navigator = ScriptNavigator(this)
  private val errors = ErrorCounter()
  private val compatibilityWarned = HashSet<String>()
  private val evaluateRunnable = Runnable { evaluate() }
  private val prelude by lazy {
    service.assets.open(PRELUDE_ASSET).use { it.readBytes().decodeToString() }
  }
  private var layer: ScriptInput.Keys? = null
  /**
   * The keys without the SandFly modifier that a script last took, and the command their key
   * repeats run, if any.
   */
  private var takenPlainKeys: Pair<ScriptInput.Keys, BoundCommand?>? = null
  private var layerTime = 0L
  private var lastApp: JSONObject? = null
  private var rulesSource: List<Pair<InstalledScript, List<ScriptRule>>> = emptyList()

  @Volatile private var closed = false
  // A priority hook must not jump ahead of an app change or pending script/permission update.
  @Volatile private var speechStateVersion = 0L
  @Volatile private var appliedSpeechStateVersion = 0L
  @Volatile private var rules = ScriptRules.EMPTY
  @Volatile private var bindings = ScriptBindings.EMPTY

  init {
    store.addListener(this)
    evaluate()
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent) {
    when (event.eventType) {
      AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
        activation.onWindowStateChanged(event)
        evaluate()
        val info = activation.appInfo
        onScriptThread { loaded.values.toList().forEach { it.notifyApp("windowChange", info) } }
      }
      AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
        mainHandler.removeCallbacks(evaluateRunnable)
        mainHandler.postDelayed(evaluateRunnable, WINDOWS_DEBOUNCE_MS)
      }
      AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED ->
        if (activation.onFocusMoved(event.packageName?.toString())) {
          evaluate()
        }
      // A list reuses its row views, so what an item is inside changes without the item changing.
      AccessibilityEvent.TYPE_VIEW_SCROLLED,
      AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->
        if (rules.dependsOnAncestors) {
          rules.clearCache()
        }
    }
    if (event.packageName?.toString() != service.packageName) {
      events.deliver(event)
    }
  }

  override fun rewriteFocusSpeech(
    node: AccessibilityNodeInfoCompat,
    speech: CharSequence,
  ): CharSequence? {
    // Scripts leave Backtalk's own screens alone, so its settings can always be used to turn a
    // script off.
    if (node.packageName?.toString() == service.packageName) {
      return null
    }
    val rule = rules.resolve(node, RuleAspect.SPEAK)
    if (rule?.hide == true) {
      return ""
    }
    if (!speechHooks.hasListeners("focus")) {
      return rule?.speak
    }
    @Suppress("DEPRECATION") val copy = AccessibilityNodeInfoCompat.obtain(node)
    return speechHooks.rewrite("focus", (rule?.speak ?: speech).toString()) {
      it.snapshotIfAllowed(copy)
    } ?: rule?.speak
  }

  override fun rewriteEventSpeech(event: AccessibilityEvent, speech: CharSequence): CharSequence? {
    if (activation.inBacktalk) {
      return null
    }
    val packageName = event.packageName?.toString()
    return when (event.eventType) {
      AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
        val notification = event.notificationJson()
        speechHooks.rewrite("notification", speech.toString()) { notification }
      }
      AccessibilityEvent.TYPE_ANNOUNCEMENT -> {
        val announcement = jsonObject("package" to packageName, "text" to event.scriptText())
        speechHooks.rewrite("announcement", speech.toString(), { it.appliesTo(packageName) }) {
          announcement
        }
      }
      else -> null
    }
  }

  override fun filterSpeech(speech: CharSequence): CharSequence? =
    if (!activation.inBacktalk && speechHooks.hasListeners("speech")) {
      speechHooks.rewrite("speech", speech.toString()) { JSONObject.NULL }
    } else {
      null
    }

  override fun rule(node: AccessibilityNodeInfoCompat, aspect: RuleAspect): NodeRule? =
    rules.resolve(node, aspect)

  override fun isHidden(node: AccessibilityNodeInfoCompat): Boolean = rules.isHidden(node)

  override fun isSilenced(node: AccessibilityNodeInfoCompat): Boolean = rules.isSilenced(node)

  override fun currentActivity(): String? = activation.activity

  override fun currentWindowTitle(): String? = activation.windowTitle

  override fun onPausedChanged(paused: Boolean) {
    feedback.paused = paused
  }

  override fun onGesture(name: String, fallback: Runnable): Boolean =
    !activation.inBacktalk &&
      bindings.gestures[name]?.also { runCommand(it, "gesture", fallback = fallback) } != null

  override fun onKeys(
    modifiers: Int,
    keyCode: Int,
    withBacktalkModifier: Boolean,
    repeated: Boolean,
    fallback: Runnable,
  ): Boolean {
    if (activation.inBacktalk) {
      return false
    }
    val pressed = ScriptInput.Keys(modifiers, keyCode)
    val plain = pressed.copy(plain = true)
    // A held key's repeats go where its press went: to the script's command, or on to the app.
    if (repeated && !withBacktalkModifier) {
      val taken = takenPlainKeys?.takeIf { it.first == plain } ?: return false
      taken.second?.let { runCommand(it, "keys") }
      return true
    }
    val prefix = layer?.takeIf { SystemClock.uptimeMillis() - layerTime < LAYER_TIMEOUT_MS }
    if (KeyEvent.isModifierKey(keyCode)) {
      if (prefix != null) {
        return false
      }
    } else {
      takenPlainKeys = null
    }
    layer = null
    val current = bindings
    if (prefix != null) {
      current.keys[prefix.copy(next = pressed)]?.let { runCommand(it, "keys") }
        ?: feedback.playSound(R.raw.complete)
      takenPlainKeys = (plain to null).takeUnless { withBacktalkModifier }
      return true
    }
    val keys = if (withBacktalkModifier) pressed else plain
    val bound = current.keys[keys]
    val isLayer = keys in current.layers
    // Typing in an edit field always reaches the field.
    if ((bound == null && !isLayer) || (!withBacktalkModifier && isTyping())) {
      return false
    }
    if (bound != null) {
      if (withBacktalkModifier) {
        runCommand(bound, "keys", fallback = fallback)
        return true
      }
      return runForPlainKeys(bound).also { if (it) takenPlainKeys = plain to bound }
    }
    layer = keys
    layerTime = SystemClock.uptimeMillis()
    feedback.playSound(R.raw.tick)
    takenPlainKeys = (plain to null).takeUnless { withBacktalkModifier }
    return true
  }

  /**
   * Runs a command for keys pressed without the SandFly modifier, and returns whether to take the
   * keys from the app. The app gets them if the command returns false before it waits for anything,
   * within [KEY_WAIT_MS]. A key event can't be given back once taken, so the key filter waits.
   */
  private fun runForPlainKeys(bound: BoundCommand): Boolean {
    // A command, timer or reply that holds up the script thread would hold up the key too.
    if (ScriptThread.heldUpFor(KEY_WAIT_MS)) {
      runCommand(bound, "keys")
      return true
    }
    val answered = CountDownLatch(1)
    val handsBack = AtomicBoolean(false)
    runCommand(bound, "keys", atFrontOfQueue = true) {
      handsBack.set(it)
      answered.countDown()
    }
    return try {
      !(answered.await(KEY_WAIT_MS, TimeUnit.MILLISECONDS) && handsBack.get())
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
      true
    }
  }

  /** Whether an edit field has input focus, so that keys type into it. */
  private fun isTyping(): Boolean {
    val node = inputFocusedNode() ?: return false
    return node.isFocused && (node.isEditable || Role.getRole(node) == Role.ROLE_EDIT_TEXT)
  }

  override fun menuItems(): List<ScriptMenuItem> =
    bindings.menu.map { bound ->
      ScriptMenuItem(bound.command.title) { node -> runCommand(bound, "menu", node = node) }
    }

  override fun readingControls(): List<ScriptReadingControl> =
    bindings.controls.map { bound ->
      val key = "${bound.runtime.id}/${bound.command.id}"
      ScriptReadingControl(key, bound.command.title) { isNext ->
        runCommand(bound, "control", direction = if (isNext) "next" else "previous")
      }
    } +
      bindings.navigation.map { bound ->
        val key = "${bound.runtime.id}/navigation/${bound.index}"
        ScriptReadingControl(key, bound.navigation.title) { isNext ->
          // The search visits every item on screen, so it stays off the main thread.
          onScriptThread {
            ScriptThread.busy(bound.runtime.id) {
              navigator.move(bound.runtime, bound.navigation.query, isNext)
            }
          }
        }
      }

  override fun itemActions(node: AccessibilityNodeInfoCompat): List<ScriptItemAction> =
    if (activation.inBacktalk) emptyList() else actions.of(node, rules)

  override fun toggleAll(): Boolean {
    store.allOff = !store.allOff
    return !store.allOff
  }

  /** Whether [thread] is this manager's script thread. */
  fun runsOn(thread: Thread): Boolean = scriptHandler.looper.thread === thread

  override fun shutdown() {
    closed = true
    // It holds a thread and, once used, the on-device translation models.
    if (translatorLazy.isInitialized()) translator.shutdown()
    store.removeListener(this)
    mainHandler.removeCallbacks(evaluateRunnable)
    // What scripts still had queued is dropped, so that turning scripts off stops them now.
    scriptHandler.removeCallbacksAndMessages(null)
    scriptHandler.postAtFrontOfQueue {
      loaded.values.forEach(ScriptRuntime::close)
      loaded.clear()
      speechHooks.clear()
      events.clear()
      actions.clear()
      dialogs.close()
      screen.close()
      bindings = ScriptBindings.EMPTY
      rules = ScriptRules.EMPTY
      rulesSource = emptyList()
      Looper.myLooper()?.quitSafely()
    }
  }

  override fun onScriptsChanged() = evaluate()

  override fun onSettingChanged(id: String, key: String) = onScriptThread {
    loaded[id]?.let {
      val value = parseJson(store.settingJson(it.script, key))
      it.notify("settingChange", jsonObject("key" to key, "value" to value))
    }
  }

  override fun onButtonPressed(id: String, key: String) = onScriptThread {
    loaded[id]?.notify("button", jsonObject("key" to key))
  }

  fun appInfo(): JSONObject = activation.appInfo

  val isClosed: Boolean
    get() = closed

  /** Whether a Backtalk screen is in front, where scripts can't read, act or show anything. */
  val inBacktalk: Boolean
    get() = activation.inBacktalk

  fun focusedNode(): AccessibilityNodeInfoCompat? =
    service
      .findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
      ?.let(AccessibilityNodeInfoCompat::wrap)

  fun inputFocusedNode(): AccessibilityNodeInfoCompat? =
    service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let(AccessibilityNodeInfoCompat::wrap)

  fun activeRoot(): AccessibilityNodeInfoCompat? =
    service.rootInActiveWindow?.let(AccessibilityNodeInfoCompat::wrap)

  fun onHooksChanged() {
    val runtimes = loaded.values.filter { it.isLoaded }
    speechHooks.update(runtimes)
    events.update(runtimes)
    actions.update(runtimes)
    bindings = ScriptBindings.of(runtimes, store)
  }

  fun onScriptError(runtime: ScriptRuntime, message: String, interrupted: Boolean) {
    val id = runtime.id
    ScriptLog.add(id, ScriptLog.Level.ERROR, message)
    // Calls are refused on Backtalk's own screens, which is no fault of the script.
    if (activation.inBacktalk && !interrupted) {
      return
    }
    if (!errors.tooMany(id, interrupted) || store.find(id)?.enabled != true) {
      return
    }
    errors.reset(id)
    ScriptLog.add(id, ScriptLog.Level.ERROR, "Turned off after repeated errors")
    store.setEnabled(id, false)
    val name = runtime.script.manifest.name
    feedback.speak(service.getString(R.string.script_turned_off, name), interrupt = false)
  }

  private fun evaluate() {
    mainHandler.removeCallbacks(evaluateRunnable)
    // A change to the script list can still be on its way here after the engine was stopped.
    if (closed) {
      return
    }
    activation.refresh()
    rules.clearCache()
    val info = activation.appInfo
    val wanted = store.all().filter(activation::wants)
    val version = ++speechStateVersion
    onScriptThread {
      apply(wanted, info)
      appliedSpeechStateVersion = version
    }
  }

  private fun apply(wanted: List<InstalledScript>, info: JSONObject) {
    val previousApp = lastApp
    lastApp = info
    loaded.values
      .filter { runtime -> wanted.none { it == runtime.script } }
      .forEach { unload(it, previousApp ?: info) }
    if (previousApp != null && previousApp.optString("package") != info.optString("package")) {
      loaded.values
        .filter { it.script.manifest.isGlobal }
        .forEach {
          it.notifyApp("appLeave", previousApp)
          it.notifyApp("appEnter", info)
        }
    }
    wanted.filter { it.id !in loaded }.forEach { load(it, info) }
    val order = wanted.map { it.id }
    val sorted =
      loaded.values.sortedWith(
        compareBy<ScriptRuntime>({ it.script.manifest.isGlobal }, { order.indexOf(it.id) })
      )
    loaded.clear()
    sorted.forEach { loaded[it.id] = it }
    onHooksChanged()
    updateRules()
  }

  private fun load(script: InstalledScript, info: JSONObject) {
    val runtime = ScriptRuntime(script, this, scriptHandler)
    try {
      runtime.load(prelude)
    } catch (e: LuaEngine.ScriptError) {
      return failLoading(runtime, e.message.orEmpty(), e.interrupted)
    } catch (e: RuntimeException) {
      return failLoading(runtime, e.toString(), interrupted = false)
    } catch (e: LinkageError) {
      return failLoading(runtime, e.toString(), interrupted = false)
    }
    loaded[script.id] = runtime
    ScriptLog.add(script.id, ScriptLog.Level.DEBUG, "Loaded for ${info.optString("package")}")
    if (compatibilityWarned.add("${script.id}:${script.revision}")) {
      script.manifest.compatibilityWarning(service)?.let {
        ScriptLog.add(script.id, ScriptLog.Level.WARN, it)
      }
    }
    runtime.notifyApp("appEnter", info)
  }

  private fun failLoading(runtime: ScriptRuntime, message: String, interrupted: Boolean) {
    runtime.close()
    onScriptError(runtime, "Loading: $message", interrupted)
  }

  private fun unload(runtime: ScriptRuntime, app: JSONObject) {
    if (!runtime.script.manifest.isGlobal) {
      runtime.notifyApp("appLeave", app, UNLOAD_LIMIT_MS)
    }
    runtime.close()
    loaded.remove(runtime.id)
  }

  fun onRulesChanged() = updateRules()

  private fun updateRules() {
    val source = loaded.values.map { it.script to it.rules }.filter { it.second.isNotEmpty() }
    if (source == rulesSource) {
      return
    }
    rulesSource = source
    rules =
      ScriptRules.of(loaded.values, service.packageName) {
        activation.activity to activation.windowTitle
      }
    mainHandler.post { TraversalTreeCache.clear("script rules") }
  }

  private fun runCommand(
    bound: BoundCommand,
    via: String,
    node: AccessibilityNodeInfoCompat? = null,
    direction: String? = null,
    fallback: Runnable? = null,
    atFrontOfQueue: Boolean = false,
    answered: ((handsBack: Boolean) -> Unit)? = null,
  ) {
    @Suppress("DEPRECATION") val copy = node?.let { AccessibilityNodeInfoCompat.obtain(it) }
    val run = Runnable {
      val runtime = bound.runtime
      val result =
        if (runtime.isLoaded) {
          val data =
            jsonObject(
              "id" to bound.command.id,
              "via" to via,
              "direction" to direction,
              "node" to runtime.snapshotIfAllowed(copy ?: focusedNode()),
            )
          runtime.dispatch("command", data)
        } else {
          FALLBACK
        }
      val handsBack = handsBack(result)
      answered?.invoke(handsBack)
      if (fallback != null && handsBack) {
        mainHandler.post(fallback)
      }
    }
    if (atFrontOfQueue) scriptHandler.postAtFrontOfQueue(run) else scriptHandler.post(run)
  }

  private fun handsBack(result: String?): Boolean =
    result != null && runCatching { JSONObject(result).optBoolean("fallback") }.getOrDefault(false)

  private fun onScriptThread(action: () -> Unit) {
    scriptHandler.post(action)
  }

  private class ErrorCounter {
    private val times = HashMap<Pair<String, Boolean>, ArrayDeque<Long>>()

    fun tooMany(id: String, interrupted: Boolean): Boolean {
      val (limit, window) =
        if (interrupted) MAX_INTERRUPTS to INTERRUPT_WINDOW_MS else MAX_ERRORS to ERROR_WINDOW_MS
      val now = SystemClock.uptimeMillis()
      val list = times.getOrPut(id to interrupted) { ArrayDeque() }
      list.addLast(now)
      while (now - list.first() > window) {
        list.removeFirst()
      }
      return list.size >= limit
    }

    fun reset(id: String) {
      times.keys.removeAll { it.first == id }
    }
  }

  private companion object {
    const val PRELUDE_ASSET = "scripting/prelude.lua"
    const val UNLOAD_LIMIT_MS = 250L
    const val WINDOWS_DEBOUNCE_MS = 100L
    const val LAYER_TIMEOUT_MS = 3000L
    const val KEY_WAIT_MS = 30L
    const val THREAD_STACK_BYTES = 4L * 1024 * 1024
    const val MAX_ERRORS = 5
    const val ERROR_WINDOW_MS = 60_000L
    const val MAX_INTERRUPTS = 3
    const val INTERRUPT_WINDOW_MS = 5 * 60_000L
    const val FALLBACK = "{\"fallback\":true}"

    fun startScriptThread(): Handler {
      val ready = CountDownLatch(1)
      var looper: Looper? = null
      Thread(
          null,
          {
            Looper.prepare()
            looper = Looper.myLooper()
            ready.countDown()
            Looper.loop()
          },
          "BacktalkScripts",
          THREAD_STACK_BYTES,
        )
        .apply {
          isDaemon = true
          start()
        }
      ready.await()
      return GuardedHandler(checkNotNull(looper))
    }
  }
}

/**
 * Runs the script thread's work so that an exception in it, such as from a node an app filled
 * with values SandFly doesn't expect, is logged instead of stopping the screen reader. Lua errors
 * are already handled where scripts are called.
 */
private class GuardedHandler(looper: Looper) : Handler(looper) {
  override fun dispatchMessage(msg: Message) {
    try {
      super.dispatchMessage(msg)
    } catch (e: Throwable) {
      LogUtils.e("ScriptManager", "Script thread work failed: %s", e)
    }
  }
}
