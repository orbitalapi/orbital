package io.vyne.pipelines.runner.transport.http

import com.mercateo.test.clock.TestClock
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.models.TypedCollection
import io.vyne.models.json.parseJson
import io.vyne.pipelines.ConsoleLogger
import io.vyne.pipelines.TypedInstanceContentProvider
import io.vyne.pipelines.runner.transport.ParameterMap
import io.vyne.pipelines.runner.transport.PipelineVariableKeys.ENV_CURRENT_TIME
import io.vyne.pipelines.runner.transport.PipelineVariableKeys.PIPELINE_LAST_RUN_TIME
import io.vyne.pipelines.runner.transport.VariableProvider
import io.vyne.schemas.OperationNames
import io.vyne.spring.SimpleVyneProvider
import io.vyne.testVyne
import org.junit.Test
import reactor.test.StepVerifier
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

class PollingTaxiOperationPipelineInputTest {

   @Test
   fun `can pass arguments into polling operation`() {
      val schedule = "*/10 * * * * *" // every 10 seconds.
      val (vyne, stub) = io.vyne.testVyne(
         """
         model Purchase {
            orderId : String
         }
         type OrderSearchStartDate inherits Instant
         type OrderSearchEndDate inherits Instant

         service OrdersService {
            operation listOrders(fromTime:OrderSearchStartDate, toTime:OrderSearchEndDate):Purchase[]
         }
      """
      )
      val purchases = vyne.parseJson(
         "Purchase[]", """
       [
         { "orderId" : "order1" },
         { "orderId" : "order2" },
         { "orderId" : "order3" }
       ]
      """.trimMargin()
      )
      stub.addResponse("listOrders", purchases)
      val testClock = TestClock.fixed(OffsetDateTime.now())

      // The params we want to pass to our operation
      val params: ParameterMap = mapOf(
         "OrderSearchStartDate" to PIPELINE_LAST_RUN_TIME,
         "OrderSearchEndDate" to ENV_CURRENT_TIME
      )

      val pipelineLastRunTime = Instant.now().minus(Duration.ofMinutes(60))
      val builder = PollingTaxiOperationInputBuilder(
         SimpleVyneProvider(vyne),
         tickFrequency = Duration.ofSeconds(1),
         clock = testClock,
         variableProvider = VariableProvider.defaultWith(
            mapOf(PIPELINE_LAST_RUN_TIME to pipelineLastRunTime),
            clock = testClock
         )
      )
      var pipelineInput: PollingTaxiOperationPipelineInput? = null
      StepVerifier.withVirtualTime {
         pipelineInput = builder.build(
            PollingTaxiOperationInputSpec(
               OperationNames.name("OrdersService", "listOrders"),
               schedule,
               params
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

            val operationCallParams = stub.invocations["listOrders"]!!
            // from time should be the last time the pipeline was run
            operationCallParams[0].value.should.equal(pipelineLastRunTime)
            // To time should be "now" (as per our test clock)
            operationCallParams[1].value.should.equal(testClock.instant())
            true
         }
         .thenCancel()
         .verify()

   }

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
      val purchases = vyne.parseJson(
         "Purchase[]", """
       [
         { "orderId" : "order1" },
         { "orderId" : "order2" },
         { "orderId" : "order3" }
       ]
      """.trimMargin()
      )
      stub.addResponse("listOrders", purchases)
      val testClock = TestClock.fixed(OffsetDateTime.now())

      val builder = PollingTaxiOperationInputBuilder(
         vyneProvider = SimpleVyneProvider(vyne),
         variableProvider = VariableProvider.default(),
         tickFrequency = Duration.ofSeconds(1),
         clock = testClock,
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
