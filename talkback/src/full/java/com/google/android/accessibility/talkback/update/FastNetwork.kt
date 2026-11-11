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
 * The watch's own Wi-Fi network, held for a large download on a watch. A watch otherwise
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

    /** Asks for a Wi-Fi network and waits for it, or returns null if none comes up. */
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
          // The phone's Bluetooth proxy counts as unmetered, so ask for Wi-Fi itself.
          .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
          .build()
      try {
        manager.requestNetwork(request, callback)
      } catch (e: RuntimeException) {
        LogUtils.w(TAG, "Cannot ask for Wi-Fi: %s", e)
        return null
      }
      val network = if (available.await(WAIT_SECONDS, TimeUnit.SECONDS)) found else null
      if (network == null) {
        manager.unregisterNetworkCallback(callback)
        LogUtils.w(
          TAG,
          "No Wi-Fi came up; downloading over the default network (%s)",
          transport(manager, runCatching { manager.activeNetwork }.getOrNull()),
        )
        return null
      }
      LogUtils.i(TAG, "Downloading over %s", transport(manager, network))
      return FastNetwork(manager, callback, network)
    }

    private fun transport(manager: ConnectivityManager, network: Network?): String {
      val capabilities =
        network?.let { runCatching { manager.getNetworkCapabilities(it) }.getOrNull() }
          ?: return "unknown"
      return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
        else -> "other"
      }
    }
  }
}
