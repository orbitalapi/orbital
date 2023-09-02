package com.orbitalhq.schema.spring.config.publisher

import com.orbitalhq.schema.publisher.http.HttpSchemaPublisher
import com.orbitalhq.schema.publisher.rsocket.RSocketSchemaPublisherTransport
import com.orbitalhq.schema.rsocket.SchemaServerRSocketFactory
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
