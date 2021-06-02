package io.vyne.models

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.conditional.ConditionalFieldSetEvaluator
import io.vyne.models.functions.FunctionRegistry
import io.vyne.models.json.Jackson
import io.vyne.models.json.JsonParsedStructure
import io.vyne.models.json.isJson
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Field
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.isNonScalarObjectType
import io.vyne.utils.log
import lang.taxi.types.Accessor
import lang.taxi.types.ColumnAccessor
import org.apache.commons.csv.CSVRecord

/**
 * Constructs a TypedObject
 *
 * @param evaluateAccessors Determines if accessors defined in the schema should be evaluated.  Normally
 * this should be true.  However, for content served from a cask, the content is already preparsed, and so
 * does not need accessors to be evaluated.
 */
class TypedObjectFactory(
   private val type: Type,
   private val value: Any,
   internal val schema: Schema,
   val nullValues: Set<String> = emptySet(),
   val source: DataSource,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val functionRegistry: FunctionRegistry = FunctionRegistry.default,
   private val evaluateAccessors: Boolean = true
) {
   private val valueReader = ValueReader()
   private val accessorReader: AccessorReader by lazy { AccessorReader(this, this.functionRegistry) }
   private val conditionalFieldSetEvaluator = ConditionalFieldSetEvaluator(this)

   private val attributesToMap by lazy {
      type.attributes.filter { it.value.formula == null }
   }

   private val fieldInitializers : Map<AttributeName,Lazy<TypedInstance>> by lazy {
      attributesToMap.map {(attributeName, field) ->
         attributeName to lazy { buildField(field, attributeName) }
      }.toMap()
   }

   suspend fun buildAsync( decorator: suspend (attributeMap: Map<AttributeName, TypedInstance>) -> Map<AttributeName, TypedInstance> = { attributesToMap -> attributesToMap}): TypedInstance {
      if (isJson(value)) {
         val jsonParsedStructure = JsonParsedStructure.from(value as String, objectMapper)
         return TypedInstance.from(type,jsonParsedStructure,schema, nullValues = nullValues, source = source, evaluateAccessors = evaluateAccessors)
      }

      // TODO : Naieve first pass.
      // This approach won't work for nested objects.
      // I think i need to build a hierachy of object factories, and allow nested access
      // via the get() method
      val mappedAttributes = attributesToMap.map { (attributeName) ->
         // The value may have already been populated on-demand from a conditional
         // field set evaluation block, prior to the iterator hitting the field
         attributeName to getOrBuild(attributeName)
      }.toMap()

      return TypedObject(type, decorator(mappedAttributes), source)
   }
   fun build( decorator: (attributeMap: Map<AttributeName, TypedInstance>) -> Map<AttributeName, TypedInstance> = { attributesToMap -> attributesToMap}): TypedInstance {
      if (isJson(value)) {
         val jsonParsedStructure = JsonParsedStructure.from(value as String, objectMapper)
         return TypedInstance.from(type,jsonParsedStructure,schema, nullValues = nullValues, source = source, evaluateAccessors = evaluateAccessors)
      }

      // TODO : Naieve first pass.
      // This approach won't work for nested objects.
      // I think i need to build a hierachy of object factories, and allow nested access
      // via the get() method
      val mappedAttributes = attributesToMap.map { (attributeName) ->
         // The value may have already been populated on-demand from a conditional
         // field set evaluation block, prior to the iterator hitting the field
         attributeName to getOrBuild(attributeName)
      }.toMap()

      return TypedObject(type, decorator(mappedAttributes), source)
   }

   private fun getOrBuild(attributeName: AttributeName): TypedInstance {
      // Originally we used a concurrentHashMap.computeIfAbsent { ... } approach here.
      // However, functions on accessors can access other fields, which can cause recursive access.
      // Therefore, migrated to using initializers with kotlin Lazy functions
      val initializer = fieldInitializers[attributeName] ?: error("Cannot request field $attributeName as no initializer has been prepared")
      return initializer.value
   }

   internal fun getValue(attributeName: AttributeName): TypedInstance {
      return getOrBuild(attributeName)
   }

   internal fun readAccessor(type: Type, accessor: Accessor): TypedInstance {
      return accessorReader.read(value, type, accessor, schema, source = source)
   }

   internal fun readAccessor(type: QualifiedName, accessor: Accessor, nullable: Boolean): TypedInstance {
      return accessorReader.read(value, type, accessor, schema, nullValues, source = source, nullable = nullable)
   }


   private fun buildField(field: Field, attributeName: AttributeName): TypedInstance {
      // We don't always want to use accessors.
      // When parsing content from a cask, which has already been processed, what we
      // receive is a TypedObject.  The accessors should be ignored in this scenario.
      // By default, we want to cosndier them.
      val considerAccessor = field.accessor != null && evaluateAccessors


      // Questionable design choice: Favour directly supplied values over accessors and conditions.
      // The idea here is that when we're reading from a file or non parsed source, we need
      // to know how to construct the instance.
      // However, if that work has already been done, and we're trying to rebuild the instance
      // from a parsing result, we need to be able to.
      // Therefore, if we've been directly supplied the value, use it.
      // Otherwise, look to leverage conditions.
      // Note - revisit if this proves to be problematic.
      val fieldType = schema.type(field.type)
      return when {
         // Cheaper readers first
         value is CSVRecord && field.accessor is ColumnAccessor && considerAccessor -> {
            readAccessor(field.type, field.accessor, field.nullable)
         }

         // Handle reading nested objects if parsing from a JsonNode.
         // Parsing from actual json string is handled elsewhere
         value is JsonNode && fieldType.taxiType.isNonScalarObjectType() -> {
            if (value[attributeName] == null) {
               log().info("Attribute $attributeName was null when reading json of type ${type.fullyQualifiedName}")
               TypedNull.create(fieldType, source)
            } else {
               TypedObjectFactory(
                  fieldType,
                  value[attributeName],
                  schema,
                  nullValues,
                  source, objectMapper, functionRegistry, evaluateAccessors
               ).build()
            }

         }

         // ValueReader can be expensive if the value is an object,
         // so only use the valueReader early if the value is a map

         // MP 19-Nov: field.accessor null check had been added here to fix a bug, but I can't remember what it was.
         // However, the impact of adding it is that when parsing TypedObjects from remote calls that have already been
         // processed (and so the accessor isn't required) means that we fall through this check and try using the
         // accessor, which will fail, as this isn't raw content anymore, it's parsed / processed.
         value is Map<*, *> && !considerAccessor && valueReader.contains(value, attributeName) -> readWithValueReader(attributeName, field)
         considerAccessor -> {
            readAccessor(field.type, field.accessor!!, field.nullable)
         }
         field.readCondition != null -> {
            conditionalFieldSetEvaluator.evaluate(field.readCondition, attributeName, fieldType)
         }
         // Not a map, so could be an object, try the value reader - but this is an expensive
         // call, so we defer to last-ish
         valueReader.contains(value, attributeName) -> readWithValueReader(attributeName, field)

         // Is there a default?
         field.defaultValue != null -> TypedValue.from(fieldType, field.defaultValue, ConversionService.DEFAULT_CONVERTER, source = DefinedInSchema)

         else -> {
           // log().debug("The supplied value did not contain an attribute of $attributeName and no accessors or strategies were found to read.  Will return null")
            TypedNull.create(fieldType)
         }
      }

   }

   private fun readWithValueReader(attributeName: AttributeName, field: Field): TypedInstance {
      val attributeValue = valueReader.read(value, attributeName)
      return if (attributeValue == null) {
         TypedNull.create(schema.type(field.type), source)
      } else {
         TypedInstance.from(schema.type(field.type.parameterizedName), attributeValue, schema, true, source = source)
      }
   }

}
