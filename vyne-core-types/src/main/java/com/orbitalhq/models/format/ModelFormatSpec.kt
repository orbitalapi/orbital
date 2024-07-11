package com.orbitalhq.models.format

import com.orbitalhq.models.DataSource
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

   // REturns a MIME Type / Content type (eg: application/json).
   // Consider using Guava MediaType in implementations
   val mediaType: String
}

interface ModelFormatSerializer {
   fun write(result: TypedInstance, metadata: Metadata, schema: Schema, index: Int):Any?

   /**
    * Writes the raw value. Typically used when exporting query history results,
    * where the raw values have been persisted.
    *
    * Not supported by all clients, as we no longer have access to
    * schema or type data.
    *
    */
   fun write(rawValue: Any?, metadata: Metadata, index: Int):Any?
   fun writeAsBytes(result: TypedInstance, metadata: Metadata, schema: Schema, index: Int):ByteArray
}

interface ModelFormatDeserializer {
   /**
    * Indicates if this spec can parse the content of the value into something else.
    * Generally returns a Map<> or List<Map<>> which the TypedObjectFactory will consume later.
    *
    * Deserializers may be called on content that has already been deseriazlied.
    * Therefore, canParse() may return false for a correctly configured deserializer,
    * indicating "no more parsing is required".
    */
   fun canParse(value: Any, metadata: Metadata): Boolean

   /**
    * Should return either List<Map<String,Any>> or Map<String,Any>, or a TypedInstance
    */
   fun parse(value: Any, type: Type, metadata: Metadata, schema: Schema, source: DataSource): Any

}
