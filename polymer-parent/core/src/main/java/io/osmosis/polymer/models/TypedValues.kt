package io.osmosis.polymer.models

import io.osmosis.polymer.schemas.Type

interface TypedInstance {
   val type: Type
   val value: Any
}

data class TypedObject(override val type: Type, override val value: Map<String, TypedInstance>) : TypedInstance, Map<String, TypedInstance> by value
data class TypedValue(override val type: Type, override val value: Any) : TypedInstance
data class TypedCollection(override val type: Type, override val value: List<TypedInstance>) : List<TypedInstance> by value, TypedInstance
