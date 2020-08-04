package io.vyne.query.planner

import io.vyne.models.TypedInstance

class ProjectionResultList(results: Collection<TypedInstance>) {

   private val container = ArrayList(results)
   private var headIndex = 0
   fun removeFirst() {
      headIndex++
   }

   fun first(): TypedInstance {
      val head = container[headIndex]
      if (headIndex == (container.size - 1)) {
         headIndex = 0
      }
      return head
   }

   fun <R> map(transform: (TypedInstance) -> R): List<R> {
      return this.container.drop(headIndex).map { typedInstance -> transform(typedInstance) }
   }

   val size: Int
      get() = container.size - headIndex
}


fun Collection<TypedInstance>.toProjectionResultList(): ProjectionResultList {
   return ProjectionResultList(this)
}

