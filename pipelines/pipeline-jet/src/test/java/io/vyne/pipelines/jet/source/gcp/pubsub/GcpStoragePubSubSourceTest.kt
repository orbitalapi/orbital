package io.vyne.pipelines.jet.source.gcp.pubsub

import com.google.api.core.ApiService
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.common.io.Resources
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.vyne.connectors.config.MutableConnectionsConfig
import io.vyne.connectors.config.gcp.GcpConnectionConfiguration
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.gcp.pubsub.GcpStoragePubSubTransportInputSpec
import org.awaitility.Awaitility
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.io.path.toPath

class GcpStoragePubSubSourceTest : BaseJetIntegrationTest() {

   // Uses real GCP.
   @Test
   fun `integration test`() {
      val spec = GcpStoragePubSubTransportInputSpec(
         "gcp",
         "Film",
         "orbital-pipelines",
         "demos-392607",
      )
      val credentialsPath = Resources.getResource("credentials/gcp-credentials.json")
         .toURI().toPath()


      val schema = """
         |
         |@io.vyne.formats.Csv(delimiter = ",")
         |model Film {
         | title : FilmTitle inherits String by column("title")
         | rating : Rating inherits String by column("rating")
         |}
      """.trimMargin()

      val testSetup = jetWithSpringAndVyne(
         schema
      )
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(testSetup.applicationContext, targetType = "Film", )
      val connections = testSetup.applicationContext.getBean(MutableConnectionsConfig::class.java)
      connections.googleCloud["gcp"] = GcpConnectionConfiguration(
         "gcp",
         credentialsPath
      )
      val pipelineSpec = PipelineSpec(
         name = "gcp-blog-source",
         input = spec,
         outputs = listOf(
            outputSpec
         )
      )

      startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, pipelineSpec)

      Awaitility.await().atMost(10, TimeUnit.MINUTES).until {
         listSinkTarget.list.isNotEmpty()
      }

      listSinkTarget.list.shouldNotBeEmpty()
   }


   @Test
   fun `dicking about`() {

      val spec = GcpStoragePubSubTransportInputSpec(
         "gcp",
         "Film",
         "orbital-pipelines",
         "demos-392607",
      )

      val credentialsJson = Resources.getResource("credentials/gcp-credentials.json")
      val credentials = GoogleCredentials.fromStream(credentialsJson.openStream())

      val projectId = "demos-392607"
      val subscriptionId = "orbital-pipelines"

      val subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId)

      val receiver = MessageReceiver { message: PubsubMessage, consumer: AckReplyConsumer ->
         println("Received message: ${message.data.toStringUtf8()}")
         consumer.ack()
      }

      val subscriber = Subscriber.newBuilder(subscriptionName, receiver)
         .setCredentialsProvider { credentials }
         .build()

      try {
         // Start the subscriber.
         subscriber.startAsync().awaitRunning()

         println("Listening for messages on $subscriptionName:")

         // Allow the subscriber to run indefinitely unless an exception occurs.
         while (true) {
            // Sleep for a bit before checking if the subscriber is still running.
            TimeUnit.SECONDS.sleep(10)

            if (subscriber.state() == ApiService.State.FAILED) {
               println("Subscriber encountered a problem: ${subscriber.failureCause()}")
               break
            }
         }
      } finally {
         // Stop the subscriber when done with it (typically at application shutdown).
         subscriber.stopAsync()
      }
   }
}
