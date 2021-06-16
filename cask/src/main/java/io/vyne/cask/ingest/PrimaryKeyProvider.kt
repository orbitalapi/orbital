package io.vyne.cask.ingest

import io.vyne.cask.ddl.PostgresDdlGenerator
import lang.taxi.types.ObjectType
import lang.taxi.types.Type

object PrimaryKeyProvider {
   private const val PrimaryKeyAnnotationName = "PrimaryKey"

   /**
    * Returns the primary key columns for the given taxi Type.
    */
   fun primaryKeyColumnsFor(caskTaxiType: Type): List<String> {
      val objectType = caskTaxiType as? ObjectType
      val primaryKeyColumns = objectType?.let { type ->
         type.definition?.fields
            ?.filter { field ->
               field.annotations.map { it.name }.contains(PrimaryKeyAnnotationName)
            }
      }?.map { it.name }
      return primaryKeyColumns ?: listOf(PostgresDdlGenerator.CASK_ROW_ID_COLUMN_NAME)
   }

   fun hasPrimaryKey(type: ObjectType): Boolean {
      return type.definition?.fields
         ?.flatMap { it -> it.annotations }
         ?.any { a -> a.name == PrimaryKeyAnnotationName } ?: false
   }
}
