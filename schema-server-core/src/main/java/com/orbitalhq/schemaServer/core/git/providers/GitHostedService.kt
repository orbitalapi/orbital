package com.orbitalhq.schemaServer.core.git.providers

import com.orbitalhq.schema.publisher.loaders.ChangesetOverview
import com.orbitalhq.schemaServer.core.git.GitProjectStoreSpec
import com.orbitalhq.schemaServer.repositories.git.GitHostingProvider

interface GitHostedService {
   fun raisePr(
       config: GitProjectStoreSpec,
       branchName: String,
       description: String,
       author: String
   ): Pair<ChangesetOverview, String>
}

class GitHostingProviderRegistry {
   fun getService(config: GitProjectStoreSpec): GitHostedService {
      return when (config.pullRequestConfig?.hostingProvider) {
         null -> error("No hosting provider defined")
         GitHostingProvider.Noop -> NoopProvider()
         GitHostingProvider.Github -> GithubProvider()
         GitHostingProvider.Gitlab -> TODO("Gitlab support not implemented")
      }
   }
}
