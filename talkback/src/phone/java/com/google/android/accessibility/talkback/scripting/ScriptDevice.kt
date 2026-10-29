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

package com.google.android.accessibility.talkback.scripting

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject

/** The device's state and Android's apps and settings screens, for scripts. */
class ScriptDevice(private val context: Context) {

  fun info(): JSONObject {
    val metrics = context.resources.displayMetrics
    val configuration = context.resources.configuration
    return jsonObject(
      "manufacturer" to Build.MANUFACTURER,
      "model" to Build.MODEL,
      "androidVersion" to Build.VERSION.RELEASE,
      "sdk" to Build.VERSION.SDK_INT,
      "sandflyVersion" to
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
          .getOrNull(),
      "screen" to
        jsonObject(
          "width" to metrics.widthPixels,
          "height" to metrics.heightPixels,
          "density" to metrics.density.toDouble(),
          "orientation" to
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape"
            else "portrait",
        ),
    )
  }

  fun battery(): JSONObject {
    val status =
      context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        ?: return jsonObject()
    val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    val state = status.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    return jsonObject(
      "level" to if (level >= 0 && scale > 0) level * 100 / scale else null,
      "charging" to
        (state == BatteryManager.BATTERY_STATUS_CHARGING ||
          state == BatteryManager.BATTERY_STATUS_FULL),
      "full" to (state == BatteryManager.BATTERY_STATUS_FULL),
      "plugged" to PLUGS[status.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)],
    )
  }

  fun network(): JSONObject {
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    val capabilities = connectivity?.let { it.getNetworkCapabilities(it.activeNetwork) }
    fun has(transport: Int) = capabilities?.hasTransport(transport) == true
    return jsonObject(
      "connected" to
        (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true),
      "wifi" to has(NetworkCapabilities.TRANSPORT_WIFI),
      "cellular" to has(NetworkCapabilities.TRANSPORT_CELLULAR),
      "ethernet" to has(NetworkCapabilities.TRANSPORT_ETHERNET),
      "vpn" to has(NetworkCapabilities.TRANSPORT_VPN),
      "metered" to (connectivity?.isActiveNetworkMetered == true),
    )
  }

  fun volume(args: JSONObject): JSONObject {
    val audio = audio()
    val stream = stream(args)
    return jsonObject(
      "level" to audio.getStreamVolume(stream),
      "max" to audio.getStreamMaxVolume(stream),
      "min" to minVolume(audio, stream),
      "muted" to audio.isStreamMute(stream),
    )
  }

  fun setVolume(args: JSONObject) {
    val audio = audio()
    val stream = stream(args)
    val level =
      args.optInt("level").coerceIn(minVolume(audio, stream), audio.getStreamMaxVolume(stream))
    permitted { audio.setStreamVolume(stream, level, 0) }
  }

  fun ringerMode(): String = RINGER_MODES.entries.first { it.value == audio().ringerMode }.key

  fun setRingerMode(args: JSONObject) {
    val name = args.optString("mode")
    val mode =
      RINGER_MODES[name]
        ?: apiError("Unknown ringer mode $name. Modes: ${RINGER_MODES.keys.joinToString()}")
    permitted { audio().ringerMode = mode }
  }

  fun openUrl(args: JSONObject): Boolean {
    val uri = Uri.parse(args.optString("url"))
    apiCheck(uri.scheme?.lowercase() in URL_SCHEMES) {
      "openUrl takes addresses that start with ${URL_SCHEMES.joinToString { "$it:" }}"
    }
    return start(Intent(Intent.ACTION_VIEW, uri))
  }

  fun share(args: JSONObject): Boolean {
    val send =
      Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, args.optString("text"))
    args.optString("subject").ifEmpty { null }?.let { send.putExtra(Intent.EXTRA_SUBJECT, it) }
    return start(Intent.createChooser(send, args.optString("title").ifEmpty { null }))
  }

  fun openSettings(args: JSONObject): Boolean {
    val page = args.optString("page", "settings")
    val packageName = args.optString("package").ifEmpty { null }
    val intent =
      when (page) {
        "app" ->
          Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", packageName ?: apiError("app needs a package"), null))
        "appNotifications" ->
          Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(
              Settings.EXTRA_APP_PACKAGE,
              packageName ?: apiError("appNotifications needs a package"),
            )
        else ->
          Intent(
            SETTINGS_PAGES[page]
              ?: apiError(
                "Unknown settings page $page. Pages: app, appNotifications, " +
                  SETTINGS_PAGES.keys.joinToString()
              )
          )
      }
    return start(intent)
  }

  fun startActivity(args: JSONObject): Boolean = start(intentOf(args))

  fun sendBroadcast(args: JSONObject) {
    val intent = intentOf(args)
    apiCheck(intent.`package` != null || intent.component != null) {
      "sendBroadcast needs a package"
    }
    context.sendBroadcast(intent)
  }

  /** The apps that have a launcher icon, by name. */
  fun apps(): JSONArray {
    val packageManager = context.packageManager
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return JSONArray(
      packageManager
        .queryIntentActivities(launcher, 0)
        .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
        .distinctBy { it.first }
        .sortedBy { it.second.lowercase() }
        .map { (packageName, label) -> jsonObject("package" to packageName, "label" to label) }
    )
  }

  fun appLabel(packageName: String?): String? =
    packageName?.let {
      runCatching {
          context.packageManager
            .getApplicationLabel(context.packageManager.getApplicationInfo(it, 0))
            .toString()
        }
        .getOrNull()
    }

  private fun intentOf(args: JSONObject): Intent {
    val intent = Intent()
    args.optString("action").ifEmpty { null }?.let(intent::setAction)
    val data = args.optString("data").ifEmpty { null }?.let(Uri::parse)
    val type = args.optString("type").ifEmpty { null }
    when {
      data != null && type != null -> intent.setDataAndType(data, type)
      data != null -> intent.data = data
      type != null -> intent.type = type
    }
    val packageName = args.optString("package").ifEmpty { null }
    val className = args.optString("className").ifEmpty { null }
    when {
      className != null ->
        intent.setClassName(packageName ?: apiError("className needs a package"), className)
      packageName != null -> intent.setPackage(packageName)
    }
    args.optJSONArray("categories").strings().forEach(intent::addCategory)
    args.optJSONObject("extras")?.let { intent.putExtras(extrasOf(it)) }
    // Scripts change nothing in SandFly itself, and SandFly's own components trust their callers.
    val target = intent.component?.packageName ?: intent.`package`
    apiCheck(target != context.packageName) { "Scripts can't start SandFly's own screens" }
    apiCheck(intent.action != null || intent.component != null) {
      "An intent needs an action or a className"
    }
    return intent
  }

  private fun extrasOf(json: JSONObject): Bundle {
    val extras = Bundle()
    for (key in json.keys()) {
      when (val value = json.get(key)) {
        is Boolean -> extras.putBoolean(key, value)
        is Int -> extras.putInt(key, value)
        is Long -> extras.putLong(key, value)
        is Number -> extras.putDouble(key, value.toDouble())
        is String -> extras.putString(key, value)
        else -> apiError("Extra $key must be text, a number or true or false")
      }
    }
    return extras
  }

  private fun start(intent: Intent): Boolean {
    // Explicit intents to SandFly were refused above. A chooser or implicit intent that only
    // SandFly answers is refused here.
    val resolved = intent.resolveActivity(context.packageManager)
    if (resolved?.packageName == context.packageName) {
      apiError("Scripts can't start SandFly's own screens")
    }
    return try {
      context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      true
    } catch (e: ActivityNotFoundException) {
      false
    } catch (e: SecurityException) {
      apiError(e.message ?: "Android refused to open it")
    }
  }

  private fun minVolume(audio: AudioManager, stream: Int): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(stream) else 0

  // Do Not Disturb keeps apps without its access from changing some volumes and ringer modes.
  private inline fun permitted(block: () -> Unit) =
    try {
      block()
    } catch (e: SecurityException) {
      apiError(e.message ?: "Android refused the change")
    }

  private fun audio(): AudioManager =
    context.getSystemService(AudioManager::class.java) ?: apiError("There is no audio service")

  private fun stream(args: JSONObject): Int {
    val name = args.optString("stream", "media")
    return STREAMS[name] ?: apiError("Unknown stream $name. Streams: ${STREAMS.keys.joinToString()}")
  }

  private companion object {
    val URL_SCHEMES = setOf("http", "https", "mailto", "tel", "sms", "smsto", "geo", "market")

    val PLUGS =
      mapOf(
        BatteryManager.BATTERY_PLUGGED_AC to "ac",
        BatteryManager.BATTERY_PLUGGED_USB to "usb",
        BatteryManager.BATTERY_PLUGGED_WIRELESS to "wireless",
        BatteryManager.BATTERY_PLUGGED_DOCK to "dock",
      )

    val STREAMS =
      linkedMapOf(
        "media" to AudioManager.STREAM_MUSIC,
        "ring" to AudioManager.STREAM_RING,
        "notification" to AudioManager.STREAM_NOTIFICATION,
        "alarm" to AudioManager.STREAM_ALARM,
        "call" to AudioManager.STREAM_VOICE_CALL,
        "system" to AudioManager.STREAM_SYSTEM,
        "accessibility" to AudioManager.STREAM_ACCESSIBILITY,
      )

    val RINGER_MODES =
      linkedMapOf(
        "normal" to AudioManager.RINGER_MODE_NORMAL,
        "vibrate" to AudioManager.RINGER_MODE_VIBRATE,
        "silent" to AudioManager.RINGER_MODE_SILENT,
      )

    val SETTINGS_PAGES =
      linkedMapOf(
        "settings" to Settings.ACTION_SETTINGS,
        "wifi" to Settings.ACTION_WIFI_SETTINGS,
        "bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
        "mobileData" to Settings.ACTION_DATA_ROAMING_SETTINGS,
        "airplaneMode" to Settings.ACTION_AIRPLANE_MODE_SETTINGS,
        "nfc" to Settings.ACTION_NFC_SETTINGS,
        "location" to Settings.ACTION_LOCATION_SOURCE_SETTINGS,
        "sound" to Settings.ACTION_SOUND_SETTINGS,
        "display" to Settings.ACTION_DISPLAY_SETTINGS,
        "battery" to Intent.ACTION_POWER_USAGE_SUMMARY,
        "batterySaver" to Settings.ACTION_BATTERY_SAVER_SETTINGS,
        "accessibility" to Settings.ACTION_ACCESSIBILITY_SETTINGS,
        "apps" to Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS,
        "defaultApps" to Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
        "notifications" to "android.settings.NOTIFICATION_SETTINGS",
        "doNotDisturb" to Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS,
        "dateTime" to Settings.ACTION_DATE_SETTINGS,
        "language" to Settings.ACTION_LOCALE_SETTINGS,
        "keyboard" to Settings.ACTION_INPUT_METHOD_SETTINGS,
        "storage" to Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
        "security" to Settings.ACTION_SECURITY_SETTINGS,
        "privacy" to Settings.ACTION_PRIVACY_SETTINGS,
        "developer" to Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
        "about" to Settings.ACTION_DEVICE_INFO_SETTINGS,
        "castScreen" to Settings.ACTION_CAST_SETTINGS,
        "hotspot" to "android.settings.TETHER_SETTINGS",
      )
  }
}
