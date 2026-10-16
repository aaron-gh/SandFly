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
import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.os.Handler
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.os.bundleOf
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.scripting.quickjs.QuickJs
import com.google.android.accessibility.scripting.quickjs.QuickJsException
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.scripting.ScriptPermission.ACTIONS
import com.google.android.accessibility.talkback.scripting.ScriptPermission.CLIPBOARD
import com.google.android.accessibility.talkback.scripting.ScriptPermission.DIALOGS
import com.google.android.accessibility.talkback.scripting.ScriptPermission.INPUT
import com.google.android.accessibility.talkback.scripting.ScriptPermission.NETWORK
import com.google.android.accessibility.talkback.scripting.ScriptPermission.PASSWORDS
import com.google.android.accessibility.talkback.scripting.ScriptPermission.SCREEN
import com.google.android.accessibility.talkback.scripting.ScriptPermission.SPEECH
import com.google.android.accessibility.talkback.scripting.ScriptPermission.SYSTEM
import java.util.concurrent.Future
import org.json.JSONArray
import org.json.JSONObject

class ScriptRuntime(
  val script: InstalledScript,
  private val manager: ScriptManager,
  private val handler: Handler,
) : QuickJs.Host {

  private class HostCall(val permission: ScriptPermission?, val handle: (JSONObject) -> Any?)

  val id: String
    get() = script.id

  var hooks: Set<String> = emptySet()
    private set

  var commands: Set<String> = emptySet()
    private set

  private val addedRules = LinkedHashMap<Int, ScriptRule>()
  private var nextRule = 1

  val rules: List<ScriptRule>
    get() = addedRules.values.toList() + script.manifest.rules

  val isLoaded: Boolean
    get() = js?.isClosed == false

  private var js: QuickJs? = null
  private val store = manager.store
  private val dialogs = manager.dialogs
  private val nodes = NodeBridge(script.has(PASSWORDS))
  private val timers = HashMap<Int, Runnable>()
  private val fetches = HashSet<Future<*>>()
  private val warned = HashSet<String>()
  private var nextPromise = 1
  // When the script's current call runs out of time. QuickJS only stops JavaScript, so host calls
  // that walk the screen stop at this themselves.
  private var deadline = Long.MAX_VALUE
  private val calls: Map<String, HostCall> = hostCalls()

  @Throws(QuickJsException::class)
  fun load(prelude: String) {
    val runtime = QuickJs(this, MEMORY_LIMIT, STACK_LIMIT).also { js = it }
    val main =
      store.readFile(id, ScriptPackage.MAIN) ?: throw QuickJsException("main.js is missing")
    ScriptThread.busy {
      timed(LOAD_LIMIT_MS) { runtime.evalScript(prelude, "prelude.js", LOAD_LIMIT_MS) }
      timed(LOAD_LIMIT_MS) {
        runtime.evalModule(main.decodeToString(), ScriptPackage.MAIN, LOAD_LIMIT_MS)
      }
    }
  }

  fun dispatch(type: String, data: Any?, timeLimitMs: Long = CALL_LIMIT_MS): String? {
    val runtime = js?.takeUnless { it.isClosed || manager.isClosed } ?: return null
    return try {
      val event = jsonObject("type" to type, "data" to data).toString()
      ScriptThread.busy { timed(timeLimitMs) { runtime.call("__bt_dispatch", event, timeLimitMs) } }
    } catch (e: QuickJsException) {
      manager.onScriptError(this, "$type: ${e.describe()}", e.interrupted)
      null
    }
  }

  fun notify(hook: String, data: Any?, timeLimitMs: Long = CALL_LIMIT_MS): String? =
    if (hook in hooks) dispatch(hook, data, timeLimitMs) else null

  /**
   * Tells the script which app is in front. A script for all apps needs the screen permission for
   * that, or it could keep a record of the apps someone uses. A script for named apps already knows
   * where it runs.
   */
  fun notifyApp(hook: String, app: Any?, timeLimitMs: Long = CALL_LIMIT_MS): String? =
    if (hook in hooks && seesApps(handlerName(hook))) dispatch(hook, app, timeLimitMs) else null

  private fun seesApps(user: String): Boolean = !script.manifest.isGlobal || allowed(SCREEN, user)

  private inline fun <T> timed(timeLimitMs: Long, block: () -> T): T {
    val outer = deadline
    deadline = minOf(outer, SystemClock.uptimeMillis() + timeLimitMs)
    try {
      return block()
    } finally {
      deadline = outer
    }
  }

  fun close() {
    dialogs.close(id)
    timers.values.forEach(handler::removeCallbacks)
    timers.clear()
    fetches.forEach { it.cancel(true) }
    fetches.clear()
    js?.close()
    js = null
  }

  fun appliesTo(packageName: String?): Boolean = script.manifest.runsIn(packageName)

  fun allowed(permission: ScriptPermission, user: String): Boolean =
    script.has(permission).also {
      if (!it) warnOnce(user, "$user needs the ${permission.key} permission, so it is not used")
    }

  fun textOf(node: AccessibilityNodeInfoCompat): String? = nodes.textOf(node)

  fun hidesPassword(password: Boolean): Boolean = password && !script.has(PASSWORDS)

  fun snapshotIfAllowed(node: AccessibilityNodeInfoCompat?): Any =
    if (script.has(SCREEN)) nodes.snapshot(node) else JSONObject.NULL

  fun warnOnce(key: String, message: String) {
    if (warned.add(key)) {
      ScriptLog.add(id, ScriptLog.Level.WARN, message)
    }
  }

  override fun loadModule(name: String): ByteArray? = store.readFile(id, name)

  override fun onUnhandledError(error: QuickJsException) {
    manager.onScriptError(this, error.describe(), interrupted = false)
  }

  override fun call(method: String, json: String?): String? {
    val call = calls[method] ?: apiError("Backtalk has no $method")
    call.permission?.let(::requirePermission)
    // Otherwise a script could keep Backtalk's settings from being used, or turn its own
    // permissions back on there.
    apiCheck(call.permission !in REFUSED_IN_BACKTALK || !manager.inBacktalk) {
      "Scripts can't use ${call.permission?.key} on Backtalk's own screens"
    }
    val result = call.handle(json?.let { JSONObject(it) } ?: JSONObject())
    return result.takeUnless { it == Unit }?.let(::jsonOf)
  }

  private fun hostCalls(): Map<String, HostCall> = buildMap {
    fun on(name: String, permission: ScriptPermission? = null, handle: (JSONObject) -> Any?) {
      put(name, HostCall(permission, handle))
    }
    on("log") { ScriptLog.add(id, logLevel(it.optString("level")), it.optString("message")) }
    on("error") {
      val message = "${it.optString("where")}: ${it.optString("message")}"
      manager.onScriptError(this@ScriptRuntime, message, interrupted = false)
    }
    on("hooks") {
      hooks = it.optJSONArray("names").strings().toSet()
      commands = it.optJSONArray("commands").strings().toSet()
      manager.onHooksChanged()
    }
    on("manifest") { JSONObject(script.manifest.json) }
    on("apiVersion") { ScriptManifest.API_VERSION }
    on("locale") { ScriptTranslations.languageOf(manager.service).toLanguageTag() }
    on("i18n") {
      val translations = store.translations(id)
      jsonObject("language" to translations.language, "messages" to translations.messages)
    }
    on("app") {
      if (script.manifest.isGlobal) requirePermission(SCREEN)
      manager.appInfo()
    }
    on("speak", SPEECH) {
      val interrupt = it.optBoolean("interrupt") && !manager.inBacktalk
      manager.feedback.speak(it.optString("text"), interrupt, voice(it))
    }
    on("audio") { playAudio(it) }
    on("sound") { args ->
      val name = args.optString("name")
      SOUNDS[name]?.let { return@on resolved(manager.feedback.playSound(it)) }
      val file =
        store.file(id, name)
          ?: apiError(
            "Unknown sound $name. Use a file in the script, such as sounds/chime.wav, or one of " +
              SOUNDS.keys.joinToString()
          )
      promise { settle -> manager.feedback.playFile(file) { settle(true, it) } }
    }
    on("vibrate") { manager.feedback.vibrate(vibrationPattern(it.optJSONArray("pattern"))) }
    on("resume", SYSTEM) { manager.feedback.resume() }
    on("storage.get") { args ->
      store.storageGet(id, key(args))?.let { jsonObject("value" to parseJson(it)) }
    }
    on("storage.set") { args ->
      val value = if (args.has("value")) jsonOf(args.get("value")) else null
      apiCheck(store.storageSet(id, key(args), value)) { "Storage is full" }
    }
    on("storage.remove") { store.storageSet(id, key(it), null) }
    on("storage.keys") { JSONArray(store.storageKeys(id)) }
    on("storage.clear") { store.storageClear(id) }
    on("settings.get") { args ->
      val key = key(args)
      parseJson(store.settingJson(script, key) ?: apiError("The manifest declares no setting $key"))
    }
    on("settings.all") { store.settingsJson(script) }
    on("settings.set") { args ->
      val declared = setting(key(args)) { it.type.hasValue }
      val value = jsonOf(args.opt("value"))
      apiCheck(value.length <= MAX_SETTING_CHARS) {
        "A setting holds at most $MAX_SETTING_CHARS characters. Keep larger data in storage."
      }
      store.setSetting(id, declared.key, value)
    }
    on("settings.setOptions") { args ->
      val declared = setting(key(args)) { it.type == ScriptSetting.Type.LIST }
      val options = args.optJSONArray("options").options()
      apiCheck(options.size in 1..MAX_OPTIONS) { "A list has 1 to $MAX_OPTIONS options" }
      apiCheck(options.sumOf { it.first.length + it.second.length } <= MAX_SETTING_CHARS) {
        "The options of a list hold at most $MAX_SETTING_CHARS characters"
      }
      store.setSettingOptions(id, declared.key, options)
    }
    on("rules.add", SPEECH) { args ->
      apiCheck(addedRules.size < MAX_ADDED_RULES) {
        "A script can add at most $MAX_ADDED_RULES rules"
      }
      val json = args.optJSONObject("rule") ?: JSONObject()
      val rule = asApiError { ManifestParser(strict = true).rule(json) }
      nextRule++.also {
        addedRules[it] = rule
        manager.onRulesChanged()
      }
    }
    on("rules.remove", SPEECH) { changeRules(addedRules.remove(it.optInt("id")) != null) }
    on("rules.clear", SPEECH) { changeRules(addedRules.isNotEmpty().also { addedRules.clear() }) }
    on("bindings.add", INPUT) {
      val (command, binding) = commandBinding(it)
      store.setBinding(id, command, binding, present = true)
    }
    on("bindings.remove", INPUT) {
      val (command, binding) = commandBinding(it)
      store.setBinding(id, command, binding, present = false)
    }
    on("screen.focused", SCREEN) { nodes.snapshot(manager.focusedNode()) }
    on("screen.root", SCREEN) { nodes.snapshot(manager.activeRoot()) }
    on("screen.find", SCREEN) { args ->
      val root = if (args.has("root")) nodes.node(args.getInt("root")) else manager.activeRoot()
      val query = args.optJSONObject("query") ?: JSONObject()
      root?.let { nodes.find(it, query, args.optInt("limit", 1).coerceIn(1, MAX_FIND), deadline) }
        ?: JSONArray()
    }
    on("node.parent", SCREEN) { nodes.snapshot(node(it).parent) }
    on("node.children", SCREEN) { nodes.children(handle(it)) }
    on("node.refresh", SCREEN) { args -> nodes.snapshot(node(args).takeIf { it.refresh() }) }
    on("node.action", ACTIONS) { args ->
      resolved(performAction(node(args), args.opt("action"), args.optJSONObject("args")))
    }
    on("node.focus", ACTIONS) { args ->
      val node = node(args)
      promise { settle -> manager.feedback.focus(node) { settle(true, it) } }
    }
    on("fetch", NETWORK) { args -> fetch(args) }
    on("clipboard.get", CLIPBOARD) {
      manager.service
        .getSystemService(ClipboardManager::class.java)
        ?.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(manager.service)
        ?.toString() ?: JSONObject.NULL
    }
    on("clipboard.set", CLIPBOARD) {
      apiCheck(manager.service.copyToClipboard(script.manifest.name, it.optString("text"))) {
        "There is no clipboard"
      }
    }
    on("system.action", SYSTEM) { args ->
      val name = args.optString("name")
      val action =
        GLOBAL_ACTIONS[name]
          ?: apiError("Unknown system action $name. Actions: ${GLOBAL_ACTIONS.keys.joinToString()}")
      resolved(manager.service.performGlobalAction(action))
    }
    on("system.openApp", SYSTEM) { args ->
      val packageName =
        args.optString("package").ifEmpty { apiError("openApp needs a package name") }
      promise { settle -> manager.feedback.openApp(packageName) { settle(true, it) } }
    }
    on("ui.choose", DIALOGS) { args ->
      val options = args.optJSONArray("options").options()
      apiCheck(options.size in 1..MAX_OPTIONS) { "choose takes 1 to $MAX_OPTIONS options" }
      val selected = args.optString("selected").ifEmpty { null }
      promise { settle -> dialogs.choose(id, title(args), options, selected) { settle(true, it) } }
    }
    on("ui.prompt", DIALOGS) { args ->
      val text = args.optString("text")
      val hint = args.optString("hint")
      promise { settle -> dialogs.prompt(id, title(args), text, hint) { settle(true, it) } }
    }
    on("ui.confirm", DIALOGS) { args ->
      val message = args.optString("message")
      val ok = args.optString("ok").ifEmpty { null }
      val cancel = args.optString("cancel").ifEmpty { null }
      promise { settle ->
        dialogs.confirm(id, title(args), message, ok, cancel) { settle(true, it) }
      }
    }
    on("ui.alert", DIALOGS) { args ->
      val message = args.optString("message")
      promise { settle -> dialogs.alert(id, title(args), message) { settle(true, null) } }
    }
    on("ui.dialog", DIALOGS) { args ->
      val items = args.optJSONArray("items")
      apiCheck(items.items().size in 1..MAX_DIALOG_ITEMS) {
        "A dialog has 1 to $MAX_DIALOG_ITEMS items"
      }
      ScriptForm.check(items)
      dialogs.form(id, args.getInt("dialog"), title(args), items) { event ->
        handler.post { dispatch("dialogEvent", event) }
      }
    }
    on("ui.update", DIALOGS) { args ->
      val props = args.optJSONObject("props") ?: JSONObject()
      apiCheck(props.optJSONArray("items").items().size <= MAX_OPTIONS) {
        "A list has at most $MAX_OPTIONS items"
      }
      dialogs.update(id, args.getInt("dialog"), args.optString("item"), props)
    }
    on("ui.close", DIALOGS) { dialogs.close(id, it.getInt("dialog")) }
    on("timer.set") { setTimer(it.getInt("id"), it.optLong("ms", 0L)) }
    on("timer.clear") { timers.remove(it.getInt("id"))?.let(handler::removeCallbacks) }
  }

  // Every dialog starts with the script's name. A dialog is drawn over whatever app is in front,
  // and someone who can't see it has only the title to tell it from the app's own dialogs.
  private fun title(args: JSONObject): String {
    val name = script.manifest.name
    val title = args.optString("title")
    return if (title.isEmpty() || title == name) {
      name
    } else {
      manager.service.getString(R.string.script_dialog_title, name, title)
    }
  }

  private fun changeRules(changed: Boolean) {
    if (changed) manager.onRulesChanged()
  }

  private fun voice(args: JSONObject): ScriptFeedback.Voice {
    fun scale(key: String) =
      args.optDouble(key, 1.0).toFloat().coerceIn(MIN_SPEECH_SCALE, MAX_SPEECH_SCALE)
    val language = args.optString("language").ifEmpty { null }
    return ScriptFeedback.Voice(scale("rate"), scale("pitch"), language)
  }

  private fun playAudio(args: JSONObject) {
    val sampleRate = args.optInt("sampleRate", DEFAULT_SAMPLE_RATE)
    apiCheck(sampleRate in MIN_SAMPLE_RATE..MAX_SAMPLE_RATE) {
      "The sample rate is $MIN_SAMPLE_RATE to $MAX_SAMPLE_RATE"
    }
    val samples = args.optJSONArray("samples").items()
    apiCheck(samples.size in 1..sampleRate * MAX_AUDIO_SECONDS) {
      "A sound has samples for at most $MAX_AUDIO_SECONDS seconds"
    }
    manager.feedback.playAudio(
      FloatArray(samples.size) { (samples[it] as? Number)?.toFloat() ?: 0f },
      sampleRate,
      args.optDouble("volume", 1.0).toFloat().coerceIn(0f, 1f),
    )
  }

  private fun setting(key: String, accepts: (ScriptSetting) -> Boolean): ScriptSetting =
    script.manifest.settings.firstOrNull { it.key == key && accepts(it) }
      ?: apiError("The manifest declares no such setting $key")

  private fun commandBinding(args: JSONObject): Pair<ScriptCommand, CommandBinding> {
    val commandId = args.optString("command")
    val command =
      script.manifest.commands.firstOrNull { it.id == commandId }
        ?: apiError("The manifest declares no command $commandId")
    val gesture = args.optString("gesture")
    val keys = args.optString("keys")
    val binding =
      when {
        ScriptInput.isGesture(gesture) -> CommandBinding.Gesture(gesture)
        gesture.isNotEmpty() -> apiError("Unknown gesture $gesture")
        keys.isNotEmpty() -> CommandBinding.KeyCombo(asApiError { ScriptInput.parseKeys(keys) })
        else -> apiError("A binding needs gesture or keys")
      }
    return command to binding
  }

  private fun fetch(args: JSONObject): JSONObject {
    apiCheck(fetches.size < MAX_FETCHES) {
      "A script can make at most $MAX_FETCHES requests at once"
    }
    val request = ScriptFetch.request(args)
    return promise { settle ->
      lateinit var future: Future<*>
      future =
        ScriptFetch.start(request) { ok, value ->
          settle(ok, value)
          handler.post { fetches.remove(future) }
        }
      fetches.add(future)
    }
  }

  private fun promise(start: ((Boolean, Any?) -> Unit) -> Unit): JSONObject {
    val promiseId = nextPromise++
    start { ok, value ->
      handler.post {
        val outcome =
          if (ok) "value" to value else "error" to (value as? String ?: "Backtalk could not do it")
        dispatch("settle", jsonObject("id" to promiseId, "ok" to ok, outcome))
      }
    }
    return jsonObject("promise" to promiseId)
  }

  private fun resolved(value: Any?): JSONObject = promise { settle -> settle(true, value) }

  private fun setTimer(timerId: Int, delayMs: Long) {
    timers.remove(timerId)?.let(handler::removeCallbacks)
    apiCheck(timers.size < MAX_TIMERS) { "A script can have at most $MAX_TIMERS timers at once" }
    val runnable = Runnable {
      if (timers.remove(timerId) != null) {
        dispatch("timer", jsonObject("id" to timerId))
      }
    }
    timers[timerId] = runnable
    handler.postDelayed(runnable, delayMs.coerceAtLeast(0L))
  }

  private fun performAction(node: AccessibilityNodeInfoCompat, action: Any?, args: JSONObject?) =
    if (action == "setText") {
      node.performAction(
        AccessibilityNodeInfo.ACTION_SET_TEXT,
        bundleOf(
          AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE to
            args?.optString("text").orEmpty()
        ),
      )
    } else {
      node.performAction(NodeBridge.actionId(node, action))
    }

  private fun requirePermission(permission: ScriptPermission) =
    apiCheck(script.has(permission)) {
      "This script needs the ${permission.key} permission. Add it to the manifest's " +
        "permissions, and turn it on on the script's page."
    }

  private fun key(args: JSONObject): String =
    args.optString("key").takeIf { it.length in 1..MAX_KEY_LENGTH }
      ?: apiError("Keys must be 1 to $MAX_KEY_LENGTH characters")

  private fun handle(args: JSONObject): Int =
    if (args.has("handle")) args.getInt("handle") else apiError("Not an item")

  private fun node(args: JSONObject): AccessibilityNodeInfoCompat = nodes.node(handle(args))

  private fun vibrationPattern(array: JSONArray?): LongArray {
    val pattern =
      array.items().map { (it as? Number)?.toLong()?.coerceIn(0L, MAX_VIBRATION_PART_MS) ?: 0L }
    apiCheck(pattern.size in 1..MAX_PATTERN) {
      "A vibration is 1 to $MAX_PATTERN durations in milliseconds"
    }
    apiCheck(pattern.sum() <= MAX_VIBRATION_MS) { "A vibration lasts at most 5 seconds" }
    return pattern.toLongArray()
  }

  private fun logLevel(level: String): ScriptLog.Level =
    ScriptLog.Level.entries.firstOrNull { it.name.equals(level, ignoreCase = true) }
      ?: ScriptLog.Level.INFO

  companion object {
    private val REFUSED_IN_BACKTALK = setOf(SCREEN, ACTIONS, SYSTEM, DIALOGS)
    const val LOAD_LIMIT_MS = 2000L
    const val CALL_LIMIT_MS = 2000L
    private const val MEMORY_LIMIT = 32L * 1024 * 1024
    private const val STACK_LIMIT = 512L * 1024
    private const val MAX_FIND = 200
    private const val MAX_KEY_LENGTH = 200
    private const val MAX_PATTERN = 20
    private const val MAX_VIBRATION_PART_MS = 2000L
    private const val MAX_VIBRATION_MS = 5000L
    private const val MAX_FETCHES = 8
    private const val MAX_TIMERS = 100
    private const val MAX_SETTING_CHARS = 16 * 1024
    private const val MAX_ADDED_RULES = 200
    private const val MAX_OPTIONS = 500
    private const val MAX_DIALOG_ITEMS = 50
    private const val MAX_AUDIO_SECONDS = 10
    private const val DEFAULT_SAMPLE_RATE = 22050
    private const val MIN_SAMPLE_RATE = 8000
    private const val MAX_SAMPLE_RATE = 48000
    private const val MIN_SPEECH_SCALE = 0.25f
    private const val MAX_SPEECH_SCALE = 4f

    private val SOUNDS: Map<String, Int> =
      mapOf(
        "focus" to R.raw.focus,
        "actionable" to R.raw.focus_actionable,
        "click" to R.raw.tick,
        "longClick" to R.raw.long_clicked,
        "scroll" to R.raw.scroll_tone,
        "listEnter" to R.raw.chime_up,
        "listExit" to R.raw.chime_down,
        "end" to R.raw.complete,
        "enter" to R.raw.view_entered,
        "typo" to R.raw.typo,
        "beep" to R.raw.volume_beep,
      )

    @SuppressLint("InlinedApi")
    private val GLOBAL_ACTIONS: Map<String, Int> =
      mapOf(
        "back" to AccessibilityService.GLOBAL_ACTION_BACK,
        "home" to AccessibilityService.GLOBAL_ACTION_HOME,
        "recents" to AccessibilityService.GLOBAL_ACTION_RECENTS,
        "notifications" to AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS,
        "quickSettings" to AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS,
        "powerDialog" to AccessibilityService.GLOBAL_ACTION_POWER_DIALOG,
        "lockScreen" to AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN,
        "takeScreenshot" to AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT,
        "allApps" to AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS,
      )

    fun handlerName(hook: String): String = "on" + hook.replaceFirstChar { it.uppercase() }
  }
}
