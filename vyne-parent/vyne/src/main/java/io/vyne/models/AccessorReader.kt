package io.vyne.models

import io.vyne.schemas.Field
import io.vyne.schemas.Schema
import lang.taxi.types.Accessor
import lang.taxi.types.DestructuredAccessor
import lang.taxi.types.XpathAccessor

class AccessorReader {
   private val xmlParser = XmlTypedInstanceParser()

   fun read(value: Any, field: Field, schema: Schema): TypedInstance {
      if (field.accessor == null) error("Accessor must not be null")
      return read(value, field, schema, field.accessor)
   }

   private fun read(value: Any, field: Field, schema: Schema, accessor: Accessor): TypedInstance {
      return when (accessor) {
         is XpathAccessor -> parseXml(value, field, schema, accessor)
         is DestructuredAccessor -> parseDestructured(value, field, schema, accessor)
         else -> TODO()
      }
   }

   private fun parseDestructured(value: Any, field: Field, schema: Schema, accessor: DestructuredAccessor): TypedInstance {
      val objectType = schema.type(field.type)
      val values = accessor.fields.map { (attributeName, accessor) ->
         val objectMemberField = objectType.attribute(attributeName)
         val attributeValue = read(value, objectMemberField, schema, accessor)
         attributeName to attributeValue
      }.toMap()
      return TypedObject(objectType, values)
   }

   private fun parseXml(value: Any, field: Field, schema: Schema, accessor: XpathAccessor): TypedInstance {
      return when (value) {
         is String -> xmlParser.parse(value, schema.type(field.type), accessor)
         else -> TODO()
      }
   }

}
