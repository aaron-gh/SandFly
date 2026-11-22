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

package com.google.android.accessibility.talkback.update

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResumableDownloadTest {
  private var data = ByteArray(300_000) { (it * 31).toByte() }
  private var etag = "\"v1\""
  private lateinit var server: HttpServer
  private lateinit var dir: File
  private lateinit var part: File
  private val ranges = mutableListOf<String?>()
  private val ifRanges = mutableListOf<String?>()
  /** Requests that send only this many bytes of the body, then drop the connection. */
  private val drops = ArrayDeque<Int>()
  private var ignoreRange = false
  private var rangeStartOffset = 0
  private var status = 0

  @Before
  fun setUp() {
    dir = Files.createTempDirectory("update").toFile()
    part = File(dir, "sandfly.apk.part")
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/apk") { ex -> serve(ex) }
    server.start()
  }

  @After
  fun tearDown() {
    server.stop(0)
    dir.deleteRecursively()
  }

  private fun serve(ex: HttpExchange) {
    val range = ex.requestHeaders.getFirst("Range")
    val ifRange = ex.requestHeaders.getFirst("If-Range")
    synchronized(ranges) {
      ranges.add(range)
      ifRanges.add(ifRange)
    }
    ex.responseHeaders.add("Connection", "close")
    if (status != 0) {
      ex.sendResponseHeaders(status, -1)
      ex.close()
      return
    }
    val useRange = range != null && !ignoreRange && (ifRange == null || ifRange == etag)
    val start =
      if (useRange) range!!.removePrefix("bytes=").removeSuffix("-").toInt() + rangeStartOffset
      else 0
    if (useRange && start >= data.size) {
      ex.sendResponseHeaders(416, -1)
      ex.close()
      return
    }
    val body = data.copyOfRange(start, data.size)
    ex.responseHeaders.add("ETag", etag)
    if (useRange) {
      ex.responseHeaders.add("Content-Range", "bytes $start-${data.size - 1}/${data.size}")
    }
    ex.sendResponseHeaders(if (useRange) 206 else 200, body.size.toLong())
    val drop = synchronized(drops) { drops.removeFirstOrNull() }
    try {
      ex.responseBody.write(body, 0, drop ?: body.size)
      ex.responseBody.flush()
      ex.close()
    } catch (e: IOException) {
      // Closing with fewer bytes than announced drops the connection.
    }
  }

  private val url: String
    get() = "http://127.0.0.1:${server.address.port}/apk"

  private fun download(size: Long = data.size.toLong(), address: String = url) =
    ResumableDownload(
      part,
      ResumableDownload.identity(address, size),
      size,
      { URL(it).openConnection() as HttpURLConnection },
    )

  @Test
  fun downloadsTheWholeFile() {
    val download = download()
    download.attempt(url, isApi = false)
    val target = File(dir, "sandfly.apk")
    download.finish(target)
    assertArrayEquals(data, target.readBytes())
    assertFalse(part.exists())
    assertFalse(File(part.path + ResumableDownload.INFO_SUFFIX).exists())
    assertEquals(listOf<String?>(null), ranges)
  }

  @Test
  fun aDroppedConnectionIsResumedWithARange() {
    drops.add(100_000)
    val download = download()
    assertThrows(IOException::class.java) { download.attempt(url, isApi = false) }
    val have = part.length()
    assertTrue("part has $have bytes", have in 1..100_000)
    download.attempt(url, isApi = false)
    assertArrayEquals(data, part.readBytes())
    assertEquals("bytes=$have-", ranges.last())
    assertEquals(etag, ifRanges.last())
  }

  @Test
  fun retriesResumeUntilTheFileIsComplete() {
    drops.addAll(listOf(50_000, 50_000, 50_000))
    val download = download()
    val slept = mutableListOf<Long>()
    DownloadRetry.run({ slept += it }) { download.attempt(url, isApi = false) }
    assertArrayEquals(data, part.readBytes())
    assertEquals(3, slept.size)
    assertNull(ranges.first())
    assertTrue(ranges.drop(1).all { it != null && it.startsWith("bytes=") })
  }

  @Test
  fun aLaterDownloadOfTheSameBuildResumesThePart() {
    drops.add(120_000)
    assertThrows(IOException::class.java) { download().attempt(url, isApi = false) }
    val have = part.length()
    download().attempt(url, isApi = false)
    assertArrayEquals(data, part.readBytes())
    assertEquals("bytes=$have-", ranges.last())
  }

  @Test
  fun aPartOfAnotherBuildIsDeleted() {
    drops.add(120_000)
    assertThrows(IOException::class.java) { download().attempt(url, isApi = false) }
    assertTrue(part.length() > 0)
    download(size = data.size.toLong() + 1)
    assertFalse(part.exists())
  }

  @Test
  fun aServerIgnoringTheRange_startsAgain() {
    drops.add(100_000)
    val download = download()
    assertThrows(IOException::class.java) { download.attempt(url, isApi = false) }
    ignoreRange = true
    download.attempt(url, isApi = false)
    assertArrayEquals(data, part.readBytes())
  }

  @Test
  fun aChangedFile_isDownloadedFromTheStart() {
    drops.add(100_000)
    // No asset size, as from an older notification.
    val download = download(size = -1)
    assertThrows(IOException::class.java) { download.attempt(url, isApi = false) }
    data = ByteArray(250_000) { (it * 7).toByte() }
    etag = "\"v2\""
    download.attempt(url, isApi = false)
    assertArrayEquals(data, part.readBytes())
  }

  @Test
  fun aRangeAnswerFromTheWrongByte_deletesThePart() {
    drops.add(100_000)
    val download = download()
    assertThrows(IOException::class.java) { download.attempt(url, isApi = false) }
    rangeStartOffset = 10
    assertThrows(IOException::class.java) { download.attempt(url, isApi = false) }
    assertFalse(part.exists())
    rangeStartOffset = 0
    download.attempt(url, isApi = false)
    assertArrayEquals(data, part.readBytes())
  }

  @Test
  fun aCompletePart_isNotRequestedAgain() {
    val download = download()
    download.attempt(url, isApi = false)
    download.attempt(url, isApi = false)
    assertEquals(1, ranges.size)
  }

  @Test
  fun aPartLongerThanTheAsset_isDeleted() {
    part.writeBytes(ByteArray(data.size + 10))
    File(part.path + ResumableDownload.INFO_SUFFIX)
      .writeText(ResumableDownload.identity(url, data.size.toLong()) + "\n" + etag)
    download().attempt(url, isApi = false)
    assertArrayEquals(data, part.readBytes())
    assertEquals(listOf<String?>(null), ranges)
  }

  @Test
  fun aRangeAnswerOfAnotherSize_deletesThePart() {
    val size = data.size.toLong() + 1
    part.writeBytes(data.copyOfRange(0, 1000))
    File(part.path + ResumableDownload.INFO_SUFFIX)
      .writeText(ResumableDownload.identity(url, size) + "\n" + etag)
    assertThrows(IOException::class.java) { download(size = size).attempt(url, isApi = false) }
    assertFalse(part.exists())
  }

  @Test
  fun aFullAnswerOfAnotherSize_isKeptWhole() {
    // The build at the address was replaced after the check; the package checks still apply.
    download(size = data.size.toLong() - 1).attempt(url, isApi = false)
    assertArrayEquals(data, part.readBytes())
  }

  @Test
  fun notFound_isPermanent() {
    status = 404
    assertThrows(PermanentDownloadException::class.java) { download().attempt(url, isApi = false) }
  }

  @Test
  fun serverErrorsAndApiFailures_areRetried() {
    status = 503
    val first = assertThrows(IOException::class.java) { download().attempt(url, isApi = false) }
    assertFalse(first is PermanentDownloadException)
    status = 403
    val api = assertThrows(IOException::class.java) { download().attempt(url, isApi = true) }
    assertFalse(api is PermanentDownloadException)
  }

  @Test
  fun parseContentRange() {
    assertEquals(100L to 200L, ResumableDownload.parseContentRange("bytes 100-199/200"))
    assertEquals(0L to -1L, ResumableDownload.parseContentRange("bytes 0-99/*"))
    assertNull(ResumableDownload.parseContentRange("bytes */200"))
    assertNull(ResumableDownload.parseContentRange(null))
  }
}
