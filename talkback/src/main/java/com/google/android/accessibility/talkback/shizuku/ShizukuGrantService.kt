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

import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Shizuku's user service: it runs in a process of its own as the shell user, as adb would, and
 * runs only the commands in [ShizukuGrants.Grant], for a package name that it checks.
 */
class ShizukuGrantService : IShizukuGrants.Stub() {

  override fun destroy() {
    exitProcess(0)
  }

  override fun grant(grant: Int, packageName: String?): String? {
    val which = ShizukuGrants.Grant.entries.getOrNull(grant) ?: return "Unknown permission $grant"
    if (packageName == null || !PACKAGE_NAME.matches(packageName)) {
      return "Bad package name $packageName"
    }
    return try {
      val process =
        ProcessBuilder(which.shellCommand(packageName)).redirectErrorStream(true).start()
      if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        return "${which.name} timed out"
      }
      val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
      if (process.exitValue() == 0) null else output.ifEmpty { "Exit code ${process.exitValue()}" }
    } catch (e: Exception) {
      e.toString()
    }
  }

  private companion object {
    val PACKAGE_NAME = Regex("""[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+""")
    const val TIMEOUT_SECONDS = 10L
  }
}
