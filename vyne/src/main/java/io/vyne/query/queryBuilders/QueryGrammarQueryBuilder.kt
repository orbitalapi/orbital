package io.vyne.query.queryBuilders

import io.vyne.models.TypedInstance
import io.vyne.query.QuerySpecTypeNode
import io.vyne.schemas.Parameter
import io.vyne.schemas.QueryOperation

interface QueryGrammarQueryBuilder {
   val supportedGrammars: List<String>
   fun canSupport(grammar: String): Boolean {
      return this.supportedGrammars.contains(grammar)
   }
   fun buildQuery(spec: QuerySpecTypeNode, queryOperation: QueryOperation):Map<Parameter, TypedInstance>
}
