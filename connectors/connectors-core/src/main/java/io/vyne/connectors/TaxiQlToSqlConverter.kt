package io.vyne.connectors

import io.vyne.models.toSql
import io.vyne.utils.Ids
import lang.taxi.TaxiDocument
import lang.taxi.query.TaxiQlQuery
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.services.operations.constraints.PropertyToParameterConstraint
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.types.ArrayType
import lang.taxi.types.DiscoveryType
import lang.taxi.types.ObjectType
import lang.taxi.types.Type

/**
 * Creates SQL statements from a TaxiQL query.
 * This is the naieve first-pass implementation, which doesn't allow for
 * taking into account the grammar of the underlying db.
 * This needs to be replaced with a new query builder using Jooq, within the sql.dml packag.e
 */
// TODO :  Replace this with a jooq powered generator in sql.dml
class TaxiQlToSqlConverter(private val schema: TaxiDocument, private val quoteColumns: Boolean = false) {
   fun toSql(
      query: TaxiQlQuery,
      queryId: String = Ids.id("q-"),
      tableNameProvider: (type: Type) -> String
   ): Pair<String, List<SqlTemplateParameter>> {
      val typesToFind = query.typesToFind
         .map { discoveryType ->
            val collectionType = collectionTypeOrType(schema.type(discoveryType.type)) as ObjectType
            collectionType to discoveryType
         }
      val tableNames: Map<Type, AliasedTableName> = typesToFind.mapIndexed { index, (type, _) ->
         val tableName = tableNameProvider(type)
         val alias = AliasedTableName(type, tableName, "t$index")
         type to alias
      }.toMap()
      val tableNameSql =
         tableNames.values.joinToString(", ") { aliasedTableName -> "${aliasedTableName.tableName} ${aliasedTableName.alias}" }

      val (whereClause, parameters) = buildWhereClause(typesToFind, tableNames)
      val sql = "select '$queryId' as _queryId, * from $tableNameSql $whereClause".trim()
      return sql to parameters
   }

   private fun buildWhereClause(
      typesToFind: List<Pair<ObjectType, DiscoveryType>>,
      tableNames: Map<Type, AliasedTableName>
   ): Pair<String, List<SqlTemplateParameter>> {
      val constraints: List<Pair<String, SqlTemplateParameter>> =
         typesToFind.filter { (_, discoveryType) -> discoveryType.constraints.isNotEmpty() }
            .flatMap { (type, discoveryType) ->
               val tableName = tableNames[type]!!
               discoveryType.constraints.mapIndexed { index, constraint ->
                  buildSqlConstraint(
                     discoveryType,
                     type,
                     tableName,
                     constraint,
                     index
                  )
               }
            }
      if (constraints.isEmpty()) {
         return "" to emptyList()
      }
      val whereClause = constraints.joinToString(" AND ", prefix = "WHERE ") { it.first }
      val parameters = constraints.map { it.second }
      return whereClause to parameters
   }

   private fun buildSqlConstraint(
      discoveryType: DiscoveryType,
      type: ObjectType,
      tableName: AliasedTableName,
      constraint: Constraint,
      constraintIndex: Int
   ): Pair<String, SqlTemplateParameter> {
      return when (constraint) {
         is PropertyToParameterConstraint -> buildSqlConstraint(
            discoveryType,
            type,
            tableName,
            constraint,
            constraintIndex
         )
         else -> error("Sql constraints not supported for constraint type ${constraint::class.simpleName}")
      }
   }

   private fun buildSqlConstraint(
      discoveryType: DiscoveryType,
      type: ObjectType,
      tableName: AliasedTableName,
      constraint: PropertyToParameterConstraint,
      constraintIndex: Int
   ): Pair<String, SqlTemplateParameter> {
      val field = when (val constraintPropertyIdentifier = constraint.propertyIdentifier) {
         is PropertyTypeIdentifier -> {
            val candidateFields = type.fields.filter { it.type.toQualifiedName() == constraintPropertyIdentifier.type }
            when (candidateFields.size) {
               0 -> error("Type ${type.qualifiedName} does not have a field with type ${constraintPropertyIdentifier.type}")
               1 -> candidateFields.first()
               else -> error("Type ${constraintPropertyIdentifier.type} is ambiguous on type ${type.qualifiedName} - there are ${candidateFields.size} matches - ${candidateFields.joinToString { it.name }}")
            }
         }
         else -> error("Sql constraint generation not supported yet for field identifier type of ${constraintPropertyIdentifier::class.simpleName}")
      }

      val sqlFieldName =
         if (quoteColumns) """${tableName.alias}."${field.name}"""" else "${tableName.alias}.${field.name}"
      val sqlParameterName = "${field.name}$constraintIndex"
      val comparisonValue = when (val expectedValue = constraint.expectedValue) {
         is ConstantValueExpression -> expectedValue.value
         else -> error("Sql constraint generation not supported yet for value expression of type ${expectedValue::class.simpleName}")
      }
      val parameterTemplatePair = SqlTemplateParameter(sqlParameterName, comparisonValue)
      return "$sqlFieldName ${constraint.operator.toSql()} :$sqlParameterName" to parameterTemplatePair
   }
}

private data class AliasedTableName(val type: Type, val tableName: String, val alias: String)
data class SqlTemplateParameter(val nameUsedInTemplate: String, val value: Any)

fun collectionTypeOrType(type: Type): Type {
   return if (type is ArrayType) {
      type.parameters[0]
   } else {
      type
   }
}
