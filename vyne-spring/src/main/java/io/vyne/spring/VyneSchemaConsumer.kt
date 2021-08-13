package io.vyne.spring

import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(
   VyneSchemaConsumerConfigRegistrar::class,
   VyneSchemaStoreConfigRegistrar::class
)
annotation class VyneSchemaConsumer(
   val publicationMethod: SchemaPublicationMethod = SchemaPublicationMethod.REMOTE
)


