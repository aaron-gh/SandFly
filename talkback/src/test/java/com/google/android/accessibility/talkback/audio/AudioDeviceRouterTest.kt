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

package com.google.android.accessibility.talkback.audio

import android.media.AudioDeviceInfo
import com.google.android.accessibility.talkback.audio.AudioDeviceRouter.AudioTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioDeviceRouterTest {

  @Test
  fun audioTarget_standardValuesParseCorrectly() {
    assertEquals(AudioTarget.SystemDefault, AudioTarget.parse("default"))
    assertEquals(AudioTarget.PhoneSpeaker, AudioTarget.parse("speaker"))
    assertEquals(AudioTarget.AnyExternal, AudioTarget.parse("bluetooth_or_wired"))
  }

  @Test
  fun audioTarget_specificDevicesParseCorrectly() {
    val storm = AudioTarget.parse("device:2:Storm wireless audio")
    assertTrue(storm is AudioTarget.SpecificDevice)
    assertEquals(2, (storm as AudioTarget.SpecificDevice).type)
    assertEquals("Storm wireless audio", storm.name)

    val bolt = AudioTarget.parse("device:3:Bolt wired headphones")
    assertTrue(bolt is AudioTarget.SpecificDevice)
    assertEquals(3, (bolt as AudioTarget.SpecificDevice).type)
    assertEquals("Bolt wired headphones", bolt.name)
  }

  @Test
  fun audioTarget_fallbacksToSystemDefault() {
    assertEquals(AudioTarget.SystemDefault, AudioTarget.parse(""))
    assertEquals(AudioTarget.SystemDefault, AudioTarget.parse(null))
    assertEquals(AudioTarget.SystemDefault, AudioTarget.parse("unknown_value"))
  }

  @Test
  fun audioTarget_multipleConnectedDevicesParseCorrectly() {
    val zebronics = AudioTarget.parse("device:8:Zebronics bluetooth speaker")
    assertTrue(zebronics is AudioTarget.SpecificDevice)
    assertEquals(8, (zebronics as AudioTarget.SpecificDevice).type)
    assertEquals("Zebronics bluetooth speaker", zebronics.name)

    val bolt = AudioTarget.parse("device:3:Bolt earphones")
    assertTrue(bolt is AudioTarget.SpecificDevice)
    assertEquals(3, (bolt as AudioTarget.SpecificDevice).type)
    assertEquals("Bolt earphones", bolt.name)
  }

  private val choices =
    listOf("default", "speaker", "device:8:Storm wireless audio", "device:3:Bolt wired headphones")

  @Test
  fun adjacentChoice_cyclesForwardAndWraps() {
    assertEquals(1, AudioTarget.adjacentChoice(choices, "default", isNext = true))
    assertEquals(3, AudioTarget.adjacentChoice(choices, "device:8:Storm wireless audio", isNext = true))
    assertEquals(0, AudioTarget.adjacentChoice(choices, "device:3:Bolt wired headphones", isNext = true))
  }

  @Test
  fun adjacentChoice_cyclesBackwardAndWraps() {
    assertEquals(3, AudioTarget.adjacentChoice(choices, "default", isNext = false))
    assertEquals(0, AudioTarget.adjacentChoice(choices, "speaker", isNext = false))
  }

  @Test
  fun adjacentChoice_unsavedValueIsSystemDefault() {
    assertEquals(1, AudioTarget.adjacentChoice(choices, null, isNext = true))
    assertEquals(1, AudioTarget.adjacentChoice(choices, "", isNext = true))
  }

  @Test
  fun adjacentChoice_findsAConnectedOutputByName() {
    // Saved with another device type, such as when the same headset connected over another profile.
    assertEquals(2, AudioTarget.indexOfChoice(choices, "device:7:storm wireless audio"))
    assertEquals(3, AudioTarget.adjacentChoice(choices, "device:7:Storm wireless audio", isNext = true))
  }

  @Test
  fun adjacentChoice_fromADisconnectedOutputStartsAtAnEnd() {
    assertEquals(-1, AudioTarget.indexOfChoice(choices, "device:8:Zebronics bluetooth speaker"))
    assertEquals(0, AudioTarget.adjacentChoice(choices, "device:8:Zebronics bluetooth speaker", isNext = true))
    assertEquals(3, AudioTarget.adjacentChoice(choices, "device:8:Zebronics bluetooth speaker", isNext = false))
  }

  @Test
  fun adjacentChoice_anyExternalIsTheFirstConnectedOutput() {
    assertEquals(2, AudioTarget.indexOfChoice(choices, "bluetooth_or_wired"))
    assertEquals(-1, AudioTarget.indexOfChoice(listOf("default", "speaker"), "bluetooth_or_wired"))
  }

  @Test
  fun adjacentChoice_noChoices() {
    assertEquals(-1, AudioTarget.adjacentChoice(emptyList(), "default", isNext = true))
  }

  @Test
  fun deviceValue_parsesBack() {
    val value = AudioTarget.deviceValue(8, "Storm wireless audio")
    assertEquals(AudioTarget.SpecificDevice(8, "Storm wireless audio"), AudioTarget.parse(value))
  }

  @Test
  fun isBluetooth_bluetoothOutputsAreNotCommunicationDevices() {
    assertTrue(AudioDeviceRouter.isBluetooth(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
    assertTrue(AudioDeviceRouter.isBluetooth(AudioDeviceInfo.TYPE_BLUETOOTH_SCO))
    assertTrue(AudioDeviceRouter.isBluetooth(AudioDeviceInfo.TYPE_BLE_HEADSET))
    assertTrue(AudioDeviceRouter.isBluetooth(AudioDeviceInfo.TYPE_BLE_SPEAKER))
    assertFalse(AudioDeviceRouter.isBluetooth(AudioDeviceInfo.TYPE_WIRED_HEADPHONES))
    assertFalse(AudioDeviceRouter.isBluetooth(AudioDeviceInfo.TYPE_USB_HEADSET))
    assertFalse(AudioDeviceRouter.isBluetooth(AudioDeviceInfo.TYPE_HEARING_AID))
    assertFalse(AudioDeviceRouter.isBluetooth(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
  }
}
