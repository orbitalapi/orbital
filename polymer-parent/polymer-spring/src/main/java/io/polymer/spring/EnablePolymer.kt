package io.polymer.spring

import org.springframework.context.annotation.Import
import kotlin.reflect.KClass

enum class RemoteSchemaStoreType {
   HTTP,
   HAZELCAST,
   NONE
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(PolymerConfigRegistrar::class)
annotation class EnablePolymer(val basePackageClasses: Array<KClass<out Any>> = emptyArray(),
                               val remoteSchemaStore: RemoteSchemaStoreType = RemoteSchemaStoreType.NONE
)
