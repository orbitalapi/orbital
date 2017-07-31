package io.polymer.spring.schemaServer

import io.polymer.schemaStore.TaxiSchemaService
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(SchemaServerConfig::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class EnableSchemaServer

@Configuration()
@ComponentScan(basePackageClasses = arrayOf(TaxiSchemaService::class))
class SchemaServerConfig
