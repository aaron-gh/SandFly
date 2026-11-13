package com.google.android.accessibility.material.preference.compose

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.Text
import com.google.android.accessibility.material.theme.AccessibilitySuiteTheme

/** The [androidx.preference.ListPreference] that renders preference UI with Compose. */
open class ComposeListPreference
@JvmOverloads
constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
  defStyleRes: Int = 0,
) : ComposeBaseListPreference(context, attrs, defStyleAttr, defStyleRes) {

  override fun providePreferenceComposable(): @Composable () -> Unit {

    return {
      var dialogOpened by remember { mutableStateOf(false) }

      AccessibilitySuiteTheme {
        AlertDialog(
          visible = dialogOpened,
          onDismissRequest = { dialogOpened = false },
          title = { Text(title.toString()) },
        ) {
          entries.forEachIndexed { index, element ->
            item {
              RadioButton(
                selected = entryValues[index] == value,
                onSelect = {
                  if (callChangeListener(entryValues[index].toString())) {
                    setValueIndex(index)
                  }
                  dialogOpened = false
                },
                modifier =
                  Modifier.fillMaxWidth().semantics {
                    this.role = Role.RadioButton
                    this.selected = entryValues[index] == value
                  },
              ) {
                Text(text = element.toString(), maxLines = 3, overflow = TextOverflow.Ellipsis)
              }
            }
          }
        }

        FilledTonalButton(
          label = { Text(title.toString(), maxLines = 3, overflow = TextOverflow.Ellipsis) },
          onClick = { dialogOpened = true },
          secondaryLabel = toSecondaryLabel(),
        )
      }
    }
  }
}
