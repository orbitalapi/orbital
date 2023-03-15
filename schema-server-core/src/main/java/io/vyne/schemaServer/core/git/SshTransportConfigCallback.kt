package io.vyne.schemaServer.core.git

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig
import org.eclipse.jgit.util.FS

data class SshTransportConfigCallback(val sshAuth: GitSshAuth): TransportConfigCallback {
   private val sshSessionFactory: SshSessionFactory = object : JschConfigSessionFactory() {
      override fun configure(hc: OpenSshConfig.Host?, session: Session) {
         session.setConfig("StrictHostKeyChecking", "no")
      }

      override fun createDefaultJSch(fs: FS?): JSch {
         val jSch = super.createDefaultJSch(fs)

         if(sshAuth.passphrase.isNullOrEmpty()) {
            jSch.addIdentity(sshAuth.privateKeyPath)
         } else {
            jSch.addIdentity(sshAuth.privateKeyPath, sshAuth.passphrase.toByteArray())
         }

         return jSch
      }
   }

   override fun configure(transport: Transport) {
      val sshTransport = transport as SshTransport
      sshTransport.sshSessionFactory = sshSessionFactory
   }
}
