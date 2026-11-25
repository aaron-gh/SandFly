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

package com.google.android.accessibility.talkback.directtouch

import android.app.Service
import android.content.Intent
import android.graphics.Rect
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.google.android.accessibility.utils.DeprecatedApiNames
import fyi.quin.backtalk.directtouch.IDirectTouch as LegacyDirectTouch
import io.github.aaron_gh.sandfly.directtouch.IDirectTouch

/**
 * Takes direct touch requests from apps that bind to it, for the app making each call, which
 * Android names. A request counts only while direct touch is on for the app in SandFly's settings.
 */
class DirectTouchService : Service() {
  private val mainHandler = Handler(Looper.getMainLooper())

  private val binder =
    object : IDirectTouch.Stub() {
      override fun requestScreen(client: IBinder?) = setRequest(client, DirectTouchTarget.Screen)

      override fun requestAreas(client: IBinder?, areas: Array<Rect?>?, viewIds: Array<String?>?) {
        val rects =
          areas.orEmpty().filterNotNull().filter { !it.isEmpty }.take(MAX_AREAS).map(::Rect)
        val packages = callingApps()
        val ids =
          viewIds
            .orEmpty()
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length <= MAX_ID_LENGTH }
            .take(MAX_AREAS)
        // A bare name is a view of the app's own package.
        val fullIds =
          packages.firstOrNull()?.let { pkg ->
            ids.map { DirectTouchDeclaration.fullViewId(pkg, it) }
          } ?: return
        setRequest(client, DirectTouchTarget.Areas(screenAreas = rects, viewIds = fullIds), packages)
      }

      override fun stop(client: IBinder?) = setRequest(client, DirectTouchTarget.None)

      override fun reset(client: IBinder?) = setRequest(client, null)

      override fun setState(client: IBinder?, state: String?) {
        client ?: return
        val uid = Binder.getCallingUid()
        val packages = callingApps()
        val value = state?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_ID_LENGTH }
        mainHandler.post { DirectTouchRequests.setState(uid, packages, client, value) }
      }
    }

  /** Read on the binder thread, before posting, as are the caller's packages. */
  private fun setRequest(
    client: IBinder?,
    target: DirectTouchTarget?,
    packages: List<String> = callingApps(),
  ) {
    client ?: return
    val uid = Binder.getCallingUid()
    mainHandler.post { DirectTouchRequests.setRequest(uid, packages, client, target) }
  }

  /** The same calls, for apps built against the deprecated interface. */
  private val legacyBinder =
    object : LegacyDirectTouch.Stub() {
      override fun requestScreen(client: IBinder?) {
        warnLegacy()
        binder.requestScreen(client)
      }

      override fun requestAreas(client: IBinder?, areas: Array<Rect?>?, viewIds: Array<String?>?) {
        warnLegacy()
        binder.requestAreas(client, areas, viewIds)
      }

      override fun stop(client: IBinder?) {
        warnLegacy()
        binder.stop(client)
      }

      override fun reset(client: IBinder?) {
        warnLegacy()
        binder.reset(client)
      }

      override fun setState(client: IBinder?, state: String?) {
        warnLegacy()
        binder.setState(client, state)
      }
    }

  override fun onBind(intent: Intent): IBinder =
    if (intent.action == LEGACY_ACTION_DIRECT_TOUCH) legacyBinder else binder

  /** Read on the binder thread, as the caller is. */
  private fun warnLegacy() {
    DeprecatedApiNames.warn(
      callingApps().firstOrNull(),
      "fyi.quin.backtalk.directtouch.IDirectTouch",
      "io.github.aaron_gh.sandfly.directtouch.IDirectTouch",
    )
  }

  /** The packages of the app making the current call. */
  private fun callingApps(): List<String> =
    packageManager.getPackagesForUid(Binder.getCallingUid())?.toList().orEmpty()

  companion object {
    /** The action that binds this service. */
    const val ACTION_DIRECT_TOUCH = "io.github.aaron_gh.sandfly.action.DIRECT_TOUCH"
    /** Deprecated; see DeprecatedApiNames. */
    const val LEGACY_ACTION_DIRECT_TOUCH = "fyi.quin.backtalk.action.DIRECT_TOUCH"

    private const val MAX_AREAS = 32
    private const val MAX_ID_LENGTH = 256
  }
}
