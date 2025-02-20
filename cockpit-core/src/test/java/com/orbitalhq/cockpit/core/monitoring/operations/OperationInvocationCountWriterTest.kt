package com.orbitalhq.cockpit.core.monitoring.operations

import com.orbitalhq.query.MetricTags
import com.orbitalhq.query.connectors.OperationInvocationCountingEvent
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

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
            eventConsumer.operationInvoked(OperationInvocationCountingEvent(loadActor, "queryId", MetricTags.NONE))
         })
      }
      repeat(150) {
         jobs.add(async(Dispatchers.Default) {
            eventConsumer.operationInvoked(OperationInvocationCountingEvent(loadCast, "queryId", MetricTags.NONE))
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
