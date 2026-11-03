package io.github.aaron_gh.sandfly.addon

import org.junit.Assert.assertEquals
import org.junit.Test

class AddonUpdaterTest {
  private fun release(build: Int, vararg assets: String) =
    """{"tag_name": "v0.4.0", "body": "build: $build\n\n* Notes", "assets": [${
      assets.joinToString(",") { """{"name": "$it", "browser_download_url": "https://github.com/x/$it", "size": 700000}""" }
    }]}"""

  @Test
  fun aNewerBuildWithTheAddOnIsAnUpdate() {
    val result = AddonUpdater.parse(release(1300, "sandfly.apk", "sandfly-addon.apk"), 1250)
    assertEquals(
      AddonUpdater.Result.Available(AddonUpdater.Update("0.4.0", 1300, "https://github.com/x/sandfly-addon.apk", 700000)),
      result,
    )
  }

  @Test
  fun theSameOrOlderBuildIsCurrent() {
    assertEquals(AddonUpdater.Result.Current, AddonUpdater.parse(release(1250, "sandfly-addon.apk"), 1250))
    assertEquals(AddonUpdater.Result.Current, AddonUpdater.parse(release(1200, "sandfly-addon.apk"), 1250))
  }

  @Test
  fun aReleaseWithoutTheAddOnIsCurrent() {
    assertEquals(AddonUpdater.Result.Current, AddonUpdater.parse(release(1300, "sandfly.apk"), 1250))
  }

  @Test
  fun aReleaseWithoutABuildNumberFails() {
    assertEquals(AddonUpdater.Result.Failed, AddonUpdater.parse("""{"body": "Notes"}""", 1250))
    assertEquals(AddonUpdater.Result.Failed, AddonUpdater.parse("not json", 1250))
  }
}
