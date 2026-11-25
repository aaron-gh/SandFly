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

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.accessibility.talkback.shizuku.ShizukuCommand
import com.google.android.accessibility.talkback.shizuku.ShizukuGrants
import com.google.android.accessibility.talkback.shizuku.ShizukuShell

/**
 * Clicks an app's tile for SandFly's quick settings panel and the App tiles commands, adding it to
 * the system's quick settings first if it isn't there, since SystemUI runs only the tiles it shows.
 *
 * The tile is clicked in the open system quick settings, as a finger would. Many tiles act on the
 * state they last showed, such as a VPN's that connects when it shows disconnected, and a tile
 * shows its state only while the system's quick settings are open; a tile clicked with them closed
 * may act on a state it never showed, and do nothing. Shizuku's click, with them closed, is kept
 * for a tile that isn't found there. Without Shizuku, the tile is clicked in the system's quick
 * settings if it's there.
 */
object AppTileClicker {
  /** How long SystemUI takes to start a tile that was just added. */
  private const val ADD_TILE_DELAY_MS = 1_000L

  private val mainHandler = Handler(Looper.getMainLooper())

  /** Tiles added to the system's quick settings while SandFly runs. */
  private val added = HashSet<ComponentName>()

  sealed class Result {
    /** Clicked in the system's quick settings, which said [state] and [checked] afterwards. */
    data class Clicked(val state: CharSequence?, val checked: Boolean?) : Result()

    /** Clicked through Shizuku. */
    object ClickedByShell : Result()

    /** Without Shizuku, the tile isn't in the system's quick settings. */
    object NotFound : Result()

    /** Without Shizuku, the system's quick settings didn't open or the click failed. */
    object PanelFailed : Result()

    /** Shizuku's command to add or click the tile failed. */
    object AddFailed : Result()

    object ClickFailed : Result()
  }

  /**
   * Clicks [component]'s tile, named [label] in the system's quick settings. [inSystem] is whether
   * the tile is in the system's quick settings, or null when SandFly can't tell, which counts the
   * tiles it added itself. [isWanted] is checked before each later step, which stops when it
   * returns false. [onDone] runs on the main thread.
   */
  fun click(
    context: Context,
    component: ComponentName,
    label: String,
    inSystem: Boolean?,
    isWanted: () -> Boolean = { true },
    onDone: (Result) -> Unit,
  ) {
    if (!ShizukuGrants.isReady()) {
      SystemPanelClicker.click(label) { result ->
        onDone(
          when (result) {
            is SystemPanelClicker.Result.Clicked -> Result.Clicked(result.state, result.checked)
            SystemPanelClicker.Result.NotFound -> Result.NotFound
            SystemPanelClicker.Result.Failed -> Result.PanelFailed
          }
        )
      }
      return
    }
    val argument = component.flattenToString()
    val click = click@{
      if (!isWanted()) return@click
      SystemPanelClicker.click(label) { result ->
        if (result is SystemPanelClicker.Result.Clicked) {
          onDone(Result.Clicked(result.state, result.checked))
        } else if (isWanted()) {
          ShizukuShell.run(context, ShizukuCommand.CLICK_TILE, argument) { worked ->
            onDone(if (worked) Result.ClickedByShell else Result.ClickFailed)
          }
        }
      }
    }
    if (inSystem ?: (component in added)) {
      click()
      return
    }
    ShizukuShell.run(context, ShizukuCommand.ADD_TILE, argument) { worked ->
      if (worked) {
        added += component
        mainHandler.postDelayed(click, ADD_TILE_DELAY_MS)
      } else {
        onDone(Result.AddFailed)
      }
    }
  }
}
