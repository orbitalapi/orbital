package io.vyne.spring

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.convert.ConversionService
import kotlin.reflect.KClass

enum class SchemaPublicationMethod {
   /**
    * Turns off schema publication
    */
   DISABLED,

   /**
    * Publish schema to local query server.
    */
   LOCAL,

   /**
    * Publish schemas to a remote query server, and execute queries there
    */
   REMOTE,

   /**
    * Publish metadata about this schema to Eureka, and let Vyne fetch on demand
    */
   EUREKA,

   /**
    * Use a distributed mesh of schemas, and execute queries locally.
    * Enterprise only.
    */
   DISTRIBUTED
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(VyneConfigRegistrar::class, VyneSpringConfig::class)
annotation class VyneSchemaPublisher(val basePackageClasses: Array<KClass<out Any>> = [],
                                     val publicationMethod: SchemaPublicationMethod = SchemaPublicationMethod.REMOTE,
                                     val schemaFile: String = ""
)

class VyneSpringConfig {
   // Required to support parsing of default durations in HttpSchemaStoreClient.
   // Not needed after Spring Boot 2.1:
   // https://stackoverflow.com/questions/51818137/spring-boot-2-converting-duration-java-8-application-properties/51823308
   @Bean
   @ConditionalOnMissingBean(ConversionService::class)
   fun conversionService(): ConversionService {
      return ApplicationConversionService.getSharedInstance()
   }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(EnableVyneConfiguration::class)
annotation class VyneQueryServer

