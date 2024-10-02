package com.orbitalhq.models

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import com.orbitalhq.expressions.OperatorExpressionCalculator
import com.orbitalhq.formats.csv.CsvAttributeAccessorParser
import com.orbitalhq.formulas.CalculatorRegistry
import com.orbitalhq.models.conditional.ConditionalFieldSetEvaluator
import com.orbitalhq.models.conditional.WhenBlockEvaluator
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.models.facts.FactDiscoveryStrategy
import com.orbitalhq.models.facts.ScopedFact
import com.orbitalhq.models.functions.FunctionRegistry
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.json.JsonAttributeAccessorParser
import com.orbitalhq.models.xml.XmlParsedStructure
import com.orbitalhq.models.xml.XmlTypedInstanceParser
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import com.orbitalhq.schemas.toVyneQualifiedName
import com.orbitalhq.utils.log
import com.orbitalhq.utils.timeBucket
import com.orbitalhq.utils.xtimed
import lang.taxi.accessors.Accessor
import lang.taxi.accessors.ColumnAccessor
import lang.taxi.accessors.ConditionalAccessor
import lang.taxi.accessors.DestructuredAccessor
import lang.taxi.accessors.FieldSourceAccessor
import lang.taxi.accessors.JsonPathAccessor
import lang.taxi.accessors.LiteralAccessor
import lang.taxi.accessors.ReadFunction
import lang.taxi.accessors.ReadFunctionFieldAccessor
import lang.taxi.accessors.XpathAccessor
import lang.taxi.expressions.CastExpression
import lang.taxi.expressions.Expression
import lang.taxi.expressions.ExtensionFunctionExpression
import lang.taxi.expressions.FieldReferenceExpression
import lang.taxi.expressions.FunctionExpression
import lang.taxi.expressions.LambdaExpression
import lang.taxi.expressions.LiteralArray
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.ObjectExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.ProjectingExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.functions.FunctionAccessor
import lang.taxi.functions.FunctionExpressionAccessor
import lang.taxi.types.ArgumentSelector
import lang.taxi.types.FieldReferenceSelector
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.FormulaOperator
import lang.taxi.types.LambdaExpressionType
import lang.taxi.types.ModelAttributeReferenceSelector
import lang.taxi.types.PrimitiveType
import lang.taxi.types.TypeReference
import lang.taxi.types.TypeReferenceSelector
import lang.taxi.types.WhenExpression
import lang.taxi.utils.takeHead
import org.apache.commons.csv.CSVRecord
import org.w3c.dom.Document
import kotlin.reflect.KClass

object Parsers {
   val xmlParser: XmlTypedInstanceParser by lazy { XmlTypedInstanceParser() }
   val csvParser: CsvAttributeAccessorParser by lazy { CsvAttributeAccessorParser() }
   val jsonParser: JsonAttributeAccessorParser by lazy { JsonAttributeAccessorParser() }
}


interface AccessorHandler<T : Accessor> {
   val accessorType: KClass<T>
   fun process(
      accessor: T,
      objectFactory: EvaluationValueSupplier,
      schema: Schema,
      targetType: Type,
      source: DataSource
   ): TypedInstance
}

class AccessorReader(
   private val objectFactory: EvaluationValueSupplier,
   private val functionRegistry: FunctionRegistry,
   private val schema: Schema,
   private val accessorHandlers: List<AccessorHandler<out Accessor>> = emptyList(),
   private val functionResultCache: MutableMap<FunctionResultCacheKey, Any> = mutableMapOf(),
   private val valueProjector: ValueProjector? = null
) {
   val accessorsByType = accessorHandlers.associateBy { it.accessorType }

   companion object {
      fun forFacts(
         facts: List<TypedInstance>,
         schema: Schema,
         accessorHandlers: List<AccessorHandler<in Accessor>> = emptyList()
      ): AccessorReader {
         return AccessorReader(
            FactBagValueSupplier.of(facts, schema),
            schema.functionRegistry,
            schema,
            accessorHandlers
         )
      }
   }

   // There's a cost to building all the Xml junk - so defer if we don't need it,
   // and re-use inbetween readers
   private val xmlParser: XmlTypedInstanceParser by lazy { Parsers.xmlParser }
   private val csvParser: CsvAttributeAccessorParser by lazy { Parsers.csvParser }
   private val jsonParser: JsonAttributeAccessorParser by lazy { Parsers.jsonParser }
   private val conditionalFieldSetEvaluator: ConditionalFieldSetEvaluator by lazy {
      ConditionalFieldSetEvaluator(
         objectFactory, schema, this
      )
   }

   fun read(
      value: Any,
      targetTypeRef: QualifiedName,
      accessor: Accessor,
      schema: Schema,
      nullValues: Set<String> = emptySet(),
      source: DataSource,
      nullable: Boolean,
      format: FormatsAndZoneOffset?,
      allowContextQuerying: Boolean = false,
   ): TypedInstance {
      val targetType = schema.type(targetTypeRef)
      return read(value, targetType, accessor, schema, nullValues, source, format, nullable, allowContextQuerying)
   }

   fun read(
      value: Any,
      targetType: Type,
      accessor: Accessor,
      schema: Schema,
      nullValues: Set<String> = emptySet(),
      source: DataSource,
      format: FormatsAndZoneOffset?,
      nullable: Boolean = false,
      allowContextQuerying: Boolean = false,
   ): TypedInstance {
      if (accessorsByType.containsKey(accessor::class)) {
         val accessorHandler = accessorsByType[accessor::class] as AccessorHandler<in Accessor>
         return accessorHandler.process(accessor, objectFactory, schema, targetType, source)
      }
      return when (accessor) {
         // TODO : Gradually move these accessors out to individual classes to enable better injection / pluggability
         is JsonPathAccessor -> parseJson(value, targetType, schema, accessor, source, format)
         is XpathAccessor -> parseXml(value, targetType, schema, accessor, source, nullable, format)
         is DestructuredAccessor -> parseDestructured(value, targetType, schema, accessor, source, format)
         is ColumnAccessor -> parseColumnData(value, targetType, schema, accessor, nullValues, source, nullable, format)
         /* was: (before removing the concept of "by default()"
         {
            if (accessor.index == null && accessor.defaultValue != null) {
               // This is some tech debt.
               // Default values (defined as by default("foo") turn up as ColumnAccessors.
               readWithDefaultValue(value, targetType, schema, accessor, nullValues, source, nullable)
            } else {
               parseColumnData(value, targetType, schema, accessor, nullValues, source, nullable, format)
            }

         }
      */

         is ConditionalAccessor -> evaluateConditionalAccessor(value, targetType, schema, accessor, nullValues, source)
         is ReadFunctionFieldAccessor -> evaluateReadFunctionAccessor(
            value,
            targetType,
            schema,
            accessor,
            nullValues,
            source,
            format
         )

         is FunctionAccessor -> evaluateFunctionAccessor(
            value,
            targetType,
            schema,
            accessor,
            nullValues,
            source,
            functionResultCache,
            format
         )

         is FieldReferenceSelector -> {
            error("FieldReferenceSelector shouldn't exist as an accessor - expected everything was migrated FieldReferenceExpression")
         }

         is ObjectExpression -> {
            val typedInstanceMap = accessor.expressionMap.map { expression ->
               expression.key to read(value, schema.type(expression.value.returnType), expression.value, schema, nullValues, source, format)
            }.toMap()
            TypedInstance.from(targetType, typedInstanceMap, schema, source = source)
         }

         is LiteralAccessor -> {
            // MP: 26-Oct-21 : Not sure about the Source here.
            // Previously we passed the source that we received as an input.
            // However, for Literals, this often meant we were providing the top-level "MixedSources"
            // that's provided into the TypedObjectFactory.
            // If that's the case, we can generally assume safely that literals have been provided from a schema
            // or input somewhere, so use Provided.  Not sure what the other use cases are here, so have left a logger
            // message to investigate further.
            val dataSource = if (source == MixedSources) {
               Provided
            } else {
               log().debug("Received a data source to LiteralAccessor that wasn't MixedSources.  See comments and investigate this.")
               source
            }
            // Note: We upcast the type (an attempt to upgrade from Any) if possible.
            // Sometimes literals are declared as Any, and we have richer type information in the value,
            // so we use that.
            return TypedInstance.from(
               TypeUtils.upcastIfPossible(schema.type(accessor.returnType), targetType),
               accessor.value,
               schema,
               source = dataSource
            )
         }

         is FunctionExpressionAccessor -> evaluateFunctionExpressionAccessor(
            value,
            targetType,
            schema,
            accessor,
            nullValues,
            source,
            functionResultCache,
            format
         )

         is TypeReferenceSelector -> objectFactory.getValue(
            accessor.type.toVyneQualifiedName(),
            queryIfNotFound = allowContextQuerying
         )

         is FieldSourceAccessor -> TypedNull.create(targetType, source)
         is LambdaExpression -> DeferredExpression(accessor, schema, source)
         is OperatorExpression -> evaluateOperatorExpression(
            targetType,
            accessor,
            schema,
            value,
            nullValues,
            source,
            format
         )

         is FieldReferenceExpression -> xtimed("evaluate field reference ${accessor.asTaxi()}") {
            evaluateFieldReference(
               targetType,
               accessor.selectors,
               source
            )
         }

         is LiteralExpression -> read(
            value,
            targetType,
            accessor.literal,
            schema,
            nullValues,
            source,
            format,
            nullable,
            allowContextQuerying
         )

         is FunctionExpression -> read(
            value,
            targetType,
            accessor.function,
            schema,
            nullValues,
            source,
            format,
            nullable,
            allowContextQuerying
         )

         is TypeExpression -> readTypeExpression(accessor, allowContextQuerying, targetType)
         is ModelAttributeReferenceSelector -> xtimed("read model Attribute ${accessor.asTaxi()}") {
            readModelAttributeSelector(
               accessor,
               allowContextQuerying,
               schema
            )
         }

         is ArgumentSelector -> readScopedReferenceSelector(value,accessor)
         is ProjectingExpression -> {
            evaluateProjectingExpression(
               value,
               targetType,
               accessor,
               schema,
               nullValues,
               source,
               format,
               nullable,
               allowContextQuerying,
               valueProjector
            )
         }

         is WhenExpression -> {
            WhenBlockEvaluator(
               this.objectFactory, schema, this
            ).evaluate(value, accessor, source, targetType, format)
         }

         is CastExpression -> {
            val evaluatedExpression = read(
               value,
               targetType,
               accessor.expression,
               schema,
               nullValues,
               source,
               format,
               nullable,
               allowContextQuerying
            )
            val castType = schema.type(accessor.type)
            TypedInstance.from(
               castType,
               evaluatedExpression.value,
               schema,
               source = EvaluatedExpression(expressionTaxi = accessor.asTaxi(), listOf(evaluatedExpression))
            )
         }
         is ExtensionFunctionExpression -> {
            evaluateExtensionFunctionExpression(
               value,
               targetType,
               accessor,
               schema,
               nullValues,
               source,
               functionResultCache,
               format
            )
         }

         is LiteralArray -> {
            val typedInstances = accessor.members.map { expression ->
              read(value, schema.type(expression.returnType), expression, schema, nullValues, source, format)
            }
            TypedInstance.from(targetType, typedInstances, schema, source = source)
         }

         else -> {
            TODO("Support for accessor not implemented with type $accessor")
         }
      }
   }

   private fun evaluateProjectingExpression(
      value: Any,
      targetType: Type,
      accessor: ProjectingExpression,
      schema: Schema,
      nullValues: Set<String>,
      source: DataSource,
      format: FormatsAndZoneOffset?,
      nullable: Boolean,
      allowContextQuerying: Boolean,
      valueProjector: ValueProjector?
   ): TypedInstance {
      val valueToProject = read(
         value,
         targetType,
         accessor.expression,
         schema,
         nullValues,
         source,
         format,
         nullable,
         allowContextQuerying
      )
      if (valueProjector == null) {
         error("Cannot project, as no ValueProjector has been supplied. Understand this use-case")
      }
      // TODO PERF: When we have projecting statements within an
      // expression (vs on a field or as a top-level concern), then they're not currently converted to Vyne types.
      // So, we have to convert them here.
      // We should find a way to convert them earlier.
      val projectionType = schema.typeCreateIfRequired(accessor.projection.projectedType)
      return if (valueToProject is DeferredTypedInstance) {
         DeferredProjection(
            valueToProject,
            accessor.projection,
            valueProjector,
            projectionType,
            schema,
            nullValues,
            source,
            format,
            nullable,
            allowContextQuerying
         )
      } else {
         valueProjector.project(
            valueToProject,
            accessor.projection,
            projectionType,
            schema,
            nullValues,
            source,
            format,
            nullable,
            allowContextQuerying
         )
      }
   }


   private fun readModelAttributeSelector(
      accessor: ModelAttributeReferenceSelector,
      allowContextQuerying: Boolean,
      schema: Schema
   ): TypedInstance {
      val source = xtimed("source value lookup") {
         objectFactory.getValue(accessor.memberSource.toVyneQualifiedName(), queryIfNotFound = allowContextQuerying)
      }
      val requestedType = schema.type(accessor.targetType)
      val accessorReturnType = schema.type(accessor.returnType.toVyneQualifiedName())
      val discoveryStrategy = // If the accessor is looking for a collection of the requestedType
      // (ie, is declared as:
      // field: (Foo:Bar)[]
      // Then we look for all possible matches
      // If the accessor is looking for a collection of the requestedType
      // (ie, is declared as:
      // field: (Foo:Bar)[]
      // Then we look for all possible matches
      // If the accessor is looking for a collection of the requestedType
      // (ie, is declared as:
      // field: (Foo:Bar)[]
         // Then we look for all possible matches
         if (accessorReturnType.isCollection && accessorReturnType.collectionType == requestedType) {
            FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY
         } else {
            FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE
         }
      val fact =
         xtimed("readModelAttributeSelector - fact bag search against ${source.type.qualifiedName.shortDisplayName}") {
            FactBag.of(listOf(source), schema)
               .getFactOrNull(requestedType, discoveryStrategy)
         }
      return fact ?: TypedNull.create(
         requestedType,
         FailedEvaluatedExpression(
            accessor.asTaxi(),
            emptyList(),
            "Unable to find instance of ${requestedType.qualifiedName.shortDisplayName} from source of ${source.type.qualifiedName.shortDisplayName}"
         )
      )
   }

   /**
    * Reads a type expression (eg., a token where a type has been used as an input).
    * Depending on the signature, this could be EITHER:
    *  - A request for an instance of that type, looked up from the objectFactory (if the return type is T)
    *  - A request for a reference to the type itself, for reflection purposes (if the return type is Type<T>)
    */
   private fun readTypeExpression(
      accessor: TypeExpression,
      allowContextQuerying: Boolean,
      targetType: Type
   ): TypedInstance {
      val result = if (TypeReference.isTypeReference(targetType.paramaterizedName)) {
         val typeReference = targetType.typeParameters[0]
         TypeReferenceInstance.from(typeReference)
      } else {
         objectFactory.getValue(
            accessor.type.toVyneQualifiedName(),
            queryIfNotFound = allowContextQuerying
         )
      }

      return result
   }

   private fun readScopedReferenceSelector(value: Any, accessor: ArgumentSelector): TypedInstance {
      val scopedInstance = if (value is FactBag) {
         value.getScopedFactOrNull(accessor.scope)?.fact
            ?: error("Failed to resolve scope argument ${accessor.scope.name}")

      } else {
         objectFactory.getScopedFactOrNull(accessor.scope)
            ?: error("Failed to resolve scope argument ${accessor.scope.name}")
      }

      val result = if (accessor.selectors.isNotEmpty()) {
         readFieldSelectorsAgainstObject(
            accessor.selectors,
            scopedInstance,
            schema.type(accessor.returnType),
            accessor.path
         )
      } else {
         scopedInstance
      }
      return result
   }

   private fun evaluateFieldReference(
      targetType: Type,
      selectors: List<FieldReferenceSelector>,
      source: DataSource
   ): TypedInstance {
      val (firstFieldRef, remainingFields) = selectors.takeHead()
      val firstObject = objectFactory.getValue(firstFieldRef.fieldName)
      return readFieldSelectorsAgainstObject(
         remainingFields,
         firstObject,
         targetType,
         selectors.joinToString(".") { it.fieldName })
   }

   private fun readFieldSelectorsAgainstObject(
      remainingFields: List<FieldReferenceSelector>,
      firstObject: TypedInstance,
      targetType: Type,
      fullPath: String
   ): TypedInstance {
      var errorMessage: String? = null
      val value = remainingFields
         .asSequence()
         .takeWhile { errorMessage == null }
         .fold(firstObject as TypedInstance?) { lastObject, fieldReference ->
            val result = when {
               lastObject is TypedNull -> {
                  errorMessage =
                     "Evaluation returned null where a ${lastObject.type.qualifiedName.shortDisplayName} was expected"
                  null
               }

               lastObject !is TypedObject -> {
                  errorMessage =
                     "Evaluation returned a type of ${lastObject!!.type.qualifiedName.shortDisplayName} which doesn't have properties"
                  null
               }

               !lastObject.hasAttribute(fieldReference.fieldName) -> {
                  errorMessage =
                     "Evaluation returned a type of ${lastObject.type.qualifiedName.shortDisplayName} which doesn't have a property named ${fieldReference.fieldName}"
                  null
               }

               else -> {
                  lastObject.get(fieldReference.fieldName)
               }
            }
            result
         }
      return if (errorMessage != null) {
         TypedNull.create(
            targetType,
            FailedEvaluatedExpression(fullPath, listOf(firstObject), errorMessage!!)
         )
      } else {
         value!!
      }
   }

   private fun evaluateFunctionAccessor(
      value: Any,
      targetType: Type,
      schema: Schema,
      accessor: FunctionAccessor,
      nullValues: Set<String>,
      source: DataSource,
      resultCache: MutableMap<FunctionResultCacheKey, Any>,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      val function = accessor.function


      // Note - don't check for == here, because of vararg params
      if (accessor.inputs.size < function.parameters.size) {
         error("Function ${function.qualifiedName} expects ${function.parameters.size} arguments, but only ${accessor.inputs.size} were provided")
      }

      val declaredInputs = timeBucket("lookup inputs for function ${accessor.function.qualifiedName}") {
         function.parameters.filter { !it.isVarArg }.mapIndexed { index, parameter ->
            require(index < accessor.inputs.size) { "Cannot read parameter ${parameter.description} as no input was provided at index $index" }
            val parameterInputAccessor = accessor.inputs[index]
            val targetParameterType = if (parameter.type is LambdaExpressionType) {
               schema.typeCreateIfRequired((parameter.type as LambdaExpressionType).returnType)
//               schema.type((parameter.type as LambdaExpressionType).returnType)
            } else {
               schema.typeCreateIfRequired(parameter.type)
            }

            // This doesn't feel like the right place to do this, it's really
            // treating this as an edge case, and we shouldn't be.
            // Here, we're saying "if the thing we're trying to build is actually the input into the function, then it's ok to search".
            // No real logic behind that, other than it's what I need to make my test pass.

//         val queryIfNotFound = if (targetType.hasExpression && targetType.expression!! is LambdaExpression) {
//            val lambdaExpression = targetType.expression as LambdaExpression
//            lambdaExpression.inputs.contains(parameterInputAccessor.returnType)
//         } else if (targetType.hasExpression) {
//            false
//         } else {
//            false
//         }

            // MP, 2-Nov-21: Modifying the rules here where types that are inputs to an expression can be
            // searched for, regardless.  I suspect this will break some stuff.
            // I think the ACTUAL approach to use here is to introduce an operator that indicates "Search for this thing".
            // Also, our search scope should (by default) consider the typed objects in our hand, where at the moment, it doesn't
            // eg: Currently
            // find { Foo } as {
            // ... <- Here, the attributes of Foo aren't available by default, but they should be.
            // nested1 : {
            // ... <-- Here, the attributes one layer up aren't available, but they should be.
            // }
            //}
            // MP 8-Nov-22: One year later...
            // We had swapped back to the above logic, but without documenting why.
            // I'd like to be able to discover expression params from services, so re-enabling this.
            // If we revert, document the reason.
            val queryIfNotFound = true

            val parameterValue = timeBucket("read function accessor ${accessor.function.qualifiedName}") {
               read(
                  value,
                  targetParameterType,
                  parameterInputAccessor,
                  schema,
                  nullValues,
                  source,
                  allowContextQuerying = queryIfNotFound,
                  format = format
               )
            }
            ScopedFact(parameter, parameterValue)
         }
      }
      val (varArgsParam, varArgsValue) = if (function.parameters.isNotEmpty() && function.parameters.last().isVarArg) {
         val varargFrom = function.parameters.size - 1
         val varargParam = function.parameters.last()
         val varargType = schema.type(varargParam.type)
         val inputs = accessor.inputs.subList(varargFrom, accessor.inputs.size)
         val varArgsValue = inputs.map { varargInputAccessor ->
            read(
               value,
               TypeUtils.mostSpecificType(varargType, schema.type(varargInputAccessor.returnType)),
               varargInputAccessor,
               schema,
               nullValues,
               source,
               format = format,
               allowContextQuerying = true
            )
         }
         varargParam to varArgsValue
      } else null to emptyList()

      // The inputs as typed instances.
      // Used if we're calling to an invoker.
      val allInputValues = declaredInputs.map { it.fact } + varArgsValue



      val functionResult = if (function.hasBody) {
         val varArgsArgument = varArgsParam?.let { param -> ScopedFact(param, TypedCollection.from(varArgsValue)) }
         val allInputArguments = (declaredInputs + varArgsArgument)
            .filterNotNull()
         val evaluationContext = this.objectFactory //.withAdditionalScopedFacts(allInputArguments)
         require(objectFactory is TypedObjectFactory) { "Cannot evaluate function ${function.qualifiedName} as evaluation context was instance of ${evaluationContext::class.simpleName} but needed ${TypedObjectFactory::class.simpleName}" }

         // When evaluating a function (which has named, scoped arguments),
         // names are important - so create a new FactBag containing the scoped arguments.
         // We don't want to use the old one, as the arguments are named incorrectly
         // (eg: names are relative to the scope they were declared, not the scope of the inputs
         // of the function we're about to evaluate).
         val args = FactBag.empty().withAdditionalScopedFacts(allInputArguments, schema)
         objectFactory.newFactoryWithOnly(schema.type(function.returnType!!), args)
            .evaluateExpression(function.body!!)
      } else {
         functionRegistry.invoke(
            function,
            allInputValues,
            schema,
            targetType,
            accessor,
            objectFactory,
            format,
            value,
            resultCache
         )
      }
      return functionResult
   }

   private fun evaluateFunctionExpressionAccessor(
      value: Any,
      targetType: Type,
      schema: Schema,
      accessor: FunctionExpressionAccessor,
      nullValues: Set<String>,
      source: DataSource,
      resultCache: MutableMap<FunctionResultCacheKey, Any>,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      val functionResult =
         this.evaluateFunctionAccessor(
            value,
            targetType,
            schema,
            accessor.functionAccessor,
            nullValues,
            source,
            resultCache,
            format
         )
      val operator = accessor.operator
      val operand = accessor.operand
      val functionValue = functionResult.value ?: return functionResult

      return when (operator) {
         FormulaOperator.Add -> {
            when (functionValue) {
               is Int -> TypedInstance.from(targetType, functionValue.plus(operand as Int), schema, source = source)
               is String -> TypedInstance.from(targetType, "$functionValue$operand", schema, source = source)
               else -> error("unexpected function expression function return value $functionValue, $operator, $operand")
            }
         }

         FormulaOperator.Subtract -> TypedInstance.from(
            targetType,
            (functionValue as Int).minus(operand as Int),
            schema,
            source = source
         )

         else -> error("unexpected function expression function return value $functionValue, $operator, $operand")
      }
   }

   private fun evaluateReadFunctionAccessor(
      value: Any,
      targetType: Type,
      schema: Schema,
      accessor: ReadFunctionFieldAccessor,
      nullValues: Set<String>,
      source: DataSource,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      if (accessor.readFunction != ReadFunction.CONCAT) {
         error("Only concat is allowed")
      }

      val arguments = accessor.arguments.mapNotNull { readFunctionArgument ->
         if (readFunctionArgument.columnAccessor != null) {
            parseColumnData(
               value,
               targetType,
               schema,
               readFunctionArgument.columnAccessor!!,
               nullValues,
               source,
               format = format
            ).value
         } else {
            readFunctionArgument.value
         }
      }

      val builder = StringBuilder()
      arguments.forEach { builder.append(it.toString()) }
      return TypedInstance.from(targetType, builder.toString(), schema, source = source)
   }

   private fun evaluateConditionalAccessor(
      value: Any,
      targetType: Type,
      schema: Schema,
      accessor: ConditionalAccessor,
      nullValues: Set<String>,
      source: DataSource
   ): TypedInstance {
      return conditionalFieldSetEvaluator.evaluate(value, accessor.expression, null, targetType, source)
   }

   private fun parseColumnData(
      value: Any,
      targetType: Type,
      schema: Schema,
      accessor: ColumnAccessor,
      nullValues: Set<String> = emptySet(),
      source: DataSource,
      nullable: Boolean = false,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      // TODO : We should really support parsing from a stream, to avoid having to load large sets in memory
      return when {
         value is String -> csvParser.parse(value, targetType, accessor, schema, source, nullable, format)
         // Efficient parsing where we've already parsed the record once (eg., streaming from disk).
         value is CSVRecord -> csvParser.parseToType(
            targetType,
            accessor,
            value,
            schema,
            nullValues,
            source,
            nullable,
            format
         )

         else -> {
            if (nullable) {
               return TypedInstance.from(targetType, null, schema, source = source)
            } else {
               throw IllegalArgumentException("Unhandled parsing $value $accessor")
            }
         }
      }
   }

   private fun parseDestructured(
      value: Any,
      targetType: Type,
      schema: Schema,
      accessor: DestructuredAccessor,
      source: DataSource,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      val values = accessor.fields.map { (attributeName, accessor) ->
         val objectMemberField = targetType.attribute(attributeName)
         val attributeValue =
            read(value, objectMemberField.type, accessor, schema, source = source, nullable = false, format = format)
         attributeName to attributeValue
      }.toMap()
      return TypedObject(targetType, values, source)
   }

   /**
    * Deprecated. Use an Xml format, and the deserialziation within the FormatSpec instead.
    */
   @Deprecated("Use an XmlFormatSpec instead.")
   private fun parseXml(
      value: Any,
      targetType: Type,
      schema: Schema,
      accessor: XpathAccessor,
      source: DataSource,
      nullable: Boolean,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      // TODO : We should really support parsing from a stream, to avoid having to load large sets in memory
      return when (value) {
         is String -> xmlParser.parse(value, targetType, accessor, schema, source, nullable, format)
         is XmlParsedStructure -> xmlParser.parse(
            value.document,
            targetType,
            accessor,
            schema,
            source,
            nullable,
            format
         )

         is Document -> xmlParser.parse(value, targetType, accessor, schema, source, nullable, format)
         else -> TODO("Value=${value} targetType=${targetType} accessor={$accessor} not supported!")
      }
   }

   private fun parseJson(
      value: Any,
      targetType: Type,
      schema: Schema,
      accessor: JsonPathAccessor,
      source: DataSource,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      return when (value) {
         is ObjectNode -> jsonParser.parseToType(targetType, accessor, value, schema, source, format)
         is Map<*, *> -> jsonParser.parseToType(targetType, accessor, value, schema, source, format)
         else -> TODO("Value=${value} targetType=${targetType} accessor={$accessor} not supported!")
      }
   }

   private fun extractFromMap(
      targetType: Type,
      accessor: JsonPathAccessor,
      value: Map<*, *>,
      schema: Schema,
      source: DataSource,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      // Strictly speaking, we shouldn't be getting maps here.
      // But it's a legacy thing, from when we used xpath(...) all over the shop, even in non xml types
      val expression = accessor.expression
      return when {
         expression.startsWith("$") -> {
            val valueAtJsonPath = try {
               jsonParser.parseToType(targetType, accessor, value, schema, source, format)
               JsonPath.parse(value).read<Any>(expression)
            } catch (e: PathNotFoundException) {
               null
            }

            TypedInstance.from(targetType, valueAtJsonPath, schema, source = source)
         }
         // Legacy support - old jsonPath as xpath mappings...
         expression.startsWith("/") -> {
            TypedInstance.from(targetType, value[accessor.expression.removePrefix("/")], schema, source = source)
         }

         else -> error("Invalid json path - expected something starting with $ or / but got $expression")

      }


   }

   fun evaluate(
      value: Any,
      returnType: Type,
      expression: Expression,
      schema: Schema = this.schema,
      nullValues: Set<String> = emptySet(),
      dataSource: DataSource,
      format: FormatsAndZoneOffset?,
      resultCache: MutableMap<FunctionResultCacheKey, Any> = mutableMapOf()
   ): TypedInstance {
      return when (expression) {
         is OperatorExpression -> evaluateOperatorExpression(
            returnType,
            expression,
            schema,
            value,
            nullValues,
            dataSource,
            format
         )

         is TypeExpression -> evaluateTypeExpression(expression, schema)
         is FunctionExpression -> evaluateFunctionExpression(
            value,
            returnType,
            expression,
            schema,
            nullValues,
            dataSource,
            resultCache,
            format
         )

         is ExtensionFunctionExpression -> evaluateExtensionFunctionExpression(
            value,
            returnType,
            expression,
            schema,
            nullValues,
            dataSource,
            resultCache,
            format
         )

         is ObjectExpression -> {
            // { id : 1 , title : "Star Wars" }
            //id -> LiteralExpression (value = 1)
            //title -> LiteralExpression (value = "Starr Wars)")
            val typedInstanceMap = expression.expressionMap.map { expression ->
              expression.key to  evaluate(value, schema.type(expression.value.returnType), expression.value, schema, nullValues, dataSource, format, resultCache)
            }.toMap()
            TypedInstance.from(returnType, typedInstanceMap, schema, source = dataSource)
         }
         is LiteralExpression -> TypedInstance.from(returnType, expression.literal.value, schema, source = dataSource)
         is LiteralArray -> {
            require(returnType.isCollection) { "Received a LiteralArray, but the type is not an array type - got ${returnType.name.parameterizedName}"}
            val collectionMembers = expression.members.map { memberExpression ->
               val member = evaluate(value, schema.type(memberExpression.returnType), memberExpression, schema, nullValues, dataSource, format, resultCache)
               member
            }
            TypedCollection.arrayOf(returnType.collectionType!!,collectionMembers, dataSource)
         }
         is LambdaExpression -> evaluateLambdaExpression(
            value,
            returnType,
            expression,
            schema,
            nullValues,
            dataSource,
            format
         )

         is FieldReferenceExpression -> {
            evaluateFieldReference(returnType, expression.selectors, dataSource)
         }

         is ModelAttributeReferenceSelector -> {
            // not sure what to set for allow context querying - seems like we shouldn't
            // for reading one value from another, but there might be a use-case
            readModelAttributeSelector(expression, false, schema)
         }

         is CastExpression -> {
            val uncastExpressionResult =
               evaluate(value, returnType, expression.expression, schema, nullValues, dataSource, format, resultCache)
            val castType = schema.type(expression.type)

            val castValue = TypedInstance.from(
               castType,
               uncastExpressionResult.value,
               schema,
               source = uncastExpressionResult.source
            )
            castValue
         }

         is ArgumentSelector -> readScopedReferenceSelector(value,expression)

         is ProjectingExpression -> {
            evaluateProjectingExpression(
               value = value,
               targetType = returnType,
               accessor = expression,
               schema = schema,
               nullValues = nullValues,
               source = dataSource,
               format = format,
               nullable = true,
               allowContextQuerying = true,
               valueProjector = valueProjector
            )
         }
         is WhenExpression -> {
            WhenBlockEvaluator(this.objectFactory, schema, this)
               .evaluate(value, expression, dataSource, returnType, format)
         }

         else -> TODO("Support for expression type ${expression::class.toString()} is not yet implemented")
      }

   }

   private fun evaluateExtensionFunctionExpression(
      value: Any,
      returnType: Type,
      expression: ExtensionFunctionExpression,
      schema: Schema,
      nullValues: Set<String>,
      dataSource: DataSource,
      resultCache: MutableMap<FunctionResultCacheKey, Any>,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      return evaluateFunctionAccessor(
         value,
         returnType,
         schema,
         expression.functionExpression.function,
         nullValues,
         dataSource,
         resultCache,
         format
      )
   }

   private fun evaluateLambdaExpression(
      value: Any,
      returnType: Type,
      expression: LambdaExpression,
      schema: Schema,
      nullValues: Set<String>,
      dataSource: DataSource,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      // Hmm... gotta use the inputs here somehow, but not sure how right now,
      // since the context will give 'em to us when we need em
      return evaluate(value, returnType, expression.expression, schema, nullValues, dataSource, format)
   }

   private fun evaluateFunctionExpression(
      value: Any,
      returnType: Type,
      expression: FunctionExpression,
      schema: Schema,
      nullValues: Set<String>,
      dataSource: DataSource,
      resultCache: MutableMap<FunctionResultCacheKey, Any>,
      format: FormatsAndZoneOffset?

   ): TypedInstance {
      return evaluateFunctionAccessor(
         value,
         returnType,
         schema,
         expression.function,
         nullValues,
         dataSource,
         resultCache,
         format
      )
   }

   private fun evaluateTypeExpression(expression: TypeExpression, schema: Schema): TypedInstance {
      return objectFactory.getValue(expression.type.qualifiedName.fqn(), queryIfNotFound = true)
   }

   private fun evaluateOperatorExpression(
      returnType: Type,
      expression: OperatorExpression,
      schema: Schema,
      value: Any,
      nullValues: Set<String>,
      dataSource: DataSource,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      val lhsReturnType = getReturnTypeFromExpression(expression.lhs, schema)
      val rhsReturnType = getReturnTypeFromExpression(expression.rhs, schema)
      val lhs = evaluate(
         value,
         lhsReturnType,
         expression.lhs,
         schema,
         nullValues,
         dataSource,
         format
      )

      /**
       * Optimisation to evaluate expression like
       * Boolean Expression 1 && Boolean Expression 2
       * Boolean Expression 1 || Boolean Expression 2
       *
       * For these cases depending on the value of 'Boolean Expression 1' we might not need to evaluate 'Boolean Expression 2'
       */
      if (
         lhsReturnType.taxiType.basePrimitive == PrimitiveType.BOOLEAN &&
         returnType.taxiType == PrimitiveType.BOOLEAN &&
         (expression.operator == FormulaOperator.LogicalAnd || expression.operator == FormulaOperator.LogicalOr)
      ) {
         // our expression is either
         // (Boolean) && (Boolean)
         // OR
         // (Boolean) || (Boolean)
         if (expression.operator == FormulaOperator.LogicalAnd && lhs.value == false) {
            // No need to calculate rhs as our expression is (false) && (Boolean expression)
            return TypedInstance.from(returnType, false, schema, source = lhs.source)
         }

         if (expression.operator == FormulaOperator.LogicalOr && lhs.value == true) {
            // No need to calculate rhs as our expression is (true) || (Boolean expression)
            return TypedInstance.from(returnType, true, schema, source = lhs.source)
         }
      }
      if (expression.operator == FormulaOperator.Coalesce && lhs !is TypedNull) {
         // No need to calculate rhs as we have our value.
         return lhs
      }

      val rhs = evaluate(
         value,
         rhsReturnType,
         expression.rhs,
         schema,
         nullValues,
         dataSource,
         format
      )
      // Doesn't really matter what the rhs value is.  Lhs was null, so return rhs.
      if (expression.operator == FormulaOperator.Coalesce) {
         return rhs
      }

      return OperatorExpressionCalculator.calculate(
         lhs,
         rhs,
         expression.operator,
         returnType,
         expression.asTaxi(),
         schema
      )
   }

   private val calculatorRegistry = CalculatorRegistry()
   private fun getReturnTypeFromExpression(expression: Expression, schema: Schema): Type {
      return when (expression) {
         is TypeExpression -> schema.type(expression.type)
         is FunctionExpression -> schema.type(expression.function.returnType)
         is OperatorExpression -> {
            val lhsType = getReturnTypeFromExpression(expression.lhs, schema)
            val rhsType = getReturnTypeFromExpression(expression.rhs, schema)
            val calculator = calculatorRegistry.getCalculator(expression.operator, listOf(lhsType, rhsType))
               ?: error("No calculator exists to perform operation ${expression.operator} against types ${lhsType.fullyQualifiedName} and ${rhsType.fullyQualifiedName}")
            return calculator.getReturnType(expression.operator, listOf(lhsType, rhsType), schema)
         }

         is LiteralExpression -> return schema.type(expression.literal.returnType)
         else -> return schema.type(expression.returnType)
      }
   }

}
