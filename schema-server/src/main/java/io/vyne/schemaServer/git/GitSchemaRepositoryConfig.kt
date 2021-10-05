package io.vyne.schemaServer.git

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import java.time.Duration

data class GitSchemaRepositoryConfig(
   val checkoutRoot: Path? = null,
   val pollFrequency: Duration = Duration.ofSeconds(30),
   val repositories: List<GitRepositoryConfig> = emptyList(),
) {

}

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
   fun buildGitRepositories(config: GitSchemaRepositoryConfig): List<GitRepositorySourceLoader> {
      return if (config.repositories.isNotEmpty()) {
         val localProjectRoot =
            requireNotNull(config.checkoutRoot) { "A localProjectRoot must be configured if git repositories are provided" }
         config.repositories.map { repositoryConfig -> buildRepository(localProjectRoot, repositoryConfig) }
      } else {
         emptyList()
      }
   }

   fun buildRepository(checkoutRoot: Path, repo: GitRepositoryConfig): GitRepositorySourceLoader {
      val workingDir = checkoutRoot.resolve(repo.name).toFile()
      return GitRepositorySourceLoader(workingDir, repo)
   }
}
