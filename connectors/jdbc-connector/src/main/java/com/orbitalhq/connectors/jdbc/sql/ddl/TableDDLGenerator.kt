package com.orbitalhq.connectors.jdbc.sql.ddl

import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.JdbcConnectorTaxi
import com.orbitalhq.connectors.jdbc.SqlUtils
import com.orbitalhq.connectors.jdbc.drivers.DatabaseSupport
import com.orbitalhq.connectors.jdbc.sql.ddl.TaxiTypeToJooqType.PkSuffix
import com.orbitalhq.connectors.jdbc.sqlBuilder
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import lang.taxi.types.*
import lang.taxi.types.EnumType
import mu.KotlinLogging
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.DSL.*
import org.jooq.impl.SQLDataType

/**
 * Generates, and optionally executes a CREATE IF NOT EXISTS
 * statement for the target type
 */
class TableGenerator(private val schema: Schema, private val databaseSupport: DatabaseSupport) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun execute(type: Type, dsl: DSLContext, tableNameSuffix: String? = null): Int {
      val (_, statement, indexes) = generate(type, dsl)
      val result = statement.execute()
      indexes.forEach { it.execute() }
      return result
   }

   fun generate(
      type: Type,
      dsl: DSLContext,
      tableNameSuffix: String? = null,
      providedTableName: String? = null
   ): TableDDLData {
      // see tableNameOrTypeName() for justification of .lowercase()
      val tableName = providedTableName ?: SqlUtils.tableNameOrTypeName(type.taxiType, tableNameSuffix)

      val columns = type.attributes.map { (attributeName, typeField) ->
         val taxiType = typeField.resolveType(schema).taxiType
         val sqlType = TaxiTypeToJooqType.getSqlType(taxiType, nullable = typeField.nullable)
            .let { dataType ->
               if (typeField.hasMetadata(JdbcConnectorTaxi.Annotations.GeneratedIdAnnotationName.fqn())) {
                  databaseSupport.addAutoGeneration(dataType, taxiType)
               } else {
                  dataType
               }

            }
         field(DSL.name(attributeName), sqlType)
      }

      val attributesWithIdAnnotation = type.getAttributesWithAnnotation("Id".fqn())
         .map { it.key }

      val attributesWithIndexAnnotation = type.getAttributesWithAnnotation("Index".fqn())
      val indexedFields = columns.filter { column -> attributesWithIndexAnnotation.contains(column.name) }

      val primaryKeyFields = columns
         .filter { column -> attributesWithIdAnnotation.contains(column.name) }

      val primaryKeyConstraints = when (primaryKeyFields.size) {
         0 -> emptyList<Constraint>()
         else -> listOf(constraint("${tableName}$PkSuffix").primaryKey(*primaryKeyFields.toTypedArray()))
      }

      val indexStatements = indexedFields.map { indexedField ->
         dsl.createIndexIfNotExists("$tableName${indexedField.name}_ix").on(table(name(tableName)), indexedField)
      }

      val sqlDsl = dsl.createTableIfNotExists(name(tableName))
         .columns(columns)
         .constraints(primaryKeyConstraints)

      return TableDDLData(tableName, sqlDsl, indexStatements)
   }

   /**
    * Generates a create table statement, without requiring a db connection.
    *  This isn't directly executable, as it's
    * disconnected from the underlying data connection.
    * Useful for testing
    */
   fun generateStatementOnly(
      type: Type,
      connectionDetails: JdbcUrlCredentialsConnectionConfiguration
   ): CreateTableFinalStep {
      return generate(type, connectionDetails.sqlBuilder()).ddlStatement
   }

   }

data class TableDDLData(
   val tableName: String,
   val ddlStatement: CreateTableFinalStep,
   val indexStatements: List<CreateIndexIncludeStep>
)
