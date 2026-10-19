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
import android.os.SystemClock
import org.json.JSONObject

class ScriptSpeechHooks(
  private val handler: Handler,
  private val ready: () -> Boolean = { true },
) {
  @Volatile private var chains: Map<String, List<ScriptRuntime>> = emptyMap()

  fun update(runtimes: List<ScriptRuntime>) {
    chains =
      PERMISSIONS.mapValues { (hook, permission) ->
        runtimes.filter {
          hook in it.hooks &&
            (permission == null || it.allowed(permission, ScriptRuntime.handlerName(hook)))
        }
      }
  }

  fun clear() {
    chains = emptyMap()
  }

  fun hasListeners(hook: String): Boolean = ready() && chains[hook].orEmpty().isNotEmpty()

  fun rewrite(
    hook: String,
    input: String,
    accepts: (ScriptRuntime) -> Boolean = { true },
    arg: (ScriptRuntime) -> Any?,
  ): String? {
    if (!ready()) return null
    val chain = chains[hook].orEmpty().filter(accepts).ifEmpty { return null }
    if (ScriptThread.heldUpFor(WAIT_MS)) return null
    val deadline = SystemClock.uptimeMillis() + WAIT_MS
    // Speech cannot wait behind a backlog of content events and timers. It still cannot
    // preempt JavaScript already running, and the main-thread wait remains bounded.
    return handler.await(WAIT_MS, hook, atFrontOfQueue = true) {
      if (ready()) runChain(chain, hook, input, deadline, arg) else null
    }
  }

  private fun runChain(
    chain: List<ScriptRuntime>,
    hook: String,
    input: String,
    deadline: Long,
    arg: (ScriptRuntime) -> Any?,
  ): String? {
    val handlerName = ScriptRuntime.handlerName(hook)
    return rewriteSpeechChain(input, chain.filter { it.isLoaded }, deadline) { runtime, text, budget ->
      val data = jsonObject("arg" to arg(runtime), "text" to text)
      // Building a node snapshot also spends the budget; do not start JS after it expires.
      val remaining = minOf(budget, deadline - SystemClock.uptimeMillis())
      if (remaining <= 0) return@rewriteSpeechChain null
      val result = runtime.dispatch(hook, data, remaining)
      if (SystemClock.uptimeMillis() >= deadline) {
        runtime.warnOnce("slow $hook", "$handlerName exceeded the speech-hook deadline")
      }
      when {
        result == null -> null
        !runtime.allowed(ScriptPermission.SPEECH, "Changing speech from $handlerName") -> null
        else -> JSONObject(result).optString("text", text)
      }
    }
  }

  private companion object {
    const val WAIT_MS = 30L
    val PERMISSIONS: Map<String, ScriptPermission?> =
      mapOf(
        // The focus hook is given the item and what Backtalk will say for it, in every app.
        "focus" to ScriptPermission.SCREEN,
        "speech" to ScriptPermission.SPEECH,
        "notification" to ScriptPermission.NOTIFICATIONS,
        "announcement" to ScriptPermission.SCREEN,
      )
  }
}
