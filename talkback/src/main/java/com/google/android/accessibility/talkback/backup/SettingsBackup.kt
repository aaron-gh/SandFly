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
 * A settings profile holds settings of the main, braille keyboard and braille display preference
 * files, without the state that only this phone has, such as whether a tutorial was shown: see
 * [profileValues]. While profiles are applied, an exported file holds the settings as they are
 * without them.
 */
object SettingsBackup {
  private const val FORMAT = "sandfly-settings"
  private const val VERSION = 1
  private const val FILE_NAME = "sandfly-settings.json"
  private const val REFERENCE_FILE_NAME = "sandfly-file-format.md"
  private const val MAX_BYTES = 5 * 1024 * 1024
  private const val PREFS_SUFFIX = ".xml"

  /** The default preference file, whose name holds the app ID, so a file from another ID fits. */
  const val DEFAULT_PREFS = "default"
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
      fragment.findPreference<Preference>(fragment.getString(R.string.pref_export_item_file_reference_key))?.let {
        it.parent?.removePreference(it)
      }
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
    setUpReference(fragment)
  }

  /** Sets up Export file format reference, which describes item files with this phone's lists. */
  private fun setUpReference(fragment: PreferenceFragmentCompat) {
    val reference =
      fragment.findPreference<Preference>(fragment.getString(R.string.pref_export_item_file_reference_key))
        ?: return
    val create =
      fragment.registerForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        // The settings screens are read with this screen's theme.
        val themed = fragment.context ?: return@registerForActivityResult
        uri?.let { ItemFileLauncher.exportText(themed, it) { ItemFileReference.build(themed) } }
      }
    reference.setOnPreferenceClickListener {
      create.launch(REFERENCE_FILE_NAME)
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

  /** Returns every exported setting as JSON, with the settings that profiles replace as without them. */
  fun export(context: Context): String = exportJson(context).toString(2)

  private fun exportJson(context: Context): JSONObject {
    val base = SettingsProfiles.baseValues(context)
    val files = JSONObject()
    for ((storage, storageContext) in storages(context)) {
      val storageFiles = JSONObject()
      for (name in prefsNames(context, storageContext)) {
        val prefs = storageContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        val exported = exportedName(context, name)
        val replaced = if (storage == DEVICE) base else emptyMap()
        val values = JSONObject()
        val all = prefs.all
        for ((key, value) in all) {
          if (isLeftOut(context, name, key, forProfile = false)) continue
          val profileKey = profileKey(exported, key)
          val kept = if (profileKey in replaced) replaced[profileKey] else value
          encode(kept)?.let { values.put(key, it) }
        }
        for ((profileKey, value) in replaced) {
          val (file, key) = splitProfileKey(profileKey) ?: continue
          if (file != exported || key in all) continue
          encode(value)?.let { values.put(key, it) }
        }
        storageFiles.put(exported, values)
      }
      files.put(storage, storageFiles)
    }
    return JSONObject().put("format", FORMAT).put("version", VERSION).put("files", files)
  }

  /** The key of a setting in a settings profile: the exported name of its file, "/" and its key. */
  @JvmStatic fun profileKey(file: String, key: String): String = "$file/$key"

  /** The file and key of [profileKey], or null when it isn't one. */
  @JvmStatic
  fun splitProfileKey(profileKey: String): Pair<String, String>? {
    val slash = profileKey.indexOf('/')
    if (slash <= 0 || slash == profileKey.length - 1) return null
    return profileKey.substring(0, slash) to profileKey.substring(slash + 1)
  }

  /** The preference key of [profileKey]. */
  @JvmStatic fun keyOf(profileKey: String): String = splitProfileKey(profileKey)?.second ?: profileKey

  /**
   * Every setting that a settings profile can hold, by [profileKey]: those of the main, braille
   * keyboard and braille display preference files in device-protected storage, without API keys,
   * the updater's state and the keys of [isKeptOutOfProfiles].
   */
  fun profileValues(context: Context): Map<String, Any> {
    val storage = context.createDeviceProtectedStorageContext()
    val values = mutableMapOf<String, Any>()
    for (name in prefsNames(context, storage)) {
      if (!isProfileFile(context, name)) continue
      val exported = exportedName(context, name)
      for ((key, value) in storage.getSharedPreferences(name, Context.MODE_PRIVATE).all) {
        if (value == null || isLeftOut(context, name, key, forProfile = true)) continue
        values[profileKey(exported, key)] = value
      }
    }
    return values
  }

  /**
   * The settings of [json], a profile saved by an earlier version as a whole settings file, by
   * [profileKey], or null if it can't be read.
   */
  fun profileValuesOf(context: Context, json: String): Map<String, Any>? =
    try {
      val root = JSONObject(json)
      if (root.optString("format") != FORMAT) null
      else {
        val device = root.getJSONObject("files").optJSONObject(DEVICE) ?: JSONObject()
        val values = mutableMapOf<String, Any>()
        for (file in device.keys()) {
          val name = importedName(context, file) ?: continue
          if (!isProfileFile(context, name)) continue
          val settings = device.optJSONObject(file) ?: continue
          for (key in settings.keys()) {
            if (isLeftOut(context, name, key, forProfile = true)) continue
            decode(settings.optJSONObject(key))?.let { values[profileKey(file, key)] = it }
          }
        }
        values
      }
    } catch (e: JSONException) {
      null
    }

  /** The preference file of [profileKey], or null when a profile can't hold the setting. */
  private fun profilePrefs(context: Context, profileKey: String): Pair<SharedPreferences, String>? {
    val (file, key) = splitProfileKey(profileKey) ?: return null
    val name = importedName(context, file) ?: return null
    if (!isProfileFile(context, name) || isLeftOut(context, name, key, forProfile = true)) return null
    val storage = context.createDeviceProtectedStorageContext()
    return storage.getSharedPreferences(name, Context.MODE_PRIVATE) to key
  }

  /**
   * The value of [profileKey] now, or its default when it isn't set, or null when it has neither or
   * a profile can't hold it.
   */
  fun currentOrDefault(context: Context, profileKey: String): Any? {
    val (prefs, key) = profilePrefs(context, profileKey) ?: return null
    return prefs.all[key] ?: defaultValue(context, key)
  }

  /** Whether a settings profile can hold [profileKey]. */
  fun isProfileKey(context: Context, profileKey: String): Boolean = profilePrefs(context, profileKey) != null

  /** The value of [profileKey], or null when it isn't set or a profile can't hold it. */
  fun readProfileValue(context: Context, profileKey: String): Any? {
    val (prefs, key) = profilePrefs(context, profileKey) ?: return null
    return prefs.all[key]
  }

  /**
   * Whether [value] can be set as [profileKey]: a profile can hold the setting, and the value is of
   * the type SandFly reads it as. Null, for a setting that isn't set, always can.
   */
  fun canSetProfileValue(context: Context, profileKey: String, value: Any?): Boolean {
    val (prefs, key) = profilePrefs(context, profileKey) ?: return false
    return value == null || fits(context, prefs, key, value)
  }

  /**
   * Returns [json], a value from a file written by hand, such as a number or a list, as the type
   * the setting [profileKey] is saved as, or null when it can't be: the setting can't be in a
   * profile, or the value doesn't fit it. A number for a setting saved as text becomes its text.
   */
  fun profileValueFromJson(context: Context, profileKey: String, json: Any): Any? {
    val (prefs, key) = profilePrefs(context, profileKey) ?: return null
    val expected = prefs.all[key] ?: defaultValue(context, key)
    val value: Any =
      when (expected) {
        is Boolean -> json as? Boolean ?: (json as? String)?.toBooleanStrictOrNull()
        is Int -> wholeNumber(json)?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
        is Long -> wholeNumber(json)
        is Float -> (json as? Number)?.toFloat() ?: (json as? String)?.toFloatOrNull()
        is String ->
          when (json) {
            is String -> json
            is Number, is Boolean -> json.toString()
            else -> null
          }
        is Set<*> -> stringSet(json)
        // A setting with no value or default resource: JSON's own type.
        else ->
          when (json) {
            is Boolean, is String -> json
            is Int -> json
            is Long -> if (json in Int.MIN_VALUE..Int.MAX_VALUE) json.toInt() else json
            is Number -> json.toFloat()
            else -> stringSet(json)
          }
      } ?: return null
    return value.takeIf { fits(context, prefs, key, it) }
  }

  private fun wholeNumber(json: Any): Long? =
    when (json) {
      is Int -> json.toLong()
      is Long -> json
      is Number -> json.toDouble().takeIf { it % 1.0 == 0.0 && Math.abs(it) < 9.0E15 }?.toLong()
      is String -> json.toLongOrNull()
      else -> null
    }

  private fun stringSet(json: Any): Set<String>? {
    val array = json as? JSONArray ?: return null
    return (0 until array.length()).map { array.get(it).toString() }.toSet()
  }

  /**
   * The [profileKey] of the setting [key]: in the braille keyboard or braille display file when it
   * is there, otherwise in the main file.
   */
  fun profileKeyOf(context: Context, key: String): String {
    val storage = context.createDeviceProtectedStorageContext()
    for (file in PROFILE_FILES.sorted()) {
      if (storage.getSharedPreferences(file, Context.MODE_PRIVATE).contains(key)) return profileKey(file, key)
    }
    return profileKey(DEFAULT_PREFS, key)
  }

  /** Sets each of [values] by [profileKey], and removes those that are null. */
  fun writeProfileValues(context: Context, values: Map<String, Any?>) {
    val edits = mutableMapOf<SharedPreferences, SharedPreferences.Editor>()
    for ((profileKey, value) in values) {
      val (prefs, key) = profilePrefs(context, profileKey) ?: continue
      val editor = edits.getOrPut(prefs) { prefs.edit() }
      @Suppress("UNCHECKED_CAST")
      when (value) {
        null -> editor.remove(key)
        is Boolean -> editor.putBoolean(key, value)
        is Int -> editor.putInt(key, value)
        is Long -> editor.putLong(key, value)
        is Float -> editor.putFloat(key, value)
        is String -> editor.putString(key, value)
        is Set<*> -> editor.putStringSet(key, value as Set<String>)
      }
    }
    for (editor in edits.values) editor.commit()
  }

  /**
   * Replaces the settings in each preference file of [json] with the file's. Files that [json]
   * doesn't hold, API keys and the updater's state are kept. Returns false if [json] is not a
   * SandFly settings file, and then changes nothing.
   */
  fun import(context: Context, json: String): Boolean =
    // The imported settings are the settings without profiles, which are then applied to them.
    SettingsProfiles.replacingSettings(context) { importNow(context, json) }

  private fun importNow(context: Context, json: String): Boolean {
    val files =
      try {
        val root = JSONObject(json)
        if (isBacktalkFile(root)) {
          return importBacktalk(backtalkStores(context), root, context)
        }
        if (root.optString("format") != FORMAT || root.optInt("version") > VERSION) return false
        root.getJSONObject("files")
      } catch (e: JSONException) {
        return false
      }
    val edits = mutableListOf<Pair<SharedPreferences, Map<String, Any>>>()
    for ((storage, storageContext) in storages(context)) {
      val storageFiles = files.optJSONObject(storage) ?: continue
      for (file in storageFiles.keys()) {
        val name = importedName(context, file) ?: continue
        val values = storageFiles.optJSONObject(file) ?: return false
        val prefs = storageContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        val decoded = mutableMapOf<String, Any>()
        for (key in values.keys()) {
          if (isLeftOut(context, name, key, forProfile = false)) continue
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
        if (key !in values && !isLeftOut(context, null, key, forProfile = false)) {
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

  private fun storages(context: Context): List<Pair<String, Context>> =
    listOf(DEVICE to context.createDeviceProtectedStorageContext(), CREDENTIAL to context)

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
