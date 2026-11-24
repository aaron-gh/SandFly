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
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.Process
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.macros.Flashlight
import com.google.android.accessibility.talkback.shizuku.ShizukuCommand
import com.google.android.accessibility.talkback.shizuku.ShizukuGrants
import com.google.android.accessibility.talkback.shizuku.ShizukuShell

/**
 * Commands that activate a quick settings tile: an app's tile, or one of the system's own that
 * [SystemTiles.find] found. Their values are [PREFIX] and the tile's [QuickItem.id]. A system tile
 * that SandFly has a control for is that control, named as SandFly's quick settings panel names
 * it, since such tiles, such as the sound mode, often show their state as their name.
 *
 * SystemUI clicks an app's tile through Shizuku. A system tile that SandFly has a control for,
 * such as Wi-Fi or the sound mode, changes that control through Android or Shizuku. Without
 * Shizuku, a tile that needs it says so. Only a system tile that nothing else can change is clicked
 * in the system's quick settings, which SandFly opens and closes.
 */
object TileCommands {
  /** Starts every saved value of these commands. It must never change once released. */
  private const val PREFIX = "QS_TILE:"

  /** How long SystemUI takes to add a tile to the system's quick settings. */
  private const val ADD_TILE_DELAY_MS = 1_000L

  private val mainHandler = Handler(Looper.getMainLooper())

  /** The apps' tiles SandFly has added to the system's quick settings since it started. */
  private val added = HashSet<ComponentName>()

  /** A tile, by the name the lists of commands show, and the command that activates it. */
  data class Tile(val label: String, val value: String)

  fun valueOf(item: QuickItem): String = PREFIX + item.id

  private fun item(value: String?): QuickItem? =
    value?.takeIf { it.startsWith(PREFIX) }?.let { QuickItem.parse(it.substring(PREFIX.length)) }

  @JvmStatic fun isAction(value: String?): Boolean = item(value) != null

  /** The tile's name, or null for no tile command. */
  @JvmStatic fun label(context: Context, value: String?): String? = item(value)?.let { name(context, it) }

  private fun name(context: Context, item: QuickItem): String =
    when (item) {
      is QuickItem.Control -> context.getString(item.control.title)
      // As the system names it now, which changes with the phone's language, as in the panel.
      is QuickItem.SystemTile ->
        SystemTiles.lastFound(context).firstOrNull { item.spec.isNotEmpty() && it.spec == item.spec }?.label
          ?: item.label
      is QuickItem.Tile -> tileLabel(context, item.component)
    }

  /**
   * An app's tile's name as the system's quick settings showed it when SandFly last read them,
   * which some tiles change, such as to a device's name, and otherwise the name the app gives it.
   */
  private fun tileLabel(context: Context, component: ComponentName): String {
    SystemTiles.appTileNames(context)[SystemTiles.spec(component)]?.let {
      return it
    }
    val packageManager = context.packageManager
    return try {
      packageManager.getServiceInfo(component, /* flags= */ 0).loadLabel(packageManager).toString()
    } catch (e: PackageManager.NameNotFoundException) {
      component.packageName
    }
  }

  /**
   * The tiles of installed apps, each named with its app when the names differ. None where this
   * build can't use Shizuku, which clicks them.
   */
  fun appTiles(context: Context): List<Tile> {
    if (!ShizukuShell.isAvailable(context)) return emptyList()
    val names = SystemTiles.appTileNames(context)
    return SystemTiles.appTiles(context).map { tile ->
      val label = names[SystemTiles.spec(tile.component)] ?: tile.label.toString()
      val name =
        if (label == tile.appName.toString()) label
        else context.getString(R.string.quick_settings_tile_name, label, tile.appName)
      Tile(name, valueOf(QuickItem.Tile(tile.component)))
    }
  }

  /** The system's own tiles that [SystemTiles.find] found last, each as SandFly's control for it. */
  fun systemTiles(context: Context): List<Tile> {
    val controls = QuickControls(context)
    return SystemTiles.lastFound(context)
      .map { tile ->
        QuickSettingsItems.controlFor(tile.spec)?.takeIf { controls.isAvailable(it) }?.let { QuickItem.Control(it) }
          ?: tile
      }
      .distinct()
      .map { Tile(name(context, it), valueOf(it)) }
  }

  /** Activates the tile of the command [value]. [onDone] gets what to say, on the main thread. */
  @JvmStatic
  fun run(context: Context, value: String, onDone: (String) -> Unit) {
    when (val item = item(value)) {
      is QuickItem.Tile -> click(context, item.component, onDone)
      is QuickItem.Control -> {
        val controls = QuickControls(context)
        val control = item.control
        when {
          controls.needsShizuku(control) -> onDone(shizukuMissing(context))
          control.kind == QuickControl.Kind.SWITCH -> toggle(context, controls, control, onDone)
          control.kind == QuickControl.Kind.CHOICE -> next(controls, control, onDone)
          else -> controls.press(control)
        }
      }
      is QuickItem.SystemTile -> clickInSystem(context, name(context, item), onDone)
      null -> {}
    }
  }

  /**
   * Has SystemUI click an app's tile through Shizuku, adding it to the system's quick settings
   * first, since SystemUI clicks only the tiles there. The system's quick settings stay closed, so
   * a tile that acts on the state it last showed there may do nothing.
   */
  private fun click(context: Context, component: ComponentName, onDone: (String) -> Unit) {
    val argument = component.flattenToString()
    val click = {
      ShizukuShell.run(context, ShizukuCommand.CLICK_TILE, argument) { worked ->
        if (!worked) onDone(shizukuMissing(context))
      }
    }
    if (component in added) {
      click()
      return
    }
    ShizukuShell.run(context, ShizukuCommand.ADD_TILE, argument) { worked ->
      if (worked) {
        added += component
        mainHandler.postDelayed(click, ADD_TILE_DELAY_MS)
      } else {
        onDone(shizukuMissing(context))
      }
    }
  }

  /**
   * Turns [control] on or off, and gives [onDone] its new state, or what Shizuku lacks when
   * SandFly can't read whether it is on.
   */
  private fun toggle(
    context: Context,
    controls: QuickControls,
    control: QuickControl,
    onDone: (String) -> Unit,
  ) {
    val unreadable = { onDone(shizukuMissing(context)) }
    fun set(on: Boolean) {
      val text = context.getString(if (on) R.string.quick_settings_on else R.string.quick_settings_off)
      val change = controls.setSwitch(control, on, onDone = { onDone(text) }, onFailed = {})
      if (change == QuickControls.Change.DONE) {
        if (control == QuickControl.FLASHLIGHT) Flashlight.saw(on)
        onDone(text)
      }
    }
    // Android tells only a callback whether the flashlight is on, and only the shell the hotspot
    // and the microphone and camera switches.
    when (control) {
      QuickControl.FLASHLIGHT -> set(!Flashlight.isOn(context))
      QuickControl.HOTSPOT ->
        ShizukuShell.state(context, ShizukuCommand.HOTSPOT) { on -> if (on == null) unreadable() else set(!on) }
      QuickControl.MICROPHONE_ACCESS,
      QuickControl.CAMERA_ACCESS -> {
        val sensor =
          if (control == QuickControl.MICROPHONE_ACCESS) SensorPrivacy.MICROPHONE else SensorPrivacy.CAMERA
        val user = Process.myUserHandle().hashCode()
        Thread({
            val blocked = runCatching { SensorPrivacy.read(user)?.get(sensor) }.getOrNull()
            // The switch says whether the sensor works, so turning it over sets it to blocked.
            mainHandler.post { if (blocked == null) unreadable() else set(blocked) }
          }, "TileCommands")
          .start()
      }
      else -> controls.isOn(control)?.let { set(!it) } ?: unreadable()
    }
  }

  /**
   * Moves [control], such as the sound mode, to its next value, as its tile and SandFly's quick
   * settings panel do, and gives [onDone] that value.
   */
  private fun next(controls: QuickControls, control: QuickControl, onDone: (String) -> Unit) {
    val choice = controls.choice(control)?.takeIf { it.options.isNotEmpty() } ?: return
    val options = choice.options
    val next = options[(options.indexOfFirst { it.value == choice.current.value } + 1) % options.size]
    val change = controls.setChoice(control, next.value) {}
    if (change == QuickControls.Change.DONE || change == QuickControls.Change.STARTED) onDone(next.label)
  }

  /** Clicks the tile named [label] in the system's quick settings, and says its new state. */
  private fun clickInSystem(context: Context, label: String, onDone: (String) -> Unit) {
    SystemPanelClicker.click(label) { result ->
      if (result is SystemPanelClicker.Result.Clicked) {
        val state =
          result.state?.toString()
            ?: result.checked?.let {
              context.getString(if (it) R.string.quick_settings_on else R.string.quick_settings_off)
            }
        state?.let(onDone)
      } else {
        onDone(context.getString(R.string.quick_settings_tile_not_found, label))
      }
    }
  }

  /** What SandFly says elsewhere when Shizuku, or root, doesn't let it run a command. */
  private fun shizukuMissing(context: Context): String =
    context.getString(
      when {
        ShizukuShell.usesRoot() -> R.string.no_root_access
        ShizukuGrants.isReady() -> R.string.overview_shizuku_not_allowed
        else -> R.string.overview_shizuku_not_running
      }
    )
}
