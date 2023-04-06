package io.vyne.query.runtime

import io.vyne.SourcePackage
import io.vyne.auth.tokens.AuthConfig
import io.vyne.http.ServicesConfig
import io.vyne.query.ResultMode

/**
 * Fully encapsulates everything required to
 * execute a query on a standalone Vyne instance
 */
data class QueryMessage(
   val query: String,
   val sourcePackages: List<SourcePackage>,
   // Careful: If you use an object mapper to convert this to another connection type
   // (eg:ObjectMapper.convertValue<JdbcConnections>(connections)),
   // you need to register that type in NativeQueryNodeRuntimeHints
   // as it's not available for reflection at runtime in this image.
   val connections: Map<String, Any>,
   val authTokens: AuthConfig,
   val services: ServicesConfig,
   val resultMode: ResultMode = ResultMode.RAW,
   val mediaType: String,
   val clientQueryId: String
)
