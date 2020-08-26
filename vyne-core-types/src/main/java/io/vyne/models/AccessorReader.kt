package io.vyne.models

import com.fasterxml.jackson.databind.node.ObjectNode
import io.vyne.models.conditional.ConditionalFieldSetEvaluator
import io.vyne.models.csv.CsvAttributeAccessorParser
import io.vyne.models.json.JsonAttributeAccessorParser
import io.vyne.models.xml.XmlTypedInstanceParser
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.TypeReference
import lang.taxi.types.*
import org.apache.commons.csv.CSVRecord
import java.lang.StringBuilder

object Parsers {
   val xmlParser: XmlTypedInstanceParser by lazy { XmlTypedInstanceParser() }
   val csvParser: CsvAttributeAccessorParser by lazy { CsvAttributeAccessorParser() }
   val jsonParser: JsonAttributeAccessorParser by lazy { JsonAttributeAccessorParser() }
}

class AccessorReader(private val objectFactory: TypedObjectFactory) {
   // There's a cost to building all the Xml junk - so defer if we don't need it,
   // and re-use inbetween readers
   private val xmlParser: XmlTypedInstanceParser by lazy { Parsers.xmlParser }
   private val csvParser: CsvAttributeAccessorParser by lazy { Parsers.csvParser }
   private val jsonParser: JsonAttributeAccessorParser by lazy { Parsers.jsonParser }
   private val conditionalFieldSetEvaluator: ConditionalFieldSetEvaluator by lazy { ConditionalFieldSetEvaluator(objectFactory) }
   private val readFunctionFieldEvaluator: ReadFunctionFieldEvaluator by lazy { ReadFunctionFieldEvaluator(objectFactory) }
   fun read(value: Any, targetTypeRef: QualifiedName, accessor: Accessor, schema: Schema, nullValues: Set<String> = emptySet(), source: DataSource, nullable: Boolean): TypedInstance {
      val targetType = schema.type(targetTypeRef)
      return read(value, targetType, accessor, schema, nullValues, source, nullable)
   }

   fun read(value: Any, targetType: Type, accessor: Accessor, schema: Schema, nullValues: Set<String> = emptySet(), source: DataSource, nullable: Boolean = false): TypedInstance {
      return when (accessor) {
         is JsonPathAccessor -> parseJson(value, targetType, schema, accessor, source)
         is XpathAccessor -> parseXml(value, targetType, schema, accessor, source)
         is DestructuredAccessor -> parseDestructured(value, targetType, schema, accessor, source)
         is ColumnAccessor -> parseColumnData(value, targetType, schema, accessor, nullValues, source, nullable)
         is ConditionalAccessor -> evaluateConditionalAccessor(value, targetType, schema, accessor, nullValues, source)
         is ReadFunctionFieldAccessor -> evaluateReadFunctionAccessor(value, targetType, schema, accessor, nullValues, source)
         else -> TODO()
      }
   }

   private fun evaluateReadFunctionAccessor(value: Any, targetType: Type, schema: Schema, accessor: ReadFunctionFieldAccessor, nullValues: Set<String>, source: DataSource): TypedInstance {
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

   private fun evaluateConditionalAccessor(value: Any, targetType: Type, schema: Schema, accessor: ConditionalAccessor, nullValues: Set<String>, source: DataSource): TypedInstance {
      return conditionalFieldSetEvaluator.evaluate(accessor.expression, targetType)
   }

   private fun parseColumnData(value: Any, targetType: Type, schema: Schema, accessor: ColumnAccessor, nullValues: Set<String> = emptySet(), source: DataSource, nullable: Boolean = false): TypedInstance {
      // TODO : We should really support parsing from a stream, to avoid having to load large sets in memory
      return when (value) {
         is String -> csvParser.parse(value, targetType, accessor, schema, source, nullable)
         // Efficient parsing where we've already parsed the record once (eg., streaming from disk).
         is CSVRecord -> csvParser.parseToType(targetType, accessor, value, schema, nullValues, source, nullable)
         else -> TODO()
      }
   }

   private fun parseDestructured(value: Any, targetType: Type, schema: Schema, accessor: DestructuredAccessor, source: DataSource): TypedInstance {
      val values = accessor.fields.map { (attributeName, accessor) ->
         val objectMemberField = targetType.attribute(attributeName)
         val attributeValue = read(value, objectMemberField.type, accessor, schema, source = source, nullable = false)
         attributeName to attributeValue
      }.toMap()
      return TypedObject(targetType, values, source)
   }

   private fun parseXml(value: Any, targetType: Type, schema: Schema, accessor: XpathAccessor, source: DataSource): TypedInstance {
      // TODO : We should really support parsing from a stream, to avoid having to load large sets in memory
      return when (value) {
         is String -> xmlParser.parse(value, targetType, accessor, schema, source)
         // Strictly speaking, we shouldn't be getting maps here.
         // But it's a legacy thing, from when we used xpath(...) all over the shop, even in non xml types
         is Map<*, *> -> TypedInstance.from(targetType, value[accessor.expression.removePrefix("/")], schema, source = source)
         else -> TODO("Value=${value} targetType=${targetType} accessor={$accessor} not supported!")
      }
   }

   private fun parseJson(value: Any, targetType: Type, schema: Schema, accessor: JsonPathAccessor, source: DataSource): TypedInstance {
      return when (value) {
         is ObjectNode -> jsonParser.parseToType(targetType, accessor, value, schema, source)
         is Map<*,*> -> extractFromMap(targetType,accessor,value,schema,source)
         else -> TODO("Value=${value} targetType=${targetType} accessor={$accessor} not supported!")
      }
   }

   private fun extractFromMap(targetType: Type, accessor: JsonPathAccessor, value: Map<*, *>, schema: Schema, source: DataSource): TypedInstance {
      // Strictly speaking, we shouldn't be getting maps here.
      // But it's a legacy thing, from when we used xpath(...) all over the shop, even in non xml types
      return TypedInstance.from(targetType, value[accessor.expression.removePrefix("/")], schema, source = source)

   }

}
