package io.vyne.queryService

import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SourceSubmissionResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.function.client.WebClient

@RunWith(SpringRunner::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publicationMethod=LOCAL"
   ])
class VyneLocalSchemaStoreIntegrationTest {
   @LocalServerPort
   val randomServerPort = 0

   @Test
   fun `should expose REST api for submitting and querying for taxi schemas`() {
      // prepare
      val client = WebClient
         .builder()
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      // act
      val expectedVersionedSource = VersionedSource("test-schema", "1.0.0", "type OrderId inherits String")
      client
         .post()
         .uri("/api/schemas/taxi")
         .bodyValue(listOf(expectedVersionedSource))
         .retrieve()
         .bodyToMono(SourceSubmissionResponse::class.java)
         .block()
         .isValid.should.be.`true`

      // assert submitted schema is in store
      val schemas = client
         .get()
         .uri("/api/schemas/taxi")
         .retrieve()
         .bodyToMono(SchemaSet::class.java)
         .block()

      schemas.sources.size.should.be.equal(1)
      schemas.sources[0].source.should.be.equal(expectedVersionedSource)
   }
}
