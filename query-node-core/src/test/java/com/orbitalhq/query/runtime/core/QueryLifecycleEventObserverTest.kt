package com.orbitalhq.query.runtime.core

import app.cash.turbine.test
import com.orbitalhq.expectTypedObject
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.right
import com.orbitalhq.query.QueryEvent
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.query.QueryResult
import com.orbitalhq.query.StreamingQueryCancelledEvent
import com.orbitalhq.query.TaxiQlQueryResultEvent
import com.orbitalhq.testVyne
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.test.runTest
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.test.StepVerifier

private val logger = KotlinLogging.logger {  }
class QueryLifecycleEventObserverTest {


   private val queryEventConsumer = object : QueryEventConsumer {
      private val messageSink = Sinks.many().unicast().onBackpressureBuffer<QueryEvent>()
      val messages: Flux<QueryEvent> = messageSink.asFlux()

      override fun handleEvent(event: QueryEvent) {
         messageSink.tryEmitNext(event)
      }

      override fun recordResult(operation: OperationResult, queryId: String) {
      }
   }

   private val taxiDef = """
          type ClientId inherits String
          type ClientName inherits String
          model Client {
            clientId : ClientId
            clientName: ClientName
         }

         service ClientService {
            operation streamClients():Stream<Client>
         }
   """.trimIndent()
   @OptIn(ExperimentalCoroutinesApi::class)
   @Test
   fun `When streaming query is cancelled StreamingQueryCancelledEvent event is published`() = runTest {
      val (vyne, stub) = testVyne(taxiDef)
      val typedInstance = TypedInstance.from(
         vyne.type("Client"),
         mapOf("clientId" to "123", "clientName" to "Marty"),
         vyne.schema
      )

      val flow = flow {
         while(true) {
            emit(typedInstance.right())
            delay(1000L)
         }
      }.flowOn(Dispatchers.IO)

      stub.addResponseFlow("streamClients") { _, _ -> flow }

      val queryResult = vyne.query(
         """stream { Client }
      """.trimMargin()
      )

     val queryResponse =  QueryLifecycleEventObserver(queryEventConsumer, null)
         .responseWithQueryHistoryListener("stream { Client }", queryResult)


      (queryResponse as QueryResult).results.test {
         expectTypedObject()
         StepVerifier.create(queryEventConsumer.messages)
            .recordWith { mutableListOf<QueryEvent>() }
            .thenConsumeWhile {
               logger.info { it.javaClass.name }
               if (it is TaxiQlQueryResultEvent) {
                  queryResult.requestCancel()
               }
               it !is StreamingQueryCancelledEvent
            }
            .expectRecordedMatches {
               it.filterIsInstance<StreamingQueryCancelledEvent>().size == 1
            }
            .expectNextMatches { it is StreamingQueryCancelledEvent }
            .thenCancel()
            .verify()
      }
   }
}
