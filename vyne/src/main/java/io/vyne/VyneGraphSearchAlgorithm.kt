package io.vyne

import es.usc.citius.hipster.algorithm.Algorithm
import es.usc.citius.hipster.model.HeuristicNode
import es.usc.citius.hipster.model.function.NodeExpander
import es.usc.citius.hipster.model.problem.SearchProblem
import es.usc.citius.hipster.util.Predicate
import io.vyne.query.graph.EvaluatedPathSet
import mu.KotlinLogging
import java.util.PriorityQueue
import java.util.Queue

private val logger = KotlinLogging.logger {}

/**
 * This is just an import from Hipster AStar implementation to allow for a future extension where
 * we can use evaluatedEdges to assist the search process. The idea is that previously tried but failed search paths
 * stored in evaluatedEdges should provide additional heuristics to the algorithm so that it will not discover these paths again.
 */
class VyneGraphSearchAlgorithm<A, S, C : Comparable<C>, N : HeuristicNode<A, S, C, N>>
(
   private val initialNode: N,
   private val expander: NodeExpander<A, S, N>,
   private val evaluatedEdges: EvaluatedPathSet,
   private val problem: SearchProblem<A, S, N>
   ) : Algorithm<A, S, N>() {
   private  var goalState: S? = null
   override fun iterator(): Iterator {
      return Iterator(this::minCost, this.evaluatedEdges)
   }

   override fun search(goalState: S): SearchResult {
      this.goalState = goalState
      return this.search(Predicate { n -> if (goalState != null) n.state() == goalState else false })
   }

   override fun search(condition: Predicate<N>): SearchResult {
      val it: kotlin.collections.Iterator<N> = Iterator(this::minCost, this.evaluatedEdges)
      return doSearch(condition, it)
   }

   private fun doSearch(condition: Predicate<N>, it: kotlin.collections.Iterator<N>): SearchResult {
      var iteration = 0
      val begin = System.currentTimeMillis()
      var currentNode: N? = null

      while (it.hasNext()) {
         ++iteration
         currentNode = it.next()
         if (condition.apply(currentNode)) {
            break
         }
      }

      val elapsed = System.currentTimeMillis() - begin
      /**
       *  Here we can detect the path we're about to return already tried and failed.
       *  and then we can 'backtrack' on this path an re-initiate the search from a backtracked location by avoiding
       *  the same path which needs to be passed into inner class Iterator that will use this information to filter out the expanded nodes.
       *  The difficulty here is determining a reliable backtracking strategy, i.e. where to return to initiate the new search?
      */
     /* if (currentNode != null &&
         currentNode is WeightedNode<*, *, *> &&
         this.evaluatedEdges.containsPath(currentNode as WeightedNode<Relationship, Element, Double>)) {
          logger.info { "Returning an already evaluated Path!" }
      }*/
      logger.debug { "Search Completed from start ${problem.initialNode} for target $goalState in $elapsed msecs" }
      return SearchResult(currentNode, iteration, elapsed)
   }

   fun minCost(currentScore: C, successorScore: C) = currentScore <= successorScore

   /**
    * Iterator is supplied with a cost comparison function to try different strategies, but currently we're only passing
    * minCost to implement AStar.
    */
   inner class Iterator(private val scoreFunc: (currentScore: C, successorScore: C) -> Boolean,
                        private val evaluatedEdges: EvaluatedPathSet
   ) : MutableIterator<N> {
      private val open: MutableMap<S, N> = mutableMapOf()
      private val closed: MutableMap<S, N> = mutableMapOf()
      private val queue: Queue<N> = PriorityQueue()
      override fun hasNext(): Boolean {
         return !open.values.isEmpty()
      }

      private fun takePromising(): N {
         var node = queue.poll() as HeuristicNode<A, S, C, N>
         while (!open.containsKey(node.state())) {
            node = queue.poll() as HeuristicNode<A, S, C, N>
         }
         return node as N
      }

      override fun next(): N {
         val current = takePromising()
         val currentState = current.state()
         open.remove(currentState)
         val var3: kotlin.collections.Iterator<*> = expander.expand(current).iterator()
         while (true) {
            var successorNode: HeuristicNode<A, S, C, N>
            var successorClose: HeuristicNode<A, S, C, N>?
            do {
               var successorOpen:HeuristicNode<A, S, C, N>?
               do {
                  if (!var3.hasNext()) {
                     closed[currentState] = current
                     return current
                  }
                  successorNode = var3.next() as HeuristicNode<A, S, C, N>
                  successorOpen = open[successorNode.state()]
               } while (successorOpen != null && scoreFunc(successorOpen.score, successorNode.score))
               successorClose = closed[successorNode.state()]
            } while (successorClose != null && scoreFunc(successorClose.score,successorNode.score))
            open[successorNode.state() as S] = successorNode as N
            queue.add(successorNode)
         }
      }

      override fun remove() {
         throw UnsupportedOperationException()
      }

      init {
         queue.add(initialNode)
         open[initialNode.state()] = initialNode
      }
   }

   companion object {
      fun <A, S, C : Comparable<C>, N : HeuristicNode<A, S, C, N>>
         create (problem: SearchProblem<A, S, N>, evaluatedEdges: EvaluatedPathSet): Algorithm<A, S, N> {
         return VyneGraphSearchAlgorithm(problem.initialNode, problem.expander, evaluatedEdges, problem)
      }
   }
}


