package com.orbitalhq.schemaServer.core.git

/**
 * The config for a git repository we're syncing / polling.
 * Simpler than other implementations, as it's intended for read-only operations
 */
data class SimpleGitRepositoryConnectionConfig(
   /**
    * Used for logging
    */
   override val name: String,
   override val uri: String,
   override val branch: String,

   ) : GitRepositoryConnectionConfig {
   override fun toString(): String {
      return description
   }

   // Not supported, prefer personal access tokens within URLs
   override val credentials: GitCredentials? = null

   // Not supported, prefer personal access tokens within URLs
   override val sshAuth: GitSshAuth? = null
}
