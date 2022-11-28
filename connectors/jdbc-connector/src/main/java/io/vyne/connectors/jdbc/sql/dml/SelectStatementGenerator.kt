package io.vyne.connectors.jdbc.sql.dml

import io.vyne.connectors.collectionTypeOrType
import io.vyne.connectors.jdbc.SqlUtils
import io.vyne.schemas.Schema
import lang.taxi.TaxiDocument
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.query.TaxiQlQuery
import lang.taxi.services.operations.constraints.*
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.types.*
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table

/**
 * Creates SQL statements from a TaxiQL query.
 * This is the naieve first-pass implementation, which doesn't allow for
 * taking into account the grammar of the underlying db.
 * This needs to be replaced with a new query builder using Jooq, within the sql.dml packag.e
 */
// TODO :  Replace this with a jooq powered generator in sql.dml
class SelectStatementGenerator(private val taxiSchema: TaxiDocument) {
   constructor(schema: Schema) : this(schema.taxi)

   /**
    * Generates a SELECT statement using a default SQL dialect
    */
   fun generateGenericSelect(query: TaxiQlQuery): Pair<Select<Record>, List<SqlTemplateParameter>> {
      return generateSelect(query, DSL.using(SQLDialect.DEFAULT))
   }

   /**
    * Generates a SELECT statement using the dialect configured in the DSL Context
    */
   fun generateSelect(query: TaxiQlQuery, sqlDsl: DSLContext): Pair<Select<Record>, List<SqlTemplateParameter>> {
      val typesToFind = getTypesToFind(query)
      val tableNamesFromType: Map<Type, AliasedTableName> = getTableNames(typesToFind)
      if (tableNamesFromType.size > 1) {
         error("Joins are not yet supported - can only select from a single table")
      }
      val sqlTablesByType = tableNamesFromType.values.map { tableName ->
         tableName.type to table(tableName.tableName).`as`(tableName.alias)
      }.toMap()
      val sqlTable = sqlDsl.select().from(sqlTablesByType.values)
      val conditionsAndParams = buildWhereClause(typesToFind, sqlTablesByType)
      val conditions = conditionsAndParams.map { it.first }
      val params = conditionsAndParams.map { it.second }
      return sqlTable.where(conditions) to params
   }

   fun generateSelectSql(query: TaxiQlQuery, sqlDsl: DSLContext): Pair<String, List<SqlTemplateParameter>> {
      val (select,params) = generateSelect(query, sqlDsl)
      val sql = sqlDsl.renderNamedParams(select)
      return sql to params
   }

   @Deprecated("use generateSelect(), as it provides richer support for different dialects")
   fun toSql(
      query: TaxiQlQuery,
      sqlDsl: DSLContext,
      tableNameProvider: (type: Type) -> String
   ): Pair<String, List<SqlTemplateParameter>> {
      val (select,params) = generateSelect(query, sqlDsl)
      val sql = sqlDsl.renderNamedParams(select)
      return sql to params
   }

   @Deprecated("use generateSelect(), as it provides richer support for different dialects")
   fun toSql(query: TaxiQlQuery, tableNameProvider: (type: Type) -> String): Pair<String, List<SqlTemplateParameter>> {
      return toSql(query, DSL.using(SQLDialect.DEFAULT), tableNameProvider)
   }

   private fun getTableNames(
      typesToFind: List<Pair<ObjectType, DiscoveryType>>,
      // TODO : I think this always uses the default, in which case, we should simplify this down.
      // Refactor once tests are green.
      tableNameProvider: (type: Type) -> String = { type -> SqlUtils.tableNameOrTypeName(type) }
   ): Map<Type, AliasedTableName> {
      val tableNames: Map<Type, AliasedTableName> = typesToFind.mapIndexed { index, (type, _) ->
         val tableName = tableNameProvider(type)
         val alias = AliasedTableName(type, tableName, "t$index")
         type to alias
      }.toMap()
      return tableNames
   }

   private fun getTypesToFind(query: TaxiQlQuery): List<Pair<ObjectType, DiscoveryType>> {
      val typesToFind = query.typesToFind
         .map { discoveryType ->
            val collectionType = collectionTypeOrType(taxiSchema.type(discoveryType.typeName)) as ObjectType
            collectionType to discoveryType
         }
      return typesToFind
   }

   private fun buildWhereClause(
      typesToFind: List<Pair<ObjectType, DiscoveryType>>,
      tableNames: Map<Type, Table<Record>>
   ): List<Pair<Condition,SqlTemplateParameter>> {
      return typesToFind.filter { (_, discoveryType) -> discoveryType.constraints.isNotEmpty() }
         .flatMap { (type, discoveryType) ->
            val sqlTable = tableNames[type]!!
            discoveryType.constraints.mapIndexed { index, constraint ->
               buildSqlConstraint(
                  discoveryType,
                  type,
                  sqlTable,
                  constraint,
                  index
               )
            }
         }
   }

   private fun buildSqlConstraint(
      discoveryType: DiscoveryType,
      type: ObjectType,
      sqlTable: Table<Record>,
      constraint: Constraint,
      constraintIndex: Int
   ): Pair<Condition,SqlTemplateParameter> {
      return when (constraint) {
         is ExpressionConstraint -> buildSqlConstraint(
            discoveryType,
            type,
            sqlTable,
            constraint,
            constraintIndex
         )

         else -> error("Sql constraints not supported for constraint type ${constraint::class.simpleName}")
      }
   }

   private fun buildSqlConstraint(
      discoveryType: DiscoveryType,
      type: ObjectType,
      sqlTable: Table<Record>,
      constraint: ExpressionConstraint,
      constraintIndex: Int
   ): Pair<Condition, SqlTemplateParameter> {
      return when (val expression = constraint.expression) {
         is OperatorExpression -> buildSqlConstraintFromOperatorExpression(discoveryType, type, sqlTable, expression, constraintIndex)
         else -> error("Unsupported expression type: ${expression::class.simpleName}")
      }
   }

   private fun buildSqlConstraintFromOperatorExpression(
      discoveryType: DiscoveryType,
      type: ObjectType,
      sqlTable: Table<Record>,
      expression: OperatorExpression,
      constraintIndex: Int
   ): Pair<Condition, SqlTemplateParameter> {
      if (expression.lhs !is TypeExpression) {
         error("${expression::class.simpleName} expressions are not yet supported on the lhs of a SQL expression")
      }

      val fieldReference = getSingleField(type, (expression.lhs as TypeExpression).type)
      val fieldName = fieldReference.path.single().name

      val field = field(DSL.name(sqlTable.name, fieldName), Any::class.java)

      if (expression.rhs !is LiteralExpression) {
         error("${expression::class.simpleName} expressions are not yet supported on the rhs of a SQL expression")
      }
      val literal = expression.rhs as LiteralExpression
      val sqlParameterName = "${field.name}$constraintIndex"
      val sqlParam = SqlTemplateParameter(sqlParameterName, literal.value)

      val condition =  when (expression.operator) {
         FormulaOperator.Equal -> field.eq(DSL.param(sqlParameterName))
         FormulaOperator.NotEqual -> field.ne(DSL.param(sqlParameterName))
         FormulaOperator.LessThan -> field.lessThan(DSL.param(sqlParameterName))
         FormulaOperator.LessThanOrEqual -> field.lessOrEqual(DSL.param(sqlParameterName))
         FormulaOperator.GreaterThan -> field.greaterThan(DSL.param(sqlParameterName))
         FormulaOperator.GreaterThanOrEqual -> field.greaterOrEqual(DSL.param(sqlParameterName))
         else -> error("${expression.operator} is not yet supported in SQL clauses")
      }

      return condition to sqlParam
   }

   private fun getSingleField(sourceType: ObjectType, fieldType: Type): FieldReference {
      val references = sourceType.fieldReferencesAssignableTo(fieldType)
      return when {
         references.isEmpty() -> error("Field ${fieldType.qualifiedName} is not present on type ${sourceType.qualifiedName}")
         references.size == 1 -> {
            val reference = references.single()
            if (reference.path.size == 1) {
               reference
            } else {
               error(
                  "${fieldType.qualifiedName} is only accessible on ${sourceType} via a nested property (${
                     reference.path.joinToString(
                        "."
                     )
                  }), which is not supported in SQL statements"
               )
            }
         }

         else -> error("Field ${fieldType.qualifiedName} is ambiguous on type ${sourceType.qualifiedName} - expected a single match, but found ${references.joinToString { it.description }}")
      }
   }

   // I don't think this method should actually be called anymore - ExpressionConstraint has broadly replaced PropertyToParameterConstraint
   private fun buildSqlConstraint(
      discoveryType: DiscoveryType,
      type: ObjectType,
      tableName: AliasedTableName,
      constraint: PropertyToParameterConstraint,
      constraintIndex: Int
   ): Pair<String, SqlTemplateParameter> {
      TODO()
//      val field = when (val constraintPropertyIdentifier = constraint.propertyIdentifier) {
//         is PropertyTypeIdentifier -> {
//            val candidateFields = type.fields.filter { it.type.toQualifiedName() == constraintPropertyIdentifier.type }
//            when (candidateFields.size) {
//               0 -> error("Type ${type.qualifiedName} does not have a field with type ${constraintPropertyIdentifier.type}")
//               1 -> candidateFields.first()
//               else -> error("Type ${constraintPropertyIdentifier.type} is ambiguous on type ${type.qualifiedName} - there are ${candidateFields.size} matches - ${candidateFields.joinToString { it.name }}")
//            }
//         }
//         else -> error("Sql constraint generation not supported yet for field identifier type of ${constraintPropertyIdentifier::class.simpleName}")
//      }
//
//      val sqlFieldName = if (quoteColumns) """${tableName.alias}."${field.name}"""" else "${tableName.alias}.${field.name}"
//      val sqlParameterName = "${field.name}$constraintIndex"
//      val comparisonValue = when (val expectedValue = constraint.expectedValue) {
//         is ConstantValueExpression -> expectedValue.value
//         else -> error("Sql constraint generation not supported yet for value expression of type ${expectedValue::class.simpleName}")
//      }
//      val parameterTemplatePair = SqlTemplateParameter(sqlParameterName, comparisonValue)
//      return "$sqlFieldName ${constraint.operator.toSql()} :$sqlParameterName" to parameterTemplatePair
   }
}

private data class AliasedTableName(val type: Type, val tableName: String, val alias: String)
data class SqlTemplateParameter(val nameUsedInTemplate: String, val value: Any)

