package com.orbitalhq.schemaServer.core.git

import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

data class CredentialsTransportConfigCallback(val credentials: GitCredentials): TransportConfigCallback {
   override fun configure(transport: Transport) {
      transport.credentialsProvider = UsernamePasswordCredentialsProvider(credentials.username, credentials.password)
   }
}
