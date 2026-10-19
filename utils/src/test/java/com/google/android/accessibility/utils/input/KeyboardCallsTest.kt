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

package com.google.android.accessibility.utils.input

import android.os.Looper
import android.os.SystemClock
import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyboardCallsTest {
  /** The text field: records each call and the thread it ran on, and has "Hello" before the cursor. */
  private val calls = mutableListOf<String>()
  private val threads = mutableSetOf<Thread>()
  private val field =
    Proxy.newProxyInstance(
      InputConnection::class.java.classLoader,
      arrayOf(InputConnection::class.java),
    ) { _, method, args ->
      synchronized(calls) {
        calls.add(method.name + (args?.firstOrNull()?.let { "($it)" } ?: ""))
        threads.add(Thread.currentThread())
      }
      when (method.name) {
        "getTextBeforeCursor" -> "Hello"
        "commitText" -> true
        else -> null
      }
    } as InputConnection
  private val connection = KeyboardCalls.wrap(field)!!
  private val keyboard = Executors.newSingleThreadExecutor()

  @After
  fun tearDown() {
    keyboard.shutdownNow()
    // Runs what is left, so that the next test starts with the main thread free.
    shadowOf(Looper.getMainLooper()).idle()
  }

  /** The keyboard calls [call] on its own thread, as Android does through the handler. */
  private fun <T> fromKeyboard(call: () -> T): T = keyboard.submit(Callable(call)).get(5, TimeUnit.SECONDS)

  @Test
  fun keyboardCallsComeOnAThreadOtherThanTheMainThread() {
    assertNotEquals(Looper.getMainLooper(), connection.handler!!.looper)
  }

  @Test
  fun questionAnsweredByTheMainThreadWhenItIsFree() {
    val answer = keyboard.submit(Callable { connection.getTextBeforeCursor(5, 0) })
    // The main thread runs the call while the keyboard waits.
    val deadline = SystemClock.uptimeMillis() + 5000
    while (!answer.isDone && SystemClock.uptimeMillis() < deadline) {
      shadowOf(Looper.getMainLooper()).idle()
      Thread.sleep(5)
    }

    assertEquals("Hello", answer.get(1, TimeUnit.SECONDS))
    assertEquals(setOf(Looper.getMainLooper().thread), threads)
  }

  @Test
  fun busyMainThreadLeavesAQuestionUnansweredRatherThanStallingTheKeyboard() {
    // The main thread doesn't run: it is waiting for the keyboard.
    val start = System.nanoTime()
    val answer = fromKeyboard { connection.getTextBeforeCursor(5, 0) }
    val waitedMs = (System.nanoTime() - start) / 1_000_000

    assertNull(answer)
    assertTrue("waited $waitedMs ms", waitedMs < 1000)
  }

  @Test
  fun changesMadeWhileTheMainThreadIsBusyStillHappenInOrder() {
    assertTrue(fromKeyboard { connection.commitText("a", 1) })
    assertTrue(fromKeyboard { connection.commitText("b", 1) })
    assertEquals(emptyList<String>(), calls)

    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(listOf("commitText(a)", "commitText(b)"), calls)
  }

  @Test
  fun changesDontWaitForTheMainThread() {
    val start = System.nanoTime()
    repeat(5) { fromKeyboard { connection.commitText("a", 1) } }
    val waitedMs = (System.nanoTime() - start) / 1_000_000

    assertTrue("waited $waitedMs ms", waitedMs < 100)
  }

  @Test
  fun whileTheMainThreadIsBusyLaterQuestionsAreUnansweredAtOnce() {
    assertNull(fromKeyboard { connection.getTextBeforeCursor(5, 0) })

    val start = System.nanoTime()
    repeat(5) { assertNull(fromKeyboard { connection.getTextBeforeCursor(5, 0) }) }
    val waitedMs = (System.nanoTime() - start) / 1_000_000

    assertTrue("waited $waitedMs ms", waitedMs < 100)
    // Only the first question was passed on.
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(listOf("getTextBeforeCursor(5)"), calls)
  }

  @Test
  fun questionsAreAnsweredAgainOnceTheMainThreadIsFree() {
    assertNull(fromKeyboard { connection.getTextBeforeCursor(5, 0) })
    shadowOf(Looper.getMainLooper()).idle()

    questionAnsweredByTheMainThreadWhenItIsFree()
  }

  @Test
  fun aQuestionAfterAChangeSeesTheChange() {
    fromKeyboard { connection.commitText("a", 1) }
    val answer = keyboard.submit(Callable { connection.getTextBeforeCursor(5, 0) })
    val deadline = SystemClock.uptimeMillis() + 5000
    while (!answer.isDone && SystemClock.uptimeMillis() < deadline) {
      shadowOf(Looper.getMainLooper()).idle()
      Thread.sleep(5)
    }

    assertEquals("Hello", answer.get(1, TimeUnit.SECONDS))
    assertEquals(listOf("commitText(a)", "getTextBeforeCursor(5)"), calls)
  }

  @Test
  fun callsFromTheMainThreadRunAtOnce() {
    assertEquals("Hello", connection.getTextBeforeCursor(5, 0))
    assertEquals(listOf("getTextBeforeCursor(5)"), calls)
  }
}
