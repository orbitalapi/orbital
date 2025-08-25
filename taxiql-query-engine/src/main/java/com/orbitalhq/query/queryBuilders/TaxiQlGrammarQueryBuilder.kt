package com.orbitalhq.query.queryBuilders

import com.google.common.annotations.VisibleForTesting
import com.orbitalhq.models.ConversionService
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedValue
import com.orbitalhq.query.ConstructedQueryDataSource
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.QuerySpecTypeNode
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOperation
import com.orbitalhq.schemas.Schema
import lang.taxi.accessors.LiteralAccessor
import lang.taxi.accessors.NullValue
import lang.taxi.expressions.Expression
import lang.taxi.expressions.ExtensionFunctionExpression
import lang.taxi.expressions.LiteralArray
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.services.operations.constraints.ExpressionConstraint
import lang.taxi.types.ArgumentSelector
import lang.taxi.types.CompilationUnit
import lang.taxi.types.MemberTypeReferenceExpression
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Responsible for taking a QuerySpecNode and turning it into a TaxiQL query.
 * Generally, this is used generating subqueries from the main query, to hand off
 * to services that support querying, such as Databases.
 *
 * Later, this TaxiQL query gets turned into the actual query language (eg. SQL)
 */
class TaxiQlGrammarQueryBuilder : QueryGrammarQueryBuilder {
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

   private fun buildExpressionConstraint(
      constraint: ExpressionConstraint,
      context: QueryContext
   ): Pair<String, List<TypedInstance>> {
      val (resolvedExpression, typedInstances) = constraint.expression.resolveVariablesUsing(context)
      return resolvedExpression.asTaxi() to typedInstances
   }
}


private fun typedInstanceToLiteralExpressionAndValues(
   value: TypedInstance,
   compilationUnits: List<CompilationUnit>
): Pair<Expression, List<TypedInstance>> {
   val result = when (value) {
      is TypedCollection -> {
         val members = value.map {
            LiteralExpression(
               LiteralAccessor(it.value ?: NullValue, it.type.taxiType),
               compilationUnits
            )
         }
         // When we return an expression of the LiteralArray,
         // it must use a compilationUnit with the values resolved against their literal values,
         // not their originating expressions,
         // eg: [1,2,3], not Foo::Bar
         // This compilationUnit is what's ultimately used to generate a TaxiQL statement
         val literalMembers = members.joinToString(prefix = "[", postfix = "]", separator = ",") { it.asTaxi() }
         val updatedCompilationUnits = compilationUnits.map {
            it.copy(source = it.source.copy(content = literalMembers))
         }
         LiteralArray(value.type.taxiType, members, updatedCompilationUnits) to value.value
      }

      else -> {
         val literalValue = if (value.value == null) {
            logger.warn { "Encountered null whilst converting an input for a TaxiQL query. ${value.type.qualifiedName.shortDisplayName} - Source: ${value.source}" }
            NullValue
         } else value.value!!
         LiteralExpression(LiteralAccessor(literalValue, value.type.taxiType), compilationUnits) to listOf(
            value
         )
      }

   }
   return result
}

/**
 * Resolves variables against the query context where possible,
 * returning a new expression where things like ArgumentSelectors have been replaced
 * with Literals
 */
fun Expression.resolveVariablesUsing(context: QueryContext): Pair<Expression, List<TypedInstance>> {
   return when (this) {
      is OperatorExpression -> {
         val (lhs, lhsInstances) = lhs.resolveVariablesUsing(context)
         val (rhs, rhsInstances) = rhs.resolveVariablesUsing(context)
         OperatorExpression(lhs, operator, rhs, compilationUnits) to lhsInstances + rhsInstances
      }

      is LiteralExpression -> {
         this to listOf(context.evaluate(this))
      }

      is LiteralArray -> {
         val resolved = this.members.map {
            it.resolveVariablesUsing(context)
         }
         val resolvedList = resolved.flatMap { it.second }

         // The returned LiteralArray must have the resolved actual values in the compilation units,
         // as that's how this will ultimately be converted back into a TaxiQL statement.
         this to resolvedList
      }

      // TODO : ORB-1009
      // This exists because the way we construct our criteria is broken.
      // We'll say something like:
      // Film(FilmId == 123)
      // Which infers "The filmId of the FIlm is 123)"
      // However, there's nothing in that statement that links FilmId to Film
      // It should probably be:
      // Film((Film) -> Film::FilmId == 123) <--- ❌ Don't do this, too much boilerplate
      // or:
      // Film(it::FilmId == 123)  <--- ✔️ do this one
      // ie., we need to qualify the FilmId part
      // Otherwise, consider a statament like:
      // given { targetFilmId:FilmId = 123 }
      // find { Film(FilmId = FilmId) }
      // That statement can't work, as there's no way to know which FilmId is resolving from
      // context vs the db
      is TypeExpression -> {
         this to emptyList()
      }

      else -> {
         val value = context.evaluate(this)
         typedInstanceToLiteralExpressionAndValues(value, this.compilationUnits)
      }
   }
}
