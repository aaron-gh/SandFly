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

import com.google.android.accessibility.talkback.audio.AudioDeviceRouter.AudioTarget
import org.junit.Assert.assertEquals
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
}
