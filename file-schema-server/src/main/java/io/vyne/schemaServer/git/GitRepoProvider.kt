package io.vyne.schemaServer.git

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.nio.file.Path

@ConditionalOnProperty(
   name = ["taxi.gitCloningJobEnabled"],
   havingValue = "true",
   matchIfMissing = false
)
@Component
class GitRepoProvider {
   fun provideRepo(rootPath: Path, repositoryConfig: GitRemoteRepository): GitRepo {
      return GitRepo.forNameInRoot(rootPath, repositoryConfig)
   }
}
