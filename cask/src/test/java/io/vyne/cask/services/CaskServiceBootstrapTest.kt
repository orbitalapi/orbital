package io.vyne.cask.services

import com.nhaarman.mockito_kotlin.*
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.ddl.caskRecordTable
import io.vyne.cask.query.CaskDAO
import io.vyne.schemas.fqn
import io.vyne.spring.SimpleTaxiSchemaProvider
import org.junit.Test
import java.time.Instant


class CaskServiceBootstrapTest {
   val caskServiceSchemaGenerator: CaskServiceSchemaGenerator = mock()
   val caskDAO: CaskDAO = mock()
   val schemaProvider = SimpleTaxiSchemaProvider("""
         namespace common.order
         type Order {}
      """.trimIndent())
   lateinit var serviceBootstrap: CaskServiceBootstrap

   @Test
   fun `Initialize cask services from CaskConfig entries`() {
      // prepare
      val versionedType = schemaProvider.schema().versionedType("common.order.Order".fqn())
      val caskTableName = versionedType.caskRecordTable()
      val typeHash = versionedType.versionHash
      val caskConfig = CaskConfig(caskTableName, "common.order.Order", typeHash, emptyList(), emptyList(), null, Instant.now())
      whenever(caskDAO.findAllCaskConfigs()).thenReturn(mutableListOf(caskConfig))

      // act
      CaskServiceBootstrap(caskServiceSchemaGenerator, schemaProvider, caskDAO).initializeCaskServices()

      // assert
      verify(caskServiceSchemaGenerator, times(1)).generateAndPublishService(versionedType)
   }

   @Test
   fun `error in schema does not prevent cask starting up`() {
      // prepare
      val schemaProvider = SimpleTaxiSchemaProvider("""
         namespace common.order
         type Order {
            id: String2
         }
      """.trimIndent())
      val caskConfig = CaskConfig("Order_hash1", "common.order.Order", "hash1", emptyList(), emptyList(), null, Instant.now())
      whenever(caskDAO.findAllCaskConfigs()).thenReturn(mutableListOf(caskConfig))

      // act
      CaskServiceBootstrap(caskServiceSchemaGenerator, schemaProvider, caskDAO).initializeCaskServices()

      // assert
      verify(caskServiceSchemaGenerator, times(0)).generateAndPublishService(any(), any())
   }
}
