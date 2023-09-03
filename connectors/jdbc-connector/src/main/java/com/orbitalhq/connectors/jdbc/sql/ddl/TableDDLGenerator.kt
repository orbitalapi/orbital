package com.orbitalhq.connectors.jdbc.sql.ddl

import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.JdbcConnectorTaxi
import com.orbitalhq.connectors.jdbc.SqlUtils
import com.orbitalhq.connectors.jdbc.sql.ddl.TableGenerator.TaxiTypeToJooqType.PkSuffix
import com.orbitalhq.connectors.jdbc.sqlBuilder
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import lang.taxi.jvm.common.PrimitiveTypes
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
class TableGenerator(private val schema: Schema) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun execute(type: Type, dsl: DSLContext, tableNameSuffix: String? = null): Int {
      val (_, statement, indexes) = generate(type, dsl, tableNameSuffix)
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
      val tableName = providedTableName?.lowercase() ?: SqlUtils.tableNameOrTypeName(type.taxiType, tableNameSuffix)

      val columns = type.attributes.map { (attributeName, typeField) ->
         val taxiType = typeField.resolveType(schema).taxiType
         val sqlType = TaxiTypeToJooqType.getSqlType(taxiType, nullable = typeField.nullable)
            .let { dataType ->
               if (typeField.hasMetadata(JdbcConnectorTaxi.Annotations.GeneratedIdAnnotationName.fqn())) {
                  addAutoGeneration(dataType, taxiType)
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

   private fun addAutoGeneration(dataType: DataType<out Any>, taxiType: lang.taxi.types.Type): DataType<out Any> {
      return if (PrimitiveType.isNumberType(taxiType)) {
         (dataType as DataType<Int>).identity(true)
      } else {
         logger.warn { "Auto generation is not supported in primitive types of ${taxiType.toQualifiedName().typeName}" }
         dataType
      }
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

   object TaxiTypeToJooqType {
      private val logger = KotlinLogging.logger {}
      const val PkSuffix = "-pk"
      fun getSqlType(type: lang.taxi.types.Type, nullable: Boolean): DataType<out Any> {
         return getSqlType(type).nullable(nullable)
      }

      private fun getSqlType(type: lang.taxi.types.Type): DataType<out Any> {
         return when {
            type is TypeAlias && type.inheritsFromPrimitive -> getSqlType(type.basePrimitive!!)
            type is PrimitiveType -> getSqlType(type)
            type is ArrayType -> SQLDataType.OTHER.arrayDataType
            type is EnumType -> SQLDataType.VARCHAR // TODO : Generate enum types
            type is ObjectType && type.fields.isNotEmpty() -> error("Only scalar types are supported.  ${type.qualifiedName} defines fields")
            type is ObjectType -> {
               require(type.basePrimitive != null) { "Type ${type.qualifiedName} is scalar, but does not have a primitive type.  This is unexpected" }
               getSqlType(type.basePrimitive!!)
            }

            else -> error("Add support for Taxi type ${type::class.simpleName}")
         }
      }

      private fun getSqlType(type: PrimitiveType): DataType<out Any> {
         return if (taxiToJooq.containsKey(type.basePrimitive)) {
            taxiToJooq[type.basePrimitive]!!
         } else {
            logger.warn { "No mapping defined between primitive type ${type.basePrimitive!!.name} and a sql type.  There should be - defaulting to VARCHAR" }
            SQLDataType.VARCHAR
         }
      }

      private val taxiToJooq = mapOf(
         PrimitiveType.ANY to SQLDataType.VARCHAR,
         PrimitiveType.BOOLEAN to SQLDataType.BOOLEAN,
         PrimitiveType.DATE_TIME to SQLDataType.OFFSETDATETIME,
         PrimitiveType.DECIMAL to SQLDataType.DECIMAL,
         PrimitiveType.DOUBLE to SQLDataType.DOUBLE,
         PrimitiveType.INTEGER to SQLDataType.INTEGER,
         PrimitiveType.INSTANT to SQLDataType.INSTANT,
         PrimitiveType.STRING to SQLDataType.VARCHAR,
         PrimitiveType.TIME to SQLDataType.TIME,
         PrimitiveType.LOCAL_DATE to SQLDataType.LOCALDATE
      )

   }
}

data class TableDDLData(
   val tableName: String,
   val ddlStatement: CreateTableFinalStep,
   val indexStatements: List<CreateIndexIncludeStep>
)