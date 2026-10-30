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

import com.google.android.accessibility.luamemory.LuaMemory
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.Closeable
import java.nio.ByteBuffer
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.lua54.Lua54

/**
 * One script's Lua 5.4 state, through LuaJava.
 *
 * Only strings and booleans cross between Java and Lua: the host's calls take and give JSON text.
 * Text crosses as UTF-8 bytes, since LuaJava's own string conversion breaks characters outside
 * the Basic Multilingual Plane, such as emoji.
 * No Java object is ever handed to the script, so a script can't reach Java through one. The
 * prelude runs the script in a table of its own, without `java`, `debug`, `io`, `package` and
 * `os.execute`, and only a script with the java permission gets LuaJava's `java` module.
 *
 * Lua can't be stopped from Java, so the prelude sets a count hook that asks [Host.isOvertime]
 * and checks the memory in use, and raises an error that the script can't catch. A call into
 * the script therefore ends soon after its time is up, except while it is inside one long C
 * function, such as a pattern match on a long text.
 *
 * The memory limit is also kept by Lua's allocator ([LuaMemory]), which refuses to grow past it
 * even inside one string operation, while Lua code runs. A script with the java permission has
 * only the hook's check, since LuaJava's `java` module changes the state from Java.
 */
class LuaEngine(private val host: Host, private val withJava: Boolean) : Closeable {

  interface Host {
    /** A call from the script. Returns JSON, or null for nothing. Throws [ScriptError]. */
    fun call(method: String, json: String?): String?

    /** A Lua file in the script's package, or null if there is none. */
    fun loadModule(name: String): ByteArray?

    /** Whether the script's current call has run out of time. */
    fun isOvertime(): Boolean
  }

  /** An error the script raised, or that stopped it. */
  class ScriptError(message: String, val interrupted: Boolean = false) : Exception(message)

  private var lua: Lua? = Lua54()

  /** The allocator's budget for the state, or 0 when there is none. */
  private var memory = 0L

  val isClosed: Boolean
    get() = lua == null

  /** Runs the prelude, which sets up the sandbox and the `sf` API. */
  @Throws(ScriptError::class)
  fun start(prelude: String, memoryLimitBytes: Long) {
    val state = open()
    synchronized(state.mainState) {
      if (!withJava) {
        memory = LuaMemory.attach(state.pointer, memoryLimitBytes)
        if (memory == 0L && LuaMemory.isAvailable) {
          LogUtils.w(TAG, "No allocator budget; only the hook checks the memory limit")
        }
      }
      for (library in LIBRARIES) {
        state.openLibrary(library)
      }
      if (!withJava) {
        // LuaJava sets up `java` in a new state. The prelude hides it from the script anyway.
        state.pushNil()
        state.setGlobal("java")
      }
      run(state, "prelude.lua") {
        loadText(state, prelude, "=prelude")
        state.push(callFunction)
        state.push(moduleFunction)
        state.push(overtimeFunction)
        state.push(withJava)
        state.push(memoryLimitBytes / 1024)
        enforcingMemory { state.pCall(5, 0) }
      }
    }
  }

  /** Runs the script's main file. */
  @Throws(ScriptError::class)
  fun runMain(source: String, name: String) {
    callGlobal("__sf_main", name, source, name)
  }

  /** Calls the prelude's dispatcher with an event as JSON, and returns its answer, if any. */
  @Throws(ScriptError::class)
  fun dispatch(json: String): String? = callGlobal("__sf_dispatch", "dispatch", json)

  override fun close() {
    val state = lua ?: return
    lua = null
    synchronized(state.mainState) {
      state.close()
      LuaMemory.free(memory)
      memory = 0
    }
  }

  /**
   * Runs Lua code with the memory budget enforced. Java must change the state only while it isn't:
   * a refused allocation raises a Lua error, which would jump over Java frames.
   */
  private inline fun <T> enforcingMemory(block: () -> T): T {
    val was = LuaMemory.setEnforced(memory, true)
    try {
      return block()
    } finally {
      LuaMemory.setEnforced(memory, was)
    }
  }

  /** Runs a Java function that Lua called, with the budget not enforced while it pushes values. */
  private inline fun <T> inJava(block: () -> T): T {
    val was = LuaMemory.setEnforced(memory, false)
    try {
      return block()
    } finally {
      LuaMemory.setEnforced(memory, was)
    }
  }

  private fun callGlobal(function: String, where: String, vararg args: String): String? {
    val state = open()
    synchronized(state.mainState) {
      return run(state, where) {
        val top = state.top
        state.getGlobal(function)
        for (arg in args) {
          pushText(state, arg)
        }
        enforcingMemory { state.pCall(args.size, 1) }
        val result = if (state.isString(-1)) readText(state, -1) else null
        state.top = top
        result
      }
    }
  }

  private inline fun <T> run(state: Lua, where: String, block: () -> T): T {
    val top = state.top
    try {
      return block()
    } catch (e: LuaException) {
      state.top = top
      val message = e.message.orEmpty()
      throw ScriptError(message.ifEmpty { "$where failed" }, interrupted = isInterruption(message))
    }
  }

  private fun loadText(state: Lua, source: String, name: String) {
    state.load(utf8(source), name)
  }

  private fun utf8(text: String): ByteBuffer {
    val bytes = text.toByteArray()
    return ByteBuffer.allocateDirect(bytes.size).put(bytes).also { it.flip() }
  }

  private fun pushText(state: Lua, text: String) {
    state.push(utf8(text))
  }

  private fun readText(state: Lua, index: Int): String? {
    val buffer = state.toBuffer(index) ?: return null
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes.decodeToString()
  }

  private fun open(): Lua = lua ?: throw ScriptError("The script was unloaded")

  // sf.call(method, json) gives true and the answer, or false and an error message.
  private val callFunction = JFunction { state -> inJava { call(state) } }

  private fun call(state: Lua): Int {
    val method = readText(state, 1).orEmpty()
    val json = if (state.isString(2)) readText(state, 2) else null
    try {
      val result = host.call(method, json)
      state.push(true)
      if (result == null) state.pushNil() else pushText(state, result)
    } catch (e: ScriptError) {
      state.push(false)
      pushText(state, e.message.orEmpty())
    } catch (e: RuntimeException) {
      state.push(false)
      pushText(state, e.message ?: e.toString())
    }
    return 2
  }

  // Gives a Lua file's text from the script's package, or nothing.
  private val moduleFunction = JFunction { state ->
    inJava {
      val name = readText(state, 1).orEmpty()
      val bytes = host.loadModule(name)
      if (bytes == null) {
        state.pushNil()
      } else {
        state.push(ByteBuffer.allocateDirect(bytes.size).put(bytes).also { it.flip() })
      }
      1
    }
  }

  private val overtimeFunction = JFunction { state ->
    inJava {
      state.push(host.isOvertime())
      1
    }
  }

  companion object {
    private const val TAG = "LuaEngine"

    /** Raised by the prelude's hook. The script can't catch either. */
    const val OUT_OF_TIME = "The script ran out of time"
    const val OUT_OF_MEMORY = "The script used too much memory"

    private val LIBRARIES = listOf("base", "coroutine", "table", "string", "math", "utf8", "os", "debug")

    /** Throws a [LinkageError] if Lua's native library can't be loaded. */
    fun checkAvailable() {
      Lua54().close()
    }

    /** Lua's own message when the allocator refuses to go past the memory limit. */
    private const val NOT_ENOUGH_MEMORY = "not enough memory"

    fun isInterruption(message: String): Boolean =
      OUT_OF_TIME in message || OUT_OF_MEMORY in message || NOT_ENOUGH_MEMORY in message
  }
}
