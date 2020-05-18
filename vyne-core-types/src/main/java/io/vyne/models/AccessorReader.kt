package io.vyne.models

import com.fasterxml.jackson.databind.node.ObjectNode
import io.vyne.models.csv.CsvAttributeAccessorParser
import io.vyne.models.json.JsonAttributeAccessorParser
import io.vyne.models.xml.XmlTypedInstanceParser
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.TypeReference
import lang.taxi.types.Accessor
import lang.taxi.types.ColumnAccessor
import lang.taxi.types.DestructuredAccessor
import lang.taxi.types.XpathAccessor
import org.apache.commons.csv.CSVRecord

object Parsers {
   val xmlParser: XmlTypedInstanceParser by lazy { XmlTypedInstanceParser() }
   val csvParser: CsvAttributeAccessorParser by lazy { CsvAttributeAccessorParser() }
   val jsonParser: JsonAttributeAccessorParser by lazy { JsonAttributeAccessorParser() }
}

class AccessorReader {
   // There's a cost to building all the Xml junk - so defer if we don't need it,
   // and re-use inbetween readers
   private val xmlParser: XmlTypedInstanceParser by lazy { Parsers.xmlParser }
   private val csvParser: CsvAttributeAccessorParser by lazy { Parsers.csvParser }
   private val jsonParser: JsonAttributeAccessorParser by lazy { Parsers.jsonParser }

   fun read(value: Any, targetTypeRef: TypeReference, accessor: Accessor, schema: Schema): TypedInstance {
      val targetType = schema.type(targetTypeRef)
      return read(value, targetType, accessor, schema)
   }

   fun read(value: Any, targetType: Type, accessor: Accessor, schema: Schema): TypedInstance {
      return when (accessor) {
         is XpathAccessor -> parseXml(value, targetType, schema, accessor)
         is DestructuredAccessor -> parseDestructured(value, targetType, schema, accessor)
         is ColumnAccessor -> parseColumnData(value, targetType, schema, accessor)
         else -> TODO()
      }
   }

   private fun parseColumnData(value: Any, targetType: Type, schema: Schema, accessor: ColumnAccessor): TypedInstance {
      // TODO : We should really support parsing from a stream, to avoid having to load large sets in memory
      return when (value) {
         is String -> csvParser.parse(value, targetType, accessor, schema)
         // Efficient parsing where we've already parsed the record once (eg., streaming from disk).
         is CSVRecord -> csvParser.parseToType(targetType, accessor, value, schema)
         else -> TODO()
      }
   }

   private fun parseDestructured(value: Any, targetType: Type, schema: Schema, accessor: DestructuredAccessor): TypedInstance {
      val values = accessor.fields.map { (attributeName, accessor) ->
         val objectMemberField = targetType.attribute(attributeName)
         val attributeValue = read(value, objectMemberField.type, accessor, schema)
         attributeName to attributeValue
      }.toMap()
      return TypedObject(targetType, values)
   }

   private fun parseXml(value: Any, targetType: Type, schema: Schema, accessor: XpathAccessor): TypedInstance {
      // TODO : We should really support parsing from a stream, to avoid having to load large sets in memory
      return when (value) {
         is String -> xmlParser.parse(value, targetType, accessor, schema)
         is ObjectNode -> jsonParser.parseToType(targetType, accessor, value, schema)
         else -> TODO("Value=${value} targetType=${targetType} accessor={$accessor} not supported!")
      }
   }

}
