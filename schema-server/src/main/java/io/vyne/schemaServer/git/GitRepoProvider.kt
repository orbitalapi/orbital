package io.vyne.schemaServer.git

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@ConditionalOnProperty(
   name = ["taxi.gitCloningJobEnabled"],
   havingValue = "true",
   matchIfMissing = false
)
@Component
class GitRepoProvider {
   fun provideRepo(rootPath: String, repoConfig: GitSchemaRepoConfig.GitRemoteRepo): GitRepo {
      return GitRepo(rootPath, repoConfig)
   }
}
