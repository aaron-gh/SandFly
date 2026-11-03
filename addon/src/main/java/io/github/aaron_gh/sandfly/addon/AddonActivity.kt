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

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import rikka.shizuku.Shizuku

/** Shows the add-on's version, whether SandFly can use it, and Shizuku's state. */
class AddonActivity : Activity() {

  private val update = Shizuku.OnBinderReceivedListener { runOnUiThread { showState() } }
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
    Shizuku.addBinderReceivedListener(update)
    Shizuku.addBinderDeadListener(dead)
    Shizuku.addRequestPermissionResultListener(permissionResult)
  }

  override fun onResume() {
    super.onResume()
    showState()
  }

  override fun onDestroy() {
    Shizuku.removeBinderReceivedListener(update)
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
