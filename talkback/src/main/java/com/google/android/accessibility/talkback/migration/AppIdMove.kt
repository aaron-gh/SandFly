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

package com.google.android.accessibility.talkback.migration

import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

/**
 * The move from Backtalk Aaron, app ID io.github.aaron_gh.backtalk, to SandFly, app ID
 * io.github.aaron_gh.sandfly.
 *
 * The old app installs the new one, shares its settings with it through [SettingsExportProvider],
 * and turns itself off when the new one tells it through [HandOverReceiver]. The two apps are
 * signed with different keys, so each checks that the other is signed with the certificate named
 * here.
 */
object AppIdMove {
  /** The app ID of Backtalk Aaron. */
  const val OLD_PACKAGE = "io.github.aaron_gh.backtalk"

  /** The app ID of SandFly. */
  const val NEW_PACKAGE = "io.github.aaron_gh.sandfly"

  /** SHA-256 of the certificate that Backtalk Aaron is signed with. */
  const val OLD_CERT_SHA256 = "dab34c182d6f17df819cebf20854dcf38189b3414d363465dd6ce0fefc43616c"

  /** SHA-256 of the certificate that SandFly is signed with. */
  const val NEW_CERT_SHA256 = "bd5fd3c56cabfb69fdbc851aae7ba1e955c9e971caab1a8115c61243d46f576a"

  /** Sent by the new app to the old one when the new screen reader is on. */
  const val ACTION_HAND_OVER = "io.github.aaron_gh.sandfly.action.HAND_OVER"

  /** The class of the screen reader service, the same in both apps. */
  const val SERVICE_CLASS = "com.google.android.marvin.talkback.TalkBackService"

  /** Lists the exported files, with the columns [COLUMN_PATH] and [COLUMN_SIZE]. */
  const val PATH_FILES = "files"

  /** Opens one exported file: content://<authority>/file/<directory>/<name>. */
  const val PATH_FILE = "file"

  const val COLUMN_PATH = "path"
  const val COLUMN_SIZE = "size"

  /** The authority of the settings export in the app with the given app ID. */
  fun authority(packageName: String): String = "$packageName.migration"

  /** The flattened name of the screen reader service in the app with the given app ID. */
  fun serviceName(packageName: String): String = "$packageName/$SERVICE_CLASS"

  /** What to do with a downloaded APK. */
  enum class InstallKind {
    /** The APK is a newer build of this app. */
    UPDATE,
    /** The APK is SandFly, which installs next to this app. */
    MOVE,
    /** The APK is something else. */
    REFUSE,
  }

  /**
   * Decides how to install an APK whose app ID is [apkPackage] while this app is [ownPackage].
   * [sameSigner] is false only when the APK is known to be signed with a different key than this
   * app, and [newSigner] only when it is known not to be signed with SandFly's key.
   */
  fun installKind(
    apkPackage: String?,
    ownPackage: String,
    sameSigner: Boolean,
    newSigner: Boolean,
  ): InstallKind =
    when {
      apkPackage.isNullOrEmpty() -> InstallKind.REFUSE
      apkPackage == ownPackage && sameSigner -> InstallKind.UPDATE
      ownPackage == OLD_PACKAGE && apkPackage == NEW_PACKAGE && newSigner -> InstallKind.MOVE
      else -> InstallKind.REFUSE
    }

  /** Returns true if the installed app [packageName] is signed with the certificate [sha256]. */
  fun isSignedBy(packageManager: PackageManager, packageName: String, sha256: String): Boolean =
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageManager.hasSigningCertificate(
          packageName,
          hexToBytes(sha256),
          PackageManager.CERT_INPUT_SHA256,
        )
      } else {
        @Suppress("DEPRECATION")
        val signatures =
          packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        !signatures.isNullOrEmpty() && signatures.all { sha256Of(it) == sha256 }
      }
    } catch (e: PackageManager.NameNotFoundException) {
      false
    }

  /** Returns the lowercase hex SHA-256 of a certificate. */
  fun sha256Of(signature: Signature): String =
    MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") {
      "%02x".format(it)
    }

  private fun hexToBytes(hex: String): ByteArray =
    ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
