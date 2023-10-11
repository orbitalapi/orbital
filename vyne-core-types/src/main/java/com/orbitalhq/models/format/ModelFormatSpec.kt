package com.orbitalhq.models.format

import com.orbitalhq.models.TypeNamedInstance
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.AttributeName
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type

/**
 * This interface is a WIP.
 * The goal is to remove the model sepcific parsing out of TypedObjectFactory
 * and into separate implementations of this spec, supproting both
 * serialization and deserialzation.
 *
 * However, TypedObjectFactory interweaves many different purposes,
 * including function and expression evaluation.
 * We need to carefully untagle these so that evaluation is not duplicated in all the
 * deserializers.
 *
 * That has not yet been implemented.
 *
 */
interface ModelFormatSpec {
   val serializer: ModelFormatSerializer
   val deserializer: ModelFormatDeserializer
   val annotations: List<QualifiedName>
}

interface ModelFormatSerializer {
   fun write(result: TypedInstance, metadata: Metadata, typedInstanceInfo: TypedInstanceInfo = EmptyTypedInstanceInfo):Any?
   fun write(result: TypeNamedInstance, attributes: Set<AttributeName>, metadata: Metadata, typedInstanceInfo: TypedInstanceInfo = EmptyTypedInstanceInfo):Any?
   fun write(result: TypeNamedInstance, type: Type, metadata: Metadata, typedInstanceInfo: TypedInstanceInfo = EmptyTypedInstanceInfo):Any?
}

interface ModelFormatDeserializer {
   /**
    * Indicates if this spec can parse the content of the value into something else.
    * Generally returns a Map<> or List<Map<>> which the TypedObjectFactory will consume later
    */
   fun parseRequired(value: Any, metadata: Metadata): Boolean

   /**
    * Should return either List<Map<String,Any>> or Map<String,Any>
    */
   fun parse(value: Any, type: Type, metadata: Metadata, schema: Schema): Any

}

interface TypedInstanceInfo {
   val index: Int
}

object EmptyTypedInstanceInfo: TypedInstanceInfo {
   override val index: Int
      get() = -1
}

object FirstTypedInstanceInfo: TypedInstanceInfo {
   override val index: Int
      get() = 0
}
