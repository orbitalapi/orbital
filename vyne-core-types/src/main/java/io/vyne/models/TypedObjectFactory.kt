package io.vyne.models

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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.accessors.Accessor
import lang.taxi.accessors.ColumnAccessor
import lang.taxi.expressions.Expression
import mu.KotlinLogging
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
   private val evaluateAccessors: Boolean = true,
   private val inPlaceQueryEngine: InPlaceQueryEngine? = null,
   private val accessorHandlers:List<AccessorHandler<out Accessor>> = emptyList()
) : EvaluationValueSupplier {
   private val logger = KotlinLogging.logger {}

   private val valueReader = ValueReader()
   private val accessorReader: AccessorReader by lazy { AccessorReader(this, this.functionRegistry, this.schema, this.accessorHandlers) }
   private val conditionalFieldSetEvaluator = ConditionalFieldSetEvaluator(this, this.schema, accessorReader)

   private val attributesToMap = type.attributes /*by lazy {
      type.attributes.filter { it.value.formula == null }
   }*/

   private val fieldInitializers: Map<AttributeName, Lazy<TypedInstance>> by lazy {
      attributesToMap.map { (attributeName, field) ->
         attributeName to lazy { buildField(field, attributeName) }
      }.toMap()
   }

   suspend fun buildAsync(decorator: suspend (attributeMap: Map<AttributeName, TypedInstance>) -> Map<AttributeName, TypedInstance> = { attributesToMap -> attributesToMap }): TypedInstance {
      if (isJson(value)) {
         val jsonParsedStructure = JsonParsedStructure.from(value as String, objectMapper)
         return TypedInstance.from(
            type,
            jsonParsedStructure,
            schema,
            nullValues = nullValues,
            source = source,
            evaluateAccessors = evaluateAccessors,
            functionRegistry = functionRegistry
         )
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

   fun build(decorator: (attributeMap: Map<AttributeName, TypedInstance>) -> Map<AttributeName, TypedInstance> = { attributesToMap -> attributesToMap }): TypedInstance {
      if (isJson(value)) {
         val jsonParsedStructure = JsonParsedStructure.from(value as String, objectMapper)
         return TypedInstance.from(
            type,
            jsonParsedStructure,
            schema,
            nullValues = nullValues,
            source = source,
            evaluateAccessors = evaluateAccessors,
            functionRegistry = functionRegistry
         )
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
      val initializer = fieldInitializers[attributeName]
         ?: error("Cannot request field $attributeName as no initializer has been prepared")
      return initializer.value
   }

   /**
    * Returns a value looked up by it's type
    */
   override fun getValue(typeName: QualifiedName, queryIfNotFound: Boolean): TypedInstance {
      val requestedType = schema.type(typeName)
      val candidateTypes = this.type.attributes.filter { (name, field) ->
         val fieldType = schema.type(field.type)
         fieldType.isAssignableTo(requestedType)
      }
      return when (candidateTypes.size) {
         0 -> {
            if (requestedType.hasExpression) {
               // If the type is an expression, we may be able to calculate it, even though the
               // value wasn't explictly present.
               // Potential for stack overflow here -- might need to do some recursion checking
               // that prevents self-referential loops.
               evaluateExpressionType(typeName)
            }
            else {
               handleTypeNotFound(requestedType, queryIfNotFound)
            }
         }
         1 -> getValue(candidateTypes.keys.first())
         else -> TypedNull.create(
            requestedType, FailedEvaluatedExpression(
               typeName.fullyQualifiedName,
               emptyList(),
               "Ambiguous type request for type ${typeName.parameterizedName} - there are ${candidateTypes.size} matching attributes: ${candidateTypes.keys.joinToString()}"
            )
         )
      }
   }

   private fun handleTypeNotFound(
      requestedType: Type,
      queryIfNotFound: Boolean,
   ): TypedInstance {
      fun createTypedNull(
         errorMessage: String = "No attribute with type ${requestedType.name.parameterizedName} is present on type ${this.type.name.parameterizedName}"
      ): TypedNull {
         return TypedNull.create(
            requestedType, FailedEvaluatedExpression(
               requestedType.name.fullyQualifiedName,
               emptyList(),
               errorMessage
            )
         )
      }
      return when {
         queryIfNotFound && inPlaceQueryEngine != null -> {
            // TODO : Remove the blocking behaviour here.
            // TypedObjectFactory has always been blocking (but
            // historically hasn't invoked services), so leaving as
            // blocking when introducing type expressions with lookups.
            // However, in future, we need to mkae the TypedObjectFactory
            // async up the chain.
            runBlocking {
               val resultsFromSearch = inPlaceQueryEngine.findType(requestedType)
                  .toList()
               when {
                  resultsFromSearch.isEmpty() -> createTypedNull(
                     "No attribute with type ${requestedType.name.parameterizedName} is present on type ${type.name.parameterizedName} and attempts to discover a value from the query engine failed"
                  )
                  resultsFromSearch.size == 1 -> resultsFromSearch.first()
                  resultsFromSearch.size > 1 && requestedType.isCollection -> {
                     TypedCollection.from(resultsFromSearch)
                  }
                  else ->  createTypedNull(
                     "No attribute with type ${requestedType.name.parameterizedName} is present on type ${type.name.parameterizedName} and attempts to discover a value from the query engine returned ${resultsFromSearch.size} results.  Given this is ambiguous, returning null"
                  )
               }

            }

         }
         queryIfNotFound && inPlaceQueryEngine == null -> {
            logger.warn { "Requested to use queryEngine to lookup value ${requestedType.qualifiedName.parameterizedName} but no query engine was provided.  Returning null" }
            createTypedNull()
         }
         else -> {
            createTypedNull()
         }
      }
   }

   /**
    * Returns a value looked up by it's name
    */
   override fun getValue(attributeName: AttributeName): TypedInstance {
      return getOrBuild(attributeName)
   }

   override fun readAccessor(type: Type, accessor: Accessor): TypedInstance {
      return accessorReader.read(value, type, accessor, schema, source = source)
   }

   override fun readAccessor(type: QualifiedName, accessor: Accessor, nullable: Boolean): TypedInstance {
      return accessorReader.read(value, type, accessor, schema, nullValues, source = source, nullable = nullable)
   }

   fun evaluateExpressionType(expressionType:Type):TypedInstance {
      val expression = expressionType.expression!!
      return accessorReader.evaluate(value, expressionType, expression, schema, nullValues, source)
   }
   fun evaluateExpression(expression:Expression):TypedInstance {
      return accessorReader.evaluate(value, schema.type(expression.returnType), expression, schema, nullValues, source)
   }
   private fun evaluateExpressionType(typeName: QualifiedName): TypedInstance {
      val type = schema.type(typeName)
     return evaluateExpressionType(type)
   }


   private fun buildField(field: Field, attributeName: AttributeName): TypedInstance {
      // We don't always want to use accessors.
      // When parsing content from a cask, which has already been processed, what we
      // receive is a TypedObject.  The accessors should be ignored in this scenario.
      // By default, we want to cosndier them.
      val considerAccessor = field.accessor != null && evaluateAccessors
      val evaluateTypeExpression = schema.type(field.type).hasExpression && evaluateAccessors

      // Questionable design choice: Favour directly supplied values over accessors and conditions.
      // The idea here is that when we're reading from a file or non parsed source, we need
      // to know how to construct the instance.
      // However, if that work has already been done, and we're trying to rebuild the instance
      // from a parsing result, we need to be able to.
      // Therefore, if we've been directly supplied the value, use it.
      // Otherwise, look to leverage conditions.
      // Note - revisit if this proves to be problematic.
      return when {
         // Cheaper readers first
         value is CSVRecord && field.accessor is ColumnAccessor && considerAccessor -> {
            readAccessor(field.type, field.accessor, field.nullable)
         }

         // ValueReader can be expensive if the value is an object,
         // so only use the valueReader early if the value is a map

         // MP 19-Nov: field.accessor null check had been added here to fix a bug, but I can't remember what it was.
         // However, the impact of adding it is that when parsing TypedObjects from remote calls that have already been
         // processed (and so the accessor isn't required) means that we fall through this check and try using the
         // accessor, which will fail, as this isn't raw content anymore, it's parsed / processed.
         value is Map<*, *> && !considerAccessor && valueReader.contains(value, attributeName) -> readWithValueReader(
            attributeName,
            field
         )
         considerAccessor -> {
            readAccessor(field.type, field.accessor!!, field.nullable)
         }
         evaluateTypeExpression -> {
            evaluateExpressionType(field.type)
         }
         field.readCondition != null -> {
            conditionalFieldSetEvaluator.evaluate("What do I pass here?",field.readCondition, attributeName, schema.type(field.type), UndefinedSource)
         }
         // Not a map, so could be an object, try the value reader - but this is an expensive
         // call, so we defer to last-ish
         valueReader.contains(value, attributeName) -> readWithValueReader(attributeName, field)

         // Is there a default?
         field.defaultValue != null -> TypedValue.from(
            schema.type(field.type),
            field.defaultValue,
            ConversionService.DEFAULT_CONVERTER,
            source = DefinedInSchema
         )

         else -> {
            // log().debug("The supplied value did not contain an attribute of $attributeName and no accessors or strategies were found to read.  Will return null")
            TypedNull.create(schema.type(field.type))
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
