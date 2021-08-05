package io.vyne.pipelines.runner.transport.http

import com.mercateo.test.clock.TestClock
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.models.TypedCollection
import io.vyne.models.json.parseJson
import io.vyne.pipelines.ConsoleLogger
import io.vyne.pipelines.TypedInstanceContentProvider
import io.vyne.query.graph.operationInvocation.DefaultOperationInvocationService
import io.vyne.schemaStore.SimpleSchemaProvider
import io.vyne.schemas.OperationNames
import io.vyne.testVyne
import org.junit.Test
import reactor.test.StepVerifier
import java.time.Duration
import java.time.OffsetDateTime

class PollingTaxiOperationPipelineInputTest {
   @Test
   fun `polls every second and invokes on schedule`() {
      val schedule = "*/10 * * * * *" // every 10 seconds.
      val (vyne, stub) = testVyne(
         """
         model Purchase {
            orderId : String
         }
         service OrdersService {
            operation listOrders():Purchase[]
         }
      """
      )
      val purchases = vyne.parseJson("Purchase[]", """
       [
         { "orderId" : "order1" },
         { "orderId" : "order2" },
         { "orderId" : "order3" }
       ]
      """.trimMargin())
      stub.addResponse("listOrders", purchases)
      val testClock = TestClock.fixed(OffsetDateTime.now())

      val builder = PollingTaxiOperationInputBuilder(
         DefaultOperationInvocationService(listOf(stub)),
         tickFrequency = Duration.ofSeconds(1),
         clock = testClock,
         schemaProvider = SimpleSchemaProvider(vyne.schema)
      )
      var pipelineInput: PollingTaxiOperationPipelineInput? = null
      StepVerifier.withVirtualTime {
         pipelineInput = builder.build(
            PollingTaxiOperationInputSpec(
               OperationNames.name("OrdersService", "listOrders"),
               schedule
            ),
            ConsoleLogger,
            mock { }
         )
         pipelineInput!!.feed
      }
         .expectSubscription()
         .then { testClock.fastForward(Duration.ofSeconds(10)) }
         .thenAwait(Duration.ofSeconds(1))
         .expectNextMatches { message ->
            val content = (message.contentProvider as TypedInstanceContentProvider).content as TypedCollection
            content.size.should.equal(3)
            true
         }
         .thenCancel()
         .verify()
   }
}

private fun <T> StepVerifier.Step<T>.moveTimeForward(duration: Duration, testClock: TestClock): StepVerifier.Step<T> {
   testClock.fastForward(duration)
   return this.thenAwait(duration)
}
