package io.vyne.cask.services

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.timeout
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.caskRecordTable
import io.vyne.cask.ddl.views.CaskViewService
import io.vyne.cask.ingest.IngestionEventHandler
import io.vyne.cask.upgrade.CaskSchemaChangeDetector
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.publisher.PublisherConfiguration
import io.vyne.schema.publisher.VersionedSourceSubmission
import io.vyne.schema.spring.SimpleTaxiSchemaProvider
import io.vyne.schema.spring.VersionedSchemaProvider
import io.vyne.schemaStore.TaxiSchemaStoreService
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.types.ObjectType
import org.junit.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant


class CaskServiceBootstrapTest {
   val caskServiceSchemaGenerator: CaskServiceSchemaGenerator = mock()
   val caskConfigRepository: CaskConfigRepository = mock()
   val changeDetector: CaskSchemaChangeDetector = mock()
   val eventPublisher: ApplicationEventPublisher = mock()
   val ingestionEventHandler: IngestionEventHandler = mock()

   @Test
   fun `Initialize cask services on startup`() {
      // prepare
      val schemaProvider = SimpleTaxiSchemaProvider("type Order {}")
      val versionedType = schemaProvider.schema().versionedType("Order".fqn())
      val caskConfig = CaskConfig(versionedType.caskRecordTable(), "Order", versionedType.versionHash, emptyList(), emptyList(), null, Instant.now())
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfig))

      // act
      CaskServiceBootstrap(
         caskServiceSchemaGenerator,
         mock {},
         schemaProvider,
         caskConfigRepository,
         mock { },
         CaskServiceRegenerationRunner(),
         changeDetector,
         ingestionEventHandler,
         eventPublisher).regenerateCaskServicesAsync()

      // assert
      verify(caskServiceSchemaGenerator, timeout(1000).times(1)).generateAndPublishServices(listOf(CaskTaxiPublicationRequest(versionedType)))
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
      CaskServiceBootstrap(
         caskServiceSchemaGenerator,
         mock { },
         schemaProviderV2,
         caskConfigRepository,
         mock { },
         CaskServiceRegenerationRunner(),
         changeDetector,
         ingestionEventHandler,
         eventPublisher)
         .regenerateCasksOnSchemaChange(event)

      // assert
      verify(caskServiceSchemaGenerator, timeout(1000).times(1)).generateAndPublishServices(listOf(CaskTaxiPublicationRequest(versionedTypeV2)))
   }

   @Test
   fun `View based casks should be checked for regeneration upon schema update`() {
      // prepare
      val schemaV1 = "type Order {}"
      val taxiSchemaV1 = TaxiSchema.from(schemaV1, "order.taxi", "1.0.1")
      val versionedTypeV1 = taxiSchemaV1.versionedType("Order".fqn())
      val caskConfigV1 = CaskConfig("Order_hash1", "Order", "hash1", emptyList(), emptyList(), null, Instant.now())
      val schemaProviderV1 = VersionedSchemaProvider(versionedTypeV1.sources)
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfigV1))
      val mockCaskViewService = mock<CaskViewService>()

      // simulate schema change
      val versionedSource1 = VersionedSource("order.taxi", "1.0.0", schemaV1)
      val caskServiceAdded = VersionedSource("vyne.cask.Order", "1.1.1", "")
      val oldSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(versionedSource1)), 1)
      val newSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(versionedSource1), ParsedSource(caskServiceAdded)), 2)
      val event = SchemaSetChangedEvent(oldSchemaSet, newSchemaSet)

      // act
      val caskBootstrapper = CaskServiceBootstrap(
         caskServiceSchemaGenerator,
         mock {},
         schemaProviderV1,
         caskConfigRepository,
         mockCaskViewService,
         CaskServiceRegenerationRunner(),
         changeDetector,
         ingestionEventHandler,
         eventPublisher)
      caskBootstrapper.regenerateCasksOnSchemaChange(event)

      // assert
      verify(caskServiceSchemaGenerator, timeout(5000).times(1)).generateAndPublishServices(any())

      // send the same schema change event again, this time view creation code should be triggered.
      caskBootstrapper.regenerateCasksOnSchemaChange(event)
      verify(mockCaskViewService, times(2)).generateViews()

   }

   @Test
   fun `Do regenerate when schema change contains added cask services`() {
      // prepare
      val schemaV1 = "type Order {}"
      val taxiSchemaV1 = TaxiSchema.from(schemaV1, "order.taxi", "1.0.1")
      val versionedTypeV1 = taxiSchemaV1.versionedType("Order".fqn())
      val caskConfigV1 = CaskConfig("Order_hash1", "Order", "hash1", emptyList(), emptyList(), null, Instant.now())
      val schemaProviderV1 = VersionedSchemaProvider(versionedTypeV1.sources)
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfigV1))

      // simulate schema change
      val versionedSource1 = VersionedSource("order.taxi", "1.0.0", schemaV1)
      val caskServiceAdded = VersionedSource("vyne.cask.Order", "1.1.1", "")
      val oldSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(versionedSource1)), 1)
      val newSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(versionedSource1), ParsedSource(caskServiceAdded)), 2)
      val event = SchemaSetChangedEvent(oldSchemaSet, newSchemaSet)

      // act
      CaskServiceBootstrap(
         caskServiceSchemaGenerator,
         mock { },
         schemaProviderV1,
         caskConfigRepository,
         mock { },
         CaskServiceRegenerationRunner(),
         changeDetector,
         ingestionEventHandler,
         eventPublisher).regenerateCasksOnSchemaChange(event)

      // assert
      verify(caskServiceSchemaGenerator, timeout(5000).times(1)).generateAndPublishServices(any())
   }

   @Test
   fun `Do regenerate when schema change contains removed cask services`() {
      // prepare
      val schemaV1 = "type Order {}"
      val taxiSchemaV1 = TaxiSchema.from(schemaV1, "order.taxi", "1.0.1")
      val versionedTypeV1 = taxiSchemaV1.versionedType("Order".fqn())
      val caskConfigV1 = CaskConfig("Order_hash1", "Order", "hash1", emptyList(), emptyList(), null, Instant.now())
      val schemaProviderV1 = VersionedSchemaProvider(versionedTypeV1.sources)
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfigV1))

      // simulate schema change
      val versionedSource1 = VersionedSource("order.taxi", "1.0.0", schemaV1)
      val caskServiceRemoved = VersionedSource("vyne.cask.Order", "1.1.1", "")
      val oldSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(versionedSource1), ParsedSource(caskServiceRemoved)), 1)
      val newSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(versionedSource1)), 2)
      val event = SchemaSetChangedEvent(oldSchemaSet, newSchemaSet)

      // act
      CaskServiceBootstrap(
         caskServiceSchemaGenerator,
         mock {},
         schemaProviderV1,
         caskConfigRepository,
         mock { },
         CaskServiceRegenerationRunner(),
         changeDetector,
         ingestionEventHandler,
         eventPublisher).regenerateCasksOnSchemaChange(event)

      // assert
      verify(caskServiceSchemaGenerator, timeout(5000).times(1)).generateAndPublishServices(any())
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
      CaskServiceBootstrap(
         caskServiceSchemaGenerator,
         mock {},
         schemaProvider,
         caskConfigRepository,
         mock { },
         CaskServiceRegenerationRunner(),
         changeDetector,
         ingestionEventHandler,
         eventPublisher).regenerateCaskServicesAsync()

      // assert
      verify(caskServiceSchemaGenerator, times(0)).generateAndPublishServices(any())
   }

   @Test
   fun `taxi view based casks should be checked for regeneration upon schema update`() {
      val orderSource = VersionedSource("broker/order.taxi", "1.0.0", """
         namespace broker {
           type OrderId inherits String
           model Order {
             id: OrderId
           }
         }
      """.trimIndent())

      val orderViewSource = VersionedSource("broker/orderView.taxi", "1.0.0", """
         import broker.Order;
         namespace broker {
           view OrderView  with query {
               find {Order[]} as {
                  orderId: String
                  }
               }
           }
      """.trimIndent())

      val orderViewModelCaskGeneratedSource = VersionedSource("vyne.cask.broker.OrderView", "1.0.0", """
         namespace broker {
           model OrderView {
              orderId: String
           }
         }
      """.trimIndent())

      val taxiSchemaStoreService = TaxiSchemaStoreService(emptyList())
      val versionedSourceSubmission =
         VersionedSourceSubmission(listOf(orderSource, orderViewSource, orderViewModelCaskGeneratedSource), PublisherConfiguration("cask"))
      taxiSchemaStoreService.submitSources(versionedSourceSubmission)
      val caskBootstrapper = CaskServiceBootstrap(
         caskServiceSchemaGenerator,
         mock {},
         taxiSchemaStoreService,
         caskConfigRepository,
         mock(),
         CaskServiceRegenerationRunner(),
         changeDetector,
         ingestionEventHandler,
         eventPublisher)


      val updatedOrderViewSource = VersionedSource("broker/orderView.taxi", "1.0.1", """
          import broker.Order;
          namespace broker {
           view OrderView with query {
               find {Order[]} as {
                  orderId: String
                  entryType: String
                  }
               }
           }
      """.trimIndent())
      taxiSchemaStoreService.submitSources(VersionedSourceSubmission(listOf(orderSource, updatedOrderViewSource, orderViewModelCaskGeneratedSource), PublisherConfiguration("cask")))

      val caskConfigV2 = CaskConfig("v_orderview",
         "broker.OrderView",
         "hash1", listOf("unknown:0.0.0"), listOf("""
         namespace broker {
           model OrderView {
              orderId: String
              entryType: String
           }
         }
      """.trimIndent()), null, Instant.now(), true)
      val publicationRequests =  caskBootstrapper.findTypesToRegister(listOf(caskConfigV2))
      val objectType = publicationRequests.first().type.taxiType as ObjectType
      objectType.fields.size.should.equal(2)
   }
}
