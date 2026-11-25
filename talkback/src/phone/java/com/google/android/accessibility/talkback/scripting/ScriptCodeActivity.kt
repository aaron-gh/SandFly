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
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.widget.KeyboardSafeEditText
import java.io.IOException
import java.util.concurrent.Executors
import org.json.JSONException

/**
 * The main.lua of a script, with its manifest. View code shows the code of an installed script,
 * Edit code changes it in place, and New script from code installs a new script. Saving checks the
 * code as an import checks a script file.
 */
class ScriptCodeActivity : AppCompatActivity() {
  private enum class Mode {
    VIEW,
    EDIT,
    NEW,
  }

  private val store: ScriptStore by lazy { ScriptStore.get(this) }
  private val scriptId: String? by lazy { intent.getStringExtra(EXTRA_ID) }
  private var mode = Mode.VIEW
  private var field: EditText? = null
  private var shown: TextView? = null
  /** The code as loaded, to tell whether it was changed; null until it is loaded. */
  private var original: String? = null
  private var saving = false
  private var dialog: AlertDialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val id = scriptId
    mode =
      when {
        id == null -> Mode.NEW
        intent.getBooleanExtra(EXTRA_EDIT, false) -> Mode.EDIT
        else -> Mode.VIEW
      }
    val title =
      getString(
        when (mode) {
          Mode.VIEW -> R.string.script_code_view
          Mode.EDIT -> R.string.script_code_edit
          Mode.NEW -> R.string.script_code_new
        }
      )
    setTitle(title)
    val name = id?.let { store.find(it)?.manifest?.name }
    if (id != null && name == null) {
      finish()
      return
    }
    val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    content.addView(heading(name ?: title))
    if (mode == Mode.VIEW) {
      // Selectable text, so that screen readers move through it by line, word and character.
      val text =
        TextView(this).apply {
          setTextIsSelectable(true)
          typeface = Typeface.MONOSPACE
          setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
          setPadding(dp(16), dp(8), dp(16), dp(16))
        }
      shown = text
      content.addView(
        ScrollView(this).apply { addView(text, MATCH_PARENT, WRAP_CONTENT) },
        LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f),
      )
    } else {
      content.addView(
        Button(this).apply {
          setText(R.string.script_code_save)
          setOnClickListener { save() }
        },
        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT),
      )
      val edit =
        KeyboardSafeEditText(this).apply {
          hint = getString(R.string.script_code_field)
          inputType =
            InputType.TYPE_CLASS_TEXT or
              InputType.TYPE_TEXT_FLAG_MULTI_LINE or
              InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
          gravity = Gravity.TOP or Gravity.START
          typeface = Typeface.MONOSPACE
          setHorizontallyScrolling(false)
          isVerticalScrollBarEnabled = true
          // The code can be up to 1 MB: too much for the saved state of the screen, which
          // onSaveInstanceState keeps only when it is small.
          isSaveEnabled = false
          setPadding(dp(16), dp(8), dp(16), dp(8))
        }
      field = edit
      content.addView(edit, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
      onBackPressedDispatcher.addCallback(
        this,
        object : OnBackPressedCallback(true) {
          override fun handleOnBackPressed() = close()
        },
      )
    }
    ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
      val bars =
        insets.getInsets(
          WindowInsetsCompat.Type.systemBars() or
            WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.ime()
        )
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
      WindowInsetsCompat.CONSUMED
    }
    setContentView(content)

    val restored = savedInstanceState?.getString(STATE_TEXT)
    if (restored != null && mode != Mode.VIEW) {
      original = savedInstanceState?.getString(STATE_ORIGINAL).orEmpty()
      field?.setText(restored)
    } else if (id == null) {
      original = ""
      field?.requestFocus()
    } else {
      load(id)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    val text = field?.text?.toString() ?: return
    val loaded = original ?: return
    if (text.length + loaded.length <= MAX_SAVED_CHARS) {
      outState.putString(STATE_TEXT, text)
      outState.putString(STATE_ORIGINAL, loaded)
    }
  }

  override fun onDestroy() {
    dialog?.dismiss()
    dialog = null
    super.onDestroy()
  }

  private fun load(id: String) {
    executor.execute {
      val text = store.readFile(id, ScriptPackage.MAIN)?.decodeToString()
      runOnUiThread {
        if (isFinishing || isDestroyed) return@runOnUiThread
        if (text == null) {
          toast(getString(R.string.script_code_load_failed))
          finish()
          return@runOnUiThread
        }
        original = text
        field?.setText(text)
        shown?.text = text
      }
    }
  }

  private fun save() {
    val text = field?.text?.toString() ?: return
    if (saving || original == null) return
    saving = true
    val id = scriptId
    executor.execute {
      var pkg: ScriptPackage? = null
      var error: String? = null
      try {
        pkg =
          if (id == null) ScriptPackage.withMain(emptyMap(), text)
          else ScriptCode.edited(store, id, text)
      } catch (e: IOException) {
        error = e.message
      } catch (e: IllegalArgumentException) {
        // ManifestException, as for an import.
        error = e.message
      } catch (e: JSONException) {
        error = e.message
      }
      runOnUiThread {
        saving = false
        if (isFinishing || isDestroyed) return@runOnUiThread
        val read = pkg
        if (read == null) failed(error.orEmpty()) else confirmSave(read, text)
      }
    }
  }

  /**
   * Installs new code after the install dialog of an import, or replaces the code of the script
   * open here, after the same dialog only when the code asks for permissions it didn't.
   */
  private fun confirmSave(pkg: ScriptPackage, text: String) {
    val id = scriptId
    if (id != null) {
      if (ScriptCode.idChanged(id, pkg)) {
        failed(getString(R.string.script_code_id_changed, id))
        return
      }
      val existing = store.find(id)
      if (existing == null) {
        finish()
        return
      }
      val done = getString(R.string.script_code_saved, existing.manifest.name)
      if (ScriptInstallQuestion.addedPermissions(existing, pkg.manifest.permissions).isEmpty()) {
        install(pkg, ScriptInstallQuestion.grantedAfter(existing, pkg.manifest.permissions), done, text)
        return
      }
      val question = ScriptInstallQuestion.of(this, store, pkg, ScriptFrom.Code)
      ask(question) { install(pkg, question.granted, done, text) }
      return
    }
    val question = ScriptInstallQuestion.of(this, store, pkg, ScriptFrom.Code)
    ask(question) { install(pkg, question.granted, question.done, text) }
  }

  private fun ask(question: ScriptInstallQuestion, onOk: () -> Unit) =
    show(
      AlertDialog.Builder(this)
        .setTitle(question.title)
        .setMessage(question.message)
        .setPositiveButton(question.button) { _, _ -> onOk() }
        .setNegativeButton(android.R.string.cancel, null)
    )

  private fun install(pkg: ScriptPackage, granted: Set<ScriptPermission>, done: String, text: String) {
    if (saving) return
    saving = true
    executor.execute {
      val error =
        try {
          store.install(pkg, granted)
          null
        } catch (e: IOException) {
          e.message.orEmpty()
        }
      runOnUiThread {
        saving = false
        if (isFinishing || isDestroyed) return@runOnUiThread
        if (error != null) {
          failed(error)
          return@runOnUiThread
        }
        original = text
        toast(done)
        finish()
      }
    }
  }

  /** Says why the code wasn't saved, and leaves it open to change. */
  private fun failed(reason: String) =
    show(
      AlertDialog.Builder(this)
        .setTitle(R.string.script_code_not_saved)
        .setMessage(reason)
        .setPositiveButton(android.R.string.ok, null)
    )

  /** Back: closes, after asking first when the code was changed. */
  private fun close() {
    val text = field?.text?.toString()
    val loaded = original
    if (text == null || loaded == null || text == loaded) {
      finish()
      return
    }
    show(
      AlertDialog.Builder(this)
        .setTitle(R.string.script_code_discard_title)
        .setPositiveButton(R.string.script_code_discard) { _, _ -> finish() }
        .setNegativeButton(R.string.script_code_keep_editing, null)
    )
  }

  private fun show(builder: AlertDialog.Builder) {
    if (dialog?.isShowing == true) return
    dialog = builder.show()
  }

  private fun heading(text: String): TextView =
    TextView(this).apply {
      this.text = text
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
      setPadding(dp(16), dp(12), dp(16), dp(4))
      ViewCompat.setAccessibilityHeading(this, true)
    }

  private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

  private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

  companion object {
    private const val EXTRA_ID = "script_id"
    private const val EXTRA_EDIT = "edit"
    private const val STATE_TEXT = "text"
    private const val STATE_ORIGINAL = "original"
    /** The most code, with the code as loaded, kept when the screen is recreated. */
    private const val MAX_SAVED_CHARS = 100_000

    private val executor = Executors.newSingleThreadExecutor()

    /** View code, or Edit code when [edit], of the installed script [id]. */
    fun intent(context: Context, id: String, edit: Boolean): Intent =
      Intent(context, ScriptCodeActivity::class.java).putExtra(EXTRA_ID, id).putExtra(EXTRA_EDIT, edit)

    /** New script from code. */
    fun newScriptIntent(context: Context): Intent = Intent(context, ScriptCodeActivity::class.java)
  }
}

/** Code typed in for an installed script, read as an import reads a script file. */
object ScriptCode {
  /**
   * The installed script [id] with [text] as its main.lua, and its other files, such as the sounds
   * and modules of a .zip. Throws [ManifestException] for code an import would refuse.
   */
  @Throws(IOException::class)
  fun edited(store: ScriptStore, id: String, text: String): ScriptPackage =
    ScriptPackage.withMain(store.codeFilesOf(id), text)

  /** Whether [pkg] has another id than the script [id] it was edited from, and so isn't it. */
  fun idChanged(id: String, pkg: ScriptPackage): Boolean = pkg.manifest.id != id
}
