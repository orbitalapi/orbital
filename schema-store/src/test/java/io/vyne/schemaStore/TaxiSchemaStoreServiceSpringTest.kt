package io.vyne.schemaStore

import io.vyne.VersionedSource
import io.vyne.schema.publisher.http.HttpPollKeepAliveStrategyMonitor
import io.vyne.schema.publisher.KeepAliveStrategyMonitor
import io.vyne.schema.publisher.NoneKeepAliveStrategyMonitor
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClient
import kotlin.test.fail


//@RunWith(SpringRunner::class)
//@Import(TestConfig::class)
//@EnableAutoConfiguration
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [TaxiSchemaStoreService::class])
@Ignore
class TaxiSchemaStoreServiceSpringTest {
   @Autowired
   private val webClient: WebTestClient? = null

   @LocalServerPort
   private val port: Int? = null

   private val brokerOrderTaxi = VersionedSource(
      "com.broker", "1.0.0", """
         namespace broker {
           model OrderView {
              orderId: String
           }
         }
      """.trimIndent()
   )

   @Test
   fun `Can submit schemas with http keep alive strategy`() {
      fail("Fix me")
//      val versionedSourceSubmission = VersionedSourceSubmission(listOf(brokerOrderTaxi),
//         PublisherConfiguration("publisher1",
//            HttpPollKeepAlive(pollFrequency = Duration.ofSeconds(60), pollUrl = "http://localhost:$port")
//         )
//      )
//
//      webClient!!.post()
//         .uri("/api/schemas/taxi")
//         .body(Mono.just(versionedSourceSubmission), VersionedSourceSubmission::class.java)
//         .exchange()
//         .expectStatus()
//         .isOk
   }

   @Test
   fun `Can submit schemas with manual keep alive strategy`() {
      fail("fix me")
//      val versionedSourceSubmission = VersionedSourceSubmission(listOf(brokerOrderTaxi), PublisherConfiguration("publisher2"))
//      webClient!!.post()
//         .uri("/api/schemas/taxi")
//         .body(Mono.just(versionedSourceSubmission), VersionedSourceSubmission::class.java)
//         .exchange()
//         .expectStatus()
//         .isOk
   }
}

@TestConfiguration
class TestConfig {
   @Bean
   fun httpPollKeepAliveStrategyMonitor(): KeepAliveStrategyMonitor {
      return HttpPollKeepAliveStrategyMonitor(webClientBuilder = WebClient.builder())
   }

   @Bean
   fun noneKeepAliveStrategyMonitor(): KeepAliveStrategyMonitor = NoneKeepAliveStrategyMonitor
}
