package io.vyne.schemaServer.local

import io.vyne.schemaServer.git.Author
import io.vyne.schemaServer.git.GitRepoProvider
import io.vyne.schemaServer.security.SecureProfile
import io.vyne.schemaStore.ResourceEditingResponse
import io.vyne.security.VyneUser
import io.vyne.security.toVyneUser
import io.vyne.utils.log
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Paths
import java.time.Instant

@ConditionalOnProperty(
   name = ["taxi.git-sync-enabled"],
   havingValue = "true",
   matchIfMissing = false
)
@Profile(SecureProfile.SECURE)
@RestController
class SchemaEditingService(private val repoProvider: GitRepoProvider) {

   /**
    * Writes content to the specified repository and branch.
    * The write is committed and pushed immediately.
    */
   @PreAuthorize(value = "isAuthenticated()")
   @PostMapping(value = ["/api/repository/{repositoryName}/branches/{branchName}"])
   fun writeContent(
      @PathVariable("repositoryName") repositoryName: String,
      @PathVariable("branchName") branchName: String,
      @RequestParam("resource") resourceName: String,
      @RequestBody body: ByteArray
   ): ResourceEditingResponse {
      TODO("Branch support not yet implemented")
   }

   /**
    * Writes content to the specified repository.
    * The write is committed and pushed immediately.
    */
   @PreAuthorize(value = "isAuthenticated()")
   @PostMapping(value = ["/api/repository"])
   fun writeContentToDefaultRepository(
      @RequestParam("resource") resourceName: String,
      @RequestBody fileContents: ByteArray,
      auth: Authentication
   ): ResourceEditingResponse {
      val editableGitRepository = repoProvider.getDefaultEditableRepository(cloneIfNotPresent = true)

      return writeContent(editableGitRepository.name, resourceName, fileContents, auth.toVyneUser())
   }

   /**
    * Writes content to the specified repository.
    * The write is committed and pushed immediately.
    */
   @PreAuthorize(value = "isAuthenticated()")
   @PostMapping(value = ["/api/repository/{repositoryName}"])
   fun writeContent(
      @PathVariable("repositoryName") repositoryName: String,
      @RequestParam("resource") resourceName: String,
      @RequestBody fileContents: ByteArray,
      auth: Authentication
   ): ResourceEditingResponse {
      return writeContent(repositoryName, resourceName, fileContents, auth.toVyneUser())
   }

   internal fun writeContent(
      @PathVariable("repositoryName") repositoryName: String,
      @RequestParam("resource") resourceName: String,
      @RequestBody fileContents: ByteArray,
      user: VyneUser
   ): ResourceEditingResponse {
      log().info("Request from ${user.userId} to update file $resourceName in repository $repositoryName")
      val repo = repoProvider.getEditableRepository(repositoryName, cloneIfNotPresent = true)
      val updateResult = repo.updateFile(
         Paths.get(resourceName),
         fileContents,
         user.asGitAuthor(),
         commitMessage = "Updated in Vyne application",
         push = true
      )
      log().info("Update attempt completed: $updateResult")
      return ResourceEditingResponse(
         updateResult.commitResult.shortSha,
         Instant.now(),
         repositoryName,
         resourceName
      )
   }
}


fun VyneUser.asGitAuthor(): Author {
   return Author(
      this.username,
      this.email
   )
}
