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

package com.google.android.accessibility.braille.translate.japanese

import com.google.android.accessibility.braille.translate.japanese.MecabCharProperty.Companion.defaultType
import com.google.android.accessibility.braille.translate.japanese.MecabCharProperty.Companion.group
import com.google.android.accessibility.braille.translate.japanese.MecabCharProperty.Companion.invoke
import com.google.android.accessibility.braille.translate.japanese.MecabCharProperty.Companion.isKindOf
import com.google.android.accessibility.braille.translate.japanese.MecabCharProperty.Companion.length
import java.io.File
import java.io.IOException

/**
 * Splits Japanese text into words with a dictionary compiled by MeCab, the way MeCab 0.996 does:
 * dictionary words and unknown words by character category make a lattice, and the path with the
 * lowest word and connection costs is the analysis. Thread-safe: each [parse] has its own lattice.
 */
internal class MecabTagger(directory: File) {
  private val system = MecabDictionary(File(directory, "sys.dic"))
  private val unknown = MecabDictionary(File(directory, "unk.dic"))
  private val matrix = MecabMatrix(File(directory, "matrix.bin"))
  private val chars = MecabCharProperty(File(directory, "char.bin"))
  /** The unknown-word tokens of each character category: first token and count. */
  private val unknownTokens: List<Pair<Int, Int>>
  private val space = chars.info(0x20)

  init {
    if (system.leftSize != matrix.leftSize || system.rightSize != matrix.rightSize) {
      throw IOException("sys.dic doesn't match matrix.bin")
    }
    unknownTokens =
      chars.names.map { name ->
        val value = unknown.exactMatchSearch(name.toByteArray(Charsets.UTF_8))
        if (value == -1) throw IOException("unk.dic has no category $name")
        MecabDictionary.firstToken(value) to MecabDictionary.tokenCount(value)
      }
  }

  /** A word in the lattice. */
  private class Node(
    val dictionary: MecabDictionary,
    val token: Int,
    /** Where the surface starts, and its length, in UTF-8 bytes. */
    val start: Int,
    val length: Int,
    /** Where the word ends, counting the spaces skipped before it. */
    val end: Int,
  ) {
    // BOS and EOS have context 0 and no word cost.
    val leftId = if (length < 0) 0 else dictionary.leftId(token)
    val rightId = if (length < 0) 0 else dictionary.rightId(token)
    val wordCost = if (length < 0) 0 else dictionary.wordCost(token)
    var cost = 0L
    var prev: Node? = null
  }

  /** The words of [text] with their features, as "surface,feature" lines like libkuraji's. */
  fun parse(text: String): List<String> {
    val bytes = text.toByteArray(Charsets.UTF_8)
    val length = bytes.size
    // The words ending at each byte position, newest first, as MeCab's end node lists are.
    val ending = arrayOfNulls<ArrayList<Node>>(length + 1)
    val bos = Node(system, 0, 0, BOUNDARY, 0)
    ending[0] = arrayListOf(bos)
    for (pos in 0 until length) {
      val left = ending[pos] ?: continue
      // MeCab's lookup gives the newest node first.
      for (node in lookup(bytes, pos).asReversed()) {
        connect(left, node, node.wordCost)
        (ending[node.end] ?: ArrayList<Node>().also { ending[node.end] = it }).add(0, node)
      }
    }
    // EOS, with context 0 and no word cost, follows the last words that end anywhere.
    val last = (length downTo 0).firstNotNullOf { ending[it] }
    val eos = Node(system, 0, length, BOUNDARY, length)
    connect(last, eos, 0)
    val words = ArrayList<String>()
    var node: Node? = eos.prev
    while (node != null && node !== bos) {
      val surface = String(bytes, node.start, node.length, Charsets.UTF_8)
      words.add(surface + "," + node.dictionary.feature(node.token))
      node = node.prev
    }
    words.reverse()
    return words
  }

  private fun connect(left: List<Node>, node: Node, wordCost: Int) {
    var best: Node? = null
    var bestCost = Long.MAX_VALUE
    for (l in left) {
      val cost = l.cost + matrix.cost(l.rightId, node.leftId) + wordCost
      // The first of equal paths wins, as in MeCab.
      if (cost < bestCost) {
        best = l
        bestCost = cost
      }
    }
    node.prev = best
    node.cost = bestCost
  }

  /** The dictionary and unknown words starting at [begin], oldest first, as MeCab's lookup. */
  private fun lookup(bytes: ByteArray, begin: Int): List<Node> {
    val end = bytes.size
    val nodes = ArrayList<Node>()
    // Spaces before a word are skipped. As in seekToOtherType, the category followed is the
    // latest character's, and [info] is that of the last character looked at.
    var begin2 = begin
    var info = 0
    var charLength = 0
    var c = space
    while (begin2 < end) {
      val next = charInfo(bytes, begin2)
      info = next.first
      charLength = next.second
      if (!isKindOf(c, info)) break
      begin2 += charLength
      c = info
    }
    system.commonPrefixSearch(bytes, begin2, end) { value, length ->
      val token = MecabDictionary.firstToken(value)
      for (j in 0 until MecabDictionary.tokenCount(value)) {
        nodes.add(Node(system, token + j, begin2, length, begin2 + length))
      }
    }
    if (nodes.isNotEmpty() && !invoke(info)) return nodes

    fun addUnknown(to: Int) {
      val (first, count) = unknownTokens[defaultType(info)]
      for (k in 0 until count) nodes.add(Node(unknown, first + k, begin2, to - begin2, to))
    }
    var begin3 = begin2 + charLength
    var groupEnd = -1
    if (begin3 > end) {
      addUnknown(end)
      return nodes
    }
    if (group(info)) {
      // As seekToOtherType: the category followed is the latest character's.
      var p = begin3
      var count = 0
      var kind = info
      while (p < end) {
        val next = charInfo(bytes, p)
        if (!isKindOf(kind, next.first)) break
        p += next.second
        count++
        kind = next.first
      }
      if (count <= MAX_GROUPING_SIZE) addUnknown(p)
      groupEnd = p
    }
    for (i in 1..length(info)) {
      if (begin3 > end) break
      if (begin3 == groupEnd) continue
      addUnknown(begin3)
      if (begin3 >= end) break
      val next = charInfo(bytes, begin3)
      if (!isKindOf(info, next.first)) break
      begin3 += next.second
    }
    if (nodes.isEmpty()) addUnknown(begin3)
    return nodes
  }

  /** The CharInfo of the character at [at] and its length in bytes, as MeCab's utf8_to_ucs2. */
  private fun charInfo(bytes: ByteArray, at: Int): Pair<Int, Int> {
    val b0 = bytes[at].toInt() and 0xff
    val left = bytes.size - at
    fun cont(i: Int) = bytes[at + i].toInt() and 0x3f
    return when {
      b0 < 0x80 -> chars.info(b0) to 1
      left >= 2 && b0 and 0xe0 == 0xc0 -> chars.info(((b0 and 0x1f) shl 6) or cont(1)) to 2
      left >= 3 && b0 and 0xf0 == 0xe0 ->
        chars.info(((b0 and 0x0f) shl 12) or (cont(1) shl 6) or cont(2)) to 3
      // Characters outside the BMP are all code 0 to MeCab.
      left >= 4 && b0 and 0xf8 == 0xf0 -> chars.info(0) to 4
      else -> chars.info(0) to 1
    }
  }

  private companion object {
    /** The length of the BOS and EOS nodes, which aren't words. */
    const val BOUNDARY = -1
    /** MeCab's default max-grouping-size. */
    const val MAX_GROUPING_SIZE = 24
  }
}
