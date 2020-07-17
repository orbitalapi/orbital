package io.vyne.spring

import org.springframework.context.annotation.Import
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
    * Use a distributed mesh of schemas, and execute queries locally.
    * Enterprise only.
    */
   DISTRIBUTED
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(VyneConfigRegistrar::class)
annotation class VyneSchemaPublisher(val basePackageClasses: Array<KClass<out Any>> = [],
                                     val publicationMethod: SchemaPublicationMethod = SchemaPublicationMethod.REMOTE,
                                     val schemaFile: String = ""
)
