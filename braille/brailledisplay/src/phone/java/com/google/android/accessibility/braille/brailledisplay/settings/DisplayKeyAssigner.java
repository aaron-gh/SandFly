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

package com.google.android.accessibility.braille.brailledisplay.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.braille.brailledisplay.controller.DisplayKeyCapture;
import com.google.android.accessibility.braille.brailledisplay.controller.utils.DisplayKeys;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.braille.common.DisplayKeyOverrides;
import com.google.android.accessibility.utils.material.MaterialComponentUtils;
import java.util.ArrayList;
import java.util.List;

/** Dialogs that assign braille display keys to a command. */
final class DisplayKeyAssigner {

  private DisplayKeyAssigner() {}

  /**
   * Shows Change keys, and Reset when the keys of {@code target} were changed, for the command
   * named {@code label}. Runs {@code onChanged} after the keys change.
   */
  static void showOptions(
      Context context,
      String target,
      String label,
      DisplayKeys.Describer describer,
      Runnable onChanged) {
    boolean changed = BrailleUserPreferences.readDisplayKeyOverrides(context).isChanged(target);
    List<CharSequence> items = new ArrayList<>();
    items.add(context.getString(R.string.bd_key_bindings_change_keys));
    if (changed) {
      items.add(context.getString(R.string.bd_key_bindings_reset));
    }
    MaterialComponentUtils.alertDialogBuilder(context)
        .setTitle(label)
        .setItems(
            items.toArray(new CharSequence[0]),
            (dialog, which) -> {
              if (which == 0) {
                showCapture(context, target, label, describer, onChanged);
              } else {
                confirmAndReset(context, target, label, onChanged);
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  /** Resets the keys of {@code target}, first asking when its own key runs another command. */
  private static void confirmAndReset(
      Context context, String target, String label, Runnable onChanged) {
    @Nullable
    String loser =
        BrailleUserPreferences.readDisplayKeyOverrides(context).targetLosingKeyOnReset(target);
    if (loser == null) {
      reset(context, target, onChanged);
      return;
    }
    MaterialComponentUtils.alertDialogBuilder(context)
        .setTitle(label)
        .setMessage(
            context.getString(
                R.string.bd_key_bindings_move_keys,
                DisplayKeys.targetLabel(context, loser),
                label))
        .setPositiveButton(android.R.string.ok, (dialog, which) -> reset(context, target, onChanged))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private static void reset(Context context, String target, Runnable onChanged) {
    DisplayKeyOverrides overrides = BrailleUserPreferences.readDisplayKeyOverrides(context);
    overrides.reset(target);
    BrailleUserPreferences.writeDisplayKeyOverrides(context, overrides);
    onChanged.run();
  }

  /** Asks to restore the default keys of every command. */
  static void showResetAll(Context context, Runnable onChanged) {
    MaterialComponentUtils.alertDialogBuilder(context)
        .setTitle(R.string.bd_key_bindings_reset_all)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              BrailleUserPreferences.writeDisplayKeyOverrides(
                  context, DisplayKeyOverrides.parse(null));
              onChanged.run();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  /** Takes the next keys pressed on the display and assigns them to {@code target} on OK. */
  private static void showCapture(
      Context context,
      String target,
      String label,
      DisplayKeys.Describer describer,
      Runnable onChanged) {
    if (!DisplayKeyCapture.isAvailable()) {
      MaterialComponentUtils.alertDialogBuilder(context)
          .setTitle(label)
          .setMessage(R.string.bd_key_bindings_connect_display)
          .setPositiveButton(android.R.string.ok, null)
          .show();
      return;
    }
    String[] captured = new String[1];
    AlertDialog dialog =
        MaterialComponentUtils.alertDialogBuilder(context)
            .setTitle(label)
            .setMessage(R.string.bd_key_bindings_press_keys)
            .setPositiveButton(
                android.R.string.ok,
                (d, which) -> {
                  if (captured[0] != null) {
                    confirmAndAssign(context, target, label, captured[0], onChanged);
                  }
                })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
    DisplayKeyCapture.Listener listener =
        key -> {
          @Nullable
          String assignable =
              key != null && DisplayKeys.isAssignableKey(context, key) ? key : null;
          captured[0] = assignable;
          String message;
          if (assignable == null) {
            message = context.getString(R.string.bd_key_bindings_keys_not_assignable);
          } else {
            message = describer.describe(assignable);
            if (message.isEmpty()) {
              message =
                  DisplayKeys.targetLabel(
                      context, DisplayKeyOverrides.defaultTarget(assignable));
            }
          }
          dialog.setMessage(message);
          setOkEnabled(dialog, assignable != null);
        };
    dialog.setOnShowListener(
        d -> {
          setOkEnabled(dialog, false);
          View message = dialog.findViewById(android.R.id.message);
          if (message != null) {
            message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
          }
          DisplayKeyCapture.start(listener);
        });
    // Display keys run their commands again once the screen is left.
    LifecycleEventObserver observer =
        (source, event) -> {
          if (event == Lifecycle.Event.ON_PAUSE) {
            dialog.dismiss();
          }
        };
    @Nullable Lifecycle lifecycle =
        context instanceof LifecycleOwner owner ? owner.getLifecycle() : null;
    if (lifecycle != null) {
      lifecycle.addObserver(observer);
    }
    dialog.setOnDismissListener(
        d -> {
          DisplayKeyCapture.stop(listener);
          if (lifecycle != null) {
            lifecycle.removeObserver(observer);
          }
        });
    dialog.show();
  }

  private static void setOkEnabled(AlertDialog dialog, boolean enabled) {
    @Nullable Button ok = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
    if (ok != null) {
      ok.setEnabled(enabled);
    }
  }

  /** Assigns {@code key}, first asking to move it when another command runs it. */
  private static void confirmAndAssign(
      Context context, String target, String label, String key, Runnable onChanged) {
    String current = BrailleUserPreferences.readDisplayKeyOverrides(context).resolve(key);
    if (current.equals(DisplayKeyOverrides.NONE) || current.equals(target)) {
      assign(context, target, key, onChanged);
      return;
    }
    MaterialComponentUtils.alertDialogBuilder(context)
        .setTitle(label)
        .setMessage(
            context.getString(
                R.string.bd_key_bindings_move_keys,
                DisplayKeys.targetLabel(context, current),
                label))
        .setPositiveButton(
            android.R.string.ok, (dialog, which) -> assign(context, target, key, onChanged))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private static void assign(Context context, String target, String key, Runnable onChanged) {
    DisplayKeyOverrides overrides = BrailleUserPreferences.readDisplayKeyOverrides(context);
    overrides.assign(target, key);
    BrailleUserPreferences.writeDisplayKeyOverrides(context, overrides);
    onChanged.run();
  }
}
