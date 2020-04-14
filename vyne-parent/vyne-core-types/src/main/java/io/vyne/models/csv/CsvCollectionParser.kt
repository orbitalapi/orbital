package io.vyne.models.csv

import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.models.TypedObjectFactory
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Field
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.types.ColumnAccessor
import org.apache.commons.csv.CSVRecord

class CsvCollectionParser(val content: String, val type: Type, val schema: Schema) {
   private val memberType: Type

   init {
      require(this.type.isCollection) { "The passed type should be a collection type" }
      require(this.type.typeParameters.size == 1) { "The collection type should contain exactly 1 type param" }
      this.memberType = this.type.typeParameters.first()
   }

   fun parse(): TypedInstance {
      val typedInstances = content.lineSequence()
         .drop(1) // Ignore the header
         .filter { it.isNotBlank() && it.isNotEmpty() }
         .map { TypedObjectFactory(memberType,it,schema).build() }
         .toList()
      return TypedCollection.from(typedInstances)
   }

}
