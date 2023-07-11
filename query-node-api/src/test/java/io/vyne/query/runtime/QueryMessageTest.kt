package io.vyne.query.runtime

import io.kotest.matchers.shouldBe
import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.auth.tokens.AuthConfig
import io.vyne.connectors.config.ConnectionsConfig
import io.vyne.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.config.jdbc.JdbcDriver
import io.vyne.http.ServicesConfig
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
         authTokens = AuthConfig(),
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
