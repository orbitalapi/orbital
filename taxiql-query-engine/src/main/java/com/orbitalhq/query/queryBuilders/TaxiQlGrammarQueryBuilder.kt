package com.orbitalhq.query.queryBuilders

import com.google.common.annotations.VisibleForTesting
import com.orbitalhq.models.ConversionService
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedValue
import com.orbitalhq.query.ConstructedQueryDataSource
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.QuerySpecTypeNode
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOperation
import com.orbitalhq.schemas.Schema
import lang.taxi.accessors.LiteralAccessor
import lang.taxi.expressions.Expression
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.services.operations.constraints.ExpressionConstraint
import lang.taxi.types.ArgumentSelector
import lang.taxi.types.MemberTypeReferenceExpression
import mu.KotlinLogging

/**
 * Responsible for taking a QuerySpecNode and turning it into a TaxiQL query.
 * Generally, this is used generating subqueries from the main query, to hand off
 * to services that support querying, such as Databases.
 *
 * Later, this TaxiQL query gets turned into the actual query language (eg. SQL)
 */
class TaxiQlGrammarQueryBuilder : QueryGrammarQueryBuilder {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override val supportedGrammars: List<String> = listOf(VyneQlGrammar.GRAMMAR_NAME)
   override fun buildQuery(
      spec: QuerySpecTypeNode,
      queryOperation: QueryOperation,
      schema: Schema,
      context: QueryContext
   ): Map<Parameter, TypedInstance> {

      val parameter =
         queryOperation.parameters.firstOrNull { it.type.name.fullyQualifiedName == VyneQlGrammar.QUERY_TYPE_NAME }
            ?: error("A TaxiQL query service must accept a parameter of type ${VyneQlGrammar.QUERY_TYPE_NAME}")
      val (taxiQl, resolvedVariables) = buildTaxiQl(spec, context)
      return mapOf(
         parameter to TypedValue.from(
            type = parameter.type,
            value = taxiQl,
            converter = ConversionService.DEFAULT_CONVERTER,
            source = ConstructedQueryDataSource(resolvedVariables)
         )
      )
   }

   @VisibleForTesting
   internal fun buildTaxiQl(spec: QuerySpecTypeNode, context: QueryContext): Pair<String, List<TypedInstance>> {
      val constraints = spec.dataConstraints
      if (constraints.size > 1) {
         logger.warn { "Received multiple constraints - expected a single, compound constraint. ${constraints.joinToString()}" }
      }

      // In converting the expressions to Taxi, we also resolve any placeholder variables
      // using the context
      val statementsAndValues = constraints.map { buildConstraint(it, context) }
      val constraintsStatement = statementsAndValues.joinToString("\n", prefix = "(\n", postfix = "\n)") { it.first }
      val resolvedValues = statementsAndValues.flatMap { it.second }
      return """find { ${spec.type.name.parameterizedName}${constraintsStatement} }""" to resolvedValues
   }

   private fun buildConstraint(constraint: Constraint, context: QueryContext): Pair<String, List<TypedInstance>> {
      return when (constraint) {
         is ExpressionConstraint -> buildExpressionConstraint(constraint, context)
         else -> error("Support for constraint type ${constraint::class.simpleName} not implemented yet")
      }
   }

   private fun buildExpressionConstraint(constraint: ExpressionConstraint, context: QueryContext): Pair<String,List<TypedInstance>> {
      val (resolvedExpression, typedInstances) = constraint.expression.resolveVariablesUsing(context)
      return resolvedExpression.asTaxi() to typedInstances
   }
}


/**
 * Resolves variables against the query context where possible,
 * returning a new expression where things like ArgumentSelectors have been replaced
 * with Literals
 */
fun Expression.resolveVariablesUsing(context:QueryContext):Pair<Expression,List<TypedInstance>> {
   return when(this) {
      is OperatorExpression -> {
         val (lhs,lhsInstances) = lhs.resolveVariablesUsing(context)
         val (rhs,rhsInstances) = rhs.resolveVariablesUsing(context)
         OperatorExpression(lhs, operator, rhs, compilationUnits) to lhsInstances + rhsInstances
      }
      is LiteralExpression -> {
         this to listOf(context.evaluate(this))
      }
      is ArgumentSelector -> {
         val value = context.evaluate(this)
         LiteralExpression(LiteralAccessor(value.value!!, value.type.taxiType), this.compilationUnits) to listOf(value)
      }
      is MemberTypeReferenceExpression -> {
         val value = context.evaluate(this)
         LiteralExpression(LiteralAccessor(value.value!!, value.type.taxiType), this.compilationUnits) to listOf(value)
      }
       else -> this to emptyList()
   }
}
