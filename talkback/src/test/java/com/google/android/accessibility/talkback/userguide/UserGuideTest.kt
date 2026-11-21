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
}
