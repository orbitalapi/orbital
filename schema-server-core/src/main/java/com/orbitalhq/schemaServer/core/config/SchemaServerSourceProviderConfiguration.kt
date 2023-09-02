package com.orbitalhq.schemaServer.core.config

import io.rsocket.core.RSocketServer
import io.rsocket.transport.netty.server.CloseableChannel
import io.rsocket.transport.netty.server.TcpServerTransport
import com.orbitalhq.schema.publisher.ExpiringSourcesStore
import com.orbitalhq.schema.publisher.KeepAliveStrategyMonitor
import com.orbitalhq.schema.publisher.NoneKeepAliveStrategyMonitor
import com.orbitalhq.schema.publisher.http.HttpPollKeepAliveStrategyMonitor
import com.orbitalhq.schema.publisher.rsocket.RSocketPublisherKeepAliveStrategyMonitor
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.schemaStore.TaxiSchemaValidator
import com.orbitalhq.schemaStore.ValidatingSchemaStoreClient
import com.orbitalhq.schemas.readers.SourceConverterRegistry
import mu.KotlinLogging
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.cbor.Jackson2CborDecoder
import org.springframework.http.codec.cbor.Jackson2CborEncoder
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.pattern.PathPatternRouteMatcher
import java.time.Duration

@Configuration
class SchemaServerSourceProviderConfiguration {
   @Bean
   fun rsocketMessageHandler(rsocketStrategies: RSocketStrategies) = RSocketMessageHandler().apply {
      rSocketStrategies = rsocketStrategies
   }

   @Bean
   fun rsocketStrategies() = RSocketStrategies.builder()
      .encoders { it.add(Jackson2CborEncoder()) }
      .decoders {
         it.add(Jackson2CborDecoder())
      }
      .routeMatcher(PathPatternRouteMatcher())
      .build()

   @Bean
   fun socketServerStarter(
      @Value("\${vyne.schema.server.port:7655}") rsocketPort: Int,
      rsocketMessageHandler: RSocketMessageHandler
   ): SocketServerStarter {
      return SocketServerStarter(rsocketPort, rsocketMessageHandler)
   }

   @Bean
   @ConditionalOnExpression("!'\${vyne.schema.server.clustered:false}'")
   fun localValidatingSchemaStoreClient(sourceConverterRegistry: SourceConverterRegistry): ValidatingSchemaStoreClient =
      LocalValidatingSchemaStoreClient(
         TaxiSchemaValidator(
            sourceLoaders = sourceConverterRegistry.converters
         )
      )

//   @Bean
//   fun httpPollKeepAliveStrategyPollUrlResolver(discoveryClient: Optional<DiscoveryClient>) =
//      HttpPollKeepAliveStrategyPollUrlResolver(discoveryClient)

   @Bean
   @ConditionalOnExpression("!'\${vyne.schema.server.clustered:false}'")
   fun expiringSourcesStore(keepAliveStrategyMonitors: List<KeepAliveStrategyMonitor>): ExpiringSourcesStore {
      return ExpiringSourcesStore(keepAliveStrategyMonitors = keepAliveStrategyMonitors)
   }

   @Bean
   @ConditionalOnExpression("!'\${vyne.schema.server.clustered:false}'")
   fun schemaUpdateNotifier(validatingStore: ValidatingSchemaStoreClient): SchemaUpdateNotifier {
      return LocalSchemaNotifier(validatingStore)
   }


   @Bean
   @ConditionalOnExpression("!'\${vyne.schema.server.clustered:false}'")
   fun httpPollKeepAliveStrategyMonitor(
      @Value("\${vyne.schema.management.keepAlivePollFrequency:1s}") keepAlivePollFrequency: Duration,
      @Value("\${vyne.schema.management.httpRequestTimeout:30s}") httpRequestTimeout: Duration,
      webClientBuilder: WebClient.Builder
   ): HttpPollKeepAliveStrategyMonitor = HttpPollKeepAliveStrategyMonitor(
      pollFrequency = keepAlivePollFrequency,
      webClientBuilder = webClientBuilder
   )

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

