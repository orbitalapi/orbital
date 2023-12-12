package com.orbitalhq.schemaServer.core.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.TransportException


enum class OperationResult {
   SUCCESS, FAILURE;

   companion object {
      fun fromBoolean(input: Boolean): OperationResult {
         return if (input) {
            SUCCESS
         } else {
            FAILURE
         }
      }
   }
}

object GitUtils {

      private fun String.objectNameToDisplayName() = this.split("/").last()
      data class TestConnectionResult(
         val successful: Boolean,
         val errorMessage: String?,
         val branchNames: List<String>?,
         val defaultBranch: String?
      ) {
         companion object {
            fun failed(error: String) = TestConnectionResult(
               false,
               error,
               null,
               null
            )
         }
      }

      fun testConnection(url: String): TestConnectionResult {
         val branches = try {
            Git.lsRemoteRepository()
               .setRemote(url)
               .callAsMap()
         } catch (e: TransportException) {
            val message = e.message ?: ""
            if (message.contains("Authentication is required but no CredentialsProvider has been registered")) {
               return TestConnectionResult.failed(
                  "Authentication failed",
               )
            }
            if (message.contains("not found")) {
               return TestConnectionResult.failed("Could not connect to the remote repository")
            }
            return TestConnectionResult.failed("Failed to connect - ${e.message}")
         }
         val defaultBranchName = branches.get("HEAD")
            ?.target?.name?.objectNameToDisplayName()

         val branchNames = branches.keys.filter { it.startsWith("refs/heads/") }
            .map { it.objectNameToDisplayName() }

         return TestConnectionResult(
            true,
            null,
            branchNames,
            defaultBranchName
         )

      }

}
