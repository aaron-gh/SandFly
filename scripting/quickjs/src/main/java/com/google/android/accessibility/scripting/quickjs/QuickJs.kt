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

package com.google.android.accessibility.scripting.quickjs

import java.io.Closeable

class QuickJs(host: Host, memoryLimitBytes: Long, stackLimitBytes: Long) : Closeable {

  interface Host {
    fun call(method: String, json: String?): String?

    fun loadModule(name: String): ByteArray?

    fun onUnhandledError(error: QuickJsException)
  }

  private var ptr: Long = nativeCreate(host, memoryLimitBytes, stackLimitBytes)

  init {
    if (ptr == 0L) {
      throw QuickJsException("Could not start the script engine")
    }
  }

  val isClosed: Boolean
    get() = ptr == 0L

  @Throws(QuickJsException::class)
  fun evalScript(source: String, fileName: String, timeLimitMs: Long) {
    eval(source, fileName, false, timeLimitMs)
  }

  @Throws(QuickJsException::class)
  fun evalModule(source: String, name: String, timeLimitMs: Long): String? =
    eval(source, name, true, timeLimitMs)

  @Throws(QuickJsException::class)
  fun call(function: String, argument: String?, timeLimitMs: Long): String? =
    nativeCall(open(), function, argument, timeLimitMs)

  override fun close() {
    if (ptr != 0L) {
      nativeDestroy(ptr)
      ptr = 0L
    }
  }

  private fun eval(source: String, name: String, module: Boolean, timeLimitMs: Long): String? =
    nativeEval(open(), source.toByteArray(), name, module, timeLimitMs)

  private fun open(): Long =
    ptr.takeIf { it != 0L } ?: throw QuickJsException("The script was unloaded")

  private companion object {
    init {
      System.loadLibrary("backtalkquickjs")
    }

    @JvmStatic external fun nativeCreate(host: Host, memoryLimit: Long, stackSize: Long): Long

    @JvmStatic external fun nativeDestroy(ptr: Long)

    @JvmStatic
    external fun nativeEval(
      ptr: Long,
      source: ByteArray,
      fileName: String,
      isModule: Boolean,
      timeLimitMs: Long,
    ): String?

    @JvmStatic
    external fun nativeCall(
      ptr: Long,
      function: String,
      argument: String?,
      timeLimitMs: Long,
    ): String?
  }
}
