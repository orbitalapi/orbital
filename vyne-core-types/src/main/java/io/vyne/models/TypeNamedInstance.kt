package io.vyne.models

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.VersionedType


@JsonDeserialize(using = TypeNamedInstanceDeserializer::class)
data class TypeNamedInstance(
   val typeName: String,
   val value: Any?,
   val source: DataSource? = null

) {
   constructor(typeName: QualifiedName, value: Any?, source:DataSource? = null) : this(typeName.fullyQualifiedName, value, source)
}

data class VersionedTypedInstance(
   val type: VersionedType,
   val instance: TypedInstance
)
