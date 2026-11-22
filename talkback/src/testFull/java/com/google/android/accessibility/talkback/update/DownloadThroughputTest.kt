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

package com.google.android.accessibility.talkback.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadThroughputTest {
  @Test
  fun unknownUntilThreeSeconds() {
    val throughput = DownloadThroughput()
    throughput.add(0, 0)
    throughput.add(2_000, 2_000)
    assertNull(throughput.secondsLeft(2_000, 10_000))
    throughput.add(3_000, 3_000)
    assertEquals(1_000L, throughput.bytesPerSecond())
    assertEquals(7L, throughput.secondsLeft(3_000, 10_000))
  }

  @Test
  fun followsTheLastFiveSeconds() {
    val throughput = DownloadThroughput()
    // 1000 bytes a second for 10 seconds, then 4000 a second.
    for (second in 0..10) throughput.add(second * 1_000L, second * 1_000L)
    for (second in 11..16) throughput.add(second * 1_000L, 10_000L + (second - 10) * 4_000L)
    assertEquals(4_000L, throughput.bytesPerSecond())
  }

  @Test
  fun aGapOrRestart_startsOver() {
    val throughput = DownloadThroughput()
    for (second in 0..4) throughput.add(second * 1_000L, second * 1_000L)
    throughput.add(20_000, 5_000)
    assertNull(throughput.bytesPerSecond())
    for (second in 0..4) throughput.add(30_000 + second * 1_000L, 6_000L + second * 1_000L)
    throughput.add(35_000, 0)
    assertNull(throughput.bytesPerSecond())
  }

  @Test
  fun unknownTotal_hasNoTimeLeft() {
    val throughput = DownloadThroughput()
    for (second in 0..4) throughput.add(second * 1_000L, second * 1_000L)
    assertNull(throughput.secondsLeft(4_000, -1))
  }

  @Test
  fun roundSecondsLeft() {
    assertEquals(45L, DownloadThroughput.roundSecondsLeft(45))
    assertEquals(60L, DownloadThroughput.roundSecondsLeft(60))
    assertEquals(130L, DownloadThroughput.roundSecondsLeft(134))
    assertEquals(140L, DownloadThroughput.roundSecondsLeft(135))
  }
}
