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

import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.ParcelFileDescriptor
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlin.system.exitProcess

/**
 * Shizuku's user service: it runs in a process of its own as the shell user, as adb would, and
 * runs only the commands in [ShizukuGrants.Grant], for a package name that it checks, and in
 * [ShizukuCommand], with an argument that it checks.
 *
 * Shizuku gives it a [Context] of SandFly's package, from which it makes one of the shell's, for
 * the calls to Android that have no shell command. Android's own shell commands use hidden APIs
 * in the same kind of process, so they are open to it too.
 */
class ShizukuGrantService(private val context: Context?) : IShizukuGrants.Stub() {

  /** For Shizuku versions that don't give user services a context. */
  constructor() : this(null)

  override fun destroy() {
    exitProcess(0)
  }

  override fun grant(grant: Int, packageName: String?): String? {
    val which = ShizukuGrants.Grant.entries.getOrNull(grant) ?: return "Unknown permission $grant"
    if (packageName == null || !PACKAGE_NAME.matches(packageName)) {
      return "Bad package name $packageName"
    }
    return exec(which.name, which.shellCommand(packageName))
  }

  override fun run(command: Int, argument: String?): String? {
    val which = ShizukuCommand.entries.getOrNull(command) ?: return "Unknown command $command"
    if (which == ShizukuCommand.HOTSPOT) {
      return when (argument) {
        ShizukuCommand.ON -> setHotspot(true)
        ShizukuCommand.OFF -> setHotspot(false)
        else -> "Bad argument $argument for ${which.name}"
      }
    }
    val shellCommand =
      argument?.let { which.shellCommand(it) } ?: return "Bad argument $argument for ${which.name}"
    return exec(which.name, shellCommand)
  }

  override fun state(command: Int): Int =
    when (ShizukuCommand.entries.getOrNull(command)) {
      ShizukuCommand.HOTSPOT ->
        try {
          val wifi = shellContext()?.getSystemService(WifiManager::class.java)
          when (wifi?.javaClass?.getMethod("isWifiApEnabled")?.invoke(wifi)) {
            true -> 1
            false -> 0
            else -> -1
          }
        } catch (e: Exception) {
          -1
        }
      else -> -1
    }

  /** A context of the shell's package, whose user ID this process has. */
  private fun shellContext(): Context? = context?.createPackageContext(SHELL_PACKAGE, 0)

  private var tethering: Any? = null

  /**
   * A TetheringManager that calls as the shell. Android's own would name SandFly's package, which
   * the tethering service refuses from the shell's user ID, so this one gets a context that names
   * the shell's package.
   */
  private fun tetheringManager(): Any? {
    tethering?.let { return it }
    val base = context ?: return null
    val shell =
      object : ContextWrapper(base) {
        override fun getPackageName(): String = SHELL_PACKAGE

        override fun getOpPackageName(): String = SHELL_PACKAGE
      }
    val binder =
      Class.forName("android.os.ServiceManager")
        .getMethod("getService", String::class.java)
        .invoke(null, TETHERING_SERVICE) as? IBinder ?: return null
    return Class.forName("android.net.TetheringManager")
      .getDeclaredConstructor(Context::class.java, Supplier::class.java)
      .apply { isAccessible = true }
      .newInstance(shell, Supplier { binder })
      .also { tethering = it }
  }

  /**
   * Starts or stops Wi-Fi tethering through TetheringManager, as the system's Hotspot tile does.
   * Returns null if it worked, or what went wrong.
   */
  private fun setHotspot(on: Boolean): String? =
    try {
      val tethering = tetheringManager() ?: return "No tethering service"
      val manager = tethering.javaClass
      if (!on) {
        manager.getMethod("stopTethering", Int::class.javaPrimitiveType).invoke(tethering, TETHERING_WIFI)
        null
      } else {
        val callbackClass = Class.forName("android.net.TetheringManager\$StartTetheringCallback")
        val done = CountDownLatch(1)
        var error: String? = null
        val callback =
          Proxy.newProxyInstance(callbackClass.classLoader, arrayOf(callbackClass)) { proxy, method, args ->
            when (method.name) {
              "onTetheringStarted" -> done.countDown()
              "onTetheringFailed" -> {
                error = "Tethering failed with error ${args?.getOrNull(0)}"
                done.countDown()
              }
              "hashCode" -> return@newProxyInstance System.identityHashCode(proxy)
              "equals" -> return@newProxyInstance proxy === args?.getOrNull(0)
              "toString" -> return@newProxyInstance "StartTetheringCallback"
            }
            null
          }
        manager
          .getMethod("startTethering", Int::class.javaPrimitiveType, Executor::class.java, callbackClass)
          .invoke(tethering, TETHERING_WIFI, Executor { it.run() }, callback)
        if (done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) error else "Hotspot timed out"
      }
    } catch (e: InvocationTargetException) {
      e.targetException.toString()
    } catch (e: Exception) {
      e.toString()
    }

  private var clipboard: ClipboardManager? = null
  private var clipListener: ClipboardManager.OnPrimaryClipChangedListener? = null
  @Volatile private var clipWatcher: IClipboardWatcher? = null

  override fun watchClipboard(watcher: IClipboardWatcher?) {
    synchronized(this) {
      clipWatcher = watcher
      val manager = shellClipboard() ?: return
      val listener = clipListener
      if (watcher != null && listener == null) {
        val added = ClipboardManager.OnPrimaryClipChangedListener { onClipChanged() }
        manager.addPrimaryClipChangedListener(added)
        clipListener = added
      } else if (watcher == null && listener != null) {
        manager.removePrimaryClipChangedListener(listener)
        clipListener = null
      }
    }
  }

  /** Tells the watcher the new clip's text. The shell may read the clipboard in the background. */
  private fun onClipChanged() {
    val watcher = clipWatcher ?: return
    val clip =
      try {
        clipboard?.primaryClip
      } catch (e: RuntimeException) {
        null
      } ?: return
    if (clip.description?.extras?.getBoolean(EXTRA_IS_SENSITIVE) == true || clip.itemCount == 0) {
      return
    }
    val text = clip.getItemAt(0).text?.toString()?.take(MAX_CLIP_LENGTH)
    if (text.isNullOrBlank()) return
    try {
      watcher.onClipText(text)
    } catch (e: Exception) {
      // SandFly has stopped.
      watchClipboard(null)
    }
  }

  /**
   * A ClipboardManager that calls as the shell, which may read the clipboard in the background.
   * Android's own would name SandFly's package, which the clipboard service refuses from the
   * shell's user ID. Its listeners run on a thread of their own.
   */
  private fun shellClipboard(): ClipboardManager? {
    clipboard?.let { return it }
    val base = context ?: return null
    val shell =
      object : ContextWrapper(base) {
        override fun getPackageName(): String = SHELL_PACKAGE
        override fun getOpPackageName(): String = SHELL_PACKAGE
      }
    return try {
      val thread = HandlerThread("clipboard").apply { start() }
      ClipboardManager::class.java
        .getDeclaredConstructor(Context::class.java, Handler::class.java)
        .apply { isAccessible = true }
        .newInstance(shell, Handler(thread.looper))
        .also { clipboard = it }
    } catch (e: Exception) {
      null
    }
  }

  override fun install(
    apks: Array<ParcelFileDescriptor>?,
    sizes: LongArray?,
    packageName: String?,
    installer: String?,
  ): String? {
    if (apks == null || sizes == null || apks.size != sizes.size || apks.isEmpty()) {
      return "No APKs to install"
    }
    if (packageName == null || !PACKAGE_NAME.matches(packageName)) {
      return "Bad package name $packageName"
    }
    if (installer == null || !PACKAGE_NAME.matches(installer)) {
      return "Bad installer $installer"
    }
    // No -p (inherit from the installed package): the caller gives the complete APKs, and
    // inheriting fails a version upgrade because the inherited files are the old version's.
    // Only for SandFly's own user: pm installs for every user by default, which would add SandFly
    // to a work profile, private space and other users.
    val user = (Binder.getCallingUid() / PER_USER_RANGE).toString()
    val create = shell(listOf("pm", "install-create", "-r", "--user", user, "-i", installer), null)
    val session =
      SESSION_ID.find(create.second)?.groupValues?.get(1)
        ?: return "No install session: ${create.second}"
    try {
      apks.forEachIndexed { index, apk ->
        ParcelFileDescriptor.AutoCloseInputStream(apk).use { input ->
          val write =
            shell(
              listOf("pm", "install-write", "-S", sizes[index].toString(), session, "apk$index", "-"),
              input,
            )
          if (write.first != 0) {
            shell(listOf("pm", "install-abandon", session), null)
            return "Writing apk$index failed: ${write.second}"
          }
        }
      }
      val commit = shell(listOf("pm", "install-commit", session), null)
      return if (commit.first == 0) null else commit.second.ifEmpty { "Install failed" }
    } catch (e: Exception) {
      shell(listOf("pm", "install-abandon", session), null)
      return e.toString()
    }
  }

  private fun exec(name: String, command: List<String>): String? {
    val result = shell(command, null)
    return if (result.first == 0) null else result.second.ifEmpty { "$name failed" }
  }

  /** Runs a shell command, feeding [stdin] to it if given, and returns its exit code and output. */
  private fun shell(command: List<String>, stdin: InputStream?): Pair<Int, String> =
    try {
      val process = ProcessBuilder(command).redirectErrorStream(true).start()
      stdin?.use { it.copyTo(process.outputStream) }
      process.outputStream.close()
      if (!process.waitFor(INSTALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        -1 to "Timed out"
      } else {
        process.exitValue() to process.inputStream.bufferedReader().use { it.readText() }.trim()
      }
    } catch (e: Exception) {
      -1 to e.toString()
    }

  private companion object {
    val PACKAGE_NAME = Regex("""[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+""")
    val SESSION_ID = Regex("""\[(\d+)\]""")
    const val TIMEOUT_SECONDS = 10L
    /** UIDs of one user, as in UserHandle.PER_USER_RANGE. */
    const val PER_USER_RANGE = 100_000
    /** Installing is slower than a setting change, so it gets longer than the other commands. */
    const val INSTALL_TIMEOUT_SECONDS = 120L
    const val SHELL_PACKAGE = "com.android.shell"
    /** ClipDescription.EXTRA_IS_SENSITIVE, from Android 13, set on clips such as passwords. */
    const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"
    /** The clipboard history keeps no more of a text than this. */
    const val MAX_CLIP_LENGTH = 10_000
    /** Context.TETHERING_SERVICE, which isn't public. */
    const val TETHERING_SERVICE = "tethering"
    /** TetheringManager.TETHERING_WIFI. */
    const val TETHERING_WIFI = 0
  }
}
