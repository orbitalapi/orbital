package com.orbitalhq.queryService

import com.orbitalhq.VersionedSource
import com.orbitalhq.cockpit.core.schemas.BuiltInTypesProvider
import com.orbitalhq.from
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import org.reactivestreams.Publisher
import reactor.core.CorePublisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

object TestSchemaProvider {
   fun withBuiltInsAnd(other: TaxiSchema): SchemaProvider {
      return withBuiltInsAnd(other.sources)
   }

   fun withBuiltInsAnd(sources: List<VersionedSource>): SchemaProvider {
      return SimpleSchemaProvider(
         TaxiSchema.from(
            BuiltInTypesProvider.sourcePackage.sources + sources
         )
      )
   }
}


fun <T> Publisher<T>.toList(): List<T> {
   return when (this) {
      is Mono<*> -> this.block() as List<T>
      is Flux<*> -> this.collectList().block() as List<T>
      else -> error("This shouldn't happen")
   }
}

fun <T> Publisher<T>.single(): T {
   return when (this) {
      is Mono<*> -> this.block() as T
      is Flux<*> -> this.single().block()!! as T
      else -> error("This shouldn't happen")
   }
}
