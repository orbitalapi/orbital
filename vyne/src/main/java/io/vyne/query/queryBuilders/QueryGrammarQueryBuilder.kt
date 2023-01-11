package io.vyne.query.queryBuilders

import io.vyne.models.TypedInstance
import io.vyne.query.QuerySpecTypeNode
import io.vyne.schemas.*
import io.vyne.schemas.taxi.toVyneQualifiedName
import io.vyne.utils.asA
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier

interface QueryGrammarQueryBuilder {
   val supportedGrammars: List<String>
   fun canSupport(grammar: String): Boolean {
      return this.supportedGrammars.contains(grammar)
   }

   fun buildQuery(
      spec: QuerySpecTypeNode,
      queryOperation: QueryOperation,
      schema: Schema
   ): Map<Parameter, TypedInstance>

   fun convertConstraintsToTypedInstances(
      dataConstraints: List<OutputConstraint>,
      schema: Schema
   ): List<TypedInstance> {
      return dataConstraints.map { outputConstraint ->
         when (outputConstraint) {
            is PropertyToParameterConstraint -> outputConstraint.toTypedInstance(schema)
            else -> TODO("Mapping of constraint type ${outputConstraint::class.simpleName} to TypedInstance not yet implemented")
         }
      }
   }
}

fun PropertyToParameterConstraint.toTypedInstance(schema: Schema): TypedInstance {
   return when {
      this.propertyIdentifier is PropertyTypeIdentifier && this.expectedValue is ConstantValueExpression -> {
         val qualifiedName = this.propertyIdentifier.asA<PropertyTypeIdentifier>().type
         TypedInstance.from(
            schema.type(qualifiedName.toVyneQualifiedName()),
            this.expectedValue.asA<ConstantValueExpression>().value,
            schema
         )
      }
      else -> TODO("Mapping on PropertyToParameterConstraint to TypedInstance is not supported for constraint ${this}")
   }
}
