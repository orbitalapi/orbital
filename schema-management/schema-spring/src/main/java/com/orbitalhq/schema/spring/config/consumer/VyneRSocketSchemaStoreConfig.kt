package com.orbitalhq.schema.spring.config.consumer

import com.orbitalhq.schema.consumer.rsocket.RSocketSchemaStore
import com.orbitalhq.schema.rsocket.CBORJackson
import com.orbitalhq.schema.rsocket.SchemaServerRSocketFactory
import com.orbitalhq.schema.spring.config.RSocketTransportConfig
import com.orbitalhq.schemas.readers.SourceConverterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@ConditionalOnProperty(SchemaConsumerConfigProperties.CONSUMER_METHOD, havingValue = "RSocket", matchIfMissing = true)
@Configuration
@Import(RSocketTransportConfig::class)
class VyneRSocketSchemaStoreConfig {
   @Bean
   fun rsocketSchemaStore(
      rsocketFactory: SchemaServerRSocketFactory,
      sourceConverterRegistry: SourceConverterRegistry
   ): RSocketSchemaStore {
      return RSocketSchemaStore(
         rsocketFactory,
         CBORJackson.defaultMapper,
         sourceConverterRegistry
      )
   }
}
