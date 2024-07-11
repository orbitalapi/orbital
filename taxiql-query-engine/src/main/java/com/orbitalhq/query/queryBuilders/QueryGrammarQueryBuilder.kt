package com.orbitalhq.query.queryBuilders

import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.UndefinedSource
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.QuerySpecTypeNode
import com.orbitalhq.schemas.*
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import com.orbitalhq.utils.asA
import lang.taxi.query.convertToConstraint
import lang.taxi.services.operations.constraints.ArgumentExpression
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.services.operations.constraints.ExpressionConstraint
import lang.taxi.services.operations.constraints.PropertyToParameterConstraint
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.types.PrimitiveType

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
}
