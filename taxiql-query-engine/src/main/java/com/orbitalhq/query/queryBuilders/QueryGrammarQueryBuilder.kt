package com.orbitalhq.query.queryBuilders

import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.QuerySpecTypeNode
import com.orbitalhq.schemas.*
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import com.orbitalhq.utils.asA
import lang.taxi.services.operations.constraints.ArgumentExpression
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
      schema: Schema,
      context: QueryContext
   ): Map<Parameter, TypedInstance>

   fun convertConstraintsToTypedInstances(
      dataConstraints: List<OutputConstraint>,
      schema: Schema,
      context: QueryContext
   ): Map<OutputConstraint,TypedInstance> {
      return dataConstraints.associateWith { outputConstraint ->
         val typedInstance = when (outputConstraint) {
            is PropertyToParameterConstraint -> convertPropertyToParameterConstraint(outputConstraint, schema, context)
            else -> TODO("Mapping of constraint type ${outputConstraint::class.simpleName} to TypedInstance not yet implemented")
         }
         typedInstance
      }
   }

   fun convertPropertyToParameterConstraint(
      outputConstraint: PropertyToParameterConstraint,
      schema: Schema,
      context: QueryContext
   ):TypedInstance {
      return when {
         outputConstraint.propertyIdentifier is PropertyTypeIdentifier && outputConstraint.expectedValue is ConstantValueExpression -> {
            constantValueToTypedInstance(outputConstraint, schema)
         }
         outputConstraint.propertyIdentifier is PropertyTypeIdentifier && outputConstraint.expectedValue is ArgumentExpression -> {
            argumentExpressionToTypedInstance(outputConstraint.expectedValue as ArgumentExpression, schema, context)
         }
         else -> TODO("Mapping on PropertyToParameterConstraint to TypedInstance is not supported for constraint ${this}")
      }
   }

   fun argumentExpressionToTypedInstance(argumentExpression: ArgumentExpression, schema: Schema, context: QueryContext): TypedInstance {
      val fromFactbag = context.facts.getScopedFactOrNull(argumentExpression.argument.scope)
      if (fromFactbag != null) {
         return fromFactbag.fact
      }
      return when (val scope = argumentExpression.argument.scope) {
         is lang.taxi.query.Parameter -> {
            TypedInstance.from(scope.value.typedValue, schema, Provided)
         }
         else -> TODO("Cannot resolve argument expression with scope type of ${scope::class.simpleName}")
      }
   }

   fun constantValueToTypedInstance(
      outputConstraint: PropertyToParameterConstraint,
      schema: Schema
   ): TypedInstance {
      val qualifiedName = outputConstraint.propertyIdentifier.asA<PropertyTypeIdentifier>().type
      return TypedInstance.from(
         schema.type(qualifiedName.toVyneQualifiedName()),
         outputConstraint.expectedValue.asA<ConstantValueExpression>().value,
         schema
      )
   }
}
