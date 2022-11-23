package io.vyne.schemaServer.core.git.providers

import io.vyne.schema.publisher.loaders.BranchOverview
import io.vyne.schemaServer.core.git.GitRepositoryConfig
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
      config: GitRepositoryConfig,
      branchName: String,
      description: String,
      author: String
   ): Pair<BranchOverview, String> {
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

         This PR was generated automatically by $author using Vyne"""
      )
      return buildPullRequestOverview(response) to response.htmlUrl.toString()
   }

   private fun buildPullRequestOverview(pullRequest: GHPullRequest): BranchOverview {
      return BranchOverview(
         additions = pullRequest.additions,
         changedFiles = pullRequest.changedFiles,
         deletions = pullRequest.deletions,
         author = pullRequest.user.login,
         description = pullRequest.body,
         lastUpdated = pullRequest.updatedAt,
      )
   }

   private fun getGitHubInstance(config: GitRepositoryConfig): GHRepository {
      return GitHub.connectUsingPassword(config.credentials!!.username, config.credentials.password)
         ?.getRepository(repositoryNameFromUri(config.uri))
         ?: error("Unable to authenticate to GitHub")
   }
}
