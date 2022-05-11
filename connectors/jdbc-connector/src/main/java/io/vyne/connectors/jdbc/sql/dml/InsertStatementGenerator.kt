package io.vyne.connectors.jdbc.sql.dml

import io.vyne.connectors.jdbc.JdbcConnectionConfiguration
import io.vyne.connectors.jdbc.SqlUtils
import io.vyne.connectors.jdbc.sqlBuilder
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.InsertValuesStepN
import org.jooq.Record
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table

class InsertStatementGenerator(private val schema: Schema) {

   private val logger = KotlinLogging.logger {}

   fun generateInsertWithoutConnecting(
      typedInstance: TypedInstance,
      connection: JdbcConnectionConfiguration,
      useUpsertSemantics: Boolean = false
   ) =
      generateInsertWithoutConnecting(listOf(typedInstance), connection, useUpsertSemantics).single()

   /**
    * Generates an insert using the provided dsl context.
    * If the context itself is connected to a db, the returned sql statement is executable
    */
   fun generateInserts(value: TypedInstance, dslContext: DSLContext) = generateInserts(listOf(value), dslContext)

   /**
    * Generates an insert using the provided dsl context.
    * If the context itself is connected to a db, the returned sql statement is executable
    */
   fun generateInserts(
      values: List<TypedInstance>,
      sql: DSLContext,
      useUpsertSemantics: Boolean = false
   ): List<InsertValuesStepN<Record>> {
      require(values.isNotEmpty()) { "No values provided to persist." }
      val recordType = assertAllValuesHaveSameType(values)

      val tableName = SqlUtils.tableNameOrTypeName(recordType.taxiType)
      val fields = findFieldsToInsert(recordType)
      val sqlFields = fields.map { it.second }
      val rowsToInsert = values.map { typedInstance ->
         require(typedInstance is TypedObject) { "Database operations are only supported on TypedObject - got ${typedInstance::class.simpleName}" }
         val rowValues = fields.map { (attributeName, _) ->
            attributeName to typedInstance[attributeName].value
         }
         rowValues
      }

      val primaryKeyFields = recordType.getAttributesWithAnnotation("Id".fqn())
         .map { field(it.key) }
      // There are nicer syntaxes for inserting multiple rows (using Records)
      // in later versions, but locked to 3.13 because of old spring dependencies.
      val insertStatements = rowsToInsert.map { row: List<Pair<AttributeName, Any?>> ->
         val insert = sql.insertInto(table(tableName), *sqlFields.toTypedArray())
         val rowValues = row.map { it.second }
         insert.values(rowValues)
         if (useUpsertSemantics) {
            appendUpsert(primaryKeyFields, insert, row, recordType)
         }
         insert
      }
      return insertStatements
   }


   private fun appendUpsert(
      primaryKeyFields: List<Field<Any>>,
      insertBuilder: InsertValuesStepN<Record>,
      row: List<Pair<AttributeName, Any?>>,
      recordType: Type
   ) {
      if (primaryKeyFields.isEmpty()) {
         logger.info { "Cannot use upsert semantics on type ${recordType.longDisplayName} as no @Id fields exist" }
         return
      }

      val nonPrimaryKeyFields = row.filter { (attributeName, _) -> primaryKeyFields.none { it.name == attributeName } }
      if (nonPrimaryKeyFields.isEmpty()) {
         logger.info { "Nothing to upsert on type ${recordType.longDisplayName} as the only field is an @Id field" }
         return
      }

      insertBuilder
         .onConflict(primaryKeyFields).doUpdate()
         .let { insertBuilder ->
            nonPrimaryKeyFields
               .forEach { (fieldName, value) ->
                  if (value != null) {
                     insertBuilder.set(field(fieldName), value)
                  } else {
                     insertBuilder.setNull(field(fieldName))
                  }
               }
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
      connection: JdbcConnectionConfiguration,
      useUpsertSemantics: Boolean = false
   ) = generateInserts(values, connection.sqlBuilder(), useUpsertSemantics)

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

