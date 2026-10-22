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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.migration.MigrationFiles
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.io.File
import java.io.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Exports SandFly's settings to a file and imports them from one, so that a phone can be set up
 * like another. The file holds every preference file, in device-protected and in
 * credential-protected storage, except the updater's state, the state of the move from Backtalk
 * Aaron, and API keys.
 *
 * Custom labels, installed scripts and sound themes are files of their own, and are not in it.
 *
 * A settings file exported from Backtalk can be imported too.
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

  /** API keys stay on the phone, since the file can be shared or synced anywhere. */
  @VisibleForTesting
  val SECRET_KEYS = setOf("pref_gemini_api_key", "pref_ai_service_api_key")

  private val SKIPPED_FILES = setOf(MigrationFiles.STATE_PREFS)

  /** Files of libraries, such as Google Play services and WebView, which hold their own state. */
  private val LIBRARY_PREFIXES = listOf("com.google.", "androidx.", "WebView", "Firebase")

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
        uri?.let { write(fragment.requireContext(), it) }
      }
    val open =
      fragment.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { read(fragment.requireContext(), it) }
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
    toast(context, if (imported) R.string.settings_import_done else R.string.settings_import_failed)
  }

  /** Returns every exported setting as JSON. */
  fun export(context: Context): String {
    val files = JSONObject()
    for ((storage, storageContext) in storages(context)) {
      val storageFiles = JSONObject()
      for (name in prefsNames(context, storageContext)) {
        val prefs = storageContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        val values = JSONObject()
        for ((key, value) in prefs.all) {
          if (key in SECRET_KEYS || isSkipped(context, name, key)) continue
          encode(value)?.let { values.put(key, it) }
        }
        storageFiles.put(exportedName(context, name), values)
      }
      files.put(storage, storageFiles)
    }
    return JSONObject()
      .put("format", FORMAT)
      .put("version", VERSION)
      .put("files", files)
      .toString(2)
  }

  /**
   * Replaces the settings in each preference file of [json] with the file's. Files that [json]
   * doesn't hold, API keys and the updater's state are kept. Returns false if [json] is not a
   * SandFly settings file, and then changes nothing.
   */
  fun import(context: Context, json: String): Boolean {
    val files =
      try {
        val root = JSONObject(json)
        if (isBacktalkFile(root)) {
          return importBacktalk(backtalkStores(context), root)
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
        val decoded = mutableMapOf<String, Any>()
        for (key in values.keys()) {
          if (key in SECRET_KEYS || isSkipped(context, name, key)) continue
          decoded[key] = decode(values.optJSONObject(key)) ?: return false
        }
        edits += storageContext.getSharedPreferences(name, Context.MODE_PRIVATE) to decoded
      }
    }
    for ((prefs, values) in edits) {
      val editor = prefs.edit()
      for (key in prefs.all.keys) {
        if (key !in values && key !in SECRET_KEYS && !isSkipped(context, null, key)) {
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
  fun importBacktalk(stores: Map<String, SharedPreferences>, root: JSONObject): Boolean {
    if (root.optInt("format") != BACKTALK_FORMAT) return false
    val storesJson = root.optJSONObject("stores") ?: return false
    val edits = mutableListOf<Pair<SharedPreferences, Map<String, Any>>>()
    for ((name, prefs) in stores) {
      val entries = storesJson.optJSONObject(name) ?: continue
      val decoded = mutableMapOf<String, Any>()
      for (key in entries.keys()) {
        if (!isBacktalkSettingKept(key)) continue
        decoded[key] = decodeBacktalk(entries.optJSONObject(key)) ?: return false
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
    return true
  }

  private fun isBacktalkSettingKept(key: String) =
    key !in SECRET_KEYS &&
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

  private fun storages(context: Context): List<Pair<String, Context>> =
    listOf(DEVICE to context.createDeviceProtectedStorageContext(), CREDENTIAL to context)

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

  /** Whether [name] is a file that isn't exported: the move's state, or a library's. */
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
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
  }
}
