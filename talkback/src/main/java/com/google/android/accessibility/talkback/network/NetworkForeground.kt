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

package com.google.android.accessibility.talkback.network

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.google.android.accessibility.talkback.network.ForegroundLeases.Acquired
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Runs SandFly's network requests while SandFly is a foreground service when Android blocks its
 * network. An accessibility service runs as a bound service while another app is in front, and
 * some phones, such as Xiaomi's with HyperOS on Android 16, block the network of apps in that
 * state. A foreground service lifts the block.
 *
 * The service starts only while the network is blocked, and stops as soon as the last request
 * ends, before Android shows its notification, about 10 seconds after the start. When Android
 * doesn't let the service start, the request runs as it is.
 */
object NetworkForeground {
  private const val TAG = "NetworkForeground"

  /** How long a request waits for the service to be in the foreground. */
  private const val START_WAIT_MS = 2_000L

  /** How long a request then waits for Android to unblock the network. */
  private const val UNBLOCK_WAIT_MS = 2_000L

  @Volatile private var appContext: Context? = null
  internal val leases = ForegroundLeases()
  private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

  /** A request's hold on the service. [release] can be called more than once. */
  class Lease internal constructor(private var held: Boolean) {
    @Synchronized
    fun release() {
      if (!held) return
      held = false
      if (leases.release()) mainHandler.post { NetworkForegroundService.stopIfIdle() }
    }
  }

  private val NONE = Lease(false)

  /** Called once in SandFly's main process; requests in other processes run as they are. */
  @JvmStatic
  fun initialize(context: Context) {
    appContext = context.applicationContext
  }

  /** Runs [block], a network request, while the network is not blocked. */
  @JvmStatic
  fun <T> run(block: () -> T): T {
    val lease = acquire()
    try {
      return block()
    } finally {
      lease.release()
    }
  }

  /**
   * Starts a network request; call [Lease.release] when it ends. Waits up to about 4 seconds when
   * the network is blocked. On the main thread nothing waits and the request runs as it is.
   */
  @JvmStatic
  fun acquire(): Lease {
    val context = appContext ?: return NONE
    if (Looper.myLooper() == Looper.getMainLooper()) return NONE
    val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return NONE
    val lease =
      when (leases.acquire(isBlocked(connectivity))) {
        Acquired.NONE -> return NONE
        Acquired.START -> {
          LogUtils.i(TAG, "Network blocked; starting the foreground service")
          if (!start(context)) leases.onStartFailed()
          Lease(true)
        }
        Acquired.JOIN -> Lease(true)
      }
    leases.awaitStarted(START_WAIT_MS)
    if (leases.phaseNow == ForegroundLeases.Phase.FOREGROUND && isBlocked(connectivity)) {
      awaitUnblocked(connectivity)
    }
    return lease
  }

  private fun start(context: Context): Boolean =
    try {
      // Not startForegroundService: that crashes the whole process when the service stops, or
      // Android refuses its foreground start, before it is in the foreground.
      context.startService(Intent(context, NetworkForegroundService::class.java)) != null
    } catch (e: RuntimeException) {
      // IllegalStateException when Android doesn't let SandFly start services in the background.
      LogUtils.w(TAG, "Cannot start the network service: %s", e)
      false
    }

  @Suppress("DEPRECATION") // The one synchronous way to read whether this app's network is blocked.
  private fun isBlocked(connectivity: ConnectivityManager): Boolean =
    try {
      connectivity.activeNetworkInfo?.detailedState == NetworkInfo.DetailedState.BLOCKED
    } catch (e: RuntimeException) {
      false
    }

  /**
   * Android unblocks the network a moment after the service is in the foreground; waits for that,
   * up to [UNBLOCK_WAIT_MS].
   */
  private fun awaitUnblocked(connectivity: ConnectivityManager) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    val unblocked = CountDownLatch(1)
    val callback =
      object : ConnectivityManager.NetworkCallback() {
        override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
          if (!blocked) unblocked.countDown()
        }
      }
    try {
      connectivity.registerDefaultNetworkCallback(callback)
    } catch (e: RuntimeException) {
      return
    }
    try {
      if (!unblocked.await(UNBLOCK_WAIT_MS, TimeUnit.MILLISECONDS)) {
        LogUtils.w(TAG, "Network still blocked after %d ms", UNBLOCK_WAIT_MS)
      }
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
    } finally {
      try {
        connectivity.unregisterNetworkCallback(callback)
      } catch (e: RuntimeException) {
        // Already unregistered.
      }
    }
  }
}
