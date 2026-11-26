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

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideMarkdownTest {

  private val strings =
    GuideMarkdown.Strings(
      title = "SandFly User Guide",
      contents = "Contents",
      appendix = { letter, title -> "Appendix $letter: $title" },
      backToContents = "Back to contents",
    )

  private class Files(val included: Map<String, String> = emptyMap(), val generated: Map<String, String> = emptyMap()) :
    GuideMarkdown.Sources {
    override fun include(name: String): String? = included[name]

    override fun generate(name: String): String? = generated[name]
  }

  private fun render(vararg chapters: Pair<String, String>, sources: GuideMarkdown.Sources = Files()): String =
    GuideMarkdown.render(
      chapters.map { GuideMarkdown.Chapter(it.first, it.second.trimIndent(), "en") },
      sources,
      strings,
      lang = "en",
      rtl = false,
      dark = false,
    )

  @Test
  fun numbersChaptersSectionsAndAppendices() {
    val html =
      render(
        "01-a.md" to
          """
          ## Get started {#start}
          ### Install {#start-install}
          ### Turn on {#start-on}
          #### With the shortcut {#start-on-shortcut}
          """,
        "02-b.md" to "## Speech {#speech}",
        "appendix-a.md" to
          """
          ## Gestures {#appendix-gestures}
          ### One finger {#appendix-gestures-one}
          """,
      )
    assertTrue(html.contains("<h2 id=\"start\">1 Get started</h2>"))
    assertTrue(html.contains("<h3 id=\"start-on\">1.2 Turn on</h3>"))
    assertTrue(html.contains("<h4 id=\"start-on-shortcut\">1.2.1 With the shortcut</h4>"))
    assertTrue(html.contains("<h2 id=\"speech\">2 Speech</h2>"))
    assertTrue(html.contains("<h2 id=\"appendix-gestures\">Appendix A: Gestures</h2>"))
    assertTrue(html.contains("<h3 id=\"appendix-gestures-one\">A.1 One finger</h3>"))
    // The contents list chapters and sections, not subsections.
    assertTrue(html.contains("<li><a href=\"#start-install\">1.1 Install</a></li>"))
    assertFalse(html.contains("<a href=\"#start-on-shortcut\">"))
  }

  @Test
  fun linksShowTheNumberAndTitleOfTheirHeading() {
    val html =
      render(
        "01-a.md" to
          """
          ## Speech {#speech}
          ### Change the rate {#speech-rate}
          See [](#speech-rate). Or [the rate](#speech-rate), [missing](#nothing) and [web](https://example.com).
          """
      )
    assertTrue(html.contains("See <a href=\"#speech-rate\">1.1 Change the rate</a>."))
    assertTrue(html.contains("<a href=\"#speech-rate\">the rate</a>"))
    assertTrue(html.contains(", missing and"))
    assertTrue(html.contains("<a href=\"https://example.com\">web</a>"))
  }

  @Test
  fun rendersListsTablesCodeNotesAndPaths() {
    val html =
      render(
        "01-a.md" to
          """
          ## Speech {#speech}

          1. Go to **Speech** > **Voice profiles**.
          2. Choose a profile.
             - Inside
             - Also inside
          3. Done.

          | Setting | Default |
          | --- | --- |
          | **Rate** | `1.0` \| fast |

          ```
          sf.speak("<hi>")
          ```

          Note: Text `a*b*c` stays_as_is.
          """
      )
    assertTrue(html.contains("<ol>\n<li>Go to <strong>Speech</strong><span class=\"sep\">"))
    assertTrue(html.contains("<li>Choose a profile.\n<ul>\n<li>Inside</li>\n<li>Also inside</li>\n</ul>\n</li>"))
    assertTrue(html.contains("<li>Done.</li>\n</ol>"))
    assertTrue(html.contains("<th scope=\"col\">Setting</th>"))
    assertTrue(html.contains("<td><code>1.0</code> | fast</td>"))
    assertTrue(html.contains("<pre><code>sf.speak(&quot;&lt;hi&gt;&quot;)</code></pre>"))
    assertTrue(html.contains("<p class=\"note\">Note: Text <code>a*b*c</code> stays_as_is.</p>"))
  }

  @Test
  fun includedDocumentsMoveDownALevelAndKeepTheirLinks() {
    val scripting =
      """
      # Writing SandFly scripts

      Read [the manifest](#the-manifest), the [speech API](speech-api.md) and [examples](scripting/examples).

      ## The manifest

      ### Where a script runs
      """.trimIndent()
    val html =
      render(
        "appendix-d.md" to
          """
          ## Scripting API {#api-scripting}
          <!-- include: scripting.md -->
          """,
        "appendix-e.md" to
          """
          ## Speech API {#api-speech}
          <!-- include: speech-api.md -->
          """,
        sources = Files(mapOf("scripting.md" to scripting, "speech-api.md" to "# Speech\n\nText.")),
      )
    assertFalse(html.contains("Writing SandFly scripts"))
    assertTrue(html.contains("<h3 id=\"scripting-the-manifest\">A.1 The manifest</h3>"))
    assertTrue(html.contains("<h4 id=\"scripting-where-a-script-runs\">A.1.1 Where a script runs</h4>"))
    assertTrue(html.contains("<a href=\"#scripting-the-manifest\">the manifest</a>"))
    assertTrue(html.contains("<a href=\"#api-speech\">speech API</a>"))
    assertTrue(html.contains("<a href=\"${GuideMarkdown.REPOSITORY_DOCS}scripting/examples\">examples</a>"))
  }

  @Test
  fun includedDocumentsKeepExplicitHeadingIds() {
    val html =
      render(
        "appendix-d.md" to
          """
          ## API skryptów {#api-scripting}
          <!-- include: scripting.md -->
          """,
        sources =
          Files(
            included = mapOf("scripting.md" to "# API\n\n## Reguły {#rules}\n\nZobacz [](#rules)."),
          ),
      )
    assertTrue(html.contains("<h3 id=\"scripting-rules\">A.1 Reguły</h3>"))
    assertTrue(html.contains("<a href=\"#scripting-rules\">A.1 Reguły</a>"))
  }

  @Test
  fun generatedPartsAreInserted() {
    val html =
      render(
        "appendix-a.md" to
          """
          ## Gestures {#appendix-gestures}
          <!-- generate: gestures -->
          """,
        sources = Files(generated = mapOf("gestures" to "### SandFly {#appendix-gestures-sandfly}\n\n| Gesture | Command |\n| --- | --- |\n| Swipe right | Next |")),
      )
    assertTrue(html.contains("<h3 id=\"appendix-gestures-sandfly\">A.1 SandFly</h3>"))
    assertTrue(html.contains("<td>Swipe right</td><td>Next</td>"))
  }

  @Test
  fun sectionPageHasOnlyTheSectionAndLinksOutToOthers() {
    val chapters =
      listOf(
        "01-a.md" to
          """
          ## Get started {#start}
          ### Install {#start-install}
          See [](#start-on-shortcut) and [](#start-install-play).
          #### From Google Play {#start-install-play}
          Text.
          ### Turn on {#start-on}
          #### With the shortcut {#start-on-shortcut}
          """,
        "02-b.md" to "## Speech {#speech}",
      )
    val html =
      GuideMarkdown.render(
        chapters.map { GuideMarkdown.Chapter(it.first, it.second.trimIndent(), "en") },
        Files(),
        strings,
        lang = "en",
        rtl = false,
        dark = false,
        section = "start-install",
      )
    assertTrue(html.contains("<h3 id=\"start-install\">1.1 Install</h3>"))
    assertTrue(html.contains("<h4 id=\"start-install-play\">1.1.1 From Google Play</h4>"))
    assertFalse(html.contains("id=\"start-on\""))
    assertFalse(html.contains("id=\"speech\""))
    assertFalse(html.contains("<nav"))
    // Links within the section stay on the page; others open their own section.
    assertTrue(html.contains("<a href=\"#start-install-play\">1.1.1 From Google Play</a>"))
    assertTrue(html.contains("<a href=\"?section=start-on-shortcut\">1.2.1 With the shortcut</a>"))
    assertTrue(html.contains("<a href=\"?section=contents\">Back to contents</a>"))
  }

  @Test
  fun unknownSectionShowsTheWholeGuide() {
    val html =
      GuideMarkdown.render(
        listOf(GuideMarkdown.Chapter("01-a.md", "## A {#a}\n### B {#a-b}", "en")),
        Files(),
        strings,
        lang = "en",
        rtl = false,
        dark = false,
        section = "nothing",
      )
    assertTrue(html.contains("<nav"))
    assertTrue(html.contains("<h3 id=\"a-b\">"))
  }

  /** Every section that the Help action of a settings screen opens is in each guide. */
  @Test
  fun settingsHelpSectionsExistInTheGuides() {
    val root = File("../docs").takeIf { it.isDirectory } ?: File("docs")
    val sections =
      SettingsGuideSections.SECTIONS.values.toSet() +
        SettingsGuideSections.PRO_SETTINGS +
        SettingsGuideSections.APP_KEYBOARD_SHORTCUTS
    for (language in listOf("en", "id", "nl", "pl")) {
      val ids =
        File(root, "user-guide/$language").listFiles { file -> file.name.endsWith(".md") }.orEmpty().flatMap {
          Regex("""\{#([A-Za-z0-9_-]+)\}""").findAll(it.readText()).map { match -> match.groupValues[1] }.toList()
        }.toSet()
      assertTrue(ids.isNotEmpty())
      assertEquals("$language guide", emptyList<String>(), sections.filter { it !in ids }.sorted())
    }
  }

  @Test
  fun problemsFindsBrokenLinksAndHeadings() {
    val chapters =
      listOf(
        GuideMarkdown.Chapter("01-a.md", "## A {#a}\n### B\n##### C {#c}\nSee [](#nothing).", "en"),
        GuideMarkdown.Chapter("02-b.md", "### First {#a}", "en"),
      )
    val problems = GuideMarkdown.problems(chapters, Files())
    assertTrue(problems.any { it.contains("heading without an ID: ### B") })
    assertTrue(problems.any { it.contains("level 5 heading") })
    assertTrue(problems.any { it.contains("link to a missing heading: #nothing") })
    assertTrue(problems.any { it.contains("02-b.md: starts with a level 3 heading") })
    assertTrue(problems.any { it.contains("ID used more than once: a") })
  }

  /** The guide in docs/user-guide/en has no broken links, missing IDs or misplaced headings. */
  @Test
  fun englishGuideHasNoProblems() {
    val root = File("../docs").takeIf { it.isDirectory } ?: File("docs")
    val english = File(root, "user-guide/en")
    val chapters =
      english.listFiles { file -> file.name.endsWith(".md") }.orEmpty().sortedBy { it.name }.map {
        GuideMarkdown.Chapter(it.name, it.readText(), "en")
      }
    assertTrue(chapters.isNotEmpty())
    val included =
      listOf("scripting.md", "speech-api.md", "braille-display-api.md", "direct-touch-api.md", "reference/item-files.md")
        .associate { it.substringAfterLast('/') to File(root, it).readText() }
    // Generated lists come from the device; any text stands in for them.
    val sources =
      Files(included, mapOf("gestures" to "Gestures.", "keyboard-shortcuts" to "Shortcuts."))
    assertEquals(emptyList<String>(), GuideMarkdown.problems(chapters, sources))
  }

  /** The guide in docs/user-guide/id has no broken links, missing IDs or misplaced headings. */
  @Test
  fun indonesianGuideHasNoProblems() {
    val root = File("../docs").takeIf { it.isDirectory } ?: File("docs")
    val indonesian = File(root, "user-guide/id")
    val chapters =
      indonesian.listFiles { file -> file.name.endsWith(".md") }.orEmpty().sortedBy { it.name }.map {
        GuideMarkdown.Chapter(it.name, it.readText(), "id")
      }
    assertTrue(chapters.isNotEmpty())
    val included =
      listOf("scripting.md", "speech-api.md", "braille-display-api.md", "direct-touch-api.md", "reference/item-files.md")
        .associate { it.substringAfterLast('/') to File(root, it).readText() }
    val sources =
      Files(included, mapOf("gestures" to "Gestur.", "keyboard-shortcuts" to "Pintasan."))
    assertEquals(emptyList<String>(), GuideMarkdown.problems(chapters, sources))
  }

  /** The guide in docs/user-guide/nl has no broken links, missing IDs or misplaced headings. */
  @Test
  fun dutchGuideHasNoProblems() {
    val root = File("../docs").takeIf { it.isDirectory } ?: File("docs")
    val dutch = File(root, "user-guide/nl")
    val chapters =
      dutch.listFiles { file -> file.name.endsWith(".md") }.orEmpty().sortedBy { it.name }.map {
        GuideMarkdown.Chapter(it.name, it.readText(), "nl")
      }
    assertTrue(chapters.isNotEmpty())
    val included =
      listOf("scripting.md", "speech-api.md", "braille-display-api.md", "direct-touch-api.md", "reference/item-files.md")
        .associate { it.substringAfterLast('/') to File(root, it).readText() }
    val sources =
      Files(included, mapOf("gestures" to "Gebaren.", "keyboard-shortcuts" to "Sneltoetsen."))
    assertEquals(emptyList<String>(), GuideMarkdown.problems(chapters, sources))
  }

  /** The Polish guide ships the same chapters as English, with translated API references. */
  @Test
  fun polishGuideHasNoProblems() {
    val root = File("../docs").takeIf { it.isDirectory } ?: File("docs")
    val polish = File(root, "user-guide/pl")
    val english = File(root, "user-guide/en")
    val chapters =
      english.listFiles { file -> file.name.endsWith(".md") }.orEmpty().sortedBy { it.name }.map {
        GuideMarkdown.Chapter(it.name, File(polish, it.name).readText(), "pl")
      }
    assertTrue(chapters.isNotEmpty())
    val included =
      listOf("scripting.md", "speech-api.md", "braille-display-api.md", "direct-touch-api.md", "item-files.md")
        .associateWith { File(polish, it).readText() }
    val sources =
      Files(included, mapOf("gestures" to "Gesty.", "keyboard-shortcuts" to "Skróty klawiszowe."))
    assertEquals(emptyList<String>(), GuideMarkdown.problems(chapters, sources))
  }
}
