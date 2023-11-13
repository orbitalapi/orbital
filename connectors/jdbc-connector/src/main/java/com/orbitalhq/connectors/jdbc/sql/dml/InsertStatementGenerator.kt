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

typealias StatementReturnsGeneratedValues = Boolean

class InsertStatementGenerator(private val schema: Schema) {

   private val logger = KotlinLogging.logger {}

   fun generateInsertWithoutConnecting(
      typedInstance: TypedInstance,
      connection: JdbcConnectionConfiguration,
      useUpsertSemantics: Boolean = false
   ) =
      generateInsertWithoutConnecting(listOf(typedInstance), connection, useUpsertSemantics).first

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
   ): Pair<InsertResultStep<Record>, StatementReturnsGeneratedValues> {
      require(values.isNotEmpty()) { "No values provided to persist." }
      val recordType = assertAllValuesHaveSameType(values)

      val actualTableName = tableName ?: SqlUtils.tableNameOrTypeName(recordType.taxiType, tableNameSuffix)
      val fields = findFieldsToInsert(recordType)
      val sqlFields = fields.map { it.second }

      val generatedFields =
         recordType.getAttributesWithAnnotation(JdbcConnectorTaxi.Annotations.GeneratedIdAnnotationName.fqn())
            .map { DSL.field(DSL.name(it.key)) }

      val rows = values.map { typedInstance ->
         val rowValues = fields.map { (fieldName, _) ->
            require(typedInstance is TypedObject) { "Expected to receive a TypedObject, but got a ${typedInstance::class.simpleName}" }
            typedInstance[fieldName].value
         }
         row(rowValues)
      }
      val primaryKeyFields = getPrimaryKeyFields(recordType)
      val statement = sql.insertInto(table(DSL.name(actualTableName)), *sqlFields.toTypedArray())
         .valuesOfRows(*rows.toTypedArray())
         .let { insert ->
            if (useUpsertSemantics && primaryKeyFields.isNotEmpty()) {
               insert.onConflict(primaryKeyFields)
                  .doUpdate().setAllToExcluded()
                  .returningResult(generatedFields)
            } else {
               insert.returningResult(generatedFields)
//               insert.returning(*generatedFields.toTypedArray())
            }
         }
      return statement to generatedFields.isNotEmpty()
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
//   fun generateInserts(
//      values: List<TypedInstance>,
//      sql: DSLContext,
//      useUpsertSemantics: Boolean = false,
//      tableNameSuffix: String? = null,
//      tableName: String? = null
//   ): List<InsertValuesStepN<Record>> {
//      require(values.isNotEmpty()) { "No values provided to persist." }
//      val recordType = assertAllValuesHaveSameType(values)
//
//      val actualTableName = tableName ?: SqlUtils.tableNameOrTypeName(recordType.taxiType, tableNameSuffix)
//      val fields = findFieldsToInsert(recordType)
//      val sqlFields = fields.map { it.second }
//      val rowsToInsert = values.mapNotNull { typedInstance ->
//         if (typedInstance is TypedCollection && typedInstance.isEmpty()) {
//            // This can sneak through because of a failed transformation
//            return@mapNotNull null
//         }
//
//         require(typedInstance is TypedObject) { "Database operations are only supported on TypedObject - got ${typedInstance::class.simpleName}" }
//         val rowValues = fields.map { (attributeName, _) ->
//            attributeName to typedInstance[attributeName].value
//         }
//         rowValues
//      }
//
//      // There are nicer syntaxes for inserting multiple rows (using Records)
//      // in later versions, but locked to 3.13 because of old spring dependencies.
//      val insertStatements = rowsToInsert.map { row: List<Pair<AttributeName, Any?>> ->
//         var insert = sql.insertInto(table(actualTableName), *sqlFields.toTypedArray())
//         val rowValues = row.map { it.second }
//         insert = insert.values(rowValues)
//         if (useUpsertSemantics) {
//            insert.onConflict(getPrimaryKeyFields(recordType))
//               .doUpdate().setAllToExcluded().returning()
//         } else {
//            insert.returning()
//         }
//      }
//      return insertStatements
//   }

   private fun getPrimaryKeyFields(recordType: Type): List<Field<Any>> {
      return recordType.getAttributesWithAnnotation("Id".fqn())
         .map { field(name(it.key)) }
   }

   private fun getNonPrimaryKeyFields(recordType: Type): List<Field<Any>> {
      return recordType.attributes
         .filter { (name, field) -> !field.hasMetadata("Id".fqn()) }
         .map { field(name(it.key)) }
   }


   private fun appendUpsert(
      insertBuilder: InsertValuesStepN<Record>,
      row: List<Pair<AttributeName, Any?>>,
      recordType: Type,
      generatedFields: List<Field<Any>>
   ): InsertValuesStepN<Record> {
      val primaryKeyFields = getPrimaryKeyFields(recordType)
      if (primaryKeyFields.isEmpty()) {
         logger.info { "Cannot use upsert semantics on type ${recordType.longDisplayName} as no @Id fields exist" }
         return insertBuilder
      }

      val nonPrimaryKeyFields = getNonPrimaryKeyFields(recordType)
      if (nonPrimaryKeyFields.isEmpty()) {
         logger.info { "Nothing to upsert on type ${recordType.longDisplayName} as the only field is an @Id field" }
         return insertBuilder
      }

//      insertBuilder.onConflict(primaryKeyFields)
//         .doUpdate()
//         .let { onDuplicateStep ->
//            nonPrimaryKeyFields.fold(onDuplicateStep) { acc, field: Field<Any> ->
//
//               val excluded:Field<Any> = excluded(field)
//               val f:Field<Any> = acc.set(field as Field<*>, excluded as Field<*>)
//               TODO()
//            }
//         }

//      val nonPrimaryKeyFields = row.filter { (attributeName, _) -> primaryKeyFields.none { it.name == attributeName } }
//      if (nonPrimaryKeyFields.isEmpty()) {
//         logger.info { "Nothing to upsert on type ${recordType.longDisplayName} as the only field is an @Id field" }
//         return insertBuilder
//      }

      val a = generatedFields.map { select(it) }
      val f = insertBuilder
         .onConflict(primaryKeyFields).doUpdate().setAllToExcluded()
         .returning()
//         .let { onDuplicateStep ->
//            onDuplicateStep.set(field(""), )
//            nonPrimaryKeyFields
//               .forEach { (fieldName, value) ->
//                  if (value != null) {
//                     onDuplicateStep.set(field(name(fieldName)), value)
//                  } else {
//                     onDuplicateStep.setNull(field(name(fieldName)))
//                  }
//               }
//         }
      return insertBuilder
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
   ) = generateInsertAsSingleStatement(values, connection.sqlBuilder(), useUpsertSemantics)

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

