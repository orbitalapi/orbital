package io.vyne.spring

import io.vyne.schema.spring.config.consumer.SchemaConsumerConfig
import io.vyne.schema.spring.config.consumer.VyneConsumerRegistrar
import io.vyne.schema.spring.config.consumer.VyneHttpSchemaStoreConfig
import io.vyne.schema.spring.config.consumer.VyneRSocketSchemaStoreConfig
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(
   VyneConsumerRegistrar::class,
   SchemaConsumerConfig::class,
   VyneSchemaStoreConfigRegistrar::class,
   VyneHttpSchemaStoreConfig::class,
   VyneRSocketSchemaStoreConfig::class
)
annotation class VyneSchemaConsumer


