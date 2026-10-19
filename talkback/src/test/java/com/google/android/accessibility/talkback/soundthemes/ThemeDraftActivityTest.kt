package com.google.android.accessibility.talkback.soundthemes

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.google.android.accessibility.talkback.controlsounds.ControlSoundsSettings
import com.google.android.accessibility.utils.preference.BasePreferencesActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import androidx.appcompat.app.AlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ThemeDraftActivityTest {
  @Test fun backAndNavigateUpConfirmDiscardAndKeepEditingPreservesDraft() {
    val context = RuntimeEnvironment.getApplication()
    com.google.android.accessibility.utils.FormFactorUtils.initialize(context)
    val draft = SoundThemes.createDraft(context, SoundThemeManifest("Keep editing"))
    val args = Bundle().apply { putString(ThemeSoundsFragment.ARG_DRAFT, draft.id) }
    val intent = Intent(context, ThemeDraftActivity::class.java)
      .putExtra(BasePreferencesActivity.FRAGMENT_NAME, ThemeSoundsFragment::class.java.name)
      .putExtra(BasePreferencesActivity.FRAGMENT_ARGS, args)
    val controller = Robolectric.buildActivity(ThemeDraftActivity::class.java, intent).setup()
    val activity = controller.get()
    val prefs = context.createDeviceProtectedStorageContext().getSharedPreferences(
      SoundThemes.draftPreferencesName(draft.id), android.content.Context.MODE_PRIVATE)
    assertFalse(prefs.contains(ControlSoundsSettings.PREF_ON))
    assertFalse(prefs.contains(ControlSoundsSettings.PREF_3D))
    activity.onBackPressedDispatcher.onBackPressed()
    val dialog = ShadowDialog.getLatestDialog() as AlertDialog
    assertFalse(activity.isFinishing)
    assertTrue(draft.directory.exists())
    dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick()
    shadowOf(Looper.getMainLooper()).idle()
    assertFalse(activity.isFinishing)
    val up = PopupMenu(activity, View(activity)).menu.add(0, android.R.id.home, 0, "Navigate up")
    assertTrue(activity.onOptionsItemSelected(up))
    val discard = ShadowDialog.getLatestDialog() as AlertDialog
    discard.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
    shadowOf(Looper.getMainLooper()).idle()
    val fragment = activity.supportFragmentManager.findFragmentById(android.R.id.content) as ThemeSoundsFragment
    assertFalse(fragment.findPreference<androidx.preference.Preference>(ControlSoundsSettings.PREF_ON)!!.callChangeListener(true))
    fragment.executor.submit {}.get()
    shadowOf(Looper.getMainLooper()).idle()
    assertTrue(activity.isFinishing)
    assertFalse(draft.directory.exists())
    controller.pause().stop().destroy()
  }
  @Test fun returningToAMissingDraftClosesEditor() {
    val context = RuntimeEnvironment.getApplication()
    com.google.android.accessibility.utils.FormFactorUtils.initialize(context)
    val draft = SoundThemes.createDraft(context, SoundThemeManifest("Expired"))
    val intent = Intent(context, ThemeDraftActivity::class.java)
      .putExtra(BasePreferencesActivity.FRAGMENT_NAME, ThemeSoundsFragment::class.java.name)
      .putExtra(BasePreferencesActivity.FRAGMENT_ARGS, Bundle().apply {
        putString(ThemeSoundsFragment.ARG_DRAFT, draft.id)
      })
    val controller = Robolectric.buildActivity(ThemeDraftActivity::class.java, intent).setup()
    controller.pause()
    draft.directory.deleteRecursively()
    controller.resume()
    assertTrue(controller.get().isFinishing)
    controller.pause().stop().destroy()
  }
}
