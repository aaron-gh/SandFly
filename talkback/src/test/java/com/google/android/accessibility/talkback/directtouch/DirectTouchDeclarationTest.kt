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

import android.graphics.RectF
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
          windowArea = RectF(0f, 0.5f, 0.75f, 1f),
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
            <area><screen /></area>
            <screen state="menu" />
        </direct-touch>
        """
      )

    // The screen inside the area is too deep to count, and the area itself is the whole window.
    assertEquals(
      listOf(
        DirectTouchRule(null, null, screen = false, windowArea = RectF(0f, 0f, 1f, 1f)),
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
          DirectTouchRule(GAME, null, screen = false, windowArea = RectF(0f, 0f, 1f, 0.5f)),
          DirectTouchRule(null, "playing", screen = true),
          DirectTouchRule(MENU, "paused", screen = false, viewId = "p:id/resume"),
        )
      )

    assertEquals(
      DirectTouchTarget.Areas(
        windowAreas = listOf(RectF(0f, 0f, 1f, 0.5f)),
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
  fun fractionsArePercentagesOrNumbersWithinTheWindow() {
    assertEquals(0.5f, DirectTouchDeclaration.parseFraction("50%"))
    assertEquals(0.25f, DirectTouchDeclaration.parseFraction("0.25"))
    assertEquals(1f, DirectTouchDeclaration.parseFraction("150%"))
    assertEquals(0f, DirectTouchDeclaration.parseFraction("-2"))
    assertNull(DirectTouchDeclaration.parseFraction("NaN"))
    assertNull(DirectTouchDeclaration.parseFraction("wide"))
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

  private companion object {
    const val GAME = "p.Game"
    const val MENU = "p.Menu"
  }
}
