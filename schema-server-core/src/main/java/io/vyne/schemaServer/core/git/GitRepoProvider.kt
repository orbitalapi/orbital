package io.vyne.schemaServer.core.git

//@ConditionalOnProperty(
//   name = ["taxi.gitCloningJobEnabled"],
//   havingValue = "true",
//   matchIfMissing = false
//)
//@Component
//class GitRepoProvider {
//   fun provideRepo(rootPath: String, repoConfig: GitSchemaRepoConfig.GitRemoteRepo): GitRepo {
//      return GitRepo(rootPath, repoConfig)
//   }
//}