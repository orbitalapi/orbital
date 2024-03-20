package com.orbitalhq.query.queryBuilders

import com.google.common.annotations.VisibleForTesting
import com.orbitalhq.models.ConversionService
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedValue
import com.orbitalhq.query.ConstructedQueryDataSource
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.QuerySpecTypeNode
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.*
import lang.taxi.services.operations.constraints.ArgumentExpression
import lang.taxi.utils.quotedIfNecessary

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
            ?: error("A vyneQl query service must accept a parameter of type ${VyneQlGrammar.QUERY_TYPE_NAME}")
      val constraintsAsTypedInstances = convertConstraintsToTypedInstances(spec.dataConstraints, schema, context)
      val vyneQl = buildTaxiQl(spec, constraintsAsTypedInstances)
      return mapOf(
         parameter to TypedValue.from(
            type = parameter.type,
            value = vyneQl,
            converter = ConversionService.DEFAULT_CONVERTER,
            source = ConstructedQueryDataSource(constraintsAsTypedInstances.values.toList())
         )
      )
   }

   @VisibleForTesting
   internal fun buildTaxiQl(spec: QuerySpecTypeNode, constraintsAsTypedInstances: Map<OutputConstraint, TypedInstance>): String {
      return """find { ${spec.type.name.parameterizedName}(
            ^     ${constraintsAsTypedInstances.entries.joinToString(" \n") { (constraint, value) -> buildConstraint(constraint,value) }}
            ^   )
            ^}
         """.trimMargin("^")
   }

   private fun buildConstraint(constraint: OutputConstraint, value: TypedInstance): String {
      return when (constraint) {
         is PropertyToParameterConstraint -> buildPropertyConstraint(constraint, value)
         is OperatorExpressionConstraint -> " ${value.value} "
         else -> error("Support for constraint type ${constraint::class.simpleName} not implemented yet")
      }
   }

   private fun buildPropertyConstraint(constraint: PropertyToParameterConstraint, value: TypedInstance): String {
      return when (constraint.expectedValue) {
         is ArgumentExpression -> {
            // Can't use .asTaxi() here, as the taxi contains a reference to a variable, which we need to substitute
            // Foo == 123
            "${constraint.propertyIdentifier.taxi} ${constraint.operator.symbol} ${value.toRawObject()?.quotedIfNecessary() ?: "null"}"
         }

         else -> constraint.asTaxi()
      }
   }


}
