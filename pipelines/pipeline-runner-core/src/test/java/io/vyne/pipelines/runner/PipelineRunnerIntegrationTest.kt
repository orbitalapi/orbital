package io.vyne.pipelines.runner

import TestWebSocketServer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jayway.awaitility.Awaitility.await
import com.netflix.appinfo.ApplicationInfoManager
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.pipelines.PipelineTransportHealthMonitor
import io.vyne.pipelines.orchestrator.events.PipelineEventsApi
import io.vyne.pipelines.runner.jobs.PipelineStateManager
import io.vyne.pipelines.runner.transport.kafka.AbstractKafkaTest
import io.vyne.schemas.fqn
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner
import java.lang.Thread.sleep


@RunWith(SpringRunner::class)
@SpringBootTest(
   classes = [PipelineRunnerTestApp::class],
   webEnvironment = RANDOM_PORT,
   properties = [
      "spring.main.allow-bean-definition-overriding=true",
      "eureka.client.enabled=false",
      "vyne.schema.publicationMethod=LOCAL",
      "spring.cloud.config.enabled= false",
      "spring.application.name=test-pipeline-runner",
      "vyne.caskService.name=CASK",
      "vyne.hazelcast.enabled=false",
      "vyne.hazelcast.discovery=multicast",
      "vyne.hazelcast.memberTag=pipeline"
   ])
@EnableConfigurationProperties(VyneSpringHazelcastConfiguration::class)
class PipelineRunnerIntegrationTest : AbstractKafkaTest() {

   @LocalServerPort
   val webServerPort = 0

   @Autowired
   lateinit var restTemplate: TestRestTemplate

   @MockBean
   lateinit var client: DiscoveryClient;

   @MockBean
   lateinit var applicationInfoManager: ApplicationInfoManager

   @MockBean
   lateinit var pipelineEventApi: PipelineEventsApi

   val websocketServer = TestWebSocketServer()

   @Autowired
   lateinit var pipelineStateManager: PipelineStateManager


   @Before
   fun setupTest() {

       //Mock cask service
      val instanceInfo: ServiceInstance = mock()
      whenever(instanceInfo.host).thenReturn("localhost")
      whenever(instanceInfo.port).thenReturn(websocketServer.port)
      whenever(client.getInstances("CASK")).thenReturn(listOf(instanceInfo))

   }

   @After
   fun cleanupTest() {
      websocketServer.clearMessages()
   }

   @Test
   fun canReceiveFromKafkaInput() {
      val pipelineDescription = """
           {
              "name" : "test-pipeline",
              "input" : {
                "type" : "${"PersonLoggedOnEvent".fqn()}",
                "transport" : {
                  "topic" : "$topicName",
                  "targetType" : "${"PersonLoggedOnEvent".fqn()}",
                  "props" : {
                    "group.id" : "vyne-pipeline-group",
                    "bootstrap.servers" : "${embeddedKafkaBroker.brokersAsString}",
                    "heartbeat.interval.ms" : "3000",
                    "session.timeout.ms" : "10000",
                    "auto.offset.reset" : "earliest"
                  },
                  "type" : "kafka",
                  "direction" : "INPUT"
                }
              },
              "output" : {
                "type" : "${"PersonLoggedOnEvent".fqn()}",
                "transport" : {
                  "props" : {
                  },
                  "targetType" : "${"PersonLoggedOnEvent".fqn()}",
                  "type" : "cask",
                  "direction" : "OUTPUT"
                }
              }
            }
        """.trimIndent()

       //Create the pipeline
      postPipeline(pipelineDescription)

       //Send for messages into kafka
      sendKafkaMessage(""" {"userId":"Marty"} """)
      sendKafkaMessage(""" {"userId":"Andrzej"} """)
      sendKafkaMessage(""" {"userId":"Serhat"} """)

      await().until { websocketServer.messagesReceived.should.have.size(3) }
   }

   @Test
   fun CaskOutput_can_cache_messages() {
      val pipelineDescription = """
           {
              "name" : "test-pipeline-1",
              "input" : {
                "type" : "${"PersonLoggedOnEvent".fqn()}",
                "transport" : {
                  "topic" : "$topicName",
                  "targetType" : "${"PersonLoggedOnEvent".fqn()}",
                  "props" : {
                    "group.id" : "vyne-pipeline-group",
                    "bootstrap.servers" : "${embeddedKafkaBroker.brokersAsString}",
                    "heartbeat.interval.ms" : "3000",
                    "session.timeout.ms" : "10000",
                    "auto.offset.reset" : "earliest"
                  },
                  "type" : "kafka",
                  "direction" : "INPUT"
                }
              },
              "output" : {
                "type" : "${"PersonLoggedOnEvent".fqn()}",
                "transport" : {
                  "props" : {
                  },
                  "targetType" : "${"PersonLoggedOnEvent".fqn()}",
                  "type" : "cask",
                  "direction" : "OUTPUT"
                }
              }
            }
        """.trimIndent()

      val instanceInfo: ServiceInstance = mock()
      whenever(instanceInfo.host).thenReturn("localhost")
      whenever(instanceInfo.port).thenReturn(websocketServer.port)
      whenever(client.getInstances("CASK")).thenAnswer {
          //Delay the response, so that CaskOutput will be in 'INIT' state.
          //discovering Cask takes time...
         sleep(2500)
         listOf(instanceInfo)
      }

       //Create the pipeline
      val pipelineId = postPipeline(pipelineDescription)!!.body!!.spec.name
      val pipeline =  pipelineStateManager.pipelines[pipelineId]

       //Set the CaskOutput state to UP manually, this will flow Kafka messages into CaskOutput
       //but CaskOutput is still waiting for Cask (see above) and hence web socket session not initialised.
       //In this case, CaskOutput should be able to buffer the incoming messages. (i.e. should not drop them)
      pipeline!!.reportHealthStatus(PipelineTransportHealthMonitor.PipelineTransportStatus.UP,
         PipelineTransportHealthMonitor.PipelineTransportStatus.UP)

       //Send for messages into kafka
      sendKafkaMessage(""" {"userId":"Marty"} """)
      sendKafkaMessage(""" {"userId":"Andrzej"} """)
      sendKafkaMessage(""" {"userId":"Serhat"} """)

      await().until { websocketServer.messagesReceived.should.have.size(3) }
   }

   /**
    * Convenient method to POST the pipeline description
    */
   fun postPipeline(pipelineDescription: String): ResponseEntity<PipelineInstanceReference>? {

       val client = ApacheClient()
       val request = org.http4k.core.Request(method = Method.POST, uri = "http://localhost:$webServerPort/api/pipelines").body(pipelineDescription).headers(
           listOf(Pair("Content-Type", "application/json")))

       val response = client.invoke(request)
       val mapper = jacksonObjectMapper().registerModule(PipelineRunnerTestApp.pipelineModule()).registerModule(JavaTimeModule())
       val pipelineInstanceReference: PipelineInstanceReference = mapper.readValue(response.bodyString())
       return ResponseEntity.ok().body(pipelineInstanceReference)

   }

}
