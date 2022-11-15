package io.vyne.models

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import io.vyne.expressions.OperatorExpressionCalculator
import io.vyne.formulas.CalculatorRegistry
import io.vyne.models.conditional.ConditionalFieldSetEvaluator
import io.vyne.models.csv.CsvAttributeAccessorParser
import io.vyne.models.facts.FactBag
import io.vyne.models.facts.FactDiscoveryStrategy
import io.vyne.models.facts.FactSearch
import io.vyne.models.functions.FunctionRegistry
import io.vyne.models.functions.FunctionResultCacheKey
import io.vyne.models.json.JsonAttributeAccessorParser
import io.vyne.models.xml.XmlTypedInstanceParser
import io.vyne.schemas.*
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Type
import io.vyne.schemas.taxi.toVyneQualifiedName
import io.vyne.utils.log
import io.vyne.utils.timed
import io.vyne.utils.xtimed
import lang.taxi.accessors.*
import lang.taxi.expressions.Expression
import lang.taxi.expressions.FieldReferenceExpression
import lang.taxi.expressions.FunctionExpression
import lang.taxi.expressions.LambdaExpression
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.functions.FunctionAccessor
import lang.taxi.functions.FunctionExpressionAccessor
import lang.taxi.types.*
import lang.taxi.utils.takeHead
import org.apache.commons.csv.CSVRecord
import org.w3c.dom.Document
import kotlin.reflect.KClass

object Parsers {
   val xmlParser: XmlTypedInstanceParser by lazy { XmlTypedInstanceParser() }
   val csvParser: CsvAttributeAccessorParser by lazy { CsvAttributeAccessorParser() }
   val jsonParser: JsonAttributeAccessorParser by lazy { JsonAttributeAccessorParser() }
}


/**
 * Turns a FactBag into a ValueSupplier for evaluating expressions.
 * Lightweight way to provide access to TypedInstances in expression evaluation that's happening
 * outside of object construction (eg., when evaluating an expression inside a function)
 *
 * Supports model scan strategies in FactBag, so asking for a type will return the closest match
 * considering polymorphism
 */
class FactBagValueSupplier(
   private val facts: FactBag,
   private val schema: Schema,
   /**
    * The value supplier to use when evaluating statements scoped with "this".
    * If none passed, an empty value bag is used, so evaluations will fail.
    *
    * Generally, this should be a TypedObjectFactory.
    */
   private val thisScopeValueSupplier: EvaluationValueSupplier = empty(schema),
   val typeMatchingStrategy: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES,

   ) : EvaluationValueSupplier {
   companion object {
      fun of(
         facts: List<TypedInstance>,
         schema: Schema,
         thisScopeValueSupplier: EvaluationValueSupplier = empty(schema),
         typeMatchingStrategy: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES
      ): EvaluationValueSupplier {
         return FactBagValueSupplier(FactBag.of(facts, schema), schema, thisScopeValueSupplier, typeMatchingStrategy)
      }

      fun empty(schema: Schema): EvaluationValueSupplier {
         return of(emptyList(), schema)
      }
   }

   override fun getValue(
      typeName: QualifiedName,
      queryIfNotFound: Boolean,
      allowAccessorEvaluation: Boolean
   ): TypedInstance {
      val type = schema.type(typeName)
      val fact = facts.getFactOrNull(
         FactSearch.findType(
            type,
            strategy = FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE,
            matcher = typeMatchingStrategy
         )
      )
//      val fact = facts.getFactOrNull(type, strategy = FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)
      return fact ?: TypedNull.create(
         type,
         FailedSearch("Type ${typeName.shortDisplayName} was not found in the provided set of facts")
      )
   }

   override fun getValue(attributeName: AttributeName): TypedInstance {
      return thisScopeValueSupplier.getValue(attributeName)
   }

   override fun getScopedFact(scope: ProjectionFunctionScope): TypedInstance {
      return facts.getScopedFact(scope).fact
   }

   override fun readAccessor(type: Type, accessor: Accessor, format: FormatsAndZoneOffset?): TypedInstance {
      TODO("Not yet implemented")
   }

   override fun readAccessor(type: QualifiedName, accessor: Accessor, nullable: Boolean, format: FormatsAndZoneOffset?): TypedInstance {
      TODO("Not yet implemented")
   }
}

/**
 * When evaluating expressions, a thing that can provide values.
 * Generally a TypedObjectFactory
 */
interface EvaluationValueSupplier {
   fun getValue(
      typeName: QualifiedName,
      queryIfNotFound: Boolean = false,
      allowAccessorEvaluation: Boolean = true
   ): TypedInstance

   fun getScopedFact(scope: ProjectionFunctionScope): TypedInstance
   fun getValue(attributeName: AttributeName): TypedInstance
   fun readAccessor(type: Type, accessor: Accessor, format: FormatsAndZoneOffset?): TypedInstance
   fun readAccessor(type: QualifiedName, accessor: Accessor, nullable: Boolean, format: FormatsAndZoneOffset?): TypedInstance
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
   private val functionResultCache: MutableMap<FunctionResultCacheKey, Any> = mutableMapOf()
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
         is JsonPathAccessor -> parseJson(value, targetType, schema, accessor, source)
         is XpathAccessor -> parseXml(value, targetType, schema, accessor, source, nullable)
         is DestructuredAccessor -> parseDestructured(value, targetType, schema, accessor, source, format)
         is ColumnAccessor -> {
            if (accessor.index == null && accessor.defaultValue != null) {
               // This is some tech debt.
               // Default values (defined as by default("foo") turn up as ColumnAccessors.
               readWithDefaultValue(value, targetType, schema, accessor, nullValues, source, nullable)
            } else {
               parseColumnData(value, targetType, schema, accessor, nullValues, source, nullable)
            }

         }

         is ConditionalAccessor -> evaluateConditionalAccessor(value, targetType, schema, accessor, nullValues, source)
         is ReadFunctionFieldAccessor -> evaluateReadFunctionAccessor(
            value,
            targetType,
            schema,
            accessor,
            nullValues,
            source
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
            return TypedInstance.from(targetType, accessor.value, schema, source = dataSource)
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
         is LambdaExpression -> DeferredTypedInstance(accessor, schema, source)
         is OperatorExpression -> evaluateOperatorExpression(targetType, accessor, schema, value, nullValues, source, format)
         is FieldReferenceExpression -> timed("evaluate field reference ${accessor.asTaxi()}") { evaluateFieldReference(
            targetType,
            accessor.selectors,
            source
         )}

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

         is TypeExpression -> readTypeExpression(accessor, allowContextQuerying)
         is ModelAttributeReferenceSelector -> timed("read model Attribute ${accessor.asTaxi()}") { readModelAttributeSelector(accessor, allowContextQuerying, schema) }
         is ScopedReferenceSelector -> readScopedReferenceSelector(accessor)
         else -> {
            TODO("Support for accessor not implemented with type $accessor")
         }
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
      val fact = xtimed("Fact bag search") { FactBag.of(listOf(source), schema)
         .getFactOrNull(requestedType, discoveryStrategy) }
      return fact ?: TypedNull.create(
         requestedType,
         FailedEvaluatedExpression(
            accessor.asTaxi(),
            emptyList(),
            "Unable to find instance of ${requestedType.qualifiedName.shortDisplayName} from source of ${source.type.qualifiedName.shortDisplayName}"
         )
      )
   }

   private fun readTypeExpression(
      accessor: TypeExpression,
      allowContextQuerying: Boolean
   ): TypedInstance {
      val result = objectFactory.getValue(
         accessor.type.toVyneQualifiedName(),
         queryIfNotFound = allowContextQuerying
      )
      return result
   }

   private fun readWithDefaultValue(
      value: Any,
      targetType: Type,
      schema: Schema,
      accessor: ColumnAccessor,
      nullValues: Set<String>,
      source: DataSource,
      nullable: Boolean
   ): TypedInstance {
      val value = objectFactory.getValue(targetType.qualifiedName, allowAccessorEvaluation = false)
      return if (value is TypedNull) {
         TypedInstance.from(targetType, accessor.defaultValue, schema, nullValues = nullValues, source = source)
      } else {
         value
      }
   }

   private fun readScopedReferenceSelector(accessor: ScopedReferenceSelector): TypedInstance {
      val scopedInstance = objectFactory.getScopedFact(accessor.scope)
      val result =  if (accessor.selectors.isNotEmpty()) {
         readFieldSelectorsAgainstObject(accessor.selectors, scopedInstance, schema.type(accessor.returnType), accessor.path)
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
      return readFieldSelectorsAgainstObject(remainingFields, firstObject, targetType, selectors.joinToString(".") { it.fieldName})
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

      val declaredInputs = timed("lookup inputs for function eval") {
         function.parameters.filter { !it.isVarArg }.mapIndexed { index, parameter ->
            require(index < accessor.inputs.size) { "Cannot read parameter ${parameter.description} as no input was provided at index $index" }
            val parameterInputAccessor = accessor.inputs[index]
            val targetParameterType = if (parameter.type is LambdaExpressionType) {
               schema.type((parameter.type as LambdaExpressionType).returnType)
            } else {
               schema.type(parameter.type)
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
      }
      val declaredVarArgs = if (function.parameters.isNotEmpty() && function.parameters.last().isVarArg) {
         val varargFrom = function.parameters.size - 1
         val varargParam = function.parameters.last()
         val varargType = schema.type(varargParam.type)
         val inputs = accessor.inputs.subList(varargFrom, accessor.inputs.size)
         inputs.map { varargInputAccessor ->
            read(value, varargType, varargInputAccessor, schema, nullValues, source, format = format, allowContextQuerying = true)
         }
      } else emptyList()

      val allInputs = declaredInputs + declaredVarArgs

      return functionRegistry.invoke(
         function,
         allInputs,
         schema,
         targetType,
         accessor,
         objectFactory,
         format,
         value,
         resultCache
      )
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
      source: DataSource
   ): TypedInstance {
      if (accessor.readFunction != ReadFunction.CONCAT) {
         error("Only concat is allowed")
      }

      val arguments = accessor.arguments.mapNotNull { readFunctionArgument ->
         if (readFunctionArgument.columnAccessor != null) {
            parseColumnData(value, targetType, schema, readFunctionArgument.columnAccessor!!, nullValues, source).value
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
      nullable: Boolean = false
   ): TypedInstance {
      // TODO : We should really support parsing from a stream, to avoid having to load large sets in memory
      return when {
         value is String -> csvParser.parse(value, targetType, accessor, schema, source, nullable)
         // Efficient parsing where we've already parsed the record once (eg., streaming from disk).
         value is CSVRecord -> csvParser.parseToType(targetType, accessor, value, schema, nullValues, source, nullable)
         accessor.defaultValue != null -> TypedInstance.from(
            targetType,
            accessor.defaultValue.toString(),
            schema,
            nullValues = nullValues,
            source = source
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
         val attributeValue = read(value, objectMemberField.type, accessor, schema, source = source, nullable = false, format = format)
         attributeName to attributeValue
      }.toMap()
      return TypedObject(targetType, values, source)
   }

   private fun parseXml(
      value: Any,
      targetType: Type,
      schema: Schema,
      accessor: XpathAccessor,
      source: DataSource,
      nullable: Boolean
   ): TypedInstance {
      // TODO : We should really support parsing from a stream, to avoid having to load large sets in memory
      return when (value) {
         is String -> xmlParser.parse(value, targetType, accessor, schema, source, nullable)
         // Strictly speaking, we shouldn't be getting maps here.
         // But it's a legacy thing, from when we used xpath(...) all over the shop, even in non xml types
         is Map<*, *> -> TypedInstance.from(
            targetType,
            value[accessor.expression.removePrefix("/")],
            schema,
            source = source
         )

         is Document -> xmlParser.parse(value, targetType, accessor, schema, source, nullable)
         else -> TODO("Value=${value} targetType=${targetType} accessor={$accessor} not supported!")
      }
   }

   private fun parseJson(
      value: Any,
      targetType: Type,
      schema: Schema,
      accessor: JsonPathAccessor,
      source: DataSource
   ): TypedInstance {
      return when (value) {
         is ObjectNode -> jsonParser.parseToType(targetType, accessor, value, schema, source)
         is Map<*, *> -> jsonParser.parseToType(targetType, accessor, value, schema, source)
         else -> TODO("Value=${value} targetType=${targetType} accessor={$accessor} not supported!")
      }
   }

   private fun extractFromMap(
      targetType: Type,
      accessor: JsonPathAccessor,
      value: Map<*, *>,
      schema: Schema,
      source: DataSource
   ): TypedInstance {
      // Strictly speaking, we shouldn't be getting maps here.
      // But it's a legacy thing, from when we used xpath(...) all over the shop, even in non xml types
      val expression = accessor.expression
      return when {
         expression.startsWith("$") -> {
            val valueAtJsonPath = try {
               jsonParser.parseToType(targetType, accessor, value, schema, source)
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

         is LiteralExpression -> TypedInstance.from(returnType, expression.literal.value, schema, source = dataSource)
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

         else -> TODO("Support for expression type ${expression::class.toString()} is not yet implemented")
      }
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
      val rhs = evaluate(
         value,
         rhsReturnType,
         expression.rhs,
         schema,
         nullValues,
         dataSource,
         format
      )
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
