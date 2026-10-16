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

package com.google.android.accessibility.talkback.soundthemes

import com.google.android.accessibility.talkback.soundthemes.ThemeLinks.Download
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeLinksTest {
  @Test
  fun aGitHubRepositoryDownloadsAsAZipFile() {
    assertEquals(
      Download("https://github.com/someone/my-theme/archive/HEAD.zip", "my-theme"),
      ThemeLinks.download("https://github.com/someone/my-theme"),
    )
    assertEquals(
      Download("https://github.com/someone/my-theme/archive/HEAD.zip", "my-theme"),
      ThemeLinks.download("github.com/someone/my-theme.git"),
    )
    assertEquals(
      Download("https://github.com/someone/my-theme/archive/dev.zip", "my-theme"),
      ThemeLinks.download("https://github.com/someone/my-theme/tree/dev"),
    )
  }

  @Test
  fun aFileOnGitHubDownloadsTheFileNotItsPage() {
    assertEquals(
      Download("https://raw.githubusercontent.com/someone/themes/main/dist/Soft.zip", "Soft"),
      ThemeLinks.download("https://github.com/someone/themes/blob/main/dist/Soft.zip"),
    )
  }

  @Test
  fun otherLinksDownloadAsTheyAre() {
    assertEquals(
      Download("https://example.com/themes/Soft.zip", "Soft"),
      ThemeLinks.download("https://example.com/themes/Soft.zip"),
    )
    assertEquals(
      Download(
        "https://github.com/someone/themes/releases/download/v1/Soft.zip",
        "Soft",
      ),
      ThemeLinks.download("https://github.com/someone/themes/releases/download/v1/Soft.zip"),
    )
  }

  @Test
  fun onlyHttpsLinks() {
    assertNull(ThemeLinks.download("http://example.com/Soft.zip"))
    assertNull(ThemeLinks.download("ftp://example.com/Soft.zip"))
  }

  @Test
  fun findsALinkInSharedText() {
    assertEquals(
      "https://github.com/someone/my-theme",
      ThemeLinks.findLink("Try this theme https://github.com/someone/my-theme"),
    )
    assertNull(ThemeLinks.findLink("no link here"))
  }
}
