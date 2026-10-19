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

package com.google.android.accessibility.talkback.update

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.migration.AppIdMove
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONException
import org.json.JSONObject

/** A stable release or development build that is newer than the running one. */
data class UpdateInfo(
  /** The build number, which counts the commits since SandFly began numbering its builds. */
  val build: Int,
  /** Where to download the APK. */
  val downloadUrl: String,
  /** The subjects of the commits that are newer than the running build, newest first. */
  val changes: List<String>,
  /** True if the build is SandFly, which installs as a new app. */
  val move: Boolean = false,
)

/**
 * Checks GitHub for a newer stable release, and with [Updater.isDevBuildsEnabled] also for a newer
 * development build.
 *
 * Stable releases are the latest release, and development builds the rolling "dev" pre-release.
 * Whichever has the higher build number is offered, so a development build is offered the next
 * stable release. Backtalk Aaron first checks SandFly's latest release, and offers it as a move
 * whenever there is one.
 *
 * CI writes the release notes as a line "build: <build number>", then one line per commit as "-
 * <7-character hash> <subject>", newest first. A release is newer only when its build number is
 * higher, so a local build of a newer commit is never offered an older build.
 */
object UpdateChecker {
  private const val TAG = "UpdateChecker"
  private const val RELEASES_URL = "https://api.github.com/repos/aaron-gh/SandFly/releases/"

  /** The latest stable release. */
  private const val STABLE_RELEASE_URL = RELEASES_URL + "latest"

  /** The rolling pre-release of development builds. */
  private const val DEV_RELEASE_URL = RELEASES_URL + "tags/dev"

  /** SandFly's latest stable release, which Backtalk Aaron moves to. */
  private const val MOVE_RELEASE_URL = STABLE_RELEASE_URL

  /** The phone build. */
  @VisibleForTesting const val PHONE_APK_NAME = "sandfly.apk"
  /** The watch build, so that a watch never installs the phone app. */
  @VisibleForTesting const val WATCH_APK_NAME = "sandfly-wear.apk"
  /** SandFly's phone and watch builds, for the move from Backtalk Aaron. */
  @VisibleForTesting const val MOVE_PHONE_APK_NAME = PHONE_APK_NAME
  @VisibleForTesting const val MOVE_WATCH_APK_NAME = WATCH_APK_NAME
  private const val SHORT_HASH_LENGTH = 7
  private const val MAX_CHANGES = 50
  private const val TIMEOUT_MS = 30_000

  private val BUILD_LINE = Regex("""build:\s*(\d+)""")

  /**
   * Returns the newer build, or null if this build is current. This blocks on the network.
   *
   * @throws IOException if GitHub cannot be reached or returns an error.
   * @throws JSONException if the release is not in the expected format.
   */
  @WorkerThread
  fun check(context: Context): UpdateInfo? {
    if (context.packageName == AppIdMove.OLD_PACKAGE) {
      checkMove(context)?.let {
        return it
      }
    }
    val urls = listOfNotNull(STABLE_RELEASE_URL, DEV_RELEASE_URL.takeIf { Updater.isDevBuildsEnabled(context) })
    val releases = urls.mapNotNull { fetchRelease(context, it) }
    if (releases.isEmpty()) {
      throw IOException("GitHub has no SandFly release")
    }
    return newest(releases, BuildConfig.COMMIT_COUNT, BuildConfig.COMMIT_HASH, apkName())
  }

  /** Returns the newest of the [releases] that is newer than the running build, or null. */
  @VisibleForTesting
  fun newest(
    releases: List<String>,
    currentBuild: Int,
    currentHash: String,
    apkName: String = PHONE_APK_NAME,
  ): UpdateInfo? =
    releases.mapNotNull { parseRelease(it, currentBuild, currentHash, apkName) }.maxByOrNull {
      it.build
    }

  /**
   * Returns SandFly's latest release, whatever its build number, since SandFly is always the way
   * forward. Returns null if there is none yet or it cannot be read, so Backtalk Aaron still gets
   * updates of its own.
   */
  @WorkerThread
  private fun checkMove(context: Context): UpdateInfo? =
    try {
      fetchRelease(context, MOVE_RELEASE_URL)?.let {
        parseMove(it, BuildConfig.COMMIT_HASH, moveApkName())
      }
    } catch (e: Exception) {
      LogUtils.w(TAG, "Cannot check for SandFly: %s", e)
      null
    }

  /** Returns the release at [url] as JSON, or null if GitHub has no such release. */
  @WorkerThread
  private fun fetchRelease(context: Context, url: String): String? {
    val connection = openConnection(context, url)
    try {
      connection.setRequestProperty("Accept", "application/vnd.github+json")
      val code = connection.responseCode
      if (code == HttpURLConnection.HTTP_NOT_FOUND) {
        return null
      }
      if (code != HttpURLConnection.HTTP_OK) {
        throw IOException("GitHub returned HTTP $code")
      }
      return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
      connection.disconnect()
    }
  }

  /** Parses a SandFly release as a move, which is offered whatever its build. */
  @VisibleForTesting
  fun parseMove(
    json: String,
    currentHash: String,
    apkName: String = MOVE_PHONE_APK_NAME,
  ): UpdateInfo? = parseRelease(json, currentBuild = 0, currentHash, apkName)?.copy(move = true)

  /** Opens a connection with the timeouts and user agent that GitHub requests should use. */
  fun openConnection(context: Context, url: String): HttpURLConnection {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = TIMEOUT_MS
    connection.readTimeout = TIMEOUT_MS
    connection.setRequestProperty("User-Agent", "SandFly/${versionName(context)}")
    return connection
  }

  private fun apkName(): String =
    if (FormFactorUtils.isAndroidWear()) WATCH_APK_NAME else PHONE_APK_NAME

  private fun moveApkName(): String =
    if (FormFactorUtils.isAndroidWear()) MOVE_WATCH_APK_NAME else MOVE_PHONE_APK_NAME

  @VisibleForTesting
  fun parseRelease(
    json: String,
    currentBuild: Int,
    currentHash: String,
    apkName: String = PHONE_APK_NAME,
  ): UpdateInfo? {
    val release = JSONObject(json)
    val lines = release.optString("body").lines().map { it.trim() }
    val build =
      lines.firstNotNullOfOrNull { BUILD_LINE.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
        ?: throw JSONException("The release has no build number")
    if (build <= currentBuild) {
      return null
    }

    val assets = release.getJSONArray("assets")
    val downloadUrl =
      (0 until assets.length())
        .map { assets.getJSONObject(it) }
        .firstOrNull { it.optString("name") == apkName }
        ?.getString("browser_download_url")
        ?: throw JSONException("The release has no $apkName")

    val commits = lines.filter { it.startsWith("- ") }.map { it.removePrefix("- ") }
    val shortHash = currentHash.take(SHORT_HASH_LENGTH)
    val currentIndex =
      if (shortHash.length == SHORT_HASH_LENGTH) {
        commits.indexOfFirst { it.substringBefore(' ') == shortHash }
      } else {
        -1
      }
    val newCommits =
      if (currentIndex >= 0) {
        commits.subList(0, currentIndex)
      } else {
        commits.take((build - currentBuild).coerceAtMost(MAX_CHANGES))
      }
    return UpdateInfo(build, downloadUrl, newCommits.map { it.substringAfter(' ') })
  }

  private fun versionName(context: Context): String =
    try {
      context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
      ""
    }
}
