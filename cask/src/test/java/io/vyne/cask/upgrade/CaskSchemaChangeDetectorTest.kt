package io.vyne.cask.upgrade

import arrow.core.extensions.list.show.show
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.query.CaskConfigService
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CaskSchemaChangeDetectorTest {

   lateinit var repository:CaskConfigRepository
   lateinit var changeDetector:CaskSchemaChangeDetector
   lateinit var configService:CaskConfigService
   @Before
   fun setup() {
      configService = mock {}
      repository = mock {  }
      changeDetector = CaskSchemaChangeDetector(repository, configService, mock {  }, mock {  })
   }
   @Test
   fun `when type has changed definition it is detected`() {
      val originalSchema = TaxiSchema.from("""
         model Person {
            firstName : FirstName as String
         }
      """.trimIndent())
      whenever(repository.findAllByStatusAndExposesType(eq(CaskStatus.ACTIVE), any())).thenReturn(listOf(caskForType(originalSchema, "Person")))
      val updatedSchema = TaxiSchema.from("""
         model Person {
            firstName : FirstName as String
            lastName : LastName as String
         }
      """.trimIndent())

      val updated = changeDetector.findCasksRequiringUpgrading(updatedSchema)
      updated.should.have.size(1)
   }

   @Test
   fun `when type has not changed definition it is not listed for upgrade`() {
      val originalSchema = TaxiSchema.from("""
         model Person {
            firstName : FirstName as String
         }
      """.trimIndent())
      whenever(repository.findAll()).thenReturn(listOf(caskForType(originalSchema, "Person")))
      val updatedSchema = TaxiSchema.from("""
         model Person {
            firstName : FirstName as String
         }
      """.trimIndent())

      val updated = changeDetector.findCasksRequiringUpgrading(updatedSchema)
      updated.should.be.empty
   }


}

fun caskForType(originalSchema: TaxiSchema, typeName: String): CaskConfig {
   return CaskConfig.forType(originalSchema.versionedType(typeName.fqn()), "$typeName-table", daysToRetain = 100000)
}

