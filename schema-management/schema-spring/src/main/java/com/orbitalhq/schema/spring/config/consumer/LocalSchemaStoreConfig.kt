package com.orbitalhq.schema.spring.config.consumer

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

@ConditionalOnProperty(SchemaConsumerConfigProperties.CONSUMER_METHOD, havingValue = "Local")
@Configuration
class LocalSchemaStoreConfig {

//   @Bean
//   fun simpleSchemaStore():SchemaStore = LocalVal


}
