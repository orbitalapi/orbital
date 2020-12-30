package io.vyne.schemaServer.git

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "taxi")
data class GitSchemaRepoConfig(val schemaLocalStorage: String? = null, val gitSchemaRepositories: List<GitRemoteRepository> = listOf())

@ConstructorBinding
data class GitRemoteRepository(
   val name: String,
   val uri: String,
   val branch: String,
   val sshPrivateKeyPath: String? = null,
   val sshPassPhrase: String? = null)
