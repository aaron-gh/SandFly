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

package io.github.aaron_gh.sandfly.addon

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONException
import org.json.JSONObject

/**
 * Updates the add-on from SandFly's stable GitHub releases, which carry sandfly-addon.apk. A check
 * runs at most once a day when SandFly connects to the add-on, and posts a notification when a
 * newer build is out; the update is installed from the add-on's screen, since Android shows the
 * install confirmation only for an app in the foreground.
 */
object AddonUpdater {
  private const val TAG = "SandFlyAddonUpdate"
  private const val RELEASE_URL = "https://api.github.com/repos/aaron-gh/SandFly/releases/latest"
  private const val APK_NAME = "sandfly-addon.apk"
  private const val PREFS = "updates"
  private const val KEY_LAST_CHECK = "last_check"
  private const val KEY_NOTIFIED_BUILD = "notified_build"
  private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
  private const val TIMEOUT_MS = 30_000
  /** The add-on is under 1 MB; anything far larger isn't it. */
  private const val MAX_APK_BYTES = 50L * 1024 * 1024
  private const val MAX_RELEASE_BYTES = 1024 * 1024
  private const val CHANNEL = "updates"
  private const val NOTIFICATION_ID = 1
  /** Version codes are this plus the build number, as SandFly's are. */
  private const val VERSION_BASE = 100_000_000L

  private val BUILD_LINE = Regex("""build:\s*(\d+)""")

  /** A newer build: its version name, build number and download address. */
  data class Update(val version: String, val build: Int, val url: String, val size: Long)

  sealed interface Result {
    data class Available(val update: Update) : Result

    data object Current : Result

    data object Failed : Result
  }

  private val executor by lazy { Executors.newSingleThreadExecutor { Thread(it, TAG) } }
  private val main by lazy { Handler(Looper.getMainLooper()) }

  /** This build's number. */
  fun build(context: Context): Long = versionCode(context) - VERSION_BASE

  /** Checks once a day at most, and posts a notification for a newer build not announced yet. */
  fun checkIfDue(context: Context) {
    val app = context.applicationContext
    val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val now = System.currentTimeMillis()
    val last = prefs.getLong(KEY_LAST_CHECK, 0)
    // A clock that moved back also makes a check due.
    if (now >= last && now - last < CHECK_INTERVAL_MS) return
    prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
    executor.execute {
      val result = safely { check(app) }
      val update = (result as? Result.Available)?.update ?: return@execute
      if (update.build <= prefs.getInt(KEY_NOTIFIED_BUILD, 0)) return@execute
      if (notify(app, update)) prefs.edit().putInt(KEY_NOTIFIED_BUILD, update.build).apply()
    }
  }

  /** Checks now, and gives the result to [done] on the main thread. */
  fun checkNow(context: Context, done: (Result) -> Unit) {
    val app = context.applicationContext
    executor.execute {
      val result = safely { check(app) }
      main.post { done(result) }
    }
  }

  /**
   * Downloads [update], checks that it is a newer add-on signed with the same key, and hands it to
   * Android's installer, which asks the user to confirm. [done] gets whether it got that far.
   */
  fun install(context: Context, update: Update, done: (Boolean) -> Unit) {
    val app = context.applicationContext
    executor.execute {
      val worked =
        try {
          val apk = download(app, update)
          try {
            verify(app, apk)
            startSession(app, apk)
            true
          } finally {
            apk.delete()
          }
        } catch (e: Throwable) {
          Log.w(TAG, "Update failed: $e")
          false
        }
      main.post { done(worked) }
    }
  }

  private fun safely(block: () -> Result): Result =
    try {
      block()
    } catch (e: Throwable) {
      Log.w(TAG, "Update check failed: $e")
      Result.Failed
    }

  private fun check(context: Context): Result {
    val connection = open(RELEASE_URL)
    try {
      connection.setRequestProperty("Accept", "application/vnd.github+json")
      if (connection.responseCode != HttpURLConnection.HTTP_OK) return Result.Failed
      val text = connection.inputStream.use { input ->
        val bytes = input.readNBytesCompat(MAX_RELEASE_BYTES + 1)
        if (bytes.size > MAX_RELEASE_BYTES) return Result.Failed
        String(bytes, Charsets.UTF_8)
      }
      return parse(text, build(context))
    } finally {
      connection.disconnect()
    }
  }

  /** The update in a GitHub release's JSON when its build is newer than [current]. */
  fun parse(json: String, current: Long): Result {
    val release =
      try {
        JSONObject(json)
      } catch (e: JSONException) {
        return Result.Failed
      }
    val build =
      release.optString("body").lineSequence().firstNotNullOfOrNull {
        BUILD_LINE.matchEntire(it.trim())?.groupValues?.get(1)?.toIntOrNull()
      } ?: return Result.Failed
    if (build <= current) return Result.Current
    val assets = release.optJSONArray("assets") ?: return Result.Current
    for (i in 0 until assets.length()) {
      val asset = assets.optJSONObject(i) ?: continue
      if (asset.optString("name") != APK_NAME) continue
      val url = asset.optString("browser_download_url")
      if (!url.startsWith("https://")) continue
      val version = release.optString("tag_name").removePrefix("v").ifEmpty { build.toString() }
      return Result.Available(Update(version, build, url, asset.optLong("size")))
    }
    // An older release without the add-on.
    return Result.Current
  }

  private fun download(context: Context, update: Update): File {
    if (update.size > MAX_APK_BYTES) throw IOException("The APK is too large: ${update.size}")
    val file = File(context.cacheDir, "addon-update.apk")
    val connection = open(update.url)
    try {
      if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        throw IOException("HTTP ${connection.responseCode}")
      }
      connection.inputStream.use { input ->
        file.outputStream().use { output ->
          val buffer = ByteArray(64 * 1024)
          var total = 0L
          while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_APK_BYTES) throw IOException("The APK is too large")
            output.write(buffer, 0, read)
          }
        }
      }
    } catch (e: Throwable) {
      file.delete()
      throw e
    } finally {
      connection.disconnect()
    }
    return file
  }

  /** Throws unless [apk] is this app, newer, and signed with the same key. */
  private fun verify(context: Context, apk: File) {
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0
    val info: PackageInfo =
      context.packageManager.getPackageArchiveInfo(apk.path, flags) ?: throw IOException("Not an APK")
    if (info.packageName != context.packageName) throw IOException("The APK is ${info.packageName}")
    if (versionCodeOf(info) <= versionCode(context)) throw IOException("The APK isn't newer")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val signers = info.signingInfo?.apkContentsSigners
      if (signers.isNullOrEmpty()) throw IOException("The APK has no signature")
      val same =
        signers.all {
          context.packageManager.hasSigningCertificate(context.packageName, it.toByteArray(), PackageManager.CERT_INPUT_RAW_X509)
        }
      if (!same) throw IOException("The APK is signed with another key")
    }
    // Before Android 9, Android itself refuses an update signed with another key.
  }

  private fun startSession(context: Context, apk: File) {
    val installer = context.packageManager.packageInstaller
    val params =
      PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
        setAppPackageName(context.packageName)
      }
    val sessionId = installer.createSession(params)
    try {
      installer.openSession(sessionId).use { session ->
        session.openWrite("addon.apk", 0, apk.length()).use { output ->
          apk.inputStream().use { it.copyTo(output) }
          session.fsync(output)
        }
        val intent = Intent(context, InstallReceiver::class.java)
        // The installer fills in the status, so the intent must be mutable.
        val flags =
          PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        session.commit(PendingIntent.getBroadcast(context, sessionId, intent, flags).intentSender)
      }
    } catch (e: Throwable) {
      installer.abandonSession(sessionId)
      throw e
    }
  }

  private fun notify(context: Context, update: Update): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      return false
    }
    val manager = context.getSystemService(NotificationManager::class.java) ?: return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      manager.createNotificationChannel(
        NotificationChannel(CHANNEL, context.getString(R.string.addon_updates_channel), NotificationManager.IMPORTANCE_DEFAULT),
      )
    }
    val open =
      PendingIntent.getActivity(
        context,
        0,
        Intent(context, AddonActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    @Suppress("DEPRECATION") // The channel is set on Android 8 and later.
    val builder =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) android.app.Notification.Builder(context, CHANNEL)
      else android.app.Notification.Builder(context)
    val notification =
      builder
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle(context.getString(R.string.addon_update_available, update.version))
        .setContentIntent(open)
        .setAutoCancel(true)
        .build()
    manager.notify(NOTIFICATION_ID, notification)
    return true
  }

  private fun open(url: String): HttpURLConnection {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = TIMEOUT_MS
    connection.readTimeout = TIMEOUT_MS
    connection.instanceFollowRedirects = true
    connection.setRequestProperty("User-Agent", "SandFly-Addon/${BuildConfig.VERSION_NAME}")
    return connection
  }

  private fun versionCode(context: Context): Long =
    versionCodeOf(context.packageManager.getPackageInfo(context.packageName, 0))

  @Suppress("DEPRECATION") // longVersionCode needs Android 9.
  private fun versionCodeOf(info: PackageInfo): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()

  /** Reads at most [limit] bytes. */
  private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (out.size() < limit) {
      val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
      if (read < 0) break
      out.write(buffer, 0, read)
    }
    return out.toByteArray()
  }
}
