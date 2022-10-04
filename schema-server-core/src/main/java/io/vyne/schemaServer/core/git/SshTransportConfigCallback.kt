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

data class SshTransportConfigCallback(val sshPrivateKeyPath: String, val sshPassPhrase: String? = null): TransportConfigCallback {
   private val sshSessionFactory: SshSessionFactory = object : JschConfigSessionFactory() {
      override fun configure(hc: OpenSshConfig.Host?, session: Session) {
         session.setConfig("StrictHostKeyChecking", "no")
      }

      override fun createDefaultJSch(fs: FS?): JSch {
         val jSch = super.createDefaultJSch(fs)

         if(sshPassPhrase.isNullOrEmpty()) {
            jSch.addIdentity(sshPrivateKeyPath)
         } else {
            jSch.addIdentity(sshPrivateKeyPath, sshPassPhrase.toByteArray())
         }

         return jSch
      }
   }

   override fun configure(transport: Transport) {
      val sshTransport = transport as SshTransport
      sshTransport.sshSessionFactory = sshSessionFactory
   }
}
