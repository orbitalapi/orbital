package com.orbitalhq.spring


import com.orbitalhq.schema.spring.config.RSocketTransportConfig
import com.orbitalhq.schema.spring.config.publisher.HttpSchemaPublisherConfig
import com.orbitalhq.schema.spring.config.publisher.SchemaPublisherConfig
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.context.annotation.Import


/**
 * A service which publishes Vyne Schemas to another component.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ImportAutoConfiguration
@Import(
   SchemaPublisherConfig::class,
   HttpSchemaPublisherConfig::class,
   RSocketTransportConfig::class
)
annotation class VyneSchemaPublisher

