package com.orbitalhq.connectors.jdbc.sql.dml

import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.jdbc.JdbcConnectorTaxi
import com.orbitalhq.connectors.jdbc.SqlUtils
import com.orbitalhq.connectors.jdbc.UpsertVerb
import com.orbitalhq.connectors.jdbc.drivers.DatabaseSupport
import com.orbitalhq.connectors.jdbc.sql.ddl.TaxiTypeToJooqType
import com.orbitalhq.connectors.jdbc.sqlBuilder
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

class InsertStatementGenerator(private val schema: Schema, private val databaseSupport: DatabaseSupport) {

   private val logger = KotlinLogging.logger {}

   @Deprecated("currently used only in tests")
   fun generateInsertWithoutConnecting(
      typedInstance: TypedInstance,
      connection: JdbcConnectionConfiguration,
      verb: UpsertVerb
   ): String {
      val result = generateInsertWithoutConnecting(listOf(typedInstance), connection, verb)
      return result.sql
   }

   /**
    * Generates a single statement inserting many rows.
    * Genertaed values (ie., db-assigned IDs) are returned.
    */
   fun generateInsertAsSingleStatement(
      values: List<TypedInstance>,
      sql: DSLContext,
      verb: UpsertVerb,
      tableNameSuffix: String? = null,
      tableName: String? = null
   ): SqlOperation {
      require(values.isNotEmpty()) { "No values provided to persist." }
      val recordType = assertAllValuesHaveSameType(values)

      val actualTableName = tableName ?: SqlUtils.tableNameOrTypeName(recordType.taxiType, tableNameSuffix)
      val fields = findFieldsToInsert(recordType, verb)
      val sqlFields = fields.map { it.second }

      val generatedFields =
         recordType.getAttributesWithAnnotation(JdbcConnectorTaxi.Annotations.GeneratedIdAnnotationName.fqn())
            .map { (fieldName, field) ->
               val taxiType = field.resolveType(schema).taxiType
               val jooqType = TaxiTypeToJooqType.getSqlType(taxiType, field.nullable)
               // We need to pass the type here as some dialects (eg: mssql)
               // define inline tables within the insert to support upsert semantics
               DSL.field(DSL.name(fieldName), jooqType)
            }

      val primaryKeyFields = getPrimaryKeyFields(recordType)
      val valuesAsMaps: List<Map<AttributeName, Any?>> = values.map { typedInstance ->
         fields.associate { (fieldName, _) ->
            require(typedInstance is TypedObject) { "Expected to receive a TypedObject, but got a ${typedInstance::class.simpleName}" }
            fieldName to typedInstance[fieldName].value
         }
      }
      val rows = valuesAsMaps.map { rowMap ->
         row(rowMap.values)
      }

      return databaseSupport.buildUpsertStatement(
         sql,
         actualTableName,
         sqlFields,
         rows,
         valuesAsMaps,
         verb,
         primaryKeyFields,
         generatedFields
      )
   }

   private fun buildUpsertStatement(
      sql: DSLContext,
      actualTableName: String,
      sqlFields: List<Field<Any>>,
      rows: List<RowN>,
      useUpsertSemantics: Boolean,
      primaryKeyFields: List<Field<Any>>,
      generatedFields: List<Field<Any>>
   ): InsertResultStep<Record> {
      val statement = sql.insertInto(table(name(actualTableName)), *sqlFields.toTypedArray())
         .valuesOfRows(*rows.toTypedArray())
         .let { insert ->
            if (useUpsertSemantics && primaryKeyFields.isNotEmpty()) {
               insert.onConflict(primaryKeyFields)
                  .doUpdate().setAllToExcluded()
                  .returningResult(generatedFields)
            } else {
               insert.returningResult(generatedFields)
            }
         }
      return statement
   }

   private fun getPrimaryKeyFields(recordType: Type): List<Field<Any>> {
      return recordType.getAttributesWithAnnotation("Id".fqn())
         .map { field(name(it.key)) }
   }


   /**
    * Generates an insert statement using the dialect in the JDBC connection.
    * As no connection is established to the database, the insert statement isn't directly
    * executable.
    * Useful for testing.
    */
   @Deprecated("currently used only in tests")
   private fun generateInsertWithoutConnecting(
      values: List<TypedInstance>,
      connection: JdbcConnectionConfiguration,
      verb: UpsertVerb
   ) = generateInsertAsSingleStatement(values, connection.sqlBuilder(), verb)

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

   private fun findFieldsToInsert(type: Type, verb: UpsertVerb): List<Pair<AttributeName, Field<out Any>>> {
      return type.attributes
         .filter { (_, field) ->
//             Don't insert fields that are auto generated.
            when (verb) {
               UpsertVerb.Insert,
               UpsertVerb.Upsert -> !field.hasMetadata(JdbcConnectorTaxi.Annotations.GeneratedIdAnnotationName.fqn())
               UpsertVerb.Update -> true
            }
         }
         // TODO : Currently we persist everything. Does this make sense for evaluated fields?
         .map { (name, field) ->
            val taxiType = field.resolveType(schema).taxiType
            name to field(DSL.name(name), TaxiTypeToJooqType.getSqlType(taxiType, nullable = field.nullable))
         }
   }
}

