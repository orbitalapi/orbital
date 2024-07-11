package com.orbitalhq.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonView
import com.orbitalhq.models.format.FormatDetector
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.models.format.StreamingModelFormatDeserializer
import com.orbitalhq.models.functions.FunctionRegistry
import com.orbitalhq.models.json.isJson
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.ImmutableEquality
import com.orbitalhq.utils.log
import lang.taxi.accessors.NullValue
import lang.taxi.types.ArrayType
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.MapType
import lang.taxi.types.ObjectType
import lang.taxi.types.isMapType
import mu.KotlinLogging
import reactor.core.publisher.Flux

interface TypedInstance {
   @get:JsonIgnore
   val type: Type
   val value: Any?

   @get:JsonView(DataSourceIncludedView::class)
   val source: DataSource

   val nodeId: String

   /**
    * Hash code which includes the datasource - normally excluded.
    * Used in determining rowIds for TypedInstances when persisting to the UI.
    */
   val hashCodeWithDataSource: Int
      get() {
         return ImmutableEquality(this, TypedInstance::typeName, TypedInstance::value, TypedInstance::source).hash()
      }

   val typeName: String
      get() {
         return type.name.parameterizedName
      }

   val metadata: Map<String, Any>

   // It's up to instances of this to reconstruct themselves with their type
   // set to the value of the typeAlias.
   fun withTypeAlias(typeAlias: Type): TypedInstance

   fun toRawObject(): Any? {
      return TypedInstanceConverter(RawObjectMapper).convert(this)
//      return convert { it.value }
   }

   fun toTypeNamedInstance(): Any? {
      return TypedInstanceConverter(TypeNamedInstanceMapper).convert(this)
   }

   fun valueEquals(valueToCompare: TypedInstance): Boolean

   companion object {

      private val logger = KotlinLogging.logger {}

      const val EXPIRY_METADATA = "expiredAt"
      fun fromNamedType(
         typeNamedInstance: TypeNamedInstance,
         schema: Schema,
         performTypeConversions: Boolean = true,
         source: DataSource
      ): TypedInstance {
         val (typeName, value) = typeNamedInstance
         val type = schema.type(typeName)
         return when {
            value == null -> TypedNull.create(type)
            value is Collection<*> -> {
               val collectionMemberType = getCollectionType(type)
               val members = value.map { member ->
                  if (member == null) {
                     TypedNull.create(collectionMemberType)
                  } else {
                     fromNamedType(member as TypeNamedInstance, schema, performTypeConversions, source)
                  }
               }
               TypedCollection(collectionMemberType, members)
            }

            type.isEnum -> type.enumTypedInstance(value, source)
            type.isScalar -> TypedValue.from(type, value, performTypeConversions, source)
            else -> createTypedObject(typeNamedInstance, schema, performTypeConversions, source)
         }
      }

      private fun createTypedObject(
         typeNamedInstance: TypeNamedInstance,
         schema: Schema,
         performTypeConversions: Boolean,
         source: DataSource
      ): TypedObject {
         val type = schema.type(typeNamedInstance.typeName)
         val attributes = typeNamedInstance.value!! as Map<String, Any>
         val typedAttributes = attributes.map { (attributeName, typedInstance) ->
            when (typedInstance) {
               is TypeNamedInstance -> attributeName to fromNamedType(
                  typedInstance,
                  schema,
                  performTypeConversions,
                  source
               )

               is Collection<*> -> {
                  val collectionTypeRef = type.attributes[attributeName]?.type
                     ?: error("Cannot look up collection type for attribute $attributeName as it is not a defined attribute on type ${type.name}")
                  val collectionType = schema.type(collectionTypeRef)
                  attributeName to TypedCollection(
                     collectionType,
                     typedInstance.map {
                        fromNamedType(
                           it as TypeNamedInstance,
                           schema,
                           performTypeConversions,
                           source
                        )
                     })
               }

               else -> error("Unhandled scenario creating typedObject from TypeNamedInstance -> ${typedInstance::class.simpleName}")
            }
         }.toMap()
         return TypedObject(type, typedAttributes, source)
      }

      fun from(taxiTypedValue: lang.taxi.types.TypedValue, schema: Schema, source: DataSource): TypedInstance {
         val type = schema.type(taxiTypedValue.type)
         return from(type, taxiTypedValue.value, schema, source = source)
      }

      /**
       * Creates a Flux<TypedInstance> from the provided value, by
       * looking for a deserializer capable of converting value to a Flux<>.
       *
       * If none exists, will fall back to creating a standard TypedInstance from value.
       * Should that produce a TypedCollection, it's unwrapped to a Flux<TypedInstance>
       * so the stream semantics are preserved
       */
      fun forStream(
         type: Type,
         value: Any,
         schema: Schema,
         performTypeConversions: Boolean = true,
         nullValues: Set<String> = emptySet(),
         source: DataSource = UndefinedSource,
         evaluateAccessors: Boolean = true,
         functionRegistry: FunctionRegistry = FunctionRegistry.default,
         formatRegistry: FormatRegistry,
         inPlaceQueryEngine: InPlaceQueryEngine? = null,
         parsingErrorBehaviour: ParsingFailureBehaviour = ParsingFailureBehaviour.ThrowException,
         format: FormatsAndZoneOffset? = type.formatAndZoneOffset,
         metadata: Map<String, Any> = emptyMap(),
         valueSuppliers: List<ValueSupplier> = emptyList()
      ): Flux<TypedInstance> {
         val deserializer = formatRegistry.forType(type).let { (metadata,formatSpec) ->
            if (formatSpec?.deserializer is StreamingModelFormatDeserializer) {
               if ((formatSpec.deserializer as StreamingModelFormatDeserializer).supportsStreamingForSource(value)) {
                  formatSpec.deserializer as StreamingModelFormatDeserializer
               } else {
                  logger.warn { "Attempting to convert a value of ${value::class.simpleName} to a stream of ${type.name.longDisplayName}, but the registered deserializer does not support streaming based on an input type of ${value::class.simpleName}. Will attempt to parse this as a collection instead." }
                  null
               }
            } else {
               logger.warn { "Attempting to convert a value of ${value::class.simpleName} to a stream of ${type.name.longDisplayName}, but there are no deserializers registered against that type. Consider adding an annotation defining the format (such as @Csv). Will attempt to parse this as a collection instead." }
               null
            }
         }

         return if (deserializer == null) {
            val result = from(
               type, value, schema, performTypeConversions, nullValues, source, evaluateAccessors, functionRegistry
            )
            when (result) {
               is TypedCollection -> Flux.fromIterable(result.value)
               else -> Flux.just(result)
            }
         } else {
            deserializer.stream(
               value,
               type,
               schema,
               source,
               functionRegistry,
               formatRegistry,
               inPlaceQueryEngine,
               parsingErrorBehaviour,
               format,
               metadata,
               valueSuppliers
            )
         }
      }

      /**
       * Parses a TypedInstance
       *
       * @param evaluateAccessors Determines if accessors defined in the schema should be evaluated.  Normally
       * this should be true.  However, for content served from a cask, the content is already preparsed, and so
       * does not need accessors to be evaluated.
       */
      fun from(
         type: Type,
         value: Any?,
         schema: Schema,
         performTypeConversions: Boolean = true,
         nullValues: Set<String> = emptySet(),
         source: DataSource = UndefinedSource,
         evaluateAccessors: Boolean = true,
         functionRegistry: FunctionRegistry = FunctionRegistry.default,
         formatSpecs: List<ModelFormatSpec> = emptyList(),
         inPlaceQueryEngine: InPlaceQueryEngine? = null,
         parsingErrorBehaviour: ParsingFailureBehaviour = ParsingFailureBehaviour.ThrowException,
         format: FormatsAndZoneOffset? = type.formatAndZoneOffset,
         metadata: Map<String, Any> = emptyMap(),
         valueSuppliers: List<ValueSupplier> = emptyList()
      ): TypedInstance {

         // Just here to DRY out the passing of params
         fun buildUsingObjectFactory(): TypedInstance {
            return TypedObject.fromValue(
               type,
               value!!,
               schema,
               nullValues,
               source = source,
               evaluateAccessors = evaluateAccessors,
               functionRegistry = functionRegistry,
               inPlaceQueryEngine = inPlaceQueryEngine,
               formatSpecs = formatSpecs,
               parsingErrorBehaviour = parsingErrorBehaviour,
               metadata = metadata,
               valueSuppliers = valueSuppliers
            )
         }
         return when {
            value is TypedInstance && value.type.taxiType.isAssignableTo(type.taxiType) -> value
            value == null -> TypedNull.create(type)
            value is NullValue -> TypedNull.create(type)
            value is java.sql.Array -> {
               val list = (value.array as Array<Any>).toList()
               from(
                  type,
                  list,
                  schema,
                  performTypeConversions,
                  nullValues,
                  source,
                  evaluateAccessors,
                  functionRegistry,
                  formatSpecs,
                  inPlaceQueryEngine,
                  parsingErrorBehaviour,
                  format,
                  metadata,
                  valueSuppliers
               )
            }

            value is Array<*> -> {
               val list = (value as Array<Any>).toList()
               from(
                  type,
                  list,
                  schema,
                  performTypeConversions,
                  nullValues,
                  source,
                  evaluateAccessors,
                  functionRegistry,
                  formatSpecs,
                  inPlaceQueryEngine,
                  parsingErrorBehaviour,
                  format,
                  metadata,
                  valueSuppliers
               )
            }

            value is Collection<*> -> {
               if (!type.isCollection) {
                  val errorMessage =
                     "Provided value is a collection, but the declared type ${type.name.parameterizedName} is not"
                  log().warn(errorMessage)
                  return TypedNull.create(type, source = FailedParsingSource(value, errorMessage))
               }
               val collectionMemberType = getCollectionType(type)
               val collectionValue = TypedCollection.arrayOf(
                  collectionMemberType,
                  value.filterNotNull().map {
                     from(
                        collectionMemberType,
                        it,
                        schema,
                        performTypeConversions,
                        source = source,
                        evaluateAccessors = evaluateAccessors,
                        inPlaceQueryEngine = inPlaceQueryEngine,
                        formatSpecs = formatSpecs,
                        functionRegistry = functionRegistry,
                        parsingErrorBehaviour = parsingErrorBehaviour,
                        format = format,
                        metadata = metadata,
                        valueSuppliers = valueSuppliers
                     )
                  },
                  source
               )
               collectionValue
            }

            type.isEnum -> {
               type.enumTypedInstance(value, source)
            }

            type.taxiType is ObjectType && type.taxiType.isMapType() -> {
               TypedMaps.parse(
                  type,
                  value,
                  schema,
                  performTypeConversions,
                  nullValues,
                  source,
                  evaluateAccessors,
                  functionRegistry,
                  formatSpecs
               )
            }

            type.taxiType is MapType -> {
               TypedMaps.parse(
                  type,
                  value,
                  schema,
                  performTypeConversions,
                  nullValues,
                  source,
                  evaluateAccessors,
                  functionRegistry,
                  formatSpecs
               )
            }

            type.isScalar -> {
               TypedValue.from(type, value, performTypeConversions, source, parsingErrorBehaviour, format)
            }

            FormatDetector.get(formatSpecs).getFormatType(type) != null -> {
               buildUsingObjectFactory()
            }
            // This is here primarily for readability.  We could just let this fall through to below.
            isJson(value) -> TypedObjectFactory(
               type,
               value,
               schema,
               nullValues,
               source,
               evaluateAccessors = evaluateAccessors,
               functionRegistry = functionRegistry,
               inPlaceQueryEngine = inPlaceQueryEngine,
               formatSpecs = formatSpecs,
               parsingErrorBehaviour = parsingErrorBehaviour,
               metadata = metadata,
               valueSuppliers = valueSuppliers
            ).build()

            else -> buildUsingObjectFactory()
         }
      }

      private fun getCollectionType(type: Type): Type {
         return if (type.isCollection) {
            type.collectionType ?: error("Type should return a collection when isCollection is true.")
         } else {
            log().warn("Collection type could not be determined - expected to find ${ArrayType.qualifiedName}, but found ${type.fullyQualifiedName}")
            type
         }
      }

      /**
       * Indciates if the instance, or it's value, is null or a TypedNull
       */
      fun isNull(instance: TypedInstance?): Boolean {
         return instance?.value == null || instance.value is TypedNull
      }
   }
}

fun TypedInstance.containsMetadata(name: String): Boolean {
   return this.metadata.containsKey(name)
}


