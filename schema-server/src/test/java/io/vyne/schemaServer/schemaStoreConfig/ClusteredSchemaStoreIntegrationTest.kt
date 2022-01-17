package io.vyne.schemaServer.schemaStoreConfig

import com.jayway.awaitility.Awaitility
import com.jayway.awaitility.Duration
import com.nhaarman.mockito_kotlin.verify
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.schemaApi.SchemaSet
import io.vyne.schemaPublisherApi.ManualRemoval
import io.vyne.schemaPublisherApi.PublisherConfiguration
import io.vyne.schemaPublisherApi.SourceSubmissionResponse
import io.vyne.schemaPublisherApi.VersionedSourceSubmission
import io.vyne.schemaServer.SchemaServerApp
import mu.KotlinLogging
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.MediaType
import org.springframework.http.codec.cbor.Jackson2CborDecoder
import org.springframework.http.codec.cbor.Jackson2CborEncoder
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import org.springframework.util.SocketUtils
import org.springframework.web.util.pattern.PathPatternRouteMatcher
import reactor.core.publisher.Flux
import java.util.UUID
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {  }
@RunWith(SpringJUnit4ClassRunner::class)
class ClusteredSchemaStoreIntegrationTest {
   private val hazelCastClusterName = UUID.randomUUID().toString()
   private val taxiSource = VersionedSource("test.taxi", "1.0.0", """
         model Foo {
            bar: String
         }
      """.trimIndent())

   companion object {
      @BeforeClass
      @JvmStatic
      fun setUpHazelcast() {
         System.setProperty( "hz.cluster-name", "schema-server-test-cluster-${UUID.randomUUID()}" )
         System.setProperty( "hz.network.join.multicast.enabled", "false")
         System.setProperty( "hz.network.join.tcp-ip.enabled", "true")
         System.setProperty( "hz.network.join.tcp-ip.members", "127.0.0.1")
      }
   }

   @Test
   fun `Create Schema Server Cluster`() {
      // Setup - bring up two instances of schema servers in cluster mode.
      val schemaServerInstance1 = createClusteredSchemaServerInstance()
      val (rSocketServerPort1, httpServerPort1) = fetchHttpAndRSocketPortsFromSchemaServerApp(schemaServerInstance1)
      logger.info { "Schema Server instance 1 exposing Web Port $httpServerPort1, RSocket Port $rSocketServerPort1" }

      val schemaServerInstance2 = createClusteredSchemaServerInstance()
      val (rSocketServerPort2, httpServerPort2) = fetchHttpAndRSocketPortsFromSchemaServerApp(schemaServerInstance2)
      logger.info { "Schema Server instance 2 exposing Web Port $httpServerPort2, RSocket Port $rSocketServerPort2" }

      // submit a schema to first schema-server through RSocket
      val submissionResult = submitSchemasThroughRSocket(rSocketServerPort1)
      submissionResult!!.schemaSet.sources.first().name.should.equal("test.taxi")

      // try to fetch schemas through second schema-server
      val schemaSet = fetchSchemaThroughRSocket(rSocketServerPort2).blockFirst()
      // then updated state is sent with same version
      Awaitility
         .await()
         .atMost(Duration(15, TimeUnit.SECONDS))
         .until {
            schemaSet!!.sources.size == 1
         }

      schemaSet!!.sources.first().name.should.equal("test.taxi")

      // kill first schema server
      SpringApplication.exit(schemaServerInstance1, object:ExitCodeGenerator {
         override fun getExitCode() = 0
      })
      schemaServerInstance1.isRunning.should.be.`false`

      // try to fetch the schema through second schema-server again
      fetchSchemaThroughRSocket(rSocketServerPort2).blockFirst().sources.first().name.should.equal("test.taxi")
   }

   private fun submitSchemasThroughRSocket(port: Int): SourceSubmissionResponse? {
      val requester = rsocketRequesterForPort(port)
      val submission = VersionedSourceSubmission(
         listOf(taxiSource),
         PublisherConfiguration("testPublisher", ManualRemoval))

      return requester
         .route("request.vyneSchemaSubmission")
         .data(submission)
         .retrieveMono(SourceSubmissionResponse::class.java)
         .block()
   }

   private fun fetchSchemaThroughRSocket(port: Int): Flux<SchemaSet> {
      val requester = rsocketRequesterForPort(port)
      return requester.route("stream.vyneSchemaSets").retrieveFlux(SchemaSet::class.java)
   }

   private fun rsocketRequesterForPort(port: Int): RSocketRequester {
      return RSocketRequester
         .builder()
         .dataMimeType(MediaType.APPLICATION_CBOR)
         .rsocketStrategies(RSocketStrategies.builder()
            .encoders { it.add(Jackson2CborEncoder()) }
            .decoders { it.add(Jackson2CborDecoder()) }
            .routeMatcher(PathPatternRouteMatcher())
            .build())
         .connectTcp("localhost", port)
         .block()!!
   }

   private fun fetchHttpAndRSocketPortsFromSchemaServerApp(ctx: ConfigurableApplicationContext): Pair<Int, Int> {
      val rSocketServerPort1 = ctx.environment.getProperty("vyne.schema.server.port")
      val httpServerPort1 = ctx.environment.getProperty("server.port")
      return Pair(rSocketServerPort1!!.toInt(), httpServerPort1!!.toInt())
   }

   private fun createClusteredSchemaServerInstance(): ConfigurableApplicationContext {
      return SpringApplicationBuilder()
         .sources(SchemaServerApp::class.java)
         .properties(
            "spring.main.allow-bean-definition-overriding=true",
            "eureka.client.enabled=false",
            "vyne.schema.server.clustered=true",
            "vyne.schema.server.port=${SocketUtils.findAvailableTcpPort()}",
            "hazelcast.config=classpath:hz-test-config.xml"
         )
         // for some reason port numbers can only be overridden via run args.
         .run(
            "--server.port=${SocketUtils.findAvailableTcpPort()}",
            "--vyne.schema.server.port=${SocketUtils.findAvailableTcpPort()}",
            "--spring.application.name=${UUID.randomUUID()}",
            "--hazelcast.config=classpath:hz-test-config.xml"
         )
   }
}
