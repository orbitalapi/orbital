package io.vyne.schemaStore


class TaxiSchemaStoreServiceTest {
//   @Rule
//   @JvmField
//   val server = MockWebServerRule()
//
//   private val brokerOrderTaxi = VersionedSource("com.broker", "1.0.0", """
//         namespace broker {
//           model OrderView {
//              orderId: String
//           }
//         }
//      """.trimIndent())
//   private val sourcePackageSubmission = SourcePackage(listOf(brokerOrderTaxi), publisherConfiguration().publisherId)
//
//
//   @Test
//   @Ignore
//   fun `A scheme publisher can publish to TaxiSchemaStoreService and fetch schemaSet from it`() {
//      server.prepareResponse { response -> response.setResponseCode(200) }
//      val keepAliveMonitors = listOf(
//         HttpPollKeepAliveStrategyMonitor(webClientBuilder = WebClient.builder()),
//         NoneKeepAliveStrategyMonitor
//      )
//      val taxiSchemaStoreService = TaxiSchemaStoreService(keepAliveMonitors)
//
//      StepVerifier
//         .create(taxiSchemaStoreService.submitSources(sourcePackageSubmission))
//         .expectNextMatches { sourceSubmissionResponse ->
//            sourceSubmissionResponse.errors.isEmpty()
//         }
//         .verifyComplete()
//
//      StepVerifier
//         .create(taxiSchemaStoreService.listSchemas())
//         .expectNextMatches { schemaSet ->
//            schemaSet.taxiSchemas.first().sources.size == 1
//         }
//         .verifyComplete()
//   }
//
//   @Test
//   @Ignore
//   fun `TaxiSchemaStoreService drops schemas from a publisher failing to heartbeat`() {
//      server.prepareResponse { response -> response.setResponseCode(401) }
//      val keepAliveMonitors = listOf(
//         HttpPollKeepAliveStrategyMonitor(webClientBuilder = WebClient.builder()),
//         NoneKeepAliveStrategyMonitor
//      )
//      val taxiSchemaStoreService = TaxiSchemaStoreService(keepAliveMonitors)
//      // our publisher declares heartbeat of 2 second!
//      val submission = sourcePackageSubmission.copy(
//         publisherId = publisherConfiguration(4L).publisherId)
//      StepVerifier
//         .create(taxiSchemaStoreService.submitSources(submission))
//         .expectNextMatches { sourceSubmissionResponse ->
//            sourceSubmissionResponse.errors.isEmpty()
//         }
//         .verifyComplete()
//
//      // publisher pulled the schema
//      StepVerifier
//         .create(taxiSchemaStoreService.listSchemas())
//         .expectNextMatches { schemaSet ->
//            schemaSet.taxiSchemas.first().sources.size == 1
//         }
//         .verifyComplete()
//
//      // wait for 8 seconds which larger than publisher keep alive ping duration.
//      Thread.sleep(8000)
//      StepVerifier
//         .create(taxiSchemaStoreService.listSchemas())
//         .expectNextMatches { schemaSet ->
//            schemaSet.taxiSchemas.isEmpty()
//         }
//         .verifyComplete()
//   }
//
//   private fun publisherConfiguration(pollFrequencyInSeconds: Long = 2L): PublisherConfiguration {
//      return PublisherConfiguration("publisher1",
//         HttpPollKeepAlive(pollFrequency = Duration.ofSeconds(pollFrequencyInSeconds), pollUrl = "http://localhost:${server.port}/ping")
//      )
//   }
}
