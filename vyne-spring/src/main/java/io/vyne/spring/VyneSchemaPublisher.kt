package io.vyne.spring


import io.vyne.schemaPublisherApi.loaders.FileSystemSourcesLoader
import io.vyne.schemaPublisherApi.loaders.SchemaSourcesLoader
import io.vyne.schemaSpring.DisabledStoreConfigurator
import io.vyne.schemaSpring.StoreConfigurator
import io.vyne.schemaSpring.VynePublisherRegistrar
import io.vyne.spring.storeconfigurators.EurekaStoreConfigurator
import io.vyne.spring.storeconfigurators.HazelcastStoreConfigurator
import io.vyne.spring.storeconfigurators.LocalStoreConfigurator
import io.vyne.spring.storeconfigurators.RemoteStoreConfigurator
import org.springframework.context.annotation.Import
import kotlin.reflect.KClass

const val VYNE_SCHEMA_PUBLICATION_METHOD = "vyne.schema.publicationMethod"
const val VyneRemoteSchemaStoreTllCheckInSeconds = "vyne.schema.management.ttlCheckInSeconds"
const val VyneRemoteSchemaStoreHttpRequestTimeoutInSeconds = "vyne.schema.management.httpRequestTimeoutInSeconds"

interface StoreConfiguratorProvider {
   fun storeConfigurator(): StoreConfigurator
}

enum class SchemaPublicationMethod: StoreConfiguratorProvider {
   /**
    * Turns off schema publication
    */
   DISABLED {
      override fun storeConfigurator() = DisabledStoreConfigurator
   },

   /**
    * Publish schema to local query server.
    */
   @Deprecated("Deprecated")
   LOCAL {
      override fun storeConfigurator() = LocalStoreConfigurator
   },

   /**
    * Publish schemas to a remote query server, and execute queries there
    */
   REMOTE {
      override fun storeConfigurator() = RemoteStoreConfigurator
   },

   /**
    * Publish metadata about this schema to Eureka, and let Vyne fetch on demand
    */
   EUREKA {
      override fun storeConfigurator() = EurekaStoreConfigurator
   },

   /**
    * Use a distributed mesh of schemas, and execute queries locally.
    * Enterprise only.
    */
   DISTRIBUTED {
      override fun storeConfigurator() = HazelcastStoreConfigurator
   },

   RSOCKET {
      override fun storeConfigurator() = DisabledStoreConfigurator
   },

   HTTP {
      override fun storeConfigurator() = DisabledStoreConfigurator
   };
}

/**
 * A service which publishes Vyne Schemas to another component.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(
   VynePublisherRegistrar::class,
   VyneSchemaStoreConfigRegistrar::class
)
annotation class VyneSchemaPublisher(
   val basePackageClasses: Array<KClass<out Any>> = [],
   @Deprecated("use projectPath")
   val schemaFile: String = "",
   val projectPath: String = "",
   val sourcesLoader:KClass<out SchemaSourcesLoader> = FileSystemSourcesLoader::class,
   val projects:Array<VyneSchemaProject> = []
)

annotation class VyneSchemaProject(
   val projectPath: String = "",
   val sourcesLoader:KClass<out SchemaSourcesLoader> = FileSystemSourcesLoader::class
)


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(EnableVyneConfiguration::class)
annotation class VyneQueryServer

