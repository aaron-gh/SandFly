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

package com.google.android.accessibility.talkback.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.voicecommands.CustomVoiceCommands
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.gesture.AppGestures
import com.google.android.accessibility.talkback.gesture.GestureProfiles
import com.google.android.accessibility.talkback.gesture.OpenAppActions
import com.google.android.accessibility.talkback.keyboard.CustomKeymaps
import com.google.android.accessibility.talkback.keyboard.KeyCombo
import com.google.android.accessibility.talkback.keyboard.KeyText
import com.google.android.accessibility.talkback.keyboard.NewKeyComboModel
import com.google.android.accessibility.talkback.keyboard.SandFlyKey
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut
import com.google.android.accessibility.talkback.macros.Macros
import com.google.android.accessibility.talkback.notifications.Replacement
import com.google.android.accessibility.talkback.speechdictionary.BrailleDictionary
import com.google.android.accessibility.talkback.speechdictionary.DictionaryEntry
import com.google.android.accessibility.talkback.speechdictionary.SpeechDictionary
import com.google.android.accessibility.talkback.speechdictionary.SymbolDictionary
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.util.Locale
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

/**
 * Files of SandFly items, such as macros and gesture profiles, to share or to write by hand: a JSON
 * object with "format": "sandfly-items" and a list for each kind of item. The format is described
 * in docs/reference/item-files.md, which the file reference starts with.
 *
 * Importing adds the items, or replaces those with the same name, and leaves the others. Commands,
 * gestures, keys and settings that SandFly doesn't have are left out and listed.
 */
object ItemFiles {
  const val FORMAT = "sandfly-items"
  const val VERSION = 1

  /** The largest file read. */
  const val MAX_BYTES = 2 * 1024 * 1024

  /** The most items of each kind read from a file. */
  const val MAX_ITEMS = 1_000

  /** The most entries a speech or braille dictionary has after an import. */
  const val MAX_DICTIONARY_ENTRIES = 5_000

  /** The most problems listed after an import. */
  const val MAX_PROBLEMS = 50

  /** The longest name of an item kept, as for settings profiles. */
  private const val MAX_NAME = SettingsProfileStore.MAX_NAME_LENGTH

  /** The longest text that a dictionary entry finds, and that it replaces it with. */
  private const val MAX_FIND = 1_000
  private const val MAX_REPLACE = 10_000

  private const val SETTINGS_FORMAT = "sandfly-settings"

  /** A kind of item: its list's name in files, its name and how many were imported. */
  enum class Kind(val key: String, @StringRes val title: Int, @PluralsRes val count: Int) {
    MACROS("macros", R.string.title_pref_macros, R.plurals.item_file_count_macros),
    GESTURE_PROFILES(
      "gestureProfiles",
      R.string.title_pref_gesture_profiles,
      R.plurals.item_file_count_gesture_profiles,
    ),
    KEYMAPS("keymaps", R.string.item_file_keymaps, R.plurals.item_file_count_keymaps),
    APPS("apps", R.string.title_pref_app_gestures, R.plurals.item_file_count_apps),
    SETTINGS_PROFILES(
      "settingsProfiles",
      R.string.title_pref_settings_profiles,
      R.plurals.item_file_count_settings_profiles,
    ),
    SPEECH_DICTIONARY(
      "speechDictionary",
      R.string.title_pref_speech_dictionary,
      R.plurals.item_file_count_speech_dictionary,
    ),
    SYMBOL_DICTIONARY(
      "symbolDictionary",
      R.string.title_pref_symbol_dictionary,
      R.plurals.item_file_count_symbol_dictionary,
    ),
    BRAILLE_DICTIONARY(
      "brailleDictionary",
      R.string.title_pref_braille_dictionary,
      R.plurals.item_file_count_braille_dictionary,
    ),
    VOICE_COMMANDS("voiceCommands", R.string.voice_commands_custom, R.plurals.item_file_count_voice_commands),
  }

  /** What an import did. */
  sealed class Result {
    /** The file is not JSON, or not an items file. */
    data object NotRead : Result()

    /** The file holds all settings, which Import settings reads. */
    data object SettingsFile : Result()

    /** The file is of a later version of the format. */
    data object Newer : Result()

    /** How many items of each kind were added or replaced, and what was left out. */
    data class Imported(val counts: Map<Kind, Int>, val problems: List<String>) : Result() {
      val keyboardChanged: Boolean
        get() = (counts[Kind.KEYMAPS] ?: 0) > 0 || (counts[Kind.APPS] ?: 0) > 0
    }
  }

  /** SandFly keys by their names in files. */
  private val SANDFLY_KEYS =
    linkedMapOf(
      SandFlyKey.META to "Meta",
      SandFlyKey.ALT to "Alt",
      SandFlyKey.CTRL_ALT to "Ctrl+Alt",
      SandFlyKey.CTRL_META to "Ctrl+Meta",
      SandFlyKey.INSERT to "Insert",
      SandFlyKey.NUMPAD_INSERT to "Numpad Insert",
    )

  /** The names of the SandFly keys in files, for the file reference. */
  fun sandFlyKeyNames(): Collection<String> = SANDFLY_KEYS.values

  private fun sandFlyKeyOf(name: String): SandFlyKey? {
    val wanted = name.filterNot { it.isWhitespace() }.lowercase(Locale.ROOT)
    return SANDFLY_KEYS.entries.firstOrNull { (_, text) ->
      text.filterNot { it.isWhitespace() }.lowercase(Locale.ROOT) == wanted
    }?.key
  }

  private fun prefs(context: Context): SharedPreferences =
    SharedPreferencesUtils.getSharedPreferences(context)

  private fun newFile(): JSONObject = JSONObject().put("format", FORMAT).put("version", VERSION)

  // ---------------------------------------------------------------------------------------------
  // Export.

  /** Returns a file of the gesture profiles with [ids], or all for null, and the macros they run. */
  fun exportGestureProfiles(context: Context, ids: Set<String>? = null): String {
    val profiles = GestureProfiles.read(prefs(context)).filter { ids == null || it.id in ids }
    val array = JSONArray()
    for (profile in profiles) {
      val item = JSONObject().put("id", profile.id).put("name", profile.name)
      profile.swipesBeforeLift?.let { item.put("swipesBeforeLift", it) }
      item.put("gestures", JSONObject(profile.gestures.toSortedMap()))
      array.put(item)
    }
    return withMacros(context, newFile().put(Kind.GESTURE_PROFILES.key, array), profiles.flatMap { it.gestures.values })
  }

  /** Returns a file of the macros with [ids], or all for null, with the macros they run. */
  fun exportMacros(context: Context, ids: Set<String>? = null): String {
    val all = Macros.read(prefs(context))
    val wanted = if (ids == null) all.map { it.id }.toSet() else referencedMacros(all, ids)
    return newFile().put(Kind.MACROS.key, JSONArray(Macros.toJson(all.filter { it.id in wanted }))).toString(2)
  }

  /** Returns a file of the gestures and keyboard shortcuts of [packages], or every app for null. */
  fun exportApps(context: Context, packages: Set<String>? = null): String {
    val apps = AppGestures.read(prefs(context)).filterKeys { packages == null || it in packages }
    val array = JSONArray()
    for ((packageName, app) in apps.toSortedMap()) {
      val keys = JSONObject()
      for ((name, value) in app.keys) {
        val text = AppGestures.keyCombo(name)?.let { KeyText.of(it) } ?: continue
        keys.put(text, value)
      }
      array.put(
        JSONObject()
          .put("package", packageName)
          .put("name", app.name)
          .put("gestures", JSONObject(app.gestures.toSortedMap()))
          .put("keys", keys)
      )
    }
    val values = apps.values.flatMap { it.gestures.values + it.keys.values }
    return withMacros(context, newFile().put(Kind.APPS.key, array), values)
  }

  /**
   * Returns a file of the keymap of [model], the keymap in use, as a custom keymap named [name] with
   * its SandFly key [sandFlyKey], and the macros its shortcuts run.
   */
  fun exportKeymap(context: Context, model: NewKeyComboModel, name: String, sandFlyKey: SandFlyKey?): String {
    val shortcuts = JSONObject()
    val values = mutableListOf<String>()
    for ((key, keyCombo) in model.keyComboMap.toSortedMap()) {
      val text = KeyText.of(keyCombo) ?: continue
      val value = commandOfKeymapKey(context, key) ?: continue
      shortcuts.put(value, text)
      values += value
    }
    val item =
      JSONObject()
        .put("name", name)
        .put("sandFlyKey", SANDFLY_KEYS[sandFlyKey ?: SandFlyKey.META])
        .put("sequences", model.usesSequences())
        .put("arrowsNavigate", model.arrowsNavigate())
        .put("shortcuts", shortcuts)
    return withMacros(context, newFile().put(Kind.KEYMAPS.key, JSONArray().put(item)), values)
  }

  /** Returns a file of the dictionary [kind]. */
  fun exportDictionary(context: Context, kind: Kind): String {
    val prefs = prefs(context)
    val entries =
      when (kind) {
        Kind.SPEECH_DICTIONARY -> SpeechDictionary.read(prefs)
        Kind.SYMBOL_DICTIONARY -> SymbolDictionary.read(prefs)
        Kind.BRAILLE_DICTIONARY -> BrailleDictionary.read(prefs)
        else -> throw IllegalArgumentException("Not a dictionary: $kind")
      }
    val array = JSONArray()
    for (entry in entries) {
      val item = JSONObject().put("find", entry.replacement.find).put("replace", entry.replacement.replace)
      if (kind != Kind.SYMBOL_DICTIONARY) {
        item.put("regex", entry.replacement.regex).put("wholeWord", entry.replacement.wholeWord)
      }
      if (entry.app.isNotEmpty()) item.put("app", entry.app).put("appName", entry.appName)
      array.put(item)
    }
    return newFile().put(kind.key, array).toString(2)
  }

  /** Returns a file of the custom voice commands, and the macros they run. */
  fun exportVoiceCommands(context: Context): String {
    val entries = CustomVoiceCommands.read(prefs(context))
    val array = JSONArray()
    for (entry in entries) array.put(JSONObject().put("phrase", entry.phrase).put("command", entry.value))
    return withMacros(context, newFile().put(Kind.VOICE_COMMANDS.key, array), entries.map { it.value })
  }

  /** Returns a file of the settings profiles with [ids], or all for null. */
  @WorkerThread
  fun exportSettingsProfiles(context: Context, ids: Set<String>? = null): String {
    val store = SettingsProfiles.ready(context)
    val array = JSONArray()
    for (profile in store.profiles()) {
      if (ids != null && profile.id !in ids) continue
      val settings = SettingsProfiles.settingsOf(context, profile.id) ?: continue
      val values = JSONObject()
      for ((profileKey, value) in settings.toSortedMap()) {
        val (file, key) = SettingsBackup.splitProfileKey(profileKey) ?: continue
        values.put(if (file == SettingsBackup.DEFAULT_PREFS) key else profileKey, jsonOf(value))
      }
      val triggers = profile.triggers
      array.put(
        JSONObject()
          .put("name", profile.name)
          .put("apps", JSONArray(triggers.apps.sorted()))
          .put("web", triggers.web)
          .put("reading", triggers.reading)
          .put("editing", triggers.editing)
          .put("settings", values)
      )
    }
    return newFile().put(Kind.SETTINGS_PROFILES.key, array).toString(2)
  }

  /** A setting's value in a file: JSON's own types, a list for a set, and null for unset. */
  private fun jsonOf(value: Any?): Any =
    when (value) {
      null -> JSONObject.NULL
      is Set<*> -> JSONArray(value.map { it.toString() }.sorted())
      is Float -> value.toDouble()
      else -> value
    }

  /** Adds the macros that [values] run, and those they run, to [file], and returns its text. */
  private fun withMacros(context: Context, file: JSONObject, values: Collection<String>): String {
    val ids = values.mapNotNull { Macros.idOf(it) }.toSet()
    if (ids.isNotEmpty()) {
      val all = Macros.read(prefs(context))
      val wanted = referencedMacros(all, ids)
      val macros = all.filter { it.id in wanted }
      if (macros.isNotEmpty()) file.put(Kind.MACROS.key, JSONArray(Macros.toJson(macros)))
    }
    return file.toString(2)
  }

  /** The IDs of [ids] and of the macros that their steps run, as far as they go. */
  private fun referencedMacros(all: List<Macros.Macro>, ids: Set<String>): Set<String> {
    val byId = all.associateBy { it.id }
    val found = LinkedHashSet<String>()
    val queue = ArrayDeque(ids)
    while (queue.isNotEmpty()) {
      val id = queue.removeFirst()
      val macro = byId[id] ?: continue
      if (!found.add(id)) continue
      for (step in macro.steps) {
        (step as? Macros.RunCommand)?.let { Macros.idOf(it.value) }?.let { queue += it }
      }
    }
    return found
  }

  /** The command whose keyboard shortcut a keymap keeps under [key], or null. */
  private fun commandOfKeymapKey(context: Context, key: String): String? {
    Commands.valueOfShortcutKey(key)?.let {
      return it
    }
    val action = TalkBackPhysicalKeyboardShortcut.getActionFromKey(context.resources, key)
    if (action == TalkBackPhysicalKeyboardShortcut.ACTION_UNKNOWN) return null
    return Commands.valueOfKeyboardAction(context, action)
  }

  /** The key under which a keymap keeps the keyboard shortcut of the command [value]. */
  private fun keymapKeyOf(context: Context, value: String): String =
    Commands.keyboardActionFor(context, value)?.getKey(context.resources) ?: Commands.shortcutKey(value)

  // ---------------------------------------------------------------------------------------------
  // Import.

  /**
   * Reads [text] as an items file and adds or replaces its items. Text before the first "{" and
   * after the last "}", such as the code fence around a file an AI chat wrote, is ignored.
   */
  @WorkerThread
  fun import(context: Context, text: String): Result {
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start < 0 || end <= start) return Result.NotRead
    val root =
      try {
        JSONObject(text.substring(start, end + 1))
      } catch (e: JSONException) {
        return Result.NotRead
      }
    val format = root.optString("format")
    if (format == SETTINGS_FORMAT || root.has("stores")) return Result.SettingsFile
    if (format.isNotEmpty() && format != FORMAT) return Result.NotRead
    if (Kind.entries.none { root.has(it.key) }) return Result.NotRead
    if (root.optInt("version", VERSION) > VERSION) return Result.Newer
    return Importer(context, root).run()
  }

  /** The gestures that can be assigned, by preference key, with their names, from the Gestures screen. */
  fun gestures(context: Context): Map<String, String> {
    val gestures = LinkedHashMap<String, String>()
    val android = "http://schemas.android.com/apk/res/android"
    context.resources.getXml(R.xml.gesture_preferences).use { parser ->
      while (parser.next() != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType != XmlPullParser.START_TAG || !parser.name.endsWith(".GestureListPreference")) {
          continue
        }
        val key = parser.getAttributeResourceValue(android, "key", 0)
        val title = parser.getAttributeResourceValue(android, "title", 0)
        if (key == 0) continue
        gestures[context.getString(key)] = if (title == 0) context.getString(key) else context.getString(title)
      }
    }
    return gestures
  }

  /** Imports one file's items, keeping what was left out. */
  private class Importer(private val context: Context, private val root: JSONObject) {
    private val prefs = prefs(context)
    private val counts = LinkedHashMap<Kind, Int>()
    private val problems = mutableListOf<String>()
    private var moreProblems = false

    // Macros and gesture profiles by their IDs and lowercase names, in the file and on the phone.
    private val macroIds = HashMap<String, String>()
    private val profileIds = HashMap<String, String>()

    // The commands of the items imported, which aren't saved yet when the others are checked.
    private val imported = HashSet<String>()

    private val gestures by lazy { gestures(context) }
    private var known: Set<String> = emptySet()

    fun run(): Result {
      planIds()
      known = knownCommands()
      importMacros()
      importGestureProfiles()
      importKeymaps()
      importApps()
      importVoiceCommands()
      importDictionary(Kind.SPEECH_DICTIONARY)
      importDictionary(Kind.SYMBOL_DICTIONARY)
      importDictionary(Kind.BRAILLE_DICTIONARY)
      importSettingsProfiles()
      if (moreProblems) problems += context.getString(R.string.item_file_problem_more)
      return Result.Imported(counts, problems)
    }

    private fun problem(@StringRes text: Int, vararg args: Any) {
      if (problems.size < MAX_PROBLEMS) problems += context.getString(text, *args) else moreProblems = true
    }

    private fun counted(kind: Kind, count: Int = 1) {
      if (count > 0) counts[kind] = (counts[kind] ?: 0) + count
    }

    /** The objects of the list of [kind], the first [MAX_ITEMS], with their numbers from 1. */
    private fun items(kind: Kind): List<Pair<Int, JSONObject>> {
      val array = root.optJSONArray(kind.key) ?: return emptyList()
      if (array.length() > MAX_ITEMS) problem(R.string.item_file_problem_limit, context.getString(kind.title), MAX_ITEMS)
      return (0 until minOf(array.length(), MAX_ITEMS)).mapNotNull { index ->
        val item = array.optJSONObject(index)
        if (item == null) problem(R.string.item_file_problem_not_read, context.getString(kind.title), index + 1)
        item?.let { index + 1 to it }
      }
    }

    private fun name(kind: Kind, number: Int, item: JSONObject, field: String = "name"): String? {
      val name = itemName(item, field)
      if (name.isEmpty()) problem(R.string.item_file_problem_not_read, context.getString(kind.title), number)
      return name.ifEmpty { null }
    }

    private fun itemName(item: JSONObject, field: String = "name"): String = item.optString(field).trim().take(MAX_NAME)

    /** Gives each macro and gesture profile of the file the ID it gets: its namesake's on the phone, or a new one. */
    private fun planIds() {
      for (macro in Macros.read(prefs)) {
        macroIds[macro.id] = macro.id
        macroIds.putIfAbsent(macro.name.lowercase(), macro.id)
      }
      for (profile in GestureProfiles.read(prefs)) {
        profileIds[profile.id] = profile.id
        profileIds.putIfAbsent(profile.name.lowercase(), profile.id)
      }
      val phoneMacros = Macros.read(prefs).associateBy { it.name.lowercase() }
      val fileMacroNames = HashMap<String, String>()
      for ((_, item) in rawItems(Kind.MACROS)) {
        val name = itemName(item)
        if (name.isEmpty()) continue
        val id = phoneMacros[name.lowercase()]?.id ?: fileMacroNames[name.lowercase()] ?: Macros.newId()
        fileMacroNames[name.lowercase()] = id
        item.optString("id").takeIf { it.isNotEmpty() }?.let { macroIds[it] = id }
        macroIds[name.lowercase()] = id
        imported += Macros.commandOf(id)
      }
      val phoneProfiles = GestureProfiles.read(prefs).associateBy { it.name.lowercase() }
      val fileProfileNames = HashMap<String, String>()
      for ((_, item) in rawItems(Kind.GESTURE_PROFILES)) {
        val name = itemName(item)
        if (name.isEmpty()) continue
        val id = phoneProfiles[name.lowercase()]?.id ?: fileProfileNames[name.lowercase()] ?: GestureProfiles.newId()
        fileProfileNames[name.lowercase()] = id
        item.optString("id").takeIf { it.isNotEmpty() }?.let { profileIds[it] = id }
        profileIds[name.lowercase()] = id
        imported += GestureProfiles.valueOf(id)
      }
    }

    // The items without reporting problems, which the import reports later.
    private fun rawItems(kind: Kind): List<Pair<Int, JSONObject>> {
      val array = root.optJSONArray(kind.key) ?: return emptyList()
      return (0 until minOf(array.length(), MAX_ITEMS)).mapNotNull { index ->
        array.optJSONObject(index)?.let { index + 1 to it }
      }
    }

    /**
     * The command [value] with the macro or gesture profile it names by its ID in the file, or by its
     * name, replaced with that item's ID on this phone, and an app named by its package with its
     * launcher activity.
     */
    private fun remap(value: String): String {
      // An app by its package name opens its launcher activity.
      if (value.startsWith(OPEN_APP) && '/' !in value) {
        val launch =
          try {
            context.packageManager.getLaunchIntentForPackage(value.removePrefix(OPEN_APP).trim())?.component
          } catch (e: RuntimeException) {
            null
          }
        return launch?.let { OpenAppActions.valueOf(it) } ?: value
      }
      Macros.idOf(value)?.let { ref ->
        val id = macroIds[ref] ?: macroIds[ref.trim().lowercase()] ?: return value
        return Macros.commandOf(id)
      }
      if (GestureProfiles.isAction(value)) {
        val ref = GestureProfiles.idOf(value)
        if (ref.isEmpty()) return value
        val id = profileIds[ref] ?: profileIds[ref.trim().lowercase()] ?: return value
        return GestureProfiles.valueOf(id)
      }
      return value
    }

    /** Every command of the file, after [remap], that SandFly has or that the file adds. */
    private fun knownCommands(): Set<String> {
      val values = LinkedHashSet<String>()
      fun addAll(obj: JSONObject?) {
        obj ?: return
        for (key in obj.keys()) values += remap(obj.optString(key).trim())
      }
      for ((_, item) in rawItems(Kind.MACROS)) {
        val steps = item.optJSONArray("steps") ?: continue
        for (i in 0 until steps.length()) {
          val step = steps.optJSONObject(i) ?: continue
          if (step.optString("type") == "command") values += remap(step.optString("command").trim())
        }
      }
      for ((_, item) in rawItems(Kind.GESTURE_PROFILES)) addAll(item.optJSONObject("gestures"))
      for ((_, item) in rawItems(Kind.APPS)) {
        addAll(item.optJSONObject("gestures"))
        addAll(item.optJSONObject("keys"))
      }
      for ((_, item) in rawItems(Kind.KEYMAPS)) {
        item.optJSONObject("shortcuts")?.keys()?.forEach { values += remap(it.trim()) }
      }
      for ((_, item) in rawItems(Kind.VOICE_COMMANDS)) values += remap(item.optString("command").trim())
      val list = values.filter { it.isNotEmpty() && it !in imported }
      val labels = Commands.labels(context, list)
      return imported + list.filterIndexed { index, _ -> labels[index] != null } +
        context.getString(R.string.shortcut_value_unassigned)
    }

    /** The command [value] as saved, or null when SandFly doesn't have it, which is reported for [item]. */
    private fun command(item: String, value: String): String? {
      val remapped = remap(value.trim())
      if (remapped in known) return remapped
      problem(R.string.item_file_problem_command, item, value)
      return null
    }

    /** The gestures of [json], by preference key, with known commands, reporting the others for [item]. */
    private fun gestureMap(item: String, json: JSONObject?): Map<String, String> {
      json ?: return emptyMap()
      val result = LinkedHashMap<String, String>()
      for (key in json.keys()) {
        if (key !in gestures) {
          problem(R.string.item_file_problem_gesture, item, key)
          continue
        }
        val value = json.optString(key).trim()
        // Default leaves the gesture out, as on a profile's screen.
        if (value.isEmpty() || value == GestureProfiles.DEFAULT_VALUE || value.equals("DEFAULT", ignoreCase = true)) {
          continue
        }
        command(item, value)?.let { result[key] = it }
      }
      return result
    }

    private fun importMacros() {
      val items = items(Kind.MACROS)
      if (items.isEmpty()) return
      val all = Macros.read(prefs).toMutableList()
      var count = 0
      for ((number, item) in items) {
        val name = name(Kind.MACROS, number, item) ?: continue
        val id = macroIds[name.lowercase()] ?: continue
        val copy = JSONObject(item.toString()).put("id", id).put("name", name)
        copy.optJSONArray("steps")?.let { steps ->
          for (i in 0 until steps.length()) {
            val step = steps.optJSONObject(i) ?: continue
            if (step.optString("type") == "open_app") step.put("app", remap(step.optString("app").trim()))
          }
        }
        val macro = Macros.fromJson(JSONArray().put(copy).toString()).firstOrNull()
        if (macro == null) {
          problem(R.string.item_file_problem_not_read, context.getString(Kind.MACROS.title), number)
          continue
        }
        // A step whose command SandFly doesn't have stays, shown as unavailable, so the blocks still match.
        val steps =
          macro.steps.map { step ->
            if (step !is Macros.RunCommand) step
            else Macros.RunCommand(command(name, step.value) ?: step.value)
          }
        val read = macro.copy(steps = steps)
        val index = all.indexOfFirst { it.id == id }
        if (index >= 0) all[index] = read else all += read
        count++
      }
      Macros.write(prefs, all)
      counted(Kind.MACROS, count)
    }

    private fun importGestureProfiles() {
      val items = items(Kind.GESTURE_PROFILES)
      if (items.isEmpty()) return
      val all = GestureProfiles.read(prefs).toMutableList()
      val defaultName = context.getString(R.string.gesture_profile_default)
      var count = 0
      for ((number, item) in items) {
        val name = name(Kind.GESTURE_PROFILES, number, item) ?: continue
        if (name.equals(defaultName, ignoreCase = true)) {
          problem(R.string.item_file_problem_not_read, context.getString(Kind.GESTURE_PROFILES.title), number)
          continue
        }
        val id = profileIds[name.lowercase()] ?: continue
        val swipes = if (item.has("swipesBeforeLift") && !item.isNull("swipesBeforeLift")) item.optBoolean("swipesBeforeLift") else null
        val profile = GestureProfiles.Profile(id, name, gestureMap(name, item.optJSONObject("gestures")), swipes)
        val index = all.indexOfFirst { it.id == id }
        if (index >= 0) all[index] = profile else all += profile
        count++
      }
      GestureProfiles.write(prefs, all)
      counted(Kind.GESTURE_PROFILES, count)
    }

    private fun importKeymaps() {
      val items = items(Kind.KEYMAPS)
      var count = 0
      for ((number, item) in items) {
        val name = name(Kind.KEYMAPS, number, item) ?: continue
        val sandFlyKey =
          item.optString("sandFlyKey").takeIf { it.isNotBlank() }?.let { text ->
            sandFlyKeyOf(text) ?: run {
              problem(R.string.item_file_problem_keys, name, text)
              null
            }
          } ?: SandFlyKey.META
        val shortcuts = LinkedHashMap<String, KeyCombo>()
        val taken = HashSet<KeyCombo>()
        val json = item.optJSONObject("shortcuts") ?: JSONObject()
        for (value in json.keys()) {
          val keysText = json.optString(value)
          val command = command(name, value) ?: continue
          val keyCombo = KeyText.parse(keysText)
          if (keyCombo == null) {
            problem(R.string.item_file_problem_keys, name, keysText)
            continue
          }
          // Two commands can't share keys; the first keeps them.
          if (!taken.add(keyCombo)) {
            problem(R.string.item_file_problem_keys_taken, name, keysText)
            continue
          }
          shortcuts[keymapKeyOf(context, command)] = keyCombo
        }
        val sequences = item.optBoolean("sequences", false)
        val arrows = item.optBoolean("arrowsNavigate", false)
        val existing = CustomKeymaps.all(context).firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (existing != null) {
          CustomKeymaps.replace(context, existing, sequences, sandFlyKey, shortcuts, arrows)
        } else {
          CustomKeymaps.create(context, name, sequences, sandFlyKey, shortcuts, arrows)
        }
        count++
      }
      counted(Kind.KEYMAPS, count)
    }

    private fun importApps() {
      val items = items(Kind.APPS)
      if (items.isEmpty()) return
      val all = LinkedHashMap(AppGestures.read(prefs))
      var count = 0
      for ((number, item) in items) {
        val packageName = name(Kind.APPS, number, item, field = "package") ?: continue
        if (!PACKAGE.matches(packageName)) {
          problem(R.string.item_file_problem_not_read, context.getString(Kind.APPS.title), number)
          continue
        }
        val appName = itemName(item).ifEmpty { all[packageName]?.name ?: packageName }
        val keys = LinkedHashMap<String, String>()
        val json = item.optJSONObject("keys") ?: JSONObject()
        for (keysText in json.keys()) {
          val keyCombo = KeyText.parse(keysText)
          if (keyCombo == null) {
            problem(R.string.item_file_problem_keys, appName, keysText)
            continue
          }
          command(appName, json.optString(keysText))?.let { keys[AppGestures.keyName(keyCombo)] = it }
        }
        all[packageName] = AppGestures.App(appName, gestureMap(appName, item.optJSONObject("gestures")), keys)
        count++
      }
      AppGestures.write(prefs, all)
      counted(Kind.APPS, count)
    }

    private fun importVoiceCommands() {
      val items = items(Kind.VOICE_COMMANDS)
      if (items.isEmpty()) return
      val locale = Locale.getDefault()
      val all = CustomVoiceCommands.read(prefs).toMutableList()
      var count = 0
      for ((number, item) in items) {
        val phrase = name(Kind.VOICE_COMMANDS, number, item, field = "phrase")?.take(CustomVoiceCommands.MAX_PHRASE) ?: continue
        val value = command(phrase, item.optString("command")) ?: continue
        val existing = CustomVoiceCommands.find(all, phrase, locale)
        if (existing != null) {
          all[all.indexOf(existing)] = existing.copy(phrase = phrase, value = value)
        } else if (all.size < CustomVoiceCommands.MAX) {
          all += CustomVoiceCommands.Entry(CustomVoiceCommands.newId(), phrase, value)
        } else {
          problem(R.string.item_file_problem_limit, context.getString(Kind.VOICE_COMMANDS.title), CustomVoiceCommands.MAX)
          break
        }
        count++
      }
      CustomVoiceCommands.write(prefs, all)
      counted(Kind.VOICE_COMMANDS, count)
    }

    private fun importDictionary(kind: Kind) {
      val items = items(kind)
      if (items.isEmpty()) return
      val symbols = kind == Kind.SYMBOL_DICTIONARY
      val all =
        when (kind) {
          Kind.SPEECH_DICTIONARY -> SpeechDictionary.read(prefs)
          Kind.SYMBOL_DICTIONARY -> SymbolDictionary.read(prefs)
          else -> BrailleDictionary.read(prefs)
        }.toMutableList()
      val max = if (symbols) SymbolDictionary.MAX_ENTRIES else MAX_DICTIONARY_ENTRIES
      val title = context.getString(kind.title)
      var count = 0
      for ((number, item) in items) {
        val find = item.optString("find")
        if (find.isEmpty() || find.length > MAX_FIND || !item.has("replace") || item.optString("replace").length > MAX_REPLACE) {
          problem(R.string.item_file_problem_not_read, title, number)
          continue
        }
        val regex = !symbols && item.optBoolean("regex", false)
        val replacement =
          Replacement(
            find,
            item.optString("replace"),
            regex,
            !symbols && item.optBoolean("wholeWord", item.optBoolean("word", true)),
          )
        // Speech waits for each expression, so one that can take very long is refused, as in macros.
        if (regex && (replacement.pattern == null || Macros.compilePattern(find) == null)) {
          problem(R.string.item_file_problem_regex, title, find)
          continue
        }
        val app = item.optString("app").trim()
        if (app.isNotEmpty() && !PACKAGE.matches(app)) {
          problem(R.string.item_file_problem_not_read, title, number)
          continue
        }
        val entry = DictionaryEntry(replacement, app, itemName(item, "appName").ifEmpty { app })
        val index = all.indexOfFirst { it.replacement.find == find && it.app == app }
        if (index >= 0) {
          all[index] = entry
        } else if (all.size < max) {
          all += entry
        } else {
          problem(R.string.item_file_problem_limit, title, max)
          break
        }
        count++
      }
      when (kind) {
        Kind.SPEECH_DICTIONARY -> SpeechDictionary.write(prefs, all)
        Kind.SYMBOL_DICTIONARY -> SymbolDictionary.write(prefs, all)
        else -> BrailleDictionary.write(prefs, all)
      }
      counted(kind, count)
    }

    private fun importSettingsProfiles() {
      val items = items(Kind.SETTINGS_PROFILES)
      var count = 0
      for ((number, item) in items) {
        val name = name(Kind.SETTINGS_PROFILES, number, item)?.take(SettingsProfileStore.MAX_NAME_LENGTH) ?: continue
        val settings = LinkedHashMap<String, Any?>()
        val json = item.optJSONObject("settings") ?: JSONObject()
        for (key in json.keys()) {
          val profileKey = if (key.contains('/')) key else SettingsBackup.profileKey(SettingsBackup.DEFAULT_PREFS, key)
          val raw = json.opt(key)
          val value =
            if (raw == null || raw == JSONObject.NULL) null
            else SettingsBackup.profileValueFromJson(context, profileKey, raw) ?: SETTING_NOT_SET
          if (value === SETTING_NOT_SET || !SettingsBackup.canSetProfileValue(context, profileKey, value)) {
            problem(R.string.item_file_problem_setting, name, key)
            continue
          }
          settings[profileKey] = value
        }
        val apps = item.optJSONArray("apps")?.let { array ->
          (0 until array.length()).mapNotNull { i -> array.optString(i).trim().takeIf { PACKAGE.matches(it) } }.toSet()
        }.orEmpty()
        val triggers =
          SettingsProfileStore.Triggers(
            apps,
            item.optBoolean("web", false),
            item.optBoolean("reading", false),
            item.optBoolean("editing", false),
          )
        if (SettingsProfiles.importProfile(context, name, triggers, settings)) {
          count++
        } else {
          problem(R.string.item_file_problem_not_read, context.getString(Kind.SETTINGS_PROFILES.title), number)
        }
      }
      counted(Kind.SETTINGS_PROFILES, count)
    }
  }

  private val SETTING_NOT_SET = Any()

  private const val OPEN_APP = "OPEN_APP:"

  /** A package name, as Android allows them. */
  private val PACKAGE = Regex("(?=.{3,255}$)[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")

  /** What [result] says: what was imported, then what was left out. */
  fun describe(context: Context, result: Result): String =
    when (result) {
      Result.NotRead -> context.getString(R.string.item_file_not_read)
      Result.SettingsFile -> context.getString(R.string.item_file_settings_file)
      Result.Newer -> context.getString(R.string.item_file_newer)
      is Result.Imported -> {
        val parts =
          result.counts.map { (kind, count) -> context.resources.getQuantityString(kind.count, count, count) }
        val summary =
          if (parts.isEmpty()) context.getString(R.string.item_file_nothing_imported)
          else context.getString(R.string.item_file_imported, parts.joinToString(", "))
        if (result.problems.isEmpty()) summary
        else summary + "\n\n" + context.getString(R.string.item_file_left_out) + "\n" + result.problems.joinToString("\n")
      }
    }
}
