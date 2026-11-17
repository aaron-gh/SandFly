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

package com.google.android.accessibility.talkback.report

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VolumeKeyLogTest {
  @After
  fun tearDown() {
    VolumeKeyLog.clear()
  }

  @Test
  fun empty_readsNull() {
    assertNull(VolumeKeyLog.read())
  }

  @Test
  fun keepsTheLastEntries_oldestFirst() {
    for (i in 1..VolumeKeyLog.MAX_ENTRIES + 5) VolumeKeyLog.note("entry $i")
    val lines = VolumeKeyLog.read()!!.lines()
    assertEquals(VolumeKeyLog.MAX_ENTRIES, lines.size)
    assertTrue(lines.first().endsWith(" entry 6"))
    assertTrue(lines.last().endsWith(" entry ${VolumeKeyLog.MAX_ENTRIES + 5}"))
  }
}
