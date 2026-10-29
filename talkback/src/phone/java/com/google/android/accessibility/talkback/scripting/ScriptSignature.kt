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

import java.io.ByteArrayOutputStream
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Checks whether SandFly signed a script. The signature is the last line of main.lua, a comment
 * that covers every file of the script as SandFly reads it, with main.lua up to that line.
 * tools/sign_script.py writes it, with the private key that belongs to [PUBLIC_KEY].
 */
object ScriptSignature {
  enum class Status {
    /** Signed by SandFly, and unchanged since. */
    SIGNED,
    /** Has a signature, but it doesn't match the files: changed since, or not SandFly's. */
    CHANGED,
    UNSIGNED,
  }

  const val PREFIX = "-- SandFly signature: "
  private const val HEADER = "SandFly script signature 1\n"
  private const val ALGORITHM = "SHA256withECDSA"
  /** The most a signature line can hold after [PREFIX]; a P-256 signature is under 100 characters. */
  private const val MAX_SIGNATURE_CHARS = 200

  /** SandFly's script signing key (EC P-256, X.509), SHA-256 0278323030068b4a…25a8bb. */
  private const val PUBLIC_KEY =
    "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEX/hmvPKIkIBBtQvSevroddRdNGt5gr9hkcmjZubISke34ICwp6GkWzuZ" +
      "DotkGseH3oKZ7NnShYMsRz1agg67AA=="

  private val key: PublicKey by lazy {
    KeyFactory.getInstance("EC")
      .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY)))
  }

  /** Whether SandFly signed [files], a script's files by name, with main.lua among them. */
  fun check(files: Map<String, ByteArray>): Status = check(files, key)

  internal fun check(files: Map<String, ByteArray>, key: PublicKey): Status {
    val main = files[ScriptPackage.MAIN] ?: return Status.UNSIGNED
    val (body, signature) = split(main) ?: return Status.UNSIGNED
    val bytes =
      try {
        Base64.getDecoder().decode(signature)
      } catch (e: IllegalArgumentException) {
        return Status.CHANGED
      }
    val signed = signedBytes(files + (ScriptPackage.MAIN to body))
    return try {
      val verifier = Signature.getInstance(ALGORITHM)
      verifier.initVerify(key)
      verifier.update(signed)
      if (verifier.verify(bytes)) Status.SIGNED else Status.CHANGED
    } catch (e: GeneralSecurityException) {
      Status.CHANGED
    }
  }

  /**
   * main.lua up to its signature line, and the signature's text, or null if it has no signature
   * line. Anything but spaces after the signature counts as part of it, so that code added after
   * the signature doesn't match.
   */
  internal fun split(main: ByteArray): Pair<ByteArray, String>? {
    val prefix = PREFIX.toByteArray()
    var start = -1
    for (i in main.size - prefix.size downTo 0) {
      if ((i == 0 || main[i - 1] == '\n'.code.toByte()) && main.startsWith(prefix, i)) {
        start = i
        break
      }
    }
    if (start < 0) return null
    val rest = main.copyOfRange(start + prefix.size, main.size).decodeToString().trimEnd()
    if (rest.length > MAX_SIGNATURE_CHARS) return main.copyOfRange(0, start) to ""
    return main.copyOfRange(0, start) to rest
  }

  /**
   * What a signature covers: a header, then each file, sorted by the bytes of its name, as its
   * name, its length and its contents, each followed by a line break.
   */
  internal fun signedBytes(files: Map<String, ByteArray>): ByteArray {
    val out = ByteArrayOutputStream()
    out.write(HEADER.toByteArray())
    val names = files.keys.map { it to it.toByteArray() }.sortedWith { a, b -> compare(a.second, b.second) }
    for ((name, nameBytes) in names) {
      val content = files.getValue(name)
      out.write(nameBytes)
      out.write('\n'.code)
      out.write(content.size.toString().toByteArray())
      out.write('\n'.code)
      out.write(content)
      out.write('\n'.code)
    }
    return out.toByteArray()
  }

  private fun compare(a: ByteArray, b: ByteArray): Int {
    for (i in 0 until minOf(a.size, b.size)) {
      val c = (a[i].toInt() and 0xff) - (b[i].toInt() and 0xff)
      if (c != 0) return c
    }
    return a.size - b.size
  }

  private fun ByteArray.startsWith(prefix: ByteArray, at: Int): Boolean =
    at + prefix.size <= size && prefix.indices.all { this[at + it] == prefix[it] }
}
