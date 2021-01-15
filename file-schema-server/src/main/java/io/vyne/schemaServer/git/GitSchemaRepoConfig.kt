package io.vyne.schemaServer.git

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "taxi")
data class GitSchemaRepoConfig(
   val schemaLocalStorage: String? = null,
   val gitSchemaRepositories: List<GitRepositoryConfig> = listOf()
)

@ConstructorBinding
data class GitRepositoryConfig(
   val name: String,
   val uri: String,
   val branch: String,
   val editable: Boolean = false,
   val sshPrivateKeyPath: String? = null,
   val sshPassPhrase: String? = null
)
