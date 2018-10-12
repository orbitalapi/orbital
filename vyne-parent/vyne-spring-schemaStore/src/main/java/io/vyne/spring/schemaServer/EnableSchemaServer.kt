package io.vyne.spring.schemaServer

import io.vyne.schemaStore.TaxiSchemaService
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(SchemaServerConfig::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class EnableSchemaServer

@EnableDiscoveryClient
@Configuration
@ComponentScan(basePackageClasses = arrayOf(TaxiSchemaService::class))
class SchemaServerConfig
