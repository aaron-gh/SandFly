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

package com.google.android.accessibility.talkback.scripting

import org.json.JSONArray
import org.json.JSONObject

/**
 * What the inspected item and the items like it have in common, from the sample Inspect focused
 * item saves (ItemInspector.sample): a query that names them all, the buttons in them, and the
 * parts SandFly reads for them, with edits that remove a part from every one of them. Texts that
 * differ from item to item, such as a person's name, are left out of what is matched.
 */
object ItemPattern {
  /** An item inside an item: its JSON, how deep it is and the child indexes that lead to it. */
  private class Inner(val json: JSONObject, val depth: Int, val path: List<Int>)

  /**
   * A button in the item: the title of its action, the query that finds it in each item, and its
   * text in the inspected item.
   */
  data class Button(val title: String, val target: JSONObject, val value: String, val longPressTitle: String? = null)

  /**
   * A text pattern. [regex] is found ignoring case; [exact] is the text when every item has the same
   * one; [core] is the part of the text every item has.
   */
  private class Pattern(val regex: String, val exact: String?, val core: String)

  private const val MIN_CORE = 3
  private const val MAX_BUTTONS = 6
  private const val MAX_GROUP_DEPTH = 5

  fun item(sample: JSONObject?): JSONObject? = sample?.optJSONObject("item")

  /**
   * The sample of the list row the inspected item is in, when it isn't the row itself: the row,
   * the rows like it beside it, and the other rows beside it.
   */
  fun rowSample(sample: JSONObject?): JSONObject? = sample?.optJSONObject("row")

  /** The samples a rule can be for: the row the inspected item is in, then the item itself. */
  fun samples(sample: JSONObject?): List<JSONObject> = listOfNotNull(rowSample(sample), sample?.takeIf { item(it) != null })

  /**
   * Items beside the inspected one that a rule for it mustn't name: rows of another kind, such as
   * the rows a post is split into beside its comments.
   */
  private fun others(sample: JSONObject): List<JSONObject> {
    val alike = similar(sample)
    return sample.optJSONArray("others").objectList() +
      sample.optJSONArray("similar").objectList().filter { candidate -> alike.none { it === candidate } }
  }

  /**
   * The items like the inspected item, without the ones built differently, such as a heading row of
   * the same class in the same list.
   */
  private val similarCache = java.util.WeakHashMap<JSONObject, List<JSONObject>>()

  private fun similar(sample: JSONObject): List<JSONObject> = synchronized(similarCache) { similarCache.getOrPut(sample) { alike(sample) } }

  private fun alike(sample: JSONObject): List<JSONObject> {
    val item = item(sample) ?: return emptyList()
    val shape = inners(item).map { it.path to it.json.optString("class") }
    return sample.optJSONArray("similar").objectList().filter { other ->
      val alike = shape.count { (path, className) -> at(other, path)?.optString("class") == className }
      alike * ALIKE_PARTS >= shape.size * ALIKE_OF && inners(other).size * ALIKE_PARTS <= shape.size * ALIKE_PARTS * 2
    }
  }

  /** At least [ALIKE_OF] in [ALIKE_PARTS] of the items in an item like it are in the same place. */
  private const val ALIKE_OF = 3
  private const val ALIKE_PARTS = 4

  /**
   * A query that names the inspected item and the items like it, or null when the sample has
   * nothing they share that the item's own query doesn't already say.
   */
  fun likeQuery(sample: JSONObject?): JSONObject? {
    val item = item(sample) ?: return null
    val similar = similar(sample!!)
    val id = item.optString("id")
    if (id.isNotEmpty() && similar.all { it.optString("id") == id }) return JSONObject().put("id", id)
    val className = item.optString("class").ifEmpty { null }
    val query = JSONObject().putOpt("className", className)
    // A row that can be clicked, such as a comment, isn't one of the groups inside it.
    if (item.optBoolean("clickable") && similar.all { it.optBoolean("clickable") }) query.put("clickable", true)
    val items = listOf(item) + similar
    val others = others(sample)
    // A query that also names an item inside a row, such as its Add Friend button holding a text
    // described "Add friend", would change that item too; one that names a row of another kind,
    // such as a post's Like button row beside a comment's, would change that row.
    for ((has, depth) in innerItems(item, similar)) {
      val candidate = JSONObject(query.toString()).put("has", has).put("hasDepth", depth)
      if (!namesInner(candidate, items) && others.none { matches(candidate, it) }) return candidate
    }
    // The item's own text, when the items share part of it.
    val own = label(item) ?: return null
    val field = fieldOf(item)
    val pattern = pattern(own, similar.map { if (fieldOf(it) == field) label(it) else null }, emptyList()) ?: return null
    if (similar.isEmpty()) return null
    val named = if (pattern.exact != null) query.put(field, pattern.exact) else query.put("matches", pattern.regex).put("field", field)
    return named.takeUnless { namesInner(it, items) || others.any { other -> matches(it, other) } }
  }

  /** Whether [query] names an item inside one of [items]. */
  private fun namesInner(query: JSONObject, items: List<JSONObject>): Boolean =
    items.any { item -> inners(item).any { matches(query, it.json) } }

  /**
   * Items inside the item that every item like it has in the same place, with the same text or the
   * same part of it, and how deep each is: the same text first, buttons with a description first.
   */
  private fun innerItems(item: JSONObject, similar: List<JSONObject>): List<Pair<JSONObject, Int>> {
    val inner = inners(item).filter { label(it.json) != null }
    val ordered = inner.sortedWith(compareBy({ !it.json.optBoolean("clickable") }, { !it.json.has("description") }, { it.depth }))
    val exact = mutableListOf<Pair<JSONObject, Int>>()
    val partial = mutableListOf<Pair<JSONObject, Int>>()
    for (candidate in ordered) {
      val value = label(candidate.json)!!
      val field = fieldOf(candidate.json)
      val others = similar.map { other -> at(other, candidate.path)?.takeIf { fieldOf(it) == field }?.let(::label) }
      if (similar.isNotEmpty() && others.any { it == null }) continue
      val pattern = pattern(value, others, if (similar.isEmpty()) names(item, candidate.json) else emptyList()) ?: continue
      val query =
        if (pattern.exact != null) JSONObject().put(field, pattern.exact)
        else JSONObject().put("matches", pattern.regex).put("field", field)
      (if (pattern.exact != null) exact else partial) += query to candidate.depth
    }
    return exact + partial
  }

  /**
   * A query that finds [inner], an item in [row] or a menu entry it opened, in any item like [row]:
   * by its id, or else by its text, without the parts of it that name [row], such as the person in
   * "Remove Chloe McCarthy". An item without text is found by the first text in it. Null when it
   * has neither.
   */
  fun target(row: JSONObject?, inner: JSONObject): JSONObject? {
    inner.optString("id").takeIf { it.isNotEmpty() }?.let { return JSONObject().put("id", it) }
    val named = if (label(inner) != null) inner else inners(inner).firstOrNull { label(it.json) != null }?.json ?: return null
    val value = label(named)!!
    val field = fieldOf(named)
    val pattern = row?.let { pattern(value, emptyList(), names(it, inner)) } ?: exact(value)
    return if (pattern.exact != null) JSONObject().put(field, pattern.exact)
    else JSONObject().put("matches", pattern.regex).put("field", field)
  }

  /** The texts in [item] a read part can take, each with its path of child numbers from 1. */
  fun readChoices(item: JSONObject): List<Pair<List<Int>, String>> =
    inners(item).mapNotNull { inner -> label(inner.json)?.let { inner.path.map { it + 1 } to it } }

  /** What a rule's read [parts] make of [item], as SandFly would say them, or null. */
  fun readText(item: JSONObject, parts: JSONArray?): String? {
    val texts =
      parts.objectList().mapNotNull { part ->
        val path = part.optJSONArray("path")
        val from = part.opt("from")
        val target =
          when {
            path != null -> at(item, (0 until path.length()).map { path.optInt(it) - 1 })
            from is JSONObject && from.length() == 0 -> item
            from is JSONObject -> (listOf(item) + inners(item).map { it.json }).firstOrNull { matches(from, it) }
            else -> null
          }
        val text = target?.let { if (it === item) label(it) else label(it) ?: inners(it).firstNotNullOfOrNull { inner -> label(inner.json) } }
        text?.let { listOfNotNull(part.optString("before").trim().ifEmpty { null }, it).joinToString(" ") }
      }
    return texts.joinToString(", ").ifEmpty { null }
  }

  /** How many of the inspected item and the items like it [query] names. */
  fun count(query: JSONObject?, sample: JSONObject?): Int {
    val item = item(sample) ?: return 0
    if (query == null) return 0
    return (listOf(item) + similar(sample!!)).count { matches(query, it) }
  }

  /** How many items like the inspected item the sample has. */
  fun similarCount(sample: JSONObject?): Int = sample?.let { similar(it).size } ?: 0

  /** Whether [query] names [item] by something a sample can tell, such as its class or a text. */
  fun tellsItem(query: JSONObject, item: JSONObject): Boolean =
    TOLD_KEYS.any { query.optString(it).isNotEmpty() || query.optJSONObject(it) != null } && matches(query, item)

  private val TOLD_KEYS = listOf("id", "className", "contains", "matches", "text", "contentDescription", "has")

  /**
   * Whether [query] names [item], for the parts of a query a sample can tell: id, class name,
   * texts, Clickable and an item inside. Other parts, such as Inside an item, are taken as met.
   */
  fun matches(query: JSONObject, item: JSONObject): Boolean {
    val id = query.optString("id")
    if (id.isNotEmpty() && !NodeQuery.idMatches(id, item.optString("id").ifEmpty { null })) return false
    val className = query.optString("className")
    if (className.isNotEmpty() && !item.optString("class").endsWith(className)) return false
    val texts =
      when (query.optString("field")) {
        "text" -> listOf(item.optString("text"))
        "contentDescription", "description" -> listOf(item.optString("description"))
        "" -> listOf(item.optString("text"), item.optString("description"))
        else -> null
      }
    if (texts != null) {
      val contains = query.optString("contains")
      if (contains.isNotEmpty() && texts.none { it.contains(contains, ignoreCase = true) }) return false
      val regex = query.optString("matches")
      if (regex.isNotEmpty()) {
        val pattern = com.google.android.accessibility.talkback.macros.Macros.compilePattern(regex) ?: return false
        if (texts.none { pattern.matcher(it).find() }) return false
      }
    }
    for ((key, own) in listOf("text" to "text", "contentDescription" to "description")) {
      val exact = query.optString(key)
      if (exact.isNotEmpty() && !exact.equals(item.optString(own), ignoreCase = true)) return false
    }
    val kinds =
      when (val kind = query.opt("kind")) {
        is String -> listOf(kind)
        is JSONArray -> (0 until kind.length()).map { kind.optString(it) }
        else -> emptyList()
      }
    if ("clickable" in kinds && !item.optBoolean("clickable")) return false
    if (query.has("clickable") && query.optBoolean("clickable") != item.optBoolean("clickable")) return false
    val has = query.optJSONObject("has")
    if (has != null) {
      val depth = query.optInt("hasDepth", NodeQuery.MAX_HAS_DEPTH)
      if (inners(item).none { it.depth <= depth && matches(has, it.json) }) return false
    }
    return true
  }

  /**
   * The buttons in the inspected item, each with a title and a query that finds it in any item like
   * it. A button whose text holds a name, such as "Remove Natalie Anderson", is titled and found by
   * the part every item has: "Remove".
   */
  fun buttons(sample: JSONObject?): List<Button> {
    val item = item(sample) ?: return emptyList()
    val similar = similar(sample!!)
    val buttons = mutableListOf<Button>()
    val clickable = inners(item).filter { it.json.optBoolean("clickable") }
    for (button in clickable) {
      // An item with buttons in it, such as a comment that can be clicked, is a container: its
      // buttons are the actions.
      if (inners(button.json).any { it.json.optBoolean("clickable") }) continue
      val (named, path) =
        if (label(button.json) != null) button.json to button.path
        else inners(button.json).firstOrNull { label(it.json) != null }?.let { it.json to button.path + it.path } ?: continue
      val value = label(named)!!
      if (value.equals(label(item), ignoreCase = true)) continue
      // The first text of a row without one of its own names the row, such as a comment's author.
      if (label(item) == null && readingOrder(item).firstOrNull { label(it) != null } === named) continue
      // A button showing a text the row has outside it, such as a comment's time, is content.
      val inButton = inners(button.json).map { it.json }.toSet() + button.json
      if (inners(item).any { it.json !in inButton && !it.json.optBoolean("clickable") && label(it.json).equals(value, ignoreCase = true) }) continue
      val field = fieldOf(named)
      val others = similar.map { other -> at(other, path)?.takeIf { fieldOf(it) == field }?.let(::label) }
      val known = if (others.all { it != null }) others else emptyList()
      // A button another row has with the same text, beside rows where it differs, such as Add
      // Friend beside Cancel request, keeps its own text. One whose text differs in every row, such
      // as a name, is content, not an action.
      val pattern =
        pattern(value, known, if (known.isEmpty()) names(item, button.json) else emptyList())
          ?: (if (known.any { it.equals(value, ignoreCase = true) }) pattern(value, emptyList(), names(item, button.json)) else null)
          ?: continue
      val title = titleOf(pattern.core).ifEmpty { continue }
      // A long text, such as a comment's own text in an item that can be clicked, is content.
      if (title.length > MAX_TITLE || title.split(' ').size > MAX_TITLE_WORDS) continue
      if (buttons.any { it.title.equals(title, ignoreCase = true) }) continue
      val target =
        if (pattern.exact != null) JSONObject().put(field, pattern.exact)
        else JSONObject().put("matches", pattern.regex).put("field", field)
      // A long press the app named, such as "Long-press for more reactions" on a Like button.
      val longPress =
        button.json.optJSONArray("actions").objectList()
          .firstOrNull { it.optInt("id") == ACTION_LONG_CLICK }
          ?.optString("label")?.let(::titleOf)?.ifEmpty { null }
          ?.takeUnless { it.equals(title, ignoreCase = true) }
      buttons += Button(title, target, value, longPress)
      if (buttons.size == MAX_BUTTONS) break
    }
    return buttons
  }

  /**
   * The parts SandFly reads for [item], in order, as pieces between commas: its own description or
   * text, or, when it has none, the texts of the items in it. Read as one item reads the same.
   */
  @Suppress("UNUSED_PARAMETER")
  fun parts(item: JSONObject, grouped: Boolean): List<String> {
    val texts = mutableListOf<String>()
    val own = label(item)
    if (own != null) {
      texts += own
    } else {
      fun collect(node: JSONObject, depth: Int) {
        if (depth >= MAX_GROUP_DEPTH) return
        for (child in node.optJSONArray("children").objectList()) {
          label(child)?.let(texts::add)
          if (!child.has("description")) collect(child, depth + 1)
        }
      }
      collect(item, 0)
    }
    return texts.flatMap { it.split(',') }.map { it.trim() }.filter { it.isNotEmpty() }
  }

  /** The parts of the inspected item, joined as SandFly reads them. */
  fun text(sample: JSONObject?, grouped: Boolean): String? =
    item(sample)?.let { parts(it, grouped).joinToString(", ").ifEmpty { null } }

  /**
   * A speech edit that removes part [index] of the inspected item from every item like it: a
   * repeated part as a repeat, a part all items have as that text, and a part with a name in it by
   * the rest of it, such as "'s profile picture".
   */
  fun removePart(sample: JSONObject, grouped: Boolean, index: Int): JSONObject? {
    val item = item(sample) ?: return null
    val parts = parts(item, grouped)
    val part = parts.getOrNull(index) ?: return null
    if (parts.take(index).any { it.equals(part, ignoreCase = true) }) {
      return JSONObject().put("op", SpeechEdits.REMOVE_REPEATS)
    }
    val similarParts = similar(sample).map { parts(it, grouped) }
    val pattern =
      if (similarParts.isNotEmpty() && similarParts.all { list -> list.any { it.equals(part, ignoreCase = true) } }) {
        exact(part)
      } else if (similarParts.isNotEmpty()) {
        pattern(part, similarParts.map { it.getOrNull(index) }, emptyList()) ?: exact(part)
      } else {
        pattern(part, emptyList(), parts.filterIndexed { i, other -> i != index && !other.equals(part, ignoreCase = true) })
          ?: exact(part)
      }
    return JSONObject().put("op", SpeechEdits.REMOVE_PARTS).put("find", pattern.regex).put("regex", true)
  }

  /**
   * A pattern for [value], given the texts in the same place in the items like it ([others], null
   * where one has none). With no other items, [names] are texts of the item that differ between
   * items, such as the name of a person, and are left out of the pattern.
   */
  private fun pattern(value: String, others: List<String?>, names: List<String>): Pattern? {
    if (others.isNotEmpty()) {
      if (others.any { it == null }) return null
      val all = others.filterNotNull() + value
      if (all.all { it.equals(value, ignoreCase = true) }) return exact(value)
      val prefix = wordPrefix(all)
      val suffix = wordSuffix(all)
      // Something follows a shared start, so that "Remove Chloe McCarthy" gives "^Remove.+", which
      // the text "Remove" inside the Remove button doesn't match.
      val more = { core: String -> if (all.all { it.trim().length > core.length }) ".+" else "" }
      return when {
        // A shared start comes first: an action's text starts with what it does, such as "Reply to".
        prefix.length >= MIN_CORE -> Pattern("^" + quote(prefix) + more(prefix), null, prefix)
        suffix.length >= MIN_CORE -> Pattern(more(suffix) + quote(suffix) + "$", null, suffix)
        else -> commonWords(all)?.let { Pattern(quote(it), null, it) }
      }
    }
    // A name itself, such as the person's name in a row, is in no other item.
    if (names.any { it.equals(value, ignoreCase = true) }) return null
    val name =
      names
        .filter { it.length >= MIN_CORE && value.contains(it, ignoreCase = true) }
        .maxByOrNull { it.length } ?: return exact(value)
    val start = value.indexOf(name, ignoreCase = true)
    val before = value.substring(0, start).trim()
    val after = value.substring(start + name.length).trim()
    return when {
      before.isEmpty() && after.isEmpty() -> null
      before.isEmpty() -> Pattern(".+" + quote(after) + "$", null, after)
      after.isEmpty() -> Pattern("^" + quote(before) + ".+", null, before)
      else -> Pattern("^" + quote(before) + ".*" + quote(after) + "$", null, before)
    }
  }

  /**
   * The longest run of whole words of the shortest of [texts] that all of them have, such as
   * "mutual friend" of "1 mutual friend" and "12 mutual friends", or null.
   */
  private fun commonWords(texts: List<String>): String? {
    val words = texts.minBy { it.length }.split(' ').filter { it.isNotEmpty() }.take(MAX_WORDS)
    for (length in words.size downTo 1) {
      for (start in 0..words.size - length) {
        val run = words.subList(start, start + length).joinToString(" ")
        if (run.count { it.isLetter() } >= MIN_CORE && texts.all { it.contains(run, ignoreCase = true) }) return run
      }
    }
    return null
  }

  private const val MAX_WORDS = 30

  private fun exact(value: String) = Pattern("^" + quote(value) + "$", value, value)

  /**
   * An action's title from a button's text: its first sentence, without "button" at its end or a
   * short word such as "to" left from a name, so that "Like Andria's comment button. Double-tap
   * and hold to show reaction tray." gives "Like" with the name taken out, and "Reply to Andria's
   * comment" gives "Reply".
   */
  fun titleOf(text: String): String {
    var title = text.split(SENTENCE_END, limit = 2).first().trim()
    title = title.replace(BUTTON_WORD, "").trim()
    while (true) {
      val words = title.split(' ')
      val last = words.last()
      if (words.size < 2 || last.length > SHORT_WORD || last.any { !it.isLowerCase() }) break
      title = words.dropLast(1).joinToString(" ")
    }
    return title.trim { !it.isLetterOrDigit() }
  }

  private val SENTENCE_END = Regex("[.!?](\\s|$)|\n")
  private val BUTTON_WORD = Regex("\\s+button$", RegexOption.IGNORE_CASE)
  private const val SHORT_WORD = 3
  private const val MAX_TITLE = 32
  private const val MAX_TITLE_WORDS = 5
  private const val ACTION_LONG_CLICK = 32

  /** The start all [texts] share, ending at a word's end. */
  private fun wordPrefix(texts: List<String>): String {
    val first = texts.first()
    var length = texts.minOf { it.length }
    for (text in texts) {
      length = minOf(length, text.zip(first).takeWhile { (a, b) -> a.equals(b, ignoreCase = true) }.size)
    }
    var cut = first.substring(0, length)
    // A prefix that ends inside a word, such as "Remove N" of two names, ends before the word.
    val inWord = texts.any { it.length > length && it[length].isLetterOrDigit() } && cut.lastOrNull()?.isLetterOrDigit() == true
    if (inWord) cut = cut.substring(0, cut.indexOfLast { !it.isLetterOrDigit() } + 1)
    return cut.trim()
  }

  /** The end all [texts] share, starting at a word's start or at a sign such as an apostrophe. */
  private fun wordSuffix(texts: List<String>): String = wordPrefix(texts.map { it.reversed() }).reversed()

  /** [text] with the signs regular expressions use escaped. */
  fun quote(text: String): String = text.replace(Regex("[\\\\^$.|?*+()\\[\\]{}]")) { "\\" + it.value }

  /**
   * Texts of [item] that differ from item to item, as far as one item can tell: the parts of its own
   * text, such as "Natalie Anderson, 3 mutual friends", or else the first text in it outside
   * [except], which names a row.
   */
  private fun names(item: JSONObject, except: JSONObject): List<String> {
    val excluded = inners(except).map { it.json } + except
    // The first text in reading order, which names a row, such as a comment's author.
    val texts =
      label(item)?.let { listOf(it) }
        ?: listOfNotNull(readingOrder(item).firstOrNull { inner -> excluded.none { it === inner } && label(inner) != null }?.let(::label))
    val parts = texts.flatMap { it.split(',') }.map { it.trim() }.filter { it.length >= MIN_CORE }
    // A person's first name, as in "Reply to Andria's comment" in Andria Jones's comment.
    val firstNames = parts.mapNotNull { part -> part.split(' ').takeIf { it.size in 2..4 }?.first()?.takeIf { it.length >= MIN_CORE && it[0].isUpperCase() } }
    return (parts + firstNames).distinct()
  }

  /** The items in [item] in reading order, each before the items in it. */
  private fun readingOrder(item: JSONObject): List<JSONObject> =
    item.optJSONArray("children").objectList().flatMap { listOf(it) + readingOrder(it) }

  /** The items in [item], breadth first. */
  private fun inners(item: JSONObject): List<Inner> {
    val found = mutableListOf<Inner>()
    val queue = ArrayDeque(item.optJSONArray("children").objectList().mapIndexed { i, child -> Inner(child, 1, listOf(i)) })
    while (queue.isNotEmpty()) {
      val inner = queue.removeFirst()
      found += inner
      inner.json.optJSONArray("children").objectList().forEachIndexed { i, child ->
        queue.add(Inner(child, inner.depth + 1, inner.path + i))
      }
    }
    return found
  }

  private fun at(item: JSONObject, path: List<Int>): JSONObject? =
    path.fold(item as JSONObject?) { node, i -> node?.optJSONArray("children")?.optJSONObject(i) }

  private fun label(node: JSONObject): String? =
    node.optString("description").ifEmpty { null } ?: node.optString("text").ifEmpty { null }

  private fun fieldOf(node: JSONObject): String = if (node.optString("description").isNotEmpty()) "contentDescription" else "text"

  private fun JSONArray?.objectList(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
}
