package io.vyne.schema.spring.config.consumer

import io.vyne.schema.consumer.rsocket.RSocketSchemaStore
import io.vyne.schema.rsocket.CBORJackson
import io.vyne.schema.rsocket.SchemaServerRSocketFactory
import io.vyne.schema.spring.config.RSocketTransportConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@ConditionalOnProperty(SchemaConsumerConfigProperties.CONSUMER_METHOD, havingValue = "RSocket", matchIfMissing = true)
@Configuration
@Import(RSocketTransportConfig::class)
class VyneRSocketSchemaStoreConfig {
   @Bean
   fun rsocketSchemaStore(rsocketFactory: SchemaServerRSocketFactory): RSocketSchemaStore {
      return RSocketSchemaStore(
          rsocketFactory,
          CBORJackson.defaultMapper
      )
   }
}
