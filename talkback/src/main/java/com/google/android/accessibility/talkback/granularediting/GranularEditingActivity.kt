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

package com.google.android.accessibility.talkback.granularediting

import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.widget.KeyboardSafeEditText

/**
 * The granular editing screen: the text as a list of pieces, split by characters, words,
 * sentences, lines or paragraphs. Double tapping a piece edits it, and its actions insert text
 * before or after it, delete it or select it. Selected pieces can be copied, cut or deleted
 * together. Save writes the text back to the text field it came from.
 */
class GranularEditingActivity : AppCompatActivity() {
  private var text = ""
  private var granularity = Granularity.WORDS
  private var pieces: List<Piece> = emptyList()
  private val selected = HashSet<Int>()
  private val undo = ArrayList<String>()
  private val redo = ArrayList<String>()
  private var lastFind = ""

  private lateinit var list: RecyclerView
  private lateinit var splitBy: Button
  private lateinit var empty: TextView
  private val adapter = PieceAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setTitle(R.string.granular_editing_title)
    text = savedInstanceState?.getString(STATE_TEXT) ?: GranularEditing.text
    savedInstanceState?.getString(STATE_GRANULARITY)?.let { granularity = Granularity.valueOf(it) }

    val title =
      TextView(this).apply {
        setText(R.string.granular_editing_title)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        setPadding(dp(16), dp(12), dp(16), dp(4))
        ViewCompat.setAccessibilityHeading(this, true)
      }
    splitBy = button("") { chooseGranularity() }
    empty =
      TextView(this).apply {
        setText(R.string.granular_editing_empty)
        setPadding(dp(16), dp(16), dp(16), dp(16))
      }
    list =
      RecyclerView(this).apply {
        layoutManager = LinearLayoutManager(this@GranularEditingActivity)
        adapter = this@GranularEditingActivity.adapter
      }
    val buttons =
      LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        if (GranularEditing.isEditable) addView(button(R.string.granular_editing_save) { save() }, weight())
        addView(button(android.R.string.copy) { copy() }, weight())
        addView(button(R.string.granular_editing_more) { showMore(it) }, weight())
      }
    val root =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(title)
        addView(splitBy)
        addView(empty)
        addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        addView(buttons)
      }
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
      val bars =
        insets.getInsets(
          WindowInsetsCompat.Type.systemBars() or
            WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.ime()
        )
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
      WindowInsetsCompat.CONSUMED
    }
    setContentView(root)
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = close()
      },
    )
    refresh()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(STATE_TEXT, text)
    outState.putString(STATE_GRANULARITY, granularity.name)
  }

  /** Splits the text again and shows it, with no piece selected. */
  private fun refresh() {
    pieces = TextPieces.split(text, granularity)
    selected.clear()
    splitBy.text = getString(R.string.granular_editing_split_by, getString(nameOf(granularity)))
    empty.visibility = if (pieces.isEmpty()) View.VISIBLE else View.GONE
    adapter.notifyDataSetChanged()
  }

  /** Makes [newText] the text, keeping the old one for Undo, and focuses the piece at [focusAt]. */
  private fun change(newText: String, focusAt: Int, @StringRes announcement: Int? = null) {
    if (newText == text) return
    undo += text
    if (undo.size > MAX_UNDO) undo.removeAt(0)
    redo.clear()
    text = newText
    refresh()
    focus(focusAt)
    announcement?.let { list.announceForAccessibility(getString(it)) }
  }

  private fun focus(index: Int) {
    if (pieces.isEmpty()) return
    val position = index.coerceIn(0, pieces.size - 1)
    list.scrollToPosition(position)
    list.post {
      list
        .findViewHolderForAdapterPosition(position)
        ?.itemView
        ?.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
    }
  }

  private fun chooseGranularity() {
    val values = Granularity.values()
    AlertDialog.Builder(this)
      .setTitle(R.string.granular_editing_split_by_title)
      .setSingleChoiceItems(
        values.map { getString(nameOf(it)) }.toTypedArray(),
        values.indexOf(granularity),
      ) { dialog, which ->
        granularity = values[which]
        refresh()
        dialog.dismiss()
        focus(0)
      }
      .show()
  }

  private fun edit(index: Int) {
    val piece = pieces.getOrNull(index) ?: return
    ask(R.string.granular_editing_edit, text.substring(piece.coreStart, piece.coreEnd)) { typed ->
      if (typed.isEmpty()) {
        change(TextPieces.delete(text, listOf(piece)), index, R.string.granular_editing_deleted)
      } else {
        change(TextPieces.replace(text, piece, typed), index)
      }
    }
  }

  private fun insert(index: Int, after: Boolean) {
    val piece = pieces.getOrNull(index) ?: return
    ask(if (after) R.string.granular_editing_insert_after else R.string.granular_editing_insert_before, "") { typed ->
      if (typed.isEmpty()) return@ask
      if (after) {
        change(TextPieces.insertAfter(text, piece, typed, granularity), index + 1)
      } else {
        change(TextPieces.insertBefore(text, piece, typed, granularity), index)
      }
    }
  }

  private fun addAtEnd() {
    ask(R.string.granular_editing_add_at_end, "") { typed ->
      if (typed.isNotEmpty()) change(TextPieces.append(text, typed, granularity), Int.MAX_VALUE)
    }
  }

  private fun delete(indexes: Collection<Int>) {
    val chosen = indexes.mapNotNull { pieces.getOrNull(it) }
    if (chosen.isEmpty()) return
    change(TextPieces.delete(text, chosen), indexes.min(), R.string.granular_editing_deleted)
  }

  private fun toggleSelected(index: Int) {
    if (!selected.remove(index)) selected += index
    adapter.notifyItemChanged(index)
  }

  /** Copies the selected pieces, or all of the text when none is selected. */
  private fun copy() {
    val copied = if (selected.isEmpty()) text else TextPieces.join(text, selected.map { pieces[it] })
    GranularEditing.copy(this, copied)
    list.announceForAccessibility(getString(R.string.granular_editing_copied))
  }

  private fun cut() {
    if (selected.isEmpty()) return
    GranularEditing.copy(this, TextPieces.join(text, selected.map { pieces[it] }))
    delete(selected.toList())
  }

  private fun find(query: String, from: Int) {
    lastFind = query
    if (pieces.isEmpty()) return
    for (step in 1..pieces.size) {
      val index = (from + step) % pieces.size
      val piece = pieces[index]
      if (text.substring(piece.coreStart, piece.coreEnd).contains(query, ignoreCase = true)) {
        focus(index)
        return
      }
    }
    list.announceForAccessibility(getString(R.string.granular_editing_not_found))
  }

  private fun focusedIndex(): Int {
    val focused = list.focusedChild ?: list.findFocus()
    val accessibilityFocused =
      (0 until list.childCount).map { list.getChildAt(it) }.firstOrNull { it.isAccessibilityFocused }
    val view = accessibilityFocused ?: focused ?: return -1
    return list.getChildAdapterPosition(view)
  }

  private fun replaceAll() {
    val find = editText(R.string.granular_editing_find, lastFind)
    val replacement = editText(R.string.granular_editing_replace_with, "")
    val fields =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(8), dp(16), 0)
        addView(find)
        addView(replacement)
      }
    AlertDialog.Builder(this)
      .setTitle(R.string.granular_editing_replace)
      .setView(fields)
      .setPositiveButton(R.string.granular_editing_replace_all) { _, _ ->
        val (newText, count) =
          TextPieces.replaceAll(text, find.text.toString(), replacement.text.toString())
        lastFind = find.text.toString()
        if (count > 0) change(newText, 0)
        list.announceForAccessibility(
          resources.getQuantityString(R.plurals.granular_editing_replaced, count, count)
        )
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun undoLast() {
    val previous = undo.removeLastOrNull() ?: return
    redo += text
    text = previous
    refresh()
    focus(0)
  }

  private fun redoLast() {
    val next = redo.removeLastOrNull() ?: return
    undo += text
    text = next
    refresh()
    focus(0)
  }

  private fun showMore(anchor: View) {
    val menu = PopupMenu(this, anchor)
    val actions = ArrayList<() -> Unit>()
    fun add(@StringRes title: Int, action: () -> Unit) {
      menu.menu.add(0, actions.size, actions.size, title)
      actions += action
    }
    if (undo.isNotEmpty()) add(R.string.granular_editing_undo) { undoLast() }
    if (redo.isNotEmpty()) add(R.string.granular_editing_redo) { redoLast() }
    add(R.string.granular_editing_find) {
      ask(R.string.granular_editing_find, lastFind) { typed -> if (typed.isNotEmpty()) find(typed, focusedIndex()) }
    }
    if (lastFind.isNotEmpty()) add(R.string.granular_editing_find_next) { find(lastFind, focusedIndex()) }
    add(R.string.granular_editing_replace) { replaceAll() }
    add(R.string.granular_editing_add_at_end) { addAtEnd() }
    if (pieces.isNotEmpty()) {
      add(android.R.string.selectAll) {
        selected.addAll(pieces.indices)
        adapter.notifyDataSetChanged()
      }
    }
    if (selected.isNotEmpty()) {
      add(R.string.granular_editing_clear_selection) {
        selected.clear()
        adapter.notifyDataSetChanged()
      }
      add(R.string.granular_editing_cut_selected) { cut() }
      add(R.string.granular_editing_delete_selected) { delete(selected.toList()) }
    }
    menu.setOnMenuItemClickListener { item ->
      actions[item.itemId]()
      true
    }
    menu.show()
  }

  private fun save() {
    GranularEditing.save(this, text)
    finish()
  }

  /** Closes the screen, asking first when the text was changed. */
  private fun close() {
    if (text == GranularEditing.text) {
      GranularEditing.close()
      finish()
      return
    }
    val dialog =
      AlertDialog.Builder(this)
        .setTitle(
          if (GranularEditing.isEditable) R.string.granular_editing_save_changes
          else R.string.granular_editing_copy_changes
        )
        .setNegativeButton(R.string.granular_editing_discard) { _, _ ->
          GranularEditing.close()
          finish()
        }
        .setNeutralButton(R.string.granular_editing_keep_editing, null)
    if (GranularEditing.isEditable) {
      dialog.setPositiveButton(R.string.granular_editing_save) { _, _ -> save() }
    } else {
      dialog.setPositiveButton(android.R.string.copy) { _, _ ->
        GranularEditing.copy(this, text)
        finish()
      }
    }
    dialog.show()
  }

  /** Asks for text in a dialog titled [title], starting with [initial], and calls [onDone] with it. */
  private fun ask(@StringRes title: Int, initial: String, onDone: (String) -> Unit) {
    val field = editText(title, initial)
    val box =
      LinearLayout(this).apply {
        setPadding(dp(16), dp(8), dp(16), 0)
        addView(field, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
      }
    AlertDialog.Builder(this)
      .setTitle(title)
      .setView(box)
      .setPositiveButton(android.R.string.ok) { _, _ -> onDone(field.text.toString()) }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
    field.requestFocus()
  }

  private fun editText(@StringRes hint: Int, initial: String): EditText =
    KeyboardSafeEditText(this).apply {
      setHint(hint)
      // No automatic capitals: the text often goes in the middle of a sentence.
      inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
      setText(initial)
      setSelection(initial.length)
    }

  /** What a piece is shown as: its text, or a name for white space, which has no text to read. */
  private fun label(piece: Piece): String {
    val shown = text.substring(piece.coreStart, piece.coreEnd)
    return when {
      shown == "\n" -> getString(R.string.granular_editing_new_line)
      shown == "\t" -> getString(R.string.granular_editing_tab)
      shown.isBlank() -> getString(R.string.granular_editing_space)
      else -> shown
    }
  }

  private inner class PieceAdapter : RecyclerView.Adapter<PieceHolder>() {
    override fun getItemCount(): Int = pieces.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PieceHolder =
      PieceHolder(
        TextView(parent.context).apply {
          layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
          minHeight = dp(48)
          gravity = Gravity.CENTER_VERTICAL
          setPadding(dp(16), dp(8), dp(16), dp(8))
          setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
          isClickable = true
          isLongClickable = true
          isFocusable = true
          val outValue = TypedValue()
          context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
          setBackgroundResource(outValue.resourceId)
        }
      )

    override fun onBindViewHolder(holder: PieceHolder, position: Int) {
      val view = holder.itemView as TextView
      val isSelected = position in selected
      view.text = label(pieces[position])
      view.isSelected = isSelected
      ViewCompat.setStateDescription(
        view,
        if (isSelected) getString(R.string.granular_editing_selected) else null,
      )
      view.setOnClickListener { edit(holder.bindingAdapterPosition) }
      view.setOnLongClickListener {
        showPieceActions(holder.bindingAdapterPosition)
        true
      }
      ViewCompat.replaceAccessibilityAction(
        view,
        AccessibilityActionCompat.ACTION_CLICK,
        getString(R.string.granular_editing_edit),
        null,
      )
      ViewCompat.replaceAccessibilityAction(
        view,
        AccessibilityActionCompat.ACTION_LONG_CLICK,
        getString(R.string.granular_editing_more),
        null,
      )
      holder.actionIds.forEach { ViewCompat.removeAccessibilityAction(view, it) }
      holder.actionIds.clear()
      pieceActions(isSelected).forEach { (title, action) ->
        holder.actionIds +=
          ViewCompat.addAccessibilityAction(view, getString(title)) { _, _ ->
            action(holder.bindingAdapterPosition)
            true
          }
      }
    }
  }

  private class PieceHolder(view: View) : RecyclerView.ViewHolder(view) {
    val actionIds = ArrayList<Int>()
  }

  /** What can be done with a piece, besides editing it, which double tapping does. */
  private fun pieceActions(isSelected: Boolean): List<Pair<Int, (Int) -> Unit>> =
    listOf(
      (if (isSelected) R.string.granular_editing_unselect else R.string.granular_editing_select) to
        { index: Int -> toggleSelected(index) },
      R.string.granular_editing_insert_before to { index: Int -> insert(index, after = false) },
      R.string.granular_editing_insert_after to { index: Int -> insert(index, after = true) },
      R.string.granular_editing_delete to { index: Int -> delete(listOf(index)) },
    )

  private fun showPieceActions(index: Int) {
    if (index !in pieces.indices) return
    val actions = listOf(R.string.granular_editing_edit to { i: Int -> edit(i) }) + pieceActions(index in selected)
    AlertDialog.Builder(this)
      .setTitle(label(pieces[index]))
      .setItems(actions.map { getString(it.first) }.toTypedArray()) { _, which -> actions[which].second(index) }
      .show()
  }

  private fun button(@StringRes title: Int, onClick: (View) -> Unit): Button =
    button(getString(title), onClick)

  private fun button(title: String, onClick: (View) -> Unit): Button =
    Button(this).apply {
      text = title
      isAllCaps = false
      setOnClickListener { onClick(it) }
    }

  private fun weight() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

  private fun dp(value: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

  companion object {
    private const val STATE_TEXT = "text"
    private const val STATE_GRANULARITY = "granularity"
    private const val MAX_UNDO = 100

    @StringRes
    fun nameOf(granularity: Granularity): Int =
      when (granularity) {
        Granularity.CHARACTERS -> R.string.granular_editing_characters
        Granularity.WORDS -> R.string.granular_editing_words
        Granularity.SENTENCES -> R.string.granular_editing_sentences
        Granularity.LINES -> R.string.granular_editing_lines
        Granularity.PARAGRAPHS -> R.string.granular_editing_paragraphs
      }
  }
}
