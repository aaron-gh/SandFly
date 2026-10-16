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

import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder

/** How a hosting service lists and serves the files of a repository. */
enum class SourceApi {
  GITHUB,
  GITLAB,
  /** Gitea, and Forgejo, which Codeberg runs. */
  GITEA,
  BITBUCKET,
}

/** The hosting services a script source can be on. Others can't be told to be safe to use. */
enum class SourceHost(val domain: String, val label: String, val api: SourceApi) {
  GITHUB("github.com", "GitHub", SourceApi.GITHUB),
  GITLAB("gitlab.com", "GitLab", SourceApi.GITLAB),
  CODEBERG("codeberg.org", "Codeberg", SourceApi.GITEA),
  GITEA("gitea.com", "Gitea", SourceApi.GITEA),
  BITBUCKET("bitbucket.org", "Bitbucket", SourceApi.BITBUCKET);

  /** What every address that a source's files are read from starts with. */
  val filesPrefix: String
    get() =
      when (this) {
        GITHUB -> "https://raw.githubusercontent.com/"
        GITLAB -> "https://gitlab.com/api/v4/projects/"
        CODEBERG,
        GITEA -> "https://$domain/"
        BITBUCKET -> "https://api.bitbucket.org/2.0/repositories/"
      }

  companion object {
    fun of(domain: String?): SourceHost? {
      val name = domain?.lowercase()?.removePrefix("www.")
      return entries.firstOrNull { it.domain == name }
    }
  }
}

/**
 * A repository, or a folder in one, that holds script files, on one of the [SourceHost]s. The list
 * of a source is the `.js` and `.zip` files directly in it. [owner] is the user or organization, or
 * for GitLab the group, which can hold subgroups: `group/subgroup`.
 */
data class ScriptSource(
  val host: SourceHost,
  val owner: String,
  val repo: String,
  val ref: String?,
  val path: List<String>,
) {
  /** The address the user entered, in its normal form. It is also what is saved. */
  val address: String
    get() {
      val where =
        when (host.api) {
          SourceApi.GITHUB -> listOf("tree")
          SourceApi.GITLAB -> listOf("-", "tree")
          SourceApi.GITEA -> listOf("src", "branch")
          SourceApi.BITBUCKET -> listOf("src")
        }
      val tail = ref?.let { where + encode(it) + path.map(::encode) }.orEmpty()
      return (listOf("https://${host.domain}", owner, repo) + tail).joinToString("/")
    }

  val title: String
    get() = "$owner/$repo"

  /** The service, and which branch and folder, if not the default branch and the top folder. */
  val where: String
    get() =
      listOfNotNull(host.label, ref, path.joinToString("/").ifEmpty { null }).joinToString(": ")

  /** The API address of the first page of the folder's list of files. */
  val listUrl: String
    get() {
      val folder = path.joinToString("") { "/" + encode(it) }
      return when (host.api) {
        SourceApi.GITHUB ->
          "https://api.github.com/repos/$owner/$repo/contents$folder" + query("ref" to ref)
        SourceApi.GITEA ->
          "https://${host.domain}/api/v1/repos/$owner/$repo/contents$folder" +
            query("ref" to ref)
        SourceApi.GITLAB ->
          "https://gitlab.com/api/v4/projects/$projectId/repository/tree" +
            query(
              "per_page" to GITLAB_PAGE.toString(),
              "ref" to ref,
              "path" to path.joinToString("/").ifEmpty { null },
            )
        SourceApi.BITBUCKET ->
          "https://api.bitbucket.org/2.0/repositories/$owner/$repo/src/" +
            encode(ref ?: "HEAD") +
            "$folder/?pagelen=100"
      }
    }

  /** GitLab's name for the repository: its path, with its slashes encoded. */
  val projectId: String
    get() = encode("$owner/$repo")

  companion object {
    /** How many files GitLab lists on a page, the most it allows. */
    const val GITLAB_PAGE = 100

    private val NAME = Regex("[A-Za-z0-9._-]+")
    private val DOTS = setOf(".", "..")
    private val GITEA_REFS = setOf("branch", "tag", "commit")

    /**
     * Reads the address of a repository, such as `github.com/owner/repo`, or of a branch or folder
     * of one, such as `github.com/owner/repo/tree/branch/folder`,
     * `gitlab.com/group/repo/-/tree/branch`, `codeberg.org/owner/repo/src/branch/main/folder` or
     * `bitbucket.org/owner/repo/src/main/folder`. Returns null for anything else. A branch name
     * with a slash in it can't be told from a folder.
     */
    fun parse(text: String): ScriptSource? {
      val trimmed = text.trim().let { if (it.contains("://")) it else "https://$it" }
      val uri =
        try {
          URI(trimmed)
        } catch (e: URISyntaxException) {
          return null
        }
      if (!"https".equals(uri.scheme, ignoreCase = true)) return null
      val host = SourceHost.of(uri.host) ?: return null
      val segments = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
      if (segments.size < 2) return null
      return when (host.api) {
        SourceApi.GITLAB -> parseGitLab(host, segments)
        else -> {
          val tail = segments.drop(2)
          val refAndPath =
            when {
              tail.isEmpty() -> emptyList()
              host.api == SourceApi.GITHUB && tail[0] == "tree" -> tail.drop(1)
              host.api == SourceApi.GITEA && tail[0] == "src" && tail.getOrNull(1) in GITEA_REFS ->
                tail.drop(2)
              host.api == SourceApi.BITBUCKET && tail[0] == "src" -> tail.drop(1)
              else -> return null
            }
          if (tail.isNotEmpty() && refAndPath.isEmpty()) return null
          make(host, segments.take(1), segments[1], refAndPath)
        }
      }
    }

    // The group, with any subgroups, and the repository come before "/-/", if there is one.
    private fun parseGitLab(host: SourceHost, segments: List<String>): ScriptSource? {
      val marker = segments.indexOf("-")
      val project = if (marker < 0) segments else segments.take(marker)
      val rest = if (marker < 0) emptyList() else segments.drop(marker + 1)
      if (project.size < 2) return null
      if (rest.isNotEmpty() && (rest[0] != "tree" || rest.size < 2)) return null
      return make(host, project.dropLast(1), project.last(), rest.drop(1))
    }

    private fun make(
      host: SourceHost,
      owner: List<String>,
      repo: String,
      refAndPath: List<String>,
    ): ScriptSource? {
      val name = repo.removeSuffix(".git")
      if ((owner + name).any { !NAME.matches(it) || it in DOTS }) return null
      if (refAndPath.any { it in DOTS }) return null
      return ScriptSource(
        host,
        owner.joinToString("/"),
        name,
        refAndPath.firstOrNull(),
        refAndPath.drop(1),
      )
    }

    private fun query(vararg parameters: Pair<String, String?>): String =
      parameters
        .filter { it.second != null }
        .joinToString("&", prefix = "?") { (key, value) -> "$key=" + encode(value.orEmpty()) }
        .takeIf { parameters.any { it.second != null } }
        .orEmpty()

    fun encode(text: String): String = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
  }
}
