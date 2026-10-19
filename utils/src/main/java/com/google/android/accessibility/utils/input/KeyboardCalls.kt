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

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.inputmethod.InputConnection
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Answers a keyboard's calls to one of the screen reader's own text fields on a thread of its own.
 *
 * The keyboard waits for each answer, and the screen reader's main thread waits for the keyboard
 * when it asks about its keys. With the text field answering on that main thread, each waited for
 * the other until the keyboard gave up, two seconds later, so typing stalled and keys were lost.
 * Each call still runs on the main thread, in order, but the keyboard waits for it at most
 * [MAX_WAIT_MS]. A call that takes longer still runs: a change to the text is made, and a question
 * about the text goes unanswered.
 */
object KeyboardCalls {
  private const val TAG = "KeyboardCalls"
  private const val MAX_WAIT_MS = 150L

  private val handler: Handler by lazy {
    Handler(HandlerThread("BacktalkKeyboardCalls").apply { start() }.looper)
  }
  private val main = Handler(Looper.getMainLooper())

  /** Returns [connection] answering the keyboard on a thread of its own. */
  @JvmStatic
  fun wrap(connection: InputConnection?): InputConnection? {
    if (connection == null) {
      return null
    }
    return Proxy.newProxyInstance(
      InputConnection::class.java.classLoader,
      arrayOf(InputConnection::class.java),
    ) { _, method, args ->
      if (method.name == "getHandler" && method.parameterCount == 0) {
        handler
      } else {
        call(connection, method, args ?: emptyArray())
      }
    } as InputConnection
  }

  private fun call(connection: InputConnection, method: Method, args: Array<Any?>): Any? {
    if (Looper.myLooper() == Looper.getMainLooper() || method.declaringClass == Any::class.java) {
      return invoke(connection, method, args)
    }
    val task = FutureTask { invoke(connection, method, args) }
    main.post(task)
    return try {
      task.get(MAX_WAIT_MS, TimeUnit.MILLISECONDS)
    } catch (e: TimeoutException) {
      Log.w(TAG, "${method.name} waits for the main thread; answering without it")
      unanswered(method)
    } catch (e: ExecutionException) {
      Log.w(TAG, "${method.name} failed", e.cause)
      unanswered(method)
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
      unanswered(method)
    }
  }

  private fun invoke(connection: InputConnection, method: Method, args: Array<Any?>): Any? =
    try {
      method.invoke(connection, *args)
    } catch (e: InvocationTargetException) {
      throw e.cause ?: e
    }

  /**
   * The answer to a call that didn't finish in time. A change still happens, so it succeeded, and a
   * question has no answer.
   */
  private fun unanswered(method: Method): Any? =
    when (method.returnType) {
      java.lang.Boolean.TYPE -> true
      Integer.TYPE -> 0
      else -> null
    }
}
