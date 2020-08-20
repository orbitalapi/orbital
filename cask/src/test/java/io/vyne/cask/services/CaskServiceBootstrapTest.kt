package io.vyne.cask.services

import com.jayway.awaitility.Awaitility
import com.nhaarman.mockito_kotlin.*
import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.caskRecordTable
import io.vyne.cask.query.CaskDAO
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.SimpleTaxiSchemaProvider
import io.vyne.spring.VersionedSchemaProvider
import org.junit.Test
import java.time.Instant
import java.util.concurrent.TimeUnit


class CaskServiceBootstrapTest {
   val caskServiceSchemaGenerator: CaskServiceSchemaGenerator = mock()
   val caskConfigRepository: CaskConfigRepository = mock()

   @Test
   fun `Initialize cask services on startup`() {
      // prepare
      val schemaProvider = SimpleTaxiSchemaProvider("type Order {}")
      val versionedType = schemaProvider.schema().versionedType("Order".fqn())
      val caskConfig = CaskConfig(versionedType.caskRecordTable(), "Order", versionedType.versionHash, emptyList(), emptyList(), null, Instant.now())
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfig))

      // act
      CaskServiceBootstrap(caskServiceSchemaGenerator, schemaProvider, caskConfigRepository, mock {  }, CaskServiceRegenerationRunner()).generateCaskServicesOnStartup()

      // assert
      verify(caskServiceSchemaGenerator,timeout(1000).times(1)).generateAndPublishServices(listOf(CaskTaxiPublicationRequest(versionedType)))
   }

   @Test
   fun `Regenerate cask services when schema changes`() {
      // prepare
      val schemaV1 = "type Order {}"
      val schemaV2 = "type Order { id: String }"
      val taxiSchemaV2 = TaxiSchema.from(schemaV2, "order.taxi", "1.0.1")
      val versionedTypeV2 = taxiSchemaV2.versionedType("Order".fqn())
      val caskConfigV1 = CaskConfig("Order_hash1", "Order", "hash1", emptyList(), emptyList(), null, Instant.now())
      val schemaProviderV2 = VersionedSchemaProvider(versionedTypeV2.sources)
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfigV1))

      // simulate schema change
      val oldSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(VersionedSource("order.taxi", "1.0.0", schemaV1))), 1)
      val newSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(VersionedSource("order.taxi", "1.0.1", schemaV2))), 2)
      val event = SchemaSetChangedEvent(oldSchemaSet, newSchemaSet)

      // act
      CaskServiceBootstrap(caskServiceSchemaGenerator, schemaProviderV2, caskConfigRepository,mock {  }, CaskServiceRegenerationRunner()).regenerateCasksOnSchemaChange(event)

      // assert
      verify(caskServiceSchemaGenerator, timeout(1000).times(1)).generateAndPublishServices(listOf(CaskTaxiPublicationRequest(versionedTypeV2)))
   }

   @Test
   fun `Do not regenerate when schema change contains added cask services`() {
      // prepare
      val schemaV1 = "type Order {}"
      val taxiSchemaV1 = TaxiSchema.from(schemaV1, "order.taxi", "1.0.1")
      val versionedTypeV1 = taxiSchemaV1.versionedType("Order".fqn())
      val caskConfigV1 = CaskConfig("Order_hash1", "Order", "hash1", emptyList(), emptyList(), null, Instant.now())
      val schemaProviderV1 = VersionedSchemaProvider(versionedTypeV1.sources)
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfigV1))

      // simulate schema change
      val versionedSource1 = VersionedSource("order.taxi", "1.0.0", schemaV1)
      val caskServiceAdded = VersionedSource("vyne.casks.Order", "1.1.1", "")
      val oldSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(versionedSource1)), 1)
      val newSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(versionedSource1), ParsedSource(caskServiceAdded)), 2)
      val event = SchemaSetChangedEvent(oldSchemaSet, newSchemaSet)

      // act
      CaskServiceBootstrap(caskServiceSchemaGenerator, schemaProviderV1, caskConfigRepository, mock {  }, CaskServiceRegenerationRunner()).regenerateCasksOnSchemaChange(event)

      // assert
      verify(caskServiceSchemaGenerator, times(0)).generateAndPublishServices(any())
   }

   @Test
   fun `Do not regenerate when schema change contains removed cask services`() {
      // prepare
      val schemaV1 = "type Order {}"
      val taxiSchemaV1 = TaxiSchema.from(schemaV1, "order.taxi", "1.0.1")
      val versionedTypeV1 = taxiSchemaV1.versionedType("Order".fqn())
      val caskConfigV1 = CaskConfig("Order_hash1", "Order", "hash1", emptyList(), emptyList(), null, Instant.now())
      val schemaProviderV1 = VersionedSchemaProvider(versionedTypeV1.sources)
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfigV1))

      // simulate schema change
      val versionedSource1 = VersionedSource("order.taxi", "1.0.0", schemaV1)
      val caskServiceRemoved = VersionedSource("vyne.casks.Order", "1.1.1", "")
      val oldSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(versionedSource1), ParsedSource(caskServiceRemoved)), 1)
      val newSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(versionedSource1)), 2)
      val event = SchemaSetChangedEvent(oldSchemaSet, newSchemaSet)

      // act
      CaskServiceBootstrap(caskServiceSchemaGenerator, schemaProviderV1, caskConfigRepository, mock {  }, CaskServiceRegenerationRunner()).regenerateCasksOnSchemaChange(event)

      // assert
      verify(caskServiceSchemaGenerator, times(0)).generateAndPublishServices(any())
   }

   @Test
   fun `Schema error does not prevent cask starting up`() {
      // prepare
      val schemaProvider = SimpleTaxiSchemaProvider("""
         namespace common.order
         type Order {
            id: String2
         }
      """.trimIndent())
      val caskConfig = CaskConfig("Order_hash1", "common.order.Order", "hash1", emptyList(), emptyList(), null, Instant.now())
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfig))

      // act
      CaskServiceBootstrap(caskServiceSchemaGenerator, schemaProvider, caskConfigRepository, mock {  }, CaskServiceRegenerationRunner()).generateCaskServicesOnStartup()

      // assert
      verify(caskServiceSchemaGenerator, times(0)).generateAndPublishServices(any())
   }
}
