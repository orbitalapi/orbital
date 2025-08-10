package com.orbitalhq.connectors.jdbc.sql.dml

import com.orbitalhq.connectors.getTypesToFind
import com.orbitalhq.connectors.jdbc.SqlTypes
import com.orbitalhq.connectors.jdbc.SqlUtils
import com.orbitalhq.connectors.utils.getSingleField
import com.orbitalhq.schemas.Schema
import lang.taxi.TaxiDocument
import lang.taxi.expressions.LiteralArray
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.query.DiscoveryType
import lang.taxi.query.TaxiQlQuery
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.services.operations.constraints.ExpressionConstraint
import lang.taxi.types.FieldReference
import lang.taxi.types.FormulaOperator
import lang.taxi.types.ObjectType
import lang.taxi.types.Type
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.table
import java.util.concurrent.atomic.AtomicInteger

/**
 * Used to determine the table name for a type.
 * Generally, the default implementation is fine.
 * However, Casks use a seperate naming algo.
 */
typealias TableNameProvider = (type: Type) -> String

/**
 * Creates SQL statements from a TaxiQL query.
 * This is the naieve first-pass implementation, which doesn't allow for
 * taking into account the grammar of the underlying db.
 * This needs to be replaced with a new query builder using Jooq, within the sql.dml packag.e
 */
// TODO :  Replace this with a jooq powered generator in sql.dml
class SelectStatementGenerator(
   private val taxiSchema: TaxiDocument,
   private val tableNameProvider: TableNameProvider = { type -> SqlUtils.tableNameOrTypeName(type) }
) {
   enum class SelectType {
      Records,
      Count
   }

   constructor(schema: Schema) : this(schema.taxi)

   /**
    * Generates a SELECT statement using a default SQL dialect
    */
   fun generateGenericSelect(query: TaxiQlQuery, selectType: SelectType = SelectType.Records): Pair<Select<out Record>, List<SqlTemplateParameter>> {
      return generateSelect(query, DSL.using(SQLDialect.DEFAULT), selectType)
   }

   /**
    * Generates a SELECT statement using the dialect configured in the DSL Context
    */
   private fun generateSelect(
      query: TaxiQlQuery,
      sqlDsl: DSLContext,
      selectType: SelectType
   ): Pair<Select<out Record>, List<SqlTemplateParameter>> {
       val typesToFind = getTypesToFind(query, taxiSchema)
      val tableNamesFromType: Map<Type, AliasedTableName> = getTableNames(typesToFind)
      if (tableNamesFromType.size > 1) {
         error("Joins are not yet supported - can only select from a single table")
      }
      val sqlTablesByType = tableNamesFromType.values.map { tableName ->
         tableName.type to table(name(tableName.tableName)).`as`(tableName.alias)
      }.toMap()

      val select = when (selectType) {
         SelectType.Count -> sqlDsl.selectCount()
         SelectType.Records -> sqlDsl.select()
      }
      val sqlTable = select.from(sqlTablesByType.values)
      val conditionsAndParams = buildWhereClause(typesToFind, sqlTablesByType)
      val conditions = conditionsAndParams.map { it.first }
      val params = conditionsAndParams.flatMap { it.second }
      return sqlTable.where(conditions) to params

   }

   fun selectSqlWithIndexedParams(
      query: TaxiQlQuery,
      sqlDsl: DSLContext,
      selectType: SelectType = SelectType.Records
   ): Pair<String, List<SqlTemplateParameter>> {
      val (select, params) = generateSelect(query, sqlDsl, selectType)
      return select.sql to params
   }

   fun selectSqlWithNamedParams(
      query: TaxiQlQuery,
      sqlDsl: DSLContext,
      selectType: SelectType = SelectType.Records
   ): Pair<String, List<SqlTemplateParameter>> {
      val (select, params) = generateSelect(query, sqlDsl, selectType)
      val sql = sqlDsl.renderNamedParams(select)
      return sql to params
   }

   @Deprecated("use generateSelect(), as it provides richer support for different dialects")
   fun toSql(
      query: TaxiQlQuery,
      sqlDsl: DSLContext, selectType: SelectType = SelectType.Records
   ): Pair<String, List<SqlTemplateParameter>> {
      val (select, params) = generateSelect(query, sqlDsl, selectType)
      val sql = sqlDsl.renderNamedParams(select)
      return sql to params
   }

   @Deprecated("use generateSelect(), as it provides richer support for different dialects")
   fun toSql(query: TaxiQlQuery, tableNameProvider: (type: Type) -> String): Pair<String, List<SqlTemplateParameter>> {
      return toSql(query, DSL.using(SQLDialect.DEFAULT))
   }

   private fun getTableNames(
      typesToFind: List<Pair<ObjectType, DiscoveryType>>,
   ): Map<Type, AliasedTableName> {
      val tableNames: Map<Type, AliasedTableName> = typesToFind.mapIndexed { index, (type, _) ->
         val tableName = tableNameProvider(type)
         val alias = AliasedTableName(type, tableName, "t$index")
         type to alias
      }.toMap()
      return tableNames
   }

   private fun buildWhereClause(
      typesToFind: List<Pair<ObjectType, DiscoveryType>>,
      tableNames: Map<Type, Table<Record>>
   ): List<Pair<Condition, List<SqlTemplateParameter>>> {
      val constraintCounter = AtomicInteger(0)
      return typesToFind.filter { (_, discoveryType) -> discoveryType.constraints.isNotEmpty() }
         .map { (type, discoveryType) ->
            // Pretty sure that now we'll only ever receive a single constraint,
            // which more often than not is an Operator Constraint.
            // (Which can nest mulitple operator constratins under it)
            require(discoveryType.constraints.size == 1) { "Expected to find a single constraint (which could be a compound operation expression).  Instead, found ${discoveryType.constraints.size}" }
            val sqlTable = tableNames[type]!!
            buildSqlConstraint(
               discoveryType,
               type,
               sqlTable,
               discoveryType.constraints.single(),
               constraintCounter
            )
         }

   }


   private fun buildSqlConstraint(
      discoveryType: DiscoveryType,
      type: ObjectType,
      sqlTable: Table<Record>,
      constraint: Constraint,
      constraintCounter: AtomicInteger
   ): Pair<Condition, List<SqlTemplateParameter>> {
      return when (constraint) {
         is ExpressionConstraint -> buildSqlConstraint(
            type,
            sqlTable,
            constraint,
            constraintCounter
         )

         else -> error("Sql constraints not supported for constraint type ${constraint::class.simpleName}")
      }
   }

   private fun buildSqlConstraint(
      type: ObjectType,
      sqlTable: Table<Record>,
      constraint: ExpressionConstraint,
      constraintCounter: AtomicInteger,
   ): Pair<Condition, List<SqlTemplateParameter>> {
      return when (val expression = constraint.expression) {
         is OperatorExpression -> buildSqlConstraintFromOperatorExpression(
            type,
            sqlTable,
            expression,
            constraintCounter
         )

         else -> error("Unsupported expression type: ${expression::class.simpleName}")
      }
   }

   private fun buildSqlConstraintFromOperatorExpression(
      type: ObjectType,
      sqlTable: Table<Record>,
      expression: OperatorExpression,
      constraintCounter: AtomicInteger
   ): Pair<Condition, List<SqlTemplateParameter>> {
      return when {
         expression.lhs is TypeExpression && expression.rhs is LiteralExpression -> {
            buildTypeToLiteralExpression(
               expression.lhs as TypeExpression,
               expression.rhs as LiteralExpression,
               expression.operator,
               type,
               sqlTable,
               constraintCounter
            )
         }

         expression.lhs is TypeExpression && expression.rhs is LiteralArray -> {
            buildTypeToLiteralArray(
               expression.lhs as TypeExpression,
               expression.rhs as LiteralArray,
               expression.operator,
               type,
               sqlTable,
               constraintCounter
            )
         }

         expression.lhs is OperatorExpression && expression.rhs is OperatorExpression -> {
            buildCompoundExpression(
               expression.lhs as OperatorExpression,
               expression.rhs as OperatorExpression,
               expression.operator,
               type,
               sqlTable,
               constraintCounter
            )
         }

         else -> error("Sql generation not implemented for operation expression ${expression.lhs::class.simpleName} and ${expression.rhs::class.simpleName}")
      }
   }

   private fun buildCompoundExpression(
      lhsExpression: OperatorExpression,
      rhsExpression: OperatorExpression,
      operator: FormulaOperator,
      type: ObjectType,
      sqlTable: Table<Record>,
      constraintCounter: AtomicInteger
   ): Pair<Condition, List<SqlTemplateParameter>> {
      val (lhsCondition, lhsParameter) = buildSqlConstraintFromOperatorExpression(
         type,
         sqlTable,
         lhsExpression,
         constraintCounter
      )
      val (rhsCondition, rhsParameter) = buildSqlConstraintFromOperatorExpression(
         type, sqlTable, rhsExpression, constraintCounter
      )
      val compoundCondition = when (operator) {
         FormulaOperator.LogicalAnd -> lhsCondition.and(rhsCondition)
         FormulaOperator.LogicalOr -> lhsCondition.or(rhsCondition)
         else -> error("Operator $operator not supported for compound expressions")
      }
      val params = lhsParameter + rhsParameter
      return compoundCondition to params

   }

   private fun resolveFieldInfo(
      lhs: TypeExpression,
      type: ObjectType,
      sqlTable: Table<Record>
   ): Triple<FieldReference, String, Field<Any>> {
      val fieldReference = getSingleField(type, lhs.type)
      val fieldName = fieldReference.path.single().name
      val taxiField = fieldReference.baseType.field(fieldName)
      val primitiveType = SqlTypes.getSqlType(taxiField.type.basePrimitive!!)
      val field = field(DSL.name(sqlTable.name, fieldName), primitiveType)
      return Triple(fieldReference, fieldName, field)
   }

   private fun buildTypeToLiteralExpression(
      lhs: TypeExpression,
      rhs: LiteralExpression,
      operator: FormulaOperator,
      type: ObjectType,
      sqlTable: Table<Record>,
      constraintCounter: AtomicInteger
   ): Pair<Condition, List<SqlTemplateParameter>> {
      val (_, _, field) = resolveFieldInfo(lhs, type, sqlTable)
      val primitiveType = field.dataType

      val sqlParameterName = "${field.name}${constraintCounter.getAndIncrement()}"
      val sqlParam = SqlTemplateParameter(sqlParameterName, SqlTypes.convertToSqlValue(rhs.value))

      val condition = when (operator) {
         FormulaOperator.Equal -> field.eq(DSL.param(sqlParameterName, primitiveType))
         FormulaOperator.NotEqual -> field.ne(DSL.param(sqlParameterName, primitiveType))
         FormulaOperator.LessThan -> field.lessThan(DSL.param(sqlParameterName, primitiveType))
         FormulaOperator.LessThanOrEqual -> field.lessOrEqual(DSL.param(sqlParameterName, primitiveType))
         FormulaOperator.GreaterThan -> field.greaterThan(DSL.param(sqlParameterName, primitiveType))
         FormulaOperator.GreaterThanOrEqual -> field.greaterOrEqual(DSL.param(sqlParameterName, primitiveType))
         else -> error("$operator is not yet supported in SQL clauses")
      }

      return condition to listOf(sqlParam)
   }

   private fun buildTypeToLiteralArray(
      lhs: TypeExpression,
      rhs: LiteralArray,
      operator: FormulaOperator,
      type: ObjectType,
      sqlTable: Table<Record>,
      constraintCounter: AtomicInteger
   ): Pair<Condition, List<SqlTemplateParameter>> {
      val (_, _, field) = resolveFieldInfo(lhs, type, sqlTable)
      val primitiveType = field.dataType

      // Create SQL parameters for each array value to avoid SQL injection
      val params = rhs.members.mapIndexed { index, member ->
         when (member) {
            is LiteralExpression -> {
               val sqlParameterName = "${field.name}${constraintCounter.getAndIncrement()}"
               SqlTemplateParameter(sqlParameterName, SqlTypes.convertToSqlValue(member.value))
            }
            else -> error("Array member must be a literal value, but found ${member::class.simpleName}")
         }
      }

      val paramFields = params.map { param -> DSL.param(param.nameUsedInTemplate, primitiveType) }

      val condition = when (operator) {
         FormulaOperator.In -> field.`in`(paramFields)
         FormulaOperator.NotIn -> field.notIn(paramFields)
         else -> error("$operator is not supported against array of values")
      }

      return condition to params
   }


}

private data class AliasedTableName(val type: Type, val tableName: String, val alias: String)
data class SqlTemplateParameter(val nameUsedInTemplate: String, val value: Any)

