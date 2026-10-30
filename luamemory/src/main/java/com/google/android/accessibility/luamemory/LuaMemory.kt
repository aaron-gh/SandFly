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

package com.google.android.accessibility.luamemory

/**
 * A memory budget for a Lua state, enforced by its allocator: an allocation past the budget fails
 * and Lua raises "not enough memory" in the script, even inside one string operation.
 */
object LuaMemory {
  /** Whether the native library is there; unit tests on a computer run without it. */
  val isAvailable: Boolean =
    try {
      System.loadLibrary("sandfly_lua_memory")
      true
    } catch (e: UnsatisfiedLinkError) {
      false
    }

  /**
   * Puts a budget of [limitBytes] on the Lua state at [statePointer], from LuaJava's
   * getPointer(). Returns its handle, or 0 if it couldn't. The budget isn't enforced until
   * [setEnforced] turns it on.
   */
  fun attach(statePointer: Long, limitBytes: Long): Long =
    if (isAvailable) nativeAttach(statePointer, limitBytes) else 0

  /**
   * Turns enforcing the budget of [handle] on or off, and returns whether it was on. It must be off
   * whenever Java code changes the state: a refused allocation there would jump over Java frames.
   */
  fun setEnforced(handle: Long, enforced: Boolean): Boolean =
    handle != 0L && nativeSetEnforced(handle, enforced)

  /** Frees the budget of [handle], once its state is closed. */
  fun free(handle: Long) {
    if (handle != 0L) nativeFree(handle)
  }

  @JvmStatic private external fun nativeAttach(state: Long, limit: Long): Long

  @JvmStatic private external fun nativeSetEnforced(handle: Long, enforced: Boolean): Boolean

  @JvmStatic private external fun nativeFree(handle: Long)
}
