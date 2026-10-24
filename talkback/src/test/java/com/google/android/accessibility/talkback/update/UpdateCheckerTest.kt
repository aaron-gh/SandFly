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

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
  private fun release(build: Int, vararg commits: String, apk: String = "sandfly.apk"): String =
    JSONObject()
      .put("body", (listOf("build: $build", "") + commits.map { "- $it" }).joinToString("\n"))
      .put(
        "assets",
        JSONArray()
          .put(
            JSONObject().put("name", apk).put("browser_download_url", "https://example.com/$apk")
          ),
      )
      .toString()

  @Test
  fun olderOrSameBuildIsNotAnUpdate() {
    assertNull(UpdateChecker.parseRelease(release(10), 10, ""))
    assertNull(UpdateChecker.parseRelease(release(9), 10, ""))
  }

  @Test
  fun newerBuildIsAnUpdate() {
    val update = UpdateChecker.parseRelease(release(11, "bbbbbbb Two", "aaaaaaa One"), 10, "aaaaaaa0")
    assertEquals(11, update!!.build)
    assertEquals(listOf("Two"), update.changes)
    assertFalse(update.move)
  }

  @Test
  fun aStableReleaseOffersItsNotesRatherThanItsCommits() {
    val json =
      JSONObject(release(12, "bbbbbbb Two", "aaaaaaa One"))
        .put(
          "body",
          listOf("build: 12", "", "## New", "", "* Dictation.", "", "## Fixes", "* A crash.")
            .joinToString("\n"),
        )
        .toString()
    val update = UpdateChecker.parseRelease(json, 10, "aaaaaaa0")
    assertEquals(listOf("New", "Dictation.", "Fixes", "A crash."), update!!.changes)
  }

  @Test
  fun aDevelopmentBuildOffersItsCommits() {
    val json =
      JSONObject(release(12, "bbbbbbb Two", "aaaaaaa One")).put("prerelease", true).toString()
    val update = UpdateChecker.parseRelease(json, 10, "aaaaaaa0")
    assertEquals(listOf("Two"), update!!.changes)
  }

  @Test
  fun moveIsOfferedWhateverTheBuild() {
    val json = release(5, "bbbbbbb SandFly", "aaaaaaa Old", apk = "sandfly.apk")
    val move = UpdateChecker.parseMove(json, "aaaaaaa0")
    assertTrue(move!!.move)
    assertEquals(5, move.build)
    assertEquals("https://example.com/sandfly.apk", move.downloadUrl)
    assertEquals(listOf("SandFly"), move.changes)
  }

  @Test(expected = JSONException::class)
  fun moveNeverTakesABacktalkBuild() {
    UpdateChecker.parseMove(release(5, apk = "backtalk.apk"), "aaaaaaa0")
  }

  @Test
  fun theNewestReleaseIsOffered() {
    val stable = release(12, "ccccccc Stable")
    val dev = release(15, "ddddddd Dev", "ccccccc Stable")
    assertEquals(15, UpdateChecker.newest(listOf(stable, dev), 10, "")!!.build)
    assertEquals(12, UpdateChecker.newest(listOf(stable, release(11)), 10, "")!!.build)
  }

  @Test
  fun aDevelopmentBuildIsOfferedTheNextStableRelease() {
    assertEquals(12, UpdateChecker.newest(listOf(release(12)), 11, "")!!.build)
    assertNull(UpdateChecker.newest(listOf(release(12), release(11)), 12, ""))
  }

  private fun releaseWithBothBuilds(build: Int): String {
    val release = JSONObject(release(build))
    release
      .getJSONArray("assets")
      .put(
        JSONObject()
          .put("name", "sandfly-wear.apk")
          .put("browser_download_url", "https://example.com/sandfly-wear.apk")
      )
    return release.toString()
  }

  @Test
  fun eachDeviceGetsItsOwnBuild() {
    val json = releaseWithBothBuilds(11)
    assertEquals(
      "https://example.com/sandfly.apk",
      UpdateChecker.parseRelease(json, 10, "", UpdateChecker.PHONE_APK_NAME)!!.downloadUrl,
    )
    assertEquals(
      "https://example.com/sandfly-wear.apk",
      UpdateChecker.parseRelease(json, 10, "", UpdateChecker.WATCH_APK_NAME)!!.downloadUrl,
    )
  }

  @Test(expected = JSONException::class)
  fun aWatchNeverTakesThePhoneBuild() {
    UpdateChecker.parseRelease(release(11), 10, "", UpdateChecker.WATCH_APK_NAME)
  }
}
