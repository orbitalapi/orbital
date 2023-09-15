package com.orbitalhq.schemaServer.core.git

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.orbitalhq.schemaServer.packages.PackageLoaderSpec
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.schemaServer.repositories.git.GitUpdateFlowConfig
import mu.KotlinLogging
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

data class GitSchemaRepositoryConfig(
   val checkoutRoot: Path = Paths.get("./gitProjects/"),
   val pollFrequency: Duration = Duration.ofSeconds(30),
   val repositories: List<GitRepositorySpec> = emptyList(),
) {
   companion object {
      fun default():GitSchemaRepositoryConfig = GitSchemaRepositoryConfig()
   }
}

data class GitCredentials(
   val username: String,
   val password: String,
)
data class GitSshAuth(
   val privateKeyPath: String,
   val passphrase: String? = null,
)

data class GitRepositorySpec(
   val name: String,

   @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
   val uri: String,
   val branch: String,
   @get:JsonIgnore
   val sshAuth: GitSshAuth? = null,
   @get:JsonIgnore
   val credentials: GitCredentials? = null,
   val pullRequestConfig: GitUpdateFlowConfig? = null,
   val isEditable: Boolean = pullRequestConfig != null,


   /**
    * The Path within the repository
    */
   @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
   val path: Path = Paths.get("/"),
   val loader: PackageLoaderSpec = TaxiPackageLoaderSpec
) {
   @JsonProperty("uri", access = JsonProperty.Access.READ_ONLY)
   val redactedUri = redactUrl(uri)

   val description: String = "$name - $redactedUri / $branch"

   @JsonProperty("path", access = JsonProperty.Access.READ_ONLY)
   val pathWithinRepository = path.toString()

   companion object {
      private val logger = KotlinLogging.logger {}
      fun redactUrl(url: String): String {
         return try {
            val uri = URI.create(url)
            val redactedUserInfo = when {
               uri.userInfo == null -> null
               uri.userInfo.length > 6 -> uri.userInfo.substring(0, 3) + "***"
               else -> "***"
            }
            UriComponentsBuilder.fromUri(uri)
               .userInfo(redactedUserInfo)
               .build()
               .toUriString()
         } catch (e: Exception) {
            logger.warn(e) { "Exception thrown trying to redact url $url" }
            "*****";
         }

      }
   }
}
