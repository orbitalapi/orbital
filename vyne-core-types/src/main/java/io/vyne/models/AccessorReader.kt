package io.vyne.models

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import io.vyne.models.conditional.ConditionalFieldSetEvaluator
import io.vyne.models.csv.CsvAttributeAccessorParser
import io.vyne.models.functions.FunctionRegistry
import io.vyne.models.functions.ReadFunctionFieldEvaluator
import io.vyne.models.json.JsonAttributeAccessorParser
import io.vyne.models.xml.XmlTypedInstanceParser
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.functions.FunctionAccessor
import lang.taxi.functions.FunctionExpressionAccessor
import lang.taxi.types.Accessor
import lang.taxi.types.ColumnAccessor
import lang.taxi.types.ConditionalAccessor
import lang.taxi.types.DestructuredAccessor
import lang.taxi.types.FieldReferenceSelector
import lang.taxi.types.FormulaOperator
import lang.taxi.types.JsonPathAccessor
import lang.taxi.types.LiteralAccessor
import lang.taxi.types.ReadFunction
import lang.taxi.types.ReadFunctionFieldAccessor
import lang.taxi.types.XpathAccessor
import org.apache.commons.csv.CSVRecord
import org.w3c.dom.Document

object Parsers {
   val xmlParser: XmlTypedInstanceParser by lazy { XmlTypedInstanceParser() }
   val csvParser: CsvAttributeAccessorParser by lazy { CsvAttributeAccessorParser() }
   val jsonParser: JsonAttributeAccessorParser by lazy { JsonAttributeAccessorParser() }
}

class AccessorReader(private val objectFactory: TypedObjectFactory, private val functionRegistry: FunctionRegistry) {
   // There's a cost to building all the Xml junk - so defer if we don't need it,
   // and re-use inbetween readers
   private val xmlParser: XmlTypedInstanceParser by lazy { Parsers.xmlParser }
   private val csvParser: CsvAttributeAccessorParser by lazy { Parsers.csvParser }
   private val jsonParser: JsonAttributeAccessorParser by lazy { Parsers.jsonParser }
   private val conditionalFieldSetEvaluator: ConditionalFieldSetEvaluator by lazy {
      ConditionalFieldSetEvaluator(
         objectFactory
      )
   }
   private val readFunctionFieldEvaluator: ReadFunctionFieldEvaluator by lazy { ReadFunctionFieldEvaluator(objectFactory) }
   fun read(
      value: Any,
      targetTypeRef: QualifiedName,
      accessor: Accessor,
      schema: Schema,
      nullValues: Set<String> = emptySet(),
      source: DataSource,
      nullable: Boolean
   ): TypedInstance {
      val targetType = schema.type(targetTypeRef)
      return read(value, targetType, accessor, schema, nullValues, source, nullable)
   }

   fun read(
      value: Any,
      targetType: Type,
      accessor: Accessor,
      schema: Schema,
      nullValues: Set<String> = emptySet(),
      source: DataSource,
      nullable: Boolean = false
   ): TypedInstance {
      return when (accessor) {
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
         is LiteralAccessor -> return TypedInstance.from(targetType, accessor.value, schema, source = source)
         is FunctionExpressionAccessor -> evaluateFunctionExpressionAccessor(
            value,
            targetType,
            schema,
            accessor,
            nullValues,
            source
         )
         else -> {
            log().warn("Unexpected Accessor value $accessor")
            TODO()
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
         val targetParameterType = schema.type(parameter.type)
         read(value, targetParameterType, parameterInputAccessor, schema, nullValues, source)
      }

      val declaredVarArgs = if (function.parameters.isNotEmpty() && function.parameters.last().isVarArg) {
         val varargFrom = function.parameters.size - 1
         val varargParam = function.parameters.last()
         val varargType = schema.type(varargParam.type)
         val inputs = accessor.inputs.subList(varargFrom, accessor.inputs.size)
         inputs.map { varargInputAccessor ->
            read(value, varargType, varargInputAccessor, schema, nullValues, source)
         }
      } else emptyList()

      val allInputs = declaredInputs + declaredVarArgs

      return functionRegistry.invoke(function, allInputs, schema, targetType)
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
      return conditionalFieldSetEvaluator.evaluate(accessor.expression, targetType)
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

}
