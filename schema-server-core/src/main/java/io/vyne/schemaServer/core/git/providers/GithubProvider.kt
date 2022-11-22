package io.vyne.schemaServer.core.git.providers

import io.vyne.schemaServer.core.git.GitRepositoryConfig
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

   override fun raisePr(config: GitRepositoryConfig, branchName: String, description: String, author: String): String {
      if (config.pullRequestConfig == null) {
         error("Don't know how to finalize changes for $branchName as there's no update flow config defined.")
      }
      // TODO Handle auth properly
      val github = GitHub.connectUsingPassword(config.credentials!!.username, config.credentials.password)
      val repo = github.getRepository(repositoryNameFromUri(config.uri))
      val response = repo.createPullRequest(
         "Update $branchName",
         config.pullRequestConfig.branchPrefix + branchName,
         config.branch,
         """$description

         This PR was generated automatically by $author using Vyne"""
      )
      return response.htmlUrl.toString()
   }
}
