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

package com.google.android.accessibility.talkback.gesture

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.util.Log
import android.util.Xml
import androidx.annotation.VisibleForTesting
import androidx.core.os.ConfigurationCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.net.URISyntaxException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Actions that open an app shortcut, as a long press on the app's icon on the home screen offers.
 *
 * Two kinds of shortcut are listed. Static shortcuts are read from each home screen app's shortcuts
 * XML file; their actions save [PREFIX], the package name, "/" and the shortcut's ID, and the file
 * is read again to open one. Added shortcuts are those an app's create shortcut activity returned,
 * as launchers add them to the home screen; they are kept under [KEY], and their actions save
 * [ADDED_PREFIX] and the ID SandFly gave them. Shortcuts that apps publish while they run are only
 * readable by the default launcher, so they aren't listed.
 *
 * Only activities are started, never the shortcut's selector, and never SandFly's own activities,
 * activities that aren't exported, or activities protected by a permission that the shortcut's app
 * doesn't hold, since they start with SandFly's identity rather than the app's.
 */
object AppShortcutActions {
  private const val TAG = "AppShortcutActions"

  /** Starts every saved value of the actions of static shortcuts. It must never change once released. */
  private const val PREFIX = "APP_SHORTCUT:"

  /** Starts every saved value of the actions of added shortcuts. It must never change once released. */
  private const val ADDED_PREFIX = "ADDED_APP_SHORTCUT:"

  /** The preference that keeps the added shortcuts, as a JSON array. */
  const val KEY = "pref_added_app_shortcuts"

  /** Flags that would give the started activity SandFly's access to the intent's URIs. */
  private const val GRANT_FLAGS =
    Intent.FLAG_GRANT_READ_URI_PERMISSION or
      Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
      Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
      Intent.FLAG_GRANT_PREFIX_URI_PERMISSION

  /** An app shortcut, and the action that opens it. */
  data class Shortcut(val label: String, val value: String)

  /**
   * A shortcut that an app's create shortcut activity returned: the ID SandFly gave it, its name,
   * the app's package name, and its intent as [Intent.toUri] gives it with
   * [Intent.URI_INTENT_SCHEME].
   */
  data class Added(val id: String, val name: String, val packageName: String, val uri: String)

  /** Returns the action that opens the static shortcut [shortcutId] of [packageName]. */
  @JvmStatic
  fun staticValue(packageName: String, shortcutId: String): String =
    "$PREFIX$packageName/$shortcutId"

  /** Returns the action that opens the added shortcut [id]. */
  @JvmStatic fun addedValue(id: String): String = ADDED_PREFIX + id

  /** Returns whether [value] is an action that opens an app shortcut. */
  @JvmStatic
  fun isAction(value: String?): Boolean = staticRef(value) != null || addedId(value) != null

  /** Returns the package name and shortcut ID of a static shortcut's action, or null. */
  @VisibleForTesting
  internal fun staticRef(value: String?): Pair<String, String>? {
    if (value == null || !value.startsWith(PREFIX)) return null
    val rest = value.substring(PREFIX.length)
    // Package names have no "/", so the first one ends it; the shortcut ID may have more.
    val slash = rest.indexOf('/')
    if (slash <= 0 || slash == rest.length - 1) return null
    return rest.substring(0, slash) to rest.substring(slash + 1)
  }

  /** Returns the ID of an added shortcut's action, or null. */
  @VisibleForTesting
  internal fun addedId(value: String?): String? =
    if (value != null && value.startsWith(ADDED_PREFIX)) {
      value.substring(ADDED_PREFIX.length).takeIf { it.isNotEmpty() }
    } else {
      null
    }

  /** Returns the added shortcuts, then the static shortcuts of the home screen apps, by name. */
  @JvmStatic
  fun shortcuts(context: Context): List<Shortcut> {
    val packageManager = context.packageManager
    val added =
      readAdded(SharedPreferencesUtils.getSharedPreferences(context)).map {
        Shortcut(label(context, appName(packageManager, it.packageName), it.name), addedValue(it.id))
      }
    val static =
      shortcutActivities(context, packageName = null)
        .groupBy { it.packageName }
        .flatMap { (packageName, activities) ->
          val app = appName(packageManager, packageName)
          staticShortcuts(context, packageName, activities).map {
            Shortcut(label(context, app, it.label), staticValue(packageName, it.id))
          }
        }
    return (added + static)
      .distinctBy { it.value }
      .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, Shortcut::label).thenBy(Shortcut::value))
  }

  /**
   * Returns the app's name and the shortcut's name, or null when [value] isn't one of these actions,
   * or its shortcut was deleted or is no longer in its app.
   */
  @JvmStatic
  fun label(context: Context, value: String?): String? {
    staticRef(value)?.let { (packageName, id) ->
      val shortcut = staticShortcuts(context, packageName).firstOrNull { it.id == id } ?: return null
      return label(context, appName(context.packageManager, packageName), shortcut.label)
    }
    addedId(value)?.let { id ->
      val added = findAdded(context, id) ?: return null
      return label(context, appName(context.packageManager, added.packageName), added.name)
    }
    return null
  }

  private fun label(context: Context, app: String, shortcut: String): String =
    context.getString(R.string.app_shortcut_label, app, shortcut)

  /**
   * Opens the shortcut of the action saved as [value]. Returns false when the shortcut is gone, or
   * its activity can't or mustn't be started.
   */
  @JvmStatic
  fun open(context: Context, value: String): Boolean {
    val intents = intentsOf(context, value)
    if (intents.isNullOrEmpty()) {
      Log.w(TAG, "No shortcut to open for $value")
      return false
    }
    return try {
      context.startActivities(intents.toTypedArray())
      true
    } catch (e: RuntimeException) {
      // ActivityNotFoundException, SecurityException, or an intent with a file URI.
      Log.w(TAG, "Could not open $value", e)
      false
    }
  }

  /** Returns the intents that open the shortcut of [value], ready to start, or null. */
  private fun intentsOf(context: Context, value: String): List<Intent>? {
    staticRef(value)?.let { (packageName, id) ->
      val shortcut = staticShortcuts(context, packageName).firstOrNull { it.id == id } ?: return null
      val intents =
        shortcut.intents.map {
          val intent = sanitized(it, context.packageName) ?: return null
          if (!startable(context, intent, packageName)) return null
          intent
        }
      // As Android starts static shortcuts.
      intents.firstOrNull()
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      return intents
    }
    addedId(value)?.let { id ->
      val added = findAdded(context, id) ?: return null
      val intent = parse(added.uri)?.let { sanitized(it, context.packageName) } ?: return null
      if (!startable(context, intent, added.packageName)) return null
      return listOf(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    return null
  }

  /** Returns the intent saved as [uri], or null when it isn't one. */
  @JvmStatic
  fun parse(uri: String): Intent? =
    try {
      Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
    } catch (e: URISyntaxException) {
      Log.w(TAG, "Bad shortcut intent", e)
      null
    } catch (e: RuntimeException) {
      // Restored settings may hold anything.
      Log.w(TAG, "Bad shortcut intent", e)
      null
    }

  /**
   * Returns a copy of [intent] without a selector, clip data or URI permission grants, or null when
   * it names a component or package of [ownPackage], SandFly's.
   */
  @VisibleForTesting
  internal fun sanitized(intent: Intent, ownPackage: String): Intent? {
    val copy = Intent(intent)
    copy.selector = null
    copy.clipData = null
    copy.flags = copy.flags and GRANT_FLAGS.inv()
    if (copy.component?.packageName == ownPackage || copy.`package` == ownPackage) {
      return null
    }
    return copy
  }

  /** Returns whether [intent], a shortcut of the app [sourcePackage], is one SandFly opens. */
  @JvmStatic
  fun openable(context: Context, intent: Intent, sourcePackage: String): Boolean =
    sanitized(intent, context.packageName)?.let { startable(context, it, sourcePackage) } == true

  /**
   * Returns whether [intent] starts an exported activity outside SandFly that needs no permission,
   * or one that [sourcePackage], the shortcut's app, holds.
   */
  private fun startable(context: Context, intent: Intent, sourcePackage: String): Boolean {
    val packageManager = context.packageManager
    val component = intent.component
    val activity =
      if (component != null) {
        try {
          packageManager.getActivityInfo(component, /* flags= */ 0)
        } catch (e: PackageManager.NameNotFoundException) {
          return false
        }
      } else {
        // Android starts implicit intents only with activities in the default category.
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
          ?: return false
      }
    if (activity.packageName == context.packageName || !activity.exported) {
      return false
    }
    val permission = activity.permission ?: return true
    return packageManager.checkPermission(permission, sourcePackage) ==
      PackageManager.PERMISSION_GRANTED
  }

  // Static shortcuts.

  private class Cached(val stamp: String, val shortcuts: List<AppShortcutXml.Shortcut>)

  /** The static shortcuts of each package read so far, until it updates or the locale changes. */
  private val cache = ConcurrentHashMap<String, Cached>()

  /** Returns the home screen activities with a shortcuts XML file, of [packageName] or all apps. */
  private fun shortcutActivities(context: Context, packageName: String?): List<ActivityInfo> =
    context.packageManager
      .queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName),
        PackageManager.GET_META_DATA,
      )
      .map { it.activityInfo }
      .filter {
        it.packageName != context.packageName && it.metaData?.containsKey(AppShortcutXml.META_DATA) == true
      }

  /** Returns the static shortcuts of [packageName] that can be opened, in the app's order. */
  private fun staticShortcuts(
    context: Context,
    packageName: String,
    activities: List<ActivityInfo>? = null,
  ): List<AppShortcutXml.Shortcut> {
    val packageManager = context.packageManager
    val updated =
      try {
        packageManager.getPackageInfo(packageName, /* flags= */ 0).lastUpdateTime
      } catch (e: PackageManager.NameNotFoundException) {
        cache.remove(packageName)
        return emptyList()
      }
    val stamp =
      "$updated ${ConfigurationCompat.getLocales(context.resources.configuration).toLanguageTags()}"
    cache[packageName]?.takeIf { it.stamp == stamp }?.let {
      return it.shortcuts
    }
    val shortcuts =
      (activities ?: shortcutActivities(context, packageName))
        .flatMap { read(context, it) }
        .distinctBy { it.id }
        .filter { shortcut -> shortcut.intents.all { openable(context, it, packageName) } }
    cache[packageName] = Cached(stamp, shortcuts)
    return shortcuts
  }

  /** Reads the shortcuts XML file of [activity], or returns no shortcuts when it can't. */
  private fun read(context: Context, activity: ActivityInfo): List<AppShortcutXml.Shortcut> {
    val packageManager = context.packageManager
    return try {
      val resources = packageManager.getResourcesForApplication(activity.applicationInfo)
      val parser = activity.loadXmlMetaData(packageManager, AppShortcutXml.META_DATA) ?: return emptyList()
      try {
        AppShortcutXml.parse(parser, { text(resources, it) }) {
          // Android ignores shortcut intents without an action.
          Intent.parseIntent(resources, it, Xml.asAttributeSet(it)).takeIf { intent ->
            !intent.action.isNullOrEmpty()
          }
        }
      } finally {
        parser.close()
      }
    } catch (e: Exception) {
      // A missing or broken file in another app.
      Log.w(TAG, "Could not read the shortcuts of ${activity.packageName}", e)
      emptyList()
    }
  }

  /** Returns the text of an attribute [value] of a compiled XML file of the app of [resources]. */
  private fun text(resources: Resources, value: String?): String? {
    val id = AppShortcutXml.resourceId(value)
    if (id != null) {
      return try {
        resources.getString(id)
      } catch (e: Resources.NotFoundException) {
        null
      }
    }
    // A theme attribute has no text without the app's theme.
    return value?.takeUnless { it.startsWith("?") }
  }

  private fun appName(packageManager: PackageManager, packageName: String): String =
    try {
      packageManager.getApplicationInfo(packageName, /* flags= */ 0).loadLabel(packageManager).toString()
    } catch (e: PackageManager.NameNotFoundException) {
      packageName
    }

  // Added shortcuts.

  private fun findAdded(context: Context, id: String): Added? =
    readAdded(SharedPreferencesUtils.getSharedPreferences(context)).firstOrNull { it.id == id }

  /** Returns a new ID for an added shortcut. */
  @JvmStatic fun newId(): String = UUID.randomUUID().toString()

  /** Returns the added shortcuts, in the order they were added. */
  @JvmStatic
  fun readAdded(prefs: SharedPreferences): List<Added> = fromJson(prefs.getString(KEY, null))

  /** Keeps [shortcuts] as the added shortcuts. */
  @JvmStatic
  fun writeAdded(prefs: SharedPreferences, shortcuts: List<Added>) {
    prefs.edit().putString(KEY, toJson(shortcuts)).apply()
  }

  @VisibleForTesting
  internal fun fromJson(json: String?): List<Added> {
    if (json.isNullOrEmpty()) return emptyList()
    return try {
      val array = JSONArray(json)
      (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        val id = item.optString("id")
        val uri = item.optString("intent")
        if (id.isEmpty() || uri.isEmpty()) return@mapNotNull null
        Added(id, item.optString("name"), item.optString("package"), uri)
      }
    } catch (e: JSONException) {
      Log.w(TAG, "Bad added app shortcuts", e)
      emptyList()
    }
  }

  @VisibleForTesting
  internal fun toJson(shortcuts: List<Added>): String {
    val array = JSONArray()
    for (shortcut in shortcuts) {
      array.put(
        JSONObject()
          .put("id", shortcut.id)
          .put("name", shortcut.name)
          .put("package", shortcut.packageName)
          .put("intent", shortcut.uri)
      )
    }
    return array.toString()
  }
}
