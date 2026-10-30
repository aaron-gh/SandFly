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
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.material.MaterialComponentUtils
import com.google.android.accessibility.utils.widget.DialogUtils
import org.json.JSONArray
import org.json.JSONObject
import com.google.android.accessibility.utils.widget.KeyboardSafeEditText

class ChoiceList(
  context: Context,
  private val options: List<Pair<String, String>>,
  private val selected: String?,
  onPick: (String) -> Unit,
) {
  private var shown = options
  private val list =
    ListView(context).apply {
      choiceMode = ListView.CHOICE_MODE_SINGLE
      setOnItemClickListener { _, _, position, _ -> onPick(shown[position].first) }
    }

  val view: View =
    LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      if (options.size > FILTER_FROM) {
        val filter =
          KeyboardSafeEditText(context).apply {
            hint = context.getString(R.string.script_list_filter)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            isSingleLine = true
            doAfterTextChanged { show(it?.toString().orEmpty()) }
          }
        addView(filter, MATCH_PARENT, WRAP_CONTENT)
      }
      addView(list, MATCH_PARENT, WRAP_CONTENT)
    }

  init {
    show("")
  }

  private fun show(filter: String) {
    val wanted = filter.trim()
    shown =
      options.filter { (value, label) ->
        label.contains(wanted, ignoreCase = true) || value.contains(wanted, ignoreCase = true)
      }
    val labels = shown.map { it.second }
    list.adapter =
      ArrayAdapter(list.context, AndroidR.layout.simple_list_item_single_choice, labels)
    val checked = shown.indexOfFirst { it.first == selected }
    if (checked >= 0) list.setItemChecked(checked, true)
  }

  private companion object {
    const val FILTER_FROM = 10
  }
}

class ScriptDialogs(private val context: Context) {
  private class Open(
    val owner: String,
    val id: Int?,
    val dialog: AlertDialog,
    val form: ScriptForm?,
  )

  private val mainHandler = Handler(Looper.getMainLooper())
  // Dialogs and windows on screen, the top one last. A script's confirm or prompt opens over its
  // own window, which stays open under it.
  private val open = ArrayList<Open>()

  fun choose(
    owner: String,
    title: String,
    options: List<Pair<String, String>>,
    selected: String?,
    done: (String?) -> Unit,
  ) =
    show(owner, null, title, null, cancel(), done) { finish ->
      setView(ChoiceList(context, options, selected, finish).view)
    }

  fun prompt(owner: String, title: String, text: String, hint: String, done: (String?) -> Unit) =
    show(owner, null, title, null, cancel(), done) { finish ->
      val field =
        KeyboardSafeEditText(context).apply {
          setText(text)
          this.hint = hint
          inputType = InputType.TYPE_CLASS_TEXT
          setSelectAllOnFocus(true)
          post { requestFocus() }
        }
      setView(field)
      setPositiveButton(AndroidR.string.ok) { _, _ -> finish(field.text.toString()) }
    }

  fun confirm(
    owner: String,
    title: String,
    message: String,
    ok: String?,
    cancel: String?,
    done: (Boolean) -> Unit,
  ) =
    show<Boolean>(owner, null, title, message, cancel ?: cancel(), { done(it == true) }) { finish ->
      setPositiveButton(ok ?: context.getString(AndroidR.string.ok)) { _, _ -> finish(true) }
    }

  fun alert(owner: String, title: String, message: String, done: () -> Unit) =
    show<Boolean>(owner, null, title, message, null, { done() }) { finish ->
      setPositiveButton(AndroidR.string.ok) { _, _ -> finish(true) }
    }

  fun form(
    owner: String,
    id: Int,
    title: String,
    items: JSONArray?,
    fullScreen: Boolean,
    emit: (JSONObject) -> Unit,
  ) {
    lateinit var form: ScriptForm
    val send = { event: JSONObject -> emit(event.put("dialog", id).put("values", form.values())) }
    val closed = { _: Unit? -> send(jsonObject("event" to "close")) }
    show(owner, id, title, null, null, closed, { form }, fullScreen) { finish ->
      form =
        ScriptForm(
          context,
          items,
          { item, event, value ->
            send(jsonObject("item" to item, "event" to event, "value" to value))
          },
          { finish(Unit) },
        )
      setView(form.view)
    }
  }

  fun update(owner: String, id: Int, item: String, props: JSONObject) = onMain {
    formOf(owner, id)?.update(item, props)
  }

  fun add(owner: String, id: Int, item: JSONObject, after: String?) = onMain {
    formOf(owner, id)?.takeIf { it.size < MAX_ITEMS }?.add(item, after)
  }

  fun remove(owner: String, id: Int, item: String) = onMain { formOf(owner, id)?.remove(item) }

  fun focus(owner: String, id: Int, item: String) = onMain { formOf(owner, id)?.focus(item) }

  fun setTitle(owner: String, id: Int, title: String) = onMain {
    open.firstOrNull { it.owner == owner && it.id == id }?.dialog?.setTitle(title)
  }

  fun close(owner: String? = null, id: Int? = null) = onMain {
    open
      .filter { (owner == null || it.owner == owner) && (id == null || it.id == id) }
      .asReversed()
      .forEach { it.dialog.dismiss() }
  }

  private fun formOf(owner: String, id: Int): ScriptForm? =
    open.firstOrNull { it.owner == owner && it.id == id }?.form

  private fun cancel(): String = context.getString(AndroidR.string.cancel)

  private fun onMain(action: () -> Unit) {
    mainHandler.post(action)
  }

  private fun <T> show(
    owner: String,
    id: Int?,
    title: String,
    message: String?,
    cancel: String?,
    done: (T?) -> Unit,
    form: () -> ScriptForm? = { null },
    fullScreen: Boolean = false,
    build: AlertDialog.Builder.(finish: (T) -> Unit) -> Unit,
  ) = onMain {
    // Scripts that open dialogs without closing them can't fill the screen with windows.
    // Taken out now: its dismiss listener runs later, after other dialogs queued at once.
    if (open.size >= MAX_OPEN) open.removeAt(0).dialog.dismiss()
    var result: T? = null
    lateinit var dialog: AlertDialog
    val builder =
      MaterialComponentUtils.alertDialogBuilder(context).setTitle(title).setMessage(message)
    builder.build { value ->
      result = value
      dialog.dismiss()
    }
    cancel?.let { builder.setNegativeButton(it, null) }
    builder.setOnDismissListener {
      open.removeAll { it.dialog == dialog }
      done(result)
    }
    dialog = builder.create()
    dialog.window?.let(DialogUtils::setWindowTypeToDialog)
    open.add(Open(owner, id, dialog, form()))
    dialog.show()
    if (fullScreen) dialog.window?.setLayout(MATCH_PARENT, MATCH_PARENT)
  }

  private companion object {
    const val MAX_OPEN = 8
    const val MAX_ITEMS = 50
  }
}
