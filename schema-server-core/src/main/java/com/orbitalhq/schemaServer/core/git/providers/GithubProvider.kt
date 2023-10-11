package com.orbitalhq.schemaServer.core.git.providers

import com.orbitalhq.schema.publisher.loaders.ChangesetOverview
import com.orbitalhq.schemaServer.core.git.GitRepositorySpec
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub

class GithubProvider : GitHostedService {

   companion object {
      fun repositoryNameFromUri(gitUrl: String): String {
         return if (gitUrl.startsWith("git@")) {
            gitUrl.split(":")
               .last()
               .removeSuffix(".git")
         } else {
            gitUrl.split("/")
               .takeLast(2)
               .joinToString("/")
               .removeSuffix(".git")
         }
      }
   }

   override fun raisePr(
       config: GitRepositorySpec,
       branchName: String,
       description: String,
       author: String
   ): Pair<ChangesetOverview, String> {
      if (config.pullRequestConfig == null) {
         error("Don't know how to finalize changes for $branchName as there's no update flow config defined.")
      }
      // TODO Handle auth properly
      val repo = getGitHubInstance(config)
      val response = repo.createPullRequest(
         "Update $branchName",
         config.pullRequestConfig.branchPrefix + branchName,
         config.branch,
         """$description

         This PR was generated automatically by $author using Vyne""".trimIndent()
      )
      return buildPullRequestOverview(response) to response.htmlUrl.toString()
   }

   private fun buildPullRequestOverview(pullRequest: GHPullRequest): ChangesetOverview {
      /**
       * TODO Additions and deletions refer to line counts here. The way GitHub API works is that it only provides these
       * and changed files count on the overall PR endpoint. To get the actual files added and deleted one needs to call
       * the endpoint for fetching details of all the files of the PR and parse that through.
       */
      return ChangesetOverview(
         additions = pullRequest.additions,
         changedFiles = pullRequest.changedFiles,
         deletions = pullRequest.deletions,
         author = pullRequest.user.login,
         description = pullRequest.body,
         lastUpdated = pullRequest.updatedAt,
      )
   }

   private fun getGitHubInstance(config: GitRepositorySpec): GHRepository {
      return GitHub.connectUsingPassword(config.credentials!!.username, config.credentials.password)
         ?.getRepository(repositoryNameFromUri(config.uri))
         ?: error("Unable to authenticate to GitHub")
   }
}
