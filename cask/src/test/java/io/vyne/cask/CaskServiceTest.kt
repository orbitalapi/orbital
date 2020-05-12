package io.vyne.cask

import arrow.core.Either
import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.*
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.ingest.Ingester
import io.vyne.cask.ingest.IngesterFactory
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.SimpleSchema
import org.junit.Test
import org.springframework.context.ApplicationEventPublisher
import reactor.core.publisher.Flux
import java.io.File
import java.io.InputStream

class CaskServiceTest {
   val schemaProvider: SchemaProvider = schemaProvider()
   val ingesterFactory = mock<IngesterFactory>()
   val ingester = mock<Ingester>()
   val applicationEventPublisher = mock<ApplicationEventPublisher>()

   @Test
   fun testRequestedTypeNotFound() {
      val service = CaskService(schemaProvider, ingesterFactory, applicationEventPublisher)
      val expectedError = Either.left(CaskService.TypeError("Type reference 'WrongType' not found."))
      service.resolveType("WrongType").should.equal(expectedError)
   }

   @Test
   fun testEmptySchema() {
      val schemaProviderMock = mock<SchemaProvider>()
      whenever(schemaProviderMock.schema()).thenReturn(SimpleSchema(emptySet(), emptySet()))
      val service = CaskService(schemaProviderMock, ingesterFactory, applicationEventPublisher)
      val expectedError = Either.left(CaskService.TypeError("Empty schema, no types defined."))
      service.resolveType("WrongType").should.equal(expectedError)
   }

   @Test
   fun testRequestedTypeValid() {
      val service = CaskService(schemaProvider, ingesterFactory, applicationEventPublisher)
      service.resolveType("OrderWindowSummary").isRight().should.equal(true)
   }

   @Test
   fun testIngestRequest() {
      val versionedTypeReference = VersionedTypeReference.parse("OrderWindowSummary")
      val versionedType = schemaProvider.schema().versionedType(versionedTypeReference)
      val service = CaskService(schemaProvider, ingesterFactory, applicationEventPublisher)
      val resource = Resources.getResource("Coinbase_BTCUSD.json").toURI()
      val input: Flux<InputStream> = Flux.just(File(resource).inputStream())
      whenever(ingesterFactory.create(isA())).thenReturn(ingester)
      whenever(ingester.ingest()).thenReturn(Flux.empty())

      service.ingestRequest(versionedType, input)

      verify(ingester, times(1)).ingest()
   }

   fun schemaProvider(): SchemaProvider {
      return object : SchemaProvider {
         override fun schemas(): List<Schema> = listOf(CoinbaseJsonOrderSchema.schemaV1)
      }
   }
}
