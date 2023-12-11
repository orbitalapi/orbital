package com.orbitalhq.schemaServer.core.git.providers

import com.orbitalhq.schema.publisher.loaders.ChangesetOverview
import com.orbitalhq.schemaServer.core.git.GitProjectStoreSpec
import java.util.*

/**
 * Only for testing purposes. Does not actually create a PR anywhere.
 */
class NoopProvider : GitHostedService {
   override fun raisePr(
       config: GitProjectStoreSpec,
       branchName: String,
       description: String,
       author: String
   ): Pair<ChangesetOverview, String> {
      return ChangesetOverview(0, 0, 0, "", "", Date()) to ""
   }
}
