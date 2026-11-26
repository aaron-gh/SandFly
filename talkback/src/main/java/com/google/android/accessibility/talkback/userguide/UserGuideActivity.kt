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

package com.google.android.accessibility.talkback.userguide

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.keyboard.KeyComboManager
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Help & feedback > User guide: the user guide in a web view, with its table of contents first.
 * With [EXTRA_SECTION], the Help action of a settings screen, only the section of that screen.
 */
class UserGuideActivity : AppCompatActivity() {

  private var webView: WebView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setTitle(R.string.title_pref_user_guide)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    val web =
      try {
        WebView(this)
      } catch (e: RuntimeException) {
        // No WebView, or one being updated.
        LogUtils.e(TAG, "No web view: %s", e)
        Toast.makeText(this, R.string.user_guide_unavailable, Toast.LENGTH_LONG).show()
        finish()
        return
      }
    webView = web
    web.settings.apply {
      javaScriptEnabled = false
      allowFileAccess = false
      allowContentAccess = false
      blockNetworkLoads = true
      textZoom = (resources.configuration.fontScale * 100).toInt()
    }
    web.webViewClient =
      object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
          val url = request.url
          // Links to headings stay on the page.
          if (url.toString().substringBefore('#') == BASE_URL) return false
          // Links from a section to other headings open their section, or the whole guide.
          if (url.toString().startsWith(BASE_URL)) {
            val target = url.getQueryParameter(GuideMarkdown.SECTION_PARAMETER)
            if (target != null) {
              val section = target.takeIf { it != GuideMarkdown.CONTENTS_ID }
              startActivity(UserGuideActivity.intent(this@UserGuideActivity, section))
            }
            return true
          }
          try {
            startActivity(Intent(Intent.ACTION_VIEW, url).addCategory(Intent.CATEGORY_BROWSABLE))
          } catch (e: ActivityNotFoundException) {
            Toast.makeText(this@UserGuideActivity, url.toString(), Toast.LENGTH_LONG).show()
          }
          return true
        }
      }
    setContentView(web)
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          // Back returns to where a link was followed from, then leaves the guide.
          if (web.canGoBack()) web.goBack() else finish()
        }
      },
    )

    val keyComboManager =
      TalkBackService.getInstance()?.keyComboManager ?: KeyComboManager.create(this)
    val context: Context = this
    val section = intent.getStringExtra(EXTRA_SECTION)
    Thread {
      val html =
        try {
          UserGuide.build(context, keyComboManager, section)
        } catch (e: Exception) {
          LogUtils.e(TAG, "User guide failed: %s", e)
          null
        }
      runOnUiThread {
        if (isDestroyed) return@runOnUiThread
        if (html == null) {
          Toast.makeText(this, R.string.user_guide_unavailable, Toast.LENGTH_LONG).show()
          finish()
          return@runOnUiThread
        }
        web.loadDataWithBaseURL(BASE_URL, html, "text/html", "utf-8", null)
      }
    }
      .start()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onDestroy() {
    webView?.destroy()
    webView = null
    super.onDestroy()
  }

  companion object {
    private const val TAG = "UserGuideActivity"

    // The page has no files or network resources; links to its headings resolve against this.
    private const val BASE_URL = "https://user-guide.sandfly.invalid/"

    /** Whether the guide can open: not on watches, which have no web view. */
    @JvmStatic fun isAvailable(): Boolean = !FormFactorUtils.isAndroidWear()

    /** The ID of the heading to show with the headings under it, instead of the whole guide. */
    private const val EXTRA_SECTION = "section"

    /** Opens the whole guide, or with [section], the heading with that ID and the ones under it. */
    @JvmStatic
    @JvmOverloads
    fun intent(context: Context, section: String? = null): Intent =
      Intent(context, UserGuideActivity::class.java).apply { if (section != null) putExtra(EXTRA_SECTION, section) }
  }
}
