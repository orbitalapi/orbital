package com.orbitalhq.spring

import com.orbitalhq.schema.spring.config.consumer.SchemaConsumerConfig
import com.orbitalhq.schema.spring.config.consumer.VyneHttpSchemaStoreConfig
import com.orbitalhq.schema.spring.config.consumer.VyneRSocketSchemaStoreConfig
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(
   SchemaConsumerConfig::class,
   VyneHttpSchemaStoreConfig::class,
   VyneRSocketSchemaStoreConfig::class
)
annotation class VyneSchemaConsumer


