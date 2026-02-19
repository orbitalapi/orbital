package com.orbitalhq.models

import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.models.functions.FunctionRegistry
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.schemas.AttributeName
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import com.orbitalhq.utils.Ids
import lang.taxi.services.operations.constraints.PropertyFieldNameIdentifier
import lang.taxi.services.operations.constraints.PropertyIdentifier
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.types.Arrays
import lang.taxi.types.AttributePath
import mu.KotlinLogging


data class TypedObject(
   override val type: Type,
   private val suppliedValue: Map<String, TypedInstance>,
   override val source: DataSource,
   override val metadata: Map<String, Any> = emptyMap()
) : TypedInstance, Map<String, TypedInstance> {

   // Note: I tried using Eclipse Collections for this backing map to optimize read time.
   // However after experimenting with both UnifiedMap and ImmutableUnifiedMap
   // some tests started failing with properties being emitted in the wrong order.
   // This is probably solvable, but after profiling, the performance benefits weren't
   // signficant, so have reverted to a plain old LinkedHashMap.
   private val combinedValues: Map<String, TypedInstance> = type.defaultValues?.plus(suppliedValue) ?: suppliedValue

   override val nodeId: String = Ids.fastUuid()

   override val value: Map<String, TypedInstance>
      get() = combinedValues

   init {
//      require(!Arrays.isArray(type.taxiType)) {
//         "It is illegal to construct a TypedObject with an array type (found ${type.qualifiedName.shortDisplayName})"
//      }
   }

   companion object {
      private val logger = KotlinLogging.logger {}
      fun fromValue(typeName: String, value: Any, schema: Schema, source: DataSource): TypedInstance {
         return fromValue(schema.type(typeName), value, schema, source = source)
      }

      fun fromAttributes(
         typeName: String,
         attributes: Map<String, Any>,
         schema: Schema,
         performTypeConversions: Boolean = true,
         source: DataSource
      ): TypedObject {
         return fromAttributes(schema.type(typeName), attributes, schema, performTypeConversions, source)
      }

      fun fromAttributes(
         type: Type,
         attributes: Map<String, Any>,
         schema: Schema,
         performTypeConversions: Boolean = true,
         source: DataSource,
         metadata: Map<String, Any> = emptyMap()
      ): TypedObject {
         val typedAttributes: Map<String, TypedInstance> = attributes
            .filterKeys { type.hasAttribute(it) }
            .map { (attributeName, value) ->
               val attributeType = type.attributes.getValue(attributeName).resolveType(schema)
               attributeName to TypedInstance.from(
                  attributeType,
                  value,
                  schema,
                  performTypeConversions,
                  source = source
               )
            }.toMap()
         return TypedObject(type, typedAttributes, source, metadata)
      }

      fun fromValue(
         type: Type,
         value: Any,
         schema: Schema,
         source: DataSource,
         evaluateAccessors: Boolean = true,
         functionRegistry: FunctionRegistry = FunctionRegistry.default,
         inPlaceQueryEngine: InPlaceQueryEngine? = null,
         formatSpecs: List<ModelFormatSpec> = emptyList(),
         parsingErrorBehaviour: ParsingFailureBehaviour = ParsingFailureBehaviour.ThrowException,
         functionResultCache: MutableMap<FunctionResultCacheKey, Any> = mutableMapOf(),
         metadata: Map<String, Any> = emptyMap(),
         valueSuppliers: List<ValueSupplier> = emptyList(),
         parsingOptions: ParsingOptions = ParsingOptions.DEFAULT
      ): TypedInstance {
         return TypedObjectFactory(
            type,
            value,
            schema,
            source,
            evaluateAccessors = evaluateAccessors,
            functionRegistry = functionRegistry,
            inPlaceQueryEngine = inPlaceQueryEngine,
            formatSpecs = formatSpecs,
            parsingErrorBehaviour = parsingErrorBehaviour,
            functionResultCache = functionResultCache,
            metadata = metadata,
            valueSuppliers = valueSuppliers,
            parsingOptions = parsingOptions
         ).build()
      }
   }

   override fun toString(): String {
      return "TypedObject(type=${type.qualifiedName.longDisplayName}, value=$suppliedValue)"
   }

   private val cachedHashCode: Int = run {
      var result = type.hashCode()
      result = 31 * result + value.hashCode()
      result
   }

   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is TypedObject) return false

      if (other.cachedHashCode != this.cachedHashCode) return false
      if (this.type.qualifiedName != other.type.qualifiedName) return false
      return this.combinedValues == other.combinedValues
//      return this.stringifiedValueValueMap == other.stringifiedValueValueMap
   }

   override fun hashCode(): Int {
      return cachedHashCode
   }


   fun hasAttribute(name: String): Boolean {
      return this.combinedValues.containsKey(name)
   }

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedObject(typeAlias, combinedValues, source, metadata)
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      if (valueToCompare !is TypedObject) {
         return false
      }
      if (!this.type.resolvesSameAs(valueToCompare.type)) {
         return false
      }
      return this.combinedValues.all { (attributeName, value) ->
         valueToCompare.hasAttribute(attributeName) && valueToCompare.get(attributeName).valueEquals(value)
      }
   }

   /**
    * Returns the attribute value identified by the propertyIdentifer
    */
   fun getAttribute(propertyIdentifier: PropertyIdentifier, schema: Schema): TypedInstance {
      return when (propertyIdentifier) {
         is PropertyFieldNameIdentifier -> get(propertyIdentifier.name)
         is PropertyTypeIdentifier -> getAttributeIdentifiedByType(
            propertyIdentifier.type.toVyneQualifiedName(),
            schema
         )
      }
   }

   private fun getAttributeIdentifiedByType(typeName: QualifiedName, schema: Schema): TypedInstance {
      return getAttributeIdentifiedByType(schema.type(typeName))
   }

   fun getAttributeIdentifiedByType(type: Type, returnNull: Boolean = false): TypedInstance {
      val candidates = this.value.filter { (_, value) ->
         value.type.isAssignableTo(type)
      }
      return when {
         candidates.isEmpty() && returnNull -> TypedNull.create(
            type,
            source = ValueLookupReturnedNull(
               "Lookup for type ${type.name.parameterizedName} returned null",
               requestedTypeName = type.name
            )
         ) // sometimes i want to allow null values
         candidates.isEmpty() -> error("No properties on type ${this.type.name.parameterizedName} have type ${type.name.parameterizedName}")
         candidates.size > 1 -> TypedInstanceCandidateFilter.resolve(candidates.values, type)
         else -> candidates.values.first()
      }
   }

   /**
    * Accepts a path of properties (eg a.b.c) and traverses it.
    * If you know that the path you're providing is a single field on this object,
    * it's more performant to call getDirectProperty()
    */
   operator fun get(path: AttributePath): TypedInstance {
      return get(path.path)
   }

   /**
    * Reads a property from the provided map.
    * Does not support path navigation (eg., a.b.c), but
    * is more performant than get()
    */
   fun getDirectAttribute(key: String): TypedInstance {
      return this.value[key]
         ?: error("No attribute named $key found on this type (${type.name})")
   }

   // TODO : Needs a test
   override operator fun get(key: String): TypedInstance {
      val parts = key.split(".").toMutableList()
      val thisFieldName = parts.removeAt(0)
      val attributeValue = getDirectAttribute(thisFieldName)

      return if (parts.isEmpty()) {
         attributeValue
      } else {
         val remainingAccessor = parts.joinToString(".")
         when (attributeValue) {
            is TypedObject -> attributeValue[remainingAccessor]
            is TypedCollection -> attributeValue[remainingAccessor]
            else -> throw IllegalArgumentException("Cannot evaluate an accessor ($remainingAccessor) as value is not an object with fields (${attributeValue.type.name})")
         }
      }
   }

   /**
    * Walks the path of properties (foo.bar.baz), collecting all
    * typed instances along the property path.
    *
    * If one of the properties is a collection, the collection is iterated,
    * and the remainder of the path is evaluated against all collection members.
    */
   fun getAllAtPath(path: String): List<TypedInstance> {
      val parts = path.split(".").toMutableList()
      val thisFieldName = parts.removeAt(0)
      val attributeValue = this.value[thisFieldName]
         ?: if (this.type.hasAttribute(thisFieldName)) {
            // Should we create a typedNull here?
            // Current tests expect an empty list, but I think a TypedNull is more appropriate
            return emptyList()
         } else {
            error("No attribute named $thisFieldName found on this type (${type.name})")
         }

      return if (parts.isEmpty()) {
         listOf(attributeValue)
      } else {
         val remainingAccessor = parts.joinToString(".")
         when (attributeValue) {
            is TypedObject -> attributeValue.getAllAtPath(remainingAccessor)
            is TypedCollection -> attributeValue.flatMap { member ->
               when (member) {
                  is TypedObject -> member.getAllAtPath(remainingAccessor)
                  else -> error("Unhandled branch in navigating a collection - got a member type of ${member::class.simpleName}")
               }
            }

            is TypedNull -> {
               logger.debug { "Reading path of $path against instance of ${this.type.qualifiedName.shortDisplayName} found null value $thisFieldName.  Returning the null value" }
               listOf(attributeValue)
            }

            else -> throw IllegalArgumentException("Cannot evaluate an accessor ($remainingAccessor) as value is not an object with fields (${attributeValue.type.name})")
         }
      }
   }

   fun copy(replacingArgs: Map<AttributeName, TypedInstance>): TypedObject {
      return TypedObject(this.type, this.value + replacingArgs, source, metadata)
   }

   override val entries: Set<Map.Entry<String, TypedInstance>>
      get() = combinedValues.entries
   override val keys: Set<String>
      get() = combinedValues.keys
   override val size: Int
      get() = combinedValues.size
   override val values: Collection<TypedInstance>
      get() = combinedValues.values

   override fun containsKey(key: String): Boolean {
      return combinedValues.containsKey(key)
   }

   override fun containsValue(value: TypedInstance): Boolean {
      return combinedValues.containsValue(value)
   }

   override fun isEmpty(): Boolean {
      return combinedValues.isEmpty()
   }
}
