package com.orbitalhq.query.runtime

import io.kotest.matchers.shouldBe
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.auth.schemes.AuthTokens
import com.orbitalhq.auth.tokens.AuthConfig
import com.orbitalhq.connectors.config.ConnectionsConfig
import com.orbitalhq.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.http.ServicesConfig
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalSerializationApi::class)
class QueryMessageTest {
   @Test
   fun `can serde to CBOR`() {
      val sourcePackages = listOf(
         SourcePackage(
            PackageMetadata.from(
               PackageIdentifier.fromId("com.foo/Test/1.0.0"),
               submissionDate = Instant.parse("2023-04-01T02:10:00Z")
            ),
            sources = listOf(
               VersionedSource(
                  name = "Source.taxi",
                  version = "1.0.90",
                  content = "type Hello"
               )
            ),
            additionalSources = emptyMap()
         )
      )
      val message = QueryMessage(
         query = "find { Hello }",
         sourcePackages = sourcePackages,
         connections = ConnectionsConfig(
            jdbc = mapOf(
               "my-db" to DefaultJdbcConnectionConfiguration(
                  "my-db",
                  JdbcDriver.POSTGRES,
                  connectionParameters = mapOf("username" to "Hello")
               )
            ),
            kafka = emptyMap()
         ),
         authTokens = AuthTokens.empty(),
         services = ServicesConfig.DEFAULT,
         mediaType = "application/json",
         clientQueryId = "123",

      )
      val bytes = QueryMessage.cbor.encodeToByteArray(message)
      val fromBytes = QueryMessage.cbor.decodeFromByteArray<QueryMessage>(bytes)
      fromBytes.shouldBe(message)

      val readSourcePackages = fromBytes.sourcePackages()
      readSourcePackages.shouldBe(sourcePackages)
   }
}
