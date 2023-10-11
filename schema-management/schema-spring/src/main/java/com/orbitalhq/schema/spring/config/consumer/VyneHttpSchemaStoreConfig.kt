package com.orbitalhq.schema.spring.config.consumer

import com.orbitalhq.schema.consumer.http.HttpSchemaStore
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@ConditionalOnProperty(SchemaConsumerConfigProperties.CONSUMER_METHOD, havingValue = "Http", matchIfMissing = false)
@Configuration
@Import(HttpSchemaStore::class)
class VyneHttpSchemaStoreConfig
