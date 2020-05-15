package io.vyne.schemaServer

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "taxi")
data class GitSchemaRepoConfig(val schemaLocalStorage: String? = null, val gitSchemaRepos: List<GitRemoteRepo> = listOf()) {
   @ConstructorBinding
   data class GitRemoteRepo(
      val name: String,
      val uri: String,
      val branch: String,
      val sshPrivateKeyPath: String? = null,
      val sshPassPhrase: String? = null)
}

