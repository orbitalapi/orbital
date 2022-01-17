package io.vyne.schemaStore

import io.vyne.VersionedSource
import io.vyne.http.MockWebServerRule
import io.vyne.httpSchemaPublisher.HttpPollKeepAliveStrategyMonitor
import io.vyne.schemaPublisherApi.HttpPollKeepAlive
import io.vyne.schemaPublisherApi.NoneKeepAliveStrategyMonitor
import io.vyne.schemaPublisherApi.PublisherConfiguration
import io.vyne.schemaPublisherApi.VersionedSourceSubmission
import org.junit.Rule
import org.junit.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier
import java.time.Duration

class TaxiSchemaStoreServiceTest {
   @Rule
   @JvmField
   val server = MockWebServerRule()

   private val brokerOrderTaxi = VersionedSource("com.broker", "1.0.0", """
         namespace broker {
           model OrderView {
              orderId: String
           }
         }
      """.trimIndent())
   private val versionedSourceSubmission = VersionedSourceSubmission(listOf(brokerOrderTaxi), publisherConfiguration())


   @Test
   fun `A scheme publisher can publish to TaxiSchemaStoreService and fetch schemaSet from it`() {
      server.prepareResponse { response -> response.setResponseCode(200) }
      val keepAliveMonitors = listOf(
         HttpPollKeepAliveStrategyMonitor(webClientBuilder = WebClient.builder()),
         NoneKeepAliveStrategyMonitor
      )
      val taxiSchemaStoreService = TaxiSchemaStoreService(keepAliveMonitors).apply { afterPropertiesSet() }

      StepVerifier
         .create(taxiSchemaStoreService.submitSources(versionedSourceSubmission))
         .expectNextMatches { sourceSubmissionResponse ->
            sourceSubmissionResponse.errors.isEmpty()
         }
         .verifyComplete()

      StepVerifier
         .create(taxiSchemaStoreService.listSchemas())
         .expectNextMatches { schemaSet ->
            schemaSet.taxiSchemas.first().sources.size == 1
         }
         .verifyComplete()
   }

   @Test
   fun `TaxiSchemaStoreService drops schemas from a publisher failing to heartbeat`() {
      server.prepareResponse { response -> response.setResponseCode(401) }
      val keepAliveMonitors = listOf(
         HttpPollKeepAliveStrategyMonitor(webClientBuilder = WebClient.builder()),
         NoneKeepAliveStrategyMonitor
      )
      val taxiSchemaStoreService = TaxiSchemaStoreService(keepAliveMonitors).apply { afterPropertiesSet() }
      // our publisher declares heartbeat of 2 second!
      val submission = versionedSourceSubmission.copy(
         identifier = publisherConfiguration(4L))
      StepVerifier
         .create(taxiSchemaStoreService.submitSources(submission))
         .expectNextMatches { sourceSubmissionResponse ->
            sourceSubmissionResponse.errors.isEmpty()
         }
         .verifyComplete()

      // publisher pulled the schema
      StepVerifier
         .create(taxiSchemaStoreService.listSchemas())
         .expectNextMatches { schemaSet ->
            schemaSet.taxiSchemas.first().sources.size == 1
         }
         .verifyComplete()

      // wait for 8 seconds which larger than publisher keep alive ping duration.
      Thread.sleep(8000)
      StepVerifier
         .create(taxiSchemaStoreService.listSchemas())
         .expectNextMatches { schemaSet ->
            schemaSet.taxiSchemas.isEmpty()
         }
         .verifyComplete()
   }

   private fun publisherConfiguration(pollFrequencyInSeconds: Long = 2L): PublisherConfiguration {
      return PublisherConfiguration("publisher1",
         HttpPollKeepAlive(pollFrequency = Duration.ofSeconds(pollFrequencyInSeconds), pollUrl = "http://localhost:${server.port}/ping"))
   }
}
