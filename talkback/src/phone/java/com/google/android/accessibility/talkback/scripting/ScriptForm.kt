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
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
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
import com.google.android.accessibility.utils.widget.KeyboardSafeEditText
import kotlin.math.roundToInt
import org.json.JSONObject

class ScriptForm(
  private val context: Context,
  items: JSONArray?,
  private val emit: (item: String, event: String, value: Any?) -> Unit,
  private val close: () -> Unit,
) {
  private val controls = LinkedHashMap<String, Control>()
  private val column =
    LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(dp(PADDING_DP))
    }

  val view: View =
    ScrollView(context).apply {
      for (spec in items.items().filterIsInstance<JSONObject>()) {
        add(spec, after = null)
      }
      addView(column)
    }

  val size: Int
    get() = controls.size

  fun values(): JSONObject =
    JSONObject().apply {
      controls.forEach { (id, control) -> control.value()?.let { put(id, it) } }
    }

  fun update(id: String, props: JSONObject) = controls[id]?.update(props)

  /** Adds an item after the one with id [after], or at the end. Ignores an id already used. */
  fun add(spec: JSONObject, after: String?) {
    val control = controlOf(spec)
    if (control.id in controls) return
    val index = after?.let { controls[it] }?.let { column.indexOfChild(it.view) + 1 } ?: -1
    controls[control.id] = control
    column.addView(control.view, index)
    control.update(spec)
  }

  fun remove(id: String) {
    controls.remove(id)?.let { column.removeView(it.view) }
  }

  /** Moves SandFly's focus to the item, or to its first row. */
  fun focus(id: String) {
    val target = controls[id]?.focusTarget() ?: return
    target.post {
      target.performAccessibilityAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, null)
    }
  }

  private fun controlOf(spec: JSONObject): Control {
    val id = spec.optString("id")
    return when (val type = spec.optString("type")) {
      "label" -> Label(id, spec)
      "heading" -> Label(id, spec, heading = true)
      "items" -> Rows(id)
      "slider" -> Slider(id)
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

    open fun focusTarget(): View = view

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

  private inner class Label(id: String, spec: JSONObject, heading: Boolean = false) : Control(id) {
    override val view =
      TextView(context).apply {
        setPadding(0, dp(GAP_DP), 0, dp(GAP_DP))
        if (heading) {
          ViewCompat.setAccessibilityHeading(this, true)
          setTextAppearance(AndroidR.style.TextAppearance_Medium)
        }
        // Text to read by character, word or line, and to select and copy.
        if (spec.optBoolean("selectable")) setTextIsSelectable(true)
      }

    override fun apply(props: JSONObject) {
      props.text("label")?.let { view.text = it }
    }
  }

  private inner class Edit(id: String, spec: JSONObject) : Control(id) {
    private val label = TextView(context)
    private val field =
      KeyboardSafeEditText(context).apply {
        this.id = View.generateViewId()
        inputType = inputTypeOf(spec)
        if (spec.optBoolean("readOnly")) {
          // Text to read and select, not to change.
          showSoftInputOnFocus = false
          keyListener = null
          setTextIsSelectable(true)
        }
        if (spec.optBoolean("notify")) doAfterTextChanged { changed(it?.toString().orEmpty()) }
        if (spec.optBoolean("submit")) {
          if (!spec.optBoolean("multiline")) imeOptions = EditorInfo.IME_ACTION_DONE
          setOnEditorActionListener { _, _, _ ->
            emit(this@Edit.id, "submit", text.toString())
            true
          }
        }
      }
    override val view =
      LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        label.labelFor = field.id
        addView(label)
        addView(field)
      }

    override fun value(): Any = field.text.toString()

    override fun focusTarget(): View = field

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

  /** A slider from min to max in steps. */
  private inner class Slider(id: String) : Control(id) {
    private val label = TextView(context)
    private var min = 0.0
    private var max = 100.0
    private var step = 1.0
    private val bar =
      SeekBar(context).apply {
        this.id = View.generateViewId()
        setOnSeekBarChangeListener(
          object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
              if (fromUser) changed(value())
            }

            override fun onStartTrackingTouch(bar: SeekBar) {}

            override fun onStopTrackingTouch(bar: SeekBar) {}
          }
        )
      }
    override val view =
      LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(GAP_DP), 0, dp(GAP_DP))
        label.labelFor = bar.id
        addView(label)
        addView(bar)
      }

    override fun value(): Any = min + bar.progress * step

    override fun focusTarget(): View = bar

    override fun apply(props: JSONObject) {
      props.text("label")?.let { label.text = it }
      val current = value() as Double
      // optDouble reads "NaN" and "Infinity", and roundToInt throws on them.
      if (props.has("min")) min = props.optDouble("min", min).takeIf { it.isFinite() } ?: min
      if (props.has("max")) max = props.optDouble("max", max).takeIf { it.isFinite() } ?: max
      if (props.has("step")) {
        step = props.optDouble("step", step).takeIf { it > 0 && it.isFinite() } ?: 1.0
      }
      bar.max = ((max - min) / step).toInt().coerceIn(1, MAX_STEPS)
      bar.keyProgressIncrement = 1
      val wanted =
        props.optDouble("value", current).takeIf { props.has("value") && it.isFinite() } ?: current
      bar.progress = ((wanted - min) / step).roundToInt().coerceIn(0, bar.max)
    }
  }

  /**
   * Rows to activate, such as the notes of a notepad. Double-tapping a row sends its value, and
   * the row's actions are in SandFly's Actions menu.
   */
  private inner class Rows(id: String) : Control(id) {
    private val label = TextView(context).apply { isVisible = false }
    private val rows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private var options: List<Pair<String, String>> = emptyList()
    private var actions: List<String> = emptyList()
    private var empty = ""
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
              CollectionInfoCompat.obtain(
                options.size,
                1,
                false,
                CollectionInfoCompat.SELECTION_MODE_NONE,
              )
            )
          }
        },
      )
    }

    override fun focusTarget(): View = rows.getChildAt(0) ?: view

    override fun apply(props: JSONObject) {
      props.text("label")?.let {
        label.text = it
        label.isVisible = it.isNotEmpty()
      }
      props.text("empty")?.let { empty = it }
      if (props.has("actions")) actions = props.optJSONArray("actions").strings()
      if (props.has("items")) options = props.optJSONArray("items").options()
      if (props.has("items") || props.has("actions") || props.has("empty")) showRows()
    }

    private fun showRows() {
      rows.removeAllViews()
      if (options.isEmpty() && empty.isNotEmpty()) {
        rows.addView(TextView(context).apply { text = empty })
        return
      }
      options.forEachIndexed { index, (value, text) -> rows.addView(row(index, value, text)) }
    }

    private fun row(index: Int, value: String, text: String): View =
      TextView(context).apply {
        this.text = text
        isFocusable = true
        setPadding(dp(GAP_DP), dp(ROW_DP), dp(GAP_DP), dp(ROW_DP))
        setBackgroundResource(
          TypedValue()
            .also {
              context.theme.resolveAttribute(AndroidR.attr.selectableItemBackground, it, true)
            }
            .resourceId
        )
        setOnClickListener { emit(this@Rows.id, "select", value) }
        setOnLongClickListener {
          emit(this@Rows.id, "longPress", value)
          true
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
                CollectionItemInfoCompat.obtain(index, 1, 0, 1, false, false)
              )
            }
          },
        )        // After the delegate, which would otherwise replace them.
        for (action in actions) {
          ViewCompat.addAccessibilityAction(this, action) { _, _ ->
            emit(this@Rows.id, "action", jsonObject("value" to value, "action" to action))
            true
          }
        }
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
    private const val MAX_STEPS = 10_000
    private const val MAX_ROWS = 500
    private val TYPES = setOf("label", "heading", "edit", "switch", "button", "list", "items", "slider")

    fun check(items: JSONArray?) {
      val ids = HashSet<String>()
      for (item in items.items()) {
        checkItem(item)
        apiCheck(ids.add((item as JSONObject).optString("id"))) { "Dialog item ids must be unique" }
      }
    }

    fun checkItem(item: Any?) {
      apiCheck(item is JSONObject) { "Each dialog item is an object" }
      val type = (item as JSONObject).optString("type")
      apiCheck(type in TYPES) { "Unknown dialog item type $type. Types: ${TYPES.joinToString()}" }
      apiCheck(item.optJSONArray("items").items().size <= MAX_ROWS) {
        "A list has at most $MAX_ROWS items"
      }
    }

    private fun JSONObject.text(key: String): String? =
      if (has(key) && !isNull(key)) get(key).toString() else null
  }
}
