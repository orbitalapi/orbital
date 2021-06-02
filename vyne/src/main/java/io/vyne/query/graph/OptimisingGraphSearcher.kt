package io.vyne.query.graph

import io.vyne.query.QuerySpecTypeNode

/**
 * This is a graph searcher which over time optimises
 * the routes taken.
 *
 * When searching across a set of facts, favour the facts that have provided results before.
 * When searching, if the set of fact types is the same set of fact types that didn't provide a path previously, don't bother researching.
 * Note we always evaluate a provided path, as things like remote calls may change the outcome.
 *
 */
class OptimisingGraphSearcher {

   fun searchFor(target:QuerySpecTypeNode) {
      
   }
}
