package io.vyne.cask.services

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
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
   fun `create new cask table for type that just changed`() {
      // prepare
      val versionedType = schemaProvider.schema().versionedType("common.order.Order".fqn())
      val now = Instant.now()
      val caskConfigV1 = CaskConfig("Order_hashv1", "common.order.Order", "hashv1", emptyList(), emptyList(), null, now.minusSeconds(100))
      val caskConfigV2 = CaskConfig("Order_hashv2", "common.order.Order", "hashv2", emptyList(), emptyList(), null, now.minusSeconds(50))
      whenever(caskDAO.findAllCaskConfigs()).thenReturn(mutableListOf(caskConfigV1, caskConfigV2))

      // act
      CaskServiceBootstrap(caskServiceSchemaGenerator, schemaProvider, caskDAO).initializeCaskServices()

      // assert
      verify(caskDAO, times(1)).createCaskConfig(versionedType)
      verify(caskDAO, times(1)).createCaskRecordTable(versionedType)
      verify(caskServiceSchemaGenerator, times(1)).generateAndPublishService(versionedType)
   }
}
