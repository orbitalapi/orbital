package io.vyne.cask.observers

import com.winterbe.expekt.should
import io.vyne.cask.ingest.CaskMutationDispatcher
import io.vyne.schemaApi.SchemaProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner


@RunWith(SpringRunner::class)
@SpringBootTest(classes = [CaskIngestionObserver::class])
@ActiveProfiles("observer")
@Import(CaskIngestionObserverTest.TestConfig::class)
@EnableConfigurationProperties(IngestionObserverConfigurationProperties::class)
class CaskIngestionObserverTest {
   @Autowired
   private lateinit var caskIngestionObserver: CaskIngestionObserver

   @MockBean
   private lateinit var schemaProvider: SchemaProvider

   @Configuration
   class TestConfig {
      @Bean
      fun caskMutationDispatcher(): CaskMutationDispatcher {
         return CaskMutationDispatcher()
      }
   }

   val taxi = lang.taxi.Compiler("""
      @ObserveChanges(writeToConnectionName = "Orders")
      model Order {
        id: String
      }

      @ObserveChanges(writeToConnectionName = "Customers")
      model Customer {
        id: String
      }
   """.trimIndent()).compile()

   @Test
   fun `should detect observer configuration from profile config`() {
      caskIngestionObserver.isObservable(taxi.type("Customer")).should.be.`true`
      caskIngestionObserver.isObservable(taxi.type("Order")).should.be.`true`
   }

   @Test
   fun `should fetch observer configuration from profile config`() {
      val kafkaObserverConfigForCustomer = caskIngestionObserver.kafkaObserverConfig(taxi.type("Customer"))
      val kafkaObserverConfigForOrder = caskIngestionObserver.kafkaObserverConfig(taxi.type("Order"))
      kafkaObserverConfigForCustomer.connectionName.should.equal("Customers")
      kafkaObserverConfigForCustomer.bootstrapServers.should.equal("localhost:9022")
      kafkaObserverConfigForCustomer.topic.should.equal("Customer")
      kafkaObserverConfigForOrder.connectionName.should.equal("Orders")
      kafkaObserverConfigForOrder.bootstrapServers.should.equal("prod1:9022,prod2:9022,prod3:9022")
      kafkaObserverConfigForOrder.topic.should.equal("Order")
   }
}
