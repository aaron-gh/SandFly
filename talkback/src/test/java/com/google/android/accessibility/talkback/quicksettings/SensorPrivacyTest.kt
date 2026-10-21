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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorPrivacyTest {

  private val blockedMicrophone =
    """
    SENSOR PRIVACY MANAGER STATE (dumpsys sensor_privacy)
    {
      storage_implementation=com.android.server.sensorprivacy.SensorPrivacyStateControllerImpl
      users={
        user_id=0
        sensors={
          sensor=1
          toggles={
            toggle_type=1
            state_type=1
            last_change=23870774
          }
          sensor=2
          toggles={
            toggle_type=1
            state_type=2
            last_change=23870900
          }
        }
        user_id=10
        sensors={
          sensor=2
          toggles={
            toggle_type=1
            state_type=1
            last_change=23870999
          }
        }
      }
    }
    """
      .trimIndent()

  @Test
  fun readsTheUsersSoftwareSwitches() {
    assertEquals(
      mapOf(SensorPrivacy.MICROPHONE to true, SensorPrivacy.CAMERA to false),
      SensorPrivacy.parse(blockedMicrophone, userId = 0),
    )
    assertEquals(
      mapOf(SensorPrivacy.MICROPHONE to false, SensorPrivacy.CAMERA to true),
      SensorPrivacy.parse(blockedMicrophone, userId = 10),
    )
  }

  @Test
  fun treatsSensorsLeftOutAsNotBlocked() {
    val dump =
      """
      SENSOR PRIVACY MANAGER STATE (dumpsys sensor_privacy)
      {
        storage_implementation=com.android.server.sensorprivacy.SensorPrivacyStateControllerImpl
      }
      """
        .trimIndent()
    assertEquals(
      mapOf(SensorPrivacy.MICROPHONE to false, SensorPrivacy.CAMERA to false),
      SensorPrivacy.parse(dump, userId = 0),
    )
  }

  @Test
  fun ignoresHardwareSwitchesAndOtherText() {
    val hardware = blockedMicrophone.replace("toggle_type=1\n        state_type=1\n        last_change=23870774", "toggle_type=2\n        state_type=1\n        last_change=23870774")
    assertEquals(false, SensorPrivacy.parse(hardware, userId = 0)[SensorPrivacy.MICROPHONE])
    assertTrue(SensorPrivacy.parse("Permission Denial", userId = 0).isEmpty())
  }
}
