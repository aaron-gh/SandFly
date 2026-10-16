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

package com.google.android.accessibility.talkback.preference.base

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.core.view.children
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils

class ItemDetailsOrderFragment : TalkbackBaseFragment() {
  private lateinit var details: MutableList<ItemDetail>
  private val adapter = DetailsAdapter()

  public override fun getTitle(): CharSequence = getText(R.string.pref_node_desc_order_title)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    val context = requireContext()
    details = detailsOf(orderValues(context)[savedOrderIndex(context)]).toMutableList()
    preferenceScreen = preferenceManager.createPreferenceScreen(context)
  }

  override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> =
    adapter

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    ItemTouchHelper(DragCallback()).attachToRecyclerView(listView)
  }

  private fun move(from: Int, to: Int): Boolean {
    if (from !in details.indices || to !in details.indices) {
      return false
    }
    details.add(to, details.removeAt(from))
    adapter.notifyItemMoved(from, to)
    val list = listView
    list?.children?.forEach { (list.getChildViewHolder(it) as? DetailViewHolder)?.bindMoveActions() }
    return true
  }

  private fun save() {
    val context = requireContext()
    val value = orderValues(context).first { detailsOf(it) == details }
    SharedPreferencesUtils.getSharedPreferences(context).edit {
      putString(context.getString(R.string.pref_node_desc_order_key), value)
    }
  }

  private fun announcePosition(holder: RecyclerView.ViewHolder, @StringRes event: Int? = null) {
    val index = holder.bindingAdapterPosition
    if (index !in details.indices) {
      return
    }
    val position = positionText(holder.itemView, index, details.size)
    announce(
      holder.itemView,
      if (event == null) position else getString(event, getString(details[index].title), position),
    )
  }

  private inner class DetailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun bind(detail: ItemDetail) {
      itemView.findViewById<TextView>(android.R.id.title).setText(detail.title)
      bindMoveActions()
    }

    fun bindMoveActions() =
      setMoveActions(itemView, bindingAdapterPosition, details.size) { offset ->
        val from = bindingAdapterPosition
        move(from, from + offset).also {
          if (it) {
            save()
            announcePosition(this)
          }
        }
      }
  }

  private inner class DetailsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemCount(): Int = details.size + 1

    override fun getItemViewType(position: Int): Int =
      if (position < details.size) R.layout.item_details_order_row
      else R.layout.item_details_order_footer

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
      val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
      return if (viewType == R.layout.item_details_order_row) DetailViewHolder(view)
      else object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
      (holder as? DetailViewHolder)?.bind(details[position])
    }
  }

  private inner class DragCallback : ItemTouchHelper.Callback() {
    override fun getMovementFlags(
      recyclerView: RecyclerView,
      viewHolder: RecyclerView.ViewHolder,
    ): Int =
      makeMovementFlags(
        if (viewHolder is DetailViewHolder) ItemTouchHelper.UP or ItemTouchHelper.DOWN else 0,
        0,
      )

    override fun onMove(
      recyclerView: RecyclerView,
      viewHolder: RecyclerView.ViewHolder,
      target: RecyclerView.ViewHolder,
    ): Boolean =
      move(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition).also {
        if (it) {
          announcePosition(viewHolder)
        }
      }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
      super.onSelectedChanged(viewHolder, actionState)
      if (viewHolder != null && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        announcePosition(viewHolder, R.string.template_item_details_order_picked_up)
      }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
      super.clearView(recyclerView, viewHolder)
      save()
      announcePosition(viewHolder, R.string.template_item_details_order_dropped)
    }
  }

  private enum class ItemDetail(val token: String, @StringRes val title: Int) {
    NAME("name", R.string.item_detail_name),
    TYPE("role", R.string.item_detail_type),
    STATE("state", R.string.item_detail_state),
  }

  companion object {
    private fun orderValues(context: Context): Array<String> =
      context.resources.getStringArray(R.array.pref_node_desc_order_values)

    private fun savedOrderIndex(context: Context): Int {
      val values = orderValues(context)
      val default = context.getString(R.string.pref_node_desc_order_default)
      val saved =
        SharedPreferencesUtils.getSharedPreferences(context)
          .getString(context.getString(R.string.pref_node_desc_order_key), default)
      return values.indexOf(saved).takeIf { it >= 0 } ?: values.indexOf(default)
    }

    private fun detailsOf(value: String): List<ItemDetail> =
      value.substringAfter("_value_").removeSuffix("_pos").split('_').map { token ->
        ItemDetail.entries.first { it.token == token }
      }

    @JvmStatic
    fun getSummary(context: Context): CharSequence {
      val entries = context.resources.getStringArray(R.array.pref_node_desc_order_entries)
      return entries[savedOrderIndex(context)]
    }
  }
}
