package io.vyne.schemaServer

import org.springframework.stereotype.Component

@Component
class GitRepoProvider {
   fun provideRepo(rootPath: String, repoConfig: GitSchemaRepoConfig.GitRemoteRepo): GitRepo {
      return GitRepo(rootPath, repoConfig)
   }
}
