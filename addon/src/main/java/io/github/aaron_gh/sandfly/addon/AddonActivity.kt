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

package io.github.aaron_gh.sandfly.addon

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import rikka.shizuku.Shizuku

/** Shows the add-on's version, whether SandFly can use it, Shizuku's state, and updates. */
class AddonActivity : Activity() {
  private var update: AddonUpdater.Update? = null
  private var busy = false

  private val binderReceived = Shizuku.OnBinderReceivedListener { runOnUiThread { showState() } }
  private val dead = Shizuku.OnBinderDeadListener { runOnUiThread { showState() } }
  private val permissionResult =
    Shizuku.OnRequestPermissionResultListener { _, _ -> runOnUiThread { showState() } }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.addon_activity)
    findViewById<TextView>(R.id.version).text = getString(R.string.addon_version, BuildConfig.VERSION_NAME)
    findViewById<Button>(R.id.allow).setOnClickListener {
      try {
        Shizuku.requestPermission(AddonShizuku.REQUEST_CODE)
      } catch (e: RuntimeException) {
        showState()
      }
    }
    Shizuku.addBinderReceivedListener(binderReceived)
    Shizuku.addBinderDeadListener(dead)
    Shizuku.addRequestPermissionResultListener(permissionResult)
    findViewById<Button>(R.id.update).setOnClickListener { onUpdateClicked() }
    // For the notification about updates.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }
    checkForUpdate()
  }

  private fun onUpdateClicked() {
    val available = update
    if (available == null) {
      checkForUpdate()
      return
    }
    busy = true
    showUpdate(getString(R.string.addon_downloading))
    AddonUpdater.install(this, available) { worked ->
      busy = false
      if (isDestroyed) return@install
      showUpdate(if (worked) getString(R.string.addon_update_available, available.version) else getString(R.string.addon_update_failed))
    }
  }

  private fun checkForUpdate() {
    busy = true
    showUpdate(getString(R.string.addon_checking))
    AddonUpdater.checkNow(this) { result ->
      busy = false
      if (isDestroyed) return@checkNow
      update = (result as? AddonUpdater.Result.Available)?.update
      showUpdate(
        when (result) {
          is AddonUpdater.Result.Available -> getString(R.string.addon_update_available, result.update.version)
          AddonUpdater.Result.Current -> getString(R.string.addon_up_to_date)
          AddonUpdater.Result.Failed -> getString(R.string.addon_check_failed)
        }
      )
    }
  }

  private fun showUpdate(status: String) {
    findViewById<TextView>(R.id.update_status).text = status
    findViewById<Button>(R.id.update).apply {
      isEnabled = !busy
      setText(if (update != null) R.string.addon_install_update else R.string.addon_check_updates)
    }
  }

  override fun onResume() {
    super.onResume()
    showState()
  }

  override fun onDestroy() {
    Shizuku.removeBinderReceivedListener(binderReceived)
    Shizuku.removeBinderDeadListener(dead)
    Shizuku.removeRequestPermissionResultListener(permissionResult)
    super.onDestroy()
  }

  private fun showState() {
    findViewById<TextView>(R.id.sandfly).setText(
      when (packageManager.checkSignatures(packageName, SANDFLY)) {
        PackageManager.SIGNATURE_MATCH -> R.string.addon_sandfly_ready
        PackageManager.SIGNATURE_UNKNOWN_PACKAGE -> R.string.addon_sandfly_missing
        else -> R.string.addon_sandfly_other_key
      }
    )
    val ready = AddonShizuku.isReady()
    val allowed = AddonShizuku.hasPermission()
    val denied = AddonShizuku.isPermissionDenied()
    findViewById<TextView>(R.id.shizuku).setText(
      when {
        !ready -> R.string.addon_shizuku_not_running
        allowed -> R.string.addon_shizuku_allowed
        denied -> R.string.addon_shizuku_denied
        else -> R.string.addon_shizuku_not_allowed
      }
    )
    findViewById<Button>(R.id.allow).visibility =
      if (ready && !allowed && !denied) View.VISIBLE else View.GONE
  }

  private companion object {
    const val SANDFLY = "io.github.aaron_gh.sandfly"
  }
}
