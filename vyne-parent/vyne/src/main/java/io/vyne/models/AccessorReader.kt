package io.vyne.models

import io.vyne.schemas.Field
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.TypeReference
import lang.taxi.types.Accessor
import lang.taxi.types.DestructuredAccessor
import lang.taxi.types.XpathAccessor

class AccessorReader {
   private val xmlParser = XmlTypedInstanceParser()

   fun read(value: Any, targetTypeRef: TypeReference, accessor: Accessor, schema: Schema): TypedInstance {
      val targetType = schema.type(targetTypeRef)
      return read(value, targetType, accessor, schema)
   }

   fun read(value: Any, targetType: Type,  accessor: Accessor, schema: Schema): TypedInstance {
      return when (accessor) {
         is XpathAccessor -> parseXml(value, targetType, schema, accessor)
         is DestructuredAccessor -> parseDestructured(value, targetType, schema, accessor)
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
      return when (value) {
         is String -> xmlParser.parse(value, targetType, accessor)
         else -> TODO()
      }
   }

}
