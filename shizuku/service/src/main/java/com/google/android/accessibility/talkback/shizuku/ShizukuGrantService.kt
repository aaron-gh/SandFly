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
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.net.wifi.WifiManager
import android.app.ActivityManager
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.google.android.accessibility.talkback.audio.AccessibilityAudioOutput
import java.io.File
import java.io.IOException
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
 * runs only the commands in [ShellGrant], for a package name that it checks, and in
 * [ShizukuCommand], with an argument that it checks.
 *
 * Shizuku gives it a [Context] of the package that started it, SandFly or the SandFly add-on,
 * from which it makes one of the shell's, for the calls to Android that have no shell command.
 * Android's own shell commands use hidden APIs in the same kind of process, so they are open to it
 * too.
 */
class ShizukuGrantService(private val context: Context?) : IShizukuGrants.Stub() {

  /** For Shizuku versions that don't give user services a context. */
  constructor() : this(null)

  init {
    restoreVolumeKeys()
  }

  override fun destroy() {
    exitProcess(0)
  }

  override fun grant(grant: Int, packageName: String?): String? {
    val which = ShellGrant.entries.getOrNull(grant) ?: return "Unknown permission $grant"
    if (packageName == null || !PACKAGE_NAME.matches(packageName)) {
      return "Bad package name $packageName"
    }
    return exec(which.name, which.shellCommand(packageName))
  }

  override fun run(command: Int, argument: String?): String? {
    val which = ShizukuCommand.entries.getOrNull(command) ?: return "Unknown command $command"
    if (which == ShizukuCommand.ACCESSIBILITY_SHORTCUT) {
      return if (argument == null) "No accessibility service" else performAccessibilityShortcut(argument)
    }
    if (which == ShizukuCommand.RESET_ACCESSIBILITY_AUDIO) {
      return AccessibilityAudioOutput.reset()
    }
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

  /** IActivityTaskManager, which runs the system's tasks: hidden, so called by reflection. */
  private var taskManagerCache: Any? = null

  private fun taskManager(): Any {
    taskManagerCache?.let { return it }
    val binder =
      Class.forName("android.os.ServiceManager")
        .getMethod("getService", String::class.java)
        .invoke(null, ACTIVITY_TASK_SERVICE) as? IBinder ?: error("No activity task service")
    return Class.forName("android.app.IActivityTaskManager\$Stub")
      .getMethod("asInterface", IBinder::class.java)
      .invoke(null, binder)
      .also { taskManagerCache = it }
  }

  override fun recentTasks(max: Int): Bundle? =
    try {
      val manager = taskManager()
      // The tasks of the caller's user and its profiles, as the launcher's overview shows them.
      val userId = Binder.getCallingUid() / PER_USER_RANGE
      val slice =
        manager.javaClass
          .getMethod(
            "getRecentTasks",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
          )
          .invoke(manager, max.coerceIn(1, MAX_TASKS), RECENT_IGNORE_UNAVAILABLE, userId)
      val tasks =
        (slice?.javaClass?.getMethod("getList")?.invoke(slice) as? List<*>).orEmpty()
          .filterIsInstance<ActivityManager.RecentTaskInfo>()
      val packageManager = shellContext()?.packageManager
      val labels = HashMap<String, String>()
      fun appLabel(packageName: String): String =
        labels.getOrPut(packageName) {
          try {
            packageManager?.let { pm ->
              pm.getApplicationInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
                .loadLabel(pm)
                .toString()
            } ?: packageName
          } catch (e: Exception) {
            packageName
          }
        }
      val ids = IntArray(tasks.size)
      val users = IntArray(tasks.size)
      val packages = arrayOfNulls<String>(tasks.size)
      val bases = arrayOfNulls<String>(tasks.size)
      val tops = arrayOfNulls<String>(tasks.size)
      val appLabels = arrayOfNulls<String>(tasks.size)
      val titles = arrayOfNulls<String>(tasks.size)
      val activityLabels = arrayOfNulls<String>(tasks.size)
      tasks.forEachIndexed { index, task ->
        val base = task.baseActivity ?: task.baseIntent?.component ?: task.origActivity
        val packageName = base?.packageName ?: task.baseIntent?.`package` ?: ""
        ids[index] =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) task.taskId
          else @Suppress("DEPRECATION") task.persistentId
        users[index] = hiddenInt(task, "userId", userId)
        packages[index] = packageName
        bases[index] = base?.flattenToString() ?: ""
        tops[index] = task.topActivity?.flattenToString() ?: ""
        appLabels[index] = if (packageName.isEmpty()) "" else appLabel(packageName)
        titles[index] = task.taskDescription?.label?.take(MAX_TITLE_LENGTH) ?: ""
        activityLabels[index] =
          (task.topActivity ?: base)?.let { activity ->
            try {
              packageManager?.let { pm -> pm.getActivityInfo(activity, 0).loadLabel(pm).toString() }
            } catch (e: Exception) {
              null
            }
          } ?: ""
      }
      Bundle().apply {
        putIntArray(RecentTaskKeys.IDS, ids)
        putIntArray(RecentTaskKeys.USERS, users)
        putStringArray(RecentTaskKeys.PACKAGES, packages)
        putStringArray(RecentTaskKeys.BASE_ACTIVITIES, bases)
        putStringArray(RecentTaskKeys.TOP_ACTIVITIES, tops)
        putStringArray(RecentTaskKeys.APP_LABELS, appLabels)
        putStringArray(RecentTaskKeys.TITLES, titles)
        putStringArray(RecentTaskKeys.ACTIVITY_LABELS, activityLabels)
      }
    } catch (e: Throwable) {
      null
    }

  override fun openTask(taskId: Int): String? =
    try {
      val manager = taskManager()
      val result =
        manager.javaClass
          .getMethod("startActivityFromRecents", Int::class.javaPrimitiveType, Bundle::class.java)
          .invoke(manager, taskId, null) as? Int
      // ActivityManager's START_ codes: errors are negative, and 100 and up mean nothing started,
      // such as START_ABORTED when a task without activities replays its first intent as its app
      // and Android blocks that as a background start.
      if (result != null && (result < 0 || result >= START_SWITCHES_CANCELED)) {
        "Start from recents failed with $result"
      } else {
        null
      }
    } catch (e: InvocationTargetException) {
      e.targetException.toString()
    } catch (e: Throwable) {
      e.toString()
    }

  override fun closeTask(taskId: Int): String? =
    try {
      val manager = taskManager()
      val removed =
        manager.javaClass
          .getMethod("removeTask", Int::class.javaPrimitiveType)
          .invoke(manager, taskId) as? Boolean
      if (removed == false) "No task $taskId" else null
    } catch (e: InvocationTargetException) {
      e.targetException.toString()
    } catch (e: Throwable) {
      e.toString()
    }

  /**
   * Has SystemUI's window manager shell put the task in split screen, in the bottom or right half,
   * beside the task in front, which takes the other half.
   */
  override fun splitTask(taskId: Int): String? =
    wmShellSplit("moveToSideStage", taskId.toString(), SPLIT_POSITION_BOTTOM_OR_RIGHT.toString())

  override fun exitSplit(taskId: Int): String? = wmShellSplit("exitSplitScreen", taskId.toString())

  private fun wmShellSplit(vararg arguments: String): String? {
    val result =
      shell(listOf("dumpsys", "activity", "service", SYSTEM_UI_SERVICE, "WMShell", "splitscreen") + arguments, null)
    // The shell prints its help when it doesn't know a command or its arguments.
    return when {
      result.first != 0 -> result.second.ifEmpty { "Split screen failed" }
      result.second.contains("Error", ignoreCase = true) ||
        result.second.contains("help", ignoreCase = true) -> result.second
      else -> null
    }
  }

  private fun hiddenInt(target: Any, field: String, default: Int): Int =
    try {
      target.javaClass.getField(field).getInt(target)
    } catch (e: Exception) {
      default
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
    // The description says what was copied without reading it, which would show Android's
    // "pasted from your clipboard" message: passwords and pictures aren't read at all.
    val description =
      try {
        clipboard?.primaryClipDescription
      } catch (e: RuntimeException) {
        null
      } ?: return
    if (
      description.extras?.getBoolean(EXTRA_IS_SENSITIVE) == true ||
        !description.hasMimeType("text/*")
    ) {
      return
    }
    val clip =
      try {
        clipboard?.primaryClip
      } catch (e: RuntimeException) {
        null
      } ?: return
    // Read, so the watcher learns of it even without text, to leave Android's message unsaid.
    val text =
      clip.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()?.take(MAX_CLIP_LENGTH)
    try {
      watcher.onClipText(text?.takeUnless { it.isBlank() })
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

  /**
   * Runs the accessibility shortcut of the service in [argument], as Android's own shortcuts do: it
   * turns the service on or off, or opens one that asks for the accessibility button, first turning
   * it on if it's off.
   *
   * Turning a service on or off is a change to the enabled services. Opening one is only possible
   * through a shortcut that has the service, since Android tells only those services that their
   * button was pressed. One of the user's shortcuts that has it is used; otherwise the service is
   * put in the volume keys shortcut for the moment it takes, and the shortcut set back as it was.
   */
  private fun performAccessibilityShortcut(argument: String): String? {
    restoreVolumeKeys()
    val button = argument.endsWith(ShizukuCommand.BUTTON)
    val target = argument.removeSuffix(ShizukuCommand.BUTTON)
    val component = ComponentName.unflattenFromString(target)
    if (!ShizukuCommand.isComponent(target) || component == null) {
      return "Bad accessibility service $target"
    }
    if (!button) {
      return setServiceEnabled(component, !enabledServices().contains(component))
    }
    val manager =
      try {
        val binder =
          Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, "accessibility")
        Class.forName("android.view.accessibility.IAccessibilityManager\$Stub")
          .getMethod("asInterface", IBinder::class.java)
          .invoke(null, binder)
      } catch (e: ReflectiveOperationException) {
        return e.toString()
      } ?: return "No accessibility manager"
    val userId = android.os.Process.myUid() / PER_USER_RANGE
    val methods = manager.javaClass.methods
    // Android 15 and later name the display and the kind of shortcut.
    val performNew = methods.firstOrNull { it.name == "performAccessibilityShortcut" && it.parameterCount == 3 }
    val performOld = methods.firstOrNull { it.name == "performAccessibilityShortcut" && it.parameterCount == 1 }
    if (performNew == null && performOld == null) return "No accessibility shortcut call"
    // Before Android 15, a shortcut can be run only as the volume keys shortcut.
    val kinds =
      if (performNew != null) listOf(HARDWARE_SHORTCUT, SOFTWARE_SHORTCUT, GESTURE_SHORTCUT)
      else listOf(OLD_KEY_SHORTCUT)
    val volumeKeys = kinds.first()
    fun perform(kind: Int) {
      if (performNew != null) performNew.invoke(manager, 0, kind, target)
      else performOld?.invoke(manager, target)
    }
    fun hasTarget(kind: Int): Boolean =
      try {
        val get = methods.firstOrNull { it.name == "getAccessibilityShortcutTargets" }
        val targets =
          when (get?.parameterCount) {
            2 -> get.invoke(manager, kind, userId)
            1 -> get.invoke(manager, kind)
            else -> null
          } as? List<*>
        targets.orEmpty().any { ComponentName.unflattenFromString(it.toString()) == component }
      } catch (e: ReflectiveOperationException) {
        false
      }
    fun isConnected(): Boolean =
      try {
        val get = methods.firstOrNull { it.name == "getEnabledAccessibilityServiceList" && it.parameterCount == 2 }
        val result = get?.invoke(manager, ALL_FEEDBACK, userId)
        val list = result as? List<*> ?: result?.javaClass?.getMethod("getList")?.invoke(result) as? List<*>
        list.orEmpty().any {
          val id = (it as? android.accessibilityservice.AccessibilityServiceInfo)?.id
          id != null && ComponentName.unflattenFromString(id) == component
        }
      } catch (e: ReflectiveOperationException) {
        false
      }
    fun waitFor(condition: () -> Boolean): Boolean {
      repeat(WAIT_STEPS) {
        if (condition()) return true
        Thread.sleep(WAIT_STEP_MS)
      }
      return condition()
    }

    try {
      if (!isConnected()) {
        setServiceEnabled(component, true)?.let { return it }
        if (!waitFor { isConnected() }) return "$target didn't start"
      }
      kinds.firstOrNull { hasTarget(it) }?.let {
        perform(it)
        return null
      }
      val old = volumeKeysSetting()
      // Kept until the shortcut is set back, so that it's set back even if this process is stopped.
      try {
        File(VOLUME_KEYS_BACKUP).writeText(old ?: NO_SETTING)
      } catch (e: IOException) {
        return e.toString()
      }
      try {
        val value = if (old.isNullOrEmpty()) target else "$old:$target"
        exec("settings", listOf("settings", "put", "secure", VOLUME_KEYS_SETTING, value))?.let { return it }
        waitFor { hasTarget(volumeKeys) }
        perform(volumeKeys)
        return null
      } finally {
        restoreVolumeKeys()
      }
    } catch (e: ReflectiveOperationException) {
      return e.toString()
    }
  }

  /** Sets the volume keys shortcut back as it was, if a shortcut run was stopped before it could. */
  private fun restoreVolumeKeys() {
    val backup = File(VOLUME_KEYS_BACKUP)
    val old =
      try {
        if (!backup.exists()) return
        backup.readText()
      } catch (e: IOException) {
        return
      }
    val restored =
      if (old == NO_SETTING) {
        exec("settings", listOf("settings", "delete", "secure", VOLUME_KEYS_SETTING))
      } else {
        exec("settings", listOf("settings", "put", "secure", VOLUME_KEYS_SETTING, old))
      }
    if (restored == null) backup.delete()
  }

  private fun volumeKeysSetting(): String? =
    shell(listOf("settings", "get", "secure", VOLUME_KEYS_SETTING), null).second.takeUnless {
      it == "null"
    }

  private fun enabledServicesSetting(): String =
    shell(listOf("settings", "get", "secure", ENABLED_SERVICES_SETTING), null).second.takeUnless {
      it == "null"
    }.orEmpty()

  private fun enabledServices(): List<ComponentName> =
    enabledServicesSetting().split(':').mapNotNull { ComponentName.unflattenFromString(it) }

  /** Turns the accessibility service [component] on or off, as Android's settings do. */
  private fun setServiceEnabled(component: ComponentName, on: Boolean): String? {
    val others =
      enabledServicesSetting().split(':').filter {
        it.isNotEmpty() && ComponentName.unflattenFromString(it) != component
      }
    val services = if (on) others + component.flattenToString() else others
    exec("settings", listOf("settings", "put", "secure", ENABLED_SERVICES_SETTING, services.joinToString(":")))
      ?.let { return it }
    return if (on) exec("settings", listOf("settings", "put", "secure", "accessibility_enabled", "1")) else null
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
    /** The setting with the volume keys shortcut's targets, separated by colons. */
    const val VOLUME_KEYS_SETTING = "accessibility_shortcut_target_service"
    /** UserShortcutType.HARDWARE, the volume keys shortcut, from Android 15. */
    const val HARDWARE_SHORTCUT = 2
    /** UserShortcutType.SOFTWARE, the accessibility button. */
    const val SOFTWARE_SHORTCUT = 1
    /** UserShortcutType.GESTURE, the accessibility gesture of gesture navigation. */
    const val GESTURE_SHORTCUT = 32
    const val ENABLED_SERVICES_SETTING = "enabled_accessibility_services"
    /** The volume keys shortcut as it was while a shortcut run has changed it. */
    const val VOLUME_KEYS_BACKUP = "/data/local/tmp/sandfly-volume-keys-shortcut"
    /** Written to [VOLUME_KEYS_BACKUP] when the shortcut had no setting. */
    const val NO_SETTING = "\u0000"
    /** AccessibilityManager.ACCESSIBILITY_SHORTCUT_KEY, the volume keys shortcut, before Android 15. */
    const val OLD_KEY_SHORTCUT = 1
    /** AccessibilityServiceInfo.FEEDBACK_ALL_MASK. */
    const val ALL_FEEDBACK = -1
    const val WAIT_STEPS = 30
    const val WAIT_STEP_MS = 100L
    /** ClipDescription.EXTRA_IS_SENSITIVE, from Android 13, set on clips such as passwords. */
    const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"
    /** The clipboard history keeps no more of a text than this. */
    const val MAX_CLIP_LENGTH = 10_000
    /** Context.TETHERING_SERVICE, which isn't public. */
    const val TETHERING_SERVICE = "tethering"
    /** TetheringManager.TETHERING_WIFI. */
    const val TETHERING_WIFI = 0
    /** Context.ACTIVITY_TASK_SERVICE, which isn't public. */
    const val ACTIVITY_TASK_SERVICE = "activity_task"
    /** ActivityManager.RECENT_IGNORE_UNAVAILABLE. */
    const val RECENT_IGNORE_UNAVAILABLE = 2
    /** More than the system keeps. */
    const val MAX_TASKS = 100
    const val MAX_TITLE_LENGTH = 200
    const val SYSTEM_UI_SERVICE = "com.android.systemui/.SystemUIService"
    /** SplitScreenConstants.SPLIT_POSITION_BOTTOM_OR_RIGHT. */
    const val SPLIT_POSITION_BOTTOM_OR_RIGHT = 1
    /** ActivityManager.START_SWITCHES_CANCELED, the first code of a start that didn't happen. */
    const val START_SWITCHES_CANCELED = 100
  }
}
