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

package com.google.android.accessibility.talkback.macros

import com.google.android.accessibility.talkback.macros.MacroRecording.AppOpened
import com.google.android.accessibility.talkback.macros.MacroRecording.Command
import com.google.android.accessibility.talkback.macros.MacroRecording.Press
import com.google.android.accessibility.talkback.macros.MacroRecording.Scroll
import com.google.android.accessibility.talkback.macros.MacroRecording.Type
import com.google.android.accessibility.talkback.macros.Macros.ActOnItem
import com.google.android.accessibility.talkback.macros.Macros.AppInFront
import com.google.android.accessibility.talkback.macros.Macros.Clause
import com.google.android.accessibility.talkback.macros.Macros.Condition
import com.google.android.accessibility.talkback.macros.Macros.EndIf
import com.google.android.accessibility.talkback.macros.Macros.EndRepeat
import com.google.android.accessibility.talkback.macros.Macros.If
import com.google.android.accessibility.talkback.macros.Macros.ItemAct
import com.google.android.accessibility.talkback.macros.Macros.ItemOnScreen
import com.google.android.accessibility.talkback.macros.Macros.Kind
import com.google.android.accessibility.talkback.macros.Macros.OpenApp
import com.google.android.accessibility.talkback.macros.Macros.RepeatTimes
import com.google.android.accessibility.talkback.macros.Macros.RepeatWhile
import com.google.android.accessibility.talkback.macros.Macros.RunCommand
import com.google.android.accessibility.talkback.macros.Macros.Tap
import com.google.android.accessibility.talkback.macros.Macros.TapPosition
import com.google.android.accessibility.talkback.macros.Macros.TouchHold
import com.google.android.accessibility.talkback.macros.Macros.TypeText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MacroRecordingTest {
  private val app = "com.example.app"
  private val default = Macros.DEFAULT_TAP_TIMEOUT_MS

  private fun press(time: Long, label: String, shown: Set<String>? = null, inScrolled: Boolean = false) =
    Press(time, app, label, inScrolled = inScrolled, shownAfter = shown)

  private fun key(label: String) = MacroRecording.labelKey(label, null)

  private fun not(test: Macros.Test) = Condition(listOf(Clause(test, not = true)))

  private fun cond(test: Macros.Test) = Condition(listOf(Clause(test)))

  @Test
  fun scrollsThenPressInList_scrollUntilItemShown() {
    val steps =
      MacroRecording.steps(
        listOf(
          Scroll(100, app, "list", Macros.Field.RESOURCE_ID, forward = true, count = 4),
          Scroll(200, app, "list", Macros.Field.RESOURCE_ID, forward = false),
          press(300, "About phone", inScrolled = true),
        ),
        startTime = 0,
      )
    assertEquals(
      listOf(
        RepeatWhile(not(ItemOnScreen("About phone"))),
        ActOnItem("list", ItemAct.SCROLL_FORWARD, 1, field = Macros.Field.RESOURCE_ID),
        EndRepeat,
        Tap("About phone", default),
      ),
      steps,
    )
    assertNull(Macros.analyze(steps).problem)
  }

  @Test
  fun scrollThenPressOutsideList_plainScroll() {
    val steps =
      MacroRecording.steps(
        listOf(Scroll(100, app, "", null, forward = true, count = 2), press(300, "Menu")),
        startTime = 0,
      )
    assertEquals(listOf(ActOnItem("", ItemAct.SCROLL_FORWARD, 2), Tap("Menu", default)), steps)
  }

  @Test
  fun samePressThreeTimes_repeatTimes() {
    val steps =
      MacroRecording.steps(
        listOf(press(100, "Add"), press(200, "Add"), press(300, "Add"), press(400, "Done")),
        startTime = 0,
      )
    assertEquals(listOf(RepeatTimes(3), Tap("Add", default), EndRepeat, Tap("Done", default)), steps)
  }

  @Test
  fun runRepeatedUntilItemGone_repeatWhileShown() {
    val shown = setOf(key("Remove"), key("OK"))
    val gone = setOf(key("OK"))
    val steps =
      MacroRecording.steps(
        listOf(
          press(100, "Remove"),
          press(200, "OK", shown),
          press(300, "Remove"),
          press(400, "OK", shown),
          press(500, "Remove"),
          press(600, "OK", gone),
        ),
        startTime = 0,
      )
    assertEquals(
      listOf(RepeatWhile(cond(ItemOnScreen("Remove"))), Tap("Remove", default), Tap("OK", default), EndRepeat),
      steps,
    )
  }

  @Test
  fun switchTurnedOn_pressedOnlyWhenOff() {
    val steps =
      MacroRecording.steps(listOf(Press(100, app, "Wi-Fi", checkedBefore = false)), startTime = 0)
    assertEquals(
      listOf(If(not(ItemOnScreen("Wi-Fi", Kind.CHECKED))), Tap("Wi-Fi", default), EndIf),
      steps,
    )
  }

  @Test
  fun switchTurnedOff_pressedOnlyWhenOn() {
    val steps =
      MacroRecording.steps(listOf(Press(100, app, "Wi-Fi", checkedBefore = true)), startTime = 0)
    assertEquals(listOf(If(cond(ItemOnScreen("Wi-Fi", Kind.CHECKED))), Tap("Wi-Fi", default), EndIf), steps)
  }

  @Test
  fun startApp_openedFirstWhenNotInFront_endingPressesLeftOut() {
    val steps =
      MacroRecording.steps(
        listOf(
          press(100, "Compose"),
          Command(200, app, "NOTIFICATIONS"),
          Press(300, MacroRecording.SYSTEM_UI, "SandFly"),
        ),
        startTime = 0,
        startPkg = app,
        startApp = "OPEN_APP:$app/.Main",
        trailing = setOf("NOTIFICATIONS"),
      )
    assertEquals(
      listOf(If(not(AppInFront(app))), OpenApp("OPEN_APP:$app/.Main"), EndIf, Tap("Compose", default)),
      steps,
    )
  }

  @Test
  fun appOpenedAndCommands() {
    val steps =
      MacroRecording.steps(
        listOf(AppOpened(100, "launcher", "OPEN_APP:$app/.Main"), press(200, "Search"), Command(300, app, "BACK")),
        startTime = 0,
        startPkg = "other",
        startApp = "OPEN_APP:other/.Main",
      )
    assertEquals(listOf(OpenApp("OPEN_APP:$app/.Main"), Tap("Search", default), RunCommand("BACK")), steps)
  }

  @Test
  fun typingWithoutPress_tapsFieldFirst() {
    val steps =
      MacroRecording.steps(
        listOf(
          Type(100, app, "Search", Macros.Field.HINT, "weather", afterPress = false),
          press(200, "Go"),
          Press(300, app, "Search", Macros.Field.HINT, editable = true),
          Type(400, app, "Search", Macros.Field.HINT, "news", afterPress = true),
        ),
        startTime = 0,
      )
    assertEquals(
      listOf(
        Tap("Search", default, field = Macros.Field.HINT),
        TypeText("weather", replace = true),
        Tap("Go", default),
        Tap("Search", default, field = Macros.Field.HINT),
        TypeText("news", replace = true),
      ),
      steps,
    )
  }

  @Test
  fun longWait_longerTimeout() {
    val steps = MacroRecording.steps(listOf(press(10_000, "Send")), startTime = 0)
    assertEquals(listOf(Tap("Send", 15_000)), steps)
  }

  @Test
  fun pressesWithoutLabel_positionOrLeftOut_heldTouchKept() {
    val steps =
      MacroRecording.steps(
        listOf(
          Press(100, app, "", x = 10, y = 20),
          Press(200, app, "", long = true),
          Press(300, app, "Record", long = true, held = true),
          Press(400, app, "Message", long = true),
        ),
        startTime = 0,
      )
    assertEquals(
      listOf(
        TapPosition(10, 20),
        TouchHold("Record", timeoutMillis = default),
        ActOnItem("Message", ItemAct.LONG_PRESS, 1, default),
      ),
      steps,
    )
  }

  @Test
  fun maxSteps_wholeBlocksOnly() {
    val steps =
      MacroRecording.steps(
        listOf(press(100, "A"), Press(200, app, "Wi-Fi", checkedBefore = false)),
        startTime = 0,
        maxSteps = 3,
      )
    assertEquals(listOf(Tap("A", default)), steps)
  }

  @Test
  fun scrollWithoutLabel_savedAndRead() {
    val macro = Macros.Macro("id", "Scroll", listOf(ActOnItem("", ItemAct.SCROLL_FORWARD, 2)))
    assertEquals(macro, Macros.macroFromJson(Macros.macroToJson(macro)))
    val longPress = Macros.Macro("id", "Long", listOf(ActOnItem("", ItemAct.LONG_PRESS)))
    assertEquals(emptyList<Macros.Step>(), Macros.macroFromJson(Macros.macroToJson(longPress))?.steps)
  }

  @Test
  fun wholeText_escapedAndAnchored() {
    assertEquals("^System$", MacroRecording.wholeText("System"))
    assertEquals("^Sound & vibration \\(on\\)$", MacroRecording.wholeText("Sound & vibration (on)"))
    assertEquals("^Long start", MacroRecording.wholeText("Long start", prefix = true))
    val pattern = Macros.compilePattern(MacroRecording.wholeText("System"))!!
    assertEquals(false, pattern.matcher("Android System notification:").find())
    assertEquals(true, pattern.matcher("system").find())
    assertEquals(true, Macros.compilePattern(MacroRecording.wholeText("a+b (1) [x] {2} \\ $ ^ | ? * ."))!!.matcher("a+b (1) [x] {2} \\ $ ^ | ? * .").find())
  }

  @Test
  fun regexLabels_keptInSteps() {
    val steps =
      MacroRecording.steps(
        listOf(
          Scroll(100, app, "^list$", Macros.Field.RESOURCE_ID, forward = true, regex = true),
          Press(200, app, "^System$", regex = true, inScrolled = true),
          Press(300, app, "^Wi-Fi$", regex = true, checkedBefore = false),
        ),
        startTime = 0,
      )
    assertEquals(
      listOf(
        RepeatWhile(not(ItemOnScreen("^System$", regex = true))),
        ActOnItem("^list$", ItemAct.SCROLL_FORWARD, 1, regex = true, field = Macros.Field.RESOURCE_ID),
        EndRepeat,
        Tap("^System$", default, regex = true),
        If(not(ItemOnScreen("^Wi-Fi$", Kind.CHECKED, regex = true))),
        Tap("^Wi-Fi$", default, regex = true),
        EndIf,
      ),
      steps,
    )
  }
}
