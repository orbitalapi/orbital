package io.vyne.schemaServer.git

import io.vyne.schemaServer.local.EditableGitRepository
import io.vyne.utils.log
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ResponseStatus
import java.nio.file.Path
import java.nio.file.Paths

@ConditionalOnProperty(
   name = ["taxi.git-sync-enabled"],
   havingValue = "true",
   matchIfMissing = false
)
@Component
class GitRepoProvider(private val gitSchemaRepoConfig: GitSchemaRepoConfig) {
   val rootPath: Path
   private val editableGitRepositories = mutableMapOf<String, EditableGitRepository>()
   val repositories: List<GitRepo>

   init {
      if (gitSchemaRepoConfig.schemaLocalStorage == null) {
         error("taxi.schema-local-storage must be set when taxi.git-sync-enabled is set to true")
      } else {
         rootPath = Paths.get(gitSchemaRepoConfig.schemaLocalStorage)
         if (!rootPath.toFile().exists()) {
            rootPath.toFile().mkdir()
         }
      }
      repositories = gitSchemaRepoConfig.gitSchemaRepositories.map { repositoryConfig ->
         GitRepo.asDirectoryInPath(rootPath, repositoryConfig)
      }
      log().info(
         "GitRepoProvider is started and configured with the following repositories: \n${
            repositories.joinToString(
               "\n"
            ) { it.toString() }
         }"
      )
   }

   fun getRepository(name: String, cloneIfNotPresent: Boolean = false): GitRepo {
      val repo = repositories.firstOrNull { it.name == name }
         ?: throw BadRepositoryRequestException("No repository named $name has been configured.")
      if (cloneIfNotPresent && !repo.existsLocally()) {
         repo.clone()
         repo.checkout()
      }
      return repo
   }

   fun getDefaultEditableRepository(cloneIfNotPresent: Boolean = false): EditableGitRepository {
      val editableRepositories = this.repositories
         .filter { it.editable }
      if (editableRepositories.isEmpty()) {
         throw BadRepositoryRequestException("There are no editable repositories defined")
      }
      if (editableRepositories.size > 1) {
         throw BadRepositoryRequestException("There are multiple editable repositories defined - expected exactly one")
      }
      return getEditableRepository(editableRepositories.first().name, cloneIfNotPresent)
   }

   fun getEditableRepository(name: String, cloneIfNotPresent: Boolean = false): EditableGitRepository {
      return editableGitRepositories.getOrPut(name) {
         val gitRepo = getRepository(name, cloneIfNotPresent)
         if (!gitRepo.editable) {
            throw BadRepositoryRequestException("Repository $name is not editable")
         }
         EditableGitRepository(gitRepo)
      }
   }


   fun provideRepo(rootPath: Path, repositoryConfig: GitRepositoryConfig): GitRepo {
      return GitRepo.asDirectoryInPath(rootPath, repositoryConfig)
   }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRepositoryRequestException(message: String) : RuntimeException(message)
