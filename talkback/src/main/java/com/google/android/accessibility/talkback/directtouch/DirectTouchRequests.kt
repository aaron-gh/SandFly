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

import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException

/**
 * What apps asked for through [DirectTouchService]: a target for each app, which replaces its
 * manifest declaration, and the state its declaration's rules match. Both last until the app
 * changes them or the process holding the app's client binder ends. Used on the main thread only.
 */
object DirectTouchRequests {
  private val requests = HashMap<String, DirectTouchTarget>()
  private val states = HashMap<String, String>()
  private val clients = HashMap<Int, Client>()
  private val handler = Handler(Looper.getMainLooper())

  /** Runs when a request or state changes. */
  var onChanged: Runnable? = null

  private class Client(val binder: IBinder, val packages: List<String>) {
    lateinit var recipient: IBinder.DeathRecipient
  }

  /** The app's request, or null when it has none and its declaration applies. */
  fun request(packageName: String): DirectTouchTarget? = requests[packageName]

  fun state(packageName: String): String? = states[packageName]

  /** Sets the target of the app with [uid], or drops it when [target] is null. */
  fun setRequest(uid: Int, packages: List<String>, client: IBinder, target: DirectTouchTarget?) {
    if (packages.isEmpty()) {
      return
    }
    var changed = watch(uid, packages, client) ?: return
    for (pkg in packages) {
      val old = if (target == null) requests.remove(pkg) else requests.put(pkg, target)
      changed = changed || old != target
    }
    if (changed) {
      onChanged?.run()
    }
  }

  fun setState(uid: Int, packages: List<String>, client: IBinder, state: String?) {
    if (packages.isEmpty()) {
      return
    }
    var changed = watch(uid, packages, client) ?: return
    for (pkg in packages) {
      val old = if (state == null) states.remove(pkg) else states.put(pkg, state)
      changed = changed || old != state
    }
    if (changed) {
      onChanged?.run()
    }
  }

  /**
   * Remembers [client] as the app's, so that its requests go when its process ends. A new process
   * of the app passes a new binder, which replaces the old one and drops what the old process
   * asked for. Returns whether that dropped anything, or null when the process has already ended.
   */
  private fun watch(uid: Int, packages: List<String>, client: IBinder): Boolean? {
    val current = clients[uid]
    if (current?.binder == client) {
      return false
    }
    val entry = Client(client, packages)
    entry.recipient = IBinder.DeathRecipient { handler.post { onClientDied(uid, entry) } }
    try {
      client.linkToDeath(entry.recipient, 0)
    } catch (_: RemoteException) {
      return null
    }
    clients[uid] = entry
    if (current == null) {
      return false
    }
    current.binder.unlinkToDeath(current.recipient, 0)
    return forget(current.packages)
  }

  private fun onClientDied(uid: Int, client: Client) {
    if (clients[uid] !== client) {
      return
    }
    clients.remove(uid)
    if (forget(client.packages)) {
      onChanged?.run()
    }
  }

  private fun forget(packages: List<String>): Boolean {
    var changed = false
    for (pkg in packages) {
      changed = (requests.remove(pkg) != null) or changed
      changed = (states.remove(pkg) != null) or changed
    }
    return changed
  }

  /** Drops everything, for tests. */
  fun clear() {
    clients.values.forEach {
      try {
        it.binder.unlinkToDeath(it.recipient, 0)
      } catch (_: Exception) {}
    }
    clients.clear()
    requests.clear()
    states.clear()
  }
}
