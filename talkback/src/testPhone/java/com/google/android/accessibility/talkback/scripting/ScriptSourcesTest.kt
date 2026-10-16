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

package com.google.android.accessibility.talkback.scripting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScriptSourcesTest {
  private fun source(text: String) = ScriptSource.parse(text)!!

  private fun names(scripts: List<SourceScript>) = scripts.map { it.name }

  @Test
  fun aRepositoryAddressIsTheWholeRepository() {
    val source = source("github.com/trypsynth/backtalk-scripts")

    assertEquals(
      ScriptSource(SourceHost.GITHUB, "trypsynth", "backtalk-scripts", null, emptyList()),
      source,
    )
    assertEquals("https://github.com/trypsynth/backtalk-scripts", source.address)
    assertEquals(
      "https://api.github.com/repos/trypsynth/backtalk-scripts/contents",
      source.listUrl,
    )
    assertEquals("GitHub", source.where)
  }

  @Test
  fun aBranchAndFolderAreKept() {
    val source = source(" https://www.github.com/a/b.git/tree/dev/scripts/my%20apps ")

    assertEquals(ScriptSource(SourceHost.GITHUB, "a", "b", "dev", listOf("scripts", "my apps")), source)
    assertEquals("https://github.com/a/b/tree/dev/scripts/my%20apps", source.address)
    assertEquals(
      "https://api.github.com/repos/a/b/contents/scripts/my%20apps?ref=dev",
      source.listUrl,
    )
    assertEquals("GitHub: dev: scripts/my apps", source.where)
    assertEquals(source, source(source.address))
  }

  @Test
  fun gitLabHasSubgroupsBeforeTheDashAndTheBranchAfterIt() {
    val whole = source("gitlab.com/group/sub/repo")
    assertEquals(ScriptSource(SourceHost.GITLAB, "group/sub", "repo", null, emptyList()), whole)
    assertEquals(
      "https://gitlab.com/api/v4/projects/group%2Fsub%2Frepo/repository/tree?per_page=100",
      whole.listUrl,
    )

    val folder = source("https://gitlab.com/group/repo/-/tree/main/scripts/x")
    assertEquals(ScriptSource(SourceHost.GITLAB, "group", "repo", "main", listOf("scripts", "x")), folder)
    assertEquals("https://gitlab.com/group/repo/-/tree/main/scripts/x", folder.address)
    assertEquals(
      "https://gitlab.com/api/v4/projects/group%2Frepo/repository/tree" +
        "?per_page=100&ref=main&path=scripts%2Fx",
      folder.listUrl,
    )
    assertEquals(folder, source(folder.address))
  }

  @Test
  fun codebergAndGiteaTakeSrcBranchAddresses() {
    val whole = source("codeberg.org/a/b")
    assertEquals("https://codeberg.org/api/v1/repos/a/b/contents", whole.listUrl)

    val folder = source("https://codeberg.org/a/b/src/tag/v1/scripts")
    assertEquals(ScriptSource(SourceHost.CODEBERG, "a", "b", "v1", listOf("scripts")), folder)
    assertEquals("https://codeberg.org/a/b/src/branch/v1/scripts", folder.address)
    assertEquals("https://codeberg.org/api/v1/repos/a/b/contents/scripts?ref=v1", folder.listUrl)
    assertEquals(folder, source(folder.address))

    assertEquals(SourceHost.GITEA, source("https://gitea.com/a/b").host)
  }

  @Test
  fun bitbucketTakesSrcAddresses() {
    val whole = source("bitbucket.org/ws/repo")
    assertEquals(
      "https://api.bitbucket.org/2.0/repositories/ws/repo/src/HEAD/?pagelen=100",
      whole.listUrl,
    )

    val folder = source("https://bitbucket.org/ws/repo/src/main/scripts")
    assertEquals(ScriptSource(SourceHost.BITBUCKET, "ws", "repo", "main", listOf("scripts")), folder)
    assertEquals("https://bitbucket.org/ws/repo/src/main/scripts", folder.address)
    assertEquals(
      "https://api.bitbucket.org/2.0/repositories/ws/repo/src/main/scripts/?pagelen=100",
      folder.listUrl,
    )
  }

  @Test
  fun otherAddressesAreRefused() {
    assertNull(ScriptSource.parse(""))
    assertNull(ScriptSource.parse("github.com/a"))
    assertNull(ScriptSource.parse("http://github.com/a/b"))
    assertNull(ScriptSource.parse("https://example.com/a/b"))
    assertNull(ScriptSource.parse("https://github.com/a/b/blob/main/x.js"))
    assertNull(ScriptSource.parse("https://github.com/a/b/tree"))
    assertNull(ScriptSource.parse("https://github.com/a/b/tree/main/../x"))
    assertNull(ScriptSource.parse("https://github.com/a%2Fb/c"))
    assertNull(ScriptSource.parse("github.com/../b"))
    assertNull(ScriptSource.parse("github.com/./b"))
    assertNull(ScriptSource.parse("gitlab.com/group"))
    assertNull(ScriptSource.parse("gitlab.com/group/repo/-/blob/main/x.js"))
    assertNull(ScriptSource.parse("gitlab.com/group/repo/-/tree"))
    assertNull(ScriptSource.parse("https://codeberg.org/a/b/src/main"))
    assertNull(ScriptSource.parse("https://codeberg.org/a/b/issues"))
    assertNull(ScriptSource.parse("https://bitbucket.org/a/b/src"))
    assertNull(ScriptSource.parse("https://git.example.com/a/b"))
  }

  private val raw = "https://raw.githubusercontent.com/a/b/main"

  @Test
  fun aGitHubListingHoldsScriptFilesOnGitHub() {
    val page =
      ScriptSources.parsePage(
        source("github.com/a/b"),
        """[
          {"name":"b.JS","type":"file","size":10,"download_url":"$raw/b.JS"},
          {"name":"a.zip","type":"file","size":10,"download_url":"$raw/a.zip"},
          {"name":"readme.md","type":"file","size":10,"download_url":"$raw/readme.md"},
          {"name":"folder.js","type":"dir","size":0,"download_url":null},
          {"name":"big.js","type":"file","size":99999999,"download_url":"$raw/big.js"},
          {"name":"x.js","type":"file","size":10,"download_url":"https://example.com/x.js"}
        ]""",
      )

    assertEquals(
      listOf(SourceScript("a.zip", "$raw/a.zip"), SourceScript("b.JS", "$raw/b.JS")),
      ScriptSources.named(page.scripts).map { it.copy(name = it.file) },
    )
    assertEquals(6, page.count)
    assertNull(page.next)
  }

  @Test
  fun aGiteaListingHoldsScriptFilesOnItsHost() {
    val url = "https://codeberg.org/a/b/raw/branch/main/x.js"
    val page =
      ScriptSources.parsePage(
        source("codeberg.org/a/b"),
        """[
          {"name":"x.js","type":"file","size":10,"download_url":"$url"},
          {"name":"y.js","type":"file","size":10,"download_url":"$raw/y.js"},
          {"name":"d","type":"dir","size":0,"download_url":null}
        ]""",
      )

    assertEquals(listOf(SourceScript("x.js", url)), page.scripts)
  }

  @Test
  fun aGitLabListingGetsAddressesMadeForItsFiles() {
    val page =
      ScriptSources.parsePage(
        source("gitlab.com/g/r/-/tree/dev/scripts"),
        """[
          {"name":"x.js","type":"blob","path":"scripts/x.js"},
          {"name":"sub","type":"tree","path":"scripts/sub"},
          {"name":"note.txt","type":"blob","path":"scripts/note.txt"}
        ]""",
      )

    assertEquals(
      listOf(
        SourceScript(
          "x.js",
          "https://gitlab.com/api/v4/projects/g%2Fr/repository/files/scripts%2Fx.js/raw?ref=dev",
        )
      ),
      page.scripts,
    )
    assertEquals(3, page.count)
  }

  @Test
  fun aBitbucketListingFollowsItsNextPageOnBitbucket() {
    val base = "https://api.bitbucket.org/2.0/repositories/ws/repo/src"
    val page =
      ScriptSources.parsePage(
        source("bitbucket.org/ws/repo"),
        """{"values":[
          {"path":"s/x.js","type":"commit_file","size":10,"links":{"self":{"href":"$base/abc/s/x.js"}}},
          {"path":"s/big.js","type":"commit_file","size":99999999,"links":{"self":{"href":"$base/abc/s/big.js"}}},
          {"path":"s/d","type":"commit_directory","links":{"self":{"href":"$base/abc/s/d/"}}}
        ],"next":"$base/HEAD/?page=2"}""",
      )

    assertEquals(listOf(SourceScript("x.js", "$base/abc/s/x.js")), page.scripts)
    assertEquals("$base/HEAD/?page=2", page.next)

    val elsewhere =
      ScriptSources.parsePage(
        source("bitbucket.org/ws/repo"),
        """{"values":[],"next":"https://example.com/?page=2"}""",
      )
    assertNull(elsewhere.next)
  }

  @Test
  fun aNameIsTheFileNameWhenTheShortNameIsEmptyOrNamesTwoFiles() {
    val scripts =
      ScriptSources.named(
        listOf("x.js", "x.zip", ".js", "y.js").map { SourceScript(it, "$raw/$it") }
      )

    assertEquals(listOf(".js", "x.js", "x.zip", "y"), names(scripts))
  }

  @Test
  fun aJsFileIsAtMostOneMegabyteAndAZipFiveInAListing() {
    val js = ScriptPackage.MAX_JS_BYTES
    val zip = ScriptPackage.MAX_BYTES
    val page =
      ScriptSources.parsePage(
        source("github.com/a/b"),
        """[
          {"name":"ok.js","type":"file","size":$js,"download_url":"$raw/ok.js"},
          {"name":"big.js","type":"file","size":${js + 1},"download_url":"$raw/big.js"},
          {"name":"ok.zip","type":"file","size":$zip,"download_url":"$raw/ok.zip"},
          {"name":"big.zip","type":"file","size":${zip + 1},"download_url":"$raw/big.zip"}
        ]""",
      )

    assertEquals(listOf("ok.js", "ok.zip"), page.scripts.map { it.file })
  }

  @Test(expected = ManifestException::class)
  fun aJsFileOverOneMegabyteIsRefusedBeforeItIsRead() {
    ScriptPackage.read(ByteArray(ScriptPackage.MAX_JS_BYTES + 1) { 'a'.code.toByte() })
  }

  @Test(expected = ManifestException::class)
  fun aZipOverFiveMegabytesIsRefusedBeforeItIsRead() {
    ScriptPackage.read(ByteArray(ScriptPackage.MAX_BYTES + 1) { 'P'.code.toByte() })
  }

  @Test(expected = org.json.JSONException::class)
  fun aFileInPlaceOfAFolderIsNotAListing() {
    ScriptSources.parsePage(source("github.com/a/b"), """{"name":"x.js","type":"file"}""")
  }
}
