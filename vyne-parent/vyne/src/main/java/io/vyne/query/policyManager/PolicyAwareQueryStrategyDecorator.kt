package io.vyne.query.policyManager

import io.vyne.query.QueryContext
import io.vyne.query.QuerySpecTypeNode
import io.vyne.query.QueryStrategy
import io.vyne.query.QueryStrategyResult

class PolicyAwareQueryStrategyDecorator(private val queryStrategy: QueryStrategy, val evaluator: PolicyEvaluator = PolicyEvaluator()) : QueryStrategy {
   override fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult {
      evaluator.evaluate(target,context)

      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

}
