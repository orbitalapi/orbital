package io.vyne.schemaServer.core.git.providers

import io.vyne.schema.publisher.loaders.ChangesetOverview
import io.vyne.schemaServer.core.git.GitRepositoryConfig
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
