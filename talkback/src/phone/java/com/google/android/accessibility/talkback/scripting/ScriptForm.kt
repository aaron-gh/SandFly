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

package com.google.android.accessibility.talkback.scripting

import android.R as AndroidR
import android.content.Context
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.widget.doAfterTextChanged
import org.json.JSONArray
import org.json.JSONObject

class ScriptForm(
  private val context: Context,
  items: JSONArray?,
  private val emit: (item: String, event: String, value: Any?) -> Unit,
  private val close: () -> Unit,
) {
  private val controls = LinkedHashMap<String, Control>()

  val view: View =
    ScrollView(context).apply {
      val column =
        LinearLayout(context).apply {
          orientation = LinearLayout.VERTICAL
          setPadding(dp(PADDING_DP))
        }
      for (spec in items.items().filterIsInstance<JSONObject>()) {
        val control = controlOf(spec)
        controls[control.id] = control
        column.addView(control.view)
        control.update(spec)
      }
      addView(column)
    }

  fun values(): JSONObject =
    JSONObject().apply {
      controls.forEach { (id, control) -> control.value()?.let { put(id, it) } }
    }

  fun update(id: String, props: JSONObject) = controls[id]?.update(props)

  private fun controlOf(spec: JSONObject): Control {
    val id = spec.optString("id")
    return when (val type = spec.optString("type")) {
      "label" -> Label(id)
      "edit" -> Edit(id, spec)
      "switch" -> Switch(id)
      "button" -> Action(id, spec.optBoolean("close"))
      else -> Choices(id, spec.has("value") || spec.optBoolean("notify"))
    }
  }

  private fun dp(value: Int): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        context.resources.displayMetrics,
      )
      .toInt()

  private abstract inner class Control(val id: String) {
    abstract val view: View
    protected var updating = false

    open fun value(): Any? = null

    protected abstract fun apply(props: JSONObject)

    fun update(props: JSONObject) {
      updating = true
      try {
        apply(props)
        if (props.has("enabled")) setEnabled(view, props.optBoolean("enabled", true))
        if (props.has("visible")) view.isVisible = props.optBoolean("visible", true)
      } finally {
        updating = false
      }
    }

    protected fun changed(value: Any?) {
      if (!updating) emit(id, "change", value)
    }

    private fun setEnabled(view: View, enabled: Boolean) {
      view.isEnabled = enabled
      (view as? LinearLayout)?.let { group ->
        (0..<group.childCount).forEach { setEnabled(group.getChildAt(it), enabled) }
      }
    }
  }

  private inner class Label(id: String) : Control(id) {
    override val view = TextView(context).apply { setPadding(0, dp(GAP_DP), 0, dp(GAP_DP)) }

    override fun apply(props: JSONObject) {
      props.text("label")?.let { view.text = it }
    }
  }

  private inner class Edit(id: String, spec: JSONObject) : Control(id) {
    private val label = TextView(context)
    private val field =
      EditText(context).apply {
        this.id = View.generateViewId()
        inputType = inputTypeOf(spec)
        if (spec.optBoolean("notify")) doAfterTextChanged { changed(it?.toString().orEmpty()) }
      }
    override val view =
      LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        label.labelFor = field.id
        addView(label)
        addView(field)
      }

    override fun value(): Any = field.text.toString()

    override fun apply(props: JSONObject) {
      props.text("label")?.let { label.text = it }
      props.text("hint")?.let { field.hint = it }
      props.text("value")?.takeIf { it != field.text.toString() }?.let(field::setText)
    }

    private fun inputTypeOf(spec: JSONObject): Int =
      when {
        spec.optBoolean("number") ->
          InputType.TYPE_CLASS_NUMBER or
            InputType.TYPE_NUMBER_FLAG_DECIMAL or
            InputType.TYPE_NUMBER_FLAG_SIGNED
        spec.optBoolean("password") ->
          InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        spec.optBoolean("multiline") ->
          InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        else -> InputType.TYPE_CLASS_TEXT
      }
  }

  private inner class Switch(id: String) : Control(id) {
    override val view =
      SwitchCompat(context).apply { setOnCheckedChangeListener { _, checked -> changed(checked) } }

    override fun value(): Any = view.isChecked

    override fun apply(props: JSONObject) {
      props.text("label")?.let { view.text = it }
      if (props.has("value")) view.isChecked = props.optBoolean("value")
    }
  }

  private inner class Action(id: String, private val closes: Boolean) : Control(id) {
    override val view =
      Button(context).apply {
        setOnClickListener {
          emit(this@Action.id, "click", null)
          if (closes) close()
        }
      }

    override fun apply(props: JSONObject) {
      props.text("label")?.let { view.text = it }
    }
  }

  private inner class Choices(id: String, private val selectable: Boolean) : Control(id) {
    private val label = TextView(context).apply { isVisible = false }
    private val rows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private var options: List<Pair<String, String>> = emptyList()
    private var selected: String? = null
    override val view =
      LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(GAP_DP), 0, dp(GAP_DP))
        addView(label)
        addView(rows)
      }

    init {
      ViewCompat.setAccessibilityDelegate(
        rows,
        object : AccessibilityDelegateCompat() {
          override fun onInitializeAccessibilityNodeInfo(
            host: View,
            info: AccessibilityNodeInfoCompat,
          ) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            info.setCollectionInfo(
              CollectionInfoCompat.obtain(options.size, 1, false, choiceMode())
            )
          }
        },
      )
    }

    override fun value(): Any? = selected.takeIf { selectable }

    override fun apply(props: JSONObject) {
      props.text("label")?.let {
        label.text = it
        label.isVisible = it.isNotEmpty()
      }
      if (props.has("value")) selected = props.text("value")
      if (props.has("items")) options = props.optJSONArray("items").options()
      if (props.has("items") || props.has("value")) showRows()
    }

    private fun showRows() {
      rows.removeAllViews()
      options.forEachIndexed { index, (value, text) -> rows.addView(row(index, value, text)) }
    }

    private fun row(index: Int, value: String, text: String): View =
      (if (selectable) CheckedTextView(context) else TextView(context)).apply {
        this.text = text
        isFocusable = true
        setPadding(dp(GAP_DP), dp(ROW_DP), dp(GAP_DP), dp(ROW_DP))
        if (this is CheckedTextView) {
          setCheckMarkDrawable(checkMark())
          isChecked = value == selected
          setOnClickListener {
            selected = value
            (0..<rows.childCount).forEach {
              (rows.getChildAt(it) as CheckedTextView).isChecked = it == index
            }
            changed(value)
          }
        }
        ViewCompat.setAccessibilityDelegate(
          this,
          object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
              host: View,
              info: AccessibilityNodeInfoCompat,
            ) {
              super.onInitializeAccessibilityNodeInfo(host, info)
              info.setCollectionItemInfo(
                CollectionItemInfoCompat.obtain(index, 1, 0, 1, false, value == selected)
              )
            }
          },
        )
      }

    private fun choiceMode(): Int =
      if (selectable) CollectionInfoCompat.SELECTION_MODE_SINGLE
      else CollectionInfoCompat.SELECTION_MODE_NONE

    private fun checkMark(): Int =
      TypedValue()
        .also { context.theme.resolveAttribute(AndroidR.attr.listChoiceIndicatorSingle, it, true) }
        .resourceId
  }

  companion object {
    private const val PADDING_DP = 16
    private const val GAP_DP = 8
    private const val ROW_DP = 12
    private val TYPES = setOf("label", "edit", "switch", "button", "list")

    fun check(items: JSONArray?) {
      val ids = HashSet<String>()
      for (item in items.items()) {
        apiCheck(item is JSONObject) { "Each dialog item is an object" }
        val type = (item as JSONObject).optString("type")
        apiCheck(type in TYPES) { "Unknown dialog item type $type. Types: ${TYPES.joinToString()}" }
        apiCheck(ids.add(item.optString("id"))) { "Dialog item ids must be unique" }
      }
    }

    private fun JSONObject.text(key: String): String? =
      if (has(key) && !isNull(key)) get(key).toString() else null
  }
}
