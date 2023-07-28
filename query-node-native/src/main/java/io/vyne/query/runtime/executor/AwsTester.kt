package io.vyne.query.runtime.executor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.auth.schemes.AuthTokens
import io.vyne.auth.tokens.AuthConfig
import io.vyne.connectors.config.ConnectionsConfig
import io.vyne.http.ServicesConfig
import io.vyne.query.runtime.QueryMessage
import io.vyne.query.runtime.QueryMessageCborWrapper
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping

//@RestController
class AwsTester {
   @GetMapping("/2018-06-01/runtime/invocation/next", produces = [MediaType.APPLICATION_CBOR_VALUE])
   fun getNextMessage(): String {
      val query = QueryMessage(
         query = "given { f:String = 'Hello, world' } find { response : String }",
         sourcePackages = listOf(
            SourcePackage(
               PackageMetadata.from(PackageIdentifier.fromId("com.foo/test/1.0.0")),
               sources = listOf(
                  VersionedSource(
                     "MyTest", "1.0.0", "type Hello"
                  )
               )
            )
         ),
         connections = ConnectionsConfig(),
         authTokens = AuthTokens.empty(),
         services = ServicesConfig.DEFAULT,
         mediaType = "application/json",
         clientQueryId = "123"
      )
      val wrapper = QueryMessageCborWrapper.from(query)
      return jacksonObjectMapper().writeValueAsString(wrapper)
//      return """
//         {
//             "query" : "given { f:String = 'Hello, world' } find { response : String }",
//             "sourcePackages" : [],
//             "connections" : {},
//             "authTokens": {},
//             "services" : {},
//             "mediaType" : "application/json",
//             "clientQueryId": "123"
//         }
//      """.trimIndent()
   }
}
