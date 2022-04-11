package io.vyne.connectors.jdbc.sql.ddl

import io.vyne.connectors.jdbc.JdbcUrlCredentialsConnectionConfiguration
import io.vyne.connectors.jdbc.SqlUtils
import io.vyne.connectors.jdbc.sqlBuilder
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import lang.taxi.types.ArrayType
import lang.taxi.types.EnumType
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import mu.KotlinLogging
import org.jooq.Constraint
import org.jooq.CreateTableFinalStep
import org.jooq.DSLContext
import org.jooq.DataType
import org.jooq.impl.DSL.constraint
import org.jooq.impl.DSL.field
import org.jooq.impl.SQLDataType

typealias TableName = String
/**
 * Generates, and optionally executes a CREATE IF NOT EXISTS
 * statement for the target type
 */
class TableGenerator(private val schema: Schema) {
   private val logger = KotlinLogging.logger {}
   fun execute(type: Type, dsl: DSLContext): Int {
      val (tableName, statement) = generate(type, dsl)
      return statement.execute()
   }

   fun generate(type: Type, dsl: DSLContext): Pair<TableName, CreateTableFinalStep> {
      val tableName = SqlUtils.tableNameOrTypeName(type.taxiType)

      val columns = type.attributes.map { (attributeName, typeField) ->
         val sqlType =
            TaxiTypeToJooqType.getSqlType(schema.type(typeField.type).taxiType, nullable = typeField.nullable)
         field(attributeName, sqlType)
      }

      val idFields = type.getAttributesWithAnnotation("Id".fqn())
         .map { it.key }

     val constraints =  when (idFields.size) {
         0 -> emptyList<Constraint>()
         1 ->  {
            val pkColumn = idFields.single()
            val pkField = columns.first { field -> field.name == pkColumn }
            listOf(constraint("pk").primaryKey(pkField))
         }
         else -> {
            logger.warn { "Composite keys are supported in SQL, but not in Taxi. (${type.name.shortDisplayName} defines ${columns.size} columns).  Not defining any primary keys " }
            emptyList<Constraint>()
         }

      }
      val sqlDsl = dsl.createTableIfNotExists(tableName)
         .columns(columns)
         .constraints(constraints)

      return tableName to sqlDsl
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
      return generate(type, connectionDetails.sqlBuilder()).second
   }

   object TaxiTypeToJooqType {
      private val logger = KotlinLogging.logger {}
      fun getSqlType(type: lang.taxi.types.Type, nullable: Boolean): DataType<out Any> {
         return getSqlType(type).nullable(nullable)
      }

      fun getSqlType(type: lang.taxi.types.Type): DataType<out Any> {
         return when {
            type is PrimitiveType -> getSqlType(type)
            type is ArrayType -> SQLDataType.OTHER.arrayDataType
            type is EnumType -> SQLDataType.VARCHAR // TODO : Generate enum types
            type is ObjectType && type.fields.isNotEmpty() -> error("Only scalar types are supported.  ${type.qualifiedName} defines fields")
            type is ObjectType && type.fields.isEmpty() -> {
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
