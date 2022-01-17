package io.vyne.spring

import io.vyne.schemaSpring.VyneConsumerRegistrar
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(VyneConsumerRegistrar::class,  VyneSchemaStoreConfigRegistrar::class)
annotation class VyneSchemaConsumer


