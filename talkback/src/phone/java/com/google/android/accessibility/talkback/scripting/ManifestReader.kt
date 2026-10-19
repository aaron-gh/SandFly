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
 * Reads the manifest from a script's main.lua without running it: the table literal in
 * `local manifest = { ... }`, which can only hold text, numbers, true, false and tables.
 *
 * A table with only listed values becomes a JSON array, and one with only named fields a JSON
 * object. An empty table is an empty array.
 */
object ManifestReader {
  private const val MAX_DEPTH = 32

  fun read(source: String): String {
    val lexer = Lexer(source)
    val start =
      findStart(lexer)
        ?: manifestError("main.lua must have local manifest = { ... } before the script's code")
    lexer.position = start
    val value = Parser(lexer).table(0)
    return when (value) {
      is JSONObject -> value.toString()
      is JSONArray ->
        if (value.length() == 0) "{}" else manifestError("The manifest must have named fields")
      else -> manifestError("The manifest must be a table")
    }
  }

  /** The position of the `{` of the first `manifest = {`, or null if there is none. */
  private fun findStart(lexer: Lexer): Int? {
    // The last three tokens, newest first.
    var last: Token? = null
    var second: Token? = null
    var third: Token? = null
    while (true) {
      val token = lexer.next() ?: return null
      if (
        token.isSymbol("{") &&
          last?.isSymbol("=") == true &&
          second?.kind == Kind.NAME &&
          second.text == "manifest" &&
          third?.isSymbol(".") != true &&
          third?.isSymbol(":") != true
      ) {
        return token.start
      }
      third = second
      second = last
      last = token
    }
  }

  private enum class Kind {
    NAME,
    STRING,
    NUMBER,
    SYMBOL,
  }

  private class Token(val kind: Kind, val text: String, val start: Int, val value: Any? = null) {
    fun isSymbol(symbol: String): Boolean = kind == Kind.SYMBOL && text == symbol
  }

  private class Lexer(private val source: String) {
    var position = 0

    fun next(): Token? {
      skipSpaceAndComments()
      if (position >= source.length) return null
      val start = position
      val c = source[position]
      return when {
        c.isLetter() || c == '_' -> {
          while (position < source.length && (source[position].isLetterOrDigit() || source[position] == '_')) {
            position++
          }
          Token(Kind.NAME, source.substring(start, position), start)
        }
        c.isDigit() || (c == '.' && source.getOrNull(position + 1)?.isDigit() == true) -> number(start)
        c == '"' || c == '\'' -> Token(Kind.STRING, "", start, quoted(c))
        c == '[' && longBracketLevel(position) >= 0 -> {
          val level = longBracketLevel(position)
          Token(Kind.STRING, "", start, longString(level))
        }
        else -> {
          position++
          Token(Kind.SYMBOL, c.toString(), start)
        }
      }
    }

    private fun skipSpaceAndComments() {
      while (position < source.length) {
        val c = source[position]
        if (c.isWhitespace()) {
          position++
        } else if (source.startsWith("--", position)) {
          position += 2
          val level = longBracketLevel(position)
          if (level >= 0) {
            longString(level)
          } else {
            while (position < source.length && source[position] != '\n') position++
          }
        } else {
          return
        }
      }
    }

    /** The level of a long bracket such as `[==[` at [at], or -1 if there is none. */
    private fun longBracketLevel(at: Int): Int {
      if (source.getOrNull(at) != '[') return -1
      var i = at + 1
      while (source.getOrNull(i) == '=') i++
      return if (source.getOrNull(i) == '[') i - at - 1 else -1
    }

    private fun longString(level: Int): String {
      position += level + 2
      if (source.getOrNull(position) == '\r') position++
      if (source.getOrNull(position) == '\n') position++
      val close = "]" + "=".repeat(level) + "]"
      val end = source.indexOf(close, position)
      if (end < 0) manifestError("The manifest has a long string or comment that isn't closed")
      val text = source.substring(position, end)
      position = end + close.length
      return text
    }

    private fun quoted(quote: Char): String {
      position++
      val out = StringBuilder()
      while (true) {
        if (position >= source.length) manifestError("The manifest has text that isn't closed")
        val c = source[position++]
        when (c) {
          quote -> return out.toString()
          '\n' -> manifestError("The manifest has text that isn't closed")
          '\\' -> escape(out)
          else -> out.append(c)
        }
      }
    }

    private fun escape(out: StringBuilder) {
      if (position >= source.length) manifestError("The manifest has text that isn't closed")
      when (val c = source[position++]) {
        'n' -> out.append('\n')
        't' -> out.append('\t')
        'r' -> out.append('\r')
        'a' -> out.append('\u0007')
        'b' -> out.append('\b')
        'f' -> out.append('\u000c')
        'v' -> out.append('\u000b')
        '\\', '"', '\'' -> out.append(c)
        '\n' -> out.append('\n')
        'z' -> while (position < source.length && source[position].isWhitespace()) position++
        'x' -> {
          val hex = source.substring(position, minOf(position + 2, source.length))
          out.append(hex.toIntOrNull(16)?.toChar() ?: manifestError("The manifest has a bad \\x escape"))
          position += 2
        }
        'u' -> {
          val close = source.indexOf('}', position)
          if (source.getOrNull(position) != '{' || close < 0) {
            manifestError("The manifest has a bad \\u escape")
          }
          val code =
            source.substring(position + 1, close).toIntOrNull(16)
              ?: manifestError("The manifest has a bad \\u escape")
          out.appendCodePoint(code)
          position = close + 1
        }
        else ->
          if (c.isDigit()) {
            var end = position
            while (end < source.length && end < position + 2 && source[end].isDigit()) end++
            val code = source.substring(position - 1, end).toInt()
            if (code > 255) manifestError("The manifest has a bad escape")
            out.append(code.toChar())
            position = end
          } else {
            manifestError("The manifest has a bad escape \\$c")
          }
      }
    }

    private fun number(start: Int): Token {
      val hex = source.startsWith("0x", start, ignoreCase = true)
      position = start + if (hex) 2 else 0
      while (position < source.length) {
        val c = source[position]
        val exponent = if (hex) c == 'p' || c == 'P' else c == 'e' || c == 'E'
        if (exponent && (source.getOrNull(position + 1) == '-' || source.getOrNull(position + 1) == '+')) {
          position += 2
        } else if (c.isLetterOrDigit() || c == '.') {
          position++
        } else {
          break
        }
      }
      val text = source.substring(start, position)
      val value: Any =
        if (hex) {
          text.substring(2).toLongOrNull(16) ?: manifestError("The manifest has a number SandFly can't read: $text")
        } else {
          text.toLongOrNull() ?: text.toDoubleOrNull() ?: manifestError("The manifest has a bad number $text")
        }
      return Token(Kind.NUMBER, text, start, value)
    }
  }

  private class Parser(private val lexer: Lexer) {
    private var peeked: Token? = null

    private fun peek(): Token? = peeked ?: lexer.next().also { peeked = it }

    private fun take(): Token? = peek().also { peeked = null }

    private fun expect(symbol: String) {
      val token = take()
      if (token?.kind != Kind.SYMBOL || token.text != symbol) {
        manifestError("The manifest is missing $symbol")
      }
    }

    /** Parses a table constructor. Gives a JSONObject or a JSONArray. */
    fun table(depth: Int): Any {
      if (depth > MAX_DEPTH) manifestError("The manifest is nested too deeply")
      expect("{")
      val list = JSONArray()
      val named = JSONObject()
      while (true) {
        val token = peek() ?: manifestError("The manifest table isn't closed")
        if (token.kind == Kind.SYMBOL && token.text == "}") {
          take()
          break
        }
        if (token.kind == Kind.SYMBOL && token.text == "[") {
          take()
          val key = value(depth)
          expect("]")
          expect("=")
          named.put(key?.toString() ?: manifestError("A manifest key can't be nil"), value(depth) ?: JSONObject.NULL)
        } else if (token.kind == Kind.NAME && !isLiteralName(token.text) && isFollowedByEquals()) {
          take()
          expect("=")
          val value = value(depth)
          if (value != null) named.put(token.text, value)
        } else {
          list.put(value(depth) ?: JSONObject.NULL)
        }
        val separator = peek()
        if (separator?.kind == Kind.SYMBOL && (separator.text == "," || separator.text == ";")) {
          take()
        } else if (separator?.kind != Kind.SYMBOL || separator.text != "}") {
          manifestError("The manifest is missing a comma")
        }
      }
      if (list.length() > 0 && named.length() > 0) {
        manifestError("A manifest table has either listed values or named fields, not both")
      }
      return if (named.length() > 0) named else list
    }

    private fun isFollowedByEquals(): Boolean {
      val saved = lexer.position
      val savedPeek = peeked
      take()
      val next = peek()
      val result = next?.kind == Kind.SYMBOL && next.text == "="
      lexer.position = saved
      peeked = savedPeek
      return result
    }

    private fun value(depth: Int): Any? {
      val token = peek() ?: manifestError("The manifest ends too soon")
      return when (token.kind) {
        Kind.STRING,
        Kind.NUMBER -> take()!!.value
        Kind.NAME ->
          when (take()!!.text) {
            "true" -> true
            "false" -> false
            "nil" -> null
            else ->
              manifestError(
                "The manifest must be plain values: text, numbers, true, false and tables, not ${token.text}"
              )
          }
        Kind.SYMBOL ->
          when (token.text) {
            "{" -> table(depth + 1)
            "-" -> {
              take()
              when (val number = take()?.takeIf { it.kind == Kind.NUMBER }?.value) {
                is Long -> -number
                is Double -> -number
                else -> manifestError("The manifest has - without a number")
              }
            }
            else -> manifestError("The manifest must be plain values, not ${token.text}")
          }
      }
    }

    private fun isLiteralName(name: String): Boolean = name == "true" || name == "false" || name == "nil"
  }
}
