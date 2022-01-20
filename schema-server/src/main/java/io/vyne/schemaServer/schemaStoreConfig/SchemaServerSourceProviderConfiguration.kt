package io.vyne.schemaServer.schemaStoreConfig

import io.rsocket.core.RSocketServer
import io.rsocket.transport.netty.server.CloseableChannel
import io.rsocket.transport.netty.server.TcpServerTransport
import io.vyne.httpSchemaPublisher.HttpPollKeepAliveStrategyMonitor
import io.vyne.httpSchemaPublisher.HttpPollKeepAliveStrategyPollUrlResolver
import io.vyne.rSocketSchemaPublisher.RSocketPublisherKeepAliveStrategyMonitor
import io.vyne.schemaApi.SchemaSet
import io.vyne.schemaPublisherApi.NoneKeepAliveStrategyMonitor
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemaStore.ValidatingSchemaStoreClient
import mu.KotlinLogging
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.cbor.Jackson2CborDecoder
import org.springframework.http.codec.cbor.Jackson2CborEncoder
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.pattern.PathPatternRouteMatcher
import reactor.core.publisher.Flux
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import java.time.Duration
import java.util.Optional

@ConditionalOnExpression("T(org.springframework.util.StringUtils).isEmpty('\${vyne.schema.publicationMethod:}')")
@Configuration
class SchemaServerSourceProviderConfiguration {
   @Bean
   fun rsocketMessageHandler(rsocketStrategies: RSocketStrategies) = RSocketMessageHandler().apply {
      rSocketStrategies = rsocketStrategies
   }

   @Bean
   fun rsocketStrategies() = RSocketStrategies.builder()
      .encoders { it.add(Jackson2CborEncoder()) }
      .decoders { it.add(Jackson2CborDecoder()) }
      .routeMatcher(PathPatternRouteMatcher())
      .build()

   @Bean
   fun socketServerStarter(
      @Value("\${vyne.schema.server.port:7655}") rsocketPort: Int,
      rsocketMessageHandler: RSocketMessageHandler): SocketServerStarter {
      return SocketServerStarter(rsocketPort, rsocketMessageHandler)
   }

   @Bean
   @ConditionalOnExpression("!'\${vyne.schema.server.clustered:false}'")
   fun localValidatingSchemaStoreClient(): ValidatingSchemaStoreClient = LocalValidatingSchemaStoreClient()

   @Bean
   fun httpPollKeepAliveStrategyPollUrlResolver(discoveryClient: Optional<DiscoveryClient>) = HttpPollKeepAliveStrategyPollUrlResolver(discoveryClient)

   @Bean
   @ConditionalOnExpression("!'\${vyne.schema.server.clustered:false}'")
   fun httpPollKeepAliveStrategyMonitor(
      @Value("\${vyne.schema.management.ttlCheckInSeconds:1}") ttlCheckInSeconds: Long,
      @Value("\${vyne.schema.management.httpRequestTimeoutInSeconds:30}") httpRequestTimeoutInSeconds: Long,
      httpPollKeepAliveStrategyPollUrlResolver: HttpPollKeepAliveStrategyPollUrlResolver,
      webClientBuilder: WebClient.Builder
   ): HttpPollKeepAliveStrategyMonitor = HttpPollKeepAliveStrategyMonitor(
      ttlCheckPeriod = Duration.ofSeconds(ttlCheckInSeconds),
      httpRequestTimeoutInSeconds = httpRequestTimeoutInSeconds,
      pollUrlResolver = httpPollKeepAliveStrategyPollUrlResolver,
      webClientBuilder = webClientBuilder)

   @Bean
   fun noneKeepAliveStrategyMonitor() = NoneKeepAliveStrategyMonitor

   @Bean
   fun rSocketPublisherKeepAliveStrategyMonitor() = RSocketPublisherKeepAliveStrategyMonitor()
}

private val logger = KotlinLogging.logger { }

class SocketServerStarter(
   private val rsocketPort: Int,
   private val rsocketMessageHandler: RSocketMessageHandler
) : InitializingBean {
   private var server: CloseableChannel? = null
   override fun afterPropertiesSet() {
      logger.info { "Starting RSocket Server on port $rsocketPort ..." }
      startRSocketServer()
   }

   private fun startRSocketServer() {
      RSocketServer
         .create(rsocketMessageHandler.responder())
         .bind(TcpServerTransport.create(rsocketPort))
         .subscribe {
            logger.info { "RSocket Server Channel Opened at address: ${it.address()}" }
            server = it
         }
   }
}

interface SchemaUpdateNotifier {
   fun sendSchemaUpdate()
   val schemaSetFlux: Flux<SchemaSet>
}

class LocalSchemaNotifier(private val validatingStore: ValidatingSchemaStoreClient): SchemaUpdateNotifier {
   private val schemaSetSink = Sinks.many().replay().latest<SchemaSet>()
   override val schemaSetFlux: Flux<SchemaSet> = schemaSetSink.asFlux()
   private val emitFailureHandler = Sinks.EmitFailureHandler { _: SignalType?, emitResult: Sinks.EmitResult ->
      (emitResult
         == Sinks.EmitResult.FAIL_NON_SERIALIZED)
   }
   override fun sendSchemaUpdate() {
      schemaSetSink.emitNext(validatingStore.schemaSet(), emitFailureHandler)
   }
}
