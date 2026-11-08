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

package com.google.android.accessibility.talkback.overview

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.os.UserHandle
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.shizuku.IShizukuGrants
import com.google.android.accessibility.talkback.shizuku.RecentTaskKeys
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/** A task of the system's recent tasks, as the overview lists it. */
data class RecentTask(
  val id: Int,
  val packageName: String,
  /** The task's first activity, if known. */
  val component: ComponentName?,
  /** The profile the task runs in, such as the work profile. */
  val user: UserHandle,
  /** The app's name, with the work profile's badge for a work app. */
  val appLabel: String,
  /** The label the app gave the task, if it gave one that isn't the app's name. */
  val title: String?,
) {
  /** The app and the task's own label, as the overview and Alt+Tab say it. */
  fun name(context: Context): String =
    title?.let { context.getString(R.string.overview_task_with_title, appLabel, it) } ?: appLabel
}

/**
 * The system's recent tasks, read and changed through SandFly's Shizuku shell service, which has
 * the shell's permissions to list every app's tasks, bring one to the front, remove one and put one
 * in split screen. Without Shizuku, the system's overview is used instead.
 *
 * Callbacks run on the main thread.
 */
object RecentTasks {
  private const val TAG = "RecentTasks"
  private const val MAX_TASKS = 60
  /** How long a split waits for the app it splits beside to come to the front. */
  private const val FRONT_WAIT_MS = 1_500L
  private const val FRONT_POLL_MS = 50L
  /** UIDs of one user, as in UserHandle.PER_USER_RANGE. */
  private const val PER_USER_RANGE = 100_000

  /** Whether the user has chosen SandFly's overview in place of the system's. */
  @JvmStatic
  fun isEnabled(context: Context): Boolean =
    SharedPreferencesUtils.getBooleanPref(
      SharedPreferencesUtils.getSharedPreferences(context),
      context.resources,
      R.string.pref_overview_key,
      R.bool.pref_overview_default,
    )

  /** Whether the tasks can be read now: Shizuku runs and allows SandFly. */
  @JvmStatic fun isAvailable(context: Context): Boolean = ShizukuShell.isAvailable(context) && ShizukuShell.isAllowed()

  /** Reads the recent tasks, most recent first: [onResult] gets null if they can't be read. */
  fun load(context: Context, onResult: (List<RecentTask>?) -> Unit) {
    val appContext = context.applicationContext
    ShizukuShell.call(appContext, { service -> read(appContext, service) }, onResult)
  }

  /** Brings [task] to the front. [onDone] gets whether it worked. */
  fun open(context: Context, task: RecentTask, onDone: (Boolean) -> Unit = {}) {
    run(context, "open ${task.id}", onDone) { it.openTask(task.id) }
  }

  /** Removes [task] from the recent tasks, which stops its app if it has no other task. */
  fun close(context: Context, task: RecentTask, onDone: (Boolean) -> Unit = {}) {
    run(context, "close ${task.id}", onDone) { it.closeTask(task.id) }
  }

  /**
   * Shows [task] in split screen beside [front], the task the user was in: [front] comes to the
   * front first, since the split takes the task in front as its other half.
   */
  fun split(context: Context, front: RecentTask, task: RecentTask, onDone: (Boolean) -> Unit = {}) {
    val appContext = context.applicationContext
    run(context, "split ${task.id} beside ${front.id}", onDone) { service ->
      service.openTask(front.id)?.let { return@run it }
      val deadline = SystemClock.uptimeMillis() + FRONT_WAIT_MS
      while (SystemClock.uptimeMillis() < deadline) {
        val tasks = read(appContext, service, includeOverview = true)
        if (tasks?.firstOrNull()?.id == front.id) break
        Thread.sleep(FRONT_POLL_MS)
      }
      service.splitTask(task.id)
    }
  }

  private fun run(
    context: Context,
    what: String,
    onDone: (Boolean) -> Unit,
    work: (IShizukuGrants) -> String?,
  ) {
    ShizukuShell.call(
      context.applicationContext,
      { service ->
        val error = work(service)
        if (error != null) {
          LogUtils.w(TAG, "%s failed: %s", what, error)
        }
        error == null
      },
    ) { worked ->
      onDone(worked == true)
    }
  }

  /**
   * The tasks [service] reports, without SandFly's overview itself, which is in front while it's
   * open, unless [includeOverview].
   */
  private fun read(
    context: Context,
    service: IShizukuGrants,
    includeOverview: Boolean = false,
  ): List<RecentTask>? {
    val bundle: Bundle = service.recentTasks(MAX_TASKS) ?: return null
    val ids = bundle.getIntArray(RecentTaskKeys.IDS) ?: return null
    val users = bundle.getIntArray(RecentTaskKeys.USERS)
    val packages = bundle.getStringArray(RecentTaskKeys.PACKAGES)
    val bases = bundle.getStringArray(RecentTaskKeys.BASE_ACTIVITIES)
    val tops = bundle.getStringArray(RecentTaskKeys.TOP_ACTIVITIES)
    val appLabels = bundle.getStringArray(RecentTaskKeys.APP_LABELS)
    val titles = bundle.getStringArray(RecentTaskKeys.TITLES)
    val activityLabels = bundle.getStringArray(RecentTaskKeys.ACTIVITY_LABELS)
    val overview = OverviewActivity::class.java.name
    val myUser = Process.myUid() / PER_USER_RANGE
    val packageManager = context.packageManager
    val tasks = ArrayList<RecentTask>()
    /** Each task's activity name, for telling apart tasks of one app that have no label. */
    val activityNames = ArrayList<String?>()
    for (index in ids.indices) {
      val packageName = packages?.getOrNull(index).orEmpty()
      if (packageName.isEmpty()) continue
      val activities = listOf(bases?.getOrNull(index), tops?.getOrNull(index))
      if (!includeOverview && activities.any { it?.substringAfter('/') == overview }) {
        continue
      }
      var label: CharSequence = appLabels?.getOrNull(index)?.takeIf { it.isNotBlank() } ?: packageName
      val user = users?.getOrNull(index) ?: myUser
      if (user != myUser) {
        label = packageManager.getUserBadgedLabel(label, UserHandle.getUserHandleForUid(user * PER_USER_RANGE))
      }
      val title =
        titles?.getOrNull(index)?.trim()?.takeIf {
          it.isNotEmpty() && !it.equals(label.toString(), ignoreCase = true) &&
            !it.equals(appLabels?.getOrNull(index), ignoreCase = true)
        }
      tasks +=
        RecentTask(
          id = ids[index],
          packageName = packageName,
          component = bases?.getOrNull(index)?.let { ComponentName.unflattenFromString(it) },
          user = UserHandle.getUserHandleForUid(user * PER_USER_RANGE),
          appLabel = label.toString(),
          title = title,
        )
      activityNames += activityLabels?.getOrNull(index)?.trim()?.takeIf { it.isNotEmpty() }
    }
    val counts = tasks.groupingBy { it.appLabel }.eachCount()
    return tasks.mapIndexed { index, task ->
      val activity = activityNames[index]
      if (
        task.title == null &&
          counts.getValue(task.appLabel) > 1 &&
          activity != null &&
          !activity.equals(task.appLabel, ignoreCase = true)
      ) {
        task.copy(title = activity)
      } else {
        task
      }
    }
  }
}
