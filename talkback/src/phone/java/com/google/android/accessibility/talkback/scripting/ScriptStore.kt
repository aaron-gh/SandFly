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

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.IOException
import java.util.Locale
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class InstalledScript(
  val manifest: ScriptManifest,
  val enabled: Boolean,
  val granted: Set<ScriptPermission>,
  val revision: Long,
  val language: String = "",
) {
  val id: String
    get() = manifest.id

  fun has(permission: ScriptPermission): Boolean = permission in granted
}

class ScriptStore private constructor(private val context: Context) {

  interface Listener {
    fun onScriptsChanged() {}

    fun onSettingChanged(id: String, key: String) {}

    fun onButtonPressed(id: String, key: String) {}
  }

  private val index: SharedPreferences =
    context.getSharedPreferences(INDEX_PREFS, Context.MODE_PRIVATE)
  private val listeners = mutableListOf<Listener>()
  private val mainHandler = Handler(Looper.getMainLooper())
  private var scripts: List<InstalledScript> = load()
  private var translated: Pair<String, List<InstalledScript>>? = null

  @Synchronized
  fun all(): List<InstalledScript> {
    val locale = ScriptTranslations.languageOf(context)
    val tag = locale.toLanguageTag()
    translated?.takeIf { it.first == tag }?.let { return it.second }
    return scripts.map { translate(it, locale) }.also { translated = tag to it }
  }

  @Synchronized fun find(id: String): InstalledScript? = all().firstOrNull { it.id == id }

  @Synchronized fun anyEnabled(): Boolean = scripts.any { it.enabled }

  /** Turns every script off without changing which ones are switched on. */
  var allOff: Boolean
    get() = index.getBoolean(KEY_ALL_OFF, false)
    set(value) {
      if (value == allOff) return
      index.edit { putBoolean(KEY_ALL_OFF, value) }
      notify { it.onScriptsChanged() }
    }

  fun translations(id: String): ScriptTranslations =
    ScriptTranslations.load(ScriptTranslations.languageOf(context)) { readFile(id, it) }

  fun addListener(listener: Listener) = synchronized(listeners) { listeners.add(listener) }

  fun removeListener(listener: Listener) = synchronized(listeners) { listeners.remove(listener) }

  @Throws(IOException::class)
  fun install(pkg: ScriptPackage, granted: Set<ScriptPermission>, enabled: Boolean = true) {
    val id = pkg.manifest.id
    val staging = File(scriptsDir(), ".$id.new").apply { deleteRecursively() }
    for ((path, bytes) in pkg.files) {
      val file = File(staging, path).insideOf(staging) ?: throw IOException("Bad path $path")
      file.parentFile?.mkdirs()
      file.writeBytes(bytes)
    }
    // The old code is moved aside until the new code is in place, so a failed install leaves the
    // script as it was rather than listed with no code.
    val code = codeDir(id)
    val old = File(scriptsDir(), ".$id.old").apply { deleteRecursively() }
    val replaces = code.exists()
    if (replaces && !code.renameTo(old)) {
      staging.deleteRecursively()
      throw IOException("Could not replace $id")
    }
    if (!staging.renameTo(code)) {
      if (replaces) old.renameTo(code)
      staging.deleteRecursively()
      throw IOException("Could not install $id")
    }
    old.deleteRecursively()
    updateScripts { list ->
      val existing = list.firstOrNull { it.id == id }
      val installed =
        InstalledScript(
          pkg.manifest,
          existing?.enabled ?: enabled,
          granted intersect pkg.manifest.permissions,
          System.currentTimeMillis(),
        )
      if (existing == null) list + installed else list.map { if (it.id == id) installed else it }
    }
  }

  fun remove(id: String) {
    updateScripts { list -> list.filterNot { it.id == id } }
    codeDir(id).deleteRecursively()
    dataPrefs(id).edit { clear() }
    context.deleteSharedPreferences(dataPrefsName(id))
  }

  fun setEnabled(id: String, enabled: Boolean) = update(id) { it.copy(enabled = enabled) }

  fun setGranted(id: String, permission: ScriptPermission, granted: Boolean) =
    update(id) { script ->
      script.copy(
        granted = if (granted) script.granted + permission else script.granted - permission
      )
    }

  fun move(id: String, delta: Int) = updateScripts { list ->
    val from = list.indexOfFirst { it.id == id }
    if (from < 0) list
    else list.toMutableList().apply { add((from + delta).coerceIn(indices), removeAt(from)) }
  }

  fun file(id: String, path: String): File? =
    File(codeDir(id), path).insideOf(codeDir(id))?.takeIf { it.isFile }

  fun readFile(id: String, path: String): ByteArray? =
    file(id, path)?.let { runCatching { it.readBytes() }.getOrNull() }

  fun storageGet(id: String, key: String): String? = dataPrefs(id).getString(STORAGE + key, null)

  fun storageSet(id: String, key: String, json: String?): Boolean {
    val prefs = dataPrefs(id)
    if (json != null) {
      val used =
        prefs.entriesWith(STORAGE).entries.filter { it.key != key }.sumOf {
          it.key.length + it.value.toString().length
        }
      if (used + key.length + json.length > MAX_STORAGE_CHARS) return false
    }
    prefs.edit { if (json == null) remove(STORAGE + key) else putString(STORAGE + key, json) }
    return true
  }

  fun storageKeys(id: String): List<String> = dataPrefs(id).entriesWith(STORAGE).keys.toList()

  fun storageClear(id: String) {
    val prefs = dataPrefs(id)
    prefs.edit { prefs.entriesWith(STORAGE).keys.forEach { remove(STORAGE + it) } }
  }

  fun settingJson(script: InstalledScript, key: String): String? =
    script.manifest.settings
      .firstOrNull { it.key == key && it.type.hasValue }
      ?.let { dataPrefs(script.id).getString(SETTING + key, null) ?: it.defaultJson }

  fun settingsJson(script: InstalledScript): JSONObject =
    JSONObject().apply {
      script.manifest.settings
        .filter { it.type.hasValue }
        .forEach { put(it.key, parseJson(settingJson(script, it.key))) }
    }

  fun setSetting(id: String, key: String, json: String) {
    dataPrefs(id).edit { putString(SETTING + key, json) }
    notify { it.onSettingChanged(id, key) }
  }

  fun pressButton(id: String, key: String) = notify { it.onButtonPressed(id, key) }

  fun settingOptions(script: InstalledScript, setting: ScriptSetting): List<Pair<String, String>> =
    dataPrefs(script.id).getString(OPTIONS + setting.key, null)?.let { JSONArray(it).options() }
      ?: setting.options

  fun setSettingOptions(id: String, key: String, options: List<Pair<String, String>>) {
    val json =
      JSONArray(options.map { (value, label) -> jsonObject("value" to value, "label" to label) })
    changeData(id) { putString(OPTIONS + key, json.toString()) }
  }

  fun bindingsOf(id: String, command: ScriptCommand): List<CommandBinding> {
    val removed = keys(id, REMOVED + command.id)
    val added = keys(id, BINDINGS + command.id).mapNotNull(CommandBinding::fromKey)
    return (command.bindings.filterNot { it.key in removed } + added).distinctBy { it.key }
  }

  fun setBinding(id: String, command: ScriptCommand, binding: CommandBinding, present: Boolean) {
    if (present) dataPrefs(id).edit { remove(bindingKey(command.id, binding)) }
    val default = command.isDefault(binding)
    val list = if (default) REMOVED else BINDINGS
    editKeys(id, list + command.id) {
      if (present != default) it + binding.key else it - binding.key
    }
  }

  fun isBindingOn(id: String, command: String, binding: CommandBinding): Boolean =
    !dataPrefs(id).getBoolean(bindingKey(command, binding), false)

  fun setBindingOn(id: String, command: String, binding: CommandBinding, on: Boolean) =
    changeData(id) { putBoolean(bindingKey(command, binding), !on) }

  fun hasChangedDefaults(id: String, command: ScriptCommand): Boolean =
    keys(id, REMOVED + command.id).isNotEmpty() ||
      command.bindings.any { !isBindingOn(id, command.id, it) }

  fun restoreDefaults(id: String, command: ScriptCommand) = changeData(id) {
    remove(REMOVED + command.id)
    command.bindings.forEach { remove(bindingKey(command.id, it)) }
  }

  /** The example script that comes with Backtalk, for the Scripts screen to offer. */
  @Throws(IOException::class)
  fun exampleBytes(): ByteArray = context.assets.open(EXAMPLE_ASSET).use { it.readBytes() }

  private fun keys(id: String, key: String): List<String> =
    dataPrefs(id).getString(key, null)?.let { JSONArray(it).strings() }.orEmpty()

  private fun editKeys(id: String, key: String, change: (List<String>) -> List<String>) {
    val json = JSONArray(change(keys(id, key)).distinct())
    changeData(id) { putString(key, json.toString()) }
  }

  private fun changeData(id: String, change: SharedPreferences.Editor.() -> Unit) {
    dataPrefs(id).edit(action = change)
    notify { it.onScriptsChanged() }
  }

  private fun update(id: String, change: (InstalledScript) -> InstalledScript) =
    updateScripts { list -> list.map { if (it.id == id) change(it) else it } }

  private fun translate(script: InstalledScript, locale: Locale): InstalledScript {
    val translations = ScriptTranslations.load(locale) { readFile(script.id, it) }
    return script.copy(
      manifest = script.manifest.translated(translations::text),
      language = translations.language,
    )
  }

  private fun updateScripts(change: (List<InstalledScript>) -> List<InstalledScript>) {
    synchronized(this) {
      scripts = change(scripts)
      translated = null
      save()
    }
    notify { it.onScriptsChanged() }
  }

  private fun notify(event: (Listener) -> Unit) {
    val copy = synchronized(listeners) { listeners.toList() }
    mainHandler.post { copy.forEach(event) }
  }

  private fun bindingKey(command: String, binding: CommandBinding) =
    "$BINDING_OFF$command/${binding.key}"

  private fun dataPrefsName(id: String) = "backtalk_script_$id"

  private fun dataPrefs(id: String): SharedPreferences =
    context.getSharedPreferences(dataPrefsName(id), Context.MODE_PRIVATE)

  private fun scriptsDir(): File = File(context.filesDir, "scripts").apply { mkdirs() }

  private fun codeDir(id: String): File = File(scriptsDir(), id)

  private fun load(): List<InstalledScript> =
    try {
      JSONArray(index.getString(KEY_INDEX, null) ?: "[]").items().filterIsInstance<JSONObject>()
        .mapNotNull { entry ->
          try {
            InstalledScript(
              ScriptManifest.parse(entry.getString("manifest"), strict = false),
              entry.optBoolean("enabled", false),
              entry.optJSONArray("granted").strings().mapNotNull(ScriptPermission::fromKey).toSet(),
              entry.optLong("revision", 0L),
            )
          } catch (e: ManifestException) {
            LogUtils.e(TAG, "Dropping a script whose manifest no longer parses: %s", e.message)
            null
          }
        }
    } catch (e: JSONException) {
      LogUtils.e(TAG, "The script list is damaged: %s", e)
      emptyList()
    }

  private fun save() {
    val array =
      JSONArray(
        scripts.map {
          jsonObject(
            "manifest" to it.manifest.json,
            "enabled" to it.enabled,
            "granted" to JSONArray(it.granted.map(ScriptPermission::key)),
            "revision" to it.revision,
          )
        }
      )
    index.edit { putString(KEY_INDEX, array.toString()) }
  }

  companion object {
    private const val TAG = "ScriptStore"
    private const val INDEX_PREFS = "backtalk_scripts"
    private const val KEY_INDEX = "index"
    private const val KEY_ALL_OFF = "all_off"
    const val EXAMPLE_ID = "example-plugin"
    private const val EXAMPLE_ASSET = "scripting/example-plugin.js"
    private const val STORAGE = "storage/"
    private const val SETTING = "setting/"
    private const val BINDING_OFF = "off/"
    private const val BINDINGS = "bindings/"
    private const val REMOVED = "removed/"
    private const val OPTIONS = "options/"
    private const val MAX_STORAGE_CHARS = 512 * 1024

    @Volatile private var instance: ScriptStore? = null

    @JvmStatic
    fun get(context: Context): ScriptStore =
      instance
        ?: synchronized(this) {
          instance ?: ScriptStore(context.applicationContext).also { instance = it }
        }

    private fun File.insideOf(dir: File): File? =
      takeIf { canonicalPath.startsWith(dir.canonicalPath + File.separator) }

    private fun SharedPreferences.entriesWith(prefix: String): Map<String, Any?> =
      all.filterKeys { it.startsWith(prefix) }.mapKeys { it.key.removePrefix(prefix) }
  }
}
