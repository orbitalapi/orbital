package io.vyne.spring

import io.vyne.schema.spring.VyneConsumerRegistrar
import io.vyne.schema.spring.VyneHttpSchemaStoreConfig
import io.vyne.schema.spring.VyneRSocketSchemaStoreConfig
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(
   VyneConsumerRegistrar::class,
   VyneSchemaStoreConfigRegistrar::class,
   VyneHttpSchemaStoreConfig::class,
   VyneRSocketSchemaStoreConfig::class
)
annotation class VyneSchemaConsumer


