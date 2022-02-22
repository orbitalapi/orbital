package io.vyne.connectors.jdbc.sql.dml

import io.vyne.connectors.jdbc.JdbcConnectionConfiguration
import io.vyne.connectors.jdbc.SqlUtils
import io.vyne.connectors.jdbc.sqlBuilder
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.InsertValuesStepN
import org.jooq.Record
import org.jooq.impl.DSL.*

class InsertStatementGenerator(private val schema: Schema) {

   fun generateInsertWithoutConnecting(typedInstance: TypedInstance, connection: JdbcConnectionConfiguration) =
      generateInsertWithoutConnecting(listOf(typedInstance), connection)

   /**
    * Generates an insert using the provided dsl context.
    * If the context itself is connected to a db, the returned sql statement is executable
    */
   fun generateInsert(value: TypedInstance, dslContext: DSLContext) = generateInsert(listOf(value), dslContext)

   /**
    * Generates an insert using the provided dsl context.
    * If the context itself is connected to a db, the returned sql statement is executable
    */
   fun generateInsert(
      values: List<TypedInstance>,
      dslContext: DSLContext,
      useUpsertSemantics: Boolean = false
   ): InsertValuesStepN<Record> {
      require(values.isNotEmpty()) { "No values provided to persist." }
      val recordType = assertAllValuesHaveSameType(values)

      val tableName = SqlUtils.tableNameOrTypeName(recordType.taxiType)
      val fields = findFieldsToInsert(recordType)
      val sqlFields = fields.map { it.second }
      val rowsToInsert = values.map { typedInstance ->
         require(typedInstance is TypedObject) { "Database operations are only supported on TypedObject - got ${typedInstance::class.simpleName}" }
         val rowValues = fields.map { (attributeName, _) ->
            typedInstance[attributeName].value
         }
         rowValues
      }
      // There are nicer syntaxes for inserting multiple rows (using Records)
      // in later versions, but locked to 3.13 because of old spring dependencies.
      return dslContext.insertInto(table(tableName), *sqlFields.toTypedArray())
         .let { insert ->
            rowsToInsert.forEach { insert.values(it) }
            insert
         }
   }

   /**
    * Generates an insert statement using the dialect in the JDBC connection.
    * As no connection is established to the database, the insert statement isn't directly
    * executable.
    * Useful for testing.
    */
   fun generateInsertWithoutConnecting(
      values: List<TypedInstance>,
      connection: JdbcConnectionConfiguration
   ): InsertValuesStepN<Record> {
      return generateInsert(values, connection.sqlBuilder())
   }

   private fun assertAllValuesHaveSameType(values: List<TypedInstance>): Type {
      val types = values.map { it.type }.distinct()
      require(types.size == 1) { "Expected all provided values should be of the same type - found ${types.joinToString { it.name.shortDisplayName }}" }
      return types.single()
   }

   private fun findFieldsToInsert(type: Type): List<Pair<AttributeName, Field<Any>>> {
      return type.attributes
         // TODO : Currently we persist everything. Does this make sense for evaluated fields?
         .map { (name, field) ->
            name to field(name)
         }
   }
}

