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

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import party.iroiro.luajava.LuaException

/**
 * Makes a script's Java proxy safe to call after the script has unloaded. LuaJava throws then, and
 * on the main thread that would stop SandFly. The prelude calls this for every `java.proxy`.
 */
object ScriptProxyGuard {
  private const val CLOSED = "lua state closed"

  @JvmStatic
  fun guarded(target: Any): Any {
    return Proxy.newProxyInstance(
      target.javaClass.classLoader,
      target.javaClass.interfaces,
      Guard(target, Proxy.getInvocationHandler(target)),
    )
  }

  /** Returns the proxy that [value] guards, for `java.unwrap`, or [value] if it guards none. */
  @JvmStatic
  fun target(value: Any): Any =
    (if (Proxy.isProxyClass(value.javaClass)) Proxy.getInvocationHandler(value) as? Guard else null)
      ?.target ?: value

  private class Guard(val target: Any, private val handler: InvocationHandler) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? =
      try {
        handler.invoke(target, method, args)
      } catch (e: LuaException) {
        if (e.message?.contains(CLOSED) == true) emptyResult(method) else throw e
      }
  }

  private fun emptyResult(method: Method): Any? =
    when (method.returnType) {
      java.lang.Boolean.TYPE -> false
      java.lang.Byte.TYPE -> 0.toByte()
      java.lang.Short.TYPE -> 0.toShort()
      java.lang.Integer.TYPE -> 0
      java.lang.Long.TYPE -> 0L
      java.lang.Float.TYPE -> 0f
      java.lang.Double.TYPE -> 0.0
      java.lang.Character.TYPE -> 0.toChar()
      else -> null
    }
}
