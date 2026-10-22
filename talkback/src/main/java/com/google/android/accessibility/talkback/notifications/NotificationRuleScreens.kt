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

package com.google.android.accessibility.talkback.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.concurrent.Executors

/** Notifications > Rules: rules that change how notifications are read in some conditions. */
class NotificationRulesFragment : NotificationCodeFragment(R.string.notification_rules_title) {
  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    screen.action(getString(R.string.notification_rule_add)) {
      askText(getString(R.string.notification_rule_add), hint = getString(R.string.notification_rule_name)) { name, _, _ ->
        val rule = NotificationRule(name = name.ifEmpty { getString(R.string.notification_rule_untitled) })
        save(settings.rules + rule)
        openRule(rule.id)
        null
      }
    }
    screen.action(getString(R.string.notification_rule_from_template)) {
      val templates = templates(context)
      choose(getString(R.string.notification_rule_from_template), templates.map { it.name }) {
        val rule = templates[it]
        save(settings.rules + rule)
        openRule(rule.id)
      }
    }
    val list = screen.category(R.string.notification_rules_category)
    val rules = settings.rules
    if (rules.isEmpty()) list.action(getString(R.string.notification_rules_empty)) {}
    for (rule in rules) {
      val summary =
        listOf(
            getString(if (rule.enabled) R.string.notification_on else R.string.notification_off),
            describeRule(context, rule),
          )
          .joinToString(". ")
      list.link(rule.name.ifEmpty { getString(R.string.notification_rule_untitled) }, summary, NotificationRuleFragment::class.java) {
        putString(NotificationRuleFragment.ARG_RULE, rule.id)
      }
    }
  }

  /** Opens the rule's screen as its entry in the list does. */
  private fun openRule(id: String) {
    val pref = androidx.preference.Preference(requireContext())
    pref.fragment = NotificationRuleFragment::class.java.name
    pref.extras.putString(NotificationRuleFragment.ARG_RULE, id)
    onPreferenceTreeClick(pref)
  }

  private fun save(rules: List<NotificationRule>) {
    prefs.edit().putString(NotificationSettings.Companion.Keys.RULES, NotificationRule.listToJson(rules)).apply()
    rebuild()
  }

  private fun templates(context: Context): List<NotificationRule> =
    listOf(
      NotificationRule(
        name = context.getString(R.string.notification_template_headphones),
        conditions = listOf(RuleCondition(RuleCondition.Type.WIRED_HEADPHONES, negate = true), RuleCondition(RuleCondition.Type.BLUETOOTH_DEVICE, negate = true)),
        actions = listOf(RuleAction(RuleAction.Type.SKIP)),
      ),
      NotificationRule(
        name = context.getString(R.string.notification_template_night),
        conditions = listOf(RuleCondition(RuleCondition.Type.TIME, startMinutes = 22 * 60, endMinutes = 7 * 60)),
        actions = listOf(RuleAction(RuleAction.Type.SKIP)),
      ),
      NotificationRule(
        name = context.getString(R.string.notification_template_screen_off),
        conditions = listOf(RuleCondition(RuleCondition.Type.SCREEN_ON)),
        actions = listOf(RuleAction(RuleAction.Type.SKIP)),
      ),
      NotificationRule(
        name = context.getString(R.string.notification_template_locked_private),
        conditions = listOf(RuleCondition(RuleCondition.Type.LOCKED)),
        actions = listOf(RuleAction(RuleAction.Type.PRIVATE)),
      ),
      NotificationRule(
        name = context.getString(R.string.notification_template_short_on_battery),
        conditions = listOf(RuleCondition(RuleCondition.Type.CHARGING, negate = true)),
        actions = listOf(RuleAction(RuleAction.Type.MAX_WORDS, number = 15)),
      ),
    )

  companion object {
    fun describeRule(context: Context, rule: NotificationRule): String {
      val conditions =
        if (rule.conditions.isEmpty()) context.getString(R.string.notification_rule_always)
        else rule.conditions.joinToString(
          context.getString(if (rule.matchAll) R.string.notification_rule_and else R.string.notification_rule_or)
        ) { describeCondition(context, it) }
      val actions =
        if (rule.actions.isEmpty()) context.getString(R.string.notification_rule_no_actions)
        else rule.actions.joinToString(", ") { describeAction(context, it) }
      return context.getString(R.string.notification_rule_summary, conditions, actions)
    }

    fun describeCondition(context: Context, c: RuleCondition): String {
      fun s(@StringRes yes: Int, @StringRes no: Int, vararg args: Any) =
        context.getString(if (c.negate) no else yes, *args)
      val what = c.label.ifEmpty { c.value }
      return when (c.type) {
        RuleCondition.Type.BLUETOOTH_DEVICE ->
          if (c.value.isEmpty()) s(R.string.notification_cond_bluetooth_any, R.string.notification_cond_bluetooth_any_not)
          else s(R.string.notification_cond_bluetooth, R.string.notification_cond_bluetooth_not, what)
        RuleCondition.Type.WIRED_HEADPHONES -> s(R.string.notification_cond_wired, R.string.notification_cond_wired_not)
        RuleCondition.Type.BATTERY_BELOW -> s(R.string.notification_cond_battery_below, R.string.notification_cond_battery_below_not, c.number)
        RuleCondition.Type.CHARGING -> s(R.string.notification_cond_charging, R.string.notification_cond_charging_not)
        RuleCondition.Type.LOCKED -> s(R.string.notification_cond_locked, R.string.notification_cond_locked_not)
        RuleCondition.Type.SCREEN_ON -> s(R.string.notification_cond_screen_on, R.string.notification_cond_screen_on_not)
        RuleCondition.Type.LANDSCAPE -> s(R.string.notification_cond_landscape, R.string.notification_cond_landscape_not)
        RuleCondition.Type.TIME -> {
          val range =
            context.getString(
              R.string.notification_time_range,
              NotificationXmlFragment.timeText(context, c.startMinutes),
              NotificationXmlFragment.timeText(context, c.endMinutes),
            )
          val days = if (c.days.isEmpty()) "" else " " + daysText(c.days)
          s(R.string.notification_cond_time, R.string.notification_cond_time_not, range + days)
        }
        RuleCondition.Type.WIFI_NETWORK ->
          if (c.value.isEmpty()) s(R.string.notification_cond_wifi_any, R.string.notification_cond_wifi_any_not)
          else s(R.string.notification_cond_wifi, R.string.notification_cond_wifi_not, what)
        RuleCondition.Type.FOREGROUND_APP -> s(R.string.notification_cond_foreground, R.string.notification_cond_foreground_not, what)
        RuleCondition.Type.FROM_APP -> s(R.string.notification_cond_from, R.string.notification_cond_from_not, what)
        RuleCondition.Type.CONTAINS -> s(R.string.notification_cond_contains, R.string.notification_cond_contains_not, c.value)
        RuleCondition.Type.RINGER_SILENT -> s(R.string.notification_cond_silent, R.string.notification_cond_silent_not)
        RuleCondition.Type.MEDIA_PLAYING -> s(R.string.notification_cond_media, R.string.notification_cond_media_not)
      }
    }

    fun describeAction(context: Context, a: RuleAction): String =
      when (a.type) {
        RuleAction.Type.SKIP -> context.getString(R.string.notification_action_skip)
        RuleAction.Type.PRIVATE -> context.getString(R.string.notification_action_private)
        RuleAction.Type.NOT_PRIVATE -> context.getString(R.string.notification_action_not_private)
        RuleAction.Type.FORMAT -> context.getString(R.string.notification_action_format_value, a.value)
        RuleAction.Type.MAX_WORDS ->
          context.getString(R.string.notification_action_max_words_value, context.resources.getQuantityString(R.plurals.notification_words, a.number, a.number))
        RuleAction.Type.NO_LIMIT -> context.getString(R.string.notification_action_no_limit)
        RuleAction.Type.KEEP_EMOJI -> context.getString(R.string.notification_action_keep_emoji)
        RuleAction.Type.KEEP_NUMBERS -> context.getString(R.string.notification_action_keep_numbers)
        RuleAction.Type.PRIORITY -> context.getString(R.string.notification_action_priority)
        RuleAction.Type.ALWAYS_READ -> context.getString(R.string.notification_action_always_read)
        RuleAction.Type.VOICE ->
          if (a.value.isEmpty()) context.getString(R.string.notification_action_voice_sandfly)
          else
            context.getString(
              R.string.notification_action_voice_value,
              voiceProfileNames(context)[a.value] ?: context.getString(R.string.notification_voice_removed),
            )
      }

    private fun daysText(days: Set<Int>): String {
      // Monday is 1 here, as Calendar's Sunday is 1.
      val names = DateFormatSymbols.getInstance().shortWeekdays
      return days.sorted().joinToString(", ") { names[it % 7 + 1] }
    }
  }
}

/** One rule: its name, whether it is on, its conditions and its actions. */
class NotificationRuleFragment : NotificationCodeFragment(R.string.notification_rules_title) {
  private val ruleId: String by lazy { requireArguments().getString(ARG_RULE).orEmpty() }
  private var afterPermission: (() -> Unit)? = null

  private val permissions =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
      if (granted.values.all { it }) afterPermission?.invoke()
      else toast(R.string.notification_permission_needed)
      afterPermission = null
    }

  private val rule: NotificationRule?
    get() = settings.rules.firstOrNull { it.id == ruleId }

  override fun getTitle(): CharSequence = rule?.name?.ifEmpty { null } ?: getString(R.string.notification_rules_title)

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val rule = rule ?: return
    screen.action(getString(R.string.notification_rule_name), rule.name) {
      askText(getString(R.string.notification_rule_name), rule.name) { name, _, _ ->
        update { it.copy(name = name) }
        activity?.title = name
        null
      }
    }
    screen.switch(getString(R.string.notification_rule_enabled), rule.enabled) { on -> update { it.copy(enabled = on) } }
    val matchChoices = listOf(getString(R.string.notification_rule_match_all), getString(R.string.notification_rule_match_any))
    screen.action(getString(R.string.notification_rule_match), matchChoices[if (rule.matchAll) 0 else 1]) {
      choose(getString(R.string.notification_rule_match), matchChoices, if (rule.matchAll) 0 else 1) { index ->
        update { it.copy(matchAll = index == 0) }
      }
    }
    val conditions = screen.category(R.string.notification_rule_conditions)
    conditions.action(getString(R.string.notification_rule_add_condition)) { addCondition() }
    rule.conditions.forEachIndexed { index, condition ->
      val text = NotificationRulesFragment.describeCondition(context, condition)
      conditions.action(text) {
        choose(text, listOf(getString(R.string.notification_rule_invert), getString(R.string.notification_remove))) { choice ->
          update {
            if (choice == 0) it.copy(conditions = it.conditions.mapIndexed { i, c -> if (i == index) c.copy(negate = !c.negate) else c })
            else it.copy(conditions = it.conditions.filterIndexed { i, _ -> i != index })
          }
        }
      }
    }
    val actions = screen.category(R.string.notification_rule_actions)
    actions.action(getString(R.string.notification_rule_add_action)) { addAction() }
    rule.actions.forEachIndexed { index, action ->
      val text = NotificationRulesFragment.describeAction(context, action)
      actions.action(text) {
        choose(text, listOf(getString(R.string.notification_remove))) {
          update { it.copy(actions = it.actions.filterIndexed { i, _ -> i != index }) }
        }
      }
    }
    screen.action(getString(R.string.notification_rule_delete)) {
      confirm(getString(R.string.notification_rule_delete_confirm, rule.name), R.string.notification_remove) {
        prefs.edit().putString(
          NotificationSettings.Companion.Keys.RULES,
          NotificationRule.listToJson(settings.rules.filter { it.id != ruleId }),
        ).apply()
        parentFragmentManager.popBackStack()
      }
    }
  }

  private fun update(change: (NotificationRule) -> NotificationRule) {
    val rules = settings.rules.map { if (it.id == ruleId) change(it) else it }
    prefs.edit().putString(NotificationSettings.Companion.Keys.RULES, NotificationRule.listToJson(rules)).apply()
    rebuild()
  }

  private fun addCondition(condition: RuleCondition) = update { it.copy(conditions = it.conditions + condition) }

  private fun addCondition() {
    val types =
      listOf(
        RuleCondition.Type.BLUETOOTH_DEVICE to R.string.notification_type_bluetooth,
        RuleCondition.Type.WIRED_HEADPHONES to R.string.notification_type_wired,
        RuleCondition.Type.WIFI_NETWORK to R.string.notification_type_wifi,
        RuleCondition.Type.TIME to R.string.notification_type_time,
        RuleCondition.Type.SCREEN_ON to R.string.notification_type_screen,
        RuleCondition.Type.LOCKED to R.string.notification_type_locked,
        RuleCondition.Type.FROM_APP to R.string.notification_type_from_app,
        RuleCondition.Type.FOREGROUND_APP to R.string.notification_type_foreground,
        RuleCondition.Type.CONTAINS to R.string.notification_type_contains,
        RuleCondition.Type.BATTERY_BELOW to R.string.notification_type_battery,
        RuleCondition.Type.CHARGING to R.string.notification_type_charging,
        RuleCondition.Type.LANDSCAPE to R.string.notification_type_landscape,
        RuleCondition.Type.RINGER_SILENT to R.string.notification_type_silent,
        RuleCondition.Type.MEDIA_PLAYING to R.string.notification_type_media,
      )
    choose(getString(R.string.notification_rule_add_condition), types.map { getString(it.second) }) { index ->
      when (val type = types[index].first) {
        RuleCondition.Type.BLUETOOTH_DEVICE -> withPermissions(bluetoothPermissions()) { chooseBluetooth() }
        RuleCondition.Type.WIFI_NETWORK ->
          withPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            val current = DeviceStateReader(requireContext()).read().wifiName
            askText(getString(R.string.notification_type_wifi), current, getString(R.string.notification_wifi_hint)) { name, _, _ ->
              addCondition(RuleCondition(type, value = name))
              null
            }
          }
        RuleCondition.Type.TIME -> chooseTime { start, end ->
          chooseDays { days -> addCondition(RuleCondition(type, startMinutes = start, endMinutes = end, days = days)) }
        }
        RuleCondition.Type.FROM_APP, RuleCondition.Type.FOREGROUND_APP -> chooseApp { packageName, label ->
          addCondition(RuleCondition(type, value = packageName, label = label))
        }
        RuleCondition.Type.CONTAINS ->
          askText(getString(R.string.notification_type_contains), checkLabels = listOf(getString(R.string.notification_regex))) { text, _, checks ->
            if (text.isEmpty()) return@askText getString(R.string.notification_enter_text)
            if (checks[0] && !TextMatch.isValidRegex(text)) return@askText getString(R.string.notification_invalid_regex)
            addCondition(RuleCondition(type, value = text, number = if (checks[0]) 1 else 0))
            null
          }
        RuleCondition.Type.BATTERY_BELOW -> {
          val levels = (1..19).map { it * 5 }
          choose(getString(R.string.notification_type_battery), levels.map { getString(R.string.notification_percent, it) }) {
            addCondition(RuleCondition(type, number = levels[it]))
          }
        }
        else -> addCondition(RuleCondition(type))
      }
    }
  }

  private fun bluetoothPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(Manifest.permission.BLUETOOTH_CONNECT) else emptyArray()

  private fun withPermissions(list: Array<String>, then: () -> Unit) {
    val context = requireContext()
    val missing = list.filter { context.checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
    if (missing.isEmpty()) {
      then()
    } else {
      afterPermission = then
      permissions.launch(missing.toTypedArray())
    }
  }

  @SuppressLint("MissingPermission")
  private fun chooseBluetooth() {
    val manager = requireContext().getSystemService(BluetoothManager::class.java)
    val devices =
      try {
        manager?.adapter?.bondedDevices?.toList().orEmpty()
      } catch (e: SecurityException) {
        emptyList()
      }.map { (it.name ?: it.address) to it.address.uppercase() }.sortedBy { it.first.lowercase() }
    val items = listOf(getString(R.string.notification_bluetooth_any)) + devices.map { it.first }
    choose(getString(R.string.notification_type_bluetooth), items) { index ->
      if (index == 0) addCondition(RuleCondition(RuleCondition.Type.BLUETOOTH_DEVICE))
      else devices[index - 1].let { (name, address) ->
        addCondition(RuleCondition(RuleCondition.Type.BLUETOOTH_DEVICE, value = address, label = name))
      }
    }
  }

  private fun chooseTime(then: (Int, Int) -> Unit) {
    val context = requireContext()
    val is24 = DateFormat.is24HourFormat(context)
    TimePickerDialog(context, { _, startHour, startMinute ->
      TimePickerDialog(context, { _, endHour, endMinute ->
        then(startHour * 60 + startMinute, endHour * 60 + endMinute)
      }, 7, 0, is24).apply { setTitle(R.string.notification_time_end) }.show()
    }, 22, 0, is24).apply { setTitle(R.string.notification_time_start) }.show()
  }

  private fun chooseDays(then: (Set<Int>) -> Unit) {
    val symbols = DateFormatSymbols.getInstance().weekdays
    // Monday first: 1 is Monday here.
    val names = (1..7).map { symbols[it % 7 + 1] }
    val checked = BooleanArray(7) { true }
    dialog()
      .setTitle(R.string.notification_time_days)
      .setMultiChoiceItems(names.toTypedArray(), checked) { _, which, isChecked -> checked[which] = isChecked }
      .setPositiveButton(android.R.string.ok) { _, _ ->
        val days = (1..7).filter { checked[it - 1] }.toSet()
        then(if (days.size == 7 || days.isEmpty()) emptySet() else days)
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun chooseApp(then: (String, String) -> Unit) {
    val context = requireContext().applicationContext
    EXECUTOR.execute {
      val pm = context.packageManager
      val apps =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
          .map { it.activityInfo.packageName }
          .distinct()
          .map { it to NotificationReader.appName(context, it) }
          .sortedBy { it.second.lowercase() }
      activity?.runOnUiThread {
        if (!isAdded) return@runOnUiThread
        choose(getString(R.string.notification_choose_app), apps.map { it.second }) { then(apps[it].first, apps[it].second) }
      }
    }
  }

  private fun addAction() {
    val context = requireContext()
    val types = RuleAction.Type.entries
    choose(getString(R.string.notification_rule_add_action), types.map { NotificationRulesFragment.describeAction(context, RuleAction(it, number = 1)).let { text -> actionName(it) ?: text } }) { index ->
      val type = types[index]
      when (type) {
        RuleAction.Type.FORMAT ->
          askText(
            getString(R.string.notification_format_title),
            settings.format,
            message = getString(R.string.notification_format_message),
          ) { text, _, _ ->
            if (text.isEmpty()) return@askText getString(R.string.notification_enter_text)
            update { it.copy(actions = it.actions + RuleAction(type, value = text)) }
            null
          }
        RuleAction.Type.MAX_WORDS -> {
          val counts = listOf(3, 5, 10, 15, 20, 30, 50)
          choose(getString(R.string.notification_max_words_title), counts.map { resources.getQuantityString(R.plurals.notification_words, it, it) }) {
            update { rule -> rule.copy(actions = rule.actions + RuleAction(type, number = counts[it])) }
          }
        }
        RuleAction.Type.VOICE -> {
          val names = voiceProfileNames(context)
          val ids = listOf("") + names.keys
          val labels = listOf(getString(R.string.notification_voice_sandfly)) + names.values
          choose(getString(R.string.notification_voice_title), labels) {
            update { rule -> rule.copy(actions = rule.actions + RuleAction(type, value = ids[it])) }
          }
        }
        else -> update { it.copy(actions = it.actions + RuleAction(type)) }
      }
    }
  }

  /** The name of an action that takes a value, in the list of actions to add. */
  private fun actionName(type: RuleAction.Type): String? =
    when (type) {
      RuleAction.Type.FORMAT -> getString(R.string.notification_action_format)
      RuleAction.Type.MAX_WORDS -> getString(R.string.notification_action_max_words)
      RuleAction.Type.VOICE -> getString(R.string.notification_action_voice)
      else -> null
    }

  companion object {
    const val ARG_RULE = "rule"
    private val EXECUTOR = Executors.newSingleThreadExecutor()
  }
}
