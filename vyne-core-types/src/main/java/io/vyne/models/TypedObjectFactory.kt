package io.vyne.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import io.vyne.models.conditional.ConditionalFieldSetEvaluator
import io.vyne.models.facts.*
import io.vyne.models.format.FormatDetector
import io.vyne.models.format.ModelFormatSpec
import io.vyne.models.functions.FunctionRegistry
import io.vyne.models.functions.FunctionResultCacheKey
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
import lang.taxi.accessors.JsonPathAccessor
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
   private val accessorHandlers: List<AccessorHandler<out Accessor>> = emptyList(),
   private val formatSpecs: List<ModelFormatSpec> = emptyList(),
   private val parsingErrorBehaviour: ParsingFailureBehaviour = ParsingFailureBehaviour.ThrowException,
   private val functionResultCache: MutableMap<FunctionResultCacheKey, Any> = mutableMapOf()
) : EvaluationValueSupplier {
   private val logger = KotlinLogging.logger {}

   private val buildSpecProvider = TypedInstancePredicateFactory()

   private val valueReader = ValueReader()
   private val accessorReader: AccessorReader by lazy {
      AccessorReader(
         this,
         this.functionRegistry,
         this.schema,
         this.accessorHandlers,
         this.functionResultCache
      )
   }
   private val conditionalFieldSetEvaluator = ConditionalFieldSetEvaluator(this, this.schema, accessorReader)
   private val formatDetector = FormatDetector.get(formatSpecs)
   private val currentValueFactBag: FactBag by lazy {
      when {
         value is FactBag -> value
         value is List<*> && value.filterIsInstance<TypedInstance>()
            .isNotEmpty() -> FactBag.of(value.filterIsInstance<TypedInstance>(), schema)

         else -> FactBag.empty()
      }
   }

   /**
    * Even if evaluateAccessors is globally true, we sometimes want to
    * disable accessor evaluation temporarily.
    *
    * This is typically for evaluating a default() value, to check if the underlying value is actually present / discoverable,
    * before falling back to the default.
    *
    * This approach isn't great, as we have mutable state here.
    * However, given the lazy function evaluation we're using, there isn't an easy alternative.
    *
    * If/when we finally remove accessors for just outright ExpressionEvaluation, (ie., removing "special" accessors like
    * default() and column() ), we can remove this.
    * */
   private var accessorEvaluationSupressed: Boolean = false


   private val attributesToMap = type.attributes

   private val fieldInitializers: Map<AttributeName, Lazy<TypedInstance>> by lazy {
      attributesToMap.map { (attributeName, field) ->
         attributeName to lazy {
            val fieldValue = buildField(field, attributeName)
            if (field.fieldProjection != null) {
//               val sw = Stopwatch.createStarted()
               val projection = projectField(field, attributeName, fieldValue)
//               logger.debug { "Projection to ${projection.type.name.shortDisplayName} took ${sw.elapsed().toMillis()}ms" }
               projection

            } else {
               fieldValue
            }
         }
      }.toMap()
   }

   /**
    * Where a field has an inline projection defined (
    * foo : Thing as {
    *    a : A, b: B
    * }
    */
   private fun projectField(field: Field, attributeName: AttributeName, fieldValue: TypedInstance): TypedInstance {
      if (fieldValue is TypedNull) {
         // Don't attempt to project nulls
         return TypedNull.create(schema.type(field.type), fieldValue.source)
      }
      val projectedType = schema.type(field.fieldProjection!!.projectedType)
      val projectedFieldValue = if (fieldValue is TypedCollection && projectedType.isCollection) {
         // Project each member of the collection seperately
         fieldValue.map { collectionMember ->
            newFactory(projectedType.collectionType!!, collectionMember)
               .build()
         }.let { projectedCollection -> TypedCollection.from(projectedCollection, source) }
      } else {
         newFactory(projectedType, fieldValue).build()
      }
      return projectedFieldValue
   }

   /**
    * Returns a new TypedObjectFactory,
    * merging the current set of known values with the newValue if possible.
    */
   private fun newFactory(
      type: Type,
      newValue: Any,
      factsToExclude: Set<TypedInstance> = emptySet()
   ): TypedObjectFactory {

      val newMergedValue = when {
         this.value is FactBag && newValue is TypedInstance -> CascadingFactBag(
            CopyOnWriteFactBag(newValue, schema),
            this.value
         )

         this.value is FactBag && newValue is FactBag -> CascadingFactBag(newValue, this.value)
         this.value !is FactBag && factsToExclude.isNotEmpty() -> error("Cannot exclude facts when the source of facts is not a FactBag")
         else -> newValue
      }

      return TypedObjectFactory(
         type,
         newMergedValue,
         schema,
         nullValues,
         source,
         objectMapper,
         functionRegistry,
         evaluateAccessors,
         inPlaceQueryEngine,
         accessorHandlers,
         formatSpecs,
         parsingErrorBehaviour,
         functionResultCache
      )
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
            functionRegistry = functionRegistry,
            formatSpecs = formatSpecs,
            parsingErrorBehaviour = parsingErrorBehaviour
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
      val metadataAndFormat = formatDetector.getFormatType(type)
      if (metadataAndFormat != null) {
         // now what?
         val (metadata, modelFormatSpec) = metadataAndFormat
         if (modelFormatSpec.deserializer.parseRequired(value, metadata)) {
            val parsed = modelFormatSpec.deserializer.parse(value, type, metadata, schema)
            return TypedInstance.from(
               type,
               parsed,
               schema,
               source = source,
               evaluateAccessors = evaluateAccessors,
               functionRegistry = functionRegistry,
               formatSpecs = formatSpecs,
               inPlaceQueryEngine = inPlaceQueryEngine,
               parsingErrorBehaviour = parsingErrorBehaviour
            )
         }
      }
      if (isJson(value)) {
         val jsonParsedStructure = JsonParsedStructure.from(value as String, objectMapper)
         return TypedInstance.from(
            type,
            jsonParsedStructure,
            schema,
            nullValues = nullValues,
            source = source,
            evaluateAccessors = evaluateAccessors,
            functionRegistry = functionRegistry,
            formatSpecs = formatSpecs,
            inPlaceQueryEngine = inPlaceQueryEngine,
            parsingErrorBehaviour = parsingErrorBehaviour
         )
      }

      if (type.isCollection) {
         return CollectionReader.readCollectionFromNonTypedCollectionValue(
            type,
            value,
            schema,
            source,
            functionRegistry,
            inPlaceQueryEngine
         )
      }
      if (type.isScalar && type.hasExpression) {
         return evaluateExpressionType(type)
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

   private fun getOrBuild(attributeName: AttributeName, allowAccessorEvaluation: Boolean = true): TypedInstance {
      // Originally we used a concurrentHashMap.computeIfAbsent { ... } approach here.
      // However, functions on accessors can access other fields, which can cause recursive access.
      // Therefore, migrated to using initializers with kotlin Lazy functions
      val initializer = fieldInitializers[attributeName]
         ?: error("Cannot request field $attributeName as no initializer has been prepared")

      val accessorEvaluationWasSupressed = accessorEvaluationSupressed
      accessorEvaluationSupressed = !allowAccessorEvaluation

      // Reading the value will trigger population the first time.
      val value = initializer.value

      accessorEvaluationSupressed = accessorEvaluationWasSupressed
      return value
   }

   /**
    * Returns a value looked up by it's type
    */
   override fun getValue(
      typeName: QualifiedName,
      queryIfNotFound: Boolean,
      allowAccessorEvaluation: Boolean
   ): TypedInstance {
      val requestedType = schema.type(typeName)

      // MP - 2-Nov-21:  Added to allow seart
      val fromFactBag = currentValueFactBag.getFactOrNull(
         FactSearch.findType(
            requestedType,
            strategy = FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT
         )
      )
      if (fromFactBag != null) {
         return fromFactBag
      }
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
            } else {
               handleTypeNotFound(requestedType, queryIfNotFound)
            }
         }

         1 -> getValue(candidateTypes.keys.first(), allowAccessorEvaluation)
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
               val resultsFromSearch = try {
                  inPlaceQueryEngine.findType(requestedType)
                     .toList()
               } catch (e: Exception) {
                  // handle io.vyne.query.UnresolvedTypeInQueryException
                  emptyList()
               }
               when {
                  resultsFromSearch.isEmpty() -> createTypedNull(
                     "No attribute with type ${requestedType.name.parameterizedName} is present on type ${type.name.parameterizedName} and attempts to discover a value from the query engine failed"
                  )

                  resultsFromSearch.size == 1 -> resultsFromSearch.first()
                  resultsFromSearch.size > 1 && requestedType.isCollection -> {
                     TypedCollection.from(resultsFromSearch, MixedSources.singleSourceOrMixedSources(resultsFromSearch))
                  }

                  else -> createTypedNull(
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


   private fun getValue(attributeName: AttributeName, allowAccessorEvaluation: Boolean): TypedInstance {
      return getOrBuild(attributeName, allowAccessorEvaluation)
   }

   /**
    * Returns a value looked up by it's name
    */
   override fun getValue(attributeName: AttributeName): TypedInstance {
      return getValue(attributeName, allowAccessorEvaluation = true)
   }

   override fun readAccessor(type: Type, accessor: Accessor): TypedInstance {
      return accessorReader.read(value, type, accessor, schema, source = source)
   }

   override fun readAccessor(type: QualifiedName, accessor: Accessor, nullable: Boolean): TypedInstance {
      return accessorReader.read(value, type, accessor, schema, nullValues, source = source, nullable = nullable)
   }

   fun evaluateExpressionType(expressionType: Type): TypedInstance {
      val expression = expressionType.expression!!
      return accessorReader.evaluate(value, expressionType, expression, schema, nullValues, source)
   }

   fun evaluateExpression(expression: Expression): TypedInstance {
      return accessorReader.evaluate(value, schema.type(expression.returnType), expression, schema, nullValues, source)
   }

   private fun evaluateExpressionType(typeName: QualifiedName): TypedInstance {
      val type = schema.type(typeName)
      return evaluateExpressionType(type)
   }


   private fun buildField(field: Field, attributeName: AttributeName): TypedInstance {
      // When we're building a field, if there's a projection on it,
      // we build the source type initially.  Once the source is built, we
      // then project to the target type.
      val fieldType = if (field.fieldProjection != null) {
         schema.type(field.fieldProjection.sourceType)
      } else {
         schema.type(field.type)
      }
      val fieldTypeName = fieldType.qualifiedName

      // We don't always want to use accessors.
      // When parsing content from a cask, which has already been processed, what we
      // receive is a TypedObject.  The accessors should be ignored in this scenario.
      // By default, we want to cosndier them.
      val considerAccessor = field.accessor != null && evaluateAccessors && !accessorEvaluationSupressed
      val evaluateTypeExpression = fieldType.hasExpression && evaluateAccessors

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
            readAccessor(fieldTypeName, field.accessor, field.nullable)
         }

         // ValueReader can be expensive if the value is an object,
         // so only use the valueReader early if the value is a map
         // MP 19-Nov-20: field.accessor null check had been added here to fix a bug, but I can't remember what it was.
         // However, the impact of adding it is that when parsing TypedObjects from remote calls that have already been
         // processed (and so the accessor isn't required) means that we fall through this check and try using the
         // accessor, which will fail, as this isn't raw content anymore, it's parsed / processed.
         value is Map<*, *> && !considerAccessor && valueReader.contains(value, attributeName) -> readWithValueReader(
            attributeName,
            fieldType
         )

         // This is not nice, but we're trying to solve the following problem where a model has a column accessor, e.g.:
         // model Foo { isin: by column("ISIN") }
         // and we have a rest operation returning Foo as a json, i.e.:
         // { "isin": "IT0003123" }
         // return value of this service can be parsed into 'Foo' without below.
         value is JsonParsedStructure && considerAccessor && field.accessor !is JsonPathAccessor && valueReader.contains(
            value,
            attributeName
         ) -> readWithValueReader(attributeName, fieldType)

         considerAccessor -> {
            readAccessor(fieldTypeName, field.accessor!!, field.nullable)
         }

         evaluateTypeExpression -> {
            evaluateExpressionType(fieldTypeName)
         }

         field.readCondition != null -> {
            conditionalFieldSetEvaluator.evaluate(
               "What do I pass here?",
               field.readCondition,
               attributeName,
               fieldType,
               UndefinedSource
            )
         }
         // Not a map, so could be an object, try the value reader - but this is an expensive
         // call, so we defer to last-ish
         valueReader.contains(value, attributeName) -> readWithValueReader(attributeName, fieldType)

         // Is there a default?
         field.defaultValue != null -> TypedValue.from(
            fieldType,
            field.defaultValue,
            ConversionService.DEFAULT_CONVERTER,
            source = DefinedInSchema,
            parsingErrorBehaviour
         )

         // 2-Nov-22: Added this when trying to build inline
         // projections.  However, concerned about knock-on effects...
         value is FactBag -> {

            // The rationale here is if I asked for Foo[], I want all the Foo's,
            // not just a single collection.
            val searchStrategy = if (fieldType.isCollection) {
               FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY
            } else {
               FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT
            }
            val searchedValue = value.getFactOrNull(
               FactSearch.findType(
                  fieldType,
                  strategy = searchStrategy
               )
            )
            searchedValue ?: queryForResult(field, fieldType, attributeName, fieldTypeName)
         }

         else -> queryForResult(field, fieldType, attributeName, fieldTypeName)
      }
   }

   private fun failWithTypedNull(
      fieldType: Type,
      attributeName: AttributeName,
      fieldTypeName: QualifiedName,
      message: String = "Can't populate attribute $attributeName on type ${type.name} as no attribute or expression was found on the supplied value of type ${value::class.simpleName}"
   ): TypedNull {
      return TypedNull.create(
         fieldType,
         ValueLookupReturnedNull(
            message,
            fieldTypeName
         )
      )
   }

   private fun queryForResult(
      field: Field,
      type: Type,
      attributeName: AttributeName,
      fieldTypeName: QualifiedName
   ): TypedInstance {
      return if (inPlaceQueryEngine != null) {
         val fieldInstanceValidPredicate = buildSpecProvider.provide(field)
         val buildResult = runBlocking {
            inPlaceQueryEngine.findType(type, fieldInstanceValidPredicate)
               .toList()
         }
         when {
            buildResult.isEmpty() -> failWithTypedNull(type, attributeName, fieldTypeName)
            buildResult.size == 1 -> buildResult.single()
            else -> {
               val message =
                  "Querying to find type ${fieldTypeName.parameterizedName} returned ${buildResult.size} results, which is ambiguous.  Returning null"
               logger.debug { message }
               failWithTypedNull(type, attributeName, fieldTypeName, message = message)
            }
         }
      } else {
         failWithTypedNull(type, attributeName, fieldTypeName)
      }
   }


   private fun readWithValueReader(attributeName: AttributeName, type: Type): TypedInstance {
      val attributeValue = valueReader.read(value, attributeName)
      return if (attributeValue == null) {
         TypedNull.create(type, source)
      } else {
         TypedInstance.from(
            schema.type(type.qualifiedName.parameterizedName),
            attributeValue,
            schema,
            true,
            source = source,
            parsingErrorBehaviour = parsingErrorBehaviour
         )
      }
   }

}
