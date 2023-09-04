package com.orbitalhq.schema.consumer.rsocket

import com.jayway.awaitility.Awaitility
import io.rsocket.SocketAcceptor
import io.rsocket.core.RSocketServer
import io.rsocket.transport.netty.server.CloseableChannel
import io.rsocket.transport.netty.server.TcpServerTransport
import io.rsocket.util.DefaultPayload
import com.orbitalhq.ParsedSource
import com.orbitalhq.VersionedSource
import com.orbitalhq.asParsedPackage
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.rsocket.CBORJackson
import com.orbitalhq.schema.rsocket.SchemaServerRSocketFactory
import com.orbitalhq.schema.rsocket.TcpAddress
import com.orbitalhq.schemas.SchemaSetChangedEvent
import com.orbitalhq.utils.log
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import org.springframework.test.util.TestSocketUtils
import reactor.core.Disposable
import reactor.core.publisher.Sinks
import reactor.kotlin.core.publisher.toFlux
import java.util.concurrent.TimeUnit

class RSocketSchemaStoreTest {

   private var server: CloseableChannel? = null

   @After
   fun tearDown() {
      server?.dispose()
   }

   @Test
   @Ignore // uysed at dev time
   fun `consume from real schema server`() {
      val events = mutableListOf<SchemaSetChangedEvent>()
      RSocketSchemaStore(
         SchemaServerRSocketFactory(TcpAddress("localhost", 7655))
      ).schemaChanged.toFlux().subscribe { event -> events.add(event) }

      Awaitility.await().atMost(5, TimeUnit.SECONDS)
         .until<Boolean> { events.size == 1 }
   }

   @Test
   fun `consumes schema over rsocket`() {
      val port = TestSocketUtils.findAvailableTcpPort()
      val (sink, _) = startServer(port)

      val events = mutableListOf<SchemaSetChangedEvent>()
      RSocketSchemaStore(
         SchemaServerRSocketFactory(TcpAddress("localhost", port))
      ).schemaChanged.toFlux().subscribe { event -> events.add(event) }

      sink.tryEmitNext(
         SchemaSet.fromParsed(
            listOf(ParsedSource(VersionedSource.sourceOnly("type HelloWorld")).asParsedPackage()),
            1
         )
      )

      Awaitility.await().atMost(5, TimeUnit.SECONDS)
         .until<Boolean> { events.size == 1 }
   }

   @Test
   fun `duplicate events from server don't emit multiple events`() {
      val port = TestSocketUtils.findAvailableTcpPort()
      val (sink, _) = startServer(port)

      val events = mutableListOf<SchemaSetChangedEvent>()
      RSocketSchemaStore(
         SchemaServerRSocketFactory(TcpAddress("localhost", port))
      ).schemaChanged.toFlux().subscribe { event -> events.add(event) }

      val schemaSet = SchemaSet.fromParsed(
         listOf(ParsedSource(VersionedSource.sourceOnly("type HelloWorld")).asParsedPackage()),
         1
      )

      // Emit the same event multiple times
      sink.tryEmitNext(schemaSet)
      sink.tryEmitNext(schemaSet)
      sink.tryEmitNext(schemaSet)

      Awaitility.await().atMost(5, TimeUnit.SECONDS)
         .until<Boolean> { events.size == 1 }
   }


   fun startServer(
      port: Int = 0,
   ): Pair<Sinks.Many<SchemaSet>, Disposable> {
      val sink = Sinks.many().unicast().onBackpressureBuffer<SchemaSet>()
      val flux = sink.asFlux()

      log().info(
         """*********************************************
         |*********************************************
         |*** Starting RSocket Server on port $port ***
         |*********************************************
      """.trimMargin().trim()
      )

      val rsocketServer = RSocketServer
         .create(
            SocketAcceptor.forRequestStream { payload ->
               flux.map {
                  DefaultPayload.create(CBORJackson.defaultMapper.writeValueAsBytes(it))
               }
            }
         )
      return sink to rsocketServer
         .bind(TcpServerTransport.create("localhost", port))
         .subscribe { closeable ->
            server = closeable
         }
   }

}
