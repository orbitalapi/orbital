package io.vyne.query.queryBuilders

import com.google.common.annotations.VisibleForTesting
import io.vyne.models.ConversionService
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedValue
import io.vyne.query.QuerySpecTypeNode
import io.vyne.query.VyneQlGrammar
import io.vyne.schemas.OutputConstraint
import io.vyne.schemas.Parameter
import io.vyne.schemas.PropertyToParameterConstraint
import io.vyne.schemas.QueryOperation


class VyneQlGrammarQueryBuilder : QueryGrammarQueryBuilder {
   override val supportedGrammars: List<String> = listOf(VyneQlGrammar.GRAMMAR_NAME)
   override fun buildQuery(spec: QuerySpecTypeNode, queryOperation: QueryOperation): Map<Parameter, TypedInstance> {
      val vyneQl = buildVyneQl(spec)
      val parameter = queryOperation.parameters.firstOrNull { it.type.name.fullyQualifiedName == VyneQlGrammar.QUERY_TYPE_NAME }
         ?: error("A vyneQl query service must accept a parameter of type ${VyneQlGrammar.QUERY_TYPE_NAME}")
      return mapOf(parameter to TypedValue.from(parameter.type, vyneQl, ConversionService.DEFAULT_CONVERTER, source = Provided))
   }

   @VisibleForTesting
   internal fun buildVyneQl(spec: QuerySpecTypeNode):String {
      return """findAll { ${spec.type.name.parameterizedName}(
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
