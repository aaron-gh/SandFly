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

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.focusmanagement.HomeScreenPages
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import java.util.Locale

/**
 * Opens SandFly's overview when the system's overview opens by the navigation gesture or the
 * navigation bar's Recents button, which don't go through SandFly's actions. Android has no way to
 * replace the system's overview, so SandFly's opens on top of it.
 *
 * Runs on the main thread.
 */
object SystemOverviewRedirect {
  /**
   * The string resources that the overview's window can be titled with: AOSP and Pixel's, and
   * Samsung's. Launchers only set the title once the overview has opened. During the quick switch
   * and the home gesture, the overview's views show under the title of the home screen.
   */
  private val OVERVIEW_TITLE_RESOURCES =
    listOf("accessibility_recent_apps", "samsung_accessibility_recent", "recents_app_name", "recent_apps")

  /**
   * Views of the launchers' overviews, which also show during the quick switch. Their events are
   * dropped until the window's title decides, so that the system's overview isn't spoken.
   */
  private val OVERVIEW_VIEW_IDS =
    listOf("overview_panel", "clear_all", "recents_activity_root_view", "recents_empty_text")

  /**
   * Where the launcher isn't the home screen, the overview has an activity of its own, in front
   * during the quick switch too. If that is still in front after this long, the overview has opened.
   */
  private const val SETTLE_MS = 700L

  /** How long a requested system overview has to open. */
  private const val KEEP_WAIT_MS = 3_000L

  /** How long the launcher's events are dropped while SandFly's overview opens. */
  private const val OPEN_WAIT_MS = 1_500L

  /**
   * How long the home screen has to have been in front to count as where the overview was opened.
   * The navigation gesture brings the launcher to the front before its overview shows.
   */
  private const val HOME_SETTLE_MS = 2_000L

  /** Time for the system's overview to be active after SandFly's closes on top of it. */
  private const val UNCOVER_WAIT_MS = 500L

  private val handler = Handler(Looper.getMainLooper())
  private var recentsRead = false
  private var recents: ComponentName? = null

  /** The system's overview was requested, so it stays until it closes. */
  private var keep = false
  private var keepUntil = 0L
  private var keepSeen = false

  /** SandFly's overview is opening: the system's opening again doesn't open it twice. */
  private var openingUntil = 0L

  /** The launcher's events are dropped until then. */
  private var dropUntil = 0L
  private var openedFromHome = false
  private var titles: Set<String> = emptySet()
  private var titlesLocale: Locale? = null
  private val openIfSettled = Runnable { openIfSettled() }

  /** When the home screen came to the front, or 0 while an app is in front. */
  private var homeSince = 0L

  /** Leaves the next system overview open, for the System overview command. */
  @JvmStatic
  fun keepSystemOverview() {
    keep = true
    keepSeen = false
    keepUntil = SystemClock.uptimeMillis() + KEEP_WAIT_MS
  }

  /** Shows the system's overview that SandFly's overview covered, as SandFly's overview closes. */
  fun showCoveredOverview(service: AccessibilityService) {
    keepSystemOverview()
    handler.postDelayed(
      {
        val active = recents(service)?.let { isOverviewShown(service, it) } == true
        if (!active) {
          TalkBackService.getInstance()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        }
      },
      UNCOVER_WAIT_MS,
    )
  }

  /** Marks SandFly's overview as on screen, ending the drop of the launcher's events. */
  fun onOverviewShown() {
    openingUntil = 0
    dropUntil = 0
  }

  /** Whether SandFly's overview was opened from the home screen rather than from an app. */
  fun openedFromHome(): Boolean = openedFromHome

  /**
   * Opens SandFly's overview if [event] shows that the system's has opened. [isAppWindow] is whether
   * the event is from an app's window. Returns whether SandFly should ignore [event].
   */
  @JvmStatic
  fun onAccessibilityEvent(
    service: AccessibilityService,
    event: AccessibilityEvent,
    isAppWindow: Boolean,
  ): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !RecentTasks.isEnabled(service)) {
      return false
    }
    val recents = recents(service) ?: return false
    val packageName = event.packageName?.toString()
    val isFromRecents = packageName == recents.packageName
    val now = SystemClock.uptimeMillis()
    val dropped = now < dropUntil && isFromRecents
    val windowEvent =
      when (event.eventType) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> true
        AccessibilityEvent.TYPE_WINDOWS_CHANGED ->
          event.windowChanges and WINDOW_CHANGES != 0
        else -> false
      }
    if (!windowEvent) {
      return dropped
    }
    val overview = isOverview(service, event, recents)
    if (!overview) {
      if (!keep && !OverviewActivity.isShown() && looksLikeOverview(service, recents) && canOpen(service)) {
        dropUntil = now + OPEN_WAIT_MS
        return true
      }
      if (keep && (keepSeen || now >= keepUntil)) {
        keep = false
      }
      if (
        event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
          isAppWindow &&
          packageName != service.packageName &&
          !dropped &&
          !OverviewActivity.isShown() &&
          service.rootInActiveWindow?.packageName?.toString() == packageName
      ) {
        homeSince =
          when {
            !HomeScreenPages.isHomePackage(service, packageName) -> 0
            homeSince == 0L -> now
            else -> homeSince
          }
      }
      return dropped
    }
    if (keep && !keepSeen && now >= keepUntil) {
      keep = false
    }
    if (keep) {
      keepSeen = true
      return false
    }
    if (now < openingUntil) {
      return true
    }
    if (!canOpen(service)) {
      sayShizukuMissing(service)
      return false
    }
    // As with the Overview action, opening the overview again switches to the previous app.
    if (OverviewActivity.isShown()) {
      markOpening(now + OPEN_WAIT_MS)
      PreviousApp.switchTo(service)
      return true
    }
    openedFromHome = homeSince != 0L && now - homeSince >= HOME_SETTLE_MS
    if (HomeScreenPages.isHomePackage(service, recents.packageName)) {
      markOpening(now + OPEN_WAIT_MS)
      OverviewActivity.openOverSystemOverview(service)
    } else {
      markOpening(now + SETTLE_MS + OPEN_WAIT_MS)
      handler.removeCallbacks(openIfSettled)
      handler.postDelayed(openIfSettled, SETTLE_MS)
    }
    return true
  }

  private fun markOpening(until: Long) {
    openingUntil = until
    dropUntil = until
  }

  private fun openIfSettled() {
    val service = TalkBackService.getInstance()
    val recents = service?.let { recents(it) }
    if (
      service == null ||
        recents == null ||
        keep ||
        OverviewActivity.isShown() ||
        !isOverviewShown(service, recents) ||
        !canOpen(service)
    ) {
      markOpening(0)
      return
    }
    OverviewActivity.openOverSystemOverview(service)
  }

  private const val WINDOW_CHANGES =
    AccessibilityEvent.WINDOWS_CHANGE_ACTIVE or
      AccessibilityEvent.WINDOWS_CHANGE_FOCUSED or
      AccessibilityEvent.WINDOWS_CHANGE_TITLE

  /**
   * Says why the system's overview opens in place of SandFly's when Shizuku isn't running or
   * doesn't allow SandFly, at most once in [SHIZUKU_MESSAGE_GAP_MS]. Nothing is said on the lock
   * screen, where the system's overview is kept anyway.
   */
  @JvmStatic
  fun sayShizukuMissing(service: AccessibilityService) {
    if (!RecentTasks.isEnabled(service) || RecentTasks.isAvailable(service)) return
    if (service.getSystemService(KeyguardManager::class.java).isKeyguardLocked) return
    val now = SystemClock.uptimeMillis()
    if (now - shizukuMessageAt < SHIZUKU_MESSAGE_GAP_MS) return
    shizukuMessageAt = now
    val talkBack = TalkBackService.getInstance() ?: return
    val text =
      service.getString(
        if (ShizukuShell.isAvailable(service)) R.string.overview_shizuku_not_allowed
        else R.string.overview_shizuku_not_running
      )
    talkBack.feedbackReturner?.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
        text,
        SpeakOptions.create()
          // The system's overview is spoken after the message, not over it.
          .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT_AND_UNINTERRUPTIBLE_BY_NEW_SPEECH)
          .setFlags(FeedbackItem.FLAG_NO_HISTORY or FeedbackItem.FLAG_FORCE_FEEDBACK_ALL),
      ),
    )
  }

  private var shizukuMessageAt = Long.MIN_VALUE / 2
  private const val SHIZUKU_MESSAGE_GAP_MS = 3_000L

  private fun canOpen(service: AccessibilityService): Boolean =
    RecentTasks.isAvailable(service) &&
      !service.getSystemService(KeyguardManager::class.java).isKeyguardLocked

  private fun isOverview(
    service: AccessibilityService,
    event: AccessibilityEvent?,
    recents: ComponentName,
  ): Boolean {
    if (service.rootInActiveWindow?.packageName?.toString() != recents.packageName) {
      return false
    }
    if (event?.className?.toString() == recents.className) {
      return true
    }
    val title = service.windows.firstOrNull { it.isActive }?.title?.toString() ?: return false
    return title in overviewTitles(service, recents.packageName)
  }

  private fun looksLikeOverview(service: AccessibilityService, recents: ComponentName): Boolean {
    val root = service.rootInActiveWindow ?: return false
    return root.packageName?.toString() == recents.packageName &&
      OVERVIEW_VIEW_IDS.any {
        root.findAccessibilityNodeInfosByViewId("${recents.packageName}:id/$it").isNotEmpty()
      }
  }

  /** Whether the overview is on screen now. Its own activity's title may be unknown. */
  private fun isOverviewShown(service: AccessibilityService, recents: ComponentName): Boolean =
    isOverview(service, null, recents) ||
      (!HomeScreenPages.isHomePackage(service, recents.packageName) &&
        service.rootInActiveWindow?.packageName?.toString() == recents.packageName)

  /** The titles the launcher [packageName] gives the overview, in the current language. */
  private fun overviewTitles(service: AccessibilityService, packageName: String): Set<String> {
    val locale = service.resources.configuration.locales[0]
    if (locale != titlesLocale) {
      titlesLocale = locale
      titles =
        runCatching {
            val launcher = service.packageManager.getResourcesForApplication(packageName)
            OVERVIEW_TITLE_RESOURCES.mapNotNull { name ->
                launcher
                  .getIdentifier(name, "string", packageName)
                  .takeIf { it != 0 }
                  ?.let { launcher.getString(it) }
              }
              .toSet()
          }
          .getOrDefault(emptySet())
    }
    return titles
  }

  /** The component that hosts the system's overview, from Android's configuration. */
  private fun recents(service: AccessibilityService): ComponentName? {
    if (!recentsRead) {
      recentsRead = true
      val system = Resources.getSystem()
      val id = system.getIdentifier("config_recentsComponentName", "string", "android")
      recents = if (id != 0) ComponentName.unflattenFromString(system.getString(id)) else null
    }
    return recents
  }
}
