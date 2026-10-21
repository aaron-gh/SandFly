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

package com.google.android.accessibility.talkback.quicksettings

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.google.android.accessibility.talkback.shizuku.ShizukuGrants
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.IOException

/** An app's tile, which apps offer for the system's quick settings. */
data class AppTile(val component: ComponentName, val label: CharSequence, val appName: CharSequence)

/**
 * What SystemUI shows for a tile in the system's quick settings. [state] is one of [Tile]'s
 * states. [isSwitch] is whether SystemUI presents the tile as a switch rather than a button.
 */
data class TileState(
  val spec: String,
  val label: String?,
  val secondaryLabel: String?,
  val state: Int,
  val isSwitch: Boolean,
)

/**
 * The tiles of the system's quick settings. Android lets only SystemUI bind to apps' tiles, so
 * SandFly reads their state from SystemUI's dump, which needs the DUMP permission and usage access,
 * and clicks them
 * through Shizuku with `cmd statusbar click-tile`. Both work only for tiles that are in the
 * system's quick settings.
 */
object SystemTiles {
  private const val TAG = "SystemTiles"
  /** The tiles that installed apps offer, by label. */
  fun appTiles(context: Context): List<AppTile> {
    val packageManager = context.packageManager
    return packageManager
      .queryIntentServices(Intent(TileService.ACTION_QS_TILE), 0)
      .map { it.serviceInfo }
      .filter { it.permission == Manifest.permission.BIND_QUICK_SETTINGS_TILE }
      .map { info ->
        AppTile(
          ComponentName(info.packageName, info.name),
          info.loadLabel(packageManager),
          info.applicationInfo.loadLabel(packageManager),
        )
      }
      .sortedBy { it.label.toString().lowercase() }
  }

  /** The permissions that reading SystemUI's dump needs. */
  val GRANTS = listOf(ShizukuGrants.Grant.DUMP, ShizukuGrants.Grant.USAGE_STATS)

  fun canRead(context: Context): Boolean = GRANTS.all { it.isGranted(context) }

  /** The spec of an app's tile in SystemUI's tile list. */
  fun spec(component: ComponentName): String = "custom(${component.flattenToShortString()})"

  /**
   * Reads the tiles from SystemUI's dump, by spec. Runs `dumpsys`, so it must not run on the main
   * thread. Returns null if the dump can't be read.
   */
  fun read(): Map<String, TileState>? {
    val text =
      Dumpsys.run("activity", "service", "com.android.systemui/.SystemUIService", "QSTileHost")
        ?: return null
    return parse(text).also { tiles ->
      if (tiles.isEmpty()) {
        LogUtils.w(TAG, "No tiles in SystemUI's dump: %s", text.take(200))
      } else {
        LogUtils.d(TAG, "Read %d tiles from SystemUI's dump", tiles.size)
      }
    }
  }

  private val LABEL = Regex(""",label=(.*?),secondaryLabel=""")
  private val SECONDARY_LABEL = Regex(""",secondaryLabel=(.*?),contentDescription=""")
  private val CLASS_NAME = Regex(""",expandedAccessibilityClassName=([^,]*),""")
  private val STATE = Regex(""",state=(\d+)[,\]]""")
  private val LINE_BREAK = Regex("""\s*\n\s*""")
  private val DIRECTION_MARKS = Regex("""[\x{200E}\x{200F}\x{202A}-\x{202E}\x{2066}-\x{2069}]""")

  /**
   * Reads the tiles from the text of SystemUI's QSTileHost dump, which prints each tile's
   * `QSTile$State[spec=…,label=…,…,state=…]`. Some phones wrap long labels onto new lines, which
   * become spaces.
   */
  fun parse(dump: String): Map<String, TileState> {
    val result = LinkedHashMap<String, TileState>()
    val text = dump.replace(LINE_BREAK, " ")
    for (entry in text.split("State[spec=").drop(1)) {
      val spec = entry.substringBefore(',').trim()
      val state = STATE.find(entry)?.groupValues?.get(1)?.toIntOrNull() ?: continue
      if (spec.isEmpty() || spec in result) continue
      result[spec] =
        TileState(
          spec = spec,
          label = clean(LABEL.find(entry)?.groupValues?.get(1)),
          secondaryLabel = clean(SECONDARY_LABEL.find(entry)?.groupValues?.get(1)),
          state = state,
          isSwitch = CLASS_NAME.find(entry)?.groupValues?.get(1) == "android.widget.Switch",
        )
    }
    return result
  }

  private fun clean(value: String?): String? =
    value?.takeIf { it != "null" }?.replace(DIRECTION_MARKS, "")?.trim()?.ifEmpty { null }
}

/** Runs `dumpsys`, which needs the DUMP permission, and reads what it prints, up to a limit. */
object Dumpsys {
  private const val TAG = "Dumpsys"
  private const val TIMEOUT_MS = 3_000L
  private const val MAX_CHARS = 1_000_000

  private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

  /** Must not run on the main thread. Returns null if `dumpsys` can't run. */
  fun run(vararg args: String): String? {
    val process =
      try {
        ProcessBuilder(listOf("dumpsys") + args).redirectErrorStream(true).start()
      } catch (e: IOException) {
        LogUtils.w(TAG, "dumpsys failed: %s", e)
        return null
      }
    // Reading a pipe can't be interrupted, so a dumpsys that hangs is killed instead, which ends
    // the read.
    val kill = Runnable { process.destroyForcibly() }
    mainHandler.postDelayed(kill, TIMEOUT_MS)
    return try {
      val text = StringBuilder()
      process.inputStream.bufferedReader().use { reader ->
        val buffer = CharArray(8192)
        while (text.length < MAX_CHARS) {
          val count = reader.read(buffer)
          if (count < 0) break
          text.append(buffer, 0, count)
        }
      }
      text.toString()
    } catch (e: IOException) {
      LogUtils.w(TAG, "dumpsys failed: %s", e)
      null
    } finally {
      mainHandler.removeCallbacks(kill)
      process.destroyForcibly()
    }
  }
}

/**
 * Whether the microphone and camera are blocked by their switches in the system's quick settings.
 * Android tells only system apps, so SandFly reads `dumpsys sensor_privacy`, which needs DUMP.
 */
object SensorPrivacy {
  const val MICROPHONE = 1
  const val CAMERA = 2

  /** The software switch, rather than a hardware one on some devices. */
  private const val SOFTWARE_TOGGLE = 1
  private const val STATE_BLOCKED = 1

  private val USER = Regex("""^\s*user_id=(\d+)""")
  private val SENSOR = Regex("""^\s*sensor=(\d+)""")
  private val TOGGLE = Regex("""^\s*toggle_type=(\d+)""")
  private val STATE = Regex("""^\s*state_type=(\d+)""")

  /** Must not run on the main thread. Returns null if the dump can't be read. */
  fun read(userId: Int): Map<Int, Boolean>? = Dumpsys.run("sensor_privacy")?.let { parse(it, userId) }

  /**
   * Whether each sensor that has a switch is blocked for [userId]. A sensor that the dump leaves
   * out has never been blocked.
   */
  fun parse(dump: String, userId: Int): Map<Int, Boolean> {
    if (!dump.contains("SENSOR PRIVACY")) return emptyMap()
    val result = mutableMapOf(MICROPHONE to false, CAMERA to false)
    var user = -1
    var sensor = -1
    var toggle = -1
    for (line in dump.lineSequence()) {
      USER.find(line)?.let { user = it.groupValues[1].toInt() }
      SENSOR.find(line)?.let { sensor = it.groupValues[1].toInt() }
      TOGGLE.find(line)?.let { toggle = it.groupValues[1].toInt() }
      STATE.find(line)?.let {
        if (user == userId && toggle == SOFTWARE_TOGGLE && sensor in result) {
          result[sensor] = it.groupValues[1].toInt() == STATE_BLOCKED
        }
      }
    }
    return result
  }
}
