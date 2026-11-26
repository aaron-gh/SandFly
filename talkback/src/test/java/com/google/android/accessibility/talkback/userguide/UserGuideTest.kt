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

package com.google.android.accessibility.talkback.userguide

import android.content.Context
import com.google.android.accessibility.talkback.keyboard.KeyComboManager
import com.google.android.accessibility.utils.FormFactorUtils
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UserGuideTest {

  /** The page has every chapter and the lists made from the defaults of gestures and keymaps. */
  @Test
  fun buildsTheGuideWithItsGeneratedLists() {
    val context = RuntimeEnvironment.getApplication() as Context
    FormFactorUtils.initialize(context)
    val html = UserGuide.build(context, KeyComboManager.create(context))
    // For reading the page in a browser.
    File("build/user-guide-test.html").writeText(html)
    assertTrue(html.contains("<h2 id=\"getting-started\">1 "))
    assertTrue(html.contains("<h3 id=\"appendix-gestures-sandfly\">A.1 "))
    assertTrue(html.contains("<h3 id=\"appendix-gestures-voiceover\">A.2 "))
    assertTrue(html.contains("<h3 id=\"appendix-keyboard-shortcuts-sandfly\">B.1 "))
    assertTrue(html.contains("<h3 id=\"api-scripting-the-manifest\"") || html.contains("id=\"scripting-the-manifest\""))
    // Gestures and shortcuts are listed with their commands.
    assertTrue(Regex("<td>[^<]+</td><td>[^<]+</td>").findAll(html).count() > 200)
  }

  /** The Help action of the Voice profiles screen opens only that section. */
  @Test
  fun buildsTheSectionOfASettingsScreen() {
    val context = RuntimeEnvironment.getApplication() as Context
    FormFactorUtils.initialize(context)
    val html = UserGuide.build(context, KeyComboManager.create(context), "speech-voice-profiles")
    assertTrue(html.contains("<h3 id=\"speech-voice-profiles\">"))
    assertTrue(html.contains("<h4 id=\"speech-add-voice-profile\">"))
    assertFalse(html.contains("id=\"speech-emoji\""))
    assertFalse(html.contains("<nav"))
    assertTrue(html.contains("href=\"?section=contents\""))
  }

  /** Under Indonesian locale, the guide loads docs/user-guide/id/ with Indonesian headings and lists. */
  @Test
  @Config(sdk = [35], qualifiers = "in-rID")
  fun buildsTheIndonesianGuideWithItsGeneratedLists() {
    val context = RuntimeEnvironment.getApplication() as Context
    FormFactorUtils.initialize(context)
    val html = UserGuide.build(context, KeyComboManager.create(context))
    val diag =
      "locale=${context.resources.configuration.locales[0]} assets=${context.assets.list("user-guide")?.toList()} head=${html.take(250)}"
    assertTrue(diag, html.contains("<html lang=\"id\""))
    assertTrue(diag, html.contains("<title>Panduan Pengguna SandFly</title>"))
    assertTrue(diag, html.contains("<h2 id=\"getting-started\">1 "))
    assertTrue(diag, html.contains("<h2 id=\"appendix-gestures\">Lampiran A:"))
    assertTrue(diag, html.contains("<h3 id=\"appendix-gestures-sandfly\">A.1 "))
    assertTrue(diag, html.contains("<p class=\"note\">Catatan:"))
    assertTrue(Regex("<td>[^<]+</td><td>[^<]+</td>").findAll(html).count() > 200)
  }

  /** Under Dutch locale, the guide loads docs/user-guide/nl/ with Dutch headings and lists. */
  @Test
  @Config(sdk = [35], qualifiers = "nl-rNL")
  fun buildsTheDutchGuideWithItsGeneratedLists() {
    val context = RuntimeEnvironment.getApplication() as Context
    FormFactorUtils.initialize(context)
    val html = UserGuide.build(context, KeyComboManager.create(context))
    val diag =
      "locale=${context.resources.configuration.locales[0]} assets=${context.assets.list("user-guide")?.toList()} head=${html.take(250)}"
    assertTrue(diag, html.contains("<html lang=\"nl\""))
    assertTrue(diag, html.contains("<title>SandFly-gebruikershandleiding</title>"))
    assertTrue(diag, html.contains("<h2 id=\"getting-started\">1 "))
    assertTrue(diag, html.contains("<h2 id=\"appendix-gestures\">Bijlage A:"))
    assertTrue(diag, html.contains("<h3 id=\"appendix-gestures-sandfly\">A.1 "))
    assertTrue(diag, html.contains("<p class=\"note\">Opmerking:"))
    assertTrue(Regex("<td>[^<]+</td><td>[^<]+</td>").findAll(html).count() > 200)
  }

  @Test
  @Config(sdk = [35], qualifiers = "pl-rPL")
  fun buildsThePolishGuideWithItsGeneratedListsAndTranslatedReferences() {
    val context = RuntimeEnvironment.getApplication() as Context
    FormFactorUtils.initialize(context)
    val html = UserGuide.build(context, KeyComboManager.create(context))
    assertTrue(html.contains("<html lang=\"pl\""))
    assertTrue(html.contains("<title>Podręcznik użytkownika SandFly</title>"))
    assertTrue(html.contains("<h2 id=\"getting-started\">1 Pierwsze kroki</h2>"))
    assertTrue(html.contains("<h3 id=\"appendix-gestures-sandfly\">A.1 "))
    assertTrue(html.contains("<h3 id=\"appendix-keyboard-shortcuts-sandfly\">B.1 "))
    assertTrue(html.contains("<h3 id=\"scripting-rules\">D.5 Reguły</h3>"))
    assertTrue(html.contains("href=\"#item-files-macros\""))
    assertTrue(html.contains("<p class=\"note\">Uwaga:"))
    assertTrue(Regex("<td>[^<]+</td><td>[^<]+</td>").findAll(html).count() > 200)
  }
}
