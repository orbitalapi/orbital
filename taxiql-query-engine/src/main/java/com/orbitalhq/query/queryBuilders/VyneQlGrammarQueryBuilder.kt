package com.orbitalhq.query.queryBuilders

import com.google.common.annotations.VisibleForTesting
import com.orbitalhq.models.ConversionService
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedValue
import com.orbitalhq.query.ConstructedQueryDataSource
import com.orbitalhq.query.QuerySpecTypeNode
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.*


class VyneQlGrammarQueryBuilder : QueryGrammarQueryBuilder {
   override val supportedGrammars: List<String> = listOf(VyneQlGrammar.GRAMMAR_NAME)
   override fun buildQuery(
      spec: QuerySpecTypeNode,
      queryOperation: QueryOperation,
      schema: Schema
   ): Map<Parameter, TypedInstance> {
      val vyneQl = buildVyneQl(spec)
      val parameter =
         queryOperation.parameters.firstOrNull { it.type.name.fullyQualifiedName == VyneQlGrammar.QUERY_TYPE_NAME }
            ?: error("A vyneQl query service must accept a parameter of type ${VyneQlGrammar.QUERY_TYPE_NAME}")
      val constraintsAsTypedInstances = convertConstraintsToTypedInstances(spec.dataConstraints, schema)
      return mapOf(
         parameter to TypedValue.from(
            type = parameter.type,
            value = vyneQl,
            converter = ConversionService.DEFAULT_CONVERTER,
            source = ConstructedQueryDataSource(constraintsAsTypedInstances)
         )
      )
   }

   @VisibleForTesting
   internal fun buildVyneQl(spec: QuerySpecTypeNode): String {
      return """find { ${spec.type.name.parameterizedName}(
            |     ${spec.dataConstraints.joinToString(", \n") { buildConstraint(it) }}
            |   )
            |}
         """.trimMargin()
   }

   private fun buildConstraint(constraint: OutputConstraint): String {
      return when (constraint) {
         is PropertyToParameterConstraint -> buildPropertyConstraint(constraint)
         else -> error("Support for constraint type ${constraint::class.simpleName} not implemented yet")
      }
   }

   private fun buildPropertyConstraint(constraint: PropertyToParameterConstraint): String {
      return constraint.asTaxi()
   }


}
