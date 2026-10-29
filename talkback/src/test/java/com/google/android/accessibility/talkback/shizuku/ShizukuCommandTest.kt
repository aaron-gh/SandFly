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

package com.google.android.accessibility.talkback.shizuku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShizukuCommandTest {

  @Test
  fun switchesTakeOnAndOff() {
    assertEquals(
      listOf("cmd", "wifi", "set-wifi-enabled", "enabled"),
      ShizukuCommand.WIFI.shellCommand(ShizukuCommand.ON),
    )
    assertEquals(
      listOf("cmd", "location", "set-location-enabled", "false"),
      ShizukuCommand.LOCATION.shellCommand(ShizukuCommand.OFF),
    )
    assertNull(ShizukuCommand.WIFI.shellCommand("com.example/.Tile"))
  }

  @Test
  fun sensorSwitchesTakeAUser() {
    assertEquals(
      listOf("cmd", "sensor_privacy", "enable", "10", "microphone"),
      ShizukuCommand.MICROPHONE_BLOCKED.shellCommand(ShizukuCommand.argument(true, 10)),
    )
    assertNull(ShizukuCommand.CAMERA_BLOCKED.shellCommand(ShizukuCommand.OFF))
    assertNull(ShizukuCommand.CAMERA_BLOCKED.shellCommand("off@0; reboot"))
  }

  @Test
  fun tilesTakeComponentNames() {
    assertEquals(
      listOf("cmd", "statusbar", "click-tile", "com.google.android.gms/.nearby.sharing.SharingTileService"),
      ShizukuCommand.CLICK_TILE.shellCommand("com.google.android.gms/.nearby.sharing.SharingTileService"),
    )
    assertEquals(
      listOf("cmd", "statusbar", "add-tile", "com.example.app/com.example.app.Tile\$Inner"),
      ShizukuCommand.ADD_TILE.shellCommand("com.example.app/com.example.app.Tile\$Inner"),
    )
  }

  @Test
  fun tilesRejectAnythingElse() {
    assertNull(ShizukuCommand.CLICK_TILE.shellCommand(ShizukuCommand.ON))
    assertNull(ShizukuCommand.CLICK_TILE.shellCommand("com.example/.Tile; reboot"))
    assertNull(ShizukuCommand.CLICK_TILE.shellCommand("com.example/.Tile extra"))
    assertNull(ShizukuCommand.CLICK_TILE.shellCommand(""))
  }

  @Test
  fun inputTextTypesSpacesAsPercentS() {
    assertEquals(
      listOf("input", "text", "new%sgame;%sreboot"),
      ShizukuCommand.INPUT_TEXT.shellCommand("new game; reboot"),
    )
    assertNull(ShizukuCommand.INPUT_TEXT.shellCommand(""))
    assertNull(ShizukuCommand.INPUT_TEXT.shellCommand("a\nb"))
  }

  @Test
  fun inputKeyeventTakesOnlyKeyCodes() {
    assertEquals(
      listOf("input", "keyevent", "KEYCODE_ENTER", "KEYCODE_DEL"),
      ShizukuCommand.INPUT_KEYEVENT.shellCommand("KEYCODE_ENTER KEYCODE_DEL"),
    )
    assertNull(ShizukuCommand.INPUT_KEYEVENT.shellCommand("KEYCODE_ENTER; reboot"))
    assertNull(ShizukuCommand.INPUT_KEYEVENT.shellCommand("66"))
  }
}
