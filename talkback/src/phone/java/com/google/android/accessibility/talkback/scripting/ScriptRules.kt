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

package com.google.android.accessibility.talkback.scripting

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.NodeOverrides

class ScriptRules
private constructor(
  entries: List<Entry>,
  actionEntries: List<Entry>,
  private val ownPackage: String?,
  private val front: () -> Pair<String?, String?>,
) {
  class Entry(
    val rule: ScriptRule,
    val packages: Set<String>?,
    val source: String,
    val runtime: ScriptRuntime,
  )

  private data class Key(
    val node: AccessibilityNodeInfoCompat,
    val text: String?,
    val description: String?,
  )

  private val changes = Index(entries)
  private val withActions = Index(actionEntries)
  private val cache =
    object : LinkedHashMap<Key, NodeRule?>(CACHE_SIZE, 0.75f, true) {
      override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, NodeRule?>) =
        size > CACHE_SIZE
    }
  // Whether an ancestor hides the item. Finding out asks the app for each ancestor in turn, and
  // Backtalk asks about every item it considers for focus.
  private val hiddenInsideCache =
    object : LinkedHashMap<AccessibilityNodeInfoCompat, Boolean>(CACHE_SIZE, 0.75f, true) {
      override fun removeEldestEntry(
        eldest: MutableMap.MutableEntry<AccessibilityNodeInfoCompat, Boolean>
      ) = size > CACHE_SIZE
    }
  private val aspects: Set<RuleAspect> =
    entries.flatMapTo(mutableSetOf(RuleAspect.ANY)) { aspectsOf(it.rule) }

  val hidesAny = entries.any { it.rule.hide != Hide.NONE || it.rule.group }
  private val hidesInside = entries.any { it.rule.hide == Hide.ALL || it.rule.group }

  /** Whether an answer for an item can change when the items around it do. */
  val dependsOnAncestors =
    hidesInside || (entries + actionEntries).any { it.rule.match?.inside != null }

  fun resolve(node: AccessibilityNodeInfoCompat, aspect: RuleAspect = RuleAspect.ANY): NodeRule? {
    if (changes.isEmpty || aspect !in aspects) {
      return null
    }
    val key = Key(node, node.text?.toString(), node.contentDescription?.toString())
    synchronized(cache) {
      if (cache.containsKey(key)) {
        return cache[key]
      }
    }
    val matching = changes.matching(node)
    val resolved = mergeRules(matching.map { it.rule }, matching.map { it.source })
    synchronized(cache) { cache[key] = resolved }
    return resolved
  }

  fun actions(node: AccessibilityNodeInfoCompat): List<Pair<ScriptRuntime, RuleItemAction>> =
    withActions.matching(node).flatMap { entry -> entry.rule.actions.map { entry.runtime to it } }

  fun isHidden(node: AccessibilityNodeInfoCompat): Boolean =
    hidesAny && (resolve(node)?.hide == true || hidesInside && isHiddenInside(node))

  private fun isHiddenInside(node: AccessibilityNodeInfoCompat): Boolean {
    synchronized(hiddenInsideCache) { hiddenInsideCache[node]?.let { return it } }
    val hidden = node.ancestors().any { resolve(it)?.hidesDescendants == true }
    synchronized(hiddenInsideCache) { hiddenInsideCache[node] = hidden }
    return hidden
  }

  fun clearCache() {
    synchronized(cache) { cache.clear() }
    synchronized(hiddenInsideCache) { hiddenInsideCache.clear() }
  }

  private inner class Index(entries: List<Entry>) {
    private val byId = entries.filter { it.rule.id != null }.groupBy { it.rule.id }
    private val byMatch = entries.filter { it.rule.id == null }
    private val order = entries.withIndex().associate { (index, entry) -> entry to index }

    val isEmpty: Boolean
      get() = order.isEmpty()

    fun matching(node: AccessibilityNodeInfoCompat): List<Entry> {
      if (isEmpty) return emptyList()
      val id = node.viewIdResourceName
      val idMatches =
        id?.let { byId[it].orEmpty() + byId[it.substringAfter(":id/", it)].orEmpty() }
      val (activity, window) = front()
      val packageName = node.packageName?.toString()
      if (packageName == ownPackage) return emptyList()
      return (idMatches.orEmpty() + byMatch)
        .distinct()
        .filter { entry ->
          val rule = entry.rule
          (entry.packages == null || packageName in entry.packages) &&
            (rule.activity == null || activity?.endsWith(rule.activity) == true) &&
            (rule.window == null || window?.contains(rule.window, ignoreCase = true) == true) &&
            (rule.match == null || rule.match.matches(node))
        }
        .sortedBy { order[it] }
    }
  }

  companion object {
    private const val CACHE_SIZE = 512

    val EMPTY = ScriptRules(emptyList(), emptyList(), null) { null to null }

    /**
     * Combines the rules that match one item, earliest first. For each thing a rule can change, the
     * first rule that sets it wins.
     */
    fun mergeRules(rules: List<ScriptRule>, sources: List<String> = emptyList()): NodeRule? {
      if (rules.isEmpty()) {
        return null
      }
      fun <T> first(pick: (ScriptRule) -> T?): T? = rules.firstNotNullOfOrNull(pick)
      val hide = first { it.hide.takeIf { hide -> hide != Hide.NONE } }
      return NodeRule(
        label = first { it.label },
        speak = first { it.speak },
        hide = hide != null,
        hideInside = hide == Hide.ALL,
        role = first { it.role }?.let(ScriptRoles::of),
        state = first { it.state },
        hint = first { it.hint },
        heading = first { it.heading },
        group = rules.any { it.group },
        order = first(::orderOf),
        sources = sources,
        activate = first { it.activate }?.let { query -> { node -> query.firstIn(node) } },
      )
    }

    fun of(
      runtimes: Collection<ScriptRuntime>,
      ownPackage: String?,
      front: () -> Pair<String?, String?>,
    ): ScriptRules {
      val all =
        runtimes
          .filter { it.rules.isNotEmpty() }
          .flatMap { runtime ->
            val manifest = runtime.script.manifest
            val packages =
              manifest.apps.map { it.packageName }.toSet().takeUnless { manifest.isGlobal }
            runtime.rules.map { Entry(it, packages, "${manifest.name}: ${it.target}", runtime) }
          }
      val entries =
        all.filter { it.rule.changesItem && it.runtime.allowed(ScriptPermission.SPEECH, "Rules") }
      val actionEntries =
        all.filter {
          it.rule.actions.isNotEmpty() &&
            it.runtime.allowed(ScriptPermission.INPUT, "Actions in rules")
        }
      return if (all.isEmpty()) EMPTY else ScriptRules(entries, actionEntries, ownPackage, front)
    }

    private fun orderOf(rule: ScriptRule): NodeOverrides.Order? =
      rule.readBefore?.let { ReadOrder(true, it) } ?: rule.readAfter?.let { ReadOrder(false, it) }

    private fun aspectsOf(rule: ScriptRule): List<RuleAspect> =
      listOfNotNull(
        RuleAspect.LABEL.takeIf { rule.label != null },
        RuleAspect.SPEAK.takeIf { rule.speak != null || rule.hide != Hide.NONE },
        RuleAspect.ROLE.takeIf { rule.role != null },
        RuleAspect.STATE.takeIf { rule.state != null },
        RuleAspect.HINT.takeIf { rule.hint != null },
        RuleAspect.HEADING.takeIf { rule.heading != null },
        RuleAspect.GROUP.takeIf { rule.group },
        RuleAspect.ORDER.takeIf { rule.readBefore != null || rule.readAfter != null },
        RuleAspect.ACTIVATE.takeIf { rule.activate != null },
      )
  }

  private class ReadOrder(private val before: Boolean, private val target: NodeQuery) :
    NodeOverrides.Order {
    override fun isBefore(): Boolean = before

    override fun isTarget(node: AccessibilityNodeInfoCompat): Boolean = target.matches(node)
  }
}
