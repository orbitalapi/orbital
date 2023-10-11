package com.orbitalhq.connectors.jdbc.sql.dml

import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.jdbc.JdbcConnectorTaxi
import com.orbitalhq.connectors.jdbc.SqlUtils
import com.orbitalhq.connectors.jdbc.sqlBuilder
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.AttributeName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import mu.KotlinLogging
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.DSL.*

class InsertStatementGenerator(private val schema: Schema) {

   private val logger = KotlinLogging.logger {}

   fun generateInsertWithoutConnecting(
       typedInstance: TypedInstance,
       connection: JdbcConnectionConfiguration,
       useUpsertSemantics: Boolean = false
   ) =
      generateInsertWithoutConnecting(listOf(typedInstance), connection, useUpsertSemantics).single()

   /**
    * Generates a single statement inserting many rows.
    * Genertaed values (ie., db-assigned IDs) are returned.
    */
   fun generateInsertAsSingleStatement(
      values: List<TypedInstance>,
      sql: DSLContext,
      useUpsertSemantics: Boolean = false,
      tableNameSuffix: String? = null,
      tableName: String? = null
   ): InsertResultStep<Record> {
      require(values.isNotEmpty()) { "No values provided to persist." }
      val recordType = assertAllValuesHaveSameType(values)

      val actualTableName = tableName ?: SqlUtils.tableNameOrTypeName(recordType.taxiType, tableNameSuffix)
      val fields = findFieldsToInsert(recordType)
      val sqlFields = fields.map { it.second }

      val generatedFields = recordType.getAttributesWithAnnotation(JdbcConnectorTaxi.Annotations.GeneratedIdAnnotationName.fqn())
         .map { DSL.field(DSL.name(it.key))}

      val rows = values.map {typedInstance ->
         val rowValues = fields.map { (fieldName,_) ->
            require(typedInstance is TypedObject) { "Expected to receive a TypedObject, but got a ${typedInstance::class.simpleName}"}
            typedInstance[fieldName].value
         }
         row(rowValues)
      }
      return sql.insertInto(table(DSL.name(actualTableName)), *sqlFields.toTypedArray())
         .valuesOfRows(*rows.toTypedArray())
         .returning(*generatedFields.toTypedArray())
   }

   /**
    * Generates an insert using the provided dsl context.
    * If the context itself is connected to a db, the returned sql statement is executable
    *
    * Returns multiple Insert statements, intended to be executed in a batch.
    * As a result, db-generated values are not returned.
    *
    * Where possible prefer using generateInsertAsSingleStatement.
    * That approach
    */
   fun generateInserts(
      values: List<TypedInstance>,
      sql: DSLContext,
      useUpsertSemantics: Boolean = false,
      tableNameSuffix: String? = null,
      tableName: String? = null
   ): List<InsertValuesStepN<Record>> {
      require(values.isNotEmpty()) { "No values provided to persist." }
      val recordType = assertAllValuesHaveSameType(values)

      val actualTableName = tableName ?: SqlUtils.tableNameOrTypeName(recordType.taxiType, tableNameSuffix)
      val fields = findFieldsToInsert(recordType)
      val sqlFields = fields.map { it.second }
      val rowsToInsert = values.mapNotNull { typedInstance ->
         if (typedInstance is TypedCollection && typedInstance.isEmpty()) {
            // This can sneak through because of a failed transformation
            return@mapNotNull null
         }

         require(typedInstance is TypedObject) { "Database operations are only supported on TypedObject - got ${typedInstance::class.simpleName}" }
         val rowValues = fields.map { (attributeName, _) ->
            attributeName to typedInstance[attributeName].value
         }
         rowValues
      }

      val primaryKeyFields = recordType.getAttributesWithAnnotation("Id".fqn())
         .map { field(name(it.key)) }
      // There are nicer syntaxes for inserting multiple rows (using Records)
      // in later versions, but locked to 3.13 because of old spring dependencies.
      val insertStatements = rowsToInsert.map { row: List<Pair<AttributeName, Any?>> ->
         val insert = sql.insertInto(table(actualTableName), *sqlFields.toTypedArray())
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
                     insertBuilder.set(field(name(fieldName)), value)
                  } else {
                     insertBuilder.setNull(field(name(fieldName)))
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
   private fun generateInsertWithoutConnecting(
       values: List<TypedInstance>,
       connection: JdbcConnectionConfiguration,
       useUpsertSemantics: Boolean = false
   ) = generateInserts(values, connection.sqlBuilder(), useUpsertSemantics)

   private fun assertAllValuesHaveSameType(values: List<TypedInstance>): Type {
      val types = values.map { it.type.collectionType ?: it.type }
         .distinct()

      val allTypesAreAnonymous = types
         // Exclude collection types, as some are Any[] when projecting to anonymous types
         .all { it.taxiType.anonymous }
      if (allTypesAreAnonymous) {
         return values.first().type
      }

      require(types.size == 1) { "Expected all provided values should be of the same type - found ${types.joinToString { it.name.shortDisplayName }}" }
      return types.single()
   }

   private fun findFieldsToInsert(type: Type): List<Pair<AttributeName, Field<Any>>> {
      return type.attributes
         .filter { (name, field) ->
            // Don't insert fields that are auto generated.
            !field.hasMetadata(JdbcConnectorTaxi.Annotations.GeneratedIdAnnotationName.fqn())
         }
         // TODO : Currently we persist everything. Does this make sense for evaluated fields?
         .map { (name, field) ->
            name to field(DSL.name(name))
         }
   }
}

