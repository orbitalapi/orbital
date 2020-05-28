package io.vyne.pipelines.runner.transport.cask

import com.jayway.awaitility.Awaitility.await
import com.nhaarman.mockitokotlin2.*
import io.vyne.VersionedTypeReference
import io.vyne.pipelines.PipelineTransportHealthMonitor
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.*
import io.vyne.schemas.fqn
import org.junit.Before
import org.junit.Test
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.WebSocketClient
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.URI
import java.util.concurrent.TimeUnit.SECONDS

class CaskOutputTest {

   lateinit var caskOutput: CaskOutput

   lateinit var spec: CaskTransportOutputSpec
   lateinit var discoveryClient: DiscoveryClient
   val caskServiceName = "CASK"
   lateinit var healthMonitor: PipelineTransportHealthMonitor
   lateinit var wsClient: WebSocketClient

   lateinit var session: WebSocketSession
   val caskInputMessages =  EmitterProcessor.create<WebSocketMessage>()

   @Before
   fun setup() {
      spec = CaskTransportOutputSpec(emptyMap(), VersionedTypeReference("imdb.Actor".fqn()))
      discoveryClient = mock()
      healthMonitor = mock()
      wsClient = mock()

      mockWebSocketSession()
   }

   private fun mockWebSocketSession() {
      // Handshake mono
      var handshakeMono = mock<Mono<Void>>();
      whenever(handshakeMono.doOnError(any())).thenReturn(mock())
      whenever(wsClient.execute(any(), any())).thenReturn(handshakeMono)

      // Websocket session mono/flux
      session = mock()
      whenever(session.send(any())).thenReturn(Mono.create {  })
      whenever(session.receive()).thenReturn(caskInputMessages)
   }

   private fun mockCaskServer(): ServiceInstance {
      val serviceInstance = mock<ServiceInstance>()
      whenever(serviceInstance.host).thenReturn("192.168.0.2")
      whenever(serviceInstance.port).thenReturn(8989)

      whenever(discoveryClient.getInstances(caskServiceName)).thenReturn(
         listOf(serviceInstance)
      )

      return serviceInstance
   }


   @Test
   fun testCaskServerWithParameters() {
      mockCaskServer()
      spec = CaskTransportOutputSpec(mapOf(
         "content-type" to "csv",
         "csv.delimiter" to "|",
         "csv.otherParam" to "XXX",
         "csv.header.included" to "false",
         "json.nonIncluded" to "YYY"
         ), VersionedTypeReference("Actor".fqn()))

      caskOutput = CaskOutput(spec, discoveryClient, caskServiceName, healthMonitor, wsClient, 100)

      await().atMost(2, SECONDS).until {
         verify(wsClient).execute(eq(URI("ws://192.168.0.2:8989/cask/csv/Actor?delimiter=%7C&header.included=false&otherParam=XXX")), any())
      }
   }

   @Test
   fun testCaskServerNoParameters() {
      mockCaskServer()

      caskOutput = CaskOutput(spec, discoveryClient, caskServiceName, healthMonitor, wsClient, 100)

      await().atMost(2, SECONDS).until {
         verify(wsClient).execute(eq(URI("ws://192.168.0.2:8989/cask/json/imdb.Actor?")), any())
      }
   }

   @Test
   fun testCaskServerHandshakeError() {
      mockCaskServer()

      whenever(wsClient.execute(any(),any())).thenReturn(Mono.error(RuntimeException("Test")))

      caskOutput = CaskOutput(spec, discoveryClient, caskServiceName, healthMonitor, wsClient, 100)

      await().atMost(2, SECONDS).until {
         verify(healthMonitor).reportStatus(DOWN)
      }
   }

   @Test
   fun testCaskServerHandshakeSuccess() {
      mockCaskServer()

      caskOutput = CaskOutput(spec, discoveryClient, caskServiceName, healthMonitor, wsClient, 100)


      caskOutput.wsHandler.handle(session)

      await().atMost(2, SECONDS).until {
         verify(healthMonitor).reportStatus(UP)
      }
   }

   @Test
   fun testCaskOutputWrite() {
      mockCaskServer()

      caskOutput = CaskOutput(spec, discoveryClient, caskServiceName, healthMonitor, wsClient, 100)

      caskOutput.write(""" This is a Message """, mock())
      caskOutput.write(""" This is a second message """, mock())

      StepVerifier
         .create(caskOutput.wsOutput)
         .expectNext(""" This is a Message """)
         .expectNext(""" This is a second Message """)
   }

}
