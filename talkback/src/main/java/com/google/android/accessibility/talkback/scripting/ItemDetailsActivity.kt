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

package com.google.android.accessibility.talkback.scripting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.utils.preference.BasePreferencesActivity

/**
 * The details of an item from Inspect focused item, under a heading for each part. Copy all copies
 * them as text; a line copies its value.
 */
class ItemDetailsActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setTitle(R.string.script_inspect_clip_label)
    val sections = sections(intent)

    val content =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(
          heading(getString(R.string.script_inspect_clip_label), 22f).apply {
            setPadding(dp(16), dp(12), dp(16), dp(4))
          }
        )
        addView(
          Button(this@ItemDetailsActivity).apply {
            setText(R.string.item_details_copy_all)
            setOnClickListener {
              copy(it, ItemInspector.plainText(this@ItemDetailsActivity, sections))
            }
          }
        )
        if (RULE_SCREEN_AVAILABLE) {
          addView(
            Button(this@ItemDetailsActivity).apply {
              setText(R.string.item_details_add_rule)
              setOnClickListener {
                startActivity(
                  Intent(this@ItemDetailsActivity, TalkBackPreferencesActivity.TalkBackSubSettings::class.java)
                    .putExtra(BasePreferencesActivity.FRAGMENT_NAME, RULE_SCREEN)
                )
                finish()
              }
            }
          )
        }
        for (section in sections) {
          addView(heading(getString(section.title), 18f).apply { setPadding(dp(16), dp(16), dp(16), dp(4)) })
          section.lines.forEach { addView(line(it)) }
        }
      }
    val root = ScrollView(this).apply { addView(content) }
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
      val bars =
        insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
      WindowInsetsCompat.CONSUMED
    }
    setContentView(root)
  }

  private fun heading(text: String, size: Float): TextView =
    TextView(this).apply {
      this.text = text
      setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
      ViewCompat.setAccessibilityHeading(this, true)
    }

  /** A line of a part. Activating it copies its value. */
  private fun line(line: Pair<String, String>): TextView =
    TextView(this).apply {
      text = ItemInspector.lineText(line)
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
      setPadding(dp(16), dp(8), dp(16), dp(8))
      minHeight = dp(48)
      layoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
      setOnClickListener { copy(it, line.second) }
      ViewCompat.replaceAccessibilityAction(
        this,
        AccessibilityActionCompat.ACTION_CLICK,
        getString(android.R.string.copy),
        null,
      )
    }

  private fun copy(view: View, text: String) {
    copyToClipboard(getString(R.string.script_inspect_clip_label), text)
    view.announceForAccessibility(getString(R.string.item_details_copied))
  }

  private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

  companion object {
    private const val EXTRA_SECTIONS = "sections"

    /** The script builder's screen that adds a rule for the inspected item, in builds with scripts. */
    private const val RULE_SCREEN = "com.google.android.accessibility.talkback.scripting.ScriptRulePickFragment"

    private val RULE_SCREEN_AVAILABLE: Boolean by lazy {
      try {
        Class.forName(RULE_SCREEN)
        true
      } catch (e: ClassNotFoundException) {
        false
      }
    }
    private const val KEY_TITLE = "title"
    private const val KEY_NAMES = "names"
    private const val KEY_VALUES = "values"

    fun intent(context: Context, sections: List<ItemInspector.Section>): Intent =
      Intent(context, ItemDetailsActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        .putParcelableArrayListExtra(
          EXTRA_SECTIONS,
          ArrayList(
            sections.map { section ->
              Bundle().apply {
                putInt(KEY_TITLE, section.title)
                putStringArrayList(KEY_NAMES, ArrayList(section.lines.map { it.first }))
                putStringArrayList(KEY_VALUES, ArrayList(section.lines.map { it.second }))
              }
            }
          ),
        )

    private fun sections(intent: Intent): List<ItemInspector.Section> {
      val bundles =
        intent.extras?.let { BundleCompat.getParcelableArrayList(it, EXTRA_SECTIONS, Bundle::class.java) }
          ?: return emptyList()
      return bundles.map { bundle ->
        val names = bundle.getStringArrayList(KEY_NAMES).orEmpty()
        val values = bundle.getStringArrayList(KEY_VALUES).orEmpty()
        ItemInspector.Section(bundle.getInt(KEY_TITLE), names.zip(values))
      }
    }
  }
}
