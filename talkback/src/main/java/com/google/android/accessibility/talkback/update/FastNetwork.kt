/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.update

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.WorkerThread
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * An unmetered network, such as Wi-Fi, held for a large download on a watch. A watch otherwise
 * sends traffic through the phone's Bluetooth connection, which takes many minutes for an update.
 */
internal class FastNetwork
private constructor(
  private val manager: ConnectivityManager,
  private val callback: ConnectivityManager.NetworkCallback,
  val network: Network,
) {
  /** Lets the watch turn the network off again. */
  fun release() {
    try {
      manager.unregisterNetworkCallback(callback)
    } catch (e: IllegalArgumentException) {
      // Already released.
    }
  }

  companion object {
    private const val TAG = "FastNetwork"
    private const val WAIT_SECONDS = 20L

    /** Asks for an unmetered network and waits for it, or returns null if none comes up. */
    @WorkerThread
    fun request(context: Context): FastNetwork? {
      val manager = context.getSystemService(ConnectivityManager::class.java) ?: return null
      val available = CountDownLatch(1)
      var found: Network? = null
      val callback =
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            found = network
            available.countDown()
          }
        }
      val request =
        NetworkRequest.Builder()
          .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
          .build()
      try {
        manager.requestNetwork(request, callback)
      } catch (e: RuntimeException) {
        LogUtils.w(TAG, "Cannot ask for an unmetered network: %s", e)
        return null
      }
      val network = if (available.await(WAIT_SECONDS, TimeUnit.SECONDS)) found else null
      if (network == null) {
        LogUtils.w(TAG, "No unmetered network came up; downloading over the default network")
        manager.unregisterNetworkCallback(callback)
        return null
      }
      return FastNetwork(manager, callback, network)
    }
  }
}
