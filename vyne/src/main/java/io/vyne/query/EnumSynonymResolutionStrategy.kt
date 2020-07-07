package io.vyne.query

import io.vyne.models.TypedInstance
import io.vyne.schemas.Type

class EnumSynonymResolutionStrategy: QueryStrategy {
   override fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult {
      val targetTypes: Map<Type, QuerySpecTypeNode> = target.associateBy { it.type }
      val matches = targetTypes
         .map { (type, querySpec) -> querySpec to resolveThroughSynonyms(type, context) }
         .filter { it.second != null }
         .toMap()
      return QueryStrategyResult(matches)
   }

    fun resolveThroughSynonyms(targetType: Type, context: QueryContext): TypedInstance? {
      if (!targetType.isEnum) {
         return null
      }
      return context.enumsWithSynonyms()?.firstOrNull { it.type.isAssignableTo(targetType) }
   }
}
