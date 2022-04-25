package io.vyne.spring


import io.vyne.schema.publisher.loaders.FileSystemSourcesLoader
import io.vyne.schema.publisher.loaders.SchemaSourcesLoader
import io.vyne.schema.spring.config.RSocketTransportConfig
import io.vyne.schema.spring.config.publisher.HttpSchemaPublisherConfig
import io.vyne.schema.spring.config.publisher.VynePublisherRegistrar
import io.vyne.schema.spring.config.publisher.SchemaPublisherConfig
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.context.annotation.Import
import kotlin.reflect.KClass

//const val VYNE_SCHEMA_PUBLICATION_METHOD = "vyne.schema.publicationMethod"
//const val VyneRemoteSchemaStoreTllCheckInSeconds = "vyne.schema.management.ttlCheckInSeconds"
//const val VyneRemoteSchemaStoreHttpRequestTimeoutInSeconds = "vyne.schema.management.httpRequestTimeoutInSeconds"

//interface StoreConfiguratorProvider {
//   fun storeConfigurator(): StoreConfigurator
//}

//enum class SchemaPublicationMethod : StoreConfiguratorProvider {
//   /**
//    * Turns off schema publication
//    */
//   DISABLED {
//      override fun storeConfigurator() = DisabledStoreConfigurator
//   },
//
//   /**
//    * Publish schema to local query server.
//    */
//   @Deprecated("Deprecated")
//   LOCAL {
//      override fun storeConfigurator() = LocalStoreConfigurator
//   },
//
//   RSOCKET {
//      override fun storeConfigurator() = DisabledStoreConfigurator
//   },
//
//   HTTP {
//      override fun storeConfigurator() = DisabledStoreConfigurator
//   };
//}

/**
 * A service which publishes Vyne Schemas to another component.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ImportAutoConfiguration
@Import(
   VynePublisherRegistrar::class,
   SchemaPublisherConfig::class,
   VyneSchemaStoreConfigRegistrar::class,
   HttpSchemaPublisherConfig::class,
   RSocketTransportConfig::class
)
annotation class VyneSchemaPublisher(
   val basePackageClasses: Array<KClass<out Any>> = [],
   @Deprecated("use projectPath")
   val schemaFile: String = "",
   val projectPath: String = "",
   val sourcesLoader: KClass<out SchemaSourcesLoader> = FileSystemSourcesLoader::class,
   val projects: Array<VyneSchemaProject> = []
)

annotation class VyneSchemaProject(
   val projectPath: String = "",
   val sourcesLoader: KClass<out SchemaSourcesLoader> = FileSystemSourcesLoader::class
)


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(EnableVyneConfiguration::class)
annotation class VyneQueryServer

