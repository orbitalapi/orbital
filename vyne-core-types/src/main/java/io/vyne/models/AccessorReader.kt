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
   fun read(value: Any, targetTypeRef: QualifiedName, accessor: Accessor, schema: Schema, nullValues: Set<String> = emptySet(), source: DataSource): TypedInstance {
      val targetType = schema.type(targetTypeRef)
      return read(value, targetType, accessor, schema, nullValues, source)
   }

   fun read(value: Any, targetType: Type, accessor: Accessor, schema: Schema, nullValues: Set<String> = emptySet(), source: DataSource): TypedInstance {
      return when (accessor) {
         is JsonPathAccessor -> parseJson(value, targetType, schema, accessor, source)
         is XpathAccessor -> parseXml(value, targetType, schema, accessor, source)
         is DestructuredAccessor -> parseDestructured(value, targetType, schema, accessor, source)
         is ColumnAccessor -> parseColumnData(value, targetType, schema, accessor, nullValues, source)
         is ConditionalAccessor -> evaluateConditionalAccessor(value, targetType, schema, accessor, nullValues, source)
         else -> TODO()
      }
   }

   private fun evaluateConditionalAccessor(value: Any, targetType: Type, schema: Schema, accessor: ConditionalAccessor, nullValues: Set<String>, source: DataSource): TypedInstance {
      return conditionalFieldSetEvaluator.evaluate(accessor.condition, targetType)
   }

   private fun parseColumnData(value: Any, targetType: Type, schema: Schema, accessor: ColumnAccessor, nullValues: Set<String> = emptySet(), source: DataSource): TypedInstance {
      // TODO : We should really support parsing from a stream, to avoid having to load large sets in memory
      return when (value) {
         is String -> csvParser.parse(value, targetType, accessor, schema, source)
         // Efficient parsing where we've already parsed the record once (eg., streaming from disk).
         is CSVRecord -> csvParser.parseToType(targetType, accessor, value, schema, nullValues, source)
         else -> TODO()
      }
   }

   private fun parseDestructured(value: Any, targetType: Type, schema: Schema, accessor: DestructuredAccessor, source: DataSource): TypedInstance {
      val values = accessor.fields.map { (attributeName, accessor) ->
         val objectMemberField = targetType.attribute(attributeName)
         val attributeValue = read(value, objectMemberField.type, accessor, schema, source = source)
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
