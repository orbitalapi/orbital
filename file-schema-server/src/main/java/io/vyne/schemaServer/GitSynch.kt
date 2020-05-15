package io.vyne.schemaServer

import io.vyne.utils.log
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@Component
class GitSynch(private val gitSchemaRepoConfig: GitSchemaRepoConfig) {
   private var inProgress = AtomicBoolean(false)

   fun cloneRepos(cloneCommand: CloneCommand) {
      if(inProgress.get()) {
         log().warn("Another cloning process is running, exiting.")
         return
      }
      inProgress.set(true)

      val rootDir = File(gitSchemaRepoConfig.schemaLocalStorage.toString())

      if (!rootDir.exists()) {
         rootDir.mkdir()
      }

      gitSchemaRepoConfig.gitSchemaRepos.forEach { repo ->
         log().info("Cloning repository: ${repo.name} - ${repo.uri} / ${repo.branch}")

         val workingDir = File(rootDir, repo.name)

         // TODO implement a proper client
         if (workingDir.exists()) {
            workingDir.deleteRecursively()
         }
         workingDir.mkdir()

         try {
            var git = cloneCommand
               .setDirectory(workingDir)
               .setURI(repo.uri)
               .setBranch(repo.branch)

            if (!repo.sshPrivateKeyPath.isNullOrEmpty()) {
               val transportConfigCallback: TransportConfigCallback = SshTransportConfigCallback(repo.sshPrivateKeyPath, repo.sshPassPhrase)

               git = git.setTransportConfigCallback(transportConfigCallback)
            }

            git.call().close()
         } catch (e: Exception) {
            log().error("Cloning error in ${repo.uri} / ${repo.branch}", e)
         }
      }

      inProgress.set(false)
   }
}
