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
import android.app.Application
import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.ServiceInfo
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.utils.FormFactorUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TileCommandsTest {
  private val context: Application = RuntimeEnvironment.getApplication()
  private val smartThings = QuickItem.SystemTile("DeviceControl", "SmartThings")
  private val vpn = ComponentName("com.example.vpn", "com.example.vpn.ConnectTile")

  @Before
  fun setUp() {
    // The lists of commands check for a watch.
    FormFactorUtils.initialize(context)
  }

  @After
  fun tearDown() {
    SystemTiles.remember(context, emptyList())
  }

  @Test
  fun systemTileIsNamedAsTheSystemNamedIt() {
    val value = TileCommands.valueOf(smartThings)
    assertTrue(TileCommands.isAction(value))
    assertEquals("SmartThings", TileCommands.label(context, value))
  }

  @Test
  fun systemTileIsNamedAsTheSystemNamesItNow() {
    // After the phone's language changed, SandFly found the tile under its new name.
    SystemTiles.remember(context, listOf(QuickItem.SystemTile("DeviceControl", "Urządzenia")))
    assertEquals("Urządzenia", TileCommands.label(context, TileCommands.valueOf(smartThings)))
    // A tile found by its name alone keeps that name.
    val byName = QuickItem.SystemTile("", "SmartThings")
    assertEquals("SmartThings", TileCommands.label(context, TileCommands.valueOf(byName)))
  }

  @Test
  fun controlIsNamedAsSandFlysPanelNamesIt() {
    val value = TileCommands.valueOf(QuickItem.Control(QuickControl.WIFI))
    assertEquals("QS_TILE:WIFI", value)
    assertEquals(context.getString(R.string.quick_settings_wifi), TileCommands.label(context, value))
  }

  private fun installVpnTile() {
    shadowOf(context.packageManager)
      .addOrUpdateService(
        ServiceInfo().apply {
          packageName = vpn.packageName
          name = vpn.className
          nonLocalizedLabel = "Connect"
          applicationInfo = ApplicationInfo().apply { packageName = vpn.packageName }
        }
      )
  }

  @Test
  fun appTileIsNamedByItsService() {
    installVpnTile()
    assertEquals("Connect", TileCommands.label(context, TileCommands.valueOf(QuickItem.Tile(vpn))))
  }

  @Test
  fun appTileIsNamedAsTheSystemShowedIt() {
    installVpnTile()
    // The tile shows the network it connects to in place of its own name.
    SystemTiles.remember(context, listOf(QuickItem.SystemTile(SystemTiles.spec(vpn), "Office")))
    assertEquals("Office", TileCommands.label(context, TileCommands.valueOf(QuickItem.Tile(vpn))))
    assertTrue(TileCommands.systemTiles(context).isEmpty())
  }

  @Test
  fun appTileOfARemovedAppIsNamedByItsPackage() {
    val value = TileCommands.valueOf(QuickItem.Tile(vpn))
    assertTrue(TileCommands.isAction(value))
    assertEquals("com.example.vpn", TileCommands.label(context, value))
  }

  @Test
  fun otherValuesAreNotTileCommands() {
    assertFalse(TileCommands.isAction(null))
    assertFalse(TileCommands.isAction("QS_TILE:NO_SUCH_CONTROL"))
    assertFalse(TileCommands.isAction("OPEN_APP:com.example.vpn/.Main"))
    assertFalse(TileCommands.isAction(smartThings.id))
    assertNull(TileCommands.label(context, "QS_TILE:NO_SUCH_CONTROL"))
  }

  @Test
  fun foundSystemTilesAreCommandsWithSandFlysControlsForThem() {
    // Galaxy phones name the sound mode tile by the mode, and Pixels have Wi-Fi as Internet.
    SystemTiles.remember(
      context,
      listOf(
        QuickItem.SystemTile("Wifi", "Wi-Fi"),
        QuickItem.SystemTile("internet", "Internet"),
        QuickItem.SystemTile("SoundMode", "Vibrate"),
        smartThings,
      ),
    )
    val category =
      Commands.categories(context).single { it.titleRes == R.string.shortcut_title_system_tiles }
    assertEquals(
      listOf("QS_TILE:WIFI", "QS_TILE:SOUND_MODE", TileCommands.valueOf(smartThings)),
      category.commands.map { it.value },
    )
    assertEquals(
      context.getString(R.string.quick_settings_sound_mode),
      Commands.label(context, "QS_TILE:SOUND_MODE"),
    )
  }

  @Test
  fun anEmptyDumpKeepsTheTilesFoundBefore() {
    val found = listOf(QuickItem.SystemTile("hotspot", "Hotspot"), smartThings)
    SystemTiles.remember(context, found)
    // A SystemUI that dumps its tiles in another way, or a dump of tiles without names.
    assertNull(SystemTiles.fromDump(emptyMap()))
    assertNull(SystemTiles.fromDump(mapOf("wifi" to TileState("wifi", null, null, 2, true))))
    SystemTiles.keep(context, SystemTiles.fromDump(emptyMap()))
    SystemTiles.keep(context, emptyList())
    assertEquals(found, SystemTiles.lastFound(context))
  }

  @Test
  fun tilesFoundByLookingThroughReplaceThoseFromTheDump() {
    val fromDump =
      SystemTiles.fromDump(
        mapOf(
          "hotspot" to TileState("hotspot", "Hotspot", null, 1, true),
          SystemTiles.spec(vpn) to TileState(SystemTiles.spec(vpn), "Office", null, 1, false),
        )
      )
    SystemTiles.keep(context, fromDump)
    assertEquals(listOf(QuickItem.SystemTile("hotspot", "Hotspot")), SystemTiles.lastFound(context))
    assertEquals(mapOf(SystemTiles.spec(vpn) to "Office"), SystemTiles.appTileNames(context))

    val scanned = listOf(QuickItem.SystemTile("", "Hotspot"), QuickItem.SystemTile("", "Screen record"))
    SystemTiles.keep(context, scanned)
    assertEquals(scanned, SystemTiles.lastFound(context))
    assertTrue(SystemTiles.appTileNames(context).isEmpty())
  }

  @Test
  fun controlsNeedShizukuOnlyForWhatAndroidDoesNotLetSandFlyChange() {
    val controls = QuickControls(context)
    assertTrue(controls.needsShizuku(QuickControl.WIFI))
    assertFalse(controls.needsShizuku(QuickControl.SOUND_MODE))
    assertTrue(controls.needsShizuku(QuickControl.COLOR_INVERSION))
    shadowOf(context).grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
    assertFalse(controls.needsShizuku(QuickControl.COLOR_INVERSION))
  }

  @Test
  fun appTileOfARemovedAppIsNotFound() {
    var said: String? = null
    TileCommands.run(context, TileCommands.valueOf(QuickItem.Tile(vpn))) { said = it }
    assertEquals(context.getString(R.string.quick_settings_tile_not_found, "com.example.vpn"), said)
  }

  @Test
  fun choiceMovesToTheNextOptionAndWrapsAround() {
    val sound = Choice("Sound", 2)
    val vibrate = Choice("Vibrate", 1)
    val mute = Choice("Mute", 0)
    val options = listOf(sound, vibrate, mute)
    assertEquals(vibrate, TileCommands.nextOption(ChoiceState(sound, options)))
    assertEquals(sound, TileCommands.nextOption(ChoiceState(mute, options)))
    // A value that isn't one of the options moves to the first.
    assertEquals(sound, TileCommands.nextOption(ChoiceState(Choice("Other", 7), options)))
    assertNull(TileCommands.nextOption(ChoiceState(sound, emptyList())))
  }
}
