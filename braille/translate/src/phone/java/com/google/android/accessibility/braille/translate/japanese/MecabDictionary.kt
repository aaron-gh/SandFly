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

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * A dictionary compiled by MeCab's mecab-dict-index (sys.dic or unk.dic): a double-array trie from
 * UTF-8 surfaces to tokens, and the tokens' feature strings. Memory-mapped, read-only.
 */
internal class MecabDictionary(file: File) {
  private val buffer: ByteBuffer = map(file)
  /** Left and right context sizes, which must match the connection matrix. */
  val leftSize: Int
  val rightSize: Int
  private val arrayStart: Int
  private val arrayUnits: Int
  private val tokenStart: Int
  private val featureStart: Int
  private val featureEnd: Int

  init {
    val size = buffer.limit()
    if (size < HEADER_SIZE) throw IOException("${file.name}: too short")
    val magic = buffer.getInt(0)
    if (magic xor MAGIC != size) throw IOException("${file.name}: not a MeCab dictionary")
    if (buffer.getInt(4) != VERSION) throw IOException("${file.name}: version ${buffer.getInt(4)}")
    leftSize = buffer.getInt(16)
    rightSize = buffer.getInt(20)
    val arrayBytes = buffer.getInt(24)
    val tokenBytes = buffer.getInt(28)
    val featureBytes = buffer.getInt(32)
    val charset = ByteArray(32)
    for (i in charset.indices) charset[i] = buffer.get(40 + i)
    if (String(charset, Charsets.US_ASCII).substringBefore('\u0000').lowercase().replace("-", "") != "utf8") {
      throw IOException("${file.name}: not UTF-8")
    }
    arrayStart = HEADER_SIZE
    arrayUnits = arrayBytes / UNIT_SIZE
    tokenStart = arrayStart + arrayBytes
    featureStart = tokenStart + tokenBytes
    featureEnd = featureStart + featureBytes
    if (arrayBytes < 0 || tokenBytes < 0 || featureBytes < 0 || featureEnd > size) {
      throw IOException("${file.name}: sizes don't match the file")
    }
  }

  private fun base(unit: Int): Int =
    if (unit in 0 until arrayUnits) buffer.getInt(arrayStart + unit * UNIT_SIZE) else 0

  private fun check(unit: Int): Int =
    if (unit in 0 until arrayUnits) buffer.getInt(arrayStart + unit * UNIT_SIZE + 4) else -1

  /**
   * Calls [found] with the value and byte length of each dictionary key that [key] starts with
   * from [from] up to [to], shortest first, as Darts' commonPrefixSearch does.
   */
  fun commonPrefixSearch(key: ByteArray, from: Int, to: Int, found: (value: Int, length: Int) -> Unit) {
    var b = base(0)
    for (i in from until to) {
      val n = base(b)
      if (b == check(b) && n < 0) found(-n - 1, i - from)
      val p = b + (key[i].toInt() and 0xff) + 1
      if (b != check(p)) return
      b = base(p)
    }
    val n = base(b)
    if (b == check(b) && n < 0) found(-n - 1, to - from)
  }

  /** The value of [key] exactly, or -1 when it isn't in the dictionary. */
  fun exactMatchSearch(key: ByteArray): Int {
    var b = base(0)
    for (byte in key) {
      val p = b + (byte.toInt() and 0xff) + 1
      if (b != check(p)) return -1
      b = base(p)
    }
    val n = base(b)
    return if (b == check(b) && n < 0) -n - 1 else -1
  }

  fun leftId(token: Int): Int = buffer.getShort(tokenStart + token * TOKEN_SIZE).toInt() and 0xffff

  fun rightId(token: Int): Int = buffer.getShort(tokenStart + token * TOKEN_SIZE + 2).toInt() and 0xffff

  fun wordCost(token: Int): Int = buffer.getShort(tokenStart + token * TOKEN_SIZE + 6).toInt()

  /** The feature string of [token]: part of speech, reading and so on, comma-separated. */
  fun feature(token: Int): String {
    val start = featureStart + buffer.getInt(tokenStart + token * TOKEN_SIZE + 8)
    var end = start
    while (end < featureEnd && buffer.get(end) != 0.toByte()) end++
    val bytes = ByteArray(end - start)
    for (i in bytes.indices) bytes[i] = buffer.get(start + i)
    return String(bytes, Charsets.UTF_8)
  }

  companion object {
    private const val MAGIC = 0xef718f77.toInt()
    private const val VERSION = 102
    private const val HEADER_SIZE = 72
    private const val UNIT_SIZE = 8
    private const val TOKEN_SIZE = 16

    /** The first token and the token count of a search result's [value]. */
    fun firstToken(value: Int): Int = value ushr 8

    fun tokenCount(value: Int): Int = value and 0xff

    fun map(file: File): ByteBuffer =
      RandomAccessFile(file, "r").use {
        it.channel.map(FileChannel.MapMode.READ_ONLY, 0, it.length()).order(ByteOrder.LITTLE_ENDIAN)
      }
  }
}

/** MeCab's matrix.bin: the cost of each right context followed by each left context. */
internal class MecabMatrix(file: File) {
  private val buffer = MecabDictionary.map(file)
  val leftSize = buffer.getShort(0).toInt() and 0xffff
  val rightSize = buffer.getShort(2).toInt() and 0xffff

  init {
    if (buffer.limit() != 4 + leftSize * rightSize * 2) throw IOException("${file.name}: wrong size")
  }

  /** The cost of a word with right context [rightId] followed by one with left context [leftId]. */
  fun cost(rightId: Int, leftId: Int): Int = buffer.getShort(4 + (rightId + leftSize * leftId) * 2).toInt()
}

/** MeCab's char.bin: the character categories of char.def, for unknown words. */
internal class MecabCharProperty(file: File) {
  private val buffer = MecabDictionary.map(file)
  /** The category names, in category number order. */
  val names: List<String>
  private val mapStart: Int

  init {
    val count = buffer.getInt(0)
    if (count !in 1..32 || buffer.limit() != 4 + count * 32 + MAP_SIZE * 4) {
      throw IOException("${file.name}: wrong size")
    }
    names =
      (0 until count).map { i ->
        val bytes = ByteArray(32)
        for (j in 0 until 32) bytes[j] = buffer.get(4 + i * 32 + j)
        String(bytes, Charsets.UTF_8).substringBefore('\u0000')
      }
    mapStart = 4 + count * 32
  }

  /** The packed CharInfo of UCS-2 code [code]: see [type], [defaultType] and the others. */
  fun info(code: Int): Int = buffer.getInt(mapStart + (if (code in 0 until MAP_SIZE) code else 0) * 4)

  companion object {
    private const val MAP_SIZE = 0xffff

    /** The categories a character is in, as bits. */
    fun type(info: Int): Int = info and 0x3ffff

    /** The category whose unknown-word entries a character gets. */
    fun defaultType(info: Int): Int = (info ushr 18) and 0xff

    /** How many characters long the unknown words started by a character can be. */
    fun length(info: Int): Int = (info ushr 26) and 0xf

    /** Whether characters of the same category are grouped into one unknown word. */
    fun group(info: Int): Boolean = (info ushr 30) and 1 != 0

    /** Whether unknown words are tried even where the dictionary has words. */
    fun invoke(info: Int): Boolean = (info ushr 31) and 1 != 0

    fun isKindOf(info: Int, other: Int): Boolean = type(info) and type(other) != 0
  }
}
