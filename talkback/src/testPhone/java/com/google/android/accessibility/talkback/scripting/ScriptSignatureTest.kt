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

import com.google.android.accessibility.talkback.scripting.ScriptSignature.Status
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Scripts signed by tools/sign_script.py, checked as SandFly checks them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScriptSignatureTest {
  private val signed =
    """
    local manifest = {
      id = 'signed-test',
      name = 'Signed test',
      version = '1.0',
      author = 'SandFly',
      minApiVersion = 1,
      testedApiVersion = 2,
    }

    sf.speak('Hello')
    -- SandFly signature: MEUCIQCvX3DDoisDJkZRsD2KjD4bxs6OyLPmMXZhiY8HQfqOeAIgOwkJMEfvyFHEyzFamfiKtiebldqlh/n0kmB/ZOAktEs=

    """
      .trimIndent()

  /** pkg/main.lua and pkg/data.txt ("hello"), signed. */
  private val signedZip =
    "UEsDBBQAAAAIAFC1XV1fpYMvywAAAPsAAAAMAAAAcGtnL21haW4ubHVhVcvBa4MwGAXwe/+K79bDKqQKazfowcU4nHqQrbJ6" +
      "Cybar9UkaCyrY//7tJTBTo/3471Gl7yBliusZG9hB9+AYorliMaxEy1XoHgrZyrQwJ0usutRq1nXU+WDPepubu9cibC5Ttai" +
      "8g3mf8P16naW4p+68LNwHLjfoMdacTt08hlStqcRLVWUnYIkIQMxxsbeGH1W569S6PotLx6KRGwy78BYfox9ZP6Gxow+dqMb" +
      "nOttVKWXK83Sp6B8DQ+abF2iveBjeCFhftrvFr9QSwMECgAAAAAAULVdXYamEDYFAAAABQAAAAwAAABwa2cvZGF0YS50eHRo" +
      "ZWxsb1BLAQIeAxQAAAAIAFC1XV1fpYMvywAAAPsAAAAMAAAAAAAAAAEAAACkgQAAAABwa2cvbWFpbi5sdWFQSwECHgMKAAAA" +
      "AABQtV1dhqYQNgUAAAAFAAAADAAAAAAAAAABAAAApIH1AAAAcGtnL2RhdGEudHh0UEsFBgAAAAACAAIAdAAAACQBAAAAAA=="

  private fun statusOf(source: String) = ScriptPackage.read(source.toByteArray()).signature

  @Test
  fun aSignedScriptIsSigned() {
    assertEquals(Status.SIGNED, statusOf(signed))
  }

  @Test
  fun aChangedScriptIsChanged() {
    assertEquals(Status.CHANGED, statusOf(signed.replace("Hello", "Hullo")))
  }

  @Test
  fun codeAfterTheSignatureCountsAsAChange() {
    assertEquals(Status.CHANGED, statusOf(signed + "os.exit()\n"))
  }

  @Test
  fun anotherSignatureIsChanged() {
    val other = signed.replace("X3DDoisD", "X3DDoisA")
    assertEquals(Status.CHANGED, statusOf(other))
  }

  @Test
  fun aScriptWithNoSignatureIsUnsigned() {
    assertEquals(Status.UNSIGNED, statusOf(signed.substringBefore(ScriptSignature.PREFIX)))
  }

  @Test
  fun aSignatureCoversEveryFileOfAZip() {
    val zip = Base64.getDecoder().decode(signedZip)
    val pkg = ScriptPackage.read(zip)
    assertEquals(Status.SIGNED, pkg.signature)
    val changed = pkg.files + ("data.txt" to "hullo".toByteArray())
    assertEquals(Status.CHANGED, ScriptSignature.check(changed))
    val added = pkg.files + ("more.lua" to "x = 1".toByteArray())
    assertEquals(Status.CHANGED, ScriptSignature.check(added))
  }
}
