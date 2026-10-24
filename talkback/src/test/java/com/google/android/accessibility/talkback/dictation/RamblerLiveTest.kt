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

package com.google.android.accessibility.talkback.dictation

import android.os.Looper
import java.io.DataInputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.security.MessageDigest
import java.time.Duration
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RamblerLiveTest {
  /**
   * A WebSocket server for one connection that answers like the Live API: setupComplete after the
   * setup, the text of a phrase after the first audio, and the last phrase after the end of the
   * audio. Keeps the headers and messages it got.
   */
  private class FakeLiveApi(private val answerAtEnd: Boolean = true) : AutoCloseable {
    private val server = ServerSocket(0)
    val headers = mutableMapOf<String, String>()
    val messages = CopyOnWriteArrayList<JSONObject>()
    val url = "ws://127.0.0.1:${server.localPort}/ws"

    init {
      Thread {
          runCatching {
            server.accept().use { socket ->
              val input = DataInputStream(socket.getInputStream())
              val output = socket.getOutputStream()
              handshake(input, output)
              var sentFirst = false
              while (true) {
                val message = JSONObject(readFrame(input) ?: break)
                messages.add(message)
                when {
                  message.has("setup") -> send(output, """{"setupComplete":{}}""", binary = true)
                  message.optJSONObject("realtimeInput")?.has("audio") == true && !sentFirst -> {
                    sentFirst = true
                    send(output, """{"serverContent":{"inputTranscription":{"text":"Hello there."}}}""")
                    send(output, """{"serverContent":{"interimInputTranscription":{"text":"how are"}}}""")
                  }
                  message.optJSONObject("realtimeInput")?.has("audioStreamEnd") == true && answerAtEnd ->
                    send(output, """{"serverContent":{"inputTranscription":{"text":" How are you? "}}}""")
                }
              }
            }
          }
        }
        .apply {
          isDaemon = true
          start()
        }
    }

    private fun handshake(input: DataInputStream, output: OutputStream) {
      val lines = generateSequence { readLine(input) }.takeWhile { it.isNotEmpty() }.toList()
      lines.drop(1).forEach { line ->
        headers[line.substringBefore(':').trim().lowercase()] = line.substringAfter(':').trim()
      }
      val accept =
        Base64.getEncoder()
          .encodeToString(
            MessageDigest.getInstance("SHA-1")
              .digest((headers["sec-websocket-key"] + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray())
          )
      output.write(
        ("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n" +
            "Sec-WebSocket-Accept: $accept\r\n\r\n")
          .toByteArray()
      )
      output.flush()
    }

    private fun readLine(input: DataInputStream): String {
      val line = StringBuilder()
      while (true) {
        val c = input.read()
        if (c < 0 || c == '\n'.code) break
        if (c != '\r'.code) line.append(c.toChar())
      }
      return line.toString()
    }

    /** Reads a client frame's text, or null when the client closes. */
    private fun readFrame(input: DataInputStream): String? {
      val first = input.read()
      if (first < 0 || first and 0x0f == 8) return null
      val second = input.read()
      var length = (second and 0x7f).toLong()
      if (length == 126L) length = input.readUnsignedShort().toLong()
      else if (length == 127L) length = input.readLong()
      val mask = ByteArray(4).also { input.readFully(it) }
      val data = ByteArray(length.toInt()).also { input.readFully(it) }
      for (i in data.indices) data[i] = (data[i].toInt() xor mask[i % 4].toInt()).toByte()
      return String(data, Charsets.UTF_8)
    }

    private fun send(output: OutputStream, text: String, binary: Boolean = false) {
      val data = text.toByteArray()
      output.write(if (binary) 0x82 else 0x81)
      output.write(data.size)
      output.write(data)
      output.flush()
    }

    override fun close() = server.close()
  }

  /** Runs the main thread's tasks until [done] or five seconds pass. */
  private fun waitFor(done: () -> Boolean) {
    repeat(100) {
      if (done()) return
      Thread.sleep(50)
      shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
    }
  }

  @Test
  fun phrasesFromTheSessionAreJoinedAfterTheEndOfTheAudio() {
    FakeLiveApi().use { api ->
      val live = RamblerLive("key", api.url)
      live.start()
      val pcm = ByteArray(3200) { it.toByte() }
      live.send(pcm)
      Thread.sleep(300)
      live.send(pcm)
      waitFor { api.messages.size >= 3 }
      var result: String? = "not yet"
      live.finish { result = it }
      waitFor { result != "not yet" }

      assertEquals("Hello there. How are you?", result)
      assertEquals("key", api.headers["x-goog-api-key"])
      val setup = api.messages[0].getJSONObject("setup")
      assertEquals("models/gemini-3.5-transcribe-live", setup.getString("model"))
      assertEquals("SMART", setup.getJSONObject("inputAudioTranscription").getString("mode"))
      val audio = api.messages[1].getJSONObject("realtimeInput").getJSONObject("audio")
      assertEquals("audio/pcm;rate=16000", audio.getString("mimeType"))
      assertTrue(audio.getString("data").isNotEmpty())
      assertTrue(api.messages.last().getJSONObject("realtimeInput").getBoolean("audioStreamEnd"))
    }
  }

  @Test
  fun aLastPhraseThatNeverComesMeansTheRecordingIsSentInstead() {
    FakeLiveApi(answerAtEnd = false).use { api ->
      val live = RamblerLive("key", api.url)
      live.start()
      live.send(ByteArray(3200))
      // The phrase must have been heard before the end of the audio, or there is none to wait for.
      waitFor { live.isPhrasePending }
      var result: String? = "not yet"
      live.finish { result = it }
      waitFor { result != "not yet" }
      assertNull(result)
    }
  }

  @Test
  fun aSessionThatCantConnectFails() {
    val live = RamblerLive("key", "ws://127.0.0.1:1/ws")
    live.start()
    Thread.sleep(500)
    var result: String? = "not yet"
    live.finish { result = it }
    waitFor { result != "not yet" }
    assertNull(result)
  }
}
