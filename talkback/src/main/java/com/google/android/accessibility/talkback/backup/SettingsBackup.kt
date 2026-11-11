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

package com.google.android.accessibility.talkback.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.commands.RemovedCommands
import com.google.android.accessibility.talkback.migration.MigrationFiles
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.io.File
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Exports SandFly's settings to a file and imports them from one, so that a phone can be set up
 * like another. The file holds every preference file, in device-protected and in
 * credential-protected storage, except the updater's state, the state of the move from Backtalk
 * Aaron, the SandFly Pro licence and API keys.
 *
 * Custom labels, installed scripts and sound themes are files of their own, and are not in it.
 *
 * A settings file exported from Backtalk can be imported too.
 *
 * A settings profile holds the same settings in the same format, but only those of the main,
 * braille keyboard and braille display preference files, and without the state that only this
 * phone has, such as whether a tutorial was shown: see [exportProfile].
 */
object SettingsBackup {
  private const val FORMAT = "sandfly-settings"
  private const val VERSION = 1
  private const val FILE_NAME = "sandfly-settings.json"
  private const val MAX_BYTES = 5 * 1024 * 1024
  private const val PREFS_SUFFIX = ".xml"

  /** The default preference file, whose name holds the app ID, so a file from another ID fits. */
  private const val DEFAULT_PREFS = "default"
  private const val DEVICE = "device"
  private const val CREDENTIAL = "credential"

  /**
   * API keys stay on the phone, since the file can be shared or synced anywhere. Each has "api_key"
   * in its name, such as pref_gemini_api_key and pref_ai_claude_api_key.
   */
  private fun isSecret(key: String): Boolean = key.contains("api_key")

  /**
   * The SandFly Pro module's file: the installation's device ID and its licence. Another phone
   * activates the code as a device of its own.
   */
  @VisibleForTesting const val PRO_PREFS = "sandfly_ai"

  private val SKIPPED_FILES = setOf(MigrationFiles.STATE_PREFS, PRO_PREFS)

  /** Files of libraries, such as Google Play services and WebView, which hold their own state. */
  private val LIBRARY_PREFIXES = listOf("com.google.", "androidx.", "WebView", "Firebase")

  /**
   * The preference files, besides the default one, that a settings profile holds. Other files keep
   * installed scripts, script sources, sound theme drafts and other content, not settings.
   */
  private val PROFILE_FILES = setOf("braille_keyboard", "braille_display")

  /**
   * Settings that stay as they are when a settings profile is applied: the state of first runs,
   * tutorials, migrations, downloads and counters, which describe this phone and not a profile.
   */
  private val PROFILE_STATE_KEYS =
    setOf(
      "has_training_exit",
      "has_onboarding_exit",
      "pref_direct_touch_seen",
      "pref_audio_outputs_seen",
      "talkback_enabled_count",
      "talkback_off_timestamp",
      "pref_previous_global_window_animation_scale_key",
      "migration_done",
      "migration_attempts",
      "cache_cleanup_version",
      "battery_optimization_check_done",
      "pref_brailleime_show_option_dialog_count",
      "pref_brailleime_exit_keyboard_count",
      "pref_bd_switch_contracted_count_key",
      "pref_bd_show_caption_tutorial_dialog_timestamp",
    )

  /** Parts of the keys of [PROFILE_STATE_KEYS]-like state. */
  private val PROFILE_STATE_FRAGMENTS =
    listOf(
      "_shown_times",
      "first_time",
      "first_update",
      "opt_in_count",
      "_installed",
      "_uninstalled",
      "install_lib",
      "_tutorial_done",
    )

  /** Whether [key] is left out of settings profiles and kept when one is applied. */
  @VisibleForTesting
  fun isKeptOutOfProfiles(key: String): Boolean =
    key in PROFILE_STATE_KEYS ||
      key in BACKTALK_SKIPPED_KEYS ||
      key.endsWith("_shown") ||
      key.endsWith("_shown_key") ||
      PROFILE_STATE_FRAGMENTS.any { key.contains(it) }

  /** Sets up Export settings and Import settings in the main settings screen. */
  @JvmStatic
  fun setUpPreferences(fragment: PreferenceFragmentCompat) {
    val export =
      fragment.findPreference<Preference>(fragment.getString(R.string.pref_export_settings_key))
    val import =
      fragment.findPreference<Preference>(fragment.getString(R.string.pref_import_settings_key))
    if (export == null || import == null) {
      return
    }
    // Watches have no file picker.
    if (FormFactorUtils.isAndroidWear()) {
      export.parent?.removePreference(export)
      import.parent?.removePreference(import)
      return
    }
    val create =
      fragment.registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
      ) { uri ->
        uri?.let {
          val context = fragment.requireContext().applicationContext
          inBackground(context, R.string.settings_export_failed) { write(context, it) }
        }
      }
    val open =
      fragment.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
          val context = fragment.requireContext().applicationContext
          inBackground(context, R.string.settings_import_failed) { read(context, it) }
        }
      }
    export.setOnPreferenceClickListener {
      create.launch(FILE_NAME)
      true
    }
    import.setOnPreferenceClickListener {
      open.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
      true
    }
  }

  /**
   * Files can be on a cloud provider, which downloads or uploads before the stream returns, and the
   * settings screen shares its main thread with the screen reader.
   */
  private val executor =
    ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())

  private fun inBackground(context: Context, @StringRes failed: Int, work: () -> Unit) {
    executor.execute {
      try {
        work()
      } catch (e: Throwable) {
        toast(context, failed)
      }
    }
  }

  private fun write(context: Context, uri: Uri) {
    val written =
      try {
        context.contentResolver.openOutputStream(uri, "wt")?.use {
          it.write(export(context).toByteArray())
        } != null
      } catch (e: IOException) {
        false
      } catch (e: SecurityException) {
        false
      }
    toast(context, if (written) R.string.settings_export_done else R.string.settings_export_failed)
  }

  private fun read(context: Context, uri: Uri) {
    val imported =
      try {
        val bytes =
          context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(MAX_BYTES + 1)
            var length = 0
            while (length < buffer.size) {
              val count = input.read(buffer, length, buffer.size - length)
              if (count < 0) break
              length += count
            }
            if (length > MAX_BYTES) throw IOException("The file is too large")
            buffer.copyOf(length)
          }
        bytes != null && import(context, bytes.decodeToString())
      } catch (e: IOException) {
        false
      } catch (e: SecurityException) {
        false
      }
    if (imported) {
      // The keyboard shortcuts are read once, when the key combo model is built.
      Handler(Looper.getMainLooper()).post {
        TalkBackService.getInstance()?.keyComboManager?.refreshKeyComboModel()
      }
    }
    toast(context, if (imported) R.string.settings_import_done else R.string.settings_import_failed)
  }

  /** Returns every exported setting as JSON. */
  fun export(context: Context): String = exportJson(context, forProfile = false).toString(2)

  /**
   * Returns the settings that a settings profile holds, as JSON in the format of [export]: the
   * main, braille keyboard and braille display preference files in device-protected storage,
   * without API keys, the updater's state and the keys of [isKeptOutOfProfiles].
   */
  fun exportProfile(context: Context): String = exportJson(context, forProfile = true).toString()

  private fun exportJson(context: Context, forProfile: Boolean): JSONObject {
    val files = JSONObject()
    for ((storage, storageContext) in storages(context, forProfile)) {
      val storageFiles = JSONObject()
      for (name in prefsNames(context, storageContext)) {
        if (forProfile && !isProfileFile(context, name)) continue
        val prefs = storageContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        val values = JSONObject()
        for ((key, value) in prefs.all) {
          if (isLeftOut(context, name, key, forProfile)) continue
          encode(value)?.let { values.put(key, it) }
        }
        storageFiles.put(exportedName(context, name), values)
      }
      files.put(storage, storageFiles)
    }
    return JSONObject().put("format", FORMAT).put("version", VERSION).put("files", files)
  }

  /**
   * Replaces the settings in each preference file of [json] with the file's. Files that [json]
   * doesn't hold, API keys and the updater's state are kept. Returns false if [json] is not a
   * SandFly settings file, and then changes nothing.
   */
  fun import(context: Context, json: String): Boolean = import(context, json, forProfile = false)

  /**
   * Replaces the settings that a settings profile holds with those of [json], from
   * [exportProfile]. Settings a profile leaves out are kept. Returns false if [json] can't be read,
   * and then changes nothing.
   */
  fun importProfile(context: Context, json: String): Boolean =
    import(context, json, forProfile = true)

  private fun import(context: Context, json: String, forProfile: Boolean): Boolean {
    val files =
      try {
        val root = JSONObject(json)
        if (!forProfile && isBacktalkFile(root)) {
          return importBacktalk(backtalkStores(context), root, context)
        }
        if (root.optString("format") != FORMAT || root.optInt("version") > VERSION) return false
        root.getJSONObject("files")
      } catch (e: JSONException) {
        return false
      }
    val edits = mutableListOf<Pair<SharedPreferences, Map<String, Any>>>()
    for ((storage, storageContext) in storages(context, forProfile)) {
      val storageFiles = files.optJSONObject(storage) ?: if (forProfile) JSONObject() else continue
      val fileNames = storageFiles.keys().asSequence().toMutableSet()
      if (forProfile) {
        // A profile without one of its files, saved before the file was made, clears it.
        fileNames +=
          prefsNames(context, storageContext)
            .filter { isProfileFile(context, it) }
            .map { exportedName(context, it) }
      }
      for (file in fileNames) {
        val name = importedName(context, file) ?: continue
        if (forProfile && !isProfileFile(context, name)) continue
        val values =
          if (storageFiles.has(file)) storageFiles.optJSONObject(file) ?: return false
          else JSONObject()
        val prefs = storageContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        val decoded = mutableMapOf<String, Any>()
        for (key in values.keys()) {
          if (isLeftOut(context, name, key, forProfile)) continue
          val value = decode(values.optJSONObject(key)) ?: return false
          // A value that doesn't fit keeps the phone's own.
          decoded[key] = if (fits(context, prefs, key, value)) value else prefs.all[key] ?: continue
        }
        edits += prefs to decoded
      }
    }
    for ((prefs, values) in edits) {
      val editor = prefs.edit()
      for (key in prefs.all.keys) {
        if (key !in values && !isLeftOut(context, null, key, forProfile)) {
          editor.remove(key)
        }
      }
      for ((key, value) in values) {
        @Suppress("UNCHECKED_CAST")
        when (value) {
          is Boolean -> editor.putBoolean(key, value)
          is Int -> editor.putInt(key, value)
          is Long -> editor.putLong(key, value)
          is Float -> editor.putFloat(key, value)
          is String -> editor.putString(key, value)
          is Set<*> -> editor.putStringSet(key, value as Set<String>)
        }
      }
      editor.commit()
    }
    // A file from an earlier version can hold bindings of removed commands.
    RemovedCommands.migrate(SharedPreferencesUtils.getSharedPreferences(context), force = true)
    return true
  }

  /** Backtalk's settings file: format 1, with a preference file for each store. */
  private const val BACKTALK_FORMAT = 1

  /** Settings that Backtalk leaves out of its file, by part of their key: state and secrets. */
  private val BACKTALK_SKIPPED_FRAGMENTS =
    listOf(
      "api_key",
      "_shown",
      "_installed",
      "_uninstalled",
      "install_lib",
      "first_update",
      "first_time",
      "opt_in_count",
    )

  /** Settings that are state of Backtalk's on its phone, not settings to bring over. */
  private val BACKTALK_SKIPPED_KEYS =
    setOf(
      "pref_developer_version_code",
      "brailleime_shared_prefs_version",
      "pref_speech_control_allowed_apps",
      "pref_speech_control_denied_apps",
      "remembered_devices_with_addresses",
    )

  private fun isBacktalkFile(root: JSONObject) =
    root.opt("format") is Int && root.optJSONObject("stores") != null

  /** SandFly's preference files for the stores in Backtalk's file, by their names there. */
  private fun backtalkStores(context: Context): Map<String, SharedPreferences> =
    mapOf(
      "main" to SharedPreferencesUtils.getSharedPreferences(context),
      "braille_keyboard" to SharedPreferencesUtils.getSharedPreferences(context, "braille_keyboard"),
      "braille_display" to SharedPreferencesUtils.getSharedPreferences(context, "braille_display"),
    )

  /**
   * Sets the settings in a file exported from Backtalk, [root], in [stores]. Settings that the file
   * doesn't hold, such as SandFly's own, are kept. Returns false if the file can't be read, and then
   * changes nothing.
   */
  @VisibleForTesting
  fun importBacktalk(
    stores: Map<String, SharedPreferences>,
    root: JSONObject,
    context: Context? = null,
  ): Boolean {
    if (root.optInt("format") != BACKTALK_FORMAT) return false
    val storesJson = root.optJSONObject("stores") ?: return false
    val edits = mutableListOf<Pair<SharedPreferences, Map<String, Any>>>()
    for ((name, prefs) in stores) {
      val entries = storesJson.optJSONObject(name) ?: continue
      val decoded = mutableMapOf<String, Any>()
      for (key in entries.keys()) {
        if (!isBacktalkSettingKept(key)) continue
        val value = decodeBacktalk(entries.optJSONObject(key)) ?: return false
        if (fits(context, prefs, key, value)) decoded[key] = value
      }
      edits += prefs to decoded
    }
    for ((prefs, values) in edits) {
      val editor = prefs.edit()
      for ((key, value) in values) {
        @Suppress("UNCHECKED_CAST")
        when (value) {
          is Boolean -> editor.putBoolean(key, value)
          is Int -> editor.putInt(key, value)
          is Long -> editor.putLong(key, value)
          is Float -> editor.putFloat(key, value)
          is String -> editor.putString(key, value)
          is Set<*> -> editor.putStringSet(key, value as Set<String>)
        }
      }
      editor.commit()
    }
    // Backtalk's file can hold bindings of commands that SandFly replaced.
    stores["main"]?.let { RemovedCommands.migrate(it, force = true) }
    return true
  }

  private fun isBacktalkSettingKept(key: String) =
    !isSecret(key) &&
      key !in BACKTALK_SKIPPED_KEYS &&
      key !in MigrationFiles.SKIPPED_PREF_KEYS &&
      BACKTALK_SKIPPED_FRAGMENTS.none { key.contains(it) }

  /** Reads one setting of Backtalk's file: its type in "t" and its value in "v". */
  private fun decodeBacktalk(json: JSONObject?): Any? {
    json ?: return null
    return try {
      when (json.getString("t")) {
        "bool" -> json.getBoolean("v")
        "int" -> json.getInt("v")
        "long" -> json.getLong("v")
        "float" -> json.getDouble("v").toFloat()
        "string" -> json.getString("v")
        "set" -> {
          val array = json.getJSONArray("v")
          (0 until array.length()).map { array.getString(it) }.toSet()
        }
        else -> null
      }
    } catch (e: JSONException) {
      null
    }
  }

  /**
   * Whether [value] can be stored under [key] in [prefs]. SandFly reads each setting as one type, so
   * a value of another type, or text where a number is stored as text, would throw when read. The
   * type comes from the setting's current value, or else from its default resource.
   */
  private fun fits(context: Context?, prefs: SharedPreferences, key: String, value: Any): Boolean {
    val expected = prefs.all[key] ?: context?.let { defaultValue(it, key) } ?: return true
    if (expected is Set<*>) return value is Set<*>
    if (expected.javaClass != value.javaClass) return false
    return !(expected is String && expected.toFloatOrNull() != null && (value as String).toFloatOrNull() == null)
  }

  /** The default resource of the setting [key], named after the key with "_default" added. */
  private fun defaultValue(context: Context, key: String): Any? {
    val res = context.resources
    for (type in listOf("bool", "integer", "string")) {
      @Suppress("DiscouragedApi")
      val id = res.getIdentifier(key + "_default", type, context.packageName)
      if (id == 0) continue
      return when (type) {
        "bool" -> res.getBoolean(id)
        "integer" -> res.getInteger(id)
        else -> res.getString(id)
      }
    }
    return null
  }

  /** Settings profiles hold only device-protected storage, where SandFly keeps its settings. */
  private fun storages(context: Context, forProfile: Boolean): List<Pair<String, Context>> {
    val device = DEVICE to context.createDeviceProtectedStorageContext()
    return if (forProfile) listOf(device) else listOf(device, CREDENTIAL to context)
  }

  private fun isProfileFile(context: Context, name: String) =
    name == defaultName(context) || name in PROFILE_FILES

  /**
   * Whether [key] of the preference file [name], or of any file for null, is left out of the file
   * or profile, and kept as it is when one is imported or applied.
   */
  private fun isLeftOut(context: Context, name: String?, key: String, forProfile: Boolean) =
    isSecret(key) || isSkipped(context, name, key) || (forProfile && isKeptOutOfProfiles(key))

  private fun prefsNames(context: Context, storageContext: Context): List<String> {
    val directory = File(storageContext.dataDir, MigrationFiles.PREFS_DIR)
    return directory
      .listFiles()
      .orEmpty()
      .map { it.name }
      .filter { it.endsWith(PREFS_SUFFIX) && it.length > PREFS_SUFFIX.length }
      .map { it.removeSuffix(PREFS_SUFFIX) }
      .filter { name -> name == defaultName(context) || !isOtherFile(name) }
      .sorted()
  }

  /** Whether [name] is a file that isn't exported: the move's state, SandFly Pro's, or a library's. */
  private fun isOtherFile(name: String) =
    name in SKIPPED_FILES || LIBRARY_PREFIXES.any { name.startsWith(it) }

  private fun defaultName(context: Context) = context.packageName + "_preferences"

  private fun exportedName(context: Context, name: String) =
    if (name == defaultName(context)) DEFAULT_PREFS else name

  private fun importedName(context: Context, name: String): String? =
    when {
      name == DEFAULT_PREFS -> defaultName(context)
      !MigrationFiles.isSafeName(name) || isOtherFile(name) -> null
      // Another app ID's default file, which this app doesn't read.
      name.endsWith("_preferences") -> null
      else -> name
    }

  /** The updater's state stays as it is on this phone. Null [name] means any file. */
  private fun isSkipped(context: Context, name: String?, key: String) =
    (name == null || name == defaultName(context)) && key in MigrationFiles.SKIPPED_PREF_KEYS

  @VisibleForTesting
  fun encode(value: Any?): JSONObject? =
    when (value) {
      is Boolean -> JSONObject().put("type", "boolean").put("value", value)
      is Int -> JSONObject().put("type", "int").put("value", value)
      is Long -> JSONObject().put("type", "long").put("value", value)
      is Float -> JSONObject().put("type", "float").put("value", value.toString())
      is String -> JSONObject().put("type", "string").put("value", value)
      is Set<*> ->
        JSONObject().put("type", "set").put("value", JSONArray(value.map { it.toString() }))
      else -> null
    }

  @VisibleForTesting
  fun decode(json: JSONObject?): Any? {
    json ?: return null
    return try {
      when (json.getString("type")) {
        "boolean" -> json.getBoolean("value")
        "int" -> json.getInt("value")
        "long" -> json.getLong("value")
        "float" -> json.getString("value").toFloat()
        "string" -> json.getString("value")
        "set" -> {
          val array = json.getJSONArray("value")
          (0 until array.length()).map { array.getString(it) }.toSet()
        }
        else -> null
      }
    } catch (e: JSONException) {
      null
    } catch (e: NumberFormatException) {
      null
    }
  }

  private fun toast(context: Context, @StringRes message: Int) {
    Handler(Looper.getMainLooper()).post {
      Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
  }
}
