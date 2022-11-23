package io.vyne.schemaServer.core.git.providers

import io.vyne.schema.publisher.loaders.BranchOverview
import io.vyne.schemaServer.core.git.GitRepositoryConfig

interface GitHostedService {
   fun raisePr(
      config: GitRepositoryConfig,
      branchName: String,
      description: String,
      author: String
   ): Pair<BranchOverview, String>
}

enum class GitHostingProvider {
   Github,
   Gitlab
}

class GitHostingProviderRegistry {
   fun getService(config: GitRepositoryConfig): GitHostedService {
      return when (config.pullRequestConfig?.hostingProvider) {
         null -> error("No hosting provider defined")
         GitHostingProvider.Github -> GithubProvider()
         GitHostingProvider.Gitlab -> TODO("Gitlab support not implemented")
      }
   }
}
