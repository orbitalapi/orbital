package com.orbitalhq.query.runtime.core.dispatcher.http

import com.orbitalhq.auth.schemes.AuthSchemeRepository
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.http.ServicesConfigRepository
import com.orbitalhq.query.ResultMode
import com.orbitalhq.query.runtime.QueryMessage
import com.orbitalhq.schema.api.SchemaProvider
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

/**
 * Responsible for consstructing a self-contained query message,
 * that has both the query itself, and all supporting connections, auth, etc.
 */
@Component
class QueryMessageFactory(
   private val servicesRepository: ServicesConfigRepository,
   private val authTokenRepository: AuthSchemeRepository,
   private val connectionsConfigProvider: SourceLoaderConnectorsRegistry,
   private val schemaProvider: SchemaProvider,
) {

   fun buildQueryMessage(
      query: String,
      clientQueryId: String,
      mediaType: String,
      resultMode: ResultMode,
      arguments: Map<String, Any?>
   ): QueryMessage {
      return QueryMessage(
         query = query,
         sourcePackages = schemaProvider.schema.packages,
         connections = connectionsConfigProvider.load(),
         authTokens = authTokenRepository.getAllTokens(),
         services = servicesRepository.load(),
         resultMode, mediaType, clientQueryId,
         arguments
      )
   }
}
