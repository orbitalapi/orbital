package com.orbitalhq.internal.packagePublisher

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import picocli.CommandLine
import java.nio.file.Path
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig
import org.eclipse.jgit.util.FS
import java.io.File

class GitTransport(
   private val repoUrl: String,
   private val workDir: Path,
   private val sshKeyPath: String,
   private val commandLine: CommandLine
) {

   private fun printOut(message: String) = commandLine.out.println(message)

   fun cloneRepository(): Git {
      printOut("Cloning repository...")
      val sessionFactory = object : JschConfigSessionFactory() {
         override fun configure(host: OpenSshConfig.Host, session: Session) {
            session.setConfig("StrictHostKeyChecking", "no")
         }

         override fun createDefaultJSch(fs: FS?): JSch {
            val defaultJSch = super.createDefaultJSch(fs)
            defaultJSch.addIdentity(sshKeyPath)
            return defaultJSch
         }
      }

      return Git.cloneRepository()
         .setURI(repoUrl)
         .setDirectory(workDir.toFile())
         .setTransportConfigCallback { transport ->
            (transport as? SshTransport)?.sshSessionFactory = sessionFactory
         }
         .call()
   }

   fun commit(git:Git, tag:String?, push:Boolean = true) {
      printOut("Committing changes...")
      git.add().addFilepattern(".").call()
      git.commit()
         .setMessage("Automated update")
         .setAllowEmpty(false)
         .call()

      tag?.let {
         printOut("Adding tag: $it")
         git.tag().setName(it).call()
      }

      if (push) {
         printOut("Pushing changes...")
         git.push().call()
         if (tag != null) {
            git.push().setPushTags().call()
         }
      }
   }
}
