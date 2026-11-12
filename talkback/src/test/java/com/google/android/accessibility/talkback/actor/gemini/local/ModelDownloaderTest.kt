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

package com.google.android.accessibility.talkback.actor.gemini.local

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ModelDownloaderTest {
  private val data = ByteArray(300_000) { (it * 31).toByte() }
  private val sha = ModelFiles.sha256(data.inputStream())
  private lateinit var server: HttpServer
  private lateinit var dir: File
  private val rangeRequests = mutableListOf<String?>()
  private var ignoreRange = false

  @Before
  fun setUp() {
    dir = Files.createTempDirectory("models").toFile()
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    // The same file on two servers, such as Hugging Face and hf-mirror.com.
    for (path in listOf("/model", "/mirror/model")) {
      server.createContext(path) { ex ->
        val range = ex.requestHeaders.getFirst("Range")
        rangeRequests.add(range)
        val start =
          if (range != null && !ignoreRange) range.removePrefix("bytes=").removeSuffix("-").toInt()
          else 0
        val body = data.copyOfRange(start, data.size)
        ex.sendResponseHeaders(if (start > 0) 206 else 200, body.size.toLong())
        ex.responseBody.use { it.write(body) }
      }
    }
    server.createContext("/missing") { ex ->
      ex.sendResponseHeaders(404, -1)
      ex.close()
    }
    server.start()
  }

  @After
  fun tearDown() {
    server.stop(0)
    dir.deleteRecursively()
  }

  private fun run(
    target: File = File(dir, "m.litertlm"),
    size: Long = data.size.toLong(),
    hash: String = sha,
    path: String = "/model",
    cancelAfter: Long = Long.MAX_VALUE,
  ): ModelDownloader.Result {
    var bytes = 0L
    return ModelDownloader()
      .download(
        "http://127.0.0.1:${server.address.port}$path",
        target,
        size,
        hash,
        { bytes = it },
        { bytes >= cancelAfter },
      )
  }

  @Test
  fun downloadsAndInstallsAVerifiedFile() {
    val target = File(dir, "m.litertlm")
    assertEquals(ModelDownloader.Result.Done, run(target))
    assertTrue(data.contentEquals(target.readBytes()))
    assertFalse(ModelFiles.partFile(target).exists())
  }

  @Test
  fun cancelKeepsThePartAndTheNextRunResumesIt() {
    val target = File(dir, "m.litertlm")
    assertEquals(ModelDownloader.Result.Cancelled, run(target, cancelAfter = 100_000))
    val partial = ModelFiles.partFile(target).length()
    assertTrue(partial in 1 until data.size)
    assertFalse(target.exists())

    rangeRequests.clear()
    assertEquals(ModelDownloader.Result.Done, run(target))
    assertEquals("bytes=$partial-", rangeRequests.single())
    assertTrue(data.contentEquals(target.readBytes()))
  }

  @Test
  fun aPartFromOneServerIsResumedFromAnother() {
    val target = File(dir, "m.litertlm")
    assertEquals(ModelDownloader.Result.Cancelled, run(target, cancelAfter = 100_000))
    val partial = ModelFiles.partFile(target).length()

    rangeRequests.clear()
    assertEquals(ModelDownloader.Result.Done, run(target, path = "/mirror/model"))
    assertEquals("bytes=$partial-", rangeRequests.single())
    assertTrue(data.contentEquals(target.readBytes()))
  }

  @Test
  fun restartsWhenTheServerIgnoresTheRange() {
    val target = File(dir, "m.litertlm")
    run(target, cancelAfter = 100_000)
    ignoreRange = true
    assertEquals(ModelDownloader.Result.Done, run(target))
    assertTrue(data.contentEquals(target.readBytes()))
  }

  @Test
  fun aWrongHashDeletesTheFile() {
    val target = File(dir, "m.litertlm")
    val result = run(target, hash = "0".repeat(64))
    assertEquals(ModelDownloader.Result.Failed(ModelFiles.Failure.HASH_MISMATCH), result)
    assertFalse(target.exists())
    assertFalse(ModelFiles.partFile(target).exists())
  }

  @Test
  fun aWrongSizeIsRejected() {
    val result = run(size = data.size.toLong() + 1)
    assertEquals(ModelDownloader.Result.Failed(ModelFiles.Failure.SIZE_MISMATCH), result)
  }

  @Test
  fun anHttpErrorFails() {
    val result = run(path = "/missing")
    assertEquals(ModelDownloader.Result.Failed(ModelFiles.Failure.HTTP), result)
  }

  @Test
  fun anUnreachableServerIsANetworkFailure() {
    server.stop(0)
    assertEquals(ModelDownloader.Result.Failed(ModelFiles.Failure.NETWORK), run())
  }
}
