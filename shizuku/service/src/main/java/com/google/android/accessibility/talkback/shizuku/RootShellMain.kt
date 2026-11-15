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

package com.google.android.accessibility.talkback.shizuku

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.os.UserHandle
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.system.exitProcess

/**
 * The process that [RootShell] starts through su with app_process: it runs a [ShizukuGrantService]
 * and sends its binder to the app that started it, in a broadcast with the token that the app wrote
 * to its standard input. It exits when its standard input closes, which happens when the app
 * unbinds or stops.
 *
 * Arguments: the app's package name, its user ID and the broadcast's action.
 */
object RootShellMain {
  private const val TAG = "SandFlyRootShell"

  @JvmStatic
  fun main(args: Array<String>) {
    if (args.size < 3) {
      Log.e(TAG, "Missing arguments")
      exitProcess(1)
    }
    val packageName = args[0]
    val userId = args[1].toIntOrNull() ?: exitProcess(1)
    val action = args[2]
    val token = readLine(System.`in`)
    if (token.isNullOrEmpty()) {
      Log.e(TAG, "No token")
      exitProcess(1)
    }
    try {
      Looper.prepareMainLooper()
      val activityThread = Class.forName("android.app.ActivityThread")
      val thread = activityThread.getMethod("systemMain").invoke(null)
      val system = activityThread.getMethod("getSystemContext").invoke(thread) as Context
      val user = UserHandle.getUserHandleForUid(userId * PER_USER_RANGE)
      val context = packageContext(system, packageName, user)
      val service = ShizukuGrantService(context)
      Thread({
          drain(System.`in`)
          exitProcess(0)
        }, "stdin")
        .apply { isDaemon = true }
        .start()
      val intent =
        Intent(action)
          .setPackage(packageName)
          .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
          .putExtra(RootShell.EXTRA_TOKEN, token)
          .putExtra(RootShell.EXTRA_SERVICE, Bundle().apply { putBinder(RootShell.EXTRA_SERVICE, service) })
      // The shell user and root may send to any user.
      @SuppressLint("MissingPermission") system.sendBroadcastAsUser(intent, user)
      Log.i(TAG, "Started for $packageName as uid ${android.os.Process.myUid()}")
      Looper.loop()
    } catch (e: Throwable) {
      Log.e(TAG, "Cannot start", e)
      System.err.println(e)
      exitProcess(1)
    }
  }

  /** A context of [packageName] for [user], or of the user that runs this process. */
  private fun packageContext(system: Context, packageName: String, user: UserHandle): Context =
    try {
      system.javaClass
        .getMethod(
          "createPackageContextAsUser",
          String::class.java,
          Int::class.javaPrimitiveType,
          UserHandle::class.java,
        )
        .invoke(system, packageName, Context.CONTEXT_IGNORE_SECURITY, user) as Context
    } catch (e: ReflectiveOperationException) {
      system.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
    }

  /** Reads one line, a byte at a time, so that nothing after it is read. */
  private fun readLine(input: InputStream): String? {
    val bytes = ByteArrayOutputStream()
    while (bytes.size() < MAX_LINE) {
      val byte = input.read()
      if (byte < 0) return null
      if (byte == '\n'.code) return bytes.toString(Charsets.UTF_8.name()).trim()
      bytes.write(byte)
    }
    return null
  }

  private fun drain(input: InputStream) {
    val buffer = ByteArray(64)
    try {
      while (input.read(buffer) >= 0) {
        // Nothing more is written; this only waits for the end.
      }
    } catch (e: Exception) {
      // Closed.
    }
  }

  private const val PER_USER_RANGE = 100_000
  private const val MAX_LINE = 256
}
