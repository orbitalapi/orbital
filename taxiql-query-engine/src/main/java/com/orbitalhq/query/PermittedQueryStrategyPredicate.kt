package com.orbitalhq.query

import com.orbitalhq.models.PermittedQueryStrategies

interface PermittedQueryStrategyPredicate {
   fun isApplicable(queryStrategy: QueryStrategy): Boolean

   companion object {
      fun forEnum(enum: PermittedQueryStrategies): PermittedQueryStrategyPredicate {
         return when (enum) {
            PermittedQueryStrategies.EVERYTHING -> AllIsApplicableQueryStrategyPredicate
            PermittedQueryStrategies.EXCLUDE_BUILDER -> ExcludeQueryStrategyKlassPredicate.ExcludeObjectBuilder
            PermittedQueryStrategies.EXCLUDE_BUILDER_AND_MODEL_SCAN -> ExcludeQueryStrategyKlassPredicate.ExcludeObjectBuilderAndModelScan
         }
      }
   }
}

object AllIsApplicableQueryStrategyPredicate : PermittedQueryStrategyPredicate {
   override fun isApplicable(queryStrategy: QueryStrategy) = true
}

class ExcludeQueryStrategyKlassPredicate<T : QueryStrategy>(private val queryStrategyClassToExclude: List<Class<T>>) :
   PermittedQueryStrategyPredicate {
   constructor(queryStrategyClassToExclude: Class<T>) : this(listOf(queryStrategyClassToExclude))

   override fun isApplicable(queryStrategy: QueryStrategy): Boolean {
      return queryStrategyClassToExclude.none { it == queryStrategy::class.java }
   }

   companion object {
      val ExcludeObjectBuilder = ExcludeQueryStrategyKlassPredicate(ObjectBuilderStrategy::class.java)
      val ExcludeObjectBuilderAndModelScan = ExcludeQueryStrategyKlassPredicate(
         listOf(
            ObjectBuilderStrategy::class.java,
            ModelsScanStrategy::class.java
         ) as List<Class<QueryStrategy>>
      )
   }
}
