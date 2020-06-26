package io.vyne.models.csv

import io.vyne.models.*
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Field
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.types.ColumnAccessor
import org.apache.commons.csv.CSVRecord

class CsvCollectionParser(val content: String, val type: Type, val schema: Schema, val source:DataSource) {
   private val memberType: Type

   init {
      require(this.type.isCollection) { "The passed type should be a collection type" }
      require(this.type.resolveAliases().typeParameters.size == 1) { "The collection type should contain exactly 1 type param" }
      this.memberType = this.type.resolveAliases().typeParameters.first()
   }

   fun parse(): TypedInstance {
      val typedInstances = content.lineSequence()
         .drop(1) // Ignore the header
         .filter { it.isNotBlank() && it.isNotEmpty() }
         .map { TypedObjectFactory(memberType,it,schema, source = source).build() }
         .toList()
      return TypedCollection.from(typedInstances)
   }

}
