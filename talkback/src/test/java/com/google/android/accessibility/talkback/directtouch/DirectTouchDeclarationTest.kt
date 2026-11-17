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

package com.google.android.accessibility.talkback.directtouch

import android.graphics.Rect
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParserFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DirectTouchDeclarationTest {

  @Test
  fun readsScreenAndAreaRules() {
    val declaration =
      parse(
        """
        <direct-touch>
            <screen activity=".GameActivity" />
            <area activity="MainActivity" state="playing" view="@id/board" />
            <area activity="com.other.Shared" top="50%" right="0.75" />
            <area bottom="100%" height="48.0dip" left="120px" />
            <area activity="*" view="com.example.game:id/stick" />
        </direct-touch>
        """
      )

    assertEquals(
      listOf(
        DirectTouchRule("com.example.game.GameActivity", null, screen = true),
        DirectTouchRule(
          "com.example.game.MainActivity",
          "playing",
          screen = false,
          viewId = "com.example.game:id/board",
        ),
        DirectTouchRule(
          "com.other.Shared",
          null,
          screen = false,
          windowArea = WindowArea(top = fraction(0.5f), right = fraction(0.75f)),
        ),
        DirectTouchRule(
          null,
          null,
          screen = false,
          windowArea =
            WindowArea(
              left = AreaLength(120f, AreaLength.Unit.PX),
              bottom = fraction(1f),
              height = AreaLength(48f, AreaLength.Unit.DP),
            ),
        ),
        DirectTouchRule(null, null, screen = false, viewId = "com.example.game:id/stick"),
      ),
      declaration.rules,
    )
    assertTrue(declaration.namesActivities)
  }

  @Test
  fun skipsUnknownElementsEmptyAreasAndBadNumbers() {
    val declaration =
      parse(
        """
        <direct-touch>
            <keyboard />
            <area top="60%" bottom="40%" />
            <area left="half" />
            <area height="-4dp" />
            <area height="0dp" />
            <area><screen /></area>
            <screen state="menu" />
        </direct-touch>
        """
      )

    // The screen inside the area is too deep to count, and the area itself is the whole window.
    assertEquals(
      listOf(
        DirectTouchRule(null, null, screen = false, windowArea = WindowArea()),
        DirectTouchRule(null, "menu", screen = true),
      ),
      declaration.rules,
    )
    assertFalse(declaration.namesActivities)
  }

  @Test
  fun keepsRulesBeforeAnError() {
    val declaration = parse("<direct-touch><screen state=\"a\" /><area")

    assertEquals(listOf(DirectTouchRule(null, "a", screen = true)), declaration.rules)
  }

  @Test
  fun screenWinsOverAreasAndRulesMatchActivityAndState() {
    val declaration =
      DirectTouchDeclaration(
        listOf(
          DirectTouchRule(GAME, null, screen = false, viewId = "p:id/board"),
          DirectTouchRule(GAME, null, screen = false, windowArea = TOP_HALF),
          DirectTouchRule(null, "playing", screen = true),
          DirectTouchRule(MENU, "paused", screen = false, viewId = "p:id/resume"),
        )
      )

    assertEquals(
      DirectTouchTarget.Areas(
        windowAreas = listOf(TOP_HALF),
        viewIds = listOf("p:id/board"),
      ),
      declaration.target(GAME, null),
    )
    assertEquals(DirectTouchTarget.Screen, declaration.target(GAME, "playing"))
    assertEquals(DirectTouchTarget.Screen, declaration.target(null, "playing"))
    assertEquals(DirectTouchTarget.None, declaration.target(MENU, null))
    assertEquals(
      DirectTouchTarget.Areas(viewIds = listOf("p:id/resume")),
      declaration.target(MENU, "paused"),
    )
  }

  @Test
  fun lengthsAreFractionsDpOrPixels() {
    assertEquals(fraction(0.5f), AreaLength.parse("50%"))
    assertEquals(fraction(0.5f), AreaLength.parse("50.0%"))
    assertEquals(fraction(0.25f), AreaLength.parse("0.25"))
    assertEquals(fraction(1f), AreaLength.parse("150%"))
    assertEquals(AreaLength(48f, AreaLength.Unit.DP), AreaLength.parse("48dp"))
    assertEquals(AreaLength(48f, AreaLength.Unit.DP), AreaLength.parse("48.0dip"))
    assertEquals(AreaLength(120f, AreaLength.Unit.PX), AreaLength.parse("120 px"))
    assertNull(AreaLength.parse("-2"))
    assertNull(AreaLength.parse("NaN"))
    assertNull(AreaLength.parse("wide"))
    assertNull(AreaLength.parse("12sp"))
  }

  @Test
  fun edgesAreMeasuredFromTheWindowsTopLeftCorner() {
    // A window below a 100 px status bar, on a display of density 3.
    val window = Rect(0, 100, 1000, 2100)

    assertEquals(Rect(0, 700, 1000, 2100), WindowArea(top = fraction(0.3f)).resolve(window, 3f))
    assertEquals(Rect(0, 100, 1000, 700), WindowArea(bottom = fraction(0.3f)).resolve(window, 3f))
    assertEquals(
      Rect(300, 100, 700, 2100),
      WindowArea(left = fraction(0.3f), right = fraction(0.7f)).resolve(window, 3f),
    )
    // A 200 dp strip along the bottom, and a 150 px square in the top left corner.
    assertEquals(
      Rect(0, 1500, 1000, 2100),
      WindowArea(bottom = fraction(1f), height = AreaLength(200f, AreaLength.Unit.DP))
        .resolve(window, 3f),
    )
    assertEquals(
      Rect(0, 100, 150, 250),
      WindowArea(width = PX_150, height = PX_150).resolve(window, 3f),
    )
    // Pixels past the window are cut off, and an area wholly outside it is nothing.
    assertEquals(
      Rect(900, 100, 1000, 2100),
      WindowArea(left = AreaLength(900f, AreaLength.Unit.PX), width = PX_150).resolve(window, 3f),
    )
    assertNull(WindowArea(top = AreaLength(5000f, AreaLength.Unit.PX)).resolve(window, 3f))
  }

  @Test
  fun viewIdsAreNamedAsAndroidNamesThem() {
    assertEquals("p:id/a", DirectTouchDeclaration.fullViewId("p", "a"))
    assertEquals("p:id/a", DirectTouchDeclaration.fullViewId("p", "@id/a"))
    assertEquals("p:id/a", DirectTouchDeclaration.fullViewId("p", "@+id/a"))
    assertEquals("q:id/a", DirectTouchDeclaration.fullViewId("p", "q:id/a"))
  }

  private fun parse(xml: String): DirectTouchDeclaration {
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(StringReader(xml.trimIndent()))
    return DirectTouchDeclaration.parse(parser, "com.example.game") { null }
  }

  private fun fraction(value: Float) = AreaLength(value, AreaLength.Unit.FRACTION)

  private companion object {
    val TOP_HALF = WindowArea(bottom = AreaLength(0.5f, AreaLength.Unit.FRACTION))
    val PX_150 = AreaLength(150f, AreaLength.Unit.PX)
    const val GAME = "p.Game"
    const val MENU = "p.Menu"
  }
}
