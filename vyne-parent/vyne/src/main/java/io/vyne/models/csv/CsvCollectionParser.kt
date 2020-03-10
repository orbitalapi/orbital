package io.vyne.models.csv

import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Field
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.types.ColumnAccessor
import org.apache.commons.csv.CSVRecord

class CsvCollectionParser(val content: String, val type: Type, val schema: Schema) {
   private var fieldsToParse: Map<AttributeName, Field>
   private val memberType: Type

   init {
      require(this.type.isCollection) { "The passed type should be a collection type" }
      require(this.type.typeParameters.size == 1) { "The collection type should contain exactly 1 type param" }
      this.memberType = this.type.typeParameters.first()

      this.fieldsToParse = this.memberType.attributes.mapNotNull { (name, field) ->
         if (field.accessor is ColumnAccessor) {
            name to field
         } else {
            log().warn("Attribute $name does not have a ColumnAccessor defined, so will not be parsed")
            null
         }
      }.toMap()
   }

   private val cache = CsvDocumentCacheBuilder.newCache()
   private val attriubteParser: CsvAttributeAccessorParser = CsvAttributeAccessorParser(documentCache = this.cache)

   fun parse(): TypedInstance {
      val records = cache.get(content)
      val typedInstances = records.map { parseRecord(it) }
      return TypedCollection.from(typedInstances)
   }

   private fun parseRecord(record: CSVRecord): TypedInstance {
      val parsedAttributes = this.fieldsToParse.map { (name, field) ->
         name to attriubteParser.parseToType(schema.type(field.type), field.accessor as ColumnAccessor, record)
      }.toMap()
      return TypedObject.fromAttributes(memberType,parsedAttributes,schema)
   }
}
