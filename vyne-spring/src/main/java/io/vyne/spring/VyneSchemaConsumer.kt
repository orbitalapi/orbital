package io.vyne.spring

import io.vyne.schemaSpring.VyneConsumerRegistrar
import io.vyne.schemaSpring.VyneHttpSchemaStoreConfig
import io.vyne.schemaSpring.VyneRSocketSchemaStoreConfig
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


