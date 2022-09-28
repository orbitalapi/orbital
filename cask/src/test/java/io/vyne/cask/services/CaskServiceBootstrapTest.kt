package io.vyne.cask.services

import com.nhaarman.mockito_kotlin.*
import com.winterbe.expekt.should
import io.vyne.*
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.caskRecordTable
import io.vyne.cask.ddl.views.CaskViewService
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema.asSourcePackage
import io.vyne.cask.ingest.IngestionEventHandler
import io.vyne.cask.upgrade.CaskSchemaChangeDetector
import io.vyne.cask.upgrade.UpdatableSchemaProvider
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.spring.SimpleTaxiSchemaProvider
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.schemas.VersionedType
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
      val schemaProvider = UpdatableSchemaProvider.withSource("type Order {}")
      val versionedType = schemaProvider.schema.versionedType("Order".fqn())
      val caskConfig = CaskConfig(
         versionedType.caskRecordTable(),
         "Order",
         versionedType.versionHash,
         emptyList(),
         emptyList(),
         null,
         Instant.now()
      )
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfig))

      // act
      CaskServiceBootstrap(
         caskServiceSchemaGenerator,
         schemaProvider,
         schemaProvider,
         caskConfigRepository,
         mock { },
         CaskServiceRegenerationRunner(),
         changeDetector,
         ingestionEventHandler,
         eventPublisher
      ).regenerateCaskServicesAsync()

      // assert
      verify(caskServiceSchemaGenerator, timeout(1000).times(1)).generateAndPublishServices(
         listOf(
            CaskTaxiPublicationRequest(versionedType)
         )
      )
   }


   @Test
   fun `Regenerate cask services when schema changes`() {
      // prepare
      val schemaV1 = "type Order {}"
      val schemaV2 = "type Order { id: String }"
      val taxiSchemaV2 = TaxiSchema.from(schemaV2, "order.taxi", "1.0.1")
      val versionedTypeV2 = taxiSchemaV2.versionedType("Order".fqn())
      val caskConfigV1 = CaskConfig("Order_hash1", "Order", "hash1", emptyList(), emptyList(), null, Instant.now())
      val schemaProviderV2 = versionedTypeV2.asSchemaProvider()
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfigV1))

      // simulate schema change
      val oldSchemaSet = SchemaSet.fromParsed(schemaV1.asSourcePackage(version = "1.0.0").toParsedPackages(), 1)
      val newSchemaSet = SchemaSet.fromParsed(schemaV2.asSourcePackage(version = "1.0.1").toParsedPackages(), 2)
      val event = SchemaSetChangedEvent(oldSchemaSet, newSchemaSet)

      // act
      CaskServiceBootstrap(
         caskServiceSchemaGenerator,
         schemaProviderV2,
         schemaProviderV2,
         caskConfigRepository,
         mock { },
         CaskServiceRegenerationRunner(),
         changeDetector,
         ingestionEventHandler,
         eventPublisher
      )
         .regenerateCasksOnSchemaChange(event)

      // assert
      argumentCaptor<List<CaskTaxiPublicationRequest>>().apply {
         verify(caskServiceSchemaGenerator, timeout(1000).times(1)).generateAndPublishServices(capture())
         val request = lastValue
         request.size.should.equal(1)
         val typeInRequest = request.single().type
         typeInRequest.sources.single().content.should.equal(versionedTypeV2.sources.single().content)
         typeInRequest.sources.single().version.should.equal(versionedTypeV2.sources.single().version)
      }

   }

   @Test
   fun `View based casks should be checked for regeneration upon schema update`() {
      // prepare
      val schemaV1 = "type Order {}"
      val taxiSchemaV1 = TaxiSchema.from(schemaV1, "order.taxi", "1.0.1")
      val versionedTypeV1 = taxiSchemaV1.versionedType("Order".fqn())
      val caskConfigV1 = CaskConfig("Order_hash1", "Order", "hash1", emptyList(), emptyList(), null, Instant.now())
      val schemaProviderV1 = versionedTypeV1.asSchemaProvider()
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfigV1))
      val mockCaskViewService = mock<CaskViewService>()

      // simulate schema change
      val versionedSource1 = VersionedSource("order.taxi", "1.0.0", schemaV1)
      val caskServiceAdded = VersionedSource("vyne.cask.Order", "1.1.1", "")
      val oldSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(versionedSource1)).asParsedPackages(), 1)
      val newSchemaSet = SchemaSet.fromParsed(
         listOf(ParsedSource(versionedSource1), ParsedSource(caskServiceAdded)).asParsedPackages(version = "1.1.1"), 2
      )
      val event = SchemaSetChangedEvent(oldSchemaSet, newSchemaSet)

      // act
      val caskBootstrapper = CaskServiceBootstrap(
         caskServiceSchemaGenerator,
         schemaProviderV1,
         schemaProviderV1,
         caskConfigRepository,
         mockCaskViewService,
         CaskServiceRegenerationRunner(),
         changeDetector,
         ingestionEventHandler,
         eventPublisher
      )
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
      val schemaProviderV1 = versionedTypeV1.asSchemaProvider()
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfigV1))

      // simulate schema change
      val versionedSource1 = VersionedSource("order.taxi", "1.0.0", schemaV1)
      val caskServiceAdded = VersionedSource("vyne.cask.Order", "1.1.1", "")
      val oldSchemaSet =
         SchemaSet.fromParsed(listOf(ParsedSource(versionedSource1)).asParsedPackages(version = "1.0.0"), 1)
      val newSchemaSet = SchemaSet.fromParsed(
         listOf(ParsedSource(versionedSource1), ParsedSource(caskServiceAdded)).asParsedPackages(version = "1.1.1"), 2
      )
      val event = SchemaSetChangedEvent(oldSchemaSet, newSchemaSet)

      // act
      CaskServiceBootstrap(
         caskServiceSchemaGenerator,
         schemaProviderV1,
         schemaProviderV1,
         caskConfigRepository,
         mock { },
         CaskServiceRegenerationRunner(),
         changeDetector,
         ingestionEventHandler,
         eventPublisher
      ).regenerateCasksOnSchemaChange(event)

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
      val schemaProviderV1 = versionedTypeV1.asSchemaProvider()
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfigV1))

      // simulate schema change
      val versionedSource1 = VersionedSource("order.taxi", "1.0.0", schemaV1)
      val caskServiceRemoved = VersionedSource("vyne.cask.Order", "1.1.1", "")
      val oldSchemaSet =
         SchemaSet.fromParsed(listOf(ParsedSource(versionedSource1), ParsedSource(caskServiceRemoved)).asParsedPackages(), 1)
      val newSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(versionedSource1)).asParsedPackages(version = "1.1.1"), 2)
      val event = SchemaSetChangedEvent(oldSchemaSet, newSchemaSet)

      // act
      CaskServiceBootstrap(
         caskServiceSchemaGenerator,
         schemaProviderV1,
         schemaProviderV1,
         caskConfigRepository,
         mock { },
         CaskServiceRegenerationRunner(),
         changeDetector,
         ingestionEventHandler,
         eventPublisher
      ).regenerateCasksOnSchemaChange(event)

      // assert
      verify(caskServiceSchemaGenerator, timeout(5000).times(1)).generateAndPublishServices(any())
   }

   @Test
   fun `Schema error does not prevent cask starting up`() {
      // prepare
      val schemaProvider = UpdatableSchemaProvider.withSource(
         """
         namespace common.order
         type Order {
            id: String2
         }
      """.trimIndent()
      )
      val caskConfig =
         CaskConfig("Order_hash1", "common.order.Order", "hash1", emptyList(), emptyList(), null, Instant.now())
      whenever(caskConfigRepository.findAll()).thenReturn(mutableListOf(caskConfig))

      // act
      CaskServiceBootstrap(
         caskServiceSchemaGenerator,
         schemaProvider,
         schemaProvider,
         caskConfigRepository,
         mock { },
         CaskServiceRegenerationRunner(),
         changeDetector,
         ingestionEventHandler,
         eventPublisher
      ).regenerateCaskServicesAsync()

      // assert
      verify(caskServiceSchemaGenerator, times(0)).generateAndPublishServices(any())
   }

   @Test
   fun `taxi view based casks should be checked for regeneration upon schema update`() {
      val orderSource = VersionedSource(
         "broker/order.taxi", "1.0.0", """
         namespace broker {
           type OrderId inherits String
           model Order {
             id: OrderId
           }
         }
      """.trimIndent()
      )

      val orderViewSource = VersionedSource(
         "broker/orderView.taxi", "1.0.0", """
         import broker.Order;
         namespace broker {
           view OrderView  with query {
               find {Order[]} as {
                  orderId: String
                  }
               }
           }
      """.trimIndent()
      )

      val orderViewModelCaskGeneratedSource = VersionedSource(
         "vyne.cask.broker.OrderView", "1.0.0", """
         namespace broker {
           model OrderView {
              orderId: String
           }
         }
      """.trimIndent()
      )

      val taxiSchemaStoreService = UpdatableSchemaProvider()
      taxiSchemaStoreService.updateSource(listOf(orderSource, orderViewSource, orderViewModelCaskGeneratedSource))
      val caskBootstrapper = CaskServiceBootstrap(
         caskServiceSchemaGenerator,
         taxiSchemaStoreService,
         taxiSchemaStoreService,
         caskConfigRepository,
         mock(),
         CaskServiceRegenerationRunner(),
         changeDetector,
         ingestionEventHandler,
         eventPublisher
      )


      val updatedOrderViewSource = VersionedSource(
         "broker/orderView.taxi", "1.0.1", """
          import broker.Order;
          namespace broker {
           view OrderView with query {
               find {Order[]} as {
                  orderId: String
                  entryType: String
                  }
               }
           }
      """.trimIndent()
      )
      taxiSchemaStoreService.updateSource(
         listOf(
            orderSource,
            updatedOrderViewSource,
            orderViewModelCaskGeneratedSource
         )
      )

      val caskConfigV2 = CaskConfig(
         "v_orderview",
         "broker.OrderView",
         "hash1", listOf("unknown:0.0.0"), listOf(
            """
         namespace broker {
           model OrderView {
              orderId: String
              entryType: String
           }
         }
      """.trimIndent()
         ), null, Instant.now(), true
      )
      val publicationRequests = caskBootstrapper.findTypesToRegister(listOf(caskConfigV2))
      val objectType = publicationRequests.first().type.taxiType as ObjectType
      objectType.fields.size.should.equal(2)
   }
}



private fun VersionedType.asSchemaProvider(): UpdatableSchemaProvider {
   return UpdatableSchemaProvider.from(this.sources)
}

