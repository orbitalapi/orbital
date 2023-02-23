package io.vyne.schemaServer.core.git.providers

import io.vyne.schema.publisher.loaders.ChangesetOverview
import io.vyne.schemaServer.core.git.GitRepositoryConfig
import io.vyne.schemaServer.repositories.git.GitHostingProvider

interface GitHostedService {
   fun raisePr(
      config: GitRepositoryConfig,
      branchName: String,
      description: String,
      author: String
   ): Pair<ChangesetOverview, String>
}

class GitHostingProviderRegistry {
   fun getService(config: GitRepositoryConfig): GitHostedService {
      return when (config.pullRequestConfig?.hostingProvider) {
         null -> error("No hosting provider defined")
         GitHostingProvider.Noop -> NoopProvider()
         GitHostingProvider.Github -> GithubProvider()
         GitHostingProvider.Gitlab -> TODO("Gitlab support not implemented")
      }
   }
}
