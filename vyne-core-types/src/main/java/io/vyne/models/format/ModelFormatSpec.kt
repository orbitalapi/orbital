package io.vyne.models.format

import io.vyne.models.TypeNamedInstance
import io.vyne.models.TypedInstance
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Metadata
import io.vyne.schemas.QualifiedName

/**
 * This interface is a WIP.
 * The goal is to remove the model sepcific parsing out of TypedObjectFactory
 * and into seperate implementations of this spec, supproting both
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
}

interface ModelFormatDeserializer {
   /**
    * Indicates if this spec can parse the content of the value into something else.
    * Generally returns a Map<> or List<Map<>> which the TypedObjectFactory will consume later
    */
   fun parseRequired(value: Any, metadata: Metadata): Boolean

   fun parse(value: Any, metadata: Metadata): Any

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
