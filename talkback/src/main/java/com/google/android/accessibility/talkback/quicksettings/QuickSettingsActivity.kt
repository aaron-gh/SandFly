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

package com.google.android.accessibility.talkback.quicksettings

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.notificationlist.NotificationListService
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.talkback.preference.base.announce
import com.google.android.accessibility.talkback.shizuku.ShizukuCommand
import com.google.android.accessibility.talkback.shizuku.ShizukuGrants
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.preference.PreferencesActivity
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * SandFly's own quick settings panel, opened by the Quick settings action instead of the system's.
 * It shows the controls and app tiles the user chose, in their order, as a list of switches,
 * sliders and buttons. A long press opens the system's settings for a control or a tile.
 */
class QuickSettingsActivity : AppCompatActivity() {
  private lateinit var column: LinearLayout
  /** The grid of items: rows of cells, which the screen reader reads as a grid. */
  private lateinit var grid: LinearLayout
  private val gridRows = ArrayList<LinearLayout>()
  private val gridDelegate = GridDelegate()
  private var columns = 2

  /** The cells and buttons in the panel, in order, as shown last. */
  private var shown: List<View> = emptyList()
  private lateinit var controls: QuickControls
  private lateinit var edit: Button
  private lateinit var systemQuickSettings: Button

  private val handler = Handler(Looper.getMainLooper())
  private var background: ExecutorService? = null

  /** A row per item, kept so that a focused row stays focused when the panel changes. */
  private val rows = HashMap<String, View>()
  private var items: List<QuickItem> = emptyList()
  private var appTiles: Map<ComponentName, AppTile> = emptyMap()

  /** The system's tiles, by spec, as SystemUI's dump showed them last. */
  private var tileStates: Map<String, TileState> = emptyMap()
  private var readingTiles = false
  private var readTilesAgain = false

  /**
   * Switches the user has just turned on or off, by item, with what they were set to and until
   * when. Until the change shows, or the time is up, the switch shows what it was set to, so that
   * it doesn't say its old state while the radio turns on.
   */
  private val pending = HashMap<String, Pair<Boolean, Long>>()

  /**
   * What tiles said their state was after SandFly clicked them in the system's quick settings, by
   * item, kept while the panel is open, since SystemUI's dump isn't always there to say it.
   */
  private val clickedStates = HashMap<String, Pair<CharSequence?, Boolean?>>()

  /** The view in the panel that has accessibility focus, if one has. */
  private var focusedView: View? = null

  /** Whether the panel has just opened, so focus goes to its first item. */
  private var opening = false

  private val refreshRunnable = Runnable { refresh() }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setTitle(R.string.quick_settings_title)
    controls = QuickControls(this)
    column =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        accessibilityDelegate = FocusTracker()
      }
    grid =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        accessibilityDelegate = gridDelegate
      }
    edit =
      button(R.string.quick_settings_edit) {
        startActivity(
          Intent(this, TalkBackPreferencesActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(PreferencesActivity.FRAGMENT_NAME, QuickSettingsItemsFragment::class.java.name)
        )
        finish()
      }
    systemQuickSettings =
      button(R.string.quick_settings_system) {
        TalkBackService.getInstance()
          ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
        finish()
      }
    val scroll = ScrollView(this).apply { addView(column) }
    ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
      val bars =
        insets.getInsets(
          WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
      insets
    }
    setContentView(scroll)
  }

  override fun onStart() {
    super.onStart()
    opening = true
    background = Executors.newSingleThreadExecutor { Thread(it, TAG) }
    controls.start { scheduleRefresh() }
    ShizukuShell.hold(this)
    appTiles = SystemTiles.appTiles(this).associateBy { it.component }
    items = QuickSettingsItems.shown(this, controls, appTiles.keys)
    columns = QuickSettingsItems.columns(SharedPreferencesUtils.getSharedPreferences(this))
    rows.keys.retainAll(items.map { it.id }.toSet())
    if (!SystemTiles.canRead(this) && ShizukuShell.isAllowed()) {
      // Shizuku already allows SandFly, so this doesn't ask the user anything.
      grantDumpAccess(SystemTiles.GRANTS)
    }
    refresh()
    readTiles()
  }

  override fun onStop() {
    controls.stop()
    ShizukuShell.release()
    handler.removeCallbacksAndMessages(null)
    background?.shutdownNow()
    background = null
    readingTiles = false
    clickedStates.clear()
    super.onStop()
    // Like the system's quick settings, the panel closes when the user leaves it.
    if (!isChangingConfigurations) {
      finish()
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    // Dark theme changes the configuration.
    scheduleRefresh()
  }

  /** Refreshes soon, once for many changes that arrive together. */
  private fun scheduleRefresh() {
    handler.removeCallbacks(refreshRunnable)
    handler.postDelayed(refreshRunnable, REFRESH_DELAY_MS)
  }

  private fun refresh() {
    handler.removeCallbacks(refreshRunnable)
    val now = SystemClock.uptimeMillis()
    pending.values.removeAll { it.second <= now }
    val cells = ArrayList<View>()
    for (item in items) {
      val cell =
        when (item) {
          is QuickItem.Control -> bindControl(item)
          is QuickItem.Tile -> bindTile(item)
          is QuickItem.SystemTile -> bindSystemTile(item)
        }
      cell?.let { cells.add(it) }
    }
    val views = cells + listOf(edit, systemQuickSettings)
    var focusAfter: View? = null
    if (opening) {
      // Otherwise focus can start on whatever the screen reader focused last in this window.
      opening = false
      focusAfter = views.first()
    }
    // The view being read was removed: focus moves to the one that takes its place.
    focusedView
      ?.takeIf { it !in views }
      ?.let { shown.indexOf(it) }
      ?.takeIf { it >= 0 }
      ?.let { index ->
        focusedView = null
        focusAfter = views[minOf(index, views.size - 1)]
      }
    layOutGrid(cells)
    setChildren(column, listOf(grid, edit, systemQuickSettings))
    shown = views
    focusAfter?.let { view ->
      view.post { focusTarget(view).performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null) }
    }
    pending.values.minOfOrNull { it.second }?.let { handler.postAtTime(refreshRunnable, it + 1) }
  }

  /**
   * Puts [cells] in rows of [columns], in order. Each cell tells the screen reader its row and
   * column, and a last row that isn't full keeps its cells the width of the others.
   */
  private fun layOutGrid(cells: List<View>) {
    val rowCount = (cells.size + columns - 1) / columns
    while (gridRows.size < rowCount) {
      gridRows +=
        LinearLayout(this).apply {
          orientation = LinearLayout.HORIZONTAL
          importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
          accessibilityDelegate = FocusTracker()
          layoutParams =
            LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.MATCH_PARENT,
              LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }
    }
    val rowsShown = gridRows.subList(0, rowCount)
    rowsShown.forEachIndexed { row, rowView ->
      val inRow = cells.subList(row * columns, minOf(cells.size, (row + 1) * columns))
      inRow.forEachIndexed { column, cell ->
        cell.layoutParams =
          LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
            setMargins(cellMargin, cellMargin, cellMargin, cellMargin)
          }
        if (cell.background == null) cell.background = cellBackground()
        cell.setTag(R.id.quick_settings_cell, intArrayOf(row, column))
        focusTarget(cell).setTag(R.id.quick_settings_cell, intArrayOf(row, column))
      }
      setChildren(rowView, inRow + spacers(rowView, columns - inRow.size))
    }
    gridDelegate.size = rowCount to columns
    setChildren(grid, rowsShown.toList())
  }

  private val cellMargin by lazy { dp(4) }

  /** The outline of a cell, so that the grid shows as tiles. */
  private fun cellBackground(): GradientDrawable =
    GradientDrawable().apply {
      cornerRadius = dp(12).toFloat()
      setStroke(dp(1), CELL_OUTLINE)
    }

  /** Empty space for the cells a row doesn't have, kept on the row to be reused. */
  private fun spacers(row: LinearLayout, count: Int): List<View> {
    @Suppress("UNCHECKED_CAST")
    val kept =
      (row.getTag(R.id.quick_settings_spacers) as? MutableList<View>)
        ?: ArrayList<View>().also { row.setTag(R.id.quick_settings_spacers, it) }
    while (kept.size < count) {
      kept +=
        View(this).apply {
          importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
          // As tall as the cells: a row with a spacer of no height gave its cells no height.
          layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }
    }
    return kept.take(count)
  }

  /** Tells the screen reader that the items are a grid, and its size. */
  private class GridDelegate : View.AccessibilityDelegate() {
    var size = 0 to 0

    @Suppress("DEPRECATION") // CollectionInfo's constructor needs Android 13.
    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
      super.onInitializeAccessibilityNodeInfo(host, info)
      info.collectionInfo =
        AccessibilityNodeInfo.CollectionInfo.obtain(size.first, size.second, false)
    }
  }

  private fun bindControl(item: QuickItem.Control): View? {
    val control = item.control
    val title = getString(control.title)
    return when (control.kind) {
      QuickControl.Kind.SWITCH -> {
        val actual = controls.isOn(control)
        val on = pending[item.id]?.first?.takeIf { actual != null } ?: actual
        if (pending[item.id]?.first == actual) {
          pending.remove(item.id)
        }
        val row = textRow(item.id)
        bindText(row, title, controls.subtitle(control))
        setState(row, ROLE_SWITCH, on, on?.let { onOffText(it) }) {
          // A state SandFly can't read, such as the microphone's without DUMP, can't be switched.
          if (on == null) switchUnknown(item) else toggle(item, on)
        }
        setLongClick(row, controls.settingsIntent(control))
        row
      }
      QuickControl.Kind.CHOICE -> {
        val choice = controls.choice(control) ?: return null
        val row = textRow(item.id)
        bindText(row, title, null)
        val change = { value: Int ->
          if (controls.setChoice(control, value) { refresh() } == QuickControls.Change.DONE) {
            refresh()
          }
        }
        setState(row, ROLE_BUTTON, null, choice.current.label) {
          // The current value may be one SandFly doesn't offer, such as Mute without access.
          val index = choice.options.indexOfFirst { it.value == choice.current.value }
          change(choice.options[(index + 1) % choice.options.size].value)
        }
        delegate(row).actions =
          choice.options
            .filter { it.value != choice.current.value }
            .map { other -> RowAction(other.label) { change(other.value) } }
        setLongClick(row, controls.settingsIntent(control))
        row
      }
      QuickControl.Kind.SLIDER -> {
        val state = controls.slider(control) ?: return null
        bindSlider(item, control, title, state)
      }
      QuickControl.Kind.BUTTON -> {
        val row = textRow(item.id)
        bindText(row, title, controls.buttonText(control))
        setState(row, ROLE_BUTTON, null, null) {
          if (controls.press(control) == QuickControls.Change.OPENED) finish()
        }
        setLongClick(row, controls.settingsIntent(control).takeIf { control != QuickControl.BATTERY })
        row
      }
      QuickControl.Kind.MEDIA -> bindMedia(item, title)
    }
  }

  /**
   * The media row says what plays and from which app; a double tap plays or pauses it, and its
   * actions skip. Without notification access, a double tap opens the setting for it.
   */
  private fun bindMedia(item: QuickItem.Control, title: String): View {
    val row = textRow(item.id)
    if (!controls.hasNotificationAccess()) {
      bindText(row, title, getString(R.string.notification_list_no_access))
      setState(row, ROLE_BUTTON, null, null) { open(notificationAccessIntent()) }
      setLongClick(row, null)
      return row
    }
    val media = controls.media()
    if (media == null) {
      bindText(row, title, getString(R.string.quick_settings_no_media))
      setState(row, ROLE_BUTTON, null, null) {}
      setLongClick(row, controls.settingsIntent(item.control))
      return row
    }
    bindText(row, title, listOfNotNull(media.title, media.app).joinToString(", ").ifEmpty { null })
    val state =
      getString(if (media.playing) R.string.notification_list_playing else R.string.notification_list_paused)
    setState(row, ROLE_BUTTON, null, state) { media.playPause?.invoke() }
    delegate(row).actions = media.actions.map { (label, run) -> RowAction(label, run) }
    setLongClick(row, controls.settingsIntent(item.control))
    return row
  }

  private fun notificationAccessIntent(): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        .putExtra(
          Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
          ComponentName(this, NotificationListService::class.java).flattenToString(),
        )
    } else {
      Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

  /**
   * A switch whose state SandFly can't read. When Shizuku runs but hasn't allowed SandFly yet,
   * which is why it can't, SandFly asks it to, reads the switch and then switches it. Otherwise it
   * opens the system's settings for the switch.
   */
  private fun switchUnknown(item: QuickItem.Control) {
    val control = item.control
    if (!controls.usesShizuku(control) || !ShizukuGrants.isReady() || ShizukuShell.isAllowed()) {
      controls.openSettings(control)
      return
    }
    ShizukuGrants.withShizukuPermission { allowed ->
      if (isFinishing || isDestroyed) return@withShizukuPermission
      if (!allowed) {
        controls.openSettings(control)
        return@withShizukuPermission
      }
      if (!SystemTiles.canRead(this)) {
        grantDumpAccess(SystemTiles.GRANTS)
      }
      controls.readShellState {
        val on = controls.isOn(control)
        if (on != null) toggle(item, on) else refresh()
      }
    }
  }

  private fun toggle(item: QuickItem.Control, on: Boolean?) {
    val target = !(on ?: return)
    val change =
      controls.setSwitch(item.control, target) {
        pending.remove(item.id)
        refresh()
      }
    if (change == QuickControls.Change.DONE || change == QuickControls.Change.STARTED) {
      pending[item.id] = target to SystemClock.uptimeMillis() + PENDING_MS
      refresh()
      readTilesLater()
    }
  }

  private fun bindTile(item: QuickItem.Tile): View {
    val tile = appTiles[item.component]
    val state = tileStates[SystemTiles.spec(item.component)]
    val label = state?.label ?: tile?.label?.toString() ?: item.component.packageName
    val row = textRow(item.id)
    bindTileState(row, item.id, label, state) { clickTile(item.component, label) }
    setLongClick(row, tilePreferences(item.component))
    return row
  }

  /**
   * A tile of the system's own, such as Hotspot. SandFly can only use it the way the user would,
   * in the system's quick settings, so a double tap opens them, clicks it and closes them.
   */
  private fun bindSystemTile(item: QuickItem.SystemTile): View {
    val state = item.spec.takeIf { it.isNotEmpty() }?.let { tileStates[it] }
    val label = state?.label ?: item.label
    val row = textRow(item.id)
    bindTileState(row, item.id, label, state) { clickThroughSystem(item.id, label) }
    setLongClick(row, null)
    return row
  }

  /**
   * Shows a tile's name, what it shows under its name and its state, as SystemUI's dump showed
   * them, or what the system's quick settings said after SandFly clicked it there.
   */
  private fun bindTileState(row: TextView, id: String, label: String, state: TileState?, onClick: () -> Unit) {
    val clicked = clickedStates[id]
    val stateText =
      clicked?.first?.toString()
        ?: when (state?.state) {
          Tile.STATE_UNAVAILABLE -> getString(R.string.quick_settings_unavailable)
          Tile.STATE_INACTIVE -> onOffText(false)
          Tile.STATE_ACTIVE -> onOffText(true)
          else -> clicked?.second?.let { onOffText(it) }
        }
    // Many tiles show their state under their name as well.
    val subtitle = state?.secondaryLabel?.takeUnless { it.equals(stateText, ignoreCase = true) }
    bindText(row, label, subtitle)
    val checked =
      clicked?.second ?: if (state?.isSwitch == true) state.state == Tile.STATE_ACTIVE else null
    setState(row, if (checked != null) ROLE_SWITCH else ROLE_BUTTON, checked, stateText, onClick)
  }

  /**
   * Clicks a tile in the system's quick settings through SandFly's accessibility service, then
   * focuses its row again and says its new state.
   */
  private fun clickThroughSystem(id: String, label: String) {
    SystemPanelClicker.click(label) { result ->
      if (isFinishing || isDestroyed) return@click
      when (result) {
        is SystemPanelClicker.Result.Clicked -> {
          clickedStates[id] = result.state to result.checked
          refresh()
          readTiles()
        }
        else -> announce(column, getString(R.string.quick_settings_tile_not_found, label))
      }
      // After the row's new state has gone out: a row that kept focus says only the new state,
      // and one that lost it says everything once focused.
      rows[id]?.let { row ->
        row.postDelayed(
          { row.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null) },
          REFOCUS_DELAY_MS,
        )
      }
    }
  }

  /**
   * Clicks an app's tile in the system's quick settings, adding it there first if it isn't there,
   * since SystemUI runs only the tiles it shows.
   */
  private fun clickTile(component: ComponentName, label: String) {
    if (!ShizukuGrants.isReady()) {
      // Without Shizuku, the tile can still be used in the system's quick settings, if it's there.
      clickThroughSystem(QuickItem.Tile(component).id, label)
      return
    }
    val argument = component.flattenToString()
    val click = {
      ShizukuShell.run(this, ShizukuCommand.CLICK_TILE, argument) { worked ->
        if (worked) readTilesLater()
      }
    }
    val inSystemPanel =
      SystemTiles.spec(component) in tileStates || component in addedTiles
    if (inSystemPanel) {
      click()
      return
    }
    ShizukuShell.run(this, ShizukuCommand.ADD_TILE, argument) { added ->
      if (added) {
        addedTiles += component
        handler.postDelayed(click, ADD_TILE_DELAY_MS)
      }
    }
  }

  /** The tile's own settings, or its app's details when it has none. */
  private fun tilePreferences(component: ComponentName): Intent {
    val preferences =
      Intent(TileService.ACTION_QS_TILE_PREFERENCES)
        .setPackage(component.packageName)
        .putExtra(Intent.EXTRA_COMPONENT_NAME, component)
    if (packageManager.resolveActivity(preferences, 0) != null) {
      return preferences
    }
    return Intent(
      Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
      Uri.fromParts("package", component.packageName, null),
    )
  }

  private fun hasDump(): Boolean = ShizukuGrants.Grant.DUMP.isGranted(this)

  private fun grantDumpAccess(grants: List<ShizukuGrants.Grant>) {
    ShizukuGrants.requestAll(this, grants) { if (!isDestroyed) readTiles() }
  }

  /** Reads the system's tiles, soon after a change and again a little later. */
  private fun readTilesLater() {
    TILE_READ_DELAYS_MS.forEach { handler.postDelayed({ readTiles() }, it) }
  }

  private fun readTiles() {
    val executor = background ?: return
    if (!SystemTiles.canRead(this) && !hasDump()) return
    val readSensors =
      hasDump() &&
        items.any {
          it is QuickItem.Control &&
            (it.control == QuickControl.MICROPHONE_ACCESS || it.control == QuickControl.CAMERA_ACCESS)
        }
    val userId = Process.myUserHandle().hashCode()
    val readStates = SystemTiles.canRead(this)
    if (readingTiles) {
      readTilesAgain = true
      return
    }
    readingTiles = true
    executor.execute {
      val states =
        try {
          if (readStates) SystemTiles.read() else null
        } catch (e: Throwable) {
          LogUtils.w(TAG, "Cannot read the system's tiles: %s", e)
          null
        }
      val sensors =
        try {
          if (readSensors) SensorPrivacy.read(userId) else null
        } catch (e: Throwable) {
          LogUtils.w(TAG, "Cannot read the microphone and camera switches: %s", e)
          null
        }
      handler.post {
        if (background !== executor) return@post
        readingTiles = false
        if (states != null && states != tileStates) {
          tileStates = states
          // The dump says the state now, after SandFly clicked a tile in the system's panel.
          clickedStates.clear()
          refresh()
        }
        if (sensors != null && sensors != controls.sensorPrivacy) {
          controls.sensorPrivacy = sensors
          refresh()
        }
        if (readTilesAgain) {
          readTilesAgain = false
          readTiles()
        }
      }
    }
  }

  private fun bindSlider(item: QuickItem.Control, control: QuickControl, title: String, state: SliderState): View {
    val container =
      rows.getOrPut(item.id) {
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          val padding = dp(16)
          setPadding(padding, padding / 2, padding, padding / 2)
          addView(
            TextView(context).apply {
              setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
              importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
          )
          addView(
            SeekBar(context).apply {
              minimumHeight = dp(48)
              setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                  override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) changeSlider(seekBar, progress)
                  }

                  override fun onStartTrackingTouch(seekBar: SeekBar) {}

                  override fun onStopTrackingTouch(seekBar: SeekBar) {}
                }
              )
              accessibilityDelegate = SliderDelegate()
            }
          )
        }
      } as LinearLayout
    (container.getChildAt(0) as TextView).text = title
    val seekBar = container.getChildAt(1) as SeekBar
    seekBar.setTag(R.id.quick_settings_slider, SliderBinding(control, state))
    seekBar.contentDescription = title
    // The user's finger decides while it is on the slider.
    if (!seekBar.isPressed) {
      seekBar.min = state.min
      seekBar.max = state.max
      seekBar.progress = state.value
    }
    ViewCompat.setStateDescription(seekBar, controls.sliderText(state.copy(value = seekBar.progress)))
    return container
  }

  private fun changeSlider(seekBar: SeekBar, value: Int) {
    val binding = seekBar.getTag(R.id.quick_settings_slider) as? SliderBinding ?: return
    // A settings screen that opened to allow it takes over; the slider's later moves would open it
    // again for each step.
    val change = controls.setSlider(binding.control, value) { scheduleRefresh() }
    if (change == QuickControls.Change.OPENED) {
      finish()
      return
    }
    ViewCompat.setStateDescription(seekBar, controls.sliderText(binding.state.copy(value = value)))
  }

  private class SliderBinding(val control: QuickControl, val state: SliderState)

  /**
   * Swipes up and down move a slider by its own step, such as one volume step. The slider also
   * says its cell in the grid.
   */
  private inner class SliderDelegate : View.AccessibilityDelegate() {
    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
      super.onInitializeAccessibilityNodeInfo(host, info)
      setCell(host, info)
    }

    override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
      val seekBar = host as SeekBar
      val binding = seekBar.getTag(R.id.quick_settings_slider) as? SliderBinding
      val direction =
        when (action) {
          AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> 1
          AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> -1
          else -> 0
        }
      if (binding == null || direction == 0) {
        return super.performAccessibilityAction(host, action, args)
      }
      val value = (seekBar.progress + direction * binding.state.step).coerceIn(seekBar.min, seekBar.max)
      if (value == seekBar.progress) {
        return false
      }
      seekBar.progress = value
      changeSlider(seekBar, value)
      return true
    }
  }

  private fun textRow(id: String): TextView =
    rows.getOrPut(id) {
      TextView(this).apply {
        val padding = dp(16)
        setPadding(padding, padding, padding, padding)
        minHeight = dp(48)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        isFocusable = true
        val delegate = RowDelegate()
        accessibilityDelegate = delegate
        // Reading the delegate back from the view needs Android 10.
        setTag(R.id.quick_settings_row_delegate, delegate)
      }
    } as TextView

  private fun delegate(row: View): RowDelegate =
    row.getTag(R.id.quick_settings_row_delegate) as RowDelegate

  private fun bindText(row: TextView, title: String, subtitle: String?) {
    val parts = listOfNotNull(title, subtitle?.takeIf { it.isNotBlank() && it != title })
    row.text = parts.joinToString("\n")
    row.contentDescription = parts.joinToString(", ")
  }

  /**
   * Sets a row's role, checked state, spoken state and what a double tap does. The spoken state
   * changes on its own when the setting changes, so the screen reader says the new state.
   */
  private fun setState(row: TextView, role: String, checked: Boolean?, state: String?, onClick: () -> Unit) {
    val delegate = delegate(row)
    delegate.role = role
    delegate.checked = checked
    delegate.actions = emptyList()
    ViewCompat.setStateDescription(row, state)
    row.setOnClickListener { onClick() }
    // A switch that is on is filled, as in the system's quick settings.
    (row.background as? GradientDrawable ?: cellBackground().also { row.background = it })
      .setColor(if (checked == true) CELL_ON else Color.TRANSPARENT)
  }

  private fun setLongClick(row: TextView, intent: Intent?) {
    delegate(row).hasLongClick = intent != null
    if (intent == null) {
      row.setOnLongClickListener(null)
      row.isLongClickable = false
    } else {
      row.setOnLongClickListener {
        open(intent)
        true
      }
    }
  }

  private fun open(intent: Intent) {
    try {
      startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      finish()
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot open %s: %s", intent, e)
    }
  }

  private fun onOffText(on: Boolean): String =
    getString(if (on) R.string.quick_settings_on else R.string.quick_settings_off)

  /** The view that takes accessibility focus for a row: the slider of a slider's row. */
  private fun focusTarget(view: View): View =
    if (view is LinearLayout && view.childCount == 2 && view.getChildAt(1) is SeekBar) {
      view.getChildAt(1)
    } else {
      view
    }

  /** An action of a cell, in the screen reader's actions menu. */
  private class RowAction(val label: CharSequence, val run: () -> Unit)

  /** Gives a row its role, checked state and actions. */
  private class RowDelegate : View.AccessibilityDelegate() {
    var role: String = ROLE_BUTTON
    var checked: Boolean? = null
    var hasLongClick = false
    var actions: List<RowAction> = emptyList()

    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
      super.onInitializeAccessibilityNodeInfo(host, info)
      setCell(host, info)
      info.className = role
      checked?.let {
        info.isCheckable = true
        @Suppress("DEPRECATION") // setChecked(int) needs Android 16.
        info.isChecked = it
      }
      if (hasLongClick) {
        info.addAction(
          AccessibilityAction(
            AccessibilityNodeInfo.ACTION_LONG_CLICK,
            host.context.getString(R.string.quick_settings_open_settings),
          )
        )
      }
      actions.take(ACTION_IDS.size).forEachIndexed { index, action ->
        info.addAction(AccessibilityAction(ACTION_IDS[index], action.label))
      }
    }

    override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
      val index = ACTION_IDS.indexOf(action)
      val rowAction = if (index >= 0) actions.getOrNull(index) else null
      if (rowAction != null) {
        rowAction.run()
        return true
      }
      return super.performAccessibilityAction(host, action, args)
    }
  }

  /**
   * Notes which cell or button in the panel has accessibility focus. It sits on the column and on
   * each row of the grid, and so sees the rows themselves too, which it leaves out.
   */
  private inner class FocusTracker : View.AccessibilityDelegate() {
    override fun onRequestSendAccessibilityEvent(
      host: ViewGroup,
      child: View,
      event: AccessibilityEvent,
    ): Boolean {
      if (child === grid || child in gridRows) {
        return super.onRequestSendAccessibilityEvent(host, child, event)
      }
      when (event.eventType) {
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> focusedView = child
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED ->
          if (focusedView === child) {
            focusedView = null
          }
      }
      return super.onRequestSendAccessibilityEvent(host, child, event)
    }
  }

  /**
   * Puts [views] in [parent] in order, moving only the views that are out of place. A view taken
   * out of the window loses accessibility focus, so the others stay where they are.
   */
  private fun setChildren(parent: ViewGroup, views: List<View>) {
    val wanted = views.toSet()
    for (i in parent.childCount - 1 downTo 0) {
      if (parent.getChildAt(i) !in wanted) {
        parent.removeViewAt(i)
      }
    }
    views.forEachIndexed { index, view ->
      if (parent.getChildAt(index) !== view) {
        (view.parent as? ViewGroup)?.removeView(view)
        parent.addView(view, index)
      }
    }
  }

  private fun button(@StringRes text: Int, onClick: () -> Unit): Button =
    Button(this).apply {
      setText(text)
      isAllCaps = false
      setOnClickListener { onClick() }
    }

  private fun dp(value: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics)
      .toInt()

  companion object {
    private const val TAG = "QuickSettings"
    private const val CELL_OUTLINE = 0x80808080.toInt()
    private const val CELL_ON = 0x4D2196F3

    /** Tells the screen reader which row and column of the grid [host] is in. */
    @Suppress("DEPRECATION") // CollectionItemInfo's constructor needs Android 13.
    private fun setCell(host: View, info: AccessibilityNodeInfo) {
      val cell = host.getTag(R.id.quick_settings_cell) as? IntArray ?: return
      info.collectionItemInfo =
        AccessibilityNodeInfo.CollectionItemInfo.obtain(cell[0], 1, cell[1], 1, false)
    }
    private const val ROLE_SWITCH = "android.widget.Switch"
    private const val ROLE_BUTTON = "android.widget.Button"
    private const val REFRESH_DELAY_MS = 50L
    /** Longer than Android waits before it sends a view's state change. */
    private const val REFOCUS_DELAY_MS = 300L
    /** How long a switch shows what it was set to before it shows its state again. */
    private const val PENDING_MS = 5_000L
    /** How long SystemUI takes to start a tile that was just added. */
    private const val ADD_TILE_DELAY_MS = 1_000L
    private val TILE_READ_DELAYS_MS = listOf(300L, 1_500L)

    /** Tiles added to the system's quick settings while SandFly runs. */
    private val addedTiles = HashSet<ComponentName>()

    private val ACTION_IDS =
      intArrayOf(
        R.id.quick_settings_action_1,
        R.id.quick_settings_action_2,
        R.id.quick_settings_action_3,
      )

    /**
     * Opens the panel in place of the system's quick settings, if the user has chosen it. The
     * system's quick settings are kept on the lock screen, where they hide what the user chose to
     * hide there.
     */
    @JvmStatic
    fun openInsteadOfSystem(context: Context): Boolean {
      if (!QuickSettingsItems.isEnabled(context)) {
        return false
      }
      if (context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
        return false
      }
      context.startActivity(
        Intent(context, QuickSettingsActivity::class.java)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      )
      return true
    }
  }
}
