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

package com.google.android.accessibility.talkback.overview

import android.os.Process
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreviousTaskTest {
  private fun task(id: Int, packageName: String, topPackage: String? = packageName) =
    RecentTask(
      id = id,
      packageName = packageName,
      component = null,
      user = Process.myUserHandle(),
      appLabel = packageName,
      title = null,
      topPackage = topPackage,
    )

  private val a = task(1, "app.a")
  private val b = task(2, "app.b")
  private val c = task(3, "app.c")

  private fun previous(
    tasks: List<RecentTask>,
    front: List<String>,
    excluded: Set<String> = setOf("launcher"),
  ) = RecentTasks.previousTask(tasks, front, excluded)

  @Test
  fun frontIndex_appInFront() {
    assertEquals(0, RecentTasks.frontIndex(listOf(a, b, c), listOf("app.a")))
  }

  @Test
  fun frontIndex_homeScreenIsBeforeTheFirstTask() {
    assertEquals(-1, RecentTasks.frontIndex(listOf(a, b, c), listOf("launcher")))
  }

  @Test
  fun frontIndex_splitScreenCountsBothApps() {
    assertEquals(1, RecentTasks.frontIndex(listOf(a, b, c), listOf("app.b", "app.a")))
  }

  @Test
  fun frontIndex_unknownWindowsTakeTheFirstTask() {
    assertEquals(0, RecentTasks.frontIndex(listOf(a, b), emptyList()))
  }

  @Test
  fun fromAnApp_takesTheNextTask() {
    assertEquals(b, previous(listOf(a, b, c), listOf("app.a")))
  }

  @Test
  fun fromTheLauncher_takesTheMostRecentTask() {
    assertEquals(a, previous(listOf(a, b, c), listOf("launcher")))
  }

  @Test
  fun listedLauncherIsLeftOut() {
    assertEquals(a, previous(listOf(task(9, "launcher"), a, b), listOf("launcher")))
  }

  @Test
  fun launcherIsNotChosenFromAnApp() {
    assertEquals(c, previous(listOf(a, task(9, "launcher"), c), listOf("app.a")))
  }

  @Test
  fun noOtherTask_isNull() {
    assertNull(previous(listOf(a), listOf("app.a")))
  }

  @Test
  fun noTasks_isNull() {
    assertNull(previous(emptyList(), listOf("app.a")))
  }

  @Test
  fun splitScreen_skipsBothApps() {
    assertEquals(c, previous(listOf(a, b, c), listOf("app.a", "app.b")))
  }

  @Test
  fun secondTaskOfTheSameApp_isTheNextTask() {
    val second = task(4, "app.a")
    assertEquals(second, previous(listOf(a, second, b), listOf("app.a")))
  }

  @Test
  fun taskMatchesByItsTopActivity() {
    val shared = task(5, "app.opener", topPackage = "app.viewer")
    assertEquals(b, previous(listOf(shared, b), listOf("app.viewer")))
  }

  @Test
  fun appNotInTheList_takesTheFirstTask() {
    assertEquals(a, previous(listOf(a, b), listOf("app.excluded.from.recents")))
  }

  @Test
  fun unknownWindows_takesTheSecondTask() {
    assertEquals(b, previous(listOf(a, b, c), emptyList()))
  }

  private fun afterRead(
    elapsedMs: Long = 100,
    locked: Boolean = false,
    tasksRead: Boolean = true,
    fromOverview: Boolean = false,
  ) = PreviousApp.afterRead(elapsedMs, locked, tasksRead, fromOverview)

  @Test
  fun tasksReadInTime_switches() {
    assertEquals(PreviousApp.AfterRead.SWITCH, afterRead())
    assertEquals(PreviousApp.AfterRead.SWITCH, afterRead(elapsedMs = PreviousApp.READ_LIMIT_MS))
  }

  @Test
  fun lateResult_saysFailed() {
    val late = PreviousApp.READ_LIMIT_MS + 1
    assertEquals(PreviousApp.AfterRead.SAY_FAILED, afterRead(elapsedMs = late))
    assertEquals(PreviousApp.AfterRead.SAY_FAILED, afterRead(elapsedMs = late, tasksRead = false))
  }

  @Test
  fun lockedDuringRead_doesNothing() {
    assertEquals(PreviousApp.AfterRead.NOTHING, afterRead(locked = true))
    assertEquals(
      PreviousApp.AfterRead.NOTHING,
      afterRead(elapsedMs = PreviousApp.READ_LIMIT_MS + 1, locked = true, tasksRead = false),
    )
  }

  @Test
  fun failedRead_performsRecentsTwice() {
    assertEquals(PreviousApp.AfterRead.PERFORM_RECENTS_TWICE, afterRead(tasksRead = false))
  }

  @Test
  fun failedReadFromOverview_saysUnavailable() {
    assertEquals(
      PreviousApp.AfterRead.SAY_UNAVAILABLE,
      afterRead(tasksRead = false, fromOverview = true),
    )
  }
}
