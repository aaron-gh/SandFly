/*
 * Copyright 2026 Backtalk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.braille.brailledisplay.platform.external;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.braille.brailledisplay.settings.ExternalDisplaysActivity;
import com.google.android.accessibility.utils.material.MaterialComponentUtils;
import java.util.HashSet;
import java.util.Set;

/**
 * Asks the user whether an app may be a braille display, and says while one is in use. A display
 * can read everything Backtalk shows and act as the user, so Backtalk uses an app only once the
 * user allows it, after a warning that says so.
 */
public final class ExternalDisplayApproval {
  private static final String TAG = "ExternalDisplayApproval";
  private static final String CHANNEL = "external_braille_displays";
  private static final int NOTIFICATION_ID_ASK_BASE = 0x6b00;
  private static final int NOTIFICATION_ID_IN_USE = 0x6bff;

  // Apps already asked about since Backtalk started, so that each is asked once at a time.
  private static final Set<String> asked = new HashSet<>();

  private ExternalDisplayApproval() {}

  /**
   * Asks with a notification whether {@code app} may be a braille display, unless it was asked
   * already. The notification opens the warning, where the user answers.
   */
  public static void ask(Context context, String app) {
    if (!asked.add(app)) {
      return;
    }
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
      BrailleDisplayLog.w(TAG, "can't ask about " + app + ", since Backtalk can't post notifications");
      return;
    }
    CharSequence label = ExternalDisplayApps.label(context, app);
    PendingIntent open =
        PendingIntent.getActivity(
            context,
            notificationId(app),
            ExternalDisplayApprovalActivity.intent(context, app),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    String title = context.getString(R.string.external_display_ask_title, label);
    notify(
        context,
        notificationId(app),
        builder(context)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(context.getString(R.string.external_display_ask_text))
            .setContentIntent(open)
            .setAutoCancel(true));
  }

  /** Removes the question about {@code app}, once the user has answered. */
  public static void answered(Context context, String app) {
    asked.remove(app);
    NotificationManagerCompat.from(context).cancel(notificationId(app));
  }

  /** Shows, for as long as it is in use, that {@code app} is the braille display. */
  public static void showInUse(Context context, String app) {
    CharSequence label = ExternalDisplayApps.label(context, app);
    PendingIntent settings =
        PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_IN_USE,
            new Intent(context, ExternalDisplaysActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    notify(
        context,
        NOTIFICATION_ID_IN_USE,
        builder(context)
            .setContentTitle(context.getString(R.string.external_display_in_use_title, label))
            .setContentText(context.getString(R.string.external_display_in_use_text, label))
            .setContentIntent(settings)
            .setOngoing(true)
            .setSilent(true));
  }

  public static void hideInUse(Context context) {
    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_IN_USE);
  }

  /**
   * Shows the warning about letting {@code app} be a braille display, and runs {@code onAllow} or
   * {@code onDeny} with the answer. Leaving the warning without answering runs {@code onCancel}.
   */
  public static void confirm(
      Context activityContext, String app, Runnable onAllow, Runnable onDeny, Runnable onCancel) {
    CharSequence label = ExternalDisplayApps.label(activityContext, app);
    MaterialComponentUtils.alertDialogBuilder(activityContext)
        .setTitle(activityContext.getString(R.string.external_display_ask_title, label))
        .setMessage(activityContext.getString(R.string.external_display_warning, label))
        .setPositiveButton(R.string.external_display_allow, (dialog, which) -> onAllow.run())
        .setNegativeButton(R.string.external_display_deny, (dialog, which) -> onDeny.run())
        .setOnCancelListener(dialog -> onCancel.run())
        .show();
  }

  /** Saves the user's answer about {@code app}. */
  public static void setAllowed(Context context, String app, boolean allowed) {
    ExternalDisplayApps.setAllowed(ExternalDisplayApps.prefs(context), app, allowed);
    answered(context, app);
  }

  private static NotificationCompat.Builder builder(Context context) {
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    if (manager != null && manager.getNotificationChannel(CHANNEL) == null) {
      manager.createNotificationChannel(
          new NotificationChannel(
              CHANNEL,
              context.getString(R.string.external_display_channel),
              NotificationManager.IMPORTANCE_DEFAULT));
    }
    return new NotificationCompat.Builder(context, CHANNEL)
        .setSmallIcon(R.drawable.gs_info_vd_theme_24)
        .setWhen(0);
  }

  @SuppressWarnings("MissingPermission") // checked with areNotificationsEnabled
  private static void notify(Context context, int id, NotificationCompat.Builder builder) {
    try {
      NotificationManagerCompat.from(context).notify(id, builder.build());
    } catch (SecurityException e) {
      BrailleDisplayLog.w(TAG, "can't post a notification", e);
    }
  }

  private static int notificationId(String app) {
    return NOTIFICATION_ID_ASK_BASE + (app.hashCode() & 0xfe);
  }
}
