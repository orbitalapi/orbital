package com.orbitalhq.schemaServer.core.git.providers

import com.orbitalhq.schema.publisher.loaders.ChangesetOverview
import com.orbitalhq.schemaServer.core.git.GitRepositoryConfig
import java.util.*

/**
 * Only for testing purposes. Does not actually create a PR anywhere.
 */
class NoopProvider : GitHostedService {
   override fun raisePr(
      config: GitRepositoryConfig,
      branchName: String,
      description: String,
      author: String
   ): Pair<ChangesetOverview, String> {
      return ChangesetOverview(0, 0, 0, "", "", Date()) to ""
   }
}
