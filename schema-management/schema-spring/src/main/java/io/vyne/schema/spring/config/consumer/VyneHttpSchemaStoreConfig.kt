package io.vyne.schema.spring.config.consumer

import io.vyne.schema.consumer.http.HttpSchemaStore
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@ConditionalOnProperty(SchemaConsumerConfigProperties.CONSUMER_METHOD, havingValue = "Http", matchIfMissing = false)
@Configuration
@Import(HttpSchemaStore::class)
class VyneHttpSchemaStoreConfig
