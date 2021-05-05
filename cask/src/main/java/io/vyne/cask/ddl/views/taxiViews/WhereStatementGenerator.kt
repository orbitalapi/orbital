package io.vyne.cask.ddl.views.taxiViews

import io.vyne.cask.api.CaskConfig
import lang.taxi.Operator
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.PropertyToParameterConstraint
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.types.AndFilterExpression
import lang.taxi.types.FilterExpression
import lang.taxi.types.FilterExpressionInParenthesis
import lang.taxi.types.InFilterExpression
import lang.taxi.types.LikeFilterExpression
import lang.taxi.types.NotInFilterExpression
import lang.taxi.types.OrFilterExpression
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type

/**
 * Generates Sql WHERE Statements for 'find' expressions when the types in a find expression have constraint filters.
 * Example:
 *    view MaleView with query {
        find { Person[] ( (Sex = 'Male') )} as {
        sex: Person::Sex
      }
    }

     Person[] has a constraint filter (Sex = 'Male')
     this generator will translate above constraint to a Sql When Statement:

     SELECT .....
     FROM person_table_name
     WHERE person_table_name."sex" = 'MALE'
 */
class WhereStatementGenerator(
   private val viewGenerator: SchemaBasedViewGenerator,
   private val tableNamesForSourceTypes: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>) {
   /**
    * Returns WHERE Sql Statement for the given (Type, FilterExpression).
    * @param typeFilterExpressionPair  First Item in the pair is the type for which the filter expression is defined.
    * @return Sql Where statement if typeFilterExpressionPair is not null otherwise " "
    */
   fun whereStatement(typeFilterExpressionPair: Pair<Type, FilterExpression>?): String {
      return if (typeFilterExpressionPair == null) {
          " "
      } else {
         filterExpressionToString(typeFilterExpressionPair)
      }
   }

   /**
    * Returns WHERE Sql Statement for the given (Type, FilterExpression) arguments.
    * @param filterExpressionBody  First Item in the pair is the type for which the filter expression is defined.
    * @param filterExpressionJoin First Item in the pair is the type for which the filter expression is defined.
    * @return
    *    when {
    *    filterExpressionBody != null && filterExpressionJoin != null -> WHERE (WhereSqlForfilterExpressionBody) AND (WhereSqlForfilterExpressionJoin)
    *    filterExpressionBody != null && filterExpressionJoin == null -> WHERE (WhereSqlForfilterExpressionBody)
    *    filterExpressionBody == null && filterExpressionJoin != null -> WHERE (WhereSqlfilterExpressionJoin)
    *    filterExpressionBody == null && filterExpressionJoin == null -> " "
    *    }
    */
   fun whereStatement(filterExpressionBody: Pair<Type, FilterExpression>?, filterExpressionJoin: Pair<Type, FilterExpression>?): String {
      return when {
         filterExpressionBody == null && filterExpressionJoin == null -> " "
         filterExpressionBody != null && filterExpressionJoin == null -> " WHERE ${whereStatement(filterExpressionBody)}"
         filterExpressionBody == null && filterExpressionJoin != null -> " WHERE ${whereStatement(filterExpressionJoin)}"
         filterExpressionBody != null && filterExpressionJoin != null -> "WHERE ${whereStatement(filterExpressionBody)} AND ${whereStatement(filterExpressionJoin)}"
         else -> " "
      }
   }

   private fun filterExpressionToString(typeFilterExpressionPair: Pair<Type, FilterExpression>): String {
      val (type, filterExpression)  = typeFilterExpressionPair
      return when (filterExpression) {
         is FilterExpressionInParenthesis -> "( ${filterExpressionToString(Pair(type,filterExpression.containedExpression))} )"
         is InFilterExpression -> inFilterToSql(Pair(type, filterExpression))
         is LikeFilterExpression -> likeFilterToSql(Pair(type, filterExpression))
         is AndFilterExpression -> "(${filterExpressionToString(Pair(type, filterExpression.filterLeft))} AND  ${filterExpressionToString(Pair(type, filterExpression.filterRight))})"
         is OrFilterExpression -> "(${filterExpressionToString(Pair(type, filterExpression.filterLeft))} OR  ${filterExpressionToString(Pair(type, filterExpression.filterRight))})"
         is PropertyToParameterConstraint -> propertyToParameterConstraintToSql(Pair(type, filterExpression))
         is NotInFilterExpression -> notInFilteToSql(Pair(type, filterExpression))
         else -> ""
      }
   }

   /**
    * Generates  NOT IN ['one', 'two']  Sql Statement for the given filter expression.
    */
   private fun notInFilteToSql(typeInFilterExpressionPair: Pair<Type, NotInFilterExpression>): String {
      val (type, inFilterExpression) = typeInFilterExpressionPair
      val inValues = if (inFilterExpression.values.first() is String) {
         inFilterExpression.values.joinToString(",") { "'$it'" }
      } else {
         inFilterExpression.values.joinToString { "," }
      }
      return " ${viewGenerator.columnName(type.toQualifiedName(), inFilterExpression.type, tableNamesForSourceTypes)} NOT IN ( $inValues ) "

   }

   private fun propertyToParameterConstraintToSql(pair: Pair<Type, PropertyToParameterConstraint>): String {
      val (type, propertyToParameterConstraint) = pair
      val propIdentifier = propertyToParameterConstraint.propertyIdentifier as PropertyTypeIdentifier
      val columnExpression = viewGenerator.columnName(type.toQualifiedName(), propIdentifier.type, tableNamesForSourceTypes)
      val valueExpression = propertyToParameterConstraint.expectedValue as ConstantValueExpression
      val rhs = if (valueExpression.value is String) {
         "'${valueExpression.value}'"
      } else "${valueExpression.value}"

      return " $columnExpression ${operatorSymbolToSqlSymbol(propertyToParameterConstraint.operator)} $rhs "
   }

   private fun operatorSymbolToSqlSymbol(operator: Operator): String {
      return when(operator) {
         Operator.NOT_EQUAL -> "<>"
         else -> operator.symbol
      }
   }

   private fun likeFilterToSql(pair: Pair<Type, LikeFilterExpression>): String {
      val (type, likeFilterExpression) = pair
      return " ${viewGenerator.columnName(type.toQualifiedName(), likeFilterExpression.type, tableNamesForSourceTypes)} LIKE '${likeFilterExpression.value}'"
   }

   private fun inFilterToSql(typeInFilterExpressionPair:  Pair<Type, InFilterExpression>): String {
      val (type, inFilterExpression) = typeInFilterExpressionPair
      val inValues = if (inFilterExpression.values.first() is String) {
         inFilterExpression.values.joinToString(",") { "'$it'" }
      } else {
         inFilterExpression.values.joinToString { "," }
      }
      return " ${viewGenerator.columnName(type.toQualifiedName(), inFilterExpression.type, tableNamesForSourceTypes)} IN ( $inValues ) "
   }
}
