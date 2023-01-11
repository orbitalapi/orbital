package io.vyne.schemaServer.core.git

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.HttpTransport
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig
import org.eclipse.jgit.util.FS

data class CredentialsTransportConfigCallback(val credentials: GitCredentials): TransportConfigCallback {
   override fun configure(transport: Transport) {
      val httpTransport = transport as HttpTransport
      httpTransport.credentialsProvider = UsernamePasswordCredentialsProvider(credentials.username, credentials.password)
   }
}
