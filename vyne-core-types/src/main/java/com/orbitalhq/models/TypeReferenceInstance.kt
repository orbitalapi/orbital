package com.orbitalhq.models

import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.Ids
import java.util.concurrent.ConcurrentHashMap

data class TypeReferenceInstance private constructor(override val type: Type) : TypedInstance {

   companion object {
      private val instances = ConcurrentHashMap<QualifiedName, TypeReferenceInstance>()
      fun from(type: Type): TypeReferenceInstance {
         return instances.getOrPut(type.qualifiedName) { TypeReferenceInstance(type) }
      }
   }

   override val value: Any = type
   override val source: DataSource = DefinedInSchema
   override val nodeId: String = Ids.fastUuid()

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      TODO("Not yet implemented")
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      TODO("Not yet implemented")
   }
}
