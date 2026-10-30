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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The apps that can provide a braille display, and which of them the user has allowed. An app can
 * only ask to be one; Backtalk uses it once the user allows it in Backtalk, not before.
 */
public final class ExternalDisplayApps {
  /** The action of a display app's service. */
  public static final String ACTION_BRAILLE_DISPLAY = "fyi.quin.backtalk.action.BRAILLE_DISPLAY";

  /**
   * The permission a display app holds to ask to be one. Named after the app ID, as the manifest
   * declares it: Android refuses to install an app that declares a permission already declared by
   * an app with a different signing key.
   */
  public static String permissionBrailleDisplay(Context context) {
    return context.getPackageName() + ".permission.BRAILLE_DISPLAY";
  }

  /** The permission a display app's service requires, which only Backtalk holds. */
  private static String permissionBindBrailleDisplay(Context context) {
    return context.getPackageName() + ".permission.BIND_BRAILLE_DISPLAY";
  }

  /** What the user said about an app. */
  public enum State {
    ALLOWED,
    DENIED,
    /** Not asked yet, or not answered. */
    UNKNOWN,
  }

  // In the braille display settings' file, alongside the other display settings.
  private static final String FILENAME = "braille_display";
  static final String PREF_ALLOWED = "pref_external_displays_allowed";
  static final String PREF_DENIED = "pref_external_displays_denied";

  private ExternalDisplayApps() {}

  public static SharedPreferences prefs(Context context) {
    return SharedPreferencesUtils.getSharedPreferences(context, FILENAME);
  }

  /**
   * The signing certificates of the apps the user answered about, as "app/certificates", so that
   * another app installed later under the same package name isn't taken for the one answered about.
   */
  static final String PREF_SIGNERS = "pref_external_displays_signers";

  /**
   * The user's answer about {@code app}, or {@link State#UNKNOWN} when the app installed now isn't
   * the one they answered about: it is signed with other certificates.
   */
  public static State state(Context context, SharedPreferences prefs, String app) {
    State state = answered(prefs, app);
    if (state == State.UNKNOWN) {
      return state;
    }
    String current = signer(context, app);
    String stored = storedSigner(prefs, app);
    if (stored == null) {
      // Answered before certificates were kept: the app installed now is the one answered about.
      if (current != null) {
        storeSigner(prefs, app, current);
      }
      return state;
    }
    return stored.equals(current) ? state : State.UNKNOWN;
  }

  private static State answered(SharedPreferences prefs, String app) {
    if (set(prefs, PREF_ALLOWED).contains(app)) {
      return State.ALLOWED;
    }
    return set(prefs, PREF_DENIED).contains(app) ? State.DENIED : State.UNKNOWN;
  }

  public static void setAllowed(
      Context context, SharedPreferences prefs, String app, boolean allowed) {
    Set<String> allowedApps = set(prefs, PREF_ALLOWED);
    Set<String> deniedApps = set(prefs, PREF_DENIED);
    if (allowed) {
      allowedApps.add(app);
      deniedApps.remove(app);
    } else {
      allowedApps.remove(app);
      deniedApps.add(app);
    }
    prefs.edit().putStringSet(PREF_ALLOWED, allowedApps).putStringSet(PREF_DENIED, deniedApps).apply();
    String signer = signer(context, app);
    if (signer != null) {
      storeSigner(prefs, app, signer);
    }
  }

  private static @Nullable String storedSigner(SharedPreferences prefs, String app) {
    String prefix = app + "/";
    for (String entry : set(prefs, PREF_SIGNERS)) {
      if (entry.startsWith(prefix)) {
        return entry.substring(prefix.length());
      }
    }
    return null;
  }

  private static void storeSigner(SharedPreferences prefs, String app, String signer) {
    Set<String> signers = set(prefs, PREF_SIGNERS);
    signers.removeIf(entry -> entry.startsWith(app + "/"));
    signers.add(app + "/" + signer);
    prefs.edit().putStringSet(PREF_SIGNERS, signers).apply();
  }

  /**
   * The SHA-256 digests of {@code app}'s signing certificates, sorted and joined, or null if they
   * can't be read. With key rotation, the original certificate, which stays the same.
   */
  static @Nullable String signer(Context context, String app) {
    try {
      PackageManager packageManager = context.getPackageManager();
      Signature[] signatures;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        SigningInfo info =
            packageManager.getPackageInfo(app, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo;
        if (info == null) {
          return null;
        }
        signatures =
            info.hasMultipleSigners()
                ? info.getApkContentsSigners()
                : new Signature[] {info.getSigningCertificateHistory()[0]};
      } else {
        signatures = packageManager.getPackageInfo(app, PackageManager.GET_SIGNATURES).signatures;
      }
      if (signatures == null || signatures.length == 0) {
        return null;
      }
      List<String> digests = new ArrayList<>();
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      for (Signature signature : signatures) {
        StringBuilder hex = new StringBuilder();
        for (byte b : sha256.digest(signature.toByteArray())) {
          hex.append(String.format("%02x", b));
        }
        digests.add(hex.toString());
      }
      Collections.sort(digests);
      return String.join(",", digests);
    } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException | RuntimeException e) {
      return null;
    }
  }

  /**
   * The display services of installed apps that can be displays, by package: the service must be one
   * that only Backtalk can bind, as an accessibility service must be one that only Android can bind,
   * and the app must hold the permission. Whether the user allowed them isn't checked here.
   */
  public static Map<String, ComponentName> displayServices(Context context, int flags) {
    PackageManager packageManager = context.getPackageManager();
    Map<String, ComponentName> services = new HashMap<>();
    for (ResolveInfo resolveInfo :
        packageManager.queryIntentServices(new Intent(ACTION_BRAILLE_DISPLAY), flags)) {
      ServiceInfo service = resolveInfo.serviceInfo;
      if (permissionBindBrailleDisplay(context).equals(service.permission)
          && holdsPermission(context, service.packageName)) {
        services.put(service.packageName, new ComponentName(service.packageName, service.name));
      }
    }
    return services;
  }

  static boolean holdsPermission(Context context, String app) {
    return context.getPackageManager().checkPermission(permissionBrailleDisplay(context), app)
        == PackageManager.PERMISSION_GRANTED;
  }

  /** The name the user knows the app by, or its package name if it has none. */
  public static CharSequence label(Context context, String app) {
    try {
      PackageManager packageManager = context.getPackageManager();
      return packageManager.getApplicationLabel(packageManager.getApplicationInfo(app, 0));
    } catch (PackageManager.NameNotFoundException e) {
      return app;
    }
  }

  private static Set<String> set(SharedPreferences prefs, String key) {
    Set<String> stored = prefs.getStringSet(key, null);
    return stored == null ? new HashSet<>() : new HashSet<>(stored);
  }
}
