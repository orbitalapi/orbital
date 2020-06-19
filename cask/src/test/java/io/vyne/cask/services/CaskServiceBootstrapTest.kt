package io.vyne.cask.services

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
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
      val caskConfig = CaskDAO.CaskConfig(caskTableName, "common.order.Order", typeHash, emptyList(), emptyList(), null, Instant.now())
      whenever(caskDAO.findAllCaskConfigs()).thenReturn(mutableListOf(caskConfig))

      // act
      CaskServiceBootstrap(caskServiceSchemaGenerator, schemaProvider, caskDAO).initializeCaskServices()

      // assert
      verify(caskServiceSchemaGenerator, times(1)).generateAndPublishService(versionedType)
   }
}
