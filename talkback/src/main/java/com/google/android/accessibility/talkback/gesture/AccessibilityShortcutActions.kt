package com.google.android.accessibility.talkback.gesture

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityManager
import com.google.android.accessibility.talkback.shizuku.ShizukuCommand
import com.google.android.accessibility.talkback.shizuku.ShizukuShell

/**
 * Actions that run another feature's accessibility shortcut, such as Reading mode or Gemini's
 * Guided Vision, as Android's accessibility button or volume keys would, without the feature taking
 * one of those shortcuts. A feature that is an activity is started; one that is an accessibility
 * service needs Shizuku, which runs the shortcut as the shell user.
 */
object AccessibilityShortcutActions {
  private const val TAG = "A11yShortcutActions"

  /** Starts every saved value of these actions. It must never change once released. */
  private const val PREFIX = "ACCESSIBILITY_SHORTCUT:"

  /** A feature that has an accessibility shortcut, and the action that runs it. */
  data class Target(val label: String, val value: String)

  /** Returns whether [value] is an action that runs an accessibility shortcut. */
  @JvmStatic fun isAction(value: String?): Boolean = component(value) != null

  private fun component(value: String?): ComponentName? =
    if (value != null && value.startsWith(PREFIX)) {
      ComponentName.unflattenFromString(value.substring(PREFIX.length))
    } else {
      null
    }

  private fun valueOf(component: ComponentName): String = PREFIX + component.flattenToString()

  /** Returns the features with an accessibility shortcut, other than SandFly, by name. */
  @JvmStatic
  fun targets(context: Context): List<Target> {
    val packageManager = context.packageManager
    val activities =
      packageManager.queryIntentActivities(shortcutIntent(), /* flags= */ 0).map {
        Target(
          it.loadLabel(packageManager).toString(),
          valueOf(ComponentName(it.activityInfo.packageName, it.activityInfo.name)),
        )
      }
    val services =
      services(context).map {
        Target(
          it.resolveInfo.loadLabel(packageManager).toString(),
          valueOf(ComponentName.unflattenFromString(it.id) ?: return@map null),
        )
      }
    return (activities + services.filterNotNull())
      .filterNot { component(it.value)?.packageName == context.packageName }
      .distinctBy { it.value }
      .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, Target::label).thenBy(Target::value))
  }

  private fun service(context: Context, component: ComponentName): AccessibilityServiceInfo? =
    services(context).firstOrNull { ComponentName.unflattenFromString(it.id) == component }

  private fun services(context: Context): List<AccessibilityServiceInfo> =
    context.getSystemService(AccessibilityManager::class.java)?.installedAccessibilityServiceList.orEmpty()

  /** Returns the feature's name, or null when [value] isn't one of these actions. */
  @JvmStatic
  fun label(context: Context, value: String?): String? {
    val component = component(value) ?: return null
    val packageManager = context.packageManager
    service(context, component)?.let {
      return it.resolveInfo.loadLabel(packageManager).toString()
    }
    return try {
      packageManager.getActivityInfo(component, /* flags= */ 0).loadLabel(packageManager).toString()
    } catch (e: Exception) {
      component.flattenToShortString()
    }
  }

  /**
   * Runs the accessibility shortcut of the action saved as [value]. [onDone] gets whether it ran,
   * at once for an activity, or when Shizuku has run it.
   */
  @JvmStatic
  fun run(context: Context, value: String, onDone: (Boolean) -> Unit) {
    val component = component(value)
    if (component == null) {
      onDone(false)
      return
    }
    val service = service(context, component)
    if (service != null) {
      // A service that asks for the accessibility button is opened by its shortcut, from Android 11.
      val opens =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
          service.flags and AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON != 0 &&
          service.resolveInfo.serviceInfo.applicationInfo.targetSdkVersion > Build.VERSION_CODES.Q
      val argument = component.flattenToString() + if (opens) ShizukuCommand.BUTTON else ""
      ShizukuShell.run(context, ShizukuCommand.ACCESSIBILITY_SHORTCUT, argument, onDone)
      return
    }
    try {
      context.startActivity(
        shortcutIntent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
      onDone(true)
    } catch (e: ActivityNotFoundException) {
      Log.w(TAG, "No accessibility shortcut activity $component", e)
      onDone(false)
    } catch (e: SecurityException) {
      // Activities that only Android starts can be started as the shell user.
      ShizukuShell.run(
        context,
        ShizukuCommand.START_SHORTCUT_ACTIVITY,
        component.flattenToString(),
        onDone,
      )
    }
  }

  private fun shortcutIntent(): Intent =
    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_ACCESSIBILITY_SHORTCUT_TARGET)
}
