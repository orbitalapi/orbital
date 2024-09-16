package com.orbitalhq.schemaServer.core.git

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.orbitalhq.schema.publisher.loaders.ProjectTransportConfig
import com.orbitalhq.schemaServer.packages.PackageLoaderSpec
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.schemaServer.repositories.git.GitUpdateFlowConfig
import mu.KotlinLogging
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

/**
 * Models the configuration options for defining git projects within a workspace (workspace.conf file)
 * This class models the git {} block inside a workspace.conf configuration file
 */
data class WorkspaceGitProjectConfig(
   val checkoutRoot: Path = Paths.get("orbital/workspace/projects/"),
   val pollFrequency: Duration = Duration.ofSeconds(30),
   val repositories: List<GitProjectSpec> = emptyList(),
) {
   companion object {
      fun default():WorkspaceGitProjectConfig = WorkspaceGitProjectConfig()
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

/**
 * Models the configuration of a workspace project
 * that is loaded from git.
 */
data class GitProjectSpec(
   override val name: String,

   @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
   override val uri: String,
   override val branch: String,
   @get:JsonIgnore
   override val sshAuth: GitSshAuth? = null,
   @get:JsonIgnore
   override val credentials: GitCredentials? = null,
   val pullRequestConfig: GitUpdateFlowConfig? = null,
   val isEditable: Boolean = pullRequestConfig != null,


   /**
    * The Path within the repository
    */
   @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
   val path: Path = Paths.get("/"),
   val loader: PackageLoaderSpec = TaxiPackageLoaderSpec
) : GitRepositoryConnectionConfig, ProjectTransportConfig {
   @JsonProperty("uri", access = JsonProperty.Access.READ_ONLY)
   val redactedUri = redactUrl(uri)

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
