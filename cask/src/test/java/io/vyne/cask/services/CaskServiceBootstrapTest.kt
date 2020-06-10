package io.vyne.cask.services

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.vyne.cask.query.CaskDAO
import io.vyne.schemas.fqn
import io.vyne.spring.SimpleTaxiSchemaProvider
import org.junit.Test
import java.time.Instant


class CaskServiceBootstrapTest {
   val caskServiceSchemaGenerator: CaskServiceSchemaGenerator = mock()
   val caskDAO: CaskDAO = mock()
   val schemaProvider = SimpleTaxiSchemaProvider("""
         namespace hpc.orders
         type Order {}
      """.trimIndent())
   lateinit var serviceBootstrap: CaskServiceBootstrap

   @Test
   fun `Initialize cask services from CaskConfig entries`() {
      // prepare
      val versionedType = schemaProvider.schema().versionedType("hpc.orders.Order".fqn())
      val caskConfig = CaskDAO.CaskConfig("Order_8d09b8_97928b", "hpc.orders.Order", "97928b", emptyList(), emptyList(), null, Instant.now())
      whenever(caskDAO.findAllCaskConfigs()).thenReturn(mutableListOf(caskConfig))

      // act
      serviceBootstrap = CaskServiceBootstrap(caskServiceSchemaGenerator, schemaProvider, caskDAO)

      // assert
      verify(caskServiceSchemaGenerator, times(1)).generateAndPublishService(versionedType)
   }
}
