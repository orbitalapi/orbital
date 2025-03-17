package com.orbitalhq.models.format

import arrow.core.Either
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.InPlaceQueryEngine
import com.orbitalhq.models.ParsingFailureBehaviour
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.ValueSupplier
import com.orbitalhq.models.functions.FunctionRegistry
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.types.FormatsAndZoneOffset
import reactor.core.publisher.Flux

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

   /// Probably this is the method you want.
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
   fun canParse(value: Any, metadata: Metadata, type: Type): Boolean

   /**
    * Should return either List<Map<String,Any>> or Map<String,Any>, or a TypedInstance
    */
   fun parse(value: Any, type: Type, metadata: Metadata, schema: Schema, source: DataSource): Any
}

/**
 * A deserializer that can produce a stream (Flux) of
 * TypedInstances from a single input.
 *
 * This is a different approach from a producer who emits a stream (such as a
 * Kafka topic), where each message is known to produce a single item.
 *
 * Instead, this approach is intended for where there is a single value (such
 * as a ByteArray, or InputStream), which may itself return 0..n items. In these
 * situations, it's not possible to generically know the boundary between items within the stream,
 * so we defer to the deserializer to handle it.
 */
interface StreamingModelFormatDeserializer {
   /**
    * Indicates if this deserializer supports streaming based on the provided input.
    *
    */
   fun supportsStreamingForSource(value: Any): Boolean
   fun stream(
      value: Any,
      type: Type,
      schema: Schema,
      source: DataSource,
      functionRegistry: FunctionRegistry = FunctionRegistry.default,
      formatRegistry: FormatRegistry,
      inPlaceQueryEngine: InPlaceQueryEngine? = null,
      parsingErrorBehaviour: ParsingFailureBehaviour = ParsingFailureBehaviour.ThrowException,
      format: FormatsAndZoneOffset? = type.formatAndZoneOffset,
      metadata: Map<String, Any> = emptyMap(),
      valueSuppliers:List<ValueSupplier> = emptyList()
   ): Flux<Either<StreamErrorMessage, TypedInstance>>
}
