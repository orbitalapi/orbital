package io.vyne.cask.upgrade

import com.winterbe.expekt.should
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.caskRecordTable
import io.vyne.cask.query.BaseCaskIntegrationTest
import io.vyne.cask.query.CaskConfigService
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit4.SpringRunner


class CaskSchemaChangeDetectorIntegrationTest : BaseCaskIntegrationTest() {

   lateinit var configService:CaskConfigService
   lateinit var changeDetector: CaskSchemaChangeDetector
   @Before
   override fun setup() {
      super.setup()
      configService = CaskConfigService(configRepository)
      changeDetector = CaskSchemaChangeDetector(configRepository,configService,caskDao)
   }

   @Test
   fun casksShouldBeTaggedAsMigrating() {
      val originalSchema = TaxiSchema.from("""
         model Person {
            firstName : FirstName as String
         }
      """.trimIndent())
      // Set up the original type
      val originalConfig = configService.createCaskConfig(originalSchema.versionedType("Person".fqn()))

      // Now update it
      val updatedSchema = schemaProvider.updateSource("""
         model Person {
            firstName : FirstName as String
            lastName : LastName as String
         }
      """.trimIndent())

      // act
      changeDetector.markModifiedCasksAsRequiringUpgrading(updatedSchema)

      // Now verify that the cask has been tagged
      val configForOldTable = configRepository.findByTableName(originalConfig.tableName)!!
      configForOldTable.status.should.equal(CaskStatus.MIGRATING)

      val expectedReplacementTableName = updatedSchema.versionedType("Person".fqn()).caskRecordTable()
      configForOldTable.replacedByTableName.should.equal(expectedReplacementTableName)
      configForOldTable.replacedByTableName.should.not.equal(configForOldTable.tableName)
   }
}
