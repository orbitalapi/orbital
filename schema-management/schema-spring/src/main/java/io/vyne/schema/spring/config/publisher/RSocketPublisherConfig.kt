package io.vyne.schema.spring.config.publisher

import io.vyne.schema.publisher.http.HttpSchemaPublisher
import io.vyne.schema.publisher.rsocket.RSocketSchemaPublisherTransport
import io.vyne.schema.rsocket.SchemaServerRSocketFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@ConditionalOnProperty(
   SchemaPublisherConfigProperties.PUBLISHER_METHOD,
   havingValue = "RSocket",
   matchIfMissing = true
)
@Configuration
class RSocketPublisherConfig {

   @Bean
   fun rsocketSchemaPublisherTransport(
      rsocketFactory: SchemaServerRSocketFactory,
   ) = RSocketSchemaPublisherTransport(rsocketFactory)

}
