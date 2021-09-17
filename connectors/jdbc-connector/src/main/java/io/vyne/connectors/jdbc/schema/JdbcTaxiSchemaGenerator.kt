package io.vyne.connectors.jdbc.schema

import io.vyne.connectors.jdbc.JdbcTable
import io.vyne.utils.log
import lang.taxi.TaxiDocument
import lang.taxi.generators.SchemaWriter
import lang.taxi.jvm.common.PrimitiveTypes
import lang.taxi.types.Annotation
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.ObjectTypeDefinition
import lang.taxi.types.Type
import schemacrawler.schema.Catalog
import schemacrawler.schema.Column
import schemacrawler.schema.ColumnDataType
import schemacrawler.schema.Table

class JdbcTaxiSchemaGenerator(
   val catalog: Catalog,
   val namespace: String,
   val schemaWriter: SchemaWriter = SchemaWriter()
) {
   private val fieldTypes = mutableMapOf<Column, Type>()
   private val models = mutableMapOf<Table, ObjectType>()
   fun buildSchema(tables: List<JdbcTable>): List<String> {
      val createdModels = tables.mapNotNull { table ->
         val tableMetadata = catalog.tables.singleOrNull { tableMetadata ->
            tableMetadata.name.equals(
               table.tableName,
               ignoreCase = true
            )
         }
         if (tableMetadata == null) {
            log().warn("Can't generate a schema for table $table as it wasn't found in the database")
            return@mapNotNull null
         }

         val fields = tableMetadata.columns.map { column ->
            val annotations = if (column.isPartOfPrimaryKey) {
               listOf(Annotation("Id"))
            } else {
               emptyList()
            }
            Field(
               name = column.name,
               type = getType(tableMetadata, column),
               nullable = column.isNullable,
               annotations = annotations,
               compilationUnit = CompilationUnit.unspecified()
            )
         }
         tableMetadata to ObjectType(
            // ie: com.foo.actor.Actor
            "$namespace.${tableMetadata.name.toTaxiConvention(firstLetterAsUppercase = false)}.${
               tableMetadata.name.toTaxiConvention(
                  firstLetterAsUppercase = true
               )
            }",
            ObjectTypeDefinition(
               fields.toSet(),
               compilationUnit = CompilationUnit.unspecified()
            )
         )
      }
      models.putAll(createdModels)
      val doc = TaxiDocument(types = (fieldTypes.values + models.values).toSet(), services = emptySet())
      return schemaWriter.generateSchemas(listOf(doc))
   }

   private fun getType(tableMetadata: Table, column: Column, followForeignKey:Boolean = true): Type {

      return if (!column.isPartOfPrimaryKey && column.isPartOfForeignKey && followForeignKey) {
         return getType(
            column.referencedColumn.parent,
            column.referencedColumn,
            // If the column is part of another foreign key on the other side, it
            // doesn't matter, that's the type we want.
            followForeignKey = false
         )
      } else {
         fieldTypes.getOrPut(column) {
            ObjectType(
               "$namespace.${tableMetadata.name.toTaxiConvention(firstLetterAsUppercase = false)}.${
                  column.name.toTaxiConvention(
                     firstLetterAsUppercase = true
                  )
               }",
               ObjectTypeDefinition(
                  inheritsFrom = setOf(getBasePrimitive(column.type)),
                  compilationUnit = CompilationUnit.unspecified()
               )
            )
         }
      }
   }

   private fun getBasePrimitive(type: ColumnDataType): Type {
      val defaultMappedClass = type.javaSqlType.defaultMappedClass
      return if (PrimitiveTypes.isClassTaxiPrimitive(defaultMappedClass)) {
         PrimitiveTypes.getTaxiPrimitive(defaultMappedClass)
      } else if (JdbcTypes.contains(defaultMappedClass)) {
         JdbcTypes.get(defaultMappedClass)
      } else {
         error("Type ${type.name} default maps to ${defaultMappedClass.canonicalName} which has no taxi equivalent")
      }
      TODO("Not yet implemented")
   }
}

private fun String.toTaxiConvention(firstLetterAsUppercase: Boolean): String {
   val parts = this.split("_", "-", ".")
   return parts.mapIndexed { index, word ->
      if (index == 0 && !firstLetterAsUppercase) {
         word.toLowerCase()
      } else {
         word.toLowerCase().capitalize()
      }
   }.joinToString("")
}
