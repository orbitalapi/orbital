package com.orbitalhq.cockpit.core.monitoring.operations

import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

class OperationInvocationCountWriterTest : BaseOperationInvocationCountTest() {

   val schema = TaxiSchema.from(
      """
         model Actor {}
         model Cast {}
         service ActorService {
            @HttpOperation(method = "GET", url =  "http://fakeurl.com")
            operation loadActor():Actor
            operation loadCast():Cast
         }
      """.trimIndent()
   )


   @Test
   fun `writes counts to database`() = runBlocking {
      val loadActor = schema.service("ActorService").operation("loadActor")
      val loadCast = schema.service("ActorService").operation("loadCast")
      val jobs = mutableListOf<Deferred<Unit>>()

      // Use async and Dispatchers.Default to write from multiple threads concurrently
      repeat(200) {
         jobs.add(async(Dispatchers.Default) {
            eventConsumer.operationInvoked(loadActor)
         })
      }
      repeat(150) {
         jobs.add(async(Dispatchers.Default) {
            eventConsumer.operationInvoked(loadCast)
         })
      }
      jobs.awaitAll()
      writer.writeNow()

      val saved = repository.findAll()
      saved.shouldHaveSize(2)

      saved.single { it.operationName == "loadActor" }.count.shouldBe(200)
      saved.single { it.operationName == "loadCast" }.count.shouldBe(150)
   }


}
