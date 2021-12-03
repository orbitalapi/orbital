package io.vyne.query

interface QueryStrategyValidPredicate {
   fun isApplicable(queryStrategy: QueryStrategy): Boolean
}

object AllIsApplicableQueryStrategyPredicate: QueryStrategyValidPredicate {
   override fun isApplicable(queryStrategy: QueryStrategy) = true
}

class ExcludeQueryStrategyKlassPredicate<T : QueryStrategy>(private val queryStrategyClassToExclude: Class<T>): QueryStrategyValidPredicate {
   override fun isApplicable(queryStrategy: QueryStrategy): Boolean {
      return queryStrategy::class.java != queryStrategyClassToExclude
   }

   companion object {
      val ExcludeObjectBuilderPredicate = ExcludeQueryStrategyKlassPredicate(ObjectBuilderStrategy::class.java)
   }
}
