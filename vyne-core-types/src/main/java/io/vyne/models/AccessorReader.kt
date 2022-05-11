package io.vyne.models

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import io.vyne.expressions.OperatorExpressionCalculator
import io.vyne.formulas.CalculatorRegistry
import io.vyne.models.conditional.ConditionalFieldSetEvaluator
import io.vyne.models.csv.CsvAttributeAccessorParser
import io.vyne.models.functions.FunctionRegistry
import io.vyne.models.functions.ReadFunctionFieldEvaluator
import io.vyne.models.json.JsonAttributeAccessorParser
import io.vyne.models.xml.XmlTypedInstanceParser
import io.vyne.schemas.AttributeName
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.TypeMatchingStrategy
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.toVyneQualifiedName
import io.vyne.utils.log
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
import lang.taxi.expressions.Expression
import lang.taxi.expressions.FieldReferenceExpression
import lang.taxi.expressions.FunctionExpression
import lang.taxi.expressions.LambdaExpression
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.functions.FunctionAccessor
import lang.taxi.functions.FunctionExpressionAccessor
import lang.taxi.types.FieldReferenceSelector
import lang.taxi.types.FormulaOperator
import lang.taxi.types.LambdaExpressionType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.TypeReferenceSelector
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
   val typeMatchingStrategy: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES
) : EvaluationValueSupplier {
   companion object {
      fun of(
         facts: List<TypedInstance>,
         schema: Schema,
         typeMatchingStrategy: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES
      ): EvaluationValueSupplier {
         return FactBagValueSupplier(FactBag.of(facts, schema), schema, typeMatchingStrategy)
      }
   }

   override fun getValue(typeName: QualifiedName, queryIfNotFound: Boolean): TypedInstance {
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
      TODO("Not yet implemented")
   }

   override fun readAccessor(type: Type, accessor: Accessor): TypedInstance {
      TODO("Not yet implemented")
   }

   override fun readAccessor(type: QualifiedName, accessor: Accessor, nullable: Boolean): TypedInstance {
      TODO("Not yet implemented")
   }
}

/**
 * When evaluating expressions, a thing that can provide values.
 * Generally a TypedObjectFactory
 */
interface EvaluationValueSupplier {
   fun getValue(typeName: QualifiedName, queryIfNotFound: Boolean = false): TypedInstance
   fun getValue(attributeName: AttributeName): TypedInstance
   fun readAccessor(type: Type, accessor: Accessor): TypedInstance
   fun readAccessor(type: QualifiedName, accessor: Accessor, nullable: Boolean): TypedInstance
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
   private val accessorHandlers: List<AccessorHandler<out Accessor>> = emptyList()
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
   private val readFunctionFieldEvaluator: ReadFunctionFieldEvaluator by lazy { ReadFunctionFieldEvaluator() }
   fun read(
      value: Any,
      targetTypeRef: QualifiedName,
      accessor: Accessor,
      schema: Schema,
      nullValues: Set<String> = emptySet(),
      source: DataSource,
      nullable: Boolean,
      allowContextQuerying: Boolean = false
   ): TypedInstance {
      val targetType = schema.type(targetTypeRef)
      return read(value, targetType, accessor, schema, nullValues, source, nullable, allowContextQuerying)
   }

   fun read(
      value: Any,
      targetType: Type,
      accessor: Accessor,
      schema: Schema,
      nullValues: Set<String> = emptySet(),
      source: DataSource,
      nullable: Boolean = false,
      allowContextQuerying: Boolean = false
   ): TypedInstance {
      if (accessorsByType.containsKey(accessor::class)) {
         val accessorHandler = accessorsByType[accessor::class] as AccessorHandler<in Accessor>
        return accessorHandler.process(accessor, objectFactory, schema, targetType, source)
      }
      return when (accessor) {
         // TODO : Gradually move these accessors out to individual classes to enable better injection / pluggability
         is JsonPathAccessor -> parseJson(value, targetType, schema, accessor, source)
         is XpathAccessor -> parseXml(value, targetType, schema, accessor, source, nullable)
         is DestructuredAccessor -> parseDestructured(value, targetType, schema, accessor, source)
         is ColumnAccessor -> parseColumnData(value, targetType, schema, accessor, nullValues, source, nullable)
         is ConditionalAccessor -> evaluateConditionalAccessor(value, targetType, schema, accessor, nullValues, source)
         is ReadFunctionFieldAccessor -> evaluateReadFunctionAccessor(
            value,
            targetType,
            schema,
            accessor,
            nullValues,
            source
         )
         is FunctionAccessor -> evaluateFunctionAccessor(value, targetType, schema, accessor, nullValues, source)
         is FieldReferenceSelector -> evaluateFieldReference(value, targetType, schema, accessor, nullValues, source)
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
            source
         )
         is TypeReferenceSelector -> objectFactory.getValue(
            accessor.type.toVyneQualifiedName(),
            queryIfNotFound = allowContextQuerying
         )
         is FieldSourceAccessor -> TypedNull.create(targetType, source)
         is LambdaExpression -> DeferredTypedInstance(accessor, schema, source)
         is OperatorExpression -> evaluateOperatorExpression(targetType,accessor,schema,value,nullValues,source)
         is FieldReferenceExpression -> evaluateFieldReference(value,targetType,schema,accessor.selector, nullValues, source)
         is LiteralExpression -> read(value, targetType, accessor.literal, schema, nullValues, source, nullable, allowContextQuerying)
         is FunctionExpression -> read(value, targetType, accessor.function, schema, nullValues, source, nullable, allowContextQuerying)
         is TypeExpression -> objectFactory.getValue(accessor.type.toVyneQualifiedName(), queryIfNotFound = allowContextQuerying)
         else -> {
            TODO("Support for accessor not implemented with type $accessor")
         }
      }
   }

   private fun evaluateFieldReference(
      value: Any,
      targetType: Type,
      schema: Schema,
      accessor: FieldReferenceSelector,
      nullValues: Set<String>,
      source: DataSource
   ): TypedInstance {
      return objectFactory.getValue(accessor.fieldName)
   }

   private fun evaluateFunctionAccessor(
      value: Any,
      targetType: Type,
      schema: Schema,
      accessor: FunctionAccessor,
      nullValues: Set<String>,
      source: DataSource
   ): TypedInstance {
      val function = accessor.function
      // Note - don't check for == here, because of vararg params
      if (accessor.inputs.size < function.parameters.size) {
         error("Function ${function.qualifiedName} expects ${function.parameters.size} arguments, but only ${accessor.inputs.size} were provided")
      }

      val declaredInputs = function.parameters.filter { !it.isVarArg }.mapIndexed { index, parameter ->
         require(index < accessor.inputs.size) { "Cannot read parameter ${parameter.description} as no input was provided at index $index" }
         val parameterInputAccessor = accessor.inputs[index]
         val targetParameterType = if (parameter.type is LambdaExpressionType) {
            schema.type(PrimitiveType.ANY) // TODO...
         } else {
            schema.type(parameter.type)
         }

         // This doesn't feel like the right place to do this, it's really
         // treating this as an edge case, and we shouldn't be.
         // Here, we're saying "if the thing we're trying to build is actually the input into the function, then it's ok to search".
         // No real logic behind that, other than it's what I need to make my test pass.

         val queryIfNotFound = if (targetType.hasExpression && targetType.expression!! is LambdaExpression) {
            val lambdaExpression = targetType.expression as LambdaExpression
            lambdaExpression.inputs.contains(parameterInputAccessor.returnType)
         } else if (targetType.hasExpression) {
            false
         } else {
            false
         }

         // MP, 2-Nov-21: Modifying the rules here where types that are inputs to an expression can be
         // searched for, regardless.  I suspect this will break some stuff.
         // I think the ACTUAL approach to use here is to introduce an operator that indicates "Search for this thing".
         // Also, our search scope should (by default) consider the typed objects in our hand, where at the moment, it doesn't
         // eg: Currently
         // findAll { Foo } as {
         // ... <- Here, the attributes of Foo aren't available by default, but they should be.
         // nested1 : {
         // ... <-- Here, the attributes one layer up aren't available, but they should be.
         // }
         //}
//         val queryIfNotFound = true

         read(
            value,
            targetParameterType,
            parameterInputAccessor,
            schema,
            nullValues,
            source,
            allowContextQuerying = queryIfNotFound
         )
      }

      val declaredVarArgs = if (function.parameters.isNotEmpty() && function.parameters.last().isVarArg) {
         val varargFrom = function.parameters.size - 1
         val varargParam = function.parameters.last()
         val varargType = schema.type(varargParam.type)
         val inputs = accessor.inputs.subList(varargFrom, accessor.inputs.size)
         inputs.map { varargInputAccessor ->
            read(value, varargType, varargInputAccessor, schema, nullValues, source, allowContextQuerying = true)
         }
      } else emptyList()

      val allInputs = declaredInputs + declaredVarArgs

      return functionRegistry.invoke(function, allInputs, schema, targetType, accessor, objectFactory, value)
   }

   private fun evaluateFunctionExpressionAccessor(
      value: Any,
      targetType: Type,
      schema: Schema,
      accessor: FunctionExpressionAccessor,
      nullValues: Set<String>,
      source: DataSource
   ): TypedInstance {
      val functionResult =
         this.evaluateFunctionAccessor(value, targetType, schema, accessor.functionAccessor, nullValues, source)
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
      source: DataSource
   ): TypedInstance {
      val values = accessor.fields.map { (attributeName, accessor) ->
         val objectMemberField = targetType.attribute(attributeName)
         val attributeValue = read(value, objectMemberField.type, accessor, schema, source = source, nullable = false)
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
      dataSource: DataSource
   ): TypedInstance {
      return when (expression) {
         is OperatorExpression -> evaluateOperatorExpression(
            returnType,
            expression,
            schema,
            value,
            nullValues,
            dataSource
         )
         is TypeExpression -> evaluateTypeExpression(expression, schema)
         is FunctionExpression -> evaluateFunctionExpression(
            value,
            returnType,
            expression,
            schema,
            nullValues,
            dataSource
         )
         is LiteralExpression -> TypedInstance.from(returnType, expression.literal.value, schema, source = dataSource)
         is LambdaExpression -> evaluateLambdaExpression(
            value,
            returnType,
            expression,
            schema,
            nullValues,
            dataSource
         )
         is FieldReferenceExpression -> objectFactory.getValue(expression.fieldName)
         else -> TODO("Support for expression type ${expression::class.toString()} is not yet implemented")
      }
   }

   private fun evaluateLambdaExpression(
      value: Any,
      returnType: Type,
      expression: LambdaExpression,
      schema: Schema,
      nullValues: Set<String>,
      dataSource: DataSource
   ): TypedInstance {
      // Hmm... gotta use the inputs here somehow, but not sure how right now,
      // since the context will give 'em to us when we need em
      return evaluate(value, returnType, expression.expression, schema, nullValues, dataSource)
   }

   private fun evaluateFunctionExpression(
      value: Any,
      returnType: Type,
      expression: FunctionExpression,
      schema: Schema,
      nullValues: Set<String>,
      dataSource: DataSource
   ): TypedInstance {
      return evaluateFunctionAccessor(value, returnType, schema, expression.function, nullValues, dataSource)
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
      dataSource: DataSource
   ): TypedInstance {
      val lhs = evaluate(
         value,
         getReturnTypeFromExpression(expression.lhs, schema),
         expression.lhs,
         schema,
         nullValues,
         dataSource
      )
      val rhs = evaluate(
         value,
         getReturnTypeFromExpression(expression.rhs, schema),
         expression.rhs,
         schema,
         nullValues,
         dataSource
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
