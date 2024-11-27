package com.orbitalhq.query.caching

import com.orbitalhq.VyneTypes
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.fqn
import lang.taxi.types.SumType
import reactor.core.publisher.Mono

object StateStoreAnnotation {
   val StateStoreTypeName = "${VyneTypes.NAMESPACE}.state.StateStore".fqn()
   val StateStoreTaxi = """
namespace ${StateStoreTypeName.namespace} {

   annotation StateStore {
      connection : String?
      [[ A name to use for the state store. Optional, one will be assigned if not provided ]]
      name: String?
      [[ If an entry is not accessed (read or write) for this duration, it is evicted from the state store ]]
      maxIdleSeconds: Int = 180
   }
}
   """.trimIndent()
}

data class StateStoreConfig(
   val connection: String? = null,
   val name: String? = null,
   val maxIdleSeconds: Int
) {
   companion object {
      val DEFAULT = StateStoreConfig(null, null, 180)
   }
}
/**
 * Returns a Cache Store for the provided key - if one is available, otherwise
 * returns null.
 */
interface StateStoreProvider {
   // Note: Be sure to pass a schema from the query context (not from the
   // schema store), as we want to ensure inline types declared by the query are available.
   fun getStateStore(stateStoreConfig: StateStoreConfig, sumType: SumType, schema: Schema,emitMode: EmitMode, namePrefix: String = "StateStore_"):StateStore?

   fun getStateStoreKey(prefix: String, sumType: SumType, emitMode: EmitMode): String {
      return "${prefix}_${sumType.qualifiedName}_($emitMode)"
   }

   enum class EmitMode {
      /**
       * Emit whenever a message arrives
       */
      ON_ANY_EVENT,

      /**
       * Emit only after all sources have provided a single message
       */
      AFTER_ALL_EVENTS
   }
}

/**
 * Intended to provide interim state when doing things like merging multiple streams.
 *
 */
interface StateStore {
   /**
    * Updates the stored instance, copying the non-null values from
    * the provided instance onto the stored instance, and returns the result.
    *
    * - @Id annotated fields are used for matching
    * - If the @Id annotated field is null, then the original instance is returned unmodified
    * q
    */
   fun mergeNotNullValues(typedInstance: TypedInstance): Mono<TypedInstance>
}


