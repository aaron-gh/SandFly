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

import android.os.Handler
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private const val TAG = "ScriptWait"

fun <T> Handler.await(
  waitMs: Long,
  what: String,
  atFrontOfQueue: Boolean = false,
  task: () -> T,
): T? {
  if (looper.isCurrentThread) return null
  val future = FutureTask<T> { task() }
  val posted = if (atFrontOfQueue) postAtFrontOfQueue(future) else post(future)
  if (!posted) return null
  return try {
    future.get(waitMs, TimeUnit.MILLISECONDS)
  } catch (e: TimeoutException) {
    LogUtils.w(TAG, "Scripts took longer than %d ms for %s", waitMs, what)
    null
  } catch (e: ExecutionException) {
    LogUtils.e(TAG, "Scripts failed for %s: %s", what, e.cause)
    null
  } catch (e: InterruptedException) {
    Thread.currentThread().interrupt()
    null
  } finally {
    // A request whose caller stopped waiting must not remain ahead of later speech hooks.
    future.cancel(false)
    removeCallbacks(future)
  }
}
