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
 * it.
 *
 * An app's tile is clicked as [AppTileClicker] clicks it. A system tile that SandFly has a control
 * for, such as Wi-Fi or the sound mode, changes that control through Android or Shizuku. Without
 * Shizuku, the command for a control that needs it announces "Shizuku not running". Only a system
 * tile that nothing else can change is clicked in the system's quick settings, which SandFly opens
 * and closes.
 */
object TileCommands {
  /** Starts every saved value of these commands. It must never change once released. */
  private const val PREFIX = "QS_TILE:"

  /** How long a tile takes to show its new state after SystemUI clicked it. */
  private const val STATE_DELAY_MS = 500L

  private val mainHandler = Handler(Looper.getMainLooper())

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
      is QuickItem.SystemTile -> currentLabel(context, item) ?: item.label
      is QuickItem.Tile -> tileLabel(context, item.component)
    }

  /** A system tile's name as SandFly found it last, by its spec, if it has one. */
  private fun currentLabel(context: Context, item: QuickItem.SystemTile): String? =
    item.spec.takeIf { it.isNotEmpty() }?.let { spec ->
      SystemTiles.lastFound(context).firstOrNull { it.spec == spec }?.label
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
   * build can't use Shizuku.
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
        val name = context.getString(control.title)
        val failed = { onDone(failed(context, name)) }
        when {
          controls.needsShizuku(control) -> onDone(shellFailure(context, name))
          control.kind == QuickControl.Kind.SWITCH -> toggle(context, controls, control, onDone, failed)
          control.kind == QuickControl.Kind.CHOICE -> next(controls, control, onDone, failed)
          controls.press(control) == QuickControls.Change.FAILED -> failed()
        }
      }
      is QuickItem.SystemTile -> clickInSystem(context, item, name(context, item), retry = true, onDone)
      null -> {}
    }
  }

  /**
   * Clicks an app's tile as [AppTileClicker] does, after reading from SystemUI's dump whether it is
   * in the system's quick settings and its name there, and gives [onDone] its new state.
   */
  private fun click(context: Context, component: ComponentName, onDone: (String) -> Unit) {
    try {
      context.packageManager.getServiceInfo(component, /* flags= */ 0)
    } catch (e: PackageManager.NameNotFoundException) {
      onDone(context.getString(R.string.quick_settings_tile_not_found, tileLabel(context, component)))
      return
    }
    val spec = SystemTiles.spec(component)
    readTiles(context) { tiles ->
      val label = tiles?.get(spec)?.label ?: tileLabel(context, component)
      AppTileClicker.click(context, component, label, inSystem = tiles?.containsKey(spec)) { result ->
        when (result) {
          is AppTileClicker.Result.Clicked ->
            onDone(stateText(context, result.state, result.checked) ?: label)
          AppTileClicker.Result.ClickedByShell -> sayStateLater(context, spec, label, onDone)
          AppTileClicker.Result.NotFound ->
            onDone(context.getString(R.string.quick_settings_tile_not_found, label))
          AppTileClicker.Result.PanelFailed -> onDone(failed(context, label))
          AppTileClicker.Result.AddFailed,
          AppTileClicker.Result.ClickFailed -> onDone(shellFailure(context, label))
        }
      }
    }
  }

  /**
   * Reads the system's tiles from SystemUI's dump, by spec, and gives [onDone] them, or null when
   * SandFly can't read them, on the main thread.
   */
  private fun readTiles(context: Context, onDone: (Map<String, TileState>?) -> Unit) {
    if (!SystemTiles.canRead(context)) {
      onDone(null)
      return
    }
    Thread({
        val tiles =
          try {
            SystemTiles.read()?.takeIf { it.isNotEmpty() }
          } catch (e: Throwable) {
            null
          }
        mainHandler.post { onDone(tiles) }
      }, "TileCommands")
      .start()
  }

  /** Gives [onDone] the state of the tile [spec] once it shows it, or [label] if it can't be read. */
  private fun sayStateLater(context: Context, spec: String, label: String, onDone: (String) -> Unit) {
    mainHandler.postDelayed(
      {
        readTiles(context) { tiles ->
          val state =
            when (tiles?.get(spec)?.state) {
              android.service.quicksettings.Tile.STATE_ACTIVE -> onOff(context, true)
              android.service.quicksettings.Tile.STATE_INACTIVE -> onOff(context, false)
              android.service.quicksettings.Tile.STATE_UNAVAILABLE ->
                context.getString(R.string.quick_settings_unavailable)
              else -> null
            }
          onDone(state ?: label)
        }
      },
      STATE_DELAY_MS,
    )
  }

  /**
   * Turns [control] on or off, and gives [onDone] its new state, or why it can't. [failed] runs
   * when the change fails.
   */
  private fun toggle(
    context: Context,
    controls: QuickControls,
    control: QuickControl,
    onDone: (String) -> Unit,
    failed: () -> Unit,
  ) {
    val unreadable = { onDone(shellFailure(context, context.getString(control.title))) }
    fun set(on: Boolean) {
      val text = onOff(context, on)
      when (controls.setSwitch(control, on, onDone = { onDone(text) }, onFailed = failed)) {
        QuickControls.Change.DONE -> {
          if (control == QuickControl.FLASHLIGHT) Flashlight.saw(on)
          onDone(text)
        }
        QuickControls.Change.FAILED -> failed()
        // A settings screen opened, or the change says when it is made.
        QuickControls.Change.OPENED,
        QuickControls.Change.STARTED -> {}
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
   * settings panel do, and gives [onDone] that value. [failed] runs when it can't.
   */
  private fun next(
    controls: QuickControls,
    control: QuickControl,
    onDone: (String) -> Unit,
    failed: () -> Unit,
  ) {
    val next = controls.choice(control)?.let { nextOption(it) } ?: return failed()
    when (controls.setChoice(control, next.value, onFailed = failed)) {
      QuickControls.Change.DONE,
      QuickControls.Change.STARTED -> onDone(next.label)
      QuickControls.Change.FAILED -> failed()
      QuickControls.Change.OPENED -> {}
    }
  }

  /** The option after [choice]'s current one, the first after the last, or null without options. */
  internal fun nextOption(choice: ChoiceState): Choice? {
    val options = choice.options.ifEmpty { return null }
    return options[(options.indexOfFirst { it.value == choice.current.value } + 1) % options.size]
  }

  /**
   * Clicks [item] in the system's quick settings by [label], and gives [onDone] its new state. When
   * it isn't found and [retry] is true, SystemUI's dump is read again, since a tile can change its
   * name, such as to its state, and the tile is looked for again by its name there.
   */
  private fun clickInSystem(
    context: Context,
    item: QuickItem.SystemTile,
    label: String,
    retry: Boolean,
    onDone: (String) -> Unit,
  ) {
    SystemPanelClicker.click(label) { result ->
      when (result) {
        is SystemPanelClicker.Result.Clicked ->
          onDone(stateText(context, result.state, result.checked) ?: label)
        SystemPanelClicker.Result.Failed -> onDone(failed(context, label))
        SystemPanelClicker.Result.NotFound -> {
          val notFound = { onDone(context.getString(R.string.quick_settings_tile_not_found, label)) }
          if (!retry || item.spec.isEmpty() || !SystemTiles.canRead(context)) {
            notFound()
            return@click
          }
          SystemTiles.readLater(context) { tiles ->
            SystemTiles.keep(context, tiles)
            val now = tiles?.firstOrNull { it.spec == item.spec }?.label
            if (now == null || SystemPanelClicker.normalize(now) == SystemPanelClicker.normalize(label)) {
              notFound()
            } else {
              clickInSystem(context, item, now, retry = false, onDone)
            }
          }
        }
      }
    }
  }

  /** What a tile said its state was after a click, if it said. */
  private fun stateText(context: Context, state: CharSequence?, checked: Boolean?): String? =
    state?.toString()?.takeIf { it.isNotBlank() } ?: checked?.let { onOff(context, it) }

  private fun onOff(context: Context, on: Boolean): String =
    context.getString(if (on) R.string.quick_settings_on else R.string.quick_settings_off)

  private fun failed(context: Context, name: String): String =
    context.getString(R.string.quick_settings_tile_failed, name)

  /**
   * Why a command through Shizuku, or root, didn't run: Shizuku isn't running or doesn't allow
   * SandFly, or the command failed.
   */
  private fun shellFailure(context: Context, name: String): String =
    when {
      !ShizukuGrants.isReady() -> context.getString(R.string.overview_shizuku_not_running)
      !ShizukuShell.isAllowed() ->
        context.getString(
          if (ShizukuShell.usesRoot()) R.string.no_root_access else R.string.overview_shizuku_not_allowed
        )
      else -> failed(context, name)
    }
}
