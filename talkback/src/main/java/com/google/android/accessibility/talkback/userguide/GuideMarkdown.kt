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

import java.util.Locale

/**
 * Makes the user guide page from its chapter files: the Markdown described in
 * docs/user-guide/README.md, numbered headings, a table of contents and links between sections.
 */
object GuideMarkdown {

  /** One chapter file. [name] is its file name, [lang] the language its text is in. */
  class Chapter(val name: String, val text: String, val lang: String)

  /** Texts of the page itself, in the language of the guide. */
  class Strings(
    val title: String,
    val contents: String,
    /** Formats an appendix heading from its letter and title, such as "Appendix A: Gestures". */
    val appendix: (letter: String, title: String) -> String,
    val backToContents: String,
  )

  /** Inserts what `include` and `generate` lines name. */
  interface Sources {
    /** Returns the document `include` names, such as `scripting.md`, or null. */
    fun include(name: String): String?

    /** Returns the Markdown that `generate` names, such as `gestures`, or null. */
    fun generate(name: String): String?
  }

  /** Where links to files of the repository that the guide doesn't include go. */
  const val REPOSITORY_DOCS = "https://github.com/aaron-gh/SandFly/blob/master/docs/"

  private val HEADING = Regex("""^(#{1,6})\s+(.*?)\s*$""")
  private val HEADING_ID = Regex("""\s*\{#([A-Za-z0-9_-]+)\}$""")
  private val DIRECTIVE = Regex("""^\s*<!--\s*(include|generate):\s*(\S+)\s*-->\s*$""")
  private val COMMENT_START = Regex("""^\s*<!--""")
  private val LIST_ITEM = Regex("""^(\s*)([-*+]|\d+[.)])(\s+)(.*)$""")
  private val TABLE_SEPARATOR = Regex("""^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)*\|?\s*$""")
  private val FENCE = Regex("""^(\s*)(```|~~~)""")
  private val LINK_TARGET = Regex("""\]\(([^)\s]+)\)""")

  /** The query parameter of links from a section page to headings outside it: `?section=ID`. */
  const val SECTION_PARAMETER = "section"

  /** The ID that links from a section page to the contents of the whole guide. */
  const val CONTENTS_ID = "contents"

  private val PAGE_LINK = Regex("""href="#([^"]*)"""")

  /**
   * Returns the page for [chapters], in order. With [section], the ID of a heading, the page has
   * only that heading and the ones under it, and links to other headings go to `?section=ID`;
   * the whole guide is shown when no heading has that ID.
   */
  fun render(
    chapters: List<Chapter>,
    sources: Sources,
    strings: Strings,
    lang: String,
    rtl: Boolean,
    dark: Boolean,
    section: String? = null,
  ): String {
    val includedBy = includedBy(chapters)
    val documents = chapters.map { expand(it, sources, includedBy) }
    val headings = number(documents)
    val byId = headings.associateBy { it.id }
    val html = StringBuilder()
    html.append("<!DOCTYPE html>\n<html lang=\"").append(attribute(lang)).append("\" dir=\"")
      .append(if (rtl) "rtl" else "ltr").append(if (dark) "\" class=\"dark" else "").append("\">\n<head>\n<meta charset=\"utf-8\">\n")
      .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
      .append("<title>").append(escape(strings.title)).append("</title>\n<style>")
      .append(STYLE).append("</style>\n</head>\n<body>\n")
    val sectionDocument = section?.let { id -> documents.firstOrNull { document -> document.headings.any { it.id == id } } }
    if (section != null && sectionDocument != null) {
      for (heading in headings) heading.display = display(heading, strings)
      sectionPage(html, sectionDocument, section, byId, lang, strings)
      return html.toString()
    }
    html.append("<h1>").append(escape(strings.title)).append("</h1>\n")
    contents(html, headings, strings)
    html.append("<main>\n")
    for (document in documents) {
      val sameLang = document.lang == lang
      html.append(if (sameLang) "<div>\n" else "<div lang=\"${attribute(document.lang)}\">\n")
      Renderer(byId, document.headings.iterator()).blocks(document.lines, html)
      html.append("<p class=\"back\"><a href=\"#contents\">").append(escape(strings.backToContents))
        .append("</a></p>\n</div>\n")
    }
    html.append("</main>\n</body>\n</html>\n")
    return html.toString()
  }

  /** Appends the heading [id] of [document] and the headings under it, and the end of the page. */
  private fun sectionPage(
    html: StringBuilder,
    document: Document,
    id: String,
    byId: Map<String, Heading>,
    lang: String,
    strings: Strings,
  ) {
    val first = document.headings.indexOfFirst { it.id == id }
    val level = document.headings[first].level
    var next = first + 1
    while (next < document.headings.size && document.headings[next].level > level) next++
    val start = document.headingLines[first]
    val end = if (next < document.headings.size) document.headingLines[next] else document.lines.size
    val body = StringBuilder()
    body.append("<main>\n")
    body.append(if (document.lang == lang) "<div>\n" else "<div lang=\"${attribute(document.lang)}\">\n")
    Renderer(byId, document.headings.subList(first, next).iterator()).blocks(document.lines.subList(start, end), body)
    body.append("<p class=\"back\"><a href=\"#").append(CONTENTS_ID).append("\">").append(escape(strings.backToContents))
      .append("</a></p>\n</div>\n</main>\n")
    // Links to headings that aren't on the page open their own section.
    val onPage = document.headings.subList(first, next).map { it.id }.toSet()
    html.append(
      PAGE_LINK.replace(body) { match ->
        val target = match.groupValues[1]
        if (target in onPage) match.value else "href=\"?$SECTION_PARAMETER=$target\""
      }
    )
    html.append("</body>\n</html>\n")
  }

  /**
   * Returns the problems in [chapters]: headings of the wrong level or without IDs, IDs used twice,
   * links to headings that don't exist, and `include` or `generate` lines that insert nothing.
   */
  fun problems(chapters: List<Chapter>, sources: Sources): List<String> {
    val problems = mutableListOf<String>()
    for (chapter in chapters) {
      var inFence = false
      var chapterHeadings = 0
      var first = true
      for (line in chapter.text.lines()) {
        if (FENCE.containsMatchIn(line)) inFence = !inFence
        if (inFence) continue
        DIRECTIVE.matchEntire(line)?.let { match ->
          val name = match.groupValues[2]
          val text = if (match.groupValues[1] == "include") sources.include(name) else sources.generate(name)
          if (text == null) problems += "${chapter.name}: nothing to ${match.groupValues[1]} for $name"
        }
        val heading = HEADING.matchEntire(line) ?: continue
        val level = heading.groupValues[1].length
        if (first && level != 2) problems += "${chapter.name}: starts with a level $level heading"
        first = false
        if (level == 2) chapterHeadings++
        if (level !in 2..4) problems += "${chapter.name}: level $level heading: $line"
        if (HEADING_ID.find(heading.groupValues[2]) == null) problems += "${chapter.name}: heading without an ID: $line"
      }
      if (chapterHeadings != 1) problems += "${chapter.name}: $chapterHeadings level 2 headings"
    }
    val includedBy = includedBy(chapters)
    val documents = chapters.map { expand(it, sources, includedBy) }
    val ids = HashMap<String, Int>()
    for (document in documents) for (heading in document.headings) ids[heading.id] = (ids[heading.id] ?: 0) + 1
    ids.filterValues { it > 1 }.keys.sorted().forEach { problems += "ID used more than once: $it" }
    for ((chapter, document) in chapters.zip(documents)) {
      var inFence = false
      for (line in document.lines) {
        if (FENCE.containsMatchIn(line)) inFence = !inFence
        if (inFence) continue
        for (match in Regex("""\]\(#([^)\s]+)\)""").findAll(line.replace(Regex("`[^`]*`"), ""))) {
          if (match.groupValues[1] !in ids) problems += "${chapter.name}: link to a missing heading: #${match.groupValues[1]}"
        }
      }
    }
    return problems
  }

  // -----------------------------------------------------------------------------------------------
  // Includes, generated parts and heading IDs.

  /** A heading of the guide. [number] is "1.2" or "A.2"; [text] has no number. */
  class Heading(val level: Int, val id: String, val text: String) {
    var number: String = ""
    var display: String = ""
  }

  /** [headingLines] has the index in [lines] of each of [headings]. */
  private class Document(
    val appendix: Boolean,
    val lang: String,
    val lines: List<String>,
    val headings: List<Heading>,
    val headingLines: List<Int>,
  )

  /**
   * Returns [chapter] with its `include` and `generate` lines replaced, and its headings with IDs:
   * included documents get IDs made from their headings, prefixed with the name of the document.
   */
  private fun expand(chapter: Chapter, sources: Sources, includedBy: Map<String, String>): Document {
    val lines = mutableListOf<String>()
    var inFence = false
    for (line in chapter.text.lines()) {
      if (FENCE.containsMatchIn(line)) inFence = !inFence
      val directive = if (inFence) null else DIRECTIVE.matchEntire(line)
      when {
        directive == null -> lines += line
        directive.groupValues[1] == "generate" -> {
          val text = sources.generate(directive.groupValues[2]) ?: continue
          lines += ""
          lines += text.lines()
          lines += ""
        }
        else -> {
          val name = directive.groupValues[2]
          val text = sources.include(name) ?: continue
          lines += ""
          lines += included(name, text, includedBy)
          lines += ""
        }
      }
    }

    val headings = mutableListOf<Heading>()
    val headingLines = mutableListOf<Int>()
    inFence = false
    for ((index, line) in lines.withIndex()) {
      if (FENCE.containsMatchIn(line)) inFence = !inFence
      if (inFence) continue
      val match = HEADING.matchEntire(line) ?: continue
      val level = match.groupValues[1].length.coerceIn(2, 4)
      val raw = match.groupValues[2]
      val id = HEADING_ID.find(raw)?.groupValues?.get(1) ?: slug(raw)
      val text = raw.replace(HEADING_ID, "")
      headings += Heading(level, id, text)
      headingLines += index
      lines[index] = "#".repeat(level) + " " + text
    }
    return Document(chapter.name.startsWith("appendix"), chapter.lang, lines, headings, headingLines)
  }

  /** Returns the ID of the chapter that includes each included document, by document name. */
  private fun includedBy(chapters: List<Chapter>): Map<String, String> {
    val includedBy = HashMap<String, String>()
    for (chapter in chapters) {
      var chapterId: String? = null
      for (line in chapter.text.lines()) {
        val heading = HEADING.matchEntire(line)
        if (chapterId == null && heading != null && heading.groupValues[1].length == 2) {
          chapterId = HEADING_ID.find(heading.groupValues[2])?.groupValues?.get(1)
        }
        val directive = DIRECTIVE.matchEntire(line) ?: continue
        if (directive.groupValues[1] == "include" && chapterId != null) {
          includedBy[directive.groupValues[2]] = chapterId
        }
      }
    }
    return includedBy
  }

  /**
   * Returns the lines of the included document [name] without its title, its headings one level
   * down and given IDs, and its links to itself and to other included documents made links in the
   * guide.
   */
  private fun included(name: String, text: String, includedBy: Map<String, String>): List<String> {
    val prefix = prefixOf(name)
    val slugs = HashMap<String, Int>()
    val out = mutableListOf<String>()
    var inFence = false
    var titleSkipped = false
    for (line in text.lines()) {
      if (FENCE.containsMatchIn(line)) {
        inFence = !inFence
        out += line
        continue
      }
      if (inFence) {
        out += line
        continue
      }
      val heading = HEADING.matchEntire(line)
      if (heading != null) {
        val level = heading.groupValues[1].length
        if (level == 1 && !titleSkipped) {
          titleSkipped = true
          continue
        }
        val rawHeading = heading.groupValues[2]
        val textOfHeading = rawHeading.replace(HEADING_ID, "")
        var id = HEADING_ID.find(rawHeading)?.groupValues?.get(1) ?: gitHubSlug(textOfHeading)
        val seen = slugs[id]
        slugs[id] = (seen ?: 0) + 1
        if (seen != null) id += "-$seen"
        out += "#".repeat((level + 1).coerceIn(3, 4)) + " " + textOfHeading + " {#" + prefix + "-" + id + "}"
        continue
      }
      out += LINK_TARGET.replace(line) { match ->
        "](" + guideTarget(match.groupValues[1], name, includedBy) + ")"
      }
    }
    return out
  }

  /** Returns where a link with [target] in the included document [from] goes in the guide. */
  private fun guideTarget(target: String, from: String, includedBy: Map<String, String>): String {
    if (target.contains("://") || target.startsWith("mailto:")) return target
    if (target.startsWith("#")) return "#" + prefixOf(from) + "-" + target.substring(1)
    val file = target.substringBefore('#').removePrefix("./")
    val anchor = target.substringAfter('#', "")
    val name = file.substringAfterLast('/')
    val chapterId = includedBy[name] ?: includedBy[file]
    return when {
      chapterId != null && anchor.isEmpty() -> "#$chapterId"
      chapterId != null -> "#" + prefixOf(name) + "-" + anchor
      else -> {
        val base = from.substringBeforeLast('/', "")
        REPOSITORY_DOCS + (if (base.isEmpty()) "" else "$base/") + target
      }
    }
  }

  private fun prefixOf(name: String): String = slug(name.substringAfterLast('/').removeSuffix(".md"))

  /** Returns [text] as an ID: lowercase letters, digits and hyphens. */
  private fun slug(text: String): String =
    text.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "section" }

  /** Returns the anchor that GitHub gives a heading with [text]. */
  internal fun gitHubSlug(text: String): String =
    text.trim().lowercase(Locale.ROOT).replace(Regex("""[^\p{L}\p{N}\s_-]"""), "").replace(' ', '-')

  /** Numbers the headings of [documents]: 1, 1.1, 1.1.1 in chapters, A, A.1, A.1.1 in appendices. */
  private fun number(documents: List<Document>): List<Heading> {
    val all = mutableListOf<Heading>()
    var chapter = 0
    var appendix = 0
    for (document in documents) {
      var top = ""
      var section = 0
      var subsection = 0
      for (heading in document.headings) {
        when (heading.level) {
          2 -> {
            top = if (document.appendix) letter(appendix++) else (++chapter).toString()
            section = 0
            subsection = 0
            heading.number = top
          }
          3 -> {
            section++
            subsection = 0
            heading.number = "$top.$section"
          }
          else -> heading.number = "$top.$section.${++subsection}"
        }
        all += heading
      }
    }
    return all
  }

  private fun letter(index: Int): String =
    if (index < 26) ('A' + index).toString() else letter(index / 26 - 1) + ('A' + index % 26)

  private fun contents(html: StringBuilder, headings: List<Heading>, strings: Strings) {
    html.append("<nav aria-labelledby=\"contents\">\n<h2 id=\"contents\">")
      .append(escape(strings.contents)).append("</h2>\n<ul>\n")
    var open = false
    for (heading in headings) {
      heading.display = display(heading, strings)
      when (heading.level) {
        2 -> {
          if (open) html.append("</ul></li>\n")
          html.append("<li><a href=\"#").append(attribute(heading.id)).append("\">")
            .append(Inline.render(heading.display, emptyMap())).append("</a><ul>\n")
          open = true
        }
        3 -> html.append("<li><a href=\"#").append(attribute(heading.id)).append("\">")
          .append(Inline.render(heading.display, emptyMap())).append("</a></li>\n")
      }
    }
    if (open) html.append("</ul></li>\n")
    html.append("</ul>\n</nav>\n")
  }

  private fun display(heading: Heading, strings: Strings): String =
    if (heading.level == 2 && heading.number.first().isLetter()) {
      strings.appendix(heading.number, heading.text)
    } else {
      heading.number + " " + heading.text
    }

  // -----------------------------------------------------------------------------------------------
  // Blocks.

  private class Renderer(val byId: Map<String, Heading>, val headings: Iterator<Heading>) {

    fun blocks(lines: List<String>, html: StringBuilder) {
      var i = 0
      while (i < lines.size) {
        val line = lines[i]
        when {
          line.isBlank() -> i++
          COMMENT_START.containsMatchIn(line) -> i = comment(lines, i)
          FENCE.containsMatchIn(line) -> i = code(lines, i, html)
          HEADING.matches(line) -> {
            heading(line, html)
            i++
          }
          LIST_ITEM.matches(line) -> i = list(lines, i, html)
          isTableStart(lines, i) -> i = table(lines, i, html)
          line.trimStart().startsWith(">") -> i = quote(lines, i, html)
          else -> i = paragraph(lines, i, html)
        }
      }
    }

    private fun comment(lines: List<String>, start: Int): Int {
      var i = start
      while (i < lines.size && !lines[i].contains("-->")) i++
      return i + 1
    }

    private fun heading(line: String, html: StringBuilder) {
      val level = HEADING.matchEntire(line)!!.groupValues[1].length
      val heading = if (headings.hasNext()) headings.next() else null
      val id = heading?.id ?: ""
      html.append("<h").append(level).append(" id=\"").append(attribute(id)).append("\">")
        .append(Inline.render(heading?.display ?: line.trimStart('#', ' '), byId))
        .append("</h").append(level).append(">\n")
    }

    private fun code(lines: List<String>, start: Int, html: StringBuilder): Int {
      val fence = FENCE.find(lines[start])!!
      val indent = fence.groupValues[1].length
      val marker = fence.groupValues[2]
      var i = start + 1
      html.append("<pre><code>")
      var first = true
      while (i < lines.size && !lines[i].trimStart().startsWith(marker)) {
        if (!first) html.append('\n')
        html.append(escape(dedent(lines[i], indent)))
        first = false
        i++
      }
      html.append("</code></pre>\n")
      return i + 1
    }

    private fun paragraph(lines: List<String>, start: Int, html: StringBuilder): Int {
      var i = start
      val text = StringBuilder()
      while (i < lines.size && !endsParagraph(lines, i, i == start)) {
        if (text.isNotEmpty()) text.append(' ')
        text.append(lines[i].trim())
        i++
      }
      paragraphText(text.toString(), html)
      return i
    }

    private fun paragraphText(text: String, html: StringBuilder) {
      if (
        text.startsWith("Note:") ||
          text.startsWith("**Note:**") ||
          text.startsWith("Catatan:") ||
          text.startsWith("**Catatan:**") ||
          text.startsWith("Opmerking:") ||
          text.startsWith("**Opmerking:**") ||
          text.startsWith("Uwaga:") ||
          text.startsWith("**Uwaga:**")
      ) {
        html.append("<p class=\"note\">").append(Inline.render(text, byId)).append("</p>\n")
      } else {
        html.append("<p>").append(Inline.render(text, byId)).append("</p>\n")
      }
    }

    private fun endsParagraph(lines: List<String>, i: Int, first: Boolean): Boolean {
      val line = lines[i]
      if (line.isBlank()) return true
      if (first) return false
      return HEADING.matches(line) ||
        FENCE.containsMatchIn(line) ||
        LIST_ITEM.matches(line) ||
        COMMENT_START.containsMatchIn(line) ||
        isTableStart(lines, i) ||
        line.trimStart().startsWith(">")
    }

    private fun quote(lines: List<String>, start: Int, html: StringBuilder): Int {
      var i = start
      val inner = mutableListOf<String>()
      while (i < lines.size && lines[i].trimStart().startsWith(">")) {
        inner += lines[i].trimStart().removePrefix(">").removePrefix(" ")
        i++
      }
      html.append("<blockquote>\n")
      blocks(inner, html)
      html.append("</blockquote>\n")
      return i
    }

    private fun isTableStart(lines: List<String>, i: Int): Boolean =
      lines[i].trimStart().startsWith("|") && i + 1 < lines.size && TABLE_SEPARATOR.matches(lines[i + 1])

    private fun table(lines: List<String>, start: Int, html: StringBuilder): Int {
      html.append("<table>\n<thead><tr>")
      for (cell in cells(lines[start])) {
        html.append("<th scope=\"col\">").append(Inline.render(cell, byId)).append("</th>")
      }
      html.append("</tr></thead>\n<tbody>\n")
      var i = start + 2
      while (i < lines.size && lines[i].trimStart().startsWith("|")) {
        html.append("<tr>")
        for (cell in cells(lines[i])) {
          html.append("<td>").append(Inline.render(cell, byId)).append("</td>")
        }
        html.append("</tr>\n")
        i++
      }
      html.append("</tbody>\n</table>\n")
      return i
    }

    private fun cells(line: String): List<String> {
      var row = line.trim()
      if (row.startsWith("|")) row = row.substring(1)
      if (row.endsWith("|") && !row.endsWith("\\|")) row = row.substring(0, row.length - 1)
      val cells = mutableListOf<String>()
      val cell = StringBuilder()
      var inCode = false
      var index = 0
      while (index < row.length) {
        val c = row[index]
        when {
          c == '\\' && index + 1 < row.length && row[index + 1] == '|' -> {
            cell.append('|')
            index++
          }
          c == '`' -> {
            inCode = !inCode
            cell.append(c)
          }
          c == '|' && !inCode -> {
            cells += cell.toString().trim()
            cell.setLength(0)
          }
          else -> cell.append(c)
        }
        index++
      }
      cells += cell.toString().trim()
      return cells
    }

    /** Renders the list that starts at [start], with the items inside its items. */
    private fun list(lines: List<String>, start: Int, html: StringBuilder): Int {
      val first = LIST_ITEM.matchEntire(lines[start])!!
      val indent = first.groupValues[1].length
      val ordered = first.groupValues[2].first().isDigit()
      html.append(if (ordered) "<ol>\n" else "<ul>\n")
      var i = start
      while (i < lines.size) {
        val item = LIST_ITEM.matchEntire(lines[i]) ?: break
        if (item.groupValues[1].length != indent) break
        val column = indent + item.groupValues[2].length + item.groupValues[3].length
        val content = mutableListOf(item.groupValues[4])
        i++
        var blank = false
        while (i < lines.size) {
          val line = lines[i]
          if (line.isBlank()) {
            blank = true
            content += ""
            i++
            continue
          }
          val lineIndent = line.length - line.trimStart().length
          val sibling = LIST_ITEM.matchEntire(line)
          if (sibling != null && sibling.groupValues[1].length <= indent) break
          if (lineIndent >= indent + 2 || (!blank && sibling == null && !HEADING.matches(line))) {
            content += dedent(line, column.coerceAtMost(lineIndent))
            blank = false
            i++
            continue
          }
          break
        }
        while (content.isNotEmpty() && content.last().isBlank()) content.removeAt(content.size - 1)
        html.append("<li>")
        item(content, html)
        html.append("</li>\n")
        // A blank line, then an item of the same list, continues it.
        var next = i
        while (next < lines.size && lines[next].isBlank()) next++
        val nextItem = if (next < lines.size) LIST_ITEM.matchEntire(lines[next]) else null
        if (nextItem != null && nextItem.groupValues[1].length == indent &&
          nextItem.groupValues[2].first().isDigit() == ordered) {
          i = next
        } else {
          break
        }
      }
      html.append(if (ordered) "</ol>\n" else "</ul>\n")
      return i
    }

    /** Renders the content of one list item: its text alone, or blocks when it has more. */
    private fun item(content: List<String>, html: StringBuilder) {
      val firstBlockEnd = content.indexOfFirst { it.isBlank() || LIST_ITEM.matches(it) || FENCE.containsMatchIn(it) }
      if (firstBlockEnd == -1) {
        html.append(Inline.render(content.joinToString(" ") { it.trim() }, byId))
        return
      }
      val text = content.subList(0, firstBlockEnd).joinToString(" ") { it.trim() }
      html.append(Inline.render(text, byId)).append('\n')
      blocks(content.subList(firstBlockEnd, content.size), html)
    }
  }

  private fun dedent(line: String, count: Int): String {
    var removed = 0
    while (removed < count && removed < line.length && line[removed] == ' ') removed++
    return line.substring(removed)
  }

  // -----------------------------------------------------------------------------------------------
  // Text within blocks.

  internal object Inline {
    private val SEPARATOR = "</strong> &gt; <strong>"
    private const val SEPARATOR_HTML =
      "</strong><span class=\"sep\"><span class=\"sr\">, </span><span aria-hidden=\"true\"> › </span></span><strong>"

    /** Returns [text] as HTML, with links to headings in [byId]. */
    fun render(text: String, byId: Map<String, Heading>): String {
      val out = StringBuilder()
      var i = 0
      while (i < text.length) {
        val c = text[i]
        when {
          c == '\\' && i + 1 < text.length && text[i + 1] in "\\`*_[]()#+-.!|<>" -> {
            out.append(escape(text[i + 1].toString()))
            i += 2
          }
          c == '`' -> {
            val ticks = run {
              var n = 0
              while (i + n < text.length && text[i + n] == '`') n++
              n
            }
            val end = text.indexOf("`".repeat(ticks), i + ticks)
            if (end < 0) {
              out.append(escape(text.substring(i, i + ticks)))
              i += ticks
            } else {
              out.append("<code>").append(escape(text.substring(i + ticks, end).trim())).append("</code>")
              i = end + ticks
            }
          }
          c == '*' && text.startsWith("**", i) -> {
            val end = text.indexOf("**", i + 2)
            if (end < 0) {
              out.append("**")
              i += 2
            } else {
              out.append("<strong>").append(render(text.substring(i + 2, end), byId)).append("</strong>")
              i = end + 2
            }
          }
          (c == '*' || c == '_') && emphasisStarts(text, i) -> {
            val end = emphasisEnd(text, i + 1, c)
            if (end < 0) {
              out.append(c)
              i++
            } else {
              out.append("<em>").append(render(text.substring(i + 1, end), byId)).append("</em>")
              i = end + 1
            }
          }
          c == '[' -> {
            val link = link(text, i)
            if (link == null) {
              out.append('[')
              i++
            } else {
              out.append(linkHtml(link.first, link.second, byId))
              i = link.third
            }
          }
          c == '<' && text.startsWith("<br>", i) -> {
            out.append("<br>")
            i += 4
          }
          else -> {
            out.append(escape(c.toString()))
            i++
          }
        }
      }
      return out.toString().replace(SEPARATOR, SEPARATOR_HTML)
    }

    private fun emphasisStarts(text: String, i: Int): Boolean {
      if (i + 1 >= text.length || text[i + 1].isWhitespace()) return false
      // Inside words, as in snake_case names, underscores are text.
      return i == 0 || !text[i - 1].isLetterOrDigit()
    }

    private fun emphasisEnd(text: String, from: Int, marker: Char): Int {
      var i = from
      while (i < text.length) {
        if (text[i] == '`') {
          val end = text.indexOf('`', i + 1)
          if (end < 0) return -1
          i = end + 1
          continue
        }
        if (text[i] == marker && !text[i - 1].isWhitespace() &&
          (i + 1 >= text.length || !text[i + 1].isLetterOrDigit())) {
          return i
        }
        i++
      }
      return -1
    }

    /** Returns the text and target of the link at [start], and where it ends, or null. */
    private fun link(text: String, start: Int): Triple<String, String, Int>? {
      var depth = 0
      var i = start
      while (i < text.length) {
        when (text[i]) {
          '[' -> depth++
          ']' -> {
            depth--
            if (depth == 0) break
          }
          '`' -> {
            val end = text.indexOf('`', i + 1)
            if (end > 0) i = end
          }
        }
        i++
      }
      if (i >= text.length || i + 1 >= text.length || text[i + 1] != '(') return null
      val close = text.indexOf(')', i + 2)
      if (close < 0) return null
      return Triple(text.substring(start + 1, i), text.substring(i + 2, close).trim(), close + 1)
    }

    private fun linkHtml(label: String, target: String, byId: Map<String, Heading>): String {
      if (target.startsWith("#")) {
        val heading = byId[target.substring(1)]
        val shown =
          when {
            label.isNotEmpty() -> render(label, byId)
            heading != null -> render(heading.display, emptyMap())
            else -> escape(target.substring(1))
          }
        // A link to a heading that doesn't exist is shown as its text.
        if (heading == null) return shown
        return "<a href=\"" + attribute(target) + "\">" + shown + "</a>"
      }
      val shown = if (label.isEmpty()) escape(target) else render(label, byId)
      return "<a href=\"" + attribute(target) + "\">" + shown + "</a>"
    }
  }

  internal fun escape(text: String): String {
    val out = StringBuilder(text.length)
    for (c in text) {
      when (c) {
        '<' -> out.append("&lt;")
        '>' -> out.append("&gt;")
        '&' -> out.append("&amp;")
        '"' -> out.append("&quot;")
        else -> out.append(c)
      }
    }
    return out.toString()
  }

  private fun attribute(text: String): String = escape(text)

  private const val STYLE =
    """
:root { color-scheme: light; }
html.dark { color-scheme: dark; }
body { font-family: sans-serif; line-height: 1.5; margin: 0 auto; max-width: 48em; padding: 0 1em 2em; overflow-wrap: break-word; }
h1 { font-size: 1.6em; }
h2 { font-size: 1.4em; margin-top: 2em; }
h3 { font-size: 1.2em; margin-top: 1.5em; }
h4 { font-size: 1.05em; margin-top: 1.2em; }
nav ul { list-style: none; padding-left: 0; }
nav ul ul { padding-left: 1.5em; }
nav li { margin: 0.3em 0; }
a { color: #0b57d0; }
code { font-family: monospace; background: rgba(127, 127, 127, 0.15); padding: 0 0.2em; border-radius: 3px; }
pre { background: rgba(127, 127, 127, 0.15); padding: 0.6em; overflow-x: auto; border-radius: 4px; }
pre code { background: none; padding: 0; }
table { border-collapse: collapse; margin: 1em 0; display: block; overflow-x: auto; }
th, td { border: 1px solid rgba(127, 127, 127, 0.5); padding: 0.3em 0.5em; text-align: start; vertical-align: top; }
.note { border-inline-start: 4px solid rgba(127, 127, 127, 0.6); padding-inline-start: 0.6em; }
blockquote { border-inline-start: 4px solid rgba(127, 127, 127, 0.6); margin-inline-start: 0; padding-inline-start: 0.6em; }
.sr { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
.back { margin-top: 1.5em; }
html.dark body { background: #121212; color: #e6e6e6; }
html.dark a { color: #a8c7fa; }
"""
}
