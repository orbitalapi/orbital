package io.vyne.schemaServer.git

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Paths
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.schema-server.git")
data class GitSchemaConfig(
   val checkoutRoot: String? = null,
   val pollFrequency: Duration = Duration.ofSeconds(30),
   val repositories: List<GitRepositoryConfig> = emptyList(),
) {

}

@ConstructorBinding
data class GitRepositoryConfig(
   val name: String,
   val uri: String,
   val branch: String,
   val sshPrivateKeyPath: String? = null,
   val sshPassPhrase: String? = null,
) {
   val description: String = "$name - $uri / $branch"
}

@Configuration
class GitSchemaConfiguration {

   @Bean
   fun buildGitRepositories(config: GitSchemaConfig): List<GitRepositorySourceLoader> {
      return if (config.repositories.isNotEmpty()) {
         val localProjectRoot =
            requireNotNull(config.checkoutRoot) { "A localProjectRoot must be configured if git repositories are provided" }
         config.repositories.map { repositoryConfig -> buildRepository(localProjectRoot, repositoryConfig) }
      } else {
         emptyList()
      }
   }

   fun buildRepository(checkoutRoot: String, repo: GitRepositoryConfig): GitRepositorySourceLoader {
      val workingDir = Paths.get(checkoutRoot, repo.name).toFile()
      return GitRepositorySourceLoader(workingDir, repo)
   }
}
