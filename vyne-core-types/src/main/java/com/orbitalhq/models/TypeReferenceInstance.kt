package com.orbitalhq.models

import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.Ids
import java.util.concurrent.ConcurrentHashMap

/**
 * A reference to a type, as used in an expression.
 * Allows for reflective expressions that refer to types, rather than values.
 *
 * WARNING: This value is interned, so the value of type may not be the current
 * type as defined in the schema, if the schema has changed since the TypeReferenceInstance
 * was instantiated.
 *
 * ALWAYS call resolveType(schema) to get the current version of the type from the schema
 */
data class TypeReferenceInstance private constructor(override val type: Type) : TypedInstance {
   fun resolveType(schema:Schema):Type {
      return schema.type(this.type.paramaterizedName)
   }

   companion object {
      private val instances = ConcurrentHashMap<String, TypeReferenceInstance>()
      fun from(type: Type): TypeReferenceInstance {
         // Use typecache hashcode to try to prevent stale lookups
         return instances.getOrPut(type.qualifiedName.parameterizedName + type.typeCache.hashCode()) { TypeReferenceInstance(type) }
      }
   }

   override val value: Any = type
   override val source: DataSource = DefinedInSchema
   override val nodeId: String = Ids.fastUuid()
   override val metadata: Map<String, Any> = emptyMap()

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      TODO("Not yet implemented")
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      TODO("Not yet implemented")
   }
}
