package io.vyne.cask.query

import com.winterbe.expekt.should
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.StringToQualifiedNameConverter
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.schemas.fqn
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant

@DataJpaTest(properties = ["spring.main.web-application-type=none"])
@RunWith(SpringRunner::class)
@AutoConfigureTestDatabase(replace = NONE)
@Import(StringToQualifiedNameConverter::class)
@ContextConfiguration(initializers = [CaskConfigServiceTest.Initializer::class])
class CaskConfigServiceTest {

   companion object {
      @ClassRule @JvmField
      var postgreSQLContainer: PostgreSQLContainer<*> = PostgreSQLContainer<Nothing>("postgres:12.3")
   }

   @Autowired
   lateinit var caskRepository:CaskConfigRepository

   lateinit var caskConfigService:CaskConfigService

   @Before
   fun setup() {
      caskConfigService = CaskConfigService(caskRepository)
   }


   @Test
   fun canCreateCaskConfig() {
      // prepare
      val taxiSchema = CoinbaseJsonOrderSchema.schemaV1
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())

      // act
      caskConfigService.createCaskConfig(versionedType)

      // assert
      val caskConfigs: MutableList<CaskConfig> = caskRepository.findAll()
      caskConfigs.size.should.be.equal(1)
      caskConfigs[0].tableName.should.equal("rderwindowsummary_f1b588_6cc56e")
      caskConfigs[0].qualifiedTypeName.should.equal("OrderWindowSummary")
      caskConfigs[0].versionHash.should.equal("6cc56e")
      caskConfigs[0].sourceSchemaIds.should.contain.elements("Coinbase:0.1.0")
      caskConfigs[0].sources.should.contain.elements(CoinbaseJsonOrderSchema.sourceV1)
      caskConfigs[0].deltaAgainstTableName.should.be.`null`
      caskConfigs[0].insertedAt.should.be.below(Instant.now())
   }

   @Test
   fun `calling cask config multiple times does not create duplicates`() {
      caskRepository.findAll().should.have.size(0)

      val taxiSchema = CoinbaseJsonOrderSchema.schemaV1
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())

      // act
      caskConfigService.createCaskConfig(versionedType)
      caskConfigService.createCaskConfig(versionedType)
      caskConfigService.createCaskConfig(versionedType)

      caskRepository.findAll().should.have.size(1)
   }

   internal class Initializer :
      ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
         TestPropertyValues
            .of(
               "spring.datasource.url=" + postgreSQLContainer.jdbcUrl,
               "spring.datasource.username=" + postgreSQLContainer.username,
               "spring.datasource.password=" + postgreSQLContainer.password
            )
            .applyTo(configurableApplicationContext.environment)
      }
   }
}
